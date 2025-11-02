package com.example.bank_account_service.dto;

import com.example.bank_account_service.entity.BankAccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface BankAccountMapper {
//BankAccountMapper INSTANCE = Mappers.getMapper(BankAccountMapper.class);

    BankAccountDto toDTO(BankAccountEntity bankAccountEntity);
    BankAccountEntity toEntity(BankAccountDto bankAccountDTO);

}


