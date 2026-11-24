import { useEffect, useState } from 'react'
import './App.css'
import DocumentosProcesso from './components/DocumentosProcesso'
import PlanoAtividadesProcesso from './components/PlanoAtividadesProcesso'
import CargaHorariaProcesso from './components/CargaHorariaProcesso'
import RelatoriosProcessos from './components/RelatoriosProcessos'
import GestaoAlunos from './components/alunos/GestaoAlunos'
import InstituicoesAdmin, {
  ConfirmacaoInstituicao,
  SolicitacaoInstituicaoPublica
} from './components/admin/InstituicoesAdmin'
import AtivacaoAluno from './components/auth/AtivacaoAluno'
import CadastroSupervisor from './components/auth/CadastroSupervisor'
import CampoSenha from './components/auth/CampoSenha'
import {
  RedefinicaoSenha,
  SolicitacaoRecuperacaoSenha
} from './components/auth/RecuperacaoSenha'
import VerificacaoEmail from './components/auth/VerificacaoEmail'
import CampoCnpj from './components/forms/CampoCnpj'
import DashboardProcessos from './components/dashboard/DashboardProcessos'
import Cabecalho from './components/layout/Cabecalho'
import { PaginaPrivacidade, PaginaTermos } from './components/legal/PaginasLegais'
import {
  HistoricoProcesso,
  MensagemAcaoProcesso,
  PendenciasProcesso,
  PrazosProcesso
} from './components/processos/ProcessoDetalhes'
import {
  ModalNovaPendencia,
  ModalPrazo,
  ModalReabertura
} from './components/processos/ProcessoModais'
import { API, mensagemDaApi } from './services/api'
import {
  apresentacaoInstituicao,
  formatarData,
  formatarDataHora,
  ROTULOS_STATUS_PROCESSO
} from './utils/apresentacao'
import { cnpjValido, emailValido } from './utils/validacao'
import { lerTokensDaUrl, possuiTokenSensivel } from './utils/tokensUrl'

const rotulosStatus = ROTULOS_STATUS_PROCESSO

const rotulosVinculo = {
  PENDENTE: 'Aguardando confirmação do supervisor',
  CONFIRMADO: 'Supervisor confirmado',
  RECUSADO: 'Solicitação recusada pelo supervisor',
  CANCELADO: 'Solicitação substituída'
}

