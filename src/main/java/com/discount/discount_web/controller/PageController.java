package com.discount.discount_web.controller;

import com.discount.discount_web.model.Discount;
import com.discount.discount_web.repository.DiscountRepository;
import com.discount.discount_web.security.JwtUtil;
import com.discount.discount_web.service.CloudinaryService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Controller
public class PageController {

    @Autowired
    private DiscountRepository discountRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // ☁️ 1. 補返注入 Cloudinary 雲端圖床服務
    @Autowired
    private CloudinaryService cloudinaryService;

    @Value("${daybuy.admin.username:admin}")
    private String adminUsername;

    @Value("${daybuy.admin.password:admin2026}")
    private String rawAdminPassword;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private String encodedAdminPassword;

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
    // 📄 詳情頁
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
    // 🛡️ CMS 後台保安系統 (JWT + BCrypt + Cookie 加固)
    // ==========================================
    @GetMapping("/daybuy-hq/login")
    public String loginPage(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "登入失敗，請檢查帳號或密碼。");
        }
        return "login";
    }

    @PostMapping("/daybuy-hq/doLogin")
    public String doLogin(@RequestParam String username, @RequestParam String password, 
                          HttpServletRequest request, HttpServletResponse response) {
        if (adminUsername.equals(username) && passwordEncoder.matches(password, encodedAdminPassword)) {
            String token = jwtUtil.generateToken(username);
            Cookie cookie = new Cookie("DAYBUY_ADMIN_TOKEN", token);
            cookie.setHttpOnly(true); // 防範 XSS 竊取 Cookie
            cookie.setPath("/");
            // 🛠️ 2. 修復返呢行嘅語法錯誤 (7日 = 604800秒)
            cookie.setMaxAge(7 * 24 * 60 * 60);

            // 🛡️ 智能判斷：喺 Render (HTTPS) 自動啟用 Secure，喺 Localhost (HTTP) 亦能正常測試
            boolean isHttps = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
            cookie.setSecure(isHttps);
            cookie.setAttribute("SameSite", "Lax"); // 防範 CSRF 跨站請求偽造攻擊

            response.addCookie(cookie);
            return "redirect:/daybuy-hq";
        }
        return "redirect:/daybuy-hq/login?error=true";
    }

    @PostMapping("/daybuy-hq/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("DAYBUY_ADMIN_TOKEN", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/daybuy-hq/login";
    }

    // ==========================================
    // 🚀 CMS 後台操作 (支援 Cloudinary 上載、編輯、刪除與 XSS 消毒)
    // ==========================================
    @GetMapping("/daybuy-hq")
    public String adminPage(Model model) {
        model.addAttribute("discount", new Discount()); // 預設空白表單 (新增模式)
        model.addAttribute("discounts", discountRepository.findAllByOrderByIdDesc()); 
        return "admin"; 
    }

    @GetMapping("/daybuy-hq/edit/{id}")
    public String editDiscountPage(@PathVariable Long id, Model model) {
        Discount existingDiscount = discountRepository.findById(id).orElse(null);
        if (existingDiscount == null) {
            return "redirect:/daybuy-hq";
        }
        model.addAttribute("discount", existingDiscount); // 將舊資料塞入表單
        model.addAttribute("discounts", discountRepository.findAllByOrderByIdDesc());
        return "admin";
    }

    // 🌟 3. 升級版：同時支援「Cloudinary 直接揀相上載」與「手動貼 Link」
    @PostMapping("/daybuy-hq/add")
    public String saveOrUpdateDiscount(
            @ModelAttribute Discount discount,
            @RequestParam(value = "coverImageFile", required = false) MultipartFile coverImageFile,
            @RequestParam(value = "extraImageFiles", required = false) MultipartFile[] extraImageFiles) {

        try {
            // ☁️ 處理封面圖片：如果有揀檔案，直接 Upload 去 Cloudinary 並覆蓋 imageUrl
            if (coverImageFile != null && !coverImageFile.isEmpty()) {
                String uploadedCoverUrl = cloudinaryService.uploadImage(coverImageFile);
                discount.setImageUrl(uploadedCoverUrl);
            }

            // ☁️ 處理額外多張單張：支援一次過揀幾張圖批量 Upload
            if (extraImageFiles != null && extraImageFiles.length > 0) {
                List<String> uploadedExtraUrls = new ArrayList<>();
                if (discount.getAdditionalImages() != null && !discount.getAdditionalImages().trim().isEmpty()) {
                    uploadedExtraUrls.add(discount.getAdditionalImages().trim());
                }
                for (MultipartFile file : extraImageFiles) {
                    if (file != null && !file.isEmpty()) {
                        uploadedExtraUrls.add(cloudinaryService.uploadImage(file));
                    }
                }
                discount.setAdditionalImages(String.join("\n", uploadedExtraUrls));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "圖片上載去 Cloudinary 失敗，請檢查 API Key 設定：" + e.getMessage());
        }

        // 🧹 Jsoup 防 XSS 消毒
        if (discount.getDescription() != null) {
            Safelist customSafelist = Safelist.relaxed().addAttributes(":all", "style", "class");
            String safeHtml = Jsoup.clean(discount.getDescription(), customSafelist);
            discount.setDescription(safeHtml);
        }

        // 📊 如果係編輯舊優惠，保留原本嘅 ViewCount 瀏覽量
        if (discount.getId() != null) {
            Discount existing = discount