package pl.lodz.p.it.ssbd2022.ssbd02.mok.service;

import pl.lodz.p.it.ssbd2022.ssbd02.entity.User;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.NoAuthenticatedUser;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.dto.EditUserInfoDto;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.facade.AuthenticationFacade;
import pl.lodz.p.it.ssbd2022.ssbd02.security.AuthenticationContext;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

@Stateless
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class UserService {

    @Inject
    private AuthenticationFacade userFacade;

    @Inject
    private AuthenticationContext authenticationContext;

    @RolesAllowed({"ADMINISTRATOR", "MODERATOR"})
    public void changeAccountStatus(String login, Boolean active) throws NoAuthenticatedUser {
        User user = userFacade.findByLogin(login);
        user.setActive(active);
        userFacade.getEm().merge(user); // TODO Po implementacji transakcyjności zmineić na wywołanie metody update fasady
    }

    @RolesAllowed({"ADMINISTRATOR", "MODERATOR", "PHOTOGRAPHER", "CLIENT"})
    public User editUserInfo(EditUserInfoDto editUserInfoDto) throws NoAuthenticatedUser {
        User user = null;
        user = authenticationContext.getCurrentUser();
        user.setEmail(editUserInfoDto.getEmail());
        user.setName(editUserInfoDto.getName());
        user.setSurname(editUserInfoDto.getSurname());
        return userFacade.update(user);
    }
}
