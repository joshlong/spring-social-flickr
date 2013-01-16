package org.springframework.social.importer.config;

import java.sql.Driver;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.social.importer.FlickrImporter;

@Configuration
@EnableBatchProcessing
public class ImporterConfiguration {

	@Inject
	private JobBuilderFactory jobs;

	@Inject
	private StepBuilderFactory steps;

	@Bean
	public Job flickrImportJob(CommonObjectThatNeedsJobParameters commonObjectThatNeedsJobParameters) {
		return jobs.get("flickrImportJob").flow(step1(commonObjectThatNeedsJobParameters)).end().build();
	}

	@Bean
	protected Step step1(CommonObjectThatNeedsJobParameters commonObjectThatNeedsJobParameters) {
		return steps.get("step1").<String, String> chunk(10).reader(itemReader(commonObjectThatNeedsJobParameters))
				.writer(itemWriter()).build();
	}

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
	@Scope(value = "step", proxyMode = ScopedProxyMode.TARGET_CLASS)
	public CommonObjectThatNeedsJobParameters commonObjectThatNeedsJobParameters(@Value("#{jobParameters['when']}")
	Date when) {
		return new CommonObjectThatNeedsJobParameters(when);
	}

	@Bean
	@StepScope
	public ItemReader<String> itemReader(CommonObjectThatNeedsJobParameters commonObjectThatNeedsJobParameters) {
		return new MyItemReader(commonObjectThatNeedsJobParameters);
	}

	@Bean
	public ItemWriter<String> itemWriter() {
		return new MyItemWriter();
	}

}