package com.example.nerdflix.service;

import com.example.nerdflix.entity.RefreshToken;
import com.example.nerdflix.entity.User;
import com.example.nerdflix.exception.RefreshTokenNotFoundException;
import com.example.nerdflix.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    public RefreshTokenService(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    public RefreshToken createRefreshToken(User user) {

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(
                LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000)
        );

        return repository.save(token);
    }

    public RefreshToken verifyToken(String tokenValue) {

        RefreshToken token = repository.findByToken(tokenValue)
                .orElseThrow(() -> new RefreshTokenNotFoundException("Refresh Token Not Found"));

        if (token.isRevoked())
            throw new RuntimeException("Refresh token revoked");

        if (token.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new RuntimeException("Refresh token expired");

        return token;
    }

    public void revokeToken(RefreshToken token) {
        token.setRevoked(true);
        repository.save(token);
    }

    public void revokeAllUserTokens(User user) {
        repository.deleteByUser(user);
    }
}

