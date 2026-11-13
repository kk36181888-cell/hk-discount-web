package com.discount.discount_web;

import com.discount.discount_web.model.Discount;
import com.discount.discount_web.repository.DiscountRepository;
import com.discount.discount_web.security.JwtUtil;
import jakarta.servlet.http.Cookie;
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
 * 無封面後備 (2026-11-14)：影片帖等冇封面圖嘅優惠，喺後台列表／待審隊列唔可以出爛圖，
 * 要出黃色文字格。登入用 JwtUtil 即場簽嘅測試 token（同 production 一樣行 cookie interceptor）。
 */
@SpringBootTest
class AdminNoCoverPlaceholderTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DiscountRepository discountRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;
    private Cookie adminCookie;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        adminCookie = new Cookie("DAYBUY_ADMIN_TOKEN", jwtUtil.generateToken("testadmin"));
        discountRepository.deleteAll();
        // 無封面草稿：商戶、標題、來源全部冇商戶名，placeholder 要出「無封面圖」
        Discount noCover = new Discount();
        noCover.setTitle("無封面測試");
        noCover.setStatus("PENDING");
        discountRepository.save(noCover);
        // 有封面公開優惠：照舊出 <img>
        Discount withCover = new Discount();
        withCover.setTitle("有封面測試");
        withCover.setMerchant("百佳");
        withCover.setImageUrl("https://example.com/cover.jpg");
        withCover.setStatus("PUBLISHED");
        discountRepository.save(withCover);
    }

    @Test
    void adminListShowsPlaceholderInsteadOfBrokenImage() throws Exception {
        mockMvc.perform(get("/daybuy-hq").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("無封面圖")))
                .andExpect(content().string(containsString("https://example.com/cover.jpg")))
                .andExpect(content().string(not(containsString("src=\"\""))));
    }

    @Test
    void reviewQueueShowsPlaceholderInsteadOfBrokenImage() throws Exception {
        mockMvc.perform(get("/daybuy-hq/review").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("無封面測試")))
                .andExpect(content().string(containsString("無封面圖")))
                .andExpect(content().string(not(containsString("src=\"\""))));
    }

    @Test
    void adminListWithoutLoginRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/daybuy-hq"))
                .andExpect(status().is3xxRedirection());
    }
}
