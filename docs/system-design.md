# System Design

## 1. Functional Design

The system uses a separated frontend and backend architecture and is divided into a user side and an admin side.

### 1.1 User Side

1. Profile module: registration, login, profile view, and profile update.
2. Lost-item module: submit lost-item title, time, location, description, contact details, and images.
3. Found-item module: submit found-item details, place, time, pickup method, and images.
4. Search module: search lost and found records by keyword, location, time, category, and type.
5. Message module: receive review notifications, match notices, and claim-related messages.

### 1.2 Admin Side

1. Review module: approve, reject, or remove lost and found posts.
2. User module: view user information and manage user status or role.
3. Dashboard module: view publishing counts, approval rates, and category distribution.
4. System module: maintain categories, announcements, and platform configuration.
5. Log module: review operation logs for auditing.

### 1.3 Data Flow

- The frontend sends requests for login, publishing, search, claim, and admin operations.
- The backend validates parameters, checks permissions, executes business logic, and reads or writes the database.
- Review results, claim events, and match events are converted into message notifications.
- Python AI services are called by the backend over HTTP for smart matching and spatiotemporal prediction.

## 2. Database Design

### 2.1 Core Entities

- `sys_user`
- `lost_item`
- `found_item`
- `item_category`
- `message_notice`
- `operation_log`
- `claim_application`
- `announcement`
- `item_report`
- `system_config`

### 2.2 Relationship Overview

- One user can publish multiple lost items.
- One user can publish multiple found items.
- One category can be referenced by many lost and found items.
- One user can receive many message notices.
- One found item can have multiple claim applications.

### 2.3 Design Notes

- Primary keys use `BIGINT`.
- Important fields use non-null constraints.
- Username and category name use unique constraints.
- Foreign keys connect users, categories, records, and claims.

## 3. API Design

The backend exposes JSON APIs.

### 3.1 Authentication

- `POST /api/auth/login`
- `POST /api/auth/register`

### 3.2 User

- `GET /api/user/profile`
- `PUT /api/user/profile`
- `GET /api/user/list`
- `PATCH /api/user/{id}/status`
- `PATCH /api/user/{id}/role`

### 3.3 Items

- `POST /api/items/lost`
- `POST /api/items/found`
- `GET /api/items/search`
- `GET /api/items/mine`
- `PUT /api/items/{itemId}`
- `POST /api/items/{itemId}/claim`
- `POST /api/items/{itemId}/report`
- `POST /api/items/{itemId}/offline`
- `POST /api/items/{itemId}/complete`

### 3.4 Admin

- `GET /api/admin/dashboard`
- `GET /api/admin/reviews`
- `POST /api/admin/reviews/{id}/approve`
- `POST /api/admin/reviews/{id}/reject`
- `POST /api/admin/reviews/{id}/delete`
- `GET /api/admin/announcements`
- `POST /api/admin/reports/{id}/resolve`
- `POST /api/admin/reports/{id}/reject`

### 3.5 AI

- `GET /api/ai/status`
- `POST /api/ai/smart-match`
- `POST /api/ai/spatiotemporal/predict`

## 4. Security Design

### 4.1 Authentication

- JWT is used for stateless authentication.
- Passwords are encrypted with BCrypt.
- The backend validates the token and reloads the current user from the database.

### 4.2 Authorization

- User endpoints require login.
- Review admin and system admin endpoints require role checks.
- System admin endpoints additionally restrict configuration and user-management operations.

### 4.3 Other Measures

- Upload size is limited by backend configuration.
- Request validation is enforced through DTO validation.
- Operation logs are written for critical actions.
- Sensitive configuration can be overridden with environment variables.
- Distributed rate limiting is implemented with Redis + Redisson for login, upload, publish, chat, and AI interfaces.
- Detailed limiter rules and environment strategy are documented in `docs/backend-rate-limit.md`.

## 5. Implementation Notes

### 5.1 Backend

- Spring Boot 3
- Spring Security
- MyBatis-Flex
- MySQL

### 5.2 Frontend

- Vue 3
- Vite
- Element Plus
- Axios
- Vue Router

### 5.3 AI Services

- FastAPI-based image search service
- FastAPI-based spatiotemporal prediction service
