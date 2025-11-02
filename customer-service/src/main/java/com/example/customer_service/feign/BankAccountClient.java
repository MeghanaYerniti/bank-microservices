package com.example.customer_service.feign;

import com.example.customer_service.dto.BankAccountDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

//@FeignClient("bank-account-service")
@FeignClient("BANK-ACCOUNT-SERVICE")
public interface BankAccountClient {

    @GetMapping("/api/v1/accounts/by-customer/{customerId}")
    public List<BankAccountDto> getAccountsByCustomerId(@PathVariable Long customerId);

    @PutMapping("/api/v1/accounts/update-name/{customerId}")
    public void updateAccountHolderName(@PathVariable Long customerId, @RequestParam String newName);
}
