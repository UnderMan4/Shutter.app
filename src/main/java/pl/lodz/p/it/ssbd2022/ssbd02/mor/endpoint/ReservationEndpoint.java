package pl.lodz.p.it.ssbd2022.ssbd02.mor.endpoint;

import pl.lodz.p.it.ssbd2022.ssbd02.entity.*;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.BaseApplicationException;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.ExceptionFactory;
import pl.lodz.p.it.ssbd2022.ssbd02.mor.dto.*;
import pl.lodz.p.it.ssbd2022.ssbd02.mor.service.MorAccountService;
import pl.lodz.p.it.ssbd2022.ssbd02.mor.service.PhotographerService;
import pl.lodz.p.it.ssbd2022.ssbd02.mor.service.ReservationService;
import pl.lodz.p.it.ssbd2022.ssbd02.security.AuthenticationContext;
import pl.lodz.p.it.ssbd2022.ssbd02.util.AbstractEndpoint;
import pl.lodz.p.it.ssbd2022.ssbd02.util.LoggingInterceptor;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static pl.lodz.p.it.ssbd2022.ssbd02.security.Roles.*;

@Stateful
@Interceptors({LoggingInterceptor.class})
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class ReservationEndpoint extends AbstractEndpoint {

    @Inject
    private ReservationService reservationService;

    @Inject
    private MorAccountService accountService;

    @Inject
    private PhotographerService photographerService;

    @Inject
    private AuthenticationContext authenticationContext;

    /**
     * Metoda służąca do tworzenia rezerwacji.
     *
     * @param createReservationDto Dane potrzebne do utworzenia rezerwacji
     * @throws BaseApplicationException W przypadku gdy nie można stworzyć rezerwacji
     */
    @RolesAllowed(reservePhotographer)
    public void createReservation(CreateReservationDto createReservationDto) throws BaseApplicationException {
        Reservation reservation = new Reservation();
        String login = authenticationContext.getCurrentUsersLogin();
        if (login.equals(createReservationDto.getPhotographerLogin())) {
            throw ExceptionFactory.cannotPerformOnSelfException();
        }
        PhotographerInfo photographerInfo = photographerService.getPhotographer(createReservationDto.getPhotographerLogin());
        if (!photographerInfo.getVisible()) {
            throw ExceptionFactory.noPhotographerFound();
        }
        reservation.setPhotographer(photographerInfo);
        reservation.setAccount(accountService.findByLogin(login));
        reservation.setTimeFrom(createReservationDto.getFrom());
        reservation.setTimeTo(createReservationDto.getTo());
        reservationService.addReservation(reservation);
    }

    /**
     * Metoda do anulowania rezerwacji przez klienta
     *
     * @param reservationId id rezerwacji, która ma być anulowana
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @RolesAllowed(cancelReservation)
    public void cancelReservation(Long reservationId) throws BaseApplicationException {
        String caller = authenticationContext.getCurrentUsersLogin();
        reservationService.cancelReservation(caller, reservationId);
    }

    /**
     * Metoda do anulowania rezerwacji przez fotografa
     *
     * @param reservationId id rezerwacji, która ma być anulowana
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @RolesAllowed(discardReservation)
    public void discardReservation(Long reservationId) throws BaseApplicationException {
        String caller = authenticationContext.getCurrentUsersLogin();
        reservationService.discardReservation(caller, reservationId);
    }

    /**
     * Metoda pozwalająca na pobieranie rezerwacji dla użytkownika (niezakończonych lub wszystkich)
     *
     * @param name      imię lub nazwisko do wyszukania
     * @param order     kolejność sortowania względem kolumny time_from
     * @param getAll    flaga decydująca o tym, czy pobierane są wszystkie rekordy, czy tylko niezakończone
     * @param localDate data, po której ma być wykonane wyszukiwanie
     * @return lista rezerwacji
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @RolesAllowed(showReservations)
    public List<ReservationListEntryDto> listReservations(String name, String order, Boolean getAll, LocalDate localDate)
            throws BaseApplicationException {
        String login = authenticationContext.getCurrentUsersLogin();
        Account account = accountService.findByLogin(login);

        List<Reservation> reservations = reservationService.listReservations(account, name, order, getAll, localDate);
        List<ReservationListEntryDto> reservationDtoList = new ArrayList<>();

        for (Reservation reservation : reservations) {
            reservationDtoList.add(new ReservationListEntryDto(reservation));
        }

        return reservationDtoList;
    }

    /**
     * Metoda pozwalająca na pobieranie rezerwacji dla fotografa (niezakończonych lub wszystkich)
     *
     * @param name      imię lub nazwisko do wyszukania
     * @param order     kolejność sortowania względem kolumny time_from
     * @param getAll    flaga decydująca o tym, czy pobierane są wszystkie rekordy, czy tylko niezakończone
     * @param localDate data, od której ma odbywać się wyszukiwanie
     * @return lista rezerwacji
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @RolesAllowed(showJobs)
    public List<ReservationListEntryDto> listJobs(String name, String order, Boolean getAll, LocalDate localDate) throws BaseApplicationException {
        String login = authenticationContext.getCurrentUsersLogin();
        PhotographerInfo photographerInfo = photographerService.getPhotographer(login);

        List<Reservation> reservations = reservationService.listJobs(photographerInfo, name, order, getAll, localDate);
        List<ReservationListEntryDto> reservationDtoList = new ArrayList<>();

        for (Reservation reservation : reservations) {
            reservationDtoList.add(new ReservationListEntryDto(reservation));
        }

        return reservationDtoList;
    }

    /**
     * Metoda pozwalająca na pobieranie rezerwacji dla fotografa. Służy do wyświetlania danych w kalendarzu
     *
     * @param login     login fotografa
     * @param localDate poniedziałek dla tygodnia, dla którego mają być pobrane rezerwacje
     * @return ReservationListEntryDto      lista rezerwacji
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @PermitAll
    public List<ReservationCalendarEntryDto> listPhotographerJobs(String login, LocalDate localDate) throws BaseApplicationException {
        PhotographerInfo photographerInfo = photographerService.getPhotographer(login);

        List<Reservation> reservations = reservationService.listPhotographerJobs(photographerInfo, localDate);
        List<ReservationCalendarEntryDto> reservationDtoList = new ArrayList<>();

        for (Reservation reservation : reservations) {
            reservationDtoList.add(new ReservationCalendarEntryDto(reservation));
        }

        return reservationDtoList;
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
    public MorListResponseDto<PhotographerListEntryDto> listPhotographers(int page, int recordsPerPage) throws BaseApplicationException {
        Long photographerCount = reservationService.countPhotographers();
        return new MorListResponseDto(
                page,
                (int) Math.ceil(photographerCount.doubleValue() / recordsPerPage),
                recordsPerPage,
                photographerCount,
                reservationService.listPhotographers(page, recordsPerPage).stream()
                        .map(PhotographerListEntryDto::new)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Wyszukuje wszystkich fotografów dostępnych w podanych godzinach
     *
     * @param timePeriod zakres czasu, w którym ma być wyszukiwanie
     * @return lista fotografów dostępnych w podanych godzinach
     */
    @PermitAll
    public List<PhotographerListEntryDto> findPhotographerByAvailability(TimePeriodDto timePeriod) {
        throw new UnsupportedOperationException();
    }

    /**
     * Metoda pozwalająca na uzyskanie stronicowanej listy wszystkich aktywnych w systemie fotografów, których imię
     * lub nazwisko zawiera szukaną frazę oraz zajmują się podaną specjalizacją
     *
     * @param name           szukana fraza
     * @param spec           specjalizacja
     * @param page           strona listy, którą należy pozyskać
     * @param recordsPerPage ilość krotek fotografów na stronie
     * @param toTime         czas, do którego ma być wyszukiwanie
     * @param fromTime       czas, od którego ma być wyszukiwanie
     * @param weekDay        dzień dnia, w którym ma być wyszukiwanie
     * @return stronicowana lista aktywnych fotografów obecnych systemie, których imię lub nazwisko zawiera podaną frazę
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @PermitAll
    public MorListResponseDto<PhotographerListEntryDto> findPhotographerByNameSurnameSpecializationWeekDayFromTimeEndTime(
            String name,
            int page,
            int recordsPerPage,
            String spec,
            String weekDay,
            LocalTime fromTime,
            LocalTime toTime
    ) throws BaseApplicationException {
        WeekDay weekDayParsed = null;
        if (weekDay != null) {
            try {
                weekDayParsed = WeekDay.valueOf(weekDay);

            } catch (IllegalArgumentException e) {
                throw ExceptionFactory.wrongDayNameException();
            }
        }

        Specialization specialization;

        if (spec != null) {
            specialization = reservationService.getSpecialization(spec);
        } else {
            specialization = null;
        }

        List<PhotographerInfo> list = reservationService.findPhotographerByNameSurnameSpecialization(
                name, page, recordsPerPage, specialization, weekDayParsed, fromTime, toTime);
        Long photographerCount = (long) list.size();

        return new MorListResponseDto(
                page,
                (int) Math.ceil(photographerCount.doubleValue() / recordsPerPage),
                recordsPerPage,
                photographerCount,
                list.stream()
                        .map(PhotographerListEntryDto::new)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Wyszukuje wszystkich fotografów zajmujących się podaną specjalnością
     *
     * @param specialization specjalność, po której ma odbywać się wyszukiwanie
     * @return lista fotografów zajmujących się określoną specjalnością
     */
    @PermitAll
    public List<PhotographerListEntryDto> findPhotographerBySpeciality(String specialization) {
        throw new UnsupportedOperationException();
    }
}
