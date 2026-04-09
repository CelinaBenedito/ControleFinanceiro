package controle.api.back_end.dto.categoria.mapper;

import controle.api.back_end.dto.categoria.CategoriaCreateDTO;
import controle.api.back_end.dto.categoria.CategoriaResponseDTO;
import controle.api.back_end.dto.categoria.CategoriaUsuarioResponseDTO;
import controle.api.back_end.model.categoria.Categoria;
import controle.api.back_end.model.categoria.CategoriaUsuario;
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

    public static CategoriaUsuarioResponseDTO toDtoUser(CategoriaUsuario entity){
        if(entity == null){
            return null;
        }
        //Criando as instâncias das classes para fazer o mapeamento.
        CategoriaUsuarioResponseDTO dto = new CategoriaUsuarioResponseDTO();
        CategoriaUsuarioResponseDTO.UsuarioDTO dtoUser = new CategoriaUsuarioResponseDTO.UsuarioDTO();
        CategoriaUsuarioResponseDTO.CategoriaDTO dtoCategoria = new CategoriaUsuarioResponseDTO.CategoriaDTO();

        //Pegando os dados da categoria na entidade e adicionando a um dto parceiro.
        dtoCategoria.setId(entity.getCategoria().getId());
        dtoCategoria.setTitulo(entity.getCategoria().getTitulo());

        //Pegando os dados do usuario na entidade e adicionando a um dto parceiro.
        dtoUser.setId(entity.getUsuario().getId());
        dtoUser.setNome(entity.getUsuario().getNome());
        dtoUser.setSobrenome(entity.getUsuario().getSobrenome());
        dtoUser.setDataNascimento(entity.getUsuario().getDataNascimento());
        dtoUser.setSexo(entity.getUsuario().getSexo());

        //Preenchendo o DTO principal com os dados da entidade principal e do DTO parceiro.
        dto.setId(entity.getId());
        dto.setCategoria(dtoCategoria);
        dto.setUsuario(dtoUser);
        dto.setAtivo(entity.getAtivo());

        return dto;
    }

    public static List<CategoriaUsuarioResponseDTO> toDtoUser(List<CategoriaUsuario> entitys){
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
