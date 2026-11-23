export const ROTULOS_STATUS_PROCESSO = {
  PENDENTE: 'Pendente',
  EM_ANALISE: 'Em análise',
  APROVADO: 'Aprovado',
  REPROVADO: 'Reprovado'
}

export const ROTULOS_SITUACAO = {
  PENDENTE: 'Pendente',
  EM_ANALISE: 'Em análise',
  APROVADO: 'Aprovado',
  REPROVADO: 'Reprovado',
  REJEITADO: 'Rejeitado',
  ABERTA: 'Aberta',
  RESOLVIDA: 'Resolvida',
  CONCLUIDA: 'Concluída',
  CANCELADA: 'Cancelada'
}

function dataLocal(valor) {
  if (!valor) return null

  const normalizado = /^\d{4}-\d{2}-\d{2}$/.test(valor)
    ? `${valor}T12:00:00`
    : valor
  const data = new Date(normalizado)

  return Number.isNaN(data.getTime()) ? null : data
}

export function formatarData(valor) {
  const data = dataLocal(valor)
  return data ? data.toLocaleDateString('pt-BR') : '—'
}

export function formatarDataHora(valor) {
  const data = dataLocal(valor)
  return data
    ? data.toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
    : '—'
}

export function rotuloSituacao(valor) {
  return ROTULOS_SITUACAO[valor] || valor || 'Não informado'
}

export function apresentacaoInstituicao(instituicao) {
  if (!instituicao) {
    return { sigla: '', nomeCompleto: 'Não informada', ficticia: false }
  }

  const nome = typeof instituicao === 'object'
    ? (instituicao.nome || instituicao.nomeInstitucional || '')
    : String(instituicao)
  const sigla = typeof instituicao === 'object'
    ? String(instituicao.sigla || '').trim().toUpperCase()
    : ''
  const ficticia = typeof instituicao === 'object'
    ? Boolean(instituicao.ficticia)
    : false

  return {
    sigla,
    nomeCompleto: nome.trim() || 'Não informada',
    ficticia
  }
}
