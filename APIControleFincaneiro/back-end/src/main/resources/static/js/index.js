// ── DASHBOARD — integração com os novos endpoints /dashboard/* ────────────────
const API_BASE = 'http://localhost:8080';
const CURRENCY = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

const semDados = `
  <div class="aviso">
    <i class='bx bx-search-alt'></i>
    <h3>Sem Dados</h3>
    <p>Selecione outro período para visualizar.</p>
  </div>`;

const carregando = `<p style="text-align:center;padding:32px;color:var(--cor-texto-secundario);">
    <i class='bx bx-loader-alt' style="font-size:1.5rem;animation:spin 1s linear infinite;"></i>
</p>`;

// Instâncias ApexCharts para destroy/recreate
const graficos = {};

// Período selecionado
let periodoCfg = null;

// ── Google Charts — controle de carregamento ──────────────────────────────────
let _gcReady = false;
const _gcQueue = [];
window._onGoogleChartsReady = function () {
    _gcReady = true;
    _gcQueue.forEach(fn => { try { fn(); } catch(e) { console.error('[Sankey]', e); } });
    _gcQueue.length = 0;
};

// ── HELPERS ───────────────────────────────────────────────────────────────────

function obterUsuarioIdDashboard() {
    try {
        const u = JSON.parse(localStorage.getItem('usuarioLogado') || 'null');
        return u?.id || null;
    } catch (_) { return null; }
}

function nomeMes(mes) {
    return ['Janeiro','Fevereiro','Março','Abril','Maio','Junho',
            'Julho','Agosto','Setembro','Outubro','Novembro','Dezembro'][(mes - 1)] || '';
}

function buildParams(cfg) {
    cfg = cfg || periodoCfg;
    const p = new URLSearchParams();
    p.set('periodo', cfg.tipo);
    p.set('ano', cfg.ano);
    if (cfg.tipo === 'MENSAL'     && cfg.mes)       p.set('mes',       cfg.mes);
    if (cfg.tipo === 'TRIMESTRAL' && cfg.trimestre) p.set('trimestre', cfg.trimestre);
    if (cfg.tipo === 'SEMESTRAL'  && cfg.semestre)  p.set('semestre',  cfg.semestre);
    return p.toString();
}

function labelPeriodoCfg(cfg) {
    cfg = cfg || periodoCfg;
    if (!cfg) return '';
    if (cfg.tipo === 'MENSAL')     return `${nomeMes(cfg.mes)} / ${cfg.ano}`;
    if (cfg.tipo === 'TRIMESTRAL') return `${cfg.trimestre}º Trimestre / ${cfg.ano}`;
    if (cfg.tipo === 'SEMESTRAL')  return `${cfg.semestre}º Semestre / ${cfg.ano}`;
    return `Anual / ${cfg.ano}`;
}

async function apiFetch(url) {
    try {
        // cache: 'no-store' garante dados frescos após importação de arquivos
        const res = await fetch(url, { cache: 'no-store' });
        if (res.status === 204) return { ok: false, json: null };
        if (!res.ok) {
            console.warn(`[Dashboard] API ${res.status} →`, url);
            return { ok: false, json: null };
        }
        const json = await res.json();
        return { ok: true, json };
    } catch (err) {
        console.error('[Dashboard] Erro ao buscar:', url, err);
        return { ok: false, json: null };
    }
}

function cssVar(name) {
    return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}

function isDark() {
    return document.body.dataset.mode === 'dark';
}

// Interpola hue 240(azul) → 0(vermelho) baseado em normalizado (0-1)
function heatColor(normalizado) {
    const n = Math.max(0, Math.min(1, normalizado || 0));
    if (n < 0.02) return 'var(--cor-fundo-inativo)';
    const hue = Math.round(240 * (1 - n));
    return `hsl(${hue}, 80%, ${isDark() ? 38 : 45}%)`;
}

/**
 * Retorna 3 cores para gráficos de múltiplas séries baseadas nas variáveis do root.
 * Série 1 → --cor-principal, Série 2 → --cor-para-destacar, Série 3 → --cor-hover
 */
function getChartPalette3() {
    return [
        cssVar('--cor-principal')     || '#367373',
        cssVar('--cor-para-destacar') || '#004C58',
        cssVar('--cor-hover')         || '#B4D9D5',
    ];
}

