package com.example.transactions_service.service;

import com.example.transactions_service.dto.BankAccountDto;
import com.example.transactions_service.dto.TransactionsDto;
import com.example.transactions_service.dto.TransactionsMapper;
import com.example.transactions_service.entity.TransactionsEntity;
import com.example.transactions_service.enums.AccountStatus;
import com.example.transactions_service.enums.AccountType;
import com.example.transactions_service.enums.TransactionsStatus;
import com.example.transactions_service.enums.TransactionsType;
import com.example.transactions_service.exception.ClosedAccountException;
import com.example.transactions_service.exception.HighValueTransactionException;
import com.example.transactions_service.exception.InsufficientFundsException;
import com.example.transactions_service.exception.MinimumBalanceException;
import com.example.transactions_service.feign.BankAccountClient;
import com.example.transactions_service.repository.TransactionsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionsService {

//    private final BankAccountRepository bankAccountRepository;
    private final BankAccountClient bankAccountClient;
    private final TransactionsRepository transactionsRepository;
    private final TransactionsMapper transactionsMapper;

    @Transactional
    public TransactionsDto transfer(Long fromAccountId, Long toAccountId, double amount) {
//        BankAccountDto fromAccount = bankAccountRepository.findById(fromAccountId)
//                .orElseThrow(() -> new AccountNotFoundException("From account not found"));
        BankAccountDto fromAccount = bankAccountClient.getAccount(fromAccountId);
        if (fromAccount == null) {
            throw new RuntimeException("No accounts found for customer: " + fromAccountId);
        }
//        BankAccountEntity toAccount = bankAccountRepository.findById(toAccountId)
//                .orElseThrow(() -> new AccountNotFoundException("To account not found"));
        BankAccountDto toAccount = bankAccountClient.getAccount(toAccountId);
        if (toAccount == null) {
            throw new RuntimeException("No accounts found for customer: " + toAccountId);
        }

        if (fromAccount.getAccountStatus() == AccountStatus.CLOSED) {
            throw new ClosedAccountException("Can't perform deposit transaction since account is closed");
        }
        if (toAccount.getAccountStatus() == AccountStatus.CLOSED) {
            throw new ClosedAccountException("Can't perform deposit transaction since account is closed");
        }

        if (fromAccount.getBalance() < amount) {
            TransactionsEntity failedTransaction = transactionsMapper.toEntity(createFailedTransaction(fromAccountId, toAccountId, amount));
            transactionsRepository.save(failedTransaction);
            throw new InsufficientFundsException("Insufficient funds for transfer");
        }
        if (fromAccount.getAccountType() == AccountType.SAVINGS && (fromAccount.getBalance() - amount) < 1000) {
            TransactionsEntity failedTransaction = transactionsMapper.toEntity(createFailedTransaction(fromAccountId, toAccountId, amount));
            transactionsRepository.save(failedTransaction);
            throw new MinimumBalanceException("SAVINGS account must maintain a minimum balance of 1000");
        }
        // High-value transfer checks
        if (amount > 100000) {
//            CustomerEntity customer = fromAccount.getCustomer();
//            if (customer.getPan() == null || customer.getPan().isBlank()) {
//                TransactionsEntity failedTransaction = transactionsMapper.toEntity(createFailedTransaction(fromAccountId, toAccountId, amount));
//                transactionsRepository.save(failedTransaction);
//                throw new RequiresPanException("PAN is required for transfers exceeding 100,000");
//            }
            if (amount > 200000) {
                TransactionsEntity failedTransaction = transactionsMapper.toEntity(createFailedTransaction(fromAccountId, toAccountId, amount));
                transactionsRepository.save(failedTransaction);
                throw new HighValueTransactionException("Transfer exceeds 200,000 limit");
            }
        }

        double newFromBalance = fromAccount.getBalance() - amount;
        double newToBalance = toAccount.getBalance() + amount;

//        fromAccount.setBalance(fromAccount.getBalance() - amount);
//        toAccount.setBalance(toAccount.getBalance() + amount);
//        fromAccount.setLastTransactionTimestamp(LocalDateTime.now());
//        toAccount.setLastTransactionTimestamp(LocalDateTime.now());
//        bankAccountRepository.save(fromAccount);
//        bankAccountRepository.save(toAccount);
        fromAccount.setLastTransactionTimestamp(LocalDateTime.now());
        toAccount.setLastTransactionTimestamp(LocalDateTime.now());
        bankAccountClient.updateBalance(fromAccountId, newFromBalance);
        bankAccountClient.updateBalance(toAccountId, newToBalance);

        TransactionsEntity transaction = new TransactionsEntity();
        transaction.setFromAccountId(fromAccountId);
        transaction.setToAccountId(toAccountId);
        transaction.setAmount(amount);
        transaction.setInitialBalance(fromAccount.getBalance() + amount);
        transaction.setRemainingBalance(fromAccount.getBalance());
        transaction.setTransactionsType(TransactionsType.TRANSFER);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setTransactionsStatus(TransactionsStatus.SUCCESS);
        return transactionsMapper.toDTO(transactionsRepository.save(transaction));
    }

    private TransactionsDto createFailedTransaction(Long fromAccountId, Long toAccountId, double amount) {

        BankAccountDto fromAccount = bankAccountClient.getAccount(fromAccountId);

        TransactionsEntity transaction = new TransactionsEntity();
        transaction.setTransactionId(UUID.randomUUID());
        transaction.setFromAccountId(fromAccountId);
        transaction.setToAccountId(toAccountId);
        transaction.setAmount(amount);
        transaction.setInitialBalance(fromAccount.getBalance());
        transaction.setRemainingBalance(transaction.getInitialBalance());
        transaction.setTransactionsType(TransactionsType.TRANSFER);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setTransactionsStatus(TransactionsStatus.FAILED);
        return transactionsMapper.toDTO(transaction);
    }

    public List<TransactionsDto> getAllTransactions() {

        return transactionsRepository.findAll()
                .stream()
                .map(transactionsMapper::toDTO)
                .collect(Collectors.toList());
    }

    public TransactionsDto getTransaction(UUID id) {
        return transactionsMapper.toDTO(transactionsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found")));
    }

    public List<TransactionsDto> getTransactionsByAccountId(Long accountId) {
        List<TransactionsEntity> transactions = transactionsRepository.findAll().stream()
                .filter(t -> (t.getFromAccountId() != null && t.getFromAccountId().equals(accountId))
                        || (t.getToAccountId() != null && t.getToAccountId().equals(accountId)))
                .toList();
        return transactions.stream()
                .map(transactionsMapper::toDTO)
                .collect(Collectors.toList());
    }

    // for BankAccount Deposit and withdraw

    public TransactionsDto createDeposit(TransactionsDto dto) {
        TransactionsEntity transaction = new TransactionsEntity();
        transaction.setToAccountId(dto.getToAccountId());
        transaction.setAmount(dto.getAmount());
        transaction.setInitialBalance(dto.getInitialBalance());
        transaction.setRemainingBalance(dto.getRemainingBalance());
        transaction.setTransactionsType(TransactionsType.DEPOSIT);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setTransactionsStatus(TransactionsStatus.SUCCESS);
        return transactionsMapper.toDTO(transactionsRepository.save(transaction));
    }

    public TransactionsDto createWithdraw(TransactionsDto dto) {
        TransactionsEntity transaction = new TransactionsEntity();
        transaction.setFromAccountId(dto.getFromAccountId());
        transaction.setAmount(dto.getAmount());
        transaction.setInitialBalance(dto.getInitialBalance());
        transaction.setRemainingBalance(dto.getRemainingBalance());
        transaction.setTransactionsType(TransactionsType.WITHDRAW);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setTransactionsStatus(TransactionsStatus.SUCCESS);
        return transactionsMapper.toDTO(transactionsRepository.save(transaction));
    }



}
