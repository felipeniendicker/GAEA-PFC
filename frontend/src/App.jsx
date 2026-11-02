import { useEffect, useRef, useState } from 'react'
import './App.css'

const API = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '')

const rotulosStatus = {
  PENDENTE: 'Pendente',
  EM_ANALISE: 'Em análise',
  APROVADO: 'Aprovado',
  REPROVADO: 'Reprovado'
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

function HistoricoProcesso({ itens }) {
  return (
    <div className="historico-processo">
      <h4>Histórico de status</h4>

      {itens.length === 0 ? (
        <p>Nenhuma alteração de status registrada.</p>
      ) : (
        itens.map((item) => (
          <div className="historico-item" key={item.id}>
            <div>
              <strong>
                {rotulosStatus[item.statusAnterior] || item.statusAnterior}
                {' → '}
                {rotulosStatus[item.statusNovo] || item.statusNovo}
              </strong>
              <span>{item.tipoOperacao === 'REABERTURA' ? 'Reabertura' : 'Alteração normal'}</span>
            </div>
            <span>{item.usuarioResponsavel}</span>
            <span>{new Date(item.dataHora).toLocaleString('pt-BR')}</span>
            {item.justificativa && (
              <p><strong>Justificativa:</strong> {item.justificativa}</p>
            )}
          </div>
        ))
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

function DashboardInstitucional({ resumo, carregando, erro, aoTentarNovamente }) {
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
          {usuario?.perfil === 'INSTITUICAO' && mostrarSair ? (
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
              <button
                className={tela === 'sistema' && secaoInstituicao === 'alunos' ? 'ativo' : ''}
                type="button"
                onClick={aoIrAlunos}
              >
                Gestão de Alunos
              </button>
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
  const tokenAtivacaoInicial = new URLSearchParams(window.location.hash.slice(1)).get('conviteAluno') || ''
  const [usuario, setUsuario] = useState(null)
  const [token, setToken] = useState('')

  const [tela, setTela] = useState(tokenAtivacaoInicial ? 'ativacaoAluno' : 'login')

  const [emailLogin, setEmailLogin] = useState('')
  const [senhaLogin, setSenhaLogin] = useState('')
  const [erroLogin, setErroLogin] = useState('')

  const [nomeCadastro, setNomeCadastro] = useState('')
  const [emailCadastro, setEmailCadastro] = useState('')
  const [senhaCadastro, setSenhaCadastro] = useState('')
  const [termosAceitos, setTermosAceitos] = useState(false)
  const [erroCadastro, setErroCadastro] = useState('')

  const [nomeAluno, setNomeAluno] = useState('')
  const [emailAluno, setEmailAluno] = useState('')
  const [nomeEmpresa, setNomeEmpresa] = useState('')
  const [cnpjEmpresa, setCnpjEmpresa] = useState('')
  const [dataInicio, setDataInicio] = useState('')
  const [dataFim, setDataFim] = useState('')
  const [erroProcesso, setErroProcesso] = useState('')
  const [processos, setProcessos] = useState([])
  const [cepEmpresa, setCepEmpresa] = useState('')
  const [enderecoEmpresa, setEnderecoEmpresa] = useState(null)
  const [erroCep, setErroCep] = useState('')
  const [logsAuditoria, setLogsAuditoria] = useState([])
  const [mostrarAuditoria, setMostrarAuditoria] = useState(false)
  const [historicos, setHistoricos] = useState({})
  const [historicoAberto, setHistoricoAberto] = useState(null)
  const [secaoInstituicao, setSecaoInstituicao] = useState('dashboard')
  const [resumoDashboard, setResumoDashboard] = useState(null)
  const [carregandoDashboard, setCarregandoDashboard] = useState(false)
  const [erroDashboard, setErroDashboard] = useState('')
  const [mensagemAcaoProcesso, setMensagemAcaoProcesso] = useState(null)
  const [processoReabertura, setProcessoReabertura] = useState(null)
  const [justificativaReabertura, setJustificativaReabertura] = useState('')
  const [erroReabertura, setErroReabertura] = useState('')
  const [reaberturaEmAndamento, setReaberturaEmAndamento] = useState(false)

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
        perfil: dados.perfil
      })

      setToken(dados.token)
      setSenhaLogin('')
      setSecaoInstituicao('dashboard')
      setTela('sistema')

      if (dados.perfil === 'INSTITUICAO') {
        buscarProcessos(dados.token)
        buscarResumoDashboard(dados.token)
      } else if (dados.perfil === 'ALUNO') {
        buscarMeusProcessos(dados.token)
      }
    } catch {
      setErroLogin('Não foi possível conectar com o servidor.')
    }
  }

  async function fazerCadastro(event) {
    event.preventDefault()
    setErroCadastro('')

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

      alert('Cadastro realizado com sucesso! Agora faça o login.')

      setNomeCadastro('')
      setEmailCadastro('')
      setSenhaCadastro('')
      setTermosAceitos(false)
      setTela('login')
    } catch {
      setErroCadastro('Não foi possível conectar com o servidor.')
    }
  }

  function sair() {
    setUsuario(null)
    setToken('')
    setProcessos([])
    setResumoDashboard(null)
    setErroDashboard('')
    setSecaoInstituicao('dashboard')
    setMensagemAcaoProcesso(null)
    setProcessoReabertura(null)
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

    try {
      const resposta = await fetch(`${API}/api/processos/${id}/historico`, {
        headers: {
          Authorization: `Bearer ${token}`
        }
      })

      if (!resposta.ok) {
        exibirMensagemAcao(
          await mensagemDaApi(resposta, 'Não foi possível consultar o histórico.'),
          'erro'
        )
        return
      }

      const dados = await resposta.json()
      setHistoricos((atuais) => ({ ...atuais, [id]: dados }))
      setHistoricoAberto(id)
    } catch {
      exibirMensagemAcao('Não foi possível conectar com o servidor.', 'erro')
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
  setEnderecoEmpresa(null)

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
    setEnderecoEmpresa(dados)
  } catch {
    setErroCep('Não foi possível conectar com o servidor.')
  }
}

  async function cadastrarEstagio(event) {
    event.preventDefault()

    setErroProcesso('')

    const nomeDoAluno = nomeAluno.trim()
    const emailDoAluno = emailAluno.trim().toLowerCase()
    const empresa = nomeEmpresa.trim()

    if (!nomeDoAluno || !empresa) {
      setErroProcesso('Preencha o nome do aluno e o nome da empresa.')
      return
    }

    if (!emailValido(emailDoAluno)) {
      setErroProcesso('Informe um e-mail de aluno válido.')
      return
    }

    if (!cnpjValido(cnpjEmpresa)) {
      setErroProcesso('Informe um CNPJ válido.')
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
      nomeAluno: nomeDoAluno,
      emailAluno: emailDoAluno,
      nomeEmpresa: empresa,
      cnpjEmpresa,
      dataInicio,
      dataFim
    }

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

        setNomeAluno('')
        setEmailAluno('')
        setNomeEmpresa('')
        setCnpjEmpresa('')
        setDataInicio('')
        setDataFim('')

        exibirMensagemAcao('Estágio cadastrado com sucesso!')
        await buscarResumoDashboard()
      } else {
        setErroProcesso(
          await mensagemDaApi(resposta, 'Não foi possível cadastrar o estágio.')
        )
      }
    } catch {
      setErroProcesso('Não foi possível conectar com o servidor.')
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
          <header className="cabecalho">
            <p className="cabecalho-contexto">
              ÁREA DO USUÁRIO
            </p>
            <h2>Olá, {usuario.nome}</h2>
            <p className="cabecalho-descricao">
              Perfil: {usuario.perfil}
            </p>
          </header>


          <section className="formulario">
            <div className="secao-cabecalho">
              <h3>
                {usuario.perfil === 'ALUNO'
                  ? 'Área do Aluno'
                  : 'Área da Empresa'}
              </h3>

              <p>
                {usuario.perfil === 'ALUNO'
                  ? 'Acompanhe seus processos de estágio.'
                  : 'Você está autenticado no GAEA.'}
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

                        <div className="processo-acoes">
                          <button
                            className="acao-historico"
                            type="button"
                            onClick={() => buscarHistorico(processo.id)}
                          >
                            {historicoAberto === processo.id ? 'Fechar histórico' : 'Ver histórico'}
                          </button>
                        </div>
                      </div>

                      {historicoAberto === processo.id && (
                        <HistoricoProcesso itens={historicos[processo.id] || []} />
                      )}
                    </div>
                  ))}
                </div>
              )
            ) : (
              <p>
                As funcionalidades disponíveis são apresentadas conforme
                o perfil e as permissões do usuário.
              </p>
            )}
          </section>
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

            <DashboardInstitucional
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

        <section className="formulario">
          <div className="secao-cabecalho">
            <h3>Novo estágio</h3>
            <p>
              Preencha os dados abaixo para iniciar um novo processo.
            </p>
          </div>

          <form onSubmit={cadastrarEstagio}>
            <div className="campo">
              <label htmlFor="nomeAluno">Nome do aluno</label>
              <input
                id="nomeAluno"
                type="text"
                value={nomeAluno}
                onChange={(e) => setNomeAluno(e.target.value)}
                required
              />
            </div>

            <div className="campo">
              <label htmlFor="emailAluno">E-mail do aluno</label>
              <input
                id="emailAluno"
                type="email"
                value={emailAluno}
                onChange={(e) => setEmailAluno(e.target.value)}
                required
              />
            </div>

            <div className="campo">
              <label htmlFor="nomeEmpresa">Empresa</label>
              <input
                id="nomeEmpresa"
                type="text"
                value={nomeEmpresa}
                onChange={(e) => setNomeEmpresa(e.target.value)}
                required
              />
            </div>

            <div className="campo">
              <label htmlFor="cnpjEmpresa">CNPJ</label>
              <input
                id="cnpjEmpresa"
                type="text"
                value={cnpjEmpresa}
                onChange={(e) => setCnpjEmpresa(e.target.value)}
              placeholder="00.000.000/0000-00"
              maxLength="18"
                required
              />
            </div>

            <div className="campo">
  <label htmlFor="cepEmpresa">CEP da empresa</label>

  <div className="campo-cep">
    <input
      id="cepEmpresa"
      type="text"
      placeholder="00000000"
      value={cepEmpresa}
      onChange={(e) => setCepEmpresa(e.target.value)}
      maxLength="9"
    />

    <button
      type="button"
      onClick={buscarCep}
    >
      Buscar CEP
    </button>
  </div>

  {erroCep && (
    <p className="mensagem-erro">{erroCep}</p>
  )}
