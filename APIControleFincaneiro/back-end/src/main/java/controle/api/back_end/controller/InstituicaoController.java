package controle.api.back_end.controller;

import controle.api.back_end.dto.instituicao.in.AtualizarInstituicaoUsuarioDto;
import controle.api.back_end.dto.instituicao.in.InstituicaoCreateDTO;
import controle.api.back_end.dto.instituicao.out.DetalheInstituicaoDto;
import controle.api.back_end.dto.instituicao.out.InstituicaoResponseDTO;
import controle.api.back_end.dto.instituicao.out.InstituicaoUsuarioResponseDTO;
import controle.api.back_end.dto.instituicao.out.ResumoInstituicaoDto;
import controle.api.back_end.dto.instituicao.mapper.InstituicaoMapper;
import controle.api.back_end.dto.instituicao.mapper.InstituicaoUsuarioMapper;
import controle.api.back_end.model.instituicao.Instituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.service.InstituicaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/instituicoes")
@Tag(name = "Instituições", description = "Endpoints referentes as instituições financeiras dos usuários, por exemplo: itau, bradesco etc...")
public class InstituicaoController {

    private final InstituicaoService instituicaoService;

    public InstituicaoController(InstituicaoService instituicaoService) {
        this.instituicaoService = instituicaoService;
    }


