package pl.lodz.p.it.ssbd2022.ssbd02.mor.endpoint;

import pl.lodz.p.it.ssbd2022.ssbd02.entity.Availability;
import pl.lodz.p.it.ssbd2022.ssbd02.entity.PhotographerInfo;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.*;
import pl.lodz.p.it.ssbd2022.ssbd02.mor.dto.AvailabilityDto;
import pl.lodz.p.it.ssbd2022.ssbd02.mor.dto.EditAvailabilityDto;
import pl.lodz.p.it.ssbd2022.ssbd02.mor.service.AvailabilityService;
import pl.lodz.p.it.ssbd2022.ssbd02.mor.service.PhotographerService;
import pl.lodz.p.it.ssbd2022.ssbd02.security.AuthenticationContext;
import pl.lodz.p.it.ssbd2022.ssbd02.util.AbstractEndpoint;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static pl.lodz.p.it.ssbd2022.ssbd02.security.Roles.changeAvailabilityHours;

@Stateful
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class AvailabilityEndpoint extends AbstractEndpoint {

    @Inject
    private AuthenticationContext authenticationContext;

    @Inject
    private AvailabilityService availabilityService;

    @Inject
    private PhotographerService photographerService;

    /**
     * Metoda nadpisująca przedziały dostępności fotografa. Poprzednie zakresy zastępowane są tymi, podanymi przez parametr
     * @param availabilitiesDto lista nowych przedziałów dostępności
     * @throws NoAuthenticatedAccountFound akcja wykonywana przez niezalogowanego użytkownika
     * @throws NoPhotographerFoundException nie znaleziono fotografa o podanym loginie
     */
    @RolesAllowed(changeAvailabilityHours)
    public void editAvailability(List<EditAvailabilityDto> availabilitiesDto) throws BaseApplicationException {
        PhotographerInfo photographer = photographerService.getPhotographer(authenticationContext.getCurrentUsersLogin());

        List<Availability> availabilities = new ArrayList<>();
        for (EditAvailabilityDto availabilityDto : availabilitiesDto) {
            Availability availability = new Availability();
            availability.setPhotographer(photographer);
            availability.setDay(availabilityDto.getDay());
            availability.setFrom(availabilityDto.getFrom());
            availability.setTo(availabilityDto.getTo());

            availabilities.add(availability);
        }

        availabilityService.editAvailability(availabilities, photographer);
    }

    /**
     * Metoda zwracająca listę godzin dostępności dla podanego fotografa
     *
     * @param photographerLogin login fotografa
     * @return AvailabilityDto lista godzin dostępności
     * @throws NoPhotographerFoundException nie znaleziono fotografa o podanym loginie
     */
    @PermitAll
    public List<AvailabilityDto> listAvailabilities(String photographerLogin) throws NoPhotographerFoundException {
        List<Availability> availabilities = photographerService.getPhotographer(photographerLogin).getAvailability();

        List<AvailabilityDto> availabilityDtoList = new ArrayList<>();
        for (Availability availability : availabilities) {
            availabilityDtoList.add(new AvailabilityDto(availability));
        }

        return availabilityDtoList;
    }
}
