const API = "/rest";

// Função genérica para Login e Registo
async function authAction(path) {
    const isLogin = path === 'login';
    const payload = isLogin ? {
        username: document.getElementById('loginUser').value,
        password: document.getElementById('loginPwd').value
    } : {
        username: document.getElementById('regUser').value,
        password: document.getElementById('regPwd').value,
        confirmation: document.getElementById('regConf').value
    };

    const resp = await fetch(`${API}/${path}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });

    const result = await resp.json();
    const msgField = document.getElementById(isLogin ? 'loginMsg' : 'regMsg');

    if (result.status === "success") {
        if (isLogin) {
            // result.data contém o AuthToken (com tokenID, role, etc)
            sessionStorage.setItem('session', JSON.stringify(result.data));
            window.location.href = "dashboard.html";
        } else {
            msgField.style.color = "green";
            msgField.innerText = result.data;
        }
    } else {
        msgField.style.color = "red";
        msgField.innerText = result.data; // Mensagem literal do ErrorCode
    }
}

async function loadDashboard() {
    const session = JSON.parse(sessionStorage.getItem('session'));
    if (!session) { window.location.href = "index.html"; return; }

    document.getElementById('welcomeText').innerText = `Olá, ${session.username}`;
    const roleSpan = document.getElementById('userRole');
    roleSpan.innerText = session.role;
    roleSpan.className = `badge role-${session.role.toLowerCase()}`;

    // Op3: Show Users
    const resp = await fetch(`${API}/showusers`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: session, input: null })
    });

    const result = await resp.json();
    const tbody = document.querySelector("#userTable tbody");
    tbody.innerHTML = "";

    if (result.status === "success") {
        result.data.forEach(user => {
            const tr = document.createElement("tr");

            // Lógica de botões baseada no Role da Sessão
            let actions = "";
            if (session.role === "ADMIN") {
                actions = `<button class="btn-action btn-delete" onclick="deleteAcc('${user.username}')">Apagar</button>`;
            } else if (session.role === "BO" && user.role === "USER") {
                actions = `<button class="btn-action" onclick="changePwd('${user.username}')">Reset Pwd</button>`;
            } else if (user.username === session.username) {
                actions = `<button class="btn-action" onclick="changePwd('${user.username}')">Mudar Minha Pwd</button>`;
            }

            tr.innerHTML = `
                <td>${user.username} ${user.username === session.username ? "<strong>(Eu)</strong>" : ""}</td>
                <td><span class="badge role-${user.role.toLowerCase()}">${user.role}</span></td>
                <td>${actions || "--"}</td>
            `;
            tbody.appendChild(tr);
        });
    }
}

async function deleteAcc(target) {
    if (!confirm(`Apagar ${target}?`)) return;
    const session = JSON.parse(sessionStorage.getItem('session'));

    const resp = await fetch(`${API}/deleteaccount`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: session, input: target })
    });

    const res = await resp.json();
    alert(res.data);
    loadDashboard();
}

async function logout() {
    const session = JSON.parse(sessionStorage.getItem('session'));
    await fetch(`${API}/logout`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: session, input: session.username })
    });
    sessionStorage.clear();
    window.location.href = "index.html";
}