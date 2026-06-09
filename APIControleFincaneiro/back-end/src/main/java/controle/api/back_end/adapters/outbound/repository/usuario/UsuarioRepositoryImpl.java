package controle.api.back_end.adapters.outbound.repository.usuario;

import controle.api.back_end.adapters.outbound.entitys.JpaUsuarioEntity;
import controle.api.back_end.domain.usuario.Usuario;
import controle.api.back_end.domain.usuario.UsuarioRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UsuarioRepositoryImpl implements UsuarioRepository {

    private final JpaUsuarioRepository jpaUsuarioRepository;

    public UsuarioRepositoryImpl(JpaUsuarioRepository jpaUsuarioRepository) {
        this.jpaUsuarioRepository = jpaUsuarioRepository;
    }

    @Override
    public Usuario save(Usuario usuario) {
        JpaUsuarioEntity entity = new JpaUsuarioEntity(usuario);
        jpaUsuarioRepository.save(entity);
        return new Usuario(
                entity.getId(),
                entity.getNome(),
                entity.getSobrenome(),
                entity.getDataNascimento(),
                entity.getSexo(),
                entity.getImagem(),
                entity.getEmail(),
                entity.getSenha()
        );
    }

    @Override
    public Optional<Usuario> findById(UUID id) {
        Optional<JpaUsuarioEntity> usuarioEntity = this.jpaUsuarioRepository.findById(id);
        return usuarioEntity.map(entity -> new Usuario(
                entity.getId(),
                entity.getNome(),
                entity.getSobrenome(),
                entity.getDataNascimento(),
                entity.getSexo(),
                entity.getImagem(),
                entity.getEmail(),
                entity.getSenha()
                )
        );
    }

    @Override
    public List<Usuario> findAll() {
        return jpaUsuarioRepository.findAll()
                .stream()
                .map(entity -> new Usuario(
                        entity.getId(),
                        entity.getNome(),
                        entity.getSobrenome(),
                        entity.getDataNascimento(),
                        entity.getSexo(),
                        entity.getImagem(),
                        entity.getEmail(),
                        entity.getSenha()
                ))
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaUsuarioRepository.deleteById(id);
    }
}
