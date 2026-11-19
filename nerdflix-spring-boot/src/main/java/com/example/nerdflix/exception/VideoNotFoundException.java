package com.example.nerdflix.exception;

public class VideoNotFoundException extends RuntimeException{//Checked

    public VideoNotFoundException(String message) {
        super(message);
    }
}
