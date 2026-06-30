package controle.api.back_end.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Popula as tabelas de referencia (instituicao, categoria) na inicializacao da aplicacao.
 *
 * Executa DEPOIS que o Hibernate ja criou/atualizou as tabelas (ddl-auto=update),
 * garantindo que o script nao falhe por tabelas inexistentes.
 *
 * Usa setContinueOnError(true) para que erros de chave duplicada (INSERT IGNORE)
 * sejam ignorados silenciosamente nas inicializacoes subsequentes.
 */
@Component
public class DataInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final DataSource dataSource;

    public DataInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("data.sql"));
            populator.setContinueOnError(true);
            populator.setSeparator(";");
            populator.execute(dataSource);
            System.out.println("[DataInitializer] Dados iniciais carregados com sucesso.");
        } catch (Exception e) {
            System.err.println("[DataInitializer] Erro ao carregar dados iniciais: " + e.getMessage());
        }
    }
}


