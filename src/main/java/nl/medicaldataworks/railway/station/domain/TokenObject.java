package nl.medicaldataworks.railway.station.domain;

import lombok.Data;

@Data
public class TokenObject {
    private String access_token;
    private String expires_in;
    private String refresh_expires_in;
    private String token_type;
    private String session_state;
    private String scope;
}
