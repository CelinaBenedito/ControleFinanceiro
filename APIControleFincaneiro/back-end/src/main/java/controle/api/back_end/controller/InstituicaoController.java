package controle.api.back_end.controller;

import controle.api.back_end.dto.instituicao.InstituicaoCreateDTO;
import controle.api.back_end.dto.instituicao.InstituicaoResponseDTO;
import controle.api.back_end.dto.instituicao.mapper.InstituicaoMapper;
import controle.api.back_end.model.Instituicao;
import controle.api.back_end.model.Usuario;
import controle.api.back_end.repository.InstituicaoRepository;
import controle.api.back_end.service.InstituicaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/instituicoes")
public class InstituicaoController {

    private final InstituicaoService instituicaoService;

    public InstituicaoController(InstituicaoService instituicaoService) {
        this.instituicaoService = instituicaoService;
    }


    @GetMapping
    @Operation(summary = "Buscar todas as intituições ",
            description = "Busca todas as instituições registradas no banco de dados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca de dados feita com sucesso e retornou com dados"),
            @ApiResponse(responseCode = "204", description = "Busca de dados feita com sucesso e não retornou dados")
    })
    public ResponseEntity<List<InstituicaoResponseDTO>> getInstituicoes(){
        List<Instituicao> all = instituicaoService.getInstituicoes();
        if(all.isEmpty()){
            return ResponseEntity.noContent().build();
        }
        List<InstituicaoResponseDTO> response = InstituicaoMapper.toDto(all);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{id}")
    @Operation(summary = "Buscar a insituição que contém o id desejado ",
            description = "Busca no banco de dados a instituição com o id desejado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca de dados feita com sucesso e retornou com dados"),
            @ApiResponse(responseCode = "404", description = "Instituição não encontrada")
    })
    public ResponseEntity<InstituicaoResponseDTO> getInsituicaoById(@PathVariable Integer id){
        Instituicao entity = instituicaoService.getInstituicaoById(id);
        InstituicaoResponseDTO response = InstituicaoMapper.toDto(entity);
        return ResponseEntity.status(200).body(response);
    }

    @PostMapping
    @Operation(summary = "Adicionar uma instituição",
            description = "Cria uma nova instituição no banco de dados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Instituição criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<InstituicaoResponseDTO> createInstituicao(@Valid @RequestBody InstituicaoCreateDTO dto){
        Instituicao entity = InstituicaoMapper.toEntity(dto);
        Instituicao created = instituicaoService.createInstituicao(entity);
        InstituicaoResponseDTO response = InstituicaoMapper.toDto(created);
        return ResponseEntity.status(201).body(response);
    }

    @DeleteMapping("{id}")
    @Operation(summary = "Deletar uma instituição",
            description = "Deletar uma instituição por seu id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Instituição deletada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Instituição não encontrada")
    })
    public ResponseEntity<InstituicaoResponseDTO> deleteInstituicao(@PathVariable Integer id){
        instituicaoService.deleteInstituicao(id);
        return ResponseEntity.status(204).build();
    }
}
