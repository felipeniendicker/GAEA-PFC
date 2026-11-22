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

  const institucionPorId = {
    1: {
      sigla: 'UMC',
      nomeCompleto: 'Universidade de Mogi das Cruzes',
      ficticia: false
    },
    2: {
      sigla: 'FHL',
      nomeCompleto: 'Faculdade Horizonte do Litoral',
      ficticia: true
    },
    3: {
      sigla: 'ISVV',
      nomeCompleto: 'Instituto Superior Vale Verde',
      ficticia: true
    }
  }

  const configuracao = typeof instituicao === 'object'
    ? institucionPorId[Number(instituicao.id)]
    : null

  if (configuracao) return configuracao

  const nome = typeof instituicao === 'object'
    ? (instituicao.nome || instituicao.nomeInstitucional || '')
    : String(instituicao)

  if (/Mogi das Cruzes|UMC/i.test(nome)) {
    return institucionPorId[1]
  }
  if (/Horizonte do Litoral|FHL/i.test(nome)) {
    return institucionPorId[2]
  }
  if (/Vale Verde|ISVV/i.test(nome)) {
    return institucionPorId[3]
  }

  return { sigla: '', nomeCompleto: nome || 'Não informada', ficticia: false }
}
