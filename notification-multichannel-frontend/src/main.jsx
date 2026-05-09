import React, { useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  Activity,
  BellRing,
  CheckCircle2,
  Clock3,
  Gauge,
  Mail,
  MessageSquareText,
  Radio,
  Search,
  Send,
  ShieldCheck,
  Smartphone,
  Workflow,
  XCircle
} from "lucide-react";
import "./styles.css";

const channels = [
  { value: "EMAIL", label: "Email", icon: Mail, placeholder: "cliente@empresa.com" },
  { value: "SMS", label: "SMS", icon: Smartphone, placeholder: "+55 11 99999-9999" },
  { value: "PUSH", label: "Push", icon: BellRing, placeholder: "device-token-123" }
];

const statusMap = {
  CREATED: { label: "Criada", tone: "created", icon: Clock3 },
  PROCESSED: { label: "Processada", tone: "processed", icon: CheckCircle2 },
  FAILED: { label: "Falhou", tone: "failed", icon: XCircle }
};

function getStatusMeta(status) {
  return statusMap[status] || { label: status || "Desconhecido", tone: "created", icon: Activity };
}

async function requestJson(url, options) {
  const response = await fetch(url, {
    headers: { "Content-Type": "application/json" },
    ...options
  });

  if (!response.ok) {
    let message = `${response.status} ${response.statusText}`;
    try {
      const payload = await response.json();
      message = payload.message || payload.error || message;
    } catch {
      const text = await response.text();
      message = text || message;
    }
    throw new Error(message);
  }

  return response.json();
}

