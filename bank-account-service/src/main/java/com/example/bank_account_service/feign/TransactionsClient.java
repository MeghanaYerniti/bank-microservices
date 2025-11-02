package com.example.bank_account_service.feign;

import com.example.bank_account_service.dto.TransactionsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("TRANSACTIONS-SERVICE")
public interface TransactionsClient {

    @PostMapping("/api/v1/transactions/deposit")
    public ResponseEntity<TransactionsDto> deposit(@RequestBody TransactionsDto dto);

    @PostMapping("/api/v1/transactions/withdraw")
    public ResponseEntity<TransactionsDto> withdraw(@RequestBody TransactionsDto dto);

}
