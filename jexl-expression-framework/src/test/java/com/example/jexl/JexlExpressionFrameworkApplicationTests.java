package com.example.jexl;

import com.example.jexl.dto.EvaluationRequest;
import com.example.jexl.dto.EvaluationResponse;
import com.example.jexl.service.ExpressionCacheService;
import com.example.jexl.service.ExpressionEvaluationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JexlExpressionFrameworkApplicationTests {

    @Autowired
    private ExpressionEvaluationService evaluationService;

    @Autowired
    private ExpressionCacheService cacheService;

    @Test
    void contextLoads() {
        Set<String> ids = cacheService.getAllExpressionIds();
        assertNotNull(ids);
        assertFalse(ids.isEmpty());
        System.out.println("Loaded expression IDs: " + ids);
    }

    // ========== PF Tests ==========

    @Test
    void testPF_WithCeiling() {
        EvaluationRequest req = new EvaluationRequest();
        req.setExpressionId("PF");
        req.setContext(Map.of(
                "pf_applicable_monthly_earnings", 25000,
                "company_pf_wage_ceiling", 15000,
                "is_company_pf_wage_ceiling_applicable", true
        ));

        EvaluationResponse resp = evaluationService.evaluate(req);
        assertNotNull(resp.getResult());
        Map<?, ?> result = (Map<?, ?>) resp.getResult();
        assertEquals(1800.0, ((Number) result.get("EMPLOYEE_PF")).doubleValue(), 0.01);
        assertEquals(1800.0, ((Number) result.get("EMPLOYER_PF")).doubleValue(), 0.01);
        System.out.println("PF with ceiling: " + result);
    }

    @Test
    void testPF_NoCeiling() {
        EvaluationRequest req = new EvaluationRequest();
        req.setExpressionId("PF");
        req.setContext(Map.of(
                "pf_applicable_monthly_earnings", 25000,
                "company_pf_wage_ceiling", 15000,
                "is_company_pf_wage_ceiling_applicable", false
        ));

        EvaluationResponse resp = evaluationService.evaluate(req);
        Map<?, ?> result = (Map<?, ?>) resp.getResult();
        assertEquals(3000.0, ((Number) result.get("EMPLOYEE_PF")).doubleValue(), 0.01);
        System.out.println("PF no ceiling: " + result);
    }

    @Test
    void testPF_OldExpression() {
        EvaluationRequest req = new EvaluationRequest();
        req.setExpressionId("PF");
        req.setTodayDate("2018-06-01");
        req.setContext(Map.of(
                "pf_applicable_monthly_earnings", 20000,
                "company_pf_wage_ceiling", 15000,
                "is_company_pf_wage_ceiling_applicable", true
        ));

        EvaluationResponse resp = evaluationService.evaluate(req);
        Map<?, ?> result = (Map<?, ?>) resp.getResult();
        // Old expression uses 12000 ceiling
        assertEquals(1440.0, ((Number) result.get("EMPLOYEE_PF")).doubleValue(), 0.01);
        assertEquals("2015-01-01", resp.getEffectiveFrom());
        System.out.println("PF old (2018): " + result);
    }

    // ========== Gratuity Tests ==========

    @Test
    void testGratuityYearly() {
        EvaluationRequest req = new EvaluationRequest();
        req.setExpressionId("GRATUITY_YEARLY");
        req.setContext(Map.of(
                "monthly_basic", 30000,
                "monthly_dearness_allowance", 5000
        ));

        EvaluationResponse resp = evaluationService.evaluate(req);
        Map<?, ?> result = (Map<?, ?>) resp.getResult();
        double expected = (35000.0 * 15) / 26;
        assertEquals(expected, ((Number) result.get("GRATUITY_AMOUNT")).doubleValue(), 0.01);
        System.out.println("Yearly Gratuity: " + result);
    }

    @Test
    void testGratuityActual() {
        EvaluationRequest req = new EvaluationRequest();
        req.setExpressionId("GRATUITY_ACTUAL");
        req.setContext(Map.of(
                "basic_salary_monthly", 50000,
                "dearness_allowance_monthly", 10000,
                "completed_years_of_service", 10,
                "months_in_final_year", 7,
                "gratuity_max_limit", 2500000
        ));

        EvaluationResponse resp = evaluationService.evaluate(req);
        Map<?, ?> result = (Map<?, ?>) resp.getResult();
        // years = 10 + 1 = 11 (months >= 6), gratuity = 15 * 60000 * 11 / 26
        double expected = (15.0 * 60000 * 11) / 26;
        assertEquals(expected, ((Number) result.get("GRATUITY_AMOUNT")).doubleValue(), 0.01);
        System.out.println("Actual Gratuity: " + result);
    }

    // ========== ESI Tests ==========

    @Test
    void testESI_Eligible() {
        EvaluationRequest req = new EvaluationRequest();
        req.setExpressionId("ESI");
        req.setContext(Map.of(
                "ESI_WAGES", 18000,
                "MONTHLY_GROSS_SALARY", 20000,
                "IS_DISABLED", false
        ));

        EvaluationResponse resp = evaluationService.evaluate(req);
        Map<?, ?> result = (Map<?, ?>) resp.getResult();
        assertEquals(18000 * 0.0075, ((Number) result.get("EMPLOYEE_ESI")).doubleValue(), 0.01);
        assertEquals(18000 * 0.0325, ((Number) result.get("EMPLOYER_ESI")).doubleValue(), 0.01);
        System.out.println("ESI eligible: " + result);
    }

    @Test
    void testESI_NotEligible() {
        EvaluationRequest req = new EvaluationRequest();
        req.setExpressionId("ESI");
        req.setContext(Map.of(
                "ESI_WAGES", 22000,
                "MONTHLY_GROSS_SALARY", 25000,
                "IS_DISABLED", false
        ));

        EvaluationResponse resp = evaluationService.evaluate(req);
        Map<?, ?> result = (Map<?, ?>) resp.getResult();
        assertEquals(0.0, ((Number) result.get("EMPLOYEE_ESI")).doubleValue(), 0.01);
        assertEquals(0.0, ((Number) result.get("EMPLOYER_ESI")).doubleValue(), 0.01);
        System.out.println("ESI not eligible: " + result);
    }

    // ========== PT Tests ==========

    @Test
    void testPT_MH_Male_Feb() {
        EvaluationRequest req = new EvaluationRequest();
        req.setExpressionId("PT_MH");
        req.setContext(Map.of(
                "MONTHLY_GROSS_SALARY", 15000,
                "GENDER", "MALE",
                "MONTH", 2
        ));

        EvaluationResponse resp = evaluationService.evaluate(req);
        Map<?, ?> result = (Map<?, ?>) resp.getResult();
        // salary 15000 > 10000 → pt=200, Feb male → +100 = 300
        assertEquals(300.0, ((Number) result.get("PT")).doubleValue(), 0.01);
        System.out.println("PT_MH Male Feb: " + result);
    }

    @Test
    void testPT_MH_Female() {
        EvaluationRequest req = new EvaluationRequest();
        req.setExpressionId("PT_MH");
        req.setContext(Map.of(
                "MONTHLY_GROSS_SALARY", 30000,
                "GENDER", "FEMALE",
                "MONTH", 3
        ));

        EvaluationResponse resp = evaluationService.evaluate(req);
        Map<?, ?> result = (Map<?, ?>) resp.getResult();
        // Female > 25000 → pt=200
        assertEquals(200.0, ((Number) result.get("PT")).doubleValue(), 0.01);
        System.out.println("PT_MH Female: " + result);
    }

    @Test
    void testPT_KA_Feb() {
        EvaluationRequest req = new EvaluationRequest();
        req.setExpressionId("PT_KA");
        req.setContext(Map.of(
                "MONTHLY_GROSS_SALARY", 25000,
                "MONTH", 2
        ));

        EvaluationResponse resp = evaluationService.evaluate(req);
        Map<?, ?> result = (Map<?, ?>) resp.getResult();
        // salary > 20000 → pt=200, Feb → +200 = 400
        assertEquals(400.0, ((Number) result.get("PT")).doubleValue(), 0.01);
        System.out.println("PT_KA Feb: " + result);
    }

    @Test
    void testPT_KA_NonFeb() {
        EvaluationRequest req = new EvaluationRequest();
        req.setExpressionId("PT_KA");
        req.setContext(Map.of(
                "MONTHLY_GROSS_SALARY", 25000,
                "MONTH", 3
        ));

        EvaluationResponse resp = evaluationService.evaluate(req);
        Map<?, ?> result = (Map<?, ?>) resp.getResult();
        assertEquals(200.0, ((Number) result.get("PT")).doubleValue(), 0.01);
        System.out.println("PT_KA Non-Feb: " + result);
    }

    @Test
    void testPT_KL() {
        EvaluationRequest req = new EvaluationRequest();
        req.setExpressionId("PT_KL");
        req.setContext(Map.of("MONTHLY_GROSS_SALARY", 8000));

        EvaluationResponse resp = evaluationService.evaluate(req);
        Map<?, ?> result = (Map<?, ?>) resp.getResult();
        // 7500 <= 8000 < 10000 → pt=160
        assertEquals(160.0, ((Number) result.get("PT")).doubleValue(), 0.01);
        System.out.println("PT_KL: " + result);
    }

    // ========== LWF Tests ==========

    @Test
    void testLWF_MH_June() {
        EvaluationRequest req = new EvaluationRequest();
        req.setExpressionId("LWF_MH");
        req.setContext(Map.of("MONTH", 6));

        EvaluationResponse resp = evaluationService.evaluate(req);
        Map<?, ?> result = (Map<?, ?>) resp.getResult();
        assertEquals(12.0, ((Number) result.get("LWF_EMPLOYEE")).doubleValue(), 0.01);
        assertEquals(36.0, ((Number) result.get("LWF_EMPLOYER")).doubleValue(), 0.01);
        System.out.println("LWF_MH June: " + result);
    }

    @Test
    void testLWF_MH_March() {
        EvaluationRequest req = new EvaluationRequest();
        req.setExpressionId("LWF_MH");
        req.setContext(Map.of("MONTH", 3));

        EvaluationResponse resp = evaluationService.evaluate(req);
        Map<?, ?> result = (Map<?, ?>) resp.getResult();
        assertEquals(0.0, ((Number) result.get("LWF_EMPLOYEE")).doubleValue(), 0.01);
        assertEquals(0.0, ((Number) result.get("LWF_EMPLOYER")).doubleValue(), 0.01);
        System.out.println("LWF_MH March: " + result);
    }

    @Test
    void testLWF_GJ() {
        EvaluationRequest req = new EvaluationRequest();
        req.setExpressionId("LWF_GJ");
        req.setContext(Map.of());

        EvaluationResponse resp = evaluationService.evaluate(req);
        Map<?, ?> result = (Map<?, ?>) resp.getResult();
        assertEquals(10.0, ((Number) result.get("LWF_EMPLOYEE")).doubleValue(), 0.01);
        assertEquals(20.0, ((Number) result.get("LWF_EMPLOYER")).doubleValue(), 0.01);
        System.out.println("LWF_GJ: " + result);
    }

    // ========== Simple Expression Test ==========

    @Test
    void testSimpleExpression_BonusPct() {
        EvaluationRequest req = new EvaluationRequest();
        req.setExpressionId("BONUS_PCT");
        req.setContext(Map.of(
                "MONTHLY_GROSS_SALARY", 50000,
                "BONUS_PERCENTAGE", 10
        ));

        EvaluationResponse resp = evaluationService.evaluate(req);
        // Simple expression: 50000 * 10 / 100 = 5000
        assertEquals(5000.0, ((Number) resp.getResult()).doubleValue(), 0.01);
        System.out.println("BONUS_PCT: " + resp.getResult());
    }

    // ========== Date-based Expression Selection ==========

    @Test
    void testPT_MH_OldSlabs() {
        EvaluationRequest req = new EvaluationRequest();
        req.setExpressionId("PT_MH");
        req.setTodayDate("2019-06-15");
        req.setContext(Map.of(
                "MONTHLY_GROSS_SALARY", 15000,
                "GENDER", "FEMALE",
                "MONTH", 6
        ));

        EvaluationResponse resp = evaluationService.evaluate(req);
        Map<?, ?> result = (Map<?, ?>) resp.getResult();
        // Old slabs (before 2020): Female > 10000 → pt=200
        assertEquals(200.0, ((Number) result.get("PT")).doubleValue(), 0.01);
        assertEquals("2015-01-01", resp.getEffectiveFrom());
        assertEquals("2022-12-31", resp.getEffectiveTo());
        System.out.println("PT_MH old slabs: " + result);
    }

    @Test
    void testInvalidExpressionId() {
        EvaluationRequest req = new EvaluationRequest();
        req.setExpressionId("INVALID_ID");
        req.setContext(Map.of());

        assertThrows(IllegalArgumentException.class, () -> evaluationService.evaluate(req));
    }
}
