package com.example.nerdflix.service;

import com.example.nerdflix.entity.Subscription;
import com.example.nerdflix.entity.SubscriptionId;
import com.example.nerdflix.entity.Video;
import com.example.nerdflix.repository.CategoryRepository;
import com.example.nerdflix.repository.SubscriptionRepository;
import com.example.nerdflix.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepo;
    private final CategoryRepository categoryRepo;
    private final VideoRepository videoRepo;

    @Autowired
    public SubscriptionService(SubscriptionRepository subscriptionRepo, CategoryRepository categoryRepo, VideoRepository videoRepo) {
        this.subscriptionRepo = subscriptionRepo;
        this.categoryRepo = categoryRepo;
        this.videoRepo = videoRepo;
    }

    @Transactional
    @PreAuthorize("#userId == authentication.principal.userId")
    public void subscribe(Integer userId, Integer categoryId) {

        if (subscriptionRepo.existsByUserIdAndCategoryId(userId, categoryId)) {
            return;
        }

        Subscription sub = new Subscription();
        sub.setUserId(userId);
        sub.setCategoryId(categoryId);
        sub.setStartDate(LocalDate.now());

        subscriptionRepo.save(sub);
    }

    @Transactional(readOnly=true)
    @PreAuthorize("#userId == authentication.principal.userId or hasRole('ADMIN')")
    public List<Subscription> getSubscriptionsByUserId(Integer userId){

        return subscriptionRepo.findByUserId(userId);
    }

    @Transactional
    @PreAuthorize("#userId == authentication.principal.userId or hasRole('ADMIN')")
    public void unsubscribe(Integer userId, Integer categoryId) {
        if (subscriptionRepo.existsByUserIdAndCategoryId(userId, categoryId)) {
            Subscription subscription = subscriptionRepo.findByUserIdAndCategoryId(userId, categoryId);
            subscriptionRepo.delete(subscription);
        }
    }

    @Transactional(readOnly = true)
    public boolean isSubscribed(Integer userId, Integer categoryId) {
        return subscriptionRepo.existsByUserIdAndCategoryId(userId, categoryId);
    }
}
