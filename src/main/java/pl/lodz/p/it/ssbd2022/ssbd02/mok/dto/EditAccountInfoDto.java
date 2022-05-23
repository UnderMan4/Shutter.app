package pl.lodz.p.it.ssbd2022.ssbd02.mok.dto;

import lombok.Data;
import pl.lodz.p.it.ssbd2022.ssbd02.validation.constraint.Email;
import pl.lodz.p.it.ssbd2022.ssbd02.validation.constraint.Name;
import pl.lodz.p.it.ssbd2022.ssbd02.validation.constraint.Surname;

import javax.validation.constraints.NotNull;

/**
 * Klasa do zmiany danych użytkownika
 */
@Data
public class EditAccountInfoDto {

    @NotNull
    @Name
    private String name;
    @NotNull
    @Surname
    private String surname;

}