package controle.api.back_end.controller;

import controle.api.back_end.dto.categoria.CategoriaCreateDTO;
import controle.api.back_end.dto.categoria.CategoriaResponseDTO;
import controle.api.back_end.dto.categoria.CategoriasResponsesDTO;
import controle.api.back_end.dto.categoria.mapper.CategoriaMapper;
import controle.api.back_end.model.Categoria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import controle.api.back_end.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/categorias")
public class CategoriaController {
    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    @Operation(summary = "Listar categorias",
            description = "Busca todas as categorias no banco de dados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca de dados feita com sucesso e retornou com dados."),
            @ApiResponse(responseCode = "204", description = "Busca de dados feita com sucesso e retornou sem dados.")
    })
    public ResponseEntity<List<CategoriaResponseDTO>> getCategorias(){
        List<Categoria> all = categoriaService.getCategorias();
        if(all.isEmpty()){
            return ResponseEntity.noContent().build();
        }
        List<CategoriaResponseDTO> response = CategoriaMapper.toDto(all);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca a categoria desejada por id",
            description = "Busca a categorias no banco de dados com base no id que possue.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca de dados feita com sucesso e retornou com dados."),
            @ApiResponse(responseCode = "404", description = "Categoria não encontrada.")
    })
    public ResponseEntity<CategoriasResponsesDTO> getById(@PathVariable Integer id){
        Categoria byId = categoriaService.getById(id);
        CategoriasResponsesDTO response = CategoriaMapper.toDtoUser(byId);
        return ResponseEntity.status(200).body(response);
    }

    @GetMapping("/usuario/{user_id}")
    @Operation(summary = "Listar categorias por usuario",
            description = "Busca as categorias no banco de dados baseado no id do usuário que as possue.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca de dados feita com sucesso e retornou com dados."),
            @ApiResponse(responseCode = "204", description = "Busca de dados feita com sucesso e retornou sem dados."),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.")
    })
    public ResponseEntity<List<CategoriasResponsesDTO>> getByUserId(@PathVariable UUID user_id){
        List<Categoria> byUserId = categoriaService.getByUserId(user_id);
        if(byUserId.isEmpty()){
            return ResponseEntity.status(204).build();
        }
        List<CategoriasResponsesDTO> response = CategoriaMapper.toDtoUser(byUserId);
        return ResponseEntity.status(200).body(response);
    }

    @PostMapping
    @Operation(summary = "Adicionar uma categoria",
            description = "Cria uma nova categoria no banco de dados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Categoria criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<CategoriaResponseDTO> createCategoria(
            @Valid @RequestBody CategoriaCreateDTO dto){
        Categoria entity = CategoriaMapper.toEntity(dto);
        Categoria categoriaCreated = categoriaService.createCategoria(entity, dto.getFkUsuario());
        CategoriaResponseDTO response = CategoriaMapper.toDto(categoriaCreated);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/lote")
    @Operation(summary = "Adicionar várias categorias",
            description = "Cria múltiplas categorias de uma vez no banco de dados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Categorias criadas com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<List<CategoriaResponseDTO>> createCategoria(
            @Valid @RequestBody List<CategoriaCreateDTO> dtos){
        List<Categoria> entitys = CategoriaMapper.toEntity(dtos);
        List<Categoria> categoriaCreated = categoriaService.createCategoria(entitys, dtos.getFirst().getFkUsuario());
        List<CategoriaResponseDTO> response = CategoriaMapper.toDto(categoriaCreated);
        return ResponseEntity.status(201).body(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar uma categoria",
            description = "Deletar uma categoria por seu id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Categorias deletada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Categoria não encontrada")
    })
    public ResponseEntity<CategoriaResponseDTO> deleteCategoria(@PathVariable Integer id){
        categoriaService.deleteCategoria(id);
        return ResponseEntity.status(204).build();
    }

}
