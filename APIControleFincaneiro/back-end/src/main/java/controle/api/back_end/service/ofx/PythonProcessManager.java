package controle.api.back_end.service.ofx;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;

/**
 * Gerencia o ciclo de vida do processo Python (FastAPI + Playwright).
 *
 * <h3>Quando o Python é iniciado?</h3>
 * <p>O Python <strong>NÃO</strong> é iniciado automaticamente no startup da aplicação.
 * Ele é iniciado de forma <em>lazy</em> em dois cenários:
 * <ol>
 *   <li>Chamada explícita via {@code POST /ofx/sessao/iniciar} (orquestrado pelo JS
 *       após o usuário se estabelecer no perfil).</li>
 *   <li>Primeira chamada a {@code POST /ofx/sync} (que internamente chama
 *       {@link #garantirRodando()}).</li>
 * </ol>
 *
 * <h3>Quem orquestra o início?</h3>
 * <p>O JavaScript do frontend — após detectar que o usuário permaneceu no perfil
 * por um determinado tempo (debounce) — chama {@code POST /ofx/sessao/iniciar}
 * para pré-aquecer o Python antes da sincronização.
 *
 * <p>Em {@link #parar()} (chamado no shutdown do Spring), o processo é encerrado.
 */
@Component
public class PythonProcessManager {

    private static final Logger log = Logger.getLogger(PythonProcessManager.class.getName());

    @Value("${python.scraper.dir:python-scraper}")
    private String scraperDir;

    @Value("${python.scraper.executable:python}")
    private String pythonExecutable;

    @Value("${python.api.url:http://127.0.0.1:8000}")
    private String pythonApiUrl;

    @Value("${python.startup.timeout.seconds:30}")
    private int startupTimeoutSeconds;

    private Process processo;

    // ─────────────────────────────────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Garante que o processo Python está rodando e pronto para receber requests.
     * Se já estiver rodando, não faz nada.
     * Chamado internamente antes de qualquer operação de sync.
     */
    public synchronized void garantirRodando() {
        if (estaRodando()) {
            return;
        }
        iniciar();
        aguardarPronto();
    }

    /**
     * Inicia o Python explicitamente. Útil quando o JS quer pré-aquecer
     * o scraper antes da sincronização (ex: logo após o debounce do usuário).
     * Se já estiver rodando, retorna sem fazer nada.
     */
    public synchronized void iniciarExplicitamente() {
        garantirRodando();
    }

    public boolean estaRodando() {
        return processo != null && processo.isAlive();
    }

    /**
     * Retorna mensagem descritiva do estado atual do processo.
     */
    public String getMensagemStatus() {
        if (estaRodando()) {
            return "Python scraper rodando (PID: " + processo.pid() + ")";
        }
        return "Python scraper parado — sera iniciado na proxima sincronizacao";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Interno
    // ─────────────────────────────────────────────────────────────────────────

    private void iniciar() {
        try {
            Path scraperPath = resolverScraperPath();
            Path mainPy      = scraperPath.resolve("src/main.py");
            String pythonExe = resolverPythonExecutavel();

            log.info("[PythonProcessManager] Iniciando Python");
            log.info("[PythonProcessManager]   executavel : " + pythonExe);
            log.info("[PythonProcessManager]   scraper dir: " + scraperPath);
            log.info("[PythonProcessManager]   main.py    : " + mainPy);

            ProcessBuilder pb = new ProcessBuilder(pythonExe, mainPy.toString())
                    .directory(scraperPath.toFile())
                    .redirectErrorStream(true);

            // Redireciona saida para o console do Spring Boot
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);

            processo = pb.start();
            log.info("[PythonProcessManager] Processo iniciado — PID: " + processo.pid());

        } catch (IOException e) {
            throw new RuntimeException(
                    "Nao foi possivel iniciar o Python scraper.\n" +
                    "  user.dir     : " + System.getProperty("user.dir") + "\n" +
                    "  scraper.dir  : " + scraperDir + "\n" +
                    "  python exe   : " + pythonExecutable + "\n" +
                    "Causa: " + e.getMessage(), e
            );
        }
    }

