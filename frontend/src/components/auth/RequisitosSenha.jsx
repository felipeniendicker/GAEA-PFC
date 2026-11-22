import './Autenticacao.css'
import { avaliarSenha } from '../../utils/senha'

export default function RequisitosSenha({ senha, confirmacao = null, id }) {
  const requisitos = avaliarSenha(senha, confirmacao)
  const item = (atendido, texto) => (
    <li className={atendido ? 'requisito-atendido' : ''}>
      <span aria-hidden="true">{atendido ? '✓' : '○'}</span>
      {texto}
    </li>
  )

  return (
    <div className="requisitos-senha" id={id} aria-live="polite">
      <p>Sua senha deve:</p>
      <ul>
        {item(requisitos.comprimento, 'Ter entre 8 e 64 caracteres')}
        {item(requisitos.maiuscula, 'Conter uma letra maiúscula')}
        {item(requisitos.minuscula, 'Conter uma letra minúscula')}
        {item(requisitos.numero, 'Conter um número')}
        {item(requisitos.especial, 'Conter um caractere especial')}
        {item(requisitos.tamanhoSeguro, 'Não exceder o tamanho máximo permitido')}
        {item(requisitos.naoComum, 'Não ser uma senha comum ou previsível')}
        {confirmacao !== null && item(requisitos.confirmacao, 'Coincidir com a confirmação')}
      </ul>
      <small>Espaços são aceitos e não contam como caractere especial.</small>
    </div>
  )
}
