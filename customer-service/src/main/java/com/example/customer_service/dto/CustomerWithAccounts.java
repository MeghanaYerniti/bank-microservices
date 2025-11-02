package com.example.customer_service.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerWithAccounts {
    private CustomerDto customer;
    private List<BankAccountDto> accounts;
}
