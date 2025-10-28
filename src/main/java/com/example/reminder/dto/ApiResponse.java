package com.example.reminder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {

    private String message;
    private String status;
    private T data;
    private LocalDateTime timeStamp = LocalDateTime.now();

    public ApiResponse(String status , String message , T data){
        this.message = message;
        this.status = status;
        this.data = data;
        this.timeStamp = LocalDateTime.now();
    }


}
