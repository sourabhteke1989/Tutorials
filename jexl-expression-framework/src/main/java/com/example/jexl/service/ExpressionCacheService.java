package com.example.jexl.service;

import com.example.jexl.engine.CompiledPayrollExpression;
import com.example.jexl.engine.JexlEvaluationEngine;
import com.example.jexl.entity.PayrollExpression;
import com.example.jexl.repository.PayrollExpressionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory cache for payroll expressions.
 * Loads all expressions from DB at startup, compiles them into JEXL objects,
 * and provides lookup by expression_id + date.
 *
 * Compiled objects (JexlScript / JexlExpression) are created once and reused
 * on every evaluation — eliminating repeated parsing overhead.
 */
@Service
public class ExpressionCacheService {

    private static final Logger log = LoggerFactory.getLogger(ExpressionCacheService.class);

    private final PayrollExpressionRepository repository;
    private final JexlEvaluationEngine engine;

    // Cache: expression_id -> list of CompiledPayrollExpression sorted by effective_from DESC
    private final Map<String, List<CompiledPayrollExpression>> cache = new ConcurrentHashMap<>();

    public ExpressionCacheService(PayrollExpressionRepository repository,
                                   JexlEvaluationEngine engine) {
        this.repository = repository;
        this.engine = engine;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadCache() {
        List<PayrollExpression> all = repository.findAll();
        cache.clear();

        int scripts = 0;
        int expressions = 0;

        // Compile each entity and group by expression_id
        Map<String, List<CompiledPayrollExpression>> grouped = new HashMap<>();
        for (PayrollExpression entity : all) {
            CompiledPayrollExpression compiled = engine.compile(entity);
            grouped.computeIfAbsent(entity.getExpressionId(), k -> new ArrayList<>())
                    .add(compiled);
            if (compiled.isScript()) { scripts++; } else { expressions++; }
        }

        // Sort each group by effective_from descending (newest first)
        grouped.forEach((key, list) -> {
            list.sort(Comparator.comparing(
                    (CompiledPayrollExpression c) -> c.getEntity().getEffectiveFrom()).reversed());
            cache.put(key, list);
        });

        log.info("Expression cache loaded: {} expression IDs, {} total entries "
                        + "({} compiled as scripts, {} as expressions)",
                cache.size(), all.size(), scripts, expressions);
    }

    /**
     * Refresh cache (can be called via API if needed).
     */
    public void refreshCache() {
        loadCache();
    }

    /**
     * Find the applicable compiled expression for the given expression_id and reference date.
     *
     * Rules:
     * 1. Filter entries where effective_from <= todayDate
     * 2. Filter entries where effective_to is null OR effective_to >= todayDate
     * 3. Among qualifying entries, pick the one with the latest effective_from
     */
    public Optional<CompiledPayrollExpression> findApplicableExpression(String expressionId, LocalDate todayDate) {
        List<CompiledPayrollExpression> entries = cache.get(expressionId);
        if (entries == null || entries.isEmpty()) {
            return Optional.empty();
        }

        return entries.stream()
                .filter(c -> !c.getEntity().getEffectiveFrom().isAfter(todayDate))
                .filter(c -> c.getEntity().getEffectiveTo() == null
                        || !c.getEntity().getEffectiveTo().isBefore(todayDate))
                .findFirst(); // already sorted by effective_from DESC
    }

    /**
     * Get all cached expression IDs.
     */
    public Set<String> getAllExpressionIds() {
        return Collections.unmodifiableSet(cache.keySet());
    }

    /**
     * Get all entries for a given expression_id.
     */
    public List<CompiledPayrollExpression> getEntries(String expressionId) {
        return cache.getOrDefault(expressionId, Collections.emptyList());
    }
}
