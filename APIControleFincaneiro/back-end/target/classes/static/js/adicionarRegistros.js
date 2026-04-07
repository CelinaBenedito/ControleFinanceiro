div_alerta.style.display = 'none';
gestaoConta.style.display = "none";

let dataGasto;

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
    select_tipo.innerHTML = "<option value='#'>Escolha um tipo</option>"
    fetch("/registros/gerarTipos", {
        method: "GET"
    }).then(res => {
        res.json().then(json => {
            for (let c = 0; json.length > c; c++) {

                select_tipo.innerHTML +=
                    `
                            <option value="${json[c].id}">${json[c].titulo}</option>
                        `
            }
        })
    })
}

async function gerarInstituicao() {
    gestaoInstituicao = document.getElementById("gestaoInstituicao")
    select_instituicao.innerHTML = "<option value='#'> Escolha uma instituição</option>"
    fetch("/registros/gerarInstituicoes", {
        method: "GET"
    }).then(res => {
        res.json().then(json => {
            for (let c = 0; json.length > c; c++) {
                select_instituicao.innerHTML +=
                    `
                        <option value="${json[c].id}">${json[c].nome}</option>
                    `

                gestaoInstituicao.innerHTML += 
                `
                 <option onclick="controleInstituicao()">${json[c].nome}</option>
                `
            }
        })
    })
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

    fetch("/registros/registrar", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
            valorServer: valor,
            descServer: Desc,
            tipoServer: tipo,
            tituloServer: titulo,
            dataServer: data,
            instituicaoServer: instituicao
        }),
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
    fetch(`/registros/atualizarSaldo`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            valorServer: valor,
            instituicaoServer: instituicao
        }),
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
    var titulo = ipt_tituloTipo.value
    fetch("/registros/adicionarTipo", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
            tituloServer: titulo
        }),
    }).then(function (resposta) {
        console.log("Resposta: ", resposta);
        if (resposta.ok) {
            gerarTipos();
            adicioarTipo.close();
            return alerta(`Tipo adicionado com sucesso!<br>
                <button onclick="div_alerta.style.display='none'">OK</button>`)
        }
        else {
            return alerta(`Houve um erro ao adicionar tipo`, resposta)
        }
    })
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

    const res = await fetch(`/registros/buscarData/${dataSelecionada}`, {
        method: "GET"
    });

    const json = await res.json();
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
    if (tela == "gestaoConta") {
        gestaoConta.style.display = "";
        addGasto.style.display = "none";
    } else {
        gestaoConta.style.display = "none";
        addGasto.style.display = "";
    }

}


