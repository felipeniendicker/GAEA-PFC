import { useState } from 'react'
import CampoSenha from './CampoSenha'
import RequisitosSenha from './RequisitosSenha'
import { senhaAtendePolitica } from '../../utils/senha'
import './Autenticacao.css'

async function mensagemDaResposta(resposta, padrao) {
  try {
    const dados = await resposta.json()
    return dados.mensagem || padrao
  } catch {
    return padrao
  }
}

export function SolicitacaoRecuperacaoSenha({ apiBase, aoVoltar }) {
  const [email, setEmail] = useState('')
  const [carregando, setCarregando] = useState(false)
  const [erro, setErro] = useState('')
  const [mensagem, setMensagem] = useState('')

  async function solicitar(event) {
    event.preventDefault()
    setErro('')
    setMensagem('')
    setCarregando(true)
    try {
      const resposta = await fetch(`${apiBase}/api/auth/senha/recuperacao`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email })
      })
      const texto = await mensagemDaResposta(
        resposta,
        'Não foi possível solicitar a recuperação de senha.'
      )
      if (!resposta.ok) {
        setErro(texto)
        return
      }
      setMensagem(texto)
    } catch {
      setErro('Não foi possível conectar com o servidor.')
    } finally {
      setCarregando(false)
    }
  }

  return (
    <main className="conteudo pagina-autenticacao">
      <header className="cabecalho cabecalho-autenticacao">
        <p className="cabecalho-contexto">RECUPERAÇÃO SEGURA</p>
        <h2>Esqueceu sua senha?</h2>
        <p className="cabecalho-descricao">
          Informe o e-mail cadastrado para receber as instruções de recuperação.
        </p>
      </header>
      <section className="formulario card-autenticacao">
        <form onSubmit={solicitar}>
          <div className="campo">
            <label htmlFor="emailRecuperacao">E-mail</label>
            <input
              id="emailRecuperacao"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              autoComplete="email"
              required
              autoFocus
            />
          </div>
          {erro && <p className="mensagem-erro alerta-autenticacao" role="alert">{erro}</p>}
          {mensagem && <p className="mensagem-sucesso alerta-autenticacao" role="status">{mensagem}</p>}
          <button type="submit" disabled={carregando || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())}>
            {carregando ? 'Solicitando...' : 'Enviar instruções'}
          </button>
        </form>
        <button className="link-voltar" type="button" onClick={aoVoltar}>Voltar para o login</button>
      </section>
    </main>
  )
}

export function RedefinicaoSenha({ apiBase, tokenRecuperacao, aoConcluir }) {
  const [senha, setSenha] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [carregando, setCarregando] = useState(false)
  const [erro, setErro] = useState('')
  const [concluido, setConcluido] = useState(false)

  async function redefinir(event) {
    event.preventDefault()
    setErro('')
    if (!senhaAtendePolitica(senha, confirmacao)) {
      setErro('Revise os requisitos da senha antes de continuar.')
      return
    }
    setCarregando(true)
    try {
      const resposta = await fetch(`${apiBase}/api/auth/senha/redefinicao`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: tokenRecuperacao, senha, confirmacaoSenha: confirmacao })
      })
      if (!resposta.ok) {
        setErro(await mensagemDaResposta(resposta, 'Não foi possível redefinir a senha.'))
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
    <main className="conteudo pagina-autenticacao">
      <header className="cabecalho cabecalho-autenticacao">
        <p className="cabecalho-contexto">REDEFINIÇÃO SEGURA</p>
        <h2>Definir nova senha</h2>
        <p className="cabecalho-descricao">O link é temporário e só pode ser usado uma vez.</p>
      </header>
      <section className="formulario card-autenticacao">
        {concluido ? (
          <div className="ativacao-concluida" role="status">
            <h3>Senha redefinida</h3>
            <p>As sessões anteriores foram encerradas. Entre novamente com a nova senha.</p>
            <button type="button" onClick={aoConcluir}>Ir para o login</button>
          </div>
        ) : (
          <form onSubmit={redefinir}>
            <CampoSenha id="novaSenha" label="Nova senha" value={senha}
              onChange={(event) => setSenha(event.target.value)} autoComplete="new-password"
              minLength={8} maxLength={64} describedBy="requisitosRedefinicao" autoFocus />
            <CampoSenha id="confirmacaoNovaSenha" label="Confirmar nova senha" value={confirmacao}
              onChange={(event) => setConfirmacao(event.target.value)} autoComplete="new-password"
              minLength={8} maxLength={64} describedBy="requisitosRedefinicao" />
            <RequisitosSenha senha={senha} confirmacao={confirmacao} id="requisitosRedefinicao" />
            {erro && <p className="mensagem-erro alerta-autenticacao" role="alert">{erro}</p>}
            <button type="submit" disabled={carregando || !senhaAtendePolitica(senha, confirmacao)}>
              {carregando ? 'Redefinindo...' : 'Redefinir senha'}
            </button>
          </form>
        )}
      </section>
    </main>
  )
}
