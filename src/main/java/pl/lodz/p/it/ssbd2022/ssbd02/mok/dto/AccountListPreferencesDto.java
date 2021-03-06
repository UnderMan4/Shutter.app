package pl.lodz.p.it.ssbd2022.ssbd02.mok.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import pl.lodz.p.it.ssbd2022.ssbd02.entity.AccountListPreferences;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@ToString

public class AccountListPreferencesDto {
    @NotNull
    private Integer page;
    @NotNull
    private Integer recordsPerPage;
    @NotNull
    private Boolean orderAsc;
    @NotNull
    private String orderBy;

    public AccountListPreferencesDto(AccountListPreferences preferences) {
        this.page = preferences.getPage();
        this.recordsPerPage = preferences.getRecordsPerPage();
        this.orderBy = preferences.getOrderBy();
        this.orderAsc = preferences.getOrderAsc();
    }
}
