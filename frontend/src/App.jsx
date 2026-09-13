import { useEffect, useState } from 'react'
import './App.css'

function App() {
  const [nomeAluno, setNomeAluno] = useState('')
  const [emailAluno, setEmailAluno] = useState('')
  const [nomeEmpresa, setNomeEmpresa] = useState('')
  const [cnpjEmpresa, setCnpjEmpresa] = useState('')
  const [dataInicio, setDataInicio] = useState('')
  const [dataFim, setDataFim] = useState('')
  const [processos, setProcessos] = useState([])

  async function buscarProcessos() {
   try {
    const resposta = await fetch('http://localhost:8080/api/processos')
    const dados = await resposta.json()

    setProcessos(dados)
  } catch (erro) {
    console.log('Erro ao buscar processos')
  }
}
  useEffect(() => {
    buscarProcessos()
  }, [])

async function alterarStatus(id, novoStatus) {
  try {
    const resposta = await fetch(
      `http://localhost:8080/api/processos/${id}/status`,
      {
        method: 'PUT',
        headers: {
          'Content-Type': 'text/plain'
        },
        body: novoStatus
      }
    )

    if (resposta.ok) {
      const processoAtualizado = await resposta.json()

      console.log('Novo status enviado:', novoStatus)
      console.log('Status retornado:', processoAtualizado.status)

      setProcessos((listaAtual) => {
        return listaAtual.map((processo) => {
          if (processo.id === processoAtualizado.id) {
            return {
              ...processo,
              status: processoAtualizado.status
            }
          }

          return processo
        })
      })
    } else {
      alert('Erro ao alterar status.')
    }
  } catch (erro) {
    console.log(erro)
    alert('Não foi possível conectar com o servidor.')
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
    const resposta = await fetch('http://localhost:8080/api/processos', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
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
  }
    else {
      alert('Erro ao cadastrar estágio.')
    }
  } catch (erro) {
    alert('Não foi possível conectar com o servidor.')
  }
}
  
  return (
  <div className="app">

    <aside className="sidebar">
      <div className="logo">
        <div className="logo-marca">G</div>
        <div>
          <h1>GAEA</h1>
          <p>Gestão de Estágios</p>
        </div>
      </div>

      <nav aria-label="Navegação principal">
        <button type="button">Início</button>
        <button type="button" className="ativo">Estágios</button>
      </nav>
    </aside>

    <main className="conteudo">

      <header className="cabecalho">
        <div>
          <p className="cabecalho-contexto">GESTÃO ACADÊMICA</p>
          <h2>Processos de Estágio</h2>
          <p className="cabecalho-descricao">Cadastre e acompanhe os processos de estágio.</p>
        </div>
      </header>

      <section className="formulario">
        <div className="secao-cabecalho">
          <h3>Novo estágio</h3>
          <p>Preencha os dados abaixo para iniciar um novo processo.</p>
        </div>

      <form onSubmit={cadastrarEstagio}>
        <div className="campo">
          <label htmlFor="nomeAluno">Nome do aluno</label>
          <input
            id="nomeAluno"
            type="text"
            value={nomeAluno}
            onChange={(e) => setNomeAluno(e.target.value)}
          />
        </div>

        <div className="campo">
          <label htmlFor="emailAluno">E-mail do aluno</label>
          <input
            id="emailAluno"
            type="email"
            value={emailAluno}
            onChange={(e) => setEmailAluno(e.target.value)}
          />
        </div>

        <div className="campo">
          <label htmlFor="nomeEmpresa">Empresa</label>
          <input
            id="nomeEmpresa"
            type="text"
            value={nomeEmpresa}
            onChange={(e) => setNomeEmpresa(e.target.value)}
          />
        </div>

        <div className="campo">
          <label htmlFor="cnpjEmpresa">CNPJ</label>
          <input
            id="cnpjEmpresa"
            type="text"
            value={cnpjEmpresa}
            onChange={(e) => setCnpjEmpresa(e.target.value)}
          />
        </div>

        <div className="campo">
          <label htmlFor="dataInicio">Data de início</label>
          <input
            id="dataInicio"
            type="date"
            value={dataInicio}
            onChange={(e) => setDataInicio(e.target.value)}
          />
        </div>

        <div className="campo">
          <label htmlFor="dataFim">Data de término</label>
          <input
            id="dataFim"
            type="date"
            value={dataFim}
            onChange={(e) => setDataFim(e.target.value)}
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
          <p>Consulte e atualize o andamento dos processos cadastrados.</p>
        </div>

      {processos.length === 0 ? (
        <p className="estado-vazio">Nenhum processo cadastrado.</p>
      ) : (
        processos.map((processo) => (
          <div className="processo" key={processo.id}>
            <div className="processo-dados">
              <p><strong>Aluno</strong><span>{processo.nomeAluno}</span></p>
              <p><strong>Empresa</strong><span>{processo.nomeEmpresa}</span></p>
              <p><strong>Data de início</strong><span>{processo.dataInicio}</span></p>
              <p><strong>Data de término</strong><span>{processo.dataFim}</span></p>
              <p><strong>Status</strong><span className="status">{processo.status}</span></p>
            </div>

            <div className="processo-acoes">
              <button
                type="button"
                onClick={() => alterarStatus(processo.id, 'EM_ANALISE')}
              >
                Em análise
              </button>

              <button
                type="button"
                onClick={() => alterarStatus(processo.id, 'APROVADO')}
              >
                Aprovar
              </button>

              <button
                type="button"
                onClick={() => alterarStatus(processo.id, 'REPROVADO')}
              >
                Reprovar
              </button>
            </div>
          </div>
        ))
      )}
    </section>
  </main>
</div>
  )
}

export default App
