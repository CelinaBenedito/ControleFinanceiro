package controle.api.back_end.controller;

import controle.api.back_end.dto.registros.in.EventoFinanceiroCreateDto;
import controle.api.back_end.dto.registros.in.EventoInstituicaoCreateDto;
import controle.api.back_end.dto.registros.in.GastoDetalheCreateDto;
import controle.api.back_end.dto.registros.in.RegistroCompletoCreateDto;
import controle.api.back_end.dto.registros.mapper.RegistrosMapper;
import controle.api.back_end.dto.registros.out.RegistroResponseDto;
import controle.api.back_end.dto.registros.out.RegistroUsuarioResponseDto;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.eventoFinanceiro.GastoDetalhe;
import controle.api.back_end.service.RegistroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/registros")
public class RegistrosController {
    private final RegistroService registroService;

    public RegistrosController(RegistroService registroService) {
        this.registroService = registroService;
    }

    @GetMapping("/{user_id}")
    public ResponseEntity<List<RegistroResponseDto>> getRegistrosByUser(@PathVariable UUID user_id){
        List<EventoFinanceiro> eventoFinanceiros = registroService.
                getEventosFinanceirosByUser(user_id);

        List<EventoInstituicao> eventoInstituicoes = registroService
                .getEventosInstituicoesByEventoFinanceiro(eventoFinanceiros);

        List<GastoDetalhe> gastosDetalhes = registroService.getGastosDetalhesByEventoFinanceiro(eventoFinanceiros);
        List<RegistroResponseDto> response = RegistrosMapper.toResponse(eventoFinanceiros,eventoInstituicoes,gastosDetalhes);

        return ResponseEntity.status(200).body(response);
    }

    @PostMapping
    @Operation(summary = "Criar um novo registro",
            description = "Cria um novo evento financeiro adicionando o ao banco de dados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Dados inválidos!")
    })
    public ResponseEntity<RegistroUsuarioResponseDto> createRegistroCompleto(
            @RequestBody @Valid RegistroCompletoCreateDto dto){
        EventoFinanceiro eventoCreated = registroService.createEventoFinanceiro(
                RegistrosMapper.toEntityFinanceiro(dto.getFinanceiro()));

        EventoInstituicao instituicaoCreated = registroService.createEventoInstituicao(
                RegistrosMapper.toEntityEvento(dto.getInstituicao()), eventoCreated);

        GastoDetalhe gastoCreated = registroService.createGastoDetalhe(
                RegistrosMapper.toEntityGasto(dto.getDetalhe()), eventoCreated);

        RegistroUsuarioResponseDto response = RegistrosMapper.toResponseUser(
                eventoCreated, instituicaoCreated, gastoCreated);

        return ResponseEntity.status(201).body(response);

    }

    @PostMapping("/lote")
    @Operation(summary = "Criar uma lista de novos registros.",
            description = "Cria uma lista de novos registros do mesmo dia.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registros criados com sucesso!"),
            @ApiResponse(responseCode = "404", description = "Dados inválidos!")
    })
    public ResponseEntity<List<RegistroResponseDto>> createListaRegistrosCompletos(
            @RequestBody @Valid List<RegistroCompletoCreateDto> dtos) {

        List<RegistroResponseDto> responses = dtos.stream()
                .map(dto -> {
                    EventoFinanceiro eventoCreated = registroService.createEventoFinanceiro(
                            RegistrosMapper.toEntityFinanceiro(dto.getFinanceiro()));

                    EventoInstituicao instituicaoCreated = registroService.createEventoInstituicao(
                            RegistrosMapper.toEntityEvento(dto.getInstituicao()), eventoCreated);

                    GastoDetalhe gastoCreated = registroService.createGastoDetalhe(
                            RegistrosMapper.toEntityGasto(dto.getDetalhe()), eventoCreated);

                    return RegistrosMapper
                            .toResponse(
                                    eventoCreated,
                                    instituicaoCreated,
                                    gastoCreated
                            );
                })
                .toList();

        return ResponseEntity.status(201).body(responses);
    }

}
