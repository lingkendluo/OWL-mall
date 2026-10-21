<template>
  <div class="stage">

    <!-- ================= 左侧品牌 ================= -->
    <section class="brand">
      <header class="brand-mark">
        <span class="mark" aria-hidden="true">
          <svg viewBox="0 0 32 32" fill="none" stroke="currentColor" stroke-width="1.7"
               stroke-linecap="round" stroke-linejoin="round">
            <path d="M7 11.4 6.6 5.6l4.7 3.6" />
            <path d="M25 11.4l.4-5.8-4.7 3.6" />
            <path d="M16 6.6c5.6 0 9.4 4.3 9.4 9.7s-4 10.9-9.4 10.9-9.4-5.5-9.4-10.9S10.4 6.6 16 6.6Z" />
            <circle cx="11.9" cy="14.4" r="3.5" />
            <circle cx="20.1" cy="14.4" r="3.5" />
            <circle cx="11.9" cy="14.4" r="1" fill="currentColor" stroke="none" />
            <circle cx="20.1" cy="14.4" r="1" fill="currentColor" stroke="none" />
            <path d="M16 17.6l-1.4 2.2h2.8L16 17.6Z" fill="currentColor" stroke="none" />
          </svg>
        </span>
        <span class="wordmark">OWL MALL</span>
        <span class="since">EST. 2020</span>
        <span class="dots" aria-hidden="true"><i></i><i></i><i></i></span>
      </header>

      <div class="hero">
        <h1>
          <span class="l1">OWL</span>
          <span class="l2">商城</span>
        </h1>
      </div>

      <!-- 一对纸片人 -->
      <div ref="dollsEl" class="dolls">
        <div class="doll" data-doll="a" data-state="0" data-face="rest" role="button" tabindex="0" aria-label="纸片人小黄">
          <div class="bubble shape-round" data-bubble>
            <span class="bubble-in" data-text>春江花月何时了～</span>
          </div>
          <div class="doll-art">
            <svg class="figure" viewBox="0 0 100 110" aria-hidden="true">
              <path d="M32 8 L68 8 Q82 8 82 24 L82 80 Q82 96 68 96 L61 96 C57 70 43 70 39 96 L32 96 Q18 96 18 80 L18 24 Q18 8 32 8 Z"
                    fill="#b07c3c" transform="translate(1.6,2.4)" />
              <g class="hands">
                <circle cx="8.4" cy="60" r="6.2" fill="#b07c3c" transform="translate(1.2,1.8)" />
                <circle cx="91.6" cy="60" r="6.2" fill="#b07c3c" transform="translate(1.2,1.8)" />
                <circle cx="8.4" cy="60" r="6.2" fill="#d19a5c" />
                <circle cx="91.6" cy="60" r="6.2" fill="#d19a5c" />
              </g>

              <path d="M32 8 L68 8 Q82 8 82 24 L82 80 Q82 96 68 96 L61 96 C57 70 43 70 39 96 L32 96 Q18 96 18 80 L18 24 Q18 8 32 8 Z"
                    fill="#dda86c" stroke="#b8843f" stroke-width="1.6" stroke-linejoin="round" />

              <!-- 抱胸 -->
              <g class="arms-x">
                <rect x="24" y="70" width="40" height="11" rx="5.5" fill="#c1843f" stroke="#9c6527" stroke-width="1.2" transform="rotate(8 44 75.5)" />
                <rect x="36" y="79" width="40" height="11" rx="5.5" fill="#d59a52" stroke="#9c6527" stroke-width="1.2" transform="rotate(-8 56 84.5)" />
                <circle cx="63" cy="79" r="5.6" fill="#c1843f" stroke="#9c6527" stroke-width="1.2" />
                <circle cx="37" cy="82" r="5.6" fill="#d59a52" stroke="#9c6527" stroke-width="1.2" />
              </g>

              <!-- 捂屁股 -->
              <g class="arms-butt">
                <path d="M25 58 Q17 74 30 84" stroke="#b8792f" stroke-width="10" fill="none" stroke-linecap="round" />
                <path d="M75 58 Q83 74 70 84" stroke="#c1853c" stroke-width="10" fill="none" stroke-linecap="round" />
                <circle cx="31" cy="86" r="5.8" fill="#b8792f" />
                <circle cx="69" cy="86" r="5.8" fill="#c1853c" />
                <ellipse cx="33" cy="90" rx="7" ry="3.6" fill="#ef7676" opacity="0.5" />
              </g>

              <!-- 常态 -->
              <g class="face face-rest">
                <ellipse class="sclera" cx="36.5" cy="44" rx="11.5" ry="13" fill="#fffdf9" stroke="#7d5527" stroke-width="1.2" stroke-opacity="0.5" />
                <ellipse class="sclera" cx="63.5" cy="44" rx="11.5" ry="13" fill="#fffdf9" stroke="#7d5527" stroke-width="1.2" stroke-opacity="0.5" />
                <g class="pupil" style="transform-origin:36.5px 44px">
                  <circle cx="36.5" cy="45.4" r="6.3" fill="#3b2a1c" />
                  <circle cx="34" cy="42.6" r="2.3" fill="#ffffff" />
                </g>
                <g class="pupil" style="transform-origin:63.5px 44px">
                  <circle cx="63.5" cy="45.4" r="6.3" fill="#3b2a1c" />
                  <circle cx="61" cy="42.6" r="2.3" fill="#ffffff" />
                </g>
                <ellipse class="blush" cx="26" cy="60" rx="7.4" ry="4.7" fill="#ef7f9c" opacity="0" />
                <ellipse class="blush" cx="74" cy="60" rx="7.4" ry="4.7" fill="#ef7f9c" opacity="0" />
                <path d="M44.5 63q5.5 5.4 11 0" fill="none" stroke="#8a4f2c" stroke-width="2.4" stroke-linecap="round" />
              </g>

              <!-- 大笑 -->
              <g class="face face-laugh">
                <path d="M25 45q11.5 -12 23 0" fill="none" stroke="#3b2a1c" stroke-width="2.8" stroke-linecap="round" />
                <path d="M52 45q11.5 -12 23 0" fill="none" stroke="#3b2a1c" stroke-width="2.8" stroke-linecap="round" />
                <ellipse cx="24.5" cy="57" rx="7.2" ry="4.6" fill="#f2798f" opacity="0.8" />
                <ellipse cx="75.5" cy="57" rx="7.2" ry="4.6" fill="#f2798f" opacity="0.8" />
                <path d="M40.5 61q9.5 20 19 0Z" fill="#6b3520" stroke="#3b2a1c" stroke-width="1.4" stroke-linejoin="round" />
                <path d="M46 71q4 5.5 8 0Z" fill="#f08298" />
              </g>

              <!-- 委屈 -->
              <g class="face face-sad">
                <path d="M25 31.5q11 3 22 0" fill="none" stroke="#8a4f2c" stroke-width="2.2" stroke-linecap="round" />
                <path d="M53 31.5q11 3 22 0" fill="none" stroke="#8a4f2c" stroke-width="2.2" stroke-linecap="round" />
                <ellipse class="sclera" cx="36.5" cy="45" rx="10" ry="10.4" fill="#fffdf9" stroke="#7d5527" stroke-width="1.1" stroke-opacity="0.45" />
                <ellipse class="sclera" cx="63.5" cy="45" rx="10" ry="10.4" fill="#fffdf9" stroke="#7d5527" stroke-width="1.1" stroke-opacity="0.45" />
                <circle cx="37" cy="48.6" r="5.2" fill="#3b2a1c" />
                <circle cx="62.8" cy="48.6" r="5.2" fill="#3b2a1c" />
                <circle cx="35" cy="46.6" r="1.9" fill="#ffffff" />
                <circle cx="60.8" cy="46.6" r="1.9" fill="#ffffff" />
                <path d="M44 70q6 -6 12 0" fill="none" stroke="#8a4f2c" stroke-width="2.4" stroke-linecap="round" />
                <path class="tear" d="M73 50q3.6 6.2 0 8.2q-3.6 -2 0 -8.2" fill="#9ed7ee" stroke="#6fb6d4" stroke-width="0.9" />
              </g>

              <!-- 生气 -->
              <g class="face face-angry">
                <path d="M24 33 L46 39" fill="none" stroke="#8a4f2c" stroke-width="2.6" stroke-linecap="round" />
                <path d="M76 33 L54 39" fill="none" stroke="#8a4f2c" stroke-width="2.6" stroke-linecap="round" />
                <ellipse class="sclera" cx="36.5" cy="46" rx="9.6" ry="10.6" fill="#fffdf9" stroke="#7d5527" stroke-width="1.1" stroke-opacity="0.45" />
                <ellipse class="sclera" cx="63.5" cy="46" rx="9.6" ry="10.6" fill="#fffdf9" stroke="#7d5527" stroke-width="1.1" stroke-opacity="0.45" />
                <g class="pupil" style="transform-origin:36.5px 46px">
                  <circle cx="36.5" cy="47.4" r="5.4" fill="#3b2a1c" />
                  <circle cx="34.4" cy="45" r="1.9" fill="#ffffff" />
                </g>
                <g class="pupil" style="transform-origin:63.5px 46px">
                  <circle cx="63.5" cy="47.4" r="5.4" fill="#3b2a1c" />
                  <circle cx="61.4" cy="45" r="1.9" fill="#ffffff" />
                </g>
                <ellipse cx="24.5" cy="58" rx="7" ry="4.4" fill="#f2798f" opacity="0.65" />
                <ellipse cx="75.5" cy="58" rx="7" ry="4.4" fill="#f2798f" opacity="0.65" />
                <path d="M43 70q7 -6 14 0" fill="none" stroke="#8a4f2c" stroke-width="2.4" stroke-linecap="round" />
                <path d="M74 20q5 -3 7 1" fill="none" stroke="#c98a4a" stroke-width="1.8" stroke-linecap="round" />
                <path d="M76 27q5 -2 6 2" fill="none" stroke="#c98a4a" stroke-width="1.8" stroke-linecap="round" />
              </g>

              <!-- 疼 -->
              <g class="face face-pain">
                <path d="M28 39 L41 50 M41 39 L28 50" fill="none" stroke="#3b2a1c" stroke-width="2.6" stroke-linecap="round" />
                <path d="M59 39 L72 50 M72 39 L59 50" fill="none" stroke="#3b2a1c" stroke-width="2.6" stroke-linecap="round" />
                <path d="M42 64q8 10 16 0Z" fill="#6b3520" stroke="#3b2a1c" stroke-width="1.4" stroke-linejoin="round" />
                <path class="sweat" d="M78 30q4 6.5 0 8.6q-4 -2.1 0 -8.6" fill="#9ed7ee" stroke="#6fb6d4" stroke-width="0.9" />
                <path class="sweat sweat-2" d="M22 36q3 5 0 6.6q-3 -1.6 0 -6.6" fill="#9ed7ee" stroke="#6fb6d4" stroke-width="0.8" />
              </g>
            </svg>
          </div>

          <svg class="impact" viewBox="0 0 40 40" aria-hidden="true">
            <path d="M20 2v9M20 29v9M2 20h9M29 20h9M8 8l6 6M26 26l6 6M32 8l-6 6M14 26l-6 6"
                  stroke="#f2a33c" stroke-width="3.2" stroke-linecap="round" fill="none" />
          </svg>

          <svg class="ball" viewBox="0 0 100 110" aria-hidden="true">
            <path d="M38 64 L52 58 L64 66 L68 80 L58 92 L42 90 L32 78 Z" fill="#e3b681" stroke="#b07c3c" stroke-width="1.2" stroke-linejoin="round" />
            <path d="M52 58 L45 75 L32 78 L38 64 Z" fill="#cfa068" />
            <path d="M64 66 L57 79 L68 80 Z" fill="#d9ae74" />
            <path d="M45 75 L57 79 L58 92 L42 90 Z" fill="#dcb37c" />
            <path d="M52 58 L45 75 M57 79 L64 66 M45 75 L42 90" stroke="#a5733a" stroke-width="0.9" fill="none" opacity="0.6" />
          </svg>
        </div>

        <div class="doll" data-doll="b" data-state="0" data-face="rest" role="button" tabindex="0" aria-label="纸片人小白">
          <div class="bubble shape-spike" data-bubble>
            <span class="bubble-in" data-text>今天晚上没吃饱 😢</span>
          </div>
          <div class="doll-art">
            <svg class="figure" viewBox="0 0 100 110" aria-hidden="true">
              <path d="M32 8 L68 8 Q82 8 82 24 L82 80 Q82 96 68 96 L61 96 C57 70 43 70 39 96 L32 96 Q18 96 18 80 L18 24 Q18 8 32 8 Z"
                    fill="#dd9db0" transform="translate(1.6,2.4)" />
              <g class="hands">
                <circle cx="8.4" cy="60" r="6.2" fill="#dd9db0" transform="translate(1.2,1.8)" />
                <circle cx="91.6" cy="60" r="6.2" fill="#dd9db0" transform="translate(1.2,1.8)" />
                <circle cx="8.4" cy="60" r="6.2" fill="#f2b9ca" />
                <circle cx="91.6" cy="60" r="6.2" fill="#f2b9ca" />
              </g>

              <path d="M32 8 L68 8 Q82 8 82 24 L82 80 Q82 96 68 96 L61 96 C57 70 43 70 39 96 L32 96 Q18 96 18 80 L18 24 Q18 8 32 8 Z"
                    fill="#f8ccd9" stroke="#d998ac" stroke-width="1.6" stroke-linejoin="round" />

              <g class="arms-x">
                <rect x="24" y="70" width="40" height="11" rx="5.5" fill="#e894ae" stroke="#c97792" stroke-width="1.2" transform="rotate(8 44 75.5)" />
                <rect x="36" y="79" width="40" height="11" rx="5.5" fill="#f2b0c5" stroke="#c97792" stroke-width="1.2" transform="rotate(-8 56 84.5)" />
                <circle cx="63" cy="79" r="5.6" fill="#e894ae" stroke="#c97792" stroke-width="1.2" />
                <circle cx="37" cy="82" r="5.6" fill="#f2b0c5" stroke="#c97792" stroke-width="1.2" />
              </g>

              <g class="arms-butt">
                <path d="M25 58 Q17 74 30 84" stroke="#d97f99" stroke-width="10" fill="none" stroke-linecap="round" />
                <path d="M75 58 Q83 74 70 84" stroke="#e395ac" stroke-width="10" fill="none" stroke-linecap="round" />
                <circle cx="31" cy="86" r="5.8" fill="#d97f99" />
                <circle cx="69" cy="86" r="5.8" fill="#e395ac" />
                <ellipse cx="33" cy="90" rx="7" ry="3.6" fill="#ef7676" opacity="0.45" />
              </g>

              <g class="face face-rest">
                <ellipse class="sclera" cx="36.5" cy="44" rx="11.5" ry="13" fill="#fffbfd" stroke="#b07d8e" stroke-width="1.2" stroke-opacity="0.45" />
                <ellipse class="sclera" cx="63.5" cy="44" rx="11.5" ry="13" fill="#fffbfd" stroke="#b07d8e" stroke-width="1.2" stroke-opacity="0.45" />
                <g class="pupil" style="transform-origin:36.5px 44px">
                  <circle cx="36.5" cy="45.4" r="6.3" fill="#5a3a44" />
                  <circle cx="34" cy="42.6" r="2.3" fill="#ffffff" />
                </g>
                <g class="pupil" style="transform-origin:63.5px 44px">
                  <circle cx="63.5" cy="45.4" r="6.3" fill="#5a3a44" />
                  <circle cx="61" cy="42.6" r="2.3" fill="#ffffff" />
                </g>
                <ellipse class="blush" cx="26" cy="60" rx="7.4" ry="4.7" fill="#ee6d90" opacity="0" />
                <ellipse class="blush" cx="74" cy="60" rx="7.4" ry="4.7" fill="#ee6d90" opacity="0" />
                <path d="M44.5 63q5.5 5.4 11 0" fill="none" stroke="#b2637a" stroke-width="2.4" stroke-linecap="round" />
              </g>

              <g class="face face-laugh">
                <path d="M25 45q11.5 -12 23 0" fill="none" stroke="#5a3a44" stroke-width="2.8" stroke-linecap="round" />
                <path d="M52 45q11.5 -12 23 0" fill="none" stroke="#5a3a44" stroke-width="2.8" stroke-linecap="round" />
                <ellipse cx="24.5" cy="57" rx="7.2" ry="4.6" fill="#f2798f" opacity="0.8" />
                <ellipse cx="75.5" cy="57" rx="7.2" ry="4.6" fill="#f2798f" opacity="0.8" />
                <path d="M40.5 61q9.5 20 19 0Z" fill="#8a4358" stroke="#5a3a44" stroke-width="1.4" stroke-linejoin="round" />
                <path d="M46 71q4 5.5 8 0Z" fill="#f08298" />
              </g>

              <g class="face face-sad">
                <path d="M25 31.5q11 3 22 0" fill="none" stroke="#b2637a" stroke-width="2.2" stroke-linecap="round" />
                <path d="M53 31.5q11 3 22 0" fill="none" stroke="#b2637a" stroke-width="2.2" stroke-linecap="round" />
                <ellipse class="sclera" cx="36.5" cy="45" rx="10" ry="10.4" fill="#fffbfd" stroke="#b07d8e" stroke-width="1.1" stroke-opacity="0.4" />
                <ellipse class="sclera" cx="63.5" cy="45" rx="10" ry="10.4" fill="#fffbfd" stroke="#b07d8e" stroke-width="1.1" stroke-opacity="0.4" />
                <circle cx="37" cy="48.6" r="5.2" fill="#5a3a44" />
                <circle cx="62.8" cy="48.6" r="5.2" fill="#5a3a44" />
                <circle cx="35" cy="46.6" r="1.9" fill="#ffffff" />
                <circle cx="60.8" cy="46.6" r="1.9" fill="#ffffff" />
                <path d="M44 70q6 -6 12 0" fill="none" stroke="#b2637a" stroke-width="2.4" stroke-linecap="round" />
                <path class="tear" d="M73 50q3.6 6.2 0 8.2q-3.6 -2 0 -8.2" fill="#9ed7ee" stroke="#6fb6d4" stroke-width="0.9" />
              </g>

              <g class="face face-angry">
                <path d="M24 33 L46 39" fill="none" stroke="#b2637a" stroke-width="2.6" stroke-linecap="round" />
                <path d="M76 33 L54 39" fill="none" stroke="#b2637a" stroke-width="2.6" stroke-linecap="round" />
                <ellipse class="sclera" cx="36.5" cy="46" rx="9.6" ry="10.6" fill="#fffbfd" stroke="#b07d8e" stroke-width="1.1" stroke-opacity="0.4" />
                <ellipse class="sclera" cx="63.5" cy="46" rx="9.6" ry="10.6" fill="#fffbfd" stroke="#b07d8e" stroke-width="1.1" stroke-opacity="0.4" />
                <g class="pupil" style="transform-origin:36.5px 46px">
                  <circle cx="36.5" cy="47.4" r="5.4" fill="#5a3a44" />
                  <circle cx="34.4" cy="45" r="1.9" fill="#ffffff" />
                </g>
                <g class="pupil" style="transform-origin:63.5px 46px">
                  <circle cx="63.5" cy="47.4" r="5.4" fill="#5a3a44" />
                  <circle cx="61.4" cy="45" r="1.9" fill="#ffffff" />
                </g>
                <ellipse cx="24.5" cy="58" rx="7" ry="4.4" fill="#f2798f" opacity="0.65" />
                <ellipse cx="75.5" cy="58" rx="7" ry="4.4" fill="#f2798f" opacity="0.65" />
                <path d="M43 70q7 -6 14 0" fill="none" stroke="#b2637a" stroke-width="2.4" stroke-linecap="round" />
                <path d="M74 20q5 -3 7 1" fill="none" stroke="#df8ba3" stroke-width="1.8" stroke-linecap="round" />
                <path d="M76 27q5 -2 6 2" fill="none" stroke="#df8ba3" stroke-width="1.8" stroke-linecap="round" />
              </g>

              <g class="face face-pain">
                <path d="M28 39 L41 50 M41 39 L28 50" fill="none" stroke="#5a3a44" stroke-width="2.6" stroke-linecap="round" />
                <path d="M59 39 L72 50 M72 39 L59 50" fill="none" stroke="#5a3a44" stroke-width="2.6" stroke-linecap="round" />
                <path d="M42 64q8 10 16 0Z" fill="#8a4358" stroke="#5a3a44" stroke-width="1.4" stroke-linejoin="round" />
                <path class="sweat" d="M78 30q4 6.5 0 8.6q-4 -2.1 0 -8.6" fill="#9ed7ee" stroke="#6fb6d4" stroke-width="0.9" />
                <path class="sweat sweat-2" d="M22 36q3 5 0 6.6q-3 -1.6 0 -6.6" fill="#9ed7ee" stroke="#6fb6d4" stroke-width="0.8" />
              </g>
            </svg>
          </div>

          <svg class="impact" viewBox="0 0 40 40" aria-hidden="true">
            <path d="M20 2v9M20 29v9M2 20h9M29 20h9M8 8l6 6M26 26l6 6M32 8l-6 6M14 26l-6 6"
                  stroke="#f2a33c" stroke-width="3.2" stroke-linecap="round" fill="none" />
          </svg>

          <svg class="ball" viewBox="0 0 100 110" aria-hidden="true">
            <path d="M38 64 L52 58 L64 66 L68 80 L58 92 L42 90 L32 78 Z" fill="#f6cad6" stroke="#d998ac" stroke-width="1.2" stroke-linejoin="round" />
            <path d="M52 58 L45 75 L32 78 L38 64 Z" fill="#e9b3c5" />
            <path d="M64 66 L57 79 L68 80 Z" fill="#efbfcf" />
            <path d="M45 75 L57 79 L58 92 L42 90 Z" fill="#f2c3d1" />
            <path d="M52 58 L45 75 M57 79 L64 66 M45 75 L42 90" stroke="#c9869b" stroke-width="0.9" fill="none" opacity="0.55" />
          </svg>
        </div>
      </div>

      <p class="copyright">© 2026 OWL MALL · 让购物更简单</p>
    </section>

    <!-- ================= 右侧登录 ================= -->
    <section class="auth">
      <div class="card">
        <header class="card-head">
          <h2>{{ META[tab].title }}</h2>
          <p>{{ META[tab].sub }}</p>
        </header>

        <div class="seg" role="tablist" aria-label="登录方式">
          <span class="seg-thumb" :style="{ transform: 'translateX(' + tabIndex * 100 + '%)' }" aria-hidden="true"></span>
          <button :class="['seg-btn', { 'is-active': tab === 'login' }]" type="button" role="tab" :aria-selected="tab === 'login'" @click="setTab('login')">登录</button>
          <button :class="['seg-btn', { 'is-active': tab === 'register' }]" type="button" role="tab" :aria-selected="tab === 'register'" @click="setTab('register')">注册</button>
          <button :class="['seg-btn', { 'is-active': tab === 'guest' }]" type="button" role="tab" :aria-selected="tab === 'guest'" @click="setTab('guest')">体验</button>
        </div>

        <div ref="panelsEl" class="panels">

          <form :class="['panel', { 'is-active': tab === 'login' }]" novalidate @submit.prevent="onLogin">
            <div class="field">
              <label class="flabel" for="lgUser">邮箱地址</label>
              <div class="ctrl">
                <input id="lgUser" v-model="loginForm.user" type="text" placeholder="请输入邮箱地址或手机号" autocomplete="username" />
              </div>
            </div>

            <div class="field">
              <label class="flabel" for="lgPwd">密码</label>
              <div class="ctrl has-btn">
                <input id="lgPwd" v-model="loginForm.pwd" :type="showPwd.lg ? 'text' : 'password'" placeholder="请输入密码" autocomplete="current-password" />
                <button class="eye" type="button" data-eye="lgPwd" :class="{ 'is-on': showPwd.lg }" :aria-label="showPwd.lg ? '隐藏密码' : '显示密码'" @click="togglePwd('lg')">
                  <svg class="eye-on" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                       stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M2.2 12S5.8 5.8 12 5.8 21.8 12 21.8 12 18.2 18.2 12 18.2 2.2 12 2.2 12Z" />
                    <circle cx="12" cy="12" r="2.9" />
                  </svg>
                  <svg class="eye-off" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                       stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M4 4l16 16" />
                    <path d="M9.6 5.1A9.9 9.9 0 0 1 12 4.9c6.2 0 9.8 6.2 9.8 6.2a17.6 17.6 0 0 1-3.3 4" />
                    <path d="M6.4 7.3A17.4 17.4 0 0 0 2.2 11.1s3.6 6.2 9.8 6.2c1.3 0 2.5-.3 3.5-.7" />
                    <path d="M10 10a2.8 2.8 0 0 0 4 4" />
                  </svg>
                </button>
              </div>
            </div>

            <div class="row">
              <label class="check">
                <input v-model="loginForm.remember" type="checkbox" />
                <span class="box"></span>保持登录
              </label>
              <a class="link" href="#" @click.prevent="hint('密码找回功能开发中')">忘记密码？</a>
            </div>

            <button class="btn-primary" type="submit" :class="{ 'is-loading': loading.login }" :disabled="loading.login">
              <span class="label">登 录</span>
              <span class="spinner"></span>
            </button>

            <div class="or"><i></i><span>或使用以下方式</span><i></i></div>

            <div class="oauth">
              <button class="oauth-btn" type="button" @click="hint('微信登录暂未开通')">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"
                     stroke-linecap="round" stroke-linejoin="round">
                  <path d="M20.5 11.4c0-3.4-3.2-6.1-7.1-6.1S6.3 8 6.3 11.4s3.2 6.1 7.1 6.1c.7 0 1.4-.1 2-.3l2.6 1.4-.6-2.3a5.9 5.9 0 0 0 3.1-4.9Z" />
                  <path d="M12.3 7.7c-1.3-1-3.1-1.5-4.9-1.5-2.6 0-4.8 1.9-4.8 4.2" opacity=".45" />
                </svg>
                <span>微信</span>
              </button>

              <button class="oauth-btn" type="button" @click="hint('QQ 登录暂未开通')">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"
                     stroke-linecap="round" stroke-linejoin="round">
                  <path d="M12 3.2c2.6 0 4.4 1.9 4.4 4.6 0 1 .6 1.6 1.2 2.6.9 1.5 1.4 2.6 1.4 4.2 0 1.4-.7 2.2-1.6 2.2-.7 0-1.3-.5-1.7-1.3-.5 1.9-2 3.1-3.7 3.1s-3.2-1.2-3.7-3.1c-.4.8-1 1.3-1.7 1.3-.9 0-1.6-.8-1.6-2.2 0-1.6.5-2.7 1.4-4.2.6-1 1.2-1.6 1.2-2.6 0-2.7 1.8-4.6 4.4-4.6Z" />
                  <circle cx="10.2" cy="8.6" r=".9" fill="currentColor" stroke="none" />
                  <circle cx="13.8" cy="8.6" r=".9" fill="currentColor" stroke="none" />
                </svg>
                <span>QQ</span>
              </button>

              <button class="oauth-btn" type="button" @click="hint('手机号登录暂未开通')">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"
                     stroke-linecap="round" stroke-linejoin="round">
                  <rect x="6.4" y="2.6" width="11.2" height="18.8" rx="2.6" />
                  <path d="M10.6 18.4h2.8" />
                </svg>
                <span>手机号</span>
              </button>
            </div>

            <p class="switch-tip">还没有账号？<a @click.prevent="setTab('register')">创建账号</a></p>
          </form>

          <form :class="['panel', { 'is-active': tab === 'register' }]" novalidate @submit.prevent="onRegister">
            <div class="field">
              <label class="flabel" for="rgName">昵称</label>
              <div class="ctrl">
                <input id="rgName" v-model="regForm.name" type="text" placeholder="给自己起个名字" autocomplete="nickname" />
              </div>
            </div>

            <div class="field">
              <label class="flabel" for="rgPhone">手机号</label>
              <div class="ctrl">
                <input id="rgPhone" v-model="regForm.phone" type="tel" placeholder="请输入 11 位手机号" autocomplete="tel" maxlength="11" />
              </div>
            </div>

            <div class="field">
              <label class="flabel" for="rgCode">短信验证码</label>
              <div class="ctrl has-code">
                <input id="rgCode" v-model="regForm.code" type="text" placeholder="6 位数字" inputmode="numeric" maxlength="6" />
                <button class="code-btn" type="button" :disabled="codeLeft > 0" @click="sendCode">{{ codeText }}</button>
              </div>
            </div>

            <div class="field">
              <label class="flabel" for="rgPwd">设置密码</label>
              <div class="ctrl has-btn">
                <input id="rgPwd" v-model="regForm.pwd" :type="showPwd.rg ? 'text' : 'password'" placeholder="8-20 位，字母加数字" autocomplete="new-password" />
                <button class="eye" type="button" data-eye="rgPwd" :class="{ 'is-on': showPwd.rg }" :aria-label="showPwd.rg ? '隐藏密码' : '显示密码'" @click="togglePwd('rg')">
                  <svg class="eye-on" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                       stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M2.2 12S5.8 5.8 12 5.8 21.8 12 21.8 12 18.2 18.2 12 18.2 2.2 12 2.2 12Z" />
                    <circle cx="12" cy="12" r="2.9" />
                  </svg>
                  <svg class="eye-off" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                       stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M4 4l16 16" />
                    <path d="M9.6 5.1A9.9 9.9 0 0 1 12 4.9c6.2 0 9.8 6.2 9.8 6.2a17.6 17.6 0 0 1-3.3 4" />
                    <path d="M6.4 7.3A17.4 17.4 0 0 0 2.2 11.1s3.6 6.2 9.8 6.2c1.3 0 2.5-.3 3.5-.7" />
                    <path d="M10 10a2.8 2.8 0 0 0 4 4" />
                  </svg>
                </button>
              </div>
            </div>

            <label class="check agree">
              <input id="rgAgree" v-model="regForm.agree" type="checkbox" />
              <span class="box"></span>
              <span>我已阅读并同意 <a href="#">《用户协议》</a> 与 <a href="#">《隐私政策》</a></span>
            </label>

            <button class="btn-primary" type="submit" :class="{ 'is-loading': loading.register }" :disabled="loading.register">
              <span class="label">创建账号</span>
              <span class="spinner"></span>
            </button>

            <p class="switch-tip">已有账号？<a @click.prevent="setTab('login')">直接登录</a></p>
          </form>

          <div :class="['panel', { 'is-active': tab === 'guest' }]">
            <p class="guest-lead">
              无需注册，以访客身份直接进入商城。可浏览全部商品、加入购物车，
              体验数据在 24 小时内保留。
            </p>

            <div class="guest-cards">
              <div class="g-card">
                <span class="g-ico">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"
                       stroke-linecap="round" stroke-linejoin="round">
                    <path d="M3 4h2.2l2.3 11.2a1.8 1.8 0 0 0 1.8 1.4h8.5a1.8 1.8 0 0 0 1.8-1.4L21 8H6" />
                    <circle cx="10" cy="20" r="1.3" />
                    <circle cx="17.6" cy="20" r="1.3" />
                  </svg>
                </span>
                <span class="g-txt">
                  <b>自由体验</b>
                  <small>逛商品 · 加购物车 · 模拟下单，功能全开放</small>
                </span>
                <span class="g-badge">全部功能</span>
              </div>
            </div>

            <button class="btn-primary" type="button" :class="{ 'is-loading': loading.guest }" :disabled="loading.guest" @click="onGuest">
              <span class="label">一键进入商城</span>
              <span class="spinner"></span>
            </button>

            <p class="guest-note">体验账号数据独立，不会影响你的正式账号</p>
          </div>

        </div>
      </div>

      <p class="foot">登录即表示同意《用户协议》与《隐私政策》</p>
    </section>
  </div>

  <div class="toast" :class="{ show: toastVisible }" role="status" aria-live="polite">
    <span class="dot"></span>
    <span>{{ toastText }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { initDollScene } from './doll-scene.js'
import './login.css'

type TabKey = 'login' | 'register' | 'guest'

const TABS: TabKey[] = ['login', 'register', 'guest']

const META: Record<TabKey, { title: string; sub: string }> = {
  login: { title: '欢迎回来', sub: '登录后继续你的购物旅程' },
  register: { title: '创建账号', sub: '30 秒完成注册，开启 OWL 之旅' },
  guest: { title: '先逛逛', sub: '无需注册，即刻进入商城' },
}

const tab = ref<TabKey>('login')
const tabIndex = computed(() => Math.max(0, TABS.indexOf(tab.value)))

const panelsEl = ref<HTMLElement | null>(null)
const dollsEl = ref<HTMLElement | null>(null)

// 三个面板高度不同，容器高度要按当前面板实测，否则切换时的高度过渡会跳
function measure() {
  const wrap = panelsEl.value
  const active = wrap?.querySelector<HTMLElement>('.panel.is-active')
  if (wrap && active) wrap.style.height = `${active.offsetHeight}px`
}

function setTab(name: TabKey) {
  tab.value = name
  nextTick(() => requestAnimationFrame(measure))
}

watch(tab, () => nextTick(() => requestAnimationFrame(measure)))

/* ---------------- 表单状态 ---------------- */
const loginForm = reactive({ user: '', pwd: '', remember: true })
const regForm = reactive({ name: '', phone: '', code: '', pwd: '', agree: false })
const showPwd = reactive({ lg: false, rg: false })
const loading = reactive({ login: false, register: false, guest: false })

function togglePwd(key: 'lg' | 'rg') {
  showPwd[key] = !showPwd[key]
}

/* ---------------- 轻提示 ---------------- */
const toastVisible = ref(false)
const toastText = ref('操作成功')
let toastTimer = 0

function toast(msg: string) {
  toastText.value = msg
  toastVisible.value = true
  clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => {
    toastVisible.value = false
  }, 2400)
}

