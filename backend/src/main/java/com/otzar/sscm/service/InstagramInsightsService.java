package com.otzar.sscm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class InstagramInsightsService {
    private static final Logger logger = LoggerFactory.getLogger(InstagramInsightsService.class);
    private static final List<String> ACCOUNT_METRICS = List.of(
            "reach", "views", "profile_views", "accounts_engaged",
            "total_interactions", "follows_and_unfollows");
    private static final List<String> COMMON_MEDIA_METRICS = List.of(
            "reach", "saved", "shares", "total_interactions", "likes", "comments");
    private static final List<String> VIDEO_MEDIA_METRICS = List.of(
            "ig_reels_avg_watch_time", "ig_reels_video_view_total_time");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String instagramUserId;
    private final String accessToken;
    private final String graphApiBaseUrl;
    private final InstagramConnectionSettingsService connectionSettings;
    private final ThreadLocal<Long> requestClientId = new ThreadLocal<>();

    @Autowired
    public InstagramInsightsService(
            @Value("${META_INSTAGRAM_USER_ID:}") String instagramUserId,
            @Value("${META_PAGE_ACCESS_TOKEN:}") String accessToken,
            @Value("${META_GRAPH_API_BASE_URL:${META_GRAPH_API_BASE:https://graph.facebook.com/v25.0}}") String graphApiBaseUrl,
            InstagramConnectionSettingsService connectionSettings) {
        this(new RestTemplate(), new ObjectMapper(), instagramUserId, accessToken, graphApiBaseUrl,
                connectionSettings);
    }

    public InstagramInsightsService(RestTemplate restTemplate, ObjectMapper objectMapper,
                                    String instagramUserId, String accessToken, String graphApiBaseUrl) {
        this(restTemplate, objectMapper, instagramUserId, accessToken, graphApiBaseUrl, null);
    }

    private InstagramInsightsService(RestTemplate restTemplate, ObjectMapper objectMapper,
                                    String instagramUserId, String accessToken, String graphApiBaseUrl,
                                    InstagramConnectionSettingsService connectionSettings) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.instagramUserId = clean(instagramUserId);
        this.accessToken = clean(accessToken);
        this.graphApiBaseUrl = clean(graphApiBaseUrl).replaceAll("/+$", "");
        this.connectionSettings = connectionSettings;
    }

    public Map<String, Object> account(LocalDate since, LocalDate until, String period) {
        validate(since, until);
        String configuredAccountId = currentInstagramUserId();
        JsonNode basic = get(configuredAccountId, Map.of("fields", "id,username"));
        String returnedAccountId = text(basic, "id");
        if (!configuredAccountId.equals(returnedAccountId)) {
            throw new InstagramInsightsException("INVALID_ACCOUNT_ID",
                    "The configured ID is not the Instagram professional account returned by Meta",
                    HttpStatus.BAD_REQUEST);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("instagramUserId", returnedAccountId);
        result.put("username", text(basic, "username"));
        List<String> unavailable = new ArrayList<>();
        Map<String, String> optionalFields = new LinkedHashMap<>();
        optionalFields.put("name", "displayName");
        optionalFields.put("profile_picture_url", "profilePictureUrl");
        optionalFields.put("followers_count", "followersCount");
        optionalFields.put("media_count", "mediaCount");
        for (Map.Entry<String, String> field : optionalFields.entrySet()) {
            try {
                JsonNode fieldResponse = get(currentInstagramUserId(), Map.of("fields", field.getKey()));
                JsonNode value = fieldResponse.get(field.getKey());
                result.put(field.getValue(), value == null || value.isNull() ? null
                        : value.isNumber() ? value.numberValue() : value.asText());
            } catch (InstagramInsightsException exception) {
                if (isFatal(exception)) throw exception;
                unavailable.add(field.getKey());
                result.put(field.getValue(), null);
            }
        }
        result.put("since", since);
        result.put("until", until);
        Map<String, Number> values = new LinkedHashMap<>();
        List<Map<String, Object>> trend = new ArrayList<>();
        for (String metric : ACCOUNT_METRICS) {
            try {
                Map<String, String> totalParams = insightParams(metric, since, until, period);
                totalParams.put("metric_type", "total_value");
                if ("follows_and_unfollows".equals(metric)) totalParams.put("breakdown", "follow_type");
                JsonNode response = get(currentInstagramUserId() + "/insights", totalParams);
                JsonNode item = response.path("data").path(0);
                if ("follows_and_unfollows".equals(metric)) {
                    JsonNode followerValue = item.path("total_value").path("breakdowns").path(0)
                            .path("results").isArray()
                            ? followerBreakdown(item.path("total_value").path("breakdowns").path(0).path("results"))
                            : item.path("values").path(0).path("value");
                    values.put("follows", objectNumber(followerValue, "follows"));
                    values.put("unfollows", objectNumber(followerValue, "unfollows"));
                } else {
                    values.put(metric, metricValue(item));
                }
                try {
                    Map<String, String> trendParams = insightParams(metric, since, until, period);
                    trendParams.put("metric_type", "time_series");
                    JsonNode trendItem = get(currentInstagramUserId() + "/insights", trendParams)
                            .path("data").path(0);
                    mergeTrend(trend, trendItem.path("values"), metric);
                } catch (InstagramInsightsException trendException) {
                    if (isFatal(trendException)) throw trendException;
                }
            } catch (InstagramInsightsException exception) {
                if (isFatal(exception)) throw exception;
                unavailable.add(metric);
                values.put(metric, null);
            }
        }
        putAccountValues(result, values);
        result.put("dailyTrend", trend);
        result.put("unavailableMetrics", unavailable);
        result.put("engagementRate", percentage(values.get("total_interactions"), values.get("reach")));
        return result;
    }

    public Map<String, Object> account(Long clientId, LocalDate since, LocalDate until, String period) {
        requestClientId.set(clientId);
        try { return account(since, until, period); }
        finally { requestClientId.remove(); }
    }

    private JsonNode followerBreakdown(JsonNode results) {
        com.fasterxml.jackson.databind.node.ObjectNode value = objectMapper.createObjectNode();
        for (JsonNode result : results) {
            String dimension = result.path("dimension_values").path(0).asText("");
            if ("FOLLOW".equalsIgnoreCase(dimension) || "follows".equalsIgnoreCase(dimension))
                value.put("follows", result.path("value").asDouble());
            if ("UNFOLLOW".equalsIgnoreCase(dimension) || "unfollows".equalsIgnoreCase(dimension))
                value.put("unfollows", result.path("value").asDouble());
        }
        return value;
    }

    public Map<String, Object> media(LocalDate since, LocalDate until, String mediaType,
                                     int limit, String after) {
        validate(since, until);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("fields", "id,caption,media_type,media_product_type,media_url,thumbnail_url,permalink,timestamp,like_count,comments_count");
        params.put("limit", String.valueOf(Math.max(1, Math.min(limit, 100))));
        if (after != null && !after.isBlank()) params.put("after", after);
        JsonNode response = get(currentInstagramUserId() + "/media", params);
        List<Map<String, Object>> items = new ArrayList<>();
        for (JsonNode node : response.path("data")) {
            if (!inRange(node, since, until) || !matchesType(node, mediaType)) continue;
            try {
                items.add(enrichMedia(node));
            } catch (InstagramInsightsException exception) {
                if (isFatal(exception)) throw exception;
                items.add(baseMedia(node, new ArrayList<>(COMMON_MEDIA_METRICS)));
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("after", nullableText(response.path("paging").path("cursors"), "after"));
        result.put("hasNext", response.path("paging").has("next"));
        result.put("topByReach", top(items, "reach"));
        result.put("topByViews", top(items, "views"));
        result.put("topByEngagement", top(items, "engagementRate"));
        result.put("topByInteractions", top(items, "totalInteractions"));
        return result;
    }

    public Map<String, Object> oneMedia(String mediaId) {
        Map<String, Object> page = media(null, null, "ALL", 100, null);
        return ((List<Map<String, Object>>) page.get("items")).stream()
                .filter(item -> mediaId.equals(item.get("mediaId"))).findFirst()
                .orElseThrow(() -> new InstagramInsightsException(
                        "MEDIA_NOT_FOUND", "Instagram media was not found for this account", HttpStatus.NOT_FOUND));
    }

    private Map<String, Object> enrichMedia(JsonNode node) {
        List<String> metrics = new ArrayList<>(COMMON_MEDIA_METRICS);
        String product = text(node, "media_product_type");
        String type = text(node, "media_type");
        if ("VIDEO".equals(type) || "REELS".equals(product)) metrics.addAll(VIDEO_MEDIA_METRICS);
        List<String> unavailable = new ArrayList<>();
        Map<String, Number> values = new HashMap<>();
        try {
            JsonNode response = get(text(node, "id") + "/insights",
                    Map.of("metric", String.join(",", metrics)));
            for (JsonNode metricNode : response.path("data")) {
                String metricName = text(metricNode, "name");
                if (!metricName.isBlank()) values.put(metricName, metricValue(metricNode));
            }
            for (String metric : metrics) if (!values.containsKey(metric)) unavailable.add(metric);
        } catch (InstagramInsightsException exception) {
            if (isFatal(exception)) throw exception;
            unavailable.addAll(metrics);
        }
        Map<String, Object> result = baseMedia(node, unavailable);
        result.put("reach", values.get("reach"));
        result.put("views", null);
        result.put("likes", values.containsKey("likes") ? values.get("likes") : number(node, "like_count"));
        result.put("comments", values.containsKey("comments") ? values.get("comments") : number(node, "comments_count"));
        result.put("saved", values.get("saved"));
        result.put("shares", values.get("shares"));
        result.put("totalInteractions", values.get("total_interactions"));
        result.put("averageWatchTime", values.get("ig_reels_avg_watch_time"));
        result.put("videoViewTotalTime", values.get("ig_reels_video_view_total_time"));
        result.put("follows", null);
        result.put("profileActivity", null);
        result.put("engagementRate", percentage(values.get("total_interactions"), values.get("reach")));
        return result;
    }

    private Map<String, Object> baseMedia(JsonNode node, List<String> unavailable) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mediaId", text(node, "id"));
        for (String key : List.of("caption","media_type","media_product_type","media_url",
                "thumbnail_url","permalink","timestamp")) result.put(camel(key), nullableText(node, key));
        result.put("unavailableMetrics", unavailable);
        return result;
    }

    private JsonNode get(String path, Map<String, String> params) {
        requireConfiguration();
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(currentGraphApiBaseUrl())
                .pathSegment(path.split("/"));
        params.forEach(builder::queryParam);
        URI uri = builder.build().encode().toUri();
        String safePath = safeMetaPath(path);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(currentAccessToken());
            ResponseEntity<String> response = restTemplate.exchange(
                    uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            logger.debug("Meta Insights request succeeded path={} status={}",
                    safePath, response.getStatusCodeValue());
            String body = response.getBody();
            return objectMapper.readTree(body == null ? "{}" : body);
        } catch (HttpStatusCodeException exception) {
            boolean identityRequest = !path.contains("/")
                    && "id,username".equals(params.get("fields"));
            throw mapMeta(safePath, identityRequest, exception);
        } catch (RestClientException exception) {
            logger.warn("Meta Insights transport failure path={} type={}",
                    safePath, exception.getClass().getSimpleName());
            throw new InstagramInsightsException("META_TEMPORARY",
                    "Meta Insights is temporarily unavailable", HttpStatus.BAD_GATEWAY);
        } catch (Exception exception) {
            logger.warn("Meta Insights invalid response path={} type={}",
                    safePath, exception.getClass().getSimpleName());
            throw new InstagramInsightsException("META_INVALID_RESPONSE",
                    "Meta returned an invalid Insights response", HttpStatus.BAD_GATEWAY);
        }
    }

    private InstagramInsightsException mapMeta(String safePath, boolean identityRequest,
                                               HttpStatusCodeException exception) {
        int status = exception.getRawStatusCode();
        Integer code = null;
        Integer subcode = null;
        String type = "";
        String message = "";
        try {
            JsonNode error = objectMapper.readTree(exception.getResponseBodyAsString()).path("error");
            code = error.path("code").isInt() ? error.path("code").intValue() : null;
            subcode = error.path("error_subcode").isInt() ? error.path("error_subcode").intValue() : null;
            type = sanitizeMetaMessage(error.path("type").asText(""));
            message = sanitizeMetaMessage(error.path("message").asText(""));
        } catch (Exception ignored) {
            message = "unparseable Meta error";
        }
        logger.warn("Meta Insights request failed path={} status={} metaType={} metaCode={} metaSubcode={} message={}",
                safePath, status, type, code, subcode, message);

        if (status >= 500)
            return new InstagramInsightsException("META_TEMPORARY",
                    "Meta Insights is temporarily unavailable", HttpStatus.BAD_GATEWAY);
        if (status == 404)
            return new InstagramInsightsException("META_NOT_FOUND",
                    "The requested Instagram resource was not found", HttpStatus.NOT_FOUND);
        if (status == 429 || Integer.valueOf(4).equals(code))
            return new InstagramInsightsException("RATE_LIMIT",
                    "Meta rate limit reached", HttpStatus.TOO_MANY_REQUESTS);
        if (Integer.valueOf(190).equals(code))
            return new InstagramInsightsException("TOKEN_INVALID",
                    "The Meta access token is invalid or expired", HttpStatus.UNAUTHORIZED);
        if (Integer.valueOf(10).equals(code) || Integer.valueOf(200).equals(code))
            return new InstagramInsightsException("MISSING_PERMISSION",
                    "The instagram_manage_insights permission is required", HttpStatus.FORBIDDEN);
        if (Integer.valueOf(100).equals(code) && identityRequest)
            return new InstagramInsightsException("INVALID_ACCOUNT_ID",
                    "Meta did not recognize the configured Instagram professional account ID",
                    HttpStatus.BAD_REQUEST);
        if (Integer.valueOf(100).equals(code))
            return new InstagramInsightsException("UNSUPPORTED_METRIC",
                    "This Instagram metric or request parameter is unavailable", HttpStatus.BAD_REQUEST);
        return new InstagramInsightsException("META_REQUEST_ERROR",
                "Meta rejected the Instagram Insights request", HttpStatus.BAD_REQUEST);
    }

    private String sanitizeMetaMessage(String message) {
        if (message == null || message.isBlank()) return "not provided";
        String sanitized = message
                .replaceAll("(?i)(access[_ ]?token[=:\\s]+)[^\\s,;]+", "$1[redacted]")
                .replaceAll("(?i)bearer\\s+[^\\s,;]+", "Bearer [redacted]");
        return sanitized.length() > 240 ? sanitized.substring(0, 240) : sanitized;
    }

    private void validate(LocalDate since, LocalDate until) {
        if (since != null && until != null && (since.isAfter(until) || ChronoUnit.DAYS.between(since, until) > 93))
            throw new InstagramInsightsException("INVALID_DATE_RANGE",
                    "Invalid Insights date range", HttpStatus.BAD_REQUEST);
    }

    private void requireConfiguration() {
        boolean accountIdPresent = !currentInstagramUserId().isEmpty();
        boolean tokenPresent = !currentAccessToken().isEmpty();
        boolean graphUrlValid = currentGraphApiBaseUrl()
                .matches("^https://graph\\.facebook\\.com/v[0-9]+\\.[0-9]+$");
        logger.info("Instagram Insights configuration accountIdPresent={} tokenPresent={} graphUrlValid={}",
                accountIdPresent, tokenPresent, graphUrlValid);
        if (!accountIdPresent || !tokenPresent || !graphUrlValid)
            throw new InstagramInsightsException("NOT_CONFIGURED",
                    configurationMessage(accountIdPresent, tokenPresent, graphUrlValid),
                    HttpStatus.SERVICE_UNAVAILABLE);
    }

    public Map<String, Object> oneMedia(Long clientId, String mediaId) {
        requestClientId.set(clientId);
        try { return oneMedia(mediaId); }
        finally { requestClientId.remove(); }
    }

    public Map<String, Object> media(Long clientId, LocalDate since, LocalDate until, String mediaType,
                                     int limit, String after) {
        requestClientId.set(clientId);
        try { return media(since, until, mediaType, limit, after); }
        finally { requestClientId.remove(); }
    }

    private String safeMetaPath(String path) {
        if (path == null || path.isBlank()) return "/instagram";
        if (path.endsWith("/insights")) return "/instagram/insights";
        if (path.endsWith("/media")) return "/instagram/media";
        return "/instagram/account";
    }

    private String configurationMessage(boolean accountIdPresent, boolean tokenPresent, boolean graphUrlValid) {
        List<String> missing = new ArrayList<>();
        if (!accountIdPresent) missing.add("Instagram professional account ID");
        if (!tokenPresent) missing.add("Meta access token");
        if (!graphUrlValid) missing.add("versioned Meta Graph API URL");
        return "Instagram Insights configuration is missing or invalid: " + String.join(", ", missing);
    }

    private Map<String,String> insightParams(String metric, LocalDate since, LocalDate until, String period) {
        Map<String,String> p = new LinkedHashMap<>();
        p.put("metric", metric);
        p.put("period", period == null || period.isBlank() ? "day" : period);
        if (since != null) p.put("since", since.toString());
        if (until != null) p.put("until", until.toString());
        return p;
    }

    private void putAccountValues(Map<String,Object> out, Map<String,Number> v) {
        out.put("reach", v.get("reach")); out.put("views", v.get("views"));
        out.put("profileViews", v.get("profile_views")); out.put("accountsEngaged", v.get("accounts_engaged"));
        out.put("totalInteractions", v.get("total_interactions"));
        Number follows = v.get("follows"), unfollows = v.get("unfollows");
        out.put("follows", follows); out.put("unfollows", unfollows);
        out.put("netFollowerChange", follows == null || unfollows == null ? null
                : follows.doubleValue() - unfollows.doubleValue());
    }

    private void mergeTrend(List<Map<String,Object>> trend, JsonNode values, String metric) {
        for (JsonNode value : values) {
            String date = text(value, "end_time");
            Map<String,Object> row = trend.stream().filter(r -> date.equals(r.get("date"))).findFirst()
                    .orElseGet(() -> { Map<String,Object> r=new LinkedHashMap<>(); r.put("date",date); trend.add(r); return r; });
            JsonNode metricValue = value.path("value");
            if ("follows_and_unfollows".equals(metric) && metricValue.isObject()) {
                Number follows = objectNumber(metricValue, "follows");
                Number unfollows = objectNumber(metricValue, "unfollows");
                row.put("follows", follows);
                row.put("unfollows", unfollows);
                row.put("netFollowerChange", follows == null || unfollows == null ? null
                        : follows.doubleValue() - unfollows.doubleValue());
            } else {
                row.put(camel(metric), metricValue.isNumber() ? metricValue.numberValue() : null);
            }
        }
    }

    private Number metricValue(JsonNode item) {
        JsonNode total = item.path("total_value").path("value");
        if (total.isNumber()) return total.numberValue();
        JsonNode value = item.path("values").path(0).path("value");
        return value.isNumber() ? value.numberValue() : null;
    }
    private Map<String,Object> top(List<Map<String,Object>> items,String key) {
        return items.stream().filter(i -> i.get(key) instanceof Number)
                .max(Comparator.comparingDouble(i -> ((Number)i.get(key)).doubleValue())).orElse(null);
    }
    private boolean inRange(JsonNode n,LocalDate s,LocalDate u) {
        if (s==null&&u==null) return true;
        try { LocalDate d=LocalDate.parse(text(n,"timestamp").substring(0,10)); return (s==null||!d.isBefore(s))&&(u==null||!d.isAfter(u)); }
        catch(Exception e){ return true; }
    }
    private boolean matchesType(JsonNode n,String filter) {
        if(filter==null||filter.equals("ALL")) return true;
        String type=text(n,"media_type"), product=text(n,"media_product_type");
        if(filter.equals("REELS")) return product.equals("REELS");
        if(filter.equals("VIDEO")) return type.equals("VIDEO") && !product.equals("REELS");
        return type.equals(filter);
    }
    private Double percentage(Number a,Number b){ return a==null||b==null||b.doubleValue()==0?null:a.doubleValue()/b.doubleValue()*100; }
    private boolean isFatal(InstagramInsightsException e){ return Set.of(
            "NOT_CONFIGURED", "INVALID_ACCOUNT_ID", "UNSUPPORTED_ACCOUNT",
            "MISSING_PERMISSION", "TOKEN_INVALID", "RATE_LIMIT", "META_TEMPORARY"
    ).contains(e.getCode()); }
    private String text(JsonNode n,String k){ return n.path(k).asText(""); }
    private String nullableText(JsonNode n,String k){ return n.hasNonNull(k)?n.get(k).asText():null; }
    private Number number(JsonNode n,String k){ return n.has(k)&&n.get(k).isNumber()?n.get(k).numberValue():null; }
    private Number objectNumber(JsonNode n,String k){ return n.isObject()&&n.path(k).isNumber()?n.path(k).numberValue():null; }
    private String camel(String s){ StringBuilder b=new StringBuilder(); boolean up=false; for(char c:s.toCharArray()){if(c=='_'){up=true;}else{b.append(up?Character.toUpperCase(c):c);up=false;}} return b.toString(); }
    private String clean(String s){ return s==null?"":s.trim(); }
    private String currentInstagramUserId() {
        if (connectionSettings == null) return instagramUserId;
        Long clientId = requestClientId.get();
        return clean(clientId == null ? connectionSettings.instagramUserId() : connectionSettings.instagramUserId(clientId));
    }
    private String currentGraphApiBaseUrl() {
        if (connectionSettings == null) return graphApiBaseUrl;
        Long clientId = requestClientId.get();
        return clean(clientId == null ? connectionSettings.graphApiBaseUrl() : connectionSettings.graphApiBaseUrl(clientId)).replaceAll("/+$", "");
    }
    private String currentAccessToken() {
        return connectionSettings == null ? accessToken : clean(connectionSettings.accessToken());
    }
}
