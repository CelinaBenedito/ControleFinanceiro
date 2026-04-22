package controle.api.back_end.controller;

import controle.api.back_end.dto.usuario.in.UsuarioCreateDTO;
import controle.api.back_end.dto.usuario.in.UsuarioEditDTO;
import controle.api.back_end.dto.usuario.in.UsuarioLoginDTO;
import controle.api.back_end.dto.usuario.out.UsuarioResponseDTO;
import controle.api.back_end.dto.usuario.mapper.UsuarioMappper;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    public final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @Operation(summary = "Buscar todos os usuarios",
            description = "Busca todos os usuarios registrados no banco de dados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca de dados feita com sucesso e retornou com dados"),
            @ApiResponse(responseCode = "204", description = "Busca de dados feita com sucesso e não retornou dados")
    })
    public ResponseEntity<List<UsuarioResponseDTO>> getUsuarios(){
        List<Usuario> all = usuarioService.getUsuarios();
        if(all.isEmpty()){
            return ResponseEntity.noContent().build();
        }
        List<UsuarioResponseDTO> response = UsuarioMappper.toDto(all);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar o usuario que contém o id desejado ",
            description = "Busca no banco de dados o usuario com o id desejado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca de dados feita com sucesso e retornou com dados"),
            @ApiResponse(responseCode = "404", description = "Usuario não encontrado.")
    })
    public ResponseEntity<UsuarioResponseDTO> getUsuarioById(@PathVariable UUID id){
        Usuario usuario = usuarioService.getUsuarioById(id);
        UsuarioResponseDTO response = UsuarioMappper.toDto(usuario);

        return ResponseEntity.ok(response);
    }
    @GetMapping("/saldo/usuarios/{user_id}")
    public ResponseEntity<BigDecimal> getSaldoByUsuario(@PathVariable UUID user_id){
        BigDecimal saldoByUsuario = usuarioService
                .getSaldoByUsuario(user_id);
        return ResponseEntity.status(200).body(saldoByUsuario);
    }

    @PostMapping
    @Operation(summary = "Adicionar um novo usuario.",
            description = "Cria um novo usuario no banco de dados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario criado com sucesso!"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos.")
    })
    public ResponseEntity<UsuarioResponseDTO> createUsuario(@Valid @RequestBody UsuarioCreateDTO dto){
        Usuario entity = UsuarioMappper.toEntity(dto);
        Usuario userCreated = usuarioService.createUsuario(entity);
        usuarioService.createConfiguracao(userCreated);
        UsuarioResponseDTO response = UsuarioMappper.toDto(userCreated);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Logar um usuario.",
            description = "Busca pelo email e a senha o usuário no banco de dados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario encontrado!"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos.")
    })
    public ResponseEntity<UsuarioResponseDTO> loginUsuario(@Valid @RequestBody UsuarioLoginDTO dto){
        Usuario login = UsuarioMappper.toEntity(dto);
        Usuario usuarioEncontrado = usuarioService.LoginUsuario(login);
        UsuarioResponseDTO response = UsuarioMappper.toDto(usuarioEncontrado);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar as informações do usuário.",
            description = "Editar de um usuario já presente no banco de dados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario editado com sucesso!"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos.")
    })
    public ResponseEntity<UsuarioResponseDTO> editUsuario(@PathVariable UUID id, @Valid @RequestBody UsuarioEditDTO dto){
        Usuario usuarioEdicao = UsuarioMappper.toEntity(dto);
        Usuario usuarioEditado = usuarioService.editUsuario(id, usuarioEdicao);
        UsuarioResponseDTO response = UsuarioMappper.toDto(usuarioEditado);
        return ResponseEntity.ok(response);
    }

}