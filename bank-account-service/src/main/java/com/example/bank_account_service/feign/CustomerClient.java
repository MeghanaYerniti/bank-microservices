package com.example.bank_account_service.feign;

import com.example.bank_account_service.dto.CustomerDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("CUSTOMER-SERVICE")
public interface CustomerClient {

    @GetMapping("/customers/id/{id}")
    public CustomerDto getCustomer(@PathVariable Long id);

}
