let nomeFiltro = "";
let mes = 'todos';
let quedas;
let dados;
// variaveis fixas
let quedasProcessadas = [];
let mesesUnicos = [];
let cidades = [];

// variaveis usadas nos filtros
let quedasFiltradas = [];
let meses = [];
let diasUnicos = [];
let cidadesOrdenadasObj = [];

// ===================== HELPERS DE FLAP / SERVIÇO =====================
const SERVICO_INICIO = 12; // 12:00
const SERVICO_FIM    = 19; // 19:00  -> janela [12:00, 19:00)

function isFlap(q){ return q.flap === true; }
function semFlap(q){ return q.flap !== true; }

// queda cujo INÍCIO (campo data / LocalDateTime) cai na janela de serviço
function dentroDoServico(q){
    if (!q.data) return false;
    const d = new Date(q.data);
    if (isNaN(d)) return false;
    const h = d.getHours();
    return h >= SERVICO_INICIO && h < SERVICO_FIM;
}

// reaproveita instância echarts já criada no mesmo DOM
function _echart(dom){ return echarts.getInstanceByDom(dom) || echarts.init(dom); }
// ====================================================================

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
    graficoImpacto();           // <-- NOVO
    fazDropCidades();
    console.log(cidades);
}

function processarQuedas(){
    console.log("\n\n======Processando quedas ======");
    quedasProcessadas = quedas.map(a => {
        const data = new Date(a.data);
        const mes = data.toLocaleString('en-US', {month: 'short'}).toUpperCase();

        if (!mesesUnicos.includes(mes)) mesesUnicos.push(mes);
        if (!cidades.includes(a.nomeCidade)) cidades.push(a.nomeCidade);

        const dia = data.getDate();
        return {
            mes, nomeCidade: a.nomeCidade, dia,
            energia: a.faltaDeLuz, coordenada: a.coordenadas,
            flap: a.flap, categoria: a.categoria, data: a.data
        };
    });
    console.log("Meses encontrados", mesesUnicos);
    console.log("Cidades com quedas encontradas", cidades);
    console.log("Quedas encontrados", quedasProcessadas);
    console.log("======Processando quedas finalizado ======\n\n");

    quedasFiltradas = quedasProcessadas;
}

function filtrarQuedas(){
    console.log("\n\n======Filtrando quedas ======");
    let filtradasNome = nomeFiltro !== ""
        ? quedasProcessadas.filter(a => a.nomeCidade.toUpperCase().includes(nomeFiltro.toUpperCase()))
        : quedasProcessadas;

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

    // OFICIAL (sem flap) + flap como extra
    cidadesOrdenadasObj = cidades.map(cidade => {
        const doCidade = quedasFiltradas.filter(a =>
            a.nomeCidade === cidade &&
            (mes === "todos" || a.mes === mes.slice(0, 3))
        );
        const oficiais = doCidade.filter(semFlap);
        return {
            cidade,
            oficial: oficiais.length,                        // total SEM flap
            flap:    doCidade.filter(isFlap).length,         // flaps (extra)
            energia: oficiais.filter(a => a.energia).length  // energia só entre oficiais
        };
    }).sort((a, b) => b.oficial - a.oficial);

    console.log("Meses encontrado", meses);
    console.log("Quedas filtradas", quedasFiltradas);
    console.log("======Filtrando quedas finalizado======\n\n\n");
}

