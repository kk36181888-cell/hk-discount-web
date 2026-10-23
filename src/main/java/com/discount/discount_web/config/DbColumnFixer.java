package com.discount.discount_web.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 🔧 一次性欄位闊度修復 (2026-10-23)
 *
 * 背景：Discount entity 嘅 description 早就標咗 @Column(length = 2000)，
 * 但 Hibernate ddl-auto=update 只會新增表/欄，永遠唔會改短咗嘅既有欄位 —
 * 線上 discounts.description 一直停喺最初建表嘅 varchar(255)，
 * 描述一過 255 字存檔就 PSQLException: value too long（2026-10-23 後台入貨實測中招）。
 *
 * 呢個 Runner 每次開機檢查 information_schema，欄位窄過目標就 ALTER 拉闊；
 * 達標就乜都唔做，冪等安全。順手檢查埋另外幾個有同樣風險嘅欄位。
 */
@Component
public class DbColumnFixer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public DbColumnFixer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        widenVarchar("description", 5000);
        widenVarchar("image_url", 1000);
        widenVarchar("source_url", 1000);
        widenToText("additional_images");
    }

    private void widenVarchar(String column, int targetLength) {
        try {
            Integer len = jdbcTemplate.queryForObject(
                    "SELECT character_maximum_length FROM information_schema.columns " +
                    "WHERE table_name = 'discounts' AND column_name = '" + column + "'",
                    Integer.class);
            if (len != null && len < targetLength) {
                jdbcTemplate.execute("ALTER TABLE discounts ALTER COLUMN " + column +
                        " TYPE varchar(" + targetLength + ")");
                System.out.println("🔧 discounts." + column + " 已由 varchar(" + len +
                        ") 擴闊到 varchar(" + targetLength + ")");
            }
        } catch (Exception e) {
            System.out.println("⚠️ discounts." + column + " 欄位檢查/擴闊失敗: " + e.getMessage());
        }
    }

    private void widenToText(String column) {
        try {
            String dataType = jdbcTemplate.queryForObject(
                    "SELECT data_type FROM information_schema.columns " +
                    "WHERE table_name = 'discounts' AND column_name = '" + column + "'",
                    String.class);
            if (dataType != null && !"text".equals(dataType)) {
                jdbcTemplate.execute("ALTER TABLE discounts ALTER COLUMN " + column + " TYPE TEXT");
                System.out.println("🔧 discounts." + column + " 已由 " + dataType + " 改為 TEXT");
            }
        } catch (Exception e) {
            System.out.println("⚠️ discounts." + column + " 欄位檢查/擴闊失敗: " + e.getMessage());
        }
    }
}
