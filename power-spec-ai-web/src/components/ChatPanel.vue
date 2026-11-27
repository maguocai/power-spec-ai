<template>
  <div class="chat-container">
    <!-- 消息列表 -->
    <div class="message-list" ref="messageListRef">
      <!-- 空状态引导 -->
      <div v-if="messageList.length === 0 && !loading" class="empty-state">
        <div class="empty-card">
          <div class="empty-icon">
            <el-icon :size="32"><Cpu /></el-icon>
          </div>
          <h3>您好，我是电力规范问答助手</h3>
          <p>请在下方输入问题，系统将自动匹配规范类目并精准解答</p>
          <div class="suggest-list">
            <div
                v-for="(q, i) in suggestions"
                :key="i"
                class="suggest-item"
                @click="fillQuestion(q)"
            >
              {{ q }}
            </div>
          </div>
        </div>
      </div>

      <div
          v-for="(item, index) in messageList"
          :key="index"
          :class="['message-item', item.role === 'user' ? 'is-user' : 'is-ai']"
      >
        <div class="avatar">
          <el-icon v-if="item.role === 'user'"><User /></el-icon>
          <el-icon v-else><Cpu /></el-icon>
        </div>
        <div class="message-content">
          <div class="message-bubble">
            <pre v-if="item.role === 'ai'" class="answer-text">{{ item.content }}</pre>
            <span v-else>{{ item.content }}</span>
          </div>
          <div class="message-meta" v-if="item.role === 'ai' && item.meta">
            <span>匹配分类：{{ item.meta.specCategory }}</span>
            <span>耗时：{{ item.meta.costTime }}ms</span>
          </div>
        </div>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="message-item is-ai">
        <div class="avatar"><el-icon><Cpu /></el-icon></div>
        <div class="message-content">
          <div class="message-bubble">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span style="margin-left: 8px;">正在匹配规范知识库...</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部输入区 -->
    <div class="input-area">
      <el-input
          v-model="inputText"
          type="textarea"
          :rows="2"
          placeholder="请输入您的电力技术规范问题，例如：储能电站一次调频有哪些要求？"
          @keydown.enter.ctrl="handleSend"
          resize="none"
      />
      <div class="input-footer">
        <span class="tip">Ctrl + Enter 快速发送</span>
        <el-button type="primary" :loading="loading" @click="handleSend">
          发送提问
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { User, Cpu, Loading } from '@element-plus/icons-vue'
import { askSpecQuestion } from '../api/qa'
import { ElMessage } from 'element-plus'
// 引入全局对话状态
import { chatStore } from '../stores/chatStore'

const inputText = ref('')
const loading = ref(false)
const messageListRef = ref(null)
// 直接使用全局状态，组件销毁数据不丢失
const messageList = chatStore.messageList

const suggestions = [
  '储能电站一次调频性能指标有哪些要求？',
  '并网光伏电站技术监督包含哪些内容？',
  '电力安全工器具定期检查周期是怎么规定的？'
]

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

// 填充推荐问题
const fillQuestion = (q) => {
  inputText.value = q
}

// 发送问题
const handleSend = async () => {
  const question = inputText.value.trim()
  if (!question) {
    ElMessage.warning('请输入问题内容')
    return
  }
  if (loading.value) return

  messageList.push({ role: 'user', content: question })
  inputText.value = ''
  scrollToBottom()

  loading.value = true
  try {
    const res = await askSpecQuestion({ question })
    messageList.push({
      role: 'ai',
      content: res.answer,
      meta: { specCategory: res.specCategory, costTime: res.costTime }
    })
  } catch (err) {
    ElMessage.error('请求失败，请检查后端服务是否启动')
    messageList.push({
      role: 'ai',
      content: '抱歉，服务暂时不可用，请稍后重试。'
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}
</script>

<style scoped>
.chat-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.message-list {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

/* 空状态卡片 */
.empty-state {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-card {
  text-align: center;
  max-width: 460px;
  padding: 30px 24px;
}

.empty-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 18px;
  border-radius: 16px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-card h3 {
  font-size: 17px;
  color: #1f5138;
  margin-bottom: 8px;
}

.empty-card p {
  font-size: 13px;
  color: #82928a;
  margin-bottom: 22px;
}

.suggest-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.suggest-item {
  padding: 12px 16px;
  background: #f6faf7;
  border: 1px solid #e4ede8;
  border-radius: 10px;
  font-size: 14px;
  color: #46574f;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s;
}

.suggest-item:hover {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.message-item {
  display: flex;
  margin-bottom: 22px;
  gap: 12px;
}

.message-item.is-user {
  flex-direction: row-reverse;
}

.avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  flex-shrink: 0;
}

.is-user .avatar {
  background: var(--el-color-primary);
}

.is-ai .avatar {
  background: #92d4b4;
}

.message-content {
  max-width: 75%;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.7;
  word-break: break-word;
}

.is-user .message-bubble {
  background: var(--el-color-primary);
  color: #fff;
  border-top-right-radius: 4px;
}

.is-ai .message-bubble {
  background: #f6faf7;
  border: 1px solid #e4ede8;
  border-top-left-radius: 4px;
  color: #303133;
}

.answer-text {
  white-space: pre-wrap;
  font-family: "Microsoft YaHei", sans-serif;
  margin: 0;
  font-size: 14px;
}

.message-meta {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  display: flex;
  gap: 16px;
}

.input-area {
  padding: 16px 24px 20px;
  border-top: 1px solid #ebeef5;
  background: #fff;
}

.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.tip {
  font-size: 12px;
  color: #909399;
}
</style>
