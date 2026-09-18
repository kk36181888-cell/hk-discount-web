package com.discount.discount_web.controller;

import com.discount.discount_web.model.Discount;
import com.discount.discount_web.repository.DiscountRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class PageController {

    @Autowired
    private DiscountRepository discountRepository;

    private static final String SECRET_KEY = "DayBuy_Super_Secret_Key_2026_xyz";

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("discounts", discountRepository.findAllByOrderByIdDesc());
        return "index"; 
    }

    // 詳情頁
    @GetMapping("/discount/{id}")
    public String discountDetail(@PathVariable Long id, Model model) {
        Discount discount = discountRepository.findById(id).orElse(null);
        if (discount == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "搵唔到呢個優惠");
        }
        model.addAttribute("discount", discount);
        return "detail"; 
    }

    // ==========================================
    // 🚀 後台路徑 (升級 7 日 Cookie 記憶功能)
    // ==========================================
    
    @GetMapping("/daybuy-hq")
    public String adminPage(Model model, 
                            @RequestParam(required = false) String key,
                            @CookieValue(value = "daybuy_admin_pass", defaultValue = "") String adminCookie,
                            HttpServletResponse response) {
        
        // 如果網址有正確嘅 key，就派發一張 VIP 通行證 (Cookie) 畀瀏覽器，有效期 7 日
        if (SECRET_KEY.equals(key)) {
            Cookie cookie = new Cookie("daybuy_admin_pass", SECRET_KEY);
            cookie.setPath("/");
            cookie.setMaxAge(7 * 24 * 60 * 60); // 記住 7 日 (秒數)
            response.addCookie(cookie);
            adminCookie = SECRET_KEY; // 即刻生效
        }

        // 檢查瀏覽器有冇 VIP 通行證
        if (!SECRET_KEY.equals(adminCookie)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Page not found");
        }

        model.addAttribute("discount", new Discount());
        model.addAttribute("discounts", discountRepository.findAllByOrderByIdDesc()); 
        return "admin"; 
    }

    @PostMapping("/daybuy-hq/add")
    public String addDiscount(@ModelAttribute Discount discount, 
                              @CookieValue(value = "daybuy_admin_pass", defaultValue = "") String adminCookie) {
        if (!SECRET_KEY.equals(adminCookie)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        discountRepository.save(discount);
        return "redirect:/daybuy-hq"; 
    }

    @PostMapping("/daybuy-hq/delete/{id}")
    public String deleteDiscount(@PathVariable Long id, 
                                 @CookieValue(value = "daybuy_admin_pass", defaultValue = "") String adminCookie) {
        if (!SECRET_KEY.equals(adminCookie)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        discountRepository.deleteById(id);
        return "redirect:/daybuy-hq"; 
    }

    @PostMapping("/daybuy-hq/delete-all")
    public String deleteAllDiscounts(@CookieValue(value = "daybuy_admin_pass", defaultValue = "") String adminCookie) {
        if (!SECRET_KEY.equals(adminCookie)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        discountRepository.deleteAll();
        return "redirect:/daybuy-hq"; 
    }

    // ==========================================
    // 🌐 前台分類路由
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
    public String faq() { return "faq"; }
}