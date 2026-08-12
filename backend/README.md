# KLGD Backend

Backend Spring Boot cho hệ thống quản lý và tính khối lượng giảng dạy.

## Cấu trúc chính

```text
src/main/java/edu/tlu/klgd
├── TeachingManagementApplication.java
├── application/dto
├── domain/common
├── domain/entity
├── domain/repository
├── domain/service
├── infracstructure/config
├── infracstructure/exception
├── infracstructure/persistence/jpa
├── infracstructure/persistence/mapper
├── infracstructure/security
└── presentation/controller
```

## Chạy backend local

Chạy PostgreSQL ở thư mục gốc project:

```powershell
docker compose up -d postgres
```

Chạy backend:

```powershell
mvn test
mvn spring-boot:run
```

## Chạy backend bằng Docker

Ở thư mục gốc project:

```powershell
Copy-Item .env.example .env
docker compose -f docker-compose.backend.yml up --build -d
```

Kiểm tra:

- Health: <http://localhost:8080/actuator/health>
- Swagger UI: <http://localhost:8080/swagger-ui.html>

## Biến môi trường

- `DB_URL`: JDBC URL, ví dụ `jdbc:postgresql://postgres:5432/klgd`
- `DB_USERNAME`: tài khoản CSDL
- `DB_PASSWORD`: mật khẩu CSDL
- `SERVER_PORT`: port backend trong container, mặc định `8080`
- `CORS_ALLOWED_ORIGINS`: danh sách origin frontend được phép gọi API
- `JWT_SECRET`: khóa ký JWT
- `JWT_EXPIRATION_MINUTES`: thời gian hết hạn JWT theo phút

## API chính

- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/dashboard/overview`
- `GET /api/workloads/teachers`
- `GET /api/workloads/departments`
- `GET /api/workloads/rules`
- `GET /api/import-batches`
- `POST /api/import-batches`
- `GET /api/import-batches/{id}/records`
- `POST /api/import-batches/{id}/calculate`
- `GET /api/reports/workload.xlsx`
