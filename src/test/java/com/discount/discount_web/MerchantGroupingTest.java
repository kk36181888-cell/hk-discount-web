package com.discount.discount_web;

import com.discount.discount_web.model.Discount;
import com.discount.discount_web.service.MerchantGrouping;
import com.discount.discount_web.service.MerchantGrouping.MerchantGroup;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MerchantGroupingTest {

    private static Discount deal(long id, String merchant, int views) {
        Discount d = new Discount();
        d.setId(id);
        d.setMerchant(merchant);
        d.setViewCount(views);
        return d;
    }

    @Test
    void groupsByCanonicalMerchantName() {
        List<MerchantGroup> groups = MerchantGrouping.group(List.of(
                deal(1, "JHC", 5),
                deal(2, "JHC 真好城", 3),
                deal(3, "百佳", 1)), "latest");
        assertEquals(2, groups.size());
        // 最新排序：百佳最新一張係 id 3，排先；JHC 變體合併做一組
        assertEquals("百佳", groups.get(0).merchant());
        MerchantGroup jhc = groups.get(1);
        assertEquals("JHC 真好城", jhc.merchant());
        assertEquals(2, jhc.totalCount());
        assertEquals(0, jhc.remainingCount());
        assertEquals(8, jhc.totalViews());
    }

    @Test
    void latestOrdersGroupsByNewestDeal() {
        List<MerchantGroup> groups = MerchantGrouping.group(List.of(
                deal(1, "百佳", 100),
                deal(5, "惠康", 0),
                deal(3, "百佳", 100)), "latest");
        // 惠康最新一張係 id 5，排先；百佳組內按 id 由新到舊
        assertEquals("惠康", groups.get(0).merchant());
        assertEquals("百佳", groups.get(1).merchant());
        assertEquals(3L, groups.get(1).visibleDeals().get(0).getId());
        assertEquals(1L, groups.get(1).visibleDeals().get(1).getId());
    }

    @Test
    void hotOrdersGroupsByTotalViews() {
        List<MerchantGroup> groups = MerchantGrouping.group(List.of(
                deal(9, "百佳", 10),
                deal(8, "惠康", 40),
                deal(7, "惠康", 30),
                deal(6, "百佳", 5)), "hot");
        // 惠康總瀏覽 70 > 百佳 15，排先；組內按瀏覽量由高到低
        assertEquals("惠康", groups.get(0).merchant());
        assertEquals(70, groups.get(0).totalViews());
        assertEquals(40, groups.get(0).visibleDeals().get(0).getViewCount());
        assertEquals("百佳", groups.get(1).merchant());
    }

    @Test
    void capsVisibleDealsAtThree() {
        List<MerchantGroup> groups = MerchantGrouping.group(List.of(
                deal(1, "百佳", 1),
                deal(2, "百佳", 1),
                deal(3, "百佳", 1),
                deal(4, "百佳", 1)), "latest");
        assertEquals(1, groups.size());
        assertEquals(3, groups.get(0).visibleDeals().size());
        assertEquals(4, groups.get(0).totalCount());
        assertEquals(1, groups.get(0).remainingCount());
    }

    @Test
    void dealsWithoutMerchantGoToOtherMerchants() {
        Discount noName = new Discount();
        noName.setId(1L);
        List<MerchantGroup> groups = MerchantGrouping.group(List.of(noName), "latest");
        assertEquals(1, groups.size());
        assertEquals("其他商戶", groups.get(0).merchant());
    }

    @Test
    void slugIsSafeForAnchorIds() {
        assertEquals("JHC-真好城", MerchantGrouping.slug("JHC 真好城"));
        assertEquals("百佳", MerchantGrouping.slug("百佳"));
        assertEquals("merchant", MerchantGrouping.slug(null));
        assertEquals("merchant", MerchantGrouping.slug("   "));
    }

    @Test
    void emptyOrNullInputReturnsEmpty() {
        assertTrue(MerchantGrouping.group(null, "latest").isEmpty());
        assertTrue(MerchantGrouping.group(List.of(), "hot").isEmpty());
    }
}