function hint(msg: string) {
  toast(msg)
}

/* ---------------- 短信验证码 ---------------- */
const PHONE_RE = /^1[3-9]\d{9}$/
const codeLeft = ref(0)
const codeSent = ref(false)
let codeTimer = 0

const codeText = computed(() => {
  if (codeLeft.value > 0) return `${codeLeft.value}s 后重发`
  return codeSent.value ? '重新获取' : '获取验证码'
})

function sendCode() {
  if (codeLeft.value > 0) return
  if (!PHONE_RE.test(regForm.phone.trim())) {
    toast('请先填写正确的手机号')
    return
  }
  codeSent.value = true
  codeLeft.value = 60
  toast('验证码已发送至 ' + regForm.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2'))
  clearInterval(codeTimer)
  codeTimer = window.setInterval(() => {
    codeLeft.value -= 1
    if (codeLeft.value <= 0) clearInterval(codeTimer)
  }, 1000)
}

/* ---------------- 提交（当前为演示态） ---------------- */
function demoSubmit(key: 'login' | 'register' | 'guest', msg: string) {
  if (loading[key]) return
  loading[key] = true
  window.setTimeout(() => {
    loading[key] = false
    toast(msg)
    // TODO: 接真实接口后改成
    //   await request('/api/v1/c/auth/login', { method: 'POST', data: loginForm })
    //   userStore.setToken(res.accessToken)
    //   router.push('/')
  }, 1100)
}

function onLogin() {
  if (!loginForm.user.trim()) {
    toast('请输入邮箱地址或手机号')
    return
  }
  if (loginForm.pwd.length < 6) {
    toast('密码至少 6 位')
    return
  }
  demoSubmit('login', '登录成功，正在进入商城…')
}

function onRegister() {
  if (!regForm.name.trim()) {
    toast('请填写昵称')
    return
  }
  if (!PHONE_RE.test(regForm.phone.trim())) {
    toast('请填写正确的手机号')
    return
  }
  if (regForm.code.length !== 6) {
    toast('请输入 6 位验证码')
    return
  }
  if (regForm.pwd.length < 8) {
    toast('密码至少 8 位')
    return
  }
  if (!regForm.agree) {
    toast('请先同意用户协议')
    return
  }
  demoSubmit('register', '注册成功，欢迎加入 OWL')
}

function onGuest() {
  demoSubmit('guest', '正在进入商城…')
}

/* ---------------- 生命周期 ---------------- */
let disposeDoll: (() => void) | null = null

function onResize() {
  const wrap = panelsEl.value
  if (!wrap) return
  wrap.style.transition = 'none'
  measure()
  requestAnimationFrame(() => {
    wrap.style.transition = ''
  })
}

onMounted(() => {
  measure()
  onResize()
  if (dollsEl.value) disposeDoll = initDollScene(dollsEl.value)
  document.fonts?.ready.then(measure)
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  clearInterval(codeTimer)
  clearTimeout(toastTimer)
  disposeDoll?.()
})
</script>
