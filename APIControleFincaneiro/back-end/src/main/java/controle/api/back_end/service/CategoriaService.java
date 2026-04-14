package controle.api.back_end.service;

import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.categoria.Categoria;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.CategoriaRepository;
import controle.api.back_end.repository.CategoriaUsuarioRepository;
import controle.api.back_end.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaUsuarioRepository categoriaUsuarioRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, UsuarioRepository usuarioRepository, CategoriaUsuarioRepository categoriaUsuarioRepository) {
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.categoriaUsuarioRepository = categoriaUsuarioRepository;
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

    public List<CategoriaUsuario> getByUserId(UUID userId) {
        if(!usuarioRepository.existsById(userId)){
            throw new EntidadeNaoEncontradaException(
                    "Usuario de id: %s não encontrado"
                            .formatted(userId)
            );
        }
        return categoriaUsuarioRepository.findAllByUsuario_Id(userId);
    }

    public Categoria createCategoria(Categoria entity) {

        return categoriaRepository.save(entity);
    }
    public List<Categoria> createCategoria(List<Categoria> entitys) {

        List<Categoria> categorias = new ArrayList<>();
        for(Categoria c : entitys){
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

    public CategoriaUsuario createCategoriaForUser(Integer categoriaId, UUID usuarioId) {
        CategoriaUsuario categoriaUsuario = new CategoriaUsuario();

        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow(() ->
                new EntidadeNaoEncontradaException(
                        "Usuario de id: %s não encontrado"
                                .formatted(usuarioId)
                )
        );

        Categoria categoria = categoriaRepository.findById(categoriaId).orElseThrow(() ->
                new EntidadeNaoEncontradaException(
                        "Categoria de id: %d não encontrada"
                                .formatted(categoriaId)
                )
        );

        categoriaUsuario.setUsuario(usuario);
        categoriaUsuario.setCategoria(categoria);
        categoriaUsuario.setAtivo(true);

        return categoriaUsuarioRepository.save(categoriaUsuario);
    }

    public CategoriaUsuario detachUserFromCategoria(Integer categoriad, UUID usuarioId){
        if (!categoriaUsuarioRepository.existsByCategoria_IdOrUsuario_Id(categoriad, usuarioId)){
            throw new EntidadeNaoEncontradaException("Entidade não foi encontrada!");
        }
        CategoriaUsuario byUsuarioIdAndCategoriaId = categoriaUsuarioRepository.findByUsuario_idAndCategoria_id(usuarioId, categoriad);
        byUsuarioIdAndCategoriaId.setAtivo(false);
        return categoriaUsuarioRepository.save(byUsuarioIdAndCategoriaId);
    }
}
