declare global {
  interface Window {
    unityInstance?: {
      SendMessage?: (objectName: string, methodName: string, value?: string) => void
    }
    gameInstance?: {
      SendMessage?: (objectName: string, methodName: string, value?: string) => void
    }
    sendUnityAuthToken?: (token: string) => void
  }
}

const UNITY_AUTH_EVENT = 'eduquest:unity-auth-token'
const UNITY_RECEIVER_NAME = 'WebAuthBridge'
const UNITY_RECEIVER_METHOD = 'ReceiveAccessToken'

const dispatchUnityAuthEvent = (token: string) => {
  if (typeof window === 'undefined') {
    return
  }

  window.dispatchEvent(
    new CustomEvent(UNITY_AUTH_EVENT, {
      detail: { token },
    })
  )
}

const sendViaUnityInstance = (token: string) => {
  if (typeof window === 'undefined') {
    return false
  }

  const candidates = [window.unityInstance, window.gameInstance]
  for (const instance of candidates) {
    if (instance?.SendMessage) {
      instance.SendMessage(UNITY_RECEIVER_NAME, UNITY_RECEIVER_METHOD, token)
      return true
    }
  }

  return false
}

const sendViaCustomBridge = (token: string) => {
  if (typeof window === 'undefined' || typeof window.sendUnityAuthToken !== 'function') {
    return false
  }

  window.sendUnityAuthToken(token)
  return true
}

export const syncUnityAccessToken = (token: string | null) => {
  const safeToken = token ?? ''

  dispatchUnityAuthEvent(safeToken)
  sendViaCustomBridge(safeToken)
  sendViaUnityInstance(safeToken)
}

export { UNITY_AUTH_EVENT, UNITY_RECEIVER_METHOD, UNITY_RECEIVER_NAME }
