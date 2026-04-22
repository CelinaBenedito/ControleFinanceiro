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

function gerarCategorias() {
    if (!userId) return;
    const selectCat = document.getElementById("select_categoria");
    const multiSelectCat = document.getElementById("multi_select_categoria");
    if (selectCat) selectCat.innerHTML = "<option value='#'>Escolha uma categoria</option>";
    if (multiSelectCat) multiSelectCat.innerHTML = "<option value='#'>Categoria</option>";
    MainAPI.getTipos(userId).then(json => {
        for (let c = 0; json.length > c; c++) {
            const opt = `<option value="${json[c].id}">${json[c].categoria.titulo}</option>`;
            if (selectCat) selectCat.innerHTML += opt;
            if (multiSelectCat) multiSelectCat.innerHTML += opt;
        }
    })
}

async function gerarInstituicao() {
    const selectInst = document.getElementById("select_instituicao");
    const multiSelectInst = document.getElementById("multi_select_instituicao");
    if (selectInst) selectInst.innerHTML = "<option value='#'> Escolha uma instituição</option>";
    if (multiSelectInst) multiSelectInst.innerHTML = "<option value='#'>Instituição</option>";
    MainAPI.getInstituicoes(userId).then(json => {
        for (let c = 0; json.length > c; c++) {
            const opt = `<option value="${json[c].id}">${json[c].intituicao.nome}</option>`;
            if (selectInst) selectInst.innerHTML += opt;
            if (multiSelectInst) multiSelectInst.innerHTML += opt;
        }
    })
}

async function controleInstituicao(){
    console.log("entrei no controle de instituição")
    escondido = document.getElementById("escondido")

    escondido.style.display = ""

}

function registrar() {
    var data = dataGasto;
    var valor = Number(document.getElementById('ipt_valor').value);
    var titulo = document.getElementById('ipt_nome').value;
    var tipo = document.getElementById('select_tipo').value;
    var categoria = document.getElementById('select_categoria').value;
    var Desc = document.getElementById('ipt_desc').value;
    var instituicao = document.getElementById('select_instituicao').value;
    var movimento = document.getElementById('select_movimento').value;

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
        instituicao: {
            instituicaoUsuario_id: Number(instituicao),
            tipoMovimento: movimento,
            valor: valor
        },
        detalhe: {
            categoriaUsuario_id: Number(categoria),
            tituloGasto: titulo
        }
    }).then((response) => {
        console.log("Resposta:", response);
        if (response.ok) {
            return setTimeout(() => alerta(
                `  
                        Registro realizado com sucesso!<br>
                        <div>

                        <button onclick='window.location.reload()'>
                            Continuar a registrar
                        </button>

                        </div>
                `, 0
            ), 1500);

        }
        else {
            alerta("Houve um erro ao registrar")
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
    confirmar.disabled = false;

    try {
        const listaGastos = await buscarGastosDia(data);

        console.log("tamanho lista gastos: ", listaGastos.length)
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
    } catch (e) {
        console.warn("Erro ao buscar gastos do dia:", e);
        gastosDia.innerHTML = "";
    }
}

confirmar.onclick = () => {
    modal.style.display = "none";
    dataP.style.display = ""
    dataFormatada = formatarDataBR(dataSelecionada)
    dataP.textContent = dataFormatada
    dataP.classList.remove("hidden");
    dataGasto = dataSelecionada;
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