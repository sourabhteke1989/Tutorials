package com.example.jexl.controller;

import com.example.jexl.dto.EvaluationRequest;
import com.example.jexl.dto.EvaluationResponse;
import com.example.jexl.service.ExpressionCacheService;
import com.example.jexl.service.ExpressionEvaluationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/expressions")
public class ExpressionController {

    private final ExpressionEvaluationService evaluationService;
    private final ExpressionCacheService cacheService;

    public ExpressionController(ExpressionEvaluationService evaluationService,
                                 ExpressionCacheService cacheService) {
        this.evaluationService = evaluationService;
        this.cacheService = cacheService;
    }

    /**
     * Evaluate an expression by expression_id with context variables.
     *
     * POST /api/expressions/evaluate
     * Body:
     * {
     *   "expressionId": "PF",
     *   "todayDate": "2025-04-01",  (optional)
     *   "context": {
     *     "pf_applicable_monthly_earnings": 25000,
     *     "company_pf_wage_ceiling": 15000,
     *     "is_company_pf_wage_ceiling_applicable": true
     *   }
     * }
     */
    @PostMapping("/evaluate")
    public ResponseEntity<EvaluationResponse> evaluate(@RequestBody EvaluationRequest request) {
        EvaluationResponse response = evaluationService.evaluate(request);
        return ResponseEntity.ok(response);
    }

    /**
     * List all available expression IDs in cache.
     */
    @GetMapping("/list")
    public ResponseEntity<Set<String>> listExpressionIds() {
        return ResponseEntity.ok(cacheService.getAllExpressionIds());
    }

    /**
     * Refresh the in-memory cache from database.
     */
    @PostMapping("/cache/refresh")
    public ResponseEntity<Map<String, String>> refreshCache() {
        cacheService.refreshCache();
        return ResponseEntity.ok(Map.of("status", "Cache refreshed successfully"));
    }
}
