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

  async function buscarMeusProcessos() {
    try {
      const resposta = await fetch(`${API}/api/processos/meus`, {
        headers: {
          Authorization: `Bearer ${token}`
        }
      })

      if (resposta.ok) {
        const dados = await resposta.json()
        setProcessos(dados)
      }
    } catch (erro) {
      console.log('Erro ao buscar processos do aluno')
    }
  }

  useEffect(() => {
    if (token && usuario?.perfil === 'INSTITUICAO') {
      buscarProcessos()
    } else if (token && usuario?.perfil === 'ALUNO') {
      buscarMeusProcessos()
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
        <Cabecalho mostrarSair={Boolean(usuario)} />

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
                  </div>

                  {processos.map((processo) => (
                    <div className="processo" key={processo.id}>
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
