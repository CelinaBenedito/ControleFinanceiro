package controle.api.back_end.controller;

import controle.api.back_end.model.Instituicao;
import controle.api.back_end.model.Usuario;
import controle.api.back_end.repository.InstituicaoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/instituicoes")
public class InstituicaoController {

    private final InstituicaoRepository repository;

    public InstituicaoController(InstituicaoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<List<Instituicao>> getInstituicoes(){
        List<Instituicao> all = repository.findAll();
        if(all.isEmpty()){
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(all);
    }
}
