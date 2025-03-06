This project builds upon the Spring Boot application provided by Auth0 for authentication.
Functionalities:
1. Adding authentication with Auth0 using the [Okta Spring Boot Starter](https://github.com/okta/okta-spring-boot) to a Spring Boot MVC application
2. Accessing profile information of the authenticated user
3. Saving user profile information to a database
4. Redis for Session Caching
   
## Requirements

- Java 17

## Configuration

### Auth0 Dashboard
1. On the [Auth0 Dashboard](https://manage.auth0.com/#/clients) create a new Application of type **Regular Web Application**.
1. On the **Settings** tab of your application, add the URL `http://localhost:3000/login/oauth2/code/auth0` to the **Allowed Callback URLs** field.
1. On the **Settings** tab of your application, add the URL `http://localhost:3000/` to the **Allowed Logout URLs** field.
1. Save the changes to your application settings. Don't close this page; you'll need some of the settings when configuring the application below.

### Application configuration

Set the application values in the `src/main/resources/application.properties` file to the values of your Auth0 application.

```properties
client-id: {YOUR-CLIENT-ID}
client-secret: {YOUR-CLIENT-SECRET}
issuer-uri: https://{YOUR-DOMAIN}/
```

