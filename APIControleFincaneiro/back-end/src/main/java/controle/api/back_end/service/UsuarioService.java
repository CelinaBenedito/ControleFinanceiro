package controle.api.back_end.service;

import controle.api.back_end.dto.usuario.mapper.UsuarioMappper;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.exception.MenorDeIdadeException;
import controle.api.back_end.model.configuracoes.Configuracoes;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.Tipo;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.EventoFinanceiroRepository;
import controle.api.back_end.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ConfiguracoesService configuracoesService;
    private final EventoFinanceiroRepository eventoFinanceiroRepository;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          ConfiguracoesService configuracoesService,
                          EventoFinanceiroRepository eventoFinanceiroRepository) {
        this.usuarioRepository = usuarioRepository;
        this.configuracoesService = configuracoesService;
        this.eventoFinanceiroRepository = eventoFinanceiroRepository;
    }

    public List<Usuario> getUsuarios(){
        return usuarioRepository.findAll();
    }

    public Usuario getUsuarioById(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Usuario de id: %s não encontrado".
                                formatted(id)
                        )
                );
    }

    public BigDecimal getSaldoByUsuario(UUID userId) {
        Usuario usuario = usuarioRepository.findById(userId).orElseThrow(() ->
                new EntidadeNaoEncontradaException("Usuario de id: %s não encontrado"
                        .formatted(userId)
                )
        );
        List<EventoFinanceiro> eventosFinanceiros = eventoFinanceiroRepository.findEventoFinanceiroByUsuario(usuario);

        BigDecimal saldo = BigDecimal.ZERO;

        for (EventoFinanceiro evento : eventosFinanceiros) {
            saldo = InstituicaoService.getSaldo(saldo, evento);
        }
        return saldo;
    }

    public Usuario createUsuario(Usuario entity) {
        //VALIDAÇÃO DE IDADE
        if(ageValidation(entity.getDataNascimento()) == false){
            throw new MenorDeIdadeException("Usuario menor de idade");
        }
        return usuarioRepository.save(entity);
    }

    public Usuario LoginUsuario(Usuario login) {
        List<Usuario> usuarioByEmailAndSenha = usuarioRepository.
                findUsuarioByEmailAndSenha(
                login.getEmail(),
                        login.getSenha()
        );
               if(usuarioByEmailAndSenha.isEmpty()){
                   throw new EntidadeNaoEncontradaException(
                           "Usuario de email: %s e senha: %s não encontrado".
                           formatted(
                                   login.getEmail(),
                                   login.getSenha()
                           )
                   );
               }
               return usuarioByEmailAndSenha.getFirst();
    }

    public Usuario editUsuario(UUID id,Usuario entity) {
        //VALIDAÇÃO DE IDADE
        if(entity.getDataNascimento()!= null){
            if(ageValidation(entity.getDataNascimento()) == false){
                throw new MenorDeIdadeException("Data para adicionado é menor que 18 anos");
            }
        }

        Usuario userAtual = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Usuario com o id: %s para editar não encontrado"
                                .formatted(id)
                        )
                );

        Usuario edit = UsuarioMappper.toEdit(entity, userAtual);

        return usuarioRepository.save(edit);
    }

    public Boolean ageValidation(LocalDate dataNascimento){
        LocalDate hoje = LocalDate.now();

        LocalDate dataMaioridade = hoje.minusYears(18);
        if (dataNascimento.isBefore(dataMaioridade) || dataNascimento.isEqual(dataMaioridade)) {
            System.out.println("Maior de 18 anos.");
            return true;
        }
        return false;
    }

    public void createConfiguracao(Usuario usuario){
        Configuracoes configuracoes = new Configuracoes();
        configuracoes.setUsuario(usuario);
        configuracoes.setInicioMesFiscal(1);
        configuracoes.setUltimaAtualizacao(LocalDate.now());
        configuracoesService.createConfiguracao(configuracoes,usuario.getId());
    }
}
