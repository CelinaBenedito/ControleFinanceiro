package controle.api.back_end.dto.configuracoes.mapper;

import controle.api.back_end.dto.categoria.CategoriaUsuarioResponseDTO;
import controle.api.back_end.dto.configuracoes.*;
import controle.api.back_end.model.categoria.CategoriaUsuario;
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
        List<LimitePorCategoriaResponseDto> limitesCategoriaDTO = new ArrayList<>();
        if (model.getLimitePorCategoria() != null) {
            for (LimitePorCategoria limite : model.getLimitePorCategoria()) {
                LimitePorCategoriaResponseDto limiteCategoriaDTO = new LimitePorCategoriaResponseDto();

                LimitePorCategoriaResponseDto.CategoriaDTO categoriaDTO = new LimitePorCategoriaResponseDto.CategoriaDTO();


                categoriaDTO.setId(limite.getCategoriaUsuario().getCategoria().getId());
                categoriaDTO.setTitulo(limite.getCategoriaUsuario().getCategoria().getTitulo());

                limiteCategoriaDTO.setCategoria(categoriaDTO);
                limiteCategoriaDTO.setLimiteDesejado(limite.getLimiteDesejado());
                limiteCategoriaDTO.setId(limite.getId());
                limitesCategoriaDTO.add(limiteCategoriaDTO);
            }
        }
        dto.setLimitePorCategoria(limitesCategoriaDTO);

        // LIMITE POR INSTITUIÇÃO (lista)
        List<LimitePorInstituicaoResponseDto> limitesInstituicaoDTO = new ArrayList<>();
        if (model.getLimitePorInstituicao() != null) {
            for (LimitePorInstituicao limite : model.getLimitePorInstituicao()) {
                LimitePorInstituicaoResponseDto limiteInstituicaoDTO =
                        new LimitePorInstituicaoResponseDto();

                LimitePorInstituicaoResponseDto.InstituicaoDTO instituicaoDTO =
                        new LimitePorInstituicaoResponseDto.InstituicaoDTO();


                instituicaoDTO.setId(limite.getInstitucaoUsuario().getInstituicao().getId());
                instituicaoDTO.setNome(limite.getInstitucaoUsuario().getInstituicao().getNome());


                limiteInstituicaoDTO.setLimiteDesejado(limite.getLimiteDesejado());
                limiteInstituicaoDTO.setInstituicao(instituicaoDTO);
                limiteInstituicaoDTO.setId(limite.getId());
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

        Usuario user = new Usuario();
        user.setId(dto.getFkUsuario());

        entity.setInicioMesFiscal(dto.getInicioMesFiscal());
        entity.setLimiteDesejadoMensal(dto.getLimiteDesejadoMensal());
        entity.setUsuario(user);
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
