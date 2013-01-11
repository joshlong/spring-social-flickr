package org.springframework.social.importer;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.Lifecycle;
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
public class FlickrImporter implements Lifecycle {

    private volatile JobLauncher jobLauncher;

    private volatile Job importFlickrPhotosJob;

    private volatile Map<File, JobExecution> mapOfFilesToRunningJobs = new ConcurrentHashMap<File, JobExecution>();

    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;
    private volatile TaskScheduler scheduler;

    public FlickrImporter(DataSource dataSource, Job importFlickrPhotosJob, JobLauncher jobLauncher, TaskScheduler s) {
        this.importFlickrPhotosJob = importFlickrPhotosJob;
        this.jobLauncher = jobLauncher;
        this.dataSource = dataSource;
        this.scheduler = s;
        this.jdbcTemplate = new JdbcTemplate(this.dataSource);

    }

    protected void doImportPhotosToDirectory(File file, JobParameters jp) throws Throwable {
        Assert.notNull(file, "you must provide a non-null File object.");
        Assert.isTrue(file.exists() || file.mkdirs(), "the " + file.getAbsolutePath() + " must exist.");
        Assert.isTrue(file.canWrite(), "we must be able to write to " + file.getAbsolutePath() + ".");
        JobExecution jobExecution = jobLauncher.run(this.importFlickrPhotosJob, jp);
        this.mapOfFilesToRunningJobs.put(file, jobExecution);
    }

    /**
     * used in a web environment when the FlickrTemplate will be
     * injected in some other way, not relying on the
     * runtime parameters
     *
     * @param out output
     * @throws Throwable
     */
    public void importPhotosToDirectory(String userId, File out) throws Throwable {
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
    public void importPhotosToDirectory(String userId, String at, String atSecret, String consumerKey, String consumerSecret, File file) throws Throwable {
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
        String q = "select   (select pp.url from  photos pp where pp.is_primary = true and pp.album_id= pa.album_id) as primary_url, ( select  count(*) from photos p where p.album_id = pa.album_id) as photos_imported , \n" +
                "( select  count(*) FRom photos p where downloaded is not null and p.album_id = pa.album_id) as photos_downloaded , pa.* from photo_albums  pa  " +
                " where user_id = ?";
        return jdbcTemplate.query(q, new RowMapper<PhotoSet>() {
            @Override
            public PhotoSet mapRow(ResultSet rs, int rowNum) throws SQLException {
                PhotoSet photoSet = new PhotoSet(
                        rs.getInt("count_videos"),
                        rs.getInt("count_photos"),
                        rs.getString("primary_url"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("album_id"), rs.getString("user_id")
                );
                photoSet.setPhotosImported(rs.getInt("photos_imported"));
                photoSet.setPhotosDownloaded(rs.getInt("photos_downloaded"));
                return photoSet;
            }
        }, userId);
    }

    /**
     * tests to see if any jobs can be removed and, if so, does.
     */
    public static class JobCleanupRunnable implements Runnable {

        private volatile Map<File, JobExecution> executionMap;

        public JobCleanupRunnable(Map<File, JobExecution> ex) {
            this.executionMap = ex;
        }

        @Override
        public void run() {
            for (Map.Entry<File, JobExecution> entry : executionMap.entrySet())
                if (!entry.getValue().isRunning())
                    executionMap.remove(entry.getKey());
        }
    }


    @Override
    public void start() {
        // we don't have a particular obligation to do anything here..
        if (null == this.scheduler) {
            this.scheduler = new ConcurrentTaskScheduler();
        }
        this.scheduler.scheduleAtFixedRate(new JobCleanupRunnable(this.mapOfFilesToRunningJobs), 1000);
    }

    @Override
    public void stop() {
        for (JobExecution jobExecution : this.mapOfFilesToRunningJobs.values()) {
            jobExecution.stop();
        }
    }

    @Override
    public boolean isRunning() {
        return this.mapOfFilesToRunningJobs.size() > 0;
    }
}
