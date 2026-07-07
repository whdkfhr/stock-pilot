package com.arok2.stockpilot.repository;

import com.arok2.stockpilot.domain.Watchlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    boolean existsByUserIdAndStockId(Long userId, Long stockId);

    Optional<Watchlist> findByUserIdAndStockId(Long userId, Long stockId);

    @Modifying
    @Query("DELETE FROM Watchlist w WHERE w.userId = :userId AND w.stockId = :stockId")
    int deleteByUserIdAndStockId(@Param("userId") Long userId, @Param("stockId") Long stockId);

    Page<Watchlist> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT w FROM Watchlist w JOIN FETCH w.stockId WHERE w.userId = :userId")
    Page<Watchlist> findByUserIdWithStock(@Param("userId") Long userId, Pageable pageable);
}
