package com.otzar.sscm.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;

import static com.otzar.sscm.utils.Constants.SCHEMA;

@Configuration
@Profile("test")
public class TestConfig {

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + SCHEMA + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        return dataSource;
    }

    @Bean
    public LocalSessionFactoryBean sessionFactory() {
        LocalSessionFactoryBean sessionFactoryBean = new LocalSessionFactoryBean();
        sessionFactoryBean.setDataSource(dataSource());
        Properties hibernateProperties = new Properties();
        hibernateProperties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        hibernateProperties.put("hibernate.hbm2ddl.auto", "update");
        hibernateProperties.put("hibernate.jdbc.batch_size", 50);
        hibernateProperties.put("hibernate.connection.characterEncoding", "utf8");
        sessionFactoryBean.setHibernateProperties(hibernateProperties);
        sessionFactoryBean.setMappingResources("objects.hbm.xml");
        return sessionFactoryBean;
    }

    @Bean
    public HibernateTransactionManager transactionManager() {
        HibernateTransactionManager transactionManager = new HibernateTransactionManager();
        transactionManager.setSessionFactory(sessionFactory().getObject());
        return transactionManager;
    }

    @Bean
    public CommandLineRunner seedTestData(JdbcTemplate jdbcTemplate) {
        return args -> {
            Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);

            if (userCount != null && userCount > 0) {
                return;
            }

            jdbcTemplate.update(
                    "INSERT INTO users (full_name, email, username, password, role, token) VALUES (?, ?, ?, ?, ?, ?)",
                    "Otzar Admin",
                    "admin@sscm.com",
                    "admin",
                    "123456",
                    "ADMIN",
                    ""
            );
            jdbcTemplate.update(
                    "INSERT INTO users (full_name, email, username, password, role, token) VALUES (?, ?, ?, ?, ?, ?)",
                    "Stav Beauty Studio",
                    "client1@sscm.com",
                    "client1",
                    "123456",
                    "CLIENT",
                    ""
            );
            jdbcTemplate.update(
                    "INSERT INTO users (full_name, email, username, password, role, token) VALUES (?, ?, ?, ?, ?, ?)",
                    "Hodaya Nails",
                    "client2@sscm.com",
                    "client2",
                    "123456",
                    "CLIENT",
                    ""
            );
        };
    }
}
