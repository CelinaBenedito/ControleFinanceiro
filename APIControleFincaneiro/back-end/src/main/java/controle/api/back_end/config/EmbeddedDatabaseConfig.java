package controle.api.back_end.config;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfiguration;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.io.File;

/**
 * Inicia o MariaDB embutido (MySQL-compatível) automaticamente ao subir a aplicação.
 * O banco de dados é armazenado em ~/.myfinance/db.
 * Funciona em Windows 64-bit e Ubuntu (Linux x86_64).
 */
@Configuration
@ConditionalOnProperty(name = "app.database.embedded", havingValue = "true", matchIfMissing = false)
public class EmbeddedDatabaseConfig {

    @Value("${app.database.port:13306}")
    private int port;

    @Value("${app.database.name:controle_financeiro}")
    private String dbName;

    /**
     * Subclasse de DB que reutiliza uma instalação MariaDB existente.
     * Chama prepareDirectories() e unpackEmbeddedDb() (ambos protected),
     * mas OMITE o install() (mysql_install_db) que falha se o datadir não estiver vazio.
     */
    private static class ExistingMariaDBInstance extends DB {
        protected ExistingMariaDBInstance(DBConfiguration config) throws ManagedProcessException {
            super(config);
            prepareDirectories(); // inicializa baseDir, libDir, tmpDir, dataDir
            unpackEmbeddedDb();   // extrai binários do JAR para o tempDir (necessário após reboot)
        }
    }

    /**
     * Mata processos mysqld residuais (de sessões anteriores encerradas abruptamente)
     * e remove arquivos .pid obsoletos que bloqueiam o ibdata1.
     * Sem isso, o erro "InnoDB: The data file './ibdata1' must be writable" impede o start.
     */
    private static void killLingeringMysqldProcesses(File dataDir) {
        try {
            ProcessHandle.allProcesses()
                .filter(p -> p.info().command()
                    .map(cmd -> cmd.toLowerCase().contains("mysqld"))
                    .orElse(false))
                .forEach(p -> {
                    System.out.println("[EmbeddedDB] Encerrando mysqld residual: PID " + p.pid());
                    p.destroyForcibly();
                });
            Thread.sleep(1500); // aguarda liberação dos locks de arquivo
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.out.println("[EmbeddedDB] Aviso ao encerrar processos residuais: " + e.getMessage());
        }

        // Remove arquivos .pid obsoletos que causam o erro "must be writable"
        File[] pidFiles = dataDir.listFiles((d, name) -> name.endsWith(".pid"));
        if (pidFiles != null) {
            for (File pid : pidFiles) {
                System.out.println("[EmbeddedDB] Removendo PID residual: " + pid.getName());
                pid.delete();
            }
        }
    }

    @Bean(destroyMethod = "stop")
    public DB embeddedMariaDB() throws ManagedProcessException {
        File dataDir = new File(
                System.getProperty("user.home"),
                ".myfinance" + File.separator + "db"
        );

        // Garante limpeza de sessões anteriores encerradas abruptamente
        killLingeringMysqldProcesses(dataDir);

        DBConfigurationBuilder config = DBConfigurationBuilder.newBuilder();
        config.setPort(port);
        config.setDataDir(dataDir);

        // A pasta "mysql/" indica instalação MariaDB ja concluída.
        // Se existir: apenas inicia o servidor (pula mysql_install_db).
        // Se não existir ou estiver corrompida: limpa e faz instalação completa.
        boolean alreadyInstalled = new File(dataDir, "mysql").isDirectory();

        DB db;
        if (alreadyInstalled) {
            System.out.println("[EmbeddedDB] Banco existente detectado em: " + dataDir.getAbsolutePath());
            db = new ExistingMariaDBInstance(config.build());
        } else {
            // Diretório parcialmente corrompido ou primeira execução
            if (dataDir.exists() && dataDir.list() != null && dataDir.list().length > 0) {
                System.out.println("[EmbeddedDB] Diretório com dados incompletos detectado. Limpando para reinstalar...");
                deleteDirectoryContents(dataDir);
            }
            System.out.println("[EmbeddedDB] Primeiro inicio — instalando banco em: " + dataDir.getAbsolutePath());
            db = DB.newEmbeddedDB(config.build());
        }

        db.start();

        // Cria o schema se ainda não existir (idempotente)
        try {
            db.createDB(dbName);
        } catch (ManagedProcessException e) {
            System.out.println("[EmbeddedDB] Schema '" + dbName + "' já existente.");
        }

        System.out.println("[EmbeddedDB] MariaDB pronto na porta " + port);
        return db;
    }

    private static void deleteDirectoryContents(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectoryContents(f);
                }
                f.delete();
            }
        }
    }

    @Bean
    @Primary
    @DependsOn("embeddedMariaDB")
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mariadb://localhost:" + port + "/" + dbName
                + "?useSSL=false&allowPublicKeyRetrieval=true"
                + "&serverTimezone=America/Sao_Paulo"
                + "&characterEncoding=UTF-8");
        ds.setUsername("root");
        ds.setPassword("");
        ds.setDriverClassName("org.mariadb.jdbc.Driver");
        ds.setMaximumPoolSize(10);
        ds.setMinimumIdle(2);
        ds.setConnectionTimeout(30000);
        return ds;
    }
}


