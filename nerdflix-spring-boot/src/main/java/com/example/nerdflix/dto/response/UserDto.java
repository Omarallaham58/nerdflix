package com.example.nerdflix.dto.response;

import com.example.nerdflix.entity.Role;

import java.util.List;

public record UserDto(Integer id, String username, List<String> roles) {
}
