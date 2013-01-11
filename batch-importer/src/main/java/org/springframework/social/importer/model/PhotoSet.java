package org.springframework.social.importer.model;

import org.apache.commons.lang.StringUtils;
import org.springframework.social.flickr.api.Photoset;

/**
 * @author Josh Long
 */
public class PhotoSet {


    // these map to records in the database
    private String description, url, id, title, userId;
    private int countOfVideos = 0, countOfPhotos = 0;

    // these are synthetic and expected to be calculated at runtime for progress updates
    private int photosImported, photosDownloaded;

    public int getPhotosImported() {
        return photosImported;
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

    private void init(int cv, int cp, String url, String title, String description, String id, String userId) {
        this.userId = userId;
        this.countOfPhotos = cp;
        this.id = id;
        this.description = StringUtils.isEmpty(description) ? "" : description;
        this.url = url;
        this.title = title;
        this.countOfVideos = cv;
    }

    public PhotoSet(int cv, int cp, String url, String title, String description, String id, String userId) {
        init(cv, cp, url, title, description, id, userId);
    }

    public PhotoSet(Photoset photoset, String userId) {

        init(photoset.getCountVideos(), photoset.getCountPhotos(),
                photoset.getUrl(), photoset.getTitle(), photoset.getDescription(), photoset.getId(), userId);
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

    public String getUrl() {
        return url;
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
