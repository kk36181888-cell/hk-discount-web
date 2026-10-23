package com.discount.discount_web.service;

/**
 * 🤖 一條從官方來源抓回來、未入 DB 的優惠原始資料 (parser 輸出 → IngestService 統一處理)
 */
public class ScrapedDeal {
    public String sourceName;   // 例：惠康官方網店
    public String sourceKey;    // 去重鍵，例：wellcome:/d/abc123.html
    public String sourceUrl;    // 官方活動頁連結
    public String title;
    public String imageUrl;     // 原始圖片網址 (之後會嘗試轉存 Cloudinary)
    public String textContent;  // 活動頁文字 (俾 DateExtractor 抽日期 + 做描述摘要)
    public String category;     // 例：supermarket
}
