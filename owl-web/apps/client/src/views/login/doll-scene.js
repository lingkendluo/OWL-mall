// 木头人动画引擎：纯 DOM 驱动，刻意不改写成 Vue 响应式。
// 理由：它操作的是 transform / CSS 变量 / class 的逐帧状态，走响应式只会让每帧都触发 diff，性能更差。
// 与 Vue 的边界只有两个：宿主要提供 .dolls 容器；离开页面时调用返回的 dispose()。
export function initDollScene(dollsWrap) {
  let lineTimer = 0
  let bootTimer = 0
  /* ================= 木头人 ================= */
  var dollEls = Array.prototype.slice.call(dollsWrap.querySelectorAll(".doll"));
  var bubbles = Array.prototype.slice.call(dollsWrap.querySelectorAll("[data-bubble]"));
  var texts = Array.prototype.slice.call(dollsWrap.querySelectorAll("[data-text]"));

  /* 对话：偏幽默，一轮一句 */
  var LINES = [
    ["春江花月何时了～", "你先把购物车清了吧 🥲"],
    ["我昨天瘦了两斤！", "那你现在搬得动快递了吗 🤨"],
    ["这个链接怎么点不开", "因为你没付钱 💸"],
    ["我决定今天开始存钱", "这是第 108 次决定了吧"],
    ["你猜我买了啥", "又是你不需要的东西 😐"],
    ["快递说放门口了", "你家门口现在有座山 ⛰️"],
    ["我在减肥", "所以点了两份轻食？🥗"],
    ["这双鞋好看吗", "上个月你买了双一模一样的 👟"]
  ];

  var SHAPES = ["shape-round", "shape-spike", "shape-blob", "shape-square"];

  var lineIdx = 0;

  function replayJelly(el) {
    var inner = el.querySelector(".bubble-in");
    if (!inner) return;
    inner.classList.remove("jelly");
    void inner.offsetWidth;
    inner.classList.add("jelly");
  }

  function applyLine(i, animate) {
    var pair = LINES[i % LINES.length];

    function paint() {
      texts[0].textContent = pair[0];
      texts[1].textContent = pair[1];

      bubbles.forEach(function (b, k) {
        SHAPES.forEach(function (s) { b.classList.remove(s); });
        b.classList.add(SHAPES[(i + k) % SHAPES.length]);
        b.classList.add("show");
        replayJelly(b);
      });
    }

    if (!animate) { paint(); return; }

    bubbles.forEach(function (b) { b.classList.remove("show"); });
    setTimeout(paint, 260);
  }

  bootTimer = setTimeout(function () { applyLine(0, false); }, 1200);

  lineTimer = setInterval(function () {
    lineIdx += 1;
    applyLine(lineIdx, true);
  }, 8000);

  /* 鼠标距离：近则脸红、瞳孔放大、气泡收起 */
  var NEAR_DIST = 165;

  /** 量出每只小人的「身体中心」（纯读，不写样式） */
  function centers() {
    return dollEls.map(function (el) {
      var r = el.getBoundingClientRect();
      return { cx: r.left + r.width / 2, cy: r.top + r.height * 0.4 };
    });
  }

  /** 按已量好的中心点写「靠近 / 远离」（纯写，不读布局） */
  function applyNear(x, y, list) {
    var anyNear = false;

    dollEls.forEach(function (el, i) {
      var c = list[i];
      var dx = x - c.cx;
      var dy = y - c.cy;
      var near = Math.sqrt(dx * dx + dy * dy) < NEAR_DIST;
      el.classList.toggle("is-near", near);
      if (near) anyNear = true;
    });

    dollsWrap.classList.toggle("is-muted", anyNear);
  }

  /* ---------------- 眼睛：一直盯着鼠标 ---------------- */
  var MAX_LOOK = 4.6;
  var forceLeftUntil = 0;
  var lookLock = {};          /* 锁定某个小人的视线：{ 角色: {x,y} } */
  var lastMouse = { x: innerWidth / 2, y: innerHeight / 2 };

  var brown = dollEls[0];     /* data-doll=a 棕黄 */
  var pink = dollEls[1];      /* data-doll=b 粉白 */

  function eyeCenter(el) {
    var sc = el.querySelectorAll(".face-rest .sclera, .face-angry .sclera");
    if (!sc.length) return null;
    var a = sc[0].getBoundingClientRect();
    var b = sc[sc.length - 1].getBoundingClientRect();
    return {
      x: (a.left + a.width / 2 + b.left + b.width / 2) / 2,
      y: (a.top + a.height / 2 + b.top + b.height / 2) / 2
    };
  }

  /** 按已量好的眼睛位置写瞳孔偏移（纯写，不读布局） */
  function applyLook(x, y, eyes) {
    dollEls.forEach(function (el, i) {
      var lock = lookLock[el.dataset.doll];
      var tx = lock ? lock.x : x;
      var ty = lock ? lock.y : y;
      var c = eyes[i];
      if (!c) return;
      var dx = tx - c.x;
      var dy = ty - c.y;
      var len = Math.sqrt(dx * dx + dy * dy) || 1;
      var k = MAX_LOOK * Math.min(1, len / 120);
      el.style.setProperty("--ex", (dx / len * k).toFixed(2) + "px");
      el.style.setProperty("--ey", (dy / len * k).toFixed(2) + "px");
    });
  }

  function lookAt(x, y) {
    applyLook(x, y, dollEls.map(eyeCenter));
  }

  function lookLeft() {
    dollEls.forEach(function (el) {
      el.style.setProperty("--ex", (-MAX_LOOK).toFixed(2) + "px");
      el.style.setProperty("--ey", "0px");
    });
  }

  /* ---------------- 走位 ---------------- */
  var cur = { a: { x: 0, y: 0 }, b: { x: 0, y: 0 } };
  var walkTimers = {};
  var rollDeg = { a: 0, b: 0 };
  var scene = "idle";                 /* idle | walking | perched | act */
  var HEAD_TOP = 8 / 110;             /* SVG 里头顶的位置 */
  var FEET = 96 / 110;                /* 脚底 */

  function setFace(el, name) {
    if (el.dataset.face !== name) el.dataset.face = name;
  }

  function setBall(el, on) {
    el.dataset.state = on ? "3" : "0";
  }

  function place(el, x, y, dur) {
    var k = el.dataset.doll;
    cur[k].x = x;
    cur[k].y = y;
    el.style.setProperty("--walk", (dur || 0) + "ms");
    el.style.setProperty("--wx", x.toFixed(2) + "px");
    el.style.setProperty("--wy", y.toFixed(2) + "px");
    clearTimeout(walkTimers[k]);
    if (dur) {
      el.classList.add("is-walking");
      walkTimers[k] = setTimeout(function () {
        el.classList.remove("is-walking");
      }, dur + 60);
    } else {
      el.classList.remove("is-walking");
    }
  }

  function hop(el, tx, ty, dur, height, done) {
    var k = el.dataset.doll;
    var sx = cur[k].x;
    var sy = cur[k].y;
    el.style.setProperty("--walk", "0ms");
    el.classList.remove("is-walking");
    el.classList.add("is-hopping");
    var t0 = performance.now();
    requestAnimationFrame(function step(t) {
      var p = Math.min(1, (t - t0) / dur);
      var x = sx + (tx - sx) * p;
      var y = sy + (ty - sy) * p - Math.sin(Math.PI * p) * height;
      cur[k].x = x;
      cur[k].y = y;
      el.style.setProperty("--wx", x.toFixed(2) + "px");
      el.style.setProperty("--wy", y.toFixed(2) + "px");
      if (p < 1) requestAnimationFrame(step);
      else {
        el.classList.remove("is-hopping");
        if (done) done();
      }
    });
  }

  function roll(el, tx, ty, dur, turns, done) {
    var k = el.dataset.doll;
    var sx = cur[k].x;
    var sy = cur[k].y;
    var sr = rollDeg[k];
    el.style.setProperty("--walk", "0ms");
    var t0 = performance.now();
    requestAnimationFrame(function step(t) {
      var p = Math.min(1, (t - t0) / dur);
      var e = p * p * (3 - 2 * p);
      var x = sx + (tx - sx) * e;
      var y = sy + (ty - sy) * e;
      cur[k].x = x;
      cur[k].y = y;
      rollDeg[k] = sr + turns * 360 * e;
      el.style.setProperty("--wx", x.toFixed(2) + "px");
      el.style.setProperty("--wy", y.toFixed(2) + "px");
      el.style.setProperty("--roll", rollDeg[k].toFixed(1) + "deg");
      if (p < 1) requestAnimationFrame(step);
      else if (done) done();
    });
  }

  /* 点密码框：粉色走过去，棕色跟着；站定后棕色跳上头顶 */
  function sceneToForm() {
    if (scene !== "idle") return;

    var card = document.querySelector(".card");
    var pwd = document.getElementById("lgPwd");
    if (!card || !pwd) return;

    scene = "walking";
    dollsWrap.classList.add("is-scene");

    var cardLeft = card.getBoundingClientRect().left;
    var pwdBox = pwd.getBoundingClientRect();
    var pwdMid = pwdBox.top + pwdBox.height / 2;

    var pR = pink.getBoundingClientRect();
    var bR0 = brown.getBoundingClientRect();

    /* 粉色：右边缘贴住卡片左侧，身体中线对准密码框 */
    var pdx = cur.b.x + (cardLeft - 6 - pR.right);
    var pdy = cur.b.y + (pwdMid - (pR.top + pR.height / 2));
    pdy = Math.max(-150, Math.min(0, pdy));

    /* 棕色：贴着粉色身后站，只留 4px 缝（注意要按“绝对位置”算，不能直接改 deltas） */
    var pinkFinalLeft = pR.left + pdx;
    var brownBaseLeft = bR0.left - cur.a.x;
    var bdx = (pinkFinalLeft - 110) - brownBaseLeft;
    var bdy = pdy;

    place(pink, pdx, pdy, 4000);
    setTimeout(function () {
      place(brown, bdx, bdy, 3100);
    }, 700);

    /* 都站定了 → 棕色跳上粉色头顶 */
    setTimeout(function () {
      var p2 = pink.getBoundingClientRect();
      var b2 = brown.getBoundingClientRect();
      var tx = cur.a.x + (p2.left - b2.left);
      var ty = cur.a.y + ((p2.top + p2.height * HEAD_TOP) - (b2.top + b2.height * FEET)) + 3;

      setFace(brown, "laugh");
      hop(brown, tx, ty, 760, 64, function () {
        setFace(pink, "angry");
        var br = brown.getBoundingClientRect();
        lookLock.b = { x: br.left + br.width / 2, y: br.top + br.height * 0.6 };
        lookAt(lastMouse.x, lastMouse.y);
        scene = "perched";
      });
    }, 5100);
  }

  /* 点小眼睛：棕色跳下来 → 粉色踢 → 棕色捂屁股跳 → 变球滚回去 → 粉色走回去 */
  function sceneEye() {
    if (scene !== "perched") {
      /* 没走到位时，先保留“眼睛看向左边”的小反馈 */
      forceLeftUntil = Date.now() + 1200;
      lookLeft();
      setTimeout(function () { lookAt(lastMouse.x, lastMouse.y); }, 1280);
      return;
    }

    scene = "act";

    var pR = pink.getBoundingClientRect();
    var bR = brown.getBoundingClientRect();

    /* 1. 从头顶跳下来，落在粉色左边挨着 */
    var tx = cur.a.x + (pR.left - 110 - bR.left);
    var ty = cur.a.y + (pR.bottom - bR.bottom);

    setFace(brown, "rest");
    delete lookLock.b;

    hop(brown, tx, ty, 600, 46, function () {
      /* 2. 粉色踢一脚 */
      pink.classList.add("is-kicking");
      setTimeout(function () {
        brown.classList.add("is-hit");
        setFace(brown, "pain");
      }, 170);
      setTimeout(function () {
        pink.classList.remove("is-kicking");
        brown.classList.remove("is-hit");
      }, 660);

      /* 3. 痛得跳一下 */
      setTimeout(function () {
        hop(brown, cur.a.x, cur.a.y - 30, 300, 26, function () {
          hop(brown, cur.a.x, cur.a.y + 30, 260, 12, function () {
            /* 4. 落地变成纸团，滚回原位 */
            setBall(brown, true);

            /* 粉色看着它滚走，再回原位 */
            setTimeout(function () {
              setFace(pink, "rest");
              place(pink, 0, 0, 1600);
            }, 240);

            setTimeout(function () {
              roll(brown, 0, 0, 1400, 2, function () {
                setBall(brown, false);
                el_unfold(brown);
                rollDeg.a = 0;
                brown.style.setProperty("--roll", "0deg");
                place(brown, 0, 0, 0);
                scene = "idle";
                dollsWrap.classList.remove("is-scene");
              });
            }, 780);
          });
        });
      }, 900);
    });
  }

  /* 从纸团变回来时弹一下 */
  function el_unfold(el) {
    setFace(el, "rest");
    el.classList.remove("is-unfolding");
    void el.offsetWidth;
    el.classList.add("is-unfolding");
    setTimeout(function () { el.classList.remove("is-unfolding"); }, 620);
  }

  /**
   * 鼠标移动：合并到每帧一次，且「先读完再写」。
   *
   * 原来每个 mousemove 都要 getBoundingClientRect 六次（两只小人的身体 + 四只眼睛），
   * 中间还夹着 class / CSS 变量的写入 —— 读-写-读交替会触发强制同步布局。
   * 高回报率鼠标一秒能发两百多条 mousemove，登录页「一动鼠标就发涩」正是这么来的。
   * 现在每帧最多处理一次，且把布局读取全部提到写样式之前。
   */
  var pendingMove = 0;

  function flushMove() {
    pendingMove = 0;
    var x = lastMouse.x;
    var y = lastMouse.y;

    var box = centers();
    var eyes = dollEls.map(eyeCenter);

    applyNear(x, y, box);
    if (Date.now() < forceLeftUntil) lookLeft();
    else applyLook(x, y, eyes);
  }

  function onMove(e) {
    lastMouse.x = e.clientX;
    lastMouse.y = e.clientY;
    if (pendingMove) return;
    pendingMove = requestAnimationFrame(flushMove);
  }
  document.addEventListener("mousemove", onMove, { passive: true })

  function onLeave() {
    dollEls.forEach(function (el) {
      el.classList.remove("is-near");
      el.style.setProperty("--ex", "0px");
      el.style.setProperty("--ey", "0px");
    });
    dollsWrap.classList.remove("is-muted")
  }
  document.documentElement.addEventListener("mouseleave", onLeave)

  /* 点密码框 → 小人过来 */
  var pwdInput = document.getElementById("lgPwd");
  if (pwdInput) {
    pwdInput.addEventListener("focus", sceneToForm);
  }

  /* 点小眼睛 → 那出戏 */
  Array.prototype.forEach.call(document.querySelectorAll("[data-eye]"), function (btn) {
    btn.addEventListener("click", sceneEye);
  });

  /* ---------------- 点击：大笑 → 委屈 → 揉成纸团 → 复原 ---------------- */
  function cycleDoll(el) {
    if (scene !== "idle") return;

    var s = Number(el.dataset.stage || 0);

    if (s === 3) {
      el.dataset.stage = 0;
      setBall(el, false);
      el.classList.remove("is-unfolding");
      void el.offsetWidth;
      el.classList.add("is-unfolding");
      setFace(el, "rest");
      setTimeout(function () { el.classList.remove("is-unfolding"); }, 620);
      return;
    }

    s += 1;
    el.dataset.stage = s;

    if (s === 1) setFace(el, "laugh");
    else if (s === 2) setFace(el, "sad");
    else if (s === 3) {
      setFace(el, "rest");
      el.classList.add("is-unfolding");
      el.classList.remove("is-unfolding");
      setBall(el, true);
    }
  }

  dollEls.forEach(function (el) {
    el.addEventListener("click", function () { cycleDoll(el); });
    el.addEventListener("keydown", function (e) {
      if (e.key === "Enter" || e.key === " ") {
        e.preventDefault();
        cycleDoll(el);
      }
    });
  });
  return function dispose() {
    clearInterval(lineTimer)
    clearTimeout(bootTimer)
    cancelAnimationFrame(pendingMove)
    document.removeEventListener('mousemove', onMove)
    document.documentElement.removeEventListener('mouseleave', onLeave)
  }
}
