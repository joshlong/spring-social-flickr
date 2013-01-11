package org.springframework.social.flickr;


import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;
import org.springframework.social.flickr.api.Flickr;
import org.springframework.social.importer.FlickrImporter;
import org.springframework.social.importer.PhotoSet;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.util.Collection;
import java.util.concurrent.Callable;

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
        if (!basePhotoPath.exists())
            assert basePhotoPath.mkdirs() : "we could not ensure that " + basePhotoPath.getAbsolutePath() + " exists.";
    }

    protected File forUserOutput(File base, String profileId) {
        return new File(base, profileId);
    }

    @Inject
    public BatchDownloadController(Flickr flickr, FlickrImporter importer) {
        this.importer = importer;
        this.flickr = flickr;
        assert flickr != null : "flickr can't be null";
        assert importer != null : "importer can't be null";
    }

    // couple of different use cases
    // 1) kick off the process
    @RequestMapping(value = "/batch/start", method = RequestMethod.GET)
    public  String startImport() throws Throwable {

        final String flickrUserId = flickr.peopleOperations().getProfileId();
        final File outputForFlickrUser = forUserOutput(this.basePhotoPath, flickrUserId);
        importer.importPhotosToDirectory(outputForFlickrUser);

        logger.info("launched the import process for user " + flickrUserId + " to output directory " + outputForFlickrUser.getAbsolutePath() + ".");

        return "welcome" ;
    }



//    @RequestMapping
//    public Collection<PhotoSet> albumsImportedForUser() {
//      return  importer.photoSetsImportedForUser( flickr.peopleOperations().getProfileId());
//    }
}
