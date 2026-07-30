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
     * Encerra todos os processos mysqld residuais e remove arquivos .pid obsoletos.
     *
     * NOTA: Esta aplicação usa um lock de instância única na porta 13308
     * (BackEndApplication.acquireSingleInstanceLock). Portanto, quando este método
     * é chamado durante a inicialização do Spring, somos SEMPRE a única instância.
     * Qualquer mysqld rodando na porta {@code dbPort} é necessariamente órfão de uma
     * sessão anterior que encerrou abruptamente — deve ser encerrado para liberar ibdata1.
     */
    private static void killLingeringMysqldProcesses(File dataDir, int dbPort) {
        boolean portActive = isPortResponding(dbPort);

        if (portActive) {
            System.out.println("[EmbeddedDB] mysqld orfao detectado na porta " + dbPort
                    + " (sessao anterior nao encerrou corretamente). Encerrando...");
        }

        // Encerra todos os processos mysqld (como somos a única instância, qualquer
        // mysqld rodando é de uma sessão anterior que crashou)
        try {
            java.util.List<ProcessHandle> mysqldProcs = ProcessHandle.allProcesses()
                    .filter(p -> p.info().command()
                            .map(cmd -> cmd.toLowerCase().contains("mysqld"))
                            .orElse(false))
                    .collect(java.util.stream.Collectors.toList());

            long killed = mysqldProcs.size();
            mysqldProcs.forEach(p -> {
                System.out.println("[EmbeddedDB] Encerrando mysqld residual: PID " + p.pid());
                p.destroyForcibly();
            });

            if (killed > 0 || portActive) {
                System.out.println("[EmbeddedDB] Aguardando liberacao de recursos do banco...");
                Thread.sleep(2000); // aguarda liberação inicial dos locks de arquivo

                // Aguarda a porta ser liberada (até 8 segundos)
                int waitMs = 0;
                while (isPortResponding(dbPort) && waitMs < 8000) {
                    Thread.sleep(500);
                    waitMs += 500;
                }
                if (!isPortResponding(dbPort)) {
                    System.out.println("[EmbeddedDB] Porta " + dbPort + " liberada. Prosseguindo...");
                } else {
                    System.out.println("[EmbeddedDB] Aviso: porta " + dbPort + " ainda ativa apos espera. Tentando mesmo assim.");
                }
            }
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

    private static boolean isPortResponding(int p) {
        try (java.net.Socket s = new java.net.Socket()) {
            s.connect(new java.net.InetSocketAddress("127.0.0.1", p), 800);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Bean(destroyMethod = "stop")
    public DB embeddedMariaDB() throws ManagedProcessException {
        File dataDir = new File(
                System.getProperty("user.home"),
                ".myfinance" + File.separator + "db"
        );

        killLingeringMysqldProcesses(dataDir, port);

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
                + "&characterEncoding=UTF-8"
                + "&autoReconnect=true"
                + "&connectTimeout=5000");
        ds.setUsername("root");
        ds.setPassword("");
        ds.setDriverClassName("org.mariadb.jdbc.Driver");
        ds.setMaximumPoolSize(10);
        ds.setMinimumIdle(2);
        ds.setConnectionTimeout(10000);          // falha rápido (10s em vez de 30s)
        ds.setIdleTimeout(300000);               // 5 min
        ds.setMaxLifetime(600000);               // 10 min (evita conexões fechadas pelo servidor)
        ds.setKeepaliveTime(60000);              // ping a cada 1 min para detectar conexão morta
        ds.setConnectionTestQuery("SELECT 1");   // query de validação simples
        ds.setInitializationFailTimeout(-1);     // nao falha no startup se DB demora
        return ds;
    }
}


