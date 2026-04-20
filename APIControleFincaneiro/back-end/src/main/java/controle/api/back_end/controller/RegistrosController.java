package controle.api.back_end.controller;

import controle.api.back_end.dto.registros.in.EventoFinanceiroCreateDto;
import controle.api.back_end.dto.registros.in.EventoInstituicaoCreateDto;
import controle.api.back_end.dto.registros.in.GastoDetalheCreateDto;
import controle.api.back_end.dto.registros.in.RegistroCompletoCreateDto;
import controle.api.back_end.dto.registros.mapper.RegistrosMapper;
import controle.api.back_end.dto.registros.out.RegistroResponseDto;
import controle.api.back_end.dto.registros.out.RegistroUsuarioResponseDto;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.eventoFinanceiro.*;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.service.RegistroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
    @Operation(summary =
            "Bucar registros associados ao ID de um usuário",
            description =
                    "Busca todos os eventos financeiros associados ao usuário com o id especificadp.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description =
                    "Busca de dados feita com sucesso e retornou dados!"),
            @ApiResponse(responseCode = "204", description =
                    "Busca de dados feita com sucesso e não retornou dados!"),
            @ApiResponse(responseCode = "404", description =
                    "Dados inválidos!")
    })
    public ResponseEntity<List<RegistroResponseDto>> getRegistrosByUser(@PathVariable UUID user_id){
        List<EventoFinanceiro> eventoFinanceiros = registroService.
                getEventosFinanceirosByUser(user_id);

        List<EventoInstituicao> eventoInstituicoes = registroService
                .getEventosInstituicoesByEventoFinanceiro(eventoFinanceiros);

        List<GastoDetalhe> gastosDetalhes = registroService
                .getGastosDetalhesByEventoFinanceiro(eventoFinanceiros);
        List<RegistroResponseDto> response = RegistrosMapper
                .toResponse(
                        eventoFinanceiros,
                        eventoInstituicoes,
                        gastosDetalhes);
        if (response.isEmpty()){
            return ResponseEntity.status(204).body(response);
        }
        return ResponseEntity.status(200).body(response);
    }

    @GetMapping("/filtro")
    public ResponseEntity<List<RegistroResponseDto>> getByFilter(
            @RequestParam(required = false) Double valor,
            @RequestParam(required = false)TipoMovimento tipoMovimento,
            @RequestParam(required = false)Tipo tipo,
            @RequestParam(required = false)LocalDate dataEvento,
            @RequestParam(required = false)InstituicaoUsuario instituicaoUsuario,
            @RequestParam(required = false)CategoriaUsuario categoriaUsuario,
            @RequestParam(required = false) String descricao,
            @RequestParam(required = false) String titulo
            ){

        List<RegistroResponseDto> responses = registroService.getByFilter(valor,
                tipoMovimento,
                tipo,
                dataEvento,
                instituicaoUsuario,
                categoriaUsuario,
                descricao,
                titulo);
        if (responses.isEmpty()){
            return ResponseEntity.status(204).build();
        }
        return ResponseEntity.status(200).body(responses);
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

    @PutMapping("/{evento_id}")
    public ResponseEntity<RegistroUsuarioResponseDto> editRegistroByEventoFinanceiro_Id(
            @PathVariable UUID evento_id,
            @RequestBody RegistroCompletoCreateDto dto){

        EventoFinanceiro entityFinanceiro = RegistrosMapper.toEntityFinanceiro(dto.getFinanceiro());

        EventoFinanceiro financeiroEdited =  registroService.editEventoFinanceiro(evento_id,entityFinanceiro);

        EventoInstituicao entityInstituicao = RegistrosMapper.toEntityEvento(dto.getInstituicao());

        EventoInstituicao instituicaoEdited = registroService.editEventoInstituicao(evento_id, entityInstituicao);

        GastoDetalhe entityGasto = RegistrosMapper.toEntityGasto(dto.getDetalhe());

        GastoDetalhe gastoEdited = registroService.editGastoDetalhe(evento_id, entityGasto);

        RegistroUsuarioResponseDto response = RegistrosMapper.toResponseUser(financeiroEdited, instituicaoEdited, gastoEdited);

        return ResponseEntity.status(200).body(response);
    }

}
