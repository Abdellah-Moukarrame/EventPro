
package org.example.eventpro.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI eventProOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EventPro REST API")
                        .version("1.0.0")
                        .description(
                                "API de gestion des événements, " +
                                        "réservations, billets, paiements et promotions."
                        )
                        .contact(new Contact()
                                .name("EventPro Development Team")));
    }
}
