import { initializeApp } from 'firebase/app'
import {
  connectAuthEmulator,
  getAuth,
  onAuthStateChanged,
  sendEmailVerification,
  sendPasswordResetEmail,
  signInWithEmailAndPassword,
  signOut,
} from 'firebase/auth'

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
}

export const firebaseConfigured = Object.values(firebaseConfig).every(Boolean)

const firebaseApp = firebaseConfigured ? initializeApp(firebaseConfig) : null
export const auth = firebaseApp ? getAuth(firebaseApp) : null

if (auth && import.meta.env.DEV && import.meta.env.VITE_USE_AUTH_EMULATOR === 'true') {
  connectAuthEmulator(auth, 'http://127.0.0.1:9099', { disableWarnings: true })
}

export function observeAuth(callback) {
  if (!auth) {
    callback(null)
    return () => {}
  }
  return onAuthStateChanged(auth, callback)
}

export async function loginWithEmail(email, password) {
  if (!auth) {
    throw new Error('O Firebase ainda não foi configurado neste ambiente.')
  }
  return signInWithEmailAndPassword(auth, email, password)
}

export async function inspectUserAccess(user, forceRefresh = false) {
  if (forceRefresh) {
    await user.reload()
  }
  const tokenResult = await user.getIdTokenResult(forceRefresh)
  return {
    emailVerified: user.emailVerified,
    role: tokenResult.claims.role || null,
  }
}

export async function resendVerificationEmail(user) {
  return sendEmailVerification(user)
}

export async function requestPasswordReset(email) {
  if (!auth) {
    throw new Error('O Firebase ainda não foi configurado neste ambiente.')
  }
  return sendPasswordResetEmail(auth, email)
}

export async function logout() {
  if (auth) {
    await signOut(auth)
  }
}
