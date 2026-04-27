const _divAlerta = document.getElementById('div_alerta');
if (_divAlerta) _divAlerta.style.display = 'none';

let dataGasto;
const usuarioLogado = JSON.parse(localStorage.getItem("usuarioLogado") || "null");
const userId = usuarioLogado ? usuarioLogado.id : null;

let _alertaTimer = null;

function alerta(texto, duracaoMs = 4000) {
    const divAl = document.getElementById("div_alerta");
    const contAl = document.getElementById("conteudoAlerta");
    if (divAl) divAl.style.display = "flex";
    if (contAl) contAl.innerHTML = texto;

    if (_alertaTimer) clearTimeout(_alertaTimer);
    if (duracaoMs > 0) {
        _alertaTimer = setTimeout(() => {
            if (divAl) divAl.style.display = "none";
            _alertaTimer = null;
        }, duracaoMs);
    }
}

function gerarInformacoes() {
    gerarCategorias();
    gerarInstituicao();
}

async function atualizarSaldoDisplay(instituicaoUsuarioId) {
    const el = document.getElementById('saldo_display');
    if (!el) return;
    if (!instituicaoUsuarioId || instituicaoUsuarioId === '#') {
        el.style.display = 'none';
        return;
    }
    try {
        const res = await fetch(`http://localhost:8080/instituicoes/saldo/${Number(instituicaoUsuarioId)}`);
        if (!res.ok) { el.style.display = 'none'; return; }
        const saldo = await res.json();
        const valor = Number(saldo);
        el.textContent = `Saldo disponível: R$ ${valor.toFixed(2)}`;
        el.style.color = valor <= 0 ? '#e53e3e' : '#367373';
        el.style.display = '';
    } catch (e) {
        el.style.display = 'none';
    }
}

function toggleParcelas(inputId, movimento) {
    const requer = movimento === 'Credito' || movimento === 'Boleto';
    const wrapId = inputId === 'ipt_parcelas' ? 'wrap_parcelas' : 'wrap_multi_parcelas';
    const wrap = document.getElementById(wrapId);
    const input = document.getElementById(inputId);
    if (wrap) wrap.style.display = requer ? '' : 'none';
    if (input && !requer) input.value = '1';
}

function gerarCategorias() {
    if (!userId) return;
    const selectCat = document.getElementById("select_categoria");
    const multiSelectCat = document.getElementById("multi_select_categoria");
    if (selectCat) selectCat.innerHTML = "<option value='#'>Escolha uma categoria</option>";
    if (multiSelectCat) multiSelectCat.innerHTML = "<option value='#'>Categoria</option>";
    MainAPI.getTipos(userId)
        .then(json => {
            for (let c = 0; json.length > c; c++) {
                const opt = `<option value="${json[c].id}">${json[c].categoria.titulo}</option>`;
                if (selectCat) selectCat.innerHTML += opt;
                if (multiSelectCat) multiSelectCat.innerHTML += opt;
            }
        })
        .catch(error => {
            console.error("Erro ao carregar categorias:", error);
            alerta("Não foi possível carregar as categorias. Tente novamente.");
        });
}

const gerarTipos = gerarCategorias;

async function gerarInstituicao() {
    if (!userId) return;
    const selectInst = document.getElementById("select_instituicao");
    const multiSelectInst = document.getElementById("multi_select_instituicao");
    if (selectInst) selectInst.innerHTML = "<option value='#'> Escolha uma instituição</option>";
    if (multiSelectInst) multiSelectInst.innerHTML = "<option value='#'>Instituição</option>";
    MainAPI.getInstituicoes(userId)
        .then(json => {
            for (let c = 0; json.length > c; c++) {
                const opt = `<option value="${json[c].id}">${json[c].intituicao.nome}</option>`;
                if (selectInst) selectInst.innerHTML += opt;
                if (multiSelectInst) multiSelectInst.innerHTML += opt;
            }
        })
        .catch(error => {
            console.error("Erro ao carregar instituições:", error);
            alerta("Não foi possível carregar as instituições. Tente novamente.");
        });
}

