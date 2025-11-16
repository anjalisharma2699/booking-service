package com.cleaning.bookingservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DurationHoursValidator implements ConstraintValidator<ValidDurationHours, Integer> {

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        // Allow null because the field is optional
        if (value == null) {
            return true;
        }
        return value == 2 || value == 4;
    }
}
