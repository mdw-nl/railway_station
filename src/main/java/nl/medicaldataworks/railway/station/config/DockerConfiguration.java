package nl.medicaldataworks.railway.station.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("docker-registry")
@Data
@EnableConfigurationProperties
public class DockerConfiguration {
    private String username;
    private String password;
}