async function controleInstituicao(){
    console.log("entrei no controle de instituição")
    escondido = document.getElementById("escondido")

    escondido.style.display = ""

}

async function registrar() {
    var data = dataGasto;
    var valor = Number(document.getElementById('ipt_valor').value);
    var titulo = document.getElementById('ipt_nome').value;
    var tipo = document.getElementById('select_tipo').value;
    var categoria = document.getElementById('select_categoria').value;
    var Desc = document.getElementById('ipt_desc').value;
    var instituicao = document.getElementById('select_instituicao').value;
    var movimento = document.getElementById('select_movimento').value;
    var parcelas = Number(document.getElementById('ipt_parcelas').value) || 1;

    if (Desc == "" || Desc == false) {
        Desc = "Nenhuma descrição fornecida"
    }
    if (data == false || data == 0) {
        return alerta("Data inválida");
    }
    if (valor <= 0 || valor == null || valor == undefined) {
        return alerta("Valor inválido");
    }
    if (titulo == "") {
        return alerta("Titulo inválido");
    }
    if (tipo == '#') {
        return alerta("Escolha o tipo do evento");
    }
    if (categoria == '#') {
        return alerta("Escolha uma categoria");
    }
    if (instituicao == '#') {
        return alerta("Escolha uma instituição");
    }
    if (movimento == '#') {
        return alerta("Escolha o tipo de movimento");
    }
    if ((movimento === 'Credito' || movimento === 'Boleto') && (parcelas < 1 || isNaN(parcelas))) {
        return alerta("Informe a quantidade de parcelas (mínimo 1)");
    }

    // Verificar saldo quando o tipo exige débito da conta
    if (tipo === 'Gasto' || tipo === 'Transferencia') {
        try {
            const resSaldo = await fetch(`http://localhost:8080/instituicoes/saldo/${Number(instituicao)}`);
            if (resSaldo.ok) {
                const saldo = await resSaldo.json();
                if (Number(saldo) < valor) {
                    return alerta(`Saldo insuficiente. Saldo disponível: R$ ${Number(saldo).toFixed(2)}`);
                }
            }
        } catch (e) {
            console.warn("Não foi possível verificar o saldo:", e);
        }
    }

    alerta(`Registrando...
        <div class="glaceonCorrendoDiv">
    <img class="glaceon correndo" src="/assets/gif/glaceon-correndo-unscreen.gif" alt="">
    </div>
        `, 0);

    MainAPI.registrarGasto({
        financeiro: {
            usuario_id: userId,
            tipo: tipo,
            valor: valor,
            descricao: Desc,
            dataEvento: data
        },
        instituicao: [{
            instituicaoUsuario_id: Number(instituicao),
            tipoMovimento: movimento,
            valor: valor,
            parcelas: parcelas
        }],
        detalhe: {
            categoriaUsuario_id: [Number(categoria)],
            tituloGasto: titulo
        }
    }).then(async (response) => {
        console.log("Resposta status:", response.status);
        if (response.ok) {
            atualizarSaldoDisplay(instituicao);
            return setTimeout(() => alerta(
                `Registro realizado com sucesso!<br>
                <div>
                    <button onclick='window.location.reload()'>Continuar a registrar</button>
                </div>`, 0
            ), 1500);
        } else {
            let detalhe = "";
            try {
                const corpo = await response.json();
                detalhe = corpo.message || corpo.error || JSON.stringify(corpo);
            } catch (_) {
                detalhe = `HTTP ${response.status}`;
            }
            console.error("Erro ao registrar:", detalhe);
            alerta(`Erro ao registrar (${response.status}): ${detalhe}`);
        }
    }).catch((err) => {
        console.error("Erro de rede:", err);
        alerta("Erro de conexão ao registrar.");
    });

}

function atualizarSaldo(valor, instituicao) {
    console.log("chamou atualizar saldo")
    MainAPI.atualizarSaldo({
        valorServer: valor,
        instituicaoServer: instituicao
    }).then((resposta) => {
        console.log("Resposta:", resposta);
        if (resposta.ok) {
            return console.log("Saldo atualizado")
        }
        else {
            console.error("Houver um erro ao atualizar o saldo", resposta)
        }
    });
}

