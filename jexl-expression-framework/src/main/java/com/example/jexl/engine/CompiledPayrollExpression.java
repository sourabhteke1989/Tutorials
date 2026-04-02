package com.example.jexl.engine;

import com.example.jexl.entity.PayrollExpression;

/**
 * Holds a PayrollExpression entity together with its pre-compiled JEXL object.
 * The compiled object is either a JexlScript or JexlExpression, stored as Object
 * so the cache layer stays decoupled from JEXL API details.
 */
public class CompiledPayrollExpression {

    private final PayrollExpression entity;
    private final Object compiled;    // JexlScript or JexlExpression
    private final boolean script;     // true = JexlScript, false = JexlExpression

    public CompiledPayrollExpression(PayrollExpression entity, Object compiled, boolean script) {
        this.entity = entity;
        this.compiled = compiled;
        this.script = script;
    }

    public PayrollExpression getEntity() { return entity; }
    public Object getCompiled() { return compiled; }
    public boolean isScript() { return script; }
}
