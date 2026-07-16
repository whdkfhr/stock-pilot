package com.arok2.stockpilot.like.dto;

/** 특정 사용자 기준 좋아요 상태(눌렀는지 + 현재 총 개수). */
public record LikeStatusResponse(String code, boolean liked, long likeCount) {
}
