const semDados =
  `
        <div class="aviso">
        <h1>Sem Dados</h1>
        <i class='bx  bx-alert-triangle'></i> 
        <p>Selecionar outra data para visualizar o gráfico<p>
        </div>
`
function atualizarDados(periodo) {
  gastosMes(periodo)
  buscarGastoTotal(periodo)
  buscarMaiorGasto(periodo)
  gerarTipos(periodo)
  percentualTipo(periodo)
}

function tempo() {
  console.log("Entrei na função tempo")
  valorSelect = document.getElementById('select_tempo').value
  console.log(valorSelect)
  const hoje = new Date();

  if (valorSelect == "ano") {
    atualizarDados(hoje.getFullYear())
    div_ano = document.getElementById('div_ano');
    visualizaçãoPeriodo.style.display = "none"

    div_ano.innerHTML =
      `
      <select id="anoAnalisado" onchange="periodoAnual()">
        <option value="#">Selecionar ano</option>
        <option value="2025">2025</option>
        <option value="2026">2026</option>
      </select>
      <label>Selecionar o ano</label>
    `
  }
  else if (valorSelect == "mes") {
    const hoje = new Date();
    ano = hoje.getFullYear();
    mes = hoje.getMonth() + 1;
    tempoAnalisado = { ano, mes }
    visualizaçãoPeriodo.style.display = ""
    atualizarDados(tempoAnalisado)

    div_ano.innerHTML =
      `
       <select id="anoAnalisado" onchange="periodo()">
          <option value="#">Selecionar ano</option>
          <option value="2025">2025</option>
          <option value="2026">2026</option>
      </select>
      <label>Selecionar o ano</label>
    `
  }
}

function periodoAnual() {
  console.log("Entrei na função periodo")
  valorSelectAno = document.getElementById('anoAnalisado').value
  ano = Number(valorSelectAno);
  atualizarDados(ano)
}

function periodo() {
  console.log("Entrei na função periodo")
  valorSelectMes = document.getElementById('select_periodo').value
  valorSelectAno = document.getElementById('anoAnalisado').value

  const hoje = new Date();
  ano = valorSelectAno;
  mes = valorSelectMes;
  tempoAnalisado = { ano, mes }
  atualizarDados(tempoAnalisado)
}

function gerarInformações() {
  console.log("Gerando informações iniciais")
  const hoje = new Date();
  ano = hoje.getFullYear();
  mes = hoje.getMonth() + 1
  tempoAnalisado = { ano, mes }
  gerarKPIS(tempoAnalisado).then(() => gerarGraficos(tempoAnalisado))
}

async function gerarKPIS(periodo) {
  console.log("Iniciando geração de KPI's")

  await Promise.all([
    buscarMaiorGasto(periodo),
    buscarGastoTotal(periodo),
    percentualTipo(periodo)
  ]);
}

function gerarGraficos(periodo) {
  console.log("Iniciando geração de gráficos")
  gastosMes(periodo);
  gerarTipos(periodo);
  graficoComparacao([10, 20, 356, 13, 121], [50, 90, 246, 113, 1221], ["jan", "fev", "mar", "abr", "mai"]);
}
//KPI
function buscarMaiorGasto(periodo) {
  let url = '';

  if (typeof periodo === 'number') {
    console.log("Ano identificado")
    url = `/registros/maiorGastoAno/${periodo}`;
    kpiMaiorGastoTitulo.innerHTML = "ano"

    labelComparacao = `${periodo - 1}`;
  }
  else if (typeof periodo === 'object') {
    console.log("Mês identificado")
    url = `/registros/maiorGastoMes/${periodo.ano}/${periodo.mes}`;
    kpiMaiorGastoTitulo.innerHTML = "mês"

    let mesAnterior = periodo.mes - 1;
    let anoAnterior = periodo.ano;

    if (mesAnterior === 0) {
      mesAnterior = 12;
      anoAnterior--;
    }
    labelComparacao = `${nomeMes(mesAnterior)}`;
  }
  console.log("Iniciando fetch na kpi de maior categoria...")
  return fetch(url)
    .then(res => res.json())
    .then(json => {
      console.log("Dentro do fetch da maior categoria\nJson:", json[0])

      let categoria = document.getElementById('categoria');
      let valorCategoria = document.getElementById('valorCategoria');
      let tituloGasto = document.getElementById('tituloGasto')

      if (!json || json.length === 0) {
        categoria.innerHTML = "Sem dados";
        valorCategoria.innerHTML = "Sem dados";
        tituloGasto.innerHTML = "Categoria: Sem dados";
        return;
      } else {
        categoria.innerHTML = json[0].tituloGasto;
        valorCategoria.innerHTML = `R$ ${json[0].valor}`
        tituloGasto.innerHTML = `Categoria: ${json[0].tipo}`
      }


    })
}

