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
            text: 'Quedas por cidade'
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
            text: 'Relação quedas energia',
            left: 'center'
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
                    {value:  quedasTotais - energiaTotal},
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
            text: 'Disponibilidade por cidade (%) - ' + mes,
            left: 'center'
        },
        tooltip: {
            trigger: 'axis',
            formatter: '{b} : {c}%',
            axisPointer: {type: 'shadow'}
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
        legend: {
            data: [
                'Média IDM',
                'NÃO OK',
                'IDM OK',
                'IDM NÃO OK',
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
                    {value: porcentagem.toFixed(2), name: 'Média IDM'},
                    {value: (100 - porcentagem).toFixed(2), name: 'NÃO OK', itemStyle: {color: '#6d1f9a'}}
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
                    {value: contadorIDMOK, name: 'IDM OK', itemStyle: {color: '#279e00'}},
                    {value: cidades.length - contadorIDMOK, name: 'IDM NÃO OK', itemStyle: {color: '#a50000'}}
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
    var myChart = echarts.init(chartDom, 'dark');

    // --- 1. PROCESSAMENTO DOS DADOS ---
    // Decidimos se filtramos por 'mes' ou por 'dia' dependendo da seleção
    const campoFiltro = (mes !== "todos") ? 'dia' : 'mes';

    const dataComEnergia = meses.map(m =>
        quedasFiltradas.filter(q => q[campoFiltro] === m && q.energia === true).length
    );

    const dataSemEnergia = meses.map(m =>
        quedasFiltradas.filter(q => q[campoFiltro] === m && q.energia === false).length
    );

    const dataTotal = dataComEnergia.map((val, i) => val + dataSemEnergia[i]);
    const dataMediaDiaria = dataTotal.map(total => (total / 30).toFixed(2));

    // --- 2. CONFIGURAÇÃO DO ECHARTS ---
    var option = {
        title: {
            text: mes !== "todos" ? `Quedas em ${mes}` : 'Análise de Quedas por Mês',
            subtext: 'Passe o mouse para ver o Top 10 Cidades',
            left: 'center'
        },
        tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'cross' },
            // --- CUSTOMIZAÇÃO DO TOOLTIP (TOP 10 CIDADES) ---
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
                label: { show: true, position: 'inside', formatter: '{c}' }, // Label dentro
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
                        return dataTotal[p.dataIndex]; // Mostra o total calculado
                    },
                    textStyle: { color: '#fff', fontWeight: 'bold' }
                },
                data: dataSemEnergia
            },
            {
                name: 'Média Diária',
                type: 'line',
                yAxisIndex: 1,
                smooth: true,
                itemStyle: { color: '#fac858' },
                data: dataMediaDiaria
            }
        ]
    };

    option && myChart.setOption(option);
}

function graficoMapa() {
    var chart = echarts.init(document.getElementById('chart'));
    var dados = quedasFiltradas;

    const transformarParaMapa = (dados) => {
        const agrupado = {};
        dados.forEach(item => {
            if (!item.coordenada) return;

            if (!agrupado[item.nomeCidade]) {
                const partes = item.coordenada.split(',');
                agrupado[item.nomeCidade] = {
                    lng: parseFloat(partes[1]),
                    lat: parseFloat(partes[0]),
                    count: 0,
                    nome: item.nomeCidade
                };
            }
            agrupado[item.nomeCidade].count += 1;
        });

        return Object.values(agrupado).map(c => [c.lng, c.lat, c.count, c.nome]);
    };

    const dadosFormatados = transformarParaMapa(dados);

    // --- CÁLCULO DO MÁXIMO DINÂMICO ---
    // Pegamos todos os counts e achamos o maior valor presente nos dados atuais
    const apenasCounts = dadosFormatados.map(d => d[2]);
    const maxLocal = apenasCounts.length > 0 ? Math.max(...apenasCounts) : 10;

    // Se o máximo for muito baixo (ex: 1), definimos um piso para o gráfico não ficar estranho
    const valorMaximoGrafico = maxLocal > 0 ? maxLocal : 10;

    var option = {
        leaflet: {
            center: [-53, -30.2317],
            zoom: 7,
            roam: true,
            tiles: [{
                urlTemplate: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'
            }]
        },
        tooltip: {
            trigger: 'item',
            formatter: function (params) {
                return `<b>${params.data[3]}</b><br/>Quedas: ${params.data[2]}`;
            }
        },
        visualMap: {
            type: 'continuous',
            min: 0,
            max: valorMaximoGrafico, // <--- AGORA É DINÂMICO
            dimension: 2,
            calculable: true,
            realtime: false, // Melhora performance ao arrastar
            inRange: {
                symbolSize: [10, 60], // Aumentei um pouco o máximo para destacar bem
                color: ['#67ff00', '#ffa500', '#ff0000']
            },
            textStyle: {
                color: '#fff'
            }
        },
        series: [
            {
                type: 'scatter',
                coordinateSystem: 'leaflet',
                data: dadosFormatados,
                encode: {
                    value: 2 // Indica explicitamente que o valor visual vem do count (index 2)
                },
                itemStyle: {
                    opacity: 0.8,
                    shadowBlur: 10,
                    shadowColor: 'rgba(0, 0, 0, 0.5)'
                }
            }
        ]
    };
    chart.setOption(option);
}

function atualizarNome(nome) {
    nomeFiltro = nome;
    mes = "todos"
    filtrarQuedas()
    atualizarGraficoIndisponibilidade()
    graficoAlertasCidades()
    graficoRelacaoQuedasEnergia()
    graficoDeQuedasMesDia()
    graficoMapa()
}

let mesAnterior = 'x';
function atualizarMes(mesNovo) {
    mes = mesNovo
    if (mesAnterior !== mesNovo && mesAnterior !== 'x') {
        document.getElementById(mesAnterior).style.backgroundColor = "#11bb79";
    }
    filtrarQuedas()
    mesAnterior = mesNovo;
    document.getElementById(mesNovo).style.backgroundColor = "#279c00";
    atualizarGraficoIndisponibilidade();
    graficoAlertasCidades()
    graficoRelacaoQuedasEnergia()
    graficoDeQuedasMesDia()
    graficoMapa()
}
