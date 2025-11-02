/*
 *
 * Copyright (C) 2025-2025 Abdalla Bushnaq
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

package de.bushnaq.abdalla.kassandra.report.html.util;

import de.bushnaq.abdalla.util.FileUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HtmlUtil {
    protected Map<String, String> snippets = new HashMap<>();

    public String getHtmlTipSnippet(String snippetFileName) throws Exception {
        snippetFileName = sanitize(snippetFileName);

        String text = null;
        if (!snippetFileName.isEmpty()) {
            text = snippets.get(snippetFileName);
            if (text == null) {
                loadHtmlTipSnippet(snippetFileName);
                text = snippets.get(snippetFileName);
                if (text == null) {
                    throw new Exception(String.format("string %s not found", snippetFileName));
                }
            }
        }
        return text;
    }

    private void loadHtmlTipSnippet(String snippetName) throws IOException {
        String snippet = FileUtil.loadFile(this, "tip/" + snippetName + ".html");
        snippets.put(snippetName, snippet);
    }

    private String sanitize(String string) {
        string = string.replace("\n", " ").replace("<br>", " ").replace("<b>", "").replace("</b>", "");
        return string;
    }

}
