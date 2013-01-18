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
package org.springframework.social.importer.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.ItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.social.flickr.api.Flickr;
import org.springframework.social.importer.FlickrImporter;
import org.springframework.social.importer.batch.DelegatingPhotoSetPhotoItemReader;
import org.springframework.social.importer.batch.PhotoDownloadingItemProcessor;
import org.springframework.social.importer.batch.PhotoSetItemReader;
import org.springframework.social.importer.model.Photo;
import org.springframework.social.importer.model.PhotoSet;

import javax.sql.DataSource;
import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * @author Josh Long
 */
@Configuration
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@Import(BatchInfrastructureConfiguration.class)
public class BatchImporterConfiguration {

    @Bean
    public FlickrImporter importer(DataSource dataSource,
                                   @Qualifier("flickrImportJob") Job importFlickrPhotosJob,
                                   JobLauncher jobLauncher,
                                   TaskScheduler[] taskScheduler) {
        return new FlickrImporter(dataSource, importFlickrPhotosJob, jobLauncher, taskScheduler[0]);
    }

    @Bean(name = "flickrImportJob")
    public Job flickrImportJob(
            JobBuilderFactory jobs,
            @Qualifier("step1") Step s1,
            @Qualifier("step2") Step s2,
            @Qualifier("step3") Step s3
    ) {
        return jobs.get("flickrImportJob")
                .flow(s1)
                .next(s2)
                .next(s3)
                .end()
                .build();
    }


    @Bean(name = "step1")
    public Step step1(StepBuilderFactory stepBuilderFactory,
                      @Qualifier("photoAlbumItemReader") ItemReader<PhotoSet> ir,
                      @Qualifier("photoAlbumItemWriter") ItemWriter<PhotoSet> iw
    ) {
        return stepBuilderFactory.get("step1")
                .<PhotoSet, PhotoSet>chunk(10)
                .reader(ir)
                .writer(iw)
                .build();
    }

    @Bean(name = "step2")
    public Step step2(StepBuilderFactory stepBuilderFactory,
                      @Qualifier("photoSetJdbcCursorItemReader") ItemStream theDelegate,
                      @Qualifier("delegatingFlickrPhotoAlbumPhotoItemReader") ItemReader<Photo> delegatingPhotoSetPhotoItemReader,
                      @Qualifier("photoDetailItemWriter") ItemWriter<Photo> itemWriter) {
        return stepBuilderFactory.get("step2")
                .<Photo, Photo>chunk(10)
                .reader(delegatingPhotoSetPhotoItemReader)
                .writer(itemWriter)
                .stream(theDelegate)
                .build();
    }

    @Bean(name = "step3")
    public Step step3(StepBuilderFactory stepBuilderFactory,
                      @Qualifier("photoDetailItemReader") ItemReader<Photo> ir,
                      @Qualifier("photoDownloadingItemProcessor") ItemProcessor<Photo, Photo> ip,
                      @Qualifier("photoDownloadedAcknowledgingItemWriter") ItemWriter<Photo> iw) {
        return stepBuilderFactory.get("step3")
                .<Photo, Photo>chunk(10)
                .reader(ir)
                .processor(ip)
                .writer(iw)
                .build();
    }


    // ===================================================
    // STEP #1
    // ===================================================
    @Bean(name = "photoAlbumItemReader")
    @StepScope
    public PhotoSetItemReader photoAlbumItemReader(Flickr flickr) throws Throwable {
        return new PhotoSetItemReader(flickr);
    }

