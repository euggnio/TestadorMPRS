const infoQueda =
`
    <div class="popup-content">
        <h2>Quedas</h2>
        <h4>Página de acompanhamento de quedas.</h4>
        <p>Registra informações sobre cada queda registrada pelo nagios e oferece controles sobre o chamado relacionado a cada queda.</p>

        <p class="enfase">Abre chamado no GLPI com <b>15 minutos de queda</b>, e fecha o chamado automaticamente quando a conexão é restabelecida.</p>
        <p>Ao fechar é adicionado um acompanhamento com um resumo das informações da ocorrencia.</p>
        <p>Clique no número do protocolo para registrar o protocolo, será incluído um acompanhamento no chamado automaticamente.</p>

        <br>

        <div class="gridBtns" style="grid-template-columns: 8em 25em;">
            <button style="background-color: #e10000;" title="Fechar">
                <img class="btnGLPI" src="\\close.png" alt="Fechar">
            </button>
            <p>Fecha o chamado no GLPI associado à queda</p>

            <button title="Acompanhamento">
                <img class="btnGLPI" src="\\comment.png" alt="Acompanhamento">
            </button>
            <p>Abre o histórico de acompanhamentos no chamado, onde podemos incluir novos acompanhamentos</p>

            <button title="Atribuir">
                <img class="btnGLPI" src="\\attrib.png" alt="Atribuir">
            </button>
            <p>Abre uma lista de nomes dos integrantes da infra para atribuir ao chamado</p>
        </div>

        <p><a target="_blank" href="http://wikidi.mp.rs.gov.br/index.php/Testador#Quedas">+ Informações</a></p>
    </div>
`

const infoConfig =
`
    <div class="popup-content">
        <h2>Configuração</h2>
        <h4>Página de configuração de Hosts.</h4>

        <p>Para criar novo host, deve-se incluir as seguintes informações:</p>

        <table>
            <tr>
                <td class="infoNome">
                    Nome Host
                </td>
                <td>
                    Como o host será chamado no Testador
                </td>
            </tr>
            <tr>
                <td class="infoNome">
                    Sigla
                </td>
                <td>
                    Código de 3 caracteres usado pela Ávato
                </td>
            </tr>
            <tr>
                <td class="infoNome">
                    Velocidade
                </td>
                <td>
                    Banda contratada para a unidade
                </td>
            </tr>
            <tr>
                <td class="infoNome">
                    Smoke
                </td>
                <td>
                    Nome correspondente para a unidade no <a target="_blank" href='http://linux61.mp.rs.gov.br/smokeping/'>SmokePing</a>
                </td>
            </tr>
            <tr>
                <td class="infoNome">
                    Cacti
                </td>
                <td>
                    Número do gráfico de tráfego na interface LAN da rb da Ávato (<a target="_blank" href="http://linux61.mp.rs.gov.br/cacti/graph_view.php?">cacti</a>)
                </td>
            </tr>
            <tr>
                <td class="infoNome">
                    Nagios
                </td>
                <td>
                    Nome da unidade no <a target="_blank" href='http://nagiosmpls.mp.rs.gov.br/nagios/'>nagiosmpls</a>
                </td>
            </tr>
            <tr>
                <td class="infoNome">
                    Intra
                </td>
                <td>
                    Código da página da promotoria na intranet do MP
                </td>
            </tr>
            <tr>
                <td class="infoNome">
                    IP
                </td>
                <td>
                    Endereço IPv4 da rb da Ávato que atende a promotoria
                </td>
            </tr>
        </table>

        <p>Abaixo temos a tabela com informações dos hosts já existentes.</p>
        <p>À direita de cada host estão os botões de Editar (ícone da caneta), e Apagar.</p>
        <p>Esses botões devem ser usados com cautela pois alteram as informações no banco de dados.</p>

        <p><a target="_blank" href="http://wikidi.mp.rs.gov.br/index.php/Testador#Configura.C3.A7.C3.A3o">+ Informações</a></p>

    </div>
`

const infoBanda =
`
    <div class="popup-content">
        <h2>Teste de Banda</h2>
        <h4>Verifica se as unidades estão alcançando a banda contradada.</h4>

        <p>O sistema realiza os testes automaticamente seguindo a lógica abaixo: </p>

        <ul>
            <li><b>00:00</b> – É executado um teste de banda para todos os hosts cadastrados.</li>
            <li><b>10:00</b> – Hosts que não atingiram a banda mínima esperada no teste da meia-noite são testados novamente.</li>
        </ul>

        <div class="gridBtns" style="grid-template-columns: 10em 25em;">

                <button class="exemplo" style="background-color: rgb(0, 173, 181);">Iniciar Teste</button>
                <p>Inicia o teste de todas as unidades registradas em ordem alfabética</p>
            

                <button class="exemplo">Histórico</button>
                <p>Mostra os resultados dos testes dos últimos 30 dias</p>
            

                <button class="exemplo">Filtrar</button>
                <p>Mostra apenas as unidades que falharam o último teste</p>
            
        </div>

        <p>Devido ao tráfego natural da rede das unidades durante a medição, considera-se uma margem de erro aproximada de ±12%. </p>


        <p><a target="_blank" href="http://wikidi.mp.rs.gov.br/index.php/Testador#Teste_de_Banda">+ Informações</a></p>


    </div>
`


const contentStyle = {
        background: "#fff",
        padding: "20px 30px",
        borderRadius: "8px",
        boxShadow: "0 5px 15px rgba(0,0,0,0.3)",
        textAlign: "center",
        minWidth: "250px",
        animation: "fadeIn 0.2s ease"
    }

const popupStyle = {
        position: "fixed",
        top: "0",
        left: "0",
        width: "100%",
        height: "100%",
        backgroundColor: "rgba(0,0,0,0.4)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        zIndex: "9999",
        color: "black"
    }

function showInfo(page) {
    const existing = document.getElementById("global-popup");
    if (existing) existing.remove();

    const popup = document.createElement("div");
    popup.id = "global-popup";

    switch(page) {
      case 'quedas':
        popup.innerHTML = infoQueda
        break;
      case 'config':
        popup.innerHTML = infoConfig
        break;
      case 'banda':
        popup.innerHTML = infoBanda
        break;
      default:
        popup.innerHTML = "ERRO"
    }
    Object.assign(popup.style, popupStyle);

    const content = popup.querySelector(".popup-content");
    Object.assign(content.style, contentStyle);

    document.body.appendChild(popup);

    const closePopup = () => {
        if (popup) popup.remove();
        document.removeEventListener("click", closePopup);
    };

    setTimeout(() => {
        document.addEventListener("click", closePopup);
    }, 50);

}