/**
 * Retorna a cor de borda do grid dos gráficos baseada na variável --cor-tinte-borda.
 */
function getGridColor() {
    return cssVar('--cor-tinte-borda') || 'rgba(54,115,115,0.18)';
}

/**
 * Retorna o array de cores dos nós do diagrama Sankey baseado nas variáveis do root.
 */
function getSankeyNodeColors() {
    const c1 = cssVar('--cor-principal')      || '#367373';
    const c2 = cssVar('--cor-para-destacar')  || '#004C58';
    const c3 = cssVar('--cor-fundo-cadastro') || '#5FA8A3';
    const c4 = cssVar('--cor-titulo')         || '#004C58';
    const c5 = cssVar('--cor-hover')          || '#B4D9D5';
    return [c1, c2, c3, c4, c5, c1, c2, c3];
}

/**
 * Retorna a cor secundária do gráfico de comparação de acordo com o tema atual.
 * Deve contrastar visivelmente com --cor-principal em qualquer combinação tema/modo.
 */
function getGraficoAntColor() {
    const tema = document.body.dataset.tema || 'padrao';
    const mode = document.body.dataset.mode || 'light';
    const map = {
        'padrao-light':    '#F59E0B',   // âmbar — contrasta com teal
        'padrao-dark':     '#FBBF24',   // âmbar claro
        'vampirico-light': '#1D4ED8',   // azul — contrasta com vermelho
        'vampirico-dark':  '#60A5FA',   // azul claro
        'ceu-azul-light':  '#7C3AED',   // roxo — contrasta com ciano
        'ceu-azul-dark':   '#A78BFA',   // roxo claro
        'purpura-light':   '#059669',   // verde — contrasta com roxo
        'purpura-dark':    '#34D399',   // verde claro
        'branco-light':    '#F59E0B',   // âmbar
        'branco-dark':     '#FBBF24',   // âmbar claro
    };
    return map[`${tema}-${mode}`] || '#F59E0B';
}

// ── ATALHOS DE PERÍODO ────────────────────────────────────────────────────────

function marcarAtalhoAtivo(atalho) {
    document.querySelectorAll('.dash-atalho-btn').forEach(btn => {
        btn.classList.toggle('ativo', btn.dataset.atalho === atalho);
    });
}

function selecionarRapido(atalho) {
    const hoje = new Date();
    const anoAtual = hoje.getFullYear();
    const mesAtual = hoje.getMonth() + 1;
    const selTipo      = document.getElementById('select_tempo');
    const selMes       = document.getElementById('select_mes');
    const selAno       = document.getElementById('select_ano');
    const divMes       = document.getElementById('div_mes');
    const divTrimestre = document.getElementById('div_trimestre');
    const divSemestre  = document.getElementById('div_semestre');

    if (divMes)       divMes.style.display       = 'none';
    if (divTrimestre) divTrimestre.style.display  = 'none';
    if (divSemestre)  divSemestre.style.display   = 'none';

    if (atalho === 'mes-atual') {
        selTipo.value = 'MENSAL';
        selMes.value  = String(mesAtual);
        selAno.value  = String(anoAtual);
        if (divMes) divMes.style.display = '';
    } else if (atalho === 'mes-passado') {
        let mes = mesAtual - 1, ano = anoAtual;
        if (mes === 0) { mes = 12; ano--; }
        selTipo.value = 'MENSAL';
        selMes.value  = String(mes);
        selAno.value  = String(ano);
        if (divMes) divMes.style.display = '';
    } else if (atalho === 'ano-atual') {
        selTipo.value = 'ANUAL';
        selAno.value  = String(anoAtual);
    } else if (atalho === 'ano-passado') {
        selTipo.value = 'ANUAL';
        selAno.value  = String(anoAtual - 1);
    }
    marcarAtalhoAtivo(atalho);
    carregarDashboard();
}

// ── INICIALIZAÇÃO ─────────────────────────────────────────────────────────────

/** Força atualização completa de KPIs e gráficos sem cache. */
function atualizarDashboard() {
    if (!periodoCfg) return;
    gerarKPIS();
    gerarGraficos();
}

/**
 * Observa mudanças de tema/modo na tag <body> e re-renderiza todos
 * os gráficos para aplicar as novas cores imediatamente.
 */
