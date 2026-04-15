let nomeFiltro = "";
let mes = 'todos';
let quedas;
let dados;
//variaveis fixas
let quedasProcessadas = [];
let mesesUnicos = [];
let cidades = [];

//variaveis usadas nos filtros
let quedasFiltradas = [];
let meses = [];
let diasUnicos = [];
let cidadesOrdenadasObj = [];


window.onload = () => {
    console.log("Iniciando processo dos gráficos");
    processarQuedas()
    filtrarQuedas()
    console.log("carregando gráficos")
    graficoMapa();
    atualizarGraficoIndisponibilidade()
    graficoAlertasCidades();
    graficoRelacaoQuedasEnergia();
    graficoDeQuedasMesDia();
    fazDropCidades();
    console.log(cidades);


}
function processarQuedas(){
    console.log("\n\n======Processando quedas ======");
    //variaveis para mapear os meses e cidades da lista de quedas.
    //Devemos mapear os dados que nos interessam das quedas
    quedasProcessadas = quedas.map(a => {
        const data = new Date(a.data);
        const mes = data.toLocaleString('en-US', {month: 'short'}).toUpperCase();

        //mapeando meses e cidades existentes na lista de quedas
        if (!mesesUnicos.includes(mes)) {
            mesesUnicos.push(mes);
        }
        if (!cidades.includes(a.nomeCidade)) {
            cidades.push(a.nomeCidade);
        }

        const dia = data.getDate();
        return {mes, nomeCidade: a.nomeCidade, dia, energia: a.faltaDeLuz, coordenada : a.coordenadas};
    });
    console.log("Meses encontrados" ,mesesUnicos);
    console.log("Cidades com quedas encontradas", cidades);
    console.log("Quedas encontrados", quedasProcessadas);
    console.log("======Processando quedas finalizado ======\n\n");

    //as quedas processadas não vao mudar então passamos para o filtradas
    quedasFiltradas = quedasProcessadas;
}

function filtrarQuedas(){
    console.log("\n\n======Filtrando quedas ======");
    //filtramos por nome
    let filtradasNome = nomeFiltro !== ""
        ? quedasProcessadas.filter(a => a.nomeCidade.toUpperCase().includes(nomeFiltro.toUpperCase()))
        : quedasProcessadas;
    //filtramos por mes da queda
    if(mes !== "todos"){
        const mesReduzido = mes.slice(0, 3);
        const quedasDoMes = filtradasNome.filter(a => a.mes === mesReduzido);
        diasUnicos = [...new Set(quedasDoMes.map(a => a.dia))].sort((a, b) => a - b);
        meses = diasUnicos;
        totaisDeQuedasPorMes = diasUnicos.map(dia =>
            quedasDoMes.filter(a => a.dia === dia).length
        );
        quedasFiltradas = quedasDoMes
    }else{
        quedasFiltradas = filtradasNome;
        meses = mesesUnicos;


    }
    //agrupamos quedas e suas respectivas cidades gerando objeto final com a cidade, quantidade de quedas e quantidade de quedas por energia
    cidadesOrdenadasObj = cidades.map(cidade => ({
        cidade,
        total: quedasFiltradas.filter(a =>
            a.nomeCidade === cidade &&
            (mes === "todos" || a.mes === mes.slice(0, 3))
        ).length,
        energia: quedasFiltradas.filter(a => a.nomeCidade === cidade && a.energia).length,
    })).sort((a, b) => b.total - a.total);
    console.log("Meses encontrado", meses);
    console.log("Quedas filtradas", quedasFiltradas);
    console.log("======Filtrando quedas finalizado======\n\n\n");
}

