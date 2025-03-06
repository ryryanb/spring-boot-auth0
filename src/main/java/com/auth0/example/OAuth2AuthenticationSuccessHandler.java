package com.auth0.example;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RedisAuthService redisAuthService;
    private final OAuth2AuthorizedClientService authorizedClientService; // ✅ Inject OAuth2AuthorizedClientService

    public OAuth2AuthenticationSuccessHandler(UserRepository userRepository, 
                                              RedisAuthService redisAuthService,
                                              OAuth2AuthorizedClientService authorizedClientService) {
        this.userRepository = userRepository;
        this.redisAuthService = redisAuthService;
        this.authorizedClientService = authorizedClientService; // ✅ Store injected service
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2User oAuth2User = oauthToken.getPrincipal();
            Map<String, Object> attributes = oAuth2User.getAttributes();

            String userId = (String) attributes.get("sub");
            String email = (String) attributes.get("email");
            String name = (String) attributes.get("name");
            String picture = (String) attributes.get("picture");
            String givenName = (String) attributes.get("given_name");
            String nickName = (String) attributes.get("nickname");
            String familyName = (String) attributes.get("family_name");

            Optional<User> existingUser = userRepository.findByAuth0Id(userId);
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

            // ✅ Extract access token using OAuth2AuthorizedClientService
            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
            );

            String accessToken = (authorizedClient != null) ? authorizedClient.getAccessToken().getTokenValue() : null;
            String refreshToken = (authorizedClient != null && authorizedClient.getRefreshToken() != null)
                    ? authorizedClient.getRefreshToken().getTokenValue()
                    : null;

            // ✅ Store in Redis
            redisAuthService.saveSession(userId, accessToken, refreshToken);
        }
        response.sendRedirect("/");
    }
}