    @Bean(name = "photoAlbumItemWriter")
    public JdbcBatchItemWriter<PhotoSet> writer(DataSource ds) {
        String upsertPhotoAlbumsSql =
                "with new_values (title, user_id, description,  album_id, url, count_photos, count_videos) as ( " +
                        " values (  :t, :ui, :d, :a, :u, :cp, :cv )  ), " +
                        "upsert as " +
                        "( update photo_albums p set  count_photos=nv.count_photos,count_videos=nv.count_videos, title = nv.title, user_id = nv.user_id, description = nv.description, album_id = nv.album_id, url = nv.url " +
                        "    FROM new_values nv WHERE p.album_id = nv.album_id RETURNING p.*  ) " +
                        "INSERT INTO photo_albums (title, user_id, description,  album_id, url, count_photos, count_videos)  " +
                        "SELECT nv.* FROM new_values nv " +
                        "WHERE NOT EXISTS (SELECT 1 FROM upsert up WHERE up.album_id = nv.album_id )";

        JdbcBatchItemWriter<PhotoSet> jdbcBatchItemWriter = new JdbcBatchItemWriter<PhotoSet>();
        jdbcBatchItemWriter.setSql(upsertPhotoAlbumsSql);
        jdbcBatchItemWriter.setDataSource(ds);
        jdbcBatchItemWriter.setAssertUpdates(false); // were doing upserts, so these writes may never take effect
        jdbcBatchItemWriter.setItemSqlParameterSourceProvider(new ItemSqlParameterSourceProvider<PhotoSet>() {
            @Override
            public SqlParameterSource createSqlParameterSource(PhotoSet item) {
                return new MapSqlParameterSource()
                        .addValue("t", item.getTitle())
                        .addValue("d", item.getDescription())
                        .addValue("ui", item.getUserId())
                        .addValue("a", item.getId())
                        .addValue("cv", item.getCountOfVideos(), Types.INTEGER)
                        .addValue("cp", item.getCountOfPhotos(), Types.INTEGER)
                        .addValue("u", item.getPrimaryImageUrl());
            }
        });
        return jdbcBatchItemWriter;
    }

    // ===================================================
    // STEP #2
    // ===================================================

    @Bean(name = "photoSetJdbcCursorItemReader")
    public JdbcCursorItemReader<PhotoSet> readPhotoAlbumsFromDatabaseItemReader(DataSource dataSource) {

        JdbcCursorItemReader<PhotoSet> photoSetJdbcCursorItemReader = new JdbcCursorItemReader<PhotoSet>();
        photoSetJdbcCursorItemReader.setRowMapper(new RowMapper<PhotoSet>() {
            @Override
            public PhotoSet mapRow(ResultSet resultSet, int i) throws SQLException {
                return new PhotoSet(
                        resultSet.getInt("count_videos"),
                        resultSet.getInt("count_photos"),
                        null,
                        resultSet.getString("title"),
                        resultSet.getString("description"),
                        resultSet.getString("album_id"),
                        resultSet.getString("user_id"));
            }
        });
        photoSetJdbcCursorItemReader.setSql("select * from photo_albums");
        photoSetJdbcCursorItemReader.setDataSource(dataSource);
        return photoSetJdbcCursorItemReader;

    }

    @Bean(name = "delegatingFlickrPhotoAlbumPhotoItemReader")
    @StepScope
    public DelegatingPhotoSetPhotoItemReader delegatingFlickrPhotoAlbumPhotoItemReader(@Qualifier("photoSetJdbcCursorItemReader") JdbcCursorItemReader<PhotoSet> photoSetJdbcCursorItemReader, Flickr flickr) {
        return new DelegatingPhotoSetPhotoItemReader(flickr, photoSetJdbcCursorItemReader);
    }

