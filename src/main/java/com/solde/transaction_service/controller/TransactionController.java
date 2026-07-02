package com.solde.transaction_service.controller;


import com.solde.transaction_service.model.Transaction;
import com.solde.transaction_service.service.TransactionService;
import org.springframework.web.bind.annotation.*;

        import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public List<Transaction> getAll() {
        return transactionService.findAll();
    }

    @GetMapping("/account/{accountId}")
    public List<Transaction> getByAccount(@PathVariable Long accountId) {
        return transactionService.findByAccount(accountId);
    }

    @PostMapping("/versement")
    public Transaction deposit(@RequestBody Map<String, Object> body) {
        Long accountId = Long.valueOf(body.get("accountId").toString());
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String description = (String) body.getOrDefault("description", "");
        return transactionService.deposit(accountId, amount, description);
    }

    @PostMapping("/retrait")
    public Transaction withdraw(@RequestBody Map<String, Object> body) {
        Long accountId = Long.valueOf(body.get("accountId").toString());
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String description = (String) body.getOrDefault("description", "");
        return transactionService.withdraw(accountId, amount, description);
    }

    @PostMapping("/virement")
    public Transaction transfer(@RequestBody Map<String, Object> body) {
        Long sourceId = Long.valueOf(body.get("sourceAccountId").toString());
        Long targetId = Long.valueOf(body.get("targetAccountId").toString());
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String description = (String) body.getOrDefault("description", "");
        return transactionService.transfer(sourceId, targetId, amount, description);
    }
}
