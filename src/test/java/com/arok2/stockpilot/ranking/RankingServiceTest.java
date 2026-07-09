package com.arok2.stockpilot.ranking;

import com.arok2.stockpilot.domain.Stock;
import com.arok2.stockpilot.ranking.dto.RankingItem;
import com.arok2.stockpilot.repository.StockRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    @SuppressWarnings("unchecked")
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private StockRepository stockRepository;

    @Test
    void 조회_발생시_ZINCRBY로_점수를_1_증가시킨다() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        RankingService rankingService = new RankingService(redisTemplate, stockRepository);
        rankingService.recordView("000660");

        verify(zSetOperations).incrementScore("rank:popular", "000660", 1);
    }

    @Test
    void 상위_종목을_순위와_종목명과_함께_반환한다() {
        Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
        tuples.add(new DefaultTypedTuple<>("000660", 30.0));
        tuples.add(new DefaultTypedTuple<>("005930", 20.0));
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRangeWithScores("rank:popular", 0, 1)).thenReturn(tuples);
        when(stockRepository.findAll()).thenReturn(List.of(
                Stock.of("000660", "SK하이닉스", 13, 1.5, 22, 1.0),
                Stock.of("005930", "삼성전자", 12, 1.4, 15, 2.0)));

        RankingService rankingService = new RankingService(redisTemplate, stockRepository);
        List<RankingItem> items = rankingService.top(2);

        assertThat(items).hasSize(2);
        assertThat(items.get(0)).extracting(RankingItem::rank, RankingItem::code, RankingItem::name, RankingItem::viewCount)
                .containsExactly(1, "000660", "SK하이닉스", 30L);
        assertThat(items.get(1)).extracting(RankingItem::rank, RankingItem::code, RankingItem::name, RankingItem::viewCount)
                .containsExactly(2, "005930", "삼성전자", 20L);
    }

    @Test
    void 랭킹이_비어있으면_빈_목록을_반환한다() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRangeWithScores("rank:popular", 0, 9)).thenReturn(null);

        RankingService rankingService = new RankingService(redisTemplate, stockRepository);

        assertThat(rankingService.top(10)).isEmpty();
    }
}
