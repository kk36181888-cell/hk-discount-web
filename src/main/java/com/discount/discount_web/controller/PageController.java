package com.discount.discount_web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    // 1. 首頁
    @GetMapping("/")
    public String index() {
        return "index"; // 自動對應 templates/index.html
    }

    // 2. 超市情報
    @GetMapping("/supermarket")
    public String supermarket() {
        return "supermarket"; // 自動對應 templates/supermarket.html
    }

    // 3. 飲食優惠情報
    @GetMapping("/dining")
    public String dining() {
        return "dining";
    }

    // 4. 開倉大減價
    @GetMapping("/warehouse")
    public String warehouse() {
        return "warehouse";
    }

    // 5. 其他著數
    @GetMapping("/others")
    public String others() {
        return "others";
    }
}