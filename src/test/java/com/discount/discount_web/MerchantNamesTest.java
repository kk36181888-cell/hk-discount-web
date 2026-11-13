package com.discount.discount_web;

import com.discount.discount_web.service.MerchantNames;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MerchantNamesTest {

    @Test
    void canonicalMapsKnownVariants() {
        assertEquals("JHC 真好城", MerchantNames.canonical("JHC"));
        assertEquals("百佳", MerchantNames.canonical("百佳 PARKnSHOP"));
        assertEquals("屈臣氏", MerchantNames.canonical("屈臣氏 Watsons"));
        assertEquals("實惠", MerchantNames.canonical("實惠 Pricerite"));
        assertEquals("必勝客", MerchantNames.canonical("必勝客 Pizza Hut"));
        assertEquals("必勝客", MerchantNames.canonical("Pizza Hut"));
        assertEquals("豐澤", MerchantNames.canonical("豐澤 Fortress"));
        assertEquals("麥當勞", MerchantNames.canonical("McDonald's"));
    }

    @Test
    void canonicalLeavesUnknownNamesAlone() {
        assertEquals("惠康", MerchantNames.canonical("惠康"));
        assertEquals("JHC 真好城", MerchantNames.canonical("JHC 真好城"));
        assertNull(MerchantNames.canonical(null));
    }

    @Test
    void variantsCoverAllStoredSpellings() {
        assertEquals(List.of("JHC 真好城", "JHC"), MerchantNames.variants("JHC"));
        assertEquals(List.of("必勝客", "必勝客 Pizza Hut", "Pizza Hut"), MerchantNames.variants("Pizza Hut"));
        assertEquals(List.of("麥當勞", "McDonald's"), MerchantNames.variants("McDonald's"));
        assertEquals(List.of("惠康"), MerchantNames.variants("惠康"));
    }
}