let _temaDebounce = null;
const _temaObserver = new MutationObserver(() => {
    clearTimeout(_temaDebounce);
    _temaDebounce = setTimeout(() => {
        if (!periodoCfg) return;
        // Destroi todas as instâncias ApexCharts existentes
        Object.keys(graficos).forEach(id => {
            try { graficos[id].destroy(); } catch (_) {}
            delete graficos[id];
        });
        // Re-renderiza com as novas cores do tema
        gerarGraficos();
    }, 200);
});
_temaObserver.observe(document.body, {
    attributes: true,
    attributeFilter: ['data-mode', 'data-tema']
});

async function gerarInformações() {
    const hoje = new Date();
    const anoAtual = hoje.getFullYear();
    const mesAtual = hoje.getMonth() + 1;
    periodoCfg = { tipo: 'MENSAL', ano: anoAtual, mes: mesAtual };

    const selTipo = document.getElementById('select_tempo');
    const selMes  = document.getElementById('select_mes');
    const selAno  = document.getElementById('select_ano');
    if (selTipo) selTipo.value = 'MENSAL';
    if (selMes)  selMes.value  = String(mesAtual);

    // Popula o select de ano com os anos presentes no banco
    await popularSelectAnos(selAno, anoAtual);

    atualizarSeletoresPeriodo(false);
    marcarAtalhoAtivo('mes-atual');
    carregarDashboard();
}

async function popularSelectAnos(selAno, anoAtual) {
    if (!selAno) return;
    const userId = obterUsuarioIdDashboard();
    let anos = [anoAtual];
    if (userId) {
        try {
            const { ok, json } = await apiFetch(`${API_BASE}/dashboard/anos/usuarios/${userId}`);
            if (ok && Array.isArray(json) && json.length > 0) anos = json;
        } catch (_) { /* fallback: usa só o ano atual */ }
    }
    selAno.innerHTML = anos.map(a =>
        `<option value="${a}"${a === anoAtual ? ' selected' : ''}>${a}</option>`
    ).join('');
}

function atualizarSeletoresPeriodo(recarregar) {
    const tipo = document.getElementById('select_tempo')?.value || 'MENSAL';
    const divMes       = document.getElementById('div_mes');
    const divTrimestre = document.getElementById('div_trimestre');
    const divSemestre  = document.getElementById('div_semestre');
    if (divMes)       divMes.style.display       = tipo === 'MENSAL'     ? '' : 'none';
    if (divTrimestre) divTrimestre.style.display  = tipo === 'TRIMESTRAL' ? '' : 'none';
    if (divSemestre)  divSemestre.style.display   = tipo === 'SEMESTRAL'  ? '' : 'none';
    if (recarregar !== false) { marcarAtalhoAtivo(null); carregarDashboard(); }
}

function carregarDashboard() {
    const tipo = document.getElementById('select_tempo')?.value;
    const ano  = Number(document.getElementById('select_ano')?.value);
    if (!tipo || !ano || isNaN(ano)) return;

    const cfg = { tipo, ano };
    if (tipo === 'MENSAL') {
        const mes = Number(document.getElementById('select_mes')?.value);
        if (!mes) return;
        cfg.mes = mes;
    } else if (tipo === 'TRIMESTRAL') {
        const trimestre = Number(document.getElementById('select_trimestre')?.value);
        if (!trimestre) return;
        cfg.trimestre = trimestre;
    } else if (tipo === 'SEMESTRAL') {
        const semestre = Number(document.getElementById('select_semestre')?.value);
        if (!semestre) return;
        cfg.semestre = semestre;
    }

    periodoCfg = cfg;
    const labelEl = document.getElementById('labelPeriodoAtual');
    if (labelEl) labelEl.textContent = labelPeriodoCfg(cfg);

    gerarKPIS();
    gerarGraficos();
}

// ── KPIs ──────────────────────────────────────────────────────────────────────

async function gerarKPIS() {
    const userId = obterUsuarioIdDashboard();
    if (!userId || !periodoCfg) return;
    const params = buildParams();
    await Promise.all([
        carregarKpiSaldoTotal(userId, params),
        carregarKpiGastoTotal(userId, params),
        carregarKpiMaiorGasto(userId, params),
        carregarKpiCategoriaImpacto(userId, params)
    ]);
}

