package nl.medicaldataworks.railway.station.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties("station")
@Data
@EnableConfigurationProperties
public class StationConfiguration {
    private Map<String, String> environmentVariables;
}
