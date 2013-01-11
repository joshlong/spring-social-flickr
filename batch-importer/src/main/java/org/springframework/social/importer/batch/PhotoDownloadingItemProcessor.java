package org.springframework.social.importer.batch;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.social.flickr.api.Flickr;
import org.springframework.social.flickr.api.PhotoSizeEnum;
import org.springframework.social.importer.model.Photo;
import org.springframework.util.Assert;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * This item processor simply actually 'fetches' the photo
 *
 * @author Josh Long
 */
public class PhotoDownloadingItemProcessor implements ItemProcessor<Photo, Photo> {

    private Log logger = LogFactory.getLog(getClass());

    private File outputDirectory; // should come from job parameters
    private Flickr flickrTemplate;

    public PhotoDownloadingItemProcessor(Flickr flickrTemplate, File outputDirectory) {
        this.outputDirectory = outputDirectory;
        this.flickrTemplate = flickrTemplate;
        Assert.notNull(flickrTemplate, "the flickrTemplate must be non-null");
        Assert.notNull(outputDirectory, "you must specify a non-null output directory");
        Assert.isTrue(outputDirectory.exists(), "the output directory must exist");
    }

    /**
     * checks to see if there's already an existing file and,
     * if it's already there and > 0 bytes, doesn't bother.
     */
    protected boolean shouldFileBeDownloaded(File output) {
        if (output.exists()) {
            if (output.length() == 0) {
                logger.info("the file " + output.getAbsolutePath() + " exists, but it's 0 bytes in length, so we're removing it.");
                output.delete();
            }
        }
        return !(output.exists() && output.length() > 0); // if  it exists and the size is > 0b, then dont download it
    }

    protected String extension(String url) {
        assert url != null;
        if (!StringUtils.isEmpty(url)) {
            return url.substring(url.lastIndexOf("."));
        }
        return null;
    }

    protected File forPhoto(Photo p) {
        File outputForAlbum = new File(this.outputDirectory, p.getAlbumId());
        if (!outputForAlbum.exists()) outputForAlbum.mkdirs();
        return outputForAlbum;
    }


    @Override
    public Photo process(Photo p) throws Exception {
        String url = p.getUrl();
        String ext = extension(url);
        File output = new File(forPhoto(p), p.getId() + ext);
        if (shouldFileBeDownloaded(output)) {
            logger.info("downloading " + url + " to " + output.getAbsolutePath() + ".");
            BufferedImage bi = flickrTemplate.photoOperations().getImage(p.getId(), PhotoSizeEnum.b);
            ImageIO.write(bi, ext.substring(1), output);
        } else {
            logger.info("not downloading " + url + " to " + output.getAbsolutePath() + " because the file already exists.");
        }
        return p;
    }
}
