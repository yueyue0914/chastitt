import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { signIn, signUp, getProfile, updateProfile } from '../api/lock'

const TOKEN_KEY = 'yue-token'
const USER_KEY = 'yue-user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref(JSON.parse(localStorage.getItem(USER_KEY) || 'null'))
  const profile = ref(null)

  const isLoggedIn = computed(() => Boolean(token.value))

  function persist(auth) {
    token.value = auth.token
    user.value = auth.user
    localStorage.setItem(TOKEN_KEY, auth.token)
    localStorage.setItem(USER_KEY, JSON.stringify(auth.user))
  }

  async function register(payload) {
    persist(await signUp(payload))
  }

  async function login(payload) {
    persist(await signIn(payload))
  }

  function logout() {
    token.value = ''
    user.value = null
    profile.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  async function loadProfile() {
    profile.value = await getProfile()
    return profile.value
  }

  async function saveProfile(data) {
    profile.value = await updateProfile(data)
    return profile.value
  }

  return {
    token,
    user,
    profile,
    isLoggedIn,
    register,
    login,
    logout,
    loadProfile,
    saveProfile,
  }
})
