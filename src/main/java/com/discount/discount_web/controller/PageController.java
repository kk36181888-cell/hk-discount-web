package com.discount.discount_web.controller;

import com.discount.discount_web.model.Discount;
import com.discount.discount_web.repository.DiscountRepository;
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

    private static final String SECRET_KEY = "DayBuy_Super_Secret_Key_2026_xyz";

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("discounts", discountRepository.findAllByOrderByIdDesc());
        return "index"; 
    }

    // 🆕 新增：專屬優惠詳情頁面路由
    @GetMapping("/discount/{id}")
    public String discountDetail(@PathVariable Long id, Model model) {
        // 喺資料庫搵呢個 ID 嘅優惠出嚟
        Discount discount = discountRepository.findById(id).orElse(null);
        if (discount == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "搵唔到呢個優惠");
        }
        model.addAttribute("discount", discount);
        return "detail"; // 呼叫 detail.html 顯示畫面
    }

    // 🚀 後台路徑
    @GetMapping("/daybuy-hq")
    public String adminPage(Model model, HttpSession session, @RequestParam(required = false) String key) {
        if (SECRET_KEY.equals(key)) {
            session.setAttribute("isAdmin", true);
        }
        if (session.getAttribute("isAdmin") == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Page not found");
        }
        model.addAttribute("discount", new Discount());
        model.addAttribute("discounts", discountRepository.findAllByOrderByIdDesc()); 
        return "admin"; 
    }

    @PostMapping("/daybuy-hq/add")
    public String addDiscount(@ModelAttribute Discount discount, HttpSession session) {
        if (session.getAttribute("isAdmin") == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        discountRepository.save(discount);
        return "redirect:/daybuy-hq"; 
    }

    @PostMapping("/daybuy-hq/delete/{id}")
    public String deleteDiscount(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("isAdmin") == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        discountRepository.deleteById(id);
        return "redirect:/daybuy-hq"; 
    }

    @PostMapping("/daybuy-hq/delete-all")
    public String deleteAllDiscounts(HttpSession session) {
        if (session.getAttribute("isAdmin") == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        discountRepository.deleteAll();
        return "redirect:/daybuy-hq"; 
    }

    // --- 前台路由 ---
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