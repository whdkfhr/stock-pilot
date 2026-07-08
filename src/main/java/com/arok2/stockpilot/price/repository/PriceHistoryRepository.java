package com.arok2.stockpilot.price.repository;

import com.arok2.stockpilot.price.domain.PriceHistory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findTop20ByCodeOrderByTradedAtDesc(String code);
}
