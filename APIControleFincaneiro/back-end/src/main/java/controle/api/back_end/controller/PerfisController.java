package controle.api.back_end.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    @GetMapping(produces = "application/json")
    public ResponseEntity<String> getPerfis() {
        try {
            if (!Files.exists(PERFIS_FILE)) {
                return ResponseEntity.ok("[]");
            }
            String content = Files.readString(PERFIS_FILE);
            if (content.isBlank()) {
                return ResponseEntity.ok("[]");
            }
            // Valida que o conteúdo é um JSON array antes de retornar
            content = content.trim();
            if (!content.startsWith("[")) {
                System.out.println("[PerfisController] Conteúdo inválido no arquivo de perfis, retornando []");
                return ResponseEntity.ok("[]");
            }
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            System.out.println("[PerfisController] Erro ao ler perfis: " + e.getMessage());
            return ResponseEntity.ok("[]");
        }
    }

    /**
     * Salva a lista de perfis em disco.
     * Recebe {"perfis": "[...]"} no corpo da requisição.
     * Só grava se o JSON contiver dados reais (array não vazio), para evitar
     * sobrescrever perfis válidos com [] em caso de falha temporária.
     */
    @PostMapping
    public ResponseEntity<Void> savePerfis(@RequestBody Map<String, String> body) {
        try {
            String json = body.getOrDefault("perfis", "[]");
            if (json == null) json = "[]";
            json = json.trim();

            // Não sobrescreve o arquivo com array vazio se já houver dados salvos
            if ("[]".equals(json) || json.isEmpty()) {
                if (Files.exists(PERFIS_FILE)) {
                    String existente = Files.readString(PERFIS_FILE).trim();
                    if (!existente.isBlank() && !"[]".equals(existente) && existente.startsWith("[")) {
                        System.out.println("[PerfisController] Ignorando gravação de [] — arquivo já possui dados.");
                        return ResponseEntity.ok().build();
                    }
                }
            }

            Files.createDirectories(PERFIS_FILE.getParent());
            Files.writeString(PERFIS_FILE, json);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.out.println("[PerfisController] Erro ao salvar perfis: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}

