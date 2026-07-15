package controle.api.back_end.controller;

import controle.api.back_end.dto.poupanca.in.CaixinhaCreateDTO;
import controle.api.back_end.dto.poupanca.out.CaixinhaResponseDTO;
import controle.api.back_end.dto.poupanca.out.GraficoProgressoConsolidadoCaixinhaDto;
import controle.api.back_end.dto.poupanca.out.KpiProgressoGeralCaixinhaDto;
import controle.api.back_end.dto.poupanca.out.KpiRendimentoEstimadoMesCaixinhaDto;
import controle.api.back_end.dto.poupanca.out.KpiStatusCaixinhasDto;
import controle.api.back_end.dto.poupanca.out.KpiTotalAcumuladoCaixinhaDto;
import controle.api.back_end.service.CaixinhaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/caixinhas")
@Tag(name = "Caixinhas / Poupança",
     description = "Gerenciamento de caixinhas de poupança com metas, rendimentos (CDI/SELIC/Prefixado) " +
                   "e projeções financeiras. Suporta metas simples e compartilhadas entre múltiplas instituições.")
public class CaixinhaController {

    private final CaixinhaService caixinhaService;

    public CaixinhaController(CaixinhaService caixinhaService) {
        this.caixinhaService = caixinhaService;
    }

    // =========================================================================
    // CRIAR
    // =========================================================================

