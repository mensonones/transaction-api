package com.emerson.transaction_api.account;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByDocumentNumber(String documentNumber);
}
