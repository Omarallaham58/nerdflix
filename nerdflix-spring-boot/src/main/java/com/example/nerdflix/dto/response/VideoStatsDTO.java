package com.example.nerdflix.dto.response;

public record VideoStatsDTO(
        Integer views,
        Integer likes,
        Boolean likedByCurrentUser
) {}

