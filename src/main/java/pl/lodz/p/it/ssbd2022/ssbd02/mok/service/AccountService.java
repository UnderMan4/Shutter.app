package pl.lodz.p.it.ssbd2022.ssbd02.mok.service;

import org.hibernate.exception.ConstraintViolationException;
import pl.lodz.p.it.ssbd2022.ssbd02.entity.*;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.*;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.dto.*;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.facade.AccessLevelFacade;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.facade.AccountChangeLogFacade;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.facade.AccountListPreferencesFacade;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.facade.AuthenticationFacade;
import pl.lodz.p.it.ssbd2022.ssbd02.security.BCryptUtils;
import pl.lodz.p.it.ssbd2022.ssbd02.security.OneTimeCodeUtils;
import pl.lodz.p.it.ssbd2022.ssbd02.util.ConfigLoader;
import pl.lodz.p.it.ssbd2022.ssbd02.util.EmailService;
import pl.lodz.p.it.ssbd2022.ssbd02.util.LoggingInterceptor;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.persistence.PersistenceException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import static pl.lodz.p.it.ssbd2022.ssbd02.security.Roles.*;
import static pl.lodz.p.it.ssbd2022.ssbd02.util.ConstraintNames.IDENTICAL_EMAIL;
import static pl.lodz.p.it.ssbd2022.ssbd02.util.ConstraintNames.IDENTICAL_LOGIN;

