package com.example.api_gateway.dto;

import lombok.Data;

@Data
public class CustomerDto {
    private Long customerId;
    private String name;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}