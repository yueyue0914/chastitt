# 月锁 Vue3 前端

## 本地开发

先启动 Java 后端（默认 `8080`），再：

```bash
cd VUE
npm install
npm run dev
```

浏览器打开 Vite 提示的地址（默认 `http://localhost:5173`）。  
开发时 `/api` 会代理到后端。

## 生产构建

```bash
npm run build
```

把 `dist/` 交给 Nginx 静态托管，并把 `/api` 反代到 Java：

```nginx
location /api/ {
  proxy_pass http://127.0.0.1:8080/api/;
}
location / {
  root /www/wwwroot/yue-lock-vue/dist;
  try_files $uri /index.html;
}
```

若前后端不同域，构建前设置：

```bash
# .env.production
VITE_API_BASE=http://你的后端地址:8080
```
