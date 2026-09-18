package com.discount.discount_web.controller;

import com.discount.discount_web.model.Discount;
import com.discount.discount_web.repository.DiscountRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    // 🌟 設定每頁顯示數量 (6 個啱啱好填滿兩排 3 格)
    private static final int PAGE_SIZE = 6; 

    // ==========================================
    // 🌐 前台分頁路由 (Pagination)
    // ==========================================
    @GetMapping("/")
    public String index(@RequestParam(defaultValue = "0") int page, Model model) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        model.addAttribute("discountPage", discountRepository.findAllByOrderByIdDesc(pageable));
        model.addAttribute("currentUrl", ""); // 記住而家喺邊度，方便分頁按鈕對位
        return "index"; 
    }

    @GetMapping("/supermarket")
    public String supermarket(@RequestParam(defaultValue = "0") int page, Model model) { 
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        model.addAttribute("discountPage", discountRepository.findByCategoryOrderByIdDesc("supermarket", pageable));
        model.addAttribute("currentUrl", "/supermarket");
        return "index"; 
    }
    
    @GetMapping("/dining")
    public String dining(@RequestParam(defaultValue = "0") int page, Model model) { 
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        model.addAttribute("discountPage", discountRepository.findByCategoryOrderByIdDesc("dining", pageable));
        model.addAttribute("currentUrl", "/dining");
        return "index"; 
    }
    
    @GetMapping("/warehouse")
    public String warehouse(@RequestParam(defaultValue = "0") int page, Model model) { 
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        model.addAttribute("discountPage", discountRepository.findByCategoryOrderByIdDesc("warehouse", pageable));
        model.addAttribute("currentUrl", "/warehouse");
        return "index"; 
    }
    
    @GetMapping("/others")
    public String others(@RequestParam(defaultValue = "0") int page, Model model) { 
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        model.addAttribute("discountPage", discountRepository.findByCategoryOrderByIdDesc("others", pageable));
        model.addAttribute("currentUrl", "/others");
        return "index"; 
    }

    @GetMapping("/faq")
    public String faq() { return "faq"; }

    // ==========================================
    // 詳情頁
    // ==========================================
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
    // 🚀 後台路徑 (保持不變)
    // ==========================================
    @GetMapping("/daybuy-hq")
    public String adminPage(Model model, 
                            @RequestParam(required = false) String key,
                            @CookieValue(value = "daybuy_admin_pass", defaultValue = "") String adminCookie,
                            HttpServletResponse response) {
        if (SECRET_KEY.equals(key)) {
            Cookie cookie = new Cookie("daybuy_admin_pass", SECRET_KEY);
            cookie.setPath("/");
            cookie.setMaxAge(7 * 24 * 60 * 60); 
            response.addCookie(cookie);
            adminCookie = SECRET_KEY; 
        }
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
}