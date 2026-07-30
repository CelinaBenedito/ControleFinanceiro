package controle.api.back_end.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Configura o Spring MVC para servir arquivos estáticos de diretórios externos
 * ao classpath — necessário para imagens de perfil salvas em tempo de execução.
 *
 * As imagens ficam em ~/.myfinance/uploads/user_images/ e são acessíveis via
 * GET /uploads/user_images/{arquivo}
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadsPath = System.getProperty("user.home")
                + File.separator + ".myfinance"
                + File.separator + "uploads"
                + File.separator;

        // Garante que o diretório existe na inicialização
        new File(uploadsPath + "user_images").mkdirs();

        // Serve GET /uploads/** a partir do diretório externo ~/.myfinance/uploads/
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadsPath);
    }
}

