

let dropAno = document.getElementById("dropAno");
let dropMes = document.getElementById("dropMes");
let calendario = document.getElementById("calendario");
let switcher = document.getElementById("toggle-switch");

function dadosSalvos() {
    const hoje = new Date();

    // pagina padrao vai tudo para o mais recente
    if (window.location.href.indexOf("mes") <= -1 &&
        window.location.href.indexOf("dia") <= -1){
        localStorage.clear();
    }

    if(localStorage.getItem('ano') == null){
        localStorage.setItem('ano', hoje.getFullYear());
        dropAno.value = hoje.getFullYear();
    }else{
        dropAno.value = parseInt(localStorage.getItem('ano'));
    }

    if(localStorage.getItem('mes') == null){
        localStorage.setItem('mes', hoje.getMonth()+1);
        dropMes.value = hoje.getMonth()+1;
    }else{
        dropMes.value = parseInt(localStorage.getItem('mes'));
    }

    if(localStorage.getItem('dia') == null){
        localStorage.setItem('dia', calendario.value);
    }else{
        calendario.value = localStorage.getItem('dia');
    }

    if(localStorage.getItem('switcher') == null){
        localStorage.setItem('switcher', false);
        switcher.checked = false;
    }else{
        switcher.checked = (localStorage.getItem('switcher') == "true");
    }

    // entra por link para dia enquanto switcher esta em mes
    let url = window.location.pathname
    if(url.includes("dia") && switcher.checked){
        let dia = url.split('/').at(-1)

        localStorage.setItem('switcher', false)
        localStorage.setItem('dia', dia);

        switcher.checked = false;
        calendario.value = dia;
    }
}

function checksFaltaDeLuz(){
    let checks = document.getElementsByClassName("checkFaltaDeLuz");
    let paginaInputs = document.getElementsByClassName("paginaAtual");

    for(elem of checks){
        elem.innerText = (elem.getAttribute("value") == "true") ? '✅' : '❌';
    }

    for(elem of paginaInputs){
        elem.value = window.location.pathname;
    }
}

function atualizaSwitcher() {
    localStorage.setItem('switcher', switcher.checked);

    if(switcher.checked){
        outroMes()
    }else{
        outraData()
    }
}
switcher.addEventListener('change', atualizaSwitcher);

function diaHoje(){
    let data = new Date();
    let hoje = data.toISOString().substring(0,10);

    localStorage.setItem('dia', hoje);
    window.location = '/historicoQuedas/dia/' + hoje;
}

function outraData(){
    localStorage.setItem('dia', calendario.value);
    window.location = '/historicoQuedas/dia/' + calendario.value;
}
calendario.addEventListener('change', outraData)

function outroMes() {
    let inputMes = document.getElementById("dropMes");
    let inputAno = document.getElementById("dropAno");

    localStorage.setItem('mes', inputMes.value);
    localStorage.setItem('ano', inputAno.value);
    window.location = '/historicoQuedas/mes/' + inputAno.value + '/' + inputMes.value;
}
dropAno.addEventListener('change', outroMes)
dropMes.addEventListener('change', outroMes)

function setasData(valor){
    const hoje = new Date();

    if(switcher.checked){
        let mes = parseInt(localStorage.getItem('mes')) + valor;
        let ano = parseInt(localStorage.getItem('ano'));
        if(mes == 0){
            mes = 12;
            ano = ano - 1;
        }
        if(mes == 13){
            mes = 1;
            ano = ano + 1;
        }
        let anos = Array.from(document.getElementById("dropAno").getElementsByTagName("option")).map(e => parseInt(e.value))

        // se o ano for menor e na lista OK, se ano for igual confere mes
        if( (ano < hoje.getFullYear()  && anos.includes(ano)) ||
            (ano == hoje.getFullYear() && mes <= hoje.getMonth()+1) )
        {
            localStorage.setItem('mes', mes);
            localStorage.setItem('ano', ano);
            window.location = '/historicoQuedas/mes/' + ano + '/' + mes;
        }
    }else{
        let titulo = document.getElementById("titulo");
        let date = new Date(titulo.innerText + "T00:00:00.000Z");
        date.setDate(date.getDate() + valor);

        if(date <= hoje){
            localStorage.setItem('dia', date.toISOString().substring(0, 10));
            window.location = '/historicoQuedas/dia/' + date.toISOString().substring(0, 10);
        }
    }

}

function countLines(){
    let table = document.getElementById("dataTable");
    let count = document.getElementById("count");
    let tr = table.getElementsByTagName("tr");
    let arr = Array.from(tr)
    arr.reverse().pop()

    let newArr = []
    newArr = arr.filter(e => {return e.getElementsByClassName("checkFaltaDeLuz")[0].getAttribute("value") == "true"})

    count.innerHTML = "Total de Quedas: " + (arr.length)
                       + "<br>Queda de Luz: " + newArr.length
}

/*

// Não necessário se tiver o NagMap
function tempoForaDownAtual(){
    let x  = document.getElementsByClassName("linha")

    for(let i=1; i < x.length; i++){
        let dia = x[i].childNodes[3].childNodes[1].innerHTML
        let hora = x[i].childNodes[3].childNodes[4].innerHTML
        let tempoForaStr = x[i].childNodes[5].children[0].innerHTML

        if(tempoForaStr == "DOWN"){
            let now = new Date();
            let queda = new Date(dia + 'T' + hora)

            let dif = (now - queda) / 60000
            let labelTempo = x[i].getElementsByClassName("tempoForaEnquantoDown")[0]

            labelTempo.innerHTML = '(' + Math.floor(dif) + " min)"
        }
    }
}
setInterval(tempoForaDownAtual, 60000)
*/

function isToday(){
    let d = new Date()
    const loc = window.location.pathname.slice(-10)

    return loc == d.toISOString().slice(0,10) || loc == "ricoQuedas"
}

function isoToSeconds(isoDuration) {
  const regex = /PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?/;
  const matches = isoDuration.match(regex);

  const hours   = parseInt(matches[1] || 0);
  const minutes = parseInt(matches[2] || 0);
  const seconds = parseInt(matches[3] || 0);

  return (hours * 3600) + (minutes * 60) + seconds;
}

function quedasIguais(q, e){
    const msmData = q.data == e.data

    const msmUptime = (q.uptime == e.uptime) || (q.uptime <= 0 && e.uptime <= 0)

    const msmTempoFora = q.tempoFora == isoToSeconds(e.tempoFora)

    return msmData && msmUptime && msmTempoFora
}

function processaNovasQuedas(e){
    const data = JSON.parse(e.data)
    let listaNovas = []

    let news = true
    for(let eventQ of data){
        news = true
        for(let q of quedas){
            if(quedasIguais(q, eventQ)) {
                news = false
            }
        }
        if(news) listaNovas.push(eventQ)
    }

    console.log(listaNovas)

    if(listaNovas.length > 0 && isToday()){
        window.location.reload()
    }
}

function showMap(){
    if(isToday()){
        let map = document.getElementById("nagmap")
        map.style.display = ""

        let lista = document.getElementById("listaQuedas")
        lista.style.width = "60%"
    }
}

window.addEventListener('load', countLines);
window.addEventListener('load', showMap);

window.addEventListener('load', checksFaltaDeLuz);
window.addEventListener('load', dadosSalvos);
//window.addEventListener('load', tempoForaDownAtual);


