package com.discount.discount_web.service;

import com.discount.discount_web.model.Discount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 🖼️ 圖片斷尾服務（2026-10-28）：將優惠嘅外部圖片網址（例如 Facebook fbcdn）
 * 轉存入 Cloudinary，之後前台只會見到永久連結。
 *
 * 保證：
 * - 已經係 Cloudinary（res.cloudinary.com）嘅圖會跳過，唔會重複上載，唔嘥額度。
 * - 轉存失敗（例如原連結已過期、Cloudinary 未設定）會保留原網址並記低，
 *   絕對唔會令儲存／批准成單死火 —— 最差情況同以前一樣，唔會更差。
 * - 唔改標題、價錢、結束日期、分類、商戶、來源、狀態以外嘅任何欄位。
 */
@Service
public class ImageRehostService {

    @Autowired
    private CloudinaryService cloudinaryService;

    /**
     * 轉存單張圖。成功返 Cloudinary 網址；跳過或失敗都原樣返入參數，絕不 throw。
     */
    public String rehostOne(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return imageUrl;
        }
        String url = imageUrl.trim();
        if (url.contains("res.cloudinary.com")) {
            return imageUrl;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return imageUrl;
        }
        try {
            String hosted = cloudinaryService.uploadImageFromUrl(url);
            return (hosted != null && !hosted.isBlank()) ? hosted : imageUrl;
        } catch (Exception e) {
            System.out.println("⚠️ 圖片轉存 Cloudinary 失敗，保留原網址: " + url + " (" + e.getMessage() + ")");
            return imageUrl;
        }
    }

    /**
     * 轉存成張優惠嘅封面＋附加圖（附加圖用換行分隔，順序不變）。
     * 只改 imageUrl / additionalImages 兩個欄位；返回成功轉存嘅張數。
     * 注意：呢個方法只改記憶體入面嘅物件，呼叫方要自己 save。
     */
    public int rehostDiscount(Discount d) {
        int moved = 0;
        String cover = d.getImageUrl();
        if (cover != null && !cover.isBlank()) {
            String hosted = rehostOne(cover);
            if (!hosted.equals(cover)) {
                d.setImageUrl(hosted);
                moved++;
            }
        }
        String extras = d.getAdditionalImages();
        if (extras != null && !extras.isBlank()) {
            List<String> out = new ArrayList<>();
            for (String line : extras.split("\n")) {
                String u = line.trim();
                if (u.isEmpty()) {
                    continue;
                }
                String hosted = rehostOne(u);
                if (!hosted.equals(u)) {
                    moved++;
                }
                out.add(hosted);
            }
            d.setAdditionalImages(String.join("\n", out));
        }
        return moved;
    }
}
