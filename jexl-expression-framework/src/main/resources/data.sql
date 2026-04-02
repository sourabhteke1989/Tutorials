-- =====================================================
-- PAYROLL EXPRESSIONS - SEED DATA
-- =====================================================

-- =====================================================
-- 1. PROVIDENT FUND (PF)
-- =====================================================
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('PF',
'// PF Calculation
// Inputs: pf_applicable_monthly_earnings, company_pf_wage_ceiling, is_company_pf_wage_ceiling_applicable
pf_base = is_company_pf_wage_ceiling_applicable == true ? (pf_applicable_monthly_earnings < company_pf_wage_ceiling ? pf_applicable_monthly_earnings : company_pf_wage_ceiling) : pf_applicable_monthly_earnings;
pf_rate = 0.12;
employee_pf = pf_base * pf_rate;
employer_pf = pf_base * pf_rate;
{ "EMPLOYEE_PF": employee_pf, "EMPLOYER_PF": employer_pf }',
'2020-01-01', NULL,
'pf_applicable_monthly_earnings,company_pf_wage_ceiling,is_company_pf_wage_ceiling_applicable');

-- =====================================================
-- 2. YEARLY GRATUITY (for offer letter display)
-- =====================================================
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('GRATUITY_YEARLY',
'// Yearly Gratuity for Offer Letter
// Inputs: monthly_basic, monthly_dearness_allowance
salary = monthly_basic + monthly_dearness_allowance;
gratuity_amount = (salary * 15) / 26.0;
{ "GRATUITY_AMOUNT": gratuity_amount }',
'2020-01-01', NULL,
'monthly_basic,monthly_dearness_allowance');

-- =====================================================
-- 3. ACTUAL GRATUITY (service period based)
-- =====================================================
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('GRATUITY_ACTUAL',
'// Actual Gratuity Calculation
// Inputs: basic_salary_monthly, dearness_allowance_monthly, completed_years_of_service, months_in_final_year, gratuity_max_limit
last_drawn_salary = basic_salary_monthly + dearness_allowance_monthly;
years = completed_years_of_service + (months_in_final_year >= 6 ? 1 : 0);
gratuity = (15 * last_drawn_salary * years) / 26.0;
gratuity_payable = gratuity > gratuity_max_limit ? gratuity_max_limit : gratuity;
{ "GRATUITY_AMOUNT": gratuity_payable }',
'2020-01-01', NULL,
'basic_salary_monthly,dearness_allowance_monthly,completed_years_of_service,months_in_final_year,gratuity_max_limit');

-- =====================================================
-- 4. ESI (Employee State Insurance)
-- =====================================================
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('ESI',
'// ESI Calculation
// Inputs: ESI_WAGES, MONTHLY_GROSS_SALARY, IS_DISABLED, DAILY_WAGE (optional)
wages = ESI_WAGES;
gross = MONTHLY_GROSS_SALARY;
disabled = IS_DISABLED == true;
wageLimit = disabled ? 25000 : 21000;
eligible = (wages <= wageLimit);
daily = DAILY_WAGE != null ? DAILY_WAGE : (gross / 30);
lowWageExempt = (daily <= 176);
employeeRate = 0.0075;
employerRate = 0.0325;
employeeESI = 0;
employerESI = 0;
if (eligible) {
  employeeESI = lowWageExempt ? 0 : (wages * employeeRate);
  employerESI = wages * employerRate;
}
{ "EMPLOYEE_ESI": employeeESI, "EMPLOYER_ESI": employerESI }',
'2020-01-01', NULL,
'ESI_WAGES,MONTHLY_GROSS_SALARY,IS_DISABLED,DAILY_WAGE');


-- =====================================================
-- 5. PROFESSIONAL TAX - STATE WISE
-- =====================================================

-- PT_MH - Maharashtra (Gender-based, Feb adjustment for males)
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('PT_MH',
'// Professional Tax - Maharashtra
// Inputs: MONTHLY_GROSS_SALARY, GENDER, MONTH
income = MONTHLY_GROSS_SALARY;
gender = GENDER != null ? GENDER : "MALE";
month = MONTH;
pt = 0;
if (gender == "FEMALE") {
  if (income <= 25000) { pt = 0; }
  if (income > 25000) { pt = 200; }
} else {
  if (income <= 7500) { pt = 0; }
  if (income > 7500 && income <= 10000) { pt = 175; }
  if (income > 10000) { pt = 200; }
}
if (gender == "MALE" && month == 2 && pt > 0) { pt = pt + 100; }
{ "PT": pt }',
'2020-01-01', NULL,
'MONTHLY_GROSS_SALARY,GENDER,MONTH');

