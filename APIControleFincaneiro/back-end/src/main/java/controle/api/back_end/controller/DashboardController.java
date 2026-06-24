package controle.api.back_end.controller;

import controle.api.back_end.dto.dashboard.out.*;
import controle.api.back_end.dto.registros.out.RegistroResponseDto;
import controle.api.back_end.model.dashboard.TipoPeriodo;
import controle.api.back_end.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints do Dashboard.
 *
 * <h3>Query params comuns a todos os endpoints</h3>
 * <pre>
 *   periodo   MENSAL | TRIMESTRAL | SEMESTRAL | ANUAL   (obrigatório)
 *   ano       inteiro, ex: 2025                         (obrigatório)
 *   mes       1-12   → obrigatório se periodo=MENSAL
 *   trimestre 1-4    → obrigatório se periodo=TRIMESTRAL
 *   semestre  1-2    → obrigatório se periodo=SEMESTRAL
 * </pre>
 *
 * <h3>Exemplos</h3>
 * <pre>
 *   ?periodo=MENSAL&amp;ano=2025&amp;mes=6
 *   ?periodo=TRIMESTRAL&amp;ano=2025&amp;trimestre=2
 *   ?periodo=SEMESTRAL&amp;ano=2025&amp;semestre=1
 *   ?periodo=ANUAL&amp;ano=2025
 * </pre>
 */
