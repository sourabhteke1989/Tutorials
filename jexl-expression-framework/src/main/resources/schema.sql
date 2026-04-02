CREATE TABLE payroll_expressions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    expression_id VARCHAR(50) NOT NULL,
    expression CLOB NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    context_variables VARCHAR(1000)
);
