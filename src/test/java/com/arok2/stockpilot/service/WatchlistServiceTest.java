package com.arok2.stockpilot.service;

import com.arok2.stockpilot.domain.Stock;
import com.arok2.stockpilot.domain.Watchlist;
import com.arok2.stockpilot.dto.WatchlistCreateResponse;
import com.arok2.stockpilot.dto.WatchlistDeleteResponse;
import com.arok2.stockpilot.dto.WatchlistPageResponse;
import com.arok2.stockpilot.exception.StockNotFoundException;
import com.arok2.stockpilot.exception.WatchlistAlreadyExistsException;
import com.arok2.stockpilot.exception.WatchlistNotFoundException;
import com.arok2.stockpilot.repository.StockRepository;
import com.arok2.stockpilot.repository.WatchlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

    @Mock
    private WatchlistRepository watchlistRepository;

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private WatchlistService watchlistService;

    private Stock stock;

    @BeforeEach
    void setUp() {
        stock = new Stock("005930", "삼성전자");
        setId(stock, 42L);
    }

    @Test
    void register_success_incrementsWatchCountOnce() {
        Long userId = 7L;
        Long stockId = 42L;

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(watchlistRepository.existsByUserIdAndStockId(userId, stockId)).thenReturn(false);

        Watchlist saved = new Watchlist(userId, stockId);
        setId(saved, 1001L);
        when(watchlistRepository.save(any(Watchlist.class))).thenReturn(saved);

        WatchlistCreateResponse response = watchlistService.register(userId, stockId);

        assertThat(response.watchlistId()).isEqualTo(1001L);
        assertThat(response.stockId()).isEqualTo(stockId);
        assertThat(response.userId()).isEqualTo(userId);

        verify(stockRepository, times(1)).incrementWatchCount(stockId);
    }

    @Test
    void register_stockNotFound_throwsStockNotFoundException() {
        Long userId = 7L;
        Long stockId = 999L;

        when(stockRepository.findById(stockId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchlistService.register(userId, stockId))
                .isInstanceOf(StockNotFoundException.class);

        verify(watchlistRepository, never()).save(any());
        verify(stockRepository, never()).incrementWatchCount(any());
    }

    @Test
    void register_alreadyExists_throwsWatchlistAlreadyExistsException() {
        Long userId = 7L;
        Long stockId = 42L;

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(watchlistRepository.existsByUserIdAndStockId(userId, stockId)).thenReturn(true);

        assertThatThrownBy(() -> watchlistService.register(userId, stockId))
                .isInstanceOf(WatchlistAlreadyExistsException.class);

        verify(watchlistRepository, never()).save(any());
        verify(stockRepository, never()).incrementWatchCount(any());
    }

    @Test
    void register_dbUniqueConstraintViolation_isMappedToWatchlistAlreadyExistsException() {
        Long userId = 7L;
        Long stockId = 42L;

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(watchlistRepository.existsByUserIdAndStockId(userId, stockId)).thenReturn(false);
        when(watchlistRepository.save(any(Watchlist.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint violated"));

        assertThatThrownBy(() -> watchlistService.register(userId, stockId))
                .isInstanceOf(WatchlistAlreadyExistsException.class);

        verify(stockRepository, never()).incrementWatchCount(any());
    }

    @Test
    void unwatch_success_decrementsWatchCountOnce() {
        Long userId = 7L;
        Long stockId = 42L;
        Watchlist existing = new Watchlist(userId, stockId);
        setId(existing, 1001L);

        when(watchlistRepository.findByUserIdAndStockId(userId, stockId)).thenReturn(Optional.of(existing));

        WatchlistDeleteResponse response = watchlistService.unwatch(userId, stockId);

        assertThat(response.stockId()).isEqualTo(stockId);
        verify(watchlistRepository, times(1)).deleteByUserIdAndStockId(userId, stockId);
        verify(stockRepository, times(1)).decrementWatchCount(stockId);
    }

    @Test
    void unwatch_notFound_throwsWatchlistNotFoundException() {
        Long userId = 7L;
        Long stockId = 42L;

        when(watchlistRepository.findByUserIdAndStockId(userId, stockId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchlistService.unwatch(userId, stockId))
                .isInstanceOf(WatchlistNotFoundException.class);

        verify(watchlistRepository, never()).deleteByUserIdAndStockId(any(), any());
        verify(stockRepository, never()).decrementWatchCount(any());
    }

    @Test
    void getMyWatchlist_returnsOnlyOwnItems_withBatchLoadedStock() {
        Long userId = 7L;
        Pageable pageable = PageRequest.of(0, 20);

        Watchlist w1 = new Watchlist(userId, 42L);
        setId(w1, 1001L);
        Page<Watchlist> page = new PageImpl<>(List.of(w1), pageable, 1);

        when(watchlistRepository.findByUserId(userId, pageable)).thenReturn(page);
        when(stockRepository.findAllById(List.of(42L))).thenReturn(List.of(stock));

        WatchlistPageResponse response = watchlistService.getMyWatchlist(userId, pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).stockId()).isEqualTo(42L);
        assertThat(response.content().get(0).stockCode()).isEqualTo("005930");
        assertThat(response.totalElements()).isEqualTo(1);

        // 배치 조회이므로 stockId당 findById가 아닌 findAllById 1회만 호출되어야 한다 (N+1 회피 검증)
        verify(stockRepository, times(1)).findAllById(anyList());
        verify(stockRepository, never()).findById(any());
    }

    private static void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
