package com.emerson.transaction_api.transaction;

import com.emerson.transaction_api.account.Account;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "operation_type_id", nullable = false)
    private Integer operationTypeId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(name = "idempotency_key", unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "event_date", nullable = false)
    private OffsetDateTime eventDate;

    protected Transaction() {}

    public Transaction(Account account, Integer operationTypeId, BigDecimal amount) {
        this.account = account;
        this.operationTypeId = operationTypeId;
        this.amount = amount;
        this.balance = amount;
        this.eventDate = OffsetDateTime.now();
    }

    public void updateBalance(BigDecimal newBalance) {
        this.balance = newBalance;
    }
}
