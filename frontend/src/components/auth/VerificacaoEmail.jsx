import { useState } from 'react'
import { API, mensagemDaApi } from '../../services/api'

export default function VerificacaoEmail({ tokenVerificacao, aoConcluir }) {
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
