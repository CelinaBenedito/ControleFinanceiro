package controle.api.back_end.dto.registros.mapper;

import controle.api.back_end.dto.registros.in.EventoFinanceiroCreateDto;
import controle.api.back_end.dto.registros.in.EventoInstituicaoCreateDto;
import controle.api.back_end.dto.registros.in.GastoDetalheCreateDto;
import controle.api.back_end.dto.registros.out.RegistroResponseDto;
import controle.api.back_end.dto.registros.out.RegistroUsuarioResponseDto;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.eventoFinanceiro.EventoDetalhe;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.List;

public class RegistrosMapper {

    public static EventoFinanceiro toEntityFinanceiro(@Valid EventoFinanceiroCreateDto dto){
        if (dto == null){
            return null;
        }

        EventoFinanceiro entity = new EventoFinanceiro();
        Usuario user = new Usuario();
        user.setId(dto.getUsuario_id());

        entity.setUsuario(user);
        entity.setValor(dto.getValor());
        entity.setTipo(dto.getTipo());
        entity.setDescricao(dto.getDescricao());
        entity.setDataEvento(dto.getDataEvento());

        return entity;
    }

    public static List<EventoFinanceiro> toEntityFinanceiro(@Valid List<EventoFinanceiroCreateDto> dtos){
        return dtos.stream()
                .map(RegistrosMapper::toEntityFinanceiro)
                .toList();
    }

    public static EventoInstituicao toEntityEvento(@Valid EventoInstituicaoCreateDto dto){
        if (dto == null){
            return null;
        }
        EventoInstituicao entity = new EventoInstituicao();

        InstituicaoUsuario instituicoes = new InstituicaoUsuario();
        instituicoes.setId(dto.getInstituicaoUsuario_id());

        entity.setParcelas(dto.getParcelas());
        entity.setValor(dto.getValor());
        entity.setTipoMovimento(dto.getTipoMovimento());
        entity.setInstituicaoUsuario(instituicoes);

        return entity;
    }

    public static List<EventoInstituicao> toEntityEvento(@Valid List<EventoInstituicaoCreateDto> dtos){
        return dtos.stream()
                .map(RegistrosMapper::toEntityEvento)
                .toList();
    }

    public static EventoDetalhe toEntityGasto(@Valid GastoDetalheCreateDto dto){
        if(dto == null){
            return null;
        }

        EventoDetalhe entity = new EventoDetalhe();
        List<CategoriaUsuario> categorias = dto.getCategoriaUsuario_id().stream()
                .map(id -> {
                    CategoriaUsuario categoria = new CategoriaUsuario();
                    categoria.setId(id);
                    return categoria;
                })
                .toList();

        entity.setCategoriaUsuario(categorias);
        entity.setTituloGasto(dto.getTituloGasto());

        return entity;
    }

    public static List<EventoDetalhe> toEntityGasto(@Valid List<GastoDetalheCreateDto> dtos){
        return dtos.stream()
                .map(RegistrosMapper::toEntityGasto)
                .toList();
    }

    public static RegistroUsuarioResponseDto toResponseUser(EventoFinanceiro eventoFinanceiro,
                                                            List<EventoInstituicao> eventoInstituicoes,
                                                            EventoDetalhe eventoDetalhe) {
        if (eventoInstituicoes == null || eventoFinanceiro == null || eventoDetalhe == null) {
            return null;
        }

        RegistroUsuarioResponseDto response = new RegistroUsuarioResponseDto();

        // Mapeando lista de EventoInstituicao
        List<RegistroUsuarioResponseDto.EventoInstituicaoDto> eventoInstituicaoDtos = eventoInstituicoes.stream()
                .map(ei -> {
                    RegistroUsuarioResponseDto.EventoInstituicaoDto.InstituicaoDto instituicaoDto =
                            new RegistroUsuarioResponseDto.EventoInstituicaoDto.InstituicaoDto();
                    instituicaoDto.setId(ei.getInstituicaoUsuario().getInstituicao().getId());
                    instituicaoDto.setNome(ei.getInstituicaoUsuario().getInstituicao().getNome());

                    RegistroUsuarioResponseDto.EventoInstituicaoDto eventoInstituicaoDto =
                            new RegistroUsuarioResponseDto.EventoInstituicaoDto();
                    eventoInstituicaoDto.setId(ei.getId());
                    eventoInstituicaoDto.setParcelas(ei.getParcelas());
                    eventoInstituicaoDto.setInstituicao(instituicaoDto);
                    eventoInstituicaoDto.setTipoMovimento(ei.getTipoMovimento());
                    eventoInstituicaoDto.setValor(ei.getValor());

                    return eventoInstituicaoDto;
                })
                .toList();

        // Mapeando EventoFinanceiro
        RegistroUsuarioResponseDto.EventoFinanceiroDto financeiroDto = new RegistroUsuarioResponseDto.EventoFinanceiroDto();
        financeiroDto.setId(eventoFinanceiro.getId());
        financeiroDto.setDescricao(eventoFinanceiro.getDescricao());
        financeiroDto.setTipo(eventoFinanceiro.getTipo());
        financeiroDto.setDataEvento(eventoFinanceiro.getDataEvento());
        financeiroDto.setValor(eventoFinanceiro.getValor());

        // Mapeando categorias do EventoDetalhe
        List<RegistroUsuarioResponseDto.GastoDetalheDto.CategoriaDto> categoriasDto = eventoDetalhe.getCategoriaUsuario().stream()
                .map(cu -> {
                    RegistroUsuarioResponseDto.GastoDetalheDto.CategoriaDto categoriaDto =
                            new RegistroUsuarioResponseDto.GastoDetalheDto.CategoriaDto();
                    categoriaDto.setId(cu.getCategoria().getId());
                    categoriaDto.setTitulo(cu.getCategoria().getTitulo());
                    return categoriaDto;
                })
                .toList();

        RegistroUsuarioResponseDto.GastoDetalheDto detalheDto = new RegistroUsuarioResponseDto.GastoDetalheDto();
        detalheDto.setId(eventoDetalhe.getId());
        detalheDto.setCategoria(categoriasDto);
        detalheDto.setTituloGasto(eventoDetalhe.getTituloGasto());

        // Mapeando usuário
        RegistroUsuarioResponseDto.UsuarioDto usuarioDto = new RegistroUsuarioResponseDto.UsuarioDto();
        usuarioDto.setId(eventoFinanceiro.getUsuario().getId());
        usuarioDto.setNome(eventoFinanceiro.getUsuario().getNome());
        usuarioDto.setEmail(eventoFinanceiro.getUsuario().getEmail());

        // Montando resposta final
        response.setUsuario(usuarioDto);
        response.setGastoDetalhe(detalheDto);
        response.setEventoFinanceiro(financeiroDto);
        response.setEventoInstituicao(eventoInstituicaoDtos);
        response.setDataRegistro(eventoFinanceiro.getDataRegistro());

        return response;
    }

