import { useEffect, useState } from 'react'
import './SolicitacoesPrivacidade.css'
import { formatarDataHora, rotuloSituacao } from '../utils/apresentacao'

const rotulosTipo = {
  ACESSO: 'Acesso aos dados',
  CORRECAO: 'Correção de dados',
  EXPORTACAO: 'Exportação dos dados',
  EXCLUSAO: 'Exclusão de dados elegíveis',
  ANONIMIZACAO: 'Anonimização de dados elegíveis',
  REVOGACAO_CONSENTIMENTO: 'Revogação de consentimento',
  ENCERRAMENTO_CONTA: 'Encerramento da conta'
}

function dataFormatada(data) {
  return formatarDataHora(data)
}

export default function SolicitacoesPrivacidade({ apiBase, token }) {
  const [solicitacoes, setSolicitacoes] = useState([])
  const [tipo, setTipo] = useState('ACESSO')
  const [descricao, setDescricao] = useState('')
  const [carregando, setCarregando] = useState(true)
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState('')
  const [mensagem, setMensagem] = useState('')

  async function mensagemDaResposta(resposta, padrao) {
    try {
      const dados = await resposta.json()
      return dados.mensagem || padrao
    } catch {
      return padrao
    }
  }

  async function carregar() {
    setCarregando(true)
    setErro('')
    try {
      const resposta = await fetch(`${apiBase}/api/privacidade/solicitacoes`, {
        headers: { Authorization: `Bearer ${token}` }
      })
      if (!resposta.ok) {
        throw new Error(await mensagemDaResposta(
          resposta, 'Não foi possível carregar suas solicitações.'
        ))
      }
      setSolicitacoes(await resposta.json())
    } catch (falha) {
      setErro(falha.message || 'Não foi possível conectar com o servidor.')
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => {
    let ativo = true
    fetch(`${apiBase}/api/privacidade/solicitacoes`, {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(async (resposta) => {
        if (!resposta.ok) {
          throw new Error(await mensagemDaResposta(
            resposta, 'Não foi possível carregar suas solicitações.'
          ))
        }
        return resposta.json()
      })
      .then((dados) => {
        if (ativo) setSolicitacoes(dados)
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
  }, [apiBase, token])

  async function cadastrar(event) {
    event.preventDefault()
    if (!descricao.trim()) {
      setErro('Informe a descrição da solicitação.')
      return
    }
    setEnviando(true)
    setErro('')
    setMensagem('')
    try {
      const resposta = await fetch(`${apiBase}/api/privacidade/solicitacoes`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ tipo, descricao: descricao.trim() })
      })
      if (!resposta.ok) {
        throw new Error(await mensagemDaResposta(
          resposta, 'Não foi possível registrar a solicitação.'
        ))
      }
      setDescricao('')
      setMensagem('Solicitação registrada com sucesso.')
      await carregar()
    } catch (falha) {
      setErro(falha.message || 'Não foi possível conectar com o servidor.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className="solicitacoes-privacidade formulario">
      <header className="secao-cabecalho">
        <h3>Solicitações de privacidade</h3>
        <p>Registre e acompanhe solicitações relacionadas aos seus próprios dados.</p>
      </header>

      <form className="privacidade-formulario" onSubmit={cadastrar}>
        <div className="campo">
          <label htmlFor="tipoSolicitacaoPrivacidade">Tipo</label>
          <select
            id="tipoSolicitacaoPrivacidade"
            value={tipo}
            onChange={(event) => setTipo(event.target.value)}
            disabled={enviando}
          >
            <option value="ACESSO">Acesso aos dados</option>
            <option value="CORRECAO">Correção de dados</option>
            <option value="EXPORTACAO">Exportação dos dados</option>
            <option value="EXCLUSAO">Exclusão de dados elegíveis</option>
            <option value="ANONIMIZACAO">Anonimização de dados elegíveis</option>
            <option value="REVOGACAO_CONSENTIMENTO">Revogação de consentimento</option>
            <option value="ENCERRAMENTO_CONTA">Encerramento da conta</option>
          </select>
        </div>
        <div className="campo privacidade-descricao">
          <label htmlFor="descricaoSolicitacaoPrivacidade">Descrição</label>
          <textarea
            id="descricaoSolicitacaoPrivacidade"
            value={descricao}
            onChange={(event) => setDescricao(event.target.value)}
            maxLength={2000}
            rows={4}
            disabled={enviando}
            required
          />
        </div>
        <button type="submit" disabled={enviando}>
          {enviando ? 'Enviando...' : 'Registrar solicitação'}
        </button>
      </form>

      {erro && <p className="mensagem-erro" role="alert">{erro}</p>}
      {mensagem && <p className="mensagem-sucesso" role="status">{mensagem}</p>}

      <div className="historico-privacidade">
        <h4>Meu histórico</h4>
        {carregando ? (
          <p>Carregando solicitações...</p>
        ) : solicitacoes.length === 0 ? (
          <p className="estado-vazio">Nenhuma solicitação registrada.</p>
        ) : (
          <div className="lista-solicitacoes">
            {solicitacoes.map((solicitacao) => (
              <article className="solicitacao-item" key={solicitacao.id}>
                <div>
                  <strong>{rotulosTipo[solicitacao.tipo] || solicitacao.tipo}</strong>
                  <span>{dataFormatada(solicitacao.criadoEm)}</span>
                </div>
                <span className={`solicitacao-situacao solicitacao-${solicitacao.situacao}`}>
                  {rotuloSituacao(solicitacao.situacao)}
                </span>
                <p>{solicitacao.descricao}</p>
                {solicitacao.decisao && (
                  <p><strong>Decisão:</strong> {solicitacao.decisao}</p>
                )}
              </article>
            ))}
          </div>
        )}
      </div>
    </section>
  )
}