    @Bean(name = "photoDetailItemWriter")
    public JdbcBatchItemWriter<Photo> photoDetailItemWriter(DataSource ds) {
        String upsertSql = " with new_values ( is_primary,photo_id,thumb_url,url,comments,album_id ) as ( " +
                " values (  :is_primary,:photo_id,:thumb_url,:url,:comments,:album_id )  )," +
                "  upsert as ( update photos p set    is_primary = nv.is_primary,photo_id = nv.photo_id," +
                "thumb_url = nv.thumb_url,url = nv.url,comments = nv.comments,album_id = nv.album_id  FROM " +
                "new_values nv WHERE p.photo_id = nv.photo_id RETURNING p.*  )  insert into photos( is_primary," +
                "photo_id,thumb_url,url,comments,album_id )  SELECT  nv.* FROM new_values nv WHERE NOT EXISTS " +
                "(SELECT 1 FROM upsert up WHERE up.photo_id = nv.photo_id ) ";
        JdbcBatchItemWriter<Photo> photoDetailJdbcBatchItemWriter = new JdbcBatchItemWriter<Photo>();
        photoDetailJdbcBatchItemWriter.setDataSource(ds);
        photoDetailJdbcBatchItemWriter.setAssertUpdates(false);
        photoDetailJdbcBatchItemWriter.setSql(upsertSql);
        photoDetailJdbcBatchItemWriter.setItemSqlParameterSourceProvider(new ItemSqlParameterSourceProvider<Photo>() {
            @Override
            public SqlParameterSource createSqlParameterSource(Photo item) {
                return new MapSqlParameterSource()
                        .addValue("photo_id", item.getId())
                        .addValue("is_primary", item.isPrimary())
                        .addValue("thumb_url", item.getThumbnailUrl())
                        .addValue("url", item.getUrl())
                        .addValue("comments", item.getComments())
                        .addValue("album_id", item.getAlbumId());

            }
        });
        return photoDetailJdbcBatchItemWriter;
    }

    // ===================================================
    // STEP #3
    // ===================================================

    @Bean(name = "photoDetailItemReader")
    @StepScope
    public JdbcCursorItemReader<Photo> photoDetailItemReader(@Value("#{jobParameters['userId']}") final String userId, DataSource dataSource) {
        JdbcCursorItemReader<Photo> photoSetJdbcCursorItemReader = new JdbcCursorItemReader<Photo>();
        photoSetJdbcCursorItemReader.setSql("select * from photos p , photo_albums pa where p.album_id = pa.album_id and p.downloaded is null and user_id = ?");
        photoSetJdbcCursorItemReader.setDataSource(dataSource);
        photoSetJdbcCursorItemReader.setRowMapper(new RowMapper<Photo>() {
            @Override
            public Photo mapRow(ResultSet resultSet, int i) throws SQLException {
                return new Photo(
                        resultSet.getString("photo_id"),
                        resultSet.getBoolean("is_primary"),
                        resultSet.getString("url"), resultSet.getString("thumb_url"),
                        resultSet.getString("title"),
                        resultSet.getString("comments"),
                        resultSet.getString("album_id")
                );
            }
        });
        photoSetJdbcCursorItemReader.setPreparedStatementSetter(new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setString(1, userId);
            }
        });
        return photoSetJdbcCursorItemReader;
    }

    @StepScope
    @Bean(name = "photoDownloadingItemProcessor")
    public ItemProcessor<Photo, Photo> photoDownloadingItemProcessor(@Value("#{jobParameters['output']}") String outputPath, Flickr flickrTemplate) {
        return new PhotoDownloadingItemProcessor(flickrTemplate, new File(outputPath));
    }

    @StepScope
    @Bean(name = "photoDownloadedAcknowledgingItemWriter")
    public ItemWriter<Photo> photoDownloadingItemWriter(DataSource dataSource) {
        JdbcBatchItemWriter<Photo> photoJdbcBatchItemWriter = new JdbcBatchItemWriter<Photo>();
        photoJdbcBatchItemWriter.setDataSource(dataSource);
        photoJdbcBatchItemWriter.setSql("UPDATE photos set downloaded = NOW() where photo_id = :pid ");
        photoJdbcBatchItemWriter.setAssertUpdates(true);
        photoJdbcBatchItemWriter.setItemSqlParameterSourceProvider(new ItemSqlParameterSourceProvider<Photo>() {
            @Override
            public SqlParameterSource createSqlParameterSource(Photo photo) {
                return new MapSqlParameterSource()
                        .addValue("pid", photo.getId());
            }
        });
        return photoJdbcBatchItemWriter;

    }


}