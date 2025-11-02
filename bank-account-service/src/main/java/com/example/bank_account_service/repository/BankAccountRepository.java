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

//    @Query(value = "SELECT * FROM bank_account " +
//            "ORDER BY balance DESC " +
//            "LIMIT 5",
//            nativeQuery = true)
//    List<BankAccountEntity> findTop5AccountsByBalance();
    List<BankAccountEntity> findTop5ByOrderByBalanceDesc();

    // in version 2
//    @Query("SELECT a FROM BankAccountEntity a WHERE a.accountType = :accountType")
//    List<BankAccountEntity> findAccountsByType(@Param("accountType") AccountType accountType);
    List<BankAccountEntity> findByAccountType(AccountType accountType);

}
