package controle.api.back_end.config.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenaApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MyFinance API")
                        .description("A MyFinance API oferece endpoints para gerenciar usuários e registros financeiros, além de recursos auxiliares como instituições, categorias, configurações e dashboards.\n" +
                                "O núcleo da aplicação está nos controllers de Usuário e Registros, permitindo cadastrar, consultar e organizar movimentações financeiras.\n" +
                                "Dados são persistidos em banco local (H2 em desenvolvimento) e há integração com Python para importar informações bancárias via arquivos OFX, sem armazenar dados sensíveis.")
                        .version("4.5.13")
                        .contact(new io.swagger.v3.oas.models.info.Contact()
                                .name("Equipe de Desenvolvimento")
                                .email("celina.benedito@sptech.school")
                                .email("isaak.guilherme@sptech.school")
                                )
                        );
    }
}