function adicionarTipos() {
    if (!userId) {
        return alerta(`Nenhum usuário logado. <button onclick='div_alerta.style.display="none"'>OK</button>`);
    }
    var titulo = document.getElementById("ipt_tituloTipo").value.trim()
    if (!titulo) {
        return alerta(`Digite um nome para o tipo <button onclick='div_alerta.style.display="none"'>OK</button>`);
    }
    MainAPI.adicionarTipo({ titulo: titulo }, userId).then(function (resposta) {
        console.log("Resposta: ", resposta);
        if (resposta.ok) {
            gerarTipos();
            adicionarTipo.close();
            return alerta(`Tipo adicionado com sucesso!<br>
                <button onclick="div_alerta.style.display='none'">OK</button>`)
        }
        else {
            return alerta(`Houve um erro ao adicionar tipo <button onclick='div_alerta.style.display="none"'>OK</button>`)
        }
    }).catch(function (error) {
        console.error("Erro ao adicionar tipo:", error);
        return alerta(`Erro ao conectar ao servidor. <button onclick='div_alerta.style.display="none"'>OK</button>`);
    });
}

/*-------------- Calendário --------------*/

const btnAbrir = document.getElementById("calendario");
const modal = document.getElementById("modal");
const fechar = document.getElementById("fechar");
const dias = document.getElementById("dias");
const mesAno = document.getElementById("mesAno");
const gastosDoDia = document.getElementById("gastosDoDia");
const confirmar = document.getElementById("confirmar");
const btnAnterior = document.getElementById("btnAnterior");
const btnProximo = document.getElementById("btnProximo");

let dataSelecionada = null;

let hoje = new Date();
let mesAtual = hoje.getMonth();
let anoAtual = hoje.getFullYear();


let modalAbertoPor = 'single';
let lote = [];

btnAbrir.onclick = () => {
    modalAbertoPor = 'single';
    modal.style.display = "flex";
};

function abrirCalendarioMulti() {
    modalAbertoPor = 'multi';
    modal.style.display = 'flex';
}
fechar.onclick = () => modal.style.display = "none";
btnAnterior.onclick = () => {
    mesAtual--;
    if (mesAtual < 0) {
        mesAtual = 11;
        anoAtual--;
    }
    gerarCalendario();
}
btnProximo.onclick = () => {
    mesAtual++;
    if (mesAtual > 11) {
        mesAtual = 0;
        anoAtual++;
    }
    gerarCalendario();
}

const gastos = {
    "2025-01-05": ["R$ 25,00 - Café", "R$ 80,00 - Mercado"],
    "2025-01-10": ["R$ 14,00 - Transporte"],
};

async function buscarGastosDia(dataSelecionada) {
    console.log("Buscando gastos para", dataSelecionada);

    const json = await MainAPI.buscarRegistrosPorData(userId, dataSelecionada);
    console.log("Tamanho de gastos:", json.length);

    let listaGastos = [];

    for (let c = 0; c < json.length; c++) {
        const data = new Date(json[c].dataGasto);
        const dataFormatada = data.toISOString().split("T")[0];

        if (dataFormatada === dataSelecionada) {
            listaGastos.push(json[c]);
        }
    }

    return listaGastos;
}


function gerarCalendario() {
    dias.innerHTML = "";
    let primeiroDia = new Date(anoAtual, mesAtual, 1).getDay();
    let totalDias = new Date(anoAtual, mesAtual + 1, 0).getDate();

    mesAno.innerText = new Date(anoAtual, mesAtual)
        .toLocaleString("pt-BR", { month: "long", year: "numeric" });

    for (let i = 0; i < primeiroDia; i++) {
        dias.innerHTML += `<span></span>`;
    }
    for (let dia = 1; dia <= totalDias; dia++) {
        let dataFormatada = `${anoAtual}-${String(mesAtual + 1).padStart(2, '0')}-${String(dia).padStart(2, '0')}`;

        let span = document.createElement("span");
        span.innerText = dia;

        span.onclick = () => selecionarDia(dataFormatada, span);

        dias.appendChild(span);
    }
}