async function carregarKpiSaldoTotal(userId, params) {
    const el = document.getElementById('saldoTotal');
    if (!el) return;
    el.textContent = '...';
    const { ok, json } = await apiFetch(`${API_BASE}/dashboard/kpi/saldo-total/usuarios/${userId}?${params}`);
    if (!ok || !json) { el.textContent = '–'; el.style.color = ''; return; }
    const saldo = Number(json.saldo ?? 0);
    el.textContent  = CURRENCY.format(saldo);
    el.style.color  = saldo >= 0 ? cssVar('--green-700') : cssVar('--red-700');
}

async function carregarKpiGastoTotal(userId, params) {
    const elValor = document.getElementById('gastoTotal');
    const elBadge = document.getElementById('percentual');
    const elSub   = document.getElementById('subtexto');
    if (!elValor) return;
    elValor.textContent = '...';

    const { ok, json } = await apiFetch(`${API_BASE}/dashboard/kpi/gasto-total/usuarios/${userId}?${params}`);
    if (!ok || !json) {
        elValor.textContent = '–';
        if (elBadge) elBadge.style.display = 'none';
        if (elSub)   elSub.textContent = '';
        return;
    }

    elValor.textContent = CURRENCY.format(Number(json.totalGastos ?? 0));
    const v = Number(json.variacaoPercentual ?? 0);
    if (!elBadge || !elSub) return;

    if (v === 0) {
        elBadge.style.display = 'none';
        elSub.textContent = 'Sem variação em relação ao período anterior';
    } else if (v > 0) {
        elBadge.style.display = '';
        elBadge.style.backgroundColor = cssVar('--red-100');
        elBadge.style.color           = cssVar('--red-700');
        elBadge.innerHTML = `<i class='bx bx-caret-big-up'></i> +${v}%`;
        elSub.textContent = `Em comparação ao ${json.labelPeriodoAnterior || 'período anterior'}`;
    } else {
        elBadge.style.display = '';
        elBadge.style.backgroundColor = cssVar('--green-100');
        elBadge.style.color           = cssVar('--green-700');
        elBadge.innerHTML = `<i class='bx bx-caret-big-down'></i> ${v}%`;
        elSub.textContent = `Em comparação ao ${json.labelPeriodoAnterior || 'período anterior'}`;
    }
}

async function carregarKpiMaiorGasto(userId, params) {
    const elSub    = document.getElementById('categoria');      // subtítulo (titulo do gasto)
    const elValor  = document.getElementById('valorCategoria'); // valor R$
    const elBadge  = document.getElementById('tituloGasto');    // badge verde "X% Do total"
    if (!elSub) return;
    elSub.textContent = '...';

    const { ok, json } = await apiFetch(`${API_BASE}/dashboard/kpi/maior-gasto/usuarios/${userId}?${params}`);
    if (!ok || !json) {
        elSub.textContent = 'Sem dados';
        if (elValor) elValor.textContent = '–';
        if (elBadge) elBadge.style.display = 'none';
        return;
    }

    // Mostra título do gasto como subtítulo e categoria entre parênteses
    elSub.textContent  = json.titulo || '–';
    if (elValor) elValor.textContent = CURRENCY.format(Number(json.valor ?? 0));
    if (elBadge) {
        if (json.percentualDoTotal != null) {
            elBadge.textContent   = `${json.percentualDoTotal}% Do total · ${json.categoria || ''}`;
            elBadge.style.display = '';
        } else {
            elBadge.style.display = 'none';
        }
    }
}

