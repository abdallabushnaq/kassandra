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

package de.bushnaq.abdalla.kassandra.rest.debug;

import de.bushnaq.abdalla.kassandra.rest.ErrorResponse;
import org.openqa.selenium.json.JsonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.databind.json.JsonMapper;

@ControllerAdvice
public class JsonResponseLogger implements ResponseBodyAdvice<Object> {

    private static final Logger logger = LoggerFactory.getLogger(JsonResponseLogger.class);
    @Autowired
    JsonMapper jsonMapper;


    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, org.springframework.http.MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        try {
            int status = ((ServletServerHttpResponse) response).getServletResponse().getStatus();
            if (status != 200) {
                if (body instanceof ErrorResponse errorResponse) {
                    if (DebugUtil.DEBUG)
                        System.out.format("Error Response: %s\n", errorResponse.getMessage());
                } else {
                    if (DebugUtil.DEBUG)
                        System.out.format("Error Response: %d\n", status);
                }
            } else {
                if (DebugUtil.DEBUG) {
                    String jsonString = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(body);
                    System.out.format("Response JSON: %s\n", jsonString);
                }
            }
        } catch (JsonException e) {
            throw new RuntimeException(e);
        }

        return body;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }
}