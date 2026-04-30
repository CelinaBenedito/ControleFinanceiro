package controle.api.back_end.controller;

import controle.api.back_end.dto.configuracoes.in.*;
import controle.api.back_end.dto.configuracoes.mapper.ConfiguracoesMapper;
import controle.api.back_end.dto.configuracoes.out.ConfiguracaoUsuarioResponseDTO;
import controle.api.back_end.dto.configuracoes.out.ConfiguracoesResponsesDTO;
import controle.api.back_end.dto.usuario.out.UsuarioResponseDTO;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.configuracoes.Configuracoes;
import controle.api.back_end.model.configuracoes.LimitePorCategoria;
import controle.api.back_end.model.configuracoes.LimitePorInstituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.service.ConfiguracoesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/configuracoes")
@Tag(name = "Configurações", description = "Endpoints referentes as configurações dos usuários.")
public class ConfiguracoesController {

    private final ConfiguracoesService configuracoesService;

    public ConfiguracoesController(ConfiguracoesService configuracoesService) {
        this.configuracoesService = configuracoesService;
    }

    @GetMapping
    @Operation(summary = "Buscar todas as configurações",
            description = "Busca todos as configurações registradas no banco de dados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca de dados feita com sucesso e retornou com dados!",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ConfiguracoesResponsesDTO.class))),
            @ApiResponse(responseCode = "204", description = "Busca de dados feita com sucesso e não retornou dados!",
                    content = @Content)
    })
    public ResponseEntity<List<ConfiguracoesResponsesDTO>> getConfiguracoes(){
        List<Configuracoes> all = configuracoesService.getConfiguracoes();
        if(all.isEmpty()){
            return ResponseEntity.noContent().build();
        }
        List<ConfiguracoesResponsesDTO> responses = ConfiguracoesMapper.toDto(all);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar a configuração com o id especificado",
            description = "Busca a configuração correspondente ao id informado junto do usuario que a possui.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca de dados feita com sucesso e retornou com dados!",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ConfiguracaoUsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Dados inválidos!",
                    content = @Content)
    })
    public ResponseEntity<ConfiguracaoUsuarioResponseDTO> getConfiguracaoById(@PathVariable UUID id){
        Configuracoes configById = configuracoesService.getConfiguracoesById(id);
        ConfiguracaoUsuarioResponseDTO response = ConfiguracoesMapper.toDtoUser(configById);
        return ResponseEntity.status(200).body(response);
    }

    @GetMapping("/usuarios/{user_id}")
    @Operation(summary = "Buscar a configuração associada ao id do usuário especificado",
            description = "Busca a configuração correspondente ao id do usuário informado junto.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca de dados feita com sucesso e retornou com dados!",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ConfiguracaoUsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Dados inválidos!",
                    content = @Content)
    })
    public ResponseEntity<ConfiguracaoUsuarioResponseDTO> getConfiguracaoByUserId(@PathVariable UUID user_id){
        Configuracoes configByUserId = configuracoesService.getConfiguracaoByUserId(user_id);
        ConfiguracaoUsuarioResponseDTO response = ConfiguracoesMapper.toDtoUser(configByUserId);
        return ResponseEntity.status(200).body(response);
    }

    @PostMapping
    @Operation(summary = "Criar uma nova configuração",
            description = "Cria uma nova configuração no banco de dados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "configuracao criada com sucesso!",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ConfiguracaoUsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Usuario já tem uma configuração cadastrada",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Dados inválidos!",
                    content = @Content)
    })
    public ResponseEntity<ConfiguracaoUsuarioResponseDTO> createConfiguracao(@Valid @RequestBody ConfiguracoesCreateDTO createDto){
        Configuracoes entity = ConfiguracoesMapper.toEntity(createDto);
        Configuracoes created = configuracoesService.createConfiguracao(entity,createDto.getFkUsuario());
        ConfiguracaoUsuarioResponseDTO response = ConfiguracoesMapper.toDtoUser(created);

        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("{id}/instituicoes")
    @Operation(summary = "Adicionar uma lista de limite para instituições.",
            description = "Adição de uma lista de instituições e seus respectivos limites nas configurações.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Lista de instituições criadas!",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ConfiguracaoUsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Limite já cadastrado!",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Dados inválidos!",
                    content = @Content)
    })
    public ResponseEntity<ConfiguracaoUsuarioResponseDTO> createLimitePorInstituicao(@RequestBody @Valid List<LimitePorInstitucaoCreateDTO> createDtos,
                                                                                     @PathVariable UUID id){
        List<LimitePorInstituicao> limites = new ArrayList<>();
        for (LimitePorInstitucaoCreateDTO dto : createDtos){
            InstituicaoUsuario instituicaoUsuario = configuracoesService.findInstituicaoUsuario(createDtos.getFirst().getInstitucaoUsuario_id());
            LimitePorInstituicao limitePorInstituicao = configuracoesService.createLimitePorInstituicao(instituicaoUsuario, dto.getLimiteDesejado());
            limites.add(limitePorInstituicao);
        }
        Configuracoes configuracoes = configuracoesService.updateLimiteInstituicao(id, limites);
        ConfiguracaoUsuarioResponseDTO response = ConfiguracoesMapper.toDtoUser(configuracoes);
        return ResponseEntity.status(200).body(response);
    }


    @PostMapping("{id}/categorias")
    @Operation(summary = "Adicionar uma lista de limite para categorias.",
            description = "Adição de uma lista de categorias e seus respectivos limites nas configurações.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Lista de categorias criadas!",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ConfiguracaoUsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Limite já cadastrado!",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Dados inválidos!",
                    content = @Content)
    })
    public ResponseEntity<ConfiguracaoUsuarioResponseDTO> createLimitePorCategoria(@RequestBody @Valid List<LimitePorCategoriaCreateDTO> createDtos,
                                                                                     @PathVariable UUID id){
        List<LimitePorCategoria> limites = new ArrayList<>();
        for (LimitePorCategoriaCreateDTO dto : createDtos){
            CategoriaUsuario categoriaUsuario = configuracoesService.findCategoriaUsuario(createDtos.getFirst().getCategoriaUsuario_id());
            LimitePorCategoria limitePorCategoria = configuracoesService.createLimitePorCategoria(categoriaUsuario, dto.getLimiteDesejado());
            limites.add(limitePorCategoria);
        }
        Configuracoes configuracoes = configuracoesService.updateLimiteCategoria(id, limites);
        ConfiguracaoUsuarioResponseDTO response = ConfiguracoesMapper.toDtoUser(configuracoes);
        return ResponseEntity.status(200).body(response);
    }

    @PostMapping(value ="/upload-arquivo/usuarios/{user_id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> postArquivo(@PathVariable UUID user_id,
                                            @RequestParam MultipartFile arquivo){
        if (arquivo.isEmpty()){
            return ResponseEntity.badRequest().body("Nenhum arquivo enviado.");
        }
        String nomeArquivo = arquivo.getOriginalFilename();
        String tipoArquivo = arquivo.getContentType();

        return ResponseEntity.status(200).build();
    }

    @PutMapping("/edit/{id}")
    @Operation(summary = "Editar as configuranções.",
            description = "Edita as informações contidas dentro das configurações")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Atualizado com sucesso!",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ConfiguracaoUsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Dados inválidos!",
                    content = @Content)
    })
    public ResponseEntity<ConfiguracaoUsuarioResponseDTO> editConfiguracao(@Valid @RequestBody ConfiguracaoEditDTO editDTO, @PathVariable UUID id){
        Configuracoes entity = ConfiguracoesMapper.toEntity(editDTO);
        Configuracoes edited = configuracoesService.editConfiguracao(entity, editDTO, id);
        ConfiguracaoUsuarioResponseDTO response = ConfiguracoesMapper.toDtoUser(edited);
        return ResponseEntity.status(200).body(response);
    }

    @DeleteMapping("{id}/dados/periodo-tempo")
    @Operation(summary = "Deleta os dados do usuário por periodo de tempo",
            description = "Deleta os registros cadastros por periodo de temp, de determinada data até determinada data.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleção ocorreu com sucesso.",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Dados inválidos",
                    content = @Content)
    })
    public ResponseEntity<Void> deleteDadosByPeriodoDeTempo(@Valid @RequestBody PeriodoTempoRequestDto tempoDto, @PathVariable UUID id){
        configuracoesService.deleteByPeriodoDeTempo(id,tempoDto);

        return ResponseEntity.status(204).build();
    }

    @DeleteMapping("/usuarios/{user_id}/dados/deletar-tudo")
    @Operation(summary = "Deleta todo os registros do usuário",
            description = "Deleta todos os registros do usuário.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleção ocorreu com sucesso.",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Dados inválidos",
                    content = @Content)
    })
    public ResponseEntity<Void> deleteAll(@PathVariable UUID user_id){
        configuracoesService.deleteAllByUsuario(user_id);
        return ResponseEntity.status(204).build();
    }

}
