package pl.lodz.p.it.ssbd2022.ssbd02.mok.endpoint;

import pl.lodz.p.it.ssbd2022.ssbd02.entity.AccessLevelValue;
import pl.lodz.p.it.ssbd2022.ssbd02.entity.Account;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.*;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.dto.*;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.service.AccountService;
import pl.lodz.p.it.ssbd2022.ssbd02.util.AbstractEndpoint;
import pl.lodz.p.it.ssbd2022.ssbd02.util.LoggingInterceptor;
import pl.lodz.p.it.ssbd2022.ssbd02.security.AuthenticationContext;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.service.VerificationTokenService;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import static pl.lodz.p.it.ssbd2022.ssbd02.security.Roles.*;

@Stateful
@Interceptors({LoggingInterceptor.class})
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class AccountEndpoint extends AbstractEndpoint {

    @Inject
    private AuthenticationContext authenticationContext;

    @Inject
    private AccountService accountService;

    @Inject
    private VerificationTokenService verificationTokenService;

    /**
     * Ustawia status użytkownika o danym loginie na zablokowany
     *
     * @param login  login użytkownika dla którego chcemy zmienić status
     * @throws NoAccountFound kiedy użytkonwik o danym loginie nie zostanie odnaleziony
     *                        w bazie danych
     */
    @RolesAllowed({blockAccount})
    public void blockAccount(String login) throws NoAccountFound {
        Account account = accountService.findByLogin(login);
        accountService.changeAccountStatus(account, false);
    }

    /**
     * Ustawia status użytkownika o danym loginie na odblokowany
     *
     * @param login  login użytkownika dla którego chcemy zmienić status
     * @throws NoAccountFound kiedy użytkonwik o danym loginie nie zostanie odnaleziony
     *                        w bazie danych
     */
    @RolesAllowed({unblockAccount})
    public void unblockAccount(String login) throws NoAccountFound {
        Account account = accountService.findByLogin(login);
        accountService.changeAccountStatus(account, true);
    }

    /**
     * Konwertuje obiekt transferu danych użytkownika na obiekt klasy encji.
     *
     * @param accountRegisterDto Obiekt zawierający dane użytkownika
     * @throws IdenticalFieldException, Występuje w przypadku gdy rejestracja się nie powiedzie
     * @throws DatabaseException,       Występuje w przypadku gdy rejestracja się nie powiedzie
     */
    @PermitAll
    public void registerAccount(AccountRegisterDto accountRegisterDto)
            throws IdenticalFieldException, DatabaseException, DataNotFoundException {
        Account account = accountRegisterDtoToAccount(accountRegisterDto);
        accountService.registerOwnAccount(account);
    }

    /**
     * Konwertuje obiekt transferu danych użytkownika (z dodatkowymi polami registered oraz active) obiekt klasy encji.
     *
     * @param accountRegisterAsAdminDto Obiekt zawierający dane użytkownika (z dodatkowymi polami registered oraz active)
     * @throws IdenticalFieldException, Występuje w przypadku gdy rejestracja się nie powiedzie
     * @throws DatabaseException,       Występuje w przypadku gdy rejestracja się nie powiedzie
     */
    @RolesAllowed({ADMINISTRATOR})
    public void registerAccountByAdmin(AccountRegisterAsAdminDto accountRegisterAsAdminDto)
            throws IdenticalFieldException, DatabaseException, DataNotFoundException {
        Account account = accountRegisterDtoToAccount(accountRegisterAsAdminDto);
        account.setActive(accountRegisterAsAdminDto.getActive());
        account.setRegistered(accountRegisterAsAdminDto.getRegistered());
        accountService.registerAccountByAdmin(account);
    }

    @RolesAllowed({ADMINISTRATOR})
    public void updatePasswordAsAdmin(String login, AccountUpdatePasswordDto passwordDto) throws NoAccountFound {
        Account account = accountService.findByLogin(login);
        accountService.changeAccountPasswordAsAdmin(account, passwordDto.getPassword());
    }

    /**
     * Metoda pomocnicza konwertująca obiekt transferu danych na obiekt encji użytkownika
     *
     * @param accountRegisterDto Obiekt zawierający dane użytkownika
     * @return Obiekt klasy encji użytkownika
     */
    private Account accountRegisterDtoToAccount(AccountRegisterDto accountRegisterDto) {
        Account account = new Account();
        account.setLogin(accountRegisterDto.getLogin());
        account.setPassword(accountRegisterDto.getPassword());
        account.setEmail(accountRegisterDto.getEmail());
        account.setName(accountRegisterDto.getName());
        account.setSurname(accountRegisterDto.getSurname());
        return account;
    }

    /**
     * Nadaje lub odbiera wskazany poziom dostępu w obiekcie klasy użytkownika.
     *
     * @param login                     Login użytkownika
     * @param data                      Obiekt zawierający informacje o zmienianym poziomie dostępu
     * @throws DataNotFoundException    W przypadku próby podania niepoprawnej nazwie poziomu dostępu
     * lub próby ustawienia aktywnego/nieaktywnego już poziomu dostępu
     * @throws CannotChangeException    W przypadku próby odebrania poziomu dostępu, którego użytkownik nigdy nie posiadał
     * @see AccountAccessLevelChangeDto
     */
    @RolesAllowed({ADMINISTRATOR})
    public void changeAccountAccessLevel(String login, AccountAccessLevelChangeDto data)
            throws CannotChangeException, DataNotFoundException, NoAccountFound {
        Account account = accountService.findByLogin(login);
        AccessLevelValue accessLevelValue = accountService.findAccessLevelValueByName(data.getAccessLevel());
        accountService.changeAccountAccessLevel(account, accessLevelValue, data.getActive());
    }

    /**
     * Dokonuje potwierdzenia konta używając tokenu weryfikacyjnego wysłanego na adres email.
     *
     * @param token Obiekt przedstawiający żeton weryfikacyjny użyty do potwierdzenia rejestracji
     * @throws BaseApplicationException Występuje w przypadku gdy potwierdzenie rejestracji się nie powiedzie
     */
    @PermitAll
    public void confirmAccountRegistration(String token) throws BaseApplicationException {
        accountService.confirmAccountRegistration(token);
    }

    /**
     * Wywołuję funkcję do edycji danych użytkownika
     *
     * @param editAccountInfoDto klasa zawierająca zmienione dane danego użytkownika
     * @throws NoAuthenticatedAccountFound W przypadku gdy nie znaleziono aktualnego użytkownika
     */
    @RolesAllowed(editOwnAccountData)
    public void editAccountInfo(EditAccountInfoDto editAccountInfoDto) throws NoAuthenticatedAccountFound {
        // Można zwrócić użytkownika do userController w przyszłości, trzeba tylko opakowac go w dto
        Account account = authenticationContext.getCurrentUsersAccount();
        accountService.editAccountInfo(account, editAccountInfoDto);
    }

    /**
     * Wywołuję funkcję do edycji danych użytkownika przez administratora
     *
     * @param editAccountInfoDto klasa zawierająca zmienione dane danego użytkownika
     * @throws NoAccountFound W przypadku gdy nie znaleziono użytkownika o danym loginie
     */
    @RolesAllowed({ADMINISTRATOR})
    public void editAccountInfoAsAdmin(String login, EditAccountInfoDto editAccountInfoDto) throws NoAccountFound {
        // Można zwrócić użytkownika do userController w przyszłości, trzeba tylko opakować go w dto
        Account account = accountService.findByLogin(login);
        accountService.editAccountInfoAsAdmin(account, editAccountInfoDto);
    }

    /**
     * Zwraca informacje o dowolnym użytkowniku
     *
     * @param login nazwa użytkownika
     * @return obiekt DTO informacji o użytkowniku
     * @throws NoAccountFound              W przypadku gdy użytkownik o podanej nazwie nie istnieje lub
     *                                     gdy konto szukanego użytkownika jest nieaktywne, lub niepotwierdzone i
     *                                     informacje próbuje uzyskać użytkownik niebędący ani administratorem,
     *                                     ani moderatorem
     * @throws NoAuthenticatedAccountFound W przypadku gdy dane próbuje uzyskać niezalogowana osoba
     * @see AccountInfoDto
     */
    @RolesAllowed({ADMINISTRATOR, MODERATOR})
    public AccountInfoDto getAccountInfo(String login) throws NoAccountFound, NoAuthenticatedAccountFound {
        Account requester = authenticationContext.getCurrentUsersAccount();
        Account account = accountService.findByLogin(login);
        return new AccountInfoDto(accountService.getAccountInfo(requester, account));
    }

    /**
     * Zwraca informacje o zalogowanym użytkowniku
     *
     * @return obiekt DTO informacji o użytkowniku
     * @throws NoAuthenticatedAccountFound W przypadku gdy dane próbuje uzyskać niezalogowana osoba
     * @see AccountInfoDto
     */
    @RolesAllowed({ADMINISTRATOR, MODERATOR, CLIENT, PHOTOGRAPHER})
    public AccountInfoDto getOwnAccountInfo() throws NoAuthenticatedAccountFound {
        Account account = authenticationContext.getCurrentUsersAccount();
        return new AccountInfoDto(account);
    }

    @RolesAllowed({ADMINISTRATOR, MODERATOR, PHOTOGRAPHER, CLIENT})
    public void updateOwnPassword(AccountUpdatePasswordDto data) throws NoAuthenticatedAccountFound, PasswordMismatchException {
        Account account = authenticationContext.getCurrentUsersAccount();
        accountService.updateOwnPassword(account, data);
    }

    /**
     * Resetuje hasło dla użytkownika o podanym loginie
     *
     * @param login            Login użytkownika, dla którego ma zostać zresetowane hasło
     * @param resetPasswordDto Informacje wymagane do resetu hasła (żeton oraz nowe hasło)
     * @throws NoAccountFound           W przypadku gdy dany użytkownik nie istnieje
     * @throws InvalidTokenException    W przypadku gdy żeton jest nieprawidłowego typu
     * @throws ExpiredTokenException    W przypadku gdy żeton wygasł
     * @throws NoVerificationTokenFound W przypadku gdy żeton nie zostanie odnalenzniony w bazie danych
     */
    @PermitAll
    public void resetPassword(String login, ResetPasswordDto resetPasswordDto) throws NoAccountFound, InvalidTokenException, NoVerificationTokenFound, ExpiredTokenException {
        Account account = accountService.findByLogin(login);
        accountService.resetPassword(account, resetPasswordDto);
    }

    /**
     * Wysyła link zawierający żeton resetu hasła na adres e-mail konta o podanym loginie
     *
     * @param login Login użytkownika, na którego email ma zostać wysłany link
     * @throws NoAccountFound Jeżeli konto nie istnieje w systemie lub jest niepotwierdzone/zablokowane
     */
    @PermitAll
    public void requestPasswordReset(String login) throws NoAccountFound {
        Account account = accountService.findByLogin(login);
        verificationTokenService.sendPasswordResetToken(account);
    }

    /**
     * Rejestruje udane logowanie na konto użytkownika.
     * @param login Login użytkownika, dla którego konta należy zarejestrować udaną operację logowania
     * @throws NoAccountFound W przypadku gdy konto, dla którego ma zostać zarejestrowane udane
     *                        logowanie nie istnieje
     */
    @PermitAll
    public void registerSuccessfulLogInAttempt(String login) throws NoAccountFound {
        Account account = accountService.findByLogin(login);
        accountService.registerSuccessfulLogInAttempt(account);
    }

    /**
     * Rejestruje nieudane logowanie na konto użytkownika.
     *
     * @param login Login użytkownika, dla którego konta należy zarejestrować nieudaną operację logowania
     * @throws NoAccountFound W przypadku gdy konto, dla którego ma zostać zarejestrowane nieudane
     *                        logowanie nie istnieje
     */
    @PermitAll
    public void registerFailedLogInAttempt(String login) throws NoAccountFound {
        Account account = accountService.findByLogin(login);
        accountService.registerFailedLogInAttempt(account);
    }
}
