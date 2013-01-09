package org.springframework.social.flickr.api;

import org.springframework.social.ApiBinding;

// TODO: Auto-generated Javadoc

/**
 * The Interface Flickr.
 *
 * @author HemantS
 */
public interface Flickr extends ApiBinding {

    PeopleOperations peopleOperations();

    PhotoOperations photoOperations();

    PhotoCommentOperations photoCommentOperations();

    PhotoLicenseOperations photoLicenseOperations();

    PhotoNoteOperations photoNoteOperations();

    PhotosetOperations photosetOperations();

    GalleriesOperations galleriesOperations();

    FavoritesOperations favoritesOperations();

    ActivityOperations activityOperations();

    BlogsOperations blogsOperations();

    CommonsOperations commonsOperations();

    GroupsOperations groupsOperations();
}
