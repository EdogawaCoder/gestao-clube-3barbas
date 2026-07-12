import { defineConfig, loadEnv } from 'vite'

export default defineConfig(({ command, mode }) => {
  const environment = { ...loadEnv(mode, process.cwd(), ''), ...process.env }
  const requiredFirebaseVariables = [
    'VITE_FIREBASE_API_KEY',
    'VITE_FIREBASE_AUTH_DOMAIN',
    'VITE_FIREBASE_PROJECT_ID',
    'VITE_FIREBASE_STORAGE_BUCKET',
    'VITE_FIREBASE_MESSAGING_SENDER_ID',
    'VITE_FIREBASE_APP_ID',
  ]

  if (command === 'build' && mode === 'production') {
    const missing = requiredFirebaseVariables.filter((name) => !environment[name])
    if (missing.length > 0) {
      throw new Error(`Configuração Firebase ausente para o build de produção: ${missing.join(', ')}`)
    }
  }

  return {
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
    preview: {
      port: 4173,
    },
  }
})
