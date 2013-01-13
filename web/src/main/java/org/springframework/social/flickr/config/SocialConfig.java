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
package org.springframework.social.flickr.config;

import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.connect.jdbc.JdbcUsersConnectionRepository;
import org.springframework.social.connect.support.ConnectionFactoryRegistry;
import org.springframework.social.connect.web.ProviderSignInController;
import org.springframework.social.flickr.api.Flickr;
import org.springframework.social.flickr.connect.FlickrConnectionFactory;
import org.springframework.social.flickr.user.SecurityContext;
import org.springframework.social.flickr.user.SimpleConnectionSignUp;
import org.springframework.social.flickr.user.SimpleSignInAdapter;
import org.springframework.social.flickr.user.User;
import org.springframework.social.importer.config.BatchImporterConfiguration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

@Configuration
@Import(BatchImporterConfiguration.class)
public class SocialConfig {


    @Bean
    public ConnectionFactoryLocator connectionFactoryLocator(Environment environment) {
        ConnectionFactoryRegistry registry = new ConnectionFactoryRegistry();

        String flickrClientId = environment.getProperty("clientId");
        String flickrClientSecret = environment.getProperty("clientSecret");
        registry.addConnectionFactory(new FlickrConnectionFactory(flickrClientId, flickrClientSecret));

        return registry;
    }

    @Bean
    public UsersConnectionRepository usersConnectionRepository(ConnectionFactoryLocator connectionFactoryLocator, DataSource dataSource) {
        JdbcUsersConnectionRepository repository = new JdbcUsersConnectionRepository(dataSource, connectionFactoryLocator, Encryptors.noOpText());
        repository.setConnectionSignUp(new SimpleConnectionSignUp());
        return repository;
    }

    @Bean
    @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.INTERFACES)
    public ConnectionRepository connectionRepository(UsersConnectionRepository usersConnectionRepository, HttpServletRequest request) {
        User user = SecurityContext.getCurrentUser(request);
        return usersConnectionRepository.createConnectionRepository(user.getId());
    }

    @Bean
    @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.INTERFACES)
    public Flickr flickr(ConnectionRepository connectionRepository) {
        return connectionRepository.getPrimaryConnection(Flickr.class).getApi();
    }

    @Bean
    public ProviderSignInController providerSignInController(ConnectionFactoryLocator connectionFactoryLocator, UsersConnectionRepository usersConnectionRepository) {
        ProviderSignInController providerSignInController = new ProviderSignInController(connectionFactoryLocator, usersConnectionRepository, new SimpleSignInAdapter());
        providerSignInController.setPostSignInUrl("/welcome");
        return providerSignInController;
    }

}
