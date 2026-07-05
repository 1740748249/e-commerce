import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

let stompClient = null
let shopSubscription = null
let broadcastSubscription = null
let reconnectTimer = null
let connected = false

/**
 * 建立 STOMP-over-WebSocket 连接，订阅商家通知频道 + 全站广播频道。
 * 连接失败会自动重试（5 秒间隔），token 或 shopId 变化时先断开再重连。
 *
 * @param {string} token      JWT
 * @param {number} shopId     商家店铺 ID
 * @param {function} onMessage  店铺通知回调
 * @param {function} onBroadcast  全站广播回调
 */
export function connectWebSocket(token, shopId, onMessage, onBroadcast) {
  if (!token || !shopId) {
    disconnectWebSocket()
    return
  }

  if (connected && stompClient?.active) return

  disconnectWebSocket()

  const client = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    connectHeaders: { Authorization: `Bearer ${token}` },
    debug: () => {},
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
  })

  client.onConnect = () => {
    connected = true
    clearTimeout(reconnectTimer)

    shopSubscription = client.subscribe(`/topic/shop/${shopId}`, (msg) => {
      try {
        const body = JSON.parse(msg.body)
        if (onMessage) onMessage(body)
      } catch { /* ignore */ }
    })

    broadcastSubscription = client.subscribe('/topic/broadcast', (msg) => {
      try {
        const body = JSON.parse(msg.body)
        if (onBroadcast) onBroadcast(body)
      } catch { /* ignore */ }
    })
  }

  client.onStompError = () => {
    connected = false
    scheduleReconnect(token, shopId, onMessage, onBroadcast)
  }

  client.onWebSocketClose = () => {
    connected = false
  }

  client.activate()
  stompClient = client
}

function scheduleReconnect(token, shopId, onMessage, onBroadcast) {
  clearTimeout(reconnectTimer)
  reconnectTimer = setTimeout(() => {
    if (token && shopId) connectWebSocket(token, shopId, onMessage, onBroadcast)
  }, 5000)
}

export function disconnectWebSocket() {
  clearTimeout(reconnectTimer)
  if (shopSubscription) {
    try { shopSubscription.unsubscribe() } catch { /* */ }
    shopSubscription = null
  }
  if (broadcastSubscription) {
    try { broadcastSubscription.unsubscribe() } catch { /* */ }
    broadcastSubscription = null
  }
  if (stompClient) {
    try { stompClient.deactivate() } catch { /* */ }
    stompClient = null
  }
  connected = false
}
