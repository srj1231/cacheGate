package com.saumya.cachegate.llmProvider.budget;

import com.saumya.cachegate.llmProvider.budget.exceptions.BudgetExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RequestBudget {

    private final int maxRequests;
    private final AtomicInteger used = new AtomicInteger(0);

    public RequestBudget(@Value("${cachegate.budget.max-requests:50}") int maxRequests) {
        this.maxRequests = maxRequests;
    }

    public void consume() {
        int current = used.incrementAndGet();
        if(current > maxRequests){
            throw new BudgetExceededException("Session budget exceeded: " + maxRequests +
                    " provider calls already used this session. Restart Claude Desktop to reset, or raise cachegate.budget.max-requests.");
        }
    }

    public int used() {
        return used.get();
    }

    public int max() {
        return maxRequests;
    }
}
