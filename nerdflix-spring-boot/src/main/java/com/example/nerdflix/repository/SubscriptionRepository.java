package com.example.nerdflix.repository;

import com.example.nerdflix.entity.Subscription;
import com.example.nerdflix.entity.SubscriptionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, SubscriptionId> {
    List<Subscription> findByUserId(Integer userId);
    boolean existsByUserIdAndCategoryId(Integer userId, Integer categoryId);
    Subscription findByUserIdAndCategoryId(Integer userId, Integer categoryId);
}
