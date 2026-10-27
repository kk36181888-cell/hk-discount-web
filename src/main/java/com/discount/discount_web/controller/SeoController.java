package com.discount.discount_web.controller;

import com.discount.discount_web.model.Discount;
import com.discount.discount_web.repository.DiscountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 🔍 SEO 基礎建設 (2026-10-23 新增)
 * - /sitemap.xml：動態產生，列出主要頁面 + 所有未過期優惠詳情頁，畀 Google 收錄
 * - /robots.txt：放行前台、擋後台，並指返去 sitemap
 * 網站正式網址可用環境變數 DAYBUY_BASE_URL 覆蓋 (application.properties: daybuy.site.base-url)
 */
@RestController
public class SeoController {

    @Autowired
    private DiscountRepository discountRepository;

    @Value("${daybuy.site.base-url:https://daybuy2.onrender.com}")
    private String baseUrl;

    private String base() {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        return "User-agent: *\n"
                + "Allow: /\n"
                + "Disallow: /daybuy-hq/\n"
                + "\n"
                + "Sitemap: " + base() + "/sitemap.xml\n";
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        String today = LocalDate.now().toString();
        addUrl(xml, base() + "/", today, "1.0");
        addUrl(xml, base() + "/supermarket", today, "0.9");
        addUrl(xml, base() + "/dining", today, "0.9");
        addUrl(xml, base() + "/warehouse", today, "0.9");
        addUrl(xml, base() + "/others", today, "0.9");
        addUrl(xml, base() + "/faq", today, "0.5");
        addUrl(xml, base() + "/merchants", today, "0.9");
        addUrl(xml, base() + "/privacy", today, "0.5");
        addUrl(xml, base() + "/about", today, "0.5");
        addUrl(xml, base() + "/contact", today, "0.5");
        addUrl(xml, base() + "/guides", today, "0.7");
        addUrl(xml, base() + "/guides/yuu-stamps", today, "0.6");
        addUrl(xml, base() + "/guides/pns-codes", today, "0.6");
        addUrl(xml, base() + "/guides/supermarket-tips", today, "0.6");

        for (Discount d : discountRepository.findAllByOrderByIdDesc()) {
            // 待審草稿 (PENDING) 唔好入 sitemap
            if (!d.isExpired() && d.isPublished()) {
                addUrl(xml, base() + "/discount/" + d.getId(), null, "0.8");
            }
        }

        xml.append("</urlset>\n");
        return xml.toString();
    }

    private void addUrl(StringBuilder xml, String loc, String lastmod, String priority) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(loc).append("</loc>\n");
        if (lastmod != null) {
            xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        }
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }
}
