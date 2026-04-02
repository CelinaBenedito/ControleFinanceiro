package controle.api.back_end.controller;

import controle.api.back_end.model.configuracoes.Configuracoes;
import controle.api.back_end.repository.ConfiguracoesRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/configuracoes")
public class ConfiguracoesController {

    private final ConfiguracoesRepository repository;

    public ConfiguracoesController(ConfiguracoesRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<List<Configuracoes>> getConfiguracoes(){
        List<Configuracoes> all = repository.findAll();
        if(all.isEmpty()){
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(all);
    }


}
