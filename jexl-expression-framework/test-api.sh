#!/bin/bash
# =====================================================
# JEXL Expression Framework - API Test Script
# Run this after starting the application:
#   mvn spring-boot:run
# Then execute:
#   bash test-api.sh
# Or on Windows with curl available:
#   sh test-api.sh
# =====================================================

BASE_URL="http://localhost:8080/api/expressions"
PASS=0
FAIL=0

echo "================================================="
echo "  JEXL Expression Framework - API Tests"
echo "================================================="
echo ""

# Helper function
test_case() {
    local name="$1"
    local payload="$2"
    local expected_field="$3"
    local expected_value="$4"

    echo "--- TEST: $name ---"
    response=$(curl -s -X POST "$BASE_URL/evaluate" \
        -H "Content-Type: application/json" \
        -d "$payload")

    echo "Response: $response"

    if echo "$response" | grep -q "$expected_field"; then
        echo "RESULT: PASS"
        PASS=$((PASS + 1))
    else
        echo "RESULT: FAIL (expected to find '$expected_field')"
        FAIL=$((FAIL + 1))
    fi
    echo ""
}

# =====================================================
# 1. List all expressions
# =====================================================
echo "--- TEST: List all expression IDs ---"
response=$(curl -s -X GET "$BASE_URL/list")
echo "Available expressions: $response"
echo ""

# =====================================================
# 2. PF Calculation (current)
# =====================================================
test_case "PF - Basic with ceiling" \
    '{"expressionId":"PF","context":{"pf_applicable_monthly_earnings":25000,"company_pf_wage_ceiling":15000,"is_company_pf_wage_ceiling_applicable":true}}' \
    "EMPLOYEE_PF" "1800"

# =====================================================
# 3. PF - No ceiling
# =====================================================
test_case "PF - No wage ceiling" \
    '{"expressionId":"PF","context":{"pf_applicable_monthly_earnings":25000,"company_pf_wage_ceiling":15000,"is_company_pf_wage_ceiling_applicable":false}}' \
    "EMPLOYEE_PF" "3000"

# =====================================================
# 4. PF - Old expression (todayDate = 2018-06-01)
# =====================================================
test_case "PF - Old expression (2018)" \
    '{"expressionId":"PF","todayDate":"2018-06-01","context":{"pf_applicable_monthly_earnings":20000,"company_pf_wage_ceiling":15000,"is_company_pf_wage_ceiling_applicable":true}}' \
    "EMPLOYEE_PF"

# =====================================================
# 5. Yearly Gratuity
# =====================================================
test_case "Yearly Gratuity" \
    '{"expressionId":"GRATUITY_YEARLY","context":{"monthly_basic":30000,"monthly_dearness_allowance":5000}}' \
    "GRATUITY_AMOUNT"

# =====================================================
# 6. Actual Gratuity
# =====================================================
test_case "Actual Gratuity (10 years service)" \
    '{"expressionId":"GRATUITY_ACTUAL","context":{"basic_salary_monthly":50000,"dearness_allowance_monthly":10000,"completed_years_of_service":10,"months_in_final_year":7,"gratuity_max_limit":2500000}}' \
    "GRATUITY_AMOUNT"

# =====================================================
# 7. ESI - Eligible employee
# =====================================================
test_case "ESI - Eligible employee" \
    '{"expressionId":"ESI","context":{"ESI_WAGES":18000,"MONTHLY_GROSS_SALARY":20000,"IS_DISABLED":false}}' \
    "EMPLOYEE_ESI"

# =====================================================
# 8. ESI - Disabled employee (higher ceiling)
# =====================================================
test_case "ESI - Disabled employee" \
    '{"expressionId":"ESI","context":{"ESI_WAGES":23000,"MONTHLY_GROSS_SALARY":25000,"IS_DISABLED":true}}' \
    "EMPLOYEE_ESI"

# =====================================================
# 9. ESI - Not eligible (wages > 21000)
# =====================================================
test_case "ESI - Not eligible" \
    '{"expressionId":"ESI","context":{"ESI_WAGES":22000,"MONTHLY_GROSS_SALARY":25000,"IS_DISABLED":false}}' \
    "EMPLOYEE_ESI"

