package com.example.reminder.model;

import com.example.reminder.validation.ReminderBeforeEvent;
import com.example.reminder.validation.TomorrowOrLater;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "events",
       indexes = {
        @Index (name = "idx_event_date" , columnList="eventDate") ,
        @Index (name = "idx_event_title" , columnList="title")
        }
        )
@Data
@NoArgsConstructor
@AllArgsConstructor
@ReminderBeforeEvent
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Event Title is required.")
    @Column(nullable = false)
    private String title;

    private String description;

    @NotNull(message = "Event Date is required.")
    @TomorrowOrLater
    @Column(nullable = false)
    private LocalDate eventDate;

    @FutureOrPresent(message = "Reminder time must be in the future or now.")
    private LocalDateTime reminderTime;

    // to track if reminder sent or not
    private boolean reminderSent=false;

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public interface OnCreate{};
    public interface OnUpdate{};

}
