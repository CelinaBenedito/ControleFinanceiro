package controle.api.back_end.controller;

import controle.api.back_end.dto.categoria.CategoriaResponseDTO;
import controle.api.back_end.dto.registros.in.RegistroCompletoCreateDto;
import controle.api.back_end.dto.registros.mapper.RegistrosMapper;
import controle.api.back_end.dto.registros.out.RegistroResponseDto;
import controle.api.back_end.dto.registros.out.RegistroUsuarioResponseDto;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.eventoFinanceiro.*;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.service.RegistroService;
import controle.api.back_end.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/registros")
@Tag(name = "Registros", description = "Endpoints referentes aos registros, onde se é feito o registro de todos os eventos financeiros do usuário.")
public class RegistrosController {
    private final RegistroService registroService;
    private final UsuarioService usuarioService;

    public RegistrosController(RegistroService registroService, UsuarioService usuarioService) {
        this.registroService = registroService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/{user_id}")
    @Operation(summary =
            "Buscar registros associados ao ID de um usuário",
            description =
                    "Busca todos os eventos financeiros associados ao usuário com o id especificado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description =
                    "Busca de dados feita com sucesso e retornou dados!",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RegistroResponseDto.class))),
            @ApiResponse(responseCode = "204", description =
                    "Busca de dados feita com sucesso e não retornou dados!"),
            @ApiResponse(responseCode = "404", description =
                    "Dados inválidos!",
            content = @Content)
    })
    public ResponseEntity<List<RegistroResponseDto>> getRegistrosByUser(@PathVariable UUID user_id){
        List<EventoFinanceiro> eventoFinanceiros = registroService
                .getEventosFinanceirosByUser(user_id);

        List<List<EventoInstituicao>> eventosInstituicoes = registroService
                .getEventosInstituicoesByEventoFinanceiro(eventoFinanceiros);

        List<GastoDetalhe> gastosDetalhes = registroService
                .getGastosDetalhesByEventoFinanceiro(eventoFinanceiros);

        List<RegistroResponseDto> response = RegistrosMapper
                .toResponse(
                        eventoFinanceiros,
                        eventosInstituicoes,
                        gastosDetalhes);

        if (response.isEmpty()){
            return ResponseEntity.status(204).body(response);
        }
        return ResponseEntity.status(200).body(response);
    }

    @GetMapping("/filtro/usuarios/{user_id}")
    @Operation(summary =
            "Buscar os registros com um filtro",
            description =
                    "Busca os registros se baseando no filtro passado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description =
                    "Busca de dados feita com sucesso e retornou dados!",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RegistroResponseDto.class))),
            @ApiResponse(responseCode = "204", description =
                    "Busca de dados feita com sucesso e não retornou dados!"),
            @ApiResponse(responseCode = "404", description =
                    "Dados inválidos!",
                    content = @Content)
    })
    public ResponseEntity<List<RegistroResponseDto>> getByFilter(
            @PathVariable UUID user_id,
            @RequestParam(required = false)Double valor,
            @RequestParam(required = false)List<TipoMovimento> tipoMovimento,
            @RequestParam(required = false)List<Tipo> tipo,
            @RequestParam(required = false)LocalDate dataEvento,
            @RequestParam(required = false)List<InstituicaoUsuario> instituicaoUsuario,
            @RequestParam(required = false)List<CategoriaUsuario> categoriaUsuario,
            @RequestParam(required = false) String descricao,
            @RequestParam(required = false) String titulo
            ){

        List<RegistroResponseDto> responses = registroService.getByFilter(user_id,valor,
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

    @GetMapping("/download/{user_id}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable UUID user_id,
                                               @RequestParam String tipo)  throws IOException {
        byte[] conteudo;
        String nomeArquivo;
        String contentType;
        Usuario usuario = usuarioService.getUsuarioById(user_id);
        switch(tipo.toLowerCase()){
            case "json":
                conteudo = registroService.createJson(user_id).getBytes(StandardCharsets.UTF_8);
                nomeArquivo = "registros_"+usuario.getNome()+"_"+LocalDate.now() +".json";
                contentType = "application/json";
                break;

            case "sql":
                conteudo = registroService.createSql(user_id).getBytes(StandardCharsets.UTF_8);
                nomeArquivo = "registros_"+usuario.getNome()+"_"+LocalDate.now() +".sql";
                contentType = "application/sql";
                break;

            case "excel":
                conteudo = registroService.createExcel(user_id);
                nomeArquivo = "registros_"+usuario.getNome()+"_"+LocalDate.now() +".xlsx";
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                break;

            case "pdf":
                conteudo = registroService.createPdf(user_id);
                nomeArquivo = "registros_"+usuario.getNome()+"_"+LocalDate.now() +".pdf";
                contentType = "application/pdf";
                break;

            default:
                return ResponseEntity.badRequest().body("Formato não suportado".getBytes());
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nomeArquivo)
                .contentType(MediaType.parseMediaType(contentType))
                .body(conteudo);
    }

    @PostMapping
    @Operation(summary = "Criar um novo registro",
            description = "Cria um novo evento financeiro adicionando o ao banco de dados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registro criado com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RegistroUsuarioResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Dados inválidos!")
    })
    public ResponseEntity<RegistroUsuarioResponseDto> createRegistroCompleto(
            @RequestBody @Valid RegistroCompletoCreateDto dto){
        EventoFinanceiro eventoCreated = registroService.createEventoFinanceiro(
                RegistrosMapper.toEntityFinanceiro(dto.getFinanceiro()));

        List<EventoInstituicao> instituicaoCreated = registroService.createEventoInstituicao(
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
            @ApiResponse(responseCode = "201", description = "Registros criados com sucesso!",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RegistroResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Dados inválidos!",
            content = @Content)
    })
    public ResponseEntity<List<RegistroResponseDto>> createListaRegistrosCompletos(
            @RequestBody @Valid List<RegistroCompletoCreateDto> dtos) {

        List<RegistroResponseDto> responses = dtos.stream()
                .map(dto -> {
                    EventoFinanceiro eventoCreated = registroService.createEventoFinanceiro(
                            RegistrosMapper.toEntityFinanceiro(dto.getFinanceiro()));

                    List<EventoInstituicao> instituicoesCreated = registroService.createEventoInstituicao(
                            RegistrosMapper.toEntityEvento(dto.getInstituicao()), eventoCreated);

                    GastoDetalhe gastoCreated = registroService.createGastoDetalhe(
                            RegistrosMapper.toEntityGasto(dto.getDetalhe()), eventoCreated);

                    return RegistrosMapper
                            .toResponse(
                                    eventoCreated,
                                    instituicoesCreated,
                                    gastoCreated
                            );
                })
                .toList();

        return ResponseEntity.status(201).body(responses);
    }


    @PutMapping("/{evento_id}")
    @Operation(summary =
            "Edita um registro buscando o id do evento financeiro associado ao registro",
            description =
                    "Edita os dados de um registro, para achar este registro usa o ID do evento financeiro que é unico, após isso edita os dados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description =
                    "Registro editado com sucesso!",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RegistroUsuarioResponseDto.class))),
            @ApiResponse(responseCode = "404", description =
                    "Dados inválidos!",
                    content = @Content)
    })
    public ResponseEntity<RegistroUsuarioResponseDto> editRegistroByEventoFinanceiro_Id(
            @PathVariable UUID evento_id,
            @RequestBody RegistroCompletoCreateDto dto){

        EventoFinanceiro entityFinanceiro = RegistrosMapper.toEntityFinanceiro(dto.getFinanceiro());
        EventoFinanceiro financeiroEdited = registroService.editEventoFinanceiro(evento_id, entityFinanceiro);

        List<EventoInstituicao> entityInstituicoes = RegistrosMapper.toEntityEvento(dto.getInstituicao());
        List<EventoInstituicao> instituicoesEdited = registroService.editEventoInstituicao(evento_id, entityInstituicoes);

        GastoDetalhe entityGasto = RegistrosMapper.toEntityGasto(dto.getDetalhe());
        GastoDetalhe gastoEdited = registroService.editGastoDetalhe(evento_id, entityGasto);

        RegistroUsuarioResponseDto response = RegistrosMapper.toResponseUser(financeiroEdited, instituicoesEdited, gastoEdited);

        return ResponseEntity.status(200).body(response);
    }


    @DeleteMapping("/{evento_id}")
    @Operation(summary =
            "deleta um registro buscando o id do evento financeiro associado ao registro",
            description =
                    "Deleta os dados de um registro, para achar este registro usa o ID do evento financeiro que é unico, após isso deleta os dados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description =
                    "Registro deletado com sucesso!",
                    content = @Content),
            @ApiResponse(responseCode = "404", description =
                    "Dados inválidos!",
                    content = @Content)
    })
    public ResponseEntity<Void> deleteRegistroByEventoFinanceiro_Id(
            @PathVariable UUID evento_id
    ){
        registroService.deleteRegistroByEventoFinanceiro_Id(evento_id);
        return ResponseEntity.status(204).build();
    }


}

