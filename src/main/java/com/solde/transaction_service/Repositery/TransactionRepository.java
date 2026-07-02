package com.solde.transaction_service.Repositery;

import com.solde.transaction_service.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Spring Data JPA comprend le nom de la methode et genere
    // automatiquement : SELECT * FROM transaction
    //                    WHERE source_account_id = ?
    //                    OR target_account_id = ?
    //                    ORDER BY created_at DESC
    List<Transaction> findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(
            Long sourceAccountId, Long targetAccountId);
}
