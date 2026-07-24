package com.arok2.stockpilot.price.quote;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    /** 종목 시세 요약(공개). 고가/저가/거래량/52주 최고·최저. */
    @GetMapping("/{code}/quote")
    public ResponseEntity<QuoteResponse> quote(@PathVariable String code) {
        return ResponseEntity.ok(quoteService.getQuote(code));
    }
}
