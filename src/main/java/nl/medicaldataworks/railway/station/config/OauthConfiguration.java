package nl.medicaldataworks.railway.station.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.UnAuthenticatedServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.security.config.Customizer.withDefaults;

@EnableWebFluxSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Configuration
public class OauthConfiguration  {
    @Bean
    WebClient webClient(ReactiveClientRegistrationRepository clientRegistrationRepository) {
        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(
                        clientRegistrationRepository,
                        new UnAuthenticatedServerOAuth2AuthorizedClientRepository());
        oauth.setDefaultClientRegistrationId("keycloak");
        oauth.setDefaultOAuth2AuthorizedClient(true);
        return WebClient.builder()
                .filter(oauth)
                .build();
    }

    @Bean
    SecurityWebFilterChain configure(ServerHttpSecurity http) throws Exception {
        http.oauth2Client(withDefaults());
        return http.build();
    }
}
