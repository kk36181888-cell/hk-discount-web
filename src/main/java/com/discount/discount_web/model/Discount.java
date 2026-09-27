package com.discount.discount_web.model;

import jakarta.persistence.*;

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

    // 🚀 新增 ViewCount 嘅 Getter 同 Setter (就係爭咗呢兩行搞到 Error！)
    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }
}