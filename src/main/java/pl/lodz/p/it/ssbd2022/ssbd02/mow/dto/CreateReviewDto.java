package pl.lodz.p.it.ssbd2022.ssbd02.mow.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import pl.lodz.p.it.ssbd2022.ssbd02.validation.constraint.Description;
import pl.lodz.p.it.ssbd2022.ssbd02.validation.constraint.Login;

import javax.validation.constraints.NotNull;


/**
 * Klasa reprezentująca obiekt transferu danych potrzebnych do zrecenzowania fotografa, zawierająca jego login, ocenę
 * w formie liczby (1-10) oraz opinię słowną.
 * Na pola klasy nałożone zostały ograniczenia not null.
 */
@Data
@NoArgsConstructor
public class CreateReviewDto {
    @NotNull(message = "validator.incorrect.photographerLogin.null")
    @Login
    private String photographerLogin;
    @NotNull(message = "validator.incorrect.score.null")
    private Long score;
    @NotNull(message = "validator.incorrect.content.null")
    @Description
    private String content;
}
