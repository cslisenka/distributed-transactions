package com.example.axonexample.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MoneyTransferRepository extends JpaRepository<MoneyTransfer, Integer> {
}