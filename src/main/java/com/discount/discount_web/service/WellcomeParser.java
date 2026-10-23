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

        // 1) 從首頁收集活動頁連結 (/d/*.html)，保持出現次序去重
        Set<String> campaignUrls = new LinkedHashSet<>();
        Elements links = home.select("a[href]");
        for (Element a : links) {
            String href = a.absUrl("href");
            if (href.contains("wellcome.com.hk/d/") && href.endsWith(".html")) {
                campaignUrls.add(href);
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

        Element ogTitle = doc.selectFirst("meta[property=og:title]");
        String title = ogTitle != null ? ogTitle.attr("content") : doc.title();
        deal.title = cleanTitle(title);

        Element ogImage = doc.selectFirst("meta[property=og:image]");
        deal.imageUrl = ogImage != null ? ogImage.attr("content") : "";

        String text = doc.text();
        deal.textContent = text.length() > 6000 ? text.substring(0, 6000) : text;

        return deal;
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
