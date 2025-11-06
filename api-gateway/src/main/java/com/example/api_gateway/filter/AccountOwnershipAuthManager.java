package com.example.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class AccountOwnershipAuthManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private static final Logger log = LoggerFactory.getLogger(AccountOwnershipAuthManager.class);
    private final WebClient webClient;

    public AccountOwnershipAuthManager(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://bank-account-service").build(); // mapping to bank account service => base url
    }

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext context) {
        return authentication.flatMap(auth -> { // unwraps the reactive obj and give actual Authentication object
            if (!(auth.getPrincipal() instanceof Jwt jwtAuth)) {
                log.warn("Principal is not a valid JWT");
                return Mono.just(new AuthorizationDecision(false));
            }

            String username = jwtAuth.getSubject();
            List<String> roles = jwtAuth.getClaimAsStringList("roles");

            // Allow admin access directly
            if (roles != null && roles.contains("ROLE_ADMIN")) {
                return Mono.just(new AuthorizationDecision(true));
            }

            // Extract account ID from the URL path
            String path = context.getExchange().getRequest().getURI().getPath();
            String[] parts = path.split("/");
            String lastSegment = parts[parts.length - 1].isBlank() ? parts[parts.length - 2] : parts[parts.length - 1];

            Long accountId;
            try {
                accountId = Long.parseLong(lastSegment);
            } catch (NumberFormatException e) {
                log.warn("Invalid account ID in path: {}", path);
                return Mono.just(new AuthorizationDecision(false));
            }

            // Forward token to the downstream service
            String token = jwtAuth.getTokenValue();

            return webClient.get()
                    .uri("/accounts/by-customer/{id}", accountId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .exchangeToMono(resp -> handleResponse(resp, username, accountId))
                    .onErrorResume(e -> {
                        log.error("Error contacting bank-account-service for ID {}: {}", accountId, e.getMessage());
                        return Mono.just(false);
                    })
                    .map(AuthorizationDecision::new);

        }).defaultIfEmpty(new AuthorizationDecision(false));
    }

    private Mono<Boolean> handleResponse(ClientResponse resp, String username, Long accountId) {
        if (resp.statusCode().is2xxSuccessful()) {
            return resp.bodyToMono(List.class).map(accounts -> {
                if (accounts == null || accounts.isEmpty()) {
                    log.warn("No accounts found for ID {}", accountId);
                    return false;
                }

                Map<String, Object> account = (Map<String, Object>) accounts.get(0);
                Object holderName = account.getOrDefault("accountHolderName", account.get("customerName"));

                boolean isOwner = holderName != null && username.equalsIgnoreCase(holderName.toString().trim());
                log.debug("Ownership check: user='{}' vs holder='{}' => {}", username, holderName, isOwner);
                return isOwner;
            });
        } else {
            log.warn("bank-account-service returned {} for account ID {}", resp.statusCode(), accountId);
            return Mono.just(false);
        }
    }
}
