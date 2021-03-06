package pl.lodz.p.it.ssbd2022.ssbd02.mor.facade;

import pl.lodz.p.it.ssbd2022.ssbd02.entity.Account;
import pl.lodz.p.it.ssbd2022.ssbd02.entity.PhotographerInfo;
import pl.lodz.p.it.ssbd2022.ssbd02.entity.Reservation;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.BaseApplicationException;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.ExceptionFactory;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.WrongParameterException;
import pl.lodz.p.it.ssbd2022.ssbd02.util.FacadeTemplate;
import pl.lodz.p.it.ssbd2022.ssbd02.util.LoggingInterceptor;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.Interceptors;
import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static pl.lodz.p.it.ssbd2022.ssbd02.security.Roles.*;

@Stateless
@Interceptors({LoggingInterceptor.class, MorFacadeAccessInterceptor.class})
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class ReservationFacade extends FacadeTemplate<Reservation> {
    @PersistenceContext(unitName = "ssbd02morPU")
    private EntityManager em;

    public ReservationFacade() {
        super(Reservation.class);
    }

    @Override
    @RolesAllowed(reservePhotographer)
    public Reservation persist(Reservation entity) throws BaseApplicationException {
        try {
            return super.persist(entity);
        } catch (OptimisticLockException ex) {
            throw ExceptionFactory.OptLockException();
        } catch (PersistenceException ex) {
            throw ExceptionFactory.databaseException();
        } catch (Exception ex) {
            throw ExceptionFactory.unexpectedFailException();
        }
    }

    @RolesAllowed(reservePhotographer)
    public List<Reservation> findInPeriod(Reservation reservation) throws BaseApplicationException {
        TypedQuery<Reservation> query = getEm().createNamedQuery("reservation.findInPeriod", Reservation.class);
        query.setParameter("photographer", reservation.getPhotographer());
        query.setParameter("time_from", reservation.getTimeFrom());
        query.setParameter("time_to", reservation.getTimeTo());
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

    @Override
    public Reservation find(Long id) throws BaseApplicationException {
        try {
            return super.find(id);
        } catch (OptimisticLockException ex) {
            throw ExceptionFactory.OptLockException();
        } catch (PersistenceException ex) {
            throw ExceptionFactory.databaseException();
        } catch (Exception ex) {
            throw ExceptionFactory.unexpectedFailException();
        }
    }

    @Override
    public void remove(Reservation entity) throws BaseApplicationException {
        try {
            super.remove(entity);
        } catch (OptimisticLockException ex) {
            throw ExceptionFactory.OptLockException();
        } catch (PersistenceException ex) {
            throw ExceptionFactory.databaseException();
        } catch (Exception ex) {
            throw ExceptionFactory.unexpectedFailException();
        }
    }

    /**
     * dodaje znak '%' na pocz??tku i na ko??cu struny
     *
     * @param s struna
     * @return struna wynikowa
     */
    private String addPercent(String s) {
        return "%" + s + "%";
    }

    private <T> void addFilterQuery(CriteriaQuery<T> query, CriteriaBuilder criteriaBuilder, Root<Reservation> table, String order) throws WrongParameterException {
        try {
            switch (order) {
                case "asc": {
                    query.orderBy(criteriaBuilder.asc(table.get("timeFrom")));
                    break;
                }
                case "desc": {
                    query.orderBy(criteriaBuilder.desc(table.get("timeFrom")));
                    break;
                }
            }
        } catch (IllegalArgumentException e) {
            throw ExceptionFactory.wrongParameterException();
        }
    }

    /**
     * Dodaje do danej kwerendy szukanie po frazie znajduj??cej si?? w imieniu b??d?? nazwisku danego
     * u??ytkownika
     *
     * @param table tabela, z kt??rej kwerenda b??dzie pobiera?? informacje kwerenda
     * @param name  fraza, kt??ra ma by?? wyszukiwana w imieniu lub nazwisku
     */
    private Predicate addByNameSurnameSearchToQuery(CriteriaBuilder criteriaBuilder, Root<Reservation> table, String name) {
        return criteriaBuilder.or(criteriaBuilder.like(criteriaBuilder.lower(table.get("account").get("name")), addPercent(name.toLowerCase())), criteriaBuilder.like(criteriaBuilder.lower(table.get("account").get("surname")), addPercent(name.toLowerCase())));
    }

    /**
     * Dodaje do danej kwerendy szukanie po frazie znajduj??cej si?? w imieniu b??d?? nazwisku danego
     * u??ytkownika
     *
     * @param table     tabela, z kt??rej kwerenda b??dzie pobiera?? informacje kwerenda
     * @param localDate tydzie??, dla kt??rego ma by?? wyszukiwana rezerwacja
     */
    private Predicate addInWeekSearchToQuery(CriteriaBuilder criteriaBuilder, Root<Reservation> table, LocalDate localDate) {
        return criteriaBuilder.and(
                criteriaBuilder.greaterThanOrEqualTo(table.get("timeFrom"), localDate.atStartOfDay()),
                criteriaBuilder.lessThan(table.get("timeTo"), localDate.plusDays(7).atStartOfDay()));
    }

    /**
     * Metoda pozwalaj??ca na pobieranie rezerwacji dla u??ytkownika (niezako??czonych lub wszystkich)
     *
     * @param account   konto u??ytkownika, dla kt??rego pobierane s?? rezerwacje
     * @param order     kolejno???? sortowania wzgl??dem kolumny time_from
     * @param getAll    flaga decyduj??ca o tym, czy pobierane s?? wszystkie rekordy, czy tylko niezako??czone
     * @param localDate data u??yta przy wyszukiwaniu
     * @param name      nazwa u??ytkownika
     * @return Reservation      lista rezerwacji
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @RolesAllowed(showReservations)
    public List<Reservation> getReservationsForUser(Account account, String name, String order, Boolean getAll, LocalDate localDate) throws BaseApplicationException {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Reservation> query = criteriaBuilder.createQuery(Reservation.class);
        Root<Reservation> table = query.from(Reservation.class);
        query.select(table);
        List<Predicate> predicates = new ArrayList<Predicate>(3);
        predicates.add(criteriaBuilder.equal(table.get("account"), account.getId()));
        addFilterQuery(query, criteriaBuilder, table, order);

        if (!getAll) {
            predicates.add(addInWeekSearchToQuery(criteriaBuilder, table, localDate));
        }
        if (name != null && !name.equals("")) {
            predicates.add(addByNameSurnameSearchToQuery(criteriaBuilder, table, name));
        }
        Predicate[] predArray = new Predicate[predicates.size()];
        predicates.toArray(predArray);
        query.where(predArray);

        try {
            return em.createQuery(query).getResultList();
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

    /**
     * Metoda pozwalaj??ca na pobieranie rezerwacji dla fotografa (niezako??czonych lub wszystkich)
     *
     * @param photographerInfo konto fotografa, dla kt??rego pobierane s?? rezerwacje
     * @param localDate        data u??yta przy wyszukiwaniu
     * @return Reservation      lista rezerwacji
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @PermitAll
    public List<Reservation> getJobsForPhotographer(PhotographerInfo photographerInfo, LocalDate localDate) throws BaseApplicationException {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Reservation> query = criteriaBuilder.createQuery(Reservation.class);
        Root<Reservation> table = query.from(Reservation.class);
        query.select(table);
        List<Predicate> predicates = new ArrayList<Predicate>(3);
        predicates.add(criteriaBuilder.equal(table.get("photographer").get("id"), photographerInfo.getId()));
        predicates.add(addInWeekSearchToQuery(criteriaBuilder, table, localDate));
        Predicate[] predArray = new Predicate[predicates.size()];
        predicates.toArray(predArray);
        query.where(predArray);

        try {
            return em.createQuery(query).getResultList();
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

    /**
     * Metoda pozwalaj??ca na pobieranie rezerwacji dla fotografa (niezako??czonych lub wszystkich)
     *
     * @param photographerInfo konto fotografa, dla kt??rego pobierane s?? rezerwacje
     * @param order            kolejno???? sortowania wzgl??dem kolumny time_from
     * @param getAll           flaga decyduj??ca o tym, czy pobierane s?? wszystkie rekordy, czy tylko niezako??czone
     * @param localDate        data u??yta do wyszukiwania
     * @param name             fraza u??yta do wyszukiwania
     * @return Reservation      lista rezerwacji
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @RolesAllowed(showJobs)
    public List<Reservation> getJobsForPhotographer(PhotographerInfo photographerInfo, String name, String order, Boolean getAll, LocalDate localDate) throws BaseApplicationException {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Reservation> query = criteriaBuilder.createQuery(Reservation.class);
        Root<Reservation> table = query.from(Reservation.class);
        query.select(table);
        List<Predicate> predicates = new ArrayList<Predicate>(3);
        predicates.add(criteriaBuilder.equal(table.get("photographer").get("id"), photographerInfo.getId()));
        addFilterQuery(query, criteriaBuilder, table, order);

        if (!getAll) {
            predicates.add(addInWeekSearchToQuery(criteriaBuilder, table, localDate));
        }
        if (name != null && !name.equals("")) {
            predicates.add(addByNameSurnameSearchToQuery(criteriaBuilder, table, name));
        }
        Predicate[] predArray = new Predicate[predicates.size()];
        predicates.toArray(predArray);
        query.where(predArray);

        try {
            return em.createQuery(query).getResultList();
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
}