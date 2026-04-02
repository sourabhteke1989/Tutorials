package com.example.jexl.repository;

import com.example.jexl.entity.PayrollExpression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayrollExpressionRepository extends JpaRepository<PayrollExpression, Integer> {

    List<PayrollExpression> findByExpressionId(String expressionId);
}
