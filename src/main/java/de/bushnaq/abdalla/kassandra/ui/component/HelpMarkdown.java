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

package de.bushnaq.abdalla.kassandra.ui.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.markdown.Markdown;
import de.bushnaq.abdalla.kassandra.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders bundled help Markdown with locally rendered Mermaid diagrams.
 */
@Tag("kassandra-help-markdown")
@JsModule("./help/help-markdown.ts")
@CssImport("./help/help.css")
@NpmPackage(value = "mermaid", version = "12.1.0")
@Slf4j
public class HelpMarkdown extends Component {
    private static final Pattern CHAPTER_HEADING = Pattern.compile("^ {0,3}##[ \\t]+(.+?)(?:[ \\t]+#+)?[ \\t]*$");
    private static final Pattern CODE_FENCE      = Pattern.compile("^ {0,3}(`{3,}|~{3,})(.*)$");
    private static final Pattern SHARED_CHAPTER  = Pattern.compile("(?m)^\\{\\{include(-admin)?:([a-z0-9-]+)}}[ \\t]*$");
    private final        String  resourcePath;

    /**
     * Loads a help page from the application's classpath.
     *
     * @param resourcePath a resource such as {@code help/product-list.md}
     * @throws IllegalArgumentException if the path or a shared chapter directive is invalid
     * @throws UncheckedIOException     if the resource cannot be read
     */
    public HelpMarkdown(String resourcePath) {
        this(resourcePath, false);
    }

    /**
     * Loads a help page with optional, initially collapsed level-two chapters.
     *
     * @param resourcePath        a resource such as {@code help/feature-list.md}
     * @param collapsibleChapters whether level-two headings become expandable chapters
     * @throws IllegalArgumentException if the path or a shared chapter directive is invalid
     * @throws UncheckedIOException     if the resource cannot be read
     */
    public HelpMarkdown(String resourcePath, boolean collapsibleChapters) {
        if (resourcePath == null || !resourcePath.matches("help/[a-z0-9-]+\\.md")) {
            throw new IllegalArgumentException("Invalid help resource path: " + resourcePath);
        }
        this.resourcePath = resourcePath;
        String content = loadResource(resourcePath);
        if (collapsibleChapters) {
            addChapters(content);
        } else {
            getElement().appendChild(createMarkdown(content).getElement());
        }
    }

    private void addChapters(String content) {
        List<Details> chapters = new ArrayList<>();
        StringBuilder body     = new StringBuilder();
        String        title    = null;
        String        fence    = null;
        for (String line : content.lines().toList()) {
            var codeFence = CODE_FENCE.matcher(line);
            if (codeFence.matches()) {
                String marker = codeFence.group(1);
                if (fence == null && (marker.charAt(0) != '`' || !codeFence.group(2).contains("`"))) {
                    fence = marker;
                } else if (fence != null && marker.charAt(0) == fence.charAt(0)
                        && marker.length() >= fence.length() && codeFence.group(2).isBlank()) {
                    fence = null;
                }
                body.append(line).append('\n');
                continue;
            }
            var heading = CHAPTER_HEADING.matcher(line);
            if (fence == null && heading.matches()) {
                addChapter(chapters, title, body.toString());
                title = heading.group(1);
                body.setLength(0);
            } else {
                body.append(line).append('\n');
            }
        }
        addChapter(chapters, title, body.toString());
        if (!chapters.isEmpty()) {
            Button expand   = new Button("Expand all", event -> chapters.forEach(chapter -> chapter.setOpened(true)));
            Button collapse = new Button("Collapse all", event -> chapters.forEach(chapter -> chapter.setOpened(false)));
            expand.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            collapse.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Div controls = new Div(expand, collapse);
            controls.addClassName("help-chapter-controls");
            getElement().appendChild(controls.getElement());
            chapters.forEach(chapter -> getElement().appendChild(chapter.getElement()));
        }
    }

    private void addChapter(List<Details> chapters, String title, String content) {
        Markdown markdown = createMarkdown(content);
        if (title == null) {
            getElement().appendChild(markdown.getElement());
        } else {
            Details chapter = new Details(new H2(title), markdown);
            chapter.addClassName("help-chapter");
            chapter.setOpened(false);
            chapters.add(chapter);
        }
    }

    private Markdown createMarkdown(String content) {
        boolean admin    = SecurityUtils.isAdmin();
        var     chapters = SHARED_CHAPTER.matcher(content);
        content = chapters.replaceAll(match -> {
            if (match.group(1) != null && !admin) {
                return "";
            }
            String chapterPath = "help/shared/" + match.group(2) + ".md";
            String chapter     = loadResource(chapterPath);
            if (chapter.contains("{{include")) {
                throw new IllegalArgumentException("Nested help chapters are not supported: " + chapterPath);
            }
            return Matcher.quoteReplacement(chapter);
        });
        if (content.contains("{{include")) {
            throw new IllegalArgumentException("Invalid shared help chapter directive in " + resourcePath);
        }
        return new Markdown(content.replace("{{audience}}", admin ? "admin" : "user"));
    }

    private String loadResource(String resourcePath) {
        try (var stream = new ClassPathResource(resourcePath).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Cannot load help resource {}", resourcePath, e);
            throw new UncheckedIOException("Cannot load help resource " + resourcePath, e);
        }
    }
}
