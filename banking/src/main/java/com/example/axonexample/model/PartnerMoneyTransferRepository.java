package com.example.axonexample.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartnerMoneyTransferRepository extends JpaRepository<PartnerMoneyTransfer, Integer> {
}