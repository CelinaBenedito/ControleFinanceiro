package controle.api.back_end.service.ofx;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Monitora a pasta uploads/ofx com {@link WatchService}.
 *
 * <p>Quando o Python salva um arquivo .ofx, este serviço detecta a criação
 * e notifica os listeners registrados. O processamento (importação) é
 * realizado de forma assíncrona para não bloquear o watcher.
 *
 * <p>Em vez de processar automaticamente (o que exigiria saber o usuário e
 * instituição), o watcher armazena o arquivo detectado e disponibiliza via
 * {@link #aguardarArquivo} para que o {@link OFXSyncService} possa recuperá-lo
 * após disparar a captura.
 */
@Service
public class OFXWatcherService {

    private static final Logger log = Logger.getLogger(OFXWatcherService.class.getName());

    @Value("${ofx.output.dir:uploads/ofx}")
    private String ofxOutputDir;

    private WatchService watchService;
    private ExecutorService executor;

    /**
     * Fila de arquivos detectados. O {@link OFXSyncService} faz poll() aqui
     * para saber qual arquivo o Python acabou de gerar.
     */
    private final LinkedBlockingQueue<String> arquivosDetectados = new LinkedBlockingQueue<>();

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @PostConstruct
    public void iniciar() {
        // Executa em thread separada para nao bloquear o startup do Spring
        new Thread(() -> {
            try {
                Path pasta = Paths.get(ofxOutputDir).toAbsolutePath();
                pasta.toFile().mkdirs();

                watchService = FileSystems.getDefault().newWatchService();
                pasta.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

                executor = Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "ofx-watcher");
                    t.setDaemon(true);
                    return t;
                });

                executor.submit(this::loop);
                log.info("[OFXWatcherService] Monitorando pasta: " + pasta);

            } catch (IOException e) {
                log.warning("[OFXWatcherService] Nao foi possivel iniciar o watcher: " + e.getMessage());
            }
        }, "ofx-watcher-init").start();
    }

    @PreDestroy
    public void parar() {
        if (executor != null) executor.shutdownNow();
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {}
        }
        log.info("[OFXWatcherService] Watcher encerrado.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Aguarda até {@code timeoutSeconds} segundos pela criação de um arquivo .ofx.
     *
     * @return nome do arquivo detectado, ou null se timeout expirar.
     */
    public String aguardarArquivo(int timeoutSeconds) {
        try {
            return arquivosDetectados.poll(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Loop interno do WatchService
    // ─────────────────────────────────────────────────────────────────────────

    private void loop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                WatchKey key = watchService.take();  // Bloqueia até haver evento

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    String nomeArquivo = ev.context().getFileName().toString();

                    if (nomeArquivo.endsWith(".ofx")) {
                        log.info("[OFXWatcherService] Novo arquivo OFX detectado: " + nomeArquivo);
                        arquivosDetectados.offer(nomeArquivo);
                    }
                }

                if (!key.reset()) {
                    log.warning("[OFXWatcherService] WatchKey inválida — pasta pode ter sido removida.");
                    break;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ClosedWatchServiceException e) {
                break;
            }
        }
    }
}


