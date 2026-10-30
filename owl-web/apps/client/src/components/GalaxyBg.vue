/**
 * 星空背景（Galaxy）
 * ---------------------------------------------------------------------------
 * 来自 vue-bits 的 Galaxy 组件（shadcn registry：https://vue-bits.dev/r/Galaxy.json），
 * 依赖 ogl 做的 WebGL 星场：4 层视差、指针斥力、闪烁、自动缓慢旋转。
 * 与原始版本的差别只有三处：
 *   1. 去掉 Tailwind 的 `relative w-full h-full`，改用本文件里的 `.galaxy`（绝对定位铺满父级）；
 *   2. 配色默认值调成「星空」——蓝紫为主、星星带一点点颜色；
 *   3. 支持 prefers-reduced-motion（只看静态星空，不跑动画），以及 WebGL 不可用时静默失败。
 * 用法：放在一个有确定宽高的容器里即可（参考 LoginView 的 .galaxy-bg）。
 * 注意：注释里不要写半个标签的示例代码，dev 下的 inspector 插件会把它当模板元素去编译。
 */
<script setup lang="ts">
import { Renderer, Program, Mesh, Color, Triangle } from 'ogl'
import { onBeforeUnmount, onMounted, ref, useTemplateRef, watch } from 'vue'

const vertexShader = `
attribute vec2 uv;
attribute vec2 position;

varying vec2 vUv;

void main() {
  vUv = uv;
  gl_Position = vec4(position, 0, 1);
}
`

