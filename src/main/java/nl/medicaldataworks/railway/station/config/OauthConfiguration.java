package nl.medicaldataworks.railway.station.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.UnAuthenticatedServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.security.config.Customizer.withDefaults;

@EnableWebFluxSecurity
@Configuration
public class OauthConfiguration  {
//
//    @Bean
//    ReactiveClientRegistrationRepository clientRegistrations() {
//        ClientRegistration clientRegistration = ClientRegistrations
//                .fromOidcIssuerLocation("http://localhost:9080/auth/realms/railway")
//                .tokenUri("")
//                .clientId("testclient")
//                .clientSecret("8fe44274-b6bb-4be2-baa7-343ebcd3021c")
//                .build();
//        return new InMemoryReactiveClientRegistrationRepository(clientRegistration);
//    }

    @Bean
    WebClient webClient(ReactiveClientRegistrationRepository clientRegistrationRepository) {
        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(
                        clientRegistrationRepository,
                        new UnAuthenticatedServerOAuth2AuthorizedClientRepository());
        oauth.setDefaultClientRegistrationId("keycloak");
        oauth.setDefaultOAuth2AuthorizedClient(true);
        return WebClient.builder()
                .baseUrl("http://localhost:8080")
                .filter(oauth)
                .build();
    }

//    @Bean
//    public WebClient webClient(ClientRegistrationRepository clientRegistrationRepository , OAuth2AuthorizedClientRepository authorizedClientRepository) {
//        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth =
//                new ServletOAuth2AuthorizedClientExchangeFilterFunction (clientRegistrationRepository , authorizedClientRepository);
//        oauth.setDefaultOAuth2AuthorizedClient(true);
//        return WebClient.builder()
//                .baseUrl("http://localhost:8080")
//                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
//                .filter(oauth)
//                .build();
//    }

    @Bean
    SecurityWebFilterChain configure(ServerHttpSecurity http) throws Exception {
        http.oauth2Client(withDefaults());
        return http.build();
    }

//    @Bean
//    public WebClient webClient(ReactiveClientRegistrationRepository reactiveClientRegistrationRepository) {
//        ServerOAuth2AuthorizedClientExchangeFilterFunction oauthFilter =
//                new ServerOAuth2AuthorizedClientExchangeFilterFunction(
//                        reactiveClientRegistrationRepository,
//                        new UnAuthenticatedServerOAuth2AuthorizedClientRepository());
//        oauthFilter.setDefaultOAuth2AuthorizedClient(true);
//        WebClient webclient = WebClient.builder()
//                .baseUrl("http://localhost:8080")
//                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
//                .filter(oauthFilter)
//                .build();
//
//        return webclient;
//    }
}
