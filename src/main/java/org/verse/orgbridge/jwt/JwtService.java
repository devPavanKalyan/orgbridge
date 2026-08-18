package org.verse.orgbridge.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.verse.orgbridge.vault.VaultService;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final VaultService vaultService;

    /* -------------------- Signing Key -------------------- */

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                vaultService.jwtSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    /* -------------------- Token Generation -------------------- */

    public String generateToken(Map<String, Object> claims, String subject) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + vaultService.jwtExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(String subject) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + vaultService.jwtRefreshExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    /* -------------------- Claim Extraction -------------------- */

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /* -------------------- Validation (FIXED) -------------------- */

    public boolean validateToken(String token, String expectedSubject) {
        try {
            Claims claims = parseClaims(token);

            String tokenSubject = claims.getSubject();
            Date expiration = claims.getExpiration();

            return tokenSubject != null
                    && tokenSubject.equals(expectedSubject)
                    && expiration.after(new Date());

        } catch (JwtException | IllegalArgumentException ex) {
            // Malformed, expired, unsupported, invalid signature
            return false;
        }
    }

    /* -------------------- Reactive Wrappers -------------------- */

    public Mono<String> extractUsernameReactive(String token) {
        return Mono.fromCallable(() -> extractUsername(token));
    }

    public Mono<Boolean> validateTokenReactive(String token, String subject) {
        return Mono.fromCallable(() -> validateToken(token, subject));
    }
}
