import { useEffect, useMemo, useRef, useState } from 'react'
import { formatarDataHora, apresentacaoInstituicao } from '../../utils/apresentacao'
import './GestaoAlunos.css'

const SITUACOES = {
  ATIVADO: { rotulo: 'Conta ativada', tom: 'sucesso' },
  AGUARDANDO_ATIVACAO: { rotulo: 'Aguardando ativação', tom: 'informacao' },
  CONVITE_EXPIRADO: { rotulo: 'Convite expirado', tom: 'perigo' },
  CONVITE_CANCELADO: { rotulo: 'Convite substituído', tom: 'neutro' },
  SEM_CONVITE: { rotulo: 'Sem convite', tom: 'alerta' },
  DESATIVADO: { rotulo: 'Aluno desativado', tom: 'inativo' }
}

const FILTROS = [
  { valor: 'TODOS', rotulo: 'Todas as situações' },
  ...Object.entries(SITUACOES).map(([valor, configuracao]) => ({
    valor,
    rotulo: configuracao.rotulo
  }))
]

const SITUACOES_COM_CONVITE_PENDENTE = new Set([
  'CONVITE_EXPIRADO',
  'CONVITE_CANCELADO',
  'SEM_CONVITE'
])

function emailValido(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
}

function normalizarPesquisa(valor) {
  return String(valor || '')
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase()
}

async function mensagemDaApi(resposta, mensagemPadrao) {
  try {
    const dados = await resposta.json()
    return dados.mensagem || mensagemPadrao
  } catch {
    return mensagemPadrao
  }
}

function BadgeSituacao({ situacao }) {
  const configuracao = SITUACOES[situacao] || {
    rotulo: situacao || 'Situação não informada',
    tom: 'neutro'
  }

  return (
    <span className={`gestao-alunos__badge gestao-alunos__badge--${configuracao.tom}`}>
      {configuracao.rotulo}
    </span>
  )
}

