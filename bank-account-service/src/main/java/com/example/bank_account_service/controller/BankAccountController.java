package com.example.bank_account_service.controller;

import com.example.bank_account_service.dto.BankAccountDto;
import com.example.bank_account_service.service.BankAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @PostMapping("/")
    public BankAccountDto createAccount(@Valid @RequestBody BankAccountDto bankAccountDto) {
        return bankAccountService.createAccount(bankAccountDto);
    }

    @GetMapping("/id/{id}")
    public BankAccountDto getAccount(@PathVariable Long id) {
        return bankAccountService.getAccount(id);
    }

    @GetMapping("/{id}/balance")
    public double getBalance(@PathVariable Long id) {
        return bankAccountService.getAccountBalance(id);
    }

    @PutMapping("/{id}/deposit")
    public String deposit(@PathVariable Long id, @RequestParam double amount) {
        BankAccountDto updatedAccount = bankAccountService.depositAmount(id, amount);
        return "Deposited " + amount + ". New Balance: " + updatedAccount.getBalance();
    }

    @PutMapping("/{id}/withdraw")
    public String withdraw(@PathVariable Long id, @RequestParam double amount) {
        BankAccountDto updatedAccount = bankAccountService.withdrawAmount(id, amount);
        return "Withdraw " + amount + " New Balance: " + updatedAccount.getBalance();
    }

    @DeleteMapping("/{id}")
    public String deleteAccount(@PathVariable Long id) {
        bankAccountService.deleteAccount(id);
        return "Deleted account " + id;
    }

    @GetMapping("/all-accounts/{page}/{pageSize}")
    public Page<BankAccountDto> getAllAccounts(@PathVariable int page, @PathVariable int pageSize) {
        return bankAccountService.getAllAccounts(page, pageSize);
    }

    @GetMapping("/report/top-balances")
    public List<BankAccountDto> getTopBalances() {
        return bankAccountService.getTopBalances(5);
    }

    // customer service
    @GetMapping("/by-customer/{customerId}")
    public List<BankAccountDto> getAccountsByCustomerId(@PathVariable Long customerId) {
        return bankAccountService.getAccountsByCustomerId(customerId);
    }

    @PutMapping("/update-name/{customerId}")
    public void updateAccountHolderName(@PathVariable Long customerId, @RequestParam String newName) {
        bankAccountService.updateAccountHolderName(customerId, newName);
    }

    // transaction service
    @PutMapping("/update-balance/{accountId}")
    public ResponseEntity<String> updateBalance(@PathVariable Long accountId, @RequestParam("balance") double balance) {

        bankAccountService.updateBalance(accountId, balance);
        return ResponseEntity.ok("Balance updated successfully for account ID: " + accountId);
    }

}
