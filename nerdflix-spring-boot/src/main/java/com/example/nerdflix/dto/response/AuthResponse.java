package com.example.nerdflix.dto.response;

import java.util.List;

public record AuthResponse(String token, String refreshToken, Integer userId, String username, List<String> roles) {}
