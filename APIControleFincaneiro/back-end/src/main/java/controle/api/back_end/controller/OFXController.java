package controle.api.back_end.controller;

import controle.api.back_end.dto.ofx.in.OFXInstituicaoSyncDTO;
import controle.api.back_end.dto.ofx.in.OFXSyncLoteRequestDTO;
import controle.api.back_end.dto.ofx.in.OFXSyncRequestDTO;
import controle.api.back_end.dto.ofx.out.*;
import controle.api.back_end.service.ofx.OFXImportService;
import controle.api.back_end.service.ofx.OFXSyncService;
import controle.api.back_end.service.ofx.OFXWatcherService;
import controle.api.back_end.service.ofx.PythonProcessManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Endpoints REST para integração com o Python OFX Scraper.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * RESPONSABILIDADES POR EQUIPE
 * ═══════════════════════════════════════════════════════════════════════
 *
 * EQUIPE BACK-END (Java — este arquivo):
 *   - Receber os requests do frontend
 *   - Orquestrar Python, WatcherService e ImportService
 *   - Retornar resultados estruturados
 *
 * EQUIPE FRONT-END / JS (a implementar):
 *   1. Detectar qual usuario esta ativo (login/troca de perfil)
 *   2. Aplicar debounce: aguardar X segundos para confirmar que o usuario
 *      permaneceu no perfil antes de iniciar a sincronizacao
 *   3. Ler credenciais bancarias do localStorage (ou outro mecanismo seguro)
 *   4. Montar o array navigationSteps para cada instituicao
 *   5. Chamar os endpoints abaixo na ordem correta:
 *      a) POST /ofx/sessao/iniciar   (pre-aquece o Python)
 *      b) POST /ofx/sync/usuario/{userId}  (sincroniza todas as instituicoes)
 *
 * ═══════════════════════════════════════════════════════════════════════
 * FLUXO ESPERADO (JS orquestra, Java executa)
 * ═══════════════════════════════════════════════════════════════════════
 *
 *  [usuario seleciona perfil]
 *       ↓
 *  [JS aguarda debounce, ex: 5s]
 *       ↓
 *  POST /ofx/sessao/iniciar   ← pre-aquece o Python
 *       ↓
 *  POST /ofx/sync/usuario/{userId}  ← envia credenciais do localStorage
 *       ↓
 *  Java processa cada instituicao sequencialmente
 *       ↓
 *  Resposta com resumo por instituicao
 */
@CrossOrigin
@RestController
@RequestMapping("/ofx")
@Tag(name = "OFX Sync", description = "Integracao com o Python scraper para captura automatica de OFX bancario")
public class OFXController {

    private static final Logger log = Logger.getLogger(OFXController.class.getName());

    @Value("${ofx.watcher.timeout.seconds:600}")
    private int watcherTimeoutSeconds;

    @Value("${python.api.url:http://127.0.0.1:8000}")
    private String pythonApiUrl;

    @Value("${python.scraper.dir:python-scraper}")
    private String scraperDir;

    @Value("${python.scraper.executable:python}")
    private String pythonExecutable;

    private final OFXSyncService syncService;
    private final OFXWatcherService watcherService;
    private final OFXImportService importService;
    private final PythonProcessManager pythonProcessManager;

    public OFXController(OFXSyncService syncService,
                         OFXWatcherService watcherService,
                         OFXImportService importService,
                         PythonProcessManager pythonProcessManager) {
        this.syncService          = syncService;
        this.watcherService       = watcherService;
        this.importService        = importService;
        this.pythonProcessManager = pythonProcessManager;
    }

    // =========================================================================
    // DIAGNOSTICO — apenas para desenvolvimento/debug
    // =========================================================================

    /**
     * Retorna informacoes de ambiente para diagnosticar problemas de configuracao.
     * Use quando o /sessao/iniciar retornar 500.
     */
    @GetMapping("/diagnostico")
    @Operation(
            summary = "Diagnostico de ambiente",
            description = "Retorna o user.dir, caminho do scraper, Python encontrado etc. Use para debugar erros de configuracao."
    )
    public ResponseEntity<Map<String, Object>> diagnostico() {
        Map<String, Object> info = new LinkedHashMap<>();
        String cwd = System.getProperty("user.dir");
        info.put("user.dir (JVM)", cwd);
        info.put("python.scraper.dir (config)", scraperDir);
        info.put("python.scraper.executable (config)", pythonExecutable);
        info.put("python.api.url (config)", pythonApiUrl);

        // Mostra TODOS os candidatos que o PythonProcessManager vai tentar
        var cwdPath = Paths.get(cwd);
        var candidatos = java.util.List.of(
                cwdPath.resolve(scraperDir).toAbsolutePath(),
                cwdPath.getParent() != null
                        ? cwdPath.getParent().resolve("back-end").resolve(scraperDir).toAbsolutePath()
                        : cwdPath.resolve(scraperDir).toAbsolutePath(),
                cwdPath.resolve("back-end").resolve(scraperDir).toAbsolutePath()
        );

        Map<String, Object> paths = new LinkedHashMap<>();
        for (int i = 0; i < candidatos.size(); i++) {
            var c = candidatos.get(i);
            boolean dirOk    = Files.isDirectory(c);
            boolean mainPyOk = Files.exists(c.resolve("src/main.py"));
            paths.put("candidato " + (i + 1),
                    c + " | dir=" + dirOk + " | main.py=" + mainPyOk
                            + (dirOk && mainPyOk ? " ✓ SERA USADO" : ""));
        }
        info.put("candidatos de caminho", paths);

        // Verifica Python
        for (String exe : List.of("python", "py", "python3")) {
            try {
                Process p = new ProcessBuilder(exe, "--version").redirectErrorStream(true).start();
                String version = new String(p.getInputStream().readAllBytes()).trim();
                p.waitFor();
                info.put("python '" + exe + "'", version.isEmpty() ? "encontrado" : version);
            } catch (Exception e) {
                info.put("python '" + exe + "'", "nao encontrado: " + e.getMessage());
            }
        }

        info.put("python rodando agora?", pythonProcessManager.estaRodando());
        return ResponseEntity.ok(info);
    }

