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

    // 🌐 前台分頁與分類查詢
    Page<Discount> findAllByOrderByIdDesc(Pageable pageable);
    Page<Discount> findByTitleContainingIgnoreCaseOrderByIdDesc(String keyword, Pageable pageable);
    Page<Discount> findByCategoryOrderByIdDesc(String category, Pageable pageable);

    // 🚀 後台管理列表查詢
    List<Discount> findAllByOrderByIdDesc();

    // ⚡ 極速更新 ViewCount (直接喺 DB 層面 +1)
    @Modifying
    @Transactional
    @Query("UPDATE Discount d SET d.viewCount = d.viewCount + 1 WHERE d.id = :id")
    void incrementViewCount(@Param("id") Long id);

    // 🌟 效能優化核心：由 PostgreSQL 直接隨機抽 3 個其他優惠，唔使 Load 爆 Java RAM
    @Query(value = "SELECT * FROM discounts WHERE id != :currentId ORDER BY RANDOM() LIMIT 3", nativeQuery = true)
    List<Discount> findRandomRelatedDiscounts(@Param("currentId") Long currentId);
}