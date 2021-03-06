package pl.lodz.p.it.ssbd2022.ssbd02.mok.endpoint;

import pl.lodz.p.it.ssbd2022.ssbd02.entity.AccessLevelValue;
import pl.lodz.p.it.ssbd2022.ssbd02.entity.Account;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.*;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.dto.*;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.service.AccountService;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.service.PhotographerService;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.service.VerificationTokenService;
import pl.lodz.p.it.ssbd2022.ssbd02.security.AuthenticationContext;
import pl.lodz.p.it.ssbd2022.ssbd02.security.recaptcha.ReCaptchaService;
import pl.lodz.p.it.ssbd2022.ssbd02.util.AbstractEndpoint;
import pl.lodz.p.it.ssbd2022.ssbd02.util.LoggingInterceptor;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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

    @Inject
    private ReCaptchaService reCaptchaService;


    @Inject
    private PhotographerService photographerService;

    /**
     * Ustawia status użytkownika o danym loginie na zablokowany
     *
     * @param login Login użytkownika, dla którego chcemy zmienić status
     * @throws NoAccountFound Konto o podanej nazwie nie istnieje
     */
    @RolesAllowed(blockAccount)
    public void blockAccount(String login) throws BaseApplicationException {
        Account account = accountService.findByLogin(login);
        accountService.changeAccountStatus(account, false);
    }

    /**
     * Ustawia status użytkownika o danym loginie na odblokowany
     *
     * @param login Login użytkownika, dla którego chcemy zmienić status
     * @throws NoAccountFound Konto o podanej nazwie nie istnieje
     */
    @RolesAllowed(unblockAccount)
    public void unblockAccount(String login) throws BaseApplicationException {
        Account account = accountService.findByLogin(login);
        accountService.changeAccountStatus(account, true);
    }

    /**
     * Potwierdza aktywację własnego konta po długim czasie nieaktywności
     *
     * @param token Obiekt przedstawiający żeton weryfikacyjny użyty do aktywacji konta
     * @throws BaseApplicationException Występuje w przypadku gdy aktywacja konta się nie powiedzie
     */
    @PermitAll
    public void confirmUnblockOwnAccount(String token) throws BaseApplicationException {
        accountService.confirmUnblockOwnAccount(token);
    }

    /**
     * Konwertuje obiekt transferu danych użytkownika na obiekt klasy encji.
     *
     * @param accountRegisterDto Obiekt zawierający dane użytkownika
     * @throws IdenticalFieldException Występuje w przypadku gdy rejestracja się nie powiedzie
     */
    @PermitAll
    public void registerAccount(AccountRegisterDto accountRegisterDto)
            throws BaseApplicationException {
        reCaptchaService.verify(accountRegisterDto.getReCaptchaToken());
        Account account = accountRegisterDtoToAccount(accountRegisterDto);
        accountService.registerOwnAccount(account);
    }

    /**
     * Konwertuje obiekt transferu danych użytkownika (z dodatkowymi polami registered oraz active) obiekt klasy encji.
     *
     * @param accountRegisterAsAdminDto Obiekt zawierający dane użytkownika (z dodatkowymi polami registered oraz active)
     * @throws IdenticalFieldException Występuje w przypadku gdy rejestracja się nie powiedzie
     */
    @RolesAllowed({createAccount})
    public void registerAccountByAdmin(AccountRegisterAsAdminDto accountRegisterAsAdminDto)
            throws BaseApplicationException {
        Account account = accountRegisterDtoToAccount(accountRegisterAsAdminDto);
        account.setActive(accountRegisterAsAdminDto.getActive());
        account.setRegistered(accountRegisterAsAdminDto.getRegistered());
        accountService.registerAccountByAdmin(account);
    }

    @RolesAllowed({changeSomeonesPassword})
    public void updatePasswordAsAdmin(String login, AccountUpdatePasswordDto passwordDto) throws BaseApplicationException {
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
        account.setLocale(Locale.forLanguageTag(accountRegisterDto.getLocale()));
        return account;
    }

    /**
     * Nadaje lub odbiera wskazany poziom dostępu w obiekcie klasy użytkownika.
     *
     * @param login Login użytkownika
     * @param data  Obiekt zawierający informacje o zmienianym poziomie dostępu
     * @throws DataNotFoundException W przypadku próby podania niepoprawnej nazwie poziomu dostępu
     *                               lub próby ustawienia aktywnego/nieaktywnego już poziomu dostępu
     * @throws CannotChangeException W przypadku próby odebrania poziomu dostępu, którego użytkownik nigdy nie posiadał
     * @see AccountAccessLevelChangeDto
     */
    @RolesAllowed({grantAccessLevel, revokeAccessLevel})
    public void changeAccountAccessLevel(String login, AccountAccessLevelChangeDto data)
            throws BaseApplicationException {
        Account account = accountService.findByLogin(login);
        AccessLevelValue accessLevelValue = accountService.findAccessLevelValueByName(data.getAccessLevel());
        accountService.changeAccountAccessLevel(account, accessLevelValue, data.getActive());
    }

    /**
     * Ustawia poziom dostępu fotografa w obiekcie klasy użytkownika na aktywny.
     *
     * @throws NoAuthenticatedAccountFound W przypadku próby zostania fotografem przez uzytkownika mającego już tę rolę
     * @throws DataNotFoundException       W przypadku próby podania niepoprawnej nazwie poziomu dostępu
     *                                     lub próby ustawienia aktywnego/nieaktywnego już poziomu dostępu
     * @throws CannotChangeException       W przypadku próby zostania fotografem przez uzytkownika mającego już tę rolę
     * @see AccountAccessLevelChangeDto
     */
    @RolesAllowed({becomePhotographer})
    public void becomePhotographer() throws BaseApplicationException {
        Account account = accountService.findByLogin(authenticationContext.getCurrentUsersLogin());
        AccessLevelValue accessLevelValue = accountService.findAccessLevelValueByName("PHOTOGRAPHER");
        accountService.changeAccountAccessLevel(account, accessLevelValue, true);
        photographerService.createOrActivatePhotographerInfo(account);
    }

    /**
     * Ukrywa informacje o fotografie i ustawia poziom dostępu fotografa w obiekcie klasy użytkownika na nieaktywny.
     *
     * @throws NoAuthenticatedAccountFound W przypadku nieznalezienia konta użytkownika w bazie danych
     *                                     na podstawie żetonu JWT
     * @throws DataNotFoundException       W przypadku nieznalezienia na koncie użytkownika roli fotografa
     * @throws CannotChangeException       W przypadku próby odebrania roli fotografa przez użytkownika niebędącego
     *                                     fotografem
     * @see AccountAccessLevelChangeDto
     */
    @RolesAllowed({stopBeingPhotographer})
    public void stopBeingPhotographer() throws BaseApplicationException {
        Account account = accountService.findByLogin(authenticationContext.getCurrentUsersLogin());
        photographerService.hidePhotographerInfo(account.getLogin());
        accountService.stopBeingPhotographer(account);
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
     * @throws NoAuthenticatedAccountFound W przypadku gdy dane próbuje uzyskać niezalogowana osoba
     */
    @RolesAllowed(editOwnAccountData)
    public void editAccountInfo(EditAccountInfoDto editAccountInfoDto) throws BaseApplicationException {
        // Można zwrócić użytkownika do userController w przyszłości, trzeba tylko opakowac go w dto
        Account account = accountService.findByLogin(authenticationContext.getCurrentUsersLogin());
        if (account.getVersion() > editAccountInfoDto.getVersion()) {
            throw ExceptionFactory.OptLockException();
        }
        accountService.editAccountInfo(account, editAccountInfoDto);
    }

    /**
     * Wywołuję funkcję do edycji danych użytkownika przez administratora
     *
     * @param editAccountInfoAsAdminDto klasa zawierająca zmienione dane danego użytkownika
     * @param login                     login użytkownika
     * @throws NoAccountFound Konto o podanej nazwie nie istnieje
     */
    @RolesAllowed({editSomeonesAccountData})
    public void editAccountInfoAsAdmin(String login, EditAccountInfoAsAdminDto editAccountInfoAsAdminDto) throws BaseApplicationException {
        // Można zwrócić użytkownika do userController w przyszłości, trzeba tylko opakować go w dto
        Account account = accountService.findByLogin(login);
        if (account.getVersion() > editAccountInfoAsAdminDto.getVersion()) {
            throw ExceptionFactory.OptLockException();
        }
        accountService.editAccountInfoAsAdmin(account, editAccountInfoAsAdminDto);
    }

    /**
     * Zwraca informacje o dowolnym użytkowniku
     *
     * @param login Login użytkownika
     * @return obiekt DTO informacji o użytkowniku
     * @throws NoAccountFound Konto o podanej nazwie nie istnieje
     * @see BaseAccountInfoDto
     */
    @RolesAllowed(getEnhancedAccountInfo)
    public DetailedAccountInfoDto getEnhancedAccountInfo(String login) throws BaseApplicationException {
        Account account = accountService.findByLogin(login);
        return new DetailedAccountInfoDto(account);
    }

    /**
     * Zwraca informacje o dowolnym użytkowniku
     *
     * @param login Login użytkownika
     * @return obiekt DTO informacji o użytkowniku
     * @throws NoAccountFound Konto o podanej nazwie nie istnieje w systemie lub jest niepotwierdzone/zablokowane
     * @see BaseAccountInfoDto
     */
    @RolesAllowed(getAccountInfo)
    public BaseAccountInfoDto getAccountInfo(String login) throws BaseApplicationException {
        Account account = accountService.findByLogin(login);
        return new BaseAccountInfoDto(accountService.getAccountInfo(account));
    }

    /**
     * Zwraca wartość secret użytkownika o danym loginie
     *
     * @param login Login użytkownika
     * @return sekret
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @PermitAll
    public String getSecret(String login) throws BaseApplicationException {
        Account account = accountService.findByLogin(login);
        return account.getSecret();
    }

    /**
     * Zwraca informacje o zalogowanym użytkowniku
     *
     * @return obiekt DTO informacji o użytkowniku
     * @throws NoAuthenticatedAccountFound W przypadku gdy dane próbuje uzyskać niezalogowana osoba
     * @see BaseAccountInfoDto
     */
    @RolesAllowed(getOwnAccountInfo)
    public DetailedAccountInfoDto getOwnAccountInfo() throws BaseApplicationException {
        Account account = accountService.findByLogin(authenticationContext.getCurrentUsersLogin());
        return new DetailedAccountInfoDto(account);
    }

    /**
     * Aktualizuje hasło obecnie uwierzytelnionego użytkownika
     *
     * @param data dane wymagane do zaktualizowania hasła
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @RolesAllowed({changeOwnPassword})
    public void updateOwnPassword(AccountUpdatePasswordDto data) throws BaseApplicationException {
        Account account = accountService.findByLogin(authenticationContext.getCurrentUsersLogin());
        accountService.updateOwnPassword(account, data);
    }

    /**
     * Zwraca listę wszystkich użytkowników w zadanej kolejności spełniających warunki zapytania
     *
     * @param recordsPerPage ilość krotek na stronę
     * @param order          kolejność sortowania
     * @param pageNo         numer strony
     * @param name           imie
     * @param surname        nazwisko
     * @param active         czy użytkownik jest aktywny
     * @param columnName     nazwa kolumny do sortowania
     * @param email          email użytkownika
     * @param login          login użytkownika
     * @param registered     czy użytkownik jest zarejestrowany
     * @return lista użytkowników
     * @throws WrongParameterException w przypadku gdy podano złą nazwę kolumny lub kolejność sortowania
     */
    @RolesAllowed(listAllAccounts)
    public ListResponseDto<TableAccountDto> getAccountList(
            int pageNo,
            int recordsPerPage,
            String columnName,
            String order,
            String login,
            String email,
            String name,
            String surname,
            Boolean registered,
            Boolean active
    ) throws BaseApplicationException {
        Account account = accountService.findByLogin(authenticationContext.getCurrentUsersLogin());

        Boolean convertedOrder = true;
        if (order.equals("desc")) convertedOrder = false;

        AccountListRequestDto requestDto = new AccountListRequestDto(
                pageNo,
                recordsPerPage,
                columnName,
                convertedOrder,
                login,
                email,
                name,
                surname,
                registered,
                active
        );

        accountService.saveAccountListPreferences(
                account,
                pageNo,
                recordsPerPage,
                columnName,
                convertedOrder
        );
        return accountService.getAccountList(account, requestDto);
    }

    /**
     * Zwraca ostatnio ustawione w dla danego użytkownika preferencje sortowania oraz stronicowania list kont
     *
     * @return obiekt dto preferencji sortowania oraz stronicowania listy użytkowników
     * @throws BaseApplicationException kiedy preferencje dla danego użytkownika nie zostaną odnalezione
     */
    @RolesAllowed(listAllAccounts)
    public AccountListPreferencesDto getAccountListPreferences() throws BaseApplicationException {
        Account account = accountService.findByLogin(authenticationContext.getCurrentUsersLogin());
        return new AccountListPreferencesDto(accountService.getAccountListPreferences(account));
    }

    /**
     * Resetuje hasło użytkownika
     *
     * @param resetPasswordDto Informacje wymagane do resetu hasła (żeton oraz nowe hasło)
     * @throws InvalidTokenException    Żeton jest nieprawidłowego typu lub nieaktualny
     * @throws ExpiredTokenException    W przypadku gdy żeton wygasł
     * @throws NoVerificationTokenFound Żeton wygasł
     */
    @PermitAll
    public void resetPassword(ResetPasswordDto resetPasswordDto) throws BaseApplicationException {
        accountService.resetPassword(resetPasswordDto);
    }

    /**
     * Wysyła link zawierający żeton resetu hasła na adres e-mail konta o podanym loginie
     *
     * @param login   Login użytkownika, na którego email ma zostać wysłany link
     * @param captcha wynik wymaganego zadania reCaptcha
     * @throws NoAccountFound Konto o podanej nazwie nie istnieje w systemie lub jest niepotwierdzone/zablokowane
     */
    @PermitAll
    public void requestPasswordReset(String login, RecaptchaTokenDto captcha) throws BaseApplicationException {
        reCaptchaService.verify(captcha.getReCaptchaToken());
        Account account = accountService.findByLogin(login);
        verificationTokenService.sendPasswordResetToken(account);
    }

    /**
     * Rejestruje udane logowanie na konto użytkownika.
     *
     * @param login Login użytkownika, dla którego konta należy zarejestrować udaną operację logowania
     * @throws NoAccountFound Konto o podanej nazwie nie istnieje
     */
    @PermitAll
    public void registerSuccessfulLogInAttempt(String login) throws BaseApplicationException {
        Account account = accountService.findByLogin(login);
        accountService.registerSuccessfulLogInAttempt(account);
    }

    /**
     * Rejestruje nieudane logowanie na konto użytkownika.
     *
     * @param login     Login użytkownika, dla którego konta należy zarejestrować nieudaną operację logowania
     * @param ipAddress adres, z którego dokonana była próba uwierzytelnienia
     * @throws NoAccountFound Konto o podanej nazwie nie istnieje
     */
    @PermitAll
    public void registerFailedLogInAttempt(String login, String ipAddress) throws BaseApplicationException {
        Account account = accountService.findByLogin(login);
        accountService.registerFailedLogInAttempt(account, ipAddress);
    }

    /**
     * Powiadamia administratora o zalogowaniu na jego konto poprzez wysłanie na adres email przypisany
     * do konta o podanym loginie wiadomości zawierającej adres IP, z którego dokonane było logowanie
     *
     * @param login     Login konto administratora, na które doszło do zalogowania
     * @param ipAddress adres IP, z którego zostało wykonane logowanie
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @PermitAll
    public void sendAdminAuthenticationWarningEmail(String login, String ipAddress) throws BaseApplicationException {
        Account account = accountService.findByLogin(login);
        accountService.sendAdminAuthenticationWarningEmail(account, ipAddress);
    }

    /*
     * Wysyła link zawierający żeton zmiany adresu email
     *
     * @param requestEmailUpdateDto E-mail użytkownika, na którego e-mail ma zostać wysłany link
     * @throws NoAccountFound              Konto o podanej nazwie nie istnieje w systemie lub jest niepotwierdzone/zablokowane
     * @throws NoAuthenticatedAccountFound W przypadku gdy dane próbuje uzyskać niezalogowana osoba
     */
    @RolesAllowed((updateEmail))
    public void requestEmailUpdate() throws BaseApplicationException {
        Account account = accountService.findByLogin(authenticationContext.getCurrentUsersLogin());
        verificationTokenService.sendEmailUpdateToken(account);
    }


    /**
     * Aktualizuje email użytkownika
     *
     * @param emailUpdateDto Informacje do zmiany emaila użytkownika
     * @throws InvalidTokenException    Żeton jest nieprawidłowego typu lub nieaktualny
     * @throws NoVerificationTokenFound Żeton nie zostanie odnaleziony w bazie
     * @throws ExpiredTokenException    Żeton wygasł
     */
    @RolesAllowed((updateEmail))
    public void updateEmail(EmailUpdateDto emailUpdateDto) throws BaseApplicationException {
        accountService.updateEmail(emailUpdateDto);
    }

    /**
     * Wyszukuje użytkownika na podstawie frazy zawartej w jego imieniu lub nazwisku
     *
     * @param name           fraza zawarta w imieniu lub nazwisku
     * @param page           number strony
     * @param recordsPerPage liczba krotek na stronę
     * @param orderBy        parametr, po którym ma się dokonywać sortowanie
     * @param order          kolejność sortowania
     * @return lista DTO krotek użytkowników spełniająca określone kryteria
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @RolesAllowed(getAccountInfo)
    public ListResponseDto<TableAccountDto> findByNameSurname(
            String name,
            int page,
            int recordsPerPage,
            String orderBy,
            String order
    ) throws BaseApplicationException {
        Boolean convertedOrder = true;
        if (order.equals("desc")) convertedOrder = false;

        Account account = accountService.findByLogin(authenticationContext.getCurrentUsersLogin());

        accountService.saveAccountListPreferences(
                account,
                page,
                recordsPerPage,
                orderBy,
                convertedOrder
        );

        return accountService.findByNameSurname(name, page, recordsPerPage, orderBy, order);
    }


    /**
     * Wysyła wymagany do zalogowania kod 2fa na adres email użytkownika
     *
     * @param login login użytkownika, dla którego ma zostać utworzony kod 2fa
     * @throws BaseApplicationException W przypadku kiedy użytkownik o podanym loginie nie zostanie znaleziony
     *                                  lub wystąpi nieoczekiwany błąd
     */
    @PermitAll
    public void reguest2faCode(String login) throws BaseApplicationException {
        Account account = accountService.findByLogin(login);
        accountService.send2faCode(account);
    }

    /**
     * Wyłącza lub włącza dwustopniowe uwierzytelnianie dla użytkownika
     *
     * @throws BaseApplicationException W przypadku kiedy użytkownik o podanym loginie nie zostanie znaleziony
     *                                  lub wystąpi nieoczekiwany błąd
     */
    @PermitAll
    public void toggle2fa() throws BaseApplicationException {
        Account account = accountService.findByLogin(authenticationContext.getCurrentUsersLogin());
        accountService.toggle2fa(account);
    }

    /**
     * Sprawdza, czy dany użytkownik ma uruchomione uwierzytelnianie dwuetapowe
     *
     * @param login użytkownik
     * @return true, jeżeli użytkownik ma włączone uwierzytelnianie dwuetapowe
     * false jeżeli użytkownik ma wyłączone uwierzytelnianie dwuetapowe
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @PermitAll
    public Boolean is2FAEnabledForUser(String login) throws BaseApplicationException {
        Account account = accountService.findByLogin(login);
        return accountService.is2FAEnabledForUser(account);
    }

    /**
     * Pobiera wszystkie grupy, w których znajduje się użytkownik o danym loginie
     *
     * @param login login użytkownika, dla którego mają zostać pobrane grupy
     * @return lista grup, w których znajduje się użytkownik o podanym loginie
     * @throws BaseApplicationException jeżeli użytkownik o podanym loginie nie istnieje
     */
    @PermitAll
    public List<String> getAccountGroups(String login) throws BaseApplicationException {
        Account account = accountService.findByLogin(login);
        return account.getAccessLevelAssignmentList().stream()
                .filter(accessLevel -> accessLevel.getActive())
                .map(accessLevel -> accessLevel.getLevel().getName())
                .collect(Collectors.toList());
    }

    /**
     * Zwraca historię zmian dla aktualnego użytkownika
     *
     * @param pageNo         numer strony
     * @param recordsPerPage liczba rekordów na stronę
     * @param orderBy        kolumna, po której następuje sortowanie
     * @param order          kolejność sortowania
     * @return Historia zmian konta
     * @throws BaseApplicationException użytkownik o podanym loginie nie istnieje
     */
    @RolesAllowed({getOwnAccountInfo})
    public ListResponseDto<AccountChangeLogDto> getOwnAccountChangeLog(
            int pageNo,
            int recordsPerPage,
            String orderBy,
            String order
    ) throws BaseApplicationException {
        Account account = accountService.findByLogin(authenticationContext.getCurrentUsersLogin());
        Long allRecords = accountService.getAccountLogListSize(account.getLogin());
        return new ListResponseDto<>(
                pageNo,
                (int) Math.ceil(allRecords.doubleValue() / recordsPerPage),
                recordsPerPage,
                allRecords,
                accountService
                        .getAccountChangeLog(pageNo, recordsPerPage, account.getLogin(), orderBy, order.equals("asc"))
                        .stream()
                        .map(AccountChangeLogDto::new)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Zwraca historię zmian dla konta
     *
     * @param login          Login użytkownika, którego historia zmian konta ma być wyszukana
     * @param order          kolejność sortowania
     * @param orderBy        parametr, po którym ma być dokonane sortowanie
     * @param pageNo         numer strony
     * @param recordsPerPage ilość krotek na stronę
     * @return Historia zmian konta
     */
    @RolesAllowed({getEnhancedAccountInfo})
    public ListResponseDto<AccountChangeLogDto> getAccountChangeLog(
            String login,
            int pageNo,
            int recordsPerPage,
            String orderBy,
            String order
    ) {
        Long allRecords = accountService.getAccountLogListSize(login);
        return new ListResponseDto<>(
                pageNo,
                (int) Math.ceil(allRecords.doubleValue() / recordsPerPage),
                recordsPerPage,
                allRecords,
                accountService
                        .getAccountChangeLog(pageNo, recordsPerPage, login, orderBy, order.equals("asc"))
                        .stream()
                        .map(AccountChangeLogDto::new)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Zwraca preferowany przez użytkownika język
     *
     * @return Preferowany język
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @PermitAll
    public LocaleDto getAccountLocale() throws BaseApplicationException {
        Account account = accountService.findByLogin(authenticationContext.getCurrentUsersLogin());
        LocaleDto localeDto = new LocaleDto();
        localeDto.setLanguageTag(account.getLocale().toLanguageTag());
        return localeDto;
    }

    /**
     * Ustawia preferowany język przez użytkownika
     *
     * @param languageTag Preferowany język
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @PermitAll
    public void changeAccountLocale(String languageTag) throws BaseApplicationException {
        Locale locale = Locale.forLanguageTag(languageTag);
        Account account = accountService.findByLogin(authenticationContext.getCurrentUsersLogin());
        accountService.changeAccountLocale(account, locale);
    }
}
