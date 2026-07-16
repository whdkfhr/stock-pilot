package com.arok2.stockpilot.like;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    @SuppressWarnings("unchecked")
    private SetOperations<String, String> setOperations;

    @Test
    void 좋아요_등록시_SADD로_사용자를_담고_SCARD_결과를_반환한다() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.size("stock:likes:000660")).thenReturn(3L);

        LikeService likeService = new LikeService(redisTemplate, new SimpleMeterRegistry());
        long count = likeService.like(42L, "000660");

        verify(setOperations).add("stock:likes:000660", "42");
        assertThat(count).isEqualTo(3L);
    }

    @Test
    void 좋아요_수가_없으면_0을_반환한다() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.size(anyString())).thenReturn(null);

        LikeService likeService = new LikeService(redisTemplate, new SimpleMeterRegistry());

        assertThat(likeService.count("999999")).isZero();
    }

    @Test
    void 좋아요_해제시_SREM으로_제거하고_감소된_수를_반환한다() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.size("stock:likes:000660")).thenReturn(1L);

        LikeService likeService = new LikeService(redisTemplate, new SimpleMeterRegistry());
        long count = likeService.unlike(42L, "000660");

        verify(setOperations).remove("stock:likes:000660", "42");
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void 좋아요_여부는_SISMEMBER로_판단한다() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember("stock:likes:000660", "42")).thenReturn(true);
        when(setOperations.isMember("stock:likes:000660", "99")).thenReturn(false);

        LikeService likeService = new LikeService(redisTemplate, new SimpleMeterRegistry());

        assertThat(likeService.hasLiked(42L, "000660")).isTrue();
        assertThat(likeService.hasLiked(99L, "000660")).isFalse();
    }
}
