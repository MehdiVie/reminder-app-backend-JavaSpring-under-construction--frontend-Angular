package com.example.reminder.exception;

/* thrown when a requested resource is not found */
public class ResourceNotFoundException extends RuntimeException{
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
