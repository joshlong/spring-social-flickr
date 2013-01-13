package org.springframework.social.importer.model;

import org.apache.commons.lang.StringUtils;
import org.springframework.social.flickr.api.Photoset;

/**
 * @author Josh Long
 */
public class PhotoSet {


    // these map to records in the database
    private String description, primaryImageUrl, id, title, userId;
    private int countOfVideos = 0, countOfPhotos = 0;

    // these are synthetic and expected to be calculated at runtime for progress updates
    private int photosImported, photosDownloaded;

    public int getPhotosImported() {
        return photosImported;
    }

    public String getPrimaryImageUrl() {
        return primaryImageUrl;
    }

    public void setPhotosImported(int photosImported) {
        this.photosImported = photosImported;
    }

    public int getPhotosDownloaded() {
        return photosDownloaded;
    }

    public void setPhotosDownloaded(int photosDownloaded) {
        this.photosDownloaded = photosDownloaded;
    }

    private void init(int cv, int cp, String title, String description, String id, String userId) {
        this.userId = userId;
        this.countOfPhotos = cp;
        this.id = id;
        this.description = StringUtils.isEmpty(description) ? "" : description;
        this.title = title;
        this.countOfVideos = cv;
    }

    public PhotoSet(int cv, int cp, String primaryImageUrl, String title, String description, String id, String userId) {
        init(cv, cp, title, description, id, userId);
        this.primaryImageUrl = primaryImageUrl;
    }

    public PhotoSet(Photoset photoset, String userId) {
        init(photoset.getCountVideos(), photoset.getCountPhotos(),
                photoset.getTitle(), photoset.getDescription(), photoset.getId(), userId);
    }

    public String getDescription() {
        return description;
    }

    public int getCountOfPhotos() {
        return countOfPhotos;
    }

    public int getCountOfVideos() {
        return countOfVideos;
    }


    public String getTitle() {
        return title;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }
}