function graficoAlertasCidades(){
    console.log("\n\n====== Gerando grafico alertas em cidades ======\n\n");
    //separamos com base nos dados do objeto
    const cidadesOrdenadas = cidadesOrdenadasObj.map(obj => obj.cidade);
    const totaisOrdenados = cidadesOrdenadasObj.map(obj => obj.total);
    const totaisEnergia = cidadesOrdenadasObj.map(obj => obj.energia);

    //definindo a div  e iniciando gráfico
    var divGrafico = document.getElementById("chart-quedasCidades");
    var grafico = echarts.init(divGrafico);

    var option = {
        title: {
            text: 'Quedas por Cidade'
        },
        textStyle: {
            fontFamily: 'Nunito, Microsoft YaHei, sans-serif'
        },
        tooltip: {
            trigger: 'axis',
            axisPointer: {
                type: 'shadow'
            }
        },
        legend: {},
        xAxis: {
            type: 'value',
            boundaryGap: [0, 0.01]
        },
        yAxis: {
            type: 'category',
            data: cidadesOrdenadas,
        },
        dataZoom: [
            {
                type: 'slider',
                yAxisIndex: 0,
                start: 8,
                end: 0
            },
            {
                type: 'inside',
                yAxisIndex: 0,
                start: 0,
                end: 20
            }

        ],
        series: [
            {
                name: 'Total',
                type: 'bar',
                label: {
                    show: true,
                },
                data: totaisOrdenados
            },
            {
                name: 'Energia',
                type: 'bar',
                color: '#f6652c',
                label: {
                    show: true,
                    formatter: function (params) {
                        return params.value == 0 ? '' : params.value;
                    },
                    color: 'white'
                },
                data: totaisEnergia
            }
        ]
    };
    grafico.clear();
    grafico.setOption(option);


}

function graficoRelacaoQuedasEnergia(){
    console.log("\n\n====== Gerando grafico relacao entre energia======\n\n");
    const quedasTotais = quedasFiltradas.length;
    const energiaTotal = quedasFiltradas.filter(a => a.energia).length
    console.log("Quedas encontrados", quedasTotais);
    console.log("Quedas de energia encontrados", energiaTotal);

    let optionEnergia = {
        color: ['#FF4500', '#1e840e'],
        title: {
            text: 'Relação Quedas por Energia',
            left: 'center'
        },
        textStyle: {
            fontFamily: 'Nunito, Microsoft YaHei, sans-serif'
        },
        tooltip: {
            trigger: 'item',
            formatter: '{b}: {c} ({d}%)'
        },
        legend: {
            top: '5%',
            left: 'center'
        },
        series: [
            {
                name: 'Acesso',
                type: 'pie',
                radius: ['40%', '70%'],
                center: ['50%', '75%'], // Ajustado levemente para baixo para caber o texto superior
                startAngle: 180,
                endAngle: 360,
                avoidLabelOverlap: false,
                label: {
                    show: true,
                    position: 'inside',
                    formatter: '{b}\n{d}%',
                    fontSize: 15,       // Tamanho do texto maior
                    fontWeight: 'bold', // Negrito              color: '#fff'       // Cor branca para destacar sobre o colorido
                },
                data: [
                    {value: energiaTotal, name: 'Energia'},
                    {value:  quedasTotais - energiaTotal, name: 'Outros'},
                ]
            }
        ]
    };
    const chartDomEnergia = document.getElementById('chart-quedasCidadesEnergia');
    const myChartEnergia = echarts.init(chartDomEnergia)
    myChartEnergia.setOption(optionEnergia);

}

