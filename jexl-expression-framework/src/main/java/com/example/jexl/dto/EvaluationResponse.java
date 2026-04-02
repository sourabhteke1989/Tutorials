package com.example.jexl.dto;

import java.util.Map;

public class EvaluationResponse {

    private String expressionId;
    private Object result;
    private String effectiveFrom;
    private String effectiveTo;
    private Map<String, Object> contextUsed;

    public String getExpressionId() { return expressionId; }
    public void setExpressionId(String expressionId) { this.expressionId = expressionId; }

    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }

    public String getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(String effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public String getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(String effectiveTo) { this.effectiveTo = effectiveTo; }

    public Map<String, Object> getContextUsed() { return contextUsed; }
    public void setContextUsed(Map<String, Object> contextUsed) { this.contextUsed = contextUsed; }
}
