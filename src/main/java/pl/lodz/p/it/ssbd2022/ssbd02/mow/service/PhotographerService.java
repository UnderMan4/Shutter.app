package pl.lodz.p.it.ssbd2022.ssbd02.mow.service;

import pl.lodz.p.it.ssbd2022.ssbd02.entity.PhotographerInfo;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.BaseApplicationException;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.ExceptionFactory;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.NoPhotographerFound;
import pl.lodz.p.it.ssbd2022.ssbd02.mow.dto.BasePhotographerInfoDto;
import pl.lodz.p.it.ssbd2022.ssbd02.mow.facade.PhotographerInfoFacade;
import pl.lodz.p.it.ssbd2022.ssbd02.util.LoggingInterceptor;

import javax.annotation.security.PermitAll;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

@Stateless
@Interceptors(LoggingInterceptor.class)
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class PhotographerService {

    @Inject
    private PhotographerInfoFacade photographerInfoFacade;

    /**
     * Odnajduje informacje o fotografie na podstawie jego loginu
     *
     * @param login Login fotografa, dla którego chemy pozyskać informacje
     * @return informacje o fotografie o podanym loginie
     * @throws NoPhotographerFound W przypadku gdy profil fotografa dla użytkownika nie istnieje
     */
    @PermitAll
    public PhotographerInfo findByLogin(String login) throws BaseApplicationException {
        return photographerInfoFacade.findPhotographerByLogin(login);
    }

    /**
     * Szuka fotografa
     *
     * @param photographerInfo Informacje o fotografie, które próbuje pozyskać użytkownik
     * @return informacje o podanym fotografie
     * @throws NoPhotographerFound W przypadku gdy fotograf o podanej nazwie użytkownika nie istnieje,
     *                             gdy konto szukanego fotografa jest nieaktywne, niepotwierdzone lub
     *                             profil nieaktywny i informacje próbuje uzyskać użytkownik
     *                             niebędący ani administratorem, ani moderatorem
     * @see BasePhotographerInfoDto
     */
    @PermitAll
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
