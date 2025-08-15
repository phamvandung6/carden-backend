# Luồng Authentication - Carden API

## Tổng quan

Hệ thống sử dụng JWT (JSON Web Token) để xác thực người dùng. Luồng bao gồm đăng ký, đăng nhập, và quản lý phiên làm việc.

## 1. Đăng ký tài khoản mới

### Endpoint: `POST /v1/auth/register`

#### Request Body:
```json
{
  "username": "testuser123",
  "email": "testuser@example.com",
  "password": "SecurePass123!",
  "displayName": "Test User"
}
```

#### Response (201 Created):
```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "user": {
      "id": 1,
      "username": "testuser123",
      "email": "testuser@example.com",
      "displayName": "Test User",
      "role": "USER",
      "isActive": true,
      "emailVerified": false
    }
  }
}
```

#### Validation Rules:
- **username**: 3-50 ký tự, chỉ alphanumeric và underscore
- **email**: Format email hợp lệ
- **password**: Tối thiểu 8 ký tự
- **displayName**: Tùy chọn, tối đa 100 ký tự

## 2. Đăng nhập

### Endpoint: `POST /v1/auth/login`

#### Request Body (với username):
```json
{
  "usernameOrEmail": "testuser123",
  "password": "SecurePass123!"
}
```

#### Request Body (với email):
```json
{
  "usernameOrEmail": "testuser@example.com",
  "password": "SecurePass123!"
}
```

#### Response (200 OK):
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJ0eXBlIjoiYWNjZXNzIiwiYXV0aG9yaXRpZXMiOlsiUk9MRV9VU0VSIl0sInN1YiI6InRlc3R1c2VyMTIzIiwiaWF0IjoxNzU1MTgzNjc3LCJleHAiOjE3NTUxOTA4Nzd9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "user": {
      "id": 1,
      "username": "testuser123",
      "email": "testuser@example.com",
      "displayName": "Test User",
      "role": "USER",
      "isActive": true,
      "emailVerified": false,
      "lastLogin": "2024-01-15T10:30:00Z"
    }
  }
}
```

## 3. Đăng xuất

### Endpoint: `POST /v1/auth/logout`

#### Headers:
```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

#### Response (200 OK):
```json
{
  "success": true,
  "message": "Successfully logged out"
}
```

## 4. Sử dụng Token để truy cập API

### Headers cho các API có bảo mật:
```
Authorization: Bearer <access_token>
Content-Type: application/json
```

### Ví dụ: Lấy profile user
```bash
curl -X GET "http://localhost:8080/v1/users/me" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

## 5. Error Responses

### 400 Bad Request - Sai thông tin đăng nhập:
```json
{
  "success": false,
  "message": "Invalid credentials",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 400 Bad Request - Email/Username đã tồn tại:
```json
{
  "success": false,
  "message": "Username already exists",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 422 Validation Error:
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": [
    {
      "field": "password",
      "message": "Password must be at least 8 characters"
    },
    {
      "field": "email",
      "message": "Invalid email format"
    }
  ],
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 401 Unauthorized - Token không hợp lệ:
```json
{
  "success": false,
  "message": "Access denied: Invalid or expired token",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## 6. Test Cases cho Swagger UI

### Test Case 1: Đăng ký thành công
```json
{
  "username": "johndoe2024",
  "email": "john.doe@example.com",
  "password": "MySecurePassword123!",
  "displayName": "John Doe"
}
```

### Test Case 2: Đăng ký với email không hợp lệ
```json
{
  "username": "invaliduser",
  "email": "not-an-email",
  "password": "Password123!",
  "displayName": "Invalid User"
}
```

### Test Case 3: Đăng nhập với username
```json
{
  "usernameOrEmail": "johndoe2024",
  "password": "MySecurePassword123!"
}
```

### Test Case 4: Đăng nhập với email
```json
{
  "usernameOrEmail": "john.doe@example.com",
  "password": "MySecurePassword123!"
}
```

### Test Case 5: Đăng nhập với sai mật khẩu
```json
{
  "usernameOrEmail": "johndoe2024",
  "password": "WrongPassword"
}
```

## 7. Các lưu ý quan trọng

1. **Token Expiry**: Access token có thời hạn 2 tiếng (7200 giây)
2. **Role-based Access**: Hệ thống có 2 role chính: `USER` và `ADMIN`
3. **Email Verification**: Sau khi đăng ký, `emailVerified` sẽ là `false`
4. **Account Status**: `isActive` có thể được admin set thành `false` để vô hiệu hóa tài khoản
5. **Password Security**: Mật khẩu được hash bằng BCrypt với strength 12

## 8. Workflow tổng thể

```mermaid
sequenceDiagram
    participant Client
    participant API
    participant Database
    
    Client->>API: POST /v1/auth/register
    API->>Database: Check username/email exists
    Database-->>API: Not exists
    API->>Database: Create user (password hashed)
    Database-->>API: User created
    API->>API: Generate JWT tokens
    API-->>Client: Return tokens + user info
    
    Client->>API: POST /v1/auth/login
    API->>Database: Validate credentials
    Database-->>API: User found, password valid
    API->>API: Generate JWT tokens
    API->>Database: Update last_login
    API-->>Client: Return tokens + user info
    
    Client->>API: Any protected endpoint
    Note over Client,API: Include Authorization header
    API->>API: Validate JWT token
    API-->>Client: Return requested data
```
