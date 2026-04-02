package controle.api.back_end.dto.categoria.mapper;

import controle.api.back_end.dto.categoria.CategoriaCreateDTO;
import controle.api.back_end.dto.categoria.CategoriaResponseDTO;
import controle.api.back_end.dto.categoria.CategoriasResponsesDTO;
import controle.api.back_end.model.categoria.Categoria;
import controle.api.back_end.model.usuario.Usuario;
import jakarta.validation.Valid;

import java.util.List;

public class CategoriaMapper {

    public static CategoriaResponseDTO toDto(Categoria entity){
        if(entity == null){
            return null;
        }
        CategoriaResponseDTO response = new CategoriaResponseDTO();
        response.setId(entity.getId());
        response.setTitulo(entity.getTitulo());

        return response;
    }

    public static List<CategoriaResponseDTO> toDto(List<Categoria> entitys){
        return entitys.stream()
                .map(CategoriaMapper::toDto)
                .toList();
    }

    public static CategoriasResponsesDTO toDtoUser(Categoria entity) {
        if(entity == null){
            return null;
        }
        CategoriasResponsesDTO response = new CategoriasResponsesDTO();
        Usuario user = entity.getUsuario();

        CategoriasResponsesDTO.UsuarioCategoriaDTO usuarioCategoria = new CategoriasResponsesDTO.UsuarioCategoriaDTO();

        usuarioCategoria.setId(user.getId());
        usuarioCategoria.setNome(user.getNome());
        usuarioCategoria.setSobrenome(user.getSobrenome());
        usuarioCategoria.setDataNascimento(user.getDataNascimento());
        usuarioCategoria.setSexo(user.getSexo());
        usuarioCategoria.setEmail(user.getEmail());

        response.setId(entity.getId());
        response.setTitulo(entity.getTitulo());
        response.setUsuario(usuarioCategoria);

        return response;
    }


    public static List<CategoriasResponsesDTO> toDtoUser(List<Categoria> entitys) {
        return entitys.stream()
                .map(CategoriaMapper::toDtoUser)
                .toList();
    }

    public static Categoria toEntity(@Valid CategoriaCreateDTO dto) {
        Categoria entity = new Categoria();

        entity.setTitulo(dto.getTitulo());
        return entity;
    }

    public static List<Categoria> toEntity(@Valid List<CategoriaCreateDTO> dtos){
        return dtos.stream()
                .map(CategoriaMapper::toEntity)
                .toList();
    }
}
