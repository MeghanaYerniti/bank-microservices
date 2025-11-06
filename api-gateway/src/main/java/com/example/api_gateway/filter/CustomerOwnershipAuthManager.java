package com.example.api_gateway.filter;

import com.example.api_gateway.dto.CustomerDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class CustomerOwnershipAuthManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private static final Logger log = LoggerFactory.getLogger(CustomerOwnershipAuthManager.class);
    private final WebClient webClient;

    public CustomerOwnershipAuthManager(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://customer-service").build();
    }

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext context) {
        return authentication.flatMap(auth -> { // unwraps the reactive obj and give actual Authentication object.
            Object principal = auth.getPrincipal(); // getting JWT token
            if (!(principal instanceof Jwt)) {
                log.warn("Principal is not a Jwt: {}", principal == null ? "null" : principal.getClass().getName());
                return Mono.just(new AuthorizationDecision(false)); // if not a jwt token then not allowed
            }

            Jwt jwtAuth = (Jwt) principal; // principal to Jwt object
            String username = jwtAuth.getSubject();
            List<String> roles = jwtAuth.getClaimAsStringList("roles");

            // if role is user then allowed
            if (roles != null && roles.contains("ROLE_ADMIN")) {
                log.debug("Admin access granted for {}", username);
                return Mono.just(new AuthorizationDecision(true));
            }

            // Extract customer ID from URL
            String path = context.getExchange().getRequest().getURI().getPath();
            String[] parts = path.split("/");
            Long customerId;
            try {
                customerId = Long.parseLong(parts[parts.length - 1]); // getting last index value
            } catch (Exception e) {
                log.warn("Failed to parse customer id from path '{}'", path, e);
                return Mono.just(new AuthorizationDecision(false));
            }

            // Call customer-service to verify ownership
            return webClient.get()
                    .uri("/customers/id/{id}", customerId)
                    .exchangeToMono((ClientResponse resp) -> {
                        if (resp.statusCode().is2xxSuccessful()) { // if status code is 200 OK
                            return resp.bodyToMono(CustomerDto.class).map(c -> { // converting body to CustomerDTo
                                boolean ok = c != null && c.getName() != null && username.trim().equalsIgnoreCase(c.getName().trim());
                                log.debug("Ownership check for user='{}' vs customer.name='{}' => {}", username, c == null ? "null" : c.getName(), ok);
                                return ok;
                            });
                        } else if (resp.statusCode().is4xxClientError()) { // 400 Bad request or 404 Not found
                            log.warn("Customer service returned {} for id {}", resp.statusCode(), customerId);
                            return Mono.just(false);
                        } else { // for remaining (500)
                            log.error("Customer service returned {} for id {}", resp.statusCode(), customerId);
                            return Mono.just(false);
                        }
                    })// If the WebClient call fails entirely (e.g., service down, timeout), this block executes.
                    .onErrorResume(e -> {
                        log.error("Error calling customer-service for id {}: {}", customerId, e.toString());
                        return Mono.just(false);
                    })
                    .map(AuthorizationDecision::new); // converting boolean to AuthorizationDecision
        }).defaultIfEmpty(new AuthorizationDecision(false)); // by default false
    }
}
