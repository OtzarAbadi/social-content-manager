package com.otzar.sscm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

@SpringBootApplication(exclude = HibernateJpaAutoConfiguration.class)
public class SocialContentManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocialContentManagerApplication.class, args);
    }

}
