package com.example.customer_service.controller;

import com.example.customer_service.dto.CustomerDto;
import com.example.customer_service.dto.CustomerMapper;
import com.example.customer_service.dto.CustomerWithAccounts;
import com.example.customer_service.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerMapper bankMapper;

    @GetMapping("/all")
    public List<CustomerDto> getAllCustomers() {
        return customerService.getAllCustomers();
    }

    @PostMapping("/")
    public CustomerWithAccounts createCustomer(@Valid @RequestBody CustomerDto customerDTO) {
        return customerService.createCustomer(customerDTO);
    }

    @PutMapping("/{id}")
    public CustomerDto updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerDto customerDTO) {
        return customerService.updateCustomer(id, customerDTO);
    }

    @GetMapping("/id/{id}")
    public CustomerDto getCustomer(@PathVariable Long id) {
        return customerService.getCustomer(id);
    }

}