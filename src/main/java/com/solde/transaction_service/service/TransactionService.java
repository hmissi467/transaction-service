package com.solde.transaction_service.service;

import com.solde.transaction_service.client.AccountClient;
import com.solde.transaction_service.dto.AmountRequest;
import com.solde.transaction_service.model.Transaction;
import com.solde.transaction_service.Repositery.TransactionRepository   ;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountClient accountClient) {
        this.transactionRepository = transactionRepository;
        this.accountClient = accountClient;
    }

    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    public List<Transaction> findByAccount(Long accountId) {
        return transactionRepository
                .findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(
                        accountId, accountId);
    }

    public Transaction deposit(Long targetAccountId, BigDecimal amount,
                               String description) {
        // 1) Appelle account-service pour crediter le compte
        accountClient.credit(targetAccountId, new AmountRequest(amount));

        // 2) Si ca reussit, enregistre l'historique
        Transaction transaction = new Transaction();
        transaction.setType("VERSEMENT");
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setTargetAccountId(targetAccountId);
        return transactionRepository.save(transaction);
    }

    public Transaction withdraw(Long sourceAccountId, BigDecimal amount,
                                String description) {
        // 1) Appelle account-service pour debiter (echoue si solde insuffisant)
        accountClient.debit(sourceAccountId, new AmountRequest(amount));

        // 2) Si ca reussit, enregistre l'historique
        Transaction transaction = new Transaction();
        transaction.setType("RETRAIT");
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setSourceAccountId(sourceAccountId);
        return transactionRepository.save(transaction);
    }

    public Transaction transfer(Long sourceAccountId, Long targetAccountId,
                                BigDecimal amount, String description) {
        // 1) Debite le compte source (echoue si solde insuffisant)
        accountClient.debit(sourceAccountId, new AmountRequest(amount));

        // 2) Credite le compte destinataire
        accountClient.credit(targetAccountId, new AmountRequest(amount));

        // 3) Enregistre l'historique seulement si les deux ont reussi
        Transaction transaction = new Transaction();
        transaction.setType("VIREMENT");
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setSourceAccountId(sourceAccountId);
        transaction.setTargetAccountId(targetAccountId);
        return transactionRepository.save(transaction);
    }
}
