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
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.markdown.Markdown;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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
    private static final Pattern SHARED_CHAPTER = Pattern.compile("(?m)^\\{\\{include:([a-z0-9-]+)}}[ \\t]*$");

    /**
     * Loads a help page from the application's classpath.
     *
     * @param resourcePath a resource such as {@code help/product-list.md}
     * @throws IllegalArgumentException if the path or a shared chapter directive is invalid
     * @throws UncheckedIOException     if the resource cannot be read
     */
    public HelpMarkdown(String resourcePath) {
        if (resourcePath == null || !resourcePath.matches("help/[a-z0-9-]+\\.md")) {
            throw new IllegalArgumentException("Invalid help resource path: " + resourcePath);
        }
        String content  = loadResource(resourcePath);
        var    chapters = SHARED_CHAPTER.matcher(content);
        content = chapters.replaceAll(match -> {
            String chapterPath = "help/shared/" + match.group(1) + ".md";
            String chapter     = loadResource(chapterPath);
            if (chapter.contains("{{include:")) {
                throw new IllegalArgumentException("Nested help chapters are not supported: " + chapterPath);
            }
            return Matcher.quoteReplacement(chapter);
        });
        if (content.contains("{{include:")) {
            throw new IllegalArgumentException("Invalid shared help chapter directive in " + resourcePath);
        }
        getElement().appendChild(new Markdown(content).getElement());
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
