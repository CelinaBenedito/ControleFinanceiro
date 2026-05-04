package controle.api.back_end.service;

import controle.api.back_end.dto.usuario.mapper.UsuarioMappper;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.exception.MenorDeIdadeException;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.configuracoes.Configuracoes;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.*;
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
    private final InstituicaoUsuarioRepository instituicaoUsuarioRepository;
    private final CategoriaUsuarioRepository categoriaUsuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          ConfiguracoesService configuracoesService,
                          EventoFinanceiroRepository eventoFinanceiroRepository,
                          InstituicaoUsuarioRepository instituicaoUsuarioRepository,
                          CategoriaUsuarioRepository categoriaUsuarioRepository) {
        this.usuarioRepository = usuarioRepository;
        this.configuracoesService = configuracoesService;
        this.eventoFinanceiroRepository = eventoFinanceiroRepository;
        this.instituicaoUsuarioRepository = instituicaoUsuarioRepository;
        this.categoriaUsuarioRepository = categoriaUsuarioRepository;
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

    public Double getXpByUserId(UUID user_id) {
        if (!usuarioRepository.existsById(user_id)) {
            throw new EntidadeNaoEncontradaException("Usuário de id: %s não encontrado"
                    .formatted(user_id));
        }
        // Buscar registros do usuário
        List<EventoFinanceiro> eventosFinanceiros = eventoFinanceiroRepository.findEventoFinanceiroByUsuario_Id(user_id);
        int qtdRegistros = eventosFinanceiros.size();

        // Buscar instituições do usuário
        List<InstituicaoUsuario> instituicoes = instituicaoUsuarioRepository.findInstituicaoUsuarioByUsuario_IdAndIsAtivoIsTrue(user_id);
        int qtdInstituicoes = instituicoes.size();

        List<CategoriaUsuario> categorias = categoriaUsuarioRepository.findAllByUsuario_IdAndIsAtivoIsTrue(user_id);
        int qtdCategorias = categorias.size();

        double xp = 0.0;
        xp += ((double) qtdRegistros / 10) * 100;
        xp += qtdInstituicoes * 100;
        xp += qtdCategorias * 50;

        return xp;
    }

    public Usuario getUsuario(UUID userId){
        return usuarioRepository.findById(userId)
                .orElseThrow(()->
                        new EntidadeNaoEncontradaException(
                                "Usuário de id: %s não encontrado."
                                        .formatted(userId)
                        )
                );
    }
}