// ===== ITEM 3: Cidades e Energia (flap em cinza escuro, total oficial x c/ flap) =====
function graficoAlertasCidades(){
    console.log("\n\n====== Gerando grafico alertas em cidades ======\n\n");

    const cidadesOrdenadas = cidadesOrdenadasObj.map(o => o.cidade);
    const totaisOficiais   = cidadesOrdenadasObj.map(o => o.oficial);
    const totaisEnergia    = cidadesOrdenadasObj.map(o => o.energia);
    const totaisFlap       = cidadesOrdenadasObj.map(o => o.flap);

    var divGrafico = document.getElementById("chart-quedasCidades");
    var grafico = echarts.init(divGrafico);

    var option = {
        title: { text: 'Quedas por Cidade' },
        textStyle: { fontFamily: 'Nunito, Microsoft YaHei, sans-serif' },
        tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'shadow' },
            formatter: function (params) {
                const i = params[0].dataIndex;
                const o = cidadesOrdenadasObj[i];
                let s = `<b>${o.cidade}</b><br/>`;
                s += `Total oficial (s/ flap): <b>${o.oficial}</b><br/>`;
                s += `Energia: ${o.energia}<br/>`;
                s += `<span style="color:#888;">Flap / Loop: ${o.flap}</span><br/>`;
                s += `Total c/ flap: <b>${o.oficial + o.flap}</b>`;
                return s;
            }
        },
        legend: {},
        xAxis: { type: 'value', boundaryGap: [0, 0.01] },
        yAxis: { type: 'category', data: cidadesOrdenadas },
        dataZoom: [
            { type: 'slider', yAxisIndex: 0, start: 8, end: 0 },
            { type: 'inside', yAxisIndex: 0, start: 0, end: 20 }
        ],
        series: [
            {
                name: 'Total (oficial)',
                type: 'bar',
                label: { show: true, formatter: p => p.value == 0 ? '' : p.value },
                data: totaisOficiais
            },
            {
                name: 'Energia',
                type: 'bar',
                color: '#f6652c',
                label: { show: true, color: 'white', formatter: p => p.value == 0 ? '' : p.value },
                data: totaisEnergia
            },
            {
                name: 'Flap / Loop',
                type: 'bar',
                color: '#4d4d4d', // cinza escuro = info adicional
                label: { show: true, color: '#ddd', formatter: p => p.value == 0 ? '' : p.value },
                data: totaisFlap
            }
        ]
    };
    grafico.clear();
    grafico.setOption(option);
}

// ===== NÃO MEXIDO (pizza relação energia) =====
function graficoRelacaoQuedasEnergia(){
    console.log("\n\n====== Gerando grafico relacao entre energia======\n\n");
    const quedasTotais = quedasFiltradas.length;
    const energiaTotal = quedasFiltradas.filter(a => a.energia === true).length
    console.log("Quedas encontrados", quedasTotais);
    console.log("Quedas de energia encontrados", energiaTotal);

    let optionEnergia = {
        color: ['#FF4500', '#1e840e'],
        title: { text: 'Relação Quedas por Energia', left: 'center' },
        textStyle: { fontFamily: 'Nunito, Microsoft YaHei, sans-serif' },
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { top: '5%', left: 'center' },
        series: [
            {
                name: 'Acesso',
                type: 'pie',
                radius: ['40%', '70%'],
                center: ['50%', '75%'],
                startAngle: 180,
                endAngle: 360,
                avoidLabelOverlap: false,
                label: {
                    show: true,
                    position: 'inside',
                    formatter: '{b}\n{d}%',
                    fontSize: 15,
                    fontWeight: 'bold',
                },
                data: [
                    {value: quedasTotais - energiaTotal, name: 'Sem energia'},
                    {value: energiaTotal, name: 'Outros'},
                ]
            }
        ]
    };
    const chartDomEnergia = document.getElementById('chart-quedasCidadesEnergia');
    const myChartEnergia = echarts.init(chartDomEnergia)
    myChartEnergia.setOption(optionEnergia);
}

