package controle.api.back_end.service;

import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.Instituicao;
import controle.api.back_end.model.InstituicaoUsuario;
import controle.api.back_end.model.Usuario;
import controle.api.back_end.repository.InstituicaoRepository;
import controle.api.back_end.repository.InstituicaoUsuarioRepository;
import controle.api.back_end.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InstituicaoService {
    private final InstituicaoRepository instituicaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final InstituicaoUsuarioRepository instituicaoUsuarioRepository;

    public InstituicaoService(InstituicaoRepository instituicaoRepository,
                              UsuarioRepository usuarioRepository,
                              InstituicaoUsuarioRepository instituicaoUsuarioRepository) {
        this.instituicaoRepository = instituicaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.instituicaoUsuarioRepository = instituicaoUsuarioRepository;
    }

    public List<Instituicao> getInstituicoes() {
        return instituicaoRepository.findAll();
    }

    public Instituicao getInstituicaoById(Integer id) {
        return instituicaoRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                                "Instituicao de id: d% não encontrada".formatted(id)
                        )
                );
    }


    public Instituicao createInstituicao(Instituicao entity) {
        return instituicaoRepository.save(entity);
    }

    public void deleteInstituicao(Integer id) {
        if(instituicaoRepository.existsById(id)){
            instituicaoRepository.deleteInstituicaoById(id);

        }else {
            throw new EntidadeNaoEncontradaException("Instituição de id: %d não encontrada.".formatted(id));
        }
    }

    public InstituicaoUsuario createInstituicaoForUsuario(Integer instituicao_id, UUID user_id) {
        if(!usuarioRepository.existsById(user_id)){
            throw new EntidadeNaoEncontradaException("Usuario de id: %s não encontrado".formatted(user_id));
        }
        if(!instituicaoRepository.existsById(instituicao_id)){
            throw new EntidadeNaoEncontradaException("Instituição de id: %s não encontrado".formatted(instituicao_id));
        }

        Optional<Usuario> user = usuarioRepository.findById(user_id);
        Optional<Instituicao> instituicao = instituicaoRepository.findById(instituicao_id);
        InstituicaoUsuario instituicaoUsuario = new InstituicaoUsuario();
        instituicaoUsuario.setFkInstituicao(instituicao.get());
        instituicaoUsuario.setFkUsuario(user.get());
        return instituicaoUsuarioRepository.save(instituicaoUsuario);
    }
}
