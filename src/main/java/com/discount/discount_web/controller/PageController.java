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
        model.addAttribute("discount", new Discount());
        model.addAttribute("discounts", discountRepository.findAll()); 
        return "admin"; 
    }

    @PostMapping("/admin/add")
    public String addDiscount(@ModelAttribute Discount discount) {
        discountRepository.save(discount);
        return "redirect:/admin"; 
    }

    @PostMapping("/admin/delete/{id}")
    public String deleteDiscount(@PathVariable Long id) {
        discountRepository.deleteById(id);
        return "redirect:/admin"; 
    }

    @GetMapping("/supermarket")
    public String supermarket(Model model) { 
        model.addAttribute("discounts", discountRepository.findByCategory("supermarket"));
        return "index"; 
    }

    @GetMapping("/dining")
    public String dining(Model model) { 
        model.addAttribute("discounts", discountRepository.findByCategory("dining"));
        return "index"; 
    }

    @GetMapping("/warehouse")
    public String warehouse(Model model) { 
        model.addAttribute("discounts", discountRepository.findByCategory("warehouse"));
        return "index"; 
    }

    @GetMapping("/others")
    public String others(Model model) { 
        model.addAttribute("discounts", discountRepository.findByCategory("others"));
        return "index"; 
    }

    // 新增：常見問題頁面導航
    @GetMapping("/faq")
    public String faq() { 
        return "faq"; 
    }
}