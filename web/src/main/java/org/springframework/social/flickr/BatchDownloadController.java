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
package org.springframework.social.flickr;


import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.social.flickr.api.Flickr;
import org.springframework.social.importer.FlickrImporter;
import org.springframework.social.importer.model.PhotoSet;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.util.Collection;

/**
 * this controller kicks off and manages the downloads of photos for an authenticated user.
 *
 * @author Josh Long
 */
@Controller
public class BatchDownloadController {

    private final FlickrImporter importer;
    private final Flickr flickr;
    private final Logger logger = Logger.getLogger(getClass());
    private File basePhotoPath = new File(SystemUtils.getUserHome(), "/flickr/");

    @PostConstruct
    public void setup() throws Throwable {
        assert basePhotoPath.exists() || basePhotoPath.mkdirs() : "we could not ensure that " + basePhotoPath.getAbsolutePath() + " exists.";
    }

    @Inject
    public BatchDownloadController(Flickr flickr, FlickrImporter importer) {
        this.importer = importer;
        this.flickr = flickr;
        assert flickr != null : "flickr can't be null";
        assert importer != null : "importer can't be null";
    }

    @RequestMapping(value = "/batch/stop", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public void stopImport() throws Throwable {
        final String flickrUserId = flickr.peopleOperations().getProfileId();
        importer.stopImport(flickrUserId);
        logger.info("attempted to stop the import process for user " + flickrUserId + " to output directory.");
    }

    @RequestMapping(value = "/batch/start", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public void startImport() throws Throwable {
        final String flickrUserId = flickr.peopleOperations().getProfileId();
        final File outputForFlickrUser = forUserOutput(this.basePhotoPath, flickrUserId);
        importer.startImport(flickrUserId, outputForFlickrUser);
        logger.info("launched the import process for user " + flickrUserId +
                " to output directory " + outputForFlickrUser.getAbsolutePath() + ".");
    }

    @RequestMapping("/batch/albums")
    @ResponseBody
    public Collection<PhotoSet> albumsImportedForUser() {
        return importer.photoSetsImportedForUser(flickr.peopleOperations().getProfileId());
    }

    protected File forUserOutput(File base, String profileId) {
        return new File(base, profileId);
    }
}
