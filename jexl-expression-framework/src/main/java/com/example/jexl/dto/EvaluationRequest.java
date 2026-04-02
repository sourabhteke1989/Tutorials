package com.example.jexl.dto;

import java.util.Map;

public class EvaluationRequest {

    private String expressionId;
    private Map<String, Object> context;
    private String todayDate; // optional, format: yyyy-MM-dd

    public String getExpressionId() { return expressionId; }
    public void setExpressionId(String expressionId) { this.expressionId = expressionId; }

    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }

    public String getTodayDate() { return todayDate; }
    public void setTodayDate(String todayDate) { this.todayDate = todayDate; }
}
