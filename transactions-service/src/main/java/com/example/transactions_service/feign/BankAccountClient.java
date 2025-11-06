package com.example.transactions_service.feign;

import com.example.transactions_service.dto.BankAccountDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("BANK-ACCOUNT-SERVICE")
public interface BankAccountClient {

    @GetMapping("/accounts/id/{id}")
    public BankAccountDto getAccount(@PathVariable Long id);

    @PutMapping("/accounts/update-balance/{accountId}")
    public ResponseEntity<String> updateBalance(@PathVariable Long accountId, @RequestParam("balance") double balance);
}
