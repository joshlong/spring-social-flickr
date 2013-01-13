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

import org.apache.commons.lang.SystemUtils;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.social.flickr.HomeController;
import org.springframework.util.Assert;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Configures the web application
 *
 * @author Josh Long
 */
public class AnnotationConfigWebApplicationContextInitializer implements ApplicationContextInitializer<AnnotationConfigWebApplicationContext> {

    @Override
    public void initialize(AnnotationConfigWebApplicationContext applicationContext) {
        registerPropertiesForFlickrConnection(applicationContext);
        applicationContext.scan(HomeController.class.getPackage().getName()); // scans everything in this directory
        applicationContext.refresh();
        applicationContext.start();
    }

    /**
     * I do this because I don't want to constantly specify the properties on the command line and I don't want
     * to check in the properties on github in a public repository since I'm working with my own photos.
     */
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
