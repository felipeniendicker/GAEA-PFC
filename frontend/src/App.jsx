import { useEffect, useRef, useState } from 'react'
import './App.css'

const API = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '')

const rotulosStatus = {
  PENDENTE: 'Pendente',
  EM_ANALISE: 'Em análise',
  APROVADO: 'Aprovado',
  REPROVADO: 'Reprovado'
}

const rotulosVinculo = {
  PENDENTE: 'Aguardando confirmação do supervisor',
  CONFIRMADO: 'Supervisor confirmado',
  RECUSADO: 'Solicitação recusada pelo supervisor',
  CANCELADO: 'Solicitação substituída'
}

const rotulosEventoHistorico = {
  CRIACAO_PROCESSO: 'Criação do processo',
  ALTERACAO_STATUS: 'Alteração de status',
  REABERTURA: 'Reabertura',
  INDICACAO_SUPERVISOR: 'Indicação de supervisor',
  CONFIRMACAO_SUPERVISOR: 'Confirmação do supervisor',
  RECUSA_SUPERVISOR: 'Recusa do supervisor',
  SUBSTITUICAO_SUPERVISOR: 'Substituição do supervisor',
  CRIACAO_PENDENCIA: 'Registro de pendência',
  RESOLUCAO_PENDENCIA: 'Resolução de pendência',
  CRIACAO_PRAZO: 'Registro de prazo',
  ATUALIZACAO_PRAZO: 'Atualização de prazo'
}

function emailValido(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
}

function cnpjValido(cnpj) {
  const numeros = cnpj.replace(/\D/g, '')

  if (numeros.length !== 14 || /^(\d)\1+$/.test(numeros)) {
    return false
  }

  function calcularDigito(tamanho) {
    let soma = 0
    let peso = tamanho - 7

    for (let indice = 0; indice < tamanho; indice += 1) {
      soma += Number(numeros[indice]) * peso
      peso -= 1

      if (peso === 1) {
        peso = 9
      }
    }

    const resto = soma % 11
    return resto < 2 ? 0 : 11 - resto
  }

  return Number(numeros[12]) === calcularDigito(12) &&
    Number(numeros[13]) === calcularDigito(13)
}

async function mensagemDaApi(resposta, mensagemPadrao) {
  try {
    const dados = await resposta.json()
    const mensagensDosCampos = dados.campos
      ? [...new Set(Object.values(dados.campos))]
      : []

    if (mensagensDosCampos.length > 0) {
      return mensagensDosCampos.join('. ')
    }

    return dados.mensagem || mensagemPadrao
  } catch {
    return mensagemPadrao
  }
}

function HistoricoProcesso({ itens, carregando, erro }) {
  return (
    <div className="historico-processo" aria-live="polite">
      <h4>Histórico do processo</h4>

      {carregando ? (
        <p>Carregando histórico...</p>
      ) : erro ? (
        <p className="mensagem-erro">{erro}</p>
      ) : itens.length === 0 ? (
        <p>Nenhuma movimentação registrada.</p>
      ) : (
        itens.map((item) => (
          <div className="historico-item" key={item.id}>
            <div>
              <strong>{rotulosEventoHistorico[item.tipo] || item.tipo}</strong>
              <span>{item.descricao}</span>
            </div>
            <span>
              {item.responsavel
                ? `Responsável: ${item.responsavel}`
                : 'Responsável não registrado'}
            </span>
            <span>{new Date(item.dataHora).toLocaleString('pt-BR')}</span>
          </div>
        ))
      )}
    </div>
  )
}

function PendenciasProcesso({
  itens,
  carregando,
  erro,
  podeGerenciar,
  resolvendoId,
  aoNovaPendencia,
  aoResolver
}) {
  return (
    <div className="pendencias-processo" aria-live="polite">
      <div className="pendencias-cabecalho">
        <h4>Pendências do processo</h4>
        {podeGerenciar && (
          <button type="button" onClick={aoNovaPendencia}>
            Registrar pendência
          </button>
        )}
      </div>

      {carregando ? (
        <p>Carregando pendências...</p>
      ) : erro ? (
        <p className="mensagem-erro" role="alert">{erro}</p>
      ) : itens.length === 0 ? (
        <p>Nenhuma pendência registrada para este processo.</p>
      ) : (
        itens.map((item) => (
          <article className="pendencia-item" key={item.id}>
            <div className="pendencia-item-cabecalho">
              <strong>{item.descricao}</strong>
              <span className={`pendencia-situacao pendencia-${item.situacao}`}>
                {item.situacao === 'RESOLVIDA' ? 'Resolvida' : 'Aberta'}
              </span>
            </div>
            <div className="pendencia-datas">
              <span>Criada em {new Date(item.criadaEm).toLocaleString('pt-BR')}</span>
              {item.resolvidaEm && (
                <span>Resolvida em {new Date(item.resolvidaEm).toLocaleString('pt-BR')}</span>
              )}
            </div>
            {podeGerenciar && item.situacao === 'ABERTA' && (
              <button
                className="acao-resolver-pendencia"
                type="button"
                disabled={resolvendoId === item.id}
                onClick={() => aoResolver(item.id)}
              >
                {resolvendoId === item.id ? 'Resolvendo...' : 'Marcar como resolvida'}
              </button>
            )}
          </article>
        ))
      )}
    </div>
  )
}

function formatarDataPrazo(data) {
  return new Date(`${data}T00:00:00`).toLocaleDateString('pt-BR')
}

function prazoVencido(data) {
  const hoje = new Date()
  hoje.setHours(0, 0, 0, 0)
  return new Date(`${data}T00:00:00`) < hoje
}

function PrazosProcesso({
  itens,
  carregando,
  erro,
  podeGerenciar,
  aoNovoPrazo,
  aoEditarPrazo
}) {
  return (
    <div className="prazos-processo" aria-live="polite">
      <div className="prazos-cabecalho">
        <h4>Prazos do processo</h4>
        {podeGerenciar && (
          <button type="button" onClick={aoNovoPrazo}>Registrar prazo</button>
        )}
      </div>

      {carregando ? (
        <p>Carregando prazos...</p>
      ) : erro ? (
        <p className="mensagem-erro" role="alert">{erro}</p>
      ) : itens.length === 0 ? (
        <p>Nenhum prazo registrado para este processo.</p>
      ) : (
        itens.map((item) => {
          const vencido = prazoVencido(item.dataVencimento)
          return (
            <article className="prazo-item" key={item.id}>
              <div className="prazo-item-cabecalho">
                <strong>{item.descricao}</strong>
                <span className={`prazo-situacao ${vencido ? 'prazo-vencido' : 'prazo-em-dia'}`}>
                  {vencido ? 'Vencido' : 'Dentro do prazo'}
                </span>
              </div>
              <p className="prazo-vencimento">
                Vencimento: <strong>{formatarDataPrazo(item.dataVencimento)}</strong>
              </p>
              <div className="prazo-datas">
                <span>Criado em {new Date(item.criadaEm).toLocaleString('pt-BR')}</span>
                <span>Atualizado em {new Date(item.atualizadaEm).toLocaleString('pt-BR')}</span>
              </div>
              {podeGerenciar && (
                <button className="acao-editar-prazo" type="button" onClick={() => aoEditarPrazo(item)}>
                  Atualizar prazo
                </button>
              )}
            </article>
          )
        })
      )}
    </div>
  )
}

function MensagemAcaoProcesso({ mensagem, aoFechar }) {
  if (!mensagem) {
    return null
  }

  return (
    <div
      className={`mensagem-acao-processo ${mensagem.tipo}`}
      role={mensagem.tipo === 'erro' ? 'alert' : 'status'}
      aria-live={mensagem.tipo === 'erro' ? 'assertive' : 'polite'}
    >
      <span>{mensagem.texto}</span>
      <button type="button" onClick={aoFechar} aria-label="Fechar mensagem">
        ×
      </button>
    </div>
  )
}

function ModalReabertura({
  processo,
  justificativa,
  erro,
  carregando,
  aoMudarJustificativa,
  aoCancelar,
  aoConfirmar
}) {
  const modalRef = useRef(null)
  const campoRef = useRef(null)
  const focoAnteriorRef = useRef(null)

  useEffect(() => {
    focoAnteriorRef.current = document.activeElement
    const overflowAnterior = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    campoRef.current?.focus()

    return () => {
      document.body.style.overflow = overflowAnterior
      if (focoAnteriorRef.current?.isConnected) {
        focoAnteriorRef.current.focus()
      } else {
        document.querySelector(`[data-processo-id="${processo.id}"]`)?.focus()
      }
    }
  }, [processo.id])

  function lidarTecla(event) {
    if (event.key === 'Escape') {
      event.preventDefault()
      if (!carregando) {
        aoCancelar()
      }
      return
    }

    if (event.key !== 'Tab') {
      return
    }

    const elementosFocaveis = [...(modalRef.current?.querySelectorAll(
      'button:not([disabled]), textarea:not([disabled]), input:not([disabled]), [tabindex]:not([tabindex="-1"])'
    ) || [])]

    if (elementosFocaveis.length === 0) {
      return
    }

    const primeiro = elementosFocaveis[0]
    const ultimo = elementosFocaveis[elementosFocaveis.length - 1]
    const indiceAtual = elementosFocaveis.indexOf(document.activeElement)

    if (event.shiftKey && indiceAtual <= 0) {
      event.preventDefault()
      ultimo.focus()
    } else if (!event.shiftKey && (indiceAtual === -1 || document.activeElement === ultimo)) {
      event.preventDefault()
      primeiro.focus()
    }
  }

  function lidarCliqueFora(event) {
    if (event.target === event.currentTarget && !carregando) {
      aoCancelar()
    }
  }

  return (
    <div className="modal-sobreposicao" onMouseDown={lidarCliqueFora}>
      <section
        className="modal-gaea"
        ref={modalRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="titulo-modal-reabertura"
        aria-describedby="descricao-modal-reabertura"
        aria-busy={carregando}
        onKeyDown={lidarTecla}
      >
        <div className="modal-cabecalho">
          <div>
            <p className="modal-contexto">PROCESSO #{processo.id}</p>
            <h2 id="titulo-modal-reabertura">Reabrir processo de estágio</h2>
          </div>
          <button
            className="modal-fechar"
            type="button"
            onClick={aoCancelar}
            disabled={carregando}
            aria-label="Cancelar e fechar modal"
          >
            ×
          </button>
        </div>

        <p id="descricao-modal-reabertura" className="modal-descricao">
          O processo retornará para análise. A decisão anterior permanecerá registrada no histórico.
        </p>

        <div className="modal-status-atual">
          <span>Status atual</span>
          <strong className={`status status-${processo.status}`}>
            {rotulosStatus[processo.status] || processo.status}
          </strong>
        </div>

        <form onSubmit={aoConfirmar} noValidate>
          <label htmlFor="justificativaReabertura">Justificativa</label>
          <textarea
            ref={campoRef}
            id="justificativaReabertura"
            value={justificativa}
            onChange={(event) => aoMudarJustificativa(event.target.value)}
            minLength={10}
            maxLength={1000}
            rows={5}
            required
            readOnly={carregando}
            aria-invalid={Boolean(erro)}
            aria-describedby={erro ? 'erro-modal-reabertura' : 'ajuda-modal-reabertura'}
          />
          <span id="ajuda-modal-reabertura" className="modal-ajuda">
            Informe pelo menos 10 caracteres.
          </span>

          {erro && (
            <p id="erro-modal-reabertura" className="modal-erro" role="alert">
              {erro}
            </p>
          )}

          <div className="modal-acoes">
            <button
              className="modal-cancelar"
              type="button"
              onClick={aoCancelar}
              disabled={carregando}
            >
              Cancelar
            </button>
            <button className="modal-confirmar" type="submit" disabled={carregando}>
              {carregando ? 'Reabrindo...' : 'Confirmar reabertura'}
            </button>
          </div>
        </form>
      </section>
    </div>
  )
}

function ModalNovaPendencia({
  processo,
  descricao,
  erro,
  carregando,
  aoMudarDescricao,
  aoCancelar,
  aoConfirmar
}) {
  const modalRef = useRef(null)
  const campoRef = useRef(null)
  const focoAnteriorRef = useRef(null)

  useEffect(() => {
    focoAnteriorRef.current = document.activeElement
    const overflowAnterior = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    campoRef.current?.focus()

    return () => {
      document.body.style.overflow = overflowAnterior
      focoAnteriorRef.current?.focus()
    }
  }, [])

  function lidarTecla(event) {
    if (event.key === 'Escape' && !carregando) {
      event.preventDefault()
      aoCancelar()
      return
    }

    if (event.key !== 'Tab') {
      return
    }

    const elementos = [...(modalRef.current?.querySelectorAll(
      'button:not([disabled]), textarea:not([disabled])'
    ) || [])]
    const primeiro = elementos[0]
    const ultimo = elementos[elementos.length - 1]

    if (event.shiftKey && document.activeElement === primeiro) {
      event.preventDefault()
      ultimo?.focus()
    } else if (!event.shiftKey && document.activeElement === ultimo) {
      event.preventDefault()
      primeiro?.focus()
    }
  }

  return (
    <div
      className="modal-sobreposicao"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !carregando) {
          aoCancelar()
        }
      }}
    >
      <section
        className="modal-gaea"
        ref={modalRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="titulo-modal-pendencia"
        aria-busy={carregando}
        onKeyDown={lidarTecla}
      >
        <div className="modal-cabecalho">
          <div>
            <p className="modal-contexto">PROCESSO #{processo.id}</p>
            <h2 id="titulo-modal-pendencia">Registrar pendência</h2>
          </div>
          <button
            className="modal-fechar"
            type="button"
            onClick={aoCancelar}
            disabled={carregando}
            aria-label="Cancelar e fechar modal"
          >
            ×
          </button>
        </div>

        <p className="modal-descricao">
          Descreva objetivamente o que precisa ser regularizado neste processo.
        </p>

        <form onSubmit={aoConfirmar} noValidate>
          <label htmlFor="descricaoPendencia">Descrição</label>
          <textarea
            ref={campoRef}
            id="descricaoPendencia"
            value={descricao}
            onChange={(event) => aoMudarDescricao(event.target.value)}
            maxLength={1000}
            rows={5}
            required
            readOnly={carregando}
            aria-invalid={Boolean(erro)}
            aria-describedby={erro ? 'erro-modal-pendencia' : undefined}
          />
          {erro && (
            <p id="erro-modal-pendencia" className="modal-erro" role="alert">
              {erro}
            </p>
          )}

          <div className="modal-acoes">
            <button className="modal-cancelar" type="button" onClick={aoCancelar} disabled={carregando}>
              Cancelar
            </button>
            <button className="modal-confirmar" type="submit" disabled={carregando}>
              {carregando ? 'Registrando...' : 'Registrar pendência'}
            </button>
          </div>
        </form>
      </section>
    </div>
  )
}

