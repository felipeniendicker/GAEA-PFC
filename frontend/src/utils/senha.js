const SENHAS_COMUNS = new Set([
  '12345678', '123456789012', 'password', 'password1!', 'password1234', 'password123!', 'qwerty123456',
  'senha123456', 'senha@123456', 'administrador', 'administrator',
  'senha@123', 'letmein123456', 'welcome123456', 'bemvindo1234', 'gaea12345678'
])

export function avaliarSenha(senha, confirmacao = null) {
  const caracteres = [...senha].length
  const bytes = new TextEncoder().encode(senha).length
  const naoComum = senha.length > 0 && !SENHAS_COMUNS.has(senha.trim().toLowerCase())

  return {
    comprimento: caracteres >= 8 && caracteres <= 64,
    maiuscula: /\p{Lu}/u.test(senha),
    minuscula: /\p{Ll}/u.test(senha),
    numero: /\p{N}/u.test(senha),
    especial: /[^\p{L}\p{N}\s]/u.test(senha),
    tamanhoSeguro: bytes <= 72,
    naoComum,
    confirmacao: confirmacao === null ? true : confirmacao.length > 0 && senha === confirmacao
  }
}

export function senhaAtendePolitica(senha, confirmacao = null) {
  return Object.values(avaliarSenha(senha, confirmacao)).every(Boolean)
}
