package com.emerson.transaction_api.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    // PostgreSQL waits for a concurrent insert with this key before deciding whether to insert.
    @Modifying
    @Query(value = """
            INSERT INTO transactions (account_id, operation_type_id, amount, event_date, idempotency_key)
            VALUES (:accountId, :operationTypeId, :amount, :eventDate, :key)
            ON CONFLICT (idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertIfKeyIsNew(@Param("accountId") Long accountId,
                         @Param("operationTypeId") Integer operationTypeId,
                         @Param("amount") BigDecimal amount,
                         @Param("eventDate") OffsetDateTime eventDate,
                         @Param("key") String key);
}
