package com.cleaning.bookingservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DurationHoursValidator.class)
@Documented
public @interface ValidDurationHours {

    String message() default "Duration Hours must be either 2 or 4 hours";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