# =====================================================
# 10. PT - Maharashtra (Male, Feb)
# =====================================================
test_case "PT_MH - Male, Feb, salary 15000" \
    '{"expressionId":"PT_MH","context":{"MONTHLY_GROSS_SALARY":15000,"GENDER":"MALE","MONTH":2}}' \
    "PT"

# =====================================================
# 11. PT - Maharashtra (Female)
# =====================================================
test_case "PT_MH - Female, salary 30000" \
    '{"expressionId":"PT_MH","context":{"MONTHLY_GROSS_SALARY":30000,"GENDER":"FEMALE","MONTH":3}}' \
    "PT"

# =====================================================
# 12. PT - Karnataka (Feb adjustment)
# =====================================================
test_case "PT_KA - Feb, salary 25000" \
    '{"expressionId":"PT_KA","context":{"MONTHLY_GROSS_SALARY":25000,"MONTH":2}}' \
    "PT"

# =====================================================
# 13. PT - Karnataka (Non-Feb)
# =====================================================
test_case "PT_KA - March, salary 25000" \
    '{"expressionId":"PT_KA","context":{"MONTHLY_GROSS_SALARY":25000,"MONTH":3}}' \
    "PT"

# =====================================================
# 14. PT - Kerala
# =====================================================
test_case "PT_KL - salary 8000" \
    '{"expressionId":"PT_KL","context":{"MONTHLY_GROSS_SALARY":8000}}' \
    "PT"

# =====================================================
# 15. PT - West Bengal
# =====================================================
test_case "PT_WB - salary 35000" \
    '{"expressionId":"PT_WB","context":{"MONTHLY_GROSS_SALARY":35000}}' \
    "PT"

# =====================================================
# 16. LWF - Maharashtra (June - deduction month)
# =====================================================
test_case "LWF_MH - June (deduction month)" \
    '{"expressionId":"LWF_MH","context":{"MONTH":6}}' \
    "LWF_EMPLOYEE"

# =====================================================
# 17. LWF - Maharashtra (March - no deduction)
# =====================================================
test_case "LWF_MH - March (no deduction)" \
    '{"expressionId":"LWF_MH","context":{"MONTH":3}}' \
    "LWF_EMPLOYEE"

# =====================================================
# 18. LWF - Gujarat (Monthly - always deducted)
# =====================================================
test_case "LWF_GJ - any month" \
    '{"expressionId":"LWF_GJ","context":{}}' \
    "LWF_EMPLOYEE"

# =====================================================
# 19. LWF - Bihar (Annual - January only)
# =====================================================
test_case "LWF_BR - January" \
    '{"expressionId":"LWF_BR","context":{"MONTH":1}}' \
    "LWF_EMPLOYEE"

# =====================================================
# 20. LWF - Bihar (Non-January)
# =====================================================
test_case "LWF_BR - March (no deduction)" \
    '{"expressionId":"LWF_BR","context":{"MONTH":3}}' \
    "LWF_EMPLOYEE"

# =====================================================
# 21. Simple Expression (BONUS_PCT - not a script)
# =====================================================
test_case "BONUS_PCT - Simple expression" \
    '{"expressionId":"BONUS_PCT","context":{"MONTHLY_GROSS_SALARY":50000,"BONUS_PERCENTAGE":10}}' \
    "result"

# =====================================================
# 22. PT_MH with old todayDate (should use old slabs)
# =====================================================
test_case "PT_MH - Old slabs (2019)" \
    '{"expressionId":"PT_MH","todayDate":"2019-06-15","context":{"MONTHLY_GROSS_SALARY":15000,"GENDER":"FEMALE","MONTH":6}}' \
    "PT"

# =====================================================
# 23. Refresh cache
# =====================================================
echo "--- TEST: Refresh cache ---"
response=$(curl -s -X POST "$BASE_URL/cache/refresh")
echo "Response: $response"
if echo "$response" | grep -q "refreshed"; then
    echo "RESULT: PASS"
    PASS=$((PASS + 1))
else
    echo "RESULT: FAIL"
    FAIL=$((FAIL + 1))
fi
echo ""

# =====================================================
# SUMMARY
# =====================================================
echo "================================================="
echo "  TEST SUMMARY"
echo "  Passed: $PASS"
echo "  Failed: $FAIL"
echo "  Total:  $((PASS + FAIL))"
echo "================================================="
