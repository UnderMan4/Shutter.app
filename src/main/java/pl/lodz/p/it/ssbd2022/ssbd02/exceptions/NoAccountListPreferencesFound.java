package pl.lodz.p.it.ssbd2022.ssbd02.exceptions;

import javax.ws.rs.core.Response;

public class NoAccountListPreferencesFound extends BaseApplicationException {
    public NoAccountListPreferencesFound(String msg) {
        super(Response.status(Response.Status.NOT_FOUND).entity(msg).build());
    }
}
