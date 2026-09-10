# 月锁 Java 后端

## 本地运行

```bash
cd JAVA
mvn spring-boot:run
```

默认：
- 端口 `8080`
- 内嵌 H2 文件库 `./data/yuelock`
- JWT 鉴权（`Authorization: Bearer <token>`）

## 生产 Postgres

```bash
export SPRING_PROFILES_ACTIVE=postgres
export DATABASE_URL='jdbc:postgresql://127.0.0.1:5432/yuelock'
export DB_USER=yuelock
export DB_PASSWORD=你的密码
# 同时改 application.yml 里 yuelock.jwt.secret
mvn -DskipTests package
java -jar target/yue-lock-1.0.0.jar
```

## 主要 API

- `POST /api/auth/sign-up` `{email,password,name?}`
- `POST /api/auth/sign-in` `{email,password}`
- `GET/PUT /api/me/profile`
- `POST /api/locks` 创建锁定（需登录）
- `GET /api/locks/by-wearer/{token}` / `by-keyholder/{token}`
- 钥匙端：加时/减时/冻结/最低锁/拍照/任务/开锁
- `POST /api/locks/claim-keyholder` 认领钥匙

前端见 `../VUE`。