function App() {
  const [channel, setChannel] = useState("EMAIL");
  const [recipient, setRecipient] = useState("");
  const [content, setContent] = useState("");
  const [lookupId, setLookupId] = useState("");
  const [lastNotification, setLastNotification] = useState(null);
  const [trackedNotification, setTrackedNotification] = useState(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState({ type: "idle", text: "API aguardando acao" });

  const activeChannel = useMemo(
    () => channels.find((item) => item.value === channel) || channels[0],
    [channel]
  );
  const ActiveChannelIcon = activeChannel.icon;
  const lastStatusMeta = getStatusMeta(lastNotification?.status);
  const LastStatusIcon = lastStatusMeta.icon;

  async function sendNotification(event) {
    event.preventDefault();
    const payload = {
      recipient: recipient.trim(),
      content: content.trim(),
      channel
    };

    if (!payload.recipient || !payload.content) {
      setMessage({ type: "error", text: "Preencha destinatario e conteudo." });
      return;
    }

    setBusy(true);
    setMessage({ type: "idle", text: "Enviando notificacao..." });

    try {
      const data = await requestJson("/v1/notifications", {
        method: "POST",
        body: JSON.stringify(payload)
      });
      setLastNotification(data);
      setTrackedNotification(data);
      setLookupId(data.id);
      setMessage({ type: "success", text: "Notificacao registrada no outbox." });
    } catch (error) {
      setMessage({ type: "error", text: `Falha ao enviar: ${error.message}` });
    } finally {
      setBusy(false);
    }
  }

  async function lookupStatus(event) {
    event.preventDefault();
    const id = lookupId.trim();

    if (!id) {
      setMessage({ type: "error", text: "Informe o UUID da notificacao." });
      return;
    }

    setBusy(true);
    setMessage({ type: "idle", text: "Consultando status..." });

    try {
      const data = await requestJson(`/v1/notifications/${encodeURIComponent(id)}`);
      setTrackedNotification(data);
      setLastNotification(data);
      setMessage({ type: "success", text: "Status atualizado." });
    } catch (error) {
      setMessage({ type: "error", text: `Falha na consulta: ${error.message}` });
    } finally {
      setBusy(false);
    }
  }

  function fillExample() {
    setChannel("EMAIL");
    setRecipient("cliente@empresa.com");
    setContent("Seu pedido foi recebido e esta em processamento.");
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">
            <Radio size={22} />
          </div>
          <div>
            <strong>NotifyHub</strong>
            <span>Multichannel Console</span>
          </div>
        </div>

        <nav className="nav-list" aria-label="Navegacao principal">
          <a href="#overview">Visao geral</a>
          <a href="#composer">Enviar</a>
          <a href="#tracking">Status</a>
          <a href="#operations">Operacao</a>
        </nav>

        <div className={`api-state ${message.type}`}>
          <span></span>
          <div>
            <strong>Backend Spring</strong>
            <small>{message.text}</small>
          </div>
        </div>
      </aside>

      <main className="main-content">
        <section id="overview" className="hero-section">
          <div className="hero-copy">
            <p>Transactional Outbox + RabbitMQ</p>
            <h1>Console operacional para notificacoes multicanal.</h1>
            <span>
              Envie mensagens por Email, SMS ou Push e acompanhe o status pelo endpoint REST do backend.
            </span>
          </div>

          <div className="metrics">
            <article>
              <Mail />
              <span>Canais</span>
              <strong>3</strong>
              <small>Email, SMS, Push</small>
            </article>
            <article>
              <Workflow />
              <span>Relay</span>
              <strong>5s</strong>
              <small>Outbox agendado</small>
            </article>
            <article>
              <LastStatusIcon />
              <span>Ultimo status</span>
              <strong>{lastNotification ? lastStatusMeta.label : "-"}</strong>
              <small>{lastNotification ? lastNotification.id.slice(0, 8) : "Sem envio nesta sessao"}</small>
            </article>
          </div>
        </section>

        <section className="flow-strip" aria-label="Fluxo da arquitetura">
          <div>
            <span>01</span>
            <strong>REST API</strong>
            <small>POST /v1/notifications</small>
          </div>
          <div>
            <span>02</span>
            <strong>Outbox</strong>
            <small>Persistencia transacional</small>
          </div>
          <div>
            <span>03</span>
            <strong>RabbitMQ</strong>
            <small>Publicacao assincrona</small>
          </div>
          <div>
            <span>04</span>
            <strong>Strategy</strong>
            <small>Canal especializado</small>
          </div>
        </section>

        <div className="workspace-grid">
          <section id="composer" className="panel">
            <div className="section-heading">
              <div>
                <p>Compositor</p>
                <h2>Nova notificacao</h2>
              </div>
              <span className="badge">HTTP 202</span>
            </div>

            <form className="form-stack" onSubmit={sendNotification}>
              <fieldset>
                <legend>Canal</legend>
                <div className="channel-grid">
                  {channels.map((item) => {
                    const Icon = item.icon;
                    return (
                      <button
                        type="button"
                        key={item.value}
                        className={channel === item.value ? "selected" : ""}
                        onClick={() => setChannel(item.value)}
                      >
                        <Icon size={18} />
                        {item.label}
                      </button>
                    );
                  })}
                </div>
              </fieldset>

              <label>
                Destinatario
                <div className="input-with-icon">
                  <ActiveChannelIcon size={18} />
                  <input
                    value={recipient}
                    onChange={(event) => setRecipient(event.target.value)}
                    placeholder={activeChannel.placeholder}
                    autoComplete="off"
                  />
                </div>
              </label>

              <label>
                Conteudo
                <textarea
                  value={content}
                  onChange={(event) => setContent(event.target.value)}
                  rows={6}
                  placeholder="Mensagem que sera enviada ao destinatario"
                />
              </label>

              <div className="actions">
                <button className="primary-button" type="submit" disabled={busy}>
                  <Send size={18} />
                  Enviar notificacao
                </button>
                <button className="secondary-button" type="button" onClick={fillExample}>
                  Exemplo
                </button>
              </div>
            </form>

            <ResultCard notification={lastNotification} />
          </section>

          <section id="tracking" className="panel">
            <div className="section-heading">
              <div>
                <p>Rastreamento</p>
                <h2>Consultar status</h2>
              </div>
            </div>

            <form className="lookup-row" onSubmit={lookupStatus}>
              <input
                value={lookupId}
                onChange={(event) => setLookupId(event.target.value)}
                placeholder="UUID da notificacao"
              />
              <button className="icon-button" type="submit" aria-label="Consultar status" disabled={busy}>
                <Search size={20} />
              </button>
            </form>

            <StatusTimeline notification={trackedNotification} />
          </section>
        </div>

        <section id="operations" className="operations-grid">
          <article>
            <Gauge />
            <strong>Controle por status</strong>
            <small>CREATED, PROCESSED e FAILED para acompanhamento direto da entrega.</small>
          </article>
          <article>
            <ShieldCheck />
            <strong>Consistencia eventual</strong>
            <small>Notificacao e outbox sao persistidos antes da publicacao.</small>
          </article>
          <article>
            <MessageSquareText />
            <strong>Canais extensíveis</strong>
            <small>Strategy Pattern preparado para novos provedores e canais.</small>
          </article>
        </section>
      </main>
    </div>
  );
}

function ResultCard({ notification }) {
  if (!notification) {
    return <div className="result-card muted">Nenhuma notificacao enviada nesta sessao.</div>;
  }

  const meta = getStatusMeta(notification.status);
  const Icon = meta.icon;

  return (
    <div className="result-card">
      <div className={`status-pill ${meta.tone}`}>
        <Icon size={16} />
        {meta.label}
      </div>
      <dl>
        <div>
          <dt>ID</dt>
          <dd>{notification.id}</dd>
        </div>
        <div>
          <dt>Canal</dt>
          <dd>{notification.channel}</dd>
        </div>
      </dl>
    </div>
  );
}

function StatusTimeline({ notification }) {
  if (!notification) {
    return (
      <div className="timeline">
        <div className="timeline-item active">
          <span></span>
          <div>
            <strong>Aguardando consulta</strong>
            <small>Use o ID retornado ao criar uma notificacao.</small>
          </div>
        </div>
      </div>
    );
  }

  const meta = getStatusMeta(notification.status);

  return (
    <div className="timeline">
      <div className="timeline-item active">
        <span></span>
        <div>
          <strong>Notificacao registrada</strong>
          <small>{notification.id}</small>
        </div>
      </div>
      <div className={`timeline-item ${meta.tone === "failed" ? "error" : "active"}`}>
        <span></span>
        <div>
          <strong>{meta.label}</strong>
          <small>Canal {notification.channel}. O processamento depende do relay, RabbitMQ e consumer.</small>
        </div>
      </div>
    </div>
  );
}

createRoot(document.getElementById("root")).render(<App />);
