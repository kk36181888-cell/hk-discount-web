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
            // 1. 連線去測試用嘅靜態網站
            String url = "http://books.toscrape.com/";
            Document doc = Jsoup.connect(url).get();

            // 2. 鎖定網頁入面裝住商品嘅「卡片」 (佢哋嘅 HTML class 係 .product_pod)
            Elements items = doc.select(".product_pod");

            // 3. 為咗唔好一次過塞爆 Database，我哋淨係抽頭 3 個
            int count = 0;
            for (Element item : items) {
                if (count >= 3) break;

                // 4. 利用 CSS Selector 抽走標題、圖片、價錢
                String title = item.select("h3 a").attr("title");
                // 處理相對路徑圖片網址
                String imageUrl = "http://books.toscrape.com/" + item.select(".image_container img").attr("src");
                String price = item.select(".price_color").text();

                // 5. 建立新 Discount 物件並塞入 Database
                Discount discount = new Discount();
                discount.setTitle("🤖 [自動抓取] " + title);
                discount.setDescription("自動化系統搵到嘅價錢: " + price);
                discount.setImageUrl(imageUrl);
                discount.setPromoPeriod("即日起");
                discount.setCategory("others"); // 預設擺落「其他」分類

                discountRepository.save(discount);
                count++;
            }
            System.out.println("✅ 爬蟲執行完畢！成功抓取 " + count + " 筆資料。");
        } catch (Exception e) {
            System.out.println("❌ 爬蟲發生錯誤: " + e.getMessage());
        }
    }
}