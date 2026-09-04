package controle.api.back_end.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Corrige colunas que o Hibernate mapeou anteriormente como ENUM nativo do MariaDB/MySQL.
 * <p>
 * Bancos criados com versões antigas da entidade {@code EventoFinanceiro} guardam a coluna
 * {@code tipo} como ENUM('Gasto','Recebimento','Transferencia','Poupanca','Emprestimo'),
 * fixado no momento da criação da tabela. Quando um novo valor é adicionado ao enum Java
 * {@code Tipo} (ex.: {@code Resgate}), o INSERT falha com
 * "Data truncated for column 'tipo'", pois o valor não existe no ENUM do banco.
 * <p>
 * Este inicializador roda uma única vez por startup (idempotente) e converte a coluna
 * para VARCHAR, que é o tipo que a entidade já usa via {@code @JdbcTypeCode(SqlTypes.VARCHAR)}.
 * Assim, novos valores de {@link controle.api.back_end.model.eventoFinanceiro.Tipo} passam
 * a ser aceitos sem exigir migração manual do banco.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SchemaFixInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final DataSource dataSource;

    public SchemaFixInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        corrigirColunaEnumParaVarchar("evento_financeiro", "tipo", 30);
    }

    private void corrigirColunaEnumParaVarchar(String tabela, String coluna, int tamanho) {
        try (Connection conn = dataSource.getConnection()) {
            String tipoAtual = obterTipoColuna(conn, tabela, coluna);
            if (tipoAtual == null) {
                return; // tabela/coluna ainda não existe (primeira execução antes do Hibernate criar)
            }
            if (tipoAtual.toLowerCase().startsWith("enum")) {
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate("ALTER TABLE " + tabela + " MODIFY COLUMN " + coluna
                            + " VARCHAR(" + tamanho + ") NOT NULL");
                    System.out.println("[SchemaFix] Coluna '" + coluna + "' de '" + tabela
                            + "' convertida de ENUM para VARCHAR(" + tamanho + ") com sucesso.");
                }
            }
        } catch (Exception e) {
            System.err.println("[SchemaFix] Falha ao corrigir coluna '" + coluna + "' de '" + tabela
                    + "': " + e.getMessage());
        }
    }

    private String obterTipoColuna(Connection conn, String tabela, String coluna) throws Exception {
        String sql = "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tabela);
            ps.setString(2, coluna);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }
}
