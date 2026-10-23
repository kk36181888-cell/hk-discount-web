package com.discount.discount_web.model;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

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
}