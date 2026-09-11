package com.discount.discount_web.model; // 留意呢行要對應返你真實嘅 package 名

import java.time.LocalDate;

import jakarta.persistence.Column;
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

    @Column(nullable = false)
    private String title;          // 優惠標題

    private String promoCode;      // 優惠碼 (有啲優惠可能冇 code)

    private String merchantName;   // 商戶名稱

    private String category;       // 分類 (例如：飲食, 網購)

    private LocalDate expiryDate;  // 到期日

    private boolean isActive = true; // 優惠係咪仲有效
}