function atualizarGraficoIndisponibilidade() {
    let dadosMes = [];

    const mesBusca = (!mes || mes === "undefined" || mes === "") ? "todos" : mes;

    if (mesBusca === "todos") {
        const mapa = new Map();
        dados.forEach(mesData => {
            mesData.disponibilidades.forEach(d => {
                if (d.nome?.trim()) {
                    const info = mapa.get(d.nome) || {soma: 0, count: 0};
                    info.soma += d.disponibilidade;
                    info.count++;
                    mapa.set(d.nome, info);
                }
            });
        });

        dadosMes = Array.from(mapa, ([nome, {soma, count}]) => ({
            nome,
            disponibilidade: soma / count
        })).sort((a, b) => a.disponibilidade - b.disponibilidade);
    } else {
        const mesEncontrado = dados.find(item => item.month === mesBusca);
        dadosMes = mesEncontrado
            ? mesEncontrado.disponibilidades
                .filter(d => d.nome?.trim())
                .sort((a, b) => a.disponibilidade - b.disponibilidade)
            : [];
    }
    const filtrados = nomeFiltro && nomeFiltro.trim() !== ""
        ? dadosMes.filter(d => d.nome.toUpperCase().includes(nomeFiltro.toUpperCase()))
        : dadosMes;

    // --- Dados finais para o gráfico ---
    const cidades = filtrados.map(d => d.nome);
    const valores = filtrados.map(d => d.disponibilidade.toFixed(2));

    const chartIndisp = echarts.init(document.getElementById('chart-indisponibilidade'));
    var option = {
        title: {
            text: 'Disponibilidade por Cidade (%) - ' + mes,
            left: 'center'
        },
        textStyle: {
            fontFamily: 'Nunito, Microsoft YaHei, sans-serif'
        },
        tooltip: {
            trigger: 'axis',
            formatter: '{b} : {c}%',
            axisPointer: {type: 'shadow'},
            textStyle: {
                fontWeight: 'bolder',
                color: '#222831'
            }
        },
        grid: {
            left: '5%',
            right: '5%',
            bottom: '2%',
            containLabel: true
        },
        xAxis: {
            type: 'value',
            max: 100
        },
        yAxis: {
            type: 'category',
            data: cidades,
            axisLabel: {
                fontSize: 15,
                formatter: function (value) {
                    return value.length > 30 ? value.slice(0, 27) + '...' : value;
                }
            },
        },
        dataZoom: [
            {type: 'slider', yAxisIndex: 0, start: 10, end: 0},
            {type: 'inside', yAxisIndex: 0, start: 0, end: 20}
        ],
        series: [{
            name: 'Indisponibilidade',
            type: 'bar',
            data: valores,
            itemStyle: {
                color: function (params) {
                    return params.value < 99.4 ? '#FA7676' : '#76A7FA'; // por exemplo, >10% de indisponibilidade
                },
                shadowColor: 'rgba(0, 0, 0, 0.5)',
                shadowBlur: 0.5
            },
            label: {show: true, formatter: '{c}%'}
        }]
    };
    chartIndisp.setOption(option);

    var port = document.getElementById('chart-porcentagem');
    var chartPorcentagem = echarts.init(port);
    var porcentagem = 0.0;
    for (let i = 0; i < valores.length; i++) {
        porcentagem += parseFloat(valores[i]);
    }
    porcentagem = porcentagem / valores.length;
    var contadorIDMOK = 0;
    contadorIDMOK = valores.filter(item => item > 99.4).length;
    port2 = {
        tooltip: {
            trigger: 'item',
            formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        textStyle: {
            fontFamily: 'Nunito, Microsoft YaHei, sans-serif'
        },
        legend: {
            data: [
                'Média OK',
                'Média NOK',
                'OK',
                'NOK',
            ]
        },
        series: [
            {
                name: 'MÉDIA IDM, DISPONIBILIDADE',
                type: 'pie',
                selectedMode: 'single',
                radius: [0, '30%'],
                label: {
                    formatter: '{b} {c}%',
                    position: 'inner',
                    fontSize: 14,
                    fontWeight: 'bold'
                },
                labelLine: {
                    show: false
                },
                data: [
                    {value: porcentagem.toFixed(2), name: 'Média OK'},
                    {value: (100 - porcentagem).toFixed(2), name: 'Média NOK', itemStyle: {color: '#6d1f9a'}}
                ],
            },
            {
                name: 'IDM',
                type: 'pie',
                radius: ['50%', '70%'],
                labelLine: {
                    length: 30
                },
                label: {
                    formatter: '{a|{a}}{abg|}\n{hr|}\n  {b|{b}：}{c}  {per|{d}%}  ',
                    backgroundColor: '#F6F8FC',
                    borderColor: '#8C8D8E',
                    fontSize: 25,
                    rich: {
                        a: {
                            color: '#6E7079',
                            lineHeight: 22,
                            align: 'center'
                        },
                        hr: {
                            borderColor: '#8C8D8E',
                            width: '100%',
                            borderWidth: 1,
                            height: 0
                        },
                        b: {
                            color: '#4C5058',
                            fontSize: 14,
                            fontWeight: 'bold',
                            lineHeight: 33
                        },
                        per: {
                            color: '#fff',
                            backgroundColor: '#4C5058',
                            padding: [3, 4],
                            borderRadius: 4
                        }
                    }
                },
                data: [
                    {value: contadorIDMOK, name: 'OK', itemStyle: {color: '#279e00'}, label:{fontSize: 12}},
                    {value: cidades.length - contadorIDMOK, name: 'NOK', itemStyle: {color: '#a50000'}, label:{fontSize: 12}}
                ]
            }
        ]
    };

    chartPorcentagem.setOption(port2);
    window.addEventListener('resize', chartIndisp.resize);
    window.addEventListener('resize', chartPorcentagem.resize);

}

function graficoDeQuedasMesDia() {
    console.log("\n\n\n ===== GRAFICO DE QUEDAS MES E DIA INICIALIZANDO =====");
    var chartDom = document.getElementById('chart-quedas');
    var myChart = echarts.init(chartDom);
    const campoFiltro = (mes !== "todos") ? 'dia' : 'mes';
    const dataComEnergia = meses.map(m =>
        quedasFiltradas.filter(q => q[campoFiltro] === m && q.energia === true).length
    );

    const dataSemEnergia = meses.map(m =>
        quedasFiltradas.filter(q => q[campoFiltro] === m && q.energia === false).length
    );


    const dataTotal = meses.map(m =>
        quedasFiltradas.filter(q => q[campoFiltro] === m).length
    );
    const cidadesCount = meses.map(m => {
        const registrosNoPeriodo = quedasFiltradas.filter(q => q[campoFiltro] === m);
        const nomesCidades = registrosNoPeriodo.map(q => q.nomeCidade);
        return new Set(nomesCidades).size;
    });

    const anoAtual = new Date().getFullYear();
    const diasArray = [];
    for (let i = 1; i < meses.length +1; i++) {
        diasArray.push(new Date(anoAtual, i,0).getDate());
    }
    const dataMediaPorCidade = dataTotal.map((total, i) => {
        const qtdCidades =(mes !== "todos") ? cidadesCount[i]:diasArray[i];
        if (qtdCidades === 0) return "0.00";
        return (total / qtdCidades).toFixed(2);
    });

    var option = {
        title: {
            text: mes !== "todos" ? `Quedas em ${mes}` : 'Análise de Quedas por Mês',
            subtext: 'Passe o mouse para ver o Top 10 Cidades',
            left: 'center'
        },
        textStyle: {
            fontFamily: 'Nunito, Microsoft YaHei, sans-serif'
        },
        tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'cross' },
            formatter: function (params) {
                let eixoX = params[0].name; // Nome do Mês ou Dia

                // Filtra as quedas apenas deste mês/dia específico
                let quedasNoPeriodo = quedasFiltradas.filter(q => q[campoFiltro].toString() === eixoX.toString());

                // Conta ocorrências por cidade
                let contagemCidades = {};
                quedasNoPeriodo.forEach(q => {
                    contagemCidades[q.nomeCidade] = (contagemCidades[q.nomeCidade] || 0) + 1;
                });

                // Transforma em array e ordena (Top 10)
                let topCidades = Object.entries(contagemCidades)
                    .sort((a, b) => b[1] - a[1])
                    .slice(0, 10);

                // Monta o HTML do tooltip
                let res = `<b>${mes !== "todos" ? 'Dia' : 'Mês'}: ${eixoX}</b><br/>`;
                params.forEach(item => {
                    if(item.seriesName !== 'Média Diária') {
                        res += `${item.marker} ${item.seriesName}: ${item.value}<br/>`;
                    }
                });

                res += `<br/><b>Top 10 Cidades:</b><br/>`;
                topCidades.forEach(([cidade, qtd]) => {
                    res += `${cidade}: ${qtd}<br/>`;
                });

                return res;
            }
        },
        legend: { data: ['Com Energia', 'Sem Energia', 'Média Diária'], bottom: 0 },
        xAxis: [{ type: 'category', data: meses, axisPointer: { type: 'shadow' } }],
        yAxis: [
            { type: 'value', name: 'Qtd.', axisLabel: { formatter: '{value} un' } },
            { type: 'value', name: 'Média', position: 'right' }
        ],
        series: [
            {
                name: 'Com Energia',
                type: 'bar',
                stack: 'total',
                barWidth: '50%',
                itemStyle: { color: '#91cc75' },
                label: {
                    show: true,
                    position: 'inside',
                    formatter: function (p){
                        console.log(p.data)
                        return p.data == 0 ? "" : p.data
                    }
                }, // Label dentro
                data: dataComEnergia
            },
            {
                name: 'Sem Energia',
                type: 'bar',
                stack: 'total',
                itemStyle: { color: '#ee6666' },
                // Label total em cima da barra (usamos a última série do stack para isso)
                label: {
                    show: true,
                    position: 'top',
                    formatter: function(p) {
                        return dataTotal[p.dataIndex] == 0 ? '' : dataTotal[p.dataIndex]; // Mostra o total calculado
                    },
                    textStyle: { fontWeight: 'bold' }
                },
                data: dataSemEnergia
            },
            {
                name: 'Média Diária',
                type: 'line',
                smooth: true,
                itemStyle: { color: '#5070dd', borderColor: '#ccc'},
                label:{
                    show: true,
                    position: mes == "todos" ? 'top' : 'bottom'
                },
                data: dataMediaPorCidade
            }
        ]
    };

    option && myChart.setOption(option);
}

