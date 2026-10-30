<script setup lang="ts">
import { useToast } from '@/composables/useToast'
import './app-toast.css'

const { text } = useToast()
</script>

<template>
  <div class="app-toast" :class="{ show: !!text }">
    <i />
    {{ text }}
  </div>
</template>
