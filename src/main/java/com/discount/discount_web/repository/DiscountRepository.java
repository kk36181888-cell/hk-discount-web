package com.discount.discount_web.repository;

import com.discount.discount_web.model.Discount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DiscountRepository extends JpaRepository<Discount, Long> {
    
    List<Discount> findAllByOrderByIdDesc();
    
    Page<Discount> findAllByOrderByIdDesc(Pageable pageable);
    
    Page<Discount> findByCategoryOrderByIdDesc(String category, Pageable pageable);
    
    // 🔍 全新搜尋功能：無視大細階搵標題
    Page<Discount> findByTitleContainingIgnoreCaseOrderByIdDesc(String keyword, Pageable pageable);

    // 🚀 高效能瀏覽量 +1 方法，避開全欄位更新及 Jsoup 攔截
    @Modifying
    @Transactional
    @Query("UPDATE Discount d SET d.viewCount = d.viewCount + 1 WHERE d.id = :id")
    void incrementViewCount(@Param("id") Long id);
}