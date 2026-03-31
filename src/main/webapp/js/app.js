const API = "/rest";

// --- LOGIN ---
async function doLogin() {
    const user = document.getElementById('loginUser').value;
    const pass = document.getElementById('loginPwd').value;
    const msg = document.getElementById('loginMsg');

    const payload = {
        input: { username: user, password: pass }
    };

    try {
        const resp = await fetch(`${API}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const res = await resp.json();

        if (res.status === "success") {
            sessionStorage.setItem('session', JSON.stringify(res.data));
            window.location.href = "dashboard.html";
        } else {
            msg.innerText = "Credenciais inválidas.";
        }
    } catch (e) {
        msg.innerText = "Erro de conexão com o servidor.";
    }
}

// --- DASHBOARD ---
async function initDashboard() {
    const session = JSON.parse(sessionStorage.getItem('session'));
    if (!session) { window.location.href = "index.html"; return; }

    document.getElementById('welcomeText').innerText = session.username;
    const rb = document.getElementById('userRole');
    rb.innerText = session.role;
    rb.className = `badge role-${session.role.toLowerCase()}`;

    loadUsers();
}

async function loadUsers() {
    const session = JSON.parse(sessionStorage.getItem('session'));

    // Formato estrito: INPUT antes de TOKEN
    const body = {
        input: null,
        token: session
    };

    const resp = await fetch(`${API}/showusers`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    });

    const res = await resp.json();
    const tbody = document.querySelector("#userTable tbody");
    tbody.innerHTML = "";

    if (res.status === "success") {
        // A tua resposta do showusers devolve uma lista de users
        res.data.users.forEach(u => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>${u.username}</td>
                <td><span class="badge role-${u.role.toLowerCase()}">${u.role}</span></td>
                <td>--</td>
            `;
            tbody.appendChild(tr);
        });
    }
}

// --- LOGOUT ---
async function logout() {
    const session = JSON.parse(sessionStorage.getItem('session'));
    if (session) {
        await fetch(`${API}/logout`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ input: { username: session.username }, token: session })
        });
    }
    sessionStorage.clear();
    window.location.href = "index.html";
}