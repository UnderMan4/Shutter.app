package pl.lodz.p.it.ssbd2022.ssbd02.mok.service;


import pl.lodz.p.it.ssbd2022.ssbd02.entity.Account;
import pl.lodz.p.it.ssbd2022.ssbd02.entity.PhotographerInfo;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.BaseApplicationException;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.ExceptionFactory;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.NoPhotographerFound;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.facade.PhotographerInfoFacade;
import pl.lodz.p.it.ssbd2022.ssbd02.util.LoggingInterceptor;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import static pl.lodz.p.it.ssbd2022.ssbd02.security.Roles.*;

@Stateless
@Interceptors(LoggingInterceptor.class)
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class PhotographerService {

    @Inject
    private PhotographerInfoFacade photographerInfoFacade;

    /**
     * Tworzy pusty obiekt reprezentujący informacje o fotografie lub aktywuje istniejący,
     * jeżeli już istnieje
     *
     * @param account Account Konto fotografa, któremu chcemy dodać informacje
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @RolesAllowed(becomePhotographer)
    public void createOrActivatePhotographerInfo(Account account) throws BaseApplicationException {
        try {
            PhotographerInfo existingPhotographerInfo = photographerInfoFacade.findPhotographerByLogin(account.getLogin());
            existingPhotographerInfo.setVisible(true);

            photographerInfoFacade.update(existingPhotographerInfo);
        } catch (NoPhotographerFound e) {
            PhotographerInfo photographerInfo = new PhotographerInfo();
            photographerInfo.setId(account.getId());
            photographerInfo.setScore(0L);
            photographerInfo.setReviewCount(0L);
            photographerInfo.setAccount(account);
            photographerInfo.setDescription("");
            photographerInfo.setVisible(true);

            photographerInfoFacade.persist(photographerInfo);
        }
    }

    /**
     * Ukrywa informacje o fotografie
     *
     * @param login Login Konto fotografa, któremu chcemy ukryć informacje
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @RolesAllowed(stopBeingPhotographer)
    public void hidePhotographerInfo(String login) throws BaseApplicationException {
        PhotographerInfo photographerInfo = photographerInfoFacade.findPhotographerByLogin(login);

        if (photographerInfo.getVisible()) {
            photographerInfo.setVisible(false);
            photographerInfoFacade.persist(photographerInfo);
        } else {
            throw ExceptionFactory.cannotChangeException();
        }

    }

    /**
     * Szuka fotografa
     *
     * @param photographerInfo Informacje o fotografie, które próbuje pozyskać użytkownik
     * @return informacje o fotografie
     * @throws NoPhotographerFound W przypadku gdy fotograf o podanej nazwie użytkownika nie istnieje,
     *                             gdy konto szukanego fotografa jest nieaktywne, niepotwierdzone lub
     *                             profil nieaktywny i informacje próbuje uzyskać użytkownik
     *                             niebędący ani administratorem, ani moderatorem
     */
    @RolesAllowed(getPhotographerInfo)
    public PhotographerInfo getPhotographerInfo(PhotographerInfo photographerInfo)
            throws NoPhotographerFound {
        if (
                Boolean.TRUE.equals(photographerInfo.getVisible())
                        && Boolean.TRUE.equals(photographerInfo.getAccount().getActive())
                        && Boolean.TRUE.equals(photographerInfo.getAccount().getRegistered())
        ) {
            return photographerInfo;
        } else {
            throw ExceptionFactory.noPhotographerFound();
        }
    }
}
