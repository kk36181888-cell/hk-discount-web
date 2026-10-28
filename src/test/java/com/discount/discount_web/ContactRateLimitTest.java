package com.discount.discount_web;

import com.discount.discount_web.repository.ContactMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contact-form rate limit: 10 submissions per IP per hour. Uses a dedicated
 * TEST-NET X-Forwarded-For address so it never consumes the quota of other
 * tests (which use the default loopback address).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ContactRateLimitTest {

    private static final Pattern CSRF_INPUT =
            Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");
    private static final String TEST_IP = "203.0.113.99";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    @Test
    void eleventhSubmissionWithinAnHourIsRejected() throws Exception {
        MvcResult form = mockMvc.perform(get("/contact")).andExpect(status().isOk()).andReturn();
        Matcher m = CSRF_INPUT.matcher(form.getResponse().getContentAsString());
        assertTrue(m.find(), "contact form should render a _csrf hidden input");
        String token = m.group(1);
        MockHttpSession session = (MockHttpSession) form.getRequest().getSession(false);

        long before = contactMessageRepository.count();
        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(post("/contact")
                            .session(session)
                            .header("X-Forwarded-For", TEST_IP)
                            .param("_csrf", token)
                            .param("subject", "rate-limit-test-" + i)
                            .param("message", "allowed"))
                    .andExpect(status().isOk());
        }
        assertEquals(before + 10, contactMessageRepository.count());

        mockMvc.perform(post("/contact")
                        .session(session)
                        .header("X-Forwarded-For", TEST_IP)
                        .param("_csrf", token)
                        .param("subject", "rate-limit-test-11")
                        .param("message", "must be rejected"))
                .andExpect(status().isOk());
        assertEquals(before + 10, contactMessageRepository.count());
    }
}
