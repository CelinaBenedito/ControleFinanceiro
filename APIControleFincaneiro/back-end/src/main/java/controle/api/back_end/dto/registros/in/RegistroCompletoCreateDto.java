package controle.api.back_end.dto.registros.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import java.util.List;

public class RegistroCompletoCreateDto {
    @Valid
    private EventoFinanceiroCreateDto financeiro;
    @Valid
    private List<EventoInstituicaoCreateDto> instituicao;
    @Valid
    private EventoDetalheCreateDto detalhe;
    @Valid
    private RecorrenciaCreateDto recorrencia;

    public List<EventoInstituicaoCreateDto> getInstituicao() {
        return instituicao;
    }

    public void setInstituicao(List<EventoInstituicaoCreateDto> instituicao) {
        this.instituicao = instituicao;
    }

    public EventoFinanceiroCreateDto getFinanceiro() {
        return financeiro;
    }

    public void setFinanceiro(EventoFinanceiroCreateDto financeiro) {
        this.financeiro = financeiro;
    }

    public EventoDetalheCreateDto getDetalhe() {
        return detalhe;
    }

    public void setDetalhe(EventoDetalheCreateDto detalhe) {
        this.detalhe = detalhe;
    }

    public RecorrenciaCreateDto getRecorrencia() {
        return recorrencia;
    }

    public void setRecorrencia(RecorrenciaCreateDto recorrencia) {
        this.recorrencia = recorrencia;
    }
}
