package com.discount.discount_web.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 🏪 商戶名正規化：同一個真實商戶喺唔同草稿入面寫法唔同（例如 JHC / JHC 真好城、
 * 百佳 / 百佳 PARKnSHOP），前台會分裂成兩個入口，撳一個名睇唔晒全部優惠。
 * 呢度將所有已知變體對應去同一個 canonical 名；未知商戶名照原樣返，唔好亂合併。
 */
public final class MerchantNames {

    private MerchantNames() {
    }

    private static final Map<String, String> VARIANT_TO_CANONICAL = new LinkedHashMap<>();

    static {
        VARIANT_TO_CANONICAL.put("JHC", "JHC 真好城");
        VARIANT_TO_CANONICAL.put("百佳 PARKnSHOP", "百佳");
        VARIANT_TO_CANONICAL.put("屈臣氏 Watsons", "屈臣氏");
        VARIANT_TO_CANONICAL.put("實惠 Pricerite", "實惠");
        VARIANT_TO_CANONICAL.put("必勝客 Pizza Hut", "必勝客");
        VARIANT_TO_CANONICAL.put("Pizza Hut", "必勝客");
        VARIANT_TO_CANONICAL.put("豐澤 Fortress", "豐澤");
        VARIANT_TO_CANONICAL.put("McDonald's", "麥當勞");
    }

    /** 將任何寫法嘅商戶名轉做 canonical 名；null 照返 null，空白／未知照原樣返。 */
    public static String canonical(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return name;
        }
        return VARIANT_TO_CANONICAL.getOrDefault(trimmed, trimmed);
    }

    /** 某 canonical 名對應嘅全部儲存寫法（canonical 本身行先），供商戶頁查詢用。 */
    public static List<String> variants(String canonicalName) {
        String canonical = canonical(canonicalName);
        List<String> result = new ArrayList<>();
        if (canonical != null && !canonical.isBlank()) {
            result.add(canonical);
        }
        for (Map.Entry<String, String> e : VARIANT_TO_CANONICAL.entrySet()) {
            if (e.getValue().equals(canonical) && !result.contains(e.getKey())) {
                result.add(e.getKey());
            }
        }
        return result;
    }
}
