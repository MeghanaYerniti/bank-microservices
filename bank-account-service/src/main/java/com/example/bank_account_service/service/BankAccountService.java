package com.example.bank_account_service.service;

import com.example.bank_account_service.dto.BankAccountDto;
import com.example.bank_account_service.dto.BankAccountMapper;
import com.example.bank_account_service.dto.CustomerDto;
import com.example.bank_account_service.dto.TransactionsDto;
import com.example.bank_account_service.entity.BankAccountEntity;
import com.example.bank_account_service.enums.AccountStatus;
import com.example.bank_account_service.enums.AccountType;
import com.example.bank_account_service.enums.TransactionsStatus;
import com.example.bank_account_service.enums.TransactionsType;
import com.example.bank_account_service.exception.*;
import com.example.bank_account_service.feign.CustomerClient;
import com.example.bank_account_service.feign.TransactionsClient;
import com.example.bank_account_service.repository.BankAccountRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j // for logs
@Service
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
//    private final TransactionsRepository transactionsRepository;
    private final TransactionsClient transactionsClient;
//    private final CustomerRepository customerRepository;
    private final CustomerClient customerClient;
    private final BankAccountMapper bankAccountMapper;

    public BankAccountDto createAccount(BankAccountDto bankAccountDto) {

        BankAccountEntity bankAccountEntity = bankAccountMapper.toEntity(bankAccountDto);
        Long customerId = bankAccountEntity.getCustomerId();
        CustomerDto customerDto = customerClient.getCustomer(customerId);
        if (customerDto == null) throw new CustomerNotFoundException("Customer not found");
        if (bankAccountEntity.getCreatedAt() == null) bankAccountEntity.setCreatedAt(LocalDateTime.now());
        if (bankAccountEntity.getUpdatedAt() == null) bankAccountEntity.setUpdatedAt(LocalDateTime.now());

        if (bankAccountEntity.getAccountType() == AccountType.SAVINGS) {
            if (bankAccountEntity.getInterestRate() < 3.5) {
                bankAccountEntity.setInterestRate(3.5);
            }
            bankAccountEntity.setAccountType(AccountType.SAVINGS);
        } else if (bankAccountEntity.getAccountType() == AccountType.CURRENT) {
            if (bankAccountEntity.getInterestRate() != 0) {
                throw new InterestMustBeZeroException("Interest rate must be 0 for CURRENT accounts");
            }
            bankAccountEntity.setAccountType(AccountType.CURRENT);
        }
        if (bankAccountEntity.getLastTransactionTimestamp() == null) {
            bankAccountEntity.setLastTransactionTimestamp(LocalDateTime.now());
        }
        bankAccountEntity.setCustomerId(customerId);
        bankAccountEntity.setAccountStatus(AccountStatus.ACTIVE);

        BankAccountEntity save = bankAccountRepository.save(bankAccountEntity);

        return bankAccountMapper.toDTO(save);
    }

    public BankAccountDto getAccount(Long id) {
        BankAccountEntity bankAccountEntity = bankAccountRepository.findById(id).orElseThrow(() -> new AccountNotFoundException("Account not found"));
        if(bankAccountEntity.getAccountStatus().equals(AccountStatus.CLOSED)) throw new ClosedAccountException("Can't perform action, since account is CLOSED");
        return bankAccountMapper.toDTO(bankAccountEntity);
    }

    public double getAccountBalance(Long id) {
        BankAccountEntity bankAccountEntity = bankAccountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        if(bankAccountEntity.getAccountStatus().equals(AccountStatus.CLOSED)) throw new ClosedAccountException("Can't perform action, since account is CLOSED");
        return bankAccountEntity.getBalance();
    }

    @Transactional  // single transaction — either all succeed, or all fail
    public BankAccountDto depositAmount(Long id, double amount) {

        BankAccountEntity account = bankAccountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        if (account.getAccountStatus() == AccountStatus.CLOSED) {
            throw new ClosedAccountException("Can't perform action, since account is CLOSED");
        }

        if (amount > 100000) {
            Long customerId = account.getCustomerId();
            CustomerDto customer = customerClient.getCustomer(customerId);
            if (customer.getPan() == null || customer.getPan().isBlank()) {
                throw new RequiresPanException("PAN is required for deposits exceeding 100,000");
            }
            if (amount > 200000) {
                throw new HighValueTransactionException("Deposit exceeds 200,000 limit");
            }
        }
        double newBalance = account.getBalance() + amount;
        if (newBalance < 0) {
            throw new MinimumBalanceException("Deposit cannot result in a negative balance");
        }

//        TransactionsEntity transaction = new TransactionsEntity();
//        //transaction.setToAccountId(id);
//        transaction.setAmount(amount);
//        transaction.setInitialBalance(account.getBalance());
//        transaction.setRemainingBalance(newBalance);
//        transaction.setTransactionsType(TransactionsType.DEPOSIT);
//        transaction.setTimestamp(LocalDateTime.now());
//        transaction.setTransactionsStatus(TransactionsStatus.SUCCESS);
//        transactionsRepository.save(transaction);

        TransactionsDto dto = new TransactionsDto();
        dto.setToAccountId(id);
        dto.setAmount(amount);
        dto.setInitialBalance(account.getBalance());
        dto.setRemainingBalance(newBalance);
        dto.setTransactionsType(TransactionsType.DEPOSIT);
        dto.setTimestamp(LocalDateTime.now());
        dto.setTransactionsStatus(TransactionsStatus.SUCCESS);

        transactionsClient.deposit(dto);

        account.setBalance(newBalance);
        account.setLastTransactionTimestamp(LocalDateTime.now());
        return bankAccountMapper.toDTO(bankAccountRepository.save(account));
    }

    @Transactional
    public BankAccountDto withdrawAmount(Long id, double amount) {
        BankAccountEntity account = bankAccountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        if (account.getAccountStatus() == AccountStatus.CLOSED) {
            throw new ClosedAccountException("Can't perform action, since account is CLOSED");
        }
        if (account.getBalance() < amount) {
            throw new InsufficientFundsException("Insufficient funds for withdrawal");
        }
        if (account.getAccountType() == AccountType.SAVINGS && (account.getBalance() - amount) < 1000) {
            throw new MinimumBalanceException("SAVINGS account must maintain a minimum balance of 1000");
        }
        if (account.getAccountType() == AccountType.CURRENT && (account.getBalance() - amount) < -5000) {
            throw new InsufficientFundsException("CURRENT account cannot go below -5000 overdraft limit");
        }
        double newBalance = account.getBalance() - amount;
        if (newBalance < 0 && account.getAccountType() == AccountType.SAVINGS) {
            throw new MinimumBalanceException("SAVINGS account cannot have a negative balance");
        }

//        TransactionsEntity transaction = new TransactionsEntity();
////        transaction.setTransactionId(UUID.randomUUID());
//        transaction.setFromAccountId(id);
//        transaction.setAmount(amount);
//        transaction.setInitialBalance(account.getBalance());
//        transaction.setRemainingBalance(newBalance);
//        transaction.setTransactionsType(TransactionsType.WITHDRAW);
//        transaction.setTimestamp(LocalDateTime.now());
//        transaction.setTransactionsStatus(TransactionsStatus.SUCCESS);
//        transactionsRepository.save(transaction);

        TransactionsDto dto = new TransactionsDto();
        dto.setFromAccountId(id);
        dto.setAmount(amount);
        dto.setInitialBalance(account.getBalance());
        dto.setRemainingBalance(newBalance);
        dto.setTransactionsType(TransactionsType.WITHDRAW);
        dto.setTimestamp(LocalDateTime.now());
        dto.setTransactionsStatus(TransactionsStatus.SUCCESS);

        transactionsClient.withdraw(dto);

        account.setBalance(newBalance);
        account.setLastTransactionTimestamp(LocalDateTime.now());
        return bankAccountMapper.toDTO(bankAccountRepository.save(account));
    }

    public void deleteAccount(Long id) {
        Optional<BankAccountEntity> account= bankAccountRepository.findById(id);
        if(account.isEmpty()){
            throw new AccountNotFoundException("Account not found");
        }
        else {
            bankAccountRepository.deleteById(id);
        }
    }

    public Page<BankAccountDto> getAllAccounts(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<BankAccountEntity> accountPage = bankAccountRepository.findAll(pageable);

        return accountPage.map(bankAccountMapper::toDTO);
    }

    public List<BankAccountDto> getTopBalances(int limit) {
        return bankAccountRepository.findTop5ByOrderByBalanceDesc()
                .stream()
                .map(bankAccountMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Customer service
    public List<BankAccountDto> getAccountsByCustomerId(Long customerId) {
        return bankAccountRepository.findByCustomerId(customerId)
                .stream()
                .map(bankAccountMapper::toDTO)
                .collect(Collectors.toList());
    }

    public void updateAccountHolderName(Long customerId, String newName) {
        List<BankAccountEntity> accounts = bankAccountRepository.findByCustomerId(customerId);
        boolean anyUpdated = false;
        if (accounts.isEmpty()) {
            log.info("Customer {} has no accounts. Skipping update.", customerId);
            return;
//            throw new AccountNotFoundException("No accounts found for customer ID: " + customerId);
        }
        for (BankAccountEntity account : accounts) {
            if (account.getAccountStatus() == AccountStatus.CLOSED) {
                continue;
            }
            account.setAccountHolderName(newName);
            account.setUpdatedAt(LocalDateTime.now());
            anyUpdated = true;
        }
        if (!anyUpdated) {
            throw new ClosedAccountException("Can't perform action, since account is CLOSED");
        }

        bankAccountRepository.saveAll(accounts);
    }

    // transactions service
    @Transactional
    public void updateBalance(Long accountId, double newBalance) {
        BankAccountEntity account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        if (account.getAccountStatus() == AccountStatus.CLOSED) {
            throw new ClosedAccountException("Cannot update balance for closed account ID: " + accountId);
        }

        account.setBalance(newBalance);
        account.setLastTransactionTimestamp(LocalDateTime.now());
        bankAccountRepository.save(account);
    }

}
