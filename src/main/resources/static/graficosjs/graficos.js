let nomeFiltro = "";
let mes = 'todos';
let quedas;
let dados;

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


function graficoDeAlertasPorCidadeEMes() {
    //pega os dados que vieram com a template
    let meses = [];
    const cidades = [];
    // aqui ele vai processar as quedas, quebrando a data da queda para mes e dia, nome e se foi energia
    // vai verificar quais meses tem na lista também
    const quedasProcessadas = quedas.map(a => {
        const data = new Date(a.data);
        const mes = data.toLocaleString('en-US', {month: 'short'}).toUpperCase();
        if (!meses.includes(mes)) {
            meses.push(mes);
        }
        if (!cidades.includes(a.nomeCidade)) {
            cidades.push(a.nomeCidade);
        }
        const dia = data.getDate();
        return {mes, nomeCidade: a.nomeCidade, dia, energia: a.faltaDeLuz};
    });


    //vamos verificar se está sendo esperado filtro ou não
    const quedasFiltrados = nomeFiltro !== ""
        ? quedasProcessadas.filter(a => a.nomeCidade.toUpperCase().includes(nomeFiltro.toUpperCase()))
        : quedasProcessadas;

    let totaisDeQuedasPorMes;
    let totalEnergiaPorMes;
    let totalEnergiaGeral;


    if (mes === "todos") {
        totaisDeQuedasPorMes = meses.map(m =>
            quedasFiltrados.filter(a => a.mes === m).length
        );
        totalEnergiaPorMes = meses.map(m =>
            quedasFiltrados.filter(a => a.mes === m && a.energia).length
        );
    } else {
        const mesReduzido = mes.slice(0, 3);
        const quedasDoMes = quedasFiltrados.filter(a => a.mes === mesReduzido);
        const diasUnicos = [...new Set(quedasDoMes.map(a => a.dia))].sort((a, b) => a - b);
        meses = diasUnicos;
        totaisDeQuedasPorMes = diasUnicos.map(dia =>
            quedasDoMes.filter(a => a.dia === dia).length
        );
        totalEnergiaPorMes = diasUnicos.map(dia =>
            quedasDoMes.filter(a => a.dia === dia && a.energia).length
        );
    }

    totalEnergiaGeral = totalEnergiaPorMes.reduce((acc, curr) => acc + curr, 0);

    const series = cidades.map(cidade => {
        return {
            name: cidade,
            type: 'bar',
            stack: 'alertas',
            data: meses.map(m => {
                return mes === "todos" ?
                    quedasFiltrados.filter(a => a.nomeCidade === cidade && a.mes === m).length
                    :
                    quedasFiltrados.filter(a => a.nomeCidade === cidade && a.mes === mes.slice(0, 3) && a.dia === m).length
            }),
        };
    });

    series.push({
        name: 'Total',
        type: 'bar',
        stack: 'alertas',
        barWidth: '50%',

        label: {
            show: true,
            position: 'top',
            formatter: params => `${totaisDeQuedasPorMes[params.dataIndex]}`,
            fontWeight: 'bold'
        },
        data: meses.map(() => 0)
    });

    series.push({
        name: 'Quedas de Energia',
        type: 'bar',
        barWidth: '15%',

        stack: 'energia',
        itemStyle: {color: '#fd4500'},
        label: {
            show: true,
            position: 'top',
            formatter: params => `${[totalEnergiaPorMes[params.dataIndex]]}⚡`,
            fontWeight: 'bold'
        },
        data: totalEnergiaPorMes
    });

    const option = {
        title: {
            text: 'Alertas por Cidade e Mês',
            left: 'center'
        }, tooltip: {
            trigger: 'axis',
            axisPointer: {type: 'shadow'},
            formatter: function (params) {
                const filtered = params
                    .filter(p => p.value > 1 && p.seriesName !== 'Total')
                    .sort((a, b) => b.value - a.value)
                    .slice(0, 11);

                let result = `${params[0].axisValue}<br/>`;
                filtered.forEach(p => {
                    result += `${p.marker}${p.seriesName}: ${p.value}<br/>`;
                });
                result += `<b>Total: ${totaisDeQuedasPorMes[params[0].dataIndex]}</b>`;
                return result;
            }
        },
        visualMap: {
            type: 'piecewise', // Legenda por pedaços/faixas
            orient: 'horizontal',
            left: 'center',
            bottom: 0,
            seriesIndex: Array.from({length: cidades.length}, (_, i) => i + 1),
            pieces: [
                {gt: 8, label: '> 8 quedas', color: '#003333'}, // Vermelho escuro
                {gt: 4, lte: 8, label: '4-8 quedas', color: '#0077a6'},
                {gt: 2, lte: 4, label: '2-4 quedas', color: '#3195bc'},
                {gt: 0, lte: 2, label: '1-2 quedas', color: '#87c2c5'}
            ],
            outOfRange: {color: '#ccc'} // Cor para valor 0
        },
        xAxis: {
            type: 'category',
            data: meses,
            name: 'Mês'
        },
        yAxis: {
            type: 'value',
            name: 'Qtd. de quedas'
        },
        series: series
    };

    const chartDom = document.getElementById('chart-quedas');
    const myChart = echarts.init(chartDom);
    myChart.clear()
    myChart.setOption(option);

    //criamos um objeto com nome e total, filtrando quantas vezes aparece a cidade no filtrados.
    const cidadesOrdenadasObj = cidades.map(cidade => ({
        cidade,
        total: quedasFiltrados.filter(q => q.nomeCidade === cidade).length
    }))
        .sort((a, b) => b.total - a.total);
    //separamos com base nos dados do objeto
    const cidadesOrdenadas = cidadesOrdenadasObj.map(obj => obj.cidade);
    const totaisOrdenados = cidadesOrdenadasObj.map(obj => obj.total);

    var chartDom2 = document.getElementById('chart-quedasCidades');
    var myChart2 = echarts.init(chartDom2);
    var option2 = {
        title: {
            text: 'Quantidade de alertas por cidade - ' + mes,
            left: 'center'
        },
        tooltip: {
            trigger: 'axis',
            axisPointer: {
                type: 'shadow'
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
        },
        yAxis: {
            type: 'category',
            data: cidadesOrdenadas,
            axisLabel: {
                fontSize: 15,
                formatter: function (value) {
                    return value.length > 30 ? value.slice(0, 27) + '...' : value;
                }
            }
        },
        dataZoom: [
            {
                type: 'slider',
                yAxisIndex: 0,
                start: 10,
                end: 0 // mostra as primeiras 20 cidades e permite scroll
            },
            {
                type: 'inside',
                yAxisIndex: 0,
                start: 0,
                end: 20
            }

        ],
        series: [{
            name: 'Alertas',
            type: 'bar',
            data: totaisOrdenados,
            itemStyle: {
                color: '#76A7FA'
            },
            label: {
                show: true
            }
        }]
    };
    myChart2.clear()
    myChart2.setOption(option2);

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
                    fontWeight: 'bold', // Negrito
                    color: '#fff'       // Cor branca para destacar sobre o colorido
                },
                data: [
                    {value: totalEnergiaGeral, name: 'Energia'},
                    {value: 11}
                ]
            }
        ]
    };
    const chartDomEnergia = document.getElementById('chart-quedasCidadesEnergia');
    const myChartEnergia = echarts.init(chartDomEnergia)
    myChartEnergia.setOption(optionEnergia);

}


function atualizarNome(nome) {
    nomeFiltro = nome;
    mes = "todos"
    atualizarGraficoIndisponibilidade()
    graficoDeAlertasPorCidadeEMes()
}


let mesAnterior = 'x';

function atualizarMes(mesNovo) {
    mes = mesNovo
    if (mesAnterior !== mesNovo && mesAnterior !== 'x') {
        document.getElementById(mesAnterior).style.backgroundColor = "#11bb79";
    }
    mesAnterior = mesNovo;
    document.getElementById(mesNovo).style.backgroundColor = "#279c00";
    atualizarGraficoIndisponibilidade();
    graficoDeAlertasPorCidadeEMes();
}
