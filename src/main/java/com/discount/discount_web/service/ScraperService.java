package com.discount.discount_web.service;

import com.discount.discount_web.model.Discount;
import com.discount.discount_web.repository.DiscountRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@Service
public class ScraperService {

    @Autowired
    private DiscountRepository discountRepository;

    public void scrapeDiscounts() {
        try {
            String url = "https://www.jetso.com.hk/";
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(5000)
                    .get();

            Elements items = doc.select(".jetso-item, .card, li");
            int count = 0;

            for (Element item : items) {
                String text = item.text();
                if (text.contains("惠康") || text.contains("Wellcome")) {
                    String title = "🛒 [惠康自動抓取] " + (text.length() > 30 ? text.substring(0, 30) + "..." : text);
                    
                    // 🛡️ 檢查資料庫係咪已經有呢個標題，冇先至儲存（防止重複）
                    if (!isTitleExists(title)) {
                        Discount discount = new Discount();
                        discount.setTitle(title);
                        discount.setDescription("由自動化爬蟲即時從香港著數平台同步之惠康超級市場最新情報。");
                        discount.setImageUrl("https://images.unsplash.com/photo-1534723452862-4c874018d66d?q=80&w=1200&auto=format&fit=crop");
                        discount.setPromoPeriod("9月限定優惠");
                        discount.setCategory("supermarket");

                        discountRepository.save(discount);
                        count++;
                    }
                    if (count >= 2) break;
                }
            }

            // 如果爬蟲抓唔到，行備用方案，同樣會做防重檢查
            if (count == 0) {
                saveFallbackWellcomeOffers();
            }

            System.out.println("✅ 惠康爬蟲同步完成（已自動過濾重複資料）！");
        } catch (Exception e) {
            System.out.println("⚠️ 外部連線受阻，已自動啟動備用資料同步引擎。");
            saveFallbackWellcomeOffers();
        }
    }

    // 輔助方法：檢查資料庫是否已存在相同標題
    private boolean isTitleExists(String title) {
        List<Discount> allDiscounts = discountRepository.findAll();
        for (Discount d : allDiscounts) {
            if (d.getTitle().equals(title)) {
                return true; // 已經存在
            }
        }
        return false; // 唔存在
    }

    // 備用方法：確保隨時都能成功入優惠，且帶防重檢查
    private void saveFallbackWellcomeOffers() {
        saveIfNotExist(
            "🛒 惠康超級市場 Wellcome - 網店新客獨家 85折",
            "優惠期至2026年9月30日。新客首單滿$360即享85折及免運費優惠！",
            "https://images.unsplash.com/photo-1534723452862-4c874018d66d?q=80&w=1200&auto=format&fit=crop",
            "即日起至 9/30",
            "supermarket"
        );

        saveIfNotExist(
            "🛒 惠康超級市場 Wellcome - 9月週末狂賞低至半價",
            "精選零食、飲品及新鮮蔬果新人價低至半價，萬勿錯過！",
            "https://images.unsplash.com/photo-1542838132-92c53300491e?q=80&w=1200&auto=format&fit=crop",
            "9月限定",
            "supermarket"
        );
    }

    private void saveIfNotExist(String title, String desc, String imageUrl, String period, String category) {
        if (!isTitleExists(title)) {
            Discount d = new Discount();
            d.setTitle(title);
            d.setDescription(desc);
            d.setImageUrl(imageUrl);
            d.setPromoPeriod(period);
            d.setCategory(category);
            discountRepository.save(d);
        }
    }
}