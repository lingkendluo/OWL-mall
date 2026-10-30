<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ApiError } from '@shared/http'
import SubPage from '@/components/SubPage.vue'
import AppToast from '@/components/AppToast.vue'
import { useToast } from '@/composables/useToast'
import { ADDRESS_TAGS, useAddressStore, type Address, type AddressInput } from '@/stores/address'
import './address.css'

/**
 * 地址管理：多地址 + 单一默认。
 * 默认地址在头部配送条、结算页、个人中心都会读到，改这里三处一起变。
 */
const router = useRouter()
const store = useAddressStore()
const { show } = useToast()

const ICONS = {
  pin: '<path d="M12 21c4.2-4 6.4-7 6.4-9.8A6.4 6.4 0 0 0 5.6 11.2C5.6 14 7.8 17 12 21Z"/><path d="M12 13.4a2.2 2.2 0 1 0 0-4.4 2.2 2.2 0 0 0 0 4.4Z"/>',
  check: '<path d="M5 12.6 10 17.6 19 8.2"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  edit: '<path d="M4.6 19.4h4l10-10a2.1 2.1 0 0 0-3-3l-10 10v3Z"/>',
  trash: '<path d="M5 7.2h14M9.4 7.2V5.4h5.2v1.8M7.2 7.2l.8 11.4h8l.8-11.4"/>',
}

const subtitle = computed(() =>
  store.count ? `共 ${store.count} 个地址 · 默认：${store.defaultAddress?.tag ?? '—'}` : '还没有收货地址',
)

/* ------------------------------ 抽屉表单 ------------------------------ */

const drawer = reactive({
  open: false,
  mode: 'create' as 'create' | 'edit',
  id: '',
  error: '',
  saving: false,
})

const blank = {
  name: '',
  phone: '',
  region: '',
  detail: '',
  tag: '家',
  isDefault: false,
}

const form = reactive({ ...blank })

/**
 * 自定义标签单独放一个值：预设标签（家/公司/学校）不往输入框里回填，
 * 否则按钮选中的词会在下面再显示一遍，看着像填了两个标签。
 */
const customTag = ref('')

function openCreate() {
  Object.assign(form, blank)
  customTag.value = ''
  // 第一个地址一定是默认
  form.isDefault = store.count === 0
  drawer.mode = 'create'
  drawer.id = ''
  drawer.error = ''
  drawer.open = true
}

function openEdit(row: Address) {
  Object.assign(form, {
    name: row.name,
    phone: row.phone,
    region: row.region,
    detail: row.detail,
    tag: row.tag,
    isDefault: row.isDefault,
  })
  customTag.value = (ADDRESS_TAGS as readonly string[]).includes(row.tag) ? '' : row.tag
  drawer.mode = 'edit'
  drawer.id = row.id
  drawer.error = ''
  drawer.open = true
}

function validate(): string {
  const name = form.name.trim()
  if (name.length < 2) return '收货人至少 2 个字'
  if (!/^1[3-9]\d{9}$/.test(form.phone.trim())) return '手机号格式不对，11 位数字'
  const region = form.region.trim()
  if (!region) return '请填写所在地区'
  // 后端 province / city / district 三个字段都必填，所以这里必须写到区
  if (region.split(/\s+/).length < 3) return '所在地区写到区，例如「浙江省 杭州市 西湖区」'
  if (form.detail.trim().length < 4) return '详细地址至少 4 个字'
  return ''
}

async function submit() {
  if (drawer.saving) return
  drawer.error = validate()
  if (drawer.error) return
  const payload: AddressInput = {
    name: form.name.trim(),
    phone: form.phone.trim(),
    region: form.region.trim().replace(/\s+/g, ' '),
    detail: form.detail.trim(),
    tag: form.tag,
    isDefault: form.isDefault,
  }
  drawer.saving = true
  try {
    if (drawer.mode === 'create') {
      const added = await store.add(payload)
      show(added.isDefault ? '已新增，并设为默认地址' : '已新增地址')
    } else {
      await store.update(drawer.id, payload)
      show('已保存')
    }
    drawer.open = false
  } catch (e) {
    drawer.error = e instanceof ApiError ? e.message : '保存失败，请稍后重试'
  } finally {
    drawer.saving = false
  }
}

/* ------------------------------ 行内操作 ------------------------------ */

async function makeDefault(row: Address) {
  if (row.isDefault) return
  try {
    await store.setDefault(row.id)
    show(`已把「${row.tag}」设为默认地址`)
  } catch (e) {
    show(e instanceof ApiError ? e.message : '设置失败，请稍后重试')
  }
}

async function remove(row: Address) {
  if (!window.confirm(`删除「${row.name} ${row.region}」这个地址？`)) return
  const wasDefault = row.isDefault
  try {
    await store.remove(row.id)
    show(wasDefault && store.defaultAddress ? '已删除，默认地址已顺延' : '已删除')
  } catch (e) {
    show(e instanceof ApiError ? e.message : '删除失败，请稍后重试')
  }
}

function pickTag(tag: string) {
  customTag.value = ''
  form.tag = tag
}

/** 输入自定义标签；清空就退回「家」，不留空标签 */
function onCustomTag(event: Event) {
  const value = (event.target as HTMLInputElement).value.trim()
  customTag.value = value
  form.tag = value || ADDRESS_TAGS[0]
}

function goBack() {
  void router.push({ name: 'home' })
}
</script>

