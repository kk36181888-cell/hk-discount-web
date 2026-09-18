package com.discount.discount_web.repository;

import com.discount.discount_web.model.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DiscountRepository extends JpaRepository<Discount, Long> {
    // 讓 Spring Boot 識得根據 Category 搵優惠出嚟
    List<Discount> findByCategory(String category);
}