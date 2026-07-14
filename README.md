# Task Management Backend

REST API quản lý công việc cá nhân được xây dựng bằng Spring Boot. Hệ thống hỗ trợ đăng ký, đăng nhập bằng JWT, làm mới token và CRUD công việc theo từng người dùng.

## Chức năng

- Đăng ký và đăng nhập tài khoản.
- Xác thực bằng Access Token và Refresh Token.
- Làm mới Access Token, thu hồi Refresh Token khi đăng xuất.
- Tạo, xem, cập nhật, hoàn thành và xóa công việc.
- Mỗi người dùng chỉ được thao tác với công việc của mình.
- Validation dữ liệu, Global Exception Handler và AOP Logging.

## Công nghệ

- Java 25
- Spring Boot 4.1.0
- Spring Web MVC
- Spring Data JPA
- Spring Security
- MySQL
- JWT (JJWT)
- Maven

## Cấu trúc chính

```text
controller  → Nhận request và trả response
service     → Xử lý nghiệp vụ
repository  → Làm việc với cơ sở dữ liệu
entity      → User, Task, RefreshToken
dto         → Dữ liệu request/response
security    → JWT Filter, JwtService, CurrentUserService
exception   → Xử lý lỗi tập trung
aspect      → Logging bằng AOP
```

## Cài đặt và chạy

### 1. Cấu hình MySQL

Tạo file `.env` từ `.env.example`:

```env
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
```

Có thể cấu hình thêm JWT secret:

```env
JWT_SECRET=your_secure_jwt_secret
```

> Không commit file `.env` lên GitHub.

Ứng dụng sử dụng database `taskdb` và có thể tự tạo database nếu tài khoản MySQL có đủ quyền.

### 2. Chạy ứng dụng

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Ứng dụng chạy tại:

```text
http://localhost:8080
```

Kiểm tra trạng thái:

```http
GET /api/health
```

## API chính

### Authentication

| Method | Endpoint | Mô tả | Xác thực |
|---|---|---|---|
| POST | `/api/auth/register` | Đăng ký | Không |
| POST | `/api/auth/login` | Đăng nhập | Không |
| POST | `/api/auth/refresh` | Làm mới Access Token | Không |
| POST | `/api/auth/logout` | Thu hồi Refresh Token | Bearer Token |

### Task

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/tasks` | Lấy danh sách công việc |
| GET | `/api/tasks/{id}` | Lấy chi tiết công việc |
| POST | `/api/tasks` | Tạo công việc |
| PUT | `/api/tasks/{id}` | Cập nhật công việc |
| PATCH | `/api/tasks/{id}/complete` | Đánh dấu hoàn thành |
| DELETE | `/api/tasks/{id}` | Xóa công việc |

Các API Task yêu cầu header:

```http
Authorization: Bearer <access_token>
```

## Request mẫu

Đăng ký:

```json
{
  "username": "demo",
  "password": "123456"
}
```

Tạo công việc:

```json
{
  "title": "Học Spring Boot",
  "description": "Hoàn thành REST API quản lý Task"
}
```

## Chạy kiểm thử

```bash
./mvnw test
```

## Mục đích

Project được xây dựng để thực hành kiến trúc Spring Boot, REST API, Dependency Injection, DTO, Validation, JPA, Spring Security, JWT, Refresh Token, Global Exception Handling và AOP Logging.
