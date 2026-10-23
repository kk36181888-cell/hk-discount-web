package com.discount.discount_web.controller;

import com.discount.discount_web.service.IngestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 🤖 自動入貨觸發 endpoint (2026-10-23)
 * 由 GitHub Actions 每日定時呼叫 (workflow 會先喚醒服務再打)。
 * 安全：靠環境變數 INGEST_KEY (application.properties: daybuy.ingest.key)；
 * 未設定密鑰時 endpoint 直接停用 (503)，密鑰唔啱回 401。
 */
@RestController
public class IngestController {

    @Autowired
    private IngestService ingestService;

    @Value("${daybuy.ingest.key:}")
    private String ingestKey;

    @PostMapping("/api/ingest/run")
    public ResponseEntity<Map<String, Object>> run(
            @RequestHeader(value = "X-Ingest-Key", required = false) String providedKey) {

        if (ingestKey == null || ingestKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "ingest disabled: INGEST_KEY not configured"));
        }
        if (!ingestKey.equals(providedKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid ingest key"));
        }
        return ResponseEntity.ok(ingestService.runIngest());
    }
}
