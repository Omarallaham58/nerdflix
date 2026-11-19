package com.example.nerdflix.dto.request;

import java.util.UUID;

public record VideoActionRequest(
        UUID videoId
) {}
