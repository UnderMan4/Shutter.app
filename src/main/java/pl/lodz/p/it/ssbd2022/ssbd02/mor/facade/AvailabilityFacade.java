package pl.lodz.p.it.ssbd2022.ssbd02.mor.facade;

import pl.lodz.p.it.ssbd2022.ssbd02.entity.Availability;
import pl.lodz.p.it.ssbd2022.ssbd02.entity.Reservation;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.BaseApplicationException;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.ExceptionFactory;
import pl.lodz.p.it.ssbd2022.ssbd02.util.FacadeTemplate;
import pl.lodz.p.it.ssbd2022.ssbd02.util.LoggingInterceptor;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.Interceptors;
import javax.persistence.*;
import java.util.List;

import static pl.lodz.p.it.ssbd2022.ssbd02.entity.WeekDay.getWeekDay;
import static pl.lodz.p.it.ssbd2022.ssbd02.security.Roles.reservePhotographer;

@Stateless
@Interceptors({LoggingInterceptor.class, MorFacadeAccessInterceptor.class})
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class AvailabilityFacade extends FacadeTemplate<Availability> {
    @PersistenceContext(unitName = "ssbd02morPU")
    private EntityManager em;

    public AvailabilityFacade() {
        super(Availability.class);
    }

    @RolesAllowed(reservePhotographer)
    public List<Availability> findInPeriod(Reservation reservation) throws BaseApplicationException {
        TypedQuery<Availability> query = getEm().createNamedQuery("availability.findInPeriod", Availability.class);
        query.setParameter("photographer", reservation.getPhotographer());
        query.setParameter("from", reservation.getTimeFrom().toLocalTime());
        query.setParameter("to", reservation.getTimeTo().toLocalTime());
        query.setParameter("day", getWeekDay(reservation.getTimeFrom()));
        try {
            return query.getResultList();
        } catch (NoResultException e) {
            throw ExceptionFactory.noAccountFound();
        } catch (OptimisticLockException ex) {
            throw ExceptionFactory.OptLockException();
        } catch (PersistenceException ex) {
            throw ExceptionFactory.databaseException();
        } catch (Exception ex) {
            throw ExceptionFactory.unexpectedFailException();
        }
    }

    @Override
    @PermitAll
    public EntityManager getEm() {
        return em;
    }
}
