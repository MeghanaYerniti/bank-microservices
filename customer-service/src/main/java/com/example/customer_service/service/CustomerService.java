package com.example.customer_service.service;

import com.example.customer_service.dto.BankAccountDto;
import com.example.customer_service.dto.CustomerDto;
import com.example.customer_service.dto.CustomerMapper;
import com.example.customer_service.dto.CustomerWithAccounts;
import com.example.customer_service.entity.CustomerEntity;
import com.example.customer_service.exception.CustomerNotFoundException;
import com.example.customer_service.feign.BankAccountClient;
import com.example.customer_service.repository.CustomerRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CustomerService {

    private final CustomerMapper customerMapper;
    private final BankAccountClient bankAccountClient;
    private final CustomerRepository customerRepository;

    public List<CustomerDto> getAllCustomers() {
        return customerRepository.findAll()
                .stream()
                .map(customerMapper::toDTO)
                .collect(Collectors.toList());
    }


    public CustomerWithAccounts createCustomer(@Valid CustomerDto customerDTO) {
        CustomerEntity customerEntity = customerMapper.toEntity(customerDTO);

        if (customerEntity.getCreatedAt() == null) {
            customerEntity.setCreatedAt(LocalDateTime.now());
        }
        if (customerEntity.getUpdatedAt() == null) {
            customerEntity.setUpdatedAt(LocalDateTime.now());
        }

        CustomerEntity savedCustomer = customerRepository.save(customerEntity);

        List<BankAccountDto> accounts = new ArrayList<>();
        try {
            // Attempt to fetch accounts
            accounts = bankAccountClient.getAccountsByCustomerId(savedCustomer.getCustomerId());

            // In case the service returns null (some Feign clients might)
            if (accounts == null) {
                accounts = new ArrayList<>();
            }
        } catch (Exception e) {
            // Log the exception for debugging
//            log.warn("No accounts found for customer ID {} or bank account service unavailable: {}",
//                    savedCustomer.getCustomerId(), e.getMessage());
            accounts = new ArrayList<>();
        }

        CustomerDto customerDto = customerMapper.toDTO(savedCustomer);
        return new CustomerWithAccounts(customerDto, accounts);
    }



    public CustomerDto updateCustomer(Long id, CustomerDto updatedCustomer) {

        CustomerEntity existingCustomer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
        if (updatedCustomer.getName() != null && !updatedCustomer.getName().isBlank()) {
            existingCustomer.setName(updatedCustomer.getName());
        }
        if (updatedCustomer.getPan() != null && !updatedCustomer.getPan().isBlank()) {
            existingCustomer.setPan(updatedCustomer.getPan());
        }
        if (updatedCustomer.getEmail() != null && !updatedCustomer.getEmail().isBlank()) {
            existingCustomer.setEmail(updatedCustomer.getEmail());
        }
        if (updatedCustomer.getPhone() != null && !updatedCustomer.getPhone().isBlank()) {
            existingCustomer.setPhone(updatedCustomer.getPhone());
        }

        CustomerEntity savedCustomer = customerRepository.save(existingCustomer);

        // updating bankAccounts table
        bankAccountClient.updateAccountHolderName(savedCustomer.getCustomerId(), savedCustomer.getName());
//        for (BankAccountDto account : existingCustomer.getAccounts()) {
//            account.setAccountHolderName(existingCustomer.getName());
//        }

        return customerMapper.toDTO(savedCustomer);
    }

    public CustomerDto getCustomer(Long id) {
        return customerMapper.toDTO(customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found")));
    }

}
