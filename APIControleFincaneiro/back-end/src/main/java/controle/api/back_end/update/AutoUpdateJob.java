package controle.api.back_end.update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Job agendado que verifica atualizações automaticamente.
 *
 * <ul>
 *   <li>Verificação à noite: Todos os dias às 02:00 (horário de menor uso)</li>
 *   <li>Verificação ao iniciar: 5 minutos após o startup da aplicação</li>
 *   <li>Limpeza automática: Remove pending-update.jar corrompido ou antigo</li>
 * </ul>
 */
@Component
public class AutoUpdateJob {

    private static final Logger log = LoggerFactory.getLogger(AutoUpdateJob.class);

    private final UpdateService updateService;

    @Value("${app.update.enabled:true}")
    private boolean updateEnabled;

    @Value("${app.update.auto-download:false}")
    private boolean autoDownload;

    public AutoUpdateJob(UpdateService updateService) {
        this.updateService = updateService;
    }

    /**
     * Executa 5 minutos após o startup para verificar se há atualização pendente
     * que não foi aplicada corretamente.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void verificarAtualizacaoPendenteNoStartup() {
        new Thread(() -> {
            try {
                Thread.sleep(5 * 60 * 1000); // 5 minutos
                verificarELimparPendingUpdate();
            } catch (Exception e) {
                log.error("[AutoUpdateJob] Erro na verificação de startup: {}", e.getMessage());
            }
        }).start();
    }

    /**
     * Verificação automática agendada: Todos os dias às 02:00 (horário de menor uso).
     *
     * Cron: "0 0 2 * * *" = segundo 0, minuto 0, hora 2, todos os dias
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void verificarAtualizacaoAutomatica() {
        if (!updateEnabled) {
            log.debug("[AutoUpdateJob] Verificação automática desabilitada");
            return;
        }

        log.info("[AutoUpdateJob] ========================================");
        log.info("[AutoUpdateJob] Iniciando verificação automática de atualização (02:00)");
        log.info("[AutoUpdateJob] ========================================");

        try {
            UpdateInfo info = updateService.checkForUpdate();

            if (!info.hasUpdate()) {
                log.info("[AutoUpdateJob] Sistema já está atualizado (versão {})", info.currentVersion());
                return;
            }

            log.info("[AutoUpdateJob] Nova versão disponível: {} -> {}",
                     info.currentVersion(), info.latestVersion());

            if (autoDownload && info.downloadUrl() != null && !info.downloadUrl().isBlank()) {
                log.info("[AutoUpdateJob] Auto-download ATIVADO. Iniciando download automático...");
                log.warn("[AutoUpdateJob] ATENÇÃO: A aplicação será reiniciada automaticamente!");

                // Aguarda 10 segundos antes de iniciar (permite cancelar se necessário)
                Thread.sleep(10000);

                updateService.applyUpdate(info.downloadUrl());
            } else {
                log.info("[AutoUpdateJob] Auto-download DESATIVADO. Notificação será exibida ao usuário.");
                log.info("[AutoUpdateJob] Para ativar download automático, defina: app.update.auto-download=true");
            }

        } catch (Exception e) {
            log.error("[AutoUpdateJob] Erro durante verificação automática: {}", e.getMessage(), e);
        }

        log.info("[AutoUpdateJob] ========================================");
        log.info("[AutoUpdateJob] Verificação automática concluída");
        log.info("[AutoUpdateJob] ========================================");
    }

    /**
     * Verifica e limpa pending-update.jar corrompido ou antigo.
     *
     * Remove o arquivo se:
     * - Tamanho menor que 10 MB (provavelmente corrompido)
     * - Mais de 7 dias desde a última modificação (usuário ignorou)
     */
    private void verificarELimparPendingUpdate() {
        try {
            Path myfinanceDir = Path.of(System.getProperty("user.home"), ".myfinance");
            Path pendingJar = myfinanceDir.resolve("pending-update.jar");

            if (!Files.exists(pendingJar)) {
                log.debug("[AutoUpdateJob] Nenhuma atualização pendente encontrada");
                return;
            }

            long size = Files.size(pendingJar);
            long ageMillis = System.currentTimeMillis() - Files.getLastModifiedTime(pendingJar).toMillis();
            long ageDays = ageMillis / (1000 * 60 * 60 * 24);

            log.warn("[AutoUpdateJob] Atualização pendente detectada:");
            log.warn("[AutoUpdateJob] - Tamanho: {} MB", String.format("%.2f", size / 1048576.0));
            log.warn("[AutoUpdateJob] - Idade: {} dias", ageDays);

            boolean shouldDelete = false;
            String reason = "";

            if (size < 10 * 1024 * 1024) {
                shouldDelete = true;
                reason = "tamanho inválido (< 10 MB)";
            } else if (ageDays > 7) {
                shouldDelete = true;
                reason = "arquivo antigo (> 7 dias)";
            }

            if (shouldDelete) {
                log.warn("[AutoUpdateJob] Removendo pending-update.jar: {}", reason);
                Files.delete(pendingJar);
                log.info("[AutoUpdateJob] Arquivo removido com sucesso");

                // Remove também o log de atualização antigo
                Path updateLog = myfinanceDir.resolve("update.log");
                if (Files.exists(updateLog)) {
                    Files.delete(updateLog);
                }
            } else {
                log.info("[AutoUpdateJob] Atualização pendente válida. Aguardando aplicação pelo usuário.");
            }

        } catch (Exception e) {
            log.error("[AutoUpdateJob] Erro ao verificar/limpar pending-update.jar: {}", e.getMessage());
        }
    }
}

