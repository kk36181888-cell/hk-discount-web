package com.discount.discount_web.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "discounts")
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;       // 優惠標題 (例如: "新客優惠 85折")
    private String description; // 優惠描述
    private String imageUrl;    // 圖片網址
    private String promoPeriod; // 優惠期限 (例如: "1/9 - 30/9")
}