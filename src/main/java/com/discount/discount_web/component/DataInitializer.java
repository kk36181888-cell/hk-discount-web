package com.discount.discount_web.component;

import com.discount.discount_web.model.Discount;
import com.discount.discount_web.repository.DiscountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer {

    @Autowired
    private DiscountRepository discountRepository;

    @Bean
    CommandLineRunner initDatabase() {
        return args -> {
            // 如果資料庫入面係空的，就自動加入兩張惠康優惠
            if (discountRepository.count() == 0) {
                Discount d1 = new Discount();
                d1.setTitle("惠康超級市場 Wellcome - 新客優惠 85折");
                d1.setDescription("優惠期至2026年9月30日。*新客優惠只限未曾於 Market Place / 惠康 / yuu 網店購物之新用戶 ^最高可享$100優惠。");
                d1.setImageUrl("https://images.unsplash.com/photo-1534723452862-4c874018d66d?q=80&w=1200&auto=format&fit=crop");
                d1.setPromoPeriod("1/9 - 30/9");
                discountRepository.save(d1);

                Discount d2 = new Discount();
                d2.setTitle("惠康超級市場 Wellcome - 新客 3 重賞");
                d2.setDescription("包羅萬有：指定產品新人價低至半價、首單滿$360*享85折及免運費三重賞！");
                d2.setImageUrl("https://images.unsplash.com/photo-1542838132-92c53300491e?q=80&w=1200&auto=format&fit=crop");
                d2.setPromoPeriod("9月限定");
                discountRepository.save(d2);
                
                System.out.println(">>> 成功自動初始化惠康優惠資料入資料庫！");
            }
        };
    }
}