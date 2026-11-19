package com.example.nerdflix.controller;

import com.example.nerdflix.entity.Category;
import com.example.nerdflix.entity.Subscription;
import com.example.nerdflix.service.CategoryService;
import com.example.nerdflix.service.JwtTokenService;
import com.example.nerdflix.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {//Checked

    private final SubscriptionService subscriptionService;
    private final JwtTokenService jwtTokenService;

    @Autowired
    public SubscriptionController(SubscriptionService subscriptionService, JwtTokenService jwtTokenService) {
        this.subscriptionService = subscriptionService;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@RequestParam Integer categoryId,
                                          @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Integer userId = jwtTokenService.extractUserId(token);
        subscriptionService.subscribe(userId, categoryId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@RequestParam Integer categoryId,
                                            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Integer userId = jwtTokenService.extractUserId(token);
        subscriptionService.unsubscribe(userId, categoryId);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/subscribed")
    public List<Subscription> getCategoriesByUser(@RequestHeader("Authorization") String authHeader){
        String token = authHeader.substring(7);
        Integer userId = jwtTokenService.extractUserId(token);

        return subscriptionService.getSubscriptionsByUserId(userId);
    }
}

