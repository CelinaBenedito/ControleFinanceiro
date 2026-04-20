div_alerta.style.display = 'none';

let dataGasto;
const usuarioLogado = JSON.parse(localStorage.getItem("usuarioLogado") || "null");
const userId = usuarioLogado ? usuarioLogado.id : null;

function alerta(texto) {
    div_alerta.style.display = "flex"
    conteudoAlerta.innerHTML =
        `
        ${texto}
        `
}

function gerarInformacoes() {
    gerarTipos();
    gerarInstituicao();
}

function gerarTipos() {
    if (!userId) return;
    const multiTipo = document.getElementById("multi_select_tipo");
    select_tipo.innerHTML = "<option value='#'>Escolha um tipo</option>";
    if (multiTipo) multiTipo.innerHTML = "<option value='#'>Tipo do Evento</option>";
    MainAPI.getTipos(userId).then(json => {
        for (let c = 0; json.length > c; c++) {
            const opt = `<option value="${json[c].categoria.id}">${json[c].categoria.titulo}</option>`;
            select_tipo.innerHTML += opt;
            if (multiTipo) multiTipo.innerHTML += opt;
        }
    }).catch(function(error) {
        console.error("Erro ao carregar tipos:", error);
    });
}

async function gerarInstituicao() {
    const gestaoInstituicao = document.getElementById("gestaoInstituicao");
    const multiInstituicao = document.getElementById("multi_select_instituicao");
    select_instituicao.innerHTML = "<option value='#'> Escolha uma institui\u00e7\u00e3o</option>";
    if (gestaoInstituicao) gestaoInstituicao.innerHTML = "";
    if (multiInstituicao) multiInstituicao.innerHTML = "<option value='#'>Institui\u00e7\u00e3o</option>";
    MainAPI.getInstituicoes().then(json => {
        for (let c = 0; json.length > c; c++) {
            select_instituicao.innerHTML += `<option value="${json[c].id}">${json[c].nome}</option>`;
            if (gestaoInstituicao) gestaoInstituicao.innerHTML += `<option onclick="controleInstituicao()">${json[c].nome}</option>`;
            if (multiInstituicao) multiInstituicao.innerHTML += `<option value="${json[c].id}">${json[c].nome}</option>`;
        }
    });
}

async function controleInstituicao(){
    console.log("entrei no controle de instituição")
    escondido = document.getElementById("escondido")

    escondido.style.display = ""

}

