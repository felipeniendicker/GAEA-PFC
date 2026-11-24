export function lerTokensDaUrl(hash) {
  const parametros = new URLSearchParams((hash || '').replace(/^#/, ''))
  return {
    conviteAluno: parametros.get('conviteAluno') || '',
    verificarEmail: parametros.get('verificarEmail') || '',
    verificarInstituicao: parametros.get('verificarInstituicao') || '',
    ativarInstituicao: parametros.get('ativarInstituicao') || '',
    redefinirSenha: parametros.get('redefinirSenha') || '',
    documentoLegal: parametros.has('termos')
      ? 'termos'
      : parametros.has('privacidade') ? 'privacidade' : ''
  }
}

export function possuiTokenSensivel(tokens) {
  return Boolean(
    tokens.conviteAluno
    || tokens.verificarEmail
    || tokens.verificarInstituicao
    || tokens.ativarInstituicao
    || tokens.redefinirSenha
  )
}
