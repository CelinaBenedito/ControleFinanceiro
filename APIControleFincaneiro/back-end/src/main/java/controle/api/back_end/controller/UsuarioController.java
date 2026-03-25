package controle.api.back_end.controller;

import controle.api.back_end.model.Usuario;
import controle.api.back_end.repository.usuario.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    public final UsuarioRepository repository;

    public UsuarioController(UsuarioRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<List<Usuario>> getUsuarios(){
        List<Usuario> all = repository.findAll();
        if(all.isEmpty()){
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(all);
    }
}
