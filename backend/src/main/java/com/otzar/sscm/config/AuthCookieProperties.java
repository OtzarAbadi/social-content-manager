package com.otzar.sscm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;

@Component
public class AuthCookieProperties {
    private static final Set<String> SAME_SITE_VALUES = Set.of("Lax", "Strict", "None");

    private final boolean secure;
    private final String sameSite;
    private final boolean exposeToken;

    public AuthCookieProperties(
            @Value("${sscm.auth.cookie.secure:false}") boolean secure,
            @Value("${sscm.auth.cookie.same-site:Lax}") String sameSite,
            @Value("${sscm.auth.cookie.expose-token:true}") boolean exposeToken) {
        this.secure = secure;
        this.sameSite = normalizeSameSite(sameSite);
        this.exposeToken = exposeToken;
        if ("None".equals(this.sameSite) && !secure) {
            throw new IllegalStateException("SameSite=None authentication cookies must be Secure");
        }
    }

    public ResponseCookie authenticated(String token) {
        return base(token).maxAge(Duration.ofDays(7)).build();
    }

    public ResponseCookie expired() {
        return base("").maxAge(Duration.ZERO).build();
    }

    public boolean isExposeToken() {
        return exposeToken;
    }

    boolean isSecure() {
        return secure;
    }

    String getSameSite() {
        return sameSite;
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from("token", value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/");
    }

    private static String normalizeSameSite(String value) {
        String cleaned = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        String normalized = cleaned.isEmpty()
                ? "Lax"
                : Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
        if (!SAME_SITE_VALUES.contains(normalized)) {
            throw new IllegalStateException("COOKIE_SAME_SITE must be Lax, Strict, or None");
        }
        return normalized;
    }
}
