package pl.lodz.p.it.ssbd2022.ssbd02.mok.endpoint;

import pl.lodz.p.it.ssbd2022.ssbd02.entity.Account;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.NoAccountFound;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.dto.AccountUpdatePasswordDto;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.BaseApplicationException;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.dto.AccountRegisterDto;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.service.AccountService;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

@Stateful
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class AccountEndpoint {

    @Inject
    private AccountService accountService;

    /**
     * Ustawia status użytkownika o danym loginie na podany
     *
     * @param login login użytkownika dla którego chcemy zmienić status
     * @param active status który chcemy chcemy ustawić dla konta tego użytkownika
     * @throws NoAccountFound kiedy użytkonwik o danym loginie nie zostanie odnaleziony
     * w bazie danych
     */
    @RolesAllowed({"ADMINISTRATOR", "MODERATOR"})
    public void changeAccountStatus(String login, Boolean active) throws NoAccountFound {
        accountService.changeAccountStatus(login, active);
    }

    /**
     * Konwertuje obiekt transferu danych użytkownika na obiekt klasy encji
     *
     * @param accountRegisterDto Obiekt zawierający dane użytkownika
     * @throws BaseApplicationException Występuje w przypadku gdy rejestracja się nie powiedzie
     */
    @PermitAll
    public void registerAccount(AccountRegisterDto accountRegisterDto) throws BaseApplicationException {

        Account account = new Account();
        account.setLogin(accountRegisterDto.getLogin());
        account.setPassword(accountRegisterDto.getPassword());
        account.setEmail(accountRegisterDto.getEmail());
        account.setName(accountRegisterDto.getName());
        account.setSurname(accountRegisterDto.getSurname());
        accountService.registerAccount(account);
    }

    @RolesAllowed({"ADMINISTRATOR"})
    public void updatePasswordAsAdmin(Long id, AccountUpdatePasswordDto password) {
        accountService.changeAccountPasswordAsAdmin(id, password);
    }
}
