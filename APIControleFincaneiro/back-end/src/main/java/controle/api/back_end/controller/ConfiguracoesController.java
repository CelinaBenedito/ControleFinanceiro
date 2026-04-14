package controle.api.back_end.controller;

import controle.api.back_end.dto.configuracoes.ConfiguracaoEditDTO;
import controle.api.back_end.dto.configuracoes.ConfiguracaoUsuarioResponseDTO;
import controle.api.back_end.dto.configuracoes.ConfiguracoesCreateDTO;
import controle.api.back_end.dto.configuracoes.ConfiguracoesResponsesDTO;
import controle.api.back_end.dto.configuracoes.mapper.ConfiguracoesMapper;
import controle.api.back_end.model.configuracoes.Configuracoes;
import controle.api.back_end.service.ConfiguracoesService;
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
@RequestMapping("/configuracoes")
public class ConfiguracoesController {

    private final ConfiguracoesService configuracoesService;

    public ConfiguracoesController(ConfiguracoesService configuracoesService) {
        this.configuracoesService = configuracoesService;
    }

    @GetMapping
    @Operation(summary = "Buscar todas as configurações",
            description = "Busca todos as configurações registradas no banco de dados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca de dados feita com sucesso e retornou com dados!"),
            @ApiResponse(responseCode = "204", description = "Busca de dados feita com sucesso e não retornou dados!")
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
            @ApiResponse(responseCode = "200", description = "Busca de dados feita com sucesso e retornou com dados!"),
            @ApiResponse(responseCode = "404", description = "Dados inválidos!")
    })
    public ResponseEntity<ConfiguracaoUsuarioResponseDTO> getConfiguracaoById(@PathVariable UUID id){
        Configuracoes configById = configuracoesService.getConfiguracoesById(id);
        ConfiguracaoUsuarioResponseDTO response = ConfiguracoesMapper.toDtoUser(configById);
        return ResponseEntity.status(200).body(response);
    }

    @PostMapping
    @Operation(summary = "Criar uma nova configuração",
            description = "Cria uma nova configuração no banco de dados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "configuracao criada com sucesso!"),
            @ApiResponse(responseCode = "409", description = "Usuario já tem uma configuração cadastrada"),
            @ApiResponse(responseCode = "404", description = "Dados inválidos!")
    })
    public ResponseEntity<ConfiguracaoUsuarioResponseDTO> createConfiguracao(@Valid @RequestBody ConfiguracoesCreateDTO createDto){
        Configuracoes entity = ConfiguracoesMapper.toEntity(createDto);
        Configuracoes created = configuracoesService.createConfiguracao(entity,createDto);
        ConfiguracaoUsuarioResponseDTO response = ConfiguracoesMapper.toDtoUser(created);

        return ResponseEntity.status(201).body(response);
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<ConfiguracaoUsuarioResponseDTO> editConfiguracao(@Valid @RequestBody ConfiguracaoEditDTO editDTO, @PathVariable UUID id){
        Configuracoes entity = ConfiguracoesMapper.toEntity(editDTO);
        Configuracoes edited = configuracoesService.editConfiguracao(entity, editDTO, id);
        ConfiguracaoUsuarioResponseDTO response = ConfiguracoesMapper.toDtoUser(edited);
        return ResponseEntity.status(200).body(response);
    }


}