-- PT_KA - Karnataka (Feb adjustment)
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('PT_KA',
'// Professional Tax - Karnataka
// Inputs: MONTHLY_GROSS_SALARY, MONTH
income = MONTHLY_GROSS_SALARY;
month = MONTH;
pt = 0;
if (income <= 15000) { pt = 0; }
if (income > 15000 && income <= 20000) { pt = 150; }
if (income > 20000) { pt = 200; }
if (month == 2 && pt == 200) { pt = pt + 200; }
{ "PT": pt }',
'2020-01-01', NULL,
'MONTHLY_GROSS_SALARY,MONTH');

-- PT_TN - Tamil Nadu
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('PT_TN',
'// Professional Tax - Tamil Nadu (annual PT / 12)
income = MONTHLY_GROSS_SALARY;
pt = 0;
if (income <= 21000) { pt = 0; }
if (income > 21000) { pt = 2500 / 12; }
{ "PT": pt }',
'2020-01-01', NULL,
'MONTHLY_GROSS_SALARY');

-- PT_WB - West Bengal
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('PT_WB',
'// Professional Tax - West Bengal
income = MONTHLY_GROSS_SALARY;
pt = 0;
if (income <= 10000) { pt = 0; }
if (income > 10000 && income <= 15000) { pt = 110; }
if (income > 15000 && income <= 25000) { pt = 130; }
if (income > 25000 && income <= 40000) { pt = 150; }
if (income > 40000) { pt = 200; }
{ "PT": pt }',
'2020-01-01', NULL,
'MONTHLY_GROSS_SALARY');

-- PT_GJ - Gujarat
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('PT_GJ',
'// Professional Tax - Gujarat
income = MONTHLY_GROSS_SALARY;
pt = 0;
if (income < 6000) { pt = 0; }
if (income >= 6000 && income < 9000) { pt = 80; }
if (income >= 9000 && income < 12000) { pt = 150; }
if (income >= 12000) { pt = 200; }
{ "PT": pt }',
'2020-01-01', NULL,
'MONTHLY_GROSS_SALARY');

-- PT_AP - Andhra Pradesh
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('PT_AP',
'// Professional Tax - Andhra Pradesh
income = MONTHLY_GROSS_SALARY;
pt = 0;
if (income <= 15000) { pt = 0; }
if (income > 15000 && income <= 20000) { pt = 150; }
if (income > 20000) { pt = 200; }
{ "PT": pt }',
'2020-01-01', NULL,
'MONTHLY_GROSS_SALARY');

-- PT_TS - Telangana
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('PT_TS',
'// Professional Tax - Telangana
income = MONTHLY_GROSS_SALARY;
pt = 0;
if (income <= 15000) { pt = 0; }
if (income > 15000 && income <= 20000) { pt = 150; }
if (income > 20000) { pt = 200; }
{ "PT": pt }',
'2020-01-01', NULL,
'MONTHLY_GROSS_SALARY');

-- PT_KL - Kerala
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('PT_KL',
'// Professional Tax - Kerala
income = MONTHLY_GROSS_SALARY;
pt = 0;
if (income < 2000) { pt = 0; }
if (income >= 2000 && income < 3000) { pt = 100; }
if (income >= 3000 && income < 5000) { pt = 120; }
if (income >= 5000 && income < 7500) { pt = 140; }
if (income >= 7500 && income < 10000) { pt = 160; }
if (income >= 10000 && income < 12500) { pt = 180; }
if (income >= 12500) { pt = 200; }
{ "PT": pt }',
'2020-01-01', NULL,
'MONTHLY_GROSS_SALARY');

-- PT_MP - Madhya Pradesh
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('PT_MP',
'// Professional Tax - Madhya Pradesh
income = MONTHLY_GROSS_SALARY;
pt = 0;
if (income <= 15000) { pt = 0; }
if (income > 15000 && income <= 20000) { pt = 150; }
if (income > 20000) { pt = 208; }
{ "PT": pt }',
'2020-01-01', NULL,
'MONTHLY_GROSS_SALARY');

-- PT_OD - Odisha
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('PT_OD',
'// Professional Tax - Odisha
income = MONTHLY_GROSS_SALARY;
pt = 0;
if (income <= 15000) { pt = 0; }
if (income > 15000 && income <= 20000) { pt = 100; }
if (income > 20000) { pt = 150; }
{ "PT": pt }',
'2020-01-01', NULL,
'MONTHLY_GROSS_SALARY');

