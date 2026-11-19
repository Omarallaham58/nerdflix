package com.example.nerdflix.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "subscription")
@IdClass(SubscriptionId.class)
public class Subscription {

    @Id
    @Column(name="user_id")
    private Integer userId;

    @Id
    @Column(name="category_id")
    private Integer categoryId;

    @Column(name="start_date")
    private LocalDate startDate;

    @Column(name="end_date")
    private LocalDate endDate;

    public Subscription(Integer userId, Integer categoryId, LocalDate startDate, LocalDate endDate) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Subscription() {
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    @Override
    public String toString() {
        return "Subscription{" +
                "userId=" + userId +
                ", categoryId=" + categoryId +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }


    // getters/setters
}