function registrar() {
    ("Iniciando registro!")
    var data = dataGasto;
    var valor = Number(document.getElementById('ipt_valor').value);
    var titulo = ipt_nome.value;
    var tipo = select_tipo.value;
    var Desc = ipt_desc.value;
    var instituicao = select_instituicao.value

    if (Desc == "" || Desc == false) {
        Desc = "Nenhuma descrição fornecida"
    }
    if (data == false || data == 0) {
        return alert("Data inválida");
    }
    if (valor <= 0 || valor == null || valor == undefined || valor == NaN) {
        return alert("Valor inválido");
    }
    if (titulo == "") {
        return alert("Titulo inválido");
    }
    if (tipo == '#') {
        return alert("Escolha um tipo");
    }
    if (instituicao == '#') {
        return alert("Escolha uma instituição");
    }

    setTimeout(alerta(`Registrando...
        <div class="glaceonCorrendoDiv">
    <img class="glaceon correndo" src="/assets/gif/glaceon-correndo-unscreen.gif" alt="">
    </div>
        `), 2000)

    MainAPI.registrarGasto({
        valorServer: valor,
        descServer: Desc,
        tipoServer: tipo,
        tituloServer: titulo,
        dataServer: data,
        instituicaoServer: instituicao
    }).then((response) => {
        console.log("Resposta:", response);
        if (response.ok) {
            atualizarSaldo(valor, instituicao);
            return setTimeout(() => alerta(
                `  
                        Registro realizado com sucesso!<br>
                        <div>

                        <button onclick='window.location.reload()'>
                            Continuar a registrar
                        </button>

                        </div>
                `
            ), 3000);

        }
        else {
            alert("Houve um erro ao registrar", response)
        }
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


btnAbrir.onclick = () => modal.style.display = "flex";
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

    const json = await MainAPI.buscarRegistrosPorData(dataSelecionada);
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

    const listaGastos = await buscarGastosDia(data);

    console.log("tamanho lista gastos: ",listaGastos.length)
    if (listaGastos.length > 0) {
        console.log("Lista de gastos > 0")
        novaData = new Date(listaGastos[0].dataGasto);
        const dataFormatada = novaData.toLocaleDateString("pt-BR");
        gastosDia.innerHTML = `
            <b>Gastos de ${dataFormatada}:</b><br>
        `;
        for (let c = 0; c < listaGastos.length; c++) {
            gastosDia.innerHTML += `
            <b>${listaGastos[c].tituloGasto} - R$${listaGastos[c].valor}</b><br>
        `;
        }
    } else {
        gastosDia.innerHTML = "<i>Nenhum gasto neste dia.</i>";
    }

    confirmar.disabled = false;
}

confirmar.onclick = () => {
    modal.style.display = "none";
    dataP.style.display = ""
    dataFormatada = formatarDataBR(dataSelecionada)
    dataP.textContent = dataFormatada
    dataP.classList.remove("hidden");
    dataGasto = dataSelecionada;

    // Atualiza data na aba de Múltiplos Registros também
    const multiDataHidden = document.getElementById("multi_data");
    const multiDataLabel = document.getElementById("multi_data_label");
    if (multiDataHidden) multiDataHidden.value = dataFormatada;
    if (multiDataLabel) {
        multiDataLabel.textContent = dataFormatada;
        multiDataLabel.classList.remove("hidden");
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

function trocarFormulario(tela) {
    const cardUnico = document.getElementById("cardUnico");
    const cardMultiplo = document.getElementById("multiplosRegistros");
    document.querySelectorAll(".ar-tab").forEach(t => t.classList.remove("ativo"));
    if (tela === "multiplosRegistros") {
        cardUnico.style.display = "none";
        cardMultiplo.style.display = "flex";
        document.getElementById("tabMultiplo").classList.add("ativo");
    } else {
        cardUnico.style.display = "";
        cardMultiplo.style.display = "none";
        document.getElementById("tabUnico").classList.add("ativo");
    }
}

/* ---- Múltiplos Registros ---- */
let loteRegistros = [];


function adicionarAoLote() {
    const titulo = document.getElementById("ipt_multi_nome").value.trim();
    const tipoSel = document.getElementById("multi_select_tipo");
    const tipo = tipoSel.value;
    const tipoNome = tipoSel.options[tipoSel.selectedIndex].text;
    const instSel = document.getElementById("multi_select_instituicao");
    const instId = instSel.value;
    const instNome = instSel.options[instSel.selectedIndex].text;
    const movSel = document.getElementById("multi_select_movimento");
    const movimento = movSel.value;
    const valor = parseFloat(document.getElementById("ipt_multi_valor").value);
    const desc = document.getElementById("ipt_multi_desc").value.trim() || "Nenhuma descrição fornecida";
    const dataRaw = document.getElementById("multi_data").value;
    // Converte DD/MM/AAAA → YYYY-MM-DD para a API
    const dataPartes = dataRaw.split("/");
    const data = dataPartes.length === 3 ? `${dataPartes[2]}-${dataPartes[1]}-${dataPartes[0]}` : "";

    if (!titulo) return alert("Título inválido");
    if (tipo === "#") return alert("Escolha um tipo de evento");
    if (instId === "#") return alert("Escolha uma instituição");
    if (movimento === "#") return alert("Escolha o movimento");
    if (!data || dataRaw.length !== 10) return alert("Data inválida. Use o formato DD/MM/AAAA");
    if (!valor || valor <= 0) return alert("Valor inválido");

    loteRegistros.push({ titulo, tipo, tipoNome, instId, instNome, movimento, valor, desc, data });
    renderizarTabelaLote();
    document.getElementById("ipt_multi_nome").value = "";
    document.getElementById("ipt_multi_valor").value = "";
    document.getElementById("ipt_multi_desc").value = "";
}

function renderizarTabelaLote() {
    const tbody = document.getElementById("corpoLote");
    if (!tbody) return;
    tbody.innerHTML = "";
    if (loteRegistros.length === 0) {
        const trVazio = document.createElement("tr");
        trVazio.id = "loteVazio";

        const tdVazio = document.createElement("td");
        tdVazio.colSpan = 6;
        tdVazio.style.textAlign = "center";
        tdVazio.style.color = "#888";
        tdVazio.style.fontStyle = "italic";
        tdVazio.style.padding = "20px";
        tdVazio.textContent = "Nenhum registro adicionado ainda.";

        trVazio.appendChild(tdVazio);
        tbody.appendChild(trVazio);
        return;
    }
    loteRegistros.forEach((r, i) => {
        const tr = document.createElement("tr");

        const tdTitulo = document.createElement("td");
        tdTitulo.textContent = r.titulo;
        tr.appendChild(tdTitulo);

        const tdTipoNome = document.createElement("td");
        tdTipoNome.textContent = r.tipoNome;
        tr.appendChild(tdTipoNome);

        const tdMovimento = document.createElement("td");
        tdMovimento.textContent = r.movimento;
        tr.appendChild(tdMovimento);

        const tdInstNome = document.createElement("td");
        tdInstNome.textContent = r.instNome;
        tr.appendChild(tdInstNome);

        const tdValor = document.createElement("td");
        tdValor.textContent = `R$ ${r.valor.toFixed(2).replace(".", ",")}`;
        tr.appendChild(tdValor);

        const tdAcao = document.createElement("td");
        const botaoRemover = document.createElement("button");
        botaoRemover.className = "ar-btn";
        botaoRemover.style.height = "32px";
        botaoRemover.style.minWidth = "0";
        botaoRemover.style.padding = "0 10px";
        botaoRemover.style.fontSize = "0.8rem";
        botaoRemover.style.background = "#e53e3e";
        botaoRemover.style.margin = "0";
        botaoRemover.textContent = "\u2716";
        botaoRemover.addEventListener("click", () => removerDoLote(i));
        tdAcao.appendChild(botaoRemover);
        tr.appendChild(tdAcao);

        tbody.appendChild(tr);
    });
}

function removerDoLote(index) {
    loteRegistros.splice(index, 1);
    renderizarTabelaLote();
}

async function salvarLote() {
    if (loteRegistros.length === 0) return alert("Adicione pelo menos um registro");
    alerta(`Salvando ${loteRegistros.length} registro(s)...
        <div class="glaceonCorrendoDiv">
            <img class="glaceon correndo" src="/assets/gif/glaceon-correndo-unscreen.gif" alt="">
        </div>
    `);
    try {
        for (const r of loteRegistros) {
            await MainAPI.registrarGasto({
                valorServer: r.valor,
                descServer: r.desc,
                tipoServer: r.tipo,
                tituloServer: r.titulo,
                dataServer: r.data,
                instituicaoServer: r.instId
            });
            await MainAPI.atualizarSaldo(r.valor, r.instId);
        }
        const total = loteRegistros.length;
        loteRegistros = [];
        renderizarTabelaLote();
        setTimeout(() => alerta(`
            ${total} registro(s) salvo(s) com sucesso!<br>
            <div><button onclick='div_alerta.style.display="none"'>OK</button></div>
        `), 500);
    } catch (e) {
        console.error("Erro ao salvar lote:", e);
        alerta(`Erro ao salvar registros. <button onclick='div_alerta.style.display="none"'>OK</button>`);
    }
}

