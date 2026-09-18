package com.discount.discount_web.repository;

import com.discount.discount_web.model.Discount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DiscountRepository extends JpaRepository<Discount, Long> {
    
    List<Discount> findAllByOrderByIdDesc();
    
    Page<Discount> findAllByOrderByIdDesc(Pageable pageable);
    
    Page<Discount> findByCategoryOrderByIdDesc(String category, Pageable pageable);
    
    // 🔍 全新搜尋功能：無視大細階搵標題
    Page<Discount> findByTitleContainingIgnoreCaseOrderByIdDesc(String keyword, Pageable pageable);
}