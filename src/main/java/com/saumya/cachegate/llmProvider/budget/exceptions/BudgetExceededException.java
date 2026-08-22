package com.saumya.cachegate.llmProvider.budget.exceptions;

public class BudgetExceededException extends RuntimeException {
    public BudgetExceededException(String message) {
        super(message);
    }
}
