package controle.api.back_end;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Controle Financeiro API")
                        .description("API para gerenciamento financeiro")
                        .version("1.0.0")
                        .contact(new io.swagger.v3.oas.models.info.Contact()
                                .name("Equipe de Desenvolvimento")
                                .email("celina.benedito@sptech.school")
                                )
                        );
    }
}
