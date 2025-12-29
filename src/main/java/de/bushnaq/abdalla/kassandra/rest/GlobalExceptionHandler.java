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

package de.bushnaq.abdalla.kassandra.rest;

import de.bushnaq.abdalla.kassandra.rest.exception.UniqueConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.error("AccessDeniedException", e);
        ErrorResponse error = new ErrorResponse(HttpStatus.FORBIDDEN, "Access Denied: " + e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON).body(error);
    }

    @ExceptionHandler({AuthenticationException.class})
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e) {
        log.error("AuthenticationException", e);
        ErrorResponse error = new ErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication Failed: " + e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body(error);
    }

    @ExceptionHandler({AuthorizationDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAuthorizationDeniedException(AuthorizationDeniedException e) {
        log.error("AuthorizationDeniedException", e);
        ErrorResponse error = new ErrorResponse(HttpStatus.FORBIDDEN, "Access Denied: " + e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON).body(error);
    }

    @ExceptionHandler({BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException e) {
        log.error("BadCredentialsException", e);
        ErrorResponse error = new ErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication Failed: " + e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected error", e);
        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(error);
    }

    @ExceptionHandler(HttpStatusCodeException.class)
    public ResponseEntity<ErrorResponse> handleHttpStatusCodeException(HttpStatusCodeException e) {
        log.error("HttpStatusCodeException", e);
        ErrorResponse error = new ErrorResponse(e.getStatusCode(), e.getMessage(), e);
        return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException e) {
        log.error("ResponseStatusException", e);
        ErrorResponse error = new ErrorResponse(e.getStatusCode(), e.getMessage(), e);
        return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(error);
    }

    @ExceptionHandler(UniqueConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleUniqueConstraintViolationException(UniqueConstraintViolationException e) {
        log.error("UniqueConstraintViolationException", e);
        ErrorResponse error = new ErrorResponse(HttpStatus.CONFLICT, e.getMessage(), e);
        error.setField(e.getField());
        error.setValue(e.getValue());
        return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body(error);
    }

}