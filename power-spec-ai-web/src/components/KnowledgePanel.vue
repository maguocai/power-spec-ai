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
              :disabled="uploading"
              :on-change="handleFileChange"
              :on-exceed="handleExceed"
          >
            <el-button :disabled="uploading">选择文档</el-button>
            <template #tip>
              <div class="upload-tip">支持 PDF、DOCX 格式，单文件建议不超过 50MB；扫描件需 OCR 识别，耗时较长</div>
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
            {{ uploading ? '正在处理文档…' : '上传并加入知识库' }}
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

    <!-- 已上传文件列表 -->
    <div class="list-card">
      <div class="list-title">本次已上传文档</div>
      <el-empty v-if="fileList.length === 0" description="暂无上传记录" :image-size="80" />
      <el-table v-else :data="fileList" stripe>
        <el-table-column prop="fileName" label="文件名" />
        <el-table-column prop="specType" label="自动分类" width="160" />
        <el-table-column prop="uploadTime" label="上传时间" width="180" />
      </el-table>
    </div>

    <!-- 全屏处理中遮罩：OCR / 向量化期间持续等待，不提前断开 -->
    <div v-if="uploading" class="upload-overlay">
      <div class="loading-panel">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <div class="loading-title">文档处理中，请耐心等待</div>
        <div class="loading-stage">{{ stageText }}</div>
        <div class="loading-timer">已等待 {{ elapsedSeconds }} 秒</div>
        <div class="loading-warning" v-if="elapsedSeconds >= 25">
          扫描件 OCR 识别较慢，请勿关闭或刷新页面
        </div>
        <el-button link type="primary" class="cancel-btn" @click="handleCancel">
          取消本次上传
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { uploadDocToKnowledge } from '../api/knowledge'
import { ElMessage } from 'element-plus'

const uploadRef = ref()
const uploading = ref(false)
const currentFile = ref(null)
const fileList = ref([])

// 处理计时与阶段文案
const elapsedSeconds = ref(0)
let timer = null
// 请求取消控制器
let abortController = null

const stageText = computed(() => {
  if (elapsedSeconds.value < 8) {
    return '正在上传文档…'
  }
  if (elapsedSeconds.value < 25) {
    return '正在解析文档、提取规范文本…'
  }
  return '正在进行 OCR 识别 / 切片向量化…'
})

const startTimer = () => {
  stopTimer()
  elapsedSeconds.value = 0
  timer = setInterval(() => {
    elapsedSeconds.value += 1
  }, 1000)
}

const stopTimer = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

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
  abortController = new AbortController()
  startTimer()

  try {
    const res = await uploadDocToKnowledge(currentFile.value, abortController.signal)
    ElMessage.success(res || '上传成功，文档已自动分类并加入知识库')

    // 加入前端临时列表
    fileList.value.unshift({
      fileName: currentFile.value.name,
      specType: '系统自动分类',
      uploadTime: new Date().toLocaleString()
    })

    // 清空选择
    uploadRef.value?.clearFiles()
    currentFile.value = null
  } catch (err) {
    // 用户主动取消，不弹错误提示
    if (err?.code === 'ERR_CANCELED') {
      ElMessage.info('已取消本次上传')
      return
    }
    // 前端超时（兜底，接口已放宽到 5 分钟）
    if (err?.code === 'ECONNABORTED') {
      ElMessage.error('处理时间过长已中断，文档可能仍在后台解析，可稍后刷新查看知识库')
      return
    }
    // 后端业务错误：优先取后端返回的 message
    const message = err?.response?.data?.message || err?.response?.data || '上传失败，请检查后端服务'
    ElMessage.error(typeof message === 'string' ? message : '上传失败，请检查后端服务')
  } finally {
    stopTimer()
    uploading.value = false
    abortController = null
  }
}

const handleCancel = () => {
  if (abortController) {
    abortController.abort()
  }
}

onUnmounted(() => {
  stopTimer()
})
</script>

<style scoped>
.knowledge-container {
  height: 100%;
  padding: 24px;
  overflow-y: auto;
  background: #fff;
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

/* 遮罩 loading 面板：fixed 全屏覆盖，v-if 控制显隐 */
.upload-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(2px);
}

.loading-panel {
  width: 360px;
  background: #fff;
  border-radius: 16px;
  padding: 32px 28px;
  text-align: center;
  box-shadow: 0 8px 32px rgba(31, 81, 56, 0.18);
}

.loading-icon {
  font-size: 36px;
  color: var(--el-color-primary);
  animation: rotating 1.4s linear infinite;
}

.loading-title {
  margin-top: 16px;
  font-size: 16px;
  font-weight: 600;
  color: #1f5138;
}

.loading-stage {
  margin-top: 8px;
  font-size: 14px;
  color: #46574f;
}

.loading-timer {
  margin-top: 10px;
  font-size: 13px;
  color: #909399;
}

.loading-warning {
  margin-top: 10px;
  font-size: 12px;
  color: #e6a23c;
}

.cancel-btn {
  margin-top: 16px;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
