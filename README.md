Welcome to this project, a Spring Boot web application that integrates OAuth2 authentication using Okta/Auth0 and securely manages user sessions with Redis. This project demonstrates how to implement secure user authentication, handle session persistence, and ensure a seamless login/logout experience.
Key Features 🚀
✅ Secure OAuth2 Authentication – Users can log in using Okta/Auth0 with OpenID Connect (OIDC).
✅ Custom Authentication Success Handling – Store user information in the database and cache access tokens in Redis.
✅ Session Management with Redis – Access and refresh tokens are securely stored and retrieved from Redis.
✅ Secure Logout with Redirection – Ensures users are properly logged out and redirected to the appropriate page.
Whether you're building a secure, scalable authentication system or just exploring Spring Security and OAuth2, this project serves as a practical example of modern authentication practices in Java. 

COMPONENTS
A. HomeController.java
The HomeController class provides:
✅ A homepage displaying user profile data from an OIDC (OpenID Connect) provider
✅ An endpoint to retrieve user session data stored in Redis
✅ An endpoint to refresh authentication tokens using Auth0's OAuth 2.0 API
Let's break it down.

1. Displaying User Profile Information
@GetMapping("/")
public String home(Model model, @AuthenticationPrincipal OidcUser principal) {
    if (principal != null) {
        model.addAttribute("profile", principal.getClaims());
    }
    return "index";
}
When a user logs in via OIDC (OpenID Connect), their authentication details (claims) are retrieved and displayed on the homepage. The profile data might include:
    • Name
    • Email
    • Profile Picture
    • Other identity claims

2. Retrieving User Sessions from Redis
@GetMapping("/user/session")
public ResponseEntity<Map<String, String>> getSession(@RequestParam String userId) {
    Map<String, String> session = redisAuthService.getSession(userId);
    if (session != null) {
        return ResponseEntity.ok(session);
    }
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Session not found"));
}
This API fetches session data for a given userId from Redis.
🔹 Example Request (Fixed URL Encoding):
curl -X GET "http://localhost:3000/user/session?userId=google-oauth2%7C115873500067805995920"
🔹 Example JSON Response (Successful):
{
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpX...",
    "refreshToken": "somerandomrefreshtokenvalue"
}
🔹 Example JSON Response (User Not Found):
{
    "error": "Session not found"
}

3. Refreshing Access Tokens via Auth0
@PostMapping("/refresh-token")
public ResponseEntity<Map<String, String>> refreshToken(@RequestParam String userId) {
    Map<String, String> session = redisAuthService.getSession(userId);

    if (session != null && session.containsKey("refreshToken")) {
        String refreshToken = session.get("refreshToken");

        String auth0Url = "https://your-auth0-domain/oauth/token";
        Map<String, String> params = Map.of(
            "grant_type", "refresh_token",
            "client_id", "your-client-id",
            "refresh_token", refreshToken
        );

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.postForEntity(auth0Url, params, Map.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            Map<String, String> tokens = response.getBody();
            redisAuthService.saveSession(userId, tokens.get("access_token"), refreshToken);
            return ResponseEntity.ok(tokens);
        }
    }

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Failed to refresh token"));
}
This endpoint requests a new access token from Auth0 using the refresh token. If successful, the new token is stored back into Redis.
🔹 Example Request:
curl -X POST "http://localhost:3000/refresh-token?userId=google-oauth2%7C115873500067805995920"
🔹 Example Response (Success):
{
    "access_token": "newlyGeneratedAccessToken",
    "refresh_token": "sameOldRefreshToken"
}
🔹 Example Response (Failure):
{
    "error": "Failed to refresh token"
}

B. SecurityConfig.java
The configuration enables:  
✅ **OAuth2 login with Okta/Auth0**.  
✅ **Custom authentication success handling**.  
✅ **Secure logout with redirection to the identity provider**.  

### **Class Overview**  
```java
@Configuration
public class SecurityConfig {
```
- The `@Configuration` annotation tells Spring that this class **provides security configurations**.  
- It **sets up authentication and authorization** using OAuth2.  

---

## **2. Configuring OAuth2 Login & Security**  

### **Injecting Configuration Values for Okta/Auth0**  
```java
@Value("${okta.oauth2.issuer}")
private String issuer;

@Value("${okta.oauth2.client-id}")
private String clientId;
```
- These values **fetch the OAuth2 issuer URL and client ID** from `application.properties`.  

