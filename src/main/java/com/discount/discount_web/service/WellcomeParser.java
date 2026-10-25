package com.discount.discount_web.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 🛒 惠康官方源頭解析器 (自動入貨 v1，2026-10-23)
 * 抓取單位 = 一個官方推廣活動頁 (網址形態 /d/xxxx.html)。
 * 只抽活動頁層級資料 (標題/主圖/內文)，不抓逐件產品價格。
 * 已實查：惠康 robots.txt 冇禁推廣頁，頁面為伺服器渲染 HTML，jsoup 可直接解析。
 */
@Component
public class WellcomeParser {

    public static final String SOURCE_NAME = "惠康官方網店";
    private static final String HOME_URL = "https://www.wellcome.com.hk/";
    private static final String USER_AGENT = "DayBuyBot/1.0 (+https://daybuy2.onrender.com)";
    private static final int MAX_CAMPAIGNS_PER_RUN = 6;

    public List<ScrapedDeal> fetchDeals() throws Exception {
        Document home = Jsoup.connect(HOME_URL)
                .userAgent(USER_AGENT)
                .timeout(20000)
                .get();

        // 1) 從首頁收集活動頁連結，保持出現次序去重
        //    ⚠️ 2026-10-23 第一次實跑修正：官網實際連結形態係
        //    /zh-hant/wellcome/d/XXXX.html?venderBrandIds=...&flowDeliveryTimeType=1
        //    即 path 先係 /d/*.html，後面仲有 query string；舊寫法用完整網址 endsWith(".html")
        //    同 "wellcome.com.hk/d/" 比對，兩樣都唔中，結果 found=0。
        //    改為：抽 path 嚟比對，並去掉 query string 令來源網址乾淨、去重鍵穩定。
        Set<String> campaignUrls = new LinkedHashSet<>();
        Elements links = home.select("a[href]");
        for (Element a : links) {
            String href = a.absUrl("href");
            try {
                String path = URI.create(href).getPath();
                if (path != null && path.contains("/d/") && path.endsWith(".html")) {
                    campaignUrls.add("https://www.wellcome.com.hk" + path);
                }
            } catch (Exception ignored) {
                // 唔合規格嘅連結跳過
            }
            if (campaignUrls.size() >= MAX_CAMPAIGNS_PER_RUN) break;
        }

        // 2) 逐個活動頁抽資料；單頁失敗唔拖累其他頁
        List<ScrapedDeal> deals = new ArrayList<>();
        for (String url : campaignUrls) {
            try {
                ScrapedDeal deal = fetchCampaign(url);
                if (deal != null) {
                    deals.add(deal);
                }
            } catch (Exception e) {
                System.out.println("⚠️ [WellcomeParser] 活動頁抓取失敗，跳過: " + url + " (" + e.getMessage() + ")");
            }
        }
        return deals;
    }

    private ScrapedDeal fetchCampaign(String url) throws Exception {
        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(20000)
                .get();

        ScrapedDeal deal = new ScrapedDeal();
        deal.sourceName = SOURCE_NAME;
        deal.sourceUrl = url;
        deal.sourceKey = "wellcome:" + URI.create(url).getPath();
        deal.category = "supermarket";

        // 惠康用 name="og:title" 多過 property，兩樣都試，冇先用 doc.title()
        Element ogTitle = doc.selectFirst("meta[property=og:title]");
        if (ogTitle == null) { ogTitle = doc.selectFirst("meta[name=og:title]"); }
        String title = ogTitle != null ? ogTitle.attr("content") : doc.title();
        deal.title = cleanTitle(title);

        Element ogImage = doc.selectFirst("meta[property=og:image]");
        if (ogImage == null) { ogImage = doc.selectFirst("meta[name=og:image]"); }
        deal.imageUrl = ogImage != null ? ogImage.attr("content") : "";

        // 🖼️ 2026-10-25 修正 (option B)：活動頁本身冇 og:image（實查原箱夠抵頁 0 個），
        //    但活動頁入面第一件產品頁有 name="og:image" 實物圖；冇圖就入產品頁攞圖做封面，
        //    攞唔到先維持空白（由 IngestService 門檻丟棄，行為不變）。
        if (deal.imageUrl == null || deal.imageUrl.isBlank()) {
            deal.imageUrl = fetchFirstProductImage(doc);
        }

        String text = doc.text();
        deal.textContent = text.length() > 6000 ? text.substring(0, 6000) : text;

        return deal;
    }

    // 由活動頁抽第一個產品連結，入產品頁攞圖：og:image（name 優先，property 其次，s-img 兜底）
    private String fetchFirstProductImage(Document doc) {
        try {
            Element productLink = doc.selectFirst("a[href*=/p/]");
            if (productLink == null) { return ""; }
            String productUrl = productLink.absUrl("href");
            if (productUrl == null || productUrl.isBlank()) { return ""; }
            Document product = Jsoup.connect(productUrl)
                    .userAgent(USER_AGENT)
                    .timeout(20000)
                    .get();
            Element img = product.selectFirst("meta[name=og:image]");
            if (img == null) { img = product.selectFirst("meta[property=og:image]"); }
            if (img != null && !img.attr("content").isBlank()) { return img.attr("content"); }
            Element sImg = product.selectFirst("img.s-img");
            if (sImg != null && !sImg.absUrl("src").isBlank()) { return sImg.absUrl("src"); }
        } catch (Exception e) {
            System.out.println("⚠️ [WellcomeParser] 產品圖抓取失敗，跳過: " + e.getMessage());
        }
        return "";
    }

    private String cleanTitle(String title) {
        if (title == null) return "";
        String t = title.trim();
        // 去走官網標題常見尾巴
        t = t.replaceAll("\\s*[|｜-]\\s*(惠康網店|惠康官方網店|Wellcome).*$", "").trim();
        if (t.length() > 80) {
            t = t.substring(0, 80).trim();
        }
        return t;
    }
}
