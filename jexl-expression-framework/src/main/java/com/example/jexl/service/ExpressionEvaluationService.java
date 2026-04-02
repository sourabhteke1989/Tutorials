package com.example.jexl.service;

import com.example.jexl.dto.EvaluationRequest;
import com.example.jexl.dto.EvaluationResponse;
import com.example.jexl.engine.CompiledPayrollExpression;
import com.example.jexl.engine.JexlEvaluationEngine;
import com.example.jexl.entity.PayrollExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class ExpressionEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(ExpressionEvaluationService.class);

    private final ExpressionCacheService cacheService;
    private final JexlEvaluationEngine engine;

    public ExpressionEvaluationService(ExpressionCacheService cacheService,
                                        JexlEvaluationEngine engine) {
        this.cacheService = cacheService;
        this.engine = engine;
    }

    public EvaluationResponse evaluate(EvaluationRequest request) {
        // Determine reference date
        LocalDate todayDate;
        if (request.getTodayDate() != null && !request.getTodayDate().isBlank()) {
            todayDate = LocalDate.parse(request.getTodayDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        } else {
            todayDate = LocalDate.now();
        }

        String expressionId = request.getExpressionId();

        // Find applicable pre-compiled expression
        CompiledPayrollExpression compiled = cacheService.findApplicableExpression(expressionId, todayDate)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No applicable expression found for ID='" + expressionId
                                + "' on date=" + todayDate));

        PayrollExpression entity = compiled.getEntity();
        log.info("Evaluating expression_id='{}', effective_from={}, effective_to={}, type={}",
                expressionId, entity.getEffectiveFrom(), entity.getEffectiveTo(),
                compiled.isScript() ? "SCRIPT" : "EXPRESSION");

        // Build context
        Map<String, Object> context = request.getContext() != null
                ? new HashMap<>(request.getContext())
                : new HashMap<>();

        // Execute the pre-compiled object — no parsing overhead
        Object result = engine.execute(compiled, context);

        // Build response
        EvaluationResponse response = new EvaluationResponse();
        response.setExpressionId(expressionId);
        response.setResult(result);
        response.setEffectiveFrom(entity.getEffectiveFrom().toString());
        response.setEffectiveTo(entity.getEffectiveTo() != null ? entity.getEffectiveTo().toString() : null);
        response.setContextUsed(context);

        return response;
    }
}
