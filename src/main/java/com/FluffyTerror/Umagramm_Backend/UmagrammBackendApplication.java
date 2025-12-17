package com.FluffyTerror.Umagramm_Backend;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

@SpringBootApplication
@EnableR2dbcAuditing
@SecurityScheme(name = "keycloak",
        type = SecuritySchemeType.OAUTH2,
        bearerFormat = "JWT",
        scheme = "bearer",
        in = SecuritySchemeIn.HEADER,
        flows = @OAuthFlows(
                password = @OAuthFlow(
                        authorizationUrl = "http://localhost:9090/realms/Umagramm/protocol/openid-connect/auth",
                        tokenUrl = "http://localhost:9090/realms/Umagramm/protocol/openid-connect/token"
                )
        )
)
public class UmagrammBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(UmagrammBackendApplication.class, args);
    }

}
