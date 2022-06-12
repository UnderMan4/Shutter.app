package pl.lodz.p.it.ssbd2022.ssbd02.mow.service;

import pl.lodz.p.it.ssbd2022.ssbd02.entity.Photo;
import pl.lodz.p.it.ssbd2022.ssbd02.entity.PhotographerInfo;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.BaseApplicationException;
import pl.lodz.p.it.ssbd2022.ssbd02.exceptions.NoPhotoFoundException;
import pl.lodz.p.it.ssbd2022.ssbd02.mow.facade.PhotoFacade;
import pl.lodz.p.it.ssbd2022.ssbd02.mow.facade.ProfileFacade;
import pl.lodz.p.it.ssbd2022.ssbd02.util.S3Service;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import java.util.UUID;

import static pl.lodz.p.it.ssbd2022.ssbd02.security.Roles.*;
import static pl.lodz.p.it.ssbd2022.ssbd02.security.Roles.unlikePhoto;

@Stateless
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class PhotoService {

    @Inject
    private S3Service s3Service;

    @Inject
    private PhotoFacade photoFacade;

    @Inject
    private ProfileFacade profileFacade;


    @PermitAll
    public Photo findById(Long id) throws NoPhotoFoundException {
        throw new UnsupportedOperationException();
    }

    /**
     * Dodaje nowe zdjęcie do galerii fotografa o podanym loginie oraz umieszcza ów zdjęcie w serwisie AWS S3
     *
     * @param login       login fotografa, w którego galerii ma zostać umieszczone zdjęcie
     * @param data        zdjęcie w postaci ciągu bajtów
     * @param title       tytuł zdjęcia
     * @param description opis zdjęcia
     * @throws BaseApplicationException niepowodzenie operacji
     */
    @RolesAllowed(addPhotoToGallery)
    public void addPhoto(String login, byte[] data, String title, String description) throws BaseApplicationException {
        PhotographerInfo photographerInfo = profileFacade.findByLogin(login);
        String objectKey = UUID.randomUUID().toString().replace("-", "");
        Photo newPhoto = new Photo();
        newPhoto.setTitle(title);
        newPhoto.setDescription(description);
        newPhoto.setLikeCount(0L);
        newPhoto.setPhotographer(photographerInfo);

        String s3Url = s3Service.uploadObject(login, data, objectKey);
        newPhoto.setS3Url(s3Url);
        newPhoto.setObjectKey(objectKey);

        photoFacade.persist(newPhoto);
    }

    @RolesAllowed(deletePhotoFromGallery)
    public void deletePhoto(Photo photo) {
        throw new UnsupportedOperationException();
    }

    @RolesAllowed(likePhoto)
    public void likePhoto(Photo photo) {
        throw new UnsupportedOperationException();
    }

    @RolesAllowed(unlikePhoto)
    public void unlikePhoto(Photo photo) {
        throw new UnsupportedOperationException();
    }

}
