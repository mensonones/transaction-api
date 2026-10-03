package com.emerson.transaction_api.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactonRepository extends JpaRepository<Transaction, Long> {
}
