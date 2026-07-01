package controle.api.back_end.model.instituicao;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
public class Instituicao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Size(max = 50)
    @NotBlank
    private String nome;

    @Column(length = 500)
    private String bankUrl;

    /**
     * Steps de navegacao serializados como JSON.
     * Usa placeholders {{CPF}}, {{SENHA}}, {{AGENCIA}}, {{CONTA}}, {{DIGITO}}.
     * null = modo manual (browser abre, usuario faz tudo sozinho).
     */
    @Column(columnDefinition = "TEXT")
    private String navigationStepsJson;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean ofxSupported = false;

    /**
     * Modo de autenticacao do banco.
     *
     * MANUAL    - browser abre, usuario faz login + OFX manualmente.
     *             Ideal para: Itau (iToken push), Nubank (app-based auth).
     *
     * AUTOMATED - Playwright executa os navigationSteps automaticamente.
     *             Ideal para: Bradesco, BB, Santander, Inter (login padrao).
     *
     * API       - Usa biblioteca Python especifica (ex: pynubank quando funcional).
     *             Reservado para uso futuro.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'MANUAL'")
    private LoginMode loginMode = LoginMode.MANUAL;

    /**
     * Endpoint Python especifico para captura.
     * Padrao: /capture (Playwright generico).
     * Alelo: /capture/alelo (interceptacao de API XHR, pois nao tem OFX nativo).
     * Nubank: /capture/nubank/sync (tenta pynubank, fallback Playwright).
     */
    @Column(length = 100, columnDefinition = "VARCHAR(100) DEFAULT '/capture'")
    private String pythonEndpoint = "/capture";

    public enum LoginMode { MANUAL, AUTOMATED, API }

    public Instituicao() {}

    public Instituicao(Integer id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getBankUrl() { return bankUrl; }
    public void setBankUrl(String bankUrl) { this.bankUrl = bankUrl; }

    public String getNavigationStepsJson() { return navigationStepsJson; }
    public void setNavigationStepsJson(String navigationStepsJson) {
        this.navigationStepsJson = navigationStepsJson;
    }

    public Boolean getOfxSupported() { return ofxSupported; }
    public void setOfxSupported(Boolean ofxSupported) { this.ofxSupported = ofxSupported; }

    public LoginMode getLoginMode() { return loginMode; }
    public void setLoginMode(LoginMode loginMode) { this.loginMode = loginMode; }

    public String getPythonEndpoint() { return pythonEndpoint; }
    public void setPythonEndpoint(String pythonEndpoint) { this.pythonEndpoint = pythonEndpoint; }
}
