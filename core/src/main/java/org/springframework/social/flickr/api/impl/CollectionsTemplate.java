package org.springframework.social.flickr.api.impl;

import org.springframework.social.flickr.api.CollectionsOperations;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

/**
 * @author HemantS
 */
public class CollectionsTemplate extends AbstractFlickrOperations implements
        CollectionsOperations {
    private final RestTemplate restTemplate;

    public CollectionsTemplate(RestTemplate restTemplate,
                               boolean isAuthorizedForUser) {
        super(isAuthorizedForUser);
        this.restTemplate = restTemplate;
    }

    @Override
    public void getInfo(String apiKey, String collectionId) {
        requireAuthorization();
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();
        if (apiKey != null)
            parameters.set("api_key", apiKey);
        if (collectionId != null)
            parameters.set("collection_id", collectionId);
        restTemplate.getForObject(
                buildUri("flickr.collections.getInfo", parameters),
                Object.class);
    }

    @Override
    public Collections getTree(String apiKey, String collectionId, String userId) {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();
        if (apiKey != null)
            parameters.set("api_key", apiKey);
        if (collectionId != null)
            parameters.set("collection_id", collectionId);
        if (userId != null)
            parameters.set("user_id", userId);
        return restTemplate.getForObject(
                buildUri("flickr.collections.getTree", parameters),
                Collections.class);
    }
}