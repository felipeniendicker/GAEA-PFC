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
  const [auditorias, setAuditorias] = useState({})
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
    const resposta = await fetch(
      `${apiBase}/api/admin/privacidade/solicitacoes/${id}/${acao}`,
      { method: 'POST', headers, body: JSON.stringify({ decisao }) }
    )
    if (!resposta.ok) setErro(await mensagem(resposta, 'Não foi possível registrar a decisão.'))
    else carregar()
  }

  async function carregarAuditoria(id) {
    if (auditorias[id]) {
      setAuditorias((atuais) => ({ ...atuais, [id]: null }))
      return
    }
    const resposta = await fetch(
      `${apiBase}/api/admin/privacidade/auditoria?solicitacaoId=${id}`,
      { headers }
    )
    if (!resposta.ok) {
      setErro(await mensagem(resposta, 'Não foi possível carregar a auditoria.'))
      return
    }
    setAuditorias((atuais) => ({ ...atuais, [id]: [] }))
    const eventos = await resposta.json()
    setAuditorias((atuais) => ({ ...atuais, [id]: eventos }))
  }

  return (
    <section className="formulario privacidade-admin">
      <div className="secao-cabecalho">
        <div>
          <h3>Solicitações de privacidade</h3>
          <p>O encerramento de conta possui execução técnica. Exclusão e anonimização permanecem sujeitas à definição dos dados elegíveis.</p>
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
                  {solicitacao.tipo === 'ENCERRAMENTO_CONTA' ? (
                    <button type="button" onClick={() => decidir(solicitacao.id, 'concluir')}>
                      Aprovar e executar encerramento
                    </button>
                  ) : (
                    <span className="badge badge-aviso">
                      Execução técnica indisponível; mantenha em análise ou rejeite fundamentadamente
                    </span>
                  )}
                  <button type="button" className="botao-perigo"
                    onClick={() => decidir(solicitacao.id, 'rejeitar')}>
                    Rejeitar com justificativa
                  </button>
                </div>
              )}
              {solicitacao.decisao && <p><strong>Decisão:</strong> {solicitacao.decisao}</p>}
              {solicitacao.evidenciaExecucao && (
                <p><strong>Resultado técnico:</strong> {solicitacao.evidenciaExecucao}</p>
              )}
              <button type="button" className="botao-secundario"
                onClick={() => carregarAuditoria(solicitacao.id)}>
                {auditorias[solicitacao.id] ? 'Ocultar auditoria' : 'Ver auditoria'}
              </button>
              {auditorias[solicitacao.id] && (
                <ul className="auditoria-privacidade">
                  {auditorias[solicitacao.id].map((evento, indice) => (
                    <li key={`${evento.dataHora}-${evento.acao}-${indice}`}>
                      {formatarDataHora(evento.dataHora)} · {evento.acao} · {evento.ator}
                    </li>
                  ))}
                </ul>
              )}
            </article>
          ))}
        </div>
      )}
    </section>
  )
}
