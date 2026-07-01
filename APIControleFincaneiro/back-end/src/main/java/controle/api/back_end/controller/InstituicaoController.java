package controle.api.back_end.controller;

import controle.api.back_end.dto.instituicao.in.InstituicaoCreateDTO;
import controle.api.back_end.dto.instituicao.in.InstituicaoOFXConfigUpdateDTO;
import controle.api.back_end.dto.instituicao.out.InstituicaoOFXConfigDTO;
import controle.api.back_end.dto.instituicao.out.InstituicaoResponseDTO;
import controle.api.back_end.dto.instituicao.out.InstituicaoUsuarioResponseDTO;
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

    // =========================================================================
    // OFX Config
    // =========================================================================

    @GetMapping("/ofx-config")
    @Operation(
            summary = "Listar config OFX de todas as instituicoes",
            description = """
                    Retorna bankUrl, navigationSteps e placeholders de todas as instituicoes.
                    O JS usa para saber quais bancos suportam OFX e quais steps usar.
                    """
    )
    public ResponseEntity<List<InstituicaoOFXConfigDTO>> getTodasOFXConfigs() {
        return ResponseEntity.ok(instituicaoService.getTodasOFXConfigs());
    }

    @GetMapping("/{id}/ofx-config")
    @Operation(
            summary = "Buscar config OFX de uma instituicao",
            description = """
                    Retorna bankUrl, navigationSteps com placeholders e lista dos placeholders necessarios.

                    **Como o JS deve usar:**
                    1. Buscar esta config
                    2. Se ofxSupported = false, nao acionar o scraper
                    3. Substituir {{PLACEHOLDER}} pelos valores do localStorage
                    4. Enviar para POST /ofx/sync/usuario/{userId}
                    """
    )
    public ResponseEntity<InstituicaoOFXConfigDTO> getOFXConfig(@PathVariable Integer id) {
        return ResponseEntity.ok(instituicaoService.getOFXConfig(id));
    }

    @PutMapping("/{id}/ofx-config")
    @Operation(
            summary = "Atualizar config OFX de uma instituicao",
            description = """
                    Atualiza bankUrl, navigationSteps e ofxSupported.
                    Use null em navigationSteps para modo manual (browser abre, usuario navega sozinho).
                    """
    )
    public ResponseEntity<InstituicaoOFXConfigDTO> updateOFXConfig(
            @PathVariable Integer id,
            @RequestBody InstituicaoOFXConfigUpdateDTO dto) {
        return ResponseEntity.ok(instituicaoService.updateOFXConfig(id, dto));
    }

}
