package com.yuelock.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {
  private final SecretKey key;
  private final long expireHours;

  public JwtService(
      @Value("${yuelock.jwt.secret}") String secret,
      @Value("${yuelock.jwt.expire-hours:168}") long expireHours) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expireHours = expireHours;
  }

  public String issue(String userId, String email) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(userId)
        .claim("email", email)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(expireHours, ChronoUnit.HOURS)))
        .signWith(key)
        .compact();
  }

  public Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
