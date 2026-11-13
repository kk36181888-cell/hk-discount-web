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

    // 唔再用 default_cloud / default_key / default_secret 做 fallback：
    // 未設定環境變數嗰陣，舊版會攞住假憑證去 upload 先失敗，難追蹤。
    // 而家缺設定就喺 upload 嗰陣即刻講清楚，唔影響 App 開機同睇頁（睇頁唔使 Cloudinary）。
    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    private Cloudinary cloudinary;

    @PostConstruct
    public void init() {
        if (isConfigured()) {
            cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret,
                    "secure", true
            ));
        }
    }

    private boolean isConfigured() {
        return cloudName != null && !cloudName.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && apiSecret != null && !apiSecret.isBlank();
    }

    private void requireConfigured() {
        if (cloudinary == null) {
            throw new IllegalStateException(
                    "Cloudinary is not configured: set cloudinary.cloud-name, cloudinary.api-key and cloudinary.api-secret (Render environment variables)");
        }
    }

    public String uploadImage(MultipartFile file) throws IOException {
        requireConfigured();
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
        requireConfigured();
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