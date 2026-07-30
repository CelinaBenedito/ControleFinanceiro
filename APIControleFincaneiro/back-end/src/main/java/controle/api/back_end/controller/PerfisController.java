package controle.api.back_end.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Endpoint para persistência local dos perfis salvos na tela inicial.
 *
 * Os perfis ficam em ~/.myfinance/perfis.json (arquivo local do usuário).
 * Isso resolve o problema do localStorage do JavaFX WebView não persistir
 * entre reinicializações do aplicativo desktop.
 */
@CrossOrigin
@RestController
@RequestMapping("/perfis")
public class PerfisController {

    private static final Path PERFIS_FILE = Path.of(
            System.getProperty("user.home"), ".myfinance", "perfis.json");

    /**
     * Retorna a lista de perfis salvos como string JSON.
     * Retorna "[]" se o arquivo ainda não existir.
     */
    @GetMapping
    public ResponseEntity<String> getPerfis() {
        try {
            if (!Files.exists(PERFIS_FILE)) {
                return ResponseEntity.ok("[]");
            }
            String content = Files.readString(PERFIS_FILE);
            return ResponseEntity.ok(content.isBlank() ? "[]" : content);
        } catch (Exception e) {
            System.out.println("[PerfisController] Erro ao ler perfis: " + e.getMessage());
            return ResponseEntity.ok("[]");
        }
    }

    /**
     * Salva a lista de perfis em disco.
     * Recebe {"perfis": "[...]"} no corpo da requisição.
     */
    @PostMapping
    public ResponseEntity<Void> savePerfis(@RequestBody Map<String, String> body) {
        try {
            String json = body.getOrDefault("perfis", "[]");
            Files.createDirectories(PERFIS_FILE.getParent());
            Files.writeString(PERFIS_FILE, json);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.out.println("[PerfisController] Erro ao salvar perfis: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}

