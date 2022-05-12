package pl.lodz.p.it.ssbd2022.ssbd02.controllers;

import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.DataNotFoundException;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.UnauthenticatedException;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.dto.*;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.BaseApplicationException;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.NoAuthenticatedUserFound;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.NoAccountFound;
import pl.lodz.p.it.ssbd2022.ssbd02.mok.endpoint.AccountEndpoint;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/account")
public class AccountController {

    @Inject
    AccountEndpoint accountEndpoint;

    /**
     * Zmienia status użytkownika o danym loginie na podany
     *
     * @param login login użytkownika dla którego ma zostać dokonana zmiana statusu
     * @param accountStatusChangeDto obiekt dto przechowujący status który ma zostać ustawiony
     */
    @PUT
    @Path("/{login}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    public void changeAccountStatus (
            @NotNull @PathParam("login") String login,
            @NotNull @Valid AccountStatusChangeDto accountStatusChangeDto
    ) {
        try {
            accountEndpoint.changeAccountStatus(login, accountStatusChangeDto.getActive());
        } catch (NoAccountFound e) {
            // to bedzie trzeba kiedys bardziej uporzadkowac
            throw new WebApplicationException(e.getMessage(), Response.Status.NOT_FOUND);
        }
    }

    @PUT
    @Path("/{accountId}/change-password")
    @Consumes(MediaType.APPLICATION_JSON)
    public void changeAccountPasswordAsAdmin(
            @NotNull @PathParam("accountId") Long accountId,
            @NotNull @Valid AccountUpdatePasswordDto password) {
        accountEndpoint.updatePasswordAsAdmin(accountId, password);
    }

    @PUT
    @Path("/change-password")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateOwnPassword(@NotNull @Valid AccountUpdatePasswordDto data) {
        try {
            accountEndpoint.updateOwnPassword(data);
        } catch (NoAuthenticatedUserFound e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.NOT_FOUND);
        }
    }

    /**
     * Punkt końcowy pozwalający na rejestrację użytkownika o poziomie dostępu klienta.
     * W przypadku powodzenia konto musi jeszcze zostać aktywowane w polu 'registered'.
     *
     * @param accountRegisterDto Obiekt przedstawiające dane użytkownika do rejestracji
     * @return Odpowiedź HTTP
     * @throws BaseApplicationException Wyjątek aplikacyjny w przypadku niepowodzenia rejestracji użytkownika
     */
    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerAccount(@NotNull @Valid AccountRegisterDto accountRegisterDto) throws BaseApplicationException {
        accountEndpoint.registerAccount(accountRegisterDto);
        return Response.status(Response.Status.CREATED).build();
    }

    /**
     * Punkt końcowy szukający użytkownika
     *
     * @param login nazwa użytkownika
     * @return obiekt DTO informacji o użytkowniku
     * @throws DataNotFoundException W przypadku gdy użytkownik o podanej nazwie nie istnieje lub
     * gdy konto szukanego użytkownika jest nieaktywne lub niepotwierdzone i informacje prubuje uzyskać uzytkownik 
     * niebędący ani administratorem ani moderatorem
     * @throws UnauthenticatedException W przypadku gdy dane próbuje uzyskać niezalogowana osoba
     * @see AccountInfoDto
     */
    @GET
    @Path("/{login}/info")
    @Produces(MediaType.APPLICATION_JSON)
    public AccountInfoDto getUserInfo(@NotNull @PathParam("login") String login) throws DataNotFoundException, UnauthenticatedException {
            return accountEndpoint.getAccountInfo(login);
    }

    /**
     * Punkt końcowy zwracający dane o zalogowanym użytkowniku
     *
     * @return obiekt DTO informacji o użytkowniku
     * @throws UnauthenticatedException W przypadku gdy dane próbuje uzyskać niezalogowana osoba
     * @see AccountInfoDto
     */
    @GET
    @Path("/info")
    @Produces(MediaType.APPLICATION_JSON)
    public AccountInfoDto getUserInfo() throws UnauthenticatedException {
            return accountEndpoint.getYourAccountInfo();
    }
    /**
     * Pozwala zmienić informację aktualnie zalogowanego użytkownika w opraciu o aktualnie zalogowanego użytkownika.
     *
     * @param editAccountInfoDto klasa zawierająca zmienione dane danego użytkownika
     */
    @PUT
    @Path("/editAccountInfo")
    @Consumes(MediaType.APPLICATION_JSON)
    public void editAccountInfo(
            @NotNull @Valid EditAccountInfoDto editAccountInfoDto
    ) {
        try {
            // Może zostać zwrócony obiekt użytkownika w przyszłości po edycji z userEndpoint
            accountEndpoint.editAccountInfo(editAccountInfoDto);
        } catch (NoAuthenticatedUserFound e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.NOT_FOUND);
        }
    }
    
    @GET
    @Path("list")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ListDto getAccountList (
            @QueryParam("pageNo") @DefaultValue("1") int pageNo,
            @QueryParam("recordsPerPage") @NotNull int recordsPerPage,
            @QueryParam("columnName") @NotNull String columnName,
            @QueryParam("order") @DefaultValue("asc") String order
    ){
        return accountEndpoint.getAccountList(
                pageNo,
                recordsPerPage,
                columnName,
                order
        );
    }

}
