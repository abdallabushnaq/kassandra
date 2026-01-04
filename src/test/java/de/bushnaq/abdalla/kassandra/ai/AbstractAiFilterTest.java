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

package de.bushnaq.abdalla.kassandra.ai;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.json.JsonMapper;

import javax.script.ScriptException;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class AbstractAiFilterTest<T> {
    private final   AiFilterService aiFilterService;
    protected final JsonMapper      filterMapper;
    protected       String          javascriptFunction;
    protected final Logger          logger = LoggerFactory.getLogger(this.getClass());
    protected final LocalDate       now;
    protected       String          regexString;
    protected       List<T>         testProducts;

    public AbstractAiFilterTest(JsonMapper mapper, AiFilterService aiFilterService, LocalDate now) {
        this.filterMapper    = mapper;
        this.aiFilterService = aiFilterService;
        this.now             = now;
    }


    /**
     * Helper method to simulate filtering products using regex patterns
     * (mimics what SmartGlobalFilter does)
     */
//    private List<T> applyRegexSearchQuery(Pattern regexPattern) throws Exception {
//        return testProducts.stream()
//                .filter(product -> {
//                    try {
//                        String json = filterMapper.writerWithDefaultPrettyPrinter().writeValueAsString(product);
//                        return regexPattern.matcher(json).find();
//                    } catch (Exception e) {
//                        return false;
//                    }
//                })
//                .collect(Collectors.toList());
//    }

    /**
     * Perform search using regex approach (existing method)
     */
    protected List<T> performSearch(String searchValue, String entityType) throws Exception {
        return performSearch(searchValue, entityType, AiFilterGenerator.FilterType.JAVASCRIPT);
    }

    /**
     * Perform search with specified filter type
     */
    protected List<T> performSearch(String searchValue, String entityType, AiFilterGenerator.FilterType filterType) throws Exception {
        int tryCount = 10;
        do {
            try {

                switch (filterType) {
                    case JAVASCRIPT: {
                        // Parse the query using JavaScript generation
                        javascriptFunction = aiFilterService.parseQuery(searchValue, entityType, filterType);

                        List<T> filtered = aiFilterService.applyJavaScriptSearchQuery(javascriptFunction, testProducts, now);
                        System.out.println("\n=== Products matched by JavaScript filter ===");
                        System.out.println("JavaScript function: " + javascriptFunction);
                        for (T product : filtered) {
                            String json = filterMapper.writeValueAsString(product);
                            System.out.println(json);
                        }
                        return filtered;
                    }
                    case JAVA: {
                        // Parse the query using Java generation and get compiled predicate
                        var     javaPredicate = aiFilterService.parseQueryToPredicate(searchValue, entityType, now);
                        List<T> filtered      = testProducts.stream().filter(javaPredicate).collect(Collectors.toList());

                        System.out.println("\n=== Products matched by Java filter ===");
//                        System.out.println("Java predicate compiled successfully");
                        for (T product : filtered) {
                            String json = filterMapper.writeValueAsString(product);
                            System.out.println(json);
                        }
                        return filtered;
                    }
                }
            } catch (PatternSyntaxException e) {
                logger.error("Invalid regex pattern '{}', retrying {}/{}", regexString, tryCount, 10, e);
            } catch (ScriptException e) {
                logger.error("JavaScript execution failed '{}', retrying {}/{}", javascriptFunction, tryCount, 10, e);
            }
        } while (--tryCount > 0);
        return null;
    }

    protected List<T> performSearch(Predicate<T> javaPredicate) throws Exception {
        List<T> filtered = testProducts.stream().filter(javaPredicate).collect(Collectors.toList());

        System.out.println("\n=== Products matched by Java filter ===");
//        System.out.println("Java predicate compiled successfully");
        for (T product : filtered) {
            String json = filterMapper.writeValueAsString(product);
            System.out.println(json);
        }
        return filtered;
    }

    /**
     * Custom annotation introspector that ignores @JsonIgnore annotations
     * but preserves all other Jackson annotations.
     */
    private static class FilterAnnotationIntrospector extends tools.jackson.databind.introspect.JacksonAnnotationIntrospector {
        @Override
        public boolean hasIgnoreMarker(MapperConfig<?> config, AnnotatedMember m) {
            // Don't ignore fields marked with @JsonIgnore for filtering purposes
            // but still process other ignore markers from the parent class
            if (m.hasAnnotation(JsonIgnore.class)) {
                return false;
            }
            return super.hasIgnoreMarker(config, m);
        }
    }
}
