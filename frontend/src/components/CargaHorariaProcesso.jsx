import { useEffect, useState } from 'react'
import './CargaHorariaProcesso.css'
import { formatarData, formatarDataHora } from '../utils/apresentacao'

const rotulosSituacao = {
  PENDENTE: 'Aguardando avaliação',
  APROVADO: 'Aprovado',
  REJEITADO: 'Rejeitado'
}

async function mensagemDaResposta(resposta, padrao) {
  try {
    const dados = await resposta.json()
    return dados.mensagem || padrao
  } catch {
    return padrao
  }
}

function formatarMinutos(total) {
  const minutos = Number(total) || 0
  const horas = Math.floor(minutos / 60)
  const restante = minutos % 60
  if (horas === 0) return `${restante} min`
  if (restante === 0) return `${horas} h`
  return `${horas} h ${restante} min`
}

export default function CargaHorariaProcesso({ apiBase, token, processoId, perfil }) {
  const [registros, setRegistros] = useState([])
  const [resumo, setResumo] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [processando, setProcessando] = useState(false)
  const [erro, setErro] = useState('')
  const [mensagem, setMensagem] = useState('')
  const [dataAtividade, setDataAtividade] = useState('')
  const [duracaoMinutos, setDuracaoMinutos] = useState('')
  const [atividade, setAtividade] = useState('')
  const [registroRejeitado, setRegistroRejeitado] = useState(null)
  const [justificativa, setJustificativa] = useState('')

  async function carregar() {
    setCarregando(true)
    setErro('')
    try {
      const cabecalhos = { Authorization: `Bearer ${token}` }
      const base = `${apiBase}/api/processos/${processoId}/cargas-horarias`
      const [respostaRegistros, respostaResumo] = await Promise.all([
        fetch(base, { headers: cabecalhos }),
        fetch(`${base}/resumo`, { headers: cabecalhos })
      ])
      if (!respostaRegistros.ok) {
        throw new Error(await mensagemDaResposta(respostaRegistros, 'Não foi possível carregar os registros.'))
      }
      if (!respostaResumo.ok) {
        throw new Error(await mensagemDaResposta(respostaResumo, 'Não foi possível carregar os totais.'))
      }
      setRegistros(await respostaRegistros.json())
      setResumo(await respostaResumo.json())
    } catch (falha) {
      setErro(falha.message || 'Não foi possível conectar com o servidor.')
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => {
    let ativo = true
    const cabecalhos = { Authorization: `Bearer ${token}` }
    const base = `${apiBase}/api/processos/${processoId}/cargas-horarias`

    Promise.all([
      fetch(base, { headers: cabecalhos }),
      fetch(`${base}/resumo`, { headers: cabecalhos })
    ])
      .then(async ([respostaRegistros, respostaResumo]) => {
        if (!respostaRegistros.ok) {
          throw new Error(await mensagemDaResposta(respostaRegistros, 'Não foi possível carregar os registros.'))
        }
        if (!respostaResumo.ok) {
          throw new Error(await mensagemDaResposta(respostaResumo, 'Não foi possível carregar os totais.'))
        }
        return Promise.all([respostaRegistros.json(), respostaResumo.json()])
      })
      .then(([novosRegistros, novoResumo]) => {
        if (!ativo) return
        setRegistros(novosRegistros)
        setResumo(novoResumo)
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
    const minutos = Number(duracaoMinutos)
    if (!dataAtividade || !atividade.trim() || !Number.isInteger(minutos) || minutos <= 0) {
      setErro('Informe data, atividade e duração inteira maior que zero.')
      return
    }
    setProcessando(true)
    setErro('')
    setMensagem('')
    try {
      const resposta = await fetch(
        `${apiBase}/api/processos/${processoId}/cargas-horarias`,
        {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            dataAtividade,
            duracaoMinutos: minutos,
            atividade: atividade.trim()
          })
        }
      )
      if (!resposta.ok) {
        throw new Error(await mensagemDaResposta(resposta, 'Não foi possível registrar a atividade.'))
      }
      setDataAtividade('')
      setDuracaoMinutos('')
      setAtividade('')
      setMensagem('Atividade registrada e enviada para avaliação.')
      await carregar()
    } catch (falha) {
      setErro(falha.message || 'Não foi possível conectar com o servidor.')
    } finally {
      setProcessando(false)
    }
  }

  async function avaliar(registroId, resultado) {
    if (resultado === 'REJEITADO' && !justificativa.trim()) {
      setErro('Informe a justificativa da rejeição.')
      return
    }
    setProcessando(true)
    setErro('')
    setMensagem('')
    try {
      const resposta = await fetch(
        `${apiBase}/api/processos/${processoId}/cargas-horarias/${registroId}/avaliacao`,
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
        throw new Error(await mensagemDaResposta(resposta, 'Não foi possível avaliar o registro.'))
      }
      setRegistroRejeitado(null)
      setJustificativa('')
      setMensagem(resultado === 'APROVADO' ? 'Registro aprovado.' : 'Registro rejeitado.')
      await carregar()
    } catch (falha) {
      setErro(falha.message || 'Não foi possível conectar com o servidor.')
    } finally {
      setProcessando(false)
    }
  }

  return (
    <section className="carga-horaria" aria-live="polite">
      <header className="carga-cabecalho">
        <div>
          <h4>Carga horária do estágio</h4>
          <p>Atividades realizadas e horas validadas pelo supervisor.</p>
        </div>
      </header>

      {erro && <p className="mensagem-erro" role="alert">{erro}</p>}
      {mensagem && <p className="mensagem-sucesso" role="status">{mensagem}</p>}

      {carregando ? (
        <p>Carregando registros de carga horária...</p>
      ) : (
        <>
          {resumo && (
            <div className="carga-totais" aria-label="Totais de carga horária">
              <div><strong>{formatarMinutos(resumo.totalRegistradoMinutos)}</strong><span>Registrado</span></div>
              <div><strong>{formatarMinutos(resumo.totalAprovadoMinutos)}</strong><span>Validado</span></div>
              <div><strong>{formatarMinutos(resumo.totalPendenteMinutos)}</strong><span>Pendente</span></div>
              <div><strong>{formatarMinutos(resumo.totalRejeitadoMinutos)}</strong><span>Rejeitado</span></div>
            </div>
          )}

          {perfil === 'ALUNO' && (
            <form className="carga-formulario" onSubmit={cadastrar}>
              <div>
                <label htmlFor={`data-carga-${processoId}`}>Data da atividade</label>
                <input
                  id={`data-carga-${processoId}`}
                  type="date"
                  value={dataAtividade}
                  onChange={(event) => setDataAtividade(event.target.value)}
                  disabled={processando}
                  required
                />
              </div>
              <div>
                <label htmlFor={`duracao-carga-${processoId}`}>Duração em minutos</label>
                <input
                  id={`duracao-carga-${processoId}`}
                  type="number"
                  min="1"
                  step="1"
                  value={duracaoMinutos}
                  onChange={(event) => setDuracaoMinutos(event.target.value)}
                  disabled={processando}
                  required
                />
              </div>
              <div className="carga-atividade-campo">
                <label htmlFor={`atividade-carga-${processoId}`}>Atividade realizada</label>
                <textarea
                  id={`atividade-carga-${processoId}`}
                  rows={3}
                  maxLength={1000}
                  value={atividade}
                  onChange={(event) => setAtividade(event.target.value)}
                  disabled={processando}
                  required
                />
              </div>
              <button type="submit" disabled={processando}>
                {processando ? 'Registrando...' : 'Registrar atividade'}
              </button>
            </form>
          )}

          {registros.length === 0 ? (
            <p className="carga-vazia">Nenhuma atividade registrada para este processo.</p>
          ) : (
            <div className="carga-lista">
              {registros.map((registro) => (
                <article className="carga-item" key={registro.id}>
                  <div className="carga-item-cabecalho">
                    <strong>{formatarData(registro.dataAtividade)}</strong>
                    <span className={`carga-situacao carga-${registro.situacao}`}>
                      {rotulosSituacao[registro.situacao] || registro.situacao}
                    </span>
                  </div>
                  <p>{registro.atividade}</p>
                  <div className="carga-metadados">
                    <span>{formatarMinutos(registro.duracaoMinutos)}</span>
                    <span>Registrado por {registro.registradoPor}</span>
                    {registro.avaliadoEm && (
                      <span>
                        Avaliado em {formatarDataHora(registro.avaliadoEm)}
                        {registro.avaliadoPor ? ` por ${registro.avaliadoPor}` : ''}
                      </span>
                    )}
                  </div>
                  {registro.justificativaRejeicao && (
                    <p className="carga-justificativa">
                      <strong>Justificativa:</strong> {registro.justificativaRejeicao}
                    </p>
                  )}

                  {perfil === 'EMPRESA' && registro.situacao === 'PENDENTE' && (
                    <div className="carga-avaliacao">
                      <div className="carga-acoes">
                        <button
                          type="button"
                          disabled={processando}
                          onClick={() => avaliar(registro.id, 'APROVADO')}
                        >
                          Aprovar
                        </button>
                        <button
                          type="button"
                          className="carga-rejeitar"
                          disabled={processando}
                          onClick={() => {
                            setRegistroRejeitado(registro.id)
                            setJustificativa('')
                          }}
                        >
                          Rejeitar
                        </button>
                      </div>
                      {registroRejeitado === registro.id && (
                        <div className="carga-rejeicao">
                          <label htmlFor={`justificativa-carga-${registro.id}`}>Justificativa da rejeição</label>
                          <textarea
                            id={`justificativa-carga-${registro.id}`}
                            rows={3}
                            maxLength={1000}
                            value={justificativa}
                            onChange={(event) => setJustificativa(event.target.value)}
                            disabled={processando}
                            autoFocus
                          />
                          <div className="carga-acoes">
                            <button
                              type="button"
                              className="botao-secundario"
                              disabled={processando}
                              onClick={() => {
                                setRegistroRejeitado(null)
                                setJustificativa('')
                              }}
                            >
                              Cancelar
                            </button>
                            <button
                              type="button"
                              disabled={processando}
                              onClick={() => avaliar(registro.id, 'REJEITADO')}
                            >
                              Confirmar rejeição
                            </button>
                          </div>
                        </div>
                      )}
                    </div>
                  )}
                </article>
              ))}
            </div>
          )}
        </>
      )}
    </section>
  )
}
