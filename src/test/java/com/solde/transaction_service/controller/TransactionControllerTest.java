package com.solde.transaction_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solde.transaction_service.model.Transaction;
import com.solde.transaction_service.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.hamcrest.Matchers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private ObjectMapper objectMapper;

    // ── GET /api/transactions ──────────────────────────────────

    @Test
    void getAll_shouldReturnAllTransactions() throws Exception {
        Transaction t1 = buildTransaction(1L, "VERSEMENT", new BigDecimal("100.00"), null, 10L);
        Transaction t2 = buildTransaction(2L, "RETRAIT", new BigDecimal("50.00"), 10L, null);

        when(transactionService.findAll()).thenReturn(List.of(t1, t2));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].type").value("VERSEMENT"))
                .andExpect(jsonPath("$[0].amount").value(100))
                .andExpect(jsonPath("$[1].type").value("RETRAIT"))
                .andExpect(jsonPath("$[1].amount").value(50));

        verify(transactionService).findAll();
    }

    @Test
    void getAll_shouldReturnEmptyList() throws Exception {
        when(transactionService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(0)));
    }

    // ── GET /api/transactions/account/{accountId} ──────────────

    @Test
    void getByAccount_shouldReturnTransactionsForAccount() throws Exception {
        Long accountId = 10L;
        Transaction t = buildTransaction(1L, "VERSEMENT", new BigDecimal("200.00"), null, accountId);

        when(transactionService.findByAccount(accountId)).thenReturn(List.of(t));

        mockMvc.perform(get("/api/transactions/account/{accountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].targetAccountId").value(accountId));

        verify(transactionService).findByAccount(accountId);
    }

    @Test
    void getByAccount_shouldReturnEmptyWhenNoTransactions() throws Exception {
        when(transactionService.findByAccount(anyLong())).thenReturn(List.of());

        mockMvc.perform(get("/api/transactions/account/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(0)));
    }

    // ── POST /api/transactions/versement ───────────────────────

    @Test
    void deposit_shouldCreateTransaction() throws Exception {
        Transaction saved = buildTransaction(1L, "VERSEMENT", new BigDecimal("500.00"), null, 10L);
        saved.setDescription("Cash deposit");

        when(transactionService.deposit(eq(10L), any(BigDecimal.class), eq("Cash deposit")))
                .thenReturn(saved);

        mockMvc.perform(post("/api/transactions/versement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "accountId": 10,
                                    "amount": 500.00,
                                    "description": "Cash deposit"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("VERSEMENT"))
                .andExpect(jsonPath("$.amount").value(500))
                .andExpect(jsonPath("$.targetAccountId").value(10))
                .andExpect(jsonPath("$.description").value("Cash deposit"));

        verify(transactionService).deposit(eq(10L), any(BigDecimal.class), eq("Cash deposit"));
    }

    @Test
    void deposit_shouldDefaultDescriptionToEmpty() throws Exception {
        Transaction saved = buildTransaction(1L, "VERSEMENT", new BigDecimal("100.00"), null, 10L);

        when(transactionService.deposit(eq(10L), any(BigDecimal.class), eq("")))
                .thenReturn(saved);

        mockMvc.perform(post("/api/transactions/versement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "accountId": 10,
                                    "amount": 100.00
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("VERSEMENT"));

        verify(transactionService).deposit(eq(10L), any(BigDecimal.class), eq(""));
    }

    // ── POST /api/transactions/retrait ─────────────────────────

    @Test
    void withdraw_shouldCreateTransaction() throws Exception {
        Transaction saved = buildTransaction(1L, "RETRAIT", new BigDecimal("200.00"), 10L, null);
        saved.setDescription("ATM");

        when(transactionService.withdraw(eq(10L), any(BigDecimal.class), eq("ATM")))
                .thenReturn(saved);

        mockMvc.perform(post("/api/transactions/retrait")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "accountId": 10,
                                    "amount": 200.00,
                                    "description": "ATM"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("RETRAIT"))
                .andExpect(jsonPath("$.amount").value(200))
                .andExpect(jsonPath("$.sourceAccountId").value(10))
                .andExpect(jsonPath("$.description").value("ATM"));

        verify(transactionService).withdraw(eq(10L), any(BigDecimal.class), eq("ATM"));
    }

    @Test
    void withdraw_shouldDefaultDescriptionToEmpty() throws Exception {
        Transaction saved = buildTransaction(1L, "RETRAIT", new BigDecimal("50.00"), 5L, null);

        when(transactionService.withdraw(eq(5L), any(BigDecimal.class), eq("")))
                .thenReturn(saved);

        mockMvc.perform(post("/api/transactions/retrait")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "accountId": 5,
                                    "amount": 50.00
                                }
                                """))
                .andExpect(status().isOk());

        verify(transactionService).withdraw(eq(5L), any(BigDecimal.class), eq(""));
    }

    // ── POST /api/transactions/virement ────────────────────────

    @Test
    void transfer_shouldCreateTransaction() throws Exception {
        Transaction saved = buildTransaction(1L, "VIREMENT", new BigDecimal("300.00"), 10L, 20L);
        saved.setDescription("Rent");

        when(transactionService.transfer(eq(10L), eq(20L), any(BigDecimal.class), eq("Rent")))
                .thenReturn(saved);

        mockMvc.perform(post("/api/transactions/virement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "sourceAccountId": 10,
                                    "targetAccountId": 20,
                                    "amount": 300.00,
                                    "description": "Rent"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("VIREMENT"))
                .andExpect(jsonPath("$.amount").value(300))
                .andExpect(jsonPath("$.sourceAccountId").value(10))
                .andExpect(jsonPath("$.targetAccountId").value(20))
                .andExpect(jsonPath("$.description").value("Rent"));

        verify(transactionService).transfer(eq(10L), eq(20L), any(BigDecimal.class), eq("Rent"));
    }

    @Test
    void transfer_shouldDefaultDescriptionToEmpty() throws Exception {
        Transaction saved = buildTransaction(1L, "VIREMENT", new BigDecimal("100.00"), 1L, 2L);

        when(transactionService.transfer(eq(1L), eq(2L), any(BigDecimal.class), eq("")))
                .thenReturn(saved);

        mockMvc.perform(post("/api/transactions/virement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "sourceAccountId": 1,
                                    "targetAccountId": 2,
                                    "amount": 100.00
                                }
                                """))
                .andExpect(status().isOk());

        verify(transactionService).transfer(eq(1L), eq(2L), any(BigDecimal.class), eq(""));
    }

    // ── helper ─────────────────────────────────────────────────

    private Transaction buildTransaction(Long id, String type, BigDecimal amount,
                                         Long sourceAccountId, Long targetAccountId) {
        Transaction t = new Transaction();
        t.setId(id);
        t.setType(type);
        t.setAmount(amount);
        t.setSourceAccountId(sourceAccountId);
        t.setTargetAccountId(targetAccountId);
        t.setCreatedAt(LocalDateTime.now());
        return t;
    }
}
