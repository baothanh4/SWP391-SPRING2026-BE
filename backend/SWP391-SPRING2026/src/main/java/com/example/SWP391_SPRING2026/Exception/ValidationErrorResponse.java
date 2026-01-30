package com.example.SWP391_SPRING2026.Exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class ValidationErrorResponse {
    private int status;
    private String error;
    private String message;
    private Map<String, String> errors;
}
