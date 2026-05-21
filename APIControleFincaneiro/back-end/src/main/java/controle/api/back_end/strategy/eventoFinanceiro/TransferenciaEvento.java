package controle.api.back_end.strategy.eventoFinanceiro;

import controle.api.back_end.model.eventoFinanceiro.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class TransferenciaEvento implements EventoFinanceiroStrategy {

    @Override
    public Registro processar(EventoFinanceiro evento,
                              List<EventoInstituicao> eventoInstituicoes,
                              EventoDetalhe eventoDetalhe) {

        List<EventoFinanceiro> eventos = new ArrayList<>();
        List<EventoInstituicao> instituicoes = new ArrayList<>();

        if (eventoInstituicoes == null || eventoInstituicoes.isEmpty()) {
            return new Registro(eventos, instituicoes, eventoDetalhe);
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
        transferenciaSaida.setDescricao("Transferência realizada para " +
                destino.getInstituicaoUsuario().getInstituicao().getNome());
        transferenciaSaida.setDataEvento(evento.getDataEvento());
        transferenciaSaida.setDataRegistro(LocalDateTime.now());
        eventos.add(transferenciaSaida);

        origem.setEventoFinanceiro(transferenciaSaida);
        origem.setValor(evento.getValor());
        origem.setParcelas(1);
        instituicoes.add(origem);

        // 2. Evento de recebimento (interno)
        if (destino.getInstituicaoUsuario().getUsuario().equals(evento.getUsuario())) {
            EventoFinanceiro transferenciaRecebida = new EventoFinanceiro();
            transferenciaRecebida.setUsuario(evento.getUsuario());
            transferenciaRecebida.setTipo(Tipo.Recebimento);
            transferenciaRecebida.setValor(evento.getValor());
            transferenciaRecebida.setDescricao("Transferência recebida da instituição " +
                    origem.getInstituicaoUsuario().getInstituicao().getNome());
            transferenciaRecebida.setDataEvento(evento.getDataEvento());
            transferenciaRecebida.setDataRegistro(LocalDateTime.now());
            eventos.add(transferenciaRecebida);

            destino.setEventoFinanceiro(transferenciaRecebida);
            destino.setValor(evento.getValor());
            destino.setParcelas(1);
            instituicoes.add(destino);
        } else {
            // Caso seja transferência externa (outro usuário)
            destino.setEventoFinanceiro(transferenciaSaida);
            destino.setValor(evento.getValor());
            destino.setParcelas(1);
            instituicoes.add(destino);
        }

        return new Registro(eventos, instituicoes, eventoDetalhe);
    }
}
