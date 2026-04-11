package com.example.ndbx.exception;

import com.example.ndbx.util.Constants;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<?> handleValidationException(ValidationException ex) {
        return ResponseEntity.badRequest().body(Map.of(Constants.FLD_MESSAGE, String.format(Constants.MSG_INVALID_FIELD, ex.getField())));
    }
}