function App() {
  const [parametrosIniciais] = useState(() => lerTokensDaUrl(window.location.hash))
  const tokenAtivacaoInicial = parametrosIniciais.conviteAluno
  const tokenVerificacaoInicial = parametrosIniciais.verificarEmail
  const tokenVerificacaoInstituicao = parametrosIniciais.verificarInstituicao
  const tokenAtivacaoInstituicao = parametrosIniciais.ativarInstituicao
  const tokenRecuperacaoInicial = parametrosIniciais.redefinirSenha
  const documentoLegalInicial = parametrosIniciais.documentoLegal
  const [usuario, setUsuario] = useState(null)
  const [token, setToken] = useState('')
  const [documentosLegais, setDocumentosLegais] = useState(null)

  useEffect(() => {
    const controlador = new AbortController()

    async function carregarDocumentosLegais() {
      try {
        const resposta = await fetch(`${API}/api/auth/documentos-legais`, {
          signal: controlador.signal
        })
        if (resposta.ok) setDocumentosLegais(await resposta.json())
      } catch (erro) {
        if (erro.name !== 'AbortError') setDocumentosLegais(null)
      }
    }

    carregarDocumentosLegais()
    return () => controlador.abort()
  }, [])

  useEffect(() => {
    if (possuiTokenSensivel(parametrosIniciais)) {
      window.history.replaceState({}, document.title, window.location.pathname)
    }
  }, [parametrosIniciais])

  const [tela, setTela] = useState(
    tokenAtivacaoInicial
      ? 'ativacaoAluno'
      : tokenVerificacaoInicial
        ? 'verificacaoEmail'
        : tokenRecuperacaoInicial ? 'redefinicaoSenha'
          : tokenVerificacaoInstituicao ? 'verificacaoInstituicao'
            : tokenAtivacaoInstituicao ? 'ativacaoInstituicao'
              : documentoLegalInicial || 'login'
  )

  const [emailLogin, setEmailLogin] = useState('')
  const [senhaLogin, setSenhaLogin] = useState('')
  const [erroLogin, setErroLogin] = useState('')
  const [carregandoLogin, setCarregandoLogin] = useState(false)


  const [nomeEmpresa, setNomeEmpresa] = useState('')
  const [cnpjEmpresa, setCnpjEmpresa] = useState('')
  const [emailSupervisor, setEmailSupervisor] = useState('')
  const [dataInicio, setDataInicio] = useState('')
  const [dataFim, setDataFim] = useState('')
  const [erroProcesso, setErroProcesso] = useState('')
  const [processos, setProcessos] = useState([])
  const [cepEmpresa, setCepEmpresa] = useState('')
  const [enderecoEmpresa, setEnderecoEmpresa] = useState('')
  const [erroCep, setErroCep] = useState('')
  const [buscandoCep, setBuscandoCep] = useState(false)
  const [salvandoProcesso, setSalvandoProcesso] = useState(false)
  const [logsAuditoria, setLogsAuditoria] = useState([])
  const [historicos, setHistoricos] = useState({})
  const [historicoAberto, setHistoricoAberto] = useState(null)
  const [carregandoHistorico, setCarregandoHistorico] = useState(null)
  const [errosHistorico, setErrosHistorico] = useState({})
  const [pendencias, setPendencias] = useState({})
  const [pendenciasAbertas, setPendenciasAbertas] = useState(null)
  const [carregandoPendencias, setCarregandoPendencias] = useState(null)
  const [errosPendencias, setErrosPendencias] = useState({})
  const [processoNovaPendencia, setProcessoNovaPendencia] = useState(null)
  const [descricaoPendencia, setDescricaoPendencia] = useState('')
  const [erroNovaPendencia, setErroNovaPendencia] = useState('')
  const [salvandoPendencia, setSalvandoPendencia] = useState(false)
  const [resolvendoPendencia, setResolvendoPendencia] = useState(null)
  const [prazos, setPrazos] = useState({})
  const [prazosAbertos, setPrazosAbertos] = useState(null)
  const [documentosAbertos, setDocumentosAbertos] = useState(null)
  const [planosAbertos, setPlanosAbertos] = useState(null)
  const [cargasHorariasAbertas, setCargasHorariasAbertas] = useState(null)
  const [carregandoPrazos, setCarregandoPrazos] = useState(null)
  const [errosPrazos, setErrosPrazos] = useState({})
  const [processoPrazo, setProcessoPrazo] = useState(null)
  const [prazoEdicao, setPrazoEdicao] = useState(null)
  const [descricaoPrazo, setDescricaoPrazo] = useState('')
  const [dataVencimentoPrazo, setDataVencimentoPrazo] = useState('')
  const [erroPrazo, setErroPrazo] = useState('')
  const [salvandoPrazo, setSalvandoPrazo] = useState(false)
  const [secaoInstituicao, setSecaoInstituicao] = useState('dashboard')
  const [resumoDashboard, setResumoDashboard] = useState(null)
  const [carregandoDashboard, setCarregandoDashboard] = useState(false)
  const [erroDashboard, setErroDashboard] = useState('')
  const [mensagemAcaoProcesso, setMensagemAcaoProcesso] = useState(null)
  const [processoReabertura, setProcessoReabertura] = useState(null)
  const [justificativaReabertura, setJustificativaReabertura] = useState('')
  const [erroReabertura, setErroReabertura] = useState('')
  const [reaberturaEmAndamento, setReaberturaEmAndamento] = useState(false)
  const [solicitacoesSupervisao, setSolicitacoesSupervisao] = useState([])
  const [justificativasRecusa, setJustificativasRecusa] = useState({})
  const [erroSupervisao, setErroSupervisao] = useState('')
  const [acaoSupervisao, setAcaoSupervisao] = useState(null)
  const [recusaSupervisaoAberta, setRecusaSupervisaoAberta] = useState(null)
  const [novosSupervisores, setNovosSupervisores] = useState({})

  async function fazerLogin(event) {
    event.preventDefault()
    setErroLogin('')
    setCarregandoLogin(true)

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
        setErroLogin(await mensagemDaApi(resposta, 'E-mail ou senha inválidos.'))
        return
      }

      const dados = await resposta.json()

      setUsuario({
        nome: dados.nome,
        email: dados.email,
        perfil: dados.perfil,
        matricula: dados.matricula,
        instituicao: dados.instituicao,
        emailVerificado: dados.emailVerificado
      })

      setToken(dados.token)
      setSenhaLogin('')
      setSecaoInstituicao('dashboard')
      setTela('sistema')
      if (dados.perfil !== 'ADMIN_GAEA') buscarResumoDashboard(dados.token)

      if (dados.perfil === 'INSTITUICAO') {
        buscarProcessos(dados.token)
      } else if (dados.perfil === 'ALUNO') {
        buscarMeusProcessos(dados.token)
      } else if (dados.perfil === 'EMPRESA' && dados.emailVerificado) {
        buscarSolicitacoesSupervisao(dados.token)
        buscarProcessos(dados.token)
      }
    } catch {
      setErroLogin('Não foi possível conectar com o servidor.')
    } finally {
      setCarregandoLogin(false)
    }
  }


  async function sair() {
    const tokenAtual = token
    setUsuario(null)
    setToken('')
    setProcessos([])
    setPendencias({})
    setPendenciasAbertas(null)
    setCarregandoPendencias(null)
    setErrosPendencias({})
    setProcessoNovaPendencia(null)
    setDescricaoPendencia('')
    setErroNovaPendencia('')
    setResolvendoPendencia(null)
    setResumoDashboard(null)
    setErroDashboard('')
    setSecaoInstituicao('dashboard')
    setMensagemAcaoProcesso(null)
    setProcessoReabertura(null)
    setSolicitacoesSupervisao([])
    setErroSupervisao('')
    setRecusaSupervisaoAberta(null)
    setEmailLogin('')
    setSenhaLogin('')
    setTela('login')
    if (tokenAtual) {
      try {
        await fetch(`${API}/api/auth/logout`, {
          method: 'POST',
          headers: { Authorization: `Bearer ${tokenAtual}` },
          keepalive: true
        })
      } catch {
        // A sessão local já foi encerrada; o JWT continuará limitado pela expiração.
      }
    }
  }

  async function buscarProcessos(tokenAcesso = token) {
    try {
      const resposta = await fetch(`${API}/api/processos`, {
        headers: {
          Authorization: `Bearer ${tokenAcesso}`
        }
      })

      if (resposta.ok) {
        const dados = await resposta.json()
        setProcessos(dados)
      }
    } catch {
      console.log('Erro ao buscar processos')
    }
  }

  async function buscarMeusProcessos(tokenAcesso = token) {
    try {
      const resposta = await fetch(`${API}/api/processos/meus`, {
        headers: {
          Authorization: `Bearer ${tokenAcesso}`
        }
      })

      if (resposta.ok) {
        const dados = await resposta.json()
        setProcessos(dados)
      }
    } catch {
      console.log('Erro ao buscar processos do aluno')
    }
  }

  async function buscarSolicitacoesSupervisao(tokenAcesso = token) {
    setErroSupervisao('')
    try {
      const resposta = await fetch(`${API}/api/supervisao/solicitacoes`, {
        headers: { Authorization: `Bearer ${tokenAcesso}` }
      })
      if (!resposta.ok) {
        setErroSupervisao(await mensagemDaApi(resposta, 'Não foi possível consultar as solicitações.'))
        return
      }
      setSolicitacoesSupervisao(await resposta.json())
    } catch {
      setErroSupervisao('Não foi possível conectar com o servidor.')
    }
  }

  async function responderSupervisao(solicitacao, acao) {
    const justificativa = (justificativasRecusa[solicitacao.vinculoId] || '').trim()
    if (acao === 'recusar' && justificativa.length < 10) {
      setErroSupervisao('Informe uma justificativa de recusa com pelo menos 10 caracteres.')
      return
    }
    setAcaoSupervisao(`${acao}-${solicitacao.vinculoId}`)
    setErroSupervisao('')
    try {
      const resposta = await fetch(
        `${API}/api/supervisao/solicitacoes/${solicitacao.vinculoId}/${acao}`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`
          },
          body: acao === 'recusar' ? JSON.stringify({ justificativa }) : null
        }
      )
      if (!resposta.ok) {
        setErroSupervisao(await mensagemDaApi(resposta, 'Não foi possível responder à solicitação.'))
        return
      }
      setSolicitacoesSupervisao((atuais) =>
        atuais.filter((item) => item.vinculoId !== solicitacao.vinculoId))
      setRecusaSupervisaoAberta(null)
      exibirMensagemAcao(
        acao === 'confirmar' ? 'Supervisão confirmada com sucesso.' : 'Solicitação recusada.'
      )
    } catch {
      setErroSupervisao('Não foi possível conectar com o servidor.')
    } finally {
      setAcaoSupervisao(null)
    }
  }

  async function reenviarVerificacaoEmail() {
    setAcaoSupervisao('reenviar-email')
    setErroSupervisao('')
    try {
      const resposta = await fetch(`${API}/api/verificacoes-email/reenviar`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` }
      })
      if (!resposta.ok) {
        setErroSupervisao(await mensagemDaApi(resposta, 'Não foi possível gerar um novo link.'))
        return
      }
      exibirMensagemAcao('Um novo link foi solicitado. Confira a caixa de entrada configurada.')
    } catch {
      setErroSupervisao('Não foi possível conectar com o servidor.')
    } finally {
      setAcaoSupervisao(null)
    }
  }

  async function indicarNovoSupervisor(processo) {
    const email = (novosSupervisores[processo.id] || '').trim().toLowerCase()
    if (!emailValido(email)) {
      exibirMensagemAcao('Informe um e-mail válido para o novo supervisor.', 'erro')
      return
    }
    setAcaoSupervisao(`troca-${processo.id}`)
    try {
      const resposta = await fetch(`${API}/api/processos/${processo.id}/supervisor`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({ emailSupervisor: email })
      })
      if (!resposta.ok) {
        exibirMensagemAcao(
          await mensagemDaApi(resposta, 'Não foi possível indicar outro supervisor.'), 'erro')
        return
      }
      setNovosSupervisores((atuais) => ({ ...atuais, [processo.id]: '' }))
      await buscarMeusProcessos()
      if (historicoAberto === processo.id) {
        await buscarHistorico(processo.id, true)
      }
      exibirMensagemAcao('Nova solicitação enviada ao supervisor informado.')
    } catch {
      exibirMensagemAcao('Não foi possível conectar com o servidor.', 'erro')
    } finally {
      setAcaoSupervisao(null)
    }
  }

  async function buscarResumoDashboard(tokenAcesso = token) {
    setCarregandoDashboard(true)
    setErroDashboard('')

    try {
      const resposta = await fetch(`${API}/api/dashboard/resumo`, {
        headers: {
          Authorization: `Bearer ${tokenAcesso}`
        }
      })

      if (!resposta.ok) {
        setResumoDashboard(null)
        if (resposta.status === 401 || resposta.status === 403) {
          setErroDashboard('Sua sessão não possui autorização para consultar estes indicadores.')
        } else {
          setErroDashboard(await mensagemDaApi(resposta, 'O servidor não conseguiu calcular os indicadores.'))
        }
        return
      }

      setResumoDashboard(await resposta.json())
    } catch {
      setResumoDashboard(null)
      setErroDashboard('Não foi possível conectar com o servidor.')
    } finally {
      setCarregandoDashboard(false)
    }
  }

  function exibirMensagemAcao(texto, tipo = 'sucesso') {
    setMensagemAcaoProcesso({ texto, tipo })
  }

  function abrirModalReabertura(processo) {
    setProcessoReabertura(processo)
    setJustificativaReabertura('')
    setErroReabertura('')
    setReaberturaEmAndamento(false)
  }

  function cancelarReabertura() {
    if (reaberturaEmAndamento) {
      return
    }

    setProcessoReabertura(null)
    setJustificativaReabertura('')
    setErroReabertura('')
  }

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
        exibirMensagemAcao('Status atualizado com sucesso.')
        await buscarResumoDashboard()

        if (historicoAberto === id) {
          await buscarHistorico(id, true)
        }
      } else {
        exibirMensagemAcao(
          await mensagemDaApi(resposta, 'Erro ao alterar status.'),
          'erro'
        )
      }
    } catch {
      exibirMensagemAcao('Não foi possível conectar com o servidor.', 'erro')
    }
  }

  async function confirmarReabertura(event) {
    event.preventDefault()

    if (!processoReabertura || reaberturaEmAndamento) {
      return
    }

    const justificativa = justificativaReabertura.trim()
    if (justificativa.length < 10) {
      setErroReabertura('A justificativa deve ter pelo menos 10 caracteres.')
      return
    }

    setErroReabertura('')
    setReaberturaEmAndamento(true)

    try {
      const resposta = await fetch(`${API}/api/processos/${processoReabertura.id}/reabrir`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({ justificativa })
      })

      if (!resposta.ok) {
        setErroReabertura(
          await mensagemDaApi(resposta, 'Não foi possível reabrir o processo.')
        )
        return
      }

      const processoAtualizado = await resposta.json()
      setProcessos((listaAtual) =>
        listaAtual.map((processo) =>
          processo.id === processoAtualizado.id ? processoAtualizado : processo
        )
      )
      await buscarResumoDashboard()

      if (historicoAberto === processoAtualizado.id) {
        await buscarHistorico(processoAtualizado.id, true)
      }

      setProcessoReabertura(null)
      setJustificativaReabertura('')
      setErroReabertura('')
      exibirMensagemAcao('Processo reaberto para análise.')
    } catch {
      setErroReabertura('Não foi possível conectar com o servidor.')
    } finally {
      setReaberturaEmAndamento(false)
    }
  }

  async function buscarHistorico(id, manterAberto = false) {
    if (!manterAberto && historicoAberto === id) {
      setHistoricoAberto(null)
      return
    }

    setHistoricoAberto(id)
    setCarregandoHistorico(id)
    setErrosHistorico((atuais) => ({ ...atuais, [id]: '' }))

    try {
      const resposta = await fetch(`${API}/api/processos/${id}/historico-completo`, {
        headers: {
          Authorization: `Bearer ${token}`
        }
      })

      if (!resposta.ok) {
        const mensagem = await mensagemDaApi(resposta, 'Não foi possível consultar o histórico.')
        setErrosHistorico((atuais) => ({ ...atuais, [id]: mensagem }))
        return
      }

      const dados = await resposta.json()
      setHistoricos((atuais) => ({ ...atuais, [id]: dados }))
    } catch {
      setErrosHistorico((atuais) => ({
        ...atuais,
        [id]: 'Não foi possível conectar com o servidor.'
      }))
    } finally {
      setCarregandoHistorico((atual) => atual === id ? null : atual)
    }
  }

  async function buscarPendencias(id, manterAberto = false) {
    if (!manterAberto && pendenciasAbertas === id) {
      setPendenciasAbertas(null)
      return
    }

    setPendenciasAbertas(id)
    setCarregandoPendencias(id)
    setErrosPendencias((atuais) => ({ ...atuais, [id]: '' }))

    try {
      const resposta = await fetch(`${API}/api/processos/${id}/pendencias`, {
        headers: { Authorization: `Bearer ${token}` }
      })

      if (!resposta.ok) {
        const mensagem = await mensagemDaApi(resposta, 'Não foi possível consultar as pendências.')
        setErrosPendencias((atuais) => ({
          ...atuais,
          [id]: mensagem
        }))
        return
      }

      const dados = await resposta.json()
      setPendencias((atuais) => ({ ...atuais, [id]: dados }))
    } catch {
      setErrosPendencias((atuais) => ({
        ...atuais,
        [id]: 'Não foi possível conectar com o servidor.'
      }))
    } finally {
      setCarregandoPendencias(null)
    }
  }

  function abrirNovaPendencia(processo) {
    setProcessoNovaPendencia(processo)
    setDescricaoPendencia('')
    setErroNovaPendencia('')
  }

  function cancelarNovaPendencia() {
    if (salvandoPendencia) {
      return
    }
    setProcessoNovaPendencia(null)
    setDescricaoPendencia('')
    setErroNovaPendencia('')
  }

  async function registrarPendencia(event) {
    event.preventDefault()
    if (!processoNovaPendencia || salvandoPendencia) {
      return
    }

    const descricao = descricaoPendencia.trim()
    if (!descricao) {
      setErroNovaPendencia('Informe a descrição da pendência.')
      return
    }

    setErroNovaPendencia('')
    setSalvandoPendencia(true)
    try {
      const resposta = await fetch(`${API}/api/processos/${processoNovaPendencia.id}/pendencias`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({ descricao })
      })

      if (!resposta.ok) {
        setErroNovaPendencia(await mensagemDaApi(resposta, 'Não foi possível registrar a pendência.'))
        return
      }

      const processoId = processoNovaPendencia.id
      await resposta.json()
      setProcessoNovaPendencia(null)
      setDescricaoPendencia('')
      await buscarPendencias(processoId, true)
      if (historicoAberto === processoId) {
        await buscarHistorico(processoId, true)
      }
      exibirMensagemAcao('Pendência registrada com sucesso.')
    } catch {
      setErroNovaPendencia('Não foi possível conectar com o servidor.')
    } finally {
      setSalvandoPendencia(false)
    }
  }

  async function resolverPendenciaDoProcesso(processoId, pendenciaId) {
    if (resolvendoPendencia !== null) {
      return
    }

    setResolvendoPendencia(pendenciaId)
    setErrosPendencias((atuais) => ({ ...atuais, [processoId]: '' }))
    try {
      const resposta = await fetch(
        `${API}/api/processos/${processoId}/pendencias/${pendenciaId}/resolver`,
        {
          method: 'PUT',
          headers: { Authorization: `Bearer ${token}` }
        }
      )

      if (!resposta.ok) {
        const mensagem = await mensagemDaApi(resposta, 'Não foi possível resolver a pendência.')
        setErrosPendencias((atuais) => ({
          ...atuais,
          [processoId]: mensagem
        }))
        return
      }

      const atualizada = await resposta.json()
      setPendencias((atuais) => ({
        ...atuais,
        [processoId]: (atuais[processoId] || []).map((item) =>
          item.id === atualizada.id ? atualizada : item
        )
      }))
      if (historicoAberto === processoId) {
        await buscarHistorico(processoId, true)
      }
      exibirMensagemAcao('Pendência resolvida com sucesso.')
    } catch {
      setErrosPendencias((atuais) => ({
        ...atuais,
        [processoId]: 'Não foi possível conectar com o servidor.'
      }))
    } finally {
      setResolvendoPendencia(null)
    }
  }

  async function buscarPrazos(id, manterAberto = false) {
    if (!manterAberto && prazosAbertos === id) {
      setPrazosAbertos(null)
      return
    }

    setPrazosAbertos(id)
    setCarregandoPrazos(id)
    setErrosPrazos((atuais) => ({ ...atuais, [id]: '' }))

    try {
      const resposta = await fetch(`${API}/api/processos/${id}/prazos`, {
        headers: { Authorization: `Bearer ${token}` }
      })

      if (!resposta.ok) {
        const mensagem = await mensagemDaApi(resposta, 'Não foi possível consultar os prazos.')
        setErrosPrazos((atuais) => ({ ...atuais, [id]: mensagem }))
        return
      }

      const dados = await resposta.json()
      setPrazos((atuais) => ({ ...atuais, [id]: dados }))
    } catch {
      setErrosPrazos((atuais) => ({
        ...atuais,
        [id]: 'Não foi possível conectar com o servidor.'
      }))
    } finally {
      setCarregandoPrazos(null)
    }
  }

  function abrirPrazo(processo, prazo = null) {
    setProcessoPrazo(processo)
    setPrazoEdicao(prazo)
    setDescricaoPrazo(prazo?.descricao || '')
    setDataVencimentoPrazo(prazo?.dataVencimento || '')
    setErroPrazo('')
  }

  function cancelarPrazo() {
    if (salvandoPrazo) {
      return
    }
    setProcessoPrazo(null)
    setPrazoEdicao(null)
    setDescricaoPrazo('')
    setDataVencimentoPrazo('')
    setErroPrazo('')
  }

  async function salvarPrazo(event) {
    event.preventDefault()
    if (!processoPrazo || salvandoPrazo) {
      return
    }

    const descricao = descricaoPrazo.trim()
    if (!descricao) {
      setErroPrazo('Informe a descrição do prazo.')
      return
    }
    if (!dataVencimentoPrazo) {
      setErroPrazo('Informe a data de vencimento.')
      return
    }

    const processoId = processoPrazo.id
    const url = prazoEdicao
      ? `${API}/api/processos/${processoId}/prazos/${prazoEdicao.id}`
      : `${API}/api/processos/${processoId}/prazos`

    setErroPrazo('')
    setSalvandoPrazo(true)
    try {
      const resposta = await fetch(url, {
        method: prazoEdicao ? 'PUT' : 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({ descricao, dataVencimento: dataVencimentoPrazo })
      })

      if (!resposta.ok) {
        setErroPrazo(await mensagemDaApi(resposta, 'Não foi possível salvar o prazo.'))
        return
      }

      const atualizando = Boolean(prazoEdicao)
      await resposta.json()
      setProcessoPrazo(null)
      setPrazoEdicao(null)
      setDescricaoPrazo('')
      setDataVencimentoPrazo('')
      await buscarPrazos(processoId, true)
      if (historicoAberto === processoId) {
        await buscarHistorico(processoId, true)
      }
      exibirMensagemAcao(atualizando ? 'Prazo atualizado com sucesso.' : 'Prazo registrado com sucesso.')
    } catch {
      setErroPrazo('Não foi possível conectar com o servidor.')
    } finally {
      setSalvandoPrazo(false)
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
      exibirMensagemAcao(
        await mensagemDaApi(resposta, 'Não foi possível consultar os logs de auditoria.'),
        'erro'
      )
      return
    }

    const dados = await resposta.json()
    setLogsAuditoria(dados)
  } catch {
    exibirMensagemAcao('Não foi possível conectar com o servidor.', 'erro')
  }
}
  async function buscarCep() {
    if (buscandoCep) {
      return
    }

    setErroCep('')
    setEnderecoEmpresa('')

    const cep = cepEmpresa.replace(/\D/g, '')
    if (cep.length !== 8) {
      setErroCep('Informe um CEP válido com 8 dígitos.')
      return
    }

    setBuscandoCep(true)
    try {
      const resposta = await fetch(`${API}/api/cep/${cep}`, {
        headers: {
          Authorization: `Bearer ${token}`
        }
      })

      if (!resposta.ok) {
        setErroCep(await mensagemDaApi(resposta, 'Não foi possível consultar o CEP.'))
        return
      }

      const dados = await resposta.json()
      const endereco = [
        dados.logradouro,
        dados.bairro,
        dados.localidade && dados.uf ? `${dados.localidade}/${dados.uf}` : dados.localidade
      ].filter(Boolean).join(', ')
      setEnderecoEmpresa(endereco)
    } catch {
      setErroCep('Não foi possível conectar com o servidor.')
    } finally {
      setBuscandoCep(false)
    }
  }

  async function cadastrarEstagio(event) {
    event.preventDefault()

    setErroProcesso('')

    const empresa = nomeEmpresa.trim()
    const cep = cepEmpresa.replace(/\D/g, '')
    const endereco = enderecoEmpresa.trim()
    const supervisor = emailSupervisor.trim().toLowerCase()

    if (!empresa) {
      setErroProcesso('Informe o nome da empresa.')
      return
    }

    if (!emailValido(supervisor)) {
      setErroProcesso('Informe um e-mail válido para o supervisor.')
      return
    }

    if (!cnpjValido(cnpjEmpresa)) {
      setErroProcesso('Informe um CNPJ válido.')
      return
    }

    if (cep.length !== 8) {
      setErroProcesso('Informe um CEP válido com 8 dígitos.')
      return
    }

    if (!endereco) {
      setErroProcesso('Informe o endereço da empresa.')
      return
    }

    if (!dataInicio || !dataFim) {
      setErroProcesso('Informe as datas inicial e final.')
      return
    }

    if (dataFim < dataInicio) {
      setErroProcesso('A data final não pode ser anterior à data inicial.')
      return
    }

    const processo = {
      nomeEmpresa: empresa,
      cnpjEmpresa,
      cepEmpresa: cep,
      enderecoEmpresa: endereco,
      dataInicio,
      dataFim,
      emailSupervisor: supervisor
    }

    setSalvandoProcesso(true)
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

        setNomeEmpresa('')
        setCnpjEmpresa('')
        setEmailSupervisor('')
        setCepEmpresa('')
        setEnderecoEmpresa('')
        setDataInicio('')
        setDataFim('')

        exibirMensagemAcao('Solicitação enviada. Aguardando confirmação do supervisor.')
      } else {
        setErroProcesso(
          await mensagemDaApi(resposta, 'Não foi possível cadastrar o estágio.')
        )
      }
    } catch {
      setErroProcesso('Não foi possível conectar com o servidor.')
    } finally {
      setSalvandoProcesso(false)
    }
  }

  const propriedadesCabecalho = {
    usuario,
    tela,
    secaoInstituicao,
    aoIrInicio: () => setTela(usuario ? 'sistema' : 'login'),
    aoIrSecao: (secao) => {
      setTela('sistema')
      setSecaoInstituicao(secao)
      if (secao === 'dashboard') {
        buscarResumoDashboard()
      }
      if (secao === 'auditoria') {
        buscarAuditoria()
      }
    },
    aoIrPrivacidade: () => setTela('privacidade'),
    aoIrTermos: () => setTela('termos'),
    aoSair: sair,
    token,
    aoAbrirProcesso: (processoId) => {
      setTela('sistema')
      setSecaoInstituicao(usuario?.perfil === 'EMPRESA' ? 'supervisionados' : 'processos')
      if (usuario?.perfil === 'INSTITUICAO') {
        buscarProcessos()
      } else if (usuario?.perfil === 'ALUNO') {
        buscarMeusProcessos()
      } else {
        buscarProcessos()
      }
      setHistoricoAberto(processoId)
    }
  }

  if (tela === 'ativacaoAluno') {
    return (
      <div className="app">
        <Cabecalho {...propriedadesCabecalho} />
        <AtivacaoAluno
          tokenAtivacao={tokenAtivacaoInicial}
          aoConcluir={() => setTela('login')}
        />
      </div>
    )
  }

  if (tela === 'verificacaoEmail') {
    return (
      <div className="app">
        <Cabecalho {...propriedadesCabecalho} />
        <VerificacaoEmail
          tokenVerificacao={tokenVerificacaoInicial}
          aoConcluir={() => setTela('login')}
        />
      </div>
    )
  }

  if (tela === 'recuperacaoSenha') {
    return (
      <div className="app">
        <Cabecalho {...propriedadesCabecalho} />
        <SolicitacaoRecuperacaoSenha apiBase={API} aoVoltar={() => setTela('login')} />
      </div>
    )
  }

  if (tela === 'redefinicaoSenha') {
    return (
      <div className="app">
        <Cabecalho {...propriedadesCabecalho} />
        <RedefinicaoSenha
          apiBase={API}
          tokenRecuperacao={tokenRecuperacaoInicial}
          aoConcluir={() => setTela('login')}
        />
      </div>
    )
  }

  if (tela === 'solicitacaoInstituicao') {
    return <div className="app"><Cabecalho {...propriedadesCabecalho} />
      <SolicitacaoInstituicaoPublica apiBase={API} aoVoltar={() => setTela('login')} /></div>
  }

  if (tela === 'verificacaoInstituicao' || tela === 'ativacaoInstituicao') {
    return <div className="app"><Cabecalho {...propriedadesCabecalho} />
      <ConfirmacaoInstituicao apiBase={API}
        token={tela === 'ativacaoInstituicao' ? tokenAtivacaoInstituicao : tokenVerificacaoInstituicao}
        tipo={tela === 'ativacaoInstituicao' ? 'ativacao' : 'verificacao'}
        aoConcluir={() => setTela('login')} /></div>
  }

  if (tela === 'privacidade') {
    return (
      <div className="app">
        <Cabecalho {...propriedadesCabecalho} mostrarSair={Boolean(usuario)} />
        <PaginaPrivacidade
          documentosLegais={documentosLegais}
          usuario={usuario}
          token={token}
        />
      </div>
    )
  }

  if (tela === 'termos') {
    return (
      <div className="app">
        <Cabecalho {...propriedadesCabecalho} mostrarSair={Boolean(usuario)} />
        <PaginaTermos documentosLegais={documentosLegais} />
      </div>
    )
  }

  if (!usuario && tela === 'cadastro') {
    return (
      <div className="app">
        <Cabecalho {...propriedadesCabecalho} />
        <CadastroSupervisor
          aoAbrirTermos={() => setTela('termos')}
          aoAbrirPrivacidade={() => setTela('privacidade')}
          aoVoltarLogin={() => setTela('login')}
        />
      </div>
    )
  }

  if (!usuario) {
    return (
      <div className="app">
        <Cabecalho {...propriedadesCabecalho} />

        <main className="conteudo pagina-autenticacao">
          <header className="cabecalho cabecalho-autenticacao">
            <p className="cabecalho-contexto">ACESSO AO SISTEMA</p>
            <h2>Entrar no GAEA</h2>
            <p className="cabecalho-descricao">
              Informe seus dados para acessar sua área.
            </p>
          </header>

          <section className="formulario card-autenticacao">
            <form onSubmit={fazerLogin}>
              <div className="campo">
                <label htmlFor="emailLogin">E-mail</label>
                <input
                  id="emailLogin"
                  type="email"
                  value={emailLogin}
                  onChange={(e) => setEmailLogin(e.target.value)}
                  autoComplete="email"
                  required
                  autoFocus
                />
              </div>

              <CampoSenha id="senhaLogin" label="Senha" value={senhaLogin}
                onChange={(event) => setSenhaLogin(event.target.value)}
                autoComplete="current-password" />

              <div className="acoes-login">
                <button className="link-recuperacao" type="button"
                  onClick={() => setTela('recuperacaoSenha')}>
                  Esqueceu sua senha?
                </button>
              </div>

              {erroLogin && (
                <p className="mensagem-erro alerta-autenticacao" role="alert">{erroLogin}</p>
              )}

              <button type="submit" disabled={carregandoLogin}>
                {carregandoLogin ? 'Entrando...' : 'Entrar'}
              </button>
            </form>

            <div className="acao-secundaria">
              <p>Ainda não possui uma conta?</p>
              <div className="acoes-criacao-conta">
                <button
                  type="button"
                  onClick={() => setTela('cadastro')}
                >
                  Cadastrar supervisor de empresa
                </button>
                <button type="button" onClick={() => setTela('solicitacaoInstituicao')}>
                  Solicitar cadastro de instituição
                </button>
              </div>
            </div>
          </section>
        </main>
      </div>
    )
  }

  if (usuario.perfil === 'ADMIN_GAEA') {
    return <div className="app"><Cabecalho {...propriedadesCabecalho} mostrarSair />
      <InstituicoesAdmin apiBase={API} token={token} /></div>
  }

  if (usuario.perfil !== 'INSTITUICAO') {
    return (
      <div className="app">
        <Cabecalho {...propriedadesCabecalho} mostrarSair />
        <MensagemAcaoProcesso
          mensagem={mensagemAcaoProcesso}
          aoFechar={() => setMensagemAcaoProcesso(null)}
        />

        <main className="conteudo">
          {secaoInstituicao === 'dashboard' ? (
            <>
              <header className="cabecalho">
                <p className="cabecalho-contexto">
                  {usuario.perfil === 'ALUNO' ? 'VISÃO DO ALUNO' : 'VISÃO DO SUPERVISOR'}
                </p>
                <h2>Dashboard de Estágios</h2>
                <p className="cabecalho-descricao">
                  {usuario.perfil === 'ALUNO'
                    ? 'Acompanhe os indicadores dos seus processos de estágio.'
                    : 'Acompanhe os indicadores dos estágios sob sua supervisão confirmada.'}
                </p>
              </header>

              <DashboardProcessos
                resumo={resumoDashboard}
                carregando={carregandoDashboard}
                erro={erroDashboard}
                aoTentarNovamente={() => buscarResumoDashboard()}
              />
            </>
          ) : (
            <>
          <header className="cabecalho">
              <p className="cabecalho-contexto">
                {usuario.perfil === 'ALUNO' ? 'ÁREA DO ALUNO' : 'ÁREA DO SUPERVISOR'}
              </p>
              <h2>
                {usuario.perfil === 'ALUNO'
                  ? secaoInstituicao === 'solicitar' ? 'Solicitar novo estágio' : 'Meus processos'
                  : secaoInstituicao === 'solicitacoes'
                    ? 'Solicitações de supervisão'
                    : secaoInstituicao === 'acompanhamento'
                      ? 'Prazos e pendências'
                      : 'Estágios supervisionados'}
              </h2>
              <p className="cabecalho-descricao">
                Olá, {usuario.nome}. Consulte apenas as informações vinculadas à sua conta.
              </p>
          </header>

          {usuario.perfil === 'ALUNO' && secaoInstituicao === 'solicitar' && (
            <section className="formulario">
              <div className="secao-cabecalho">
                <h3>Solicitar novo estágio</h3>
                <p>Informe os dados da empresa e do supervisor responsável.</p>
              </div>

              <form onSubmit={cadastrarEstagio}>
                <div className="dados-somente-leitura" aria-label="Dados do aluno">
                  <div className="campo">
                    <label htmlFor="alunoNome">Nome do aluno</label>
                    <input id="alunoNome" value={usuario.nome} readOnly />
                  </div>
                  <div className="campo">
                    <label htmlFor="alunoEmail">E-mail</label>
                    <input id="alunoEmail" value={usuario.email} readOnly />
                  </div>
                  <div className="campo">
                    <label htmlFor="alunoMatricula">Matrícula (RA/RGM)</label>
                    <input id="alunoMatricula" value={usuario.matricula || ''} readOnly />
                  </div>
                  <div className="campo">
                    <label htmlFor="alunoInstituicao">Instituição</label>
                    <input
                      id="alunoInstituicao"
                      value={
                        usuario.instituicao
                          ? apresentacaoInstituicao(usuario.instituicao).nomeComSigla
                          : ''
                      }
                      readOnly
                    />
                  </div>
                </div>

                <div className="campo">
                  <label htmlFor="nomeEmpresaAluno">Empresa</label>
                  <input id="nomeEmpresaAluno" value={nomeEmpresa} onChange={(event) => setNomeEmpresa(event.target.value)} maxLength="255" required />
                </div>
                <CampoCnpj id="cnpjEmpresaAluno" value={cnpjEmpresa} onChange={setCnpjEmpresa} />
                <div className="campo">
                  <label htmlFor="cepEmpresaAluno">CEP da empresa</label>
                  <div className="campo-cep">
                    <input
                      id="cepEmpresaAluno"
                      value={cepEmpresa}
                      onChange={(event) => {
                        setCepEmpresa(event.target.value)
                        setErroCep('')
                      }}
                      placeholder="00000-000"
                      maxLength="9"
                      inputMode="numeric"
                      required
                    />
                    <button type="button" onClick={buscarCep} disabled={buscandoCep}>
                      {buscandoCep ? 'Buscando...' : 'Buscar CEP'}
                    </button>
                  </div>
                  {erroCep && <p className="mensagem-erro" role="alert">{erroCep}</p>}
                </div>
                <div className="campo">
                  <label htmlFor="enderecoEmpresaAluno">Endereço da empresa</label>
                  <input id="enderecoEmpresaAluno" value={enderecoEmpresa} onChange={(event) => setEnderecoEmpresa(event.target.value)} maxLength="500" required />
                </div>
                <div className="campo">
                  <label htmlFor="emailSupervisorAluno">E-mail do supervisor</label>
                  <input id="emailSupervisorAluno" type="email" value={emailSupervisor} onChange={(event) => setEmailSupervisor(event.target.value)} maxLength="255" required />
                </div>
                <div className="campo">
                  <label htmlFor="dataInicioAluno">Data de início</label>
                  <input id="dataInicioAluno" type="date" value={dataInicio} onChange={(event) => setDataInicio(event.target.value)} required />
                </div>
                <div className="campo">
                  <label htmlFor="dataFimAluno">Data de término</label>
                  <input id="dataFimAluno" type="date" value={dataFim} onChange={(event) => setDataFim(event.target.value)} min={dataInicio} required />
                </div>

                {erroProcesso && <p className="mensagem-erro">{erroProcesso}</p>}
                <button type="submit" disabled={salvandoProcesso}>
                  {salvandoProcesso ? 'Enviando...' : 'Solicitar estágio'}
                </button>
              </form>
            </section>
          )}


          {((usuario.perfil === 'ALUNO' && secaoInstituicao === 'processos')
            || (usuario.perfil === 'EMPRESA' && secaoInstituicao === 'solicitacoes')) && (
          <section className="formulario">
            <div className="secao-cabecalho">
              <h3>
                {usuario.perfil === 'ALUNO'
                  ? 'Meus processos'
                  : 'Solicitações de supervisão'}
              </h3>

              <p>
                {usuario.perfil === 'ALUNO'
                  ? 'Acompanhe suas solicitações de estágio.'
                  : 'Confirme ou recuse os vínculos direcionados ao seu e-mail verificado.'}
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
                    <span>Ações</span>
                  </div>

                  {processos.map((processo) => (
                    <div className="processo-container" key={processo.id}>
                      <div className="processo">
                        <p>
                          <strong>Empresa</strong>
                          <span>{processo.nomeEmpresa}</span>
                        </p>

                        <p>
                          <strong>Período</strong>
                          <span>
                            {formatarData(processo.dataInicio)} — {formatarData(processo.dataFim)}
                          </span>
                        </p>

                        <p>
                          <strong>Status</strong>
                          <span className={`status status-${processo.status}`}>
                            {rotulosStatus[processo.status] || processo.status}
                          </span>
                        </p>

                        <p>
                          <strong>Supervisor</strong>
                          <span>
                            {rotulosVinculo[processo.vinculoSupervisor?.situacao]
                              || 'Vínculo legado não informado'}
                          </span>
                        </p>

                        {processo.vinculoSupervisor?.situacao === 'RECUSADO' && (
                          <div className="troca-supervisor">
                            <p>
                              <strong>Justificativa da recusa</strong>
                              <span>{processo.vinculoSupervisor.justificativaRecusa}</span>
                            </p>
                            <label htmlFor={`novo-supervisor-${processo.id}`}>Novo e-mail do supervisor</label>
                            <input
                              id={`novo-supervisor-${processo.id}`}
                              type="email"
                              value={novosSupervisores[processo.id] || ''}
                              onChange={(event) => setNovosSupervisores((atuais) => ({
                                ...atuais,
                                [processo.id]: event.target.value
                              }))}
                            />
                            <button
                              type="button"
                              disabled={acaoSupervisao === `troca-${processo.id}`}
                              onClick={() => indicarNovoSupervisor(processo)}
                            >
                              {acaoSupervisao === `troca-${processo.id}` ? 'Enviando...' : 'Indicar outro supervisor'}
                            </button>
                          </div>
                        )}

                        <div className="processo-acoes processo-acoes-processo">
                          <button
                            className="acao-historico"
                            type="button"
                            onClick={() => buscarHistorico(processo.id)}
                            aria-pressed={historicoAberto === processo.id}
                          >
                            {historicoAberto === processo.id ? 'Fechar histórico' : 'Ver histórico'}
                          </button>
                          <button
                            className="acao-pendencias"
                            type="button"
                            onClick={() => buscarPendencias(processo.id)}
                            aria-pressed={pendenciasAbertas === processo.id}
                          >
                            {pendenciasAbertas === processo.id ? 'Fechar pendências' : 'Ver pendências'}
                          </button>
                          <button
                            className="acao-prazos"
                            type="button"
                            onClick={() => buscarPrazos(processo.id)}
                            aria-pressed={prazosAbertos === processo.id}
                          >
                            {prazosAbertos === processo.id ? 'Fechar prazos' : 'Ver prazos'}
                          </button>
                          <button
                            className="acao-documentos"
                            type="button"
                            onClick={() => setDocumentosAbertos(
                              documentosAbertos === processo.id ? null : processo.id
                            )}
                            aria-pressed={documentosAbertos === processo.id}
                          >
                            {documentosAbertos === processo.id ? 'Fechar documentos' : 'Ver documentos'}
                          </button>
                          <button
                            className="acao-plano"
                            type="button"
                            onClick={() => setPlanosAbertos(
                              planosAbertos === processo.id ? null : processo.id
                            )}
                            aria-pressed={planosAbertos === processo.id}
                          >
                            {planosAbertos === processo.id ? 'Fechar plano' : 'Plano de atividades'}
                          </button>
                          <button
                            className="acao-carga-horaria"
                            type="button"
                            onClick={() => setCargasHorariasAbertas(
                              cargasHorariasAbertas === processo.id ? null : processo.id
                            )}
                            aria-pressed={cargasHorariasAbertas === processo.id}
                          >
                            {cargasHorariasAbertas === processo.id ? 'Fechar carga horária' : 'Carga horária'}
                          </button>
                        </div>
                      </div>

                      {historicoAberto === processo.id && (
                        <HistoricoProcesso
                          itens={historicos[processo.id] || []}
                          carregando={carregandoHistorico === processo.id}
                          erro={errosHistorico[processo.id]}
                        />
                      )}
                      {pendenciasAbertas === processo.id && (
                        <PendenciasProcesso
                          itens={pendencias[processo.id] || []}
                          carregando={carregandoPendencias === processo.id}
                          erro={errosPendencias[processo.id]}
                          podeGerenciar={false}
                          resolvendoId={null}
                        />
                      )}
                      {prazosAbertos === processo.id && (
                        <PrazosProcesso
                          itens={prazos[processo.id] || []}
                          carregando={carregandoPrazos === processo.id}
                          erro={errosPrazos[processo.id]}
                          podeGerenciar={false}
                        />
                      )}
                      {documentosAbertos === processo.id && (
                        <DocumentosProcesso
                          apiBase={API}
                          token={token}
                          processoId={processo.id}
                          perfil={usuario.perfil}
                          aoAtualizarHistorico={() => {
                            if (historicoAberto === processo.id) buscarHistorico(processo.id, true)
                          }}
                        />
                      )}
                      {planosAbertos === processo.id && (
                        <PlanoAtividadesProcesso
                          apiBase={API}
                          token={token}
                          processoId={processo.id}
                          perfil={usuario.perfil}
                        />
                      )}
                      {cargasHorariasAbertas === processo.id && (
                        <CargaHorariaProcesso
                          apiBase={API}
                          token={token}
                          processoId={processo.id}
                          perfil={usuario.perfil}
                        />
                      )}
                    </div>
                  ))}
                </div>
              )
            ) : (
              <div className="solicitacoes-supervisao">
                {!usuario.emailVerificado ? (
                  <div className="estado-vazio">
                    <h4>Verifique seu e-mail</h4>
                    <p>As solicitações ficam protegidas até a confirmação da titularidade do e-mail.</p>
                    <button type="button" disabled={acaoSupervisao === 'reenviar-email'} onClick={reenviarVerificacaoEmail}>
                      {acaoSupervisao === 'reenviar-email' ? 'Solicitando...' : 'Reenviar link de verificação'}
                    </button>
                  </div>
                ) : solicitacoesSupervisao.length === 0 ? (
                  <p className="estado-vazio">Nenhuma solicitação de supervisão pendente.</p>
                ) : (
                  solicitacoesSupervisao.map((solicitacao) => (
                    <article className="solicitacao-supervisao" key={solicitacao.vinculoId}>
                      <h4>{solicitacao.empresa}</h4>
                      <dl>
                        <div><dt>Aluno</dt><dd>{solicitacao.aluno}</dd></div>
                        <div><dt>Instituição</dt><dd>{(() => {
                          const instituicao = apresentacaoInstituicao(solicitacao.instituicao)
                          return instituicao.nomeComSigla
                        })()}</dd></div>
                        <div><dt>CNPJ</dt><dd>{solicitacao.cnpjEmpresa}</dd></div>
                        <div><dt>Período</dt><dd>{formatarData(solicitacao.dataInicio)} — {formatarData(solicitacao.dataFim)}</dd></div>
                      </dl>
                      {recusaSupervisaoAberta === solicitacao.vinculoId ? (
                        <div className="recusa-supervisao">
                          <label htmlFor={`justificativa-${solicitacao.vinculoId}`}>
                            Justificativa para recusa
                          </label>
                          <textarea
                            id={`justificativa-${solicitacao.vinculoId}`}
                            value={justificativasRecusa[solicitacao.vinculoId] || ''}
                            onChange={(event) => setJustificativasRecusa((atuais) => ({
                              ...atuais,
                              [solicitacao.vinculoId]: event.target.value
                            }))}
                            maxLength="1000"
                            placeholder="Explique por que não poderá supervisionar este estágio"
                            autoFocus
                          />
                          <div className="processo-acoes processo-acoes-confirmacao">
                            <button
                              type="button"
                              disabled={acaoSupervisao !== null}
                              onClick={() => setRecusaSupervisaoAberta(null)}
                            >
                              Cancelar
                            </button>
                            <button
                              className="botao-perigo"
                              type="button"
                              disabled={acaoSupervisao !== null}
                              onClick={() => responderSupervisao(solicitacao, 'recusar')}
                            >
                              {acaoSupervisao === `recusar-${solicitacao.vinculoId}`
                                ? 'Recusando...'
                                : 'Confirmar recusa'}
                            </button>
                          </div>
                        </div>
                      ) : (
                        <div className="processo-acoes processo-acoes-confirmacao">
                          <button type="button" disabled={acaoSupervisao !== null} onClick={() => responderSupervisao(solicitacao, 'confirmar')}>
                            Confirmar supervisão
                          </button>
                          <button
                            className="botao-perigo"
                            type="button"
                            disabled={acaoSupervisao !== null}
                            onClick={() => setRecusaSupervisaoAberta(solicitacao.vinculoId)}
                          >
                            Recusar
                          </button>
                        </div>
                      )}
                    </article>
                  ))
                )}
                {erroSupervisao && <p className="mensagem-erro" role="alert">{erroSupervisao}</p>}
              </div>
            )}
          </section>
          )}

          {usuario.perfil === 'EMPRESA' && !usuario.emailVerificado
            && (secaoInstituicao === 'supervisionados'
              || secaoInstituicao === 'acompanhamento') && (
            <section className="formulario">
              <div className="estado-vazio">
                <h3>Verifique seu e-mail para acessar esta seção</h3>
                <p>Os processos supervisionados ficam disponíveis após a confirmação do e-mail.</p>
                <button
                  type="button"
                  onClick={() => setSecaoInstituicao('solicitacoes')}
                >
                  Ir para solicitações de supervisão
                </button>
              </div>
            </section>
          )}

          {usuario.perfil === 'EMPRESA' && usuario.emailVerificado
            && (secaoInstituicao === 'supervisionados'
              || secaoInstituicao === 'acompanhamento') && (
            <section className="formulario">
              <div className="secao-cabecalho">
                <h3>
                  {secaoInstituicao === 'acompanhamento'
                    ? 'Prazos e pendências dos estágios'
                    : 'Estágios supervisionados'}
                </h3>
                <p>
                  {secaoInstituicao === 'acompanhamento'
                    ? 'Consulte as pendências e os prazos dos processos com supervisão confirmada.'
                    : 'Consulte o histórico e o andamento dos processos confirmados.'}
                </p>
              </div>

              {processos.length === 0 ? (
                <p className="estado-vazio">Nenhum processo com supervisão confirmada.</p>
              ) : (
                <div className="lista-processos lista-processos-aluno">
                  {processos.map((processo) => (
                    <div className="processo-container" key={processo.id}>
                      <div className="processo">
                        <p><strong>Aluno</strong><span>{processo.nomeAluno}</span></p>
                        <p><strong>Empresa</strong><span>{processo.nomeEmpresa}</span></p>
                        <p>
                          <strong>Status</strong>
                          <span className={`status status-${processo.status}`}>
                            {rotulosStatus[processo.status] || processo.status}
                          </span>
                        </p>
                        <div className="processo-acoes processo-acoes-processo">
                          {secaoInstituicao !== 'acompanhamento' && <button
                            className="acao-historico"
                            type="button"
                            onClick={() => buscarHistorico(processo.id)}
                            aria-pressed={historicoAberto === processo.id}
                          >
                            {historicoAberto === processo.id ? 'Fechar histórico' : 'Ver histórico'}
                          </button>}
                          <button
                            className="acao-pendencias"
                            type="button"
                            onClick={() => buscarPendencias(processo.id)}
                            aria-pressed={pendenciasAbertas === processo.id}
                          >
                            {pendenciasAbertas === processo.id ? 'Fechar pendências' : 'Ver pendências'}
                          </button>
                          <button
                            className="acao-prazos"
                            type="button"
                            onClick={() => buscarPrazos(processo.id)}
                            aria-pressed={prazosAbertos === processo.id}
                          >
                            {prazosAbertos === processo.id ? 'Fechar prazos' : 'Ver prazos'}
                          </button>
                          <button
                            className="acao-documentos"
                            type="button"
                            onClick={() => setDocumentosAbertos(
                              documentosAbertos === processo.id ? null : processo.id
                            )}
                            aria-pressed={documentosAbertos === processo.id}
                          >
                            {documentosAbertos === processo.id ? 'Fechar documentos' : 'Ver documentos'}
                          </button>
                          <button
                            className="acao-plano"
                            type="button"
                            onClick={() => setPlanosAbertos(
                              planosAbertos === processo.id ? null : processo.id
                            )}
                            aria-pressed={planosAbertos === processo.id}
                          >
                            {planosAbertos === processo.id ? 'Fechar plano' : 'Plano de atividades'}
                          </button>
                          <button
                            className="acao-carga-horaria"
                            type="button"
                            onClick={() => setCargasHorariasAbertas(
                              cargasHorariasAbertas === processo.id ? null : processo.id
                            )}
                            aria-pressed={cargasHorariasAbertas === processo.id}
                          >
                            {cargasHorariasAbertas === processo.id ? 'Fechar carga horária' : 'Carga horária'}
                          </button>
                        </div>
                      </div>
                      {secaoInstituicao !== 'acompanhamento'
                        && historicoAberto === processo.id && (
                        <HistoricoProcesso
                          itens={historicos[processo.id] || []}
                          carregando={carregandoHistorico === processo.id}
                          erro={errosHistorico[processo.id]}
                        />
                      )}
                      {pendenciasAbertas === processo.id && (
                        <PendenciasProcesso
                          itens={pendencias[processo.id] || []}
                          carregando={carregandoPendencias === processo.id}
                          erro={errosPendencias[processo.id]}
                          podeGerenciar={false}
                          resolvendoId={null}
                        />
                      )}
                      {prazosAbertos === processo.id && (
                        <PrazosProcesso
                          itens={prazos[processo.id] || []}
                          carregando={carregandoPrazos === processo.id}
                          erro={errosPrazos[processo.id]}
                          podeGerenciar={false}
                        />
                      )}
                      {documentosAbertos === processo.id && (
                        <DocumentosProcesso
                          apiBase={API}
                          token={token}
                          processoId={processo.id}
                          perfil={usuario.perfil}
                          aoAtualizarHistorico={() => {
                            if (historicoAberto === processo.id) buscarHistorico(processo.id, true)
                          }}
                        />
                      )}
                      {planosAbertos === processo.id && (
                        <PlanoAtividadesProcesso
                          apiBase={API}
                          token={token}
                          processoId={processo.id}
                          perfil={usuario.perfil}
                        />
                      )}
                      {cargasHorariasAbertas === processo.id && (
                        <CargaHorariaProcesso
                          apiBase={API}
                          token={token}
                          processoId={processo.id}
                          perfil={usuario.perfil}
                        />
                      )}
                    </div>
                  ))}
                </div>
              )}
            </section>
          )}
            </>
          )}
        </main>
      </div>
    )
  }

  return (
    <div className="app">
      <Cabecalho {...propriedadesCabecalho} mostrarSair />
      <MensagemAcaoProcesso
        mensagem={mensagemAcaoProcesso}
        aoFechar={() => setMensagemAcaoProcesso(null)}
      />

      {processoReabertura && (
        <ModalReabertura
          processo={processoReabertura}
          justificativa={justificativaReabertura}
          erro={erroReabertura}
          carregando={reaberturaEmAndamento}
          aoMudarJustificativa={(valor) => {
            setJustificativaReabertura(valor)
            if (erroReabertura) {
              setErroReabertura('')
            }
          }}
          aoCancelar={cancelarReabertura}
          aoConfirmar={confirmarReabertura}
        />
      )}

      {processoNovaPendencia && (
        <ModalNovaPendencia
          processo={processoNovaPendencia}
          descricao={descricaoPendencia}
          erro={erroNovaPendencia}
          carregando={salvandoPendencia}
          aoMudarDescricao={(valor) => {
            setDescricaoPendencia(valor)
            if (erroNovaPendencia) {
              setErroNovaPendencia('')
            }
          }}
          aoCancelar={cancelarNovaPendencia}
          aoConfirmar={registrarPendencia}
        />
      )}

      {processoPrazo && (
        <ModalPrazo
          processo={processoPrazo}
          prazo={prazoEdicao}
          descricao={descricaoPrazo}
          dataVencimento={dataVencimentoPrazo}
          erro={erroPrazo}
          carregando={salvandoPrazo}
          aoMudarDescricao={(valor) => {
            setDescricaoPrazo(valor)
            setErroPrazo('')
          }}
          aoMudarData={(valor) => {
            setDataVencimentoPrazo(valor)
            setErroPrazo('')
          }}
          aoCancelar={cancelarPrazo}
          aoConfirmar={salvarPrazo}
        />
      )}

      <main className="conteudo">
        {secaoInstituicao === 'dashboard' ? (
          <>
            <header className="cabecalho">
              <p className="cabecalho-contexto">VISÃO INSTITUCIONAL</p>
              <h2>Dashboard de Estágios</h2>
              <p className="cabecalho-descricao">
                Acompanhe os principais indicadores dos processos cadastrados.
              </p>
            </header>

            <DashboardProcessos
              resumo={resumoDashboard}
              carregando={carregandoDashboard}
              erro={erroDashboard}
              aoTentarNovamente={() => buscarResumoDashboard()}
            />
          </>
        ) : secaoInstituicao === 'alunos' ? (
          <GestaoAlunos
            apiBase={API}
            token={token}
            instituicao={usuario.instituicao}
          />
        ) : secaoInstituicao === 'relatorios' ? (
          <RelatoriosProcessos apiBase={API} token={token} />
        ) : secaoInstituicao === 'auditoria' ? (
          <>
            <header className="cabecalho">
              <p className="cabecalho-contexto">RASTREABILIDADE INSTITUCIONAL</p>
              <h2>Auditoria</h2>
              <p className="cabecalho-descricao">
                Consulte as principais ações registradas para a sua instituição.
              </p>
            </header>

            <section className="acompanhamento">
              <div className="secao-cabecalho">
                <h3>Logs de auditoria</h3>
                <p>Os registros são apresentados do mais recente para o mais antigo.</p>
              </div>

              {logsAuditoria.length === 0 ? (
                <p className="estado-vazio">Nenhum registro de auditoria encontrado.</p>
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
                          ? formatarDataHora(log.dataHora)
                          : '-'}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </section>
          </>
        ) : (
          <>
        <header className="cabecalho">
          <div>
            <p className="cabecalho-contexto">GESTÃO ACADÊMICA</p>
            <h2>
              {secaoInstituicao === 'acompanhamento'
                ? 'Pendências e prazos'
                : 'Processos de estágio'}
            </h2>
            <p className="cabecalho-descricao">
              {secaoInstituicao === 'acompanhamento'
                ? 'Acompanhe o que precisa ser regularizado e os próximos vencimentos.'
                : 'Consulte e atualize o andamento dos processos de estágio.'}
            </p>
          </div>
        </header>

        <section className="acompanhamento">
          <div className="secao-cabecalho">
            <h3>
              {secaoInstituicao === 'acompanhamento'
                ? 'Acompanhamento por processo'
                : 'Acompanhamento de estágios'}
            </h3>
            <p>
              {secaoInstituicao === 'acompanhamento'
                ? 'Abra as pendências e os prazos do processo que deseja acompanhar.'
                : 'Consulte e atualize o andamento dos processos cadastrados.'}
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
                  className="processo-container"
                  key={processo.id}
                  data-processo-id={processo.id}
                  tabIndex="-1"
                >
                  <div className="processo">
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
                        {formatarData(processo.dataInicio)} — {formatarData(processo.dataFim)}
                      </span>
                    </p>

                    <p>
                      <strong>Status</strong>
                      <span
                        className={`status status-${processo.status}`}
                      >
                        {rotulosStatus[processo.status] || processo.status}
                      </span>
                    </p>

                    <div className="processo-acoes processo-acoes-processo">
                      {secaoInstituicao !== 'acompanhamento' && <>
                      {processo.status === 'PENDENTE'
                        && processo.vinculoSupervisor?.situacao === 'CONFIRMADO' && (
                        <button
                          className="acao-analise"
                          type="button"
                          onClick={() => alterarStatus(processo.id, 'EM_ANALISE')}
                        >
                          Iniciar análise
                        </button>
                      )}

                      {processo.status === 'PENDENTE'
                        && processo.vinculoSupervisor?.situacao !== 'CONFIRMADO' && (
                        <span className="status status-PENDENTE">
                          Aguardando confirmação do supervisor
                        </span>
                      )}

                      {processo.status === 'EM_ANALISE' && (
                        <>
                          <button
                            className="acao-aprovar"
                            type="button"
                            onClick={() => alterarStatus(processo.id, 'APROVADO')}
                          >
                            Aprovar
                          </button>
                          <button
                            className="acao-reprovar"
                            type="button"
                            onClick={() => alterarStatus(processo.id, 'REPROVADO')}
                          >
                            Reprovar
                          </button>
                        </>
                      )}

                      {(processo.status === 'APROVADO' || processo.status === 'REPROVADO') && (
                        <button
                          className="acao-reabrir"
                          type="button"
                          onClick={() => abrirModalReabertura(processo)}
                        >
                          Reabrir
                        </button>
                      )}

                      <button
                        className="acao-historico"
                        type="button"
                        onClick={() => buscarHistorico(processo.id)}
                        aria-pressed={historicoAberto === processo.id}
                      >
                        {historicoAberto === processo.id ? 'Fechar histórico' : 'Histórico'}
                      </button>
                      </>}
                      <button
                        className="acao-pendencias"
                        type="button"
                        onClick={() => buscarPendencias(processo.id)}
                        aria-pressed={pendenciasAbertas === processo.id}
                      >
                        {pendenciasAbertas === processo.id ? 'Fechar pendências' : 'Pendências'}
                      </button>
                      <button
                        className="acao-prazos"
                        type="button"
                        onClick={() => buscarPrazos(processo.id)}
                        aria-pressed={prazosAbertos === processo.id}
                      >
                        {prazosAbertos === processo.id ? 'Fechar prazos' : 'Prazos'}
                      </button>
                      <button
                        className="acao-documentos"
                        type="button"
                        onClick={() => setDocumentosAbertos(
                          documentosAbertos === processo.id ? null : processo.id
                        )}
                        aria-pressed={documentosAbertos === processo.id}
                      >
                        {documentosAbertos === processo.id ? 'Fechar documentos' : 'Documentos'}
                      </button>
                      <button
                        className="acao-plano"
                        type="button"
                        onClick={() => setPlanosAbertos(
                          planosAbertos === processo.id ? null : processo.id
                        )}
                        aria-pressed={planosAbertos === processo.id}
                      >
                        {planosAbertos === processo.id ? 'Fechar plano' : 'Plano de atividades'}
                      </button>
                      <button
                        className="acao-carga-horaria"
                        type="button"
                        onClick={() => setCargasHorariasAbertas(
                          cargasHorariasAbertas === processo.id ? null : processo.id
                        )}
                        aria-pressed={cargasHorariasAbertas === processo.id}
                      >
                        {cargasHorariasAbertas === processo.id ? 'Fechar carga horária' : 'Carga horária'}
                      </button>
                    </div>
                  </div>

                  {secaoInstituicao !== 'acompanhamento' && historicoAberto === processo.id && (
                    <HistoricoProcesso
                      itens={historicos[processo.id] || []}
                      carregando={carregandoHistorico === processo.id}
                      erro={errosHistorico[processo.id]}
                    />
                  )}
                  {pendenciasAbertas === processo.id && (
                    <PendenciasProcesso
                      itens={pendencias[processo.id] || []}
                      carregando={carregandoPendencias === processo.id}
                      erro={errosPendencias[processo.id]}
                      podeGerenciar
                      resolvendoId={resolvendoPendencia}
                      aoNovaPendencia={() => abrirNovaPendencia(processo)}
                      aoResolver={(pendenciaId) => resolverPendenciaDoProcesso(processo.id, pendenciaId)}
                    />
                  )}
                  {prazosAbertos === processo.id && (
                    <PrazosProcesso
                      itens={prazos[processo.id] || []}
                      carregando={carregandoPrazos === processo.id}
                      erro={errosPrazos[processo.id]}
                      podeGerenciar
                      aoNovoPrazo={() => abrirPrazo(processo)}
                      aoEditarPrazo={(prazo) => abrirPrazo(processo, prazo)}
                    />
                  )}
                  {documentosAbertos === processo.id && (
                    <DocumentosProcesso
                      apiBase={API}
                      token={token}
                      processoId={processo.id}
                      perfil={usuario.perfil}
                      aoAtualizarHistorico={() => {
                        if (historicoAberto === processo.id) buscarHistorico(processo.id, true)
                      }}
                    />
                  )}
                  {planosAbertos === processo.id && (
                    <PlanoAtividadesProcesso
                      apiBase={API}
                      token={token}
                      processoId={processo.id}
                      perfil={usuario.perfil}
                    />
                  )}
                  {cargasHorariasAbertas === processo.id && (
                    <CargaHorariaProcesso
                      apiBase={API}
                      token={token}
                      processoId={processo.id}
                      perfil={usuario.perfil}
                    />
                  )}
                </div>
              ))}
            </div>
          )}
        </section>
          </>
        )}
      </main>
    </div>
  )
}

export default App
