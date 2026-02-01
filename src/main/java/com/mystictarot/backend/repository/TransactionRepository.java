package com.mystictarot.backend.repository;

import com.mystictarot.backend.entity.Transaction;
import com.mystictarot.backend.entity.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Transaction entity
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Find all transactions by user ID with pagination
     * @param userId user ID
     * @param pageable pagination parameters
     * @return Page of Transactions
     */
    Page<Transaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find all transactions by user ID and status
     * @param userId user ID
     * @param status transaction status
     * @return List of Transactions
     */
    List<Transaction> findByUserIdAndStatus(UUID userId, TransactionStatus status);

    /**
     * Find transaction by provider transaction ID
     * Used for webhook processing to avoid duplicate processing
     * @param providerTransactionId provider transaction ID
     * @return Optional Transaction
     */
    Optional<Transaction> findByProviderTransactionId(String providerTransactionId);

    /**
     * Check if transaction exists by provider transaction ID
     * @param providerTransactionId provider transaction ID
     * @return true if exists
     */
    boolean existsByProviderTransactionId(String providerTransactionId);
}