---

### **Defining the Security Filter Chain**  

```java
@Bean
public SecurityFilterChain configure(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/", "/images/**").permitAll()
            .anyRequest().authenticated()
        )
        .oauth2Login(oauth2 -> oauth2.successHandler(successHandler))
        .logout(logout -> logout
            .addLogoutHandler(logoutHandler()));
    return http.build();
}
```

🔹 **What happens here?**  
1️⃣ **Publicly Accessible Routes:**  
   - Requests to `/` and `/images/**` are **allowed without authentication**.  
2️⃣ **Protected Routes:**  
   - Any other request **requires authentication**.  
3️⃣ **OAuth2 Login:**  
   - Configures **OAuth2 login with Okta/Auth0**.  
   - Uses a **custom success handler** (`successHandler`).  
4️⃣ **Logout Handling:**  
   - Calls `logoutHandler()` to **log out the user from Okta/Auth0** and **redirect them back**.  

---

## **3. Handling Logout & Redirecting Users**  

```java
private LogoutHandler logoutHandler() {
    return (request, response, authentication) -> {
        try {
            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            response.sendRedirect(issuer + "v2/logout?client_id=" + clientId + "&returnTo=" + baseUrl);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };
}
```

🔹 **How does this work?**  
- Builds the **logout URL for Okta/Auth0**, including:  
  ✅ `issuer`: The **OAuth2 provider URL** (Okta/Auth0).  
  ✅ `clientId`: The **client ID** registered with Okta/Auth0.  
  ✅ `returnTo`: Redirects the user **back to the application** after logout.  

🔹 **Example Logout URL for Auth0:**  
```
https://your-auth0-domain.com/v2/logout?client_id=YOUR_CLIENT_ID&returnTo=http://localhost:8080
```
✅ Ensures users **log out completely** from the identity provider.  

---

## **4. Custom Authentication Success Handling**  

This class **injects a custom authentication success handler**:  
```java
private final AuthenticationSuccessHandler successHandler;

public SecurityConfig(AuthenticationSuccessHandler successHandler) {
    this.successHandler = successHandler;
}
```
- The `successHandler` processes user information **after successful login**.  
- It can **store user data** in a database, **create sessions**, or **redirect users**.  

---

## **5. Setting Up OAuth2 with Okta or Auth0**  

### **Okta Configuration (`application.properties`)**  
```properties
okta.oauth2.issuer=https://your-okta-domain.com/oauth2/default/
okta.oauth2.client-id=your-client-id
okta.oauth2.client-secret=your-client-secret
```
---

## **6. Testing OAuth2 Login & Logout**  

### **Login Flow**  
1️⃣ **Start your Spring Boot application.**  
2️⃣ **Visit `http://localhost:8080`** and click the login button.  
3️⃣ You will be redirected to **Okta/Auth0’s login page**.  
4️⃣ After logging in, you should be redirected **back to your app**.  

### **Logout Flow**  
1️⃣ Click **Logout** in your application.  
2️⃣ You will be redirected to Okta/Auth0’s logout URL.  
3️⃣ After logging out, you’ll return to your app’s homepage.  

C. OAuth2AuthenticationSuccessHandler.java
This plays a crucial role in handling **OAuth 2.0 login success events**. This component ensures that:  

✅ User data is **retrieved and stored in the database**.  
✅ The **OAuth access token and refresh token** are **stored in Redis** for session management.  
✅ Users are **redirected to the homepage** after successful authentication.  


### **Class Overview**  

```java
@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {
```
This class implements `AuthenticationSuccessHandler`, which Spring Security invokes **after a successful OAuth login**. It handles storing **user details**, **tokens**, and managing **user sessions**.  

### **Injected Dependencies**  

```java
private final UserRepository userRepository;
private final RedisAuthService redisAuthService;
private final OAuth2AuthorizedClientService authorizedClientService;
```
- **`UserRepository`**: Interacts with the database to store user details.  
- **`RedisAuthService`**: Handles **storing OAuth tokens** in Redis.  
- **`OAuth2AuthorizedClientService`**: Retrieves the **OAuth 2.0 access and refresh tokens**.  

---

## **2. Handling User Authentication Data**  

### **Extracting User Information from OAuth Response**  

