package org.springframework.social.importer.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Driver;


/**
 *
 * Separates out the common
 * @author Josh Long
 */
@Configuration
@EnableBatchProcessing
public class BatchInfrastructureConfiguration {

     @Bean
    public TaskScheduler taskScheduler() {
        return new ConcurrentTaskScheduler();
    }

 /*   @Bean
    public TaskExecutor taskExecutor() {
        return new ConcurrentTaskExecutor();
    }
*/
    @Bean
    public PlatformTransactionManager transactionManager(DataSource ds) {
        return new DataSourceTransactionManager(ds);
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
}
