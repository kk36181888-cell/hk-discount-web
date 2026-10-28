package com.discount.discount_web.service;

import com.discount.discount_web.model.Discount;
import com.discount.discount_web.repository.DiscountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 🔍 搜尋服務（2026-10-28）：解決「逐字對標題＋描述」搵唔到嘢嘅問題。
 *
 * 以前：淨係標題 OR 描述 LIKE %關鍵字%，所以搵「漢堡」會 miss 晒
 * 標題寫「安格斯」「雞扒包」嘅優惠。
 *
 * 而家三層擴展：
 * 1. 欄位擴展：標題、描述、商戶、條款撮要、來源名一齊對。
 * 2. 同義詞擴展：通用詞對應站內實際出現嘅產品寫法（下便 SYNONYMS，越加越準）。
 * 3. 分類詞：餐飲／超市／倉庫／家居／其他 直接對返分類碼。
 *
 * 實作取捨：先用 findAllPublished() 拎前台可見嘅全部優惠再喺 Java 入面過濾，
 * 而唔係逐個同義詞打 JPQL。站內得幾百張優惠，呢個做法夠快又易維護；
 * 如果將來優惠過幾千張，就要改做數據庫全文檢索。
 */
@Service
public class SearchService {

    @Autowired
    private DiscountRepository discountRepository;

    /** 通用詞 → 站內實際產品寫法（key 同 value 全部用小寫比較）。 */
    private static final Map<String, List<String>> SYNONYMS = new LinkedHashMap<>();

    /** 中文分類詞 → 分類碼。 */
    private static final Map<String, String> CATEGORY_WORDS = new LinkedHashMap<>();

    static {
        SYNONYMS.put("漢堡", List.of("漢堡包", "安格斯", "雞扒包", "麥香雞", "芝士堡", "堡"));
        SYNONYMS.put("咖啡", List.of("咖啡", "拿鐵", "美式", "摩卡", "latte"));
        SYNONYMS.put("雞翼", List.of("雞翼", "雞翅"));
        SYNONYMS.put("雪糕", List.of("雪糕", "冰淇淋", "雪條", "霜淇淋"));
        SYNONYMS.put("披薩", List.of("披薩", "批薩", "pizza", "芝心批"));
        SYNONYMS.put("尿片", List.of("尿片", "紙尿片", "紙尿褲", "尿褲"));
        SYNONYMS.put("奶粉", List.of("奶粉", "配方奶粉"));
        SYNONYMS.put("早餐", List.of("早餐", "早市"));
        SYNONYMS.put("午餐", List.of("午餐", "午市"));
        SYNONYMS.put("下午茶", List.of("下午茶", "茶市"));
        SYNONYMS.put("晚餐", List.of("晚餐", "晚市"));
        SYNONYMS.put("汽水", List.of("汽水", "可樂", "梳打", "soda"));

        CATEGORY_WORDS.put("餐飲", "dining");
        CATEGORY_WORDS.put("超市", "supermarket");
        CATEGORY_WORDS.put("便利店", "supermarket");
        CATEGORY_WORDS.put("倉庫", "warehouse");
        CATEGORY_WORDS.put("家居", "warehouse");
        CATEGORY_WORDS.put("其他", "others");
    }

    public Page<Discount> search(String keyword, String sort, Pageable pageable) {
        String q = keyword == null ? "" : keyword.trim().toLowerCase();
        List<String> terms = new ArrayList<>();
        terms.add(q);
        terms.addAll(SYNONYMS.getOrDefault(q, List.of()));
        String categoryCode = CATEGORY_WORDS.get(q);

        List<Discount> matched = new ArrayList<>();
        for (Discount d : discountRepository.findAllPublished()) {
            if (matches(d, terms, categoryCode)) {
                matched.add(d);
            }
        }

        if ("hot".equals(sort)) {
            matched.sort(Comparator.comparingInt(Discount::getViewCount).reversed()
                    .thenComparing(Comparator.comparing(Discount::getId).reversed()));
        } else {
            matched.sort(Comparator.comparing(Discount::getId).reversed());
        }

        int total = matched.size();
        int start = (int) Math.min(pageable.getOffset(), total);
        int end = Math.min(start + pageable.getPageSize(), total);
        return new PageImpl<>(matched.subList(start, end), pageable, total);
    }

    private boolean matches(Discount d, List<String> terms, String categoryCode) {
        if (categoryCode != null && categoryCode.equals(d.getCategory())) {
            return true;
        }
        String haystack = joinLower(d.getTitle(), d.getDescription(), d.getMerchant(), d.getTerms(), d.getSourceName());
        for (String term : terms) {
            if (!term.isEmpty() && haystack.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String joinLower(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null) {
                sb.append(p.toLowerCase()).append(' ');
            }
        }
        return sb.toString();
    }
}
