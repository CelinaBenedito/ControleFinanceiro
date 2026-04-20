package controle.api.back_end.dto.registros.mapper;

import controle.api.back_end.dto.registros.in.EventoFinanceiroCreateDto;
import controle.api.back_end.dto.registros.in.EventoInstituicaoCreateDto;
import controle.api.back_end.dto.registros.in.GastoDetalheCreateDto;
import controle.api.back_end.dto.registros.out.RegistroResponseDto;
import controle.api.back_end.dto.registros.out.RegistroUsuarioResponseDto;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.eventoFinanceiro.GastoDetalhe;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import jakarta.validation.Valid;

import java.time.LocalDate;
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
        InstituicaoUsuario instituicaoUsuario = new InstituicaoUsuario();
        instituicaoUsuario.setId(dto.getInstituicaoUsuario_id());

        entity.setValor(dto.getValor());
        entity.setTipoMovimento(dto.getTipoMovimento());
        entity.setInstituicaoUsuario(instituicaoUsuario);

        return entity;
    }

    public static List<EventoInstituicao> toEntityEvento(@Valid List<EventoInstituicaoCreateDto> dtos){
        return dtos.stream()
                .map(RegistrosMapper::toEntityEvento)
                .toList();
    }

    public static GastoDetalhe toEntityGasto(@Valid GastoDetalheCreateDto dto){
        if(dto == null){
            return null;
        }

        GastoDetalhe entity = new GastoDetalhe();
        CategoriaUsuario categoriaUsuario = new CategoriaUsuario();

        categoriaUsuario.setId(dto.getCategoriaUsuario_id());

        entity.setCategoriaUsuario(categoriaUsuario);
        entity.setTituloGasto(dto.getTituloGasto());

        return entity;
    }

    public static List<GastoDetalhe> toEntity(@Valid List<GastoDetalheCreateDto> dtos){
        return dtos.stream()
                .map(RegistrosMapper::toEntityGasto)
                .toList();
    }

    public static RegistroUsuarioResponseDto toResponseUser(EventoFinanceiro eventoFinanceiro, EventoInstituicao eventoInstituicao, GastoDetalhe gastoDetalhe){
        if(eventoInstituicao == null || eventoFinanceiro == null || gastoDetalhe == null){
            return null;
        }

        RegistroUsuarioResponseDto response = new RegistroUsuarioResponseDto();
        RegistroUsuarioResponseDto.EventoInstituicaoDto eventoInstituicaoDto = new RegistroUsuarioResponseDto.EventoInstituicaoDto();
        RegistroUsuarioResponseDto.EventoInstituicaoDto.InstituicaoDto instituicaoDto = new RegistroUsuarioResponseDto.EventoInstituicaoDto.InstituicaoDto();
        RegistroUsuarioResponseDto.EventoFinanceiroDto financeiroDto = new RegistroUsuarioResponseDto.EventoFinanceiroDto();
        RegistroUsuarioResponseDto.GastoDetalheDto detalheDto = new RegistroUsuarioResponseDto.GastoDetalheDto();
        RegistroUsuarioResponseDto.GastoDetalheDto.CategoriaDto categoriaDto = new RegistroUsuarioResponseDto.GastoDetalheDto.CategoriaDto();
        RegistroUsuarioResponseDto.UsuarioDto usuarioDto = new RegistroUsuarioResponseDto.UsuarioDto();

        instituicaoDto.setId(eventoInstituicao.getInstituicaoUsuario().getInstituicao().getId());
        instituicaoDto.setNome(eventoInstituicao.getInstituicaoUsuario().getInstituicao().getNome());

        eventoInstituicaoDto.setId(eventoInstituicao.getId());
        eventoInstituicaoDto.setInstituicao(instituicaoDto);
        eventoInstituicaoDto.setTipoMovimento(eventoInstituicao.getTipoMovimento());
        eventoInstituicaoDto.setValor(eventoInstituicao.getValor());

        financeiroDto.setId(eventoFinanceiro.getId());
        financeiroDto.setDescricao(eventoFinanceiro.getDescricao());
        financeiroDto.setTipo(eventoFinanceiro.getTipo());
        financeiroDto.setDataEvento(eventoFinanceiro.getDataEvento());
        financeiroDto.setValor(eventoFinanceiro.getValor());

        categoriaDto.setId(gastoDetalhe.getCategoriaUsuario().getCategoria().getId());
        categoriaDto.setTitulo(gastoDetalhe.getCategoriaUsuario().getCategoria().getTitulo());

        detalheDto.setId(gastoDetalhe.getId());
        detalheDto.setCategoria(categoriaDto);
        detalheDto.setTituloGasto(gastoDetalhe.getTituloGasto());

        usuarioDto.setId(eventoFinanceiro.getUsuario().getId());
        usuarioDto.setNome(eventoFinanceiro.getUsuario().getNome());
        usuarioDto.setEmail(eventoFinanceiro.getUsuario().getEmail());

        LocalDate dataRegistro = eventoFinanceiro.getDataRegistro();

        response.setUsuario(usuarioDto);
        response.setGastoDetalhe(detalheDto);
        response.setEventoFinanceiro(financeiroDto);
        response.setEventoInstituicao(eventoInstituicaoDto);
        response.setDataRegistro(dataRegistro);

        return response;
    }

    public static RegistroResponseDto toResponse(EventoFinanceiro eventoFinanceiro, EventoInstituicao eventoInstituicao, GastoDetalhe gastoDetalhe){
        if(eventoInstituicao == null || eventoFinanceiro == null || gastoDetalhe == null){
            return null;
        }
        RegistroResponseDto response = new RegistroResponseDto();
        RegistroResponseDto.EventoInstituicaoDto.InstituicaoDto instituicaoDto = new RegistroResponseDto.EventoInstituicaoDto.InstituicaoDto();
        RegistroResponseDto.EventoInstituicaoDto eventoInstituicaoDto = new RegistroResponseDto.EventoInstituicaoDto();
        RegistroResponseDto.EventoFinanceiroDto financeiroDto = new RegistroResponseDto.EventoFinanceiroDto();
        RegistroResponseDto.GastoDetalheDto.CategoriaDto categoriaDto = new RegistroResponseDto.GastoDetalheDto.CategoriaDto();
        RegistroResponseDto.GastoDetalheDto detalheDto = new RegistroResponseDto.GastoDetalheDto();

        instituicaoDto.setId(eventoInstituicao.getInstituicaoUsuario().getInstituicao().getId());
        instituicaoDto.setNome(eventoInstituicao.getInstituicaoUsuario().getInstituicao().getNome());

        eventoInstituicaoDto.setId(eventoInstituicao.getId());
        eventoInstituicaoDto.setInstituicao(instituicaoDto);
        eventoInstituicaoDto.setTipoMovimento(eventoInstituicao.getTipoMovimento());
        eventoInstituicaoDto.setValor(eventoInstituicao.getValor());

        financeiroDto.setId(eventoFinanceiro.getId());
        financeiroDto.setDescricao(eventoFinanceiro.getDescricao());
        financeiroDto.setTipo(eventoFinanceiro.getTipo());
        financeiroDto.setDataEvento(eventoFinanceiro.getDataEvento());
        financeiroDto.setValor(eventoFinanceiro.getValor());

        categoriaDto.setId(gastoDetalhe.getCategoriaUsuario().getCategoria().getId());
        categoriaDto.setTitulo(gastoDetalhe.getCategoriaUsuario().getCategoria().getTitulo());

        detalheDto.setId(gastoDetalhe.getId());
        detalheDto.setCategoria(categoriaDto);
        detalheDto.setTituloGasto(gastoDetalhe.getTituloGasto());
        LocalDate dataRegistro = eventoFinanceiro.getDataRegistro();

        response.setDataRegistro(dataRegistro);
        response.setEventoFinanceiro(financeiroDto);
        response.setEventoInstituicao(eventoInstituicaoDto);
        response.setGastoDetalhe(detalheDto);

        return response;
    }

    public static List<RegistroResponseDto> toResponse(
            List<EventoFinanceiro> eventosFinanceiros,
            List<EventoInstituicao> eventosInstituicao,
            List<GastoDetalhe> gastoDetalhes) {

        if (eventosFinanceiros.size() != eventosInstituicao.size()
                || eventosFinanceiros.size() != gastoDetalhes.size()) {
            throw new IllegalArgumentException("As listas devem ter o mesmo tamanho");
        }

        List<RegistroResponseDto> responses = new ArrayList<>();

        for (int i = 0; i < eventosFinanceiros.size(); i++) {
            EventoFinanceiro eventoFinanceiro = eventosFinanceiros.get(i);
            EventoInstituicao eventoInstituicao = eventosInstituicao.get(i);
            GastoDetalhe gastoDetalhe = gastoDetalhes.get(i);

            RegistroResponseDto response = toResponse(eventoFinanceiro, eventoInstituicao, gastoDetalhe);
            if (response != null) {
                responses.add(response);
            }
        }

        return responses;
    }

}
