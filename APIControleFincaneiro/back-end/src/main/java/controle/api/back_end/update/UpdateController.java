package controle.api.back_end.update;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/update")
public class UpdateController {

    private final UpdateService updateService;

    public UpdateController(UpdateService updateService) {
        this.updateService = updateService;
    }

    /**
     * GET /api/update/check
     * Consulta o GitHub Releases e retorna se há uma versão mais nova disponível.
     */
    @GetMapping("/check")
    public ResponseEntity<?> checkUpdate() {
        try {
            UpdateInfo info = updateService.checkForUpdate();
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            System.err.println("[Update] Erro ao verificar atualizações: " + e.getMessage());
            // Retorna "sem atualização" em caso de falha (ex: sem internet)
            return ResponseEntity.ok(
                    new UpdateInfo(false, updateService.getCurrentVersion(),
                            updateService.getCurrentVersion(), null, null, null)
            );
        }
    }

    /**
     * POST /api/update/apply
     * Inicia o download e reinicialização da aplicação com a nova versão.
     * Body: { "downloadUrl": "https://..." }
     */
    @PostMapping("/apply")
    public ResponseEntity<?> applyUpdate(@RequestBody Map<String, String> body) {
        String downloadUrl = body.get("downloadUrl");
        if (downloadUrl == null || downloadUrl.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "O campo 'downloadUrl' é obrigatório."));
        }

        // Executa em thread separada para que a resposta HTTP seja enviada antes de o app fechar
        Thread updateThread = new Thread(() -> {
            try {
                Thread.sleep(800); // Aguarda a resposta ser despachada
                updateService.applyUpdate(downloadUrl);
            } catch (Exception e) {
                System.err.println("[Update] Falha ao aplicar atualização: " + e.getMessage());
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();

        return ResponseEntity.ok(Map.of(
                "status", "iniciado",
                "message", "Atualização em andamento. A aplicação será reiniciada em instantes."
        ));
    }
}