async function carregarKpiCategoriaImpacto(userId, params) {
    const elCateg   = document.getElementById('percentualKPItipo'); // nome da categoria
    const elValor   = document.getElementById('valorKPItipo');      // valor atual
    const elBadge   = document.getElementById('kpi4-badge');        // badge de variação
    const elSub     = document.getElementById('variacaoCategoriaImpacto');
    if (!elCateg) return;
    elCateg.textContent = '...';

    const { ok, json } = await apiFetch(`${API_BASE}/dashboard/kpi/categoria-impacto/usuarios/${userId}?${params}`);
    if (!ok || !json) {
        elCateg.textContent = 'Sem dados';
        if (elValor) elValor.textContent = '–';
        if (elBadge) elBadge.style.display = 'none';
        if (elSub)   elSub.textContent = '';
        return;
    }

    elCateg.textContent = json.categoria || '–';
    if (elValor) elValor.textContent = CURRENCY.format(Number(json.valorAtual ?? 0));

    const v = Number(json.variacaoPercentual ?? 0);
    if (!elBadge || !elSub) return;

    if (v === 0) {
        elBadge.style.display = 'none';
        elSub.textContent = 'Sem variação em relação ao período anterior';
    } else if (v > 0) {
        elBadge.style.display = '';
        elBadge.style.backgroundColor = cssVar('--red-100');
        elBadge.style.color           = cssVar('--red-700');
        elBadge.innerHTML = `<i class='bx bx-caret-big-up'></i> +${v}%`;
        elSub.textContent = 'Em comparação ao período anterior';
    } else {
        elBadge.style.display = '';
        elBadge.style.backgroundColor = cssVar('--green-100');
        elBadge.style.color           = cssVar('--green-700');
        elBadge.innerHTML = `<i class='bx bx-caret-big-down'></i> ${v}%`;
        elSub.textContent = 'Em comparação ao período anterior';
    }
}

// ── GRÁFICOS ─────────────────────────────────────────────────────────────────

function gerarGraficos() {
    const userId = obterUsuarioIdDashboard();
    if (!userId || !periodoCfg) return;

    // Verifica se ApexCharts carregou (local ou CDN)
    if (typeof ApexCharts === 'undefined') {
        console.error('[Dashboard] ApexCharts não carregado — tentando CDN de fallback...');
        const s = document.createElement('script');
        s.src = 'https://cdn.jsdelivr.net/npm/apexcharts/dist/apexcharts.min.js';
        s.onload = () => { gerarGraficos(); };
        s.onerror = () => {
            ['graficoTemporal','graficoComparacao','graficoTipo','graficoDiaSemana'].forEach(id => {
                const el = document.getElementById(id);
                if (el) el.innerHTML = `<div class="aviso"><i class='bx bx-error'></i>
                    <h3>Biblioteca não disponível</h3>
                    <p>Não foi possível carregar o mecanismo de gráficos (ApexCharts).</p></div>`;
            });
        };
        document.head.appendChild(s);
        return;
    }

    const params = buildParams();
    carregarGraficoEvolucao(userId, params);
    carregarGraficoComparacao(userId, params);
    carregarGraficoCategorias(userId, params);
    carregarGraficoDiaSemana(userId, params);
    carregarGraficoFluxo(userId, params);
}

function destroyGrafico(id) {
    if (graficos[id]) { graficos[id].destroy(); delete graficos[id]; }
}

function apexTheme() {
    return isDark() ? 'dark' : 'light';
}

// ── Gráfico 1: Evolução dos gastos + recebimentos (area) ─────────────────────
async function carregarGraficoEvolucao(userId, params) {
    const el = document.getElementById('graficoTemporal');
    if (!el) return;
    el.innerHTML = carregando;

    const { ok, json } = await apiFetch(`${API_BASE}/dashboard/grafico/evolucao-gastos/usuarios/${userId}?${params}`);
    if (!ok || !json?.dados?.length) { el.innerHTML = semDados; return; }

    destroyGrafico('graficoTemporal');
    el.innerHTML = '';

    const corGastos = cssVar('--cor-principal') || '#367373';
    const corRec    = cssVar('--green-700') || '#15803D';

    const series = [{ name: 'Gastos', data: json.dados.map(d => Number(d.valor)) }];
    const colors = [corGastos];

    const hasRec = Array.isArray(json.dadosRecebimentos) && json.dadosRecebimentos.length > 0;
    if (hasRec) {
        series.push({ name: 'Recebimentos', data: json.dadosRecebimentos.map(d => Number(d.valor)) });
        colors.push(corRec);
    }

    try {
        graficos['graficoTemporal'] = new ApexCharts(el, {
            chart: { type: 'area', height: 320, toolbar: { show: false }, zoom: { enabled: false } },
            series,
            xaxis: { categories: json.dados.map(d => d.label), labels: { rotate: -35 } },
            yaxis: { labels: { formatter: v => `R$ ${Number(v).toFixed(0)}` } },
            colors,
            fill: { type: 'gradient', gradient: { shadeIntensity: 1, opacityFrom: 0.45, opacityTo: 0.04 } },
            stroke: { curve: 'smooth', width: 3 },
            dataLabels: { enabled: false },
            legend: { position: 'top' },
            grid: { borderColor: getGridColor(), strokeDashArray: 4 },
            tooltip: { theme: apexTheme(), y: { formatter: v => CURRENCY.format(v) } }
        });
        graficos['graficoTemporal'].render();
    } catch (e) {
        console.error('[Dashboard] Erro ao renderizar graficoTemporal:', e);
        el.innerHTML = semDados;
    }
}

