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
               let tempoDigital = queda.data.substring(11, 16).split(':')
               let minutosAteQueda = parseInt(tempoDigital[0]) * 60
               minutosAteQueda += parseInt(tempoDigital[1])

               return {data: queda.data.substring(0,10),
                       tempo: (queda.tempoFora/60),
                       hora: minutosAteQueda}
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

        let tempo = 0
        if(exce < 1440){
            tempo = exce
            exce = 0
        }else{
            tempo = 1440
            exce = exce - 1440
        }

        quedasDia.forEach(q => {
            if(q.tempo + q.hora < 1440){
                tempo += q.tempo
            }else{  // caso transborde para o prox dia
                tempo += 1440 - q.hora
                exce = q.tempo - tempo
            }
        })

        data.push([dia, tempo]);
    }
    console.log(filtraCidade(quedas, cidade))
    return data;
}


function fazGraficoQntQuedasTodas(elem, quedas, ano){
    let dados = dadosQntQuedas(ano, quedas);

    option = {
      gradientColor: ['#cddede', '#00adb5', '#00588b', '#003333'],
      title: {
        top: 30,
        left: 'center',
        text: 'Quedas por dia (' + ano + ')'
      },
      tooltip: {
        formatter: function (params) {
                   const data = params.value[0];
                   const value = params.value[1];

                   return '<a class="tooltip" href=/historicoQuedas/dia/'
                          + data + ' target="_blank">' + data + '</a>'
                          + '<br>' + value + ' quedas';
                   },
        position: (point) => [point[0]-2, point[1]-60],
        triggerOn: 'click',
        enterable: true
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
        range: ano,
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

function fazGraficoQntQuedasCidade(elem, quedas, cidade, ano){
    let dados = dadosQntQuedas(ano, filtraCidade(quedas, cidade));

    let option = {
          gradientColor: ['#dedede', '#00adb5', '#00588b', '#000000'],
          title: {
            top: 30,
            left: 'center',
            text: "Quantidade de Quedas em " + cidade + " (" + ano + ')'
          },
          tooltip: {
            formatter: function (params) {
                                const data = params.value[0];
                                const value = params.value[1];

                                return '<a class="tooltip" href=/historicoQuedas/dia/'
                                       + data + ' target="_blank">' + data + '</a>'
                                       + '<br>' + value + ' quedas';
                            },
            position: (point) => [point[0]-2, point[1]-60],
            triggerOn: 'click',
            enterable: true
          },
          visualMap: {
            type: 'piecewise',
            pieces: [
                        { min: 0, max: 0},
                        { min: 1, max: 1},
                        { min: 2, max: 3},
                        { min:3, label: '3+'}
                    ],
            orient: 'horizontal',
            left: 'center',
            top: 65,
            target: {
                outOfRange: {
                    color: '#dedede'
                }
            }
          },
          calendar: {
            top: 120,
            left: 30,
            right: 30,
            cellSize: ['auto', 13],
            range: ano,
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

function fazGraficoTempoQuedasCidade(elem, quedas, cidade, ano){
    let dados = dadosTempoQuedas(ano, quedas, cidade);

    let option = {
          gradientColor: ['#dedede', '#abdede', '#00adb5', '#00588b'],
          title: {
            top: 30,
            left: 'center',
            text: "Tempo Fora em " + cidade + " (" + ano + ')'
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
                           let str = params.value[1] == 0 ? '0 min' : brFormatter.format(value)

                           return '<a class="tooltip" href=/historicoQuedas/dia/'
                                  + data + ' target="_blank">' + data + '</a>'
                                  + '<br>' + str;
                           },
            position: (point) => [point[0]-2, point[1]-60],
            triggerOn: 'click',
            enterable: true
          },
          visualMap: {
            min: 0,
            max: 24,
            type: 'piecewise',
            pieces: [
                 { max: 5, label: "Sem quedas"},
                 { min: 5, max: 10, label: "Até 10min"},
                 { min: 10, max: 60, label: "Até 1h"},
                 { min: 60, max: 120, label: "Até 2h"},
                 { min: 120, max: 240, label: "Até 4h"},
                 { min: 240, max: 1440, label: "Mais de 4h"}
            ],
            splitNumber: 4,
            orient: 'horizontal',
            left: 'center',
            top: 65,
            inactiveColor: '#dedede',
            target: {
              outOfRange: {
                color: '#dedede'
              }
            }
          },
          calendar: {
            top: 120,
            left: 30,
            right: 30,
            cellSize: ['auto', 13],
            range: ano,
            itemStyle: {
              borderWidth: 0.5
            },
            yearLabel: { show: false }
          },
          series: {
            type: 'heatmap',
            coordinateSystem: 'calendar',
            data: dadosTempoQuedas(ano, quedas, cidade)
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

let aleatoria = document.getElementById("aleatoria")
let inputCidade = document.getElementById("seletorCidade")
inputCidade.addEventListener('change', () => {
    let input = document.getElementById("seletorCidade")
    let ano = document.getElementById("dropAno").value
    novaCidade(input.value, ano)
})
aleatoria.addEventListener('click', () => {
    let ano = document.getElementById("dropAno").value
    novaCidade(cidadeAleatoria(), ano)
})

function novaCidade(cidade, ano){
    document.getElementById("seletorCidade").value = cidade
    fazGraficoQntQuedasCidade(document.getElementById('chartQuedasPorCidade'), quedas, cidade, ano)
    fazGraficoTempoQuedasCidade(document.getElementById('chartTempoPorCidade'), quedas, cidade, ano)

    fazGraficoEstatisticas(document.getElementById("statsCid"), filtraCidade(quedas, cidade), ano)
    fazGraficoQntPorTempo(document.getElementById("temposCid"), filtraCidade(quedas, cidade), ano)
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
let txtUlt = ""
let tempoUltima = segundosParaDuration(quedas[0].tempoFora).round("minutes")
if(quedas[0].tempoFora > 0){
    txtUlt = '(' + quedas[0].nomeCidade + " por "
            + tempoUltima.toLocaleString('pt') + ')'
}else{
    txtUlt = '(' + quedas[0].nomeCidade + " atualmente DOWN)"
}
document.getElementById("ultima").innerText = txtUlt
timer()
setInterval(timer, 1000)

function pad(num){return num <= 9 ? "0" + num : "" + num;}

function fazGraficoEstatisticas(elem, quedas, ano){
    const todosTempos = quedas.filter(q => q.data.substring(0,4) == ano).map(q => q.tempoFora)

    const media = Math.floor(ss.mean(todosTempos)/60)
    const mediana = Math.floor(ss.median(todosTempos)/60)
    const desvio = Math.floor(ss.standardDeviation(todosTempos)/60)
    const moda = Math.floor(ss.mode(todosTempos)/60)

    let option = {
      color: '#00adb5',
      title: {
        top: 30,
        left: 'center',
        text: "Duração das Quedas " + ano + "\n(minutos)"
      },
      tooltip: {
        formatter: function (params) {
          let data = segundosParaDuration(params.data * 60)
          return data.toLocaleString('pt')
        }
      },
      yAxis: {
        type: 'category',
        data: ['Desvio Padrão', 'Média', 'Mediana', 'Moda'],
        axisLabel: {show: false}
      },
      xAxis: {
        type: 'value'
      },
      series: [
        {
          data: [desvio, media, mediana, moda],
          type: 'bar',
          itemStyle: {
            borderRadius: [0, 5, 5, 0],
          },
          label: {
            position: 'insideLeft',
            formatter: '{b}',
            fontSize: 12,
            show: true
          }
        }
      ]
    };

    const chart = echarts.init(elem)
    chart.clear()
    chart.setOption(option)
}

function fazGraficoQntPorTempo(elem, quedas, ano){
    const todosTempos = quedas.filter(q => q.data.substring(0,4) == ano).map(q => Math.floor(q.tempoFora/60))

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
        top: 10,
        left: 'center',
        text: "Quantidade de Quedas\npor Duração " + ano
      },
      tooltip: {
        formatter: function (params) {
          return params.data + " quedas"
        }
      },
      xAxis: {
        type: 'category',
        data: ['Até 10min', 'Até 1h', 'Até 2h', 'Até 4h', 'Mais de 4h'],
        axisLabel: {
            fontSize: 11,
            rotate: 75
        }
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          data: tempos,
          type: 'bar',
          itemStyle: {
            borderRadius: [5, 5, 0, 0],
          },
        }
      ]
    };

    const chart = echarts.init(elem)
    chart.clear()
    chart.setOption(option)
}

function novoAno(){
    let dropAno = document.getElementById("dropAno");
    let ano = dropAno.value

    fazGraficoQntQuedasTodas(document.getElementById('chartQuedasPorDia'), quedas, ano)

    fazGraficoEstatisticas(document.getElementById("stats"), quedas, ano)
    fazGraficoQntPorTempo(document.getElementById("tempos"), quedas, ano)

    novaCidade(document.getElementById("seletorCidade").value, ano)
}

let dropAno = document.getElementById("dropAno");
dropAno.addEventListener('change', novoAno)

let anoAtual = new Date().getFullYear()
dropAno.value = anoAtual

let total = document.getElementById("total")
total.innerText = "Total de quedas no ano: " + quedas.filter(q => q.data.substring(0,4) == anoAtual).length

fazGraficoQntQuedasTodas(document.getElementById('chartQuedasPorDia'), quedas, anoAtual)

fazGraficoEstatisticas(document.getElementById("stats"), quedas, anoAtual)
fazGraficoQntPorTempo(document.getElementById("tempos"), quedas, anoAtual)

window.addEventListener('load', () => novaCidade(cidadeAleatoria(), anoAtual))
