package com.auth0.example;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

/**
 * Controller for the home page.
 */
@Controller
public class HomeController {
	
	private final RedisAuthService redisAuthService;
	
	public HomeController( RedisAuthService redisAuthService) {

        this.redisAuthService = redisAuthService;
    }

    @GetMapping("/")
    public String home(Model model, @AuthenticationPrincipal OidcUser principal) {
        if (principal != null) {
            model.addAttribute("profile", principal.getClaims());
        }
        return "index";
    }
    
    @GetMapping("/user/session")
    public ResponseEntity<Map<String, String>> getSession(@RequestParam String userId) {
        Map<String, String> session = redisAuthService.getSession(userId);
        if (session != null) {
            return ResponseEntity.ok(session);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Session not found"));
    }
    
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

}