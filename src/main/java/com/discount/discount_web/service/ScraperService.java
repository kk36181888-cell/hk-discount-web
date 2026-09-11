package com.discount.discount_web.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Service
public class ScraperService {

    public void scrapeWellcome() {
        try {
            // 惠康嘅目標網址
            String url = "https://www.wellcome.com.hk/zh-hant/d/0BnIfjXEWesZ.html?venderBrandIds=6%252C5&flowDeliveryTimeType=1";
            System.out.println("🛒 準備出發去惠康: " + url);

            // 加入 UserAgent 扮成真實嘅電腦 Google Chrome 瀏覽器，減少被惠康 Server 踢走嘅機會
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(10000) // 畀 10 秒時間佢 Load
                    .get();

            // ⚠️ 照妖鏡：印出 Jsoup 真正下載到嘅 HTML 原始碼！
            System.out.println("--- Jsoup 睇到嘅 HTML 節錄 ---");
            String rawHtml = doc.html();
            
            // 因為成個網頁太長，我哋淨係印最頭 2000 個字元出嚟睇吓，避免 Terminal 洗版
            System.out.println(rawHtml.substring(0, Math.min(rawHtml.length(), 2000))); 
            System.out.println("--------------------------------");

        } catch (Exception e) {
            System.out.println("❌ 抓取失敗: " + e.getMessage());
        }
    }
}