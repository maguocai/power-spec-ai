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
          >
            <el-button>选择文档</el-button>
            <template #tip>
              <div class="upload-tip">支持 PDF、DOCX 格式，单文件建议不超过 10MB</div>
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
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { uploadDocToKnowledge } from '../api/knowledge'
import { ElMessage } from 'element-plus'

const uploadRef = ref()
const uploading = ref(false)
const currentFile = ref(null)
const fileList = ref([])

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
  try {
    await uploadDocToKnowledge(currentFile.value)
    ElMessage.success('上传成功，文档已自动分类并加入知识库')

    // 加入前端临时列表
    fileList.value.unshift({
      fileName: currentFile.value.name,
      specType: '智能识别中',
      uploadTime: new Date().toLocaleString()
    })

    // 模拟分类结果展示（实际分类在后端完成，这里做展示优化）
    setTimeout(() => {
      fileList.value[0].specType = '系统自动分类'
    }, 800)

    // 清空选择
    uploadRef.value?.clearFiles()
    currentFile.value = null
  } catch (err) {
    ElMessage.error('上传失败，请检查后端服务')
  } finally {
    uploading.value = false
  }
}
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
</style>
