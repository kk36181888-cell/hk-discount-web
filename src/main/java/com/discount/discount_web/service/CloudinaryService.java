package com.discount.discount_web.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    // 🛡️ 加上冒號同預設字串做 fallback，避免找不到變數時 App 直接 Crash
    @Value("${cloudinary.cloud-name:default_cloud}")
    private String cloudName;

    @Value("${cloudinary.api-key:default_key}")
    private String apiKey;

    @Value("${cloudinary.api-secret:default_secret}")
    private String apiSecret;

    private Cloudinary cloudinary;

    @PostConstruct
    public void init() {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true 
        ));
    }

    public String uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        Map<?, ?> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap("folder", "daybuy_discounts")
        );
        return uploadResult.get("secure_url").toString();
    }

    // 🤖 自動入貨用：將外部圖片網址轉存入 Cloudinary (Cloudinary 會自己去抓原圖)
    //    失敗會 throw，呼叫方要 catch 住並決定用返原網址定丟棄
    public String uploadImageFromUrl(String imageUrl) throws IOException {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        Map<?, ?> uploadResult = cloudinary.uploader().upload(
                imageUrl,
                ObjectUtils.asMap("folder", "daybuy_discounts")
        );
        Object secureUrl = uploadResult.get("secure_url");
        return secureUrl != null ? secureUrl.toString() : null;
    }
}