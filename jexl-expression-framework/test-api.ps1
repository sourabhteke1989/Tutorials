# =====================================================
# JEXL Expression Framework - API Test Script (PowerShell)
# Run this after starting the application:
#   mvn spring-boot:run
# Then execute:
#   .\test-api.ps1
# =====================================================

$BASE_URL = "http://localhost:8080/api/expressions"
$PASS = 0
$FAIL = 0
$TOTAL = 0

Write-Host "=================================================" -ForegroundColor Cyan
Write-Host "  JEXL Expression Framework - API Tests" -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor Cyan
Write-Host ""

function Test-Expression {
    param (
        [string]$Name,
        [string]$Payload,
        [string]$ExpectedField
    )

    $script:TOTAL++
    Write-Host "--- TEST: $Name ---" -ForegroundColor Yellow

    try {
        $response = Invoke-RestMethod -Uri "$BASE_URL/evaluate" `
            -Method POST `
            -ContentType "application/json" `
            -Body $Payload

        $responseJson = $response | ConvertTo-Json -Depth 5
        Write-Host "Response: $responseJson"

        if ($responseJson -match $ExpectedField) {
            Write-Host "RESULT: PASS" -ForegroundColor Green
            $script:PASS++
        } else {
            Write-Host "RESULT: FAIL (expected '$ExpectedField' in response)" -ForegroundColor Red
            $script:FAIL++
        }
    }
    catch {
        Write-Host "RESULT: FAIL - $($_.Exception.Message)" -ForegroundColor Red
        $script:FAIL++
    }
    Write-Host ""
}

# =====================================================
# 1. List all expressions
# =====================================================
Write-Host "--- TEST: List all expression IDs ---" -ForegroundColor Yellow
try {
    $list = Invoke-RestMethod -Uri "$BASE_URL/list" -Method GET
    Write-Host "Available expressions: $($list -join ', ')"
    Write-Host "RESULT: PASS" -ForegroundColor Green
    $PASS++; $TOTAL++
} catch {
    Write-Host "RESULT: FAIL - $($_.Exception.Message)" -ForegroundColor Red
    $FAIL++; $TOTAL++
}
Write-Host ""

# =====================================================
# 2. PF - With ceiling
# =====================================================
Test-Expression -Name "PF - Basic with ceiling" `
    -Payload '{"expressionId":"PF","context":{"pf_applicable_monthly_earnings":25000,"company_pf_wage_ceiling":15000,"is_company_pf_wage_ceiling_applicable":true}}' `
    -ExpectedField "EMPLOYEE_PF"

# =====================================================
# 3. PF - No ceiling
# =====================================================
Test-Expression -Name "PF - No wage ceiling" `
    -Payload '{"expressionId":"PF","context":{"pf_applicable_monthly_earnings":25000,"company_pf_wage_ceiling":15000,"is_company_pf_wage_ceiling_applicable":false}}' `
    -ExpectedField "EMPLOYEE_PF"

# =====================================================
# 4. PF - Old expression (2018)
# =====================================================
Test-Expression -Name "PF - Old expression (2018)" `
    -Payload '{"expressionId":"PF","todayDate":"2018-06-01","context":{"pf_applicable_monthly_earnings":20000,"company_pf_wage_ceiling":15000,"is_company_pf_wage_ceiling_applicable":true}}' `
    -ExpectedField "EMPLOYEE_PF"

# =====================================================
# 5. Yearly Gratuity
# =====================================================
Test-Expression -Name "Yearly Gratuity" `
    -Payload '{"expressionId":"GRATUITY_YEARLY","context":{"monthly_basic":30000,"monthly_dearness_allowance":5000}}' `
    -ExpectedField "GRATUITY_AMOUNT"

# =====================================================
# 6. Actual Gratuity
# =====================================================
Test-Expression -Name "Actual Gratuity (10 years)" `
    -Payload '{"expressionId":"GRATUITY_ACTUAL","context":{"basic_salary_monthly":50000,"dearness_allowance_monthly":10000,"completed_years_of_service":10,"months_in_final_year":7,"gratuity_max_limit":2500000}}' `
    -ExpectedField "GRATUITY_AMOUNT"

# =====================================================
# 7. ESI - Eligible
# =====================================================
Test-Expression -Name "ESI - Eligible employee" `
    -Payload '{"expressionId":"ESI","context":{"ESI_WAGES":18000,"MONTHLY_GROSS_SALARY":20000,"IS_DISABLED":false}}' `
    -ExpectedField "EMPLOYEE_ESI"

# =====================================================
# 8. ESI - Disabled
# =====================================================
Test-Expression -Name "ESI - Disabled employee" `
    -Payload '{"expressionId":"ESI","context":{"ESI_WAGES":23000,"MONTHLY_GROSS_SALARY":25000,"IS_DISABLED":true}}' `
    -ExpectedField "EMPLOYEE_ESI"

# =====================================================
# 9. ESI - Not eligible
# =====================================================
Test-Expression -Name "ESI - Not eligible (wages > 21000)" `
    -Payload '{"expressionId":"ESI","context":{"ESI_WAGES":22000,"MONTHLY_GROSS_SALARY":25000,"IS_DISABLED":false}}' `
    -ExpectedField "EMPLOYEE_ESI"

# =====================================================
# 10. PT_MH - Male, Feb
# =====================================================
Test-Expression -Name "PT_MH - Male, Feb, salary 15000" `
    -Payload '{"expressionId":"PT_MH","context":{"MONTHLY_GROSS_SALARY":15000,"GENDER":"MALE","MONTH":2}}' `
    -ExpectedField "PT"

# =====================================================
# 11. PT_MH - Female
# =====================================================
Test-Expression -Name "PT_MH - Female, salary 30000" `
    -Payload '{"expressionId":"PT_MH","context":{"MONTHLY_GROSS_SALARY":30000,"GENDER":"FEMALE","MONTH":3}}' `
    -ExpectedField "PT"

# =====================================================
# 12. PT_KA - Feb (adjustment)
# =====================================================
Test-Expression -Name "PT_KA - Feb, salary 25000" `
    -Payload '{"expressionId":"PT_KA","context":{"MONTHLY_GROSS_SALARY":25000,"MONTH":2}}' `
    -ExpectedField "PT"

# =====================================================
# 13. PT_KA - Non-Feb
# =====================================================
Test-Expression -Name "PT_KA - March, salary 25000" `
    -Payload '{"expressionId":"PT_KA","context":{"MONTHLY_GROSS_SALARY":25000,"MONTH":3}}' `
    -ExpectedField "PT"

# =====================================================
# 14. PT_KL
# =====================================================
Test-Expression -Name "PT_KL - salary 8000" `
    -Payload '{"expressionId":"PT_KL","context":{"MONTHLY_GROSS_SALARY":8000}}' `
    -ExpectedField "PT"

# =====================================================
# 15. PT_WB
# =====================================================
Test-Expression -Name "PT_WB - salary 35000" `
    -Payload '{"expressionId":"PT_WB","context":{"MONTHLY_GROSS_SALARY":35000}}' `
    -ExpectedField "PT"

# =====================================================
# 16. LWF_MH - June
# =====================================================
Test-Expression -Name "LWF_MH - June (deduction month)" `
    -Payload '{"expressionId":"LWF_MH","context":{"MONTH":6}}' `
    -ExpectedField "LWF_EMPLOYEE"

# =====================================================
# 17. LWF_MH - March (no deduction)
# =====================================================
Test-Expression -Name "LWF_MH - March (no deduction)" `
    -Payload '{"expressionId":"LWF_MH","context":{"MONTH":3}}' `
    -ExpectedField "LWF_EMPLOYEE"

# =====================================================
# 18. LWF_GJ
# =====================================================
Test-Expression -Name "LWF_GJ - any month" `
    -Payload '{"expressionId":"LWF_GJ","context":{}}' `
    -ExpectedField "LWF_EMPLOYEE"

# =====================================================
# 19. LWF_BR - January
# =====================================================
Test-Expression -Name "LWF_BR - January" `
    -Payload '{"expressionId":"LWF_BR","context":{"MONTH":1}}' `
    -ExpectedField "LWF_EMPLOYEE"

# =====================================================
# 20. LWF_BR - March (no deduction)
# =====================================================
Test-Expression -Name "LWF_BR - March (no deduction)" `
    -Payload '{"expressionId":"LWF_BR","context":{"MONTH":3}}' `
    -ExpectedField "LWF_EMPLOYEE"

# =====================================================
# 21. Simple Expression (BONUS_PCT)
# =====================================================
Test-Expression -Name "BONUS_PCT - Simple expression" `
    -Payload '{"expressionId":"BONUS_PCT","context":{"MONTHLY_GROSS_SALARY":50000,"BONUS_PERCENTAGE":10}}' `
    -ExpectedField "result"

# =====================================================
# 22. PT_MH - Old slabs (2021)
# =====================================================
Test-Expression -Name "PT_MH - Old slabs (todayDate=2019)" `
    -Payload '{"expressionId":"PT_MH","todayDate":"2019-06-15","context":{"MONTHLY_GROSS_SALARY":15000,"GENDER":"FEMALE","MONTH":6}}' `
    -ExpectedField "PT"

# =====================================================
# 23. Refresh cache
# =====================================================
Write-Host "--- TEST: Refresh cache ---" -ForegroundColor Yellow
$TOTAL++
try {
    $resp = Invoke-RestMethod -Uri "$BASE_URL/cache/refresh" -Method POST
    $respJson = $resp | ConvertTo-Json
    Write-Host "Response: $respJson"
    Write-Host "RESULT: PASS" -ForegroundColor Green
    $PASS++
} catch {
    Write-Host "RESULT: FAIL - $($_.Exception.Message)" -ForegroundColor Red
    $FAIL++
}
Write-Host ""

# =====================================================
# SUMMARY
# =====================================================
Write-Host "=================================================" -ForegroundColor Cyan
Write-Host "  TEST SUMMARY" -ForegroundColor Cyan
Write-Host "  Passed: $PASS" -ForegroundColor Green
Write-Host "  Failed: $FAIL" -ForegroundColor $(if ($FAIL -gt 0) { "Red" } else { "Green" })
Write-Host "  Total:  $TOTAL" -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor Cyan