    // =========================================================================
    // SESSAO — gerenciamento do processo Python
    // =========================================================================

    /**
     * Inicia (pre-aquece) o processo Python.
     *
     * O JS deve chamar este endpoint logo apos o debounce do usuario,
     * para que o Python ja esteja pronto quando a sincronizacao comecar.
     * Se o Python ja estiver rodando, retorna imediatamente sem erro.
     */
    @PostMapping("/sessao/iniciar")
    @Operation(
            summary = "Iniciar sessao do scraper Python",
            description = """
                    Pre-aquece o processo Python (FastAPI + Playwright).

                    **Quando chamar (responsabilidade do JS):**
                    - Apos o debounce: quando o usuario ficar X segundos no mesmo perfil
                    - Antes de chamar POST /ofx/sync/usuario/{userId}

                    Se o Python ja estiver rodando, retorna 200 sem fazer nada.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Python iniciado ou ja estava rodando"),
            @ApiResponse(responseCode = "500", description = "Nao foi possivel iniciar o Python")
    })
    public ResponseEntity<OFXSessaoStatusDTO> iniciarSessao() {
        try {
            pythonProcessManager.iniciarExplicitamente();
            return ResponseEntity.ok(new OFXSessaoStatusDTO(
                    true,
                    pythonProcessManager.getMensagemStatus(),
                    pythonApiUrl
            ));
        } catch (Exception e) {
            log.severe("[OFXController] Falha ao iniciar Python: " + e.getMessage());
            return ResponseEntity.internalServerError().body(new OFXSessaoStatusDTO(
                    false,
                    "Falha ao iniciar Python: " + e.getMessage(),
                    pythonApiUrl
            ));
        }
    }

    /**
     * Retorna o status atual do processo Python.
     * O JS pode usar para mostrar indicador de "sincronizando..." na UI.
     */
    @GetMapping("/sessao/status")
    @Operation(
            summary = "Status do scraper Python",
            description = "Verifica se o processo Python esta rodando. Use para mostrar indicador na UI."
    )
    public ResponseEntity<OFXSessaoStatusDTO> statusSessao() {
        return ResponseEntity.ok(new OFXSessaoStatusDTO(
                pythonProcessManager.estaRodando(),
                pythonProcessManager.getMensagemStatus(),
                pythonApiUrl
        ));
    }

    // =========================================================================
    // SYNC — sincronizacao de OFX
    // =========================================================================

    /**
     * Sincroniza OFX de TODAS as instituicoes de um usuario em uma unica chamada.
     *
     * <p>Este e o endpoint principal que o JS deve chamar apos o debounce.
     * As credenciais de cada banco devem vir do localStorage e ser embutidas
     * nos navigationSteps de cada instituicao.
     *
     * <p>As instituicoes sao processadas sequencialmente para evitar
     * multiplas janelas de browser abertas ao mesmo tempo.
     */
    @PostMapping("/sync/usuario/{userId}")
    @Operation(
            summary = "Sincronizar OFX de todas as instituicoes do usuario",
            description = """
                    Processa todas as instituicoes financeiras do usuario em sequencia.

                    **Responsabilidade do JS ao chamar este endpoint:**
                    1. Ler credenciais do localStorage para cada instituicao
                    2. Montar o array navigationSteps com os passos de login + download
                    3. Enviar o payload com todas as instituicoes

                    **Estrutura de navigationSteps:**
                    ```json
                    [
                      { "action": "fill", "selector": "input[name='cpf']", "text": "credencial_do_localstorage" },
                      { "action": "fill", "selector": "input[name='senha']", "text": "senha_do_localstorage" },
                      { "action": "click", "selector": "button[type='submit']" },
                      { "action": "wait_for_selector", "selector": "a[href*='ofx']", "timeout": 30000 },
                      { "action": "click", "selector": "a[href*='ofx']" }
                    ]
                    ```

                    **Credenciais:** trafegam apenas em memoria e nunca sao persistidas.

                    **navigationSteps null:** browser abre em modo manual (usuario faz login).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sincronizacao concluida (mesmo que parcialmente)"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "500", description = "Erro critico no processamento")
    })
    public ResponseEntity<OFXSyncLoteResponseDTO> sincronizarLote(
            @PathVariable UUID userId,
            @Valid @RequestBody OFXSyncLoteRequestDTO req) {

        log.info("[OFXController] Sincronizacao lote — usuario: " + userId +
                " | instituicoes: " + req.instituicoes().size());

        List<OFXInstituicaoResultadoDTO> resultados = new ArrayList<>();
        int totalImportadas = 0;
        int totalDuplicadas = 0;

        for (OFXInstituicaoSyncDTO inst : req.instituicoes()) {
            OFXInstituicaoResultadoDTO resultado = processarInstituicao(userId, inst);
            resultados.add(resultado);
            totalImportadas += resultado.transacoesImportadas();
            totalDuplicadas += resultado.transacoesDuplicadas();
        }

        boolean tudoOk = resultados.stream().allMatch(OFXInstituicaoResultadoDTO::sucesso);
        String mensagem = tudoOk
                ? "Todas as %d instituicoes sincronizadas com sucesso.".formatted(resultados.size())
                : "%d/%d instituicoes sincronizadas com sucesso.".formatted(
                        (int) resultados.stream().filter(OFXInstituicaoResultadoDTO::sucesso).count(),
                        resultados.size());

        return ResponseEntity.ok(new OFXSyncLoteResponseDTO(
                tudoOk, mensagem,
                resultados.size(), totalImportadas, totalDuplicadas,
                resultados
        ));
    }

    /**
     * Sincroniza OFX de uma unica instituicao.
     * Mantido para compatibilidade e testes individuais.
     */
    @PostMapping("/sync")
    @Operation(
            summary = "Sincronizar OFX de uma instituicao especifica",
            description = """
                    Versao de instituicao unica. Use POST /ofx/sync/usuario/{userId}
                    para sincronizar todas as instituicoes de uma vez.

                    As credenciais devem vir nos navigationSteps — nunca sao salvas.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OFX sincronizado"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "500", description = "Erro no scraping")
    })
    public ResponseEntity<OFXSyncResponseDTO> sincronizar(@Valid @RequestBody OFXSyncRequestDTO req) {
        log.info("[OFXController] Sync individual — usuario: " + req.userId());

        OFXInstituicaoSyncDTO inst = new OFXInstituicaoSyncDTO(
                req.instituicaoUsuarioId(),
                req.bankUrl(),
                req.navigationSteps(),
                req.description()
        );

        OFXInstituicaoResultadoDTO resultado = processarInstituicao(req.userId(), inst);

        return resultado.sucesso()
                ? ResponseEntity.ok(new OFXSyncResponseDTO(
                        true, resultado.mensagem(),
                        resultado.transacoesImportadas(),
                        resultado.transacoesDuplicadas(),
                        resultado.erros()))
                : ResponseEntity.internalServerError().body(new OFXSyncResponseDTO(
                        false, resultado.mensagem(), 0, 0, resultado.erros()));
    }

    // =========================================================================
    // Helpers internos
    // =========================================================================

    private OFXInstituicaoResultadoDTO processarInstituicao(UUID userId, OFXInstituicaoSyncDTO inst) {
        try {
            // 1. Dispara Python (inicia se necessario)
            syncService.dispararCaptura(inst);

            // 2. Aguarda o arquivo aparecer em uploads/ofx/
            String nomeArquivo = watcherService.aguardarArquivo(watcherTimeoutSeconds);
            if (nomeArquivo == null) {
                return new OFXInstituicaoResultadoDTO(
                        inst.instituicaoUsuarioId(), false,
                        "Timeout: nenhum OFX recebido em " + watcherTimeoutSeconds + "s",
                        0, 0, List.of("Timeout aguardando arquivo .ofx"));
            }

            // 3. Importa e deleta o arquivo
            OFXImportService.ResultadoImportacao res =
                    importService.importar(nomeArquivo, userId, inst.instituicaoUsuarioId());

            return new OFXInstituicaoResultadoDTO(
                    inst.instituicaoUsuarioId(), true,
                    "%d novas transacoes, %d duplicadas".formatted(res.importadas(), res.duplicadas()),
                    res.importadas(), res.duplicadas(), res.erros());

        } catch (Exception e) {
            log.warning("[OFXController] Erro na instituicao " + inst.instituicaoUsuarioId() +
                    ": " + e.getMessage());
            return new OFXInstituicaoResultadoDTO(
                    inst.instituicaoUsuarioId(), false,
                    "Erro: " + e.getMessage(),
                    0, 0, List.of(e.getMessage()));
        }
    }
}