-- PT_JH - Jharkhand
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('PT_JH',
'// Professional Tax - Jharkhand
income = MONTHLY_GROSS_SALARY;
pt = 0;
if (income <= 25000) { pt = 0; }
if (income > 25000 && income <= 33333) { pt = 150; }
if (income > 33333 && income <= 50000) { pt = 175; }
if (income > 50000) { pt = 208; }
{ "PT": pt }',
'2020-01-01', NULL,
'MONTHLY_GROSS_SALARY');

-- PT_AS - Assam
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('PT_AS',
'// Professional Tax - Assam
income = MONTHLY_GROSS_SALARY;
pt = 0;
if (income <= 10000) { pt = 0; }
if (income > 10000 && income <= 15000) { pt = 150; }
if (income > 15000 && income <= 25000) { pt = 180; }
if (income > 25000) { pt = 208; }
{ "PT": pt }',
'2020-01-01', NULL,
'MONTHLY_GROSS_SALARY');

-- PT_ML - Meghalaya
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('PT_ML',
'// Professional Tax - Meghalaya (annual / 12)
income = MONTHLY_GROSS_SALARY;
pt = 0;
if (income <= 10416) { pt = 0; }
if (income > 10416 && income <= 20833) { pt = 1250 / 12; }
if (income > 20833) { pt = 2500 / 12; }
{ "PT": pt }',
'2020-01-01', NULL,
'MONTHLY_GROSS_SALARY');

-- PT_MN - Manipur
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('PT_MN',
'// Professional Tax - Manipur
income = MONTHLY_GROSS_SALARY;
pt = 0;
if (income <= 4166) { pt = 0; }
if (income > 4166 && income <= 8333) { pt = 30; }
if (income > 8333 && income <= 12500) { pt = 75; }
if (income > 12500 && income <= 16666) { pt = 150; }
if (income > 16666) { pt = 208; }
{ "PT": pt }',
'2020-01-01', NULL,
'MONTHLY_GROSS_SALARY');

-- PT_TR - Tripura
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('PT_TR',
'// Professional Tax - Tripura
income = MONTHLY_GROSS_SALARY;
pt = 0;
if (income <= 5000) { pt = 0; }
if (income > 5000 && income <= 8000) { pt = 60; }
if (income > 8000 && income <= 12000) { pt = 90; }
if (income > 12000 && income <= 16000) { pt = 120; }
if (income > 16000 && income <= 20000) { pt = 150; }
if (income > 20000) { pt = 200; }
{ "PT": pt }',
'2020-01-01', NULL,
'MONTHLY_GROSS_SALARY');

-- PT_MZ - Mizoram
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('PT_MZ',
'// Professional Tax - Mizoram (annual / 12)
income = MONTHLY_GROSS_SALARY;
pt = 0;
if (income <= 5000) { pt = 0; }
if (income > 5000 && income <= 10000) { pt = 300 / 12; }
if (income > 10000) { pt = 600 / 12; }
{ "PT": pt }',
'2020-01-01', NULL,
'MONTHLY_GROSS_SALARY');

-- PT_SK - Sikkim
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('PT_SK',
'// Professional Tax - Sikkim
income = MONTHLY_GROSS_SALARY;
pt = 0;
if (income <= 10000) { pt = 0; }
if (income > 10000 && income <= 15000) { pt = 100; }
if (income > 15000 && income <= 20000) { pt = 150; }
if (income > 20000) { pt = 200; }
{ "PT": pt }',
'2020-01-01', NULL,
'MONTHLY_GROSS_SALARY');

-- PT_BR - Bihar (annual PT / 12)
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('PT_BR',
'// Professional Tax - Bihar (annual / 12)
income = MONTHLY_GROSS_SALARY;
pt = 0;
if (income * 12 <= 300000) { pt = 0; }
if (income * 12 > 300000 && income * 12 <= 500000) { pt = 1000 / 12; }
if (income * 12 > 500000 && income * 12 <= 1000000) { pt = 2000 / 12; }
if (income * 12 > 1000000) { pt = 2500 / 12; }
{ "PT": pt }',
'2020-01-01', NULL,
'MONTHLY_GROSS_SALARY');

-- PT_CG - Chhattisgarh
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('PT_CG',
'// Professional Tax - Chhattisgarh
income = MONTHLY_GROSS_SALARY;
pt = 0;
if (income <= 12500) { pt = 0; }
if (income > 12500 && income <= 16666) { pt = 150; }
if (income > 16666) { pt = 208; }
{ "PT": pt }',
'2020-01-01', NULL,
'MONTHLY_GROSS_SALARY');

