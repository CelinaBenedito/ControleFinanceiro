package controle.api.back_end.service;

import controle.api.back_end.exception.EntidadeJaExisteException;
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
        Instituicao instituicaoByNomeContainingIgnoreCase = instituicaoRepository.findInstituicaoByNomeContainingIgnoreCase(entity.getNome());
        if (instituicaoByNomeContainingIgnoreCase != null){
            throw new EntidadeJaExisteException("Já existe uma instituição com o nome %s no banco de dados".formatted(entity.getNome()));
        }
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

        if (instituicaoUsuarioRepository.existsByFkUsuarioAndFkInstituicao(user.get(), instituicao.get())) {
            throw new IllegalArgumentException("Usuário já vinculado a esta instituição.");
        }
        InstituicaoUsuario instituicaoUsuario = new InstituicaoUsuario();
        instituicaoUsuario.setFkInstituicao(instituicao.get());
        instituicaoUsuario.setFkUsuario(user.get());
        instituicaoUsuario.setAtivo(true);
        return instituicaoUsuarioRepository.save(instituicaoUsuario);
    }

    public List<InstituicaoUsuario> getInstituicoesByUserId(UUID idUser) {
        if(!usuarioRepository.existsById(idUser)){
            throw new EntidadeNaoEncontradaException("Usuario de id: %s não encontrado".formatted(idUser));
        }
        return instituicaoUsuarioRepository.findInstituicaoUsuarioByFkUsuario_IdAndIsAtivoIsTrue(idUser);
    }

    public InstituicaoUsuario detachUserFromInstituicao(Integer instituicaoId, UUID userId) {
        if(!usuarioRepository.existsById(userId)){
            throw new EntidadeNaoEncontradaException("Usuario de id: %s não encontrado".formatted(userId));
        }
        if(!instituicaoRepository.existsById(instituicaoId)){
            throw new EntidadeNaoEncontradaException("Instituicao de id: %d não encontrado".formatted(instituicaoId));
        }
        InstituicaoUsuario instituicaoUsuario = instituicaoUsuarioRepository.findByFkUsuario_IdAndFkInstituicao_Id(userId,instituicaoId);
        instituicaoUsuario.setAtivo(false);
        return instituicaoUsuarioRepository.save(instituicaoUsuario);
    }
}