    /**
     * Resolve o diretorio do scraper testando varias bases:
     * 1. Valor de python.scraper.dir como caminho absoluto
     * 2. Relativo ao user.dir atual
     * 3. Relativo ao diretorio do JAR/classe
     * 4. Relativo ao diretorio pai do user.dir (caso o IDE aponte para subpasta)
     */
    private Path resolverScraperPath() {
        // 1. Absoluto — se ja vier configurado como caminho absoluto
        Path candidato = Paths.get(scraperDir);
        if (candidato.isAbsolute() && Files.isDirectory(candidato)) {
            return candidato;
        }

        // 2. Relativo ao user.dir (CWD do processo Java)
        Path cwd = Paths.get(System.getProperty("user.dir"));
        List<Path> candidatos = List.of(
                cwd.resolve(scraperDir),                     // ex: back-end/python-scraper
                cwd.getParent().resolve("back-end").resolve(scraperDir), // ex: projeto/back-end/python-scraper
                cwd.resolve("back-end").resolve(scraperDir), // ex: projeto/back-end/python-scraper
                // Caminho absoluto derivado da localizacao desta classe
                resolverPorClasse()
        );

        for (Path c : candidatos) {
            log.info("[PythonProcessManager] Testando caminho: " + c.toAbsolutePath());
            if (Files.isDirectory(c) && Files.exists(c.resolve("src/main.py"))) {
                log.info("[PythonProcessManager] Scraper encontrado em: " + c.toAbsolutePath());
                return c.toAbsolutePath();
            }
        }

        throw new RuntimeException(
                "Diretorio do Python scraper nao encontrado.\n" +
                "  user.dir configurado: " + scraperDir + "\n" +
                "  user.dir (JVM)      : " + cwd + "\n" +
                "  Candidatos testados : " + candidatos.stream()
                        .map(p -> p.toAbsolutePath().toString()).toList() + "\n" +
                "Dica: configure python.scraper.dir com o caminho ABSOLUTO no application.properties.\n" +
                "  Exemplo: python.scraper.dir=C:/caminho/completo/back-end/python-scraper"
        );
    }

    /** Resolve o caminho baseado na localizacao do .class compilado. */
    private Path resolverPorClasse() {
        try {
            Path classesDir = Paths.get(
                    getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
            );
            // classesDir pode ser .../target/classes ou .../back-end.jar
            // Sobe ate encontrar o diretorio pai que contem python-scraper
            Path dir = classesDir;
            for (int i = 0; i < 4; i++) {
                dir = dir.getParent();
                if (dir == null) break;
                Path candidato = dir.resolve(scraperDir);
                if (Files.isDirectory(candidato)) return candidato;
            }
        } catch (Exception ignored) {}
        return Paths.get(scraperDir); // fallback
    }

    /**
     * Resolve o executavel Python testando nomes comuns no PATH.
     * Ordem de preferencia: python.scraper.executable config → python → py → python3
     */
    private String resolverPythonExecutavel() {
        List<String> candidatos = List.of(
                pythonExecutable,  // valor do application.properties (default: "python")
                "python",
                "py",
                "python3"
        );

        for (String exe : candidatos) {
            try {
                ProcessBuilder teste = new ProcessBuilder(exe, "--version")
                        .redirectErrorStream(true);
                Process p = teste.start();
                int exit = p.waitFor();
                if (exit == 0) {
                    log.info("[PythonProcessManager] Python encontrado: " + exe);
                    return exe;
                }
            } catch (Exception ignored) {}
        }

        // Tenta o caminho completo padrao do Windows
        String fullPath = "C:\\Users\\" + System.getProperty("user.name") +
                "\\AppData\\Local\\Programs\\Python\\Python314\\python.exe";
        if (Files.exists(Paths.get(fullPath))) {
            log.info("[PythonProcessManager] Python encontrado via caminho padrao: " + fullPath);
            return fullPath;
        }

        throw new RuntimeException(
                "Python nao encontrado no PATH. Candidatos testados: " + candidatos + "\n" +
                "Solucao: configure python.scraper.executable com o caminho completo no application.properties.\n" +
                "  Exemplo: python.scraper.executable=C:/Users/SEU_USUARIO/AppData/Local/Programs/Python/Python314/python.exe"
        );
    }

    private void aguardarPronto() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        String healthUrl = pythonApiUrl + "/health";
        long inicio = System.currentTimeMillis();
        long timeoutMs = startupTimeoutSeconds * 1000L;

        log.info("[PythonProcessManager] Aguardando Python ficar pronto em " + healthUrl);

        while (System.currentTimeMillis() - inicio < timeoutMs) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(healthUrl))
                        .GET()
                        .timeout(Duration.ofSeconds(2))
                        .build();

                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    log.info("[PythonProcessManager] Python pronto ✓");
                    return;
                }
            } catch (Exception ignored) {
                // Ainda não está pronto — tenta novamente
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        throw new RuntimeException(
                "Python scraper não ficou pronto após " + startupTimeoutSeconds + " segundos."
        );
    }

    @PreDestroy
    public void parar() {
        if (estaRodando()) {
            log.info("[PythonProcessManager] Encerrando processo Python (PID: " + processo.pid() + ")");
            processo.destroy();
            try {
                processo.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                processo.destroyForcibly();
            }
            log.info("[PythonProcessManager] Processo Python encerrado.");
        }
    }
}





