package com.example.nerdflix.repository;

import com.example.nerdflix.entity.Category;
import com.example.nerdflix.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category,Integer> {
}
