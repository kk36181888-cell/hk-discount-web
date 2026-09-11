package com.discount.discount_web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.discount.discount_web.model.Discount;
import com.discount.discount_web.repository.DiscountRepository;
import com.discount.discount_web.service.ScraperService;

@RestController
@RequestMapping("/api/discounts")
public class DiscountController {

    @Autowired
    private DiscountRepository discountRepository;

    @Autowired
    private ScraperService scraperService; // 注入爬蟲 Service

    @GetMapping
    public List<Discount> getAllDiscounts() {
        return discountRepository.findAll();
    }

    @PostMapping
    public Discount createDiscount(@RequestBody Discount discount) {
        return discountRepository.save(discount);
    }

    // 新增呢個 API 用嚟撻着惠康爬蟲
    @GetMapping("/test-scrape")
    public String runTestScraper() {
        scraperService.scrapeWellcome(); // <--- 已經幫你改咗做呼叫惠康爬蟲！
        return "惠康抓取測試中... 請睇 VS Code Terminal！";
    }
}