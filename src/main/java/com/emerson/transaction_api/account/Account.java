package com.emerson.transaction_api.account;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(name = "accounts")
public class Account {

    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountId;

    @Column(name = "document_number", unique = true, nullable = false, length = 32)
    private String documentNumber;

    protected Account() {}

    public Account(String documentNumber) {
        this.documentNumber = documentNumber;
    }
}
