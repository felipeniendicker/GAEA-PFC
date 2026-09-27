import { useEffect, useState } from 'react'
import './App.css'

const API = 'http://localhost:8080'

const rotulosStatus = {
  PENDENTE: 'Pendente',
  EM_ANALISE: 'Em análise',
  APROVADO: 'Aprovado',
  REPROVADO: 'Reprovado'
}

function App() {
  const [usuario, setUsuario] = useState(null)
  const [token, setToken] = useState('')

  const [tela, setTela] = useState('login')

  const [emailLogin, setEmailLogin] = useState('')
  const [senhaLogin, setSenhaLogin] = useState('')
  const [erroLogin, setErroLogin] = useState('')

  const [nomeCadastro, setNomeCadastro] = useState('')
  const [emailCadastro, setEmailCadastro] = useState('')
  const [senhaCadastro, setSenhaCadastro] = useState('')
  const [perfilCadastro, setPerfilCadastro] = useState('ALUNO')
  const [termosAceitos, setTermosAceitos] = useState(false)
  const [erroCadastro, setErroCadastro] = useState('')

  const [nomeAluno, setNomeAluno] = useState('')
  const [emailAluno, setEmailAluno] = useState('')
  const [nomeEmpresa, setNomeEmpresa] = useState('')
  const [cnpjEmpresa, setCnpjEmpresa] = useState('')
  const [dataInicio, setDataInicio] = useState('')
  const [dataFim, setDataFim] = useState('')
  const [processos, setProcessos] = useState([])
  const [cepEmpresa, setCepEmpresa] = useState('')
  const [enderecoEmpresa, setEnderecoEmpresa] = useState(null)
  const [erroCep, setErroCep] = useState('')
  const [logsAuditoria, setLogsAuditoria] = useState([])
  const [mostrarAuditoria, setMostrarAuditoria] = useState(false)

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
        setErroLogin('E-mail ou senha inválidos.')
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
      setTela('sistema')
    } catch (erro) {
      setErroLogin('Não foi possível conectar com o servidor.')
    }
  }

  async function fazerCadastro(event) {
    event.preventDefault()
    setErroCadastro('')

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
          nome: nomeCadastro,
          email: emailCadastro,
          senha: senhaCadastro,
          perfil: perfilCadastro,
          termosAceitos
        })
      })

      if (!resposta.ok) {
        setErroCadastro('Não foi possível realizar o cadastro.')
        return
      }

      alert('Cadastro realizado com sucesso! Agora faça o login.')

      setNomeCadastro('')
      setEmailCadastro('')
      setSenhaCadastro('')
      setPerfilCadastro('ALUNO')
      setTermosAceitos(false)
      setTela('login')
    } catch (erro) {
      setErroCadastro('Não foi possível conectar com o servidor.')
    }
  }

  function sair() {
    setUsuario(null)
    setToken('')
    setProcessos([])
    setEmailLogin('')
    setSenhaLogin('')
    setTela('login')
  }

  async function buscarProcessos() {
    try {
      const resposta = await fetch(`${API}/api/processos`, {
        headers: {
          Authorization: `Bearer ${token}`
        }
      })

      if (resposta.ok) {
        const dados = await resposta.json()
        setProcessos(dados)
      }
    } catch (erro) {
      console.log('Erro ao buscar processos')
    }
  }

  useEffect(() => {
    if (token && usuario?.perfil === 'INSTITUICAO') {
      buscarProcessos()
    }
  }, [token, usuario])

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
      } else {
        alert('Erro ao alterar status.')
      }
    } catch (erro) {
      alert('Não foi possível conectar com o servidor.')
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
  } catch (erro) {
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
  } catch (erro) {
    setErroCep('Não foi possível conectar com o servidor.')
  }
}

  async function cadastrarEstagio(event) {
    event.preventDefault()

    const processo = {
      nomeAluno,
      emailAluno,
      nomeEmpresa,
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

        alert('Estágio cadastrado com sucesso!')
      } else {
        alert('Erro ao cadastrar estágio.')
      }
    } catch (erro) {
      alert('Não foi possível conectar com o servidor.')
    }
  }

  function Cabecalho({ mostrarSair = false }) {
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
            <button
              type="button"
              onClick={() => setTela(usuario ? 'sistema' : 'login')}
            >
              Início
            </button>

            <button
              type="button"
              onClick={() => setTela('privacidade')}
            >
              Privacidade
            </button>

            <button
              type="button"
              onClick={() => setTela('termos')}
            >
              Termos
            </button>

            {mostrarSair && (
              <button type="button" onClick={sair}>
                Sair
              </button>
            )}
          </nav>
        </div>
      </header>
    )
  }

  if (tela === 'privacidade') {
    return (
      <div className="app">
        <Cabecalho mostrarSair={Boolean(usuario)} />

        <main className="conteudo">
          <header className="cabecalho">
            <p className="cabecalho-contexto">LGPD</p>
            <h2>Política de Privacidade</h2>
            <p className="cabecalho-descricao">
              Versão 1.0 — 27/09/2026
            </p>
          </header>

          <section className="formulario">
            <div className="secao-cabecalho">
              <h3>Privacidade no GAEA</h3>
              <p>
                Informações sobre o tratamento de dados pessoais no sistema.
              </p>
            </div>

            <h3>Dados tratados</h3>
            <p>
              O GAEA pode tratar nome, e-mail, perfil de acesso e senha
              protegida por hash para criação e utilização da conta.
              Nos processos de estágio são utilizados dados do aluno,
              da empresa, período do estágio e situação do processo.
            </p>

            <h3>Finalidades</h3>
            <p>
              Os dados são utilizados para autenticação, controle de acesso,
              cadastro e acompanhamento dos processos de estágio, consulta
              de endereço e registro de ações importantes para auditoria.
            </p>

            <h3>Segurança</h3>
            <p>
              As senhas são protegidas com BCrypt. O sistema utiliza JWT
              para autenticação e possui controle de acesso conforme o
              perfil do usuário. Ações relevantes também podem ser
              registradas nos logs de auditoria.
            </p>

            <h3>Serviços externos</h3>
            <p>
              O GAEA utiliza o ViaCEP para consulta de endereço. Nessa
              integração é enviado somente o CEP necessário para realizar
              a consulta. Senhas, tokens e dados completos do processo de
              estágio não são enviados ao ViaCEP.
            </p>

            <h3>Armazenamento</h3>
            <p>
              Os dados devem permanecer armazenados somente enquanto forem
              necessários às finalidades do sistema. Por se tratar de um
              projeto acadêmico, ainda não existe uma política automática
              definitiva de exclusão.
            </p>

            <h3>Direitos do usuário</h3>
            <p>
              O usuário poderá solicitar informações sobre seus dados,
              correção e, quando aplicável, exclusão dos dados tratados
              pelo sistema.
            </p>

            <h3>Contato</h3>
            <p>
              Para questões relacionadas à privacidade:
              {' '}feliperafaelniendicker@gmail.com
            </p>
          </section>
        </main>
      </div>
    )
  }

  if (tela === 'termos') {
    return (
      <div className="app">
        <Cabecalho mostrarSair={Boolean(usuario)} />

        <main className="conteudo">
          <header className="cabecalho">
            <p className="cabecalho-contexto">GAEA</p>
            <h2>Termos de Uso</h2>
            <p className="cabecalho-descricao">
              Versão 1.0 — 27/09/2026
            </p>
          </header>

          <section className="formulario">
            <div className="secao-cabecalho">
              <h3>Termo de Aceite do GAEA</h3>
              <p>
                Condições para utilização do sistema.
              </p>
            </div>

            <h3>Sobre o sistema</h3>
            <p>
              O GAEA é um projeto acadêmico destinado à gestão e ao
              acompanhamento de processos de estágio.
            </p>

            <h3>Perfis de acesso</h3>
            <p>
              O sistema possui diferentes perfis, incluindo aluno, empresa
              e instituição. As funcionalidades disponíveis dependem do
              perfil e das permissões atribuídas ao usuário.
            </p>

            <h3>Conta e acesso</h3>
            <p>
              O usuário deve utilizar suas próprias credenciais e é
              responsável por não compartilhar sua senha ou utilizar
              indevidamente a conta de terceiros.
            </p>

            <h3>Uso adequado</h3>
            <p>
              Não é permitido tentar acessar funcionalidades sem
              autorização, utilizar credenciais de terceiros, inserir
              informações intencionalmente falsas ou comprometer a
              segurança do sistema.
            </p>

            <h3>Processos de estágio</h3>
            <p>
              Conforme o perfil de acesso, o usuário poderá utilizar
              funcionalidades relacionadas ao cadastro, consulta e
              acompanhamento dos processos de estágio.
            </p>

            <h3>Serviços externos</h3>
            <p>
              Algumas funcionalidades podem utilizar serviços externos,
              como o ViaCEP, cuja disponibilidade também depende do
              respectivo provedor.
            </p>

            <h3>Aceite</h3>
            <p>
              Ao marcar a opção de aceite durante o cadastro, o usuário
              declara que teve acesso aos Termos de Uso e à Política de
              Privacidade do GAEA.
            </p>

            <h3>Contato</h3>
            <p>
              Para questões relacionadas ao projeto:
              {' '}feliperafaelniendicker@gmail.com
            </p>
          </section>
        </main>
      </div>
    )
  }

  if (!usuario && tela === 'cadastro') {
    return (
      <div className="app">
        <Cabecalho />

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
                  required
                />
              </div>

              <div className="campo">
                <label htmlFor="perfilCadastro">Perfil</label>
                <select
                  id="perfilCadastro"
                  value={perfilCadastro}
                  onChange={(e) => setPerfilCadastro(e.target.value)}
                >
                  <option value="ALUNO">Aluno</option>
                  <option value="EMPRESA">Empresa</option>
                </select>
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
        <Cabecalho />

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
        <Cabecalho mostrarSair />

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
                Você está autenticado no GAEA.
              </p>
            </div>

            <p>
              As funcionalidades disponíveis são apresentadas conforme
              o perfil e as permissões do usuário.
            </p>
          </section>
        </main>
      </div>
    )
  }

  return (
    <div className="app">
      <Cabecalho mostrarSair />

      <main className="conteudo">
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
                required
              />
            </div>

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
                  className="processo"
                  key={processo.id}
                >
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
                      {rotulosStatus[processo.status] ||
                        processo.status}
                    </span>
                  </p>

                  <div className="processo-acoes">
                    <button
                      type="button"
                      onClick={() =>
                        alterarStatus(
                          processo.id,
                          'EM_ANALISE'
                        )
                      }
                    >
                      Em análise
                    </button>

                    <button
                      type="button"
                      onClick={() =>
                        alterarStatus(
                          processo.id,
                          'APROVADO'
                        )
                      }
                    >
                      Aprovar
                    </button>

                    <button
                      type="button"
                      onClick={() =>
                        alterarStatus(
                          processo.id,
                          'REPROVADO'
                        )
                      }
                    >
                      Reprovar
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      </main>
    </div>
  )
}

export default App
