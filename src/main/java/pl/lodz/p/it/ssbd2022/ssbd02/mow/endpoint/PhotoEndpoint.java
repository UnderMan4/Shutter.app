package pl.lodz.p.it.ssbd2022.ssbd02.mow.endpoint;

import org.apache.commons.codec.binary.Base64;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.BaseApplicationException;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.NoAuthenticatedAccountFound;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.NoPhotoFoundException;
import pl.lodz.p.it.ssbd2022.ssbd02.mow.dto.AddPhotoDto;
import pl.lodz.p.it.ssbd2022.ssbd02.mow.service.PhotoService;
import pl.lodz.p.it.ssbd2022.ssbd02.security.AuthenticationContext;
import pl.lodz.p.it.ssbd2022.ssbd02.util.AbstractEndpoint;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import static pl.lodz.p.it.ssbd2022.ssbd02.security.Roles.*;
import static pl.lodz.p.it.ssbd2022.ssbd02.security.Roles.unlikePhoto;

@Stateful
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class PhotoEndpoint extends AbstractEndpoint {

    @Inject
    private PhotoService photoService;

    @Inject
    private AuthenticationContext authenticationContext;

    /**
     * Dodaje nowe zdjęcie do galerii obecnie uwierzytelnionego fotografa
     *
     * @param addPhotoDto obiekt DTO zawierający informacje potrzebne do dodania zdjęcia
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @RolesAllowed(addPhotoToGallery)
    public void addPhotoToGallery(AddPhotoDto addPhotoDto) throws BaseApplicationException {
        String login = authenticationContext.getCurrentUsersLogin();
        photoService.addPhoto(
                login,
                Base64.decodeBase64(addPhotoDto.getData()),
                addPhotoDto.getTitle(),
                addPhotoDto.getDescription()
        );
    }

    @RolesAllowed(deletePhotoFromGallery)
    public void deletePhotoFromGallery(Long photoId) throws NoAuthenticatedAccountFound, NoPhotoFoundException {
        throw new UnsupportedOperationException();
    }

    @RolesAllowed(likePhoto)
    public void likePhoto(Long photoId) throws NoAuthenticatedAccountFound, NoPhotoFoundException {
        throw new UnsupportedOperationException();
    }

    @RolesAllowed(unlikePhoto)
    public void unlikePhoto(Long photoId) throws NoAuthenticatedAccountFound, NoPhotoFoundException {
        throw new UnsupportedOperationException();
    }
}
