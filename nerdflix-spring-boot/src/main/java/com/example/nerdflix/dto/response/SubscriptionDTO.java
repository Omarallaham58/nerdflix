package com.example.nerdflix.dto.response;

import java.time.LocalDate;

public record SubscriptionDTO(
        Integer userId,
        Integer categoryId,
        LocalDate startDate,
        LocalDate endDate
) {}