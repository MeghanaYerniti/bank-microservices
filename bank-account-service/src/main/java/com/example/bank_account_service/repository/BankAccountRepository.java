package com.example.bank_account_service.repository;

import com.example.bank_account_service.entity.BankAccountEntity;
import com.example.bank_account_service.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccountEntity, Long> {

    List<BankAccountEntity> findByCustomerId(Long customerId);

    List<BankAccountEntity> findTop5ByOrderByBalanceDesc();

    List<BankAccountEntity> findByAccountType(AccountType accountType);

}
