package com.discount.discount_web.model;

import jakarta.persistence.*;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

@Entity
@Table(name = "discounts")
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    
    @Column(length = 2000)
    private String description;

    @Column(length = 1000)
    private String imageUrl;

    private String promoPeriod;
    private String category;

    // 🚀 BI 數據收集升級：新增瀏覽量追蹤欄位 (預設為 0)
    @Column(columnDefinition = "integer default 0")
    private int viewCount = 0;

    /**
     * JPA 生命週期勾點：每次 Save 或 Update 入 PostgreSQL 前自動攔截並清洗 HTML
     * XSS 終極防護網
     */
    @PrePersist
    @PreUpdate
    public void sanitizeHtml() {
        // 1. 清洗富文本 (Rich Text) 欄位：保留安全排版標籤 (例如 <b>, <img>, <a>)
        if (this.description != null) {
            this.description = Jsoup.clean(this.description, Safelist.relaxed());
        }

        // 2. 清洗純文字欄位：極度嚴格，唔允許任何 HTML 標籤
        if (this.title != null) {
            this.title = Jsoup.clean(this.title, Safelist.none());
        }
        if (this.imageUrl != null) {
            this.imageUrl = Jsoup.clean(this.imageUrl, Safelist.none());
        }
        if (this.promoPeriod != null) {
            this.promoPeriod = Jsoup.clean(this.promoPeriod, Safelist.none());
        }
        if (this.category != null) {
            this.category = Jsoup.clean(this.category, Safelist.none());
        }
    }

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

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    // 🚀 新增 ViewCount 嘅 Getter 同 Setter
    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }
}