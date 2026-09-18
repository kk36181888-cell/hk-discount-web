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

    // 首頁：從資料庫撈取全部優惠並傳送到前端
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("discounts", discountRepository.findAll());
        return "index"; 
    }

    // ==========================================
    // 管理員後台 (解決 404 Error 嘅關鍵)
    // ==========================================
    
    // 1. 顯示新增優惠嘅表單
    @GetMapping("/admin")
    public String adminPage(Model model) {
        model.addAttribute("discount", new Discount());
        return "admin"; 
    }

    // 2. 接收表單資料，並存入 Database
    @PostMapping("/admin/add")
    public String addDiscount(@ModelAttribute Discount discount) {
        discountRepository.save(discount);
        return "redirect:/"; // 成功新增後跳轉返去首頁
    }

    // ==========================================
    // 其他導航頁面
    // ==========================================
    @GetMapping("/supermarket")
    public String supermarket() { return "supermarket"; }

    @GetMapping("/dining")
    public String dining() { return "dining"; }

    @GetMapping("/warehouse")
    public String warehouse() { return "warehouse"; }

    @GetMapping("/others")
    public String others() { return "others"; }
}