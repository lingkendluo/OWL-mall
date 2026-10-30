<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ApiError } from '@shared/http'
import { useAdminSession } from '@/stores/adminSession'
import './admin.css'

const router = useRouter()
const session = useAdminSession()

const username = ref('admin')
const password = ref('')
const pending = ref(false)
const error = ref('')

onMounted(() => {
  // 已经登录过就别停在登录页
  if (session.isLoggedIn) void router.replace({ name: 'admin-dashboard' })
})

async function submit() {
  if (pending.value) return
  error.value = ''
  if (!username.value.trim()) {
    error.value = '请输入管理员账号'
    return
  }
  if (!password.value) {
    error.value = '请输入密码'
    return
  }
  pending.value = true
  try {
    await session.login(username.value, password.value)
    await router.replace({ name: 'admin-dashboard' })
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '登录失败，请稍后重试'
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <div class="adm-login">
    <aside class="adm-login-aside">
      <div>
        <div class="adm-brand" style="border: 0; padding: 0 0 26px">
          <div class="adm-brand-mark">OWL</div>
          <div class="adm-brand-text">
            <b>OWL 管理后台</b>
            <span>Merchant Console</span>
          </div>
        </div>
        <h1>上架、定价、发公告，<br />一个后台管完整个商城。</h1>
        <p>
          商品的增删改查、上下架、库存调整，公告的起草与发布都走真实的 Spring Boot 接口。
          所有写操作都会落一条操作日志，出问题能查到是谁改的。
        </p>
        <div class="adm-login-list">
          <span>商品上架</span>
          <span>价格与库存</span>
          <span>公告发布</span>
          <span>经营概览</span>
          <span>操作审计</span>
        </div>
      </div>
      <div class="adm-login-foot">OWL-System · Vue 3 + Spring Boot 3 · 管理端 v1</div>
    </aside>

    <main class="adm-login-main">
      <form class="adm-login-card" @submit.prevent="submit">
        <h2>管理员登录</h2>
        <p>请使用后台账号登录，与商城的会员账号是两套体系。</p>

        <div class="adm-field">
          <label for="adm-user">管理员账号</label>
          <input
            id="adm-user"
            v-model="username"
            class="adm-input"
            autocomplete="username"
            placeholder="admin"
          />
        </div>

        <div class="adm-field">
          <label for="adm-pass">密码</label>
          <input
            id="adm-pass"
            v-model="password"
            class="adm-input"
            type="password"
            autocomplete="current-password"
            placeholder="请输入密码"
          />
        </div>

        <div v-if="error" class="adm-alert">{{ error }}</div>

        <button class="adm-btn primary" type="submit" :disabled="pending">
          {{ pending ? '登录中…' : '登录后台' }}
        </button>

        <div class="adm-login-tip">
          初始账号 <b>admin</b> / <b>admin123456</b>，由后端 DataSeeder 在首次启动时写入。
          连续输错 5 次会锁定 15 分钟。
        </div>
      </form>
    </main>
  </div>
</template>
