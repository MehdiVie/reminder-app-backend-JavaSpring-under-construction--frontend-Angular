package com.example.reminder.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = TomorrowOrLaterValidator.class)
@Target({ ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TomorrowOrLater {
    String message() default "Event-Date must be at least tomorrow.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
