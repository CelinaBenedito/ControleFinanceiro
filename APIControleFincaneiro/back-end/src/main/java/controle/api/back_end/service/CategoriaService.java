package controle.api.back_end.service;

import controle.api.back_end.exception.EntidadeJaExisteException;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.Categoria;
import controle.api.back_end.model.Usuario;
import controle.api.back_end.repository.CategoriaRepository;
import controle.api.back_end.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, UsuarioRepository usuarioRepository) {
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<Categoria> getCategorias() {
        return categoriaRepository.findAll();
    }

    public Categoria getById(Integer id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Categoria de id: d% não encontrada".formatted(id)
                        )
                );
    }

    public List<Categoria> getByUserId(UUID userId) {
        if(!usuarioRepository.existsById(userId)){
            throw new EntidadeNaoEncontradaException(
                    "Usuario de id: %s não encontrado"
                            .formatted(userId)
            );
        }
        List<Categoria> categorias = categoriaRepository.findAllByUsuarioId(userId);
        return categorias;
    }

    public Categoria createCategoria(Categoria entity, UUID fkUsuario) {
        if (!usuarioRepository.existsById(fkUsuario)){
            throw new EntidadeNaoEncontradaException("Usuario de id: %s não encontrado".formatted(fkUsuario));
        }
        Optional<Usuario> byId = usuarioRepository.findById(fkUsuario);
        entity.setUsuario(byId.get());
        return categoriaRepository.save(entity);
    }
    public List<Categoria> createCategoria(List<Categoria> entitys, UUID fkUsuario) {
        if (!usuarioRepository.existsById(fkUsuario)){
            throw new EntidadeNaoEncontradaException("Usuario de id: %s não encontrado".formatted(fkUsuario));
        }
        Optional<Usuario> byId = usuarioRepository.findById(fkUsuario);
        List<Categoria> categorias = new ArrayList<>();
        for(Categoria c : entitys){
            c.setUsuario(byId.get());
            categoriaRepository.save(c);
            categorias.add(c);
        }
        return categorias;
    }


    public void deleteCategoria(Integer id) {
        if(categoriaRepository.existsById(id)){
            categoriaRepository.deleteCategoriaById(id);

        }else {
            throw new EntidadeNaoEncontradaException("Categoria de id: %d não encontrada.".formatted(id));
        }
    }
}
