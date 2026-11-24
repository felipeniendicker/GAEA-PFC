import { useState } from 'react'
import CampoSenha from './CampoSenha'
import RequisitosSenha from './RequisitosSenha'
import AceiteDocumentos from '../forms/AceiteDocumentos'
import MensagemFormulario from '../forms/MensagemFormulario'
import { API, mensagemDaApi } from '../../services/api'
import { senhaAtendePolitica } from '../../utils/senha'
import { emailValido } from '../../utils/validacao'

export default function CadastroSupervisor({
  aoVoltarLogin
}) {
const [nomeCadastro, setNomeCadastro] = useState('')
const [emailCadastro, setEmailCadastro] = useState('')
const [senhaCadastro, setSenhaCadastro] = useState('')
const [confirmacaoSenhaCadastro, setConfirmacaoSenhaCadastro] = useState('')
const [termosAceitos, setTermosAceitos] = useState(false)
const [erroCadastro, setErroCadastro] = useState('')
const [sucessoCadastro, setSucessoCadastro] = useState('')
const [carregandoCadastro, setCarregandoCadastro] = useState(false)
async function fazerCadastro(event) {
  event.preventDefault()
  setErroCadastro('')
  setSucessoCadastro('')

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

  if (!senhaAtendePolitica(senhaCadastro, confirmacaoSenhaCadastro)) {
    setErroCadastro('Revise os requisitos da senha antes de continuar.')
    return
  }

  if (!termosAceitos) {
    setErroCadastro(
      'É necessário aceitar os Termos de Uso e a Política de Privacidade.'
    )
    return
  }

  setCarregandoCadastro(true)
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
        confirmacaoSenha: confirmacaoSenhaCadastro,
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

    setSucessoCadastro('Cadastro realizado. Verifique seu e-mail antes de responder solicitações.')

    setNomeCadastro('')
    setEmailCadastro('')
    setSenhaCadastro('')
    setConfirmacaoSenhaCadastro('')
    setTermosAceitos(false)
  } catch {
    setErroCadastro('Não foi possível conectar com o servidor.')
  } finally {
    setCarregandoCadastro(false)
  }
}

  return (
          <main className="conteudo">
            <header className="cabecalho">
              <p className="cabecalho-contexto">NOVO USUÁRIO</p>
              <h2>Cadastrar supervisor de empresa</h2>
              <p className="cabecalho-descricao">
                Crie o acesso do profissional que acompanhará os estágios pela empresa.
              </p>
            </header>

            <section className="formulario">
              <form onSubmit={fazerCadastro} noValidate>
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

                <CampoSenha id="senhaCadastro" label="Senha" value={senhaCadastro}
                  onChange={(event) => setSenhaCadastro(event.target.value)}
                  autoComplete="new-password" minLength={8} maxLength={64}
                  describedBy="requisitosCadastro" />

                <CampoSenha id="confirmacaoSenhaCadastro" label="Confirmar senha"
                  value={confirmacaoSenhaCadastro}
                  onChange={(event) => setConfirmacaoSenhaCadastro(event.target.value)}
                  autoComplete="new-password" minLength={8} maxLength={64}
                  describedBy="requisitosCadastro" />

                <RequisitosSenha senha={senhaCadastro} confirmacao={confirmacaoSenhaCadastro}
                  id="requisitosCadastro" />

                <div className="campo">
                  <label htmlFor="perfilCadastro">Perfil</label>
                  <input
                    id="perfilCadastro"
                    value="Supervisor da Empresa"
                    readOnly
                  />
                  <small>Contas de aluno são criadas pela instituição de ensino.</small>
                </div>

                <AceiteDocumentos id="termosAceitos" checked={termosAceitos}
                  onChange={setTermosAceitos}
                  erro={erroCadastro.includes('aceitar os Termos') ? erroCadastro : ''} />

                <MensagemFormulario tipo="erro">{erroCadastro}</MensagemFormulario>
                <MensagemFormulario tipo="sucesso">{sucessoCadastro}</MensagemFormulario>

                <button
                  type="submit"
                  disabled={carregandoCadastro}
                >
                  {carregandoCadastro ? 'Cadastrando...' : 'Cadastrar supervisor'}
                </button>
              </form>

              <div className="acao-secundaria">
                <p>Já possui uma conta?</p>
                <button
                  type="button"
                  onClick={aoVoltarLogin}
                >
                  Voltar para o login
                </button>
              </div>
            </section>
          </main>
  )
}
