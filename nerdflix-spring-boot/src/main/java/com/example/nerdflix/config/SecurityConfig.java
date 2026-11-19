package com.example.nerdflix.config;

import com.example.nerdflix.filter.JwtAuthenticationFilter;
import com.example.nerdflix.config.AdminAuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain adminChain(HttpSecurity http,
                                          AdminAuthenticationSuccessHandler adminAuthSuccessHandler) throws Exception {
        http
                .securityMatcher("/admin", "/admin/**", "/login", "/logout")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/login", "/logout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/login", "/logout").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        .anyRequest().hasRole("ADMIN")
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(adminAuthSuccessHandler)   // <-- custom handler
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                );
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
        http
                .exceptionHandling(e -> e.accessDeniedHandler((req, res, ex) -> {
                    System.out.println("ACCESS DENIED: " + req.getMethod() + " " + req.getRequestURI());
                    System.out.println("Reason: " + ex.getMessage());
                    res.setStatus(403);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"error\":\"forbidden\",\"reason\":\"" + ex.getMessage() + "\"}");
                }))
                .securityMatcher("/api/**", "/auth/**", "/videos/**", "/category/**")
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // PUBLIC ENDPOINTS
                        .requestMatchers(HttpMethod.POST,
                                "/auth/login",
                                "/auth/register",
                                "/auth/refresh"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/videos",
                                "/api/videos/{videoId}"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/category",
                                "/category/{categoryId}"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/license").permitAll()
                        // AUTHENTICATED ENDPOINTS
                        .requestMatchers(HttpMethod.GET,
                                "/api/videos/user",
                                "/api/videos/{videoId}/stream-info"
                        ).authenticated()
                        .requestMatchers(HttpMethod.POST,
                                "/api/videos/{videoId}/view",
                                "/api/videos/{videoId}/like",
                                "/api/videos/{videoId}/unlike"
                        ).authenticated()
                        .requestMatchers("/api/subscriptions/**").authenticated()
                        // ADMIN-ONLY
                        .requestMatchers("/api/test/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Bean for the default success handler used by admin authentication
    @Bean
    public SavedRequestAwareAuthenticationSuccessHandler defaultAdminAuthSuccessHandler() {
        SavedRequestAwareAuthenticationSuccessHandler handler = new SavedRequestAwareAuthenticationSuccessHandler();
        handler.setDefaultTargetUrl("/admin/videos");   // where to go after direct login
        return handler;
    }
}