-- PT_PY - Puducherry
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('PT_PY',
'// Professional Tax - Puducherry
income = MONTHLY_GROSS_SALARY;
pt = 0;
if (income <= 3000) { pt = 0; }
if (income > 3000 && income <= 5000) { pt = 20; }
if (income > 5000) { pt = 50; }
{ "PT": pt }',
'2020-01-01', NULL,
'MONTHLY_GROSS_SALARY');

-- PT_GA - Goa
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('PT_GA',
'// Professional Tax - Goa
income = MONTHLY_GROSS_SALARY;
pt = 0;
if (income <= 7000) { pt = 0; }
if (income > 7000 && income <= 10000) { pt = 150; }
if (income > 10000) { pt = 200; }
{ "PT": pt }',
'2020-01-01', NULL,
'MONTHLY_GROSS_SALARY');


-- =====================================================
-- 6. LWF - STATE WISE
-- =====================================================

-- LWF_MH - Maharashtra (Half-yearly: June, December)
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('LWF_MH',
'// LWF - Maharashtra (Half-yearly: June, December)
month = MONTH;
empLWF = 0;
emprLWF = 0;
if (month == 6 || month == 12) { empLWF = 12; emprLWF = 36; }
{ "LWF_EMPLOYEE": empLWF, "LWF_EMPLOYER": emprLWF }',
'2020-01-01', NULL,
'MONTH');

-- LWF_GJ - Gujarat (Monthly)
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('LWF_GJ',
'// LWF - Gujarat (Monthly)
empLWF = 10;
emprLWF = 20;
{ "LWF_EMPLOYEE": empLWF, "LWF_EMPLOYER": emprLWF }',
'2020-01-01', NULL,
'');

-- LWF_KA - Karnataka (Half-yearly: June, December)
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('LWF_KA',
'// LWF - Karnataka (Half-yearly: June, December)
month = MONTH;
empLWF = 0;
emprLWF = 0;
if (month == 6 || month == 12) { empLWF = 20; emprLWF = 40; }
{ "LWF_EMPLOYEE": empLWF, "LWF_EMPLOYER": emprLWF }',
'2020-01-01', NULL,
'MONTH');

-- LWF_TN - Tamil Nadu (Half-yearly: Jan, Jul)
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('LWF_TN',
'// LWF - Tamil Nadu (Half-yearly: January, July)
month = MONTH;
empLWF = 0;
emprLWF = 0;
if (month == 1 || month == 7) { empLWF = 20; emprLWF = 40; }
{ "LWF_EMPLOYEE": empLWF, "LWF_EMPLOYER": emprLWF }',
'2020-01-01', NULL,
'MONTH');

-- LWF_AP - Andhra Pradesh (Monthly)
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('LWF_AP',
'// LWF - Andhra Pradesh (Monthly)
empLWF = 20;
emprLWF = 30;
{ "LWF_EMPLOYEE": empLWF, "LWF_EMPLOYER": emprLWF }',
'2020-01-01', NULL,
'');

-- LWF_TS - Telangana (Monthly)
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('LWF_TS',
'// LWF - Telangana (Monthly)
empLWF = 20;
emprLWF = 30;
{ "LWF_EMPLOYEE": empLWF, "LWF_EMPLOYER": emprLWF }',
'2020-01-01', NULL,
'');

-- LWF_KL - Kerala (Monthly)
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('LWF_KL',
'// LWF - Kerala (Monthly)
empLWF = 4;
emprLWF = 8;
{ "LWF_EMPLOYEE": empLWF, "LWF_EMPLOYER": emprLWF }',
'2020-01-01', NULL,
'');

-- LWF_MP - Madhya Pradesh (Half-yearly: June, December)
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('LWF_MP',
'// LWF - Madhya Pradesh (Half-yearly: June, December)
month = MONTH;
empLWF = 0;
emprLWF = 0;
if (month == 6 || month == 12) { empLWF = 10; emprLWF = 20; }
{ "LWF_EMPLOYEE": empLWF, "LWF_EMPLOYER": emprLWF }',
'2020-01-01', NULL,
'MONTH');

-- LWF_PB - Punjab (Monthly)
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('LWF_PB',
'// LWF - Punjab (Monthly)
empLWF = 1;
emprLWF = 2;
{ "LWF_EMPLOYEE": empLWF, "LWF_EMPLOYER": emprLWF }',
'2020-01-01', NULL,
'');

-- LWF_HR - Haryana (Monthly)
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('LWF_HR',
'// LWF - Haryana (Monthly)
empLWF = 0.2;
emprLWF = 0.4;
{ "LWF_EMPLOYEE": empLWF, "LWF_EMPLOYER": emprLWF }',
'2020-01-01', NULL,
'');

