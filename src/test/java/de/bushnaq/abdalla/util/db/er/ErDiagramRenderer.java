/*
 *
 * Copyright (C) 2025-2026 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.bushnaq.abdalla.util.db.er;

import lombok.extern.slf4j.Slf4j;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import java.awt.*;
import java.awt.geom.Path2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

/**
 * Renders an {@link ErSchema} as an SVG file.
 *
 * <h2>Layout</h2>
 * Tables are placed using a FK-aware forest layout.  Each FK relationship
 * defines a parent→child edge; BFS assigns a column (depth from root) to
 * every connected table, and post-order DFS assigns rows so children are
 * always stacked directly below their parent's row.  Tables with no FK
 * edges (isolated tables) are packed in a compact grid below the connected
 * section.
 *
 * <h2>Connectors</h2>
 * Because parents are always to the <em>left</em> of their children in the
 * tree layout, connectors exit from the <strong>left</strong> edge of the FK
 * (child) table, route through the inter-column gap, and enter the
 * <strong>right</strong> edge of the PK (parent) table.  A filled arrowhead
 * marks the PK end; a small filled circle marks the FK end.  When a child is
 * not directly to the right of its parent (edge connecting non-adjacent
 * columns, or the legacy right-side path for same-column tables), the
 * original right-side routing is used as a fallback.
 *
 * <h2>Extensibility</h2>
 * All layout constants are package-private statics, making them easy to
 * override in a subclass.  The rendering passes (tables, connectors) are
 * split into protected methods so individual passes can be overridden
 * independently.
 */
@Slf4j
public class ErDiagramRenderer {

    // ── Layout constants ───────────────────────────────────────────────────
    static final int DEFAULT_TABLES_PER_ROW = 5;
    static final int MARGIN                 = 40;
    static final int TABLE_WIDTH            = 300;
    static final int HEADER_HEIGHT          = 28;
    static final int ROW_HEIGHT             = 22;
    static final int H_GAP                  = 100; // horizontal gap between table columns (connector routing space)
    static final int V_GAP                  = 30;  // vertical gap between table rows
    static final int ISOLATED_V_GAP         = 60;  // extra gap before isolated table section
    static final int BADGE_WIDTH            = 22;
    static final int BADGE_HEIGHT           = 13;
    static final int BADGE_MARGIN           = 4;
    static final int CONNECTOR_STEP         = 8;  // per-connector x-offset inside the gap to avoid overlaps

    // ── Colours ────────────────────────────────────────────────────────────
    private static final Color BG_COLOR         = new Color(245, 247, 250);
    private static final Color HEADER_BG         = new Color(44, 62, 80);
    private static final Color HEADER_FG         = Color.WHITE;
    private static final Color PK_ROW_BG         = new Color(255, 243, 205);
    private static final Color FK_ROW_BG         = new Color(209, 236, 241);
    private static final Color NORMAL_ROW_BG     = Color.WHITE;
    private static final Color ALT_ROW_BG        = new Color(248, 249, 250);
    private static final Color ROW_BORDER        = new Color(222, 226, 230);
    private static final Color TABLE_BORDER      = new Color(108, 117, 125);
    private static final Color PK_BADGE_BG       = new Color(230, 169, 0);
    private static final Color FK_BADGE_BG       = new Color(23, 162, 184);
    private static final Color BADGE_FG          = Color.WHITE;
    private static final Color CONNECTOR_COLOR   = new Color(192, 57, 43);
    private static final Color TYPE_COLOR        = new Color(108, 117, 125);

    // ── Fonts ──────────────────────────────────────────────────────────────
    private static final Font HEADER_FONT = new Font("SansSerif", Font.BOLD, 12);
    private static final Font COLUMN_FONT = new Font("SansSerif", Font.PLAIN, 11);
    private static final Font TYPE_FONT   = new Font("SansSerif", Font.ITALIC, 10);
    private static final Font BADGE_FONT  = new Font("SansSerif", Font.BOLD, 8);

    // ── State ──────────────────────────────────────────────────────────────
    private final int tablesPerRow;

