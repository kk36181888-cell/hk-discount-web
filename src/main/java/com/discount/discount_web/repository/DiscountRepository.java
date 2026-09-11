package com.discount.discount_web.repository; // 同樣留意 package 名

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.discount.discount_web.model.Discount;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {
    // 只要 extends 咗 JpaRepository，Spring Boot 就會自動送晒 save(), findById(), findAll(), deleteById() 呢啲功能畀你，唔使自己寫 SQL！
}