    @GetMapping
    @Operation(summary = "Buscar todas as intituições ",
            description = "Busca todas as instituições registradas no banco de dados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca de dados feita com sucesso e retornou com dados",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InstituicaoResponseDTO.class))),
            @ApiResponse(responseCode = "204", description = "Busca de dados feita com sucesso e não retornou dados",
            content = @Content)
    })
    public ResponseEntity<Page<InstituicaoResponseDTO>> getInstituicoes(
            @RequestParam(defaultValue = "0") int pagina){
        Page<InstituicaoResponseDTO> response = instituicaoService
                .getInstituicoes(PageRequest.of(pagina, 5))
                .map(InstituicaoMapper::toDto);
        if(response.isEmpty()){
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{id}")
    @Operation(summary = "Buscar a insituição que contém o id desejado ",
            description = "Busca no banco de dados a instituição com o id desejado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca de dados feita com sucesso e retornou com dados",
            content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = InstituicaoResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Instituição não encontrada",
            content = @Content)
    })
    public ResponseEntity<InstituicaoResponseDTO> getInsituicaoById(@PathVariable Integer id){
        Instituicao entity = instituicaoService.getInstituicaoById(id);
        InstituicaoResponseDTO response = InstituicaoMapper.toDto(entity);
        return ResponseEntity.status(200).body(response);
    }

    @GetMapping("/saldo/{instituicaoUsuario_id}")
    @Operation(summary = "Buscar o saldo pelo id da instituição",
            description = "Busca o saldo de uma instituição pela assosiação do usuário e da instituição.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca feita com sucesso e a dados para retornar",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BigDecimal.class))),
            @ApiResponse(responseCode = "404", description = "Dados Inválidos",
                    content = @Content)
    })
    public ResponseEntity<BigDecimal> getSaldoByInstituicao(@PathVariable Integer instituicaoUsuario_id){
        BigDecimal saldoByInstituicao = instituicaoService
                .getSaldoByInstituicao(instituicaoUsuario_id);
        return ResponseEntity.status(200).body(saldoByInstituicao);
    }

    @PostMapping
    @Operation(summary = "Adicionar uma instituição.",
            description = "Cria uma nova instituição no banco de dados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Instituição criada com sucesso!",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InstituicaoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos.",
                    content = @Content)
    })
    public ResponseEntity<InstituicaoResponseDTO> createInstituicao(@Valid @RequestBody InstituicaoCreateDTO dto){
        Instituicao entity = InstituicaoMapper.toEntity(dto);
        Instituicao created = instituicaoService.createInstituicao(entity);
        InstituicaoResponseDTO response = InstituicaoMapper.toDto(created);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/{instituicao_id}/usuarios/{usuario_id}")
    @Operation(summary = "Vincular uma instituição a um usuario",
            description = "Vincular por meio de uma tabela associativa no banco de dados uma instituição a um usuario.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Instituição associada a um usuario com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InstituicaoUsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Dados Inválidos",
                    content = @Content)
    })
    public ResponseEntity<InstituicaoUsuarioResponseDTO> createInstituicaoForUser(
            @PathVariable Integer instituicao_id,
            @PathVariable UUID usuario_id){
        InstituicaoUsuario created = instituicaoService.createInstituicaoForUsuario(instituicao_id, usuario_id);
        InstituicaoUsuarioResponseDTO response = InstituicaoUsuarioMapper.toDto(created);
        return ResponseEntity.status(201).body(response);
    }


    @PatchMapping("/{instituicao_id}/usuarios/{user_id}")
    @Operation(summary = "Desvincular uma instituição de um usuário",
            description = "Desativa o vinculo do usuario com a instituição.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Instituição desasociado do usuario com sucesso",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Dados Inválidos",
                    content = @Content)
    })
    public ResponseEntity<List<InstituicaoUsuarioResponseDTO>> detachUserFromInstituicao(
            @PathVariable Integer instituicao_id,
            @PathVariable UUID user_id
    ){
        instituicaoService.detachUserFromInstituicao(instituicao_id,user_id);
        return ResponseEntity.status(204).build();
    }

    @GetMapping("/usuarios/{user_id}")
    @Operation(summary = "Buscar instituições pelo id do usuário",
            description = "Busca uma instituição que está associada a um usuário")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca feita com sucesso e a dados para retornar",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InstituicaoUsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "204", description = "Busca feita com sucesso e não a dados para retornar",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content)
    })
    public ResponseEntity<Page<InstituicaoUsuarioResponseDTO>> getInstituicoesByUserId(
            @PathVariable UUID user_id,
            @RequestParam(defaultValue = "0") int pagina){
        Page<InstituicaoUsuarioResponseDTO> response = instituicaoService
                .getInstituicoesByUserId(user_id, PageRequest.of(pagina, 5))
                .map(InstituicaoUsuarioMapper::toDto);
        if (response.isEmpty()){
            return ResponseEntity.status(204).build();
        }
        return ResponseEntity.status(200).body(response);
    }

    @PutMapping("/desvincular-todas-as-instituicoes/usuarios/{user_id}")
    public ResponseEntity<Void> detachAllIntituicoes(@PathVariable UUID user_id){
        instituicaoService.detachAllIntituicoes(user_id);
        return ResponseEntity.status(204).build();
    }

    @DeleteMapping("{id}")
    @Operation(summary = "Deletar uma instituição",
            description = "Deletar uma instituição por seu id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Instituição deletada com sucesso",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Instituição não encontrada",
                    content = @Content)
    })
    public ResponseEntity<Void> deleteInstituicao(@PathVariable Integer id){
        instituicaoService.deleteInstituicao(id);
        return ResponseEntity.status(204).build();
    }

    @GetMapping("/resumo/usuarios/{user_id}")
    @Operation(summary = "Cards de resumo das instituições do usuário",
            description = "Retorna um resumo de cada instituição ativa do usuário com saldo, crédito, débito, parcelamentos ativos e % de crédito utilizado. Suporta filtro por período (periodo, ano, mes, trimestre, semestre).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ResumoInstituicaoDto.class))),
            @ApiResponse(responseCode = "204", content = @Content),
            @ApiResponse(responseCode = "404", content = @Content)
    })
    public ResponseEntity<List<ResumoInstituicaoDto>> getResumoInstituicoes(
            @PathVariable UUID user_id,
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer trimestre,
            @RequestParam(required = false) Integer semestre) {

        LocalDate dataInicio = null;
        LocalDate dataFim = null;

        if (periodo != null && ano != null) {
            switch (periodo) {
                case "MENSAL" -> {
                    int m = mes != null ? mes : 1;
                    dataInicio = LocalDate.of(ano, m, 1);
                    dataFim = dataInicio.withDayOfMonth(dataInicio.lengthOfMonth());
                }
                case "TRIMESTRAL" -> {
                    int t = trimestre != null ? trimestre : 1;
                    int mesInicio = (t - 1) * 3 + 1;
                    dataInicio = LocalDate.of(ano, mesInicio, 1);
                    dataFim = dataInicio.plusMonths(3).minusDays(1);
                }
                case "SEMESTRAL" -> {
                    int s = semestre != null ? semestre : 1;
                    int mesInicio = (s - 1) * 6 + 1;
                    dataInicio = LocalDate.of(ano, mesInicio, 1);
                    dataFim = dataInicio.plusMonths(6).minusDays(1);
                }
                case "ANUAL" -> {
                    dataInicio = LocalDate.of(ano, 1, 1);
                    dataFim = LocalDate.of(ano, 12, 31);
                }
            }
        }

        List<ResumoInstituicaoDto> resultado = instituicaoService.getResumoInstituicoes(user_id, dataInicio, dataFim);
        return resultado.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(resultado);
    }

    @GetMapping("/{instUsuario_id}/detalhe")
    @Operation(summary = "Detalhe de uma instituição com distribuição por tipo de movimento",
            description = "Retorna os dados da associação usuário-instituição e a distribuição dos valores por tipo de movimento (débito, crédito, pix, etc).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = DetalheInstituicaoDto.class))),
            @ApiResponse(responseCode = "404", content = @Content)
    })
    public ResponseEntity<DetalheInstituicaoDto> getDetalheInstituicao(@PathVariable Integer instUsuario_id) {
        return ResponseEntity.ok(instituicaoService.getDetalheInstituicao(instUsuario_id));
    }

    @PatchMapping("/{instUsuario_id}/configurar")
    @Operation(summary = "Atualizar limite de crédito e taxa de juros da instituição",
            description = "Permite ao usuário configurar o limite de crédito disponível e a taxa de juros da instituição.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = InstituicaoUsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "404", content = @Content)
    })
    public ResponseEntity<InstituicaoUsuarioResponseDTO> configurarInstituicaoUsuario(
            @PathVariable Integer instUsuario_id,
            @RequestBody AtualizarInstituicaoUsuarioDto dto) {
        InstituicaoUsuario updated = instituicaoService.atualizarInstituicaoUsuario(instUsuario_id, dto);
        return ResponseEntity.ok(InstituicaoUsuarioMapper.toDto(updated));
    }

    @PostMapping("/{instUsuario_id}/pagar-fatura")
    @Operation(summary = "Pagar fatura do cartão de crédito",
            description = "Registra o pagamento de fatura, liberando crédito disponível.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pagamento registrado."),
            @ApiResponse(responseCode = "400", description = "Valor inválido.", content = @Content),
            @ApiResponse(responseCode = "404", content = @Content)
    })
    public ResponseEntity<?> pagarFatura(
            @PathVariable Integer instUsuario_id,
            @RequestBody java.util.Map<String, Double> body) {
        Double valor = body.get("valor");
        if (valor == null || valor <= 0) {
            return ResponseEntity.badRequest().body("Informe um valor válido.");
        }
        instituicaoService.pagarFatura(instUsuario_id, java.math.BigDecimal.valueOf(valor));
        return ResponseEntity.ok().build();
    }
}
