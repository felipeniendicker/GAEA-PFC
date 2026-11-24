import { useEffect, useState } from 'react'
import CampoSenha from '../auth/CampoSenha'
import RequisitosSenha from '../auth/RequisitosSenha'
import AceiteDocumentos from '../forms/AceiteDocumentos'
import CampoCnpj from '../forms/CampoCnpj'
import MensagemFormulario from '../forms/MensagemFormulario'
import { senhaAtendePolitica } from '../../utils/senha'
import {
  possuiErros,
  validarSolicitacaoInstituicao
} from '../../utils/validacao'
import { apresentacaoInstituicao } from '../../utils/apresentacao'
import PrivacidadeAdmin from './PrivacidadeAdmin'
import './InstituicoesAdmin.css'

async function mensagem(resposta, padrao) {
  try { const dados = await resposta.json(); return dados.mensagem || padrao } catch { return padrao }
}

export function SolicitacaoInstituicaoPublica({ apiBase, aoVoltar }) {
  const dadosIniciais = {
    nomeInstitucional: '',
    cnpj: '',
    responsavelNome: '',
    emailInstitucional: ''
  }
  const [dados, setDados] = useState(dadosIniciais)
  const [termos, setTermos] = useState(false)
  const [erros, setErros] = useState({})
  const [estado, setEstado] = useState({ carregando: false, erro: '', sucesso: '' })

  function alterar(campo, valor) {
    setDados((atual) => ({ ...atual, [campo]: valor }))
    setErros((atuais) => ({ ...atuais, [campo]: '' }))
  }

  async function enviar(event) {
    event.preventDefault()
    const errosAtuais = validarSolicitacaoInstituicao(dados, termos)
    setErros(errosAtuais)
    setEstado({ carregando: false, erro: '', sucesso: '' })

    if (possuiErros(errosAtuais)) return

    setEstado({ carregando: true, erro: '', sucesso: '' })
    try {
      const resposta = await fetch(`${apiBase}/api/instituicoes/solicitacoes`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ...dados, termosAceitos: termos }) })
      const texto = await mensagem(resposta, 'Não foi possível registrar a solicitação.')
      if (!resposta.ok) return setEstado({ carregando: false, erro: texto, sucesso: '' })
      setDados(dadosIniciais)
      setTermos(false)
      setErros({})
      setEstado({ carregando: false, erro: '', sucesso: texto })
    } catch { setEstado({ carregando: false, erro: 'Não foi possível conectar com o servidor.', sucesso: '' }) }
  }

  return (
    <main className="conteudo pagina-admin pagina-publica-cadastro">
      <header className="cabecalho">
        <p className="cabecalho-contexto">ACESSO INSTITUCIONAL</p>
        <h2>Solicitar cadastro da instituição</h2>
        <p className="cabecalho-descricao">Informe os dados oficiais. A senha será definida somente após aprovação.</p>
      </header>
      <section className="formulario card-admin">
        <form onSubmit={enviar} noValidate>
          <div className="campo">
            <label htmlFor="nomeInstitucional">Nome institucional *</label>
            <input id="nomeInstitucional" value={dados.nomeInstitucional}
              onChange={(event) => alterar('nomeInstitucional', event.target.value)}
              aria-invalid={Boolean(erros.nomeInstitucional)} maxLength="255" required />
            {erros.nomeInstitucional && <small className="campo-erro" role="alert">{erros.nomeInstitucional}</small>}
          </div>
          <CampoCnpj id="cnpjInstitucional" value={dados.cnpj}
            onChange={(valor) => alterar('cnpj', valor)} erroExterno={erros.cnpj} />
          <div className="campo">
            <label htmlFor="responsavelInstitucional">Responsável *</label>
            <input id="responsavelInstitucional" value={dados.responsavelNome}
              onChange={(event) => alterar('responsavelNome', event.target.value)}
              aria-invalid={Boolean(erros.responsavelNome)} maxLength="255" required />
            {erros.responsavelNome && <small className="campo-erro" role="alert">{erros.responsavelNome}</small>}
          </div>
          <div className="campo">
            <label htmlFor="emailInstitucional">E-mail institucional *</label>
            <input id="emailInstitucional" type="email" value={dados.emailInstitucional}
              onChange={(event) => alterar('emailInstitucional', event.target.value)}
              aria-invalid={Boolean(erros.emailInstitucional)} maxLength="255" required />
            {erros.emailInstitucional && <small className="campo-erro" role="alert">{erros.emailInstitucional}</small>}
          </div>
          <AceiteDocumentos id="termosInstitucionais" checked={termos}
            onChange={(aceitos) => { setTermos(aceitos); setErros((atuais) => ({ ...atuais, termos: '' })) }}
            erro={erros.termos} />
          <MensagemFormulario tipo="erro">{estado.erro}</MensagemFormulario>
          <MensagemFormulario tipo="sucesso">{estado.sucesso}</MensagemFormulario>
          <button type="submit" disabled={estado.carregando}>
            {estado.carregando ? 'Enviando...' : 'Enviar para análise'}
          </button>
        </form>
        <button type="button" className="link-voltar" onClick={aoVoltar}>Voltar para o login</button>
      </section>
    </main>
  )
}

