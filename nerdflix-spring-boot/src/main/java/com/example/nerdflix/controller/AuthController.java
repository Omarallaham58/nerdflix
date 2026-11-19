package com.example.nerdflix.controller;
import com.example.nerdflix.dto.request.CreateUserRequest;
//import com.example.nerdflix.dto.request.RefreshRequest;
import com.example.nerdflix.dto.request.RefreshRequest;
import com.example.nerdflix.dto.response.AuthResponse;
import com.example.nerdflix.dto.response.UserDto;
import com.example.nerdflix.entity.RefreshToken;
import com.example.nerdflix.entity.Role;
import com.example.nerdflix.entity.User;
import com.example.nerdflix.service.JwtTokenService;
import com.example.nerdflix.service.RefreshTokenService;
import com.example.nerdflix.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {//Checked

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(UserService userService, AuthenticationManager authenticationManager,
                          JwtTokenService jwtTokenService, RefreshTokenService refreshTokenService) {

        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
    }


    @PostMapping("/register")
    public UserDto register(@Valid @RequestBody CreateUserRequest request) {

        return userService.createUser(request);
    }


    @PostMapping("/login")
    public AuthResponse login(@RequestBody CreateUserRequest request) {

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userService.getUserEntityForAuth(request.getUsername());

        String accessToken = jwtTokenService.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .toList()
        );

        RefreshToken refreshToken =
                refreshTokenService.createRefreshToken(user);

        return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                user.getId(),
                user.getUsername(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .toList()
        );
    }


    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody RefreshRequest request) {

        RefreshToken refreshToken = refreshTokenService.verifyToken(request.refreshToken());

        User user = refreshToken.getUser();

        String newAccessToken = jwtTokenService.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .toList()
        );

        return new AuthResponse(
                newAccessToken,
                refreshToken.getToken(), // same refresh token
                user.getId(),
                user.getUsername(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .toList()
        );
    }

    @PostMapping("/logout")
    public void logout(@RequestBody RefreshRequest request) {

        RefreshToken token =
                refreshTokenService.verifyToken(request.refreshToken());

        refreshTokenService.revokeToken(token);
    }


    private Integer extractUserId(String username) {
        return userService.getUserEntityForAuth(username).getId();
    }

    @PutMapping("/manage/{id}")
    public UserDto update(@PathVariable Integer id, @RequestBody CreateUserRequest request){

        return userService.updateUser(id,request);
    }

}