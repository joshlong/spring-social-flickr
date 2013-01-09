package org.springframework.social.importer.config;

import java.sql.Driver;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.social.importer.FlickrImporter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@ImportResource("/batch.xml")
public class ImporterConfiguration {

	@Bean
	public FlickrImporter importer(@Qualifier("flickrImportJob")
	Job importFlickrPhotosJob, JobLauncher jobLauncher, TaskScheduler[] taskScheduler) {
		return new FlickrImporter(importFlickrPhotosJob, jobLauncher, taskScheduler[0]);
	}

	@Bean
	public TaskExecutor taskExecutor() {
		return new ConcurrentTaskExecutor();
	}

	@Bean
	public TaskScheduler taskScheduler() {
		return new ConcurrentTaskScheduler();
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
	public DataSourceInitializer initializer(DataSource dataSource) {
		DataSourceInitializer initializer = new DataSourceInitializer();
		initializer.setDataSource(dataSource);
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		populator.addScript(new ClassPathResource("org/springframework/batch/core/schema-h2.sql"));
		initializer.setDatabasePopulator(populator);
		populator.setContinueOnError(true);
		return initializer;
	}

	@Bean
	public DataSource dataSource(Environment environment) {
		String pw = environment.getProperty("dataSource.password"), user = environment.getProperty("dataSource.user"), url = environment
				.getProperty("dataSource.url");
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
	public SimpleJobLauncher jobLauncher(TaskExecutor[] te, JobRepository jobRepository) throws Exception {
		SimpleJobLauncher simpleJobLauncher = new SimpleJobLauncher();
		simpleJobLauncher.setJobRepository(jobRepository);
		simpleJobLauncher.setTaskExecutor(te[0]);
		return simpleJobLauncher;
	}

	public static class CommonObjectThatNeedsJobParameters {
		private Date date;

		protected CommonObjectThatNeedsJobParameters() {
		}

		public CommonObjectThatNeedsJobParameters(Date da) {
			this.date = da;
		}

		public Date getDate() {
			return this.date;
		}
	}

	public static class MyItemReader implements ItemReader<String> {
		private CommonObjectThatNeedsJobParameters sharedReference;

		public MyItemReader(CommonObjectThatNeedsJobParameters commonObjectThatNeedsJobParameters) {
			this.sharedReference = commonObjectThatNeedsJobParameters;
			System.out.println("the date is ");
		}

		@Override
		public String read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
			return this.sharedReference.getDate().toString();
		}
	}

	public static class MyItemWriter implements ItemWriter<String> {
		@Override
		public void write(List<? extends String> items) throws Exception {
			for (String incoming : items)
				System.out.println("Received item ('" + incoming + "')");
		}
	}

	@Bean
	@Scope("step")
	public CommonObjectThatNeedsJobParameters commonObjectThatNeedsJobParameters(@Value("#{jobParameters['when']}")
	Date when) {
		return new CommonObjectThatNeedsJobParameters(when);
	}

	@Bean
	@Scope("step")
	public ItemReader<String> itemReader(CommonObjectThatNeedsJobParameters commonObjectThatNeedsJobParameters) {
		return new MyItemReader(commonObjectThatNeedsJobParameters);
	}

	@Bean
	public ItemWriter<String> itemWriter() {
		return new MyItemWriter();
	}

}