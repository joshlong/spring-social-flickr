/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.social.importer.batch;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.social.flickr.api.Flickr;
import org.springframework.social.flickr.api.MediaEnum;
import org.springframework.social.flickr.api.PhotoSizeEnum;
import org.springframework.social.flickr.api.Photoset;
import org.springframework.social.importer.model.Photo;
import org.springframework.social.importer.model.PhotoSet;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Simple Item reader that reads all the photos for a given {@link org.springframework.social.importer.model.PhotoSet}
 * and then passes that information onto an {@link org.springframework.batch.item.ItemWriter}.
 *
 * @author Josh Long
 */
public class DelegatingPhotoSetPhotoItemReader implements ItemReader<Photo> {

    private JdbcCursorItemReader<PhotoSet> masterAlbumDelegate;
    private Flickr flickrTemplate;
    private PhotoSet photoSet;
    private Queue<org.springframework.social.flickr.api.Photo> photoCollection = new ConcurrentLinkedQueue<org.springframework.social.flickr.api.Photo>();

    public DelegatingPhotoSetPhotoItemReader(Flickr flickr, JdbcCursorItemReader<PhotoSet> masterAlbumDelegate) {
        this.flickrTemplate = flickr;
        this.masterAlbumDelegate = masterAlbumDelegate;
    }

    @Override
    public Photo read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {

        // if there's nothing in the photo collection...
        if (photoCollection.isEmpty()) {
            // then load a PhotoSet
            photoSet = this.masterAlbumDelegate.read();
            // if theres no PhotoSet, then we're done, no more photos to read
            if (null == photoSet)
                return null;

            // if there is a PhotoSet, then load its PhotoDetails
            Photoset photosSet = flickrTemplate.photosetOperations().getPhotos(photoSet.getId(), null, null, null, null, MediaEnum.PHOTOS);
            for (org.springframework.social.flickr.api.Photo p : photosSet.getPhoto()) {
                photoCollection.add(p);
            }
        }

        // if were here, then the photoCollection might still be empty because it might be a flickr album with nothing in it
        org.springframework.social.flickr.api.Photo photo = photoCollection.isEmpty() ? null : photoCollection.remove();
        if (null == photo)
            return null;


        // downloads the 'large' image
        return new Photo(photo.getId(), photo.getIsPrimary(), photo.getUrl(PhotoSizeEnum.b), photo.getUrl(PhotoSizeEnum.s), photo.getTitle(), null, photoSet.getId());
    }
}