<script setup>
/**
 * 水墨意境背景层：远山 / 墨竹 / 飞鸟 / 淡墨晕染
 * 纯装饰，不接收交互（pointer-events: none），fixed 铺满视口，位于内容之下。
 */
</script>

<template>
  <div class="ink-bg" aria-hidden="true">
    <svg viewBox="0 0 1440 900" preserveAspectRatio="xMidYMax slice">
      <defs>
        <filter id="inkSoft" x="-30%" y="-30%" width="160%" height="160%">
          <feGaussianBlur stdDeviation="16" />
        </filter>
        <filter id="inkEdge" x="-20%" y="-20%" width="140%" height="140%">
          <feGaussianBlur stdDeviation="3" />
        </filter>
        <linearGradient id="mtnFar" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#1f1d1a" stop-opacity=".13" />
          <stop offset="100%" stop-color="#1f1d1a" stop-opacity=".01" />
        </linearGradient>
        <linearGradient id="mtnMid" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#1f1d1a" stop-opacity=".2" />
          <stop offset="100%" stop-color="#1f1d1a" stop-opacity=".02" />
        </linearGradient>
        <linearGradient id="mtnNear" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#1f1d1a" stop-opacity=".3" />
          <stop offset="100%" stop-color="#1f1d1a" stop-opacity=".04" />
        </linearGradient>
      </defs>

      <!-- 天际淡墨晕染 -->
      <ellipse cx="230" cy="150" rx="300" ry="120" fill="#1f1d1a" opacity=".05" filter="url(#inkSoft)" />
      <ellipse cx="1210" cy="230" rx="260" ry="110" fill="#1f1d1a" opacity=".045" filter="url(#inkSoft)" />
      <ellipse cx="760" cy="90" rx="420" ry="90" fill="#1f1d1a" opacity=".03" filter="url(#inkSoft)" />

      <!-- 远山（三层，越近越浓） -->
      <path
        fill="url(#mtnFar)"
        d="M0 660 L95 596 L168 632 L268 540 L352 606 L452 528 L548 600 L640 556 L742 616 L836 566 L944 622 L1046 556 L1150 610 L1252 552 L1352 604 L1440 566 L1440 900 L0 900 Z"
      />
      <path
        fill="url(#mtnMid)"
        d="M0 742 L120 690 L212 726 L318 648 L410 714 L520 660 L620 720 L724 678 L830 736 L940 686 L1052 742 L1158 694 L1266 748 L1368 706 L1440 738 L1440 900 L0 900 Z"
      />
      <path
        fill="url(#mtnNear)"
        d="M0 820 L140 776 L246 812 L360 756 L468 806 L590 766 L700 812 L812 772 L926 820 L1040 780 L1152 822 L1268 786 L1372 824 L1440 800 L1440 900 L0 900 Z"
      />

      <!-- 水面：两笔淡墨横扫 -->
      <path d="M120 858 q180 -10 360 0 t400 -6" stroke="#1f1d1a" stroke-opacity=".1" stroke-width="2" fill="none" filter="url(#inkEdge)" />
      <path d="M420 880 q200 -8 400 0 t380 -4" stroke="#1f1d1a" stroke-opacity=".07" stroke-width="2" fill="none" filter="url(#inkEdge)" />

      <!-- 墨竹（右下） -->
      <g stroke="#1f1d1a" fill="none" opacity=".22">
        <path d="M1300 900 C1290 780 1306 660 1298 540 C1292 440 1304 350 1298 268" stroke-width="4" stroke-linecap="round" />
        <path d="M1298 700 l-3 -34 M1300 596 l-3 -32 M1299 486 l-3 -30 M1299 384 l-3 -28" stroke-width="3" />
        <path d="M1352 900 C1348 800 1360 706 1355 622" stroke-width="3" stroke-linecap="round" opacity=".7" />
        <path d="M1355 786 l-3 -26 M1356 700 l-3 -24" stroke-width="2.5" opacity=".7" />
      </g>
      <g fill="#1f1d1a" opacity=".26">
        <path d="M1297 470 q-52 -18 -74 12 q40 16 74 -12 Z" />
        <path d="M1300 428 q56 -26 86 6 q-46 20 -86 -6 Z" />
        <path d="M1299 356 q-46 -14 -64 14 q34 12 64 -14 Z" />
        <path d="M1298 306 q50 -22 76 6 q-40 18 -76 -6 Z" />
        <path d="M1354 706 q-42 -14 -58 12 q30 10 58 -12 Z" />
        <path d="M1355 660 q44 -18 66 6 q-34 14 -66 -6 Z" />
      </g>

      <!-- 墨竹（左上，更淡） -->
      <g stroke="#1f1d1a" fill="none" opacity=".1">
        <path d="M52 300 C46 240 58 190 52 140" stroke-width="3" stroke-linecap="round" />
        <path d="M52 232 q40 -18 60 6 q-32 14 -60 -6 Z" fill="#1f1d1a" opacity=".5" />
        <path d="M52 186 q-38 -14 -54 10 q28 10 54 -10 Z" fill="#1f1d1a" opacity=".5" />
      </g>

      <!-- 飞鸟（三笔） -->
      <g stroke="#1f1d1a" stroke-opacity=".28" stroke-width="2" fill="none" stroke-linecap="round">
        <path d="M300 210 q10 -9 20 0 q10 -9 20 0" />
        <path d="M356 176 q8 -7 16 0 q8 -7 16 0" />
        <path d="M268 258 q7 -6 14 0 q7 -6 14 0" />
      </g>

      <!-- 几点墨迹 -->
      <circle cx="1080" cy="150" r="4" fill="#1f1d1a" opacity=".16" />
      <circle cx="1112" cy="176" r="2.5" fill="#1f1d1a" opacity=".13" />
      <circle cx="1046" cy="188" r="3" fill="#1f1d1a" opacity=".1" />
      <circle cx="158" cy="640" r="3.5" fill="#1f1d1a" opacity=".12" />
    </svg>
  </div>
</template>

<style scoped>
.ink-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}
.ink-bg svg {
  width: 100%;
  height: 100%;
  display: block;
}
</style>
