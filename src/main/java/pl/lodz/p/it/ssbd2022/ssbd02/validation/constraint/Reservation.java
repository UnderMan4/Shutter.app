package pl.lodz.p.it.ssbd2022.ssbd02.validation.constraint;

import pl.lodz.p.it.ssbd2022.ssbd02.validation.implementations.ReservationValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.ReportAsSingleViolation;
import java.lang.annotation.*;

@Constraint(validatedBy = ReservationValidator.class)
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)

@ReportAsSingleViolation
public @interface Reservation {
    String message() default "validator.incorrect.reservation.time";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
