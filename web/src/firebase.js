import { initializeApp } from 'firebase/app'
import {
  PhoneAuthProvider,
  PhoneMultiFactorGenerator,
  RecaptchaVerifier,
  connectAuthEmulator,
  getAuth,
  getMultiFactorResolver,
  multiFactor,
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
  try {
    return await signInWithEmailAndPassword(auth, email, password)
  } catch (error) {
    if (error.code !== 'auth/multi-factor-auth-required') {
      throw error
    }
    const resolver = getMultiFactorResolver(auth, error)
    return {
      requiresSecondFactor: true,
      resolver,
      factors: resolver.hints,
    }
  }
}

export async function sendSmsChallenge(resolver, factorIndex = 0) {
  const hint = resolver.hints[factorIndex]
  const recaptchaVerifier = new RecaptchaVerifier(auth, 'recaptcha-container', {
    size: 'invisible',
  })
  const phoneInfoOptions = {
    multiFactorHint: hint,
    session: resolver.session,
  }
  const provider = new PhoneAuthProvider(auth)
  try {
    const verificationId = await provider.verifyPhoneNumber(phoneInfoOptions, recaptchaVerifier)
    return { verificationId, recaptchaVerifier }
  } catch (error) {
    recaptchaVerifier.clear()
    throw error
  }
}

export async function finishSmsChallenge(resolver, verificationId, code) {
  const credential = PhoneAuthProvider.credential(verificationId, code)
  const assertion = PhoneMultiFactorGenerator.assertion(credential)
  return resolver.resolveSignIn(assertion)
}

export async function inspectUserAccess(user, forceRefresh = false) {
  if (forceRefresh) {
    await user.reload()
  }
  const tokenResult = await user.getIdTokenResult(forceRefresh)
  const firebaseClaims = tokenResult.claims.firebase || {}
  return {
    emailVerified: user.emailVerified,
    enrolledFactors: multiFactor(user).enrolledFactors,
    role: tokenResult.claims.role || null,
    signedInWithSecondFactor: Boolean(firebaseClaims.sign_in_second_factor),
  }
}

export async function resendVerificationEmail(user) {
  return sendEmailVerification(user)
}

export async function startMfaEnrollment(user, phoneNumber) {
  const session = await multiFactor(user).getSession()
  const recaptchaVerifier = new RecaptchaVerifier(auth, 'recaptcha-container', {
    size: 'invisible',
  })
  const provider = new PhoneAuthProvider(auth)
  try {
    const verificationId = await provider.verifyPhoneNumber(
      { phoneNumber, session },
      recaptchaVerifier,
    )
    return { verificationId, recaptchaVerifier }
  } catch (error) {
    recaptchaVerifier.clear()
    throw error
  }
}

export async function finishMfaEnrollment(user, verificationId, code) {
  const credential = PhoneAuthProvider.credential(verificationId, code)
  const assertion = PhoneMultiFactorGenerator.assertion(credential)
  return multiFactor(user).enroll(assertion, 'SMS principal')
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
