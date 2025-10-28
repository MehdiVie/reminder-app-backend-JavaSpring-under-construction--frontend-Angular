package com.example.reminder.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Clock;
import java.time.LocalDate;

public class TomorrowOrLaterValidator implements ConstraintValidator<TomorrowOrLater, LocalDate> {
    private final Clock clock;
    public TomorrowOrLaterValidator() { this(Clock.systemDefaultZone()); }
    TomorrowOrLaterValidator(Clock clock) { this.clock = clock; }
    @Override
    public boolean isValid (LocalDate date, ConstraintValidatorContext context) {
        if (date == null) return true;
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        return !date.isBefore(tomorrow);
    }
}
