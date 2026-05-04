# Spring Boot RESTful API

> This is an example of a Java RESTful API with Spring Boot and MariaDB.
> It exposes REST resources at <http://localhost:8080/api/v1/>.
> See the list of resources in the Swagger UI (<http://localhost:8080/swagger-ui>) or OpenAPI documentation as JSON (<http://localhost:8080/api-docs>).

## IDE tasks

IntelliJ offers the "Maven" tool window on the right side.

- Development mode: Choose rest-api -> Plugins -> spring-boot -> spring-boot:run to run and have live reload (on save).
- Create `jar`: Choose rest-api -> Plugins -> spring-boot -> spring-boot:re-package to create `target/rest-api.jar`.

## Documentation

- General references: <https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle>
- Application properties: <https://docs.spring.io/spring-boot/docs/current/reference/html/appendix-application-properties.html>

## Troubleshooting

If there is no table `pe2.cats` reported, stop `spring-boot:run` and re-run that task again.

## Advanced Usage

In case you installed the JDK locally, you can use the Windows Terminal / bash to execute commands:

```bash
# build and package executable --> appears in target/rest-api.jar
./mvnw clean install

# execute tests only
./mvnw test

# generate test coverage report (execute tests first) --> appears in target/site/jacoco/index.html
./mvnw jacoco:report

# build and package executable without running tests
./mvnw clean install -DskipTests

# run the created JAR file
# --> http://localhost:8080/api/v1/cats
java -jar ./target/rest-api.jar

# for development: build and run in live-reload mode (rebuild on save)
# --> http://localhost:8080/api/v1/cats
./mvnw spring-boot:run
```

After you run the created JAR file, you should be able to see the implemented resources in your browser (<http://localhost:8080/api/v1/cats>).
