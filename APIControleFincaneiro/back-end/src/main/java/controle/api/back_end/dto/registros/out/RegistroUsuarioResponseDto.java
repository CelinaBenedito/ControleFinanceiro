package controle.api.back_end.dto.registros.out;

import controle.api.back_end.model.eventoFinanceiro.Tipo;
import controle.api.back_end.model.eventoFinanceiro.TipoMovimento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class RegistroUsuarioResponseDto {
    private GastoDetalheDto gastoDetalhe;
    private EventoFinanceiroDto eventoFinanceiro;
    private List<EventoInstituicaoDto> eventoInstituicao;
    private LocalDateTime dataRegistro;
    private UsuarioDto usuario;


    public static class EventoFinanceiroDto{
        private UUID id;
        private Tipo tipo;
        private Double valor;
        private String descricao;
        private LocalDate dataEvento;

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public Tipo getTipo() {
            return tipo;
        }

        public void setTipo(Tipo tipo) {
            this.tipo = tipo;
        }

        public Double getValor() {
            return valor;
        }

        public void setValor(Double valor) {
            this.valor = valor;
        }

        public String getDescricao() {
            return descricao;
        }

        public void setDescricao(String descricao) {
            this.descricao = descricao;
        }

        public LocalDate getDataEvento() {
            return dataEvento;
        }

        public void setDataEvento(LocalDate dataEvento) {
            this.dataEvento = dataEvento;
        }
    }
    public static class UsuarioDto{
        private UUID id;
        private String nome;
        private String email;

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
    public static class EventoInstituicaoDto{
        private Integer id;
        private InstituicaoDto instituicao;
        private TipoMovimento tipoMovimento;
        private Double valor;
        private Integer parcelas;

        public static class InstituicaoDto{
            private Integer id;
            private String nome;

            public Integer getId() {
                return id;
            }

            public void setId(Integer id) {
                this.id = id;
            }

            public String getNome() {
                return nome;
            }

            public void setNome(String nome) {
                this.nome = nome;
            }
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public InstituicaoDto getInstituicao() {
            return instituicao;
        }

        public void setInstituicao(InstituicaoDto instituicao) {
            this.instituicao = instituicao;
        }

        public TipoMovimento getTipoMovimento() {
            return tipoMovimento;
        }

        public void setTipoMovimento(TipoMovimento tipoMovimento) {
            this.tipoMovimento = tipoMovimento;
        }

        public Double getValor() {
            return valor;
        }

        public void setValor(Double valor) {
            this.valor = valor;
        }

        public Integer getParcelas() {
            return parcelas;
        }

        public void setParcelas(Integer parcelas) {
            this.parcelas = parcelas;
        }
    }
    public static class GastoDetalheDto{
        private Long id;
        private List<CategoriaDto> categoria;
        private String tituloGasto;

        public static class CategoriaDto{
            private Integer id;
            private String titulo;

            public Integer getId() {
                return id;
            }

            public void setId(Integer id) {
                this.id = id;
            }

            public String getTitulo() {
                return titulo;
            }

            public void setTitulo(String titulo) {
                this.titulo = titulo;
            }
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public List<CategoriaDto> getCategoria() {
            return categoria;
        }

        public void setCategoria(List<CategoriaDto> categoria) {
            this.categoria = categoria;
        }

        public String getTituloGasto() {
            return tituloGasto;
        }

        public void setTituloGasto(String tituloGasto) {
            this.tituloGasto = tituloGasto;
        }
    }

    public GastoDetalheDto getGastoDetalhe() {
        return gastoDetalhe;
    }

    public void setGastoDetalhe(GastoDetalheDto gastoDetalhe) {
        this.gastoDetalhe = gastoDetalhe;
    }

    public EventoFinanceiroDto getEventoFinanceiro() {
        return eventoFinanceiro;
    }

    public void setEventoFinanceiro(EventoFinanceiroDto eventoFinanceiro) {
        this.eventoFinanceiro = eventoFinanceiro;
    }

    public List<EventoInstituicaoDto> getEventoInstituicao() {
        return eventoInstituicao;
    }

    public void setEventoInstituicao(List<EventoInstituicaoDto> eventoInstituicao) {
        this.eventoInstituicao = eventoInstituicao;
    }

    public LocalDateTime getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(LocalDateTime dataRegistro) {
        this.dataRegistro = dataRegistro;
    }

    public UsuarioDto getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioDto usuario) {
        this.usuario = usuario;
    }
}