-- LWF_RJ - Rajasthan (Half-yearly: June, December)
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('LWF_RJ',
'// LWF - Rajasthan (Half-yearly: June, December)
month = MONTH;
empLWF = 0;
emprLWF = 0;
if (month == 6 || month == 12) { empLWF = 20; emprLWF = 40; }
{ "LWF_EMPLOYEE": empLWF, "LWF_EMPLOYER": emprLWF }',
'2020-01-01', NULL,
'MONTH');

-- LWF_GA - Goa (Half-yearly: June, December)
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('LWF_GA',
'// LWF - Goa (Half-yearly: June, December)
month = MONTH;
empLWF = 0;
emprLWF = 0;
if (month == 6 || month == 12) { empLWF = 20; emprLWF = 40; }
{ "LWF_EMPLOYEE": empLWF, "LWF_EMPLOYER": emprLWF }',
'2020-01-01', NULL,
'MONTH');

-- LWF_CH - Chandigarh (Monthly)
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('LWF_CH',
'// LWF - Chandigarh (Monthly)
empLWF = 0.2;
emprLWF = 0.4;
{ "LWF_EMPLOYEE": empLWF, "LWF_EMPLOYER": emprLWF }',
'2020-01-01', NULL,
'');

-- LWF_BR - Bihar (Annual: January)
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('LWF_BR',
'// LWF - Bihar (Annual: January)
month = MONTH;
empLWF = 0;
emprLWF = 0;
if (month == 1) { empLWF = 7; emprLWF = 14; }
{ "LWF_EMPLOYEE": empLWF, "LWF_EMPLOYER": emprLWF }',
'2020-01-01', NULL,
'MONTH');

-- LWF_AS - Assam (Half-yearly: June, December)
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('LWF_AS',
'// LWF - Assam (Half-yearly: June, December)
month = MONTH;
empLWF = 0;
emprLWF = 0;
if (month == 6 || month == 12) { empLWF = 10; emprLWF = 20; }
{ "LWF_EMPLOYEE": empLWF, "LWF_EMPLOYER": emprLWF }',
'2020-01-01', NULL,
'MONTH');

-- LWF_PY - Puducherry (Annual: January)
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('LWF_PY',
'// LWF - Puducherry (Annual: January)
month = MONTH;
empLWF = 0;
emprLWF = 0;
if (month == 1) { empLWF = 7; emprLWF = 14; }
{ "LWF_EMPLOYEE": empLWF, "LWF_EMPLOYER": emprLWF }',
'2020-01-01', NULL,
'MONTH');


-- =====================================================
-- 7. VERSION HISTORY EXAMPLES (effective_from/to ranges)
--    PF with different ceiling before 2021
-- =====================================================
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('PF',
'// PF Calculation (Old - wage ceiling 12000)
pf_base = is_company_pf_wage_ceiling_applicable == true ? (pf_applicable_monthly_earnings < 12000 ? pf_applicable_monthly_earnings : 12000) : pf_applicable_monthly_earnings;
pf_rate = 0.12;
employee_pf = pf_base * pf_rate;
employer_pf = pf_base * pf_rate;
{ "EMPLOYEE_PF": employee_pf, "EMPLOYER_PF": employer_pf }',
'2015-01-01', '2019-12-31',
'pf_applicable_monthly_earnings,company_pf_wage_ceiling,is_company_pf_wage_ceiling_applicable');

-- PT_MH with different slabs before 2023
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('PT_MH',
'// Professional Tax - Maharashtra (OLD slabs before 2023)
income = MONTHLY_GROSS_SALARY;
gender = GENDER != null ? GENDER : "MALE";
month = MONTH;
pt = 0;
if (gender == "FEMALE") {
  if (income <= 10000) { pt = 0; }
  if (income > 10000) { pt = 200; }
} else {
  if (income <= 7500) { pt = 0; }
  if (income > 7500 && income <= 10000) { pt = 175; }
  if (income > 10000) { pt = 200; }
}
if (gender == "MALE" && month == 2 && pt > 0) { pt = pt + 100; }
{ "PT": pt }',
'2015-01-01', '2022-12-31',
'MONTHLY_GROSS_SALARY,GENDER,MONTH');


-- =====================================================
-- 8. SIMPLE EXPRESSION EXAMPLE (non-script, pure expression)
-- =====================================================
INSERT INTO payroll_expressions (expression_id, expression, effective_from, effective_to, context_variables) VALUES
('BONUS_PCT',
'MONTHLY_GROSS_SALARY * BONUS_PERCENTAGE / 100',
'2020-01-01', NULL,
'MONTHLY_GROSS_SALARY,BONUS_PERCENTAGE');
