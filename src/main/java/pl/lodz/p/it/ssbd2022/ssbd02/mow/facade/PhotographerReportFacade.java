package pl.lodz.p.it.ssbd2022.ssbd02.mow.facade;

import pl.lodz.p.it.ssbd2022.ssbd02.entity.PhotographerReport;
import pl.lodz.p.it.ssbd2022.ssbd02.entity.PhotographerReportCause;
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
import javax.persistence.criteria.Root;
import java.util.List;

import static pl.lodz.p.it.ssbd2022.ssbd02.security.Roles.listAllReports;

@Stateless
@Interceptors({LoggingInterceptor.class, MowFacadeAccessInterceptor.class})
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class PhotographerReportFacade extends FacadeTemplate<PhotographerReport> {
    @PersistenceContext(unitName = "ssbd02mowPU")
    private EntityManager em;

    public PhotographerReportFacade() {
        super(PhotographerReport.class);
    }

    @Override
    @PermitAll
    public EntityManager getEm() {
        return em;
    }

    @Override
    public PhotographerReport persist(PhotographerReport entity) throws BaseApplicationException {
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

    @Override
    public PhotographerReport update(PhotographerReport entity) throws BaseApplicationException {
        try {
            return super.update(entity);
        } catch (OptimisticLockException ex) {
            throw ExceptionFactory.OptLockException();
        } catch (PersistenceException ex) {
            throw ExceptionFactory.databaseException();
        } catch (Exception ex) {
            throw ExceptionFactory.unexpectedFailException();
        }
    }

    @Override
    public PhotographerReport find(Long id) throws BaseApplicationException {
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

    /**
     * Pobiera list?? wszystkich zg??osze?? fotograf??w
     *
     * @return lista zg??osze?? fotograf??w
     */
    @PermitAll
    public List<String> getAllPhotographerReportCauses() {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<String> query = criteriaBuilder.createQuery(String.class);
        Root<PhotographerReportCause> root = query.from(PhotographerReportCause.class);
        query.select(root.get("cause"));
        return em.createQuery(query).getResultList();
    }

    /**
     * Pobiera pow??d zg??oszenia fotografa na podstawie ci??gu znak??w
     *
     * @param cause ci??g znak??w okre??laj??cy pow??d zg??oszenia
     * @return pow??d zg??oszenia fotografa
     * @throws BaseApplicationException wyst??pi?? b????d podczas pobierania powodu zg??oszenia z bazy danych
     */
    public PhotographerReportCause getPhotographerReportCause(String cause) throws BaseApplicationException {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<PhotographerReportCause> query = criteriaBuilder.createQuery(PhotographerReportCause.class);
        Root<PhotographerReportCause> root = query.from(PhotographerReportCause.class);
        query.select(root);
        query.where(criteriaBuilder.equal(root.get("cause"), cause));
        try {
            return em.createQuery(query).getSingleResult();
        } catch (NoResultException e) {
            throw ExceptionFactory.noPhotographerReportFoundException();
        } catch (OptimisticLockException ex) {
            throw ExceptionFactory.OptLockException();
        } catch (PersistenceException ex) {
            throw ExceptionFactory.databaseException();
        } catch (Exception ex) {
            throw ExceptionFactory.unexpectedFailException();
        }
    }

    /**
     * Pobiera list?? zg??osze?? danego fotografa przez danego u??ytkownika
     *
     * @param photographerLogin nazwa u??ytkownika fotografa
     * @param reporterLogin     nazwa u??ytkownika zg??aszaj??cego
     * @return lista zg??osze?? fotografa
     */
    public List<PhotographerReport> getPhotographerReportByPhotographerLoginAndReporterLogin(String photographerLogin, String reporterLogin) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<PhotographerReport> query = criteriaBuilder.createQuery(PhotographerReport.class);
        Root<PhotographerReport> root = query.from(PhotographerReport.class);
        query.select(root);
        query.where(
                criteriaBuilder.equal(root.get("photographer").get("account").get("login"), photographerLogin),
                criteriaBuilder.equal(root.get("account").get("login"), reporterLogin)
        );
        return em.createQuery(query).getResultList();

    }


    /**
     * Pobiera list?? zg??osze?? fotograf??w
     *
     * @param reviewed       czy sprawdzone
     * @param order          kierunek sortowania
     * @param page           numer strony
     * @param recordsPerPage liczba rekord??w na stronie
     * @return lista zg??osze?? fotograf??w
     * @throws WrongParameterException niepoprawna kolejno???? sortowania
     */
    @RolesAllowed(listAllReports)
    public List<PhotographerReport> getPhotographerReportList(Boolean reviewed, String order, int page, int recordsPerPage) throws WrongParameterException {
        CriteriaBuilder criteriaBuilder = getEm().getCriteriaBuilder();
        CriteriaQuery<PhotographerReport> query = criteriaBuilder.createQuery(PhotographerReport.class);
        Root<PhotographerReport> table = query.from(PhotographerReport.class);
        query.select(table);

        switch (order) {
            case "asc": {
                query.orderBy(criteriaBuilder.asc(table.get("createdAt")));
                break;

            }
            case "desc": {
                query.orderBy(criteriaBuilder.desc(table.get("createdAt")));
                break;
            }
        }

        if (reviewed != null) query.where(criteriaBuilder.equal(table.get("reviewed"), reviewed));

        return getEm()
                .createQuery(query)
                .setFirstResult((page - 1) * recordsPerPage)
                .setMaxResults(recordsPerPage)
                .getResultList();
    }

    /**
     * Pobiera liczb?? zg??osze?? fotograf??w
     *
     * @param reviewed czy sprawdzone
     * @return liczba zg??osze?? fotograf??w
     */
    @RolesAllowed(listAllReports)
    public Long getPhotographerReportListSize(Boolean reviewed) {
        CriteriaBuilder criteriaBuilder = getEm().getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<PhotographerReport> table = query.from(PhotographerReport.class);
        query.select(criteriaBuilder.count(table));
        if (reviewed != null) query.where(criteriaBuilder.equal(table.get("reviewed"), reviewed));
        return getEm().createQuery(query).getSingleResult();
    }
}
