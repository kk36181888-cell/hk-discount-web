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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CSRF negative test (audit follow-up): POSTs without a CSRF token must be
 * rejected with 403 and must not write anything; the same POST carrying the
 * token rendered in the form must succeed. The token is taken from the real
 * rendered form, so this also proves the contact form actually carries a
 * _csrf field (Thymeleaf th:action forms carry it automatically).
 * No spring-security-test dependency is needed: the token is extracted
 * from the GET response and replayed with the same session.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CsrfProtectionTest {

    private static final Pattern CSRF_INPUT =
            Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    @Test
    void contactFormRendersCsrfToken() throws Exception {
        mockMvc.perform(get("/contact"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("_csrf")));
    }

    @Test
    void contactPostWithoutCsrfIsForbiddenAndWritesNothing() throws Exception {
        long before = contactMessageRepository.count();
        mockMvc.perform(post("/contact")
                        .param("subject", "no-csrf-test")
                        .param("message", "this must not be stored"))
                .andExpect(status().isForbidden());
        assertEquals(before, contactMessageRepository.count());
    }

    @Test
    void contactPostWithCsrfSucceeds() throws Exception {
        MvcResult form = mockMvc.perform(get("/contact")).andExpect(status().isOk()).andReturn();
        String html = form.getResponse().getContentAsString();
        Matcher m = CSRF_INPUT.matcher(html);
        assertTrue(m.find(), "contact form should render a _csrf hidden input");
        String token = m.group(1);
        MockHttpSession session = (MockHttpSession) form.getRequest().getSession(false);

        long before = contactMessageRepository.count();
        mockMvc.perform(post("/contact")
                        .session(session)
                        .param("_csrf", token)
                        .param("subject", "csrf-test")
                        .param("message", "this may be stored"))
                .andExpect(status().isOk());
        assertEquals(before + 1, contactMessageRepository.count());
    }

    @Test
    void adminLoginPostWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post("/daybuy-hq/doLogin")
                        .param("username", "testadmin")
                        .param("password", "wrong"))
                .andExpect(status().isForbidden());
    }
}
