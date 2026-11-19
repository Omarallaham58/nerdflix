package com.example.nerdflix.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record VideoDTO(
        UUID id,
        String title,
        String description,
        Integer duration,
        LocalDateTime createdAt,
        CategoryDTO category,
        VideoStatsDTO stats
) {}
