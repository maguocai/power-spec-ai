<template>
  <div class="knowledge-container">
    <div class="page-header">
      <h3>知识库管理</h3>
      <p class="desc">上传电力技术规范文档，系统将自动分类、切片向量化，用于问答精准检索</p>
    </div>

    <!-- 上传卡片 -->
    <div class="upload-card">
      <el-form label-width="100px">
        <el-form-item label="选择文件">
          <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :limit="1"
              accept=".pdf,.docx"
              :on-change="handleFileChange"
              :on-exceed="handleExceed"
              :disabled="uploading"
          >
            <el-button :disabled="uploading">选择文档</el-button>
            <template #tip>
              <div class="upload-tip">支持 PDF、DOCX 格式，单文件建议不超过 10MB；扫描件需OCR识别，耗时较长</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item>
          <el-button
              type="primary"
              :loading="uploading"
              :disabled="!currentFile"
              @click="handleUpload"
          >
            上传并加入知识库
          </el-button>
          <span class="auto-tip">上传后系统将自动识别规范分类</span>
        </el-form-item>
      </el-form>

      <el-alert
          title="提示：当前为内存版知识库，应用重启后上传的文档会丢失"
          type="info"
          :closable="false"
          show-icon
      />
    </div>

    <!-- 上传历史记录 -->
    <div class="list-card">
      <div class="list-title">上传历史记录</div>
      <el-empty v-if="fileList.length === 0" description="暂无上传历史记录" :image-size="80" />
      <el-table v-else :data="fileList" stripe>
        <el-table-column prop="fileName" label="文件名" />
        <el-table-column prop="specType" label="自动分类" width="160" />
        <el-table-column prop="uploadTime" label="上传时间" width="180" />
      </el-table>
    </div>

    <!-- 上传中遮罩 -->
    <div v-if="uploading" class="upload-loading-mask">
      <div class="loading-panel">
        <el-icon class="loading-icon" :size="32"><Loading /></el-icon>
        <div class="loading-title">文档处理中，请耐心等待</div>
        <div class="loading-desc">{{ loadingText }}</div>
        <div class="loading-time">已等待 {{ waitSeconds }} 秒</div>
        <el-button type="primary" text @click="handleCancel">取消本次上传</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { uploadDocToKnowledge } from '../api/knowledge'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'

const uploadRef = ref()
const uploading = ref(false)
const currentFile = ref(null)
const fileList = ref([])
const waitSeconds = ref(0)
const loadingText = ref('正在上传文档...')
let timer = null
let abortController = null

// 初始化：从本地缓存读取上传历史
onMounted(() => {
  const saved = localStorage.getItem('power-spec-upload-history')
  if (saved) {
    fileList.value = JSON.parse(saved)
  }
})

// 组件销毁清理定时器
onUnmounted(() => {
  if (timer) clearInterval(timer)
})

const handleFileChange = (file) => {
  currentFile.value = file.raw
}

const handleExceed = () => {
  ElMessage.warning('只能上传一个文件，请先移除再选择')
}

const handleUpload = async () => {
  if (!currentFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  uploading.value = true
  waitSeconds.value = 0
  loadingText.value = '正在上传文档...'
  abortController = new AbortController()

  // 启动计时
  timer = setInterval(() => {
    waitSeconds.value++
    // 分阶段更新文案
    if (waitSeconds.value > 25) {
      loadingText.value = '正在进行OCR识别 / 切片向量化，请勿关闭或刷新页面'
    } else if (waitSeconds.value > 8) {
      loadingText.value = '正在解析文档、提取规范文本...'
    }
  }, 1000)

  try {
    await uploadDocToKnowledge(currentFile.value, abortController.signal)
    ElMessage.success('上传成功，文档已自动分类并加入知识库')

    // 加入历史记录，并存入本地缓存
    const newRecord = {
      fileName: currentFile.value.name,
      specType: '系统自动分类',
      uploadTime: new Date().toLocaleString()
    }
    fileList.value.unshift(newRecord)
    localStorage.setItem('power-spec-upload-history', JSON.stringify(fileList.value))

    // 清空选择
    uploadRef.value?.clearFiles()
    currentFile.value = null
  } catch (err) {
    if (err.name === 'CanceledError') {
      ElMessage.info('已取消上传')
    } else {
      const msg = err.response?.data?.message || err.message || '上传失败，请检查后端服务'
      ElMessage.error(msg)
    }
  } finally {
    uploading.value = false
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }
}

const handleCancel = () => {
  if (abortController) {
    abortController.abort()
  }
}
</script>

<style scoped>
.knowledge-container {
  height: 100%;
  padding: 24px;
  overflow-y: auto;
  background: #fff;
  position: relative;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h3 {
  font-size: 18px;
  color: #1f5138;
  margin-bottom: 4px;
}

.page-header .desc {
  font-size: 13px;
  color: #82928a;
}

.upload-card,
.list-card {
  background: #f8faf9;
  border: 1px solid #e4ede8;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 20px;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}

.auto-tip {
  margin-left: 12px;
  font-size: 12px;
  color: #82928a;
}

.list-title {
  font-size: 15px;
  font-weight: 500;
  color: #1f5138;
  margin-bottom: 14px;
}

/* 上传遮罩 */
.upload-loading-mask {
  position: fixed;
  inset: 0;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(2px);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-panel {
  background: #fff;
  padding: 32px 48px;
  border-radius: 14px;
  box-shadow: 0 8px 32px rgba(31, 81, 56, 0.12);
  text-align: center;
  min-width: 320px;
}

.loading-icon {
  color: var(--el-color-primary);
  margin-bottom: 16px;
  animation: rotate 1.2s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.loading-title {
  font-size: 16px;
  font-weight: 500;
  color: #1f5138;
  margin-bottom: 8px;
}

.loading-desc {
  font-size: 13px;
  color: #607269;
  margin-bottom: 12px;
}

.loading-time {
  font-size: 12px;
  color: #909399;
  margin-bottom: 16px;
}
</style>
