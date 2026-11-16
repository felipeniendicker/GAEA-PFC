import { useEffect, useState } from 'react'
import './PlanoAtividadesProcesso.css'

const rotulosSituacao = {
  PENDENTE: 'Aguardando avaliação do supervisor',
  APROVADO: 'Aprovado pelo supervisor',
  REJEITADO: 'Rejeitado pelo supervisor'
}

async function mensagemDaResposta(resposta, padrao) {
  try {
    const dados = await resposta.json()
    return dados.mensagem || padrao
  } catch {
    return padrao
  }
}

export default function PlanoAtividadesProcesso({ apiBase, token, processoId, perfil }) {
  const [plano, setPlano] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [mensagem, setMensagem] = useState('')
  const [atividades, setAtividades] = useState('')
  const [cargaHoraria, setCargaHoraria] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [rejeitando, setRejeitando] = useState(false)
  const [justificativa, setJustificativa] = useState('')

  useEffect(() => {
    let ativo = true
    fetch(`${apiBase}/api/processos/${processoId}/plano-atividades`, {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(async (resposta) => {
        if (resposta.status === 404) return null
        if (!resposta.ok) {
          throw new Error(await mensagemDaResposta(resposta, 'Não foi possível carregar o plano.'))
        }
        return resposta.json()
      })
      .then((dados) => {
        if (ativo) setPlano(dados)
      })
      .catch((falha) => {
        if (ativo) setErro(falha.message || 'Não foi possível conectar com o servidor.')
      })
      .finally(() => {
        if (ativo) setCarregando(false)
      })

    return () => {
      ativo = false
    }
  }, [apiBase, processoId, token])

  async function cadastrar(event) {
    event.preventDefault()
    const lista = atividades
      .split('\n')
      .map((descricao) => descricao.trim())
      .filter(Boolean)
    const carga = Number(cargaHoraria)
    if (lista.length === 0) {
      setErro('Informe ao menos uma atividade prevista.')
      return
    }
    if (!Number.isFinite(carga) || carga <= 0) {
      setErro('Informe uma carga horária planejada positiva.')
      return
    }

    setSalvando(true)
    setErro('')
    setMensagem('')
    try {
      const resposta = await fetch(
        `${apiBase}/api/processos/${processoId}/plano-atividades`,
        {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            atividades: lista.map((descricao) => ({ descricao })),
            cargaHorariaPlanejada: carga
          })
        }
      )
      if (!resposta.ok) {
        throw new Error(await mensagemDaResposta(resposta, 'Não foi possível cadastrar o plano.'))
      }
      setPlano(await resposta.json())
      setAtividades('')
      setCargaHoraria('')
      setMensagem('Plano de atividades cadastrado e enviado ao supervisor.')
    } catch (falha) {
      setErro(falha.message || 'Não foi possível conectar com o servidor.')
    } finally {
      setSalvando(false)
    }
  }

  async function avaliar(resultado) {
    if (resultado === 'REJEITADO' && !justificativa.trim()) {
      setErro('Informe a justificativa da rejeição.')
      return
    }
    setSalvando(true)
    setErro('')
    setMensagem('')
    try {
      const resposta = await fetch(
        `${apiBase}/api/processos/${processoId}/plano-atividades/avaliacao`,
        {
          method: 'PUT',
          headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            resultado,
            justificativa: resultado === 'REJEITADO' ? justificativa.trim() : null
          })
        }
      )
      if (!resposta.ok) {
        throw new Error(await mensagemDaResposta(resposta, 'Não foi possível avaliar o plano.'))
      }
      setPlano(await resposta.json())
      setRejeitando(false)
      setJustificativa('')
      setMensagem(resultado === 'APROVADO' ? 'Plano aprovado.' : 'Plano rejeitado.')
    } catch (falha) {
      setErro(falha.message || 'Não foi possível conectar com o servidor.')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <section className="plano-atividades" aria-live="polite">
      <header className="plano-cabecalho">
        <div>
          <h4>Plano de atividades</h4>
          <p>Atividades previstas e carga horária planejada para o estágio.</p>
        </div>
        {plano && (
          <span className={`plano-situacao plano-${plano.situacao}`}>
            {rotulosSituacao[plano.situacao] || plano.situacao}
          </span>
        )}
      </header>

      {erro && <p className="mensagem-erro" role="alert">{erro}</p>}
      {mensagem && <p className="mensagem-sucesso" role="status">{mensagem}</p>}

      {carregando ? (
        <p>Carregando plano de atividades...</p>
      ) : plano ? (
        <div className="plano-conteudo">
          <div className="plano-resumo">
            <strong>{plano.cargaHorariaPlanejada} horas planejadas</strong>
            <span>Cadastrado em {new Date(plano.criadoEm).toLocaleString('pt-BR')}</span>
            <span>Por {plano.criadoPor}</span>
            {plano.avaliadoEm && (
              <span>
                Avaliado em {new Date(plano.avaliadoEm).toLocaleString('pt-BR')}
                {plano.avaliadoPor ? ` por ${plano.avaliadoPor}` : ''}
              </span>
            )}
          </div>
          <ol className="plano-lista">
            {plano.atividades.map((atividade) => (
              <li key={atividade.id}>{atividade.descricao}</li>
            ))}
          </ol>
          {plano.justificativaRejeicao && (
            <p className="plano-justificativa">
              <strong>Justificativa da rejeição:</strong> {plano.justificativaRejeicao}
            </p>
          )}

          {perfil === 'EMPRESA' && plano.situacao === 'PENDENTE' && (
            <div className="plano-avaliacao">
              <div className="plano-acoes">
                <button type="button" disabled={salvando} onClick={() => avaliar('APROVADO')}>
                  {salvando ? 'Processando...' : 'Aprovar plano'}
                </button>
                <button
                  type="button"
                  className="plano-rejeitar"
                  disabled={salvando}
                  onClick={() => setRejeitando(true)}
                >
                  Rejeitar plano
                </button>
              </div>
              {rejeitando && (
                <div className="plano-rejeicao">
                  <label htmlFor={`justificativa-plano-${processoId}`}>Justificativa da rejeição</label>
                  <textarea
                    id={`justificativa-plano-${processoId}`}
                    rows={3}
                    maxLength={1000}
                    value={justificativa}
                    onChange={(event) => setJustificativa(event.target.value)}
                    disabled={salvando}
                    autoFocus
                  />
                  <div className="plano-acoes">
                    <button
                      type="button"
                      className="botao-secundario"
                      disabled={salvando}
                      onClick={() => {
                        setRejeitando(false)
                        setJustificativa('')
                      }}
                    >
                      Cancelar
                    </button>
                    <button type="button" disabled={salvando} onClick={() => avaliar('REJEITADO')}>
                      Confirmar rejeição
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      ) : perfil === 'ALUNO' ? (
        <form className="plano-formulario" onSubmit={cadastrar}>
          <label htmlFor={`atividades-plano-${processoId}`}>Atividades previstas</label>
          <textarea
            id={`atividades-plano-${processoId}`}
            rows={5}
            maxLength={5000}
            value={atividades}
            onChange={(event) => setAtividades(event.target.value)}
            placeholder="Informe uma atividade por linha"
            disabled={salvando}
            required
          />
          <label htmlFor={`carga-plano-${processoId}`}>Carga horária planejada</label>
          <input
            id={`carga-plano-${processoId}`}
            type="number"
            min="0.01"
            max="99999.99"
            step="0.01"
            value={cargaHoraria}
            onChange={(event) => setCargaHoraria(event.target.value)}
            disabled={salvando}
            required
          />
          <button type="submit" disabled={salvando}>
            {salvando ? 'Cadastrando...' : 'Cadastrar plano'}
          </button>
        </form>
      ) : (
        <p className="plano-vazio">Nenhum plano de atividades cadastrado para este processo.</p>
      )}
    </section>
  )
}
