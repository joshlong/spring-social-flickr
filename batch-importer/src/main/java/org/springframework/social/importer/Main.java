package org.springframework.social.importer;

import org.apache.commons.lang.SystemUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.social.importer.config.BatchImporterConfiguration;
import org.springframework.social.importer.config.MainBatchImporterConfiguration;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Entry point for the program. Reads in properties from the Desktop (<CODE>~/Desktop/flickr.properties</CODE>),
 * and uses them to launch a <code>Job</code>.
 *
 * @author Josh Long
 */
public class Main {

    public static void main(String args[]) throws Throwable {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        registerPropertiesForFlickrConnection(applicationContext);
        applicationContext.register( MainBatchImporterConfiguration.class);
        applicationContext.refresh();
        applicationContext.start();

        FlickrImporter flickrImporter = applicationContext.getBean(FlickrImporter.class);
        Environment environment = applicationContext.getEnvironment();
        File output = new File(new File(SystemUtils.getUserHome(), "Desktop"), "flickr-output");

        if (!output.exists()) {
            Assert.isTrue(output.mkdirs(),
                    "there should be a directory to contain the " +
                            "output photos, but we could not create it!.");
        }

        String clientId = environment.getProperty("clientId"),
                clientSecret = environment.getProperty("clientSecret"),
                accessToken = environment.getProperty("accessToken"),
                accessTokenSecret = environment.getProperty("accessTokenSecret");

        flickrImporter.importPhotosToDirectory(accessToken, accessTokenSecret, clientId, clientSecret, output);


    }
    private static <T extends AbstractApplicationContext> void registerPropertiesForFlickrConnection(T applicationContext) {
        try {
            File propertiesFile = new File(SystemUtils.getUserHome(), "flickr.properties");
            Assert.isTrue(propertiesFile.exists(),
                    "could not find " + propertiesFile.getAbsolutePath() +
                            ", which must exist and contain at a minimum a Flickr client ID and secret ('" +
                            "clientId, and clientSecret) and database connection information (dataSource.user, " +
                            "dataSource.password, dataSource.url, and dataSource.driverClassName)."
            );
            Resource propertiesResource = new FileSystemResource(propertiesFile);
            Properties properties = new Properties();
            properties.load(propertiesResource.getInputStream());
            PropertiesPropertySource mapPropertySource = new PropertiesPropertySource("flickr", properties);
            applicationContext.getEnvironment().getPropertySources().addLast(mapPropertySource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}