//KPI
function buscarGastoTotal(periodo) {
  console.log("Entrei em buscarGastoTotal")
  let url = '';
  let div_titulo = document.getElementById('dataKPIgastoTotal');
  let labelComparacao = "";

  if (typeof periodo === 'number') {
    console.log("Ano identificado")
    url = `/registros/gastoTotalAno/${periodo}`;
    div_titulo.innerHTML = "ano"

    labelComparacao = `${periodo - 1}`;
  }
  else if (periodo && typeof periodo === 'object') {
    console.log("Mês identificado")
    url = `/registros/gastoTotalMes/${periodo.ano}/${periodo.mes}`;
    div_titulo.innerHTML = "mês"

    let mesAnterior = periodo.mes - 1;
    let anoAnterior = periodo.ano;

    if (mesAnterior === 0) {
      mesAnterior = 12;
      anoAnterior--;
    }

    labelComparacao = `${nomeMes(mesAnterior)}`;

  }

  console.log("Iniciando fetch...")
  fetch(url)
    .then(res => res.json())
    .then(json => {
      console.log("Dentro do fetch\nJson:", json[0])
      div_gastoTotal = document.getElementById('gastoTotal');
      div_percentual = document.getElementById('percentual');
      div_subtexto = document.getElementById('subtexto');

      div_gastoTotal.innerHTML = `R$ ${json[0].total_atual.toFixed(2)}`
      if (json[0].total_anterior === 0 && json[0].total_atual > 0) {
        console.log("Valor anterior nulo")
        div_percentual.style.display = "none"
        div_subtexto.innerHTML = "variação não aplicável"
      }
      else if (json[0].total_anterior === 0 && json[0].total_atual === 0) {
        console.log("Valor anterior e atual nulo")
        div_percentual.style.display = "none"
        div_subtexto.innerHTML = "variação não aplicável"
      }
      else {

        diferenca = (json[0].total_atual - json[0].total_anterior).toFixed(2);
        diferença_percentual = ((diferenca / json[0].total_anterior) * 100).toFixed(2);
        const root = document.documentElement;
        if (diferença_percentual > 0) {
          const corFundo = getComputedStyle(root).getPropertyValue('--red-700');
          const corTexto = getComputedStyle(root).getPropertyValue('--red-100');
          div_percentual.style.display = ""
          div_percentual.style.backgroundColor = corFundo;
          div_percentual.style.color = corTexto
          div_percentual.innerHTML = `<i class='bx  bx-caret-big-up'></i> +${diferença_percentual}%`
          div_subtexto.innerHTML = `Em relação a ${typeof periodo === 'object' ? 'mês de ' : 'ano de '} ${labelComparacao}`;

        }
        else if (diferença_percentual < 0) {
          const corFundo = getComputedStyle(root).getPropertyValue('--green-700');
          const corTexto = getComputedStyle(root).getPropertyValue('--green-100');
          div_percentual.style.display = ""
          div_percentual.style.backgroundColor = corFundo;
          div_percentual.style.color = corTexto

          div_percentual.innerHTML = `<i class='bx  bx-caret-big-down'></i> ${diferença_percentual}%`
          div_subtexto.innerHTML = `Em relação a ${typeof periodo === 'object' ? 'mês de ' : 'ano de '} ${labelComparacao}`;

        }

      }
    })
}

//GRÁFICO
function gerarTipos(periodo) {
  let url = ''
  let dados = [];
  let labels = [];

  // 📅 Ano
  if (typeof periodo === 'number') {
    url = `/registros/quantidadeTipoAno/${periodo}`;
  }
  if (typeof periodo === 'object') {
    url = `/registros/quantidadeTipoMes/${periodo.ano}/${periodo.mes}`;
  }

  fetch(url)
    .then(res => res.json())
    .then(json => {
      console.log("Dentro do fetch do gráfico de barra dos tipos")
      if (!json || json.length === 0) {
        graficoTipo.innerHTML = semDados
        return;
      }
      graficoTipo.innerHTML = ""

      for (let c = 0; json.length > c; c++) {
        dados.push(json[c].qtd);
        labels.push(json[c].titulo);
      }
      console.log("Dados do gráfico de barra dos tipos:", dados)

      criarGrafico(
        350,
        'bar',
        "Quantidade por categoria",
        dados,
        labels,
        'graficoTipo'
      );
    });
}

//GRÁFICO
async function gastosMes(param) {
  let url = '';
  let titulo = '';
  let tipoLabel = ''; // 'mes' ou 'dia'

  // 📅 Ano
  if (typeof param === 'number') {
    url = `/registros/gastosMes/${param}`;
    titulo = 'Gasto por mês';
    tipoLabel = 'mes';
  }
  // 📆 Mês
  else if (typeof param === 'object') {
    url = `/registros/gastosDia/${param.ano}/${param.mes}`;
    titulo = 'Gasto por dia';
    tipoLabel = 'dia';
  }

  fetch(url)
    .then(res => res.json())
    .then(json => {
      const dados = [];
      const labels = [];
      if (!json || json.length === 0) {
        graficoTemporal.innerHTML = semDados
        return;
      }
      graficoTemporal.innerHTML = ""
      json.forEach(item => {
        const valor = Math.round(item.total_gasto * 100) / 100;
        dados.push(valor);

        // Labels para ANO → meses
        if (tipoLabel === 'mes') {
          const nome = new Date(param, item.mes_num - 1)
            .toLocaleString('pt-BR', { month: 'long' });
          labels.push(nome.charAt(0).toUpperCase() + nome.slice(1));
        }

        // Labels para MÊS → dias
        if (tipoLabel === 'dia') {
          const [dia, mes, ano] = item.dia_label.split('/');

          const nomeMes = new Date(ano, mes - 1)
            .toLocaleString('pt-BR', { month: 'short' });

          labels.push(`${dia} de ${nomeMes}`);
        }
      });

      criarGrafico(
        350,
        'line',
        titulo,
        dados,
        labels,
        'graficoTemporal'
      );
    });
}