```java
OAuth2User oAuth2User = oauthToken.getPrincipal();
Map<String, Object> attributes = oAuth2User.getAttributes();
```
Once a user **successfully logs in**, we retrieve **their profile details** from the **OAuth 2.0 provider**.  

#### **Captured User Data**  
```java
String userId = (String) attributes.get("sub");
String email = (String) attributes.get("email");
String name = (String) attributes.get("name");
String picture = (String) attributes.get("picture");
String givenName = (String) attributes.get("given_name");
String nickName = (String) attributes.get("nickname");
String familyName = (String) attributes.get("family_name");
```
This information is extracted from the **OAuth provider’s response** (e.g., **Google, Auth0, GitHub**).  

🔹 **Example JSON Response from Auth0 or Google OAuth**  
```json
{
    "sub": "auth0|1234567890",
    "email": "user@example.com",
    "name": "John Doe",
    "picture": "https://example.com/profile.jpg",
    "given_name": "John",
    "nickname": "Johnny",
    "family_name": "Doe"
}
```

---

## **3. Storing User Information in the Database**  

### **Check if the User Exists**  

```java
Optional<User> existingUser = userRepository.findByAuth0Id(userId);
```
- If the user **already exists**, we simply **update their last login time**.  
- If the user **does not exist**, we **create a new record** in the database.  

### **Saving or Updating the User Record**  

```java
if (existingUser.isEmpty()) {
    User user = new User();
    user.setAuth0Id(userId);
    user.setEmail(email);
    user.setName(name);
    user.setPicture(picture);
    user.setGivenName(givenName);
    user.setNickname(nickName);
    user.setFamilyName(familyName);
    user.setLastLogin(LocalDateTime.now());
    userRepository.save(user);
} else {
    existingUser.ifPresent(user -> {
        user.setLastLogin(LocalDateTime.now()); 
        userRepository.save(user);
    });
}
```

---

## **4. Extracting OAuth Tokens**  

To make authenticated API requests, we **retrieve the OAuth access and refresh tokens**.  

```java
OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
    oauthToken.getAuthorizedClientRegistrationId(),
    oauthToken.getName()
);

String accessToken = (authorizedClient != null) ? authorizedClient.getAccessToken().getTokenValue() : null;
String refreshToken = (authorizedClient != null && authorizedClient.getRefreshToken() != null)
        ? authorizedClient.getRefreshToken().getTokenValue()
        : null;
```
This step ensures that **access and refresh tokens** are securely stored for later use.  

---

## **5. Storing OAuth Tokens in Redis**  

Finally, the **access token and refresh token** are stored in **Redis** for session management.  

```java
redisAuthService.saveSession(userId, accessToken, refreshToken);
```

---

## **6. Redirecting the User to the Homepage**  

After successful login, the user is redirected to the home page.  

```java
response.sendRedirect("/");
```

D. RedisAuthService
The `RedisAuthService` class is responsible for **managing user authentication sessions using Redis** in the Spring Boot application.  

This service provides:  
✅ **Session storage** for OAuth2 access and refresh tokens.  
✅ **Fast retrieval** of session data using Redis.  
✅ **Automatic session expiration** after 15 minutes.  
✅ **Session deletion** when the user logs out.  

---
### **Class Overview**  
```java
@Service
public class RedisAuthService {
```
This class is annotated with `@Service`, meaning it's a **Spring-managed bean** responsible for handling authentication-related operations with Redis.  

### **Injecting RedisTemplate**  
```java
private final RedisTemplate<String, Object> redisTemplate;

@Autowired
public RedisAuthService(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
}
```
- **`RedisTemplate<String, Object>`** → Spring Boot's **Redis utility** for interacting with Redis.  
- **`@Autowired` constructor injection** → Ensures the service gets an instance of `RedisTemplate` from Spring’s application context.  

🔹 **Why use Redis?**  
- **In-memory storage** makes session retrieval **blazing fast**.  
- **Scalability** → Suitable for distributed systems and **stateless authentication**.  
- **TTL (Time-to-Live)** support ensures **automatic expiration** of stale sessions.  

---

## **2. Storing User Sessions in Redis**  

