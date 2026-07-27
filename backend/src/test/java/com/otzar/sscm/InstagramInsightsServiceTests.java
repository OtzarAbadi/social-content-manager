package com.otzar.sscm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.otzar.sscm.service.InstagramInsightsException;
import com.otzar.sscm.service.InstagramInsightsService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class InstagramInsightsServiceTests {
    @Test
    void accountSummaryUsesRealResponseAndUnsupportedMetricIsUnavailable() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).ignoreExpectOrder(true).build();
        server.expect(requestTo(anything()))
                .andRespond(withSuccess("{\"id\":\"ig-user\",\"username\":\"studio\",\"followers_count\":42,\"media_count\":3}", MediaType.APPLICATION_JSON));
        server.expect(times(4), requestTo(anything()))
                .andRespond(withSuccess("{\"name\":\"Studio\",\"profile_picture_url\":\"https://example.test/p.jpg\",\"followers_count\":42,\"media_count\":3}", MediaType.APPLICATION_JSON));
        server.expect(manyTimes(), requestTo(anything()))
                .andRespond(withSuccess("{\"data\":[{\"values\":[{\"value\":74,\"end_time\":\"2026-07-27\"}]}]}", MediaType.APPLICATION_JSON));
        InstagramInsightsService service = service(rest);

        Map<String,Object> result = service.account(LocalDate.parse("2026-07-21"), LocalDate.parse("2026-07-27"), "day");

        assertEquals("studio", result.get("username"));
        assertEquals(42, ((Number)result.get("followersCount")).intValue());
        assertEquals(74, ((Number)result.get("reach")).intValue());
        assertFalse(result.containsKey("accessToken"));
        server.verify();
    }

    @Test
    void mediaListLoadsInsightsAndReturnsOnlySafeCursor() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).ignoreExpectOrder(true).build();
        server.expect(requestTo(anything()))
                .andRespond(withSuccess("{\"data\":[{\"id\":\"m1\",\"caption\":\"Real\",\"media_type\":\"IMAGE\",\"timestamp\":\"2026-07-25T10:00:00+0000\",\"like_count\":5,\"comments_count\":2}],\"paging\":{\"cursors\":{\"after\":\"safe-cursor\"},\"next\":\"https://graph.example/next?access_token=secret\"}}", MediaType.APPLICATION_JSON));
        server.expect(manyTimes(), requestTo(anything()))
                .andRespond(withSuccess("{\"data\":[{\"total_value\":{\"value\":10}}]}", MediaType.APPLICATION_JSON));
        Map<String,Object> result = service(rest).media(LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-27"), "ALL", 25, null);

        assertEquals("safe-cursor", result.get("after"));
        assertFalse(result.toString().contains("access_token"));
        assertEquals(1, ((java.util.List<?>)result.get("items")).size());
        server.verify();
    }

    @Test
    void partialUnsupportedMetricsDoNotFailMedia() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).ignoreExpectOrder(true).build();
        server.expect(requestTo(anything()))
                .andRespond(withSuccess("{\"data\":[{\"id\":\"m1\",\"media_type\":\"IMAGE\",\"timestamp\":\"2026-07-25T10:00:00+0000\"}]}", MediaType.APPLICATION_JSON));
        server.expect(manyTimes(), requestTo(anything()))
                .andRespond(withBadRequest().body("{\"error\":{\"code\":100,\"message\":\"Unsupported metric\"}}").contentType(MediaType.APPLICATION_JSON));
        Map<String,Object> result = service(rest).media(null,null,"ALL",25,null);
        Map<?,?> item = (Map<?,?>)((java.util.List<?>)result.get("items")).get(0);
        assertNull(item.get("reach"));
        assertFalse(((java.util.List<?>)item.get("unavailableMetrics")).isEmpty());
    }

    @Test
    void unsupportedAccountMetricDoesNotFailAccountSummary() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
        server.expect(requestTo(anything())).andRespond(withSuccess(
                "{\"id\":\"ig-user\",\"username\":\"studio\"}", MediaType.APPLICATION_JSON));
        server.expect(times(4), requestTo(anything())).andRespond(withSuccess(
                "{\"name\":\"Studio\",\"followers_count\":42,\"media_count\":3}", MediaType.APPLICATION_JSON));
        server.expect(manyTimes(), requestTo(anything())).andRespond(withBadRequest().body(
                "{\"error\":{\"code\":100,\"type\":\"OAuthException\",\"message\":\"Unsupported metric\"}}")
                .contentType(MediaType.APPLICATION_JSON));

        Map<String,Object> result = service(rest).account(null, null, "day");

        assertEquals("studio", result.get("username"));
        assertNull(result.get("reach"));
        assertFalse(((java.util.List<?>)result.get("unavailableMetrics")).isEmpty());
        server.verify();
    }

    @Test
    void metaErrorCodesAreMappedAccurately() {
        assertEquals("MISSING_PERMISSION", failureCode(HttpStatus.BAD_REQUEST,
                "{\"error\":{\"code\":10,\"message\":\"Permission denied\"}}"));
        assertEquals("TOKEN_INVALID", failureCode(HttpStatus.BAD_REQUEST,
                "{\"error\":{\"code\":190,\"message\":\"Invalid OAuth access token\"}}"));
        assertEquals("UNSUPPORTED_METRIC", failureCode(HttpStatus.BAD_REQUEST,
                "{\"error\":{\"code\":100,\"message\":\"Invalid metric\"}}"));
        assertEquals("RATE_LIMIT", failureCode(HttpStatus.BAD_REQUEST,
                "{\"error\":{\"code\":4,\"message\":\"Application request limit reached\"}}"));
        assertEquals("META_TEMPORARY", failureCode(HttpStatus.INTERNAL_SERVER_ERROR,
                "{\"error\":{\"code\":2,\"message\":\"Service temporarily unavailable\"}}"));
        assertEquals("META_NOT_FOUND", failureCode(HttpStatus.NOT_FOUND,
                "{\"error\":{\"message\":\"Unsupported get request\"}}"));
    }

    @Test
    void code100MentioningAccessTokenIsNotMislabelledAsExpiredToken() {
        assertEquals("UNSUPPORTED_METRIC", failureCode(HttpStatus.BAD_REQUEST,
                "{\"error\":{\"code\":100,\"message\":\"The access token cannot query this unsupported metric\"}}"));
    }

    @Test
    void missingConfigurationIsSafeAndDoesNotExposeValues() {
        InstagramInsightsException error = assertThrows(InstagramInsightsException.class,
                () -> new InstagramInsightsService(new RestTemplate(), new ObjectMapper(),
                        "", "", "https://graph.facebook.com/v25.0").account(null, null, "day"));
        assertEquals("NOT_CONFIGURED", error.getCode());
        assertFalse(error.getMessage().contains("token-value"));
    }

    @Test
    void tokenUsesAuthorizationHeaderAndNeverAppearsInRequestUrlOrResponse() {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
        server.expect(requestTo(not(containsString("access_token"))))
                .andExpect(requestTo(not(containsString("secret-token"))))
                .andExpect(header("Authorization", "Bearer secret-token"))
                .andRespond(withBadRequest().body(
                        "{\"error\":{\"code\":100,\"message\":\"Invalid fields\"}}")
                        .contentType(MediaType.APPLICATION_JSON));
        InstagramInsightsException error = assertThrows(InstagramInsightsException.class,
                () -> service(rest).account(null, null, "day"));
        assertFalse(error.getMessage().contains("secret-token"));
        server.verify();
    }

    @Test
    void invalidDateRangeFailsBeforeCallingMeta() {
        InstagramInsightsException error = assertThrows(InstagramInsightsException.class,
                () -> service(new RestTemplate()).account(LocalDate.parse("2026-07-27"), LocalDate.parse("2026-07-01"), "day"));
        assertEquals("INVALID_DATE_RANGE", error.getCode());
    }

    private String failureCode(HttpStatus status, String body) {
        RestTemplate rest = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
        server.expect(requestTo(anything())).andRespond(
                withStatus(status).body(body).contentType(MediaType.APPLICATION_JSON));
        return assertThrows(InstagramInsightsException.class,
                () -> service(rest).account(null,null,"day")).getCode();
    }
    private InstagramInsightsService service(RestTemplate rest) {
        return new InstagramInsightsService(rest,new ObjectMapper(),"ig-user","secret-token","https://graph.example/v25.0");
    }
}
