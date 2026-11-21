<template>
  <div class="layout-container">
    <!-- 顶部标题栏 -->
    <header class="header">
      <div class="header-title">
        <span class="logo-icon">
          <el-icon :size="20"><Lightning /></el-icon>
        </span>
        <span>电力技术规范智能问答助手</span>
      </div>
    </header>

    <div class="main-content">
      <!-- 左侧功能导航 -->
      <aside class="sidebar">
        <div class="menu-group">
          <div class="menu-title">功能导航</div>
          <el-menu
              :default-active="activeTab"
              class="spec-menu"
              @select="handleTabSelect"
          >
            <el-menu-item index="chat">
              <el-icon><ChatDotRound /></el-icon>
              <span>智能问答</span>
            </el-menu-item>
            <el-menu-item index="knowledge">
              <el-icon><FolderOpened /></el-icon>
              <span>知识库管理</span>
            </el-menu-item>
          </el-menu>
        </div>
      </aside>

      <!-- 右侧内容区 -->
      <main class="content">
        <div class="content-inner">
          <ChatPanel v-if="activeTab === 'chat'" />
          <KnowledgePanel v-else />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import ChatPanel from './components/ChatPanel.vue'
import KnowledgePanel from './components/KnowledgePanel.vue'
import { Lightning, ChatDotRound, FolderOpened } from '@element-plus/icons-vue'

const activeTab = ref('chat')

const handleTabSelect = (key) => {
  activeTab.value = key
}
</script>

<style scoped>
.layout-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.header {
  height: 60px;
  background: #ffffff;
  border-bottom: 1px solid #e4ede8;
  display: flex;
  align-items: center;
  padding: 0 28px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 17px;
  font-weight: 600;
  color: #1f5138;
}

.logo-icon {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.sidebar {
  width: 220px;
  background: #ffffff;
  border-right: 1px solid #e4ede8;
  flex-shrink: 0;
  padding: 8px 0;
}

.menu-group {
  margin-bottom: 12px;
}

.menu-title {
  color: #7a8b82;
  padding: 12px 20px 8px;
  font-size: 13px;
}

.spec-menu {
  border-right: none;
}

.spec-menu .el-menu-item {
  height: 46px;
  line-height: 46px;
  border-radius: 8px;
  margin: 2px 12px;
  color: #46574f;
}

.spec-menu .el-menu-item.is-active {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-weight: 500;
}

.content {
  flex: 1;
  overflow: hidden;
  padding: 20px;
}

.content-inner {
  max-width: 960px;
  margin: 0 auto;
  height: 100%;
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #e4ede8;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(31, 81, 56, 0.06);
}
</style>
