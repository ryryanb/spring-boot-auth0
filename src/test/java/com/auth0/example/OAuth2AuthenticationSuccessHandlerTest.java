package com.auth0.example;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisAuthService redisAuthService;

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private OAuth2AuthenticationToken authentication;

    @Mock
    private OAuth2User oAuth2User;

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(Map.of(
            "sub", "auth0|12345",
            "email", "test@example.com",
            "name", "Test User",
            "picture", "https://example.com/pic.jpg",
            "given_name", "Test",
            "nickname", "Tester",
            "family_name", "User"
        ));
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("auth0");
        when(authentication.getName()).thenReturn("auth0|12345");
    }

    @Test
    void testOnAuthenticationSuccess_NewUser() throws IOException, ServletException {
        when(userRepository.findByAuth0Id("auth0|12345")).thenReturn(Optional.empty());
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(accessToken.getTokenValue()).thenReturn("access-token");
        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(authorizedClientService.loadAuthorizedClient("auth0", "auth0|12345"))
            .thenReturn(authorizedClient);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(userRepository).save(any(User.class)); // Ensure user is saved
        verify(redisAuthService).saveSession("auth0|12345", "access-token", null); // Verify Redis session storage
        verify(response).sendRedirect("/"); // Ensure redirection
    }

    @Test
    void testOnAuthenticationSuccess_ExistingUser() throws IOException, ServletException {
        User existingUser = new User();
        existingUser.setAuth0Id("auth0|12345");
        existingUser.setLastLogin(LocalDateTime.now().minusDays(1));
        when(userRepository.findByAuth0Id("auth0|12345")).thenReturn(Optional.of(existingUser));

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(userRepository).save(any(User.class)); // Ensure last login is updated
        verify(response).sendRedirect("/"); // Ensure redirection
    }
}
