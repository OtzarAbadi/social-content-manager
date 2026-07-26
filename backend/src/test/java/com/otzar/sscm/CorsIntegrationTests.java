package com.otzar.sscm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.http.Cookie;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsIntegrationTests {

    private static final String LOCALHOST_ORIGIN = "http://localhost:5173";
    private static final String LAN_ORIGIN = "http://192.168.1.139:5173";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RequestMappingHandlerMapping handlerMapping;

    @Test
    void configuredCorsUsesOriginPatternsWithoutWildcardAllowedOrigin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/users/login");
        request.addHeader(HttpHeaders.ORIGIN, LOCALHOST_ORIGIN);
        HandlerExecutionChain chain = handlerMapping.getHandler(request);
        assertNotNull(chain);

        CorsConfiguration configuration = null;
        for (HandlerInterceptor interceptor : chain.getInterceptors()) {
            if (interceptor.getClass().getSimpleName().equals("CorsInterceptor")) {
                configuration = (CorsConfiguration) ReflectionTestUtils.getField(interceptor, "config");
                break;
            }
        }
        assertNotNull(configuration);
        assertFalse(configuration.getAllowedOrigins().contains("*"));
        assertTrue(configuration.getAllowedOriginPatterns().containsAll(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://192.168.*.*:5173",
                "http://10.*.*.*:5173",
                "http://172.*.*.*:5173")));
        assertEquals(Boolean.TRUE, configuration.getAllowCredentials());
    }

    @Test
    void allowsLocalhostFrontendOrigin() throws Exception {
        performLogin(LOCALHOST_ORIGIN)
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LOCALHOST_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void allowsLanFrontendOrigin() throws Exception {
        performLogin(LAN_ORIGIN)
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LAN_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void allowsApiPreflight() throws Exception {
        mockMvc.perform(options("/users/login")
                        .header(HttpHeaders.ORIGIN, LAN_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                "Content-Type, Authorization, Accept, X-Requested-With"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LAN_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        org.hamcrest.Matchers.containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        org.hamcrest.Matchers.containsString("Content-Type")));
    }

    @Test
    void allowsCredentialedRequest() throws Exception {
        String loginBody = performLogin(LAN_ORIGIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();
        Cookie token = new Cookie("token", objectMapper.readTree(loginBody).get("token").asText());

        mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.ORIGIN, LAN_ORIGIN)
                        .cookie(token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LAN_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    private org.springframework.test.web.servlet.ResultActions performLogin(String origin) throws Exception {
        return mockMvc.perform(post("/users/login")
                .header(HttpHeaders.ORIGIN, origin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"123456\"}"));
    }
}