const fragmentShader = `
precision highp float;

uniform float uTime;
uniform vec3 uResolution;
uniform vec2 uFocal;
uniform vec2 uRotation;
uniform float uStarSpeed;
uniform float uDensity;
uniform float uHueShift;
uniform float uSpeed;
uniform vec2 uMouse;
uniform float uGlowIntensity;
uniform float uSaturation;
uniform bool uMouseRepulsion;
uniform float uTwinkleIntensity;
uniform float uRotationSpeed;
uniform float uRepulsionStrength;
uniform float uMouseActiveFactor;
uniform float uAutoCenterRepulsion;
uniform bool uTransparent;

varying vec2 vUv;

#define NUM_LAYER 4.0
#define STAR_COLOR_CUTOFF 0.2
#define MAT45 mat2(0.7071, -0.7071, 0.7071, 0.7071)
#define PERIOD 3.0

float Hash21(vec2 p) {
  p = fract(p * vec2(123.34, 456.21));
  p += dot(p, p + 45.32);
  return fract(p.x * p.y);
}

float tri(float x) {
  return abs(fract(x) * 2.0 - 1.0);
}

float tris(float x) {
  float t = fract(x);
  return 1.0 - smoothstep(0.0, 1.0, abs(2.0 * t - 1.0));
}

float trisn(float x) {
  float t = fract(x);
  return 2.0 * (1.0 - smoothstep(0.0, 1.0, abs(2.0 * t - 1.0))) - 1.0;
}

vec3 hsv2rgb(vec3 c) {
  vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
  vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
  return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

float Star(vec2 uv, float flare) {
  float d = length(uv);
  float m = (0.05 * uGlowIntensity) / d;
  float rays = smoothstep(0.0, 1.0, 1.0 - abs(uv.x * uv.y * 1000.0));
  m += rays * flare * uGlowIntensity;
  uv *= MAT45;
  rays = smoothstep(0.0, 1.0, 1.0 - abs(uv.x * uv.y * 1000.0));
  m += rays * 0.3 * flare * uGlowIntensity;
  m *= smoothstep(1.0, 0.2, d);
  return m;
}

vec3 StarLayer(vec2 uv) {
  vec3 col = vec3(0.0);

  vec2 gv = fract(uv) - 0.5;
  vec2 id = floor(uv);

  for (int y = -1; y <= 1; y++) {
    for (int x = -1; x <= 1; x++) {
      vec2 offset = vec2(float(x), float(y));
      vec2 si = id + vec2(float(x), float(y));
      float seed = Hash21(si);
      float size = fract(seed * 345.32);
      float glossLocal = tri(uStarSpeed / (PERIOD * seed + 1.0));
      float flareSize = smoothstep(0.9, 1.0, size) * glossLocal;

      float red = smoothstep(STAR_COLOR_CUTOFF, 1.0, Hash21(si + 1.0)) + STAR_COLOR_CUTOFF;
      float blu = smoothstep(STAR_COLOR_CUTOFF, 1.0, Hash21(si + 3.0)) + STAR_COLOR_CUTOFF;
      float grn = min(red, blu) * seed;
      vec3 base = vec3(red, grn, blu);

      float hue = atan(base.g - base.r, base.b - base.r) / (2.0 * 3.14159) + 0.5;
      hue = fract(hue + uHueShift / 360.0);
      float sat = length(base - vec3(dot(base, vec3(0.299, 0.587, 0.114)))) * uSaturation;
      float val = max(max(base.r, base.g), base.b);
      base = hsv2rgb(vec3(hue, sat, val));

      vec2 pad = vec2(tris(seed * 34.0 + uTime * uSpeed / 10.0), tris(seed * 38.0 + uTime * uSpeed / 30.0)) - 0.5;

      float star = Star(gv - offset - pad, flareSize);
      vec3 color = base;

      float twinkle = trisn(uTime * uSpeed + seed * 6.2831) * 0.5 + 1.0;
      twinkle = mix(1.0, twinkle, uTwinkleIntensity);
      star *= twinkle;

      col += star * size * color;
    }
  }

  return col;
}

void main() {
  vec2 focalPx = uFocal * uResolution.xy;
  vec2 uv = (vUv * uResolution.xy - focalPx) / uResolution.y;

  vec2 mouseNorm = uMouse - vec2(0.5);

  if (uAutoCenterRepulsion > 0.0) {
    vec2 centerUV = vec2(0.0, 0.0);
    float centerDist = length(uv - centerUV);
    vec2 repulsion = normalize(uv - centerUV) * (uAutoCenterRepulsion / (centerDist + 0.1));
    uv += repulsion * 0.05;
  } else if (uMouseRepulsion) {
    vec2 mousePosUV = (uMouse * uResolution.xy - focalPx) / uResolution.y;
    float mouseDist = length(uv - mousePosUV);
    vec2 repulsion = normalize(uv - mousePosUV) * (uRepulsionStrength / (mouseDist + 0.1));
    uv += repulsion * 0.05 * uMouseActiveFactor;
  } else {
    vec2 mouseOffset = mouseNorm * 0.1 * uMouseActiveFactor;
    uv += mouseOffset;
  }

  float autoRotAngle = uTime * uRotationSpeed;
  mat2 autoRot = mat2(cos(autoRotAngle), -sin(autoRotAngle), sin(autoRotAngle), cos(autoRotAngle));
  uv = autoRot * uv;

  uv = mat2(uRotation.x, -uRotation.y, uRotation.y, uRotation.x) * uv;

  vec3 col = vec3(0.0);

  for (float i = 0.0; i < 1.0; i += 1.0 / NUM_LAYER) {
    float depth = fract(i + uStarSpeed * uSpeed);
    float scale = mix(20.0 * uDensity, 0.5 * uDensity, depth);
    float fade = depth * smoothstep(1.0, 0.9, depth);
    col += StarLayer(uv * scale + i * 453.32) * fade;
  }

  if (uTransparent) {
    float alpha = length(col);
    alpha = smoothstep(0.0, 0.3, alpha);
    alpha = min(alpha, 1.0);
    gl_FragColor = vec4(col, alpha);
  } else {
    gl_FragColor = vec4(col, 1.0);
  }
}
`

interface GalaxyProps {
  focal?: [number, number]
  rotation?: [number, number]
  starSpeed?: number
  density?: number
  hueShift?: number
  disableAnimation?: boolean
  speed?: number
  mouseInteraction?: boolean
  glowIntensity?: number
  saturation?: number
  mouseRepulsion?: boolean
  twinkleIntensity?: number
  rotationSpeed?: number
  repulsionStrength?: number
  autoCenterRepulsion?: number
  transparent?: boolean
}

const props = withDefaults(defineProps<GalaxyProps>(), {
  focal: () => [0.5, 0.5],
  rotation: () => [1.0, 0.0],
  // 默认值按「星空」调过：星流慢一点、密度高一点、蓝紫为主、闪烁明显
  starSpeed: 0.35,
  density: 1.15,
  hueShift: 212,
  disableAnimation: false,
  speed: 1.0,
  mouseInteraction: true,
  glowIntensity: 0.34,
  saturation: 0.3,
  mouseRepulsion: true,
  twinkleIntensity: 0.4,
  rotationSpeed: 0.06,
  repulsionStrength: 1.6,
  autoCenterRepulsion: 0,
  transparent: true,
})