### **Saving a User Session**  
```java
private static final long TOKEN_EXPIRATION = 900; // 15 minutes

public void saveSession(String userId, String accessToken, String refreshToken) {
    Map<String, String> sessionData = new HashMap<>();
    sessionData.put("accessToken", accessToken);
    sessionData.put("refreshToken", refreshToken);
    redisTemplate.opsForValue().set("session:" + userId, sessionData, TOKEN_EXPIRATION, TimeUnit.SECONDS);
}
```
#### **What happens here?**  
1️⃣ **Creates a HashMap** to store access and refresh tokens.  
2️⃣ **Stores the session in Redis** under the key `"session:userId"`.  
3️⃣ **Sets an expiration time of 15 minutes** (900 seconds).  

🔹 **Why expire the token?**  
- Enhances **security** by reducing the risk of stolen tokens.  
- Prevents **session buildup** in Redis, **saving memory**.  
- OAuth2 access tokens are usually short-lived, **matching Redis TTL** ensures consistency.  

---

## **3. Retrieving a User Session**  

```java
public Map<String, String> getSession(String userId) {
    return (Map<String, String>) redisTemplate.opsForValue().get("session:" + userId);
}
```
#### **How it works:**  
1️⃣ Retrieves the **user’s session** from Redis using the key `"session:userId"`.  
2️⃣ Returns the session data **as a `Map<String, String>`**, containing the access and refresh tokens.  

🔹 **Example Session Data from Redis**  
```json
{
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI...",
    "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4..."
}
```
---

## **4. Deleting a User Session**  

```java
public void deleteSession(String userId) {
    redisTemplate.delete("session:" + userId);
}
```
#### **How it works:**  
1️⃣ **Removes the session data** from Redis when a user logs out.  
2️⃣ **Ensures that access and refresh tokens are no longer valid.**  

🔹 **Why manually delete sessions?**  
- Prevents unauthorized access after a user logs out.  
- Useful when a **user logs out early**, before the session expires.  
- Helps **clear invalid tokens** that might cause unintended authentication issues.  

---

## **5. Redis CLI Example: Checking Stored Sessions**  

### **Saving a Session**  
```bash
> SET session:google-oauth2|115873500067805995920 '{"accessToken":"abc123","refreshToken":"xyz789"}' EX 900
```
### **Retrieving a Session**  
```bash
> GET session:google-oauth2|115873500067805995920
{"accessToken":"abc123","refreshToken":"xyz789"}
```
### **Deleting a Session**  
```bash
> DEL session:google-oauth2|115873500067805995920
(integer) 1
```
---

## **6. When to Use Redis for Authentication?**  

✅ **Best for stateless authentication** → If you don’t want to store sessions in your database.  
✅ **Perfect for microservices** → Since sessions are centralized in Redis, multiple services can access them.  
✅ **Fast performance** → Ideal for systems handling **thousands of user logins per second**.  

🔹 **Potential Downsides**  
- **If Redis crashes, all sessions are lost** (unless persistent storage is enabled).  
- **Session hijacking risk** if Redis is exposed **without authentication**.  
- **Extra infrastructure needed** (a Redis server must be running).  

---
E. RedisConfig.java
The `RedisConfig` class **sets up Redis as the session storage for authentication** in the Spring Boot application.  

This configuration enables:  
✅ **Fast session storage and retrieval** with Redis.  
✅ **Efficient serialization and deserialization** of data.  
✅ **Connection pooling** via **Lettuce**, a high-performance Redis client.  

---

### **Class Overview**  
```java
@Configuration
public class RedisConfig {
```
- The `@Configuration` annotation tells Spring that this class **provides configuration beans**.  
- It **sets up Redis** for storing authentication sessions.  

### **Creating a RedisTemplate Bean**  
```java
@Bean
public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    return template;
}
```
🔹 **What happens here?**  
1️⃣ **Creates a `RedisTemplate<String, Object>` Bean**, allowing Spring Boot to interact with Redis.  
2️⃣ **Sets the connection factory** using `LettuceConnectionFactory`, which manages the Redis connection.  
3️⃣ **Serializes keys as strings** using `StringRedisSerializer()`.  
4️⃣ **Serializes values as JSON** using `GenericJackson2JsonRedisSerializer()`, allowing complex objects to be stored efficiently.  

---

## **2. Why Use Lettuce for Redis?**  