@CrossOrigin
@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard",
        description = "KPIs e gráficos financeiros. Todos os endpoints aceitam os query params: " +
                "'periodo' (MENSAL|TRIMESTRAL|SEMESTRAL|ANUAL), 'ano', e opcionalmente " +
                "'mes', 'trimestre' ou 'semestre'.")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // =======================================================================
    //  KPIs
    // =======================================================================

    @GetMapping("/kpi/saldo-total/usuarios/{user_id}")
    @Operation(summary = "KPI — Saldo total do período",
            description = "Saldo acumulado de toda a história financeira do usuário até o final " +
                    "do período selecionado (ou até hoje se o período estiver em aberto).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SaldoTotalDto.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetros de período inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",          content = @Content)
    })
    public ResponseEntity<SaldoTotalDto> getSaldoTotal(
            @PathVariable UUID user_id,
            @RequestParam TipoPeriodo periodo,
            @RequestParam Integer ano,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer trimestre,
            @RequestParam(required = false) Integer semestre) {
        return ResponseEntity.ok(
                dashboardService.getSaldoTotal(periodo, ano, mes, trimestre, semestre, user_id));
    }

    @GetMapping("/kpi/gasto-total/usuarios/{user_id}")
    @Operation(summary = "KPI — Gasto total do período",
            description = "Soma de todos os gastos e transferências no período, com variação " +
                    "percentual em relação ao período anterior do mesmo tipo. " +
                    "Percentual positivo = gastou mais; negativo = gastou menos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = GastoTotalDto.class))),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "404", content = @Content)
    })
    public ResponseEntity<GastoTotalDto> getGastoTotal(
            @PathVariable UUID user_id,
            @RequestParam TipoPeriodo periodo,
            @RequestParam Integer ano,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer trimestre,
            @RequestParam(required = false) Integer semestre) {
        return ResponseEntity.ok(
                dashboardService.getGastoTotal(periodo, ano, mes, trimestre, semestre, user_id));
    }

    @GetMapping("/kpi/maior-gasto/usuarios/{user_id}")
    @Operation(summary = "KPI — Maior gasto do período",
            description = "Evento de maior valor (tipo Gasto) dentro do período, com título, " +
                    "categoria, valor e percentual que representa sobre o total de gastos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = MaiorGastoDto.class))),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "404", content = @Content)
    })
    public ResponseEntity<MaiorGastoDto> getMaiorGasto(
            @PathVariable UUID user_id,
            @RequestParam TipoPeriodo periodo,
            @RequestParam Integer ano,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer trimestre,
            @RequestParam(required = false) Integer semestre) {
        return ResponseEntity.ok(
                dashboardService.getMaiorGasto(periodo, ano, mes, trimestre, semestre, user_id));
    }

    @GetMapping("/kpi/categoria-impacto/usuarios/{user_id}")
    @Operation(summary = "KPI — Categoria que mais impactou",
            description = "Categoria com maior valor de gastos no período e variação percentual " +
                    "em relação ao período anterior.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = CategoriaImpactoDto.class))),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "404", content = @Content)
    })
    public ResponseEntity<CategoriaImpactoDto> getCategoriaImpacto(
            @PathVariable UUID user_id,
            @RequestParam TipoPeriodo periodo,
            @RequestParam Integer ano,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer trimestre,
            @RequestParam(required = false) Integer semestre) {
        return ResponseEntity.ok(
                dashboardService.getCategoriaImpacto(periodo, ano, mes, trimestre, semestre, user_id));
    }

    // =======================================================================
    //  GRÁFICOS
    // =======================================================================

    @GetMapping("/grafico/evolucao-gastos/usuarios/{user_id}")
    @Operation(summary = "Gráfico — Evolução dos gastos (line chart)",
            description = "Série temporal de gastos no período. " +
                    "Granularidade: DIARIO (mensal/trimestral), SEMANAL (semestral), MENSAL (anual).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = EvolucaoGastosDto.class))),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "404", content = @Content)
    })
    public ResponseEntity<EvolucaoGastosDto> getEvolucaoGastos(
            @PathVariable UUID user_id,
            @RequestParam TipoPeriodo periodo,
            @RequestParam Integer ano,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer trimestre,
            @RequestParam(required = false) Integer semestre) {
        return ResponseEntity.ok(
                dashboardService.getEvolucaoGastos(periodo, ano, mes, trimestre, semestre, user_id));
    }

    @GetMapping("/grafico/categorias/usuarios/{user_id}")
    @Operation(summary = "Gráfico — Gastos por categoria (stacked bar)",
            description = "Para cada categoria: valor total, percentual sobre o total e número de ocorrências. " +
                    "Ordenado do maior para o menor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = CategoriasGraficoDto.class))),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "404", content = @Content)
    })
    public ResponseEntity<CategoriasGraficoDto> getCategoriasGrafico(
            @PathVariable UUID user_id,
            @RequestParam TipoPeriodo periodo,
            @RequestParam Integer ano,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer trimestre,
            @RequestParam(required = false) Integer semestre) {
        return ResponseEntity.ok(
                dashboardService.getCategoriasGrafico(periodo, ano, mes, trimestre, semestre, user_id));
    }

    @GetMapping("/grafico/comparacao-periodo/usuarios/{user_id}")
    @Operation(summary = "Gráfico — Comparação de gastos por período (multiple line chart)",
            description = "Gastos diários do período atual versus o período imediatamente anterior do mesmo tipo. " +
                    "Ambas as séries são normalizadas por posição (Dia 1, Dia 2, … Dia N).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ComparacaoPeriodoDto.class))),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "404", content = @Content)
    })
    public ResponseEntity<ComparacaoPeriodoDto> getComparacaoPeriodo(
            @PathVariable UUID user_id,
            @RequestParam TipoPeriodo periodo,
            @RequestParam Integer ano,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer trimestre,
            @RequestParam(required = false) Integer semestre) {
        return ResponseEntity.ok(
                dashboardService.getComparacaoPeriodo(periodo, ano, mes, trimestre, semestre, user_id));
    }

    @GetMapping("/grafico/gastos-dia-semana/usuarios/{user_id}")
    @Operation(summary = "Gráfico — Gastos por dia da semana (heat map)",
            description = "Valor total gasto em cada dia da semana no período. " +
                    "O campo 'normalizado' varia de 0.0 a 1.0 (onde 1.0 = dia com maior gasto).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = HeatMapDiaSemanaDto.class))),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "404", content = @Content)
    })
    public ResponseEntity<HeatMapDiaSemanaDto> getHeatMapDiaSemana(
            @PathVariable UUID user_id,
            @RequestParam TipoPeriodo periodo,
            @RequestParam Integer ano,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer trimestre,
            @RequestParam(required = false) Integer semestre) {
        return ResponseEntity.ok(
                dashboardService.getHeatMapDiaSemana(periodo, ano, mes, trimestre, semestre, user_id));
    }

    @GetMapping("/grafico/fluxo-financeiro/usuarios/{user_id}")
    @Operation(summary = "Gráfico — Fluxo de entradas e saídas (Sankey)",
            description = "Representa o caminho do dinheiro: Entrada → Instituição → Categoria/Transferência/Poupança. " +
                    "Retorna listas de nós e links prontos para renderização de diagrama Sankey.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = FluxoFinanceiroDto.class))),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "404", content = @Content)
    })
    public ResponseEntity<FluxoFinanceiroDto> getFluxoFinanceiro(
            @PathVariable UUID user_id,
            @RequestParam TipoPeriodo periodo,
            @RequestParam Integer ano,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer trimestre,
            @RequestParam(required = false) Integer semestre) {
        return ResponseEntity.ok(
                dashboardService.getFluxoFinanceiro(periodo, ano, mes, trimestre, semestre, user_id));
    }

    // =======================================================================
    //  REGISTROS DO PERÍODO
    // =======================================================================

    @GetMapping("/registros/usuarios/{user_id}")
    @Operation(summary = "Registros do período",
            description = "Lista completa de eventos financeiros no período solicitado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = RegistroResponseDto.class))),
            @ApiResponse(responseCode = "204", description = "Nenhum registro no período", content = @Content),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "404", content = @Content)
    })
    public ResponseEntity<List<RegistroResponseDto>> getRegistrosPorPeriodo(
            @PathVariable UUID user_id,
            @RequestParam TipoPeriodo periodo,
            @RequestParam Integer ano,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer trimestre,
            @RequestParam(required = false) Integer semestre) {
        List<RegistroResponseDto> resultado = dashboardService.getRegistrosPorPeriodo(
                periodo, ano, mes, trimestre, semestre, user_id);
        return resultado.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(resultado);
    }
}