export function ConfirmacaoInstituicao({ apiBase, token, tipo, aoConcluir }) {
  const [senha, setSenha] = useState(''); const [confirmacao, setConfirmacao] = useState(''); const [termos, setTermos] = useState(false)
  const [estado, setEstado] = useState({ carregando: false, erro: '', concluido: false })
  const [erroTermos, setErroTermos] = useState('')
  async function confirmar(event) {
    event?.preventDefault()
    if (tipo === 'ativacao' && !senhaAtendePolitica(senha, confirmacao)) {
      setErroTermos(termos ? '' : 'É necessário aceitar os documentos para ativar a conta.')
      return setEstado({ ...estado, erro: 'Revise os requisitos da senha antes de continuar.' })
    }
    if (tipo === 'ativacao' && !termos) {
      setErroTermos('É necessário aceitar os documentos para ativar a conta.')
      return setEstado({ ...estado, erro: '' })
    }
    setEstado({ carregando: true, erro: '', concluido: false })
    const url = tipo === 'ativacao' ? '/api/instituicoes/ativacoes' : '/api/instituicoes/solicitacoes/verificar-email'
    const corpo = tipo === 'ativacao' ? { token, senha, confirmacaoSenha: confirmacao, termosAceitos: termos } : { token }
    try { const resposta = await fetch(`${apiBase}${url}`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(corpo) }); if (!resposta.ok) return setEstado({ carregando: false, erro: await mensagem(resposta, 'Não foi possível concluir.'), concluido: false }); setEstado({ carregando: false, erro: '', concluido: true }) } catch { setEstado({ carregando: false, erro: 'Não foi possível conectar com o servidor.', concluido: false }) }
  }
  if (estado.concluido) return <main className="conteudo pagina-admin"><section className="formulario card-admin"><h2>Etapa concluída</h2><p>{tipo === 'ativacao' ? 'A conta institucional foi ativada.' : 'E-mail confirmado. A solicitação seguirá para análise.'}</p><button onClick={aoConcluir}>Ir para o login</button></section></main>
  return <main className="conteudo pagina-admin"><header className="cabecalho"><h2>{tipo === 'ativacao' ? 'Ativar acesso institucional' : 'Confirmar e-mail institucional'}</h2></header><section className="formulario card-admin">
    {tipo === 'ativacao' ? <form onSubmit={confirmar} noValidate><CampoSenha id="senhaInstituicao" label="Senha" value={senha} onChange={(e) => setSenha(e.target.value)} minLength={8} maxLength={64} /><CampoSenha id="confirmacaoInstituicao" label="Confirmar senha" value={confirmacao} onChange={(e) => setConfirmacao(e.target.value)} minLength={8} maxLength={64} /><RequisitosSenha senha={senha} confirmacao={confirmacao} id="requisitosInstituicao" /><AceiteDocumentos id="termosAtivacaoInstituicao" checked={termos} onChange={(aceitos) => { setTermos(aceitos); setErroTermos('') }} erro={erroTermos} /><MensagemFormulario tipo="erro">{estado.erro}</MensagemFormulario><button disabled={estado.carregando}>{estado.carregando ? 'Ativando...' : 'Ativar conta'}</button></form>
      : <div>{estado.erro && <p className="mensagem-erro" role="alert">{estado.erro}</p>}<button disabled={estado.carregando} onClick={confirmar}>{estado.carregando ? 'Confirmando...' : 'Confirmar e-mail'}</button></div>}
  </section></main>
}

