import { useEffect, useState } from "react";
import { api } from "./api.js";

const novo = { automovelId: "", dataInicio: "", dataFim: "" };
const hoje = (() => {
    const data = new Date();
    data.setMinutes(data.getMinutes() - data.getTimezoneOffset());
    return data.toISOString().slice(0, 10);
})();

export default function Pedidos({
    user,
    carroSelecionadoId,
    onCarroSelecionado,
}) {
    const [pedidos, setPedidos] = useState([]);
    const [carros, setCarros] = useState([]);
    const [f, setF] = useState(novo);
    const [editId, setEditId] = useState(null);
    const [erro, setErro] = useState("");
    const [mensagem, setMensagem] = useState("");
    const [pareceres, setPareceres] = useState({});
    const [creditos, setCreditos] = useState({});
    const [salvando, setSalvando] = useState(false);
    const agente = user.papel !== "CLIENTE";
    const pendentes = pedidos.filter((p) => p.status === "PENDENTE").length;
    const ativos = pedidos.filter(
        (p) => !["CANCELADO", "CONTRATADO", "REJEITADO", "REPROVADO"].includes(p.status),
    ).length;
    const carroSelecionado = carros.find(
        (carro) => String(carro.id) === String(f.automovelId),
    );
    const diariasSelecionadas = f.dataInicio && f.dataFim
        ? Math.max(0, Math.round(
            (new Date(`${f.dataFim}T00:00:00`) - new Date(`${f.dataInicio}T00:00:00`)) /
                86400000,
        ))
        : 0;
    const totalEstimado = carroSelecionado?.valorDiaria != null
        ? Number(carroSelecionado.valorDiaria) * diariasSelecionadas
        : null;
    const moeda = (valor) => Number(valor).toLocaleString("pt-BR", {
        style: "currency",
        currency: "BRL",
    });

    const carregar = () =>
        api("/pedidos")
            .then((dados) => {
                setPedidos(dados);
                setPareceres((atuais) => Object.fromEntries(
                    dados
                        .filter((p) => p.status === "EM_ANALISE")
                        .map((p) => [p.id, atuais[p.id] ?? p.parecerFinanceiro ?? ""]),
                ));
                setCreditos((atuais) => Object.fromEntries(
                    dados
                        .filter((p) => p.status === "EM_ANALISE")
                        .map((p) => [p.id, atuais[p.id] ?? p.valorCreditoAprovado?.toString() ?? ""]),
                ));
            })
            .catch((e) => setErro(e.message));
    useEffect(() => {
        carregar();
        api("/automoveis")
            .then(setCarros)
            .catch((e) => setErro(e.message));
    }, []);
    useEffect(() => {
        if (carroSelecionadoId == null) return;
        setF({ ...novo, automovelId: String(carroSelecionadoId) });
        setEditId(null);
        onCarroSelecionado?.();
    }, [carroSelecionadoId, onCarroSelecionado]);

    // Executa uma ação da API e recarrega a lista
    const acao = async (fn) => {
        try {
            setErro("");
            setMensagem("");
            await fn();
            setPedidos(await api("/pedidos"));
        } catch (e) {
            setErro(e.message);
        }
    };

    const salvar = () => {
        setSalvando(true);
        acao(async () => {
            const body = { ...f, automovelId: Number(f.automovelId) };
            await (editId
                ? api(`/pedidos/${editId}`, "PUT", body)
                : api("/pedidos", "POST", body));
            setF(novo);
            setEditId(null);
            setMensagem(editId
                ? agente ? "Pedido atualizado e devolvido à fila de análise." : "Pedido atualizado."
                : "Solicitação enviada com sucesso.");
        }).finally(() => setSalvando(false));
    };

    const editar = (p) => {
        setEditId(p.id);
        setF({
            automovelId: p.automovel.id,
            dataInicio: p.dataInicio,
            dataFim: p.dataFim,
        });
    };

    const avaliar = (p, aprovado) => {
        const parecerFinanceiro = pareceres[p.id]?.trim();
        if (!parecerFinanceiro) {
            setErro("Informe o parecer financeiro antes de avaliar o pedido.");
            return;
        }
        const creditoDigitado = aprovado && user.papel === "BANCO"
            ? creditos[p.id]?.trim()
            : "";
        if (creditoDigitado && (!Number.isFinite(Number(creditoDigitado)) || Number(creditoDigitado) <= 0)) {
            setErro("O valor do crédito deve ser maior que zero.");
            return;
        }
        acao(() =>
            api(`/pedidos/${p.id}/avaliacao`, "POST", {
                aprovado,
                valorCredito: creditoDigitado ? Number(creditoDigitado) : null,
                parecerFinanceiro,
            }),
        );
    };

    const assumir = (p) => acao(() => api(`/pedidos/${p.id}/assumir`, "POST"));

    return (
        <>
            <div className="page-heading">
                <div>
                    <h1>{agente ? "Pedidos recebidos" : "Meus pedidos"}</h1>
                    <p>
                        {agente
                            ? "Analise solicitações e acompanhe contratos."
                            : "Acompanhe suas solicitações de aluguel."}
                    </p>
                </div>
            </div>
            {erro && <p className="erro" role="alert">{erro}</p>}
            {mensagem && <p className="catalog-success" role="status">{mensagem}</p>}
            <div className="stats-grid">
                <div className="stat-card">
                    <span>
                        <i className="stat-dot" />
                        Pedidos ativos
                    </span>
                    <strong>{ativos}</strong>
                </div>
                <div className="stat-card">
                    <span>
                        <i
                            className="stat-dot"
                            style={{ background: "#e6bf58" }}
                        />
                        Aguardando análise
                    </span>
                    <strong>{pendentes}</strong>
                </div>
                <div className="stat-card">
                    <span>
                        <i
                            className="stat-dot"
                            style={{ background: "#7a9ce0" }}
                        />
                        Total de pedidos
                    </span>
                    <strong>{pedidos.length}</strong>
                </div>
            </div>
            {(!agente || editId !== null) && (
                <div className="card">
                    <h2>
                        {editId
                            ? `Modificar pedido #${editId}`
                            : "Solicitar um aluguel"}
                    </h2>
                    <form
                        className="form-grid"
                        onSubmit={(event) => {
                            event.preventDefault();
                            salvar();
                        }}
                    >
                        <select
                            value={f.automovelId}
                            required
                            onChange={(e) =>
                                setF({ ...f, automovelId: e.target.value })
                            }
                        >
                            <option value="">Automóvel...</option>
                            {carros.map((c) => (
                                <option key={c.id} value={c.id}>
                                    {c.marca} {c.modelo} {c.ano} ({c.placa})
                                    {c.valorDiaria != null
                                        ? ` · ${moeda(c.valorDiaria)}/dia`
                                        : ""}
                                </option>
                            ))}
                        </select>
                        <input
                            type="date"
                            min={hoje}
                            value={f.dataInicio}
                            required
                            onChange={(e) =>
                                setF({ ...f, dataInicio: e.target.value })
                            }
                        />
                        <input
                            type="date"
                            min={f.dataInicio || hoje}
                            value={f.dataFim}
                            required
                            onChange={(e) =>
                                setF({ ...f, dataFim: e.target.value })
                            }
                        />
                        <button type="submit" disabled={salvando}>
                            {salvando
                                ? editId ? "Salvando…" : "Enviando…"
                                : editId
                                  ? "Salvar alterações"
                                  : "Enviar pedido"}
                        </button>
                        {editId && (
                            <button
                                type="button"
                                className="secondary"
                                onClick={() => {
                                    setEditId(null);
                                    setF(novo);
                                }}
                            >
                                Cancelar
                            </button>
                        )}
                    </form>
                    {carroSelecionado?.valorDiaria != null && (
                        <p className="muted rental-daily-rate">
                            {diariasSelecionadas > 0
                                ? `${diariasSelecionadas} diária(s) · total previsto ${moeda(totalEstimado)}.`
                                : `Diária: ${moeda(carroSelecionado.valorDiaria)}.`}
                        </p>
                    )}
                    {carroSelecionado && carroSelecionado.valorDiaria == null && (
                        <p className="erro" role="alert">
                            Este automóvel não tem diária cadastrada e não pode receber pedidos.
                        </p>
                    )}
                </div>
            )}
            <div className="card">
                <h2>Solicitações recentes</h2>
                <div className="table-wrap">
                    <table>
                        <thead>
                            <tr>
                                <th>#</th>
                                {agente && <th>Cliente</th>}
                                <th>Carro</th>
                                <th>Período</th>
                                <th>Total</th>
                                <th>Status</th>
                                <th>Parecer financeiro</th>
                                <th>Crédito</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            {pedidos.map((p) => (
                                <tr key={p.id}>
                                    <td className="muted">
                                        #{String(p.id).padStart(4, "0")}
                                    </td>
                                    {agente && (
                                        <td className="row-title">
                                            {p.cliente.nome}
                                        </td>
                                    )}
                                    <td>
                                        <span className="row-title">
                                            {p.automovel.marca}{" "}
                                            {p.automovel.modelo}
                                        </span>
                                        <span
                                            className="muted"
                                            style={{ display: "block" }}
                                        >
                                            {p.automovel.ano} ·{" "}
                                            {p.automovel.placa}
                                        </span>
                                    </td>
                                    <td>
                                        {p.dataInicio}{" "}
                                        <span className="muted">→</span>{" "}
                                        {p.dataFim}
                                    </td>
                                    <td>{p.valorTotal != null ? moeda(p.valorTotal) : "—"}</td>
                                    <td>
                                        <span
                                            className={`badge badge-${String(p.status).toLowerCase()}`}
                                        >
                                            {p.status}
                                        </span>
                                    </td>
                                    <td>{p.parecerFinanceiro || "—"}</td>
                                    <td>
                                        {p.credito
                                            ? moeda(p.credito.valorFinanciado)
                                            : p.valorCreditoAprovado != null
                                                ? `${moeda(p.valorCreditoAprovado)} (aprovado)`
                                                : "-"}
                                    </td>
                                    <td>
                                        {agente && p.status === "EM_ANALISE" && (
                                            <textarea
                                                className="pedido-parecer"
                                                aria-label={`Parecer financeiro do pedido ${p.id}`}
                                                placeholder="Parecer financeiro (obrigatório)"
                                                rows={2}
                                                maxLength={1000}
                                                value={pareceres[p.id] || ""}
                                                onChange={(event) => setPareceres({ ...pareceres, [p.id]: event.target.value })}
                                            />
                                        )}
                                        {user.papel === "BANCO" && p.status === "EM_ANALISE" && (
                                            <label className="pedido-credito">
                                                Crédito concedido (opcional)
                                                <input
                                                    type="number"
                                                    min="0.01"
                                                    step="0.01"
                                                    placeholder="Valor em R$"
                                                    value={creditos[p.id] || ""}
                                                    onChange={(event) => setCreditos({ ...creditos, [p.id]: event.target.value })}
                                                />
                                            </label>
                                        )}
                                        <div className="actions">
                                            {!agente &&
                                                p.status === "PENDENTE" && (
                                                    <button
                                                        className="secondary"
                                                        onClick={() =>
                                                            editar(p)
                                                        }
                                                    >
                                                        Editar
                                                    </button>
                                                )}
                                            {!agente &&
                                                ![
                                                    "CANCELADO",
                                                    "CONTRATADO",
                                                ].includes(p.status) && (
                                                    <button
                                                        className="danger"
                                                        onClick={() =>
                                                            acao(() =>
                                                                api(
                                                                    `/pedidos/${p.id}`,
                                                                    "DELETE",
                                                                ),
                                                            )
                                                        }
                                                    >
                                                        Cancelar
                                                    </button>
                                                )}
                                            {agente &&
                                                p.status === "PENDENTE" && (
                                                    <button onClick={() => assumir(p)}>
                                                        Assumir análise
                                                    </button>
                                                )}
                                            {agente &&
                                                p.status === "EM_ANALISE" && (
                                                    <>
                                                        <button
                                                            className="secondary"
                                                            onClick={() => editar(p)}
                                                        >
                                                            Editar pedido
                                                        </button>
                                                        <button
                                                            onClick={() =>
                                                                avaliar(p, true)
                                                            }
                                                        >
                                                            Aprovar
                                                        </button>
                                                        <button
                                                            className="danger"
                                                            onClick={() => avaliar(p, false)}
                                                        >
                                                            Rejeitar
                                                        </button>
                                                    </>
                                                )}
                                            {agente &&
                                                p.status === "APROVADO" && (
                                                    <button
                                                        onClick={() =>
                                                            acao(() =>
                                                                api(
                                                                    `/pedidos/${p.id}/contrato`,
                                                                    "POST",
                                                                ),
                                                            )
                                                        }
                                                    >
                                                        Executar
                                                    </button>
                                                )}
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                    {pedidos.length === 0 && (
                        <div className="empty-state">
                            Ainda não há pedidos por aqui. Suas solicitações
                            aparecerão nesta lista.
                        </div>
                    )}
                </div>
            </div>
        </>
    );
}
