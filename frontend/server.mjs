import { createReadStream } from 'node:fs'
import { stat } from 'node:fs/promises'
import { createServer } from 'node:http'
import { extname, join, normalize, relative, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const host = '0.0.0.0'
const port = Number.parseInt(process.env.PORT || '4173', 10)
const distDirectory = resolve(fileURLToPath(new URL('./dist', import.meta.url)))
const indexFile = join(distDirectory, 'index.html')

const contentTypes = new Map([
  ['.css', 'text/css; charset=utf-8'],
  ['.html', 'text/html; charset=utf-8'],
  ['.ico', 'image/x-icon'],
  ['.jpeg', 'image/jpeg'],
  ['.jpg', 'image/jpeg'],
  ['.js', 'text/javascript; charset=utf-8'],
  ['.json', 'application/json; charset=utf-8'],
  ['.map', 'application/json; charset=utf-8'],
  ['.png', 'image/png'],
  ['.svg', 'image/svg+xml; charset=utf-8'],
  ['.webp', 'image/webp'],
  ['.woff', 'font/woff'],
  ['.woff2', 'font/woff2']
])

if (!Number.isInteger(port) || port < 1 || port > 65535) {
  throw new Error('PORT deve ser um número entre 1 e 65535')
}

function resolveRequestedFile(pathname) {
  let decodedPath
  try {
    decodedPath = decodeURIComponent(pathname)
  } catch {
    return null
  }

  const requestedPath = resolve(distDirectory, `.${normalize(decodedPath)}`)
  const pathFromDist = relative(distDirectory, requestedPath)
  if (pathFromDist.startsWith('..') || requestedPath === distDirectory) {
    return null
  }
  return requestedPath
}

async function regularFile(path) {
  try {
    return (await stat(path)).isFile()
  } catch {
    return false
  }
}

function sendFile(request, response, file) {
  const fileFromDist = relative(distDirectory, file)
  const isVersionedAsset = fileFromDist.split(/[\\/]/)[0] === 'assets'

  response.statusCode = 200
  response.setHeader(
    'Content-Type',
    contentTypes.get(extname(file).toLowerCase()) || 'application/octet-stream'
  )
  response.setHeader('X-Content-Type-Options', 'nosniff')
  response.setHeader('X-Frame-Options', 'DENY')
  response.setHeader('Referrer-Policy', 'no-referrer')
  response.setHeader('Permissions-Policy', 'camera=(), microphone=(), geolocation=()')
  response.setHeader('Cross-Origin-Opener-Policy', 'same-origin')
  response.setHeader(
    'Content-Security-Policy',
    "default-src 'self'; base-uri 'self'; connect-src 'self' https:; " +
      "font-src 'self'; form-action 'self'; frame-ancestors 'none'; " +
      "img-src 'self' data:; object-src 'none'; script-src 'self'; style-src 'self'"
  )
  response.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains')
  response.setHeader(
    'Cache-Control',
    isVersionedAsset ? 'public, max-age=31536000, immutable' : 'no-cache'
  )

  if (request.method === 'HEAD') {
    response.end()
    return
  }

  createReadStream(file)
    .on('error', () => {
      if (!response.headersSent) response.writeHead(500)
      response.end('Erro ao ler o arquivo')
    })
    .pipe(response)
}

const server = createServer(async (request, response) => {
  if (request.method !== 'GET' && request.method !== 'HEAD') {
    response.writeHead(405, { Allow: 'GET, HEAD' })
    response.end('Método não permitido')
    return
  }

  const pathname = new URL(request.url || '/', 'http://localhost').pathname
  const requestedFile = resolveRequestedFile(pathname)

  if (requestedFile && await regularFile(requestedFile)) {
    sendFile(request, response, requestedFile)
    return
  }

  if (extname(pathname)) {
    response.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' })
    response.end('Arquivo não encontrado')
    return
  }

  if (await regularFile(indexFile)) {
    sendFile(request, response, indexFile)
    return
  }

  response.writeHead(503, { 'Content-Type': 'text/plain; charset=utf-8' })
  response.end('Frontend ainda não foi compilado')
})

server.listen(port, host, () => {
  console.log(`GAEA frontend disponível em http://${host}:${port}`)
})
