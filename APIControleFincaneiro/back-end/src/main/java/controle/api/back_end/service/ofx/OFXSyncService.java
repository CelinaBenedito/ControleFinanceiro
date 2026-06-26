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
        return dispararCaptura(
                "/capture",
                req.bankUrl(), req.navigationSteps(),
                req.description() != null ? req.description() : "Sync automatico"
        );
    }

    /**
     * Dispara a captura OFX para uma entrada de lote.
     * Usa o pythonEndpoint da instituicao (ex: /capture/alelo para o Alelo).
     */
    public String dispararCaptura(OFXInstituicaoSyncDTO inst) {
        String endpoint = inst.pythonEndpoint() != null ? inst.pythonEndpoint() : "/capture";
        return dispararCaptura(
                endpoint,
                inst.bankUrl(), inst.navigationSteps(),
                inst.descricao() != null ? inst.descricao() : "Sync automatico lote"
        );
    }

    /**
     * Implementacao interna.
     * Para endpoints especializados (/capture/alelo, /capture/nubank/sync),
     * o body e adaptado para o schema esperado por cada endpoint Python.
     */
    private String dispararCaptura(String pythonEndpoint, String bankUrl,
                                   java.util.List<NavigationStepDTO> steps, String description) {
        pythonProcessManager.garantirRodando();

        // Endpoints especializados usam schema proprio (cpf/senha no body)
        // O OFXInstituicaoSyncDTO ja trata isso — aqui apenas passamos o que veio
        Map<String, Object> body = new HashMap<>();

        if (pythonEndpoint.startsWith("/capture/alelo")) {
            // Alelo: cpf e senha extraidos dos navigationSteps (tipo fill com placeholders)
            body.put("cpf",   extrairPlaceholderDeStep(steps, "{{CPF}}"));
            body.put("senha", extrairPlaceholderDeStep(steps, "{{SENHA}}"));
        } else if (pythonEndpoint.startsWith("/capture/nubank")) {
            body.put("userId", description);
            body.put("cpf",   extrairPlaceholderDeStep(steps, "{{CPF}}"));
            body.put("senha", extrairPlaceholderDeStep(steps, "{{SENHA}}"));
        } else {
            // Endpoint padrao /capture
            body.put("bank_url", bankUrl);
            body.put("description", description);
            if (steps != null && !steps.isEmpty()) {
                body.put("navigation_steps", steps.stream().map(this::stepParaMap).toList());
            } else {
                body.put("navigation_steps", null);
            }
        }

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            log.info("[OFXSyncService] Chamando Python " + pythonEndpoint);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(pythonApiUrl + pythonEndpoint))
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

    /**
     * Extrai o valor de um placeholder dos steps.
     * O front-end envia steps de fill com selector="{{CPF}}" e text="12345678900" (valor real).
     * Este metodo encontra o step pelo selector (placeholder) e retorna o text (valor real).
     * Usado para endpoints que recebem cpf/senha diretamente (Alelo, Nubank).
     */
    private String extrairPlaceholderDeStep(java.util.List<NavigationStepDTO> steps, String placeholder) {
        if (steps == null) return "";
        return steps.stream()
                .filter(s -> "fill".equals(s.action()) && placeholder.equals(s.selector()))
                .map(NavigationStepDTO::text)
                .findFirst()
                .orElse("");
    }
}