//KPI e GRÁFICO
function percentualTipo(periodo) {
  kpiTipo = document.getElementById('percentualKPItipo')
  valorKPI = document.getElementById('valorKPItipo')
  const dados = [];
  const labels = [];
// 📅 Ano
  if (typeof periodo === 'number') {
    console.log("Mês identificado no percentual por categoria")
    url = `/registros/percentualTipoAno/${periodo}`;
  }
  // 📅 Mês
  else if (periodo && typeof periodo === 'object') {
    console.log("Ano identificado no percentual por categoria")
    url = `/registros/percentualTipoMes/${periodo.ano}/${periodo.mes}`;
  }

  fetch(url)
    .then(res => res.json())
    .then(json => {

      if (!json || json.length === 0) {
        kpiTipo.innerHTML = "Sem dados";
        valorKPI.innerHTML = "Sem dados";
        graficoDonut.innerHTML = semDados
        return;
      }
      graficoDonut.innerHTML = ""
      let maiorTipo = json[0]
      for (let c = 0; json.length > c; c++) {
        dados.push(json[c].percentual);
        labels.push(json[c].tipo);
        if (json[c].percentual > maiorTipo.percentual) {
          maiorTipo = json[c];
        }
      }
      kpiTipo.innerHTML = `${maiorTipo.tipo}`;
      valorKPI.innerHTML = `${maiorTipo.percentual}%`

      opcaoDonut = {
        chart: {
          type: 'donut',
          height: 350
        },
        series: dados,
        labels: labels,
        fill: {
          colors: [
            '#1E4F56', // azul petróleo
            '#2C6E73', // teal mais escuro
            '#4F918C', // teal médio
            '#6FB9B5', // teal claro
            '#B5E3E0', // gelo suave
            '#C8ECF0', // azul glacial
            '#6FAFD6', // azul céu frio
            '#D8F3F5', // quase branco gelo
          ]
        },
        plotOptions: {
          pie: {
            customScale: 0.8
          }
        },
        responsive: [
          {
            breakpoint: 480,
            options: {
              chart: {
                width: '100%'
              }
            }
          }
        ]
      }


      var Donut = new ApexCharts(document.querySelector("#graficoDonut"), opcaoDonut)

      Donut.render();


    });

}
const graficos = {};


function criarGrafico(altura, tipo, nome, dados, labels, div) {
  const options = {
    chart: {
      height: altura,
      type: tipo
    },
    fill: {
      colors: [
        '#1E4F56', // azul petróleo
        '#2C6E73', // teal mais escuro
        '#4F918C', // teal médio
        '#6FB9B5', // teal claro
        '#B5E3E0', // gelo suave
        '#C8ECF0', // azul glacial
        '#6FAFD6', // azul céu frio
        '#D8F3F5', // quase branco gelo
      ]
    },
    series: [{
      name: nome,
      data: dados
    }],
    xaxis: {
      categories: labels
    },
    grid: {
      borderColor: 'rgba(54, 115, 115, 0.15)',
      strokeDashArray: 4
    },
  };

  if (graficos[div]) {
    graficos[div].destroy();
  }

  graficos[div] = new ApexCharts(
    document.querySelector(`#${div}`),
    options
  );

  graficos[div].render();
}

function graficoComparacao(periodoAtual, periodoAnterior, labels) {
  const options = {
    chart: {
      type: 'line',
      height: 350,
      toolbar: { show: false }
    },
    series: [
      {
        name: 'Período selecionado',
        data: periodoAtual
      },
      {
        name: 'Período anterior',
        data: periodoAnterior
      }
    ],
    xaxis: {
      categories: labels
    },
    stroke: {
      curve: 'smooth',
      width: 3
    },
    colors: ['#367373', '#B4D9D5'],
    grid: {
      borderColor: 'rgba(54,115,115,0.15)',
      strokeDashArray: 4
    },
    legend: {
      position: 'top'
    }
  };

  new ApexCharts(
    document.querySelector("#graficoComparacao"),
    options
  ).render();
}


function nomeMes(mes) {
  const meses = [
    "Janeiro", "Fevereiro", "Março", "Abril",
    "Maio", "Junho", "Julho", "Agosto",
    "Setembro", "Outubro", "Novembro", "Dezembro"
  ];

  return meses[mes - 1];
}
