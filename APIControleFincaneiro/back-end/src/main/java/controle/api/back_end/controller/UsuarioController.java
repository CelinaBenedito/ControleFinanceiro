package controle.api.back_end.controller;

import controle.api.back_end.dto.usuario.UsuarioCreateDTO;
import controle.api.back_end.dto.usuario.UsuarioEditDTO;
import controle.api.back_end.dto.usuario.UsuarioLoginDTO;
import controle.api.back_end.dto.usuario.UsuarioResponseDTO;
import controle.api.back_end.dto.usuario.mapper.UsuarioMappper;
import controle.api.back_end.model.Usuario;
import controle.api.back_end.repository.UsuarioRepository;
import controle.api.back_end.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    public final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> getUsuarios(){
        List<Usuario> all = usuarioService.getUsuarios();
        if(all.isEmpty()){
            return ResponseEntity.noContent().build();
        }
        List<UsuarioResponseDTO> response = UsuarioMappper.toDto(all);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> getUsuarioById(@PathVariable UUID id){
        Usuario usuario = usuarioService.getUsuarioById(id);
        UsuarioResponseDTO response = UsuarioMappper.toDto(usuario);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> createUsuario(@Valid @RequestBody UsuarioCreateDTO dto){
        Usuario entity = UsuarioMappper.toEntity(dto);
        Usuario userCreated = usuarioService.createUsuario(entity);
        UsuarioResponseDTO response = UsuarioMappper.toDto(userCreated);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<UsuarioResponseDTO> loginUsuario(@Valid @RequestBody UsuarioLoginDTO dto){
        Usuario login = UsuarioMappper.toEntity(dto);
        Usuario usuarioEncontrado = usuarioService.LoginUsuario(login);
        UsuarioResponseDTO response = UsuarioMappper.toDto(usuarioEncontrado);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> editUsuario(@PathVariable UUID id, @Valid @RequestBody UsuarioEditDTO dto){
        Usuario usuarioEdicao = UsuarioMappper.toEntity(dto);
        Usuario usuarioEditado = usuarioService.editUsuario(id, usuarioEdicao);
        UsuarioResponseDTO response = UsuarioMappper.toDto(usuarioEditado);
        return ResponseEntity.ok(response);
    }


}
