import { ref, onUnmounted } from 'vue'
import { Client, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

export function useWebSocket() {
  const connected = ref(false)
  let client: Client | null = null
  let onMessageCallback: ((msg: IMessage) => void) | null = null

  function onMessage(cb: (msg: IMessage) => void) {
    onMessageCallback = cb
  }

  function connect(token: string) {
    if (client?.active) return

    const wsUrl = `${window.location.origin}/api/ws`
    client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      connectHeaders: {
        token: token,
      },
      onConnect: () => {
        connected.value = true
        client?.subscribe('/user/queue/messages', (msg: IMessage) => {
          onMessageCallback?.(msg)
        })
      },
      onDisconnect: () => {
        connected.value = false
      },
      onStompError: () => {
        connected.value = false
      },
    })

    client.activate()
  }

  function disconnect() {
    client?.deactivate()
    client = null
    connected.value = false
  }

  onUnmounted(() => {
    disconnect()
  })

  return { connected, connect, disconnect, onMessage }
}