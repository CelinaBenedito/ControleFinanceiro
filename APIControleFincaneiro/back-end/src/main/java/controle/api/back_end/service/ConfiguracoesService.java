package controle.api.back_end.service;

import controle.api.back_end.exception.EntidadeJaExisteException;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.configuracoes.Configuracoes;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.ConfiguracoesRepository;
import controle.api.back_end.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ConfiguracoesService {

    private final ConfiguracoesRepository configuracoesRepository;
    private final UsuarioRepository usuarioRepository;

    public ConfiguracoesService(ConfiguracoesRepository configuracoesRepository, UsuarioRepository usuarioRepository) {
        this.configuracoesRepository = configuracoesRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<Configuracoes> getConfiguracoes() {
        return configuracoesRepository.findAll();
    }

    public Configuracoes getConfiguracoesById(UUID id) {
        return configuracoesRepository.findById(id).orElseThrow(()-> new EntidadeNaoEncontradaException(
                "Configuração de id: %s não encontrada"
                        .formatted(id)
                )
        );
    }

    public Configuracoes createConfiguracao(Configuracoes entity, UUID fkUsuario) {
        Usuario user = usuarioRepository.findById(fkUsuario).orElseThrow(() ->
                new EntidadeNaoEncontradaException(
                "Usuario de id: %s não encontrado"
                        .formatted(fkUsuario)
                )
        );

        if(configuracoesRepository.existsConfiguracoesByFkUsuario_Id(fkUsuario)){
            throw new EntidadeJaExisteException("Já existe uma configuração associada ao usuário informado");
        }

        LocalDate hoje = LocalDate.now();

        entity.setFkUsuario(user);
        entity.setUltimaAtualizacao(hoje);

        return configuracoesRepository.save(entity);
    }
}
