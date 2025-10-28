package com.example.reminder.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventRequest {
    @NotNull
    private String title;

    private String description;

    @NotNull
    private LocalDate eventDate;

    private LocalDateTime reminderTime;

}
