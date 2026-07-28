package com.otzar.sscm.config;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

@Configuration
@Profile({"default", "production"})
public class AppConfig {

    private final Environment env;

    public AppConfig(Environment env) {
        this.env = env;
    }

    @Bean
    public DataSource dataSource() throws Exception {
        String driverClassName = env.getRequiredProperty("spring.datasource.driver-class-name");
        String jdbcUrl = env.getProperty(
                "SPRING_DATASOURCE_URL",
                env.getRequiredProperty("spring.datasource.url"));
        String dbUser = env.getProperty(
                "SPRING_DATASOURCE_USERNAME",
                env.getRequiredProperty("spring.datasource.username"));
        String dbPass = env.getProperty(
                "SPRING_DATASOURCE_PASSWORD",
                env.getProperty("spring.datasource.password", ""));

        Class.forName(driverClassName);
        String schema = extractSchemaName(jdbcUrl);
        if (schema != null && !schema.trim().isEmpty()) {
            try (Connection connection = DriverManager.getConnection(createSchemaUrl(jdbcUrl), dbUser, dbPass);
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE SCHEMA IF NOT EXISTS `" + schema.replace("`", "``") + "`");
            }
        }

        ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setDriverClass(driverClassName);
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUser(dbUser);
        dataSource.setPassword(dbPass);
        dataSource.setMaxPoolSize(20);
        dataSource.setMinPoolSize(5);
        dataSource.setIdleConnectionTestPeriod(3600);
        dataSource.setTestConnectionOnCheckin(true);
        return dataSource;
    }

    private String createSchemaUrl(String jdbcUrl) {
        int queryStart = jdbcUrl.indexOf('?');
        String query = queryStart >= 0 ? jdbcUrl.substring(queryStart) : "";
        String withoutQuery = queryStart >= 0 ? jdbcUrl.substring(0, queryStart) : jdbcUrl;
        int schemaSlash = withoutQuery.indexOf('/', "jdbc:mysql://".length());

        if (schemaSlash < 0) {
            return withoutQuery + "/" + query;
        }

        return withoutQuery.substring(0, schemaSlash + 1) + query;
    }

    private String extractSchemaName(String jdbcUrl) {
        int queryStart = jdbcUrl.indexOf('?');
        String withoutQuery = queryStart >= 0 ? jdbcUrl.substring(0, queryStart) : jdbcUrl;
        int schemaSlash = withoutQuery.indexOf('/', "jdbc:mysql://".length());

        if (schemaSlash < 0 || schemaSlash == withoutQuery.length() - 1) {
            return null;
        }

        return withoutQuery.substring(schemaSlash + 1);
    }

    @Bean
    public LocalSessionFactoryBean sessionFactory() throws Exception {
        LocalSessionFactoryBean sessionFactoryBean = new LocalSessionFactoryBean();
        sessionFactoryBean.setDataSource(dataSource());
        Properties hibernateProperties = new Properties();
        hibernateProperties.put("hibernate.dialect", "org.hibernate.dialect.MySQL5InnoDBDialect");
        hibernateProperties.put("hibernate.hbm2ddl.auto", "update");
        hibernateProperties.put("hibernate.jdbc.batch_size", 50);
        hibernateProperties.put("hibernate.connection.characterEncoding", "utf8");
        hibernateProperties.put("hibernate.enable_lazy_load_no_trans", "true");
        sessionFactoryBean.setHibernateProperties(hibernateProperties);
        sessionFactoryBean.setMappingResources("objects.hbm.xml");
        return sessionFactoryBean;
    }

    @Bean
    public HibernateTransactionManager transactionManager() throws Exception {
        HibernateTransactionManager transactionManager = new HibernateTransactionManager();
        transactionManager.setSessionFactory(sessionFactory().getObject());
        return transactionManager;
    }

}