Lettuce is a **high-performance, non-blocking Redis client** built on Netty.  
Compared to **Jedis**, Lettuce offers:  
✅ **Better scalability** (supports reactive programming).  
✅ **Built-in connection pooling** (reduces connection overhead).  
✅ **Support for Redis Cluster and Sentinel**.  

🔹 **How to ensure Lettuce is being used?**  
Spring Boot automatically **defaults to Lettuce**, but you can verify by checking your dependencies:  
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```
or explicitly defining the factory bean:  
```java
@Bean
public LettuceConnectionFactory redisConnectionFactory() {
    return new LettuceConnectionFactory();
}
```

---

## **3. Customizing Redis Serialization**  

### **Why Serialize Keys as Strings?**  
```java
template.setKeySerializer(new StringRedisSerializer());
```
- Ensures **human-readable keys** in Redis.  
- Avoids issues when using Redis commands like `KEYS *`.  

### **Why Serialize Values as JSON?**  
```java
template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
```
- Converts **Java objects into JSON** for easy storage and retrieval.  
- **More efficient than Java’s default serialization** (`JdkSerializationRedisSerializer`).  
- **Ensures compatibility** with non-Java applications that might access Redis.  

🔹 **Example Stored Data in Redis**  
```bash
> GET session:google-oauth2|115873500067805995920
{"accessToken":"abc123","refreshToken":"xyz789"}
```
JSON format makes it **easy to debug and inspect** sessions directly in Redis CLI.  

---

## **4. How to Verify Redis Connection?**  

### **Check if Redis is Running**  
```bash
redis-cli ping
# Response: PONG
```
### **Check Redis Connection in Spring Boot**  
Add this to `application.properties`:  
```properties
spring.redis.host=localhost
spring.redis.port=6379
```
Run the app and check for Redis connection logs:  
```bash
Connected to Redis at localhost:6379
```
---

## **5. Testing RedisTemplate**  

To confirm that your Redis setup works, create a simple test:  

```java
@SpringBootTest
public class RedisTemplateTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    public void testRedisConnection() {
        redisTemplate.opsForValue().set("testKey", "Hello Redis!");
        String value = (String) redisTemplate.opsForValue().get("testKey");
        assertEquals("Hello Redis!", value);
    }
}
```
✅ **If the test passes**, your RedisTemplate is correctly configured.  

F. ProfileController.java

The `ProfileController` class  **retrieves and displays user profile information** after OAuth 2.0 authentication.  

This controller is responsible for:  
✅ Extracting **user claims** (such as name, email, and picture) from an **OIDC (OpenID Connect) token**.  
✅ Converting claims into **a JSON format** for easy viewing.  
✅ Logging any errors that occur while processing user data.  
✅ Rendering the **profile page** with the extracted information.  

---

### **Class Overview**  
```java
@Controller
public class ProfileController {
```
This class is annotated with `@Controller`, meaning it **handles HTTP requests** and **renders a Thymeleaf or JSP view** instead of returning JSON data like a typical REST controller.  

### **Logger and ObjectMapper**  

```java
private final Logger log = LoggerFactory.getLogger(this.getClass());
private final static ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
```
- **`Logger log`** → Used to log errors when processing JSON data.  
- **`ObjectMapper mapper`** → Converts Java objects (like user claims) to **JSON format**.  
- **`JavaTimeModule`** → Ensures compatibility with Java 8+ date/time fields.  

---

## **2. Handling Profile Requests**  

### **Defining the Profile Endpoint**  

```java
@GetMapping("/profile")
public String profile(Model model, @AuthenticationPrincipal OidcUser oidcUser) {
    model.addAttribute("profile", oidcUser.getClaims());
    model.addAttribute("profileJson", claimsToJson(oidcUser.getClaims()));
    return "profile";
}
```
#### **What happens here?**  
1️⃣ The user accesses **`/profile`**, triggering the `profile()` method.  
2️⃣ The method retrieves **user details** from the OAuth 2.0 **OIDC token** (`OidcUser`).  
3️⃣ The **claims (user info)** are added to the `Model`, making them accessible in the view.  
4️⃣ The **claims are also converted into a JSON string** for easy debugging.  
5️⃣ The method returns `"profile"`, telling Spring to render a **profile page** (`profile.html` or `profile.jsp`).  
---

## **3. Converting User Claims to JSON**  

### **Defining the JSON Conversion Method**  

```java
private String claimsToJson(Map<String, Object> claims) {
    try {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(claims);
    } catch (JsonProcessingException jpe) {
        log.error("Error parsing claims to JSON", jpe);
    }
    return "Error parsing claims to JSON.";
}
```
#### **What does this method do?**  
✅ Converts **user claims** (a Java `Map<String, Object>`) into a **formatted JSON string**.  
✅ Uses **Jackson’s ObjectMapper** for conversion.  
✅ If an error occurs, it logs the issue and returns an error message.  

### **Example JSON Output (User Claims from Google OAuth2)**  
```json
{
    "sub": "auth0|1234567890",
    "email": "user@example.com",
    "name": "John Doe",
    "picture": "https://example.com/profile.jpg",
    "given_name": "John",
    "nickname": "Johnny",
    "family_name": "Doe"
}
```
---

## **4. Handling the Profile View**  

### **Example Thymeleaf Profile Page (`profile.html`)**
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>User Profile</title>
</head>
<body>
    <h1>User Profile</h1>
    <p><strong>Name:</strong> <span th:text="${profile['name']}"></span></p>
    <p><strong>Email:</strong> <span th:text="${profile['email']}"></span></p>
    <p><strong>Picture:</strong> <img th:src="${profile['picture']}" alt="User Picture" width="100"/></p>
    <h2>Raw JSON Data</h2>
    <pre th:text="${profileJson}"></pre>
</body>
</html>
```
This **renders user details** with Thymeleaf, showing:  
✅ **User's name and email**.  
✅ **Profile picture** (if available).  
✅ **Raw JSON claims** for debugging.  

---

## **5. Logging and Error Handling**  

If JSON serialization fails, an error message is logged:  

```java
log.error("Error parsing claims to JSON", jpe);
```
## Requirements

- Java 17

### Running the Application  

This section provides step-by-step instructions to configure OAuth2 credentials, start the Spring Boot application, and test authentication features, including session storage in Redis.

---

#### **1. Set Up OAuth2 Credentials**  

Before running the application, configure OAuth2 credentials in `application.properties`. 

```properties
okta.oauth2.issuer=https://YOUR_OKTA_DOMAIN/oauth2/default
okta.oauth2.client-id=YOUR_OKTA_CLIENT_ID
okta.oauth2.client-secret=YOUR_OKTA_CLIENT_SECRET
```

Replace `YOUR_AUTH0_CLIENT_ID`, `YOUR_AUTH0_CLIENT_SECRET`, `YOUR_AUTH0_DOMAIN`, `YOUR_OKTA_CLIENT_ID`, and `YOUR_OKTA_CLIENT_SECRET` with the actual values from your Auth0 or Okta dashboard.

---

#### **2. Start the Spring Boot Application**  

To run the application, ensure you have **Java 17+**, **Maven**, and **Redis** installed.

1. **Start Redis:**  
   If Redis is installed locally, start it with:
   ```sh
   redis-server
   ```
   If using Docker, start Redis with:
   ```sh
   docker run --name redis -d -p 6379:6379 redis
   ```

2. **Run the Spring Boot app:**  
   Navigate to the project directory and execute:
   ```sh
   mvn spring-boot:run
   ```
   or if using Gradle:
   ```sh
   ./gradlew bootRun
   ```

3. The application should now be running on [http://localhost:3000](http://localhost:3000).

---

#### **3. Test Login and Logout Functionality**  

1. Open a browser and go to `http://localhost:3000`.
2. Click the **Login** button, which redirects you to the Auth0 authentication page.
3. Log in using a valid account.
4. After a successful login, you should be redirected back to the application, and your user profile should be visible.
5. Click **Logout**, and you should be redirected to the Auth0 logout URL.

---

#### **4. Check Redis for Stored Sessions**  

After logging in, check if the session is stored in Redis:

1. Open a Redis CLI session:
   ```sh
   redis-cli
   ```

2. List stored sessions:
   ```sh
   KEYS session:*
   ```
   This should return keys like `session:USER_ID`.

3. Retrieve session details:
   ```sh
   GET session:USER_ID
   ```
   Replace `USER_ID` with your actual user ID. The response should contain the stored **accessToken** and **refreshToken**.

4. After logging out, check Redis again:
   ```sh
   KEYS session:*
   ```
   The session should be **deleted**, confirming that the logout process also clears the session.

---
