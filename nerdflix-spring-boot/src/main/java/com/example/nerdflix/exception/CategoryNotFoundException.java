package com.example.nerdflix.exception;

public class CategoryNotFoundException extends RuntimeException{//Checked
    public CategoryNotFoundException(String message) {
        super(message);
    }
}
