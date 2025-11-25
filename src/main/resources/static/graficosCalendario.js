function mapaDeQuedasNum(quedas){
    let mapa = new Map()
    quedas.forEach((queda) => {
        let dia = queda.data.substring(0,10);
        mapa.set(dia, (mapa.get(dia) || 0) + 1);
        })

    return mapa
}

function filtraCidade(quedas, cidade){
    return quedas.filter((queda) => queda.nomeCidade == cidade)
}

function listaTempos(quedas, cidade){
    let quedasCidade = filtraCidade(quedas, cidade)
    return quedasCidade.map((queda => {
               return {data: queda.data.substring(0,10), tempo: (queda.tempoFora/60)}
           }))
}

function dadosQntQuedas(year, quedas) {
    const date = +echarts.time.parse(year + '-01-01');
    const end = +echarts.time.parse(+year + 1 + '-01-01');
    const dayTime = 3600 * 24 * 1000;
    const data = [];
    let mapDias = mapaDeQuedasNum(quedas)
    for (let time = date; time < end; time += dayTime) {
        let dia = echarts.time.format(time, '{yyyy}-{MM}-{dd}', false)

        data.push([
            dia,
            (mapDias.get(dia) || 0)
        ]);
    }
    return data;
}

function dadosTempoQuedas(year, quedas, cidade) {
    const date = +echarts.time.parse(year + '-01-01');
    const end = +echarts.time.parse(+year + 1 + '-01-01');
    const dayTime = 3600 * 24 * 1000;
    listaDeTempos = listaTempos(quedas, cidade)
    const data = [];
    let exce = 0;
    for (let time = date; time < end; time += dayTime) {
        let dia = echarts.time.format(time, '{yyyy}-{MM}-{dd}', false)
        let quedasDia = listaDeTempos.filter((tempo) => tempo.data == dia)

        let tempo = quedasDia.reduce((total, queda) => total + queda.tempo, 0) + exce

        if(tempo > 1440) {
            exce = tempo - 1440
            tempo = 1440
        }else{
            exce = 0
        }

        data.push([
            dia,
            tempo
        ]);
    }
    return data;
}


function fazGraficoQntQuedasTodas(elem, quedas){
    const anoAtual = new Date().getFullYear()
    let dados = dadosQntQuedas(anoAtual, quedas);

    option = {
      gradientColor: ['#dedede', '#00adb5', '#00588b'],
      title: {
        top: 30,
        left: 'center',
        text: 'Quedas por dia'
      },
      tooltip: {
        formatter: function (params) {
                   const data = params.value[0];
                   const value = params.value[1];

                   return data + '<br>' + value + ' quedas';
                   }
      },
      visualMap: {
        min: 0,
        max: 40,
        type: 'piecewise',
        pieces: [
            { min: 0, max: 2},
            { min: 2, max: 7},
            { min: 7, max: 12},
            { min: 12, max: 20},
            { min: 20, max: 60}
        ],
        splitNumber: 5,
        orient: 'horizontal',
        left: 'center',
        top: 65,
        target: {
            outOfRange: {
                color: ["ffff"]
            }
        }
      },
      calendar: {
        top: 120,
        left: 30,
        right: 30,
        cellSize: ['auto', 13],
        range: '2025',
        itemStyle: {
          borderWidth: 0.5
        },
        yearLabel: { show: false }
      },
      series: {
        type: 'heatmap',
        coordinateSystem: 'calendar',
        data: dados
      }
    };

    const chart = echarts.init(elem)
    chart.clear()
    chart.setOption(option)
}

function fazGraficoQntQuedasCidade(elem, quedas, cidade){
    const anoAtual = new Date().getFullYear()
    let dados = dadosQntQuedas(anoAtual, filtraCidade(quedas, cidade));

    let option = {
          gradientColor: ['#dedede', '#00adb5', '#00588b'],
          title: {
            top: 30,
            left: 'center',
            text: "Quantidade de Quedas em " + cidade
          },
          tooltip: {
            formatter: function (params) {
                                const data = params.value[0];
                                const value = params.value[1];

                                return data + '<br>' + value + ' quedas';
                            }
          },
          visualMap: {
            min: 0,
            max: 2,
            type: 'piecewise',
            pieces: [
                        { min: 0, max: 0},
                        { min: 1, max: 1},
                        { min: 2, max: 2}
                    ],
            splitNumber: 3,
            orient: 'horizontal',
            left: 'center',
            top: 65,
            target: {
                outOfRange: {
                    color: ["ffff"]
                }
            }
          },
          calendar: {
            top: 120,
            left: 30,
            right: 30,
            cellSize: ['auto', 13],
            range: '2025',
            itemStyle: {
              borderWidth: 0.5
            },
            yearLabel: { show: false }
          },
          series: {
            type: 'heatmap',
            coordinateSystem: 'calendar',
            data: dados
          }
        };

    const chart = echarts.init(elem)
    chart.clear()
    chart.setOption(option)
}

function fazGraficoTempoQuedasCidade(elem, quedas, cidade){
    const anoAtual = new Date().getFullYear()
    let dados = dadosTempoQuedas(anoAtual, quedas, cidade);

    let option = {
          gradientColor: ['#dedede', '#00adb5', '#00588b'],
          title: {
            top: 30,
            left: 'center',
            text: "Tempo Fora em " + cidade
          },
          tooltip: {
            formatter: function (params) {
                           const data = params.value[0];

                           let horas = Math.trunc(params.value[1] / 60)
                           let mins =  Math.trunc((params.value[1] % 60))
                           const value = {
                               hours: horas,
                               minutes: mins
                           };
                           const brFormatter = new Intl.DurationFormat('pt', { style: 'short' });

                           return data + '<br>' + brFormatter.format(value);
                           }
          },
          visualMap: {
            min: 0,
            max: 24,
            type: 'piecewise',
            pieces: [
                 { min: 0, max: 10, label: "Até 10min"},
                 { min: 10, max: 60, label: "Até 1h"},
                 { min: 60, max: 120, label: "Até 2h"},
                 { min: 120, max: 240, label: "Até 4h"},
                 { min: 240, max: 1440, label: "Mais de 4h"}
            ],
            splitNumber: 4,
            orient: 'horizontal',
            left: 'center',
            top: 65,
            inactiveColor: '#d3d3d3'
          },
          calendar: {
            top: 120,
            left: 30,
            right: 30,
            cellSize: ['auto', 13],
            range: '2025',
            itemStyle: {
              borderWidth: 0.5
            },
            yearLabel: { show: false }
          },
          series: {
            type: 'heatmap',
            coordinateSystem: 'calendar',
            data: dadosTempoQuedas(anoAtual, quedas, cidade)
          }
        };

    const chart = echarts.init(elem)
    chart.clear()
    chart.setOption(option)
}


const chartQuedasPorDia = document.getElementById('chartQuedasPorDia')
fazGraficoQntQuedasTodas(chartQuedasPorDia, quedas)

const chartQuedasPorCidade = document.getElementById('chartQuedasPorCidade')
fazGraficoQntQuedasCidade(chartQuedasPorCidade, quedas, "Pedro_Osorio")

const chartTempoPorCidade = document.getElementById('chartTempoPorCidade')
fazGraficoTempoQuedasCidade(chartTempoPorCidade, quedas, "Pedro_Osorio")

function novaCidade(){
    let input = document.getElementById("seletorCidade")

    let cidade = input.value

    fazGraficoQntQuedasCidade(chartQuedasPorCidade, quedas, cidade)
    fazGraficoTempoQuedasCidade(chartTempoPorCidade, quedas, cidade)
}