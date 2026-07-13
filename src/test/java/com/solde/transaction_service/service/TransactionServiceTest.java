package com.solde.transaction_service.service;

import com.solde.transaction_service.client.AccountClient;
import com.solde.transaction_service.dto.AmountRequest;
import com.solde.transaction_service.model.Transaction;
import com.solde.transaction_service.Repositery.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountClient accountClient;

    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
    }

    // ── findAll ────────────────────────────────────────────────

    @Test
    void findAll_shouldReturnAllTransactions() {
        Transaction t1 = new Transaction();
        t1.setId(1L);
        Transaction t2 = new Transaction();
        t2.setId(2L);

        when(transactionRepository.findAll()).thenReturn(List.of(t1, t2));

        List<Transaction> result = transactionService.findAll();

        assertThat(result).hasSize(2);
        verify(transactionRepository, times(1)).findAll();
    }

    @Test
    void findAll_shouldReturnEmptyList() {
        when(transactionRepository.findAll()).thenReturn(List.of());

        List<Transaction> result = transactionService.findAll();

        assertThat(result).isEmpty();
    }

    // ── findByAccount ──────────────────────────────────────────

    @Test
    void findByAccount_shouldCallRepositoryWithSameIdForBothParams() {
        Long accountId = 5L;
        Transaction t = new Transaction();
        t.setId(1L);
        t.setSourceAccountId(accountId);

        when(transactionRepository
                .findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(accountId, accountId))
                .thenReturn(List.of(t));

        List<Transaction> result = transactionService.findByAccount(accountId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSourceAccountId()).isEqualTo(accountId);
        verify(transactionRepository)
                .findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(accountId, accountId);
    }

    @Test
    void findByAccount_shouldReturnEmptyWhenNoTransactions() {
        Long accountId = 999L;
        when(transactionRepository
                .findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(accountId, accountId))
                .thenReturn(List.of());

        List<Transaction> result = transactionService.findByAccount(accountId);

        assertThat(result).isEmpty();
    }

    // ── deposit ────────────────────────────────────────────────

    @Test
    void deposit_shouldCreditAccountAndSaveTransaction() {
        Long accountId = 10L;
        BigDecimal amount = new BigDecimal("500.00");
        String description = "Cash deposit";

        doNothing().when(accountClient).credit(eq(accountId), any(AmountRequest.class));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        Transaction result = transactionService.deposit(accountId, amount, description);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo("VERSEMENT");
        assertThat(result.getAmount().compareTo(amount)).isEqualTo(0);
        assertThat(result.getTargetAccountId()).isEqualTo(accountId);
        assertThat(result.getSourceAccountId()).isNull();
        assertThat(result.getDescription()).isEqualTo(description);

        verify(accountClient).credit(eq(accountId), any(AmountRequest.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void deposit_shouldPassCorrectAmountRequest() {
        Long accountId = 20L;
        BigDecimal amount = new BigDecimal("250.50");

        ArgumentCaptor<AmountRequest> captor = ArgumentCaptor.forClass(AmountRequest.class);

        doNothing().when(accountClient).credit(eq(accountId), captor.capture());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(2L);
            return t;
        });

        transactionService.deposit(accountId, amount, "desc");

        AmountRequest captured = captor.getValue();
        assertThat(captured.getAmount().compareTo(amount)).isEqualTo(0);
    }

    @Test
    void deposit_shouldPropagateExceptionWhenCreditFails() {
        Long accountId = 10L;
        BigDecimal amount = new BigDecimal("500.00");

        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountClient).credit(eq(accountId), any(AmountRequest.class));

        try {
            transactionService.deposit(accountId, amount, "desc");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Insufficient funds");
        }

        verify(transactionRepository, never()).save(any());
    }

    // ── withdraw ───────────────────────────────────────────────

    @Test
    void withdraw_shouldDebitAccountAndSaveTransaction() {
        Long accountId = 10L;
        BigDecimal amount = new BigDecimal("200.00");
        String description = "ATM withdrawal";

        doNothing().when(accountClient).debit(eq(accountId), any(AmountRequest.class));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(3L);
            return t;
        });

        Transaction result = transactionService.withdraw(accountId, amount, description);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getType()).isEqualTo("RETRAIT");
        assertThat(result.getAmount().compareTo(amount)).isEqualTo(0);
        assertThat(result.getSourceAccountId()).isEqualTo(accountId);
        assertThat(result.getTargetAccountId()).isNull();
        assertThat(result.getDescription()).isEqualTo(description);

        verify(accountClient).debit(eq(accountId), any(AmountRequest.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void withdraw_shouldPropagateExceptionWhenDebitFails() {
        Long accountId = 10L;
        BigDecimal amount = new BigDecimal("10000.00");

        doThrow(new RuntimeException("Insufficient balance"))
                .when(accountClient).debit(eq(accountId), any(AmountRequest.class));

        try {
            transactionService.withdraw(accountId, amount, "desc");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Insufficient balance");
        }

        verify(transactionRepository, never()).save(any());
    }

    // ── transfer ───────────────────────────────────────────────

    @Test
    void transfer_shouldDebitSourceCreditTargetAndSaveTransaction() {
        Long sourceId = 10L;
        Long targetId = 20L;
        BigDecimal amount = new BigDecimal("300.00");
        String description = "Rent payment";

        doNothing().when(accountClient).debit(eq(sourceId), any(AmountRequest.class));
        doNothing().when(accountClient).credit(eq(targetId), any(AmountRequest.class));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(4L);
            return t;
        });

        Transaction result = transactionService.transfer(sourceId, targetId, amount, description);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(4L);
        assertThat(result.getType()).isEqualTo("VIREMENT");
        assertThat(result.getAmount().compareTo(amount)).isEqualTo(0);
        assertThat(result.getSourceAccountId()).isEqualTo(sourceId);
        assertThat(result.getTargetAccountId()).isEqualTo(targetId);
        assertThat(result.getDescription()).isEqualTo(description);

        verify(accountClient).debit(eq(sourceId), any(AmountRequest.class));
        verify(accountClient).credit(eq(targetId), any(AmountRequest.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void transfer_shouldCallDebitBeforeCredit() {
        Long sourceId = 10L;
        Long targetId = 20L;
        BigDecimal amount = new BigDecimal("100.00");

        doNothing().when(accountClient).debit(eq(sourceId), any(AmountRequest.class));
        doNothing().when(accountClient).credit(eq(targetId), any(AmountRequest.class));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.transfer(sourceId, targetId, amount, "test");

        var inOrder = inOrder(accountClient);
        inOrder.verify(accountClient).debit(eq(sourceId), any(AmountRequest.class));
        inOrder.verify(accountClient).credit(eq(targetId), any(AmountRequest.class));
    }

    @Test
    void transfer_shouldNotSaveWhenDebitFails() {
        Long sourceId = 10L;
        Long targetId = 20L;
        BigDecimal amount = new BigDecimal("500.00");

        doThrow(new RuntimeException("No funds"))
                .when(accountClient).debit(eq(sourceId), any(AmountRequest.class));

        try {
            transactionService.transfer(sourceId, targetId, amount, "test");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("No funds");
        }

        verify(accountClient, never()).credit(any(), any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_shouldNotSaveWhenCreditFailsAfterDebit() {
        Long sourceId = 10L;
        Long targetId = 20L;
        BigDecimal amount = new BigDecimal("500.00");

        doNothing().when(accountClient).debit(eq(sourceId), any(AmountRequest.class));
        doThrow(new RuntimeException("Credit failed"))
                .when(accountClient).credit(eq(targetId), any(AmountRequest.class));

        try {
            transactionService.transfer(sourceId, targetId, amount, "test");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Credit failed");
        }

        verify(accountClient).debit(eq(sourceId), any(AmountRequest.class));
        verify(accountClient).credit(eq(targetId), any(AmountRequest.class));
        verify(transactionRepository, never()).save(any());
    }
}
