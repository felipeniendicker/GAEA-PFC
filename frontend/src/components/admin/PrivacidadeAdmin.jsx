import { useCallback, useEffect, useState } from 'react'
import { formatarDataHora, rotuloSituacao } from '../../utils/apresentacao'

const rotulosTipo = {
  ACESSO: 'Acesso',
  CORRECAO: 'Correção',
  EXPORTACAO: 'Exportação',
  EXCLUSAO: 'Exclusão',
  ANONIMIZACAO: 'Anonimização',
  REVOGACAO_CONSENTIMENTO: 'Revogação de consentimento',
  ENCERRAMENTO_CONTA: 'Encerramento da conta'
}

async function mensagem(resposta, padrao) {
  try {
    const dados = await resposta.json()
    return dados.mensagem || padrao
  } catch {
    return padrao
  }
}

export default function PrivacidadeAdmin({ apiBase, token }) {
  const [solicitacoes, setSolicitacoes] = useState([])
  const [situacao, setSituacao] = useState('PENDENTE')
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(true)
  const headers = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }

  const carregar = useCallback(async () => {
    setCarregando(true)
    try {
      const resposta = await fetch(
        `${apiBase}/api/admin/privacidade/solicitacoes?situacao=${situacao}`,
        { headers }
      )
      if (!resposta.ok) throw new Error(await mensagem(resposta, 'Não foi possível carregar.'))
      setSolicitacoes(await resposta.json())
      setErro('')
    } catch (falha) {
      setErro(falha.message || 'Não foi possível carregar as solicitações de privacidade.')
    } finally {
      setCarregando(false)
    }
  }, [apiBase, token, situacao]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const agendamento = window.setTimeout(() => carregar(), 0)
    return () => window.clearTimeout(agendamento)
  }, [carregar])

  async function iniciarAnalise(id) {
    const resposta = await fetch(
      `${apiBase}/api/admin/privacidade/solicitacoes/${id}/analise`,
      { method: 'PUT', headers }
    )
    if (!resposta.ok) setErro(await mensagem(resposta, 'Não foi possível iniciar a análise.'))
    else carregar()
  }

  async function decidir(id, acao) {
    const decisao = window.prompt(
      acao === 'concluir'
        ? 'Decisão fundamentada (mínimo 10 caracteres):'
        : 'Justificativa da rejeição (mínimo 10 caracteres):'
    )
    if (!decisao) return
    const evidenciaExecucao = acao === 'concluir'
      ? window.prompt('Evidência da execução, sem incluir segredos ou dados excessivos:')
      : null
    if (acao === 'concluir' && !evidenciaExecucao) return
    const resposta = await fetch(
      `${apiBase}/api/admin/privacidade/solicitacoes/${id}/${acao}`,
      { method: 'POST', headers, body: JSON.stringify({ decisao, evidenciaExecucao }) }
    )
    if (!resposta.ok) setErro(await mensagem(resposta, 'Não foi possível registrar a decisão.'))
    else carregar()
  }

  return (
    <section className="formulario privacidade-admin">
      <div className="secao-cabecalho">
        <div>
          <h3>Solicitações de privacidade</h3>
          <p>Acesso restrito para análise; nenhuma exclusão é executada automaticamente.</p>
        </div>
        <label className="campo">
          Situação
          <select value={situacao} onChange={(event) => setSituacao(event.target.value)}>
            {['PENDENTE', 'EM_ANALISE', 'CONCLUIDA', 'REJEITADA'].map((item) => (
              <option key={item} value={item}>{rotuloSituacao(item)}</option>
            ))}
          </select>
        </label>
      </div>
      {erro && <p className="mensagem-erro" role="alert">{erro}</p>}
      {carregando ? <p>Carregando solicitações...</p> : solicitacoes.length === 0 ? (
        <p className="estado-vazio">Nenhuma solicitação nesta situação.</p>
      ) : (
        <div className="admin-lista">
          {solicitacoes.map((solicitacao) => (
            <article className="card" key={solicitacao.id}>
              <div className="secao-cabecalho">
                <div>
                  <h4>{rotulosTipo[solicitacao.tipo] || solicitacao.tipo}</h4>
                  <p>{solicitacao.titularNome} · {solicitacao.titularPerfil}</p>
                </div>
                <span className="badge">{rotuloSituacao(solicitacao.situacao)}</span>
              </div>
              <p>{solicitacao.descricao}</p>
              <p><small>Recebida em {formatarDataHora(solicitacao.criadoEm)}</small></p>
              {solicitacao.situacao === 'PENDENTE' && (
                <button type="button" onClick={() => iniciarAnalise(solicitacao.id)}>
                  Iniciar análise
                </button>
              )}
              {solicitacao.situacao === 'EM_ANALISE' && (
                <div className="processo-acoes">
                  <button type="button" onClick={() => decidir(solicitacao.id, 'concluir')}>
                    Concluir com evidência
                  </button>
                  <button type="button" className="botao-perigo"
                    onClick={() => decidir(solicitacao.id, 'rejeitar')}>
                    Rejeitar com justificativa
                  </button>
                </div>
              )}
              {solicitacao.decisao && <p><strong>Decisão:</strong> {solicitacao.decisao}</p>}
            </article>
          ))}
        </div>
      )}
    </section>
  )
}
