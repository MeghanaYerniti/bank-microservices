package com.example.transactions_service.controller;

import com.example.transactions_service.dto.TransactionsDto;
import com.example.transactions_service.service.TransactionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionsController {

    private final TransactionsService transactionsService;
//    private final TransactionsMapper transactionsMapper;

    @GetMapping("/all")
    public List<TransactionsDto> getAllTransactions() {
        return transactionsService.getAllTransactions();
    }

    @GetMapping("/id/{id}")
    public TransactionsDto getTransaction(@PathVariable UUID id) {
        return transactionsService.getTransaction(id);
    }

    @PutMapping("/transfer")
    public TransactionsDto transfer(@RequestParam Long fromAccountId, @RequestParam Long toAccountId, @RequestParam double amount) {
        return transactionsService.transfer(fromAccountId, toAccountId, amount);
    }

    @GetMapping("/{id}/transactions")
    public List<TransactionsDto> getAccountTransactions(@PathVariable Long id) {
        return transactionsService.getTransactionsByAccountId(id);
    }

    // for BankAccount Deposit and withdraw

    @PostMapping("/deposit")
    public ResponseEntity<TransactionsDto> deposit(@RequestBody TransactionsDto dto) {
        TransactionsDto result = transactionsService.createDeposit(dto);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionsDto> withdraw(@RequestBody TransactionsDto dto) {
        TransactionsDto result = transactionsService.createWithdraw(dto);
        return ResponseEntity.ok(result);
    }

}
