import { reactive, watch } from 'vue'

const STORAGE_KEY = 'power-spec-chat-history'

// 初始化：从本地缓存读取，刷新页面也不丢
const savedHistory = localStorage.getItem(STORAGE_KEY)
const initMessageList = savedHistory ? JSON.parse(savedHistory) : []

// 全局响应式状态，组件销毁数据也在
export const chatStore = reactive({
    messageList: initMessageList
})

// 深度监听：对话变化自动存入本地缓存
watch(
    () => chatStore.messageList,
    (newList) => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(newList))
    },
    { deep: true }
)

// 清空对话方法
export function clearChatHistory() {
    chatStore.messageList = []
    localStorage.removeItem(STORAGE_KEY)
}
