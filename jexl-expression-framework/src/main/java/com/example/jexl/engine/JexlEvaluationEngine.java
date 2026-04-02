package com.example.jexl.engine;

import com.example.jexl.entity.PayrollExpression;
import org.apache.commons.jexl3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Core JEXL evaluation engine.
 * Supports both simple JEXL expressions and multi-statement JEXL scripts.
 *
 * Strategy:
 *  - If the expression text contains assignments (=), control flow (if/for/while),
 *    or multiple statements (;), it is treated as a JEXL Script.
 *  - Otherwise it is evaluated as a simple JEXL Expression.
 *
 * Performance:
 *  - Expressions/scripts are compiled once at cache-load time via {@link #compile(PayrollExpression)}.
 *  - At evaluation time, the pre-compiled object is executed directly — no parsing overhead.
 */
@Component
public class JexlEvaluationEngine {

    private static final Logger log = LoggerFactory.getLogger(JexlEvaluationEngine.class);

    private final JexlEngine jexlEngine;

    public JexlEvaluationEngine() {
        this.jexlEngine = new JexlBuilder()
                .cache(512)
                .strict(false)
                .silent(false)
                .create();
    }

    /**
     * Compile a PayrollExpression into a CompiledPayrollExpression.
     * Called once at cache-load time so parsing cost is paid upfront.
     */
    public CompiledPayrollExpression compile(PayrollExpression entity) {
        String text = entity.getExpression();
        boolean isScriptType = isScript(text);

        Object compiled;
        if (isScriptType) {
            compiled = jexlEngine.createScript(text);
            log.debug("Compiled as JEXL Script: expression_id={}, id={}",
                    entity.getExpressionId(), entity.getId());
        } else {
            compiled = jexlEngine.createExpression(text);
            log.debug("Compiled as JEXL Expression: expression_id={}, id={}",
                    entity.getExpressionId(), entity.getId());
        }

        return new CompiledPayrollExpression(entity, compiled, isScriptType);
    }

    /**
     * Execute a pre-compiled expression/script with the given context variables.
     * No parsing happens here — the compiled object is reused every time.
     */
    public Object execute(CompiledPayrollExpression compiled, Map<String, Object> contextVars) {
        JexlContext context = new MapContext();
        if (contextVars != null) {
            contextVars.forEach(context::set);
        }

        if (compiled.isScript()) {
            return ((JexlScript) compiled.getCompiled()).execute(context);
        } else {
            return ((JexlExpression) compiled.getCompiled()).evaluate(context);
        }
    }

    /**
     * Convenience method: evaluate raw text (compiles on the fly).
     * Use {@link #execute(CompiledPayrollExpression, Map)} for cached expressions.
     */
    public Object evaluate(String expressionText, Map<String, Object> contextVars) {
        JexlContext context = new MapContext();
        if (contextVars != null) {
            contextVars.forEach(context::set);
        }

        if (isScript(expressionText)) {
            JexlScript script = jexlEngine.createScript(expressionText);
            return script.execute(context);
        } else {
            JexlExpression expression = jexlEngine.createExpression(expressionText);
            return expression.evaluate(context);
        }
    }

    /**
     * Determine whether the text should be treated as a script (multi-statement)
     * or a simple expression.
     */
    boolean isScript(String text) {
        if (text == null) return false;
        String trimmed = text.trim();

        // Strip comments and string literals for accurate detection
        String stripped = stripCommentsAndStrings(trimmed);

        return stripped.contains(";")
                || stripped.matches("(?s).*\\b(if|for|foreach|while|var|return)\\b.*")
                || stripped.matches("(?s).*[^=!<>]=[^=].*");
    }

    private String stripCommentsAndStrings(String text) {
        // Remove single-line comments
        String result = text.replaceAll("//[^\n]*", "");
        // Remove multi-line comments
        result = result.replaceAll("/\\*.*?\\*/", "");
        // Remove string literals
        result = result.replaceAll("\"[^\"]*\"", "\"\"");
        result = result.replaceAll("'[^']*'", "''");
        return result;
    }
}
