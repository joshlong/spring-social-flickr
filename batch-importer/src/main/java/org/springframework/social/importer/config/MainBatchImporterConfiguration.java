package org.springframework.social.importer.config;


import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.social.flickr.api.Flickr;
import org.springframework.social.flickr.api.impl.FlickrTemplate;

/**
 * Simple configuration that provides a defintion of the FlickrTemplate object outside of a web session.
 * Relies on the user having the required OAuth values to setup a FlickrTemplate handy
 *
 * @author Josh Long
 */
@Configuration
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@Import(BatchImporterConfiguration.class)
public class MainBatchImporterConfiguration {
    @Bean
    @StepScope
    public Flickr flickrTemplate(@Value("#{jobParameters['accessToken']}") String accessToken,
                                 @Value("#{jobParameters['accessTokenSecret']}") String atSecret,
                                 @Value("#{jobParameters['consumerKey']}") String consumerKey,
                                 @Value("#{jobParameters['consumerSecret']}") String consumerSecret) {
        return new FlickrTemplate(consumerKey, consumerSecret, accessToken, atSecret);
    }
}
