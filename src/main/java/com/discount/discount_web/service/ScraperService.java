package com.discount.discount_web.service;

import com.discount.discount_web.model.Discount;
import com.discount.discount_web.repository.DiscountRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ScraperService {

    @Autowired
    private DiscountRepository discountRepository;

    public void scrapeDiscounts() {
        try {
            // 換成你截圖入面嗰個優惠資訊網頁真實 URL
            String url = "https://example-discount-blog.com/offers"; 
            Document doc = Jsoup.connect(url).get();

            // 假設網頁入面嘅優惠項目係裝喺 <li> 標籤入面
            Elements listItems = doc.select("ul li");

            int count = 0;
            for (Element item : listItems) {
                String fullText = item.text(); // 例如：「百佳超市：全單88折 (19/9)」

                // 過濾一啲唔關事嘅字眼
                if (fullText.contains("超市") || fullText.contains("折") || fullText.contains("優惠")) {
                    
                    Discount discount = new Discount();
                    discount.setTitle("🤖 [自動抓取] " + fullText);
                    discount.setDescription("由自動化爬蟲從香港著數網即時同步嘅最新情報。");
                    // 畀個預設靚靚超市圖片
                    discount.setImageUrl("https://images.unsplash.com/photo-1578916171728-46686eac8d58?q=80&w=600&auto=format&fit=crop");
                    discount.setPromoPeriod("限時優惠");
                    discount.setCategory("supermarket"); // 自動歸類去超市情報

                    discountRepository.save(discount);
                    count++;
                }

                if (count >= 5) break; // 每次最多抓 5 個
            }
            System.out.println("✅ 成功從靜態清單抓取 " + count + " 筆優惠！");
        } catch (Exception e) {
            System.out.println("❌ 爬蟲發生錯誤: " + e.getMessage());
        }
    }
}