package com.discount.discount_web.service;

import com.discount.discount_web.model.Discount;
import com.discount.discount_web.repository.DiscountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 🤖 自動入貨主流程 (2026-10-23)
 * 原則：抓到嘅一律入 PENDING 待審草稿；抓唔到就只記錯誤，絕不作假資料頂替。
 */
@Service
public class IngestService {

    @Autowired
    private DiscountRepository discountRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private WellcomeParser wellcomeParser;

    public Map<String, Object> runIngest() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("ranAt", LocalDateTime.now().toString());

        List<Map<String, Object>> sources = new ArrayList<>();
        sources.add(ingestFromWellcome());
        summary.put("sources", sources);
        return summary;
    }

    private Map<String, Object> ingestFromWellcome() {
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("source", WellcomeParser.SOURCE_NAME);
        int found = 0, created = 0, skipped = 0, discarded = 0;

        try {
            List<ScrapedDeal> deals = wellcomeParser.fetchDeals();
            found = deals.size();

            for (ScrapedDeal deal : deals) {
                // 去重：sourceKey 已入過 (不論已批/待審/曾拒絕後刪走以外) 就跳過
                if (deal.sourceKey != null && discountRepository.existsBySourceKey(deal.sourceKey)) {
                    skipped++;
                    continue;
                }
                // 品質門檻：冇標題或冇圖 → 丟棄，唔入草稿
                if (deal.title == null || deal.title.trim().length() < 4
                        || deal.imageUrl == null || deal.imageUrl.isBlank()) {
                    discarded++;
                    continue;
                }

                DateExtractor.Result dateResult = DateExtractor.extract(deal.textContent);

                Discount d = new Discount();
                d.setTitle(deal.title.trim());
                d.setCategory(deal.category);
                d.setStatus("PENDING");
                d.setSourceName(deal.sourceName);
                d.setSourceUrl(deal.sourceUrl);
                d.setSourceKey(deal.sourceKey);
                d.setScrapedAt(LocalDateTime.now());
                d.setEndDate(dateResult.endDate);
                d.setPromoPeriod(dateResult.periodText != null ? dateResult.periodText : "詳見官方活動頁");
                d.setDescription(buildDescription(deal));
                d.setImageUrl(rehostImage(deal.imageUrl));

                try {
                    discountRepository.save(d);
                    created++;
                } catch (Exception saveEx) {
                    // unique 衝突等 → 當重複跳過
                    skipped++;
                }
            }
        } catch (Exception e) {
            stat.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("⚠️ [Ingest] 惠康源頭失敗: " + e.getMessage());
        }

        stat.put("found", found);
        stat.put("created", created);
        stat.put("skipped", skipped);
        stat.put("discarded", discarded);
        return stat;
    }

    // 描述：活動頁文字摘要 + 來源註明；做 HTML escape 因為詳情頁用 utext 渲染
    private String buildDescription(ScrapedDeal deal) {
        String text = deal.textContent == null ? "" : deal.textContent.trim().replaceAll("\\s+", " ");
        if (text.length() > 220) {
            text = text.substring(0, 220) + "…";
        }
        String escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return escaped + "<br><br>（資料來源：" + deal.sourceName + "，優惠詳情、條款及細則以官方公佈為準。）";
    }

    // 圖片轉存 Cloudinary；失敗就用返原網址，唔好成條入貨死火
    private String rehostImage(String imageUrl) {
        try {
            String hosted = cloudinaryService.uploadImageFromUrl(imageUrl);
            return hosted != null ? hosted : imageUrl;
        } catch (Exception e) {
            System.out.println("⚠️ [Ingest] 圖片轉存失敗，用返原網址: " + e.getMessage());
            return imageUrl;
        }
    }
}
