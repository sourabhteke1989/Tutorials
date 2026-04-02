package com.example.jexl.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "payroll_expressions")
public class PayrollExpression {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "expression_id", nullable = false)
    private String expressionId;

    @Column(name = "expression", nullable = false, columnDefinition = "CLOB")
    private String expression;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "context_variables")
    private String contextVariables;

    public PayrollExpression() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getExpressionId() { return expressionId; }
    public void setExpressionId(String expressionId) { this.expressionId = expressionId; }

    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }

    public String getContextVariables() { return contextVariables; }
    public void setContextVariables(String contextVariables) { this.contextVariables = contextVariables; }
}
