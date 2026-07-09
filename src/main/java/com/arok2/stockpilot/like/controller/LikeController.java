package com.arok2.stockpilot.like.controller;

import com.arok2.stockpilot.like.LikeService;
import com.arok2.stockpilot.like.dto.LikeResponse;
import com.arok2.stockpilot.support.AuthenticatedUser;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    /** 좋아요 등록(인증 필요). 1인 1좋아요. */
    @PostMapping("/{code}/like")
    public ResponseEntity<LikeResponse> like(@PathVariable String code, @AuthenticatedUser Long userId) {
        return ResponseEntity.ok(new LikeResponse(code, likeService.like(userId, code)));
    }

    /** 좋아요 수 조회(공개). */
    @GetMapping("/{code}/likes")
    public ResponseEntity<LikeResponse> likes(@PathVariable String code) {
        return ResponseEntity.ok(new LikeResponse(code, likeService.count(code)));
    }
}
