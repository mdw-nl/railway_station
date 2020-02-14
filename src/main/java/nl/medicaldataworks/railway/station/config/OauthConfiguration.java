package nl.medicaldataworks.railway.station.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.client.RestTemplate;

import static org.springframework.security.config.Customizer.withDefaults;

@EnableGlobalMethodSecurity(prePostEnabled = true)
@Configuration
public class OauthConfiguration  {
    @Bean
    RestTemplate restTemplate(){
        return new RestTemplate();
    }

    @Bean
    SecurityWebFilterChain configure(ServerHttpSecurity http) throws Exception {
        http.oauth2Client(withDefaults());
        return http.build();
    }
}
