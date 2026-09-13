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
    <div className="container">
      <h1>GAEA</h1>
      <h2>Novo Estágio</h2>

      <form onSubmit={cadastrarEstagio}>
        <label>Nome do aluno</label>
        <input
          type="text"
          value={nomeAluno}
          onChange={(e) => setNomeAluno(e.target.value)}
        />

        <label>E-mail do aluno</label>
        <input
          type="email"
          value={emailAluno}
          onChange={(e) => setEmailAluno(e.target.value)}
        />

        <label>Empresa</label>
        <input
          type="text"
          value={nomeEmpresa}
          onChange={(e) => setNomeEmpresa(e.target.value)}
        />

        <label>CNPJ</label>
        <input
          type="text"
          value={cnpjEmpresa}
          onChange={(e) => setCnpjEmpresa(e.target.value)}
        />

        <label>Data de início</label>
        <input
          type="date"
          value={dataInicio}
          onChange={(e) => setDataInicio(e.target.value)}
        />

        <label>Data de término</label>
        <input
          type="date"
          value={dataFim}
          onChange={(e) => setDataFim(e.target.value)}
        />

        <button type="submit">
          Cadastrar estágio
        </button>
      </form>
      <h2>Acompanhamento de Estágios</h2>

      {processos.length === 0 ? (
        <p>Nenhum processo cadastrado.</p>
      ) : (
        processos.map((processo) => (
          <div className="processo" key={processo.id}>
            <p><strong>Aluno:</strong> {processo.nomeAluno}</p>
            <p><strong>Empresa:</strong> {processo.nomeEmpresa}</p>
            <p><strong>Data de início:</strong> {processo.dataInicio}</p>
            <p><strong>Data de término:</strong> {processo.dataFim}</p>
            <p><strong>Status:</strong> {processo.status}</p>
            
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
        ))
      )}
    </div>
  )
}

export default App