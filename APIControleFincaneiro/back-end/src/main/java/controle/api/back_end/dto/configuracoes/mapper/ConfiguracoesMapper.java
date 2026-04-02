package controle.api.back_end.dto.configuracoes.mapper;

import controle.api.back_end.dto.configuracoes.ConfiguracaoUsuarioResponseDTO;
import controle.api.back_end.dto.configuracoes.ConfiguracoesCreateDTO;
import controle.api.back_end.dto.configuracoes.ConfiguracoesResponsesDTO;
import controle.api.back_end.model.configuracoes.Configuracoes;
import controle.api.back_end.model.usuario.Usuario;
import jakarta.validation.Valid;

import java.util.List;

public class ConfiguracoesMapper {

    public static ConfiguracoesResponsesDTO toDto(Configuracoes model) {
        if(model == null){
            return null;
        }

        ConfiguracoesResponsesDTO dto = new ConfiguracoesResponsesDTO();

        dto.setId(model.getId());
        dto.setInicioMesFiscal(model.getInicioMesFiscal());
        dto.setFinalMesFiscal(model.getFinalMesFiscal());
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
        if(model == null){
            return null;
        }
        Usuario modelUser = model.getFkUsuario();
        ConfiguracaoUsuarioResponseDTO dto = new ConfiguracaoUsuarioResponseDTO();
        ConfiguracaoUsuarioResponseDTO.ConfiguracaoUsuarioDTO dtoUser = new ConfiguracaoUsuarioResponseDTO.ConfiguracaoUsuarioDTO();

        dtoUser.setId(modelUser.getId());
        dtoUser.setNome(modelUser.getNome());
        dtoUser.setSobrenome(modelUser.getSobrenome());
        dtoUser.setEmail(modelUser.getEmail());

        dto.setId(model.getId());
        dto.setUsuario(dtoUser);
        dto.setInicioMesFiscal(model.getInicioMesFiscal());
        dto.setFinalMesFiscal(model.getFinalMesFiscal());
        dto.setLimiteDesejadoMensal(model.getLimiteDesejadoMensal());
        dto.setUltimaAtualizacao(model.getUltimaAtualizacao());

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

        entity.setInicioMesFiscal(dto.getInicioMesFiscal());
        entity.setFinalMesFiscal(dto.getFinalMesFiscal());
        entity.setLimiteDesejadoMensal(dto.getLimiteDesejadoMensal());
        return entity;
    }
}