async function selecionarDia(data, elemento) {
    dataP = document.getElementById('data')
    dataSelecionada = data;

    document.querySelectorAll(".diaSelecionado")
        .forEach(e => e.classList.remove("diaSelecionado"));

    elemento.classList.add("diaSelecionado");
    confirmar.disabled = false;

    try {
        const listaGastos = await buscarGastosDia(data);

        console.log("tamanho lista gastos: ", listaGastos.length)
        if (listaGastos.length > 0) {
            console.log("Lista de gastos > 0")
            const dataRegistro = listaGastos[0].eventoFinanceiro?.dataEvento ?? listaGastos[0].dataGasto;
            const novaData = new Date(dataRegistro);
            const dataFormatada = isNaN(novaData.getTime())
                ? formatarDataBR(data)
                : novaData.toLocaleDateString("pt-BR");
            gastosDia.innerHTML = `
                <b>Gastos de ${dataFormatada}:</b><br>
            `;
            for (let c = 0; c < listaGastos.length; c++) {
                const gasto = listaGastos[c];
                const tituloGasto = gasto.gastoDetalhe?.tituloGasto ?? gasto.tituloGasto;
                const valorGasto = gasto.eventoFinanceiro?.valor ?? gasto.valor;
                gastosDia.innerHTML += `
                <b>${tituloGasto} - R$${valorGasto}</b><br>
            `;
            }
        } else {
            gastosDia.innerHTML = "<i>Nenhum gasto neste dia.</i>";
        }
    } catch (e) {
        console.warn("Erro ao buscar gastos do dia:", e);
        gastosDia.innerHTML = "";
    }
}

confirmar.onclick = () => {
    modal.style.display = "none";
    const dataFormatada = formatarDataBR(dataSelecionada);
    if (modalAbertoPor === 'multi') {
        const label = document.getElementById('multi_data_label');
        const hidden = document.getElementById('multi_data');
        if (label) { label.textContent = dataFormatada; label.classList.remove('hidden'); label.style.display = ''; }
        if (hidden) hidden.value = dataSelecionada;
    } else {
        const dataEl = document.getElementById('data');
        if (dataEl) { dataEl.style.display = ''; dataEl.textContent = dataFormatada; dataEl.classList.remove('hidden'); }
        dataGasto = dataSelecionada;
    }
};

gerarCalendario();

function formatarDataBR(dataISO) {
    const [ano, mes, dia] = dataISO.split("-");
    return `${dia}/${mes}/${ano}`;
}

/*---------------- Parte do Header ----------------*/
const titulos = document.querySelectorAll(".headerFormulario .titulo");

titulos.forEach(item => {
    item.addEventListener("click", () => {

        // Remove classe 'ativo' de todos
        titulos.forEach(e => e.classList.remove("ativo"));

        // Adiciona no clicado
        item.classList.add("ativo");

        // Identificar qual aba foi selecionada (opcional)
        let aba = item.dataset.form;
        console.log("Aba selecionada:", aba);

        // aqui você pode trocar telas, formularios etc
    });
});

/*---------------- Múltiplos Registros ----------------*/

