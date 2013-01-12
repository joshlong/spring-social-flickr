package org.springframework.social.importer.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.item.database.ItemPreparedStatementSetter;
import org.springframework.batch.item.database.ItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.social.flickr.api.Flickr;
import org.springframework.social.importer.FlickrImporter;
import org.springframework.social.importer.batch.DelegatingPhotoSetPhotoItemReader;
import org.springframework.social.importer.batch.PhotoDownloadingItemProcessor;
import org.springframework.social.importer.batch.PhotoSetItemReader;
import org.springframework.social.importer.model.Photo;
import org.springframework.social.importer.model.PhotoSet;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;


@Configuration
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@ImportResource("classpath:/batch.xml")
public class BatchImporterConfiguration {

    @Bean
    public FlickrImporter importer(DataSource dataSource,
                                   @Qualifier("flickrImportJob") Job importFlickrPhotosJob,
                                   JobLauncher jobLauncher,
                                   TaskScheduler[] taskScheduler) {
        return new FlickrImporter(dataSource, importFlickrPhotosJob, jobLauncher, taskScheduler[0]);
    }

    @Bean
    public TaskScheduler taskScheduler() {
        return new ConcurrentTaskScheduler();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        return new ConcurrentTaskExecutor();
    }

    @Bean
    public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor() throws Exception {
        JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor = new JobRegistryBeanPostProcessor();
        jobRegistryBeanPostProcessor.setJobRegistry(this.mapJobRegistry());
        return jobRegistryBeanPostProcessor;
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }

    @Bean
    public MapJobRegistry mapJobRegistry() throws Exception {
        return new MapJobRegistry();
    }

    @Bean
    public DataSource dataSource(Environment environment) {

        String pw = environment.getProperty("dataSource.password"),
                user = environment.getProperty("dataSource.user"),
                url = environment.getProperty("dataSource.url");

        Class<Driver> classOfDs = environment.getPropertyAsClass("dataSource.driverClassName", Driver.class);

        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setPassword(pw);
        dataSource.setUrl(url);
        dataSource.setUsername(user);
        dataSource.setDriverClass(classOfDs);

        return dataSource;
    }

    @Bean
    public JobRepositoryFactoryBean jobRepository(DataSource ds, PlatformTransactionManager tx) throws Exception {
        JobRepositoryFactoryBean jobRepositoryFactoryBean = new JobRepositoryFactoryBean();
        jobRepositoryFactoryBean.setDataSource(ds);
        jobRepositoryFactoryBean.setTransactionManager(tx);
        return jobRepositoryFactoryBean;
    }

    @Bean
    public SimpleJobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
        SimpleJobLauncher simpleJobLauncher = new SimpleJobLauncher();
        simpleJobLauncher.setJobRepository(jobRepository);
        return simpleJobLauncher;
    }


    // ===================================================
    // STEP #1
    // ===================================================
    @Bean(name = "photoAlbumItemReader")
    @Scope(value = "step")
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
    @Scope(value = "step")
    public DelegatingPhotoSetPhotoItemReader delegatingFlickrPhotoAlbumPhotoItemReader(@Qualifier("photoSetJdbcCursorItemReader") JdbcCursorItemReader<PhotoSet> photoSetJdbcCursorItemReader, Flickr flickr) {
        return new DelegatingPhotoSetPhotoItemReader(flickr, photoSetJdbcCursorItemReader);
    }

    @Bean(name = "photoDetailItemWriter")
    public JdbcBatchItemWriter<Photo> photoDetailItemWriter(DataSource ds) {

        String upsertSql = " with new_values ( is_primary,photo_id,thumb_url,url,comments,album_id ) as (  values (  :is_primary,:photo_id,:thumb_url,:url,:comments,:album_id )  ),  upsert as ( update photos p set    is_primary = nv.is_primary,photo_id = nv.photo_id,thumb_url = nv.thumb_url,url = nv.url,comments = nv.comments,album_id = nv.album_id  FROM new_values nv WHERE p.photo_id = nv.photo_id RETURNING p.*  )  insert into photos( is_primary,photo_id,thumb_url,url,comments,album_id )  SELECT  nv.* FROM new_values nv WHERE NOT EXISTS (SELECT 1 FROM upsert up WHERE up.photo_id = nv.photo_id ) ";
        JdbcBatchItemWriter<Photo> photoDetailJdbcBatchItemWriter = new JdbcBatchItemWriter<Photo>();
        photoDetailJdbcBatchItemWriter.setDataSource(ds);
        photoDetailJdbcBatchItemWriter.setAssertUpdates(false);
        photoDetailJdbcBatchItemWriter.setSql(upsertSql);
        photoDetailJdbcBatchItemWriter.setItemSqlParameterSourceProvider(new ItemSqlParameterSourceProvider<Photo>() {
            @Override
            public SqlParameterSource createSqlParameterSource(Photo item) {
                Map<String, Object> params = new HashMap<String, Object>();
                params.put("photo_id", item.getId());
                params.put("is_primary", item.isPrimary());
                params.put("thumb_url", item.getThumbnailUrl());
                params.put("url", item.getUrl());
                params.put("comments", item.getComments());
                params.put("album_id", item.getAlbumId());
                return new MapSqlParameterSource(params);
            }
        });
        return photoDetailJdbcBatchItemWriter;
    }

    @Bean(name = "photoDetailItemReader")
    @Scope("step")
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

    @Scope("step")
    @Bean(name = "photoDownloadingItemProcessor")
    public PhotoDownloadingItemProcessor photoDownloadingItemProcessor(@Value("#{jobParameters['output']}") String outputPath, Flickr flickrTemplate) {
        return new PhotoDownloadingItemProcessor(flickrTemplate, new File(outputPath));
    }

    @Scope("step")
    @Bean(name = "photoDownloadedAcknowledgingItemWriter")
    public JdbcBatchItemWriter photoDownloadingItemWriter(DataSource dataSource) {

        JdbcBatchItemWriter<Photo> photoJdbcBatchItemWriter = new JdbcBatchItemWriter<Photo>();
        photoJdbcBatchItemWriter.setDataSource(dataSource);
        photoJdbcBatchItemWriter.setSql("UPDATE photos set downloaded = NOW() where photo_id = ? ");
        photoJdbcBatchItemWriter.setAssertUpdates(true);
        photoJdbcBatchItemWriter.setItemPreparedStatementSetter(new ItemPreparedStatementSetter<Photo>() {
            @Override
            public void setValues(Photo item, PreparedStatement ps) throws SQLException {
                ps.setString(1, item.getId());
            }
        });
        return photoJdbcBatchItemWriter;

    }


}