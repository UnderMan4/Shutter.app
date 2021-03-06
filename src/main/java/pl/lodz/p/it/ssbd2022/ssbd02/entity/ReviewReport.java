package pl.lodz.p.it.ssbd2022.ssbd02.entity;

import lombok.*;
import org.hibernate.Hibernate;
import pl.lodz.p.it.ssbd2022.ssbd02.util.ManagedEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Klasa reprezentująca zgłoszenie recenzji fotografa
 */

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "review_report")
public class ReviewReport extends ManagedEntity {

    @Setter(value = AccessLevel.NONE)
    @Version
    @Column(name = "version")
    private Long version;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(optional = false)
    @NotNull
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;


    /**
     * Flaga wskazująca, czy moderator rozpatrzył zgłoszenie
     */
    @NotNull
    @Column(name = "reviewed", nullable = false)
    private Boolean reviewed;

    @ManyToOne(optional = false)
    @NotNull
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cause_id", nullable = false)
    @NotNull
    private ReviewReportCause cause;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        ReviewReport that = (ReviewReport) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
