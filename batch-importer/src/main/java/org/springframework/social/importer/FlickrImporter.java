package org.springframework.social.importer;

import org.apache.log4j.Logger;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.social.importer.model.PhotoSet;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * component that manages Spring Batch jobs to import photos from Flickr and
 * downloads them to a local cache where they can be used.
 *
 * @author Josh Long
 */
public class FlickrImporter {

    private volatile JobLauncher jobLauncher;
    private volatile Job importFlickrPhotosJob;
    private volatile Map<String, JobExecution> mapOfUserIdsToJobs = new ConcurrentHashMap<String, JobExecution>();
    private DataSource dataSource;
    private JdbcTemplate jdbcTemplate;
    private volatile TaskScheduler scheduler;
    private final Logger logger = Logger.getLogger(getClass());

    public FlickrImporter(DataSource dataSource, Job importFlickrPhotosJob, JobLauncher jobLauncher, TaskScheduler s) {
        this.importFlickrPhotosJob = importFlickrPhotosJob;
        this.jobLauncher = jobLauncher;
        this.dataSource = dataSource;
        this.scheduler = s;
        this.jdbcTemplate = new JdbcTemplate(this.dataSource);
        start();
    }

    /**
     * used in a web environment when the FlickrTemplate will be
     * injected in some other way, not relying on the
     * runtime parameters
     *
     * @param out output
     * @throws Throwable
     */
    public void startImport(String userId, File out) throws Throwable {
        JobParameters jp = new JobParametersBuilder()
                .addDate("when", new Date())
                .addString("output", out.getAbsolutePath())
                .addString("userId", userId)
                .toJobParameters();
        doImportPhotosToDirectory(out, jp);
    }

    /**
     * call this to kick off the import job.
     *
     * @param file the directory to which the imported photos should be written
     */
    public void startImport(String userId, String at, String atSecret, String consumerKey, String consumerSecret, File file) throws Throwable {
        JobParameters jp = new JobParametersBuilder()
                .addDate("when", new Date())
                .addString("accessToken", at)
                .addString("userId", userId)
                .addString("accessTokenSecret", atSecret)
                .addString("consumerKey", consumerKey)
                .addString("consumerSecret", consumerSecret)
                .addString("output", file.getAbsolutePath())
                .toJobParameters();
        doImportPhotosToDirectory(file, jp);
    }


    public Collection<PhotoSet> photoSetsImportedForUser(String userId) {
        String q = " select (select pp.thumb_url from  photos pp where pp.is_primary = true and pp.album_id= pa.album_id) as primary_url,  ( select  count(*) from photos p where p.album_id = pa.album_id) as photos_imported ,   ( select  count(*) FRom photos p where downloaded is not null and p.album_id = pa.album_id) as photos_downloaded , pa.* from photo_albums  pa where user_id = ?";
        return jdbcTemplate.query(q, new RowMapper<PhotoSet>() {
            @Override
            public PhotoSet mapRow(ResultSet rs, int rowNum) throws SQLException {
                PhotoSet photoSet = new PhotoSet(
                        rs.getInt("count_videos"),
                        rs.getInt("count_photos"),
                        rs.getString("primary_url"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("album_id"),
                        rs.getString("user_id")
                );
                photoSet.setPhotosImported(rs.getInt("photos_imported"));
                photoSet.setPhotosDownloaded(rs.getInt("photos_downloaded"));
                return photoSet;
            }
        }, userId);
    }

    public void start() {
        // we don't have a particular obligation to do anything here..
        if (null == this.scheduler) {
            this.scheduler = new ConcurrentTaskScheduler();
        }
        this.scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                doPurgeDeadJobs();
            }
        }, 10 * 1000); // 10 s
        logger.info("start() called on " + FlickrImporter.class.getName());
    }


    public void stop() {
        for (JobExecution jobExecution : this.mapOfUserIdsToJobs.values()) {
            jobExecution.stop();
        }
        doPurgeDeadJobs();
        logger.debug("stop() called on " + FlickrImporter.class.getName());
    }


    protected void doPurgeDeadJobs() {
        logger.debug("doPurgeDeadJobs(): there are " + this.mapOfUserIdsToJobs.size() + " jobs.");
        for (Map.Entry<String, JobExecution> entry : mapOfUserIdsToJobs.entrySet())
            if (!entry.getValue().isRunning() || entry.getValue().isStopping())
                mapOfUserIdsToJobs.remove(entry.getKey());
    }

    protected void doImportPhotosToDirectory(File file, JobParameters jp) throws Throwable {
        Assert.notNull(file, "you must provide a non-null File object.");
        Assert.isTrue(file.exists() || file.mkdirs(), "the " + file.getAbsolutePath() + " must exist.");
        Assert.isTrue(file.canWrite(), "we must be able to write to " + file.getAbsolutePath() + ".");
        String userId = jp.getString("userId");

     //   stopImport(userId);

        JobExecution jobExecution = jobLauncher.run(this.importFlickrPhotosJob, jp);

        logger.debug("starting import " + userId);

        this.mapOfUserIdsToJobs.put(userId, jobExecution);
    }
}
