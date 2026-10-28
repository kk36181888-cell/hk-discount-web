package com.discount.discount_web.controller;

import com.discount.discount_web.model.ContactMessage;
import com.discount.discount_web.model.Discount;
import com.discount.discount_web.repository.ContactMessageRepository;
import com.discount.discount_web.repository.DiscountRepository;
import com.discount.discount_web.security.JwtUtil;
import com.discount.discount_web.service.CloudinaryService;
import com.discount.discount_web.service.ImageRehostService;
import com.discount.discount_web.service.SearchService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class PageController {

    @Autowired
    private DiscountRepository discountRepository;

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private ImageRehostService imageRehostService;

    @Autowired
    private SearchService searchService;

    @Value("${daybuy.admin.username}")
    private String adminUsername;

    @Value("${daybuy.admin.password}")
    private String rawAdminPassword;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private String encodedAdminPassword;

    // 🛡️ 聯絡表單限流：同一 IP 每小時最多 10 次提交，擋洗版式灌水。
    // 正常訪客極少一小時內留超過兩次言，10 次係寬鬆上限；蜜罐已先擋 naive bot，
    // 呢度擋嘅係識避蜜罐但用同一 IP 狂 submit 嘅 flood。
    private static final int MAX_CONTACT_PER_HOUR = 10;
    private static final long CONTACT_WINDOW_MS = 3600_000L;
    private static final Map<String, ArrayDeque<Long>> contactSubmissions = new ConcurrentHashMap<>();

    private static String contactClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static boolean allowContact(String ip) {
        long now = System.currentTimeMillis();
        synchronized (contactSubmissions) {
            ArrayDeque<Long> times = contactSubmissions.computeIfAbsent(ip, k -> new ArrayDeque<>());
            while (!times.isEmpty() && now - times.peekFirst() > CONTACT_WINDOW_MS) {
                times.pollFirst();
            }
            if (times.size() >= MAX_CONTACT_PER_HOUR) {
                return false;
            }
            times.addLast(now);
            return true;
        }
    }

    // 🛡️ 登入限流：同一 IP 連續 5 次失敗就鎖 15 分鐘，擋持續在線猜密碼
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOGIN_LOCK_MS = 15 * 60 * 1000L;
    private final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();

    private static class LoginAttempt {
        int failures = 0;
        long lockUntil = 0;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostConstruct
    public void initSecurity() {
        this.encodedAdminPassword = passwordEncoder.encode(rawAdminPassword);
    }

    // 首頁／分類／搜尋每頁顯示張數：2026-10-26 由 6 改做 18（全站上架 17 張時首頁第一頁就睇晒，唔使再分 3 頁）
    private static final int PAGE_SIZE = 18;

    // 🔥 排序參數正規化：只認 latest / hot，其他一律當 latest
    private String normalizeSort(String sort) {
        return "hot".equals(sort) ? "hot" : "latest";
    } 

    // ==========================================
    // 🌐 前台分頁與搜尋路由
    // ==========================================
    @GetMapping("/")
    public String index(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "latest") String sort, Model model) {
        sort = normalizeSort(sort);
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        model.addAttribute("discountPage", "hot".equals(sort) ? discountRepository.findAllByOrderByViewCountDesc(pageable) : discountRepository.findAllByOrderByIdDesc(pageable));
        model.addAttribute("currentUrl", "/");
        model.addAttribute("searchKeyword", null);
        model.addAttribute("sort", sort);
        return "index";
    }

    @GetMapping("/search")
    public String search(@RequestParam String keyword, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "latest") String sort, Model model) {
        sort = normalizeSort(sort);
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        // 🔍 搜尋改行 SearchService：多欄位＋同義詞＋分類詞，唔再淨係逐字對標題同描述
        model.addAttribute("discountPage", searchService.search(keyword, sort, pageable));
        model.addAttribute("currentUrl", "/search");
        model.addAttribute("searchKeyword", keyword);
        model.addAttribute("sort", sort);
        return "index";
    }

    @GetMapping("/supermarket")
    public String supermarket(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "latest") String sort, Model model) {
        sort = normalizeSort(sort);
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        model.addAttribute("discountPage", "hot".equals(sort) ? discountRepository.findByCategoryOrderByViewCountDesc("supermarket", pageable) : discountRepository.findByCategoryOrderByIdDesc("supermarket", pageable));
        model.addAttribute("currentUrl", "/supermarket");
        model.addAttribute("searchKeyword", null);
        model.addAttribute("sort", sort);
        return "index";
    }
    
    @GetMapping("/dining")
    public String dining(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "latest") String sort, Model model) {
        sort = normalizeSort(sort);
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        model.addAttribute("discountPage", "hot".equals(sort) ? discountRepository.findByCategoryOrderByViewCountDesc("dining", pageable) : discountRepository.findByCategoryOrderByIdDesc("dining", pageable));
        model.addAttribute("currentUrl", "/dining");
        model.addAttribute("searchKeyword", null);
        model.addAttribute("sort", sort);
        return "index";
    }
    
    @GetMapping("/warehouse")
    public String warehouse(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "latest") String sort, Model model) {
        sort = normalizeSort(sort);
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        model.addAttribute("discountPage", "hot".equals(sort) ? discountRepository.findByCategoryOrderByViewCountDesc("warehouse", pageable) : discountRepository.findByCategoryOrderByIdDesc("warehouse", pageable));
        model.addAttribute("currentUrl", "/warehouse");
        model.addAttribute("searchKeyword", null);
        model.addAttribute("sort", sort);
        return "index";
    }
    
    @GetMapping("/others")
    public String others(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "latest") String sort, Model model) {
        sort = normalizeSort(sort);
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        model.addAttribute("discountPage", "hot".equals(sort) ? discountRepository.findByCategoryOrderByViewCountDesc("others", pageable) : discountRepository.findByCategoryOrderByIdDesc("others", pageable));
        model.addAttribute("currentUrl", "/others");
        model.addAttribute("searchKeyword", null);
        model.addAttribute("sort", sort);
        return "index";
    }

    @GetMapping("/faq")
    public String faq() { return "faq"; }

    // ==========================================
    // 📄 資訊頁（2026-10-27 新增：私隱政策／關於我們／聯絡我們／著數攻略）
    // ==========================================
    @GetMapping("/privacy")
    public String privacy() { return "privacy"; }

    @GetMapping("/about")
    public String about() { return "about"; }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("sent", false);
        return "contact";
    }

    @PostMapping("/contact")
    public String submitContact(@RequestParam(required = false) String name,
                                @RequestParam(required = false) String contact,
                                @RequestParam(required = false) String subject,
                                @RequestParam(required = false) String message,
                                @RequestParam(required = false) String website,
                                HttpServletRequest request,
                                Model model) {
        // 🛡️ 蜜罐欄：正常訪客唔會填 website 呢格，填咗就當機械人，直接當成功但唔儲存。
        // 蜜罐提交唔計入限流，免得 bot 用假提交塞爆同一 IP 嘅額度。
        if (website != null && !website.isBlank()) {
            model.addAttribute("sent", true);
            return "contact";
        }
        // 🛡️ 限流：超額就當表單錯誤退回，唔儲存，訪客遲啲再試就得。
        if (!allowContact(contactClientIp(request))) {
            model.addAttribute("sent", false);
            model.addAttribute("error", true);
            model.addAttribute("formName", name);
            model.addAttribute("formContact", contact);
            model.addAttribute("formSubject", subject);
            model.addAttribute("formMessage", message);
            return "contact";
        }
        String cleanSubject = subject == null ? "" : subject.trim();
        String cleanMessage = message == null ? "" : message.trim();
        if (cleanSubject.isEmpty() || cleanMessage.isEmpty()) {
            model.addAttribute("sent", false);
            model.addAttribute("error", true);
            model.addAttribute("formName", name);
            model.addAttribute("formContact", contact);
            model.addAttribute("formSubject", subject);
            model.addAttribute("formMessage", message);
            return "contact";
        }
        ContactMessage cm = new ContactMessage();
        cm.setName(name == null ? null : name.trim());
        cm.setContact(contact == null ? null : contact.trim());
        cm.setSubject(cleanSubject.length() > 200 ? cleanSubject.substring(0, 200) : cleanSubject);
        cm.setMessage(cleanMessage.length() > 2000 ? cleanMessage.substring(0, 2000) : cleanMessage);
        contactMessageRepository.save(cm);
        model.addAttribute("sent", true);
        return "contact";
    }

    @GetMapping("/guides")
    public String guides() { return "guides"; }

    @GetMapping("/guides/yuu-stamps")
    public String guideYuu() { return "guide-yuu"; }

    @GetMapping("/guides/pns-codes")
    public String guidePns() { return "guide-pns"; }

    @GetMapping("/guides/supermarket-tips")
    public String guideTips() { return "guide-tips"; }

    // ==========================================
    // 🏪 商戶專頁
    // ==========================================
    @GetMapping("/merchants")
    public String merchants(Model model) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Discount d : discountRepository.findAllPublished()) {
            String m = d.getMerchantDisplay();
            if (m != null) { counts.put(m, counts.getOrDefault(m, 0) + 1); }
        }
        List<Map.Entry<String, Integer>> merchantList = new ArrayList<>(counts.entrySet());
        merchantList.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        model.addAttribute("merchantList", merchantList);
        model.addAttribute("currentUrl", "/merchants");
        return "merchants";
    }

    @GetMapping("/merchant/{name}")
    public String merchant(@PathVariable String name, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "latest") String sort, Model model) {
        sort = normalizeSort(sort);
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Page<Discount> result = "hot".equals(sort) ? discountRepository.findByMerchantOrTitleOrderByViewCountDesc(name, pageable) : discountRepository.findByMerchantOrTitle(name, pageable);
        model.addAttribute("discountPage", result);
        model.addAttribute("currentUrl", "/merchant/" + name);
        model.addAttribute("searchKeyword", null);
        model.addAttribute("sort", sort);
        model.addAttribute("merchantName", name);
        return "index";
    }

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
        String ip = clientIp(request);
        LoginAttempt attempt = loginAttempts.computeIfAbsent(ip, k -> new LoginAttempt());
        if (System.currentTimeMillis() < attempt.lockUntil) {
            System.out.println("🛡️ 登入已鎖定（多次失敗），IP: " + ip);
            return "redirect:/daybuy-hq/login?error=true";
        }
        if (adminUsername.equals(username) && passwordEncoder.matches(password, encodedAdminPassword)) {
            loginAttempts.remove(ip);
            String token = jwtUtil.generateToken(username);
            Cookie cookie = new Cookie("DAYBUY_ADMIN_TOKEN", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(7 * 24 * 60 * 60);

            boolean isHttps = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
            cookie.setSecure(isHttps);
            cookie.setAttribute("SameSite", "Lax");

            response.addCookie(cookie);
            return "redirect:/daybuy-hq";
        }
        attempt.failures++;
        if (attempt.failures >= MAX_LOGIN_ATTEMPTS) {
            attempt.lockUntil = System.currentTimeMillis() + LOGIN_LOCK_MS;
            attempt.failures = 0;
            System.out.println("🛡️ 登入多次失敗，已鎖定 IP 15 分鐘: " + ip);
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
        model.addAttribute("discount", new Discount());
        model.addAttribute("discounts", discountRepository.findAllByOrderByIdDesc());
        model.addAttribute("pendingCount", discountRepository.countByStatus("PENDING"));
        return "admin";
    }

    // ==========================================
    // 🤖 自動入貨待審清單 (PENDING 草稿：批准先出街)
    // ==========================================
    @GetMapping("/daybuy-hq/review")
    public String reviewPage(Model model) {
        model.addAttribute("pendingDiscounts", discountRepository.findByStatusOrderByScrapedAtDesc("PENDING"));
        return "review";
    }

    @PostMapping("/daybuy-hq/review/approve/{id}")
    public String approveDiscount(@PathVariable Long id) {
        Discount d = discountRepository.findById(id).orElse(null);
        if (d != null) {
            // 🖼️ 斷尾：批准出街之前，先將未轉存嘅外部圖（例如 fbcdn）轉入 Cloudinary；
            // 轉唔到嘅圖會保留原網址，唔會阻住批准。
            int moved = imageRehostService.rehostDiscount(d);
            if (moved > 0) {
                System.out.println("🖼️ 批准 ID " + id + " 前轉存咗 " + moved + " 張圖入 Cloudinary");
            }
            // 🛡️ 批准時都行一次描述清洗：唔經新增／編輯表單入庫嘅內容（例如 ingest）都唔可以原樣上線
            d.setDescription(sanitizeDescription(d.getDescription()));
            d.setStatus("PUBLISHED");
            discountRepository.save(d);
        }
        return "redirect:/daybuy-hq/review";
    }

    @PostMapping("/daybuy-hq/review/reject/{id}")
    public String rejectDiscount(@PathVariable Long id) {
        discountRepository.deleteById(id);
        return "redirect:/daybuy-hq/review";
    }

    @GetMapping("/daybuy-hq/edit/{id}")
    public String editDiscountPage(@PathVariable Long id, Model model) {
        Discount existingDiscount = discountRepository.findById(id).orElse(null);
        if (existingDiscount == null) {
            return "redirect:/daybuy-hq";
        }
        model.addAttribute("discount", existingDiscount); 
        model.addAttribute("discounts", discountRepository.findAllByOrderByIdDesc());
        return "admin";
    }

    @PostMapping("/daybuy-hq/add")
    public String saveOrUpdateDiscount(
            @ModelAttribute Discount discount,
            @RequestParam(value = "coverImageFile", required = false) MultipartFile coverImageFile,
            @RequestParam(value = "extraImageFiles", required = false) MultipartFile[] extraImageFiles) {

        try {
            if (coverImageFile != null && !coverImageFile.isEmpty()) {
                String uploadedCoverUrl = cloudinaryService.uploadImage(coverImageFile);
                discount.setImageUrl(uploadedCoverUrl);
            }

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
            // ⚠️ 2026-10-23 修復：Cloudinary 從未設定過帳號 (Render 環境冇 cloudinary 變數，
            // 一直用緊程式嘅 placeholder 值)，上載必然失敗；之前 catch 完照 throw 500，
            // 搞到成條優惠入唔到 (2026-10-23 後台入 OSAMU GOODS 優惠實測中招)。
            // 改為：上載失敗只記低，保留表單填嘅圖片網址繼續存檔，唔好成單死火。
            // (封面若已上載成功先失敗，imageUrl 已 set 好；附加圖未完成就保留原值。)
            System.out.println("⚠️ Cloudinary 圖片上載失敗，改用表單圖片網址存檔: " + e.getMessage());
        }

        // 🖼️ 斷尾：檔案上載完之後，將表單網址欄入面嘅外部圖（例如 fbcdn）
        // 即時轉存入 Cloudinary；已經係 Cloudinary 嘅圖會跳過，轉唔到嘅保留原網址。
        // 咁之後無論人手定排程入嘅草稿，一出世就已經係永久圖，唔使事後逐批轉。
        imageRehostService.rehostDiscount(discount);

        // 🛡️ 只接受 http／https 圖片同來源連結，擋 javascript: 之類 scheme
        discount.setImageUrl(onlyHttpUrl(discount.getImageUrl()));
        discount.setSourceUrl(onlyHttpUrl(discount.getSourceUrl()));
        if (discount.getAdditionalImages() != null && !discount.getAdditionalImages().isBlank()) {
            List<String> keptUrls = new ArrayList<>();
            for (String line : discount.getAdditionalImages().split("\n")) {
                String keptUrl = onlyHttpUrl(line);
                if (keptUrl != null) {
                    keptUrls.add(keptUrl);
                }
            }
            discount.setAdditionalImages(String.join("\n", keptUrls));
        }

        discount.setDescription(sanitizeDescription(discount.getDescription()));

        if (discount.getId() != null) {
            Discount existing = discountRepository.findById(discount.getId()).orElse(null);
            if (existing != null) {
                discount.setViewCount(existing.getViewCount());
                // 🤖 表單冇嘅欄位要保留原值，唔好俾編輯一次就洗走入貨來源資料/狀態
                if (discount.getStatus() == null) {
                    discount.setStatus(existing.getStatus());
                }
                // 來源名稱/連結而家後台表單有得改，唔好再用舊值覆蓋；去重鍵同抓取時間冇表單欄位，繼續保留原值
                discount.setSourceKey(existing.getSourceKey());
                discount.setScrapedAt(existing.getScrapedAt());
            }
        }
        if (discount.getStatus() == null) {
            discount.setStatus("PUBLISHED");
        }
        
        discountRepository.save(discount);
        return "redirect:/daybuy-hq"; 
    }

    // 🛡️ 描述清洗 choke point：新增／編輯／批准全部行同一套 Jsoup 清洗，唔好俾惡意 HTML 經任何路徑上線
    private String sanitizeDescription(String html) {
        if (html == null) {
            return null;
        }
        Safelist customSafelist = Safelist.relaxed().addAttributes(":all", "style", "class");
        return Jsoup.clean(html, customSafelist);
    }

    // 🛡️ 只接受 http／https，否則返 null（擋 javascript: 等危險 scheme）
    private String onlyHttpUrl(String url) {
        if (url == null) {
            return null;
        }
        String u = url.trim();
        if (u.isEmpty()) {
            return null;
        }
        if (u.startsWith("http://") || u.startsWith("https://")) {
            return u;
        }
        return null;
    }

    // 🖼️ 一鍵遷移：將所有已存優惠入面仲係外部網址嘅圖一次過轉入 Cloudinary。
    // 已經係 Cloudinary 嘅圖會跳過；轉唔到嘅保留原網址。只改圖片網址，唔改其他欄位。
    // （後台管理員先入到嚟，/daybuy-hq/** 本身已經受管理員登入保護。）
    @PostMapping("/daybuy-hq/rehost-all-images")
    public String rehostAllImages() {
        int deals = 0;
        int images = 0;
        for (Discount d : discountRepository.findAll()) {
            int moved = imageRehostService.rehostDiscount(d);
            if (moved > 0) {
                discountRepository.save(d);
                deals++;
                images += moved;
            }
        }
        System.out.println("🖼️ 一鍵轉圖完成：轉咗 " + deals + " 張優惠、" + images + " 張圖入 Cloudinary");
        return "redirect:/daybuy-hq?rehosted=" + deals + "&rehostedImages=" + images;
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