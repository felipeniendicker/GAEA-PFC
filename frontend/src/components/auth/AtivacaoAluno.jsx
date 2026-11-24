import { useState } from 'react'
import CampoSenha from './CampoSenha'
import RequisitosSenha from './RequisitosSenha'
import AceiteDocumentos from '../forms/AceiteDocumentos'
import MensagemFormulario from '../forms/MensagemFormulario'
import { API, mensagemDaApi } from '../../services/api'
import { senhaAtendePolitica } from '../../utils/senha'

export default function AtivacaoAluno({ tokenAtivacao, aoConcluir }) {
  const [senha, setSenha] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [termos, setTermos] = useState(false)
  const [erro, setErro] = useState('')
  const [concluido, setConcluido] = useState(false)
  const [carregando, setCarregando] = useState(false)
  const [erroTermos, setErroTermos] = useState('')

  async function ativar(event) {
    event.preventDefault()
    setErro('')
    if (!senhaAtendePolitica(senha, confirmacao)) {
      setErroTermos(termos ? '' : 'É necessário aceitar os documentos para ativar a conta.')
      setErro('Revise os requisitos da senha antes de continuar.')
      return
    }
    if (!termos) {
      setErroTermos('É necessário aceitar os documentos para ativar a conta.')
      return
    }

    setCarregando(true)
    try {
      const resposta = await fetch(`${API}/api/ativacoes/aluno`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          token: tokenAtivacao,
          senha,
          confirmacaoSenha: confirmacao,
          termosAceitos: termos
        })
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
          <form onSubmit={ativar} noValidate>
            <CampoSenha id="senhaAtivacao" label="Nova senha" value={senha}
              onChange={(event) => setSenha(event.target.value)} autoComplete="new-password"
              minLength={8} maxLength={64} describedBy="requisitosAtivacao" autoFocus />
            <CampoSenha id="confirmacaoSenhaAtivacao" label="Confirmar senha" value={confirmacao}
              onChange={(event) => setConfirmacao(event.target.value)} autoComplete="new-password"
              minLength={8} maxLength={64} describedBy="requisitosAtivacao" />
            <RequisitosSenha senha={senha} confirmacao={confirmacao} id="requisitosAtivacao" />
            <AceiteDocumentos id="termosAtivacaoAluno" checked={termos}
              onChange={(aceitos) => { setTermos(aceitos); setErroTermos('') }} erro={erroTermos} />
            <MensagemFormulario tipo="erro">{erro}</MensagemFormulario>
            <button type="submit" disabled={carregando}>{carregando ? 'Ativando...' : 'Ativar minha conta'}</button>
          </form>
        )}
      </section>
    </main>
  )
}