// ── Gráfico 2: Comparação por período (multi-line) ────────────────────────────
async function carregarGraficoComparacao(userId, params) {
    const el = document.getElementById('graficoComparacao');
    if (!el) return;
    el.innerHTML = carregando;

    const { ok, json } = await apiFetch(`${API_BASE}/dashboard/grafico/comparacao-periodo/usuarios/${userId}?${params}`);
    if (!ok || !json?.dados?.length) { el.innerHTML = semDados; return; }

    destroyGrafico('graficoComparacao');
    el.innerHTML = '';

    try {
        graficos['graficoComparacao'] = new ApexCharts(el, {
            chart: { type: 'line', height: 320, toolbar: { show: false }, zoom: { enabled: false } },
            series: [
                { name: json.labelPeriodoAtual    || 'Período atual',    data: json.dados.map(d => Number(d.valorAtual)) },
                { name: json.labelPeriodoAnterior || 'Período anterior', data: json.dados.map(d => Number(d.valorAnterior)) }
            ],
            xaxis: { categories: json.dados.map(d => d.label), labels: { rotate: -35 } },
            yaxis: { labels: { formatter: v => `R$ ${Number(v).toFixed(0)}` } },
            stroke: { curve: 'smooth', width: [3, 3] },
            colors: [cssVar('--cor-principal') || '#367373', getGraficoAntColor()],
            grid: { borderColor: getGridColor(), strokeDashArray: 4 },
            legend: { position: 'top' },
            tooltip: { theme: apexTheme(), y: { formatter: v => CURRENCY.format(v) } }
        });
        graficos['graficoComparacao'].render();
    } catch (e) {
        console.error('[Dashboard] Erro ao renderizar graficoComparacao:', e);
        el.innerHTML = semDados;
    }
}

// ── Gráfico 3: Gastos por Categoria — 3 séries agrupadas ─────────────────────
async function carregarGraficoCategorias(userId, params) {
    const el = document.getElementById('graficoTipo');
    if (!el) return;
    el.innerHTML = carregando;

    const { ok, json } = await apiFetch(`${API_BASE}/dashboard/grafico/categorias/usuarios/${userId}?${params}`);
    if (!ok || !json?.categorias?.length) { el.innerHTML = semDados; return; }

    const cats   = json.categorias;
    const nomes  = cats.map(c => c.nome);
    const valores   = cats.map(c => Number(c.valorTotal));
    const pcts      = cats.map(c => Number(c.percentualDoTotal));
    const ocorrs    = cats.map(c => Number(c.ocorrencias));

    destroyGrafico('graficoTipo');
    el.innerHTML = '';

    try {
        graficos['graficoTipo'] = new ApexCharts(el, {
            chart: { type: 'bar', height: 350, toolbar: { show: false } },
            series: [
                { name: 'Valor (R$)',   data: valores, yAxisIndex: 0 },
                { name: '% do Total',  data: pcts,    yAxisIndex: 1 },
                { name: 'Ocorrências', data: ocorrs,  yAxisIndex: 1 }
            ],
            xaxis: { categories: nomes },
            yaxis: [
                {
                    seriesName: 'Valor (R$)',
                    title: { text: 'Valor (R$)', style: { color: cssVar('--cor-texto-secundario') } },
                    labels: { formatter: v => `R$ ${Number(v).toFixed(0)}` }
                },
                {
                    seriesName: '% do Total',
                    opposite: true,
                    title: { text: '% / Qtd', style: { color: cssVar('--cor-texto-secundario') } },
                    labels: { formatter: v => Number(v).toFixed(0) },
                    min: 0
                },
                {
                    seriesName: 'Ocorrências',
                    opposite: true,
                    show: false
                }
            ],
            colors: getChartPalette3(),
            plotOptions: { bar: { horizontal: false, borderRadius: 4, dataLabels: { position: 'center' } } },
            dataLabels: {
                enabled: true,
                formatter: function(val, opts) {
                    const idx = opts.seriesIndex;
                    if (idx === 0) return `R$ ${Number(val).toFixed(0)}`;
                    if (idx === 1) return `${Number(val).toFixed(0)}%`;
                    return `${Number(val).toFixed(0)}x`;
                },
                style: { fontSize: '10px', colors: [cssVar('--cor-texto-claro') || '#fff'] }
            },
            legend: { position: 'top' },
            grid: { borderColor: getGridColor(), strokeDashArray: 4 },
            tooltip: {
                theme: apexTheme(),
                shared: true,
                intersect: false,
                y: {
                    formatter: function(val, { seriesIndex }) {
                        if (seriesIndex === 0) return CURRENCY.format(val);
                        if (seriesIndex === 1) return `${val}%`;
                        return `${val} vez(es)`;
                    }
                }
            }
        });
        graficos['graficoTipo'].render();
    } catch (e) {
        console.error('[Dashboard] Erro ao renderizar graficoTipo:', e);
        el.innerHTML = semDados;
    }
}

