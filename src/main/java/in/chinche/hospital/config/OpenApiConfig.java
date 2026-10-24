package in.chinche.hospital.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI chincheOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Chinche Hospital API")
                        .version("v1")
                        .description("REST API for चींचे हॉस्पिटल, Katol (Nagpur) — doctor discovery, "
                                + "slot availability, multi-step appointment booking, health packages and CMS content.")
                        .contact(new Contact()
                                .name("Digital Care Team")
                                .email("dadakalbande111@gmail.com")
                                .url("https://chinchehospital.in"))
                        .license(new License().name("Proprietary")))
                .servers(List.of(new Server().url("/").description("This instance")));
    }
}
