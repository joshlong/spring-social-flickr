package org.springframework.social.importer.model;


import org.springframework.social.flickr.api.PhotoDetail;
import org.springframework.social.flickr.api.Url;

/**
 * wrapper for SS Flickr's {@link PhotoDetail photo details} object.
 *
 * @author Josh Long
 */
public class Photo {

    private String url, thumbnailUrl, id, title, comments, albumId;
    private boolean primary ;
    private org.springframework.social.flickr.api.Photo flickrPhoto ;// we can use this if its available

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    protected void setup(String id,  boolean p,String url, String thumbUrl, String title, String comments, String albumId) {
        this.url = url;
        this.id = id;
        this.thumbnailUrl = thumbUrl ;
        this.primary = p ;
        this.title = title;
        this.comments = comments;
        this.albumId = albumId;
    }

    public Photo(String id,  boolean primary,String url,String thumbUrl,  String title, String comments, String albumId) {
        setup(id,    primary , url, thumbUrl, title, comments, albumId);
    }

    public Photo(org.springframework.social.flickr.api.Photo flickrPhoto, String id, boolean p, String url,  String thumbUrl, String title, String comments, String albumId) {
        this.flickrPhoto = flickrPhoto ;
        setup(id,   p, url, thumbUrl,title, comments, albumId);
    }

    public String getUrl() {
        return url;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public String getId() {
        return this.id;
    }

    public String getTitle() {
        return title;
    }

    public String getComments() {
        return comments;
    }


    public String getAlbumId() {
        return albumId;
    }


}
