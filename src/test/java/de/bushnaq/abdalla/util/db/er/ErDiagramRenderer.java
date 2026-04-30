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
import java.util.List;

/**
 * Renders an {@link ErSchema} as an SVG file.
 *
 * <h2>Layout</h2>
 * Tables are arranged in a configurable grid (default {@value #DEFAULT_TABLES_PER_ROW}
 * per row).  Each table box consists of a coloured header row followed by one
 * row per column.  Primary-key columns are highlighted in amber and carry a
 * {@code PK} badge; foreign-key columns are highlighted in cyan and carry an
 * {@code FK} badge.
 *
 * <h2>Connectors</h2>
 * Every real FK constraint (those discovered via
 * {@code INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS}) is drawn as an
 * orthogonal three-segment line: a short horizontal exit from the FK column
 * row, a vertical routing segment, and a short horizontal entry into the PK
 * column row on the same (right) side.  A filled arrowhead marks the PK
 * (referenced) end; a small filled circle marks the FK (owning) end.
 *
 * <h2>Extensibility</h2>
 * All layout constants are package-private statics, making them easy to
 * override in a subclass.  The three rendering passes (tables, connectors,
 * decorations) are split into separate methods so individual passes can be
 * overridden independently.
 */
@Slf4j
public class ErDiagramRenderer {

    // ── Layout constants ───────────────────────────────────────────────────
    static final int DEFAULT_TABLES_PER_ROW = 5;
    static final int MARGIN                 = 40;
    static final int TABLE_WIDTH            = 300;
    static final int HEADER_HEIGHT          = 28;
    static final int ROW_HEIGHT             = 22;
    static final int H_GAP                  = 50;  // horizontal gap between table columns
    static final int V_GAP                  = 80;  // vertical gap between table rows (space for connectors)
    static final int BADGE_WIDTH            = 22;
    static final int BADGE_HEIGHT           = 13;
    static final int BADGE_MARGIN           = 4;
    static final int CONNECTOR_EXIT_OFFSET  = 15; // px to the right of a table before routing vertically
    static final int CONNECTOR_STEP         = 8;  // per-connector horizontal offset to avoid overlaps

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
        int canvasWidth  = computeCanvasWidth();
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
     * schema using a grid layout.
     *
     * @param schema the schema whose tables will be mutated with layout data
     */
    protected void computeLayout(ErSchema schema) {
        List<ErTable> tables = schema.getTables();
        int           rows   = (int) Math.ceil((double) tables.size() / tablesPerRow);

        // Pre-compute each table's pixel height
        for (ErTable t : tables) {
            t.setWidth(TABLE_WIDTH);
            t.setHeight(HEADER_HEIGHT + t.getColumns().size() * ROW_HEIGHT);
        }

        // Assign positions row by row
        int gridY = MARGIN;
        for (int row = 0; row < rows; row++) {
            int maxRowHeight = 0;
            int startIdx     = row * tablesPerRow;
            int endIdx       = Math.min(startIdx + tablesPerRow, tables.size());

            // Find the tallest table in this grid row
            for (int i = startIdx; i < endIdx; i++) {
                maxRowHeight = Math.max(maxRowHeight, tables.get(i).getHeight());
            }

            // Place tables horizontally
            int gridX = MARGIN;
            for (int i = startIdx; i < endIdx; i++) {
                ErTable t = tables.get(i);
                t.setX(gridX);
                t.setY(gridY);
                gridX += TABLE_WIDTH + H_GAP;
            }
            gridY += maxRowHeight + V_GAP;
        }
    }

    /**
     * Returns the total pixel width of the SVG canvas.
     *
     * @return canvas width in pixels
     */
    protected int computeCanvasWidth() {
        return MARGIN * 2 + tablesPerRow * TABLE_WIDTH + (tablesPerRow - 1) * H_GAP
               + tablesPerRow * CONNECTOR_EXIT_OFFSET; // extra room for connectors
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
     * <p>Each connector uses a three-segment orthogonal path:
     * <ol>
     *   <li>Horizontal exit from the FK column row right edge to a routing
     *       x-coordinate.</li>
     *   <li>Vertical segment from FK column y to PK column y.</li>
     *   <li>Horizontal entry into the PK column row right edge from the same
     *       routing x-coordinate.</li>
     * </ol>
     * A filled arrowhead is drawn at the PK (referenced) end; a filled circle
     * at the FK (owning) end.  Each connector is offset horizontally by
     * {@value #CONNECTOR_STEP} px relative to the previous one so that
     * parallel connections do not overlap.
     *
     * @param g      graphics context
     * @param schema the fully laid-out schema
     */
    protected void drawConnectors(Graphics2D g, ErSchema schema) {
        int connectorIndex = 0;
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
            int pkY = toTable.getY() + HEADER_HEIGHT + toColIdx * ROW_HEIGHT + ROW_HEIGHT / 2;

            int fkRight = fromTable.getX() + TABLE_WIDTH;
            int pkRight = toTable.getX() + TABLE_WIDTH;

            // Route to the right of the further table + per-connector offset
            int routeX = Math.max(fkRight, pkRight) + CONNECTOR_EXIT_OFFSET + connectorIndex * CONNECTOR_STEP;

            g.setColor(CONNECTOR_COLOR);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Segment 1: horizontal exit from FK column right edge
            g.drawLine(fkRight, fkY, routeX, fkY);
            // Segment 2: vertical routing
            g.drawLine(routeX, fkY, routeX, pkY);
            // Segment 3: horizontal entry into PK column right edge
            g.drawLine(routeX, pkY, pkRight, pkY);

            // ── FK end: small filled circle ───────────────────────────
            int dotR = 4;
            g.fillOval(fkRight - dotR, fkY - dotR, dotR * 2, dotR * 2);

            // ── PK end: filled arrowhead pointing left (←) ────────────
            drawArrowLeft(g, pkRight, pkY);

            connectorIndex++;
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