function adicionarAoLote() {
    const titulo = document.getElementById('ipt_multi_nome').value.trim();
    const tipo = document.getElementById('multi_select_tipo').value;
    const categoria = document.getElementById('multi_select_categoria').value;
    const instituicao = document.getElementById('multi_select_instituicao').value;
    const movimento = document.getElementById('multi_select_movimento').value;
    const parcelas = Number(document.getElementById('ipt_multi_parcelas').value) || 1;
    const valor = Number(document.getElementById('ipt_multi_valor').value);
    const desc = document.getElementById('ipt_multi_desc').value.trim() || 'Nenhuma descrição fornecida';
    const data = document.getElementById('multi_data').value;

    if (!titulo) return alerta('Título inválido');
    if (tipo === '#') return alerta('Escolha o tipo do evento');
    if (categoria === '#') return alerta('Escolha uma categoria');
    if (instituicao === '#') return alerta('Escolha uma instituição');
    if (movimento === '#') return alerta('Escolha o tipo de movimento');
    if ((movimento === 'Credito' || movimento === 'Boleto') && (parcelas < 1 || isNaN(parcelas))) return alerta('Informe a quantidade de parcelas (mínimo 1)');
    if (valor <= 0 || isNaN(valor)) return alerta('Valor inválido');
    if (!data) return alerta('Escolha uma data');

    const instSelect = document.getElementById('multi_select_instituicao');
    const instNome = instSelect.options[instSelect.selectedIndex].text;

    lote.push({
        financeiro: { usuario_id: userId, tipo, valor, descricao: desc, dataEvento: data },
        instituicao: [{ instituicaoUsuario_id: Number(instituicao), tipoMovimento: movimento, valor, parcelas }],
        detalhe: { categoriaUsuario_id: [Number(categoria)], tituloGasto: titulo },
        _display: { titulo, tipo, movimento, instNome, valor }
    });

    renderizarLote();

    document.getElementById('ipt_multi_nome').value = '';
    document.getElementById('ipt_multi_valor').value = '';
    document.getElementById('ipt_multi_desc').value = '';
    document.getElementById('multi_select_tipo').value = '#';
    document.getElementById('multi_select_categoria').value = '#';
    document.getElementById('multi_select_instituicao').value = '#';
    document.getElementById('multi_select_movimento').value = '#';
    document.getElementById('ipt_multi_parcelas').value = '1';
    toggleParcelas('ipt_multi_parcelas', '#');
}

function renderizarLote() {
    const tbody = document.getElementById('corpoLote');
    if (lote.length === 0) {
        tbody.innerHTML = `<tr id="loteVazio"><td colspan="6" style="text-align:center; color:#888; font-style:italic; padding:20px;">Nenhum registro adicionado ainda.</td></tr>`;
        return;
    }
    tbody.innerHTML = '';
    lote.forEach((item, i) => {
        const d = item._display;
        tbody.innerHTML += `
            <tr>
                <td>${d.titulo}</td>
                <td>${d.tipo}</td>
                <td>${d.movimento}</td>
                <td>${d.instNome}</td>
                <td>R$ ${d.valor.toFixed(2)}</td>
                <td><button onclick="removerDoLote(${i})" style="background:none;border:none;cursor:pointer;color:red;font-size:1.1rem;">✕</button></td>
            </tr>`;
    });
}

function removerDoLote(i) {
    lote.splice(i, 1);
    renderizarLote();
}

function salvarLote() {
    if (lote.length === 0) return alerta('Nenhum registro no lote');
    alerta(`Salvando ${lote.length} registro(s)...`, 0);

    const promessas = lote.map(item => MainAPI.registrarGasto({
        financeiro: item.financeiro,
        instituicao: item.instituicao,
        detalhe: item.detalhe
    }));

    Promise.all(promessas).then(respostas => {
        const erros = respostas.filter(r => !r.ok).length;
        if (erros === 0) {
            lote = [];
            renderizarLote();
            alerta(`${respostas.length} registro(s) salvos com sucesso!<br><button onclick="document.getElementById('div_alerta').style.display='none'">OK</button>`, 0);
        } else {
            alerta(`${erros} erro(s) ao salvar. Verifique e tente novamente.`);
        }
    }).catch(() => alerta('Erro ao conectar ao servidor'));
}

function trocarFormulario(tela) {
    const cardUnico = document.getElementById("cardUnico");
    const cardMultiplo = document.getElementById("multiplosRegistros");
    const tabUnico = document.getElementById("tabUnico");
    const tabMultiplo = document.getElementById("tabMultiplo");
    if (tela === "multiplosRegistros") {
        if (cardUnico) cardUnico.style.display = "none";
        if (cardMultiplo) cardMultiplo.style.display = "flex";
        if (tabUnico) tabUnico.classList.remove("ativo");
        if (tabMultiplo) tabMultiplo.classList.add("ativo");
    } else {
        if (cardUnico) cardUnico.style.display = "";
        if (cardMultiplo) cardMultiplo.style.display = "none";
        if (tabUnico) tabUnico.classList.add("ativo");
        if (tabMultiplo) tabMultiplo.classList.remove("ativo");
    }
}