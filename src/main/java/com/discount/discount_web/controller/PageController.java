package com.discount.discount_web.controller;

import com.discount.discount_web.model.Discount;
import com.discount.discount_web.repository.DiscountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PageController {

    @Autowired
    private DiscountRepository discountRepository;

    // --- 1. 首頁 (載入全部優惠) ---
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("discounts", discountRepository.findAll());
        return "index"; 
    }

    // --- 2. 管理員後台 (顯示新增優惠表單) - 解決 404 嘅關鍵！ ---
    @GetMapping("/admin")
    public String adminPage(Model model) {
        model.addAttribute("discount", new Discount());
        return "admin"; 
    }

    // --- 3. 管理員後台 (接收並儲存表單資料) ---
    @PostMapping("/admin/add")
    public String addDiscount(@ModelAttribute Discount discount) {
        discountRepository.save(discount);
        return "redirect:/"; // 儲存完自動彈返去首頁
    }

    // --- 4. 分類導航頁面 ---
    @GetMapping("/supermarket")
    public String supermarket() { return "supermarket"; }

    @GetMapping("/dining")
    public String dining() { return "dining"; }

    @GetMapping("/warehouse")
    public String warehouse() { return "warehouse"; }

    @GetMapping("/others")
    public String others() { return "others"; }
}