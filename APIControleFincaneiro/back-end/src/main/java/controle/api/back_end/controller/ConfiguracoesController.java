package controle.api.back_end.controller;

import controle.api.back_end.dto.configuracoes.in.*;
import controle.api.back_end.dto.configuracoes.mapper.ConfiguracoesMapper;
import controle.api.back_end.dto.configuracoes.out.ConfiguracaoUsuarioResponseDTO;
import controle.api.back_end.dto.configuracoes.out.ConfiguracoesResponsesDTO;
import controle.api.back_end.dto.upload.ImportResultDto;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.configuracoes.Configuracoes;
import controle.api.back_end.model.configuracoes.LimitePorCategoria;
import controle.api.back_end.model.configuracoes.LimitePorInstituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.service.ConfiguracoesService;
import controle.api.back_end.service.UploadService;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/configuracoes")
@Tag(name = "Configurações", description = "Endpoints referentes as configurações dos usuários.")
public class ConfiguracoesController {

    private final ConfiguracoesService configuracoesService;
    private final UploadService uploadService;

    public ConfiguracoesController(ConfiguracoesService configuracoesService,
                                   UploadService uploadService) {
        this.configuracoesService = configuracoesService;
        this.uploadService = uploadService;
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

    @PostMapping(value = "/upload-arquivo/usuarios/{user_id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importar registros a partir de um arquivo",
            description = "Importa eventos financeiros a partir de arquivos nos formatos:\n" +
                    "- **JSON / SQL / Excel (.xlsx)**: gerados pelo endpoint `/download` da aplicação\n" +
                    "- **OFX / QFX**: extrato bancário padrão Open Financial Exchange (todos os bancos)\n" +
                    "- **CSV**: extrato bancário CSV (Inter e bancos com layout Data;Descrição;Valor;Saldo)\n" +
                    "- **PDF**: extrato bancário PDF (melhor esforço — prefira OFX ou CSV)\n\n" +
                    "Para formatos de extrato bancário (OFX, CSV, PDF), use o parâmetro `bancoNome` " +
                    "para vincular as transações à instituição cadastrada do usuário " +
                    "(ex: `bancoNome=Inter`). No OFX, a tag `<ORG>` é usada automaticamente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Importação concluída (verifique o campo 'erros' para falhas parciais).",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImportResultDto.class))),
            @ApiResponse(responseCode = "400", description = "Arquivo vazio ou formato não suportado.",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.",
                    content = @Content)
    })
    public ResponseEntity<ImportResultDto> postArquivo(
            @PathVariable UUID user_id,
            @RequestParam MultipartFile arquivo,
            @RequestParam(required = false) String bancoNome) throws IOException {

        if (arquivo.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String contentType = arquivo.getContentType() != null
                ? arquivo.getContentType().toLowerCase() : "";
        String nomeArquivo = arquivo.getOriginalFilename() != null
                ? arquivo.getOriginalFilename().toLowerCase() : "";

        byte[] bytes = arquivo.getBytes();
        ImportResultDto resultado;

        if (contentType.contains("json") || nomeArquivo.endsWith(".json")) {
            resultado = uploadService.importFromJson(user_id, bytes);

        } else if (contentType.contains("sql") || nomeArquivo.endsWith(".sql")) {
            resultado = uploadService.importFromSql(user_id, bytes);

        } else if (contentType.contains("spreadsheetml") || contentType.contains("excel")
                || nomeArquivo.endsWith(".xlsx") || nomeArquivo.endsWith(".xls")) {
            resultado = uploadService.importFromExcel(user_id, bytes);

        } else if (nomeArquivo.endsWith(".ofx") || nomeArquivo.endsWith(".qfx")
                || contentType.contains("ofx") || contentType.contains("x-ofx")) {
            // OFX: usa <ORG> internamente + bancoNome como fallback
            resultado = uploadService.importFromOfxWithBank(user_id, bytes, bancoNome);

        } else if (nomeArquivo.endsWith(".csv")
                || contentType.contains("csv") || contentType.contains("comma-separated")) {
            resultado = uploadService.importFromBankStatementCsv(user_id, bytes, bancoNome);

        } else if (contentType.contains("pdf") || nomeArquivo.endsWith(".pdf")) {
            // PDF: se bancoNome fornecido ou nome do arquivo sugere extrato → trata como bancário
            boolean isBankStatement = (bancoNome != null && !bancoNome.isBlank())
                    || nomeArquivo.contains("extrato") || nomeArquivo.contains("statement");
            if (isBankStatement) {
                resultado = uploadService.importFromBankStatementPdf(user_id, bytes, bancoNome);
            } else {
                // Tenta como exportação da própria aplicação
                resultado = uploadService.importFromPdf(user_id, bytes);
                // Fallback: se não importou nada, tenta como extrato bancário
                if (resultado.getTotalImportados() == 0 && resultado.getErros().isEmpty()) {
                    resultado = uploadService.importFromBankStatementPdf(user_id, bytes, bancoNome);
                }
            }

        } else {
            // Inferência pelo conteúdo
            String preview = new String(bytes, 0, Math.min(bytes.length, 200)).trim();
            if (preview.startsWith("{") || preview.startsWith("[")) {
                resultado = uploadService.importFromJson(user_id, bytes);
            } else if (preview.startsWith("--") || preview.toUpperCase().startsWith("INSERT")) {
                resultado = uploadService.importFromSql(user_id, bytes);
            } else if (preview.startsWith("PK")) {
                resultado = uploadService.importFromExcel(user_id, bytes);
            } else if (preview.toUpperCase().contains("OFXHEADER") || preview.toUpperCase().contains("<OFX>")) {
                resultado = uploadService.importFromOfxWithBank(user_id, bytes, bancoNome);
            } else if (preview.toLowerCase().contains("data") && preview.contains(";")) {
                resultado = uploadService.importFromBankStatementCsv(user_id, bytes, bancoNome);
            } else {
                return ResponseEntity.badRequest().build();
            }
        }

        return ResponseEntity.ok(resultado);
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