    @PostMapping
    @Operation(summary = "Criar uma nova caixinha",
               description = """
                       Cria uma caixinha de poupança com meta, prazo e rendimento configuráveis.
                       
                       **Tipos de rendimento:**
                       - `CDI` – percentual do CDI (ex.: 100% ou 110%). Informe `taxaReferenciaAtual` (CDI atual em % a.a.) e `percentualRendimento`.
                       - `SELIC` – percentual da SELIC. Informe `taxaReferenciaAtual` e `percentualRendimento`.
                       - `POUPANCA` – rendimento tradicional (70% da SELIC). Informe `taxaReferenciaAtual`.
                       - `PREFIXADO` – taxa fixa anual. Informe `taxaAnualPersonalizada` (ex.: 12.5 para 12,5% a.a.).
                       - `PERSONALIZADO` – taxa definida livremente. Informe `taxaAnualPersonalizada`.
                       
                       **Meta compartilhada:** informe `isCompartilhada=true` e 2 ou mais `instituicaoUsuarioIds`.
                       A API somará os aportes de todas as instituições para calcular o saldo total.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Caixinha criada com sucesso.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CaixinhaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário ou instituição não encontrados.", content = @Content)
    })
    public ResponseEntity<CaixinhaResponseDTO> criar(@Valid @RequestBody CaixinhaCreateDTO dto) {
        return ResponseEntity.status(201).body(caixinhaService.criar(dto));
    }

    // =========================================================================
    // CONSULTAS
    // =========================================================================

    @GetMapping("/usuarios/{user_id}")
    @Operation(summary = "Listar todas as caixinhas do usuário",
               description = "Retorna todas as caixinhas (ativas e encerradas) com saldo atual, " +
                             "projeções e aporte sugerido calculados em tempo real.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Caixinhas encontradas.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CaixinhaResponseDTO.class))),
            @ApiResponse(responseCode = "204", description = "Nenhuma caixinha cadastrada.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.", content = @Content)
    })
    public ResponseEntity<List<CaixinhaResponseDTO>> listarPorUsuario(@PathVariable UUID user_id) {
        List<CaixinhaResponseDTO> lista = caixinhaService.listarPorUsuario(user_id);
        return lista.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(lista);
    }

    @GetMapping("/ativas/usuarios/{user_id}")
    @Operation(summary = "Listar caixinhas ativas do usuário",
               description = "Retorna apenas as caixinhas em andamento (não encerradas).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Caixinhas encontradas.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CaixinhaResponseDTO.class))),
            @ApiResponse(responseCode = "204", description = "Nenhuma caixinha ativa.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.", content = @Content)
    })
    public ResponseEntity<List<CaixinhaResponseDTO>> listarAtivasPorUsuario(@PathVariable UUID user_id) {
        List<CaixinhaResponseDTO> lista = caixinhaService.listarAtivasPorUsuario(user_id);
        return lista.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(lista);
    }

    @GetMapping("/{caixinha_id}")
    @Operation(summary = "Buscar uma caixinha por ID",
               description = "Retorna os dados completos de uma caixinha com todos os cálculos atualizados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Caixinha encontrada.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CaixinhaResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Caixinha não encontrada.", content = @Content)
    })
    public ResponseEntity<CaixinhaResponseDTO> buscarPorId(@PathVariable UUID caixinha_id) {
        return ResponseEntity.ok(caixinhaService.buscarPorId(caixinha_id));
    }

    @GetMapping("/resumo/usuarios/{user_id}")
    @Operation(summary = "Total acumulado em poupança",
               description = "Soma o saldo atual de todas as caixinhas ativas do usuário. " +
                             "Útil para exibir um número único de 'total guardado' na tela inicial.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Total calculado.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = "4750.00"))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.", content = @Content)
    })
    public ResponseEntity<BigDecimal> resumoTotal(@PathVariable UUID user_id) {
        return ResponseEntity.ok(caixinhaService.resumoTotalPoupanca(user_id));
    }

    @GetMapping("/kpi/total-acumulado/usuarios/{user_id}")
    @Operation(summary = "KPI de total acumulado das caixinhas",
            description = "Retorna o total acumulado em todas as caixinhas ativas e a quantidade de caixinhas ativas do usuário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "KPI calculado com sucesso.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = KpiTotalAcumuladoCaixinhaDto.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.", content = @Content)
    })
    public ResponseEntity<KpiTotalAcumuladoCaixinhaDto> obterKpiTotalAcumulado(@PathVariable UUID user_id) {
        return ResponseEntity.ok(caixinhaService.obterKpiTotalAcumulado(user_id));
    }

    @GetMapping("/kpi/progresso-geral/usuarios/{user_id}")
    @Operation(summary = "KPI de progresso geral das caixinhas",
            description = "Retorna total acumulado, total de metas ativas e percentual geral de progresso. Se total de metas for zero, o percentual será zero.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "KPI calculado com sucesso.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = KpiProgressoGeralCaixinhaDto.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.", content = @Content)
    })
    public ResponseEntity<KpiProgressoGeralCaixinhaDto> obterKpiProgressoGeral(@PathVariable UUID user_id) {
        return ResponseEntity.ok(caixinhaService.obterKpiProgressoGeral(user_id));
    }

    @GetMapping("/kpi/rendimento-estimado-mes/usuarios/{user_id}")
    @Operation(summary = "KPI de rendimento estimado no mês",
            description = "Retorna a soma do rendimento estimado do mês atual em todas as caixinhas ativas, respeitando o tipo de taxa de cada caixinha.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "KPI calculado com sucesso.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = KpiRendimentoEstimadoMesCaixinhaDto.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.", content = @Content)
    })
    public ResponseEntity<KpiRendimentoEstimadoMesCaixinhaDto> obterKpiRendimentoEstimadoMes(@PathVariable UUID user_id) {
        return ResponseEntity.ok(caixinhaService.obterKpiRendimentoEstimadoMes(user_id));
    }

    @GetMapping("/kpi/status/usuarios/{user_id}")
    @Operation(summary = "KPI de status das caixinhas",
            description = "Retorna quantidade de caixinhas ativas e encerradas do usuário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "KPI calculado com sucesso.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = KpiStatusCaixinhasDto.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.", content = @Content)
    })
    public ResponseEntity<KpiStatusCaixinhasDto> obterKpiStatus(@PathVariable UUID user_id) {
        return ResponseEntity.ok(caixinhaService.obterKpiStatusCaixinhas(user_id));
    }

    @GetMapping("/grafico/progresso-consolidado/usuarios/{user_id}")
    @Operation(summary = "Gráfico de progresso consolidado das caixinhas",
            description = "Retorna percentual geral de progresso, total acumulado e total de metas para consumo no gráfico consolidado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados do gráfico calculados com sucesso.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = GraficoProgressoConsolidadoCaixinhaDto.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.", content = @Content)
    })
    public ResponseEntity<GraficoProgressoConsolidadoCaixinhaDto> obterGraficoProgressoConsolidado(@PathVariable UUID user_id) {
        return ResponseEntity.ok(caixinhaService.obterGraficoProgressoConsolidado(user_id));
    }

    // =========================================================================
    // EDITAR
    // =========================================================================

    @PutMapping("/{caixinha_id}")
    @Operation(summary = "Editar uma caixinha",
               description = "Atualiza nome, descrição, meta, prazo e/ou configuração de rendimento. " +
                             "Os aportes já realizados não são afetados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Caixinha atualizada.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CaixinhaResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Caixinha não encontrada.", content = @Content)
    })
    public ResponseEntity<CaixinhaResponseDTO> editar(@PathVariable UUID caixinha_id,
                                                       @RequestBody CaixinhaCreateDTO dto) {
        return ResponseEntity.ok(caixinhaService.editar(caixinha_id, dto));
    }

    @PatchMapping("/{caixinha_id}/encerrar")
    @Operation(summary = "Encerrar uma caixinha",
               description = "Marca a caixinha como encerrada (meta atingida ou cancelada pelo usuário). " +
                             "Os dados e histórico de aportes são mantidos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Caixinha encerrada.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CaixinhaResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Caixinha não encontrada.", content = @Content)
    })
    public ResponseEntity<CaixinhaResponseDTO> encerrar(@PathVariable UUID caixinha_id) {
        return ResponseEntity.ok(caixinhaService.encerrar(caixinha_id));
    }

    @PatchMapping("/{caixinha_id}/reabrir")
    @Operation(summary = "Reabrir uma caixinha encerrada",
               description = "Desfaz o encerramento, marcando a caixinha como ativa novamente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Caixinha reaberta.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CaixinhaResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Caixinha não encontrada.", content = @Content)
    })
    public ResponseEntity<CaixinhaResponseDTO> reabrir(@PathVariable UUID caixinha_id) {
        return ResponseEntity.ok(caixinhaService.reabrir(caixinha_id));
    }

    @PostMapping("/{caixinha_id}/instituicoes/{inst_id}")
    @Operation(summary = "Adicionar instituição a uma caixinha compartilhada",
               description = "Vincula uma nova instituição à caixinha, habilitando a meta compartilhada. " +
                             "O saldo total passa a somar os aportes de todas as instituições vinculadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Instituição adicionada.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CaixinhaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Instituição já vinculada.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Caixinha ou instituição não encontradas.", content = @Content)
    })
    public ResponseEntity<CaixinhaResponseDTO> adicionarInstituicao(@PathVariable UUID caixinha_id,
                                                                      @PathVariable Integer inst_id) {
        return ResponseEntity.ok(caixinhaService.adicionarInstituicao(caixinha_id, inst_id));
    }

    // =========================================================================
    // DELETAR
    // =========================================================================

    @DeleteMapping("/{caixinha_id}")
    @Operation(summary = "Deletar uma caixinha",
               description = "Remove permanentemente a caixinha e seus vínculos com instituições. " +
                             "Os aportes registrados como EventoFinanceiro NÃO são deletados.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Caixinha deletada.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Caixinha não encontrada.", content = @Content)
    })
    public ResponseEntity<Void> deletar(@PathVariable UUID caixinha_id) {
        caixinhaService.deletar(caixinha_id);
        return ResponseEntity.noContent().build();
    }
}

