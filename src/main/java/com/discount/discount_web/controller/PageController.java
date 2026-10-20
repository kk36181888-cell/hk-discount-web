package com.discount.discount_web.controller;

import com.discount.discount_web.model.Discount;
import com.discount.discount_web.repository.DiscountRepository;
import com.discount.discount_web.security.JwtUtil;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Controller
public class PageController {

    @Autowired
    private DiscountRepository discountRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // 🛡️ 從設定檔或雲端環境變數讀取帳號密碼，告別 Hardcode！
    @Value("${daybuy.admin.username:admin}")
    private String adminUsername;

    @Value("${daybuy.admin.password:admin2026}")
    private String rawAdminPassword;

    // 🔒 BCrypt 密碼加密器與加密後嘅 Hash
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private String encodedAdminPassword;

    // 系統啟動時，自動將密碼轉化為不可逆嘅 BCrypt Hash 存入記憶體
    @PostConstruct
    public void initSecurity() {
        this.encodedAdminPassword = passwordEncoder.encode(rawAdminPassword);
    }

    private static final int PAGE_SIZE = 6; 

    // ==========================================
    // 🌐 前台分頁與搜尋路由
    // ==========================================
    @GetMapping("/")
    public String index(@RequestParam(defaultValue = "0") int page, Model model) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        model.addAttribute("discountPage", discountRepository.findAllByOrderByIdDesc(pageable));
        model.addAttribute("currentUrl", "/"); 
        model.addAttribute("searchKeyword", null); 
        return "index"; 
    }

    @GetMapping("/search")
    public String search(@RequestParam String keyword, @RequestParam(defaultValue = "0") int page, Model model) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        model.addAttribute("discountPage", discountRepository.findByTitleContainingIgnoreCaseOrderByIdDesc(keyword, pageable));
        model.addAttribute("currentUrl", "/search");
        model.addAttribute("searchKeyword", keyword); 
        return "index";
    }

    @GetMapping("/supermarket")
    public String supermarket(@RequestParam(defaultValue = "0") int page, Model model) { 
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        model.addAttribute("discountPage", discountRepository.findByCategoryOrderByIdDesc("supermarket", pageable));
        model.addAttribute("currentUrl", "/supermarket");
        model.addAttribute("searchKeyword", null);
        return "index"; 
    }
    
    @GetMapping("/dining")
    public String dining(@RequestParam(defaultValue = "0") int page, Model model) { 
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        model.addAttribute("discountPage", discountRepository.findByCategoryOrderByIdDesc("dining", pageable));
        model.addAttribute("currentUrl", "/dining");
        model.addAttribute("searchKeyword", null);
        return "index"; 
    }
    
    @GetMapping("/warehouse")
    public String warehouse(@RequestParam(defaultValue = "0") int page, Model model) { 
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        model.addAttribute("discountPage", discountRepository.findByCategoryOrderByIdDesc("warehouse", pageable));
        model.addAttribute("currentUrl", "/warehouse");
        model.addAttribute("searchKeyword", null);
        return "index"; 
    }
    
    @GetMapping("/others")
    public String others(@RequestParam(defaultValue = "0") int page, Model model) { 
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        model.addAttribute("discountPage", discountRepository.findByCategoryOrderByIdDesc("others", pageable));
        model.addAttribute("currentUrl", "/others");
        model.addAttribute("searchKeyword", null);
        return "index"; 
    }

    @GetMapping("/faq")
    public String faq() { return "faq"; }

    // ==========================================
    // 📄 詳情頁 (🚀 BI 數據追蹤 + 🌟 多圖畫廊 + ⚡ 極速隨機推介)
    // ==========================================
    @GetMapping("/discount/{id}")
    public String discountDetail(@PathVariable Long id, Model model) {
        discountRepository.incrementViewCount(id);

        Discount discount = discountRepository.findById(id).orElse(null);
        if (discount == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "搵唔到呢個優惠");
        }
        
        List<String> extraImages = new ArrayList<>();
        if (discount.getAdditionalImages() != null && !discount.getAdditionalImages().trim().isEmpty()) {
            String[] urls = discount.getAdditionalImages().split("\\r?\\n");
            for (String url : urls) {
                if (!url.trim().isEmpty()) {
                    extraImages.add(url.trim());
                }
            }
        }
        
        List<Discount> relatedDiscounts = discountRepository.findRandomRelatedDiscounts(id);
        
        model.addAttribute("discount", discount);
        model.addAttribute("extraImages", extraImages);
        model.addAttribute("relatedDiscounts", relatedDiscounts);
        return "detail"; 
    }

    // ==========================================
    // 🛡️ CMS 後台保安系統 (JWT + BCrypt 雜湊驗證)
    // ==========================================
    @GetMapping("/daybuy-hq/login")
    public String loginPage(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "登入失敗，請檢查帳號或密碼。");
        }
        return "login";
    }

    @PostMapping("/daybuy-hq/doLogin")
    public String doLogin(@RequestParam String username, @RequestParam String password, HttpServletResponse response) {
        // 🔒 使用 BCrypt 安全比對密碼 Hash，防範明文洩漏與時序攻擊
        if (adminUsername.equals(username) && passwordEncoder.matches(password, encodedAdminPassword)) {
            String token = jwtUtil.generateToken(username);
            Cookie cookie = new Cookie("DAYBUY_ADMIN_TOKEN", token);
            cookie.setHttpOnly(true); // 防範 JavaScript 竊取 Cookie
            cookie.setPath("/");
            cookie.setMaxAge(7 * 24 * 60 * 60);
            response.addCookie(cookie);
            return "redirect:/daybuy-hq";
        }
        return "redirect:/daybuy-hq/login?error=true";
    }

    @PostMapping("/daybuy-hq/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("DAYBUY_ADMIN_TOKEN", null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/daybuy-hq/login";
    }

    // ==========================================
    // 🚀 CMS 後台操作 (加入 Jsoup 防 XSS 消毒機制)
    // ==========================================
    @GetMapping("/daybuy-hq")
    public String adminPage(Model model) {
        model.addAttribute("discount", new Discount());
        model.addAttribute("discounts", discountRepository.findAllByOrderByIdDesc()); 
        return "admin"; 
    }

    @PostMapping("/daybuy-hq/add")
    public String addDiscount(@ModelAttribute Discount discount) {
        // 🧹 儲存前消毒：過濾所有 <script> 等惡意 XSS 標籤，同時保留 TinyMCE 安全排版與 style 屬性
        if (discount.getDescription() != null) {
            Safelist customSafelist = Safelist.relaxed().addAttributes(":all", "style", "class");
            String safeHtml = Jsoup.clean(discount.getDescription(), customSafelist);
            discount.setDescription(safeHtml);
        }
        
        discountRepository.save(discount);
        return "redirect:/daybuy-hq"; 
    }

    @PostMapping("/daybuy-hq/delete/{id}")
    public String deleteDiscount(@PathVariable Long id) {
        discountRepository.deleteById(id);
        return "redirect:/daybuy-hq"; 
    }

    @PostMapping("/daybuy-hq/delete-all")
    public String deleteAllDiscounts() {
        discountRepository.deleteAll();
        return "redirect:/daybuy-hq"; 
    }
}