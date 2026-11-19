package com.example.nerdflix.controller;

import com.example.nerdflix.entity.Category;
import com.example.nerdflix.service.CategoryService;
import com.example.nerdflix.service.JwtTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController {//Checked

    private final CategoryService categoryService;

    @Autowired
    public CategoryController(CategoryService categoryService){
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<Category> getAll(){
        return categoryService.getCategories();
    }

    @GetMapping("/{categoryId}")
    public Category getById(@PathVariable Integer categoryId){
        return categoryService.getCategoryById(categoryId);
    }

}