// ── Gráfico 4: Dia da semana (heat map) ou Mês (ANUAL — bar chart) ────────────
async function carregarGraficoDiaSemana(userId, params) {
    const el       = document.getElementById('graficoDiaSemana');
    const tituloEl = document.getElementById('tituloDiaSemana');
    const tooltipEl = document.getElementById('tooltipDiaSemana');
    if (!el) return;

    // Para ANUAL → mostra gastos por mês em vez de dia da semana
    if (periodoCfg?.tipo === 'ANUAL') {
        if (tituloEl)  tituloEl.textContent  = 'Gastos por mês';
        if (tooltipEl) tooltipEl.textContent = 'Total gasto em cada mês do ano selecionado.';
        await carregarGastosPorMes(userId, params, el);
        return;
    }

    if (tituloEl)  tituloEl.textContent  = 'Gastos por dia da semana';
    if (tooltipEl) tooltipEl.textContent = 'Dias com mais gastos aparecem em vermelho; dias com menos gastos em azul.';
    el.innerHTML = carregando;

    const { ok, json } = await apiFetch(`${API_BASE}/dashboard/grafico/gastos-dia-semana/usuarios/${userId}?${params}`);
    if (!ok || !json?.dias?.length) { el.innerHTML = semDados; return; }

    const ABREV = {
        'segunda-feira': 'Seg', 'terça-feira': 'Ter', 'quarta-feira': 'Qua',
        'quinta-feira': 'Qui', 'sexta-feira': 'Sex', 'sábado': 'Sáb', 'domingo': 'Dom'
    };

    const cells = json.dias.map(d => {
        const abrev = ABREV[d.dia.toLowerCase()] || d.dia.slice(0, 3);
        const cor   = heatColor(d.normalizado);
        const valor = d.valorTotal > 0 ? CURRENCY.format(d.valorTotal) : '–';
        return `<div class="hm-cell" title="${d.dia}: ${valor}">
            <span class="hm-nome">${abrev}</span>
            <div class="hm-box" style="background:${cor};"></div>
            <span class="hm-valor">${valor}</span>
        </div>`;
    }).join('');

    el.innerHTML = `
        <div class="hm-wrap">
            <div class="hm-grid">${cells}</div>
            <div class="hm-legenda">
                <span class="hm-leg-txt">Menos Gastos</span>
                <div class="hm-grad"></div>
                <span class="hm-leg-txt">Mais Gastos</span>
            </div>
        </div>`;
}

