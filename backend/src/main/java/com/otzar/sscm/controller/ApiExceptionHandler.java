package com.otzar.sscm.controller;

import com.otzar.sscm.models.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception) {
        String receivedType = exception.getContentType() == null
                ? "missing Content-Type"
                : exception.getContentType().toString();

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ApiResponse(false,
                        "Unsupported media type: " + receivedType
                                + ". Use multipart/form-data for file uploads."));
    }
}