// ===== NÃO MEXIDO (Disponibilidade + IDM) =====
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
            nome, disponibilidade: soma / count
        })).sort((a, b) => a.disponibilidade - b.disponibilidade);
    } else {
        const mesEncontrado = dados.find(item => item.month === mesBusca);
        dadosMes = mesEncontrado
            ? mesEncontrado.disponibilidades.filter(d => d.nome?.trim())
                .sort((a, b) => a.disponibilidade - b.disponibilidade)
            : [];
    }
    const filtrados = nomeFiltro && nomeFiltro.trim() !== ""
        ? dadosMes.filter(d => d.nome.toUpperCase().includes(nomeFiltro.toUpperCase()))
        : dadosMes;

    const cidades = filtrados.map(d => d.nome);
    const valores = filtrados.map(d => d.disponibilidade.toFixed(2));

    const chartIndisp = echarts.init(document.getElementById('chart-indisponibilidade'));
    var option = {
        title: { text: 'Disponibilidade por Cidade (%) - ' + mes, left: 'center' },
        textStyle: { fontFamily: 'Nunito, Microsoft YaHei, sans-serif' },
        tooltip: {
            trigger: 'axis', formatter: '{b} : {c}%', axisPointer: {type: 'shadow'},
            textStyle: { fontWeight: 'bolder', color: '#222831' }
        },
        grid: { left: '5%', right: '5%', bottom: '2%', containLabel: true },
        xAxis: { type: 'value', max: 100 },
        yAxis: {
            type: 'category', data: cidades,
            axisLabel: {
                fontSize: 15,
                formatter: value => value.length > 30 ? value.slice(0, 27) + '...' : value
            },
        },
        dataZoom: [
            {type: 'slider', yAxisIndex: 0, start: 10, end: 0},
            {type: 'inside', yAxisIndex: 0, start: 0, end: 20}
        ],
        series: [{
            name: 'Indisponibilidade', type: 'bar', data: valores,
            itemStyle: {
                color: params => params.value < 99.4 ? '#FA7676' : '#76A7FA',
                shadowColor: 'rgba(0, 0, 0, 0.5)', shadowBlur: 0.5
            },
            label: {show: true, formatter: '{c}%'}
        }]
    };
    chartIndisp.setOption(option);

    var port = document.getElementById('chart-porcentagem');
    var chartPorcentagem = echarts.init(port);
    var porcentagem = 0.0;
    for (let i = 0; i < valores.length; i++) porcentagem += parseFloat(valores[i]);
    porcentagem = porcentagem / valores.length;
    var contadorIDMOK = valores.filter(item => item > 99.4).length;
    port2 = {
        tooltip: { trigger: 'item', formatter: '{a} <br/>{b}: {c} ({d}%)' },
        textStyle: { fontFamily: 'Nunito, Microsoft YaHei, sans-serif' },
        legend: { data: ['Média OK', 'Média NOK', 'OK', 'NOK'] },
        series: [
            {
                name: 'MÉDIA IDM, DISPONIBILIDADE', type: 'pie', selectedMode: 'single',
                radius: [0, '30%'],
                label: { formatter: '{b} {c}%', position: 'inner', fontSize: 14, fontWeight: 'bold' },
                labelLine: { show: false },
                data: [
                    {value: porcentagem.toFixed(2), name: 'Média OK'},
                    {value: (100 - porcentagem).toFixed(2), name: 'Média NOK', itemStyle: {color: '#6d1f9a'}}
                ],
            },
            {
                name: 'IDM', type: 'pie', radius: ['50%', '70%'],
                labelLine: { length: 30 },
                label: {
                    formatter: '{a|{a}}{abg|}\n{hr|}\n  {b|{b}：}{c}  {per|{d}%}  ',
                    backgroundColor: '#F6F8FC', borderColor: '#8C8D8E', fontSize: 25,
                    rich: {
                        a: { color: '#6E7079', lineHeight: 22, align: 'center' },
                        hr: { borderColor: '#8C8D8E', width: '100%', borderWidth: 1, height: 0 },
                        b: { color: '#4C5058', fontSize: 14, fontWeight: 'bold', lineHeight: 33 },
                        per: { color: '#fff', backgroundColor: '#4C5058', padding: [3, 4], borderRadius: 4 }
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

// ===== ITEM 4: Mensal (oficial x c/ flap no topo, flap cinza escuro, MÉDIA intocada) =====
function graficoDeQuedasMesDia() {
    console.log("\n\n\n ===== GRAFICO DE QUEDAS MES E DIA INICIALIZANDO =====");
    var chartDom = document.getElementById('chart-quedas');
    var myChart = echarts.init(chartDom);
    const campoFiltro = (mes !== "todos") ? 'dia' : 'mes';

    // OFICIAL (sem flap)
    const dataComEnergia = meses.map(m =>
        quedasFiltradas.filter(q => q[campoFiltro] === m && q.energia === true  && semFlap(q)).length
    );
    const dataSemEnergia = meses.map(m =>
        quedasFiltradas.filter(q => q[campoFiltro] === m && q.energia === false && semFlap(q)).length
    );
    // FLAP (extra)
    const dataFlap = meses.map(m =>
        quedasFiltradas.filter(q => q[campoFiltro] === m && isFlap(q)).length
    );
    const dataTotalSemFlap = meses.map((m, i) => dataComEnergia[i] + dataSemEnergia[i]);
    const dataTotalComFlap = meses.map((m, i) => dataTotalSemFlap[i] + dataFlap[i]);

    // MÉDIA DIÁRIA — EXATAMENTE como hoje (total inclui tudo)
    const dataTotal = meses.map(m =>
        quedasFiltradas.filter(q => q[campoFiltro] === m).length
    );
    const cidadesCount = meses.map(m => {
        const registrosNoPeriodo = quedasFiltradas.filter(q => q[campoFiltro] === m);
        return new Set(registrosNoPeriodo.map(q => q.nomeCidade)).size;
    });
    const anoAtual = new Date().getFullYear();
    const diasArray = [];
    for (let i = 1; i < meses.length + 1; i++) diasArray.push(new Date(anoAtual, i, 0).getDate());
    const dataMediaPorCidade = dataTotal.map((total, i) => {
        const qtdCidades = (mes !== "todos") ? cidadesCount[i] : diasArray[i];
        if (qtdCidades === 0) return "0.00";
        return (total / qtdCidades).toFixed(2);
    });

    var option = {
        title: {
            text: mes !== "todos" ? `Quedas em ${mes}` : 'Análise de Quedas por Mês',
            subtext: 'Passe o mouse para ver o Top 10 Cidades',
            left: 'center'
        },
        textStyle: { fontFamily: 'Nunito, Microsoft YaHei, sans-serif' },
        tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'cross' },
            formatter: function (params) {
                let eixoX = params[0].name;
                let idx = meses.findIndex(m => m.toString() === eixoX.toString());

                let oficiaisNoPeriodo = quedasFiltradas.filter(q =>
                    q[campoFiltro].toString() === eixoX.toString() && semFlap(q)
                );
                let contagemCidades = {};
                oficiaisNoPeriodo.forEach(q => {
                    contagemCidades[q.nomeCidade] = (contagemCidades[q.nomeCidade] || 0) + 1;
                });
                let topCidades = Object.entries(contagemCidades).sort((a, b) => b[1] - a[1]).slice(0, 10);

                let res = `<b>${mes !== "todos" ? 'Dia' : 'Mês'}: ${eixoX}</b><br/>`;
                params.forEach(item => {
                    if (item.seriesName !== 'Média Diária') {
                        res += `${item.marker} ${item.seriesName}: ${item.value}<br/>`;
                    }
                });
                res += `<br/><b>Total oficial (s/ flap):</b> ${dataTotalSemFlap[idx]}<br/>`;
                res += `<span style="color:#888;"><b>Total c/ flap:</b> ${dataTotalComFlap[idx]} (flap: ${dataFlap[idx]})</span><br/>`;
                res += `<br/><b>Top 10 Cidades (s/ flap):</b><br/>`;
                if (topCidades.length === 0) res += `Sem registros oficiais<br/>`;
                else topCidades.forEach(([cidade, qtd]) => { res += `${cidade}: ${qtd}<br/>`; });
                return res;
            }
        },
        legend: { data: ['Com Energia', 'Sem Energia', 'Flap / Loop', 'Média Diária'], bottom: 0 },
        xAxis: [{ type: 'category', data: meses, axisPointer: { type: 'shadow' } }],
        yAxis: [
            { type: 'value', name: 'Qtd.', axisLabel: { formatter: '{value} un' } },
            { type: 'value', name: 'Média', position: 'right' }
        ],
        series: [
            {
                name: 'Com Energia', type: 'bar', stack: 'total', barWidth: '50%',
                itemStyle: { color: '#91cc75' },
                label: { show: true, position: 'inside', formatter: p => p.data == 0 ? "" : p.data },
                data: dataComEnergia
            },
            {
                name: 'Sem Energia', type: 'bar', stack: 'total',
                itemStyle: { color: '#ee6666' },
                label: { show: true, position: 'inside', formatter: p => p.data == 0 ? "" : p.data },
                data: dataSemEnergia
            },
            {
                name: 'Flap / Loop', type: 'bar', stack: 'total',
                itemStyle: { color: '#4d4d4d' }, // cinza escuro
                label: {
                    show: true, position: 'top', color: '#595959', fontWeight: 'bold',
                    formatter: function (p) {
                        const semF = dataTotalSemFlap[p.dataIndex];
                        const comF = dataTotalComFlap[p.dataIndex];
                        const flap = dataFlap[p.dataIndex];
                        if (comF === 0) return '';
                        return flap > 0 ? `${semF} oficial\n${comF} c/ flap` : `${semF}`;
                    }
                },
                data: dataFlap
            },
            {
                // MÉDIA DIÁRIA — idêntica ao seu código atual
                name: 'Média Diária', type: 'line', smooth: true,
                itemStyle: { color: '#5070dd', borderColor: '#ccc' },
                label: { show: true, position: mes == "todos" ? 'top' : 'bottom' },
                data: dataMediaPorCidade
            }
        ]
    };
    option && myChart.setOption(option);
}

// ===== ITEM 5: Impacto por hora do dia — colunas 24h com destaque para horário de serviço =====
function graficoImpacto() {
    console.log("\n\n\n ===== GRAFICO IMPACTO 24H COLUNAS INICIALIZANDO =====");

    const chartDom = document.getElementById('chart-impacto');

    if (!chartDom) {
        console.warn("Div chart-impacto não encontrada.");
        return;
    }

    const chart = echarts.init(chartDom);

    const horas = Array.from({ length: 24 }, (_, i) => i);
    const rotulosHoras = horas.map(h => `${String(h).padStart(2, '0')}h`);

    const horaEstaNoServico = hora => hora >= 12 && hora < 20;

    const isFlapLocal = q =>
        q.loop === true ||
        q.flap === true ||
        q.categoria === 'FLAP' ||
        q.categoria === 'Flap' ||
        q.categoria === 'flap';

    const semFlapLocal = q => !isFlapLocal(q);

    const temDataValida = q => {
        if (!q.data) return false;
        const data = new Date(q.data);
        return !isNaN(data.getTime());
    };

    const quedasComData = quedasFiltradas.filter(temDataValida);

    const quedasReais = quedasComData.filter(semFlapLocal);
    const quedasFlap = quedasComData.filter(isFlapLocal);

    const contarPorHora = lista => {
        return horas.map(h => {
            return lista.filter(q => {
                const data = new Date(q.data);
                return data.getHours() === h;
            }).length;
        });
    };

    const dadosQuedasPorHora = contarPorHora(quedasReais);
    const dadosFlapPorHora = contarPorHora(quedasFlap);

    const totalQuedas = dadosQuedasPorHora.reduce((a, b) => a + b, 0);
    const totalFlap = dadosFlapPorHora.reduce((a, b) => a + b, 0);

    const totalQuedasServico = dadosQuedasPorHora.reduce((acc, val, idx) => {
        return acc + (horaEstaNoServico(idx) ? val : 0);
    }, 0);

    const totalFlapServico = dadosFlapPorHora.reduce((acc, val, idx) => {
        return acc + (horaEstaNoServico(idx) ? val : 0);
    }, 0);

    const montarDadosComCor = (dados, corDentro, corFora) => {
        return dados.map((valor, hora) => ({
            value: valor,
            itemStyle: {
                color: horaEstaNoServico(hora) ? corDentro : corFora
            }
        }));
    };

    const dadosQuedasColoridos = montarDadosComCor(
        dadosQuedasPorHora,
        '#ee6666', // dentro do horário de serviço
        'rgba(30,132,14,0.67)'  // fora do horário de serviço
    );

    const dadosFlapColoridos = montarDadosComCor(
        dadosFlapPorHora,
        '#4d4d4d', // dentro do horário de serviço
        '#bdbdbd'  // fora do horário de serviço
    );

    const option = {
        title: {
            text: 'Impacto por Hora do Dia',
            subtext: `Quedas reais: ${totalQuedas} (serviço: ${totalQuedasServico}) | Flaps: ${totalFlap} (serviço: ${totalFlapServico})`,
            left: 'center'
        },

        textStyle: {
            fontFamily: 'Nunito, Microsoft YaHei, sans-serif'
        },

        tooltip: {
            trigger: 'axis',
            axisPointer: {
                type: 'shadow'
            },
            formatter: function (params) {
                const idx = params[0].dataIndex;
                const hora = horas[idx];
                const emServico = horaEstaNoServico(hora);

                let res = `<b>${rotulosHoras[idx]}</b><br/>`;
                res += `<span style="color:${emServico ? '#2e7d32' : '#888'};">${emServico ? 'Horário de serviço' : 'Fora do horário de serviço'}</span><br/><br/>`;

                params.forEach(item => {
                    const valor = typeof item.value === 'object' ? item.value.value : item.value;
                    res += `${item.marker} ${item.seriesName}: ${valor}<br/>`;
                });

                const valorQuedas = dadosQuedasPorHora[idx];
                const valorFlaps = dadosFlapPorHora[idx];
                const totalHora = valorQuedas + valorFlaps;

                res += `<br/><b>Total na hora:</b> ${totalHora}`;

                return res;
            }
        },

        legend: {
            data: ['Quedas reais', 'Flaps'],
            bottom: 0
        },

        grid: {
            left: '4%',
            right: '4%',
            top: '22%',
            bottom: '15%',
            containLabel: true
        },

        xAxis: {
            type: 'category',
            name: 'Hora do dia',
            data: rotulosHoras,
            axisLabel: {
                rotate: 45
            }
        },

        yAxis: {
            type: 'value',
            name: 'Qtd.',
            minInterval: 1
        },

        series: [
            {
                name: 'Quedas reais',
                type: 'bar',
                data: dadosQuedasColoridos,
                barMaxWidth: 22,
                label: {
                    show: true,
                    position: 'top',
                    formatter: function (p) {
                        return p.value == 0 ? '' : p.value;
                    },
                    fontWeight: 'bold'
                }
            },
            {
                name: 'Flaps',
                type: 'bar',
                data: dadosFlapColoridos,
                barMaxWidth: 22,
                label: {
                    show: true,
                    position: 'top',
                    formatter: function (p) {
                        return p.value == 0 ? '' : p.value;
                    },
                    fontWeight: 'bold'
                }
            }
        ]
    };

    chart.setOption(option, true);

    window.addEventListener('resize', function () {
        chart.resize();
    });
}
// ===== ITEM 8: Mapa — checkbox "Incluir flaps" (padrão: SEM flap) =====
function graficoMapa() {
    var chart = echarts.init(document.getElementById('chart'));

    var filtroEnergia = 'todos';
    var tipoVisualizacao = 'scatter';
    var incluirFlap = false; // padrão: só quedas reais

    const chartEl = document.getElementById('chart');
    let controles = document.getElementById('mapa-controles');
    if (controles) controles.remove();

    controles = document.createElement('div');
    controles.id = 'mapa-controles';
    controles.style.cssText = `
        display:flex;gap:8px;align-items:center;flex-wrap:wrap;
        padding:8px 12px;background:rgb(34 40 49);
        border-bottom:1px solid #2a2a2a;font-family:sans-serif;`;
    controles.innerHTML = `
        <span style="color:#666;font-size:11px;font-weight:700;letter-spacing:1.5px;text-transform:uppercase;">Energia</span>
        <button id="btn-todos" class="map-btn" style="padding:4px 12px;border-radius:20px;font-size:12px;cursor:pointer;">Todos</button>
        <button id="btn-com" class="map-btn" style="padding:4px 12px;border-radius:20px;font-size:12px;cursor:pointer;">Com energia</button>
        <button id="btn-sem" class="map-btn" style="padding:4px 12px;border-radius:20px;font-size:12px;cursor:pointer;">Sem energia</button>
        <div style="width:1px;height:20px;background:#2a2a2a;margin:0 4px;"></div>
        <span style="color:#666;font-size:11px;font-weight:700;letter-spacing:1.5px;text-transform:uppercase;">Modo</span>
        <button id="btn-bolhas" class="map-btn" style="padding:4px 12px;border-radius:20px;font-size:12px;cursor:pointer;">Bolhas</button>
        <button id="btn-calor" class="map-btn" style="padding:4px 12px;border-radius:20px;font-size:12px;cursor:pointer;">Calor</button>
        <div style="width:1px;height:20px;background:#2a2a2a;margin:0 4px;"></div>
        <label style="display:flex;align-items:center;gap:6px;color:#888;font-size:12px;cursor:pointer;">
            <input type="checkbox" id="chk-flap"> Incluir flaps
        </label>
        <span id="mapa-total" style="margin-left:auto;color:#666;font-size:12px;"></span>`;
    chartEl.parentNode.insertBefore(controles, chartEl);

    const aplicarEstiloBotao = (btn, ativo) => {
        btn.style.background  = ativo ? '#3a3a3a' : 'transparent';
        btn.style.color       = ativo ? '#fff' : '#888';
        btn.style.borderColor = ativo ? '#555' : '#444';
        btn.style.borderStyle = 'solid';
    };
    const atualizarBotoesUI = () => {
        aplicarEstiloBotao(document.getElementById('btn-todos'), filtroEnergia === 'todos');
        aplicarEstiloBotao(document.getElementById('btn-com'),   filtroEnergia === 'com');
        aplicarEstiloBotao(document.getElementById('btn-sem'),   filtroEnergia === 'sem');
        aplicarEstiloBotao(document.getElementById('btn-bolhas'), tipoVisualizacao === 'scatter');
        aplicarEstiloBotao(document.getElementById('btn-calor'),  tipoVisualizacao === 'heatmap');
    };

    document.getElementById('btn-todos').onclick  = () => { filtroEnergia = 'todos'; renderizar(); };
    document.getElementById('btn-com').onclick    = () => { filtroEnergia = 'com';   renderizar(); };
    document.getElementById('btn-sem').onclick    = () => { filtroEnergia = 'sem';   renderizar(); };
    document.getElementById('btn-bolhas').onclick = () => { tipoVisualizacao = 'scatter'; renderizar(); };
    document.getElementById('btn-calor').onclick  = () => { tipoVisualizacao = 'heatmap'; renderizar(); };
    document.getElementById('chk-flap').onchange  = (e) => { incluirFlap = e.target.checked; renderizar(); };

    // agrupa separando oficial x flap e energia x sem energia (sem duplicar)
    const agrupar = (dados) => {
        const mapa = {};
        dados.forEach(item => {
            if (!item.coordenada) return;
            if (!mapa[item.nomeCidade]) {
                const [lat, lng] = item.coordenada.split(',').map(Number);
                mapa[item.nomeCidade] = {
                    nome: item.nomeCidade.replace(/_/g, ' '), lat, lng,
                    oficialCom: 0, oficialSem: 0, flapCom: 0, flapSem: 0
                };
            }
            const c = mapa[item.nomeCidade];
            if (isFlap(item)) { item.energia ? c.flapCom++ : c.flapSem++; }
            else              { item.energia ? c.oficialCom++ : c.oficialSem++; }
        });
        return Object.values(mapa);
    };

    const getTooltipFormatter = (params) => {
        const c = params.data.extra;
        if (!c) return '';
        const oficial = c.oficial, flap = c.flap;
        const pct = oficial > 0 ? Math.round((c.oficialSem / oficial) * 100) : 0;
        const countExibido = params.data.value[2];
        return `
            <div style="font-family:sans-serif;min-width:200px;color:#ddd;">
                <b style="font-size:13px;color:#fff;">${c.nome}</b>
                <hr style="margin:4px 0;border:none;border-top:1px solid #444;"/>
                <div style="margin-bottom:2px;">Exibido (${incluirFlap ? 'com flaps' : 'sem flaps'}):
                    <b style="color:#fff;font-size:13px;">${countExibido}</b></div>
                <div style="font-size:11px;color:#aaa;">Oficiais (s/ flap): ${oficial}</div>
                <div style="font-size:11px;color:#8d8d8d;">Flap / Loop: ${flap}</div>
                <div style="font-size:11px;color:#aaa;">Com energia: ${c.oficialCom} &nbsp;|&nbsp; Sem: ${c.oficialSem}</div>
                <div style="margin-top:6px;background:#222;border-radius:3px;height:6px;overflow:hidden;">
                    <div style="background:#ff4444;border-radius:3px;height:6px;width:${pct}%;"></div>
                </div>
                <div style="font-size:10px;color:#F44;text-align:right;margin-top:2px;">${pct}% sem energia</div>
            </div>`;
    };

    const renderizar = () => {
        atualizarBotoesUI();
        const todasCidades = agrupar(quedasFiltradas);

        const cidadesFiltradas = todasCidades.map(c => {
            const oficial = c.oficialCom + c.oficialSem;
            const flap    = c.flapCom + c.flapSem;
            let count;
            if (filtroEnergia === 'com')      count = c.oficialCom + (incluirFlap ? c.flapCom : 0);
            else if (filtroEnergia === 'sem') count = c.oficialSem + (incluirFlap ? c.flapSem : 0);
            else                              count = oficial      + (incluirFlap ? flap      : 0);
            return { ...c, oficial, flap, total: oficial + flap, count };
        }).filter(c => c.count > 0);

        const maxCount = cidadesFiltradas.length > 0 ? Math.max(...cidadesFiltradas.map(c => c.count)) : 1;

        const el = document.getElementById('mapa-total');
        if (el) {
            const totalQ = cidadesFiltradas.reduce((s, c) => s + c.count, 0);
            el.textContent = `${totalQ} quedas em ${cidadesFiltradas.length} cidades ${incluirFlap ? '(com flaps)' : '(sem flaps)'}`;
        }

        let series = [];
        let visualMap = {
            show: true, min: 0, max: maxCount, calculable: true,
            inRange: { color: ['#50c850', '#ffb300', '#dc2626'] },
            textStyle: { color: '#aaa' }, left: 10, bottom: 40
        };

        if (tipoVisualizacao === 'heatmap') {
            series = [
                { type: 'heatmap', coordinateSystem: 'leaflet',
                    data: cidadesFiltradas.map(c => [c.lng, c.lat, c.count]), pointSize: 20, blurSize: 30 },
                { type: 'scatter', coordinateSystem: 'leaflet',
                    data: cidadesFiltradas.map(c => ({ value: [c.lng, c.lat, c.count], extra: c })),
                    symbolSize: 5, itemStyle: { opacity: 0 },
                    tooltip: { trigger: 'item', formatter: getTooltipFormatter } }
            ];
        } else {
            series = [{
                type: 'effectScatter', coordinateSystem: 'leaflet',
                data: cidadesFiltradas.map(c => ({
                    value: [c.lng, c.lat, c.count], extra: c,
                    symbolSize: 10 + (c.count / maxCount) * 40
                })),
                rippleEffect: { brushType: 'stroke', scale: 3 },
                tooltip: { trigger: 'item', formatter: getTooltipFormatter }
            }];
        }

        chart.setOption({
            tooltip: { show: true, trigger: 'item', backgroundColor: 'rgba(15,15,20,0.9)',
                borderColor: '#333', borderWidth: 1, padding: 0 },
            leaflet: { center: [-53, -30.2317], zoom: 7, roam: true,
                tiles: [{ urlTemplate: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png' }] },
            visualMap, series
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
    graficoImpacto()        // <-- NOVO
    graficoMapa()
}