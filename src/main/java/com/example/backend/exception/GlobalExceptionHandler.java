package com.example.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<HttpErrorResponse> handleException(final Exception exception) {
        log.error("handled exception: ", exception);
        var response = HttpErrorResponse.of(exception.getMessage(), 500);
        return new ResponseEntity<>(response, HttpStatus.valueOf(500));
    }
}
