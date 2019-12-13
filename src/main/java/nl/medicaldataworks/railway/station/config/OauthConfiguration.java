package nl.medicaldataworks.railway.station.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.UnAuthenticatedServerOAuth2AuthorizedClientRepository;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OauthConfiguration {

    @Bean
    public WebClient webClient(ReactiveClientRegistrationRepository reactiveClientRegistrationRepository) {
        ServerOAuth2AuthorizedClientExchangeFilterFunction oauthFilter =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(
                        reactiveClientRegistrationRepository,
                        new UnAuthenticatedServerOAuth2AuthorizedClientRepository());
        oauthFilter.setDefaultClientRegistrationId("keycloak");
        return WebClient.builder()
                .filter(oauthFilter)
                .build();
    }

//    @Bean
//    public WebClient webClient(ClientRegistrationRepository clientRegistrationRepository , OAuth2AuthorizedClientRepository authorizedClientRepository) {
//        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth =
//                new ServletOAuth2AuthorizedClientExchangeFilterFunction (clientRegistrationRepository , authorizedClientRepository);
//        oauth.setDefaultClientRegistrationId("authProvider");
//        return WebClient.builder().apply(oauth.oauth2Configuration()).build();
//    }
}