</div>

<div className="campo">
  <label>Endereço encontrado</label>

  <div className="endereco-cep">
    {enderecoEmpresa ? (
      <>
        <strong>
          {enderecoEmpresa.logradouro || 'Logradouro não informado'}
        </strong>

        <span>
          {enderecoEmpresa.bairro || 'Bairro não informado'}
        </span>

        <span>
          {enderecoEmpresa.localidade}/{enderecoEmpresa.uf}
        </span>
      </>
    ) : (
      <span>Consulte o CEP para visualizar o endereço.</span>
    )}
  </div>
</div>

            <div className="campo">
              <label htmlFor="dataInicio">Data de início</label>
              <input
                id="dataInicio"
                type="date"
                value={dataInicio}
                onChange={(e) => setDataInicio(e.target.value)}
                required
              />
            </div>

            <div className="campo">
              <label htmlFor="dataFim">Data de término</label>
              <input
                id="dataFim"
                type="date"
                value={dataFim}
                onChange={(e) => setDataFim(e.target.value)}
                min={dataInicio}
                required
              />
            </div>

            {erroProcesso && (
              <p className="mensagem-erro">{erroProcesso}</p>
            )}

            <button type="submit">
              Cadastrar estágio
            </button>
          </form>
        </section>

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
                      {processo.status === 'PENDENTE' && (
                        <button
                          className="acao-analise"
                          type="button"
                          onClick={() => alterarStatus(processo.id, 'EM_ANALISE')}
                        >
                          Iniciar análise
                        </button>
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
                    </div>
                  </div>

                  {historicoAberto === processo.id && (
                    <HistoricoProcesso itens={historicos[processo.id] || []} />
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
