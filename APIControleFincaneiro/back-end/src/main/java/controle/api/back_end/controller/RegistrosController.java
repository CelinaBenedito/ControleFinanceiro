package controle.api.back_end.controller;

import controle.api.back_end.dto.registros.in.RegistroCompletoCreateDto;
import controle.api.back_end.dto.registros.mapper.RegistrosMapper;
import controle.api.back_end.dto.registros.out.RegistroResponseDto;
import controle.api.back_end.dto.registros.out.RegistroUsuarioResponseDto;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.eventoFinanceiro.*;
import controle.api.back_end.model.eventoFinanceiro.recorrenciaFinanceira.RecorrenciaFinanceira;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.service.RegistroExportacaoService;
import controle.api.back_end.service.RegistroService;
import controle.api.back_end.strategy.eventoFinanceiro.Registro;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/registros")
@Tag(name = "Registros",
     description = "Gerenciamento de eventos financeiros: gastos, recebimentos, transferências, " +
                   "poupança e empréstimos. Suporta múltiplas instituições (meios de pagamento) " +
                   "e múltiplas categorias por evento.")
public class RegistrosController {

    private final RegistroService registroService;
    private final RegistroExportacaoService exportacaoService;

    public RegistrosController(RegistroService registroService,
                               RegistroExportacaoService exportacaoService) {
        this.registroService  = registroService;
        this.exportacaoService = exportacaoService;
    }

    // =========================================================================
    // CONSULTAS
    // =========================================================================

