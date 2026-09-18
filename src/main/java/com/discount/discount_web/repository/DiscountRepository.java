package com.discount.discount_web.repository;

import com.discount.discount_web.model.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DiscountRepository extends JpaRepository<Discount, Long> {
    
    // 1. 搵所有優惠，並按 ID 倒序排列 (最新嘅排最頂)
    List<Discount> findAllByOrderByIdDesc();
    
    // 2. 根據分類搵優惠，並按 ID 倒序排列
    List<Discount> findByCategoryOrderByIdDesc(String category);
}