function graficoMapa() {
    var chart = echarts.init(document.getElementById('chart'));

    // --- ESTADO ---
    var filtroEnergia = 'todos';
    var tipoVisualizacao = 'scatter';

    // --- PAINEL DE CONTROLES ---
    const chartEl = document.getElementById('chart');
    let controles = document.getElementById('mapa-controles');

    // Se já existir, removemos para evitar duplicação em atualizações sem recarregar a página
    if (controles) {
        controles.remove();
    }

    controles = document.createElement('div');
    controles.id = 'mapa-controles';
    controles.style.cssText = `
        display:flex;gap:8px;align-items:center;flex-wrap:wrap;
        padding:8px 12px;background:rgb(34 40 49);
        border-bottom:1px solid #2a2a2a;font-family:sans-serif;
    `;
    controles.innerHTML = `
        <span style="color:#666;font-size:11px;font-weight:700;letter-spacing:1.5px;text-transform:uppercase;">Energia</span>
        <button id="btn-todos" class="map-btn" style="padding:4px 12px;border-radius:20px;font-size:12px;cursor:pointer;">Todos</button>
        <button id="btn-com" class="map-btn" style="padding:4px 12px;border-radius:20px;font-size:12px;cursor:pointer;">Com energia</button>
        <button id="btn-sem" class="map-btn" style="padding:4px 12px;border-radius:20px;font-size:12px;cursor:pointer;">Sem energia</button>
        <div style="width:1px;height:20px;background:#2a2a2a;margin:0 4px;"></div>
        <span style="color:#666;font-size:11px;font-weight:700;letter-spacing:1.5px;text-transform:uppercase;">Modo</span>
        <button id="btn-bolhas" class="map-btn" style="padding:4px 12px;border-radius:20px;font-size:12px;cursor:pointer;">Bolhas</button>
        <button id="btn-calor" class="map-btn" style="padding:4px 12px;border-radius:20px;font-size:12px;cursor:pointer;">Calor</button>
        <span id="mapa-total" style="margin-left:auto;color:#666;font-size:12px;"></span>
    `;
    chartEl.parentNode.insertBefore(controles, chartEl);

    // --- LÓGICA DE RESET E ESTILO DOS BOTÕES ---
    const aplicarEstiloBotao = (btn, ativo) => {
        if (ativo) {
            btn.style.background = '#3a3a3a';
            btn.style.color = '#fff';
            btn.style.borderColor = '#555';
            btn.style.borderStyle = 'solid';
        } else {
            btn.style.background = 'transparent';
            btn.style.color = '#888';
            btn.style.borderColor = '#444';
            btn.style.borderStyle = 'solid';
        }
    };

    const atualizarBotoesUI = () => {
        // Reset de Energia
        aplicarEstiloBotao(document.getElementById('btn-todos'), filtroEnergia === 'todos');
        aplicarEstiloBotao(document.getElementById('btn-com'), filtroEnergia === 'com');
        aplicarEstiloBotao(document.getElementById('btn-sem'), filtroEnergia === 'sem');

        // Reset de Modo
        aplicarEstiloBotao(document.getElementById('btn-bolhas'), tipoVisualizacao === 'scatter');
        aplicarEstiloBotao(document.getElementById('btn-calor'), tipoVisualizacao === 'heatmap');
    };

    // Listeners
    document.getElementById('btn-todos').onclick = () => { filtroEnergia = 'todos'; renderizar(); };
    document.getElementById('btn-com').onclick   = () => { filtroEnergia = 'com';   renderizar(); };
    document.getElementById('btn-sem').onclick   = () => { filtroEnergia = 'sem';   renderizar(); };
    document.getElementById('btn-bolhas').onclick = () => { tipoVisualizacao = 'scatter'; renderizar(); };
    document.getElementById('btn-calor').onclick  = () => { tipoVisualizacao = 'heatmap'; renderizar(); };

    // --- PROCESSAMENTO DE DADOS ---
    const agrupar = (dados) => {
        const mapa = {};
        dados.forEach(item => {
            if (!item.coordenada) return;
            if (!mapa[item.nomeCidade]) {
                const [lat, lng] = item.coordenada.split(',').map(Number);
                mapa[item.nomeCidade] = {
                    nome: item.nomeCidade.replace(/_/g, ' '),
                    lat, lng, total: 0, com: 0, sem: 0
                };
            }
            mapa[item.nomeCidade].total++;
            item.energia ? mapa[item.nomeCidade].com++ : mapa[item.nomeCidade].sem++;
        });
        return Object.values(mapa);
    };

    // --- FORMATAÇÃO DO TOOLTIP (Recuperado e Padronizado) ---
    const getTooltipFormatter = (params) => {
        const c = params.data.extra;
        if (!c) return '';
        const pct = c.total > 0 ? Math.round((c.sem / c.total) * 100) : 0;
        const countExibido = params.data.value[2];

        return `
            <div style="font-family:sans-serif;min-width:180px;color:#ddd;">
                <b style="font-size:13px;color:#fff;">${c.nome}</b>
                <hr style="margin:4px 0;border:none;border-top:1px solid #444;"/>
                <div style="margin-bottom:2px;">Quedas (filtro): <b style="color:#fff;font-size:13px;">${countExibido}</b></div>
                <div style="font-size:11px;color:#aaa;">Total geral: ${c.total}</div>
                <div style="font-size:11px;color:#aaa;">Com energia: ${c.com} &nbsp;|&nbsp; Sem: ${c.sem}</div>
                <div style="margin-top:6px;background:#222;border-radius:3px;height:6px;overflow:hidden;">
                    <div style="background:#ff4444;border-radius:3px;height:6px;width:${pct}%;"></div>
                </div>
                <div style="font-size:10px;color:#F44;text-align:right;margin-top:2px;">${pct}% sem energia</div>
            </div>
        `;
    };

    const renderizar = () => {
        atualizarBotoesUI();
        const todasCidades = agrupar(quedasFiltradas);

        const cidadesFiltradas = todasCidades
            .map(c => {
                let count = filtroEnergia === 'com' ? c.com : (filtroEnergia === 'sem' ? c.sem : c.total);
                return { ...c, count };
            })
            .filter(c => c.count > 0);

        const maxCount = cidadesFiltradas.length > 0 ? Math.max(...cidadesFiltradas.map(c => c.count)) : 1;

        const el = document.getElementById('mapa-total');
        if (el) el.textContent = `${cidadesFiltradas.reduce((s,c) => s+c.count, 0)} quedas em ${cidadesFiltradas.length} cidades`;

        let series = [];
        let visualMap = {
            show: true,
            min: 0,
            max: maxCount,
            calculable: true,
            inRange: { color: ['#50c850', '#ffb300', '#dc2626'] },
            textStyle: { color: '#aaa' },
            left: 10,
            bottom: 40
        };

        if (tipoVisualizacao === 'heatmap') {
            // HEATMAP REAL do ECharts
            series = [
                {
                    type: 'heatmap',
                    coordinateSystem: 'leaflet',
                    data: cidadesFiltradas.map(c => [c.lng, c.lat, c.count]),
                    pointSize: 20, // Ajuste conforme a densidade desejada
                    blurSize: 30
                },
                {
                    // Pontos de interação no topo do heatmap
                    type: 'scatter',
                    coordinateSystem: 'leaflet',
                    data: cidadesFiltradas.map(c => ({
                        value: [c.lng, c.lat, c.count],
                        extra: c
                    })),
                    symbolSize: 5,
                    itemStyle: { opacity: 0 }, // Invisível, apenas para o tooltip
                    tooltip: {
                        trigger: 'item',
                        formatter: getTooltipFormatter
                    }
                }
            ];
        } else {
            // MODO SCATTER (BOLHAS)
            series = [{
                type: 'effectScatter',
                coordinateSystem: 'leaflet',
                data: cidadesFiltradas.map(c => ({
                    value: [c.lng, c.lat, c.count],
                    extra: c,
                    symbolSize: 10 + (c.count / maxCount) * 40
                })),
                rippleEffect: { brushType: 'stroke', scale: 3 },
                tooltip: {
                    trigger: 'item',
                    formatter: getTooltipFormatter
                }
            }];
        }

        chart.setOption({
            // ADICIONE ISSO AQUI:
            tooltip: {
                show: true,
                trigger: 'item',
                backgroundColor: 'rgba(15,15,20,0.9)',
                borderColor: '#333',
                borderWidth: 1,
                padding: 0 // Para que seu HTML customizado preencha tudo
            },
            leaflet: {
                center: [-53, -30.2317],
                zoom: 7,
                roam: true,
                tiles: [{ urlTemplate: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png' }]
            },
            visualMap,
            series
        }, true);
    };

    renderizar();
    window.addEventListener('resize', () => chart.resize());
}

function atualizarNome(nome) {
    nomeFiltro = nome;
    mes = "todos"
    atualizarTudo();
}

let mesAnterior = 'x';
function atualizarMes(mesNovo) {
    mes = mesNovo
    if (mesAnterior !== mesNovo && mesAnterior !== 'x') {
        document.getElementById(mesAnterior).style.backgroundColor = "#11bb79";
    }
    mesAnterior = mesNovo;
    document.getElementById(mesNovo).style.backgroundColor = "#279c00";
    atualizarTudo();
}

function atualizarTudo(){
    filtrarQuedas()
    atualizarGraficoIndisponibilidade();
    graficoAlertasCidades()
    graficoRelacaoQuedasEnergia()
    graficoDeQuedasMesDia()
    graficoMapa()
}

