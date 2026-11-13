package com.discount.discount_web.service;

import com.discount.discount_web.model.Discount;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 🏪 首頁按商戶分組：首頁本來係全部商戶撈埋一齊按時間排，每日望落去好亂。
 * 呢度將前台可見嘅優惠按 canonical 商戶名分組，等每間公司嘅優惠企埋一齊；
 * 純邏輯、唔掂 DB，方便單元測試。分組結果只供首頁用，分類／搜尋／商戶頁不變。
 */
public final class MerchantGrouping {

    private MerchantGrouping() {
    }

    /** 首頁每個商戶 section 最多顯示幾多張，超出嘅用「睇埋其餘 N 張」連去商戶頁。 */
    public static final int MAX_VISIBLE_PER_SECTION = 3;

    /** 商戶顯示名為空／未知時歸入嘅組名（唔好因為冇商戶名而收起張優惠）。 */
    public static final String UNKNOWN_MERCHANT = "其他商戶";

    /** 一個商戶喺首頁嘅一個 section。 */
    public record MerchantGroup(
            String merchant,
            String slug,
            List<Discount> visibleDeals,
            int totalCount,
            int remainingCount,
            long totalViews
    ) {
    }

    /**
     * 將前台優惠清單按 canonical 商戶名分組。
     * sort = "hot" 時組內按瀏覽量排、組之間按總瀏覽量排；其他一律當 latest，
     * 組內按 id（新到舊）排、組之間按最新一張優惠嘅 id 排。
     * 輸入唔需要預先排好序，呢度會自己排；null／空清單返空 list。
     */
    public static List<MerchantGroup> group(List<Discount> deals, String sort) {
        if (deals == null || deals.isEmpty()) {
            return List.of();
        }
        boolean hot = "hot".equals(sort);
        Map<String, List<Discount>> byMerchant = new LinkedHashMap<>();
        for (Discount d : deals) {
            String merchant = MerchantNames.canonical(d.getMerchantDisplay());
            if (merchant == null || merchant.isBlank()) {
                merchant = UNKNOWN_MERCHANT;
            }
            byMerchant.computeIfAbsent(merchant, k -> new ArrayList<>()).add(d);
        }
        List<MerchantGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<Discount>> e : byMerchant.entrySet()) {
            List<Discount> sorted = new ArrayList<>(e.getValue());
            if (hot) {
                sorted.sort(Comparator.comparingInt(Discount::getViewCount).reversed()
                        .thenComparing(Comparator.comparing(Discount::getId).reversed()));
            } else {
                sorted.sort(Comparator.comparing(Discount::getId).reversed());
            }
            long totalViews = sorted.stream().mapToLong(Discount::getViewCount).sum();
            List<Discount> visible = sorted.subList(0, Math.min(MAX_VISIBLE_PER_SECTION, sorted.size()));
            groups.add(new MerchantGroup(
                    e.getKey(),
                    slug(e.getKey()),
                    List.copyOf(visible),
                    sorted.size(),
                    sorted.size() - visible.size(),
                    totalViews));
        }
        if (hot) {
            groups.sort(Comparator.comparingLong(MerchantGroup::totalViews).reversed()
                    .thenComparing(MerchantGroup::merchant));
        } else {
            groups.sort(Comparator.comparingLong((MerchantGroup g) -> g.visibleDeals().get(0).getId()).reversed()
                    .thenComparing(MerchantGroup::merchant));
        }
        return groups;
    }

    /** 商戶名轉做 section anchor id：去空格、只留字、數字同連字號，手機撳 chips 跳轉用。 */
    public static String slug(String merchant) {
        if (merchant == null) {
            return "merchant";
        }
        String s = merchant.trim().replaceAll("\\s+", "-");
        s = s.replaceAll("[^\\p{L}\\p{N}-]", "");
        return s.isEmpty() ? "merchant" : s;
    }
}
