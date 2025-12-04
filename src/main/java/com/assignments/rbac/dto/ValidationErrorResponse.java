package com.assignments.rbac.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {
    private boolean success = false;
    private String error;
    private Map<String, String> fieldErrors;
}