    public static RegistroResponseDto toResponse(EventoFinanceiro eventoFinanceiro,
                                                 List<EventoInstituicao> eventoInstituicoes,
                                                 EventoDetalhe eventoDetalhe) {
        if (eventoInstituicoes == null || eventoFinanceiro == null || eventoDetalhe == null) {
            return null;
        }

        RegistroResponseDto response = new RegistroResponseDto();

        // Mapeando lista de EventoInstituicao
        List<RegistroResponseDto.EventoInstituicaoDto> eventoInstituicaoDtos = eventoInstituicoes.stream()
                .map(ei -> {
                    RegistroResponseDto.EventoInstituicaoDto.InstituicaoDto instituicaoDto =
                            new RegistroResponseDto.EventoInstituicaoDto.InstituicaoDto();
                    instituicaoDto.setId(ei.getInstituicaoUsuario().getInstituicao().getId());
                    instituicaoDto.setNome(ei.getInstituicaoUsuario().getInstituicao().getNome());

                    RegistroResponseDto.EventoInstituicaoDto eventoInstituicaoDto =
                            new RegistroResponseDto.EventoInstituicaoDto();
                    eventoInstituicaoDto.setId(ei.getId());
                    eventoInstituicaoDto.setParcelas(ei.getParcelas());
                    eventoInstituicaoDto.setInstituicao(instituicaoDto);
                    eventoInstituicaoDto.setTipoMovimento(ei.getTipoMovimento());
                    eventoInstituicaoDto.setValor(ei.getValor());

                    return eventoInstituicaoDto;
                })
                .toList();

        // Mapeando EventoFinanceiro
        RegistroResponseDto.EventoFinanceiroDto financeiroDto = new RegistroResponseDto.EventoFinanceiroDto();
        financeiroDto.setId(eventoFinanceiro.getId());
        financeiroDto.setDescricao(eventoFinanceiro.getDescricao());
        financeiroDto.setTipo(eventoFinanceiro.getTipo());
        financeiroDto.setDataEvento(eventoFinanceiro.getDataEvento());
        financeiroDto.setValor(eventoFinanceiro.getValor());

        // Mapeando categorias do EventoDetalhe
        List<RegistroResponseDto.GastoDetalheDto.CategoriaDto> categoriasDto = eventoDetalhe.getCategoriaUsuario().stream()
                .map(cu -> {
                    RegistroResponseDto.GastoDetalheDto.CategoriaDto categoriaDto =
                            new RegistroResponseDto.GastoDetalheDto.CategoriaDto();
                    categoriaDto.setId(cu.getCategoria().getId());
                    categoriaDto.setTitulo(cu.getCategoria().getTitulo());
                    return categoriaDto;
                })
                .toList();

        RegistroResponseDto.GastoDetalheDto detalheDto = new RegistroResponseDto.GastoDetalheDto();
        detalheDto.setId(eventoDetalhe.getId());
        detalheDto.setCategoria(categoriasDto); // agora é lista
        detalheDto.setTituloGasto(eventoDetalhe.getTituloGasto());

        // Montando resposta final
        response.setDataRegistro(eventoFinanceiro.getDataRegistro());
        response.setEventoFinanceiro(financeiroDto);
        response.setEventoInstituicao(eventoInstituicaoDtos);
        response.setGastoDetalhe(detalheDto);

        return response;
    }


    public static List<RegistroResponseDto> toResponse(
            List<EventoFinanceiro> eventosFinanceiros,
            List<List<EventoInstituicao>> eventosInstituicoes,
            List<EventoDetalhe> eventoDetalhes) {

        if (eventosFinanceiros.size() != eventosInstituicoes.size()
                || eventosFinanceiros.size() != eventoDetalhes.size()) {
            throw new IllegalArgumentException("As listas devem ter o mesmo tamanho");
        }

        List<RegistroResponseDto> responses = new ArrayList<>();

        for (int i = 0; i < eventosFinanceiros.size(); i++) {
            EventoFinanceiro eventoFinanceiro = eventosFinanceiros.get(i);
            List<EventoInstituicao> instituicoes = eventosInstituicoes.get(i);
            EventoDetalhe eventoDetalhe = eventoDetalhes.get(i);

            RegistroResponseDto response = toResponse(eventoFinanceiro, instituicoes, eventoDetalhe);
            if (response != null) {
                responses.add(response);
            }
        }

        return responses;
    }


}
