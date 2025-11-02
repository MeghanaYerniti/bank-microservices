package com.example.transactions_service.dto;

import com.example.transactions_service.entity.TransactionsEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionsMapper {
    TransactionsDto toDTO(TransactionsEntity transactionsEntity);
    TransactionsEntity toEntity(TransactionsDto transactionsDto);
}
