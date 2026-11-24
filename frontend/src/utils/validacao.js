export function emailValido(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())
}

export function normalizarCnpj(cnpj = '') {
  return cnpj.replace(/\D/g, '').slice(0, 14)
}

export function formatarCnpj(cnpj = '') {
  const numeros = normalizarCnpj(cnpj)

  return numeros
    .replace(/^(\d{2})(\d)/, '$1.$2')
    .replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d)/, '.$1/$2')
    .replace(/(\d{4})(\d)/, '$1-$2')
}

export function cnpjValido(cnpj = '') {
  const numeros = normalizarCnpj(cnpj)

  if (numeros.length !== 14 || /^(\d)\1+$/.test(numeros)) {
    return false
  }

  function calcularDigito(tamanho) {
    let soma = 0
    let peso = tamanho - 7

    for (let indice = 0; indice < tamanho; indice += 1) {
      soma += Number(numeros[indice]) * peso
      peso -= 1

      if (peso === 1) {
        peso = 9
      }
    }

    const resto = soma % 11
    return resto < 2 ? 0 : 11 - resto
  }

  return Number(numeros[12]) === calcularDigito(12) &&
    Number(numeros[13]) === calcularDigito(13)
}

export function erroCnpj(cnpj = '') {
  const numeros = normalizarCnpj(cnpj)

  if (numeros.length === 0) return 'Informe o CNPJ.'
  if (numeros.length < 14) return 'CNPJ incompleto. Informe os 14 dígitos.'
  if (!cnpjValido(numeros)) return 'CNPJ inválido. Confira os dígitos verificadores.'
  return ''
}

export function validarSolicitacaoInstituicao(dados, termosAceitos) {
  return {
    nomeInstitucional: dados.nomeInstitucional.trim() ? '' : 'Informe o nome institucional.',
    cnpj: erroCnpj(dados.cnpj),
    responsavelNome: dados.responsavelNome.trim() ? '' : 'Informe o nome do responsável.',
    emailInstitucional: emailValido(dados.emailInstitucional)
      ? ''
      : 'Informe um e-mail institucional válido.',
    termos: termosAceitos
      ? ''
      : 'É necessário aceitar os Termos de Uso e a Política de Privacidade.'
  }
}

export function possuiErros(erros) {
  return Object.values(erros).some(Boolean)
}