function ModalPrazo({
  processo,
  prazo,
  descricao,
  dataVencimento,
  erro,
  carregando,
  aoMudarDescricao,
  aoMudarData,
  aoCancelar,
  aoConfirmar
}) {
  const modalRef = useRef(null)
  const campoRef = useRef(null)
  const focoAnteriorRef = useRef(null)

  useEffect(() => {
    focoAnteriorRef.current = document.activeElement
    campoRef.current?.focus()
    return () => focoAnteriorRef.current?.focus()
  }, [])

  function lidarTecla(event) {
    if (event.key === 'Escape' && !carregando) {
      event.preventDefault()
      aoCancelar()
    }
  }

  return (
    <div className="modal-sobreposicao">
      <section
        className="modal-gaea"
        ref={modalRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="titulo-modal-prazo"
        aria-busy={carregando}
        onKeyDown={lidarTecla}
      >
        <div className="modal-cabecalho">
          <div>
            <p className="modal-contexto">PROCESSO #{processo.id}</p>
            <h2 id="titulo-modal-prazo">{prazo ? 'Atualizar prazo' : 'Registrar prazo'}</h2>
          </div>
          <button
            className="modal-fechar"
            type="button"
            onClick={aoCancelar}
            disabled={carregando}
            aria-label="Cancelar e fechar modal"
          >
            ×
          </button>
        </div>

        <form onSubmit={aoConfirmar} noValidate>
          <label htmlFor="descricaoPrazo">Descrição</label>
          <textarea
            id="descricaoPrazo"
            ref={campoRef}
            value={descricao}
            onChange={(event) => aoMudarDescricao(event.target.value)}
            maxLength={1000}
            rows={4}
            required
            readOnly={carregando}
          />

          <label htmlFor="dataVencimentoPrazo">Data de vencimento</label>
          <input
            id="dataVencimentoPrazo"
            type="date"
            value={dataVencimento}
            onChange={(event) => aoMudarData(event.target.value)}
            required
            disabled={carregando}
          />

          {erro && <p className="modal-erro" role="alert">{erro}</p>}

          <div className="modal-acoes">
            <button className="modal-cancelar" type="button" onClick={aoCancelar} disabled={carregando}>
              Cancelar
            </button>
            <button className="modal-confirmar" type="submit" disabled={carregando}>
              {carregando ? 'Salvando...' : prazo ? 'Atualizar prazo' : 'Registrar prazo'}
            </button>
          </div>
        </form>
      </section>
    </div>
  )
}

function DashboardProcessos({ resumo, carregando, erro, aoTentarNovamente }) {
  if (carregando) {
    return (
      <section className="dashboard-estado" aria-live="polite">
        <div className="dashboard-carregando" aria-hidden="true" />
        <p>Carregando indicadores institucionais...</p>
      </section>
    )
  }

  if (erro) {
    return (
      <section className="dashboard-estado dashboard-erro" role="alert">
        <h3>Não foi possível carregar o dashboard</h3>
        <p>{erro}</p>
        <button type="button" onClick={aoTentarNovamente}>Tentar novamente</button>
      </section>
    )
  }

  if (!resumo) {
    return null
  }

  const indicadores = [
    { chave: 'pendentes', rotulo: 'Pendentes', valor: resumo.pendentes, classe: 'pendente' },
    { chave: 'emAnalise', rotulo: 'Em análise', valor: resumo.emAnalise, classe: 'analise' },
    { chave: 'aprovados', rotulo: 'Aprovados', valor: resumo.aprovados, classe: 'aprovado' },
    { chave: 'reprovados', rotulo: 'Reprovados', valor: resumo.reprovados, classe: 'reprovado' }
  ]
  const percentualAprovacao = Number(resumo.percentualAprovacao)

  return (
    <>
      {resumo.totalProcessos === 0 && (
        <div className="dashboard-vazio">
          <strong>Nenhum processo cadastrado.</strong>
          <span>Os indicadores serão exibidos conforme os processos forem registrados.</span>
        </div>
      )}

      <section className="dashboard-cards" aria-label="Indicadores dos processos">
        <article className="dashboard-card total">
          <span>Total de processos</span>
          <strong>{resumo.totalProcessos}</strong>
        </article>
        {indicadores.map((indicador) => (
          <article className={`dashboard-card ${indicador.classe}`} key={indicador.chave}>
            <span>{indicador.rotulo}</span>
            <strong>{indicador.valor}</strong>
          </article>
        ))}
      </section>

      <section className="dashboard-detalhes">
        <article className="dashboard-grafico">
          <div className="secao-cabecalho">
            <h3>Distribuição por status</h3>
            <p>Quantidade atual de processos em cada etapa.</p>
          </div>

          {resumo.totalProcessos === 0 ? (
            <p className="estado-vazio">Não há dados para apresentar no gráfico.</p>
          ) : (
            <div className="grafico-barras">
              {indicadores.map((indicador) => {
                const largura = (indicador.valor / resumo.totalProcessos) * 100
                return (
                  <div className="grafico-linha" key={indicador.chave}>
                    <div className="grafico-rotulo">
                      <span>{indicador.rotulo}</span>
                      <strong>{indicador.valor}</strong>
                    </div>
                    <div className="grafico-trilho" aria-hidden="true">
                      <div
                        className={`grafico-barra ${indicador.classe}`}
                        style={{ width: `${largura}%` }}
                      />
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </article>

        <article className="dashboard-percentual">
          <span>Percentual de aprovação</span>
          <strong>
            {percentualAprovacao.toLocaleString('pt-BR', {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2
            })}%
          </strong>
          <p>Calculado somente entre processos aprovados e reprovados.</p>
        </article>
      </section>
    </>
  )
}

const rotulosSituacaoAtivacao = {
  ATIVADO: 'Conta ativada',
  AGUARDANDO_ATIVACAO: 'Aguardando ativação',
  CONVITE_EXPIRADO: 'Convite expirado',
  CONVITE_CANCELADO: 'Convite substituído',
  SEM_CONVITE: 'Sem convite',
  DESATIVADO: 'Aluno desativado'
}

function GestaoAlunos({ token }) {
  const [alunos, setAlunos] = useState([])
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [matricula, setMatricula] = useState('')
  const [erro, setErro] = useState('')
  const [mensagem, setMensagem] = useState('')
  const [carregando, setCarregando] = useState(true)
  const [acaoEmAndamento, setAcaoEmAndamento] = useState('')
  const [edicao, setEdicao] = useState(null)
  const [convite, setConvite] = useState(null)

  useEffect(() => {
    const controle = new AbortController()

    async function carregarAlunos() {
      try {
        const resposta = await fetch(`${API}/api/instituicoes/alunos`, {
          headers: { Authorization: `Bearer ${token}` },
          signal: controle.signal
        })

        if (!resposta.ok) {
          setErro(await mensagemDaApi(resposta, 'Não foi possível carregar os alunos.'))
          return
        }

        setAlunos(await resposta.json())
      } catch (erroRequisicao) {
        if (erroRequisicao.name !== 'AbortError') {
          setErro('Não foi possível conectar com o servidor.')
        }
      } finally {
        if (!controle.signal.aborted) {
          setCarregando(false)
        }
      }
    }

    carregarAlunos()
    return () => controle.abort()
  }, [token])

  function linkDoConvite(tokenAtivacao) {
    const url = new URL(window.location.href)
    url.search = ''
    url.hash = `conviteAluno=${encodeURIComponent(tokenAtivacao)}`
    return url.toString()
  }

  function apresentarConvite(dados) {
    setConvite({
      aluno: dados.aluno,
      link: linkDoConvite(dados.tokenAtivacao),
      expiraEm: dados.expiraEm
    })
  }

  async function cadastrarAluno(event) {
    event.preventDefault()
    setErro('')
    setMensagem('')

    const nomeNormalizado = nome.trim()
    const emailNormalizado = email.trim().toLowerCase()
    if (!nomeNormalizado) {
      setErro('Informe o nome completo do aluno.')
      return
    }
    if (!emailValido(emailNormalizado)) {
      setErro('Informe um e-mail válido.')
      return
    }
    if (!matricula.trim()) {
      setErro('Informe a matrícula (RA/RGM) do aluno.')
      return
    }

    setAcaoEmAndamento('cadastro')
    try {
      const resposta = await fetch(`${API}/api/instituicoes/alunos`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          nome: nomeNormalizado,
          email: emailNormalizado,
          matricula: matricula.trim()
        })
      })

      if (!resposta.ok) {
        setErro(await mensagemDaApi(resposta, 'Não foi possível cadastrar o aluno.'))
        return
      }

      const dados = await resposta.json()
      setAlunos((atuais) => [...atuais, dados.aluno].sort((a, b) => a.nome.localeCompare(b.nome)))
      setNome('')
      setEmail('')
      setMatricula('')
      apresentarConvite(dados)
      setMensagem('Aluno cadastrado. Compartilhe o link de ativação por um canal seguro.')
    } catch {
      setErro('Não foi possível conectar com o servidor.')
    } finally {
      setAcaoEmAndamento('')
    }
  }

  function iniciarEdicao(aluno) {
    setEdicao({ id: aluno.id, nome: aluno.nome, matricula: aluno.matricula || '' })
    setErro('')
    setMensagem('')
  }

  async function salvarEdicao(event) {
    event.preventDefault()
    if (!edicao.nome.trim()) {
      setErro('Informe o nome completo do aluno.')
      return
    }
    if (!edicao.matricula.trim()) {
      setErro('Informe a matrícula (RA/RGM) do aluno.')
      return
    }

    setAcaoEmAndamento(`editar-${edicao.id}`)
    setErro('')
    try {
      const resposta = await fetch(`${API}/api/instituicoes/alunos/${edicao.id}`, {
        method: 'PUT',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          nome: edicao.nome.trim(),
          matricula: edicao.matricula.trim()
        })
      })

      if (!resposta.ok) {
        setErro(await mensagemDaApi(resposta, 'Não foi possível atualizar o aluno.'))
        return
      }

      const atualizado = await resposta.json()
      setAlunos((atuais) => atuais.map((aluno) => aluno.id === atualizado.id ? atualizado : aluno))
      setEdicao(null)
      setMensagem('Dados do aluno atualizados.')
    } catch {
      setErro('Não foi possível conectar com o servidor.')
    } finally {
      setAcaoEmAndamento('')
    }
  }

  async function alterarSituacao(aluno) {
    const novoAtivo = !aluno.ativo
    setAcaoEmAndamento(`situacao-${aluno.id}`)
    setErro('')
    setMensagem('')

    try {
      const resposta = await fetch(`${API}/api/instituicoes/alunos/${aluno.id}/situacao`, {
        method: 'PATCH',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ ativo: novoAtivo })
      })

      if (!resposta.ok) {
        setErro(await mensagemDaApi(resposta, 'Não foi possível alterar a situação do aluno.'))
        return
      }

      const atualizado = await resposta.json()
      setAlunos((atuais) => atuais.map((item) => item.id === atualizado.id ? atualizado : item))
      setMensagem(novoAtivo ? 'Aluno reativado.' : 'Aluno desativado.')
    } catch {
      setErro('Não foi possível conectar com o servidor.')
    } finally {
      setAcaoEmAndamento('')
    }
  }

  async function gerarNovoConvite(aluno) {
    setAcaoEmAndamento(`convite-${aluno.id}`)
    setErro('')
    setMensagem('')

    try {
      const resposta = await fetch(`${API}/api/instituicoes/alunos/${aluno.id}/convite`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` }
      })

      if (!resposta.ok) {
        setErro(await mensagemDaApi(resposta, 'Não foi possível gerar um novo convite.'))
        return
      }

      const dados = await resposta.json()
      setAlunos((atuais) => atuais.map((item) => item.id === dados.aluno.id ? dados.aluno : item))
      apresentarConvite(dados)
      setMensagem('Novo convite gerado. O convite anterior foi invalidado.')
    } catch {
      setErro('Não foi possível conectar com o servidor.')
    } finally {
      setAcaoEmAndamento('')
    }
  }

  async function copiarConvite() {
    try {
      await navigator.clipboard.writeText(convite.link)
      setMensagem('Link de ativação copiado.')
    } catch {
      setErro('Não foi possível copiar automaticamente. Selecione o link e copie manualmente.')
    }
  }

  return (
    <>
      <header className="cabecalho">
        <p className="cabecalho-contexto">GESTÃO INSTITUCIONAL</p>
        <h2>Gestão de Alunos</h2>
        <p className="cabecalho-descricao">
          Cadastre e acompanhe os alunos vinculados à sua instituição.
        </p>
      </header>

      {(erro || mensagem) && (
        <p className={erro ? 'mensagem-erro painel-mensagem' : 'mensagem-sucesso painel-mensagem'} role={erro ? 'alert' : 'status'}>
          {erro || mensagem}
        </p>
      )}

      {convite && (
        <section className="convite-aluno" aria-labelledby="titulo-convite-aluno">
          <div>
            <h3 id="titulo-convite-aluno">Link de ativação de {convite.aluno.nome}</h3>
            <p>Este link é exibido somente agora. Compartilhe-o diretamente com o aluno por um canal seguro.</p>
          </div>
          <input value={convite.link} readOnly aria-label="Link de ativação" onFocus={(event) => event.target.select()} />
          <div className="convite-acoes">
            <small>Expira em {new Date(convite.expiraEm).toLocaleString('pt-BR')}.</small>
            <button type="button" onClick={copiarConvite}>Copiar link</button>
            <button type="button" className="botao-secundario" onClick={() => setConvite(null)}>Ocultar</button>
          </div>
        </section>
      )}

      <section className="formulario gestao-alunos-formulario">
        <div className="secao-cabecalho">
          <h3>Cadastrar aluno</h3>
          <p>A instituição é definida automaticamente pela sua conta.</p>
        </div>
        <form onSubmit={cadastrarAluno}>
          <div className="campo">
            <label htmlFor="nomeNovoAluno">Nome completo</label>
            <input id="nomeNovoAluno" value={nome} onChange={(event) => setNome(event.target.value)} maxLength="255" required />
          </div>
          <div className="campo">
            <label htmlFor="emailNovoAluno">E-mail</label>
            <input id="emailNovoAluno" type="email" value={email} onChange={(event) => setEmail(event.target.value)} maxLength="255" required />
          </div>
          <div className="campo">
            <label htmlFor="matriculaNovoAluno">Matrícula (RA/RGM)</label>
            <input id="matriculaNovoAluno" value={matricula} onChange={(event) => setMatricula(event.target.value)} maxLength="50" required />
          </div>
          <button type="submit" disabled={acaoEmAndamento === 'cadastro'}>
            {acaoEmAndamento === 'cadastro' ? 'Cadastrando...' : 'Cadastrar e gerar convite'}
          </button>
        </form>
      </section>

      <section className="acompanhamento gestao-alunos-lista">
        <div className="secao-cabecalho">
          <h3>Alunos da instituição</h3>
          <p>Somente alunos vinculados à sua faculdade são apresentados.</p>
        </div>

        {carregando ? (
          <p className="estado-vazio">Carregando alunos...</p>
        ) : alunos.length === 0 ? (
          <p className="estado-vazio">Nenhum aluno cadastrado nesta instituição.</p>
        ) : (
          <div className="alunos-grid">
            {alunos.map((aluno) => (
              <article className={`aluno-card ${aluno.ativo ? '' : 'desativado'}`} key={aluno.id}>
                {edicao?.id === aluno.id ? (
                  <form className="aluno-edicao" onSubmit={salvarEdicao}>
                    <div className="campo">
                      <label htmlFor={`editar-nome-${aluno.id}`}>Nome</label>
                      <input id={`editar-nome-${aluno.id}`} value={edicao.nome} onChange={(event) => setEdicao({ ...edicao, nome: event.target.value })} maxLength="255" required autoFocus />
                    </div>
                    <div className="campo">
                      <label htmlFor={`editar-matricula-${aluno.id}`}>Matrícula (RA/RGM)</label>
                      <input id={`editar-matricula-${aluno.id}`} value={edicao.matricula} onChange={(event) => setEdicao({ ...edicao, matricula: event.target.value })} maxLength="50" required />
                    </div>
                    <div className="aluno-acoes">
                      <button type="submit" disabled={acaoEmAndamento === `editar-${aluno.id}`}>Salvar</button>
                      <button type="button" className="botao-secundario" onClick={() => setEdicao(null)}>Cancelar</button>
                    </div>
                  </form>
                ) : (
                  <>
                    <div className="aluno-card-cabecalho">
                      <div>
                        <h4>{aluno.nome}</h4>
                        <p>{aluno.email}</p>
                      </div>
                      <span className={`situacao-aluno situacao-${aluno.situacaoAtivacao.toLowerCase()}`}>
                        {rotulosSituacaoAtivacao[aluno.situacaoAtivacao] || aluno.situacaoAtivacao}
                      </span>
                    </div>
                    <dl>
                      <div><dt>Matrícula (RA/RGM)</dt><dd>{aluno.matricula || 'Registro legado sem matrícula'}</dd></div>
                      <div><dt>Convite</dt><dd>{aluno.conviteExpiraEm ? `Expira em ${new Date(aluno.conviteExpiraEm).toLocaleString('pt-BR')}` : 'Não disponível'}</dd></div>
                    </dl>
                    <div className="aluno-acoes">
                      <button type="button" className="botao-secundario" onClick={() => iniciarEdicao(aluno)}>Editar</button>
                      {!aluno.ativado && aluno.ativo && (
                        <button type="button" className="botao-secundario" disabled={acaoEmAndamento === `convite-${aluno.id}`} onClick={() => gerarNovoConvite(aluno)}>
                          Gerar novo convite
                        </button>
                      )}
                      <button type="button" className={aluno.ativo ? 'botao-perigo' : ''} disabled={acaoEmAndamento === `situacao-${aluno.id}`} onClick={() => alterarSituacao(aluno)}>
                        {aluno.ativo ? 'Desativar' : 'Reativar'}
                      </button>
                    </div>
                  </>
                )}
              </article>
            ))}
          </div>
        )}
      </section>
    </>
  )
}

function AtivacaoAluno({ tokenAtivacao, aoConcluir }) {
  const [senha, setSenha] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [termos, setTermos] = useState(false)
  const [erro, setErro] = useState('')
  const [concluido, setConcluido] = useState(false)
  const [carregando, setCarregando] = useState(false)

  async function ativar(event) {
    event.preventDefault()
    setErro('')
    if (senha.length < 8) {
      setErro('A senha deve ter pelo menos 8 caracteres.')
      return
    }
    if (senha !== confirmacao) {
      setErro('A confirmação da senha não corresponde.')
      return
    }
    if (!termos) {
      setErro('É necessário aceitar os Termos de Uso e a Política de Privacidade.')
      return
    }

    setCarregando(true)
    try {
      const resposta = await fetch(`${API}/api/ativacoes/aluno`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: tokenAtivacao, senha, termosAceitos: termos })
      })

      if (!resposta.ok) {
        setErro(await mensagemDaApi(resposta, 'Não foi possível ativar a conta.'))
        return
      }

      window.history.replaceState({}, document.title, window.location.pathname)
      setSenha('')
      setConfirmacao('')
      setConcluido(true)
    } catch {
      setErro('Não foi possível conectar com o servidor.')
    } finally {
      setCarregando(false)
    }
  }

  return (
    <main className="conteudo ativacao-aluno-pagina">
      <header className="cabecalho">
        <p className="cabecalho-contexto">ATIVAÇÃO SEGURA</p>
        <h2>Ativar conta de aluno</h2>
        <p className="cabecalho-descricao">Defina sua senha pessoal para acessar o GAEA.</p>
      </header>
      <section className="formulario">
        {concluido ? (
          <div className="ativacao-concluida" role="status">
            <h3>Conta ativada com sucesso</h3>
            <p>Sua senha foi definida. Você já pode entrar no sistema.</p>
            <button type="button" onClick={aoConcluir}>Ir para o login</button>
          </div>
        ) : (
          <form onSubmit={ativar}>
            <div className="campo">
              <label htmlFor="senhaAtivacao">Nova senha</label>
              <input id="senhaAtivacao" type="password" value={senha} onChange={(event) => setSenha(event.target.value)} minLength="8" autoComplete="new-password" required autoFocus />
            </div>
            <div className="campo">
              <label htmlFor="confirmacaoSenhaAtivacao">Confirmar senha</label>
              <input id="confirmacaoSenhaAtivacao" type="password" value={confirmacao} onChange={(event) => setConfirmacao(event.target.value)} minLength="8" autoComplete="new-password" required />
            </div>
            <label className="aceite ativacao-aceite">
              <input type="checkbox" checked={termos} onChange={(event) => setTermos(event.target.checked)} />
              Li e aceito os Termos de Uso e a Política de Privacidade.
            </label>
            {erro && <p className="mensagem-erro" role="alert">{erro}</p>}
            <button type="submit" disabled={carregando}>{carregando ? 'Ativando...' : 'Ativar minha conta'}</button>
          </form>
        )}
      </section>
    </main>
  )
}

function VerificacaoEmail({ tokenVerificacao, aoConcluir }) {
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(false)
  const [concluido, setConcluido] = useState(false)

  async function confirmar() {
    setCarregando(true)
    setErro('')
    try {
      const resposta = await fetch(`${API}/api/verificacoes-email/confirmar`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: tokenVerificacao })
      })
      if (!resposta.ok) {
        setErro(await mensagemDaApi(resposta, 'Não foi possível verificar o e-mail.'))
        return
      }
      window.history.replaceState({}, document.title, window.location.pathname)
      setConcluido(true)
    } catch {
      setErro('Não foi possível conectar com o servidor.')
    } finally {
      setCarregando(false)
    }
  }

  return (
    <main className="conteudo ativacao-aluno-pagina">
      <header className="cabecalho">
        <p className="cabecalho-contexto">VERIFICAÇÃO SEGURA</p>
        <h2>Confirmar e-mail</h2>
        <p className="cabecalho-descricao">Confirme a titularidade do e-mail da sua conta de Supervisor da Empresa.</p>
      </header>
      <section className="formulario">
        {concluido ? (
          <div className="ativacao-concluida" role="status">
            <h3>E-mail verificado</h3>
            <p>Agora você pode responder às solicitações de supervisão.</p>
            <button type="button" onClick={aoConcluir}>Ir para o login</button>
          </div>
        ) : (
          <div className="ativacao-concluida">
            <p>O link é pessoal e será invalidado após o uso.</p>
            {erro && <p className="mensagem-erro" role="alert">{erro}</p>}
            <button type="button" disabled={carregando} onClick={confirmar}>
              {carregando ? 'Verificando...' : 'Confirmar meu e-mail'}
            </button>
          </div>
        )}
      </section>
    </main>
  )
}

function Cabecalho({
  mostrarSair = false,
  usuario,
  tela,
  secaoInstituicao,
  aoIrInicio,
  aoIrDashboard,
  aoIrProcessos,
  aoIrAlunos,
  aoIrPrivacidade,
  aoIrTermos,
  aoSair
}) {
  return (
    <header className="navbar">
      <div className="navbar-conteudo">
        <div className="logo">
          <div className="logo-marca">G</div>

          <div>
            <h1>GAEA</h1>
            <p>Gestão de Estágios</p>
          </div>
        </div>

        <nav aria-label="Navegação principal">
          {usuario && mostrarSair ? (
            <>
              <button
                className={tela === 'sistema' && secaoInstituicao === 'dashboard' ? 'ativo' : ''}
                type="button"
                onClick={aoIrDashboard}
              >
                Dashboard
              </button>
              <button
                className={tela === 'sistema' && secaoInstituicao === 'processos' ? 'ativo' : ''}
                type="button"
                onClick={aoIrProcessos}
              >
                Processos
              </button>
              {usuario.perfil === 'INSTITUICAO' && (
                <button
                  className={tela === 'sistema' && secaoInstituicao === 'alunos' ? 'ativo' : ''}
                  type="button"
                  onClick={aoIrAlunos}
                >
                  Gestão de Alunos
                </button>
              )}
            </>
          ) : (
            <button
              className={tela === 'sistema' ? 'ativo' : ''}
              type="button"
              onClick={aoIrInicio}
            >
              Início
            </button>
          )}

          <button
            className={tela === 'privacidade' ? 'ativo' : ''}
            type="button"
            onClick={aoIrPrivacidade}
          >
            Privacidade
          </button>

          <button
            className={tela === 'termos' ? 'ativo' : ''}
            type="button"
            onClick={aoIrTermos}
          >
            Termos
          </button>

          {mostrarSair && (
            <button type="button" onClick={aoSair}>
              Sair
            </button>
          )}
        </nav>
      </div>
    </header>
  )
}

function App() {
  const parametrosIniciais = new URLSearchParams(window.location.hash.slice(1))
  const tokenAtivacaoInicial = parametrosIniciais.get('conviteAluno') || ''
  const tokenVerificacaoInicial = parametrosIniciais.get('verificarEmail') || ''
  const [usuario, setUsuario] = useState(null)
  const [token, setToken] = useState('')

  const [tela, setTela] = useState(
    tokenAtivacaoInicial ? 'ativacaoAluno' : tokenVerificacaoInicial ? 'verificacaoEmail' : 'login'
  )

  const [emailLogin, setEmailLogin] = useState('')
  const [senhaLogin, setSenhaLogin] = useState('')
  const [erroLogin, setErroLogin] = useState('')

  const [nomeCadastro, setNomeCadastro] = useState('')
  const [emailCadastro, setEmailCadastro] = useState('')
  const [senhaCadastro, setSenhaCadastro] = useState('')
  const [termosAceitos, setTermosAceitos] = useState(false)
  const [erroCadastro, setErroCadastro] = useState('')
  const [sucessoCadastro, setSucessoCadastro] = useState('')

  const [nomeEmpresa, setNomeEmpresa] = useState('')
  const [cnpjEmpresa, setCnpjEmpresa] = useState('')
  const [emailSupervisor, setEmailSupervisor] = useState('')
  const [dataInicio, setDataInicio] = useState('')
  const [dataFim, setDataFim] = useState('')
  const [erroProcesso, setErroProcesso] = useState('')
  const [processos, setProcessos] = useState([])
  const [cepEmpresa, setCepEmpresa] = useState('')
  const [enderecoEmpresa, setEnderecoEmpresa] = useState('')
  const [erroCep, setErroCep] = useState('')
  const [salvandoProcesso, setSalvandoProcesso] = useState(false)
  const [logsAuditoria, setLogsAuditoria] = useState([])
  const [mostrarAuditoria, setMostrarAuditoria] = useState(false)
  const [historicos, setHistoricos] = useState({})
  const [historicoAberto, setHistoricoAberto] = useState(null)
  const [carregandoHistorico, setCarregandoHistorico] = useState(null)
  const [errosHistorico, setErrosHistorico] = useState({})
  const [pendencias, setPendencias] = useState({})
  const [pendenciasAbertas, setPendenciasAbertas] = useState(null)
  const [carregandoPendencias, setCarregandoPendencias] = useState(null)
  const [errosPendencias, setErrosPendencias] = useState({})
  const [processoNovaPendencia, setProcessoNovaPendencia] = useState(null)
  const [descricaoPendencia, setDescricaoPendencia] = useState('')
  const [erroNovaPendencia, setErroNovaPendencia] = useState('')
  const [salvandoPendencia, setSalvandoPendencia] = useState(false)
  const [resolvendoPendencia, setResolvendoPendencia] = useState(null)
  const [prazos, setPrazos] = useState({})
  const [prazosAbertos, setPrazosAbertos] = useState(null)
  const [carregandoPrazos, setCarregandoPrazos] = useState(null)
  const [errosPrazos, setErrosPrazos] = useState({})
  const [processoPrazo, setProcessoPrazo] = useState(null)
  const [prazoEdicao, setPrazoEdicao] = useState(null)
  const [descricaoPrazo, setDescricaoPrazo] = useState('')
  const [dataVencimentoPrazo, setDataVencimentoPrazo] = useState('')
  const [erroPrazo, setErroPrazo] = useState('')
  const [salvandoPrazo, setSalvandoPrazo] = useState(false)
  const [secaoInstituicao, setSecaoInstituicao] = useState('dashboard')
  const [resumoDashboard, setResumoDashboard] = useState(null)
  const [carregandoDashboard, setCarregandoDashboard] = useState(false)
  const [erroDashboard, setErroDashboard] = useState('')
  const [mensagemAcaoProcesso, setMensagemAcaoProcesso] = useState(null)
  const [processoReabertura, setProcessoReabertura] = useState(null)
  const [justificativaReabertura, setJustificativaReabertura] = useState('')
  const [erroReabertura, setErroReabertura] = useState('')
  const [reaberturaEmAndamento, setReaberturaEmAndamento] = useState(false)
  const [solicitacoesSupervisao, setSolicitacoesSupervisao] = useState([])
  const [justificativasRecusa, setJustificativasRecusa] = useState({})
  const [erroSupervisao, setErroSupervisao] = useState('')
  const [acaoSupervisao, setAcaoSupervisao] = useState(null)
  const [novosSupervisores, setNovosSupervisores] = useState({})

  async function fazerLogin(event) {
    event.preventDefault()
    setErroLogin('')

    try {
      const resposta = await fetch(`${API}/api/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          email: emailLogin,
          senha: senhaLogin
        })
      })

      if (!resposta.ok) {
        setErroLogin(await mensagemDaApi(resposta, 'E-mail ou senha inválidos.'))
        return
      }

      const dados = await resposta.json()

      setUsuario({
        nome: dados.nome,
        email: dados.email,
        perfil: dados.perfil,
        matricula: dados.matricula,
        instituicao: dados.instituicao,
        emailVerificado: dados.emailVerificado
      })

      setToken(dados.token)
      setSenhaLogin('')
      setSecaoInstituicao('dashboard')
      setTela('sistema')
      buscarResumoDashboard(dados.token)

      if (dados.perfil === 'INSTITUICAO') {
        buscarProcessos(dados.token)
      } else if (dados.perfil === 'ALUNO') {
        buscarMeusProcessos(dados.token)
      } else if (dados.perfil === 'EMPRESA' && dados.emailVerificado) {
        buscarSolicitacoesSupervisao(dados.token)
        buscarProcessos(dados.token)
      }
    } catch {
      setErroLogin('Não foi possível conectar com o servidor.')
    }
  }

  async function fazerCadastro(event) {
    event.preventDefault()
    setErroCadastro('')
    setSucessoCadastro('')

    const nome = nomeCadastro.trim()
    const email = emailCadastro.trim().toLowerCase()

    if (!nome) {
      setErroCadastro('Informe o nome.')
      return
    }

    if (!emailValido(email)) {
      setErroCadastro('Informe um e-mail válido.')
      return
    }

    if (senhaCadastro.length < 8) {
      setErroCadastro('A senha deve ter pelo menos 8 caracteres.')
      return
    }

    if (!termosAceitos) {
      setErroCadastro(
        'É necessário aceitar os Termos de Uso e a Política de Privacidade.'
      )
      return
    }

    try {
      const resposta = await fetch(`${API}/api/auth/cadastro`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          nome,
          email,
          senha: senhaCadastro,
          perfil: 'EMPRESA',
          termosAceitos
        })
      })

      if (!resposta.ok) {
        setErroCadastro(
          await mensagemDaApi(resposta, 'Não foi possível realizar o cadastro.')
        )
        return
      }

      setSucessoCadastro('Cadastro realizado. Verifique seu e-mail antes de responder solicitações.')

      setNomeCadastro('')
      setEmailCadastro('')
      setSenhaCadastro('')
      setTermosAceitos(false)
    } catch {
      setErroCadastro('Não foi possível conectar com o servidor.')
    }
  }

  function sair() {
    setUsuario(null)
    setToken('')
    setProcessos([])
    setPendencias({})
    setPendenciasAbertas(null)
    setCarregandoPendencias(null)
    setErrosPendencias({})
    setProcessoNovaPendencia(null)
    setDescricaoPendencia('')
    setErroNovaPendencia('')
    setResolvendoPendencia(null)
    setResumoDashboard(null)
    setErroDashboard('')
    setSecaoInstituicao('dashboard')
    setMensagemAcaoProcesso(null)
    setProcessoReabertura(null)
    setSolicitacoesSupervisao([])
    setErroSupervisao('')
    setEmailLogin('')
    setSenhaLogin('')
    setTela('login')
  }

  async function buscarProcessos(tokenAcesso = token) {
    try {
      const resposta = await fetch(`${API}/api/processos`, {
        headers: {
          Authorization: `Bearer ${tokenAcesso}`
        }
      })

      if (resposta.ok) {
        const dados = await resposta.json()
        setProcessos(dados)
      }
    } catch {
      console.log('Erro ao buscar processos')
    }
  }

  async function buscarMeusProcessos(tokenAcesso = token) {
    try {
      const resposta = await fetch(`${API}/api/processos/meus`, {
        headers: {
          Authorization: `Bearer ${tokenAcesso}`
        }
      })

      if (resposta.ok) {
        const dados = await resposta.json()
        setProcessos(dados)
      }
    } catch {
      console.log('Erro ao buscar processos do aluno')
    }
  }

  async function buscarSolicitacoesSupervisao(tokenAcesso = token) {
    setErroSupervisao('')
    try {
      const resposta = await fetch(`${API}/api/supervisao/solicitacoes`, {
        headers: { Authorization: `Bearer ${tokenAcesso}` }
      })
      if (!resposta.ok) {
        setErroSupervisao(await mensagemDaApi(resposta, 'Não foi possível consultar as solicitações.'))
        return
      }
      setSolicitacoesSupervisao(await resposta.json())
    } catch {
      setErroSupervisao('Não foi possível conectar com o servidor.')
    }
  }

  async function responderSupervisao(solicitacao, acao) {
    const justificativa = (justificativasRecusa[solicitacao.vinculoId] || '').trim()
    if (acao === 'recusar' && justificativa.length < 10) {
      setErroSupervisao('Informe uma justificativa de recusa com pelo menos 10 caracteres.')
      return
    }
    setAcaoSupervisao(`${acao}-${solicitacao.vinculoId}`)
    setErroSupervisao('')
    try {
      const resposta = await fetch(
        `${API}/api/supervisao/solicitacoes/${solicitacao.vinculoId}/${acao}`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`
          },
          body: acao === 'recusar' ? JSON.stringify({ justificativa }) : null
        }
      )
      if (!resposta.ok) {
        setErroSupervisao(await mensagemDaApi(resposta, 'Não foi possível responder à solicitação.'))
        return
      }
      setSolicitacoesSupervisao((atuais) =>
        atuais.filter((item) => item.vinculoId !== solicitacao.vinculoId))
      exibirMensagemAcao(
        acao === 'confirmar' ? 'Supervisão confirmada com sucesso.' : 'Solicitação recusada.'
      )
    } catch {
      setErroSupervisao('Não foi possível conectar com o servidor.')
    } finally {
      setAcaoSupervisao(null)
    }
  }

  async function reenviarVerificacaoEmail() {
    setAcaoSupervisao('reenviar-email')
    setErroSupervisao('')
    try {
      const resposta = await fetch(`${API}/api/verificacoes-email/reenviar`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` }
      })
      if (!resposta.ok) {
        setErroSupervisao(await mensagemDaApi(resposta, 'Não foi possível gerar um novo link.'))
        return
      }
      exibirMensagemAcao('Um novo link foi solicitado. Confira a caixa de entrada configurada.')
    } catch {
      setErroSupervisao('Não foi possível conectar com o servidor.')
    } finally {
      setAcaoSupervisao(null)
    }
  }

  async function indicarNovoSupervisor(processo) {
    const email = (novosSupervisores[processo.id] || '').trim().toLowerCase()
    if (!emailValido(email)) {
      exibirMensagemAcao('Informe um e-mail válido para o novo supervisor.', 'erro')
      return
    }
    setAcaoSupervisao(`troca-${processo.id}`)
    try {
      const resposta = await fetch(`${API}/api/processos/${processo.id}/supervisor`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({ emailSupervisor: email })
      })
      if (!resposta.ok) {
        exibirMensagemAcao(
          await mensagemDaApi(resposta, 'Não foi possível indicar outro supervisor.'), 'erro')
        return
      }
      setNovosSupervisores((atuais) => ({ ...atuais, [processo.id]: '' }))
      await buscarMeusProcessos()
      if (historicoAberto === processo.id) {
        await buscarHistorico(processo.id, true)
      }
      exibirMensagemAcao('Nova solicitação enviada ao supervisor informado.')
    } catch {
      exibirMensagemAcao('Não foi possível conectar com o servidor.', 'erro')
    } finally {
      setAcaoSupervisao(null)
    }
  }

  async function buscarResumoDashboard(tokenAcesso = token) {
    setCarregandoDashboard(true)
    setErroDashboard('')

    try {
      const resposta = await fetch(`${API}/api/dashboard/resumo`, {
        headers: {
          Authorization: `Bearer ${tokenAcesso}`
        }
      })

      if (!resposta.ok) {
        setResumoDashboard(null)
        if (resposta.status === 401 || resposta.status === 403) {
          setErroDashboard('Sua sessão não possui autorização para consultar estes indicadores.')
        } else {
          setErroDashboard(await mensagemDaApi(resposta, 'O servidor não conseguiu calcular os indicadores.'))
        }
        return
      }

      setResumoDashboard(await resposta.json())
    } catch {
      setResumoDashboard(null)
      setErroDashboard('Não foi possível conectar com o servidor.')
    } finally {
      setCarregandoDashboard(false)
    }
  }

  function exibirMensagemAcao(texto, tipo = 'sucesso') {
    setMensagemAcaoProcesso({ texto, tipo })
  }

  function abrirModalReabertura(processo) {
    setProcessoReabertura(processo)
    setJustificativaReabertura('')
    setErroReabertura('')
    setReaberturaEmAndamento(false)
  }

  function cancelarReabertura() {
    if (reaberturaEmAndamento) {
      return
    }

    setProcessoReabertura(null)
    setJustificativaReabertura('')
    setErroReabertura('')
  }

  async function alterarStatus(id, novoStatus) {
    try {
      const resposta = await fetch(
        `${API}/api/processos/${id}/status`,
        {
          method: 'PUT',
          headers: {
            'Content-Type': 'text/plain',
            Authorization: `Bearer ${token}`
          },
          body: novoStatus
        }
      )

      if (resposta.ok) {
        const processoAtualizado = await resposta.json()

        setProcessos((listaAtual) =>
          listaAtual.map((processo) => {
            if (processo.id === processoAtualizado.id) {
              return {
                ...processo,
                status: processoAtualizado.status
              }
            }

            return processo
          })
        )
        exibirMensagemAcao('Status atualizado com sucesso.')
        await buscarResumoDashboard()

        if (historicoAberto === id) {
          await buscarHistorico(id, true)
        }
      } else {
        exibirMensagemAcao(
          await mensagemDaApi(resposta, 'Erro ao alterar status.'),
          'erro'
        )
      }
    } catch {
      exibirMensagemAcao('Não foi possível conectar com o servidor.', 'erro')
    }
  }

  async function confirmarReabertura(event) {
    event.preventDefault()

    if (!processoReabertura || reaberturaEmAndamento) {
      return
    }

    const justificativa = justificativaReabertura.trim()
    if (justificativa.length < 10) {
      setErroReabertura('A justificativa deve ter pelo menos 10 caracteres.')
      return
    }

    setErroReabertura('')
    setReaberturaEmAndamento(true)

    try {
      const resposta = await fetch(`${API}/api/processos/${processoReabertura.id}/reabrir`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({ justificativa })
      })

      if (!resposta.ok) {
        setErroReabertura(
          await mensagemDaApi(resposta, 'Não foi possível reabrir o processo.')
        )
        return
      }

      const processoAtualizado = await resposta.json()
      setProcessos((listaAtual) =>
        listaAtual.map((processo) =>
          processo.id === processoAtualizado.id ? processoAtualizado : processo
        )
      )
      await buscarResumoDashboard()

      if (historicoAberto === processoAtualizado.id) {
        await buscarHistorico(processoAtualizado.id, true)
      }

      setProcessoReabertura(null)
      setJustificativaReabertura('')
      setErroReabertura('')
      exibirMensagemAcao('Processo reaberto para análise.')
    } catch {
      setErroReabertura('Não foi possível conectar com o servidor.')
    } finally {
      setReaberturaEmAndamento(false)
    }
  }

  async function buscarHistorico(id, manterAberto = false) {
    if (!manterAberto && historicoAberto === id) {
      setHistoricoAberto(null)
      return
    }

    setHistoricoAberto(id)
    setCarregandoHistorico(id)
    setErrosHistorico((atuais) => ({ ...atuais, [id]: '' }))

    try {
      const resposta = await fetch(`${API}/api/processos/${id}/historico-completo`, {
        headers: {
          Authorization: `Bearer ${token}`
        }
      })

      if (!resposta.ok) {
        const mensagem = await mensagemDaApi(resposta, 'Não foi possível consultar o histórico.')
        setErrosHistorico((atuais) => ({ ...atuais, [id]: mensagem }))
        return
      }

      const dados = await resposta.json()
      setHistoricos((atuais) => ({ ...atuais, [id]: dados }))
    } catch {
      setErrosHistorico((atuais) => ({
        ...atuais,
        [id]: 'Não foi possível conectar com o servidor.'
      }))
    } finally {
      setCarregandoHistorico((atual) => atual === id ? null : atual)
    }
  }

  async function buscarPendencias(id, manterAberto = false) {
    if (!manterAberto && pendenciasAbertas === id) {
      setPendenciasAbertas(null)
      return
    }

    setPendenciasAbertas(id)
    setCarregandoPendencias(id)
    setErrosPendencias((atuais) => ({ ...atuais, [id]: '' }))

    try {
      const resposta = await fetch(`${API}/api/processos/${id}/pendencias`, {
        headers: { Authorization: `Bearer ${token}` }
      })

      if (!resposta.ok) {
        const mensagem = await mensagemDaApi(resposta, 'Não foi possível consultar as pendências.')
        setErrosPendencias((atuais) => ({
          ...atuais,
          [id]: mensagem
        }))
        return
      }

      const dados = await resposta.json()
      setPendencias((atuais) => ({ ...atuais, [id]: dados }))
    } catch {
      setErrosPendencias((atuais) => ({
        ...atuais,
        [id]: 'Não foi possível conectar com o servidor.'
      }))
    } finally {
      setCarregandoPendencias(null)
    }
  }

  function abrirNovaPendencia(processo) {
    setProcessoNovaPendencia(processo)
    setDescricaoPendencia('')
    setErroNovaPendencia('')
  }

  function cancelarNovaPendencia() {
    if (salvandoPendencia) {
      return
    }
    setProcessoNovaPendencia(null)
    setDescricaoPendencia('')
    setErroNovaPendencia('')
  }

  async function registrarPendencia(event) {
    event.preventDefault()
    if (!processoNovaPendencia || salvandoPendencia) {
      return
    }

    const descricao = descricaoPendencia.trim()
    if (!descricao) {
      setErroNovaPendencia('Informe a descrição da pendência.')
      return
    }

    setErroNovaPendencia('')
    setSalvandoPendencia(true)
    try {
      const resposta = await fetch(`${API}/api/processos/${processoNovaPendencia.id}/pendencias`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({ descricao })
      })

      if (!resposta.ok) {
        setErroNovaPendencia(await mensagemDaApi(resposta, 'Não foi possível registrar a pendência.'))
        return
      }

      const processoId = processoNovaPendencia.id
      await resposta.json()
      setProcessoNovaPendencia(null)
      setDescricaoPendencia('')
      await buscarPendencias(processoId, true)
      if (historicoAberto === processoId) {
        await buscarHistorico(processoId, true)
      }
      exibirMensagemAcao('Pendência registrada com sucesso.')
    } catch {
      setErroNovaPendencia('Não foi possível conectar com o servidor.')
    } finally {
      setSalvandoPendencia(false)
    }
  }

  async function resolverPendenciaDoProcesso(processoId, pendenciaId) {
    if (resolvendoPendencia !== null) {
      return
    }

    setResolvendoPendencia(pendenciaId)
    setErrosPendencias((atuais) => ({ ...atuais, [processoId]: '' }))
    try {
      const resposta = await fetch(
        `${API}/api/processos/${processoId}/pendencias/${pendenciaId}/resolver`,
        {
          method: 'PUT',
          headers: { Authorization: `Bearer ${token}` }
        }
      )

      if (!resposta.ok) {
        const mensagem = await mensagemDaApi(resposta, 'Não foi possível resolver a pendência.')
        setErrosPendencias((atuais) => ({
          ...atuais,
          [processoId]: mensagem
        }))
        return
      }

      const atualizada = await resposta.json()
      setPendencias((atuais) => ({
        ...atuais,
        [processoId]: (atuais[processoId] || []).map((item) =>
          item.id === atualizada.id ? atualizada : item
        )
      }))
      if (historicoAberto === processoId) {
        await buscarHistorico(processoId, true)
      }
      exibirMensagemAcao('Pendência resolvida com sucesso.')
    } catch {
      setErrosPendencias((atuais) => ({
        ...atuais,
        [processoId]: 'Não foi possível conectar com o servidor.'
      }))
    } finally {
      setResolvendoPendencia(null)
    }
  }

  async function buscarPrazos(id, manterAberto = false) {
    if (!manterAberto && prazosAbertos === id) {
      setPrazosAbertos(null)
      return
    }

    setPrazosAbertos(id)
    setCarregandoPrazos(id)
    setErrosPrazos((atuais) => ({ ...atuais, [id]: '' }))

    try {
      const resposta = await fetch(`${API}/api/processos/${id}/prazos`, {
        headers: { Authorization: `Bearer ${token}` }
      })

      if (!resposta.ok) {
        const mensagem = await mensagemDaApi(resposta, 'Não foi possível consultar os prazos.')
        setErrosPrazos((atuais) => ({ ...atuais, [id]: mensagem }))
        return
      }

      const dados = await resposta.json()
      setPrazos((atuais) => ({ ...atuais, [id]: dados }))
    } catch {
      setErrosPrazos((atuais) => ({
        ...atuais,
        [id]: 'Não foi possível conectar com o servidor.'
      }))
    } finally {
      setCarregandoPrazos(null)
    }
  }

  function abrirPrazo(processo, prazo = null) {
    setProcessoPrazo(processo)
    setPrazoEdicao(prazo)
    setDescricaoPrazo(prazo?.descricao || '')
    setDataVencimentoPrazo(prazo?.dataVencimento || '')
    setErroPrazo('')
  }

  function cancelarPrazo() {
    if (salvandoPrazo) {
      return
    }
    setProcessoPrazo(null)
    setPrazoEdicao(null)
    setDescricaoPrazo('')
    setDataVencimentoPrazo('')
    setErroPrazo('')
  }

  async function salvarPrazo(event) {
    event.preventDefault()
    if (!processoPrazo || salvandoPrazo) {
      return
    }

    const descricao = descricaoPrazo.trim()
    if (!descricao) {
      setErroPrazo('Informe a descrição do prazo.')
      return
    }
    if (!dataVencimentoPrazo) {
      setErroPrazo('Informe a data de vencimento.')
      return
    }

    const processoId = processoPrazo.id
    const url = prazoEdicao
      ? `${API}/api/processos/${processoId}/prazos/${prazoEdicao.id}`
      : `${API}/api/processos/${processoId}/prazos`

    setErroPrazo('')
    setSalvandoPrazo(true)
    try {
      const resposta = await fetch(url, {
        method: prazoEdicao ? 'PUT' : 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({ descricao, dataVencimento: dataVencimentoPrazo })
      })

      if (!resposta.ok) {
        setErroPrazo(await mensagemDaApi(resposta, 'Não foi possível salvar o prazo.'))
        return
      }

      const atualizando = Boolean(prazoEdicao)
      await resposta.json()
      setProcessoPrazo(null)
      setPrazoEdicao(null)
      setDescricaoPrazo('')
      setDataVencimentoPrazo('')
      await buscarPrazos(processoId, true)
      if (historicoAberto === processoId) {
        await buscarHistorico(processoId, true)
      }
      exibirMensagemAcao(atualizando ? 'Prazo atualizado com sucesso.' : 'Prazo registrado com sucesso.')
    } catch {
      setErroPrazo('Não foi possível conectar com o servidor.')
    } finally {
      setSalvandoPrazo(false)
    }
  }

  async function buscarAuditoria() {
  try {
    const resposta = await fetch(`${API}/api/auditoria`, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })

    if (!resposta.ok) {
      alert('Não foi possível consultar os logs de auditoria.')
      return
    }

    const dados = await resposta.json()
    setLogsAuditoria(dados)
    setMostrarAuditoria(true)
  } catch {
    alert('Não foi possível conectar com o servidor.')
  }
}
  async function buscarCep() {
  setErroCep('')
  setEnderecoEmpresa('')

  if (!cepEmpresa.trim()) {
    setErroCep('Informe o CEP da empresa.')
    return
  }

  try {
    const resposta = await fetch(`${API}/api/cep/${cepEmpresa}`, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })

    if (!resposta.ok) {
      setErroCep('Não foi possível consultar o CEP.')
      return
    }

    const dados = await resposta.json()
    const endereco = [
      dados.logradouro,
      dados.bairro,
      dados.localidade && dados.uf ? `${dados.localidade}/${dados.uf}` : dados.localidade
    ].filter(Boolean).join(', ')
    setEnderecoEmpresa(endereco)
  } catch {
    setErroCep('Não foi possível conectar com o servidor.')
  }
}

  async function cadastrarEstagio(event) {
    event.preventDefault()

    setErroProcesso('')

    const empresa = nomeEmpresa.trim()
    const cep = cepEmpresa.replace(/\D/g, '')
    const endereco = enderecoEmpresa.trim()
    const supervisor = emailSupervisor.trim().toLowerCase()

    if (!empresa) {
      setErroProcesso('Informe o nome da empresa.')
      return
    }

    if (!emailValido(supervisor)) {
      setErroProcesso('Informe um e-mail válido para o supervisor.')
      return
    }

    if (!cnpjValido(cnpjEmpresa)) {
      setErroProcesso('Informe um CNPJ válido.')
      return
    }

    if (cep.length !== 8) {
      setErroProcesso('Informe um CEP válido com 8 dígitos.')
      return
    }

    if (!endereco) {
      setErroProcesso('Informe o endereço da empresa.')
      return
    }

    if (!dataInicio || !dataFim) {
      setErroProcesso('Informe as datas inicial e final.')
      return
    }

    if (dataFim < dataInicio) {
      setErroProcesso('A data final não pode ser anterior à data inicial.')
      return
    }

    const processo = {
      nomeEmpresa: empresa,
      cnpjEmpresa,
      cepEmpresa: cep,
      enderecoEmpresa: endereco,
      dataInicio,
      dataFim,
      emailSupervisor: supervisor
    }

    setSalvandoProcesso(true)
    try {
      const resposta = await fetch(`${API}/api/processos`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify(processo)
      })

      if (resposta.ok) {
        const novoProcesso = await resposta.json()

        setProcessos((listaAtual) => [
          ...listaAtual,
          novoProcesso
        ])

        setNomeEmpresa('')
        setCnpjEmpresa('')
        setEmailSupervisor('')
        setCepEmpresa('')
        setEnderecoEmpresa('')
        setDataInicio('')
        setDataFim('')

        exibirMensagemAcao('Solicitação enviada. Aguardando confirmação do supervisor.')
      } else {
        setErroProcesso(
          await mensagemDaApi(resposta, 'Não foi possível cadastrar o estágio.')
        )
      }
    } catch {
      setErroProcesso('Não foi possível conectar com o servidor.')
    } finally {
      setSalvandoProcesso(false)
    }
  }

  const propriedadesCabecalho = {
    usuario,
    tela,
    secaoInstituicao,
    aoIrInicio: () => setTela(usuario ? 'sistema' : 'login'),
    aoIrDashboard: () => {
      setTela('sistema')
      setSecaoInstituicao('dashboard')
      buscarResumoDashboard()
    },
    aoIrProcessos: () => {
      setTela('sistema')
      setSecaoInstituicao('processos')
    },
    aoIrAlunos: () => {
      setTela('sistema')
      setSecaoInstituicao('alunos')
    },
    aoIrPrivacidade: () => setTela('privacidade'),
    aoIrTermos: () => setTela('termos'),
    aoSair: sair
  }

  if (tela === 'ativacaoAluno') {
    return (
      <div className="app">
        <Cabecalho {...propriedadesCabecalho} />
        <AtivacaoAluno
          tokenAtivacao={tokenAtivacaoInicial}
          aoConcluir={() => setTela('login')}
        />
      </div>
    )
  }

  if (tela === 'verificacaoEmail') {
    return (
      <div className="app">
        <Cabecalho {...propriedadesCabecalho} />
        <VerificacaoEmail
          tokenVerificacao={tokenVerificacaoInicial}
          aoConcluir={() => setTela('login')}
        />
      </div>
    )
  }

  if (tela === 'privacidade') {
    return (
      <div className="app">
        <Cabecalho {...propriedadesCabecalho} mostrarSair={Boolean(usuario)} />

        <main className="conteudo">
          <header className="cabecalho">
            <p className="cabecalho-contexto">LGPD</p>
            <h2>Política de Privacidade</h2>
            <p className="cabecalho-descricao">
              Versão 1.0 — 27/09/2026
            </p>
          </header>

          <article className="formulario documento-lgpd">
            <div className="secao-cabecalho">
              <h3>Política de Privacidade do GAEA</h3>
              <p>
                Informações sobre o uso e a proteção de dados pessoais no
                sistema, conforme a Lei nº 13.709/2018 (Lei Geral de Proteção
                de Dados Pessoais — LGPD).
              </p>
            </div>

            <section className="documento-secao">
              <h3>1. Sobre esta política</h3>
              <p>
                Esta Política apresenta como os dados pessoais são utilizados
                no GAEA, sistema acadêmico voltado à gestão e ao acompanhamento
                de processos de estágio.
              </p>
            </section>

            <section className="documento-secao documento-destaque">
              <h3>2. Dados tratados pelo GAEA</h3>
              <p>De acordo com as funcionalidades atuais, o sistema trata:</p>
              <ul>
                <li>nome, e-mail e perfil de acesso (ALUNO, EMPRESA ou INSTITUICAO);</li>
                <li>senha protegida por hash BCrypt, sem armazenamento em texto puro;</li>
                <li>nome e e-mail do aluno relacionados ao processo de estágio;</li>
                <li>nome e CNPJ da empresa;</li>
                <li>datas de início e fim e status do processo de estágio;</li>
                <li>CEP e dados de endereço consultados por meio do ViaCEP;</li>
                <li>usuário, ação, recurso e data/hora dos registros de auditoria;</li>
                <li>aceite dos Termos e da Política de Privacidade e sua data/hora.</li>
              </ul>
            </section>

            <section className="documento-secao documento-destaque">
              <h3>3. Finalidades</h3>
              <p>Os dados são utilizados para:</p>
              <ul>
                <li>criar e identificar a conta do usuário;</li>
                <li>autenticar o usuário e controlar o acesso conforme seu perfil;</li>
                <li>cadastrar e acompanhar processos de estágio;</li>
                <li>identificar o aluno e a empresa envolvidos no processo;</li>
                <li>consultar o endereço da empresa a partir do CEP;</li>
                <li>registrar ações importantes para rastreabilidade e segurança;</li>
                <li>registrar o aceite dos Termos e da Política de Privacidade.</li>
              </ul>
            </section>

            <section className="documento-secao documento-destaque">
              <h3>4. Tratamento e segurança</h3>
              <p>
                Os dados são recebidos e persistidos pelo backend e pelo banco
                de dados do GAEA. As senhas são protegidas com BCrypt, a
                autenticação utiliza JWT e a autorização por perfil é
                verificada no backend. O sistema também mantém registros de
                auditoria e se comunica com o ViaCEP para consultar endereços.
              </p>
            </section>

            <section className="documento-secao">
              <h3>5. Serviço externo ViaCEP</h3>
              <p>
                O GAEA envia ao ViaCEP somente o CEP necessário para consultar
                o endereço. Senhas, tokens de autenticação e dados completos
                do processo de estágio não são enviados ao serviço.
              </p>
            </section>

            <section className="documento-secao">
              <h3>6. Retenção</h3>
              <p>
                Os dados são mantidos enquanto forem necessários às
                funcionalidades e à finalidade acadêmica do sistema. O projeto
                ainda não possui exclusão automática por prazo. Em um ambiente
                real, regras específicas de retenção deverão observar as
                finalidades e as obrigações aplicáveis.
              </p>
            </section>

            <section className="documento-secao documento-destaque">
              <h3>7. Seus direitos</h3>
              <p>Conforme aplicável, o titular pode solicitar:</p>
              <ul>
                <li>confirmação da existência de tratamento;</li>
                <li>acesso aos seus dados;</li>
                <li>correção de dados incompletos, inexatos ou desatualizados;</li>
                <li>informações sobre o tratamento realizado;</li>
                <li>eliminação de dados tratados com consentimento, quando aplicável e observadas as hipóteses legais;</li>
                <li>revogação do consentimento, quando ele for a base aplicável.</li>
              </ul>
            </section>

            <section className="documento-secao">
              <h3>8. Contato</h3>
              <p>
                Dúvidas e solicitações relacionadas à privacidade podem ser
                enviadas ao canal de contato do projeto GAEA:
                {' '}feliperafaelniendicker@gmail.com.
              </p>
            </section>

            <section className="documento-secao">
              <h3>9. Atualização</h3>
              <p>
                Esta Política poderá ser atualizada conforme o desenvolvimento
                do projeto. A versão disponível no sistema deve ser consultada
                para conhecer o conteúdo vigente.
              </p>
            </section>
          </article>
        </main>
      </div>
    )
  }

  if (tela === 'termos') {
    return (
      <div className="app">
        <Cabecalho {...propriedadesCabecalho} mostrarSair={Boolean(usuario)} />

        <main className="conteudo">
          <header className="cabecalho">
            <p className="cabecalho-contexto">GAEA</p>
            <h2>Termos de Uso e Termo de Aceite</h2>
            <p className="cabecalho-descricao">
              Versão 1.0 — 27/09/2026
            </p>
          </header>

          <article className="formulario documento-lgpd">
            <div className="secao-cabecalho">
              <h3>Termo de Aceite do GAEA</h3>
              <p>
                Condições para utilização do sistema acadêmico GAEA.
              </p>
            </div>

            <section className="documento-secao">
              <h3>1. Sobre o GAEA</h3>
              <p>
                O GAEA é um projeto acadêmico destinado à gestão e ao
                acompanhamento de processos de estágio.
              </p>
            </section>

            <section className="documento-secao">
              <h3>2. Perfis de usuário</h3>
              <p>
                O sistema possui os perfis ALUNO, EMPRESA e INSTITUICAO.
                As funcionalidades e permissões disponíveis dependem do perfil
                associado à conta.
              </p>
            </section>

            <section className="documento-secao">
              <h3>3. Conta e credenciais</h3>
              <p>
                O usuário deve informar dados corretos, utilizar suas próprias
                credenciais e não compartilhar sua senha ou a conta com outras
                pessoas.
              </p>
            </section>

            <section className="documento-secao">
              <h3>4. Utilização do sistema</h3>
              <p>
                O GAEA deve ser utilizado para suas finalidades acadêmicas.
                Não é permitido tentar acessar áreas sem autorização, usar
                credenciais de terceiros, inserir informações intencionalmente
                falsas ou comprometer a segurança do sistema.
              </p>
            </section>

            <section className="documento-secao">
              <h3>5. Processos de estágio</h3>
              <p>
                Conforme o perfil, o usuário poderá cadastrar, consultar ou
                atualizar informações dos processos. O aluno pode consultar
                somente os processos relacionados ao e-mail de sua conta.
              </p>
            </section>

            <section className="documento-secao">
              <h3>6. Registros de auditoria</h3>
              <p>
                Ações importantes podem ser registradas com o usuário
                responsável, a ação realizada, o recurso relacionado e a
                data/hora, permitindo rastreabilidade e apoio à segurança.
              </p>
            </section>

            <section className="documento-secao">
              <h3>7. Uso do ViaCEP</h3>
              <p>
                O GAEA utiliza o ViaCEP para consultar endereço a partir do
                CEP informado. A disponibilidade dessa consulta também depende
                do serviço externo.
              </p>
            </section>

            <section className="documento-secao">
              <h3>8. Privacidade e proteção de dados</h3>
              <p>
                O tratamento dos dados pessoais é explicado na Política de
                Privacidade do GAEA, que apresenta os dados utilizados, suas
                finalidades, o tratamento realizado e os direitos dos titulares.
              </p>
            </section>

            <section className="documento-secao">
              <h3>9. Disponibilidade do projeto</h3>
              <p>
                Por ser um projeto acadêmico em desenvolvimento, o GAEA pode
                passar por atualizações, manutenção e períodos de
                indisponibilidade.
              </p>
            </section>

            <section className="documento-secao documento-destaque">
              <h3>10. Aceite dos Termos</h3>
              <p>
                Durante o cadastro, o usuário deve marcar a opção específica
                declarando que leu e aceitou estes Termos de Uso e a Política
                de Privacidade. O GAEA registra o aceite e sua respectiva
                data/hora. Sem essa confirmação, o cadastro não é concluído.
              </p>
            </section>

            <section className="documento-secao">
              <h3>11. Contato</h3>
              <p>
                Dúvidas relacionadas ao projeto podem ser enviadas para:
                {' '}feliperafaelniendicker@gmail.com.
              </p>
            </section>
          </article>
        </main>
      </div>
    )
  }

  if (!usuario && tela === 'cadastro') {
    return (
      <div className="app">
        <Cabecalho {...propriedadesCabecalho} />

        <main className="conteudo">
          <header className="cabecalho">
            <p className="cabecalho-contexto">NOVO USUÁRIO</p>
            <h2>Criar conta</h2>
            <p className="cabecalho-descricao">
              Cadastre-se para acessar o GAEA.
            </p>
          </header>

          <section className="formulario">
            <form onSubmit={fazerCadastro}>
              <div className="campo">
                <label htmlFor="nomeCadastro">Nome</label>
                <input
                  id="nomeCadastro"
                  type="text"
                  value={nomeCadastro}
                  onChange={(e) => setNomeCadastro(e.target.value)}
                  required
                />
              </div>

              <div className="campo">
                <label htmlFor="emailCadastro">E-mail</label>
                <input
                  id="emailCadastro"
                  type="email"
                  value={emailCadastro}
                  onChange={(e) => setEmailCadastro(e.target.value)}
                  required
                />
              </div>

              <div className="campo">
                <label htmlFor="senhaCadastro">Senha</label>
                <input
                  id="senhaCadastro"
                  type="password"
                  value={senhaCadastro}
                  onChange={(e) => setSenhaCadastro(e.target.value)}
                  minLength="8"
                  required
                />
              </div>

              <div className="campo">
                <label htmlFor="perfilCadastro">Perfil</label>
                <input
                  id="perfilCadastro"
                  value="Supervisor da Empresa"
                  readOnly
                />
                <small>Contas de aluno são criadas pela instituição de ensino.</small>
              </div>

              <div className="aceite-lgpd">
                <input
                  id="termosAceitos"
                  type="checkbox"
                  checked={termosAceitos}
                  onChange={(e) => setTermosAceitos(e.target.checked)}
                />

                <label htmlFor="termosAceitos">
                  Li e aceito os{' '}
                  <button
                    type="button"
                    className="link-texto"
                    onClick={() => setTela('termos')}
                  >
                    Termos de Uso
                  </button>
                  {' '}e a{' '}
                  <button
                    type="button"
                    className="link-texto"
                    onClick={() => setTela('privacidade')}
                  >
                    Política de Privacidade
                  </button>.
                </label>
              </div>

              {erroCadastro && (
                <p className="mensagem-erro">{erroCadastro}</p>
              )}
              {sucessoCadastro && (
                <p className="mensagem-sucesso" role="status">{sucessoCadastro}</p>
              )}

              <button type="submit">
                Criar conta
              </button>
            </form>

            <div className="acao-secundaria">
              <p>Já possui uma conta?</p>
              <button
                type="button"
                onClick={() => setTela('login')}
              >
                Voltar para o login
              </button>
            </div>
          </section>
        </main>
      </div>
    )
  }

  if (!usuario) {
    return (
      <div className="app">
        <Cabecalho {...propriedadesCabecalho} />

        <main className="conteudo">
          <header className="cabecalho">
            <p className="cabecalho-contexto">ACESSO AO SISTEMA</p>
            <h2>Entrar no GAEA</h2>
            <p className="cabecalho-descricao">
              Informe seus dados para acessar sua área.
            </p>
          </header>

          <section className="formulario">
            <form onSubmit={fazerLogin}>
              <div className="campo">
                <label htmlFor="emailLogin">E-mail</label>
                <input
                  id="emailLogin"
                  type="email"
                  value={emailLogin}
                  onChange={(e) => setEmailLogin(e.target.value)}
                  required
                />
              </div>

              <div className="campo">
                <label htmlFor="senhaLogin">Senha</label>
                <input
                  id="senhaLogin"
                  type="password"
                  value={senhaLogin}
                  onChange={(e) => setSenhaLogin(e.target.value)}
                  required
                />
              </div>

              {erroLogin && (
                <p className="mensagem-erro">{erroLogin}</p>
              )}

              <button type="submit">
                Entrar
              </button>
            </form>

            <div className="acao-secundaria">
              <p>Ainda não possui uma conta?</p>
              <button
                type="button"
                onClick={() => setTela('cadastro')}
              >
                Criar conta
              </button>
            </div>
          </section>
        </main>
      </div>
    )
  }

  if (usuario.perfil !== 'INSTITUICAO') {
    return (
      <div className="app">
        <Cabecalho {...propriedadesCabecalho} mostrarSair />
        <MensagemAcaoProcesso
          mensagem={mensagemAcaoProcesso}
          aoFechar={() => setMensagemAcaoProcesso(null)}
        />

        <main className="conteudo">
          {secaoInstituicao === 'dashboard' ? (
            <>
              <header className="cabecalho">
                <p className="cabecalho-contexto">
                  {usuario.perfil === 'ALUNO' ? 'VISÃO DO ALUNO' : 'VISÃO DO SUPERVISOR'}
                </p>
                <h2>Dashboard de Estágios</h2>
                <p className="cabecalho-descricao">
                  {usuario.perfil === 'ALUNO'
                    ? 'Acompanhe os indicadores dos seus processos de estágio.'
                    : 'Acompanhe os indicadores dos estágios sob sua supervisão confirmada.'}
                </p>
              </header>

              <DashboardProcessos
                resumo={resumoDashboard}
                carregando={carregandoDashboard}
                erro={erroDashboard}
                aoTentarNovamente={() => buscarResumoDashboard()}
              />
            </>
          ) : (
            <>
          <header className="cabecalho">
            <p className="cabecalho-contexto">
              ÁREA DO USUÁRIO
            </p>
            <h2>Olá, {usuario.nome}</h2>
            <p className="cabecalho-descricao">
              Perfil: {usuario.perfil}
            </p>
          </header>

          {usuario.perfil === 'ALUNO' && (
            <section className="formulario">
              <div className="secao-cabecalho">
                <h3>Solicitar novo estágio</h3>
                <p>Informe os dados da empresa e do supervisor responsável.</p>
              </div>

              <form onSubmit={cadastrarEstagio}>
                <div className="dados-somente-leitura" aria-label="Dados do aluno">
                  <div className="campo">
                    <label htmlFor="alunoNome">Nome do aluno</label>
                    <input id="alunoNome" value={usuario.nome} readOnly />
                  </div>
                  <div className="campo">
                    <label htmlFor="alunoEmail">E-mail</label>
                    <input id="alunoEmail" value={usuario.email} readOnly />
                  </div>
                  <div className="campo">
                    <label htmlFor="alunoMatricula">Matrícula (RA/RGM)</label>
                    <input id="alunoMatricula" value={usuario.matricula || ''} readOnly />
                  </div>
                  <div className="campo">
                    <label htmlFor="alunoInstituicao">Instituição</label>
                    <input id="alunoInstituicao" value={usuario.instituicao?.nome || ''} readOnly />
                  </div>
                </div>

                <div className="campo">
                  <label htmlFor="nomeEmpresaAluno">Empresa</label>
                  <input id="nomeEmpresaAluno" value={nomeEmpresa} onChange={(event) => setNomeEmpresa(event.target.value)} maxLength="255" required />
                </div>
                <div className="campo">
                  <label htmlFor="cnpjEmpresaAluno">CNPJ</label>
                  <input id="cnpjEmpresaAluno" value={cnpjEmpresa} onChange={(event) => setCnpjEmpresa(event.target.value)} placeholder="00.000.000/0000-00" maxLength="18" required />
                </div>
                <div className="campo">
                  <label htmlFor="cepEmpresaAluno">CEP da empresa</label>
                  <div className="campo-cep">
                    <input id="cepEmpresaAluno" value={cepEmpresa} onChange={(event) => setCepEmpresa(event.target.value)} placeholder="00000-000" maxLength="9" required />
                    <button type="button" onClick={buscarCep}>Buscar CEP</button>
                  </div>
                  {erroCep && <p className="mensagem-erro">{erroCep}</p>}
                </div>
                <div className="campo">
                  <label htmlFor="enderecoEmpresaAluno">Endereço da empresa</label>
                  <input id="enderecoEmpresaAluno" value={enderecoEmpresa} onChange={(event) => setEnderecoEmpresa(event.target.value)} maxLength="500" required />
                </div>
                <div className="campo">
                  <label htmlFor="emailSupervisorAluno">E-mail do supervisor</label>
                  <input id="emailSupervisorAluno" type="email" value={emailSupervisor} onChange={(event) => setEmailSupervisor(event.target.value)} maxLength="255" required />
                </div>
                <div className="campo">
                  <label htmlFor="dataInicioAluno">Data de início</label>
                  <input id="dataInicioAluno" type="date" value={dataInicio} onChange={(event) => setDataInicio(event.target.value)} required />
                </div>
                <div className="campo">
                  <label htmlFor="dataFimAluno">Data de término</label>
                  <input id="dataFimAluno" type="date" value={dataFim} onChange={(event) => setDataFim(event.target.value)} min={dataInicio} required />
                </div>

                {erroProcesso && <p className="mensagem-erro">{erroProcesso}</p>}
                <button type="submit" disabled={salvandoProcesso}>
                  {salvandoProcesso ? 'Enviando...' : 'Solicitar estágio'}
                </button>
              </form>
            </section>
          )}


          <section className="formulario">
            <div className="secao-cabecalho">
              <h3>
                {usuario.perfil === 'ALUNO'
                  ? 'Meus processos'
                  : 'Solicitações de supervisão'}
              </h3>

              <p>
                {usuario.perfil === 'ALUNO'
                  ? 'Acompanhe suas solicitações de estágio.'
                  : 'Confirme ou recuse os vínculos direcionados ao seu e-mail verificado.'}
              </p>
            </div>

            {usuario.perfil === 'ALUNO' ? (
              processos.length === 0 ? (
                <p className="estado-vazio">
                  Nenhum processo de estágio foi encontrado para sua conta.
                </p>
              ) : (
                <div className="lista-processos lista-processos-aluno">
                  <div className="lista-cabecalho">
                    <span>Empresa</span>
                    <span>Período</span>
                    <span>Status</span>
                    <span>Ações</span>
                  </div>

                  {processos.map((processo) => (
                    <div className="processo-container" key={processo.id}>
                      <div className="processo">
                        <p>
                          <strong>Empresa</strong>
                          <span>{processo.nomeEmpresa}</span>
                        </p>

                        <p>
                          <strong>Período</strong>
                          <span>
                            {processo.dataInicio} — {processo.dataFim}
                          </span>
                        </p>

                        <p>
                          <strong>Status</strong>
                          <span className={`status status-${processo.status}`}>
                            {rotulosStatus[processo.status] || processo.status}
                          </span>
                        </p>

                        <p>
                          <strong>Supervisor</strong>
                          <span>
                            {rotulosVinculo[processo.vinculoSupervisor?.situacao]
                              || 'Vínculo legado não informado'}
                          </span>
                        </p>

                        {processo.vinculoSupervisor?.situacao === 'RECUSADO' && (
                          <div className="troca-supervisor">
                            <p>
                              <strong>Justificativa da recusa</strong>
                              <span>{processo.vinculoSupervisor.justificativaRecusa}</span>
                            </p>
                            <label htmlFor={`novo-supervisor-${processo.id}`}>Novo e-mail do supervisor</label>
                            <input
                              id={`novo-supervisor-${processo.id}`}
                              type="email"
                              value={novosSupervisores[processo.id] || ''}
                              onChange={(event) => setNovosSupervisores((atuais) => ({
                                ...atuais,
                                [processo.id]: event.target.value
                              }))}
                            />
                            <button
                              type="button"
                              disabled={acaoSupervisao === `troca-${processo.id}`}
                              onClick={() => indicarNovoSupervisor(processo)}
                            >
                              {acaoSupervisao === `troca-${processo.id}` ? 'Enviando...' : 'Indicar outro supervisor'}
                            </button>
                          </div>
                        )}

                        <div className="processo-acoes">
                          <button
                            className="acao-historico"
                            type="button"
                            onClick={() => buscarHistorico(processo.id)}
                          >
                            {historicoAberto === processo.id ? 'Fechar histórico' : 'Ver histórico'}
                          </button>
                          <button
                            className="acao-pendencias"
                            type="button"
                            onClick={() => buscarPendencias(processo.id)}
                          >
                            {pendenciasAbertas === processo.id ? 'Fechar pendências' : 'Ver pendências'}
                          </button>
                          <button
                            className="acao-prazos"
                            type="button"
                            onClick={() => buscarPrazos(processo.id)}
                          >
                            {prazosAbertos === processo.id ? 'Fechar prazos' : 'Ver prazos'}
                          </button>
                        </div>
                      </div>

                      {historicoAberto === processo.id && (
                        <HistoricoProcesso
                          itens={historicos[processo.id] || []}
                          carregando={carregandoHistorico === processo.id}
                          erro={errosHistorico[processo.id]}
                        />
                      )}
                      {pendenciasAbertas === processo.id && (
                        <PendenciasProcesso
                          itens={pendencias[processo.id] || []}
                          carregando={carregandoPendencias === processo.id}
                          erro={errosPendencias[processo.id]}
                          podeGerenciar={false}
                          resolvendoId={null}
                        />
                      )}
                      {prazosAbertos === processo.id && (
                        <PrazosProcesso
                          itens={prazos[processo.id] || []}
                          carregando={carregandoPrazos === processo.id}
                          erro={errosPrazos[processo.id]}
                          podeGerenciar={false}
                        />
                      )}
                    </div>
                  ))}
                </div>
              )
            ) : (
              <div className="solicitacoes-supervisao">
                {!usuario.emailVerificado ? (
                  <div className="estado-vazio">
                    <h4>Verifique seu e-mail</h4>
                    <p>As solicitações ficam protegidas até a confirmação da titularidade do e-mail.</p>
                    <button type="button" disabled={acaoSupervisao === 'reenviar-email'} onClick={reenviarVerificacaoEmail}>
                      {acaoSupervisao === 'reenviar-email' ? 'Solicitando...' : 'Reenviar link de verificação'}
                    </button>
                  </div>
                ) : solicitacoesSupervisao.length === 0 ? (
                  <p className="estado-vazio">Nenhuma solicitação de supervisão pendente.</p>
                ) : (
                  solicitacoesSupervisao.map((solicitacao) => (
                    <article className="solicitacao-supervisao" key={solicitacao.vinculoId}>
                      <h4>{solicitacao.empresa}</h4>
                      <dl>
                        <div><dt>Aluno</dt><dd>{solicitacao.aluno}</dd></div>
                        <div><dt>Instituição</dt><dd>{solicitacao.instituicao}</dd></div>
                        <div><dt>CNPJ</dt><dd>{solicitacao.cnpjEmpresa}</dd></div>
                        <div><dt>Período</dt><dd>{solicitacao.dataInicio} — {solicitacao.dataFim}</dd></div>
                      </dl>
                      <label htmlFor={`justificativa-${solicitacao.vinculoId}`}>
                        Justificativa para recusa
                      </label>
                      <textarea
                        id={`justificativa-${solicitacao.vinculoId}`}
                        value={justificativasRecusa[solicitacao.vinculoId] || ''}
                        onChange={(event) => setJustificativasRecusa((atuais) => ({
                          ...atuais,
                          [solicitacao.vinculoId]: event.target.value
                        }))}
                        maxLength="1000"
                        placeholder="Obrigatória somente em caso de recusa"
                      />
                      <div className="processo-acoes">
                        <button type="button" disabled={acaoSupervisao !== null} onClick={() => responderSupervisao(solicitacao, 'confirmar')}>
                          Confirmar supervisão
                        </button>
                        <button className="botao-perigo" type="button" disabled={acaoSupervisao !== null} onClick={() => responderSupervisao(solicitacao, 'recusar')}>
                          Recusar
                        </button>
                      </div>
                    </article>
                  ))
                )}
                {erroSupervisao && <p className="mensagem-erro" role="alert">{erroSupervisao}</p>}
              </div>
            )}
          </section>

          {usuario.perfil === 'EMPRESA' && usuario.emailVerificado && (
            <section className="formulario">
              <div className="secao-cabecalho">
                <h3>Estágios supervisionados</h3>
                <p>Consulte o histórico, as pendências e os prazos dos processos confirmados.</p>
              </div>

              {processos.length === 0 ? (
                <p className="estado-vazio">Nenhum processo com supervisão confirmada.</p>
              ) : (
                <div className="lista-processos lista-processos-aluno">
                  {processos.map((processo) => (
                    <div className="processo-container" key={processo.id}>
                      <div className="processo">
                        <p><strong>Aluno</strong><span>{processo.nomeAluno}</span></p>
                        <p><strong>Empresa</strong><span>{processo.nomeEmpresa}</span></p>
                        <p>
                          <strong>Status</strong>
                          <span className={`status status-${processo.status}`}>
                            {rotulosStatus[processo.status] || processo.status}
                          </span>
                        </p>
                        <div className="processo-acoes">
                          <button
                            className="acao-historico"
                            type="button"
                            onClick={() => buscarHistorico(processo.id)}
                          >
                            {historicoAberto === processo.id ? 'Fechar histórico' : 'Ver histórico'}
                          </button>
                          <button
                            className="acao-pendencias"
                            type="button"
                            onClick={() => buscarPendencias(processo.id)}
                          >
                            {pendenciasAbertas === processo.id ? 'Fechar pendências' : 'Ver pendências'}
                          </button>
                          <button
                            className="acao-prazos"
                            type="button"
                            onClick={() => buscarPrazos(processo.id)}
                          >
                            {prazosAbertos === processo.id ? 'Fechar prazos' : 'Ver prazos'}
                          </button>
                        </div>
                      </div>
                      {historicoAberto === processo.id && (
                        <HistoricoProcesso
                          itens={historicos[processo.id] || []}
                          carregando={carregandoHistorico === processo.id}
                          erro={errosHistorico[processo.id]}
                        />
                      )}
                      {pendenciasAbertas === processo.id && (
                        <PendenciasProcesso
                          itens={pendencias[processo.id] || []}
                          carregando={carregandoPendencias === processo.id}
                          erro={errosPendencias[processo.id]}
                          podeGerenciar={false}
                          resolvendoId={null}
                        />
                      )}
                      {prazosAbertos === processo.id && (
                        <PrazosProcesso
                          itens={prazos[processo.id] || []}
                          carregando={carregandoPrazos === processo.id}
                          erro={errosPrazos[processo.id]}
                          podeGerenciar={false}
                        />
                      )}
                    </div>
                  ))}
                </div>
              )}
            </section>
          )}
            </>
          )}
        </main>
      </div>
    )
  }

  return (
    <div className="app">
      <Cabecalho {...propriedadesCabecalho} mostrarSair />
      <MensagemAcaoProcesso
        mensagem={mensagemAcaoProcesso}
        aoFechar={() => setMensagemAcaoProcesso(null)}
      />

      {processoReabertura && (
        <ModalReabertura
          processo={processoReabertura}
          justificativa={justificativaReabertura}
          erro={erroReabertura}
          carregando={reaberturaEmAndamento}
          aoMudarJustificativa={(valor) => {
            setJustificativaReabertura(valor)
            if (erroReabertura) {
              setErroReabertura('')
            }
          }}
          aoCancelar={cancelarReabertura}
          aoConfirmar={confirmarReabertura}
        />
      )}

      {processoNovaPendencia && (
        <ModalNovaPendencia
          processo={processoNovaPendencia}
          descricao={descricaoPendencia}
          erro={erroNovaPendencia}
          carregando={salvandoPendencia}
          aoMudarDescricao={(valor) => {
            setDescricaoPendencia(valor)
            if (erroNovaPendencia) {
              setErroNovaPendencia('')
            }
          }}
          aoCancelar={cancelarNovaPendencia}
          aoConfirmar={registrarPendencia}
        />
      )}

      {processoPrazo && (
        <ModalPrazo
          processo={processoPrazo}
          prazo={prazoEdicao}
          descricao={descricaoPrazo}
          dataVencimento={dataVencimentoPrazo}
          erro={erroPrazo}
          carregando={salvandoPrazo}
          aoMudarDescricao={(valor) => {
            setDescricaoPrazo(valor)
            setErroPrazo('')
          }}
          aoMudarData={(valor) => {
            setDataVencimentoPrazo(valor)
            setErroPrazo('')
          }}
          aoCancelar={cancelarPrazo}
          aoConfirmar={salvarPrazo}
        />
      )}

      <main className="conteudo">
        {secaoInstituicao === 'dashboard' ? (
          <>
            <header className="cabecalho">
              <p className="cabecalho-contexto">VISÃO INSTITUCIONAL</p>
              <h2>Dashboard de Estágios</h2>
              <p className="cabecalho-descricao">
                Acompanhe os principais indicadores dos processos cadastrados.
              </p>
            </header>

            <DashboardProcessos
              resumo={resumoDashboard}
              carregando={carregandoDashboard}
              erro={erroDashboard}
              aoTentarNovamente={() => buscarResumoDashboard()}
            />
          </>
        ) : secaoInstituicao === 'alunos' ? (
          <GestaoAlunos token={token} />
        ) : (
          <>
        <header className="cabecalho">
          <div>
            <p className="cabecalho-contexto">GESTÃO ACADÊMICA</p>
            <h2>Processos de Estágio</h2>
            <p className="cabecalho-descricao">
              Cadastre e acompanhe os processos de estágio.
            </p>
            <div className="acoes-cabecalho">
  <button
    type="button"
    onClick={() => {
      if (mostrarAuditoria) {
        setMostrarAuditoria(false)
      } else {
        buscarAuditoria()
      }
    }}
  >
    {mostrarAuditoria ? 'Voltar aos processos' : 'Consultar auditoria'}
  </button>
</div>
          </div>
        </header>

          {mostrarAuditoria && (
  <section className="acompanhamento">
    <div className="secao-cabecalho">
      <h3>Logs de Auditoria</h3>
      <p>
        Registro das principais ações realizadas no sistema.
      </p>
    </div>

    {logsAuditoria.length === 0 ? (
      <p className="estado-vazio">
        Nenhum registro de auditoria encontrado.
      </p>
    ) : (
      <div className="tabela-auditoria">
        <div className="auditoria-cabecalho">
          <span>Usuário</span>
          <span>Ação</span>
          <span>Recurso</span>
          <span>Data e hora</span>
        </div>

        {logsAuditoria.map((log) => (
          <div className="auditoria-linha" key={log.id}>
            <span>{log.usuario}</span>
            <span>{log.acao}</span>
            <span>{log.recurso}</span>
            <span>
              {log.dataHora
                ? new Date(log.dataHora).toLocaleString('pt-BR')
                : '-'}
            </span>
          </div>
        ))}
      </div>
    )}
  </section>
)}

        <section className="acompanhamento">
          <div className="secao-cabecalho">
            <h3>Acompanhamento de Estágios</h3>
            <p>
              Consulte e atualize o andamento dos processos cadastrados.
            </p>
          </div>

          {processos.length === 0 ? (
            <p className="estado-vazio">
              Nenhum processo cadastrado.
            </p>
          ) : (
            <div className="lista-processos">
              <div
                className="lista-cabecalho"
                aria-hidden="true"
              >
                <span>Aluno</span>
                <span>Empresa</span>
                <span>Período</span>
                <span>Status</span>
                <span>Ações</span>
              </div>

              {processos.map((processo) => (
                <div
                  className="processo-container"
                  key={processo.id}
                  data-processo-id={processo.id}
                  tabIndex="-1"
                >
                  <div className="processo">
                    <p>
                      <strong>Aluno</strong>
                      <span>{processo.nomeAluno}</span>
                    </p>

                    <p>
                      <strong>Empresa</strong>
                      <span>{processo.nomeEmpresa}</span>
                    </p>

                    <p>
                      <strong>Período</strong>
                      <span>
                        {processo.dataInicio} — {processo.dataFim}
                      </span>
                    </p>

                    <p>
                      <strong>Status</strong>
                      <span
                        className={`status status-${processo.status}`}
                      >
                        {rotulosStatus[processo.status] || processo.status}
                      </span>
                    </p>

                    <div className="processo-acoes">
                      {processo.status === 'PENDENTE'
                        && processo.vinculoSupervisor?.situacao === 'CONFIRMADO' && (
                        <button
                          className="acao-analise"
                          type="button"
                          onClick={() => alterarStatus(processo.id, 'EM_ANALISE')}
                        >
                          Iniciar análise
                        </button>
                      )}

                      {processo.status === 'PENDENTE'
                        && processo.vinculoSupervisor?.situacao !== 'CONFIRMADO' && (
                        <span className="status status-PENDENTE">
                          Aguardando confirmação do supervisor
                        </span>
                      )}

                      {processo.status === 'EM_ANALISE' && (
                        <>
                          <button
                            className="acao-aprovar"
                            type="button"
                            onClick={() => alterarStatus(processo.id, 'APROVADO')}
                          >
                            Aprovar
                          </button>
                          <button
                            className="acao-reprovar"
                            type="button"
                            onClick={() => alterarStatus(processo.id, 'REPROVADO')}
                          >
                            Reprovar
                          </button>
                        </>
                      )}

                      {(processo.status === 'APROVADO' || processo.status === 'REPROVADO') && (
                        <button
                          className="acao-reabrir"
                          type="button"
                          onClick={() => abrirModalReabertura(processo)}
                        >
                          Reabrir
                        </button>
                      )}

                      <button
                        className="acao-historico"
                        type="button"
                        onClick={() => buscarHistorico(processo.id)}
                      >
                        {historicoAberto === processo.id ? 'Fechar histórico' : 'Histórico'}
                      </button>
                      <button
                        className="acao-pendencias"
                        type="button"
                        onClick={() => buscarPendencias(processo.id)}
                      >
                        {pendenciasAbertas === processo.id ? 'Fechar pendências' : 'Pendências'}
                      </button>
                      <button
                        className="acao-prazos"
                        type="button"
                        onClick={() => buscarPrazos(processo.id)}
                      >
                        {prazosAbertos === processo.id ? 'Fechar prazos' : 'Prazos'}
                      </button>
                    </div>
                  </div>

                  {historicoAberto === processo.id && (
                    <HistoricoProcesso
                      itens={historicos[processo.id] || []}
                      carregando={carregandoHistorico === processo.id}
                      erro={errosHistorico[processo.id]}
                    />
                  )}
                  {pendenciasAbertas === processo.id && (
                    <PendenciasProcesso
                      itens={pendencias[processo.id] || []}
                      carregando={carregandoPendencias === processo.id}
                      erro={errosPendencias[processo.id]}
                      podeGerenciar
                      resolvendoId={resolvendoPendencia}
                      aoNovaPendencia={() => abrirNovaPendencia(processo)}
                      aoResolver={(pendenciaId) => resolverPendenciaDoProcesso(processo.id, pendenciaId)}
                    />
                  )}
                  {prazosAbertos === processo.id && (
                    <PrazosProcesso
                      itens={prazos[processo.id] || []}
                      carregando={carregandoPrazos === processo.id}
                      erro={errosPrazos[processo.id]}
                      podeGerenciar
                      aoNovoPrazo={() => abrirPrazo(processo)}
                      aoEditarPrazo={(prazo) => abrirPrazo(processo, prazo)}
                    />
                  )}
                </div>
              ))}
            </div>
          )}
        </section>
          </>
        )}
      </main>
    </div>
  )
}

export default App