    @GetMapping("/{user_id}")
    @Operation(summary = "Listar todos os registros do usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registros encontrados.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RegistroResponseDto.class))),
            @ApiResponse(responseCode = "204", description = "Nenhum registro encontrado.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.", content = @Content)
    })
    public ResponseEntity<List<RegistroResponseDto>> listarRegistros(@PathVariable UUID user_id) {
        List<EventoFinanceiro> eventos = registroService.getEventosFinanceirosByUser(user_id);

        if (eventos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        List<List<EventoInstituicao>> instituicoes = registroService.getEventosInstituicoesByEventoFinanceiro(eventos);
        List<EventoDetalhe> detalhes               = registroService.getGastosDetalhesByEventoFinanceiro(eventos);
        List<RegistroResponseDto> resposta         = RegistrosMapper.toResponse(eventos, instituicoes, detalhes);

        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/saldo-poupanca/usuarios/{user_id}")
    @Operation(summary = "Consultar saldo acumulado na poupança do usuário")
    public ResponseEntity<Double> getSaldoPoupanca(@PathVariable UUID user_id) {
        return ResponseEntity.ok(registroService.getSaldoPoupanca(user_id));
    }

    @GetMapping("/filtro/usuarios/{user_id}")
    @Operation(summary = "Buscar registros com filtros",
               description = "Todos os filtros são opcionais e podem ser combinados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = RegistroResponseDto.class))),
            @ApiResponse(responseCode = "204", description = "Nenhum registro para os filtros.", content = @Content),
            @ApiResponse(responseCode = "404", content = @Content)
    })
    public ResponseEntity<List<RegistroResponseDto>> buscarComFiltro(
            @PathVariable UUID user_id,
            @RequestParam(required = false) Double valor,
            @RequestParam(required = false) List<TipoMovimento> tipoMovimento,
            @RequestParam(required = false) List<Tipo> tipo,
            @RequestParam(required = false) LocalDate dataEvento,
            @RequestParam(required = false) List<InstituicaoUsuario> instituicaoUsuario,
            @RequestParam(required = false) List<CategoriaUsuario> categoriaUsuario,
            @RequestParam(required = false) String descricao,
            @RequestParam(required = false) String titulo) {

        List<RegistroResponseDto> resposta = registroService.getByFilter(
                user_id, valor, tipoMovimento, tipo, dataEvento,
                instituicaoUsuario, categoriaUsuario, descricao, titulo);

        return resposta.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(resposta);
    }

    // =========================================================================
    // EXPORTAÇÃO
    // =========================================================================

    @GetMapping("/download/{user_id}")
    @Operation(summary = "Exportar registros do usuário",
               description = "Gera um arquivo com todos os registros. Formatos: json | sql | excel | pdf")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Arquivo gerado.",
                    content = @Content(mediaType = "application/octet-stream",
                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "400", description = "Formato não suportado.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.", content = @Content)
    })
    public ResponseEntity<Resource> exportarRegistros(@PathVariable UUID user_id,
                                                       @RequestParam String tipo) {
        RegistroExportacaoService.ExportacaoResultado resultado = exportacaoService.exportar(user_id, tipo);
        ByteArrayResource arquivo = new ByteArrayResource(resultado.conteudo());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + resultado.nomeArquivo())
                .contentType(MediaType.parseMediaType(resultado.contentType()))
                .body(arquivo);
    }

    // =========================================================================
    // CRIAR
    // =========================================================================

    @PostMapping
    @Operation(summary = "Criar um registro financeiro",
               description = "Cria um evento com um ou mais meios de pagamento (instituições) " +
                             "e uma ou mais categorias. O tipo do evento define as regras aplicadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registro criado.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RegistroUsuarioResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário, instituição ou categoria não encontrados.", content = @Content)
    })
    public ResponseEntity<RegistroUsuarioResponseDto> criarRegistro(
            @RequestBody @Valid RegistroCompletoCreateDto dto) {

        EventoFinanceiro financeiro           = RegistrosMapper.toEntityFinanceiro(dto.getFinanceiro());
        List<EventoInstituicao> instituicoes  = RegistrosMapper.toEntityEvento(dto.getInstituicao());
        EventoDetalhe detalhe                 = RegistrosMapper.toEntityGasto(dto.getDetalhe());

        Registro registro = registroService.createEventoFinanceiro(financeiro, instituicoes, detalhe);
        return ResponseEntity.status(201).body(RegistrosMapper.toResponseUser(registro));
    }

    @PostMapping("/recorrente")
    @Operation(summary = "Criar um registro recorrente",
               description = "Gera automaticamente múltiplos eventos com base na periodicidade " +
                             "e no intervalo informados. Permitido apenas para Gasto e Recebimento. " +
                             "Exemplo: salário todo dia 15, almoço todo dia útil.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Eventos recorrentes criados.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RegistroResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Tipo inválido para recorrência.", content = @Content),
            @ApiResponse(responseCode = "404", content = @Content)
    })
    public ResponseEntity<List<RegistroResponseDto>> criarRegistroRecorrente(
            @Valid @RequestBody RegistroCompletoCreateDto dto) {

        Tipo tipo = dto.getFinanceiro().getTipo();
        if (tipo != Tipo.Gasto && tipo != Tipo.Recebimento) {
            return ResponseEntity.badRequest().build();
        }

        // Mapeamento feito na camada do controller — service recebe entidades prontas
        RecorrenciaFinanceira recorrencia    = RegistrosMapper.toEntityRecorrencia(dto.getFinanceiro());
        List<EventoInstituicao> instituicoes = RegistrosMapper.toEntityEvento(dto.getInstituicao());
        EventoDetalhe detalhe                = RegistrosMapper.toEntityGasto(dto.getDetalhe());

        List<RegistroResponseDto> resposta = registroService.createEventosRecorrentes(
                recorrencia, instituicoes, detalhe);

        return ResponseEntity.status(201).body(resposta);
    }

    @PostMapping("/lote")
    @Operation(summary = "Criar múltiplos registros de uma vez",
               description = "Cria todos os eventos da lista em uma única requisição.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registros criados.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RegistroResponseDto.class))),
            @ApiResponse(responseCode = "404", content = @Content)
    })
    public ResponseEntity<List<RegistroResponseDto>> criarRegistrosEmLote(
            @RequestBody @Valid List<RegistroCompletoCreateDto> dtos) {

        List<RegistroResponseDto> resposta = dtos.stream()
                .map(dto -> {
                    EventoFinanceiro financeiro          = RegistrosMapper.toEntityFinanceiro(dto.getFinanceiro());
                    List<EventoInstituicao> instituicoes = RegistrosMapper.toEntityEvento(dto.getInstituicao());
                    EventoDetalhe detalhe                = RegistrosMapper.toEntityGasto(dto.getDetalhe());

                    Registro registro = registroService.createEventoFinanceiro(financeiro, instituicoes, detalhe);

                    // Para o lote, retorna a visão do primeiro evento (Gasto / Recebimento)
                    EventoFinanceiro ev = registro.getEventosFinanceiros().getFirst();
                    return RegistrosMapper.toResponse(
                            ev,
                            registro.getInstituicoesPorEvento().getOrDefault(ev, List.of()),
                            registro.getDetalhePorEvento().get(ev));
                })
                .toList();

        return ResponseEntity.status(201).body(resposta);
    }

    // =========================================================================
    // EDITAR
    // =========================================================================

    @PutMapping("/{evento_id}")
    @Operation(summary = "Editar um registro completo",
               description = "Atualiza o evento financeiro, os meios de pagamento e as categorias.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro atualizado.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RegistroUsuarioResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.", content = @Content)
    })
    public ResponseEntity<RegistroUsuarioResponseDto> editarRegistro(
            @PathVariable UUID evento_id,
            @RequestBody RegistroCompletoCreateDto dto) {

        EventoFinanceiro financeiroEditado = registroService.editEventoFinanceiro(
                evento_id, RegistrosMapper.toEntityFinanceiro(dto.getFinanceiro()));

        List<EventoInstituicao> instituicoesEditadas = registroService.editEventoInstituicao(
                evento_id, RegistrosMapper.toEntityEvento(dto.getInstituicao()));

        EventoDetalhe detalheEditado = registroService.editGastoDetalhe(
                evento_id, RegistrosMapper.toEntityGasto(dto.getDetalhe()));

        RegistroUsuarioResponseDto resposta = RegistrosMapper.toResponseUser(
                financeiroEditado, instituicoesEditadas, detalheEditado);

        return ResponseEntity.ok(resposta);
    }

    // =========================================================================
    // DELETAR
    // =========================================================================

    @DeleteMapping("/{evento_id}")
    @Operation(summary = "Deletar um registro",
               description = "Remove o evento financeiro e todos os dados vinculados a ele.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro removido.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Registro não encontrado.", content = @Content)
    })
    public ResponseEntity<Void> deletarRegistro(@PathVariable UUID evento_id) {
        registroService.deleteRegistroByEventoFinanceiro_Id(evento_id);
        return ResponseEntity.noContent().build();
    }
}
