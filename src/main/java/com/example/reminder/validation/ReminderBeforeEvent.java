package com.example.reminder.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ReminderBeforeEventValidator.class)
@Target({ ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReminderBeforeEvent {
    String message() default "ReminderDate must be before Event-Date";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}