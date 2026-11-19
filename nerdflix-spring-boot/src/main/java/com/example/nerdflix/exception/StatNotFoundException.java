package com.example.nerdflix.exception;

public class StatNotFoundException extends RuntimeException{//Checked
    public StatNotFoundException(String message) {
        super(message);
    }
}