export default function InstituicoesAdmin({ apiBase, token }) {
  const [solicitacoes, setSolicitacoes] = useState([]); const [instituicoes, setInstituicoes] = useState([]); const [resumo, setResumo] = useState({}); const [filtro, setFiltro] = useState('PENDENTE'); const [erro, setErro] = useState('')
  const [aprovacaoAbertaId, setAprovacaoAbertaId] = useState(null)
  const [siglaAprovacao, setSiglaAprovacao] = useState('')
  const [ficticiaAprovacao, setFicticiaAprovacao] = useState(false)
  const [erroAprovacao, setErroAprovacao] = useState('')
  const headers = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
  async function carregar() { try { const [s, i, r] = await Promise.all([fetch(`${apiBase}/api/admin/instituicoes/solicitacoes?status=${filtro}`, { headers }), fetch(`${apiBase}/api/admin/instituicoes`, { headers }), fetch(`${apiBase}/api/admin/instituicoes/dashboard`, { headers })]); if (![s, i, r].every((x) => x.ok)) throw new Error(); setSolicitacoes(await s.json()); setInstituicoes(await i.json()); setResumo(await r.json()); setErro('') } catch { setErro('Não foi possível carregar o painel administrativo.') } }
  useEffect(() => {
    const agendamento = window.setTimeout(() => carregar(), 0)
    return () => window.clearTimeout(agendamento)
  }, [filtro]) // eslint-disable-line react-hooks/exhaustive-deps
  async function acao(id, nome) {
    if (nome === 'aprovar') {
      setAprovacaoAbertaId(id)
      setSiglaAprovacao('')
      setFicticiaAprovacao(false)
      setErroAprovacao('')
      return
    }
    let body
    if (nome === 'rejeitar') {
      const justificativa = window.prompt('Justificativa da rejeição (mínimo 10 caracteres):')
      if (!justificativa) return
      body = JSON.stringify({ justificativa })
    }
    const metodo = nome === 'analise' ? 'PUT' : 'POST'
    const resposta = await fetch(`${apiBase}/api/admin/instituicoes/solicitacoes/${id}/${nome}`, { method: metodo, headers, body })
    if (!resposta.ok) setErro(await mensagem(resposta, 'Não foi possível concluir a ação.'));
    else carregar()
  }
  async function confirmarAprovacao(id) {
    const sigla = siglaAprovacao.trim().toUpperCase()
    if (!/^[A-Z0-9.-]{2,20}$/.test(sigla)) {
      setErroAprovacao('Informe uma sigla válida com 2 a 20 caracteres.')
      return
    }
    setErroAprovacao('')
    const resposta = await fetch(`${apiBase}/api/admin/instituicoes/solicitacoes/${id}/aprovar`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ sigla, ficticia: ficticiaAprovacao })
    })
    if (!resposta.ok) setErroAprovacao(await mensagem(resposta, 'Não foi possível aprovar a instituição.'))
    else {
      setAprovacaoAbertaId(null)
      carregar()
    }
  }
  return <main className="conteudo pagina-admin"><header className="cabecalho"><p className="cabecalho-contexto">ADMINISTRAÇÃO GAEA</p><h2>Aprovação de instituições</h2><p className="cabecalho-descricao">Este perfil administra cadastros institucionais e não acessa processos ou dados de alunos.</p></header>
    <section className="admin-resumo">{['PENDENTE','EM_ANALISE','APROVADA','REJEITADA','INSTITUICOES'].map((x) => <article className="card" key={x}><strong>{resumo[x] || 0}</strong><span>{x.replace('_',' ')}</span></article>)}</section>
    {erro && <p className="mensagem-erro" role="alert">{erro}</p>}
    <section className="formulario">
      <div className="secao-cabecalho">
        <h3>Solicitações</h3>
        <select value={filtro} onChange={(e) => setFiltro(e.target.value)}>
          {['AGUARDANDO_EMAIL','PENDENTE','EM_ANALISE','CORRECAO_SOLICITADA','APROVADA','REJEITADA'].map((x) => <option key={x}>{x}</option>)}
        </select>
      </div>
      <div className="admin-lista">
        {solicitacoes.map((s) => {
          return (
          <article className="card" key={s.id}>
            <h4>{s.nomeInstitucional}</h4>
            <p>{s.cnpj} · {s.responsavelNome} · {s.emailInstitucional}</p>
            <span className="badge">{s.status}</span>
            {!s.emailVerificado && <span className="badge badge-aviso">E-mail pendente</span>}
            <div className="processo-acoes">
              {s.status === 'PENDENTE' && s.emailVerificado && <button onClick={() => acao(s.id,'analise')}>Iniciar análise</button>}
              {s.status === 'EM_ANALISE' && <>
                <button onClick={() => acao(s.id,'aprovar')}>Aprovar</button>
                <button className="botao-perigo" onClick={() => acao(s.id,'rejeitar')}>Rejeitar</button>
              </>}
            </div>
            {aprovacaoAbertaId === s.id && (
              <div className="instituicao-aprovacao">
                <div className="campo">
                  <label htmlFor={`sigla-${s.id}`}>Sigla da instituição</label>
                  <input
                    id={`sigla-${s.id}`}
                    value={siglaAprovacao}
                    onChange={(event) => setSiglaAprovacao(event.target.value.toUpperCase())}
                    minLength="2"
                    maxLength="20"
                    pattern="[A-Z0-9.-]{2,20}"
                    required
                  />
                </div>
                <label className="aceite">
                  <input
                    type="checkbox"
                    checked={ficticiaAprovacao}
                    onChange={(event) => setFicticiaAprovacao(event.target.checked)}
                  />
                  Instituição fictícia de demonstração
                </label>
                {erroAprovacao && <p className="mensagem-erro" role="alert">{erroAprovacao}</p>}
                <div className="processo-acoes">
                  <button type="button" onClick={() => confirmarAprovacao(s.id)}>Confirmar aprovação</button>
                  <button type="button" className="botao-secundario" onClick={() => setAprovacaoAbertaId(null)}>Cancelar</button>
                </div>
              </div>
            )}
            {s.justificativa && <p>Justificativa: {s.justificativa}</p>}
          </article>
          )
        })}
      </div>
    </section>
    <section className="formulario"><h3>Instituições existentes</h3><div className="admin-lista">{instituicoes.map((i) => {
      const instituicao = apresentacaoInstituicao(i)
      return <article className="instituicao-card card" key={i.id}>
        <div className="instituicao-identificacao">
          {instituicao.sigla && <span className="instituicao-sigla" aria-label={`Sigla ${instituicao.sigla}`}>{instituicao.sigla}</span>}
          <div className="instituicao-dados">
            <h4>{instituicao.nomeCompleto}</h4>
            {instituicao.ficticia && <span className="instituicao-ficticia">Instituição fictícia de demonstração</span>}
          </div>
        </div>
        <div className="instituicao-meta">
          <p>{i.cnpj}</p>
          <span className={`instituicao-status ${i.ativa ? 'instituicao-status-ativa' : 'instituicao-status-inativa'}`}>
            {i.ativa ? 'ATIVA' : 'INATIVA'}
          </span>
        </div>
      </article>
    })}</div></section>
    <PrivacidadeAdmin apiBase={apiBase} token={token} />
  </main>
}