@Stateless
@Interceptors(LoggingInterceptor.class)
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class AccountService {

    @Inject
    private AuthenticationFacade accountFacade;

    @Inject
    private AccessLevelFacade accessLevelFacade;

    @Inject
    private VerificationTokenService verificationTokenService;

    @Inject
    private AccountListPreferencesFacade accountListPreferencesFacade;

    @Inject
    private EmailService emailService;

    @Inject
    private OneTimeCodeUtils codeUtils;

    @Inject
    private AccountChangeLogFacade accountChangeLogFacade;

    @Inject
    private ConfigLoader configLoader;

    /**
     * Odnajduje konto u??ytkownika o podanym loginie
     *
     * @param login Login u??ytkownika, kt??rego konta ma by?? wyszukane
     * @return wyszukane konto u??ytkownika
     * @throws NoAccountFound Konto o podanej nazwie nie istnieje
     */
    @PermitAll
    public Account findByLogin(String login) throws BaseApplicationException {
        return accountFacade.findByLogin(login);
    }

    /**
     * Odnajduje wybran?? warto???? poziomu dost??pu na bazie jej nazwy
     *
     * @param name Nazwa poziomu dost??pu
     * @return poziom dost??pu
     * @throws DataNotFoundException W momencie, gdy dany poziom dost??pu nie zostanie odnaleziony
     */
    @PermitAll
    public AccessLevelValue findAccessLevelValueByName(String name) throws DataNotFoundException {
        return accessLevelFacade.getAccessLevelValue(name);
    }

    /**
     * Zmienia status u??ytkownika o danym loginie na podany
     *
     * @param account u??ytkownika, dla kt??rego ma zosta?? dokonana zmiana statusu
     * @param active  status, kt??ry ma zosta?? ustawiony
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @RolesAllowed({blockAccount, unblockAccount})
    public void changeAccountStatus(Account account, Boolean active) throws BaseApplicationException {
        account.setActive(active);

        if (!active) {
            emailService.sendAccountBlocked(account.getEmail(), account.getLocale());
        } else {
            emailService.sendAccountUnblockedEmail(account.getEmail(), account.getLocale());
        }
        accountFacade.update(account);
    }

    /**
     * Potwierdza aktywacj?? w??asnego konta po d??ugim czasie nieaktywno??ci
     *
     * @param token Obiekt przedstawiaj??cy ??eton weryfikacyjny u??yty do aktywacji konta
     * @throws BaseApplicationException Wyst??puje w przypadku gdy aktywacja konta si?? nie powiedzie
     */
    @PermitAll
    public void confirmUnblockOwnAccount(String token) throws BaseApplicationException {
        Account account = verificationTokenService.confirmUnblockOwnAccount(token);
        account.setActive(true);
        accountFacade.update(account);
    }

    /**
     * Szuka u??ytkownika
     *
     * @param account konto u??ytkownika, kt??rego dane maj?? zosta?? pozyskane
     * @return obiekt DTO informacji o u??ytkowniku
     * @throws NoAccountFound Konto o podanej nazwie nie istnieje w systemie lub jest niepotwierdzone/zablokowane
     * @see Account
     */
    @RolesAllowed(getAccountInfo)
    public Account getAccountInfo(Account account) throws NoAccountFound {
        if (Boolean.TRUE.equals(account.getActive()) && Boolean.TRUE.equals(account.getRegistered())) {
            return account;
        } else {
            throw ExceptionFactory.noAccountFound();
        }
    }


    /**
     * Metoda pozwalaj??ca administratorowi zmieni?? has??o dowolnego u??ytkowika
     *
     * @param account  U??ytkownik, kt??rego has??o administrator chce zmieni??
     * @param password Nowe has??o dla wskazanego u??ytkownika
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @RolesAllowed(changeSomeonesPassword)
    public void changeAccountPasswordAsAdmin(Account account, String password) throws BaseApplicationException {
        changePassword(account, password);
        verificationTokenService.sendForcedPasswordResetToken(account);
    }

    /**
     * Metoda pozwalaj??ca zmieni?? w??asne has??o
     *
     * @param data    obiekt zawieraj??cy stare has??o (w celu weryfikacji) oraz nowe maj??ce by?? ustawione dla u??ytkownika
     * @param account konto, dla kt??rego nale??y zmieni?? has??o
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @RolesAllowed(changeOwnPassword)
    public void updateOwnPassword(Account account, AccountUpdatePasswordDto data) throws BaseApplicationException {
        if (data.getOldPassword() == null) {
            throw ExceptionFactory.wrongPasswordException();
        }
        if (!BCryptUtils.verify(data.getOldPassword().toCharArray(), account.getPassword())) {
            throw ExceptionFactory.passwordMismatchException();
        }

        changePassword(account, data.getPassword());
    }

    /**
     * Pomocnicza metoda utworzone w celu unikni??cia powtarzania kodu.
     * Zmienia has??o wskazanego u??ytkownika
     *
     * @param target      Obiekt u??ytkownika, kt??rego modyfikujemy
     * @param newPassword nowe has??o dla u??ytkownika
     */
    private void changePassword(Account target, String newPassword) throws BaseApplicationException {
        if (newPassword.trim().length() < 8) {
            throw ExceptionFactory.wrongPasswordException();
        }
        String hashed = BCryptUtils.generate(newPassword.toCharArray());
        if (!isPasswordUniqueForUser(hashed, target)) {
            throw ExceptionFactory.nonUniquePasswordException();
        }
        target.setPassword(hashed);
        accountFacade.update(target);
    }

    /**
     * Resetuje has??o u??ytkownika na podane pod warunkiem, ??e ??eton weryfikuj??cy jest aktualny oraz poprawny
     *
     * @param resetPasswordDto Informacje wymagane do resetu has??a (??eton oraz nowe has??o)
     * @throws InvalidTokenException    ??eton jest nieprawid??owego typu lub nieaktualny
     * @throws NoVerificationTokenFound ??eton nie zostanie odnaleziony w bazie
     * @throws ExpiredTokenException    ??eton wygas??
     */
    @PermitAll
    public void resetPassword(ResetPasswordDto resetPasswordDto) throws BaseApplicationException {
        Account account = verificationTokenService.confirmPasswordReset(resetPasswordDto.getToken());
        changePassword(account, resetPasswordDto.getNewPassword());
    }

    /**
     * Aktualizuje adres email u??ytkownika na podane pod warunkiem, ??e ??eton weryfikuj??cy jest aktualny oraz poprawny
     *
     * @param emailUpdateDto Informacje do zmiany emaila u??ytkownika
     * @throws InvalidTokenException    ??eton jest nieprawid??owego typu lub nieaktualny
     * @throws NoVerificationTokenFound ??eton nie zostanie odnaleziony w bazie
     * @throws ExpiredTokenException    ??eton wygas??
     */
    @RolesAllowed((updateEmail))
    public void updateEmail(EmailUpdateDto emailUpdateDto) throws BaseApplicationException {
        Account account = verificationTokenService.confirmEmailUpdate(emailUpdateDto.getToken());
        account.setEmail(emailUpdateDto.getNewEmail());
        accountFacade.update(account);
    }

    /**
     * Nadaje lub odbiera wskazany poziom dost??pu w obiekcie klasy u??ytkownika.
     *
     * @param account          Konto u??ytkownika, dla kt??rego ma nast??pi?? zmiana poziomu dost??pu
     * @param accessLevelValue Poziom dost??pu, kt??ry ma zosta?? zmieniony dla u??ytkownika
     * @param active           Status poziomu dost??pu, kt??ry ma by?? ustawiony
     * @throws CannotChangeException W przypadku pr??by odebrania poziomu dost??pu, kt??rego u??ytkownik nigdy nie posiada??
     * @see AccountAccessLevelChangeDto
     */
    @RolesAllowed({grantAccessLevel, revokeAccessLevel, becomePhotographer})
    public void changeAccountAccessLevel(Account account, AccessLevelValue accessLevelValue, Boolean active)
            throws BaseApplicationException {

        AccessLevelAssignment accessLevelFound = accessLevelFacade.getAccessLevelAssignmentForAccount(
                account,
                accessLevelValue
        );

        if (accessLevelFound != null) {
            if (accessLevelFound.getActive() == active) {
                throw new CannotChangeException("exception.access_level.already_set");
            }

            accessLevelFound.setActive(active);

            sendAccessLevelChangeMail(accessLevelValue, account, active);
            accessLevelFacade.update(accessLevelFound);
        } else {
            AccessLevelAssignment assignment = new AccessLevelAssignment();

            if (!active) {
                throw new CannotChangeException("exception.access_level.already_false");
            }

            assignment.setLevel(accessLevelValue);
            assignment.setAccount(account);
            assignment.setActive(active);

            sendAccessLevelChangeMail(accessLevelValue, account, true);
            accessLevelFacade.persist(assignment);
        }
    }

    /**
     * Metoda pomocnicza s??u????ca do wysy??ania powiadomie?? o zmianach poziomu dost??pu u??ytkownika
     *
     * @param accessLevelValue warto???? poziomu dost??pu
     * @param account          konto dla kt??rego poziom dost??pu zosta?? zmieniony
     * @param active           okre??la czy zmiana stanowi??a przyznanie, czy odebranie poziomu dost??pu
     */
    private void sendAccessLevelChangeMail(AccessLevelValue accessLevelValue, Account account, Boolean active) {
        if (active) {
            emailService.sendAccessLevelGrantedEmail(
                    account.getEmail(),
                    account.getLocale(),
                    accessLevelValue.getName()
            );
        } else {
            emailService.sendAccessLevelRevokedEmail(
                    account.getEmail(),
                    account.getLocale(),
                    accessLevelValue.getName()
            );
        }
    }

    /**
     * Ustawia poziom dost??pu fotografa w obiekcie klasy u??ytkownika na aktywny.
     *
     * @param account Konto u??ytkownika, dla kt??rego ma nast??pi?? nadanie roli fotografa
     * @throws CannotChangeException W przypadku pr??by zostania fotografem przez uzytkownika maj??cego ju?? t?? rol??
     */
    @RolesAllowed(becomePhotographer)
    public void becomePhotographer(Account account)
            throws BaseApplicationException {

        AccessLevelAssignment accessLevelFound = accessLevelFacade.getAccessLevelAssignmentForAccount(
                account,
                accessLevelFacade.getAccessLevelValue("PHOTOGRAPHER")
        );

        if (accessLevelFound != null) {
            if (accessLevelFound.getActive()) {
                throw ExceptionFactory.cannotChangeException();
            }

            accessLevelFound.setActive(true);
            accessLevelFacade.update(accessLevelFound);
        } else {
            AccessLevelAssignment assignment = new AccessLevelAssignment();

            assignment.setLevel(accessLevelFacade.getAccessLevelValue("PHOTOGRAPHER"));
            assignment.setAccount(account);
            assignment.setActive(true);

            accessLevelFacade.persist(assignment);
        }
    }


    /**
     * Odbiera rol?? fotografa poprzez ustawienie poziomu dost??pu fotografa w obiekcie klasy u??ytkownika na nieaktywny.
     *
     * @param account Konto u??ytkownika, dla kt??rego ma nast??pi?? odebranie roli fotografa
     * @throws CannotChangeException W przypadku pr??by zaprzestania bycia fotografem przez uzytkownika maj??cego
     *                               t?? rol?? nieaktywn?? b??d?? wcale jej niemaj??cego
     */
    @RolesAllowed(stopBeingPhotographer)
    public void stopBeingPhotographer(Account account) throws BaseApplicationException {
        AccessLevelAssignment accessLevelFound = accessLevelFacade.getAccessLevelAssignmentForAccount(
                account,
                accessLevelFacade.getAccessLevelValue("PHOTOGRAPHER")
        );

        if (accessLevelFound != null) {
            if (accessLevelFound.getActive()) {
                accessLevelFound.setActive(false);
                accessLevelFacade.update(accessLevelFound);
            } else {
                throw ExceptionFactory.cannotChangeException();
            }
        } else {
            throw ExceptionFactory.cannotChangeException();
        }

    }

    /**
     * Rejestruje konto u??ytkownika z danych podanych w obiekcie klasy u??ytkownika
     * oraz przypisuje do niego poziom dost??pu klienta.
     * W celu aktywowania konta nale??y jeszcze zmieni?? pole 'registered' na warto???? 'true'.
     *
     * @param account Obiekt klasy Account reprezentuj??cej dane u??ytkownika
     * @throws IdenticalFieldException Wyj??tek otrzymywany w przypadku niepowodzenia rejestracji
     *                                 (login lub adres email ju?? istnieje)
     * @see Account
     */
    @PermitAll
    public void registerOwnAccount(Account account)
            throws BaseApplicationException {
        account.setPassword(BCryptUtils.generate(account.getPassword().toCharArray()));
        account.setTwoFAEnabled(false);
        account.setActive(true);
        account.setRegistered(false);
        account.setFailedLogInAttempts(0);
        account.setSecret(UUID.randomUUID().toString());

        addNewAccount(account);

        verificationTokenService.sendRegistrationToken(account);
    }

    /**
     * Rejestruje konto u??ytkownika z danych podanych w obiekcie klasy u??ytkownika (wraz z polami registered i active)
     * oraz przypisuje do niego poziom dost??pu klienta.
     *
     * @param account Obiekt klasy Account reprezentuj??cej dane u??ytkownika
     * @throws IdenticalFieldException Wyj??tek otrzymywany w przypadku niepowodzenia rejestracji
     *                                 (login lub adres email ju?? istnieje)
     * @see Account
     */
    @RolesAllowed({createAccount})
    public void registerAccountByAdmin(Account account)
            throws BaseApplicationException {
        account.setPassword(BCryptUtils.generate(account.getPassword().toCharArray()));
        account.setTwoFAEnabled(false);
        account.setFailedLogInAttempts(0);
        account.setLocale(Locale.forLanguageTag("en"));
        account.setSecret(UUID.randomUUID().toString());

        addNewAccount(account);

        if (!account.getRegistered()) {
            verificationTokenService.sendRegistrationToken(account);
        } else {
            addClientAccessLevel(account);
        }
    }

    /**
     * Tworzy konto u??ytkownika w bazie danych,
     * w przypadku naruszenia unikatowo??ci loginu lub adresu email otrzymujemy wyj??tek
     *
     * @param account obiekt encji u??ytkownika
     * @throws IdenticalFieldException W przypadku, gdy login lub adres email ju?? si?? znajduje w bazie danych
     */
    private void addNewAccount(Account account) throws BaseApplicationException {
        try {
            accountFacade.persist(account);
        } catch (PersistenceException ex) {
            if (ex.getCause() instanceof ConstraintViolationException) {
                String name = ((ConstraintViolationException) ex.getCause()).getConstraintName();
                switch (name) {
                    case IDENTICAL_LOGIN:
                        throw ExceptionFactory.identicalFieldException("exception.identical_login");
                    case IDENTICAL_EMAIL:
                        throw ExceptionFactory.identicalFieldException("exception.identical_email");
                }
            }
            throw ExceptionFactory.databaseException();
        }
    }

    /**
     * Metoda pomocnicza tworz??ca wpis o poziomie dost??pu klient dla danego u??ytkownika.
     *
     * @param account Obiekt klasy Account reprezentuj??cej dane u??ytkownika
     */
    private void addClientAccessLevel(Account account) throws BaseApplicationException {
        AccessLevelValue levelValue = accessLevelFacade.getAccessLevelValue(CLIENT);

        AccessLevelAssignment assignment = new AccessLevelAssignment();

        assignment.setLevel(levelValue);
        assignment.setAccount(account);
        assignment.setActive(true);

        accessLevelFacade.persist(assignment);
    }

    /**
     * Potwierdza rejestracje konta ustawiaj??c pole 'registered' na warto???? 'true'
     *
     * @param token Obiekt przedstawiaj??cy ??eton weryfikacyjny u??yty do potwierdzenia rejestracji
     * @throws BaseApplicationException Wyst??puje w przypadku gdy potwierdzenie rejestracji si?? nie powiedzie
     */
    @PermitAll
    public void confirmAccountRegistration(String token) throws BaseApplicationException {
        Account account = verificationTokenService.confirmRegistration(token);
        account.setRegistered(true);
        addClientAccessLevel(account);
        accountFacade.update(account);
        emailService.sendAccountActivated(account.getEmail(), account.getLocale());
    }

    /**
     * Funkcja do edycji danych u??ytkownika. Zmienia tylko proste informacje, a nie role dost??pu itp
     *
     * @param editAccountInfoDto klasa zawieraj??ca zmienione dane danego u??ytkownika
     * @param account            donto, dla kt??rego nale??y dokona?? edycji danych
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @RolesAllowed(editOwnAccountData)
    public void editAccountInfo(Account account, EditAccountInfoDto editAccountInfoDto) throws BaseApplicationException {
        account.setName(editAccountInfoDto.getName());
        account.setSurname(editAccountInfoDto.getSurname());
        accountFacade.update(account);
    }

    /**
     * Funkcja do edycji danych innego u??ytkownika przez Administratora. Pozwala zmieni?? jedynie email,
     * imi?? oraz nazwisko
     *
     * @param editAccountInfoAsAdminDto klasa zawieraj??ca zmienione dane danego u??ytkownika
     * @param account                   konto, dla kt??rego nale??y zmieni?? dane
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @RolesAllowed({editSomeonesAccountData})
    public void editAccountInfoAsAdmin(Account account, EditAccountInfoAsAdminDto editAccountInfoAsAdminDto) throws BaseApplicationException {
        account.setEmail(editAccountInfoAsAdminDto.getEmail());
        account.setName(editAccountInfoAsAdminDto.getName());
        account.setSurname(editAccountInfoAsAdminDto.getSurname());
        account.setActive(editAccountInfoAsAdminDto.getActive());
        accountFacade.update(account);
    }

    /**
     * Zwraca list?? wszystkich u??ytkownik??w w zadanej kolejno??ci spe??niaj??cych warunki zapytania
     *
     * @param requestDto obiekt DTO zawieraj??cy informacje o sortowaniu i filtrowaniu
     * @param requester  osoba chc??ca pozyska?? list?? u??ytkownik??w
     * @return lista u??ytkownik??w
     */
    @RolesAllowed(listAllAccounts)
    public ListResponseDto<TableAccountDto> getAccountList(Account requester, AccountListRequestDto requestDto) {
        List<Account> list = accountFacade.getAccountList(
                requestDto.getPage(),
                requestDto.getRecordsPerPage(),
                requestDto.getOrderBy(),
                requestDto.getOrderAsc(),
                requestDto.getLogin(),
                requestDto.getEmail(),
                requestDto.getName(),
                requestDto.getSurname(),
                requestDto.getRegistered(),
                requestDto.getActive()
        );
        Long allRecords = accountFacade.getAccountListSize(
                requestDto.getLogin(),
                requestDto.getEmail(),
                requestDto.getName(),
                requestDto.getSurname(),
                requestDto.getRegistered(),
                requestDto.getActive()
        );

        return new ListResponseDto<>(
                requestDto.getPage(),
                (int) Math.ceil(allRecords.doubleValue() / requestDto.getRecordsPerPage()),
                requestDto.getRecordsPerPage(),
                allRecords,
                list.stream().map(TableAccountDto::new).collect(Collectors.toList())
        );
    }

    /**
     * Zapisuje preferencje wy??wietlania listy kont u??ytkownika
     *
     * @param account        konto u??ytkownika, dla kt??rego maj?? zosta?? zapisane preferencje
     * @param orderAsc       kierunek sortowania
     * @param orderBy        parametr sortowania
     * @param page           numer strony
     * @param recordsPerPage ilo???? krotek na stronie
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @RolesAllowed(listAllAccounts)
    public void saveAccountListPreferences(
            Account account,
            int page,
            int recordsPerPage,
            String orderBy,
            Boolean orderAsc
    ) throws BaseApplicationException {
        try {
            AccountListPreferences accountListPreferences = accountListPreferencesFacade.findByAccount(account);
            savePreferences(accountListPreferences, account, page, recordsPerPage, orderBy, orderAsc);
            accountListPreferencesFacade.update(accountListPreferences);
        } catch (NoAccountListPreferencesFound e) {
            AccountListPreferences accountListPreferences = new AccountListPreferences();
            savePreferences(accountListPreferences, account, page, recordsPerPage, orderBy, orderAsc);
            accountListPreferencesFacade.persist(accountListPreferences);
        }
    }


    /**
     * Metoda pomocnicza zapisuj??ca preferencje wy??wietlania listy kont u??ytkownika
     *
     * @param preferences obiekt preferencji, do kt??rego preferencje maj?? zosta?? zapisane
     * @param account     konto u??ytkownika, dla kt??rego maj?? zosta?? zapisane preferencje
     */
    private void savePreferences(
            AccountListPreferences preferences,
            Account account,
            int page,
            int recordsPerPage,
            String orderBy,
            Boolean orderAsc
    ) {
        preferences.setAccount(account);
        preferences.setOrderAsc(orderAsc);
        preferences.setOrderBy(orderBy);
        preferences.setPage(page);
        preferences.setRecordsPerPage(recordsPerPage);
    }

    /**
     * Zwraca ostatnio ustawione w dla danego u??ytkownika preferencje sortowania oraz stronicowania list kont
     *
     * @param account konto, dla kt??rego nale??y zwr??ci?? preferencje
     * @return ostatnio ustawione w dla danego u??ytkownika preferencje sortowania oraz stronicowania list kont
     * @throws BaseApplicationException preferencje dla danego u??ytkownika nie zostan?? odnalezione
     */
    @RolesAllowed(listAllAccounts)
    public AccountListPreferences getAccountListPreferences(Account account) throws BaseApplicationException {
        return accountListPreferencesFacade.findByAccount(account);
    }

    /**
     * Rejestruje nieudane logowanie na konto u??ytkownika poprzez inkrementacj?? licznika nieudanych
     * logowa?? jego konta. Je??eli liczba nieudanych logowa?? b??dzie r??wna lub wi??ksza od 3, to konto zostaje
     * automatycznie zablokowane, a u??ytkownik zostaje powiadomiony o tym drog?? mailow??.
     *
     * @param account   Konto, dla kt??rego nale??y zarejestrowa?? nieudan?? operacj?? logowania
     * @param ipAddress Adres IP, z kt??rego dosz??o do logowania
     */
    @PermitAll
    public void registerFailedLogInAttempt(Account account, String ipAddress) {
        account.setLastFailedLogInAttempt(LocalDateTime.now());
        account.setLastFailedLoginIp(ipAddress);
        if (!account.getActive() || !account.getRegistered()) return;

        Integer failedAttempts = account.getFailedLogInAttempts();
        failedAttempts++;
        account.setFailedLogInAttempts(failedAttempts);

        if (failedAttempts >= configLoader.getAllowedFailedAttempts()) {
            account.setActive(false);
            account.setFailedLogInAttempts(0);
            emailService.sendAccountBlockedDueToToManyLogInAttemptsEmail(account.getEmail(), account.getLocale());
        }
    }

    /**
     * Rejestruje udane logowanie na konto u??ytkownika poprzez wyzerowanie licznika nieudanych zalogowa??.
     *
     * @param account Konto, dla kt??rego nale??y wyzerowa?? licznik nieudanych logowa??
     */
    @PermitAll
    public void registerSuccessfulLogInAttempt(Account account) {
        if (!account.getActive() || !account.getRegistered()) return;
        account.setLastLogIn(LocalDateTime.now());
        account.setFailedLogInAttempts(0);
    }

    /**
     * Powiadamia administratora o zalogowaniu na jego konto poprzez wys??anie na adres email przypisany
     * do jego konta wiadomo??ci zawieraj??cej adres IP, z kt??rego dokonane by??o logowanie
     *
     * @param account   konto administratora, na kt??re dosz??o do zalogowania
     * @param ipAddress adres IP, z kt??rego zosta??o wykonane logowanie
     */
    @PermitAll
    public void sendAdminAuthenticationWarningEmail(Account account, String ipAddress) {
        emailService.sendAdminAuthenticationWaringEmail(
                account.getEmail(),
                account.getLocale(),
                account.getLogin(),
                ipAddress
        );
    }

    @RolesAllowed(getAccountInfo)
    public ListResponseDto<TableAccountDto> findByNameSurname(
            String name,
            int page,
            int recordsPerPage,
            String orderBy,
            String order
    ) throws WrongParameterException {
        List<Account> list = accountFacade.findByNameSurname(name, page, recordsPerPage, orderBy, order);
        Long allRecords = accountFacade.getAccountListSizeNameSurname(name);
        return new ListResponseDto<>(
                page,
                (int) Math.ceil(allRecords.doubleValue() / recordsPerPage),
                recordsPerPage,
                allRecords,
                list.stream().map(TableAccountDto::new).collect(Collectors.toList())
        );
    }

    /**
     * Sprawdza, czy dany u??ytkownik ma uruchomione uwierzytelnianie dwuetapowe
     *
     * @param account u??ytkownik
     * @return true, je??eli u??ytkownik ma w????czone uwierzytelnianie dwuetapowe
     * false jezeli u??ytkownik ma wy????czone uwierzytelnianie dwuetapowe
     */
    @PermitAll
    public Boolean is2FAEnabledForUser(Account account) {
        return account.getTwoFAEnabled();
    }

    /**
     * Sprawdza, czy dany u??ytkownik mia?? ju?? dane has??o ustawione w przesz??o??ci
     *
     * @param newPassword nowe has??o do sprawdzenia
     * @param account     u??ytkownik zmieniaj??cy has??o
     * @return true, je??eli u??ytkownik nie mia?? ustawionego danego has??a
     */
    private boolean isPasswordUniqueForUser(String newPassword, Account account) {
        return account.getOldPasswordList().stream().noneMatch(op -> op.getPassword().equals(newPassword));
    }

    /**
     * Generuje oraz wysy??a kod 2fa dla danego u??ytkownika na jego adres email
     *
     * @param account Konto u??ytkownika
     */
    @PermitAll
    public void send2faCode(Account account) {
        String totp = codeUtils.generateCode(account.getSecret());
        emailService.sendEmail2FA(
                account.getEmail(),
                account.getLocale(),
                account.getLogin(),
                totp
        );
    }

    /**
     * Zwraca histori?? zmian dla konta
     *
     * @param login          Login u??ytkownika, kt??rego historia zmian konta ma by?? wyszukana
     * @param page           numer strony
     * @param recordsPerPage liczba rekord??w na stron??
     * @param orderBy        kolumna, po kt??rej nast??puje sortowanie
     * @param orderAsc       kolejno???? sortowania
     * @return Historia zmian konta
     */
    @RolesAllowed({getOwnAccountInfo, getEnhancedAccountInfo})
    public List<AccountChangeLog> getAccountChangeLog(
            int page,
            int recordsPerPage,
            String login,
            String orderBy,
            Boolean orderAsc
    ) {
        return accountChangeLogFacade.findByLogin(login, orderBy, orderAsc, recordsPerPage, page);
    }


    @RolesAllowed({getOwnAccountInfo, getEnhancedAccountInfo})
    public Long getAccountLogListSize(String login) {
        return accountChangeLogFacade.getListSize(login);
    }


    /**
     * Wy????cza lub w????cza dwustopniowe uwierzytelnianie dla u??ytkownika
     *
     * @param account Konto u??ytkownika
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @PermitAll
    public void toggle2fa(Account account) throws BaseApplicationException {
        account.setTwoFAEnabled(!account.getTwoFAEnabled());
        accountFacade.update(account);
    }

    /**
     * Ustawia preferowany j??zyk przez u??ytkownika
     *
     * @param account Konto u??ytkownika
     * @param locale  J??zyk
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @PermitAll
    public void changeAccountLocale(Account account, Locale locale) throws BaseApplicationException {
        account.setLocale(locale);
        accountFacade.update(account);
    }

}