package com.example.nerdflix.service;

import com.example.nerdflix.entity.Category;
import com.example.nerdflix.exception.CategoryNotFoundException;
import com.example.nerdflix.repository.CategoryRepository;
//import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {//Checked

    private final CategoryRepository categoryRepository;
    private List<Category> categoryList;

    @Autowired
    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly=true)
    public List<Category> getCategories(){
        if (categoryList == null) {
            categoryList = categoryRepository.findAll();
        }
        return categoryList;
    }

    @Transactional(readOnly=true)
    public Category getCategoryById(Integer id){
        return categoryRepository.findById(id)
                .orElseThrow(()->new CategoryNotFoundException("Category not found"));
    }

}