    /**
     * Creates a renderer using the default number of tables per row
     * ({@value #DEFAULT_TABLES_PER_ROW}).
     */
    public ErDiagramRenderer() {
        this(DEFAULT_TABLES_PER_ROW);
    }

    /**
     * Creates a renderer with a custom number of tables per row.
     *
     * @param tablesPerRow how many table boxes to place on each grid row
     */
    public ErDiagramRenderer(int tablesPerRow) {
        this.tablesPerRow = tablesPerRow;
    }

    /**
     * Renders the given schema as an SVG file at the specified path.
     *
     * @param schema    the schema to render; must not be {@code null}
     * @param outputPath absolute or relative path to the output {@code .svg} file
     * @throws Exception if the file cannot be written or SVG generation fails
     */
    public void render(ErSchema schema, String outputPath) throws Exception {
        log.info("Rendering ER diagram with {} tables to {}", schema.getTables().size(), outputPath);

        // ── Step 1: layout pass ─────────────────────────────────────────
        computeLayout(schema);

        // ── Step 2: compute canvas dimensions ──────────────────────────
        int canvasWidth  = computeCanvasWidth(schema);
        int canvasHeight = computeCanvasHeight(schema);

        // ── Step 3: initialise SVG generator ───────────────────────────
        DOMImplementation domImpl   = GenericDOMImplementation.getDOMImplementation();
        String            svgNS     = SVGDOMImplementation.SVG_NAMESPACE_URI;
        Document          document  = domImpl.createDocument(svgNS, "svg", null);
        SVGGraphics2D     svg       = new SVGGraphics2D(document);
        svg.setSVGCanvasSize(new Dimension(canvasWidth, canvasHeight));

        // ── Step 4: background ──────────────────────────────────────────
        svg.setColor(BG_COLOR);
        svg.fillRect(0, 0, canvasWidth, canvasHeight);

        // ── Step 5: draw all table boxes ────────────────────────────────
        for (ErTable table : schema.getTables()) {
            drawTable(svg, table);
        }

        // ── Step 6: draw FK connectors ──────────────────────────────────
        drawConnectors(svg, schema);

        // ── Step 7: stream to file ──────────────────────────────────────
        File parent = new File(outputPath).getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            log.warn("Could not create output directory: {}", parent.getAbsolutePath());
        }
        try (FileOutputStream fos = new FileOutputStream(outputPath);
             Writer out = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            svg.stream(out, true);
        }
        log.info("ER diagram written to {}", outputPath);
    }

    // ── Layout ─────────────────────────────────────────────────────────────

    /**
     * Assigns pixel coordinates and dimensions to every {@link ErTable} in the
     * schema.
     *
     * <p>Phase 1 – connected tables (those involved in FK relationships):
     * <ol>
     *   <li>Build a directed parent→child graph from {@code schema.getForeignKeys()}.</li>
     *   <li>BFS from root tables (no incoming FK edges) assigns each table a
     *       <em>column</em> = {@code max(parent columns) + 1}.</li>
     *   <li>Post-order DFS assigns each table a <em>row</em> = next available
     *       slot in its column.  Children are placed before their parent, so
     *       the parent ends up just below the last child in the same column,
     *       keeping connectors short.</li>
     * </ol>
     *
     * <p>Phase 2 – isolated tables (no FK edges at all) are packed in a plain
     * grid of {@code tablesPerRow} columns directly below the connected section.
     *
     * @param schema the schema whose tables will be mutated with layout data
     */
    protected void computeLayout(ErSchema schema) {
        // ── Pre-compute pixel sizes ──────────────────────────────────────
        Map<String, ErTable> byName = new LinkedHashMap<>();
        for (ErTable t : schema.getTables()) {
            t.setWidth(TABLE_WIDTH);
            t.setHeight(HEADER_HEIGHT + t.getColumns().size() * ROW_HEIGHT);
            byName.put(t.getTableName(), t);
        }

        // ── Build FK graph ───────────────────────────────────────────────
        // Edge direction: parent (toTable) → child (fromTable)
        Map<String, List<String>> parentToChildren = new LinkedHashMap<>();
        Map<String, List<String>> childToParents   = new LinkedHashMap<>();
        for (ErForeignKey fk : schema.getForeignKeys()) {
            parentToChildren.computeIfAbsent(fk.getToTable(),   k -> new ArrayList<>()).add(fk.getFromTable());
            childToParents.computeIfAbsent(fk.getFromTable(), k -> new ArrayList<>()).add(fk.getToTable());
        }

        // ── Classify tables ──────────────────────────────────────────────
        // roots    = have children but no parents (never appear as fromTable)
        // isolated = no FK edges at all
        // children = appear as fromTable; reached via DFS from their parents
        List<String> roots    = new ArrayList<>();
        List<String> isolated = new ArrayList<>();
        for (ErTable t : schema.getTables()) {
            String name = t.getTableName();
            if (!childToParents.containsKey(name)) {
                if (parentToChildren.containsKey(name)) {
                    roots.add(name);
                } else {
                    isolated.add(name);
                }
            }
        }
        Collections.sort(roots);
        Collections.sort(isolated);

        // ── BFS: assign columns ──────────────────────────────────────────
        // column[t] = depth of t from its nearest root
        Map<String, Integer> colMap = new HashMap<>();
        Queue<String>        queue  = new LinkedList<>(roots);
        for (String r : roots) {
            colMap.put(r, 0);
        }
        while (!queue.isEmpty()) {
            String current    = queue.poll();
            int    currentCol = colMap.get(current);
            for (String child : parentToChildren.getOrDefault(current, Collections.emptyList())) {
                int newCol = currentCol + 1;
                // Take the maximum column in case a child has multiple parents
                if (!colMap.containsKey(child) || colMap.get(child) < newCol) {
                    colMap.put(child, newCol);
                    queue.add(child);
                }
            }
        }

        // ── Post-order DFS: assign rows ──────────────────────────────────
        // nextRow[col] tracks the next available row slot per column.
        // Children are placed first so the parent always sits just below its
        // last child, keeping connectors short.
        Map<Integer, Integer> nextRow = new HashMap<>();
        Map<String, Integer>  rowMap  = new HashMap<>();
        Set<String>           visited = new LinkedHashSet<>();
        for (String root : roots) {
            assignRowsDfs(root, parentToChildren, colMap, rowMap, nextRow, visited);
        }

        // ── Apply pixel positions to connected tables ────────────────────
        // Compute per-row maximum table height so compact rows are not
        // inflated to the height of the tallest row in the whole diagram.
        Map<Integer, Integer> maxHeightPerRow = new HashMap<>();
        for (Map.Entry<String, Integer> e : rowMap.entrySet()) {
            ErTable t = byName.get(e.getKey());
            if (t != null) {
                maxHeightPerRow.merge(e.getValue(), t.getHeight(), Math::max);
            }
        }
        int maxRowIdx = maxHeightPerRow.keySet().stream()
                .mapToInt(Integer::intValue).max().orElse(0);
        int[] rowTopY = new int[maxRowIdx + 1];
        rowTopY[0] = MARGIN;
        for (int i = 1; i <= maxRowIdx; i++) {
            rowTopY[i] = rowTopY[i - 1] + maxHeightPerRow.getOrDefault(i - 1, HEADER_HEIGHT) + V_GAP;
        }

        for (Map.Entry<String, Integer> entry : colMap.entrySet()) {
            ErTable t   = byName.get(entry.getKey());
            Integer row = rowMap.get(entry.getKey());
            if (t != null && row != null) {
                t.setX(MARGIN + entry.getValue() * (TABLE_WIDTH + H_GAP));
                t.setY(rowTopY[row]);
            }
        }

        // ── Pack isolated tables in a grid below connected section ───────
        int connectedBottomY = schema.getTables().stream()
                .filter(t -> colMap.containsKey(t.getTableName()))
                .mapToInt(t -> t.getY() + t.getHeight())
                .max().orElse(MARGIN);
        int isolatedStartY    = connectedBottomY + ISOLATED_V_GAP;
        int iCol              = 0;
        int iRowY             = isolatedStartY;
        int rowMaxHeight      = 0;
        for (String name : isolated) {
            ErTable t = byName.get(name);
            if (t == null) continue;
            t.setX(MARGIN + iCol * (TABLE_WIDTH + H_GAP));
            t.setY(iRowY);
            rowMaxHeight = Math.max(rowMaxHeight, t.getHeight());
            iCol++;
            if (iCol >= tablesPerRow) {
                iCol      = 0;
                iRowY    += rowMaxHeight + V_GAP;
                rowMaxHeight = 0;
            }
        }
    }

    /**
     * Post-order DFS helper: places a node's entire subtree before placing the
     * node itself, so the node's row slot is directly below its last child.
     * Already-visited nodes are skipped to handle shared-parent (DAG) cases.
     *
     * @param node             the node to place
     * @param parentToChildren directed parent→child adjacency map
     * @param colMap           column assigned to each table name
     * @param rowMap           output: row assigned to each table name
     * @param nextRow          mutable next-available-row counter per column
     * @param visited          set of already-placed table names
     */
    private void assignRowsDfs(String node,
                               Map<String, List<String>> parentToChildren,
                               Map<String, Integer> colMap,
                               Map<String, Integer> rowMap,
                               Map<Integer, Integer> nextRow,
                               Set<String> visited) {
        if (visited.contains(node)) {
            return;
        }
        visited.add(node);

        // Recurse on children first (post-order)
        for (String child : parentToChildren.getOrDefault(node, Collections.emptyList())) {
            if (!visited.contains(child)) {
                assignRowsDfs(child, parentToChildren, colMap, rowMap, nextRow, visited);
            }
        }

        // Place this node at the next available row in its column
        int col = colMap.getOrDefault(node, 0);
        int row = nextRow.getOrDefault(col, 0);
        rowMap.put(node, row);
        nextRow.put(col, row + 1);
    }

    /**
     * Returns the total pixel width of the SVG canvas, derived from the actual
     * table positions set by {@link #computeLayout(ErSchema)}.
     *
     * @param schema the schema after layout has been computed
     * @return canvas width in pixels
     */
    protected int computeCanvasWidth(ErSchema schema) {
        int maxRight = schema.getTables().stream()
                .mapToInt(t -> t.getX() + t.getWidth())
                .max().orElse(MARGIN);
        // Reserve extra space to the right for connector routing
        int extraForConnectors = CONNECTOR_STEP * (schema.getForeignKeys().size() + 1) + H_GAP;
        return maxRight + extraForConnectors;
    }

    /**
     * Returns the total pixel height of the SVG canvas based on the laid-out
     * tables.
     *
     * @param schema the schema after {@link #computeLayout(ErSchema)} has run
     * @return canvas height in pixels
     */
    protected int computeCanvasHeight(ErSchema schema) {
        int maxY = 0;
        for (ErTable t : schema.getTables()) {
            maxY = Math.max(maxY, t.getY() + t.getHeight());
        }
        return maxY + MARGIN;
    }

    // ── Table drawing ───────────────────────────────────────────────────────

    /**
     * Draws a single table box (header + column rows + border).
     *
     * @param g     the SVG graphics context
     * @param table the table to draw
     */
    protected void drawTable(Graphics2D g, ErTable table) {
        int x = table.getX();
        int y = table.getY();
        int w = table.getWidth();

        // Header
        g.setColor(HEADER_BG);
        g.fillRect(x, y, w, HEADER_HEIGHT);
        g.setFont(HEADER_FONT);
        g.setColor(HEADER_FG);
        g.drawString(table.getTableName(), x + 8, y + HEADER_HEIGHT - 8);

        // Column rows
        List<ErColumn> columns = table.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            ErColumn col    = columns.get(i);
            int      rowY   = y + HEADER_HEIGHT + i * ROW_HEIGHT;

            // Row background
            Color bg;
            if (col.isPrimaryKey()) {
                bg = PK_ROW_BG;
            } else if (col.isForeignKey()) {
                bg = FK_ROW_BG;
            } else {
                bg = (i % 2 == 0) ? NORMAL_ROW_BG : ALT_ROW_BG;
            }
            g.setColor(bg);
            g.fillRect(x, rowY, w, ROW_HEIGHT);

            // Row separator
            g.setColor(ROW_BORDER);
            g.setStroke(new BasicStroke(0.5f));
            g.drawLine(x, rowY, x + w, rowY);

            // Badge (PK / FK)
            int textStartX = x + 6;
            if (col.isPrimaryKey()) {
                drawBadge(g, x + 4, rowY + (ROW_HEIGHT - BADGE_HEIGHT) / 2, "PK", PK_BADGE_BG);
                textStartX = x + BADGE_WIDTH + BADGE_MARGIN + 4;
            } else if (col.isForeignKey()) {
                drawBadge(g, x + 4, rowY + (ROW_HEIGHT - BADGE_HEIGHT) / 2, "FK", FK_BADGE_BG);
                textStartX = x + BADGE_WIDTH + BADGE_MARGIN + 4;
            }

            // Column name
            g.setFont(COLUMN_FONT);
            g.setColor(Color.DARK_GRAY);
            g.drawString(col.getName(), textStartX, rowY + ROW_HEIGHT - 6);

            // Data type – right-aligned
            g.setFont(TYPE_FONT);
            g.setColor(TYPE_COLOR);
            FontMetrics fm       = g.getFontMetrics();
            String      typeText = col.isNullable() ? col.getDataType() + "?" : col.getDataType();
            int         typeW    = fm.stringWidth(typeText);
            g.drawString(typeText, x + w - typeW - 6, rowY + ROW_HEIGHT - 6);
        }

        // Table outer border
        g.setColor(TABLE_BORDER);
        g.setStroke(new BasicStroke(1.0f));
        g.drawRect(x, y, w, table.getHeight());
    }

    /**
     * Draws a small pill-shaped badge (PK / FK) at the given position.
     *
     * @param g    graphics context
     * @param bx   top-left x of the badge
     * @param by   top-left y of the badge
     * @param text badge label
     * @param bg   badge background colour
     */
    protected void drawBadge(Graphics2D g, int bx, int by, String text, Color bg) {
        g.setColor(bg);
        g.fillRoundRect(bx, by, BADGE_WIDTH, BADGE_HEIGHT, 4, 4);
        g.setFont(BADGE_FONT);
        g.setColor(BADGE_FG);
        FontMetrics fm = g.getFontMetrics();
        int         tw = fm.stringWidth(text);
        g.drawString(text, bx + (BADGE_WIDTH - tw) / 2, by + BADGE_HEIGHT - 3);
    }

    // ── Connector drawing ───────────────────────────────────────────────────

    /**
     * Draws all FK connector lines between table boxes.
     *
     * <p>When the FK (child) table is to the <em>right</em> of the PK (parent)
     * table — as it always is in the tree layout — connectors exit from the
     * <strong>left</strong> edge of the FK table, route vertically through the
     * inter-column gap, and re-enter the <strong>right</strong> edge of the PK
     * table.  This keeps all routing inside the gap, well clear of the table
     * content.
     *
     * <p>When the FK table is to the left of or at the same x-position as the
     * PK table (should not happen with tree layout but handled for resilience),
     * the original right-side routing path is used.
     *
     * <p>Each connector is offset by {@value #CONNECTOR_STEP} px relative to
     * the previous one so that parallel connections inside the same gap do not
     * overlap.
     *
     * @param g      graphics context
     * @param schema the fully laid-out schema
     */
    protected void drawConnectors(Graphics2D g, ErSchema schema) {
        // Per-gap connector counter: key = "parentRightX-childLeftX" so that
        // each column-pair gap has its own stagger sequence.  Global staggering
        // caused the offset to overshoot the gap width for heavily-connected parents.
        Map<String, Integer> gapCounters = new HashMap<>();

        for (ErForeignKey fk : schema.getForeignKeys()) {
            ErTable fromTable = findTable(schema, fk.getFromTable());
            ErTable toTable   = findTable(schema, fk.getToTable());
            if (fromTable == null || toTable == null) {
                log.warn("Cannot draw connector for FK {} — table not found", fk.getConstraintName());
                continue;
            }

            int fromColIdx = fromTable.columnIndex(fk.getFromColumn());
            int toColIdx   = toTable.columnIndex(fk.getToColumn());

            int fkY = fromTable.getY() + HEADER_HEIGHT + fromColIdx * ROW_HEIGHT + ROW_HEIGHT / 2;
            int pkY = toTable.getY()   + HEADER_HEIGHT + toColIdx   * ROW_HEIGHT + ROW_HEIGHT / 2;

            g.setColor(CONNECTOR_COLOR);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int dotR = 4;
            if (fromTable.getX() > toTable.getX()) {
                // ── Tree layout: child is to the RIGHT of parent ────────
                int fkLeft  = fromTable.getX();
                int pkRight = toTable.getX() + TABLE_WIDTH;

                // Stagger connectors that share the same gap (same pkRight/fkLeft pair)
                String gapKey          = pkRight + "-" + fkLeft;
                int    gapIndex        = gapCounters.getOrDefault(gapKey, 0);
                gapCounters.put(gapKey, gapIndex + 1);

                // Route through the centre of the gap, offset per connector.
                // Clamp so the routing line always stays inside the gap.
                int gapWidth = fkLeft - pkRight;
                int routeX   = pkRight + gapWidth / 2 + gapIndex * CONNECTOR_STEP;
                routeX = Math.min(routeX, fkLeft  - CONNECTOR_STEP);
                routeX = Math.max(routeX, pkRight + CONNECTOR_STEP);

                // Segment 1: horizontal exit left from FK table
                g.drawLine(fkLeft, fkY, routeX, fkY);
                // Segment 2: vertical routing inside the gap
                g.drawLine(routeX, fkY, routeX, pkY);
                // Segment 3: horizontal entry into PK table right edge
                g.drawLine(routeX, pkY, pkRight, pkY);

                // FK end: filled circle on the LEFT edge of child table
                g.fillOval(fkLeft - dotR, fkY - dotR, dotR * 2, dotR * 2);

                // PK end: arrowhead pointing left (←) at PK table right edge
                drawArrowLeft(g, pkRight, pkY);

            } else {
                // ── Fallback: FK table is to the left or same column ────
                int fkRight = fromTable.getX() + TABLE_WIDTH;
                int pkRight = toTable.getX()   + TABLE_WIDTH;

                String gapKey   = fkRight + "-" + pkRight;
                int    gapIndex = gapCounters.getOrDefault(gapKey, 0);
                gapCounters.put(gapKey, gapIndex + 1);

                int routeX = Math.max(fkRight, pkRight) + H_GAP / 4 + gapIndex * CONNECTOR_STEP;

                g.drawLine(fkRight, fkY, routeX, fkY);
                g.drawLine(routeX,  fkY, routeX, pkY);
                g.drawLine(routeX,  pkY, pkRight, pkY);

                g.fillOval(fkRight - dotR, fkY - dotR, dotR * 2, dotR * 2);
                drawArrowLeft(g, pkRight, pkY);
            }
        }
    }

    /**
     * Draws a small filled arrowhead pointing left at the given point.
     *
     * @param g graphics context
     * @param x tip x-coordinate (leftmost point of the arrowhead)
     * @param y tip y-coordinate (vertical centre)
     */
    protected void drawArrowLeft(Graphics2D g, int x, int y) {
        int size = 8;
        Path2D arrow = new Path2D.Float();
        arrow.moveTo(x, y);
        arrow.lineTo(x + size, y - size / 2.0);
        arrow.lineTo(x + size, y + size / 2.0);
        arrow.closePath();
        g.fill(arrow);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Finds an {@link ErTable} by name (case-insensitive).
     *
     * @param schema    the schema to search
     * @param tableName the table name to find
     * @return the matching table, or {@code null} if not found
     */
    protected ErTable findTable(ErSchema schema, String tableName) {
        for (ErTable t : schema.getTables()) {
            if (t.getTableName().equalsIgnoreCase(tableName)) {
                return t;
            }
        }
        return null;
    }
}

