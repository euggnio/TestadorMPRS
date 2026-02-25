async function abrirAcompanhamento(dados) {
    const texts = await getAcompanhamento(dados.dataset.id);

    // 1. Criar o overlay do popup
    const popup = document.createElement("div");
    popup.id = "global-popup";
    Object.assign(popup.style, {
        position: "fixed", top: "0", left: "0", width: "100%", height: "100%",
        backgroundColor: "rgba(0,0,0,0.7)", display: "flex", alignItems: "center",
        justifyContent: "center", zIndex: "9999", color: "black", fontFamily: "sans-serif"
    });

    // 2. Estrutura do conteúdo
    const content = document.createElement("div");
    Object.assign(content.style, {
        background: "#fff", padding: "25px", borderRadius: "12px",
        boxShadow: "0 10px 30px rgba(0,0,0,0.5)", width: "90%", maxWidth: "35%"
    });

    // Criamos o HTML base com os containers
    content.innerHTML = `
        <h3 style="margin-top:0; border-bottom: 2px solid #009300; padding-bottom:10px">Histórico de Acompanhamentos</h3>
        <div id="lista-items" style="max-height: 300px; text-align: left; overflow-y: auto; margin: 20px 0; border: 1px solid #eee; padding: 10px; borderRadius: 8px">
            <!-- Itens entram aqui -->
        </div>
        <div style="display: flex; justify-content: space-between; align-items: center; gap: 10px">
            <button id="btn-add-novo">
                <span style="font-weight: bold">+</span> Adicionar acompanhamento
            </button>
            <button id="close-popup">Fechar</button>
        </div>
    `;

    popup.appendChild(content);
    document.body.appendChild(popup);

    // 3. Preencher a lista de textos
    const listaContainer = content.querySelector("#lista-items");
    if (texts && texts.length > 0) {
        texts.forEach((item, index) => {
            const p = document.createElement("p");
            p.style.cssText = "margin: 0; padding: 10px; border-bottom: 1px dashed #ddd; font-size: 1em";
            p.innerHTML = ` ${item}`;
            listaContainer.appendChild(p);
        });
    } else {
        listaContainer.innerHTML = "<p style='text-align:center; color:#999'>Nenhum acompanhamento encontrado.</p>";
    }

    // 4. Lógica do Botão "Adicionar Novo" (Clonando os atributos do botão original)
    const btnAdd = content.querySelector("#btn-add-novo");

    // Passamos os dados do botão que clicamos para o novo botão dentro do popup
    btnAdd.dataset.id = dados.dataset.id;
    btnAdd.dataset.endpoint = '/adicionarFollowUp'; // Endpoint fixo ou vindo de dados.dataset.endpoint
    btnAdd.dataset.acao = 'Adicionar acompanhamento';

    btnAdd.onclick = function() {
        popup.remove(); // Fecha o popup atual
        abrirModal(this); // Chama sua função original de abrir o modal de escrita
    };

    content.querySelector("#close-popup").onclick = () => popup.remove();
    popup.onclick = (e) => { if(e.target === popup) popup.remove(); };
}

async function getAcompanhamento(id) {
    const url = "/getTicketFollowups/" + id;
    const res = await fetch(url);
    return await res.json();
}