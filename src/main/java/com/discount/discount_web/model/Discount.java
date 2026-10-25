package com.discount.discount_web.model;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "discounts")
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    
    // ⚠️ 線上舊表欄位曾停喺 varchar(255) (ddl-auto=update 唔會改既有欄位闊度)，
    //    由 DbColumnFixer 喺開機時 ALTER 拉闊到 5000，呢度同步返避免新裝環境唔一致。
    @Column(length = 5000)
    private String description;

    @Column(length = 1000)
    private String imageUrl;

    private String promoPeriod;

    // 📅 優惠結束日期 (俾系統判斷過期/計剩餘日數用；promoPeriod 繼續做顯示文字)
    // 冇填 = 長期/未知，前台照常顯示，唔會被當過期
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String category;

    // 🌟 全新升級：額外圖片網址 (支援多張，用換行符號隔開)
    @Column(columnDefinition = "TEXT")
    private String additionalImages;

    // 🚀 BI 數據收集：瀏覽量
    @Column(columnDefinition = "integer default 0")
    private int viewCount = 0;

    // 🤖 自動入貨欄位 (2026-10-23 爬蟲 v1)：狀態 PUBLISHED=公開 / PENDING=待審草稿
    //    columnDefinition 帶 default，現有資料加欄時會自動填 PUBLISHED，唔會突然消失
    @Column(columnDefinition = "varchar(20) default 'PUBLISHED'")
    private String status = "PUBLISHED";

    // 來源顯示名 (例：惠康官方網店)；人手入嘅優惠可留空
    private String sourceName;

    // 官方來源頁連結 (詳情頁標示 + 查核用)
    @Column(length = 1000)
    private String sourceUrl;

    // 🏪 商戶名 (例：惠康/百佳；後台填寫，商戶專頁用)
    private String merchant;

    // 📜 條款撮要 (後台填寫，詳情頁「資料來源及條款撮要」區顯示)
    @Column(length = 2000)
    private String terms;

    // 去重鍵 (例：wellcome:/d/abc123.html)，unique 保證同一活動唔會重複入貨
    @Column(unique = true, length = 500)
    private String sourceKey;

    // 抓取時間 (人手入嘅為 null)
    private LocalDateTime scrapedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getPromoPeriod() { return promoPeriod; }
    public void setPromoPeriod(String promoPeriod) { this.promoPeriod = promoPeriod; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    // ⏰ 係咪已經過期 (冇結束日期 = 未過期)
    @Transient
    public boolean isExpired() {
        return endDate != null && endDate.isBefore(LocalDate.now());
    }

    // ⏰ 距離結束仲剩幾多日 (今日完 = 0；冇結束日期/已過期 = null)
    @Transient
    public Long getDaysRemaining() {
        if (endDate == null || isExpired()) { return null; }
        return ChronoUnit.DAYS.between(LocalDate.now(), endDate);
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    // 🌟 額外圖片嘅 Getter 同 Setter (就係爭咗呢兩行搞到 Error！)
    public String getAdditionalImages() { return additionalImages; }
    public void setAdditionalImages(String additionalImages) { this.additionalImages = additionalImages; }

    // 🚀 瀏覽量嘅 Getter 同 Setter
    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    // 🤖 自動入貨欄位 Getter / Setter
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }

    public String getTerms() { return terms; }
    public void setTerms(String terms) { this.terms = terms; }

    // 🏪 商戶顯示名：後台 merchant 欄優先；舊資料冇填就由標題關鍵字推斷；再冇就用來源名
    @Transient
    public String getMerchantDisplay() {
        if (merchant != null && !merchant.trim().isEmpty()) { return merchant.trim(); }
        if (title != null) {
            if (title.contains("惠康")) { return "惠康"; }
            if (title.contains("百佳")) { return "百佳"; }
            if (title.contains("759")) { return "759阿信屋"; }
            if (title.contains("萬寧")) { return "萬寧"; }
            if (title.contains("屈臣氏")) { return "屈臣氏"; }
            if (title.contains("AEON")) { return "AEON"; }
            if (title.contains("Donki") || title.contains("驚安")) { return "Don Don Donki"; }
        }
        if (sourceName != null && !sourceName.trim().isEmpty()) { return sourceName.trim(); }
        return null;
    }

    public String getSourceKey() { return sourceKey; }
    public void setSourceKey(String sourceKey) { this.sourceKey = sourceKey; }

    public LocalDateTime getScrapedAt() { return scrapedAt; }
    public void setScrapedAt(LocalDateTime scrapedAt) { this.scrapedAt = scrapedAt; }

    // 係咪公開中 (status 未填當公開，雙重保險配合 DB default)
    @Transient
    public boolean isPublished() {
        return status == null || "PUBLISHED".equals(status);
    }
}