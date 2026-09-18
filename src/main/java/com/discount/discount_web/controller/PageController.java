package com.discount.discount_web.controller;

import com.discount.discount_web.model.Discount;
import com.discount.discount_web.repository.DiscountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/admin")
    public String adminPage(Model model) {
        // 準備一個空物件畀「新增表單」
        model.addAttribute("discount", new Discount());
        // 準備 Database 所有資料畀「刪除清單」
        model.addAttribute("discounts", discountRepository.findAll()); 
        return "admin"; 
    }

    @PostMapping("/admin/add")
    public String addDiscount(@ModelAttribute Discount discount) {
        discountRepository.save(discount);
        return "redirect:/admin"; // 加完之後留返喺後台
    }

    @PostMapping("/admin/delete/{id}")
    public String deleteDiscount(@PathVariable Long id) {
        discountRepository.deleteById(id);
        return "redirect:/admin"; // 刪完之後留返喺後台
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