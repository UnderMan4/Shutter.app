package pl.lodz.p.it.ssbd2022.ssbd02.mok.dto;

import lombok.Data;
import pl.lodz.p.it.ssbd2022.ssbd02.security.etag.SignableEntity;
import pl.lodz.p.it.ssbd2022.ssbd02.validation.constraint.Login;
import pl.lodz.p.it.ssbd2022.ssbd02.validation.constraint.Name;
import pl.lodz.p.it.ssbd2022.ssbd02.validation.constraint.Surname;

import javax.validation.constraints.NotNull;

/**
 * Klasa do zmiany danych użytkownika
 */
@Data
public class EditAccountInfoDto implements SignableEntity {

    @NotNull
    Long version;
    @NotNull(message = "validator.incorrect.login.null")
    @Login
    private String login;
    @NotNull
    @Name
    private String name;
    @NotNull
    @Surname
    private String surname;

    @Override
    public String getSignablePayload() {
        return login + version.toString();
    }
}