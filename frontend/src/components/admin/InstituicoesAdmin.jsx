import { useEffect, useState } from 'react'
import CampoSenha from '../auth/CampoSenha'
import RequisitosSenha from '../auth/RequisitosSenha'
import { senhaAtendePolitica } from '../../utils/senha'
import { apresentacaoInstituicao } from '../../utils/apresentacao'
import './InstituicoesAdmin.css'

function cnpjValido(cnpj) {
  const numeros = cnpj.replace(/\D/g, '')
  if (numeros.length !== 14 || /^(\d)\1+$/.test(numeros)) return false

  function calcularDigito(tamanho) {
    let soma = 0
    let peso = tamanho - 7
    for (let indice = 0; indice < tamanho; indice += 1) {
      soma += Number(numeros[indice]) * peso
      peso -= 1
      if (peso === 1) peso = 9
    }
    const resto = soma % 11
    return resto < 2 ? 0 : 11 - resto
  }

  return Number(numeros[12]) === calcularDigito(12) &&
    Number(numeros[13]) === calcularDigito(13)
}

async function mensagem(resposta, padrao) {
  try { const dados = await resposta.json(); return dados.mensagem || padrao } catch { return padrao }
}

export function SolicitacaoInstituicaoPublica({ apiBase, aoVoltar }) {
  const [dados, setDados] = useState({ nomeInstitucional: '', cnpj: '', responsavelNome: '', emailInstitucional: '' })
  const [termos, setTermos] = useState(false)
  const [estado, setEstado] = useState({ carregando: false, erro: '', sucesso: '' })
  const alterar = (campo) => (event) => setDados((atual) => ({ ...atual, [campo]: event.target.value }))
  async function enviar(event) {
    event.preventDefault(); setEstado({ carregando: true, erro: '', sucesso: '' })
    try {
      const resposta = await fetch(`${apiBase}/api/instituicoes/solicitacoes`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ...dados, termosAceitos: termos }) })
      const texto = await mensagem(resposta, 'Não foi possível registrar a solicitação.')
      if (!resposta.ok) return setEstado({ carregando: false, erro: texto, sucesso: '' })
      setDados({ nomeInstitucional: '', cnpj: '', responsavelNome: '', emailInstitucional: '' })
      setTermos(false)
      setEstado({ carregando: false, erro: '', sucesso: texto })
    } catch { setEstado({ carregando: false, erro: 'Não foi possível conectar com o servidor.', sucesso: '' }) }
  }
  return <main className="conteudo pagina-admin pagina-publica-cadastro"><header className="cabecalho"><p className="cabecalho-contexto">ACESSO INSTITUCIONAL</p><h2>Solicitar cadastro da instituição</h2><p className="cabecalho-descricao">Informe os dados oficiais. A senha será definida somente após aprovação.</p></header><section className="formulario card-admin"><form onSubmit={enviar}>
    <div className="campo"><label htmlFor="nomeInstitucional">Nome institucional</label><input id="nomeInstitucional" value={dados.nomeInstitucional} onChange={alterar('nomeInstitucional')} required maxLength="255" /></div>
    <div className="campo"><label htmlFor="cnpjInstitucional">CNPJ</label><input id="cnpjInstitucional" value={dados.cnpj} onChange={alterar('cnpj')} required inputMode="numeric" /></div>
    <div className="campo"><label htmlFor="responsavelInstitucional">Responsável</label><input id="responsavelInstitucional" value={dados.responsavelNome} onChange={alterar('responsavelNome')} required maxLength="255" /></div>
    <div className="campo"><label htmlFor="emailInstitucional">E-mail institucional</label><input id="emailInstitucional" type="email" value={dados.emailInstitucional} onChange={alterar('emailInstitucional')} required /></div>
    <label className="aceite"><input type="checkbox" checked={termos} onChange={(event) => setTermos(event.target.checked)} /> Li e aceito os Termos e a Política de Privacidade.</label>
    {estado.erro && <p className="mensagem-erro" role="alert">{estado.erro}</p>}{estado.sucesso && <p className="mensagem-sucesso" role="status">{estado.sucesso}</p>}
    <button type="submit" disabled={estado.carregando || !termos || !dados.nomeInstitucional.trim() || !cnpjValido(dados.cnpj) || !dados.responsavelNome.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(dados.emailInstitucional.trim())}>{estado.carregando ? 'Enviando...' : 'Enviar para análise'}</button>
  </form><button type="button" className="link-voltar" onClick={aoVoltar}>Voltar para o login</button></section></main>
}

export function ConfirmacaoInstituicao({ apiBase, token, tipo, aoConcluir }) {
  const [senha, setSenha] = useState(''); const [confirmacao, setConfirmacao] = useState(''); const [termos, setTermos] = useState(false)
  const [estado, setEstado] = useState({ carregando: false, erro: '', concluido: false })
  async function confirmar(event) {
    event?.preventDefault(); if (tipo === 'ativacao' && !senhaAtendePolitica(senha, confirmacao)) return setEstado({ ...estado, erro: 'Revise os requisitos da senha.' })
    setEstado({ carregando: true, erro: '', concluido: false })
    const url = tipo === 'ativacao' ? '/api/instituicoes/ativacoes' : '/api/instituicoes/solicitacoes/verificar-email'
    const corpo = tipo === 'ativacao' ? { token, senha, confirmacaoSenha: confirmacao, termosAceitos: termos } : { token }
    try { const resposta = await fetch(`${apiBase}${url}`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(corpo) }); if (!resposta.ok) return setEstado({ carregando: false, erro: await mensagem(resposta, 'Não foi possível concluir.'), concluido: false }); setEstado({ carregando: false, erro: '', concluido: true }) } catch { setEstado({ carregando: false, erro: 'Não foi possível conectar com o servidor.', concluido: false }) }
  }
  if (estado.concluido) return <main className="conteudo pagina-admin"><section className="formulario card-admin"><h2>Etapa concluída</h2><p>{tipo === 'ativacao' ? 'A conta institucional foi ativada.' : 'E-mail confirmado. A solicitação seguirá para análise.'}</p><button onClick={aoConcluir}>Ir para o login</button></section></main>
  return <main className="conteudo pagina-admin"><header className="cabecalho"><h2>{tipo === 'ativacao' ? 'Ativar acesso institucional' : 'Confirmar e-mail institucional'}</h2></header><section className="formulario card-admin">
    {tipo === 'ativacao' ? <form onSubmit={confirmar}><CampoSenha id="senhaInstituicao" label="Senha" value={senha} onChange={(e) => setSenha(e.target.value)} minLength={8} maxLength={64} /><CampoSenha id="confirmacaoInstituicao" label="Confirmar senha" value={confirmacao} onChange={(e) => setConfirmacao(e.target.value)} minLength={8} maxLength={64} /><RequisitosSenha senha={senha} confirmacao={confirmacao} id="requisitosInstituicao" /><label className="aceite"><input type="checkbox" checked={termos} onChange={(e) => setTermos(e.target.checked)} /> Li e aceito os Termos e a Política de Privacidade.</label>{estado.erro && <p className="mensagem-erro" role="alert">{estado.erro}</p>}<button disabled={estado.carregando || !termos || !senhaAtendePolitica(senha, confirmacao)}>{estado.carregando ? 'Ativando...' : 'Ativar conta'}</button></form>
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
  </main>
}
