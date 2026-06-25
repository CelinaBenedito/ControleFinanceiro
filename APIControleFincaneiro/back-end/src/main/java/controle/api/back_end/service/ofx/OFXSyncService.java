package controle.api.back_end.service.ofx;

import com.fasterxml.jackson.databind.ObjectMapper;
import controle.api.back_end.dto.ofx.in.OFXInstituicaoSyncDTO;
import controle.api.back_end.dto.ofx.in.NavigationStepDTO;
import controle.api.back_end.dto.ofx.in.OFXSyncRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Chama a API Python (FastAPI) para acionar o scraping bancário.
 *
 * <p>As credenciais do usuário são repassadas dentro de {@code navigation_steps}
 * como texto nos steps do tipo {@code fill}. Elas nunca são persistidas.
 */
@Service
public class OFXSyncService {

    private static final Logger log = Logger.getLogger(OFXSyncService.class.getName());

    @Value("${python.api.url:http://127.0.0.1:8000}")
    private String pythonApiUrl;

    private final PythonProcessManager pythonProcessManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OFXSyncService(PythonProcessManager pythonProcessManager) {
        this.pythonProcessManager = pythonProcessManager;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Dispara a captura OFX para a instituição especificada (versão OFXSyncRequestDTO).
     */
    public String dispararCaptura(OFXSyncRequestDTO req) {
        return dispararCaptura(req.bankUrl(), req.navigationSteps(),
                req.description() != null ? req.description() : "Sync automatico");
    }

    /**
     * Dispara a captura OFX para uma entrada de lote (OFXInstituicaoSyncDTO).
     */
    public String dispararCaptura(OFXInstituicaoSyncDTO inst) {
        return dispararCaptura(inst.bankUrl(), inst.navigationSteps(),
                inst.descricao() != null ? inst.descricao() : "Sync automatico lote");
    }

    /**
     * Implementacao interna comum a todos os overloads.
     */
    private String dispararCaptura(String bankUrl, java.util.List<NavigationStepDTO> steps, String description) {
        pythonProcessManager.garantirRodando();

        Map<String, Object> body = new HashMap<>();
        body.put("bank_url", bankUrl);
        body.put("description", description);

        if (steps != null && !steps.isEmpty()) {
            body.put("navigation_steps", steps.stream().map(this::stepParaMap).toList());
        } else {
            body.put("navigation_steps", null);
        }

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            log.info("[OFXSyncService] Chamando Python /capture para: " + bankUrl);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(pythonApiUrl + "/capture"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofMinutes(10))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Python retornou status " + response.statusCode() +
                        ": " + response.body());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);

            Boolean success = (Boolean) responseMap.get("success");
            if (Boolean.FALSE.equals(success)) {
                throw new RuntimeException("Captura OFX falhou: " + responseMap.get("message"));
            }

            String fileName = (String) responseMap.get("file_name");
            log.info("[OFXSyncService] OFX gerado: " + fileName);
            return fileName;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao comunicar com o Python scraper.", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> stepParaMap(NavigationStepDTO step) {
        Map<String, Object> m = new HashMap<>();
        m.put("action",   step.action());
        m.put("selector", step.selector());
        m.put("text",     step.text());
        m.put("timeout",  step.timeout());
        return m;
    }
}



