# Spring Boot RESTful API

> This is an example of a Java RESTful API with Spring Boot and MariaDB.
> It exposes REST resources at <http://localhost:8080/api/v1/>.
> See the list of resources in the Swagger UI (<http://localhost:8080/swagger-ui>) or OpenAPI documentation as JSON (<http://localhost:8080/api-docs>).

## Running the Application

### Prerequisites

- Java Development Kit (JDK) 21 or higher
- Apache Maven
- MariaDB

### Steps to Run

1. **Clone the repository:**

    ```bash
    git clone <repository-url>
    cd <repository-directory>
    ```

2. **Configure the database:**

    Update the `application.properties` file in `src/main/resources` with your MariaDB configuration.

3. **Build the application:**

    ```bash
    ./mvnw clean install
    ```

4. **Run the application:**

    ```bash
    ./mvnw spring-boot:run
    ```

    Alternatively, you can run the JAR file:

    ```bash
    java -jar ./target/rest-api.jar
    ```

### Accessing the API

- API Base URL: <http://localhost:8080/api/v1/>
- Swagger UI: <http://localhost:8080/swagger-ui>
- OpenAPI Documentation: <http://localhost:8080/api-docs>

## Documentation

- General references: <https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle>
- Application properties: <https://docs.spring.io/spring-boot/docs/current/reference/html/appendix-application-properties.html>

## Troubleshooting

If there is no table `todo_assignee` reported, stop the application and re-run the `spring-boot:run` task again.

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
# --> http://localhost:8080/api/v1/
java -jar ./target/rest-api.jar

# for development: build and run in live-reload mode (rebuild on save)
# --> http://localhost:8080/api/v1/
./mvnw spring-boot:run
```
After you run the created JAR file, you should be able to see the implemented resources in your browser (http://localhost:8080/api/v1/).


# Frontend

This project is a single-page application built with [Vue.js v3.]. The application can be accessed at <http://localhost:5173> during development.

## Prerequisites

1. Install [Node.js](https://nodejs.org/en/).
   - Make sure the root folder of the Node.js installation is added to your system's PATH.
   - Verify by running:
     ```sh
     node -v
     npm -v
     ```

2. Ensure your terminal or command prompt is set up for executing npm commands.

## Development Setup

You can use any text editor or IDE of your choice. Here are some recommendations:

- [VSCode](https://code.visualstudio.com/) with extensions:
  - [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (disable Vetur if using Vue.js).
  - [TypeScript Vue Plugin (Volar)](https://marketplace.visualstudio.com/items?itemName=Vue.vscode-typescript-vue-plugin).
- [WebStorm](https://www.jetbrains.com/webstorm/) (includes required plugins).
- Any text editor + CLI tools.

## Project Setup

To set up and run the project locally:

1. Clone the repository and navigate to the project folder.

2. Install the required dependencies:
   ```sh
   npm install
   ```

3. Start the development server:
   ```sh
   npm run dev
   ```

4. Access the application in your browser at:
   <http://localhost:5173>

## Building for Production

To prepare the application for production:

1. Compile and minify the code:
   ```sh
   npm run build
   ```

2. The production-ready files will be available in the `dist/` folder.

## Linting

To ensure code quality, run the linter:
```sh
npm run lint
```

## Notes

- If you encounter any issues during setup or development, ensure that all dependencies are correctly installed and your Node.js version matches the requirements specified in the `package.json` file.




