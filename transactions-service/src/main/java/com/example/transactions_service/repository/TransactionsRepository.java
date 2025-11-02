package com.example.transactions_service.repository;

import com.example.transactions_service.entity.TransactionsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TransactionsRepository extends JpaRepository<TransactionsEntity, UUID> {
}
