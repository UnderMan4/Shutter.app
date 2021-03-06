package pl.lodz.p.it.ssbd2022.ssbd02.mor.service;

import pl.lodz.p.it.ssbd2022.ssbd02.entity.*;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.BaseApplicationException;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.ExceptionFactory;
import pl.lodz.p.it.ssbd2022.ssbd02.mor.facade.AvailabilityFacade;
import pl.lodz.p.it.ssbd2022.ssbd02.mor.facade.PhotographerFacade;
import pl.lodz.p.it.ssbd2022.ssbd02.mor.facade.ReservationFacade;
import pl.lodz.p.it.ssbd2022.ssbd02.util.EmailService;
import pl.lodz.p.it.ssbd2022.ssbd02.util.LoggingInterceptor;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static pl.lodz.p.it.ssbd2022.ssbd02.security.Roles.*;

@Stateless
@Interceptors(LoggingInterceptor.class)
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class ReservationService {

    @Inject
    private PhotographerFacade photographerFacade;

    @Inject
    private ReservationFacade reservationFacade;

    @Inject
    private EmailService emailService;

    @Inject
    private AvailabilityFacade availabilityFacade;

    /**
     * Metoda wyszukująca rezerwację z danym numerem ID
     *
     * @param id id rezerwacji
     * @return rezerwacja o wskazanym ID
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @PermitAll
    public Reservation findById(Long id) throws BaseApplicationException {
        return Optional.ofNullable(reservationFacade.find(id)).orElseThrow(ExceptionFactory::noReservationFoundException);
    }

    /**
     * Metoda wyszukująca specjalizację po nazwie
     *
     * @param specialization nazwa specjalizacji
     * @return specjalizacja
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @PermitAll
    public Specialization getSpecialization(String specialization) throws BaseApplicationException {
        return photographerFacade.getSpecialization(specialization);
    }


    /**
     * Metoda służąca do tworzenia rezerwacji.
     * <p>
     * Jeżeli nie ma wolnych terminów, to nie rezerwacja nie może być utworzona.
     * Jeżeli jest więcej niż jeden termin, to nie można stworzyć rezerwacji
     *
     * @param newReservation Nowa rezerwacja
     * @throws BaseApplicationException W przypadku gdy nie można stworzyć rezerwacji
     */
    @RolesAllowed(reservePhotographer)
    public void addReservation(Reservation newReservation) throws BaseApplicationException {
        List<Availability> availability = availabilityFacade.findInPeriod(newReservation);
        // Jeżeli nie ma wolnych terminów, to nie rezerwacja nie może być utworzona.
        // Jeżeli jest więcej niż jeden termin, to nie można stworzyć rezerwacji
        if (availability.size() != 1) {
            throw ExceptionFactory.invalidReservationTimeException("exception.no_availability_in_period");
        }

        List<Reservation> reservationList = reservationFacade.findInPeriod(newReservation);
        if (reservationList.size() > 0) {
            throw ExceptionFactory.invalidReservationTimeException("exception.reservation_already_exists");
        }

        reservationFacade.persist(newReservation);
    }

    /**
     * Metoda odwołująca rezerwację w imieniu wskazanego klienta
     * Klient może odwołać tylko własną rezerwację
     *
     * @param caller        login klienta odwołującego rezerwację
     * @param reservationId id rezerwacji mającej być odwołanej
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @RolesAllowed(cancelReservation)
    public void cancelReservation(String caller, Long reservationId) throws BaseApplicationException {
        Reservation r = findById(reservationId);
        if (!r.getTimeFrom().isAfter(LocalDateTime.now())) {
            throw ExceptionFactory.cannotChangeException();
        }
        if (!r.getAccount().getLogin().equals(caller)) {
            throw ExceptionFactory.noReservationFoundException();
        }
        PhotographerInfo pInfo = r.getPhotographer();
        reservationFacade.remove(r);
        emailService.sendReservationCanceledEmail(pInfo.getAccount().getEmail(), r.getId(), pInfo.getAccount().getLocale());
    }

    /**
     * Metoda odrzucająca rezerwację przez fotografa
     * Fotograf może odwołać tylko własną rezerwację
     *
     * @param caller        login fotografa odwołującego rezerwację
     * @param reservationId id rezerwacji mającej być odwołanej
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @RolesAllowed(discardReservation)
    public void discardReservation(String caller, Long reservationId) throws BaseApplicationException {
        Reservation r = findById(reservationId);
        if (!r.getTimeFrom().isAfter(LocalDateTime.now())) {
            throw ExceptionFactory.cannotChangeException();
        }
        if (!r.getPhotographer().getAccount().getLogin().equals(caller)) {
            throw ExceptionFactory.noReservationFoundException();
        }
        PhotographerInfo pInfo = r.getPhotographer();
        reservationFacade.remove(r);
        emailService.sendReservationDiscardedEmail(r.getAccount().getEmail(), r.getId(), pInfo.getAccount().getLocale());
    }


    /**
     * Metoda pozwalająca na pobieranie rezerwacji dla użytkownika (niezakończonych lub wszystkich)
     *
     * @param account   konto użytkownika, dla którego pobierane są rezerwacje
     * @param order     kolejność sortowania względem kolumny time_from
     * @param getAll    flaga decydująca o tym, czy pobierane są wszystkie rekordy, czy tylko niezakończone
     * @param localDate data, po której ma odbywać się wyszukiwanie
     * @param name      data, po której ma odbywać się wyszukiwanie
     * @return lista rezerwacji
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @RolesAllowed(showReservations)
    public List<Reservation> listReservations(Account account, String name, String order, Boolean getAll, LocalDate localDate)
            throws BaseApplicationException {
        return reservationFacade.getReservationsForUser(account, name, order, getAll, localDate);
    }

    /**
     * Metoda pozwalająca na pobieranie rezerwacji dla fotografa (niezakończonych lub wszystkich)
     *
     * @param photographerInfo konto użytkownika, dla którego pobierane są rezerwacje
     * @param order            kolejność sortowania względem kolumny time_from
     * @param getAll           flaga decydująca o tym, czy pobierane są wszystkie rekordy, czy tylko niezakończone
     * @param localDate        data, po której mają być wyszukiwane prace
     * @param name             fraza, po której ma odbywać się wyszukiwanie
     * @return Reservation      lista rezerwacji
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @RolesAllowed(showJobs)
    public List<Reservation> listJobs(PhotographerInfo photographerInfo, String name, String order, Boolean getAll, LocalDate localDate) throws BaseApplicationException {
        return reservationFacade.getJobsForPhotographer(photographerInfo, name, order, getAll, localDate);
    }

    /**
     * Metoda pozwalająca na pobieranie rezerwacji dla fotografa. Służy do wyświetlania danych w kalendarzu
     *
     * @param photographerInfo konto użytkownika, dla którego pobierane są rezerwacje
     * @param localDate        data p której a
     * @return Reservation      lista rezerwacji
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @PermitAll
    public List<Reservation> listPhotographerJobs(PhotographerInfo photographerInfo, LocalDate localDate) throws BaseApplicationException {
        return reservationFacade.getJobsForPhotographer(photographerInfo, localDate);
    }

    /**
     * Metoda pozwalająca na uzyskanie stronicowanej listy wszystkich aktywnych w systemie fotografów
     *
     * @param page           strona listy, którą należy pozyskać
     * @param recordsPerPage ilość krotek fotografów na stronie
     * @return stronicowana lista aktywnych fotografów obecnych systemie
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @PermitAll
    public List<PhotographerInfo> listPhotographers(int page, int recordsPerPage) throws BaseApplicationException {
        return photographerFacade.getAllPhotographersWithVisibility(true, page, recordsPerPage);
    }

    /**
     * Metoda pozwalająca na uzyskanie liczby wszystkich aktywnych fotografów o podanej widoczności
     *
     * @return liczba aktywnych fotografów obecnych systemie
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @PermitAll
    public Long countPhotographers() throws BaseApplicationException {
        return photographerFacade.countAllPhotographersWithVisibility(true);
    }

    /**
     * Metoda pozwalająca na uzyskanie stronicowanej listy wszystkich aktywnych w systemie fotografów
     * Według imienia i nazwiska oraz specjalizacji
     *
     * @param page           strona listy, którą należy pozyskać
     * @param recordsPerPage ilość krotek fotografów na stronie
     * @param weekDay        dzień tygodnia, w którym szukani są fotografowie
     * @param fromTime       godzina, od której szukani są fotografowie
     * @param toTime         godzina, do której szukani są fotografowie
     * @param spec           specjalizacja, po której należy szukać
     * @param name           fraza, po której należy szukać
     * @return stronicowana lista aktywnych fotografów obecnych systemie
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @PermitAll
    public List<PhotographerInfo> findPhotographerByNameSurnameSpecialization(
            String name,
            int page,
            int recordsPerPage,
            Specialization spec,
            WeekDay weekDay,
            LocalTime fromTime,
            LocalTime toTime
    ) throws BaseApplicationException {
        return photographerFacade.getAllVisiblePhotographersByNameSurnameSpecialization(name, page, recordsPerPage, spec, weekDay, fromTime, toTime);
    }

    @PermitAll
    public Long countAllVisiblePhotographersByNameSurname(String name) throws BaseApplicationException {
        return photographerFacade.countAllVisiblePhotographersByNameSurname(name);
    }
}