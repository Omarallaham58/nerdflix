package com.example.nerdflix.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Service
public class JwtTokenService {


    private final SecretKey secretKey;

    private final long expiration;

    private final String issuer;


    public JwtTokenService(@Value("${jwt.secret}")String secret, @Value("${jwt.expiration}")long expiration,
                           @Value("${jwt.issuer}")String issuer) {
        //using HS256
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        this.issuer = issuer;
    }

    public String generateToken(Integer userId, String username, List<String> roles) {

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("roles", roles)
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }


    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }


    public Integer extractUserId(String token) {
        return Integer.parseInt(parseClaims(token).getSubject());
    }


    public String extractUsername(String token) {
        return parseClaims(token).get("username", String.class);
    }

    public List<String> extractRoles(String token) {
        return parseClaims(token).get("roles", List.class);
    }





}
