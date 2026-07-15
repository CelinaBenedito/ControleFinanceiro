package controle.api.back_end.strategy.eventoFinanceiro;

import controle.api.back_end.model.eventoFinanceiro.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TransferenciaEvento implements EventoFinanceiroStrategy {

    @Override
    public Registro processar(EventoFinanceiro evento,
                              List<EventoInstituicao> eventoInstituicoes,
                              EventoDetalhe eventoDetalhe) {

        List<EventoFinanceiro> eventos = new ArrayList<>();
        Map<EventoFinanceiro, List<EventoInstituicao>> instituicoesPorEvento = new HashMap<>();
        Map<EventoFinanceiro, EventoDetalhe> detalhePorEvento = new HashMap<>();

        if (eventoInstituicoes == null || eventoInstituicoes.isEmpty()) {
            return new Registro(eventos, instituicoesPorEvento, detalhePorEvento);
        }

        // Origem = primeira instituição
        EventoInstituicao origem = eventoInstituicoes.getFirst();

        // Destino = última instituição
        EventoInstituicao destino = eventoInstituicoes.getLast();

        // 1. Evento de saída (transferência)
        EventoFinanceiro transferenciaSaida = new EventoFinanceiro();
        transferenciaSaida.setUsuario(evento.getUsuario());
        transferenciaSaida.setTipo(Tipo.Transferencia);
        transferenciaSaida.setValor(evento.getValor());
        String nomeDestino = (destino.getInstituicaoUsuario() != null
                && destino.getInstituicaoUsuario().getInstituicao() != null)
                ? destino.getInstituicaoUsuario().getInstituicao().getNome() : "instituição destino";
        transferenciaSaida.setDescricao("Transferência realizada para " + nomeDestino);
        transferenciaSaida.setDataEvento(evento.getDataEvento());
        transferenciaSaida.setDataRegistro(LocalDateTime.now());
        eventos.add(transferenciaSaida);

        // Vincular origem ao evento de saída
        origem.setEventoFinanceiro(transferenciaSaida);
        origem.setValor(evento.getValor());
        origem.setParcelas(1);
        instituicoesPorEvento.put(transferenciaSaida, List.of(origem));

        // Detalhe único vinculado ao evento de saída
        if (eventoDetalhe != null) {
            eventoDetalhe.setEventoFinanceiro(transferenciaSaida);
            detalhePorEvento.put(transferenciaSaida, eventoDetalhe);
        }

        // 2. Evento de recebimento (interno)
        if (destino.getInstituicaoUsuario().getUsuario().equals(evento.getUsuario())) {
            EventoFinanceiro transferenciaRecebida = new EventoFinanceiro();
            transferenciaRecebida.setUsuario(evento.getUsuario());
            transferenciaRecebida.setTipo(Tipo.Recebimento);
            transferenciaRecebida.setValor(evento.getValor());
            String nomeOrigem = (origem.getInstituicaoUsuario() != null
                    && origem.getInstituicaoUsuario().getInstituicao() != null)
                    ? origem.getInstituicaoUsuario().getInstituicao().getNome() : "instituição origem";
            transferenciaRecebida.setDescricao("Transferência recebida da instituição " + nomeOrigem);
            transferenciaRecebida.setDataEvento(evento.getDataEvento());
            transferenciaRecebida.setDataRegistro(LocalDateTime.now());
            eventos.add(transferenciaRecebida);

            destino.setEventoFinanceiro(transferenciaRecebida);
            destino.setValor(evento.getValor());
            destino.setParcelas(1);
            instituicoesPorEvento.put(transferenciaRecebida, List.of(destino));

            // Detalhe único vinculado ao recebimento
            if (eventoDetalhe != null) {
                detalhePorEvento.put(transferenciaRecebida, eventoDetalhe);
            }
        } else {
            // Caso seja transferência externa (outro usuário)
            destino.setEventoFinanceiro(transferenciaSaida);
            destino.setValor(evento.getValor());
            destino.setParcelas(1);
            instituicoesPorEvento.get(transferenciaSaida).add(destino);
        }

        return new Registro(eventos, instituicoesPorEvento, detalhePorEvento);
    }
}

