package nl.medicaldataworks.railway.station.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.PostConstruct;
import java.net.URISyntaxException;

import static org.apache.http.HttpVersion.HTTP;

@Service
@Slf4j
public class AuthenticationService {
    public static final String CLIENT_CREDENTIALS = "client_credentials";
    public static final String GRANT_TYPE = "grant_type";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    private static final String API_PATH = "";
    private WebClient webclient;
    private ConfigurableApplicationContext context;

    public AuthenticationService(ConfigurableApplicationContext context) {

        this.context = context;
    }

    @PostConstruct
    public void authenticate(){
//        URIBuilder builder = new URIBuilder();
//        builder.setScheme(HTTP);
//        builder.setHost(keycloakConfiguration.getHostname());
//        builder.setPort(keycloakConfiguration.getPort());
//        builder.setPath(API_PATH);
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
//        body.add(GRANT_TYPE, CLIENT_CREDENTIALS);
//        body.add(CLIENT_ID, clientConfiguration.getId());
//        body.add(CLIENT_SECRET, clientConfiguration.getSecret());
//        HttpEntity<MultiValueMap<String, Object>> requestEntity
//                = new HttpEntity<>(body, headers);
//        ResponseEntity<String> response = null;
//        try {
//            response = restTemplate.exchange(builder.build(),
//                    HttpMethod.POST,
//                    requestEntity,
//                    String.class);
//        } catch (URISyntaxException e) {
//            log.error("Unable to authenticate: {}", e);
//            context.close();
//        }
//        log.info("response: {}", response);
//
//        if (!response.getStatusCode().is2xxSuccessful()){
//            context.close();
//        }
    }
}
