<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <main class="placeholder">
    <h1>OWL 商城 · 首页</h1>
    <p>这里是 D1 要落地的首页（搜索框 / 轮播 / 金刚区 / 楼层）。</p>
    <p class="hint">
      当前已完成：<strong>登录页</strong>（Vue 3 单文件组件 + 独立动画引擎）。<br />
      进入方式：<RouterLink to="/login">/login</RouterLink>
    </p>
  </main>
</template>

<style scoped>
.placeholder {
  display: grid;
  place-content: center;
  gap: 12px;
  height: 100%;
  padding: 40px;
  text-align: center;
  color: #1f2027;
}

h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 500;
}

p {
  margin: 0;
  font-size: 14px;
  color: #4d4f5a;
}

.hint {
  margin-top: 8px;
  line-height: 1.9;
  color: #8b8e99;
}

a {
  color: #1d4ed8;
  text-decoration: underline;
}
</style>
