package com.arok2.stockpilot.repository;

import com.arok2.stockpilot.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByCode(String code);

    @Modifying
    @Query("UPDATE Stock s SET s.watchCount = s.watchCount + 1 WHERE s.id = :stockId")
    int incrementWatchCount(@Param("stockId") Long stockId);

    @Modifying
    @Query("UPDATE Stock s SET s.watchCount = s.watchCount - 1 WHERE s.id = :stockId AND s.watchCount > 0")
    int decrementWatchCount(@Param("stockId") Long stockId);
}
