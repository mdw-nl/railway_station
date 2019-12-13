package nl.medicaldataworks.railway.station.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("central")
@Data
@EnableConfigurationProperties
public class CentralConfiguration {
    String hostname;
    int port;
}