const ctnDom = useTemplateRef<HTMLDivElement>('ctnDom')
const targetMousePos = ref({ x: 0.5, y: 0.5 })
const smoothMousePos = ref({ x: 0.5, y: 0.5 })
const targetMouseActive = ref(0)
const smoothMouseActive = ref(0)

let cleanup: (() => void) | null = null

/** 系统开了「减少动态效果」就只画一帧静态星空 */
function prefersReduced() {
  return typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

/**
 * 当前用的是哪条渲染路径，同时在 console 里留个可查的标记。
 * 排查「为什么没星星」时，控制台敲 __owlGalaxy 就能看到原因。
 */
type Mode = 'webgl' | 'canvas2d' | 'none'

function markMode(mode: Mode, reason = '') {
  ;(window as unknown as Record<string, unknown>).__owlGalaxy = {
    mode,
    reason,
    dpr: window.devicePixelRatio,
    reducedMotion: prefersReduced(),
  }
  if (mode === 'canvas2d') {
    console.info('[GalaxyBg] WebGL 不可用，已降级为 Canvas2D 星场。原因：' + reason)
  } else if (mode === 'none') {
    console.warn('[GalaxyBg] WebGL 与 Canvas2D 都拿不到，只剩 CSS 渐变底。原因：' + reason)
  }
}

/**
 * 降级方案：用 Canvas2D 画一片会飘、会闪的星。
 * WebGL 被禁用（关了硬件加速 / 显卡在黑名单 / 远程桌面）时兜底，保证「星空」这件事一定看得见。
 */
function setupCanvas2D(ctn: HTMLDivElement) {
  const canvas = document.createElement('canvas')
  canvas.style.cssText = 'display:block;width:100%;height:100%;pointer-events:none'
  ctn.appendChild(canvas)

  const ctx = canvas.getContext('2d')
  if (!ctx) {
    canvas.remove()
    markMode('none', 'canvas.getContext("2d") 返回 null')
    return
  }

  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  let w = 0
  let h = 0
  let stars: { x: number; y: number; r: number; base: number; phase: number; speed: number; hue: number; drift: number }[] = []

  function seedStars() {
    // 按面积给密度，1440×900 大约 250 颗；比 WebGL 版略少，2D 也要照顾性能
    const count = Math.max(90, Math.round((w * h) / 5600))
    stars = Array.from({ length: count }, () => {
      const r = Math.random()
      return {
        x: Math.random() * w,
        y: Math.random() * h,
        // 少数大亮星 + 大量细小星
        r: r > 0.94 ? 1.5 + Math.random() * 1.1 : 0.35 + Math.random() * 0.7,
        base: 0.25 + Math.random() * 0.6,
        phase: Math.random() * Math.PI * 2,
        speed: 0.4 + Math.random() * 1.1,
        // 色相收在 200~265 的蓝紫区间，跟 WebGL 版一致
        hue: 200 + Math.random() * 65,
        drift: 1.6 + Math.random() * 4.2,
      }
    })
  }

  function resize() {
    const nw = ctn.offsetWidth || window.innerWidth
    const nh = ctn.offsetHeight || window.innerHeight
    if (nw === w && nh === h) return
    w = nw
    h = nh
    canvas.width = Math.round(w * dpr)
    canvas.height = Math.round(h * dpr)
    ctx!.setTransform(dpr, 0, 0, dpr, 0, 0)
    seedStars()
  }

  const still = props.disableAnimation || prefersReduced()
  let raf = 0
  let last = 0

  function paint(t: number, dt: number) {
    ctx!.clearRect(0, 0, w, h)
    for (const s of stars) {
      if (!still) {
        // 极慢地往下飘，飘出底边就回到顶边
        s.y += (s.drift * dt) / 16
        if (s.y > h + 4) s.y = -4
      }
      const tw = still ? 0.85 : 0.62 + 0.38 * Math.sin(t * 0.001 * s.speed + s.phase)
      const alpha = s.base * tw

      // 大星给一圈很淡的光晕，小星只画点
      if (s.r > 1.4) {
        const g = ctx!.createRadialGradient(s.x, s.y, 0, s.x, s.y, s.r * 6)
        g.addColorStop(0, `hsla(${s.hue}, 92%, 84%, ${alpha * 0.55})`)
        g.addColorStop(1, `hsla(${s.hue}, 92%, 84%, 0)`)
        ctx!.fillStyle = g
        ctx!.beginPath()
        ctx!.arc(s.x, s.y, s.r * 6, 0, Math.PI * 2)
        ctx!.fill()
      }
      ctx!.fillStyle = `hsla(${s.hue}, 90%, 89%, ${alpha})`
      ctx!.beginPath()
      ctx!.arc(s.x, s.y, s.r, 0, Math.PI * 2)
      ctx!.fill()
    }
  }

  function loop(t: number) {
    raf = requestAnimationFrame(loop)
    // 与 WebGL 版同一套限帧策略：后台不画，前台最多 30fps
    if (document.hidden) return
    if (t - last < FRAME_MS - 4) return
    const dt = last ? Math.min(t - last, 48) : 16
    last = t
    paint(t, dt)
  }

  resize()
  if (still) paint(0, 0)
  else raf = requestAnimationFrame(loop)

  // 尺寸变化（含 0 → 有 尺寸的首帧）都重新量一遍
  const ro = new ResizeObserver(resize)
  ro.observe(ctn)

  markMode('canvas2d', 'WebGL 上下文创建失败（多为浏览器关闭了硬件加速）')

  cleanup = () => {
    cancelAnimationFrame(raf)
    ro.disconnect()
    canvas.remove()
  }
}

/**
 * 渲染缩放：画布按 0.85 倍分辨率画，再由 CSS 拉回容器尺寸。
 *
 * 星场是柔光点阵，肉眼分辨不出 0.85 倍与原尺寸的差别，但片元数量直接降到 0.72 倍。
 * 登录页是整屏 1440×900 的 4 层星场，这一项在集显上大约省下 5ms/帧。
 */
const RENDER_SCALE = 0.85

/** 星空帧率上限：缓慢旋转 + 闪烁，30fps 与 60fps 几乎分辨不出，每秒的片元开销却少一半 */
const FRAME_MS = 1000 / 30

function setup(ctn: HTMLDivElement) {
  let renderer: Renderer
  try {
    renderer = new Renderer({ dpr: RENDER_SCALE, alpha: props.transparent, premultipliedAlpha: false })
  } catch {
    // 没有 WebGL（关了硬件加速 / 显卡被拉黑 / 远程桌面）就换 Canvas2D，不能只剩一个空背景
    setupCanvas2D(ctn)
    return
  }

  const gl = renderer.gl
  if (props.transparent) {
    gl.enable(gl.BLEND)
    gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)
    gl.clearColor(0, 0, 0, 0)
  } else {
    gl.clearColor(0, 0, 0, 1)
  }

  let program: Program | null = null

  function resize() {
    // 容器万一还没量出尺寸（刚挂载的那一帧），退回视口尺寸，别把画布做成 0×0
    const w = ctn.offsetWidth || window.innerWidth
    const h = ctn.offsetHeight || window.innerHeight
    renderer.setSize(w, h)
    if (program && gl.canvas.height > 0) {
      program.uniforms.uResolution.value = new Color(
        gl.canvas.width,
        gl.canvas.height,
        gl.canvas.width / gl.canvas.height,
      )
    }
  }
  window.addEventListener('resize', resize, false)
  // window.resize 抓不到「容器从 0 变成有尺寸」，用 ResizeObserver 补上
  const ro = new ResizeObserver(resize)
  ro.observe(ctn)
  resize()

  const geometry = new Triangle(gl)
  program = new Program(gl, {
    vertex: vertexShader,
    fragment: fragmentShader,
    uniforms: {
      uTime: { value: 0 },
      uResolution: {
        value: new Color(gl.canvas.width, gl.canvas.height, gl.canvas.width / gl.canvas.height),
      },
      uFocal: { value: new Float32Array(props.focal) },
      uRotation: { value: new Float32Array(props.rotation) },
      uStarSpeed: { value: props.starSpeed },
      uDensity: { value: props.density },
      uHueShift: { value: props.hueShift },
      uSpeed: { value: props.speed },
      uMouse: { value: new Float32Array([smoothMousePos.value.x, smoothMousePos.value.y]) },
      uGlowIntensity: { value: props.glowIntensity },
      uSaturation: { value: props.saturation },
      uMouseRepulsion: { value: props.mouseRepulsion },
      uTwinkleIntensity: { value: props.twinkleIntensity },
      uRotationSpeed: { value: props.rotationSpeed },
      uRepulsionStrength: { value: props.repulsionStrength },
      uMouseActiveFactor: { value: 0 },
      uAutoCenterRepulsion: { value: props.autoCenterRepulsion },
      uTransparent: { value: props.transparent },
    },
  })

  const mesh = new Mesh(gl, { geometry, program })
  let animateId = 0

  const still = props.disableAnimation || prefersReduced()
  // 静态模式给个固定的时间点，星位不会全挤在一起
  if (still) program.uniforms.uTime.value = 12.4

  /** 画一帧（不含调度），限帧、暂停都复用这一份逻辑 */
  function draw(t: number) {
    if (!still) {
      program!.uniforms.uTime.value = t * 0.001
      program!.uniforms.uStarSpeed.value = (t * 0.001 * props.starSpeed) / 10.0
    }

    const lerp = 0.05
    smoothMousePos.value.x += (targetMousePos.value.x - smoothMousePos.value.x) * lerp
    smoothMousePos.value.y += (targetMousePos.value.y - smoothMousePos.value.y) * lerp
    smoothMouseActive.value += (targetMouseActive.value - smoothMouseActive.value) * lerp

    const uMouse = program!.uniforms.uMouse.value as Float32Array
    uMouse[0] = smoothMousePos.value.x
    uMouse[1] = smoothMousePos.value.y
    program!.uniforms.uMouseActiveFactor.value = smoothMouseActive.value

    renderer.render({ scene: mesh })
  }

  let lastDraw = -FRAME_MS

  function update(t: number) {
    animateId = requestAnimationFrame(update)
    // 切到别的标签页就彻底不画，回来接着画
    if (document.hidden) return
    // 限帧：差几毫秒就等下一帧，别在临界点上抖
    if (t - lastDraw < FRAME_MS - 4) return
    lastDraw = t
    draw(t)
  }

  ctn.appendChild(gl.canvas)

  // 静态模式（系统开了「减少动态效果」）只出一帧就停，不进 rAF 循环
  if (still) draw(0)
  else animateId = requestAnimationFrame(update)

  function handleMouseMove(e: MouseEvent) {
    const rect = ctn.getBoundingClientRect()
    targetMousePos.value = {
      x: (e.clientX - rect.left) / rect.width,
      y: 1 - (e.clientY - rect.top) / rect.height,
    }
    targetMouseActive.value = 1
  }

  function handleMouseLeave() {
    targetMouseActive.value = 0
  }

  if (props.mouseInteraction) {
    ctn.addEventListener('mousemove', handleMouseMove)
    ctn.addEventListener('mouseleave', handleMouseLeave)
  }

  // 运行中被系统回收了 GPU 上下文（切显卡 / 显存吃紧）也别留一块白板，立刻换 Canvas2D
  gl.canvas.addEventListener(
    'webglcontextlost',
    (e) => {
      e.preventDefault()
      cancelAnimationFrame(animateId)
      cleanup?.()
      cleanup = null
      setupCanvas2D(ctn)
    },
    false,
  )

  markMode('webgl')

  cleanup = () => {
    cancelAnimationFrame(animateId)
    window.removeEventListener('resize', resize)
    ro.disconnect()
    if (props.mouseInteraction) {
      ctn.removeEventListener('mousemove', handleMouseMove)
      ctn.removeEventListener('mouseleave', handleMouseLeave)
    }
    if (gl.canvas.parentNode === ctn) ctn.removeChild(gl.canvas)
    gl.getExtension('WEBGL_lose_context')?.loseContext()
  }
}

/** 挂载 / 重挂载：容器没渲染出来（或被卸载）就什么都不做 */
function mount() {
  const el = ctnDom.value
  if (!el) return
  cleanup?.()
  setup(el)
}

onMounted(mount)

onBeforeUnmount(() => {
  cleanup?.()
  cleanup = null
})

watch(() => props, mount, { deep: true })
</script>

<template>
  <div ref="ctnDom" class="galaxy" />
</template>

<style scoped>
.galaxy {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

/* 画布铺满，且不抢文字的点击 */
.galaxy :deep(canvas) {
  display: block;
  width: 100%;
  height: 100%;
  pointer-events: none;
}
</style>
