package org.springframework.social.importer.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.social.importer.FlickrImporter;


@Configuration
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@ImportResource("/batch.xml")
@Import(BatchConfiguration.class)
public class ImporterConfiguration {

    @Bean
    public FlickrImporter importer(
            @Qualifier("flickrImportJob")  Job importFlickrPhotosJob,
            JobLauncher jobLauncher,
            TaskScheduler[] taskScheduler) {
        return new FlickrImporter(importFlickrPhotosJob, jobLauncher, taskScheduler[0]);
    }

}