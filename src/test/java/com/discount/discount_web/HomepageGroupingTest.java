package com.discount.discount_web;

import com.discount.discount_web.model.Discount;
import com.discount.discount_web.repository.DiscountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 首頁按商戶分組 (2026-11-14)：首頁要按商戶出 section＋快速跳轉 chips，
 * 分類頁保持扁平列表。用真實 Thymeleaf 渲染驗證，fragment 引用錯咗會即時爆。
 */
@SpringBootTest
class HomepageGroupingTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DiscountRepository discountRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        discountRepository.deleteAll();
        // 百佳 4 張（第 4 張要收喺「睇埋其餘 1 張」）、惠康 1 張
        save("分組測試百佳A", "百佳", 10);
        save("分組測試百佳B", "百佳", 20);
        save("分組測試百佳C", "百佳", 30);
        save("分組測試百佳D", "百佳", 40);
        save("分組測試惠康A", "惠康", 5);
    }

    private void save(String title, String merchant, int views) {
        Discount d = new Discount();
        d.setTitle(title);
        d.setMerchant(merchant);
        d.setViewCount(views);
        discountRepository.save(d);
    }

    @Test
    void homepageGroupsByMerchantWithChips() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                // 快速跳轉 chips
                .andExpect(content().string(containsString("跳去商戶")))
                .andExpect(content().string(containsString("href=\"#百佳\"")))
                .andExpect(content().string(containsString("href=\"#惠康\"")))
                // 商戶 section：名＋張數徽章＋睇晒全部
                .andExpect(content().string(containsString("id=\"百佳\"")))
                .andExpect(content().string(containsString("4 個優惠")))
                .andExpect(content().string(containsString("1 個優惠")))
                .andExpect(content().string(containsString("睇晒全部")))
                // 超出 3 張嘅收起連結；第 4 張（百佳A）唔直接顯示
                .andExpect(content().string(containsString("睇埋其餘")))
                // 每張卡照舊用同一 fragment 渲染（百佳D／B 係頭 3 張，百佳A 係第 4 張要收起）
                .andExpect(content().string(containsString("分組測試百佳D")))
                .andExpect(content().string(containsString("分組測試百佳B")))
                .andExpect(content().string(not(containsString("分組測試百佳A"))))
                .andExpect(content().string(containsString("分組測試惠康A")));
    }

    @Test
    void homepageHotSortStillRendersGrouped() throws Exception {
        mockMvc.perform(get("/").param("sort", "hot"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("跳去商戶")))
                .andExpect(content().string(containsString("id=\"百佳\"")));
    }

    @Test
    void categoryPageStaysFlatWithoutChips() throws Exception {
        mockMvc.perform(get("/supermarket"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("aria-label=\"快速跳去商戶\""))))
                .andExpect(content().string(not(containsString("睇埋其餘"))));
    }
}
