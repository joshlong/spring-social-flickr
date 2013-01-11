package org.springframework.social.importer.batch;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.social.flickr.api.*;
import org.springframework.social.flickr.api.impl.FlickrTemplate;
import org.springframework.social.importer.model.PhotoSet;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * loads the data about the photo albums from flickr
 *
 * @author Josh Long
 */
public class PhotoSetItemReader implements ItemReader<PhotoSet>, InitializingBean {

    private Flickr flickrTemplate;
    private Person person;
    private Queue<Photoset> photoSets = new ConcurrentLinkedQueue<Photoset>();
    private PhotosetOperations photosetOperations;
    private String userId;

    public PhotoSetItemReader(Flickr flickrTemplate) {
        this.flickrTemplate = flickrTemplate;
        this.photosetOperations = this.flickrTemplate.photosetOperations();
        this.person = this.flickrTemplate.peopleOperations().getPersonProfile();
        this.userId = person.getId();
    }

    @Override
    public PhotoSet read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        Photoset og = photoSets.isEmpty() ? null : photoSets.remove();
        if (null == og)
            return null;

        Photoset ps =  this.photosetOperations.getInfo(og.getId()) ;
        return new PhotoSet(  ps , userId);
    }

    public void setFlickrTemplate(FlickrTemplate flickrTemplate) {
        this.flickrTemplate = flickrTemplate;
        this.photosetOperations = flickrTemplate.photosetOperations();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Photosets photosets = photosetOperations.getList(userId, null, null);
        for (Photoset ps : photosets.getPhotoset())
            this.photoSets.add(ps);
    }
}

