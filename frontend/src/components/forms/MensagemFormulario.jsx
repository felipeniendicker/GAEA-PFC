export default function MensagemFormulario({ tipo = 'erro', children }) {
  if (!children) return null

  const role = tipo === 'erro' ? 'alert' : 'status'
  return <p className={`mensagem-formulario mensagem-${tipo}`} role={role}>{children}</p>
}
