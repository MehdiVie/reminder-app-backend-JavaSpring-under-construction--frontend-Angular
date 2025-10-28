package com.example.reminder.validation;

import com.example.reminder.model.Event;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReminderBeforeEventValidator implements ConstraintValidator<ReminderBeforeEvent, Event> {

    @Override
    public boolean isValid(Event event ,  ConstraintValidatorContext context) {
        if (event == null || event.getReminderTime()==null || event.getEventDate()==null)
            return true;
        LocalDate eventDate = event.getEventDate();
        LocalDateTime reminder = event.getReminderTime();

        LocalDate reminderDateOnly = reminder.toLocalDate();

        boolean valid= reminderDateOnly.isBefore(eventDate);

        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "ReminderTime must be before the EventDate."
            ).addPropertyNode("reminderTime").addConstraintViolation();
        }

        return  valid;
    }
}
