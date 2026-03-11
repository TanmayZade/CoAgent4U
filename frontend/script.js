/* ==========================================
   CoAgent4U — Scripts
   ========================================== */

// ---- Particle Background ----
(function initParticles() {
    const canvas = document.getElementById('particle-canvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    let particles = [];
    let mouse = { x: null, y: null };

    function resize() {
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;
    }
    resize();
    window.addEventListener('resize', resize);

    window.addEventListener('mousemove', (e) => {
        mouse.x = e.clientX;
        mouse.y = e.clientY;
    });

    class Particle {
        constructor() {
            this.reset();
        }

        reset() {
            this.x = Math.random() * canvas.width;
            this.y = Math.random() * canvas.height;
            this.vx = (Math.random() - 0.5) * 0.4;
            this.vy = (Math.random() - 0.5) * 0.4;
            this.radius = Math.random() * 1.8 + 0.5;
            this.opacity = Math.random() * 0.5 + 0.1;
            this.hue = 240 + Math.random() * 60; // indigo-purple range
        }

        update() {
            this.x += this.vx;
            this.y += this.vy;

            // Mouse interaction
            if (mouse.x !== null) {
                const dx = this.x - mouse.x;
                const dy = this.y - mouse.y;
                const dist = Math.sqrt(dx * dx + dy * dy);
                if (dist < 150) {
                    const force = (150 - dist) / 150;
                    this.vx += (dx / dist) * force * 0.02;
                    this.vy += (dy / dist) * force * 0.02;
                }
            }

            // Damping
            this.vx *= 0.999;
            this.vy *= 0.999;

            // Wrap
            if (this.x < 0) this.x = canvas.width;
            if (this.x > canvas.width) this.x = 0;
            if (this.y < 0) this.y = canvas.height;
            if (this.y > canvas.height) this.y = 0;
        }

        draw() {
            ctx.beginPath();
            ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2);
            ctx.fillStyle = `hsla(${this.hue}, 80%, 70%, ${this.opacity})`;
            ctx.fill();
        }
    }

    const count = Math.min(120, Math.floor((canvas.width * canvas.height) / 12000));
    for (let i = 0; i < count; i++) {
        particles.push(new Particle());
    }

    function drawConnections() {
        for (let i = 0; i < particles.length; i++) {
            for (let j = i + 1; j < particles.length; j++) {
                const dx = particles[i].x - particles[j].x;
                const dy = particles[i].y - particles[j].y;
                const dist = Math.sqrt(dx * dx + dy * dy);
                if (dist < 120) {
                    const opacity = (1 - dist / 120) * 0.12;
                    ctx.beginPath();
                    ctx.moveTo(particles[i].x, particles[i].y);
                    ctx.lineTo(particles[j].x, particles[j].y);
                    ctx.strokeStyle = `rgba(99, 102, 241, ${opacity})`;
                    ctx.lineWidth = 0.6;
                    ctx.stroke();
                }
            }
        }
    }

    function animate() {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        particles.forEach(p => {
            p.update();
            p.draw();
        });
        drawConnections();
        requestAnimationFrame(animate);
    }

    animate();
})();


// ---- Terminal Typing Animation ----
(function initTerminal() {
    const commandEl = document.getElementById('typed-command');
    const outputEl = document.getElementById('terminal-output');
    const cursorEl = document.getElementById('cursor');
    if (!commandEl || !outputEl) return;

    const sequences = [
        {
            command: 'coagent init --user tanmay',
            output: [
                { text: '⚡ Initializing your personal agent...', cls: '' },
                { text: '✓ Connected to Slack workspace', cls: 'success' },
                { text: '✓ Calendar sync enabled', cls: 'success' },
                { text: '✓ Approval workflows configured', cls: 'success' },
                { text: '🤖 Agent ready! Awaiting your commands.', cls: 'highlight' },
            ]
        },
        {
            command: 'coagent schedule "Team standup" --daily 9am',
            output: [
                { text: '📅 Checking calendars for conflicts...', cls: '' },
                { text: '✓ No conflicts found for all 5 participants', cls: 'success' },
                { text: '✓ Meeting scheduled: Mon-Fri 9:00 AM IST', cls: 'success' },
                { text: '📨 Invites sent via Slack', cls: 'highlight' },
            ]
        },
        {
            command: 'coagent approve --request "Cloud budget increase"',
            output: [
                { text: '🔍 Routing approval to manager chain...', cls: '' },
                { text: '→ L1: @alice (direct manager)', cls: 'accent' },
                { text: '→ L2: @bob (director)', cls: 'accent' },
                { text: '✓ Approval request submitted', cls: 'success' },
                { text: '⏳ Tracking approval progress...', cls: 'highlight' },
            ]
        },
    ];

    let seqIndex = 0;

    async function sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    async function typeCommand(text) {
        commandEl.textContent = '';
        for (let i = 0; i < text.length; i++) {
            commandEl.textContent += text[i];
            await sleep(35 + Math.random() * 40);
        }
    }

    async function showOutput(lines) {
        outputEl.innerHTML = '';
        for (const line of lines) {
            const span = document.createElement('span');
            span.className = `out-line ${line.cls}`;
            span.textContent = line.text;
            outputEl.appendChild(span);
            await sleep(400);
        }
    }

    async function clearTerminal() {
        outputEl.innerHTML = '';
        commandEl.textContent = '';
    }

    async function runSequence() {
        while (true) {
            const seq = sequences[seqIndex % sequences.length];
            seqIndex++;

            await typeCommand(seq.command);
            cursorEl.style.display = 'none';
            await sleep(300);
            await showOutput(seq.output);
            await sleep(3000);
            cursorEl.style.display = '';
            await clearTerminal();
            await sleep(600);
        }
    }

    // Start after page loads with a delay
    setTimeout(runSequence, 2000);
})();


// ---- Subscribe Form ----
(function initSubscribe() {
    const form = document.getElementById('subscribe-form');
    const btn = document.getElementById('subscribe-btn');
    if (!form || !btn) return;

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        const email = document.getElementById('email-input').value;
        if (!email) return;

        // Simulate success
        btn.innerHTML = '<span class="btn-text">✓ Subscribed!</span>';
        btn.classList.add('success');
        form.classList.add('success');

        setTimeout(() => {
            btn.innerHTML = '<span class="btn-text">Notify Me</span><svg class="btn-arrow" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M12 5l7 7-7 7"/></svg>';
            btn.classList.remove('success');
            form.classList.remove('success');
            document.getElementById('email-input').value = '';
        }, 3000);
    });
})();
