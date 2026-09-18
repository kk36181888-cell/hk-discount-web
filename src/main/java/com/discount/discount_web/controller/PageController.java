package com.discount.discount_web.controller;

import com.discount.discount_web.model.Discount;
import com.discount.discount_web.repository.DiscountRepository;
import com.discount.discount_web.service.ScraperService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class PageController {

    @Autowired
    private DiscountRepository discountRepository;

    @Autowired
    private ScraperService scraperService;

    // 🔐 專屬私密鑰匙
    private static final String SECRET_KEY = "DayBuy_Super_Secret_Key_2026_xyz";

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("discounts", discountRepository.findAllByOrderByIdDesc());
        return "index"; 
    }

    // ==========================================
    // 🛡️ 嚴密保護嘅後台路由
    // ==========================================

    @GetMapping("/admin")
    public String adminPage(Model model, HttpSession session, @RequestParam(required = false) String key) {
        // 1. 如果網址帶有正確嘅私密 key，自動授予管理員權限 (Session)
        if (SECRET_KEY.equals(key)) {
            session.setAttribute("isAdmin", true);
        }

        // 2. 如果未獲授權，直接回傳 404 Not Found (隱形防護)
        if (session.getAttribute("isAdmin") == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Page not found");
        }

        model.addAttribute("discount", new Discount());
        model.addAttribute("discounts", discountRepository.findAllByOrderByIdDesc()); 
        return "admin"; 
    }

    @PostMapping("/admin/add")
    public String addDiscount(@ModelAttribute Discount discount, HttpSession session) {
        if (session.getAttribute("isAdmin") == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        discountRepository.save(discount);
        return "redirect:/admin"; 
    }

    // 呢段就係你頭先可能唔小心洗咗嘅「單獨刪除」功能！
    @PostMapping("/admin/delete/{id}")
    public String deleteDiscount(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("isAdmin") == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        discountRepository.deleteById(id);
        return "redirect:/admin"; 
    }

    // 呢段係最新加嘅「一鍵清空」大絕招
    @PostMapping("/admin/delete-all")
    public String deleteAllDiscounts(HttpSession session) {
        if (session.getAttribute("isAdmin") == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        discountRepository.deleteAll();
        return "redirect:/admin"; 
    }

    @PostMapping("/admin/scrape")
    public String runScraper(HttpSession session) {
        if (session.getAttribute("isAdmin") == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        scraperService.scrapeDiscounts();
        return "redirect:/admin"; 
    }

    // ==========================================
    // 🌐 前台公開路由
    // ==========================================

    @GetMapping("/supermarket")
    public String supermarket(Model model) { 
        model.addAttribute("discounts", discountRepository.findByCategoryOrderByIdDesc("supermarket"));
        return "index"; 
    }

    @GetMapping("/dining")
    public String dining(Model model) { 
        model.addAttribute("discounts", discountRepository.findByCategoryOrderByIdDesc("dining"));
        return "index"; 
    }

    @GetMapping("/warehouse")
    public String warehouse(Model model) { 
        model.addAttribute("discounts", discountRepository.findByCategoryOrderByIdDesc("warehouse"));
        return "index"; 
    }

    @GetMapping("/others")
    public String others(Model model) { 
        model.addAttribute("discounts", discountRepository.findByCategoryOrderByIdDesc("others"));
        return "index"; 
    }

    @GetMapping("/faq")
    public String faq() { 
        return "faq"; 
    }
}