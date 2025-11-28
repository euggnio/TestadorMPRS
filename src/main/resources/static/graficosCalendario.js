// const quedas -> deve ser definido em tag inline do template html assim
//
//<script type="text/javascript" th:inline="javascript">
//    const quedas = /*[[${quedas}]]*/ null;
//</script>

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

function listaCidades(){
    let setCid = new Set()
    quedas.forEach(q => setCid.add(q.nomeCidade))

    return Array.from(setCid).sort()
}

function cidadeAleatoria(){return listaCidades()[Math.floor(Math.random() * 181)]}

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

        data.push([dia, tempo]);
    }
    return data;
}


function fazGraficoQntQuedasTodas(elem, quedas){
    const anoAtual = new Date().getFullYear()
    let dados = dadosQntQuedas(anoAtual, quedas);

    option = {
      gradientColor: ['#cddede', '#00adb5', '#00588b', '#003333'],
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
            { min: 1, max: 3},
            { min: 3, max: 7},
            { min: 7, max: 12},
            { min: 12, max: 20},
            { min: 20, max: 60},
            { min: 60}
        ],
        splitNumber: 5,
        orient: 'horizontal',
        left: 'center',
        top: 65,
        target: {
            outOfRange: {
                color: '#eeeeef'
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


function fazDropCidades(){
    let select = document.getElementById("seletorCidade")
    const lista = listaCidades()

    lista.forEach(cid => {
        let elem = document.createElement('option')
        elem.value = elem.innerText = cid
        select.appendChild(elem)
    })
}
window.addEventListener('load', fazDropCidades)

let inputCidade = document.getElementById("seletorCidade")
inputCidade.addEventListener('change', () => {
    let input = document.getElementById("seletorCidade")
    novaCidade(input.value)
})

function novaCidade(cidade){
    document.getElementById("seletorCidade").value = cidade
    fazGraficoQntQuedasCidade(document.getElementById('chartQuedasPorCidade'), quedas, cidade)
    fazGraficoTempoQuedasCidade(document.getElementById('chartTempoPorCidade'), quedas, cidade)

    fazGraficoEstatisticas(document.getElementById("statsCid"), filtraCidade(quedas, cidade))
    fazGraficoQntPorTempo(document.getElementById("temposCid"), filtraCidade(quedas, cidade))
}

function segundosParaDuration(s){
    s = Math.floor(s)
    let horas = Math.floor(s / 3600);
    let minutos = Math.floor((s - horas * 3600) / 60)
    let segundos = s - (horas * 3600) - (minutos * 60)

    return Temporal.Duration.from({hours: horas, minutes: minutos, seconds: segundos})
}

const ult = new Date(quedas[0].data)
const agr = new Date()
let segundosT = Math.floor((agr - ult) / 1000)

function timer(){
    let timer = document.getElementById("timer")
    ++segundosT
    const dur = segundosParaDuration(segundosT)

    timer.innerText = `${pad(dur.hours)}:${pad(dur.minutes)}:${pad(dur.seconds)}`
}
document.getElementById("ultima").innerText = '(' + quedas[0].nomeCidade + " por "
                                       + segundosParaDuration(quedas[0].tempoFora).toLocaleString('pt')
                                       + ')'
timer()
setInterval(timer, 1000)

function pad(num){return num <= 9 ? "0" + num : "" + num;}

function fazGraficoEstatisticas(elem, quedas){
    const todosTempos = quedas.map(q => q.tempoFora)

    const media = Math.floor(ss.mean(todosTempos)/60)
    const mediana = Math.floor(ss.median(todosTempos)/60)
    const desvio = Math.floor(ss.standardDeviation(todosTempos)/60)
    const moda = Math.floor(ss.mode(todosTempos)/60)

    let option = {
      color: '#00adb5',
      title: {
        top: 30,
        left: 'center',
        text: "Duração das Quedas (minutos)"
      },
      tooltip: {
        formatter: function (params) {
          let data = segundosParaDuration(params.data * 60)
          return data.toLocaleString('pt')
        }
      },
      yAxis: {
        type: 'category',
        data: ['Média', 'Mediana', 'Moda', 'Desvio Padrão']
      },
      xAxis: {
        type: 'value'
      },
      series: [
        {
          data: [media, mediana, moda, desvio],
          type: 'bar'
        }
      ]
    };

    const chart = echarts.init(elem)
    chart.clear()
    chart.setOption(option)
}

function fazGraficoQntPorTempo(elem, quedas){
    const todosTempos = quedas.map(q => Math.floor(q.tempoFora/60))

    let tempos = [0,0,0,0,0]

    todosTempos.forEach(t => {
        switch(true){
            case t <= 10:
                tempos[0]++
                break
            case t <= 60:
                tempos[1]++
                break
            case t <= 120:
                tempos[2]++
                break
            case t <= 240:
                tempos[3]++
                break
            default:
                tempos[4]++
        }
    })

    let option = {
      color: '#00adb5',
      title: {
        top: 30,
        left: 'center',
        text: "Quantidade de Quedas por Duração"
      },
      tooltip: {
        formatter: function (params) {
          return params.data + " quedas"
        }
      },
      xAxis: {
        type: 'category',
        data: ['Até 10min', 'Até 1h', 'Até 2h', 'Até 4h', 'Mais de 4h']
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          data: tempos,
          type: 'bar'
        }
      ]
    };

    console.log(tempos.reduce((a,b)=>a+b), todosTempos)

    const chart = echarts.init(elem)
    chart.clear()
    chart.setOption(option)
}


fazGraficoQntQuedasTodas(document.getElementById('chartQuedasPorDia'), quedas)

fazGraficoEstatisticas(document.getElementById("stats"), quedas)
fazGraficoQntPorTempo(document.getElementById("tempos"), quedas)

window.addEventListener('load', () => novaCidade(cidadeAleatoria()))
