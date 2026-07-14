# Backend

The default runtime profile is `production`, configured in `src/main/resources/application.properties`.
With that profile active, Spring Boot also loads `src/main/resources/application-production.properties`.

## Local MySQL

The application expects a local MySQL database named `social_content_manager` by default:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/social_content_manager?useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

If your local MySQL `root` user has a password, set it with `SPRING_DATASOURCE_PASSWORD`
or update `spring.datasource.password` in `application-production.properties`.
The `YOUR_PASSWORD` value in `application-example.properties` is only a placeholder.

To recreate and seed the local database, run:

```sql
SOURCE database/init.sql;
```

You can also execute `database/init.sql` with any MySQL client from the `backend` directory.
