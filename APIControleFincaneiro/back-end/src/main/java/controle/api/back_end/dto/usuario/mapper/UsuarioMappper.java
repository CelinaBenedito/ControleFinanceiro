package controle.api.back_end.dto.usuario.mapper;

import controle.api.back_end.dto.usuario.UsuarioCreateDTO;
import controle.api.back_end.dto.usuario.UsuarioEditDTO;
import controle.api.back_end.dto.usuario.UsuarioLoginDTO;
import controle.api.back_end.dto.usuario.UsuarioResponseDTO;
import controle.api.back_end.model.usuario.Usuario;
import jakarta.validation.Valid;

import java.util.List;

public class UsuarioMappper {

    public static UsuarioResponseDTO toDto(Usuario usuario) {
        if(usuario == null){
            return null;
        }

        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setId(usuario.getId());
        dto.setNome(usuario.getNome());
        dto.setSobrenome(usuario.getSobrenome());
        dto.setSexo(usuario.getSexo());
        dto.setImagem(usuario.getImagem());
        dto.setDataNascimento(usuario.getDataNascimento());

        return dto;
    }

    public static List<UsuarioResponseDTO> toDto(List<Usuario> usuarios){
        return usuarios.stream()
                .map(UsuarioMappper::toDto)
                .toList();
    }

    public static Usuario toEntity(@Valid UsuarioCreateDTO dto) {
        if(dto == null){
            return null;
        }

        Usuario entity = new Usuario();
        entity.setNome(dto.getNome());
        entity.setSobrenome(dto.getSobrenome());
        entity.setSexo(dto.getSexo());
        entity.setDataNascimento(dto.getDataNascimento());
        entity.setEmail(dto.getEmail());
        entity.setSenha(dto.getSenha());

        return entity;
    }

    public static Usuario toEntity(@Valid UsuarioLoginDTO dto) {
        if(dto == null){
            return null;
        }

        Usuario entity = new Usuario();
        entity.setEmail(dto.getEmail());
        entity.setSenha(dto.getSenha());

        return entity;
    }

    public static Usuario toEntity(@Valid UsuarioEditDTO dto) {

        if(dto == null){
            return null;
        }

        Usuario entity = new Usuario();
        entity.setNome(dto.getNome());
        entity.setSobrenome(dto.getSobrenome());
        entity.setSexo(dto.getSexo());
        entity.setDataNascimento(dto.getDataNascimento());
        entity.setImagem(dto.getImagem());
        entity.setEmail(dto.getEmail());

        return entity;
    }

    public static Usuario toEdit(@Valid Usuario editUser, Usuario actualUser){
        Usuario user = new Usuario();

        user.setId(actualUser.getId());
        user.setSenha(actualUser.getSenha());

        //NOME
        if(editUser.getNome()== null){user.setNome(actualUser.getNome());}
        else{user.setNome(editUser.getNome());}

        //SOBRENOME
        if(editUser.getSobrenome()== null){user.setSobrenome(actualUser.getSobrenome());}
        else{user.setSobrenome(editUser.getSobrenome());}

        //EMAIL
        if(editUser.getEmail()== null){user.setEmail(actualUser.getEmail());}
        else{user.setEmail(editUser.getEmail());}

        //DATA DE NASCIMENTO
        if(editUser.getDataNascimento()== null){user.setDataNascimento(actualUser.getDataNascimento());}
        else{user.setDataNascimento(editUser.getDataNascimento());}

        //SEXO
        if(editUser.getSexo()== null){user.setSexo(actualUser.getSexo());}
        else{user.setSexo(editUser.getSexo());}

        //IMAGEM
        if(editUser.getImagem()== null){user.setImagem(actualUser.getImagem());}
        else{user.setImagem(editUser.getImagem());}

        return user;
    }
}
