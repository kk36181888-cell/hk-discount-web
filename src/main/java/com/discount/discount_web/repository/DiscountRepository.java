package com.discount.discount_web.repository;

import com.discount.discount_web.model.Discount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DiscountRepository extends JpaRepository<Discount, Long> {
    
    // 舊版：畀後台用，一次過攞晒所有資料方便管理
    List<Discount> findAllByOrderByIdDesc();
    
    // 🆕 升級：前台專用嘅分頁尋找功能
    Page<Discount> findAllByOrderByIdDesc(Pageable pageable);
    
    // 🆕 升級：分類搜尋嘅分頁尋找功能
    Page<Discount> findByCategoryOrderByIdDesc(String category, Pageable pageable);
}