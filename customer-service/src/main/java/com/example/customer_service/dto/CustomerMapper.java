package com.example.customer_service.dto;

import com.example.customer_service.entity.CustomerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerDto toDTO(CustomerEntity customerEntity);
    CustomerEntity toEntity(CustomerDto customerDto);
}
