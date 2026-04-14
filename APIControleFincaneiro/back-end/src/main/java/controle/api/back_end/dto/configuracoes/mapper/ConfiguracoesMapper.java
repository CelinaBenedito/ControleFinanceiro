package controle.api.back_end.dto.configuracoes.mapper;

import controle.api.back_end.dto.configuracoes.ConfiguracaoEditDTO;
import controle.api.back_end.dto.configuracoes.ConfiguracaoUsuarioResponseDTO;
import controle.api.back_end.dto.configuracoes.ConfiguracoesCreateDTO;
import controle.api.back_end.dto.configuracoes.ConfiguracoesResponsesDTO;
import controle.api.back_end.model.configuracoes.Configuracoes;
import controle.api.back_end.model.configuracoes.LimitePorCategoria;
import controle.api.back_end.model.configuracoes.LimitePorInstituicao;
import controle.api.back_end.model.usuario.Usuario;
import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.List;

public class ConfiguracoesMapper {

    public static ConfiguracoesResponsesDTO toDto(Configuracoes model) {
        if(model == null){
            return null;
        }

        ConfiguracoesResponsesDTO dto = new ConfiguracoesResponsesDTO();

        dto.setId(model.getId());
        dto.setInicioMesFiscal(model.getInicioMesFiscal());
        dto.setLimiteDesejadoMensal(model.getLimiteDesejadoMensal());
        dto.setUltimaAtualizacao(model.getUltimaAtualizacao());

        return dto;
    }
    public static List<ConfiguracoesResponsesDTO> toDto(List<Configuracoes> models){
        return models.stream()
                .map(ConfiguracoesMapper::toDto)
                .toList();
    }

    public static ConfiguracaoUsuarioResponseDTO toDtoUser(Configuracoes model) {
        if (model == null) {
            return null;
        }

        Usuario modelUser = model.getUsuario();
        ConfiguracaoUsuarioResponseDTO dto = new ConfiguracaoUsuarioResponseDTO();

        // USUÁRIO
        ConfiguracaoUsuarioResponseDTO.ConfiguracaoUsuarioDTO dtoUser = new ConfiguracaoUsuarioResponseDTO.ConfiguracaoUsuarioDTO();
        dtoUser.setId(modelUser.getId());
        dtoUser.setNome(modelUser.getNome());
        dtoUser.setSobrenome(modelUser.getSobrenome());
        dtoUser.setEmail(modelUser.getEmail());

        dto.setId(model.getId());
        dto.setUsuario(dtoUser);
        dto.setInicioMesFiscal(model.getInicioMesFiscal());
        dto.setLimiteDesejadoMensal(model.getLimiteDesejadoMensal());
        dto.setUltimaAtualizacao(model.getUltimaAtualizacao());

        // LIMITE POR CATEGORIA (lista)
        List<ConfiguracaoUsuarioResponseDTO.LimiteCategoriaDTO> limitesCategoriaDTO = new ArrayList<>();
        if (model.getLimitePorCategoria() != null) {
            for (LimitePorCategoria limite : model.getLimitePorCategoria()) {
                ConfiguracaoUsuarioResponseDTO.LimiteCategoriaDTO limiteCategoriaDTO = new ConfiguracaoUsuarioResponseDTO.LimiteCategoriaDTO();

                ConfiguracaoUsuarioResponseDTO.LimiteCategoriaDTO.CategoriaUsuarioDTO categoriaUsuarioDTO =
                        new ConfiguracaoUsuarioResponseDTO.LimiteCategoriaDTO.CategoriaUsuarioDTO();

                ConfiguracaoUsuarioResponseDTO.LimiteCategoriaDTO.CategoriaUsuarioDTO.CategoriaDTO categoriaDTO =
                        new ConfiguracaoUsuarioResponseDTO.LimiteCategoriaDTO.CategoriaUsuarioDTO.CategoriaDTO();

                ConfiguracaoUsuarioResponseDTO.LimiteCategoriaDTO.CategoriaUsuarioDTO.UsuarioDTO usuarioDTO =
                        new ConfiguracaoUsuarioResponseDTO.LimiteCategoriaDTO.CategoriaUsuarioDTO.UsuarioDTO();

                usuarioDTO.setId(limite.getCategoriaUsuario().getUsuario().getId());
                usuarioDTO.setNome(modelUser.getNome());
                usuarioDTO.setSobrenome(modelUser.getSobrenome());
                usuarioDTO.setSexo(modelUser.getSexo());
                usuarioDTO.setDataNascimento(modelUser.getDataNascimento());

                categoriaDTO.setId(limite.getCategoriaUsuario().getCategoria().getId());
                categoriaDTO.setTitulo(limite.getCategoriaUsuario().getCategoria().getTitulo());

                categoriaUsuarioDTO.setCategoria(categoriaDTO);
                categoriaUsuarioDTO.setUsuario(usuarioDTO);

                limiteCategoriaDTO.setCategoriaUsuario(categoriaUsuarioDTO);
                limiteCategoriaDTO.setLimiteDesejado(limite.getLimiteDesejado());

                limitesCategoriaDTO.add(limiteCategoriaDTO);
            }
        }
        dto.setLimitePorCategoria(limitesCategoriaDTO);

        // LIMITE POR INSTITUIÇÃO (lista)
        List<ConfiguracaoUsuarioResponseDTO.LimiteInstituicaoDTO> limitesInstituicaoDTO = new ArrayList<>();
        if (model.getLimitePorInstituicao() != null) {
            for (LimitePorInstituicao limite : model.getLimitePorInstituicao()) {
                ConfiguracaoUsuarioResponseDTO.LimiteInstituicaoDTO limiteInstituicaoDTO =
                        new ConfiguracaoUsuarioResponseDTO.LimiteInstituicaoDTO();

                ConfiguracaoUsuarioResponseDTO.LimiteInstituicaoDTO.InstituicaoUsuarioDTO instituicaoUsuarioDTO =
                        new ConfiguracaoUsuarioResponseDTO.LimiteInstituicaoDTO.InstituicaoUsuarioDTO();

                ConfiguracaoUsuarioResponseDTO.LimiteInstituicaoDTO.InstituicaoUsuarioDTO.InstituicaoDTO instituicaoDTO =
                        new ConfiguracaoUsuarioResponseDTO.LimiteInstituicaoDTO.InstituicaoUsuarioDTO.InstituicaoDTO();

                ConfiguracaoUsuarioResponseDTO.LimiteInstituicaoDTO.InstituicaoUsuarioDTO.UsuarioDTO usuarioDTO2 =
                        new ConfiguracaoUsuarioResponseDTO.LimiteInstituicaoDTO.InstituicaoUsuarioDTO.UsuarioDTO();

                usuarioDTO2.setId(modelUser.getId());
                usuarioDTO2.setNome(modelUser.getNome());
                usuarioDTO2.setSobrenome(modelUser.getSobrenome());
                usuarioDTO2.setSexo(modelUser.getSexo());
                usuarioDTO2.setDataNascimento(modelUser.getDataNascimento());

                instituicaoDTO.setId(limite.getInstitucaoUsuario().getInstituicao().getId());
                instituicaoDTO.setNome(limite.getInstitucaoUsuario().getInstituicao().getNome());

                instituicaoUsuarioDTO.setInstituicao(instituicaoDTO);
                instituicaoUsuarioDTO.setUsuario(usuarioDTO2);

                limiteInstituicaoDTO.setLimiteDesejado(limite.getLimiteDesejado());
                limiteInstituicaoDTO.setInstituicaoUsuario(instituicaoUsuarioDTO);

                limitesInstituicaoDTO.add(limiteInstituicaoDTO);
            }
        }
        dto.setLimiteInstituicao(limitesInstituicaoDTO);

        return dto;
    }