// Gastos por mês — usado no modo ANUAL
async function carregarGastosPorMes(userId, params, el) {
    el.innerHTML = carregando;

    const { ok, json } = await apiFetch(`${API_BASE}/dashboard/grafico/evolucao-gastos/usuarios/${userId}?${params}`);
    if (!ok || !json?.dados?.length) { el.innerHTML = semDados; return; }

    destroyGrafico('graficoDiaSemana');
    el.innerHTML = '';

    try {
        graficos['graficoDiaSemana'] = new ApexCharts(el, {
            chart: { type: 'bar', height: 300, toolbar: { show: false } },
            series: [{ name: 'Gasto mensal', data: json.dados.map(d => Number(d.valor)) }],
            xaxis: {
                categories: json.dados.map(d => d.label),
                labels: { rotate: -35 }
            },
            yaxis: { labels: { formatter: v => `R$ ${Number(v).toFixed(0)}` } },
            colors: [cssVar('--cor-principal') || '#367373'],
            plotOptions: { bar: { borderRadius: 6, dataLabels: { position: 'top' } } },
            dataLabels: {
                enabled: true,
                formatter: v => `R$ ${Number(v).toFixed(0)}`,
                style: { fontSize: '10px', colors: [cssVar('--cor-texto-principal') || '#1A1A1A'] },
                offsetY: -20
            },
            grid: { borderColor: getGridColor(), strokeDashArray: 4 },
            tooltip: { theme: apexTheme(), y: { formatter: v => CURRENCY.format(v) } }
        });
        graficos['graficoDiaSemana'].render();
    } catch (e) {
        console.error('[Dashboard] Erro ao renderizar gastos por mês:', e);
        el.innerHTML = semDados;
    }
}

// ── Gráfico 5: Fluxo de entradas e saídas — Sankey (Google Charts) ────────────
async function carregarGraficoFluxo(userId, params) {
    const el = document.getElementById('graficoFluxo');
    if (!el) return;
    el.innerHTML = carregando;
    el.style.minHeight = '420px';

    const { ok, json } = await apiFetch(`${API_BASE}/dashboard/grafico/fluxo-financeiro/usuarios/${userId}?${params}`);
    if (!ok || !json) { el.innerHTML = semDados; return; }

    const links = json.links || [];
    const nos   = json.nos   || [];
    if (!links.length) { el.innerHTML = semDados; return; }

    // Mapeia id → label dos nós
    const labelMap = {};
    nos.forEach(n => { if (n.id && n.label) labelMap[n.id] = n.label; });

    const rows = links
        .filter(l => l.de && l.para && Number(l.valor) > 0)
        .map(l => [
            labelMap[l.de]   || l.de,
            labelMap[l.para] || l.para,
            Number(l.valor)
        ]);

    if (!rows.length) { el.innerHTML = semDados; return; }

    const renderizar = () => {
        try {
            el.innerHTML = '';
            el.style.height = '420px';

            const dark = isDark();
            const txtColor = dark
                ? (cssVar('--cor-texto-principal') || '#FFFFFF')
                : (cssVar('--cor-texto-principal') || '#1A1A1A');

            const data = new google.visualization.DataTable();
            data.addColumn('string', 'De');
            data.addColumn('string', 'Para');
            data.addColumn('number', 'Valor (R$)');
            data.addRows(rows);

            const chart = new google.visualization.Sankey(el);
            chart.draw(data, {
                width: el.clientWidth || 800,
                height: 420,
                sankey: {
                    node: {
                        colors: getSankeyNodeColors(),
                        label: { color: txtColor, fontName: 'Open Sans', fontSize: 13 },
                        width: 18,
                        nodePadding: 24,
                        interactivity: true
                    },
                    link: { colorMode: 'gradient', fillOpacity: 0.38 },
                    iterations: 64
                }
            });
        } catch (e) {
            console.error('[Dashboard] Erro ao renderizar Sankey:', e);
            el.innerHTML = semDados;
        }
    };

    // Tenta renderizar; se Google Charts não carregou ainda, agenda para quando estiver pronto
    if (_gcReady) {
        renderizar();
    } else {
        // Mostra mensagem enquanto aguarda
        el.innerHTML = `<p style="text-align:center;padding:32px;color:var(--cor-texto-secundario);">
            Aguardando carregamento do gráfico de fluxo…</p>`;
        _gcQueue.push(renderizar);
        // Timeout de segurança: se após 8s ainda não carregou, mostra aviso
        setTimeout(() => {
            if (!_gcReady && el.innerHTML.includes('Aguardando')) {
                el.innerHTML = `<div class="aviso">
                    <i class='bx bx-wifi-off'></i>
                    <h3>Gráfico indisponível</h3>
                    <p>Não foi possível carregar a biblioteca de fluxo. Verifique a conexão.</p>
                </div>`;
            }
        }, 8000);
    }
}
