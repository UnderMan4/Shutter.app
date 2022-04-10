package pl.lodz.p.it.ssbd2022.ssbd02.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Objects;

@Getter
@Setter
@ToString

@NoArgsConstructor
@Entity
@Table(name = "access_level_assignment")
public class AccessLevelAssignment {

    @Column(name = "version")
    private Long version;

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(optional = false)
    @NotNull
    @JoinColumn(name = "account_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "access_level_id", nullable = false)
    @NotNull
    private AccessLevelValue level;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        AccessLevelAssignment that = (AccessLevelAssignment) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
