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
    // 📅 2026-10-23 升級：前台自動隱藏已過期優惠 (endDate 已過)；冇填 endDate 嘅照常顯示。
    // 🤖 同日再升級：前台只顯示已發佈 (status = PUBLISHED 或未填)；待審草稿 (PENDING) 唔出街。
    //    後台列表 (下面 List 版本) 唔加呢啲條件，管理員要見到晒先可以管理。
    @Query("SELECT d FROM Discount d WHERE (d.status IS NULL OR d.status = 'PUBLISHED') AND (d.endDate IS NULL OR d.endDate >= CURRENT_DATE) ORDER BY d.id DESC")
    Page<Discount> findAllByOrderByIdDesc(Pageable pageable);

    @Query("SELECT d FROM Discount d WHERE (LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND (d.status IS NULL OR d.status = 'PUBLISHED') AND (d.endDate IS NULL OR d.endDate >= CURRENT_DATE) ORDER BY d.id DESC")
    Page<Discount> findByTitleContainingIgnoreCaseOrderByIdDesc(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT d FROM Discount d WHERE d.category = :category AND (d.status IS NULL OR d.status = 'PUBLISHED') AND (d.endDate IS NULL OR d.endDate >= CURRENT_DATE) ORDER BY d.id DESC")
    Page<Discount> findByCategoryOrderByIdDesc(@Param("category") String category, Pageable pageable);

    // 🚀 後台管理列表查詢
    List<Discount> findAllByOrderByIdDesc();

    // 🤖 自動入貨：去重 + 待審清單
    boolean existsBySourceKey(String sourceKey);
    List<Discount> findByStatusOrderByScrapedAtDesc(String status);
    long countByStatus(String status);

    // ⚡ 極速更新 ViewCount (直接喺 DB 層面 +1)
    @Modifying
    @Transactional
    @Query("UPDATE Discount d SET d.viewCount = d.viewCount + 1 WHERE d.id = :id")
    void incrementViewCount(@Param("id") Long id);

    // 🔥 最熱排序：瀏覽量高到低，同分就新到舊 (前台已發佈 + 未過期)
    @Query("SELECT d FROM Discount d WHERE (d.status IS NULL OR d.status = 'PUBLISHED') AND (d.endDate IS NULL OR d.endDate >= CURRENT_DATE) ORDER BY d.viewCount DESC, d.id DESC")
    Page<Discount> findAllByOrderByViewCountDesc(Pageable pageable);

    @Query("SELECT d FROM Discount d WHERE (LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND (d.status IS NULL OR d.status = 'PUBLISHED') AND (d.endDate IS NULL OR d.endDate >= CURRENT_DATE) ORDER BY d.viewCount DESC, d.id DESC")
    Page<Discount> findByTitleContainingIgnoreCaseOrderByViewCountDesc(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT d FROM Discount d WHERE d.category = :category AND (d.status IS NULL OR d.status = 'PUBLISHED') AND (d.endDate IS NULL OR d.endDate >= CURRENT_DATE) ORDER BY d.viewCount DESC, d.id DESC")
    Page<Discount> findByCategoryOrderByViewCountDesc(@Param("category") String category, Pageable pageable);

    // 🏪 商戶專頁：商戶欄完全相符，或者標題包含商戶名 (覆蓋未填商戶欄嘅舊資料)
    @Query("SELECT d FROM Discount d WHERE (d.merchant = :name OR LOWER(d.title) LIKE LOWER(CONCAT('%', :name, '%'))) AND (d.status IS NULL OR d.status = 'PUBLISHED') AND (d.endDate IS NULL OR d.endDate >= CURRENT_DATE) ORDER BY d.id DESC")
    Page<Discount> findByMerchantOrTitle(@Param("name") String name, Pageable pageable);

    @Query("SELECT d FROM Discount d WHERE (d.merchant = :name OR LOWER(d.title) LIKE LOWER(CONCAT('%', :name, '%'))) AND (d.status IS NULL OR d.status = 'PUBLISHED') AND (d.endDate IS NULL OR d.endDate >= CURRENT_DATE) ORDER BY d.viewCount DESC, d.id DESC")
    Page<Discount> findByMerchantOrTitleOrderByViewCountDesc(@Param("name") String name, Pageable pageable);

    // 🏪 商戶一覽：前台可見嘅全部優惠 (controller 再按商戶歸納計數；量大時改 distinct 查詢)
    @Query("SELECT d FROM Discount d WHERE (d.status IS NULL OR d.status = 'PUBLISHED') AND (d.endDate IS NULL OR d.endDate >= CURRENT_DATE) ORDER BY d.id DESC")
    List<Discount> findAllPublished();

    // 🌟 效能優化核心：由 PostgreSQL 直接隨機抽 3 個其他優惠，唔使 Load 爆 Java RAM
    @Query(value = "SELECT * FROM discounts WHERE id != :currentId AND (status IS NULL OR status = 'PUBLISHED') AND (end_date IS NULL OR end_date >= CURRENT_DATE) ORDER BY RANDOM() LIMIT 3", nativeQuery = true)
    List<Discount> findRandomRelatedDiscounts(@Param("currentId") Long currentId);
}