<template>
  <SubPage title="地址管理" :subtitle="subtitle">
    <div class="addr-page">
      <header class="addr-head">
        <div>
          <h2>收货地址</h2>
          <p>可以存多个，下单时默认用带「默认」标记的那个</p>
        </div>
        <button class="addr-new" type="button" @click="openCreate">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.9"
            stroke-linecap="round"
            v-html="ICONS.plus"
          />
          新增地址
        </button>
      </header>

      <div v-if="store.loading && store.count === 0" class="addr-empty">
        <b>正在读取地址…</b>
        <p>从服务器拉取你的收货地址</p>
      </div>

      <div v-else-if="store.count === 0" class="addr-empty">
        <span class="addr-empty-ico">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
            v-html="ICONS.pin"
          />
        </span>
        <b>还没有收货地址</b>
        <p>加一个地址，下单时就不用每次重填了</p>
        <button class="addr-new" type="button" @click="openCreate">新增地址</button>
      </div>

      <div v-else class="addr-grid">
        <article
          v-for="row in store.list"
          :key="row.id"
          class="addr-card"
          :class="{ 'is-default': row.isDefault }"
        >
          <div class="addr-card-top">
            <span class="addr-tag">{{ row.tag }}</span>
            <b class="addr-name">{{ row.name }}</b>
            <span class="addr-phone">{{ row.phone.replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2') }}</span>
            <span v-if="row.isDefault" class="addr-badge">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.2"
                stroke-linecap="round"
                stroke-linejoin="round"
                v-html="ICONS.check"
              />
              默认
            </span>
          </div>

          <p class="addr-text">
            <span>{{ row.region }}</span>
            {{ row.detail }}
          </p>

          <div class="addr-card-foot">
            <button
              v-if="!row.isDefault"
              class="addr-link"
              type="button"
              @click="makeDefault(row)"
            >
              设为默认
            </button>
            <span v-else class="addr-link is-static">当前默认</span>

            <span class="addr-gap" />

            <button class="addr-link" type="button" @click="openEdit(row)">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.7"
                stroke-linecap="round"
                stroke-linejoin="round"
                v-html="ICONS.edit"
              />
              编辑
            </button>
            <button class="addr-link is-danger" type="button" @click="remove(row)">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.7"
                stroke-linecap="round"
                stroke-linejoin="round"
                v-html="ICONS.trash"
              />
              删除
            </button>
          </div>
        </article>
      </div>

      <div class="addr-tip">
        <b>关于默认地址</b>
        同一时间只有一个默认地址。删掉默认地址时，列表里的第一条会自动顶上，
        不会出现「一个默认都没有」的情况。
        <button type="button" class="addr-back" @click="goBack">回首页继续逛</button>
      </div>
    </div>

    <template #footer>
      <div class="addr-foot">
        <button class="addr-new" type="button" @click="openCreate">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.9"
            stroke-linecap="round"
            v-html="ICONS.plus"
          />
          新增地址
        </button>
        <button class="addr-ghost" type="button" @click="goBack">返回商城</button>
      </div>
    </template>
  </SubPage>

  <!-- 新增 / 编辑 -->
  <template v-if="drawer.open">
    <div class="addr-mask" @click="drawer.open = false" />
    <aside class="addr-drawer">
      <header class="addr-drawer-head">
        <h2>{{ drawer.mode === 'create' ? '新增收货地址' : '编辑地址' }}</h2>
        <button class="addr-ghost sm" type="button" @click="drawer.open = false">关闭</button>
      </header>

      <div class="addr-drawer-body">
        <p v-if="drawer.error" class="addr-alert">{{ drawer.error }}</p>

        <label class="addr-field">
          <span>收货人</span>
          <input v-model="form.name" type="text" maxlength="20" placeholder="真实姓名，方便快递员核对" />
        </label>

        <label class="addr-field">
          <span>手机号</span>
          <input v-model="form.phone" type="tel" inputmode="numeric" maxlength="11" placeholder="11 位手机号" />
        </label>

        <label class="addr-field">
          <span>所在地区</span>
          <input v-model="form.region" type="text" placeholder="浙江省 杭州市 西湖区" />
          <em>省 / 市 / 区 用空格隔开</em>
        </label>

        <label class="addr-field">
          <span>详细地址</span>
          <textarea
            v-model="form.detail"
            rows="3"
            placeholder="街道、门牌号、楼栋单元室"
          />
        </label>

        <div class="addr-field">
          <span>标签</span>
          <div class="addr-tags">
            <button
              v-for="tag in ADDRESS_TAGS"
              :key="tag"
              type="button"
              class="addr-tag-btn"
              :class="{ 'is-on': form.tag === tag }"
              @click="pickTag(tag)"
            >
              {{ tag }}
            </button>
            <input
              :value="customTag"
              class="addr-tag-input"
              type="text"
              maxlength="6"
              placeholder="其它"
              @input="onCustomTag"
            />
          </div>
        </div>

        <label class="addr-check">
          <input v-model="form.isDefault" type="checkbox" />
          设为默认地址
        </label>
      </div>

      <footer class="addr-drawer-foot">
        <button class="addr-ghost" type="button" @click="drawer.open = false">取消</button>
        <button class="addr-new" type="button" @click="submit">
          {{ drawer.mode === 'create' ? '保存地址' : '保存修改' }}
        </button>
      </footer>
    </aside>
  </template>

  <AppToast />
</template>