    public static List<ConfiguracaoUsuarioResponseDTO> toDtoUser(List<Configuracoes> models){
        return models.stream()
                .map(ConfiguracoesMapper::toDtoUser)
                .toList();
    }

    public static Configuracoes toEntity(@Valid ConfiguracoesCreateDTO dto) {
        if(dto==null){
            return null;
        }

        Configuracoes entity = new Configuracoes();
        List<ConfiguracoesCreateDTO.LimiteCategoriaCreateDTO> limiteCategoriaCreateDTO = new ConfiguracoesCreateDTO.LimiteCategoriaCreateDTO();
        List<ConfiguracoesCreateDTO.LimiteInstituicaoCreateDTO> limiteInstituicaoCreateDTO = new ConfiguracoesCreateDTO.LimiteInstituicaoCreateDTO()

        Usuario user = new Usuario();
        user.setId(dto.getFkUsuario());

        List<ConfiguracoesCreateDTO.LimiteCategoriaCreateDTO> limitesCategoria = dto.getLimitesCategoria();


        entity.setInicioMesFiscal(dto.getInicioMesFiscal());
        entity.setLimiteDesejadoMensal(dto.getLimiteDesejadoMensal());
        entity.setUsuario(user);
        entity.setLimitePorInstituicao();
        return entity;
    }

    public static Configuracoes toEntity(@Valid ConfiguracaoEditDTO dto) {
        if (dto == null){
            return null;
        }

        Configuracoes entity = new Configuracoes();
        if (dto.getInicioMesFiscal()!= null){
            entity.setInicioMesFiscal(dto.getInicioMesFiscal());
        }

        if(dto.getLimiteDesejadoMensal()!= null){
            entity.setLimiteDesejadoMensal(dto.getLimiteDesejadoMensal());
        }

        return entity;
    }
}