export default function GestaoAlunos({ apiBase, token, instituicao }) {
  const [alunos, setAlunos] = useState([])
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [matricula, setMatricula] = useState('')
  const [busca, setBusca] = useState('')
  const [filtro, setFiltro] = useState('TODOS')
  const [cadastroAberto, setCadastroAberto] = useState(false)
  const [erro, setErro] = useState('')
  const [mensagem, setMensagem] = useState('')
  const [carregando, setCarregando] = useState(true)
  const [acaoEmAndamento, setAcaoEmAndamento] = useState('')
  const [edicao, setEdicao] = useState(null)
  const [convite, setConvite] = useState(null)
  const [confirmacaoDesativacao, setConfirmacaoDesativacao] = useState(null)
  const nomeCadastroRef = useRef(null)
  const conviteRef = useRef(null)
  const apresentacaoInstitucional = apresentacaoInstituicao(instituicao)
  const nomeInstituicao = apresentacaoInstitucional.nomeCompleto
  const siglaInstituicao = apresentacaoInstitucional.sigla

  useEffect(() => {
    const controle = new AbortController()

    async function carregarAlunos() {
      try {
        const resposta = await fetch(`${apiBase}/api/instituicoes/alunos`, {
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
        if (!controle.signal.aborted) setCarregando(false)
      }
    }

    carregarAlunos()
    return () => controle.abort()
  }, [apiBase, token])

  useEffect(() => {
    if (cadastroAberto) nomeCadastroRef.current?.focus()
  }, [cadastroAberto])

  useEffect(() => {
    if (convite) conviteRef.current?.focus()
  }, [convite])

  const indicadores = useMemo(() => ({
    total: alunos.length,
    ativados: alunos.filter((aluno) => aluno.situacaoAtivacao === 'ATIVADO').length,
    aguardando: alunos.filter((aluno) => aluno.situacaoAtivacao === 'AGUARDANDO_ATIVACAO').length,
    atencao: alunos.filter((aluno) => SITUACOES_COM_CONVITE_PENDENTE.has(aluno.situacaoAtivacao)).length
  }), [alunos])

  const alunosFiltrados = useMemo(() => {
    const termo = normalizarPesquisa(busca.trim())

    return alunos.filter((aluno) => {
      const correspondeSituacao = filtro === 'TODOS' || aluno.situacaoAtivacao === filtro
      const correspondeBusca = !termo || [aluno.nome, aluno.email, aluno.matricula]
        .some((valor) => normalizarPesquisa(valor).includes(termo))
      return correspondeSituacao && correspondeBusca
    })
  }, [alunos, busca, filtro])

  const filtrosAtivos = Boolean(busca.trim()) || filtro !== 'TODOS'

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

  function limparFeedback() {
    setErro('')
    setMensagem('')
  }

  async function cadastrarAluno(event) {
    event.preventDefault()
    limparFeedback()

    const nomeNormalizado = nome.trim()
    const emailNormalizado = email.trim().toLowerCase()
    const matriculaNormalizada = matricula.trim()

    if (!nomeNormalizado) return setErro('Informe o nome completo do aluno.')
    if (!emailValido(emailNormalizado)) return setErro('Informe um e-mail válido.')
    if (!matriculaNormalizada) return setErro('Informe a matrícula (RA/RGM) do aluno.')

    setAcaoEmAndamento('cadastro')
    try {
      const resposta = await fetch(`${apiBase}/api/instituicoes/alunos`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          nome: nomeNormalizado,
          email: emailNormalizado,
          matricula: matriculaNormalizada
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
      setCadastroAberto(false)
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
    setConfirmacaoDesativacao(null)
    limparFeedback()
  }

  async function salvarEdicao(event) {
    event.preventDefault()
    if (!edicao.nome.trim()) return setErro('Informe o nome completo do aluno.')
    if (!edicao.matricula.trim()) return setErro('Informe a matrícula (RA/RGM) do aluno.')

    setAcaoEmAndamento(`editar-${edicao.id}`)
    setErro('')
    try {
      const resposta = await fetch(`${apiBase}/api/instituicoes/alunos/${edicao.id}`, {
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
    limparFeedback()

    try {
      const resposta = await fetch(`${apiBase}/api/instituicoes/alunos/${aluno.id}/situacao`, {
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
      setConfirmacaoDesativacao(null)
      setMensagem(novoAtivo ? 'Aluno reativado.' : 'Aluno desativado.')
    } catch {
      setErro('Não foi possível conectar com o servidor.')
    } finally {
      setAcaoEmAndamento('')
    }
  }

  async function gerarNovoConvite(aluno) {
    setAcaoEmAndamento(`convite-${aluno.id}`)
    limparFeedback()

    try {
      const resposta = await fetch(`${apiBase}/api/instituicoes/alunos/${aluno.id}/convite`, {
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
      setErro('')
    } catch {
      setErro('Não foi possível copiar automaticamente. Selecione o link e copie manualmente.')
    }
  }

  function limparFiltros() {
    setBusca('')
    setFiltro('TODOS')
  }

  return (
    <div className="gestao-alunos">
      <header className="cabecalho gestao-alunos__cabecalho">
        <div>
          <p className="cabecalho-contexto">GESTÃO INSTITUCIONAL</p>
          <h2>Alunos</h2>
          <p className="cabecalho-descricao">
            Cadastre e acompanhe os alunos vinculados à sua instituição.
          </p>
          <p className="gestao-alunos__instituicao" title={nomeInstituicao}>
            <span>Instituição</span>
            <strong>{siglaInstituicao ? `${siglaInstituicao} — ${nomeInstituicao}` : nomeInstituicao}</strong>
          </p>
        </div>
        <button
          type="button"
          className="botao-primario gestao-alunos__botao-cadastro"
          aria-expanded={cadastroAberto}
          aria-controls="painel-cadastro-aluno"
          onClick={() => setCadastroAberto((aberto) => !aberto)}
        >
          {cadastroAberto ? 'Fechar cadastro' : 'Cadastrar aluno'}
        </button>
      </header>

      <section className="gestao-alunos__indicadores" aria-label="Resumo dos alunos">
        <article className="gestao-alunos__indicador">
          <span>Total de alunos</span><strong>{indicadores.total}</strong>
        </article>
        <article className="gestao-alunos__indicador gestao-alunos__indicador--sucesso">
          <span>Contas ativas</span><strong>{indicadores.ativados}</strong>
        </article>
        <article className="gestao-alunos__indicador gestao-alunos__indicador--informacao">
          <span>Aguardando ativação</span><strong>{indicadores.aguardando}</strong>
        </article>
        <article className="gestao-alunos__indicador gestao-alunos__indicador--alerta">
          <span>Convites com atenção</span><strong>{indicadores.atencao}</strong>
        </article>
      </section>

      {(erro || mensagem) && (
        <p
          className={erro ? 'mensagem-erro gestao-alunos__mensagem' : 'mensagem-sucesso gestao-alunos__mensagem'}
          role={erro ? 'alert' : 'status'}
        >
          {erro || mensagem}
        </p>
      )}

      {convite && (
        <section
          className="gestao-alunos__convite"
          aria-labelledby="titulo-convite-aluno"
          ref={conviteRef}
          tabIndex="-1"
        >
          <div className="gestao-alunos__convite-cabecalho">
            <div>
              <span className="gestao-alunos__confidencial">Link confidencial</span>
              <h3 id="titulo-convite-aluno">Ativação de {convite.aluno.nome}</h3>
            </div>
            <button type="button" className="botao-secundario" onClick={() => setConvite(null)}>
              Ocultar
            </button>
          </div>
          <p>O link é exibido somente nesta etapa. Compartilhe-o diretamente por um canal seguro.</p>
          <div className="gestao-alunos__convite-link">
            <input
              value={convite.link}
              readOnly
              aria-label="Link confidencial de ativação"
              onFocus={(event) => event.target.select()}
            />
            <button type="button" className="botao-primario" onClick={copiarConvite}>Copiar link</button>
          </div>
          <small>Expira em {formatarDataHora(convite.expiraEm)}.</small>
        </section>
      )}

      {cadastroAberto && (
        <section id="painel-cadastro-aluno" className="gestao-alunos__painel-cadastro">
          <div className="secao-cabecalho">
            <div>
              <h3>Cadastrar aluno</h3>
              <p>A instituição é vinculada automaticamente pela conta autenticada.</p>
            </div>
          </div>
          <form className="gestao-alunos__formulario" onSubmit={cadastrarAluno}>
            <div className="campo">
              <label htmlFor="nomeNovoAluno">Nome completo</label>
              <input ref={nomeCadastroRef} id="nomeNovoAluno" value={nome}
                onChange={(event) => setNome(event.target.value)} maxLength="255" required />
            </div>
            <div className="campo">
              <label htmlFor="emailNovoAluno">E-mail</label>
              <input id="emailNovoAluno" type="email" value={email}
                onChange={(event) => setEmail(event.target.value)} maxLength="255" required />
            </div>
            <div className="campo">
              <label htmlFor="matriculaNovoAluno">Matrícula (RA/RGM)</label>
              <input id="matriculaNovoAluno" value={matricula}
                onChange={(event) => setMatricula(event.target.value)} maxLength="50" required />
            </div>
            <div className="gestao-alunos__formulario-acoes">
              <button type="submit" className="botao-primario" disabled={acaoEmAndamento === 'cadastro'}>
                {acaoEmAndamento === 'cadastro' ? 'Cadastrando...' : 'Cadastrar e gerar convite'}
              </button>
              <button type="button" className="botao-secundario" onClick={() => setCadastroAberto(false)}>
                Cancelar
              </button>
            </div>
          </form>
        </section>
      )}

      <section className="gestao-alunos__lista" aria-labelledby="titulo-lista-alunos">
        <div className="gestao-alunos__lista-cabecalho">
          <div>
            <h3 id="titulo-lista-alunos">Alunos da instituição</h3>
            <p>Somente alunos vinculados à sua instituição são apresentados.</p>
          </div>
          <span aria-live="polite">
            {alunosFiltrados.length} {alunosFiltrados.length === 1 ? 'resultado' : 'resultados'}
          </span>
        </div>

        <div className="gestao-alunos__filtros">
          <div className="campo gestao-alunos__pesquisa">
            <label htmlFor="pesquisaAluno">Pesquisar aluno</label>
            <input id="pesquisaAluno" type="search" value={busca}
              onChange={(event) => setBusca(event.target.value)}
              placeholder="Nome, e-mail ou matrícula" />
          </div>
          <div className="campo gestao-alunos__filtro">
            <label htmlFor="filtroSituacaoAluno">Situação</label>
            <select id="filtroSituacaoAluno" value={filtro}
              onChange={(event) => setFiltro(event.target.value)}>
              {FILTROS.map((opcao) => (
                <option key={opcao.valor} value={opcao.valor}>{opcao.rotulo}</option>
              ))}
            </select>
          </div>
          <button type="button" className="botao-secundario gestao-alunos__limpar"
            disabled={!filtrosAtivos} onClick={limparFiltros}>
            Limpar filtros
          </button>
        </div>

        {carregando ? (
          <p className="gestao-alunos__estado">Carregando alunos...</p>
        ) : alunos.length === 0 ? (
          <p className="gestao-alunos__estado">Nenhum aluno cadastrado nesta instituição.</p>
        ) : alunosFiltrados.length === 0 ? (
          <div className="gestao-alunos__estado">
            <p>Nenhum aluno corresponde à pesquisa e aos filtros selecionados.</p>
            <button type="button" className="botao-secundario" onClick={limparFiltros}>Limpar filtros</button>
          </div>
        ) : (
          <div className="gestao-alunos__tabela-container">
            <table className="gestao-alunos__tabela">
              <thead>
                <tr>
                  <th scope="col">Aluno</th>
                  <th scope="col">Matrícula</th>
                  <th scope="col">Situação</th>
                  <th scope="col">Convite</th>
                  <th scope="col"><span className="sr-only">Ações</span></th>
                </tr>
              </thead>
              <tbody>
                {alunosFiltrados.map((aluno) => edicao?.id === aluno.id ? (
                  <tr className="gestao-alunos__linha-edicao" key={aluno.id}>
                    <td colSpan="5">
                      <form className="gestao-alunos__edicao" onSubmit={salvarEdicao}>
                        <div className="campo">
                          <label htmlFor={`editar-nome-${aluno.id}`}>Nome</label>
                          <input id={`editar-nome-${aluno.id}`} value={edicao.nome}
                            onChange={(event) => setEdicao({ ...edicao, nome: event.target.value })}
                            maxLength="255" required autoFocus />
                        </div>
                        <div className="campo">
                          <label htmlFor={`editar-matricula-${aluno.id}`}>Matrícula (RA/RGM)</label>
                          <input id={`editar-matricula-${aluno.id}`} value={edicao.matricula}
                            onChange={(event) => setEdicao({ ...edicao, matricula: event.target.value })}
                            maxLength="50" required />
                        </div>
                        <div className="gestao-alunos__acoes">
                          <button type="submit" className="botao-primario"
                            disabled={acaoEmAndamento === `editar-${aluno.id}`}>
                            {acaoEmAndamento === `editar-${aluno.id}` ? 'Salvando...' : 'Salvar'}
                          </button>
                          <button type="button" className="botao-secundario" onClick={() => setEdicao(null)}>
                            Cancelar
                          </button>
                        </div>
                      </form>
                    </td>
                  </tr>
                ) : (
                  <tr className={aluno.ativo ? '' : 'gestao-alunos__linha--inativa'} key={aluno.id}>
                    <td data-label="Aluno">
                      <div className="gestao-alunos__identificacao">
                        <strong>{aluno.nome}</strong>
                        <span className="gestao-alunos__email">{aluno.email}</span>
                      </div>
                    </td>
                    <td data-label="Matrícula">{aluno.matricula || 'Registro legado sem matrícula'}</td>
                    <td data-label="Situação"><BadgeSituacao situacao={aluno.situacaoAtivacao} /></td>
                    <td data-label="Convite">
                      {aluno.conviteExpiraEm ? formatarDataHora(aluno.conviteExpiraEm) : 'Não disponível'}
                    </td>
                    <td data-label="Ações" className="gestao-alunos__celula-acoes">
                      {confirmacaoDesativacao === aluno.id ? (
                        <div className="gestao-alunos__confirmacao" role="group"
                          aria-label={`Confirmar desativação de ${aluno.nome}`}>
                          <p>Desativar <strong>{aluno.nome}</strong>?</p>
                          <div>
                            <button type="button" className="botao-perigo"
                              disabled={acaoEmAndamento === `situacao-${aluno.id}`}
                              onClick={() => alterarSituacao(aluno)}>
                              {acaoEmAndamento === `situacao-${aluno.id}` ? 'Desativando...' : 'Confirmar'}
                            </button>
                            <button type="button" className="botao-secundario"
                              onClick={() => setConfirmacaoDesativacao(null)}>Cancelar</button>
                          </div>
                        </div>
                      ) : (
                        <div className="gestao-alunos__acoes">
                          <button type="button" className="botao-secundario" onClick={() => iniciarEdicao(aluno)}>
                            Editar
                          </button>
                          {!aluno.ativado && aluno.ativo && (
                            <button type="button" className="botao-secundario"
                              disabled={acaoEmAndamento === `convite-${aluno.id}`}
                              onClick={() => gerarNovoConvite(aluno)}>
                              {acaoEmAndamento === `convite-${aluno.id}` ? 'Gerando...' : 'Novo convite'}
                            </button>
                          )}
                          <button type="button" className={aluno.ativo ? 'botao-perigo' : 'botao-secundario'}
                            disabled={acaoEmAndamento === `situacao-${aluno.id}`}
                            onClick={() => aluno.ativo
                              ? setConfirmacaoDesativacao(aluno.id)
                              : alterarSituacao(aluno)}>
                            {acaoEmAndamento === `situacao-${aluno.id}`
                              ? (aluno.ativo ? 'Desativando...' : 'Reativando...')
                              : (aluno.ativo ? 'Desativar' : 'Reativar')}
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}
