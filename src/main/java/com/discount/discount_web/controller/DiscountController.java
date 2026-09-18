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

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("discounts", discountRepository.findAll());
        return "index"; 
    }

    // ----------------------------------------------------
    // 管理員後台：顯示新增優惠嘅表單
    // ----------------------------------------------------
    @GetMapping("/admin")
    public String adminPage(Model model) {
        // 準備一個空嘅 Discount 物件畀前端表單填寫
        model.addAttribute("discount", new Discount());
        return "admin"; 
    }

    // ----------------------------------------------------
    // 接收表單資料，並存入 Database
    // ----------------------------------------------------
    @PostMapping("/admin/add")
    public String addDiscount(@ModelAttribute Discount discount) {
        // 呢行一出，Spring Data JPA 就會自動生成 INSERT SQL 將資料寫入 Supabase
        discountRepository.save(discount);
        // 儲存成功後，自動跳轉返去首頁 (即刻睇到新加嘅優惠)
        return "redirect:/"; 
    }

    @GetMapping("/supermarket")
    public String supermarket() { return "supermarket"; }

    @GetMapping("/dining")
    public String dining() { return "dining"; }

    @GetMapping("/warehouse")
    public String warehouse() { return "warehouse"; }

    @GetMapping("/others")
    public String others() { return "others"; }
}