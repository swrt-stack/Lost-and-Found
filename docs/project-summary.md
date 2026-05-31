# Project Summary

## Positioning

This project is a graduation-design prototype for a campus lost and found management platform. It covers the core flows for both users and administrators and can be used for demonstrations, coursework delivery, and further extension.

## Technical Architecture

### Backend

- Spring Boot 3
- Spring Security
- MyBatis-Flex
- JWT
- MySQL

### Frontend

- Vue 3
- Vite
- Element Plus
- Axios
- Vue Router

### AI

- FastAPI
- PyTorch
- FAISS

## Delivered Features

### User Features

- Login and registration
- Profile center
- Lost-item publishing
- Found-item publishing
- Search
- Message center
- Smart matching

### Admin Features

- Dashboard
- Review workflow
- User management
- Category management
- Announcement management
- Report handling
- System configuration
- Operation log viewing

## Project Highlights

1. Clear separated frontend and backend architecture.
2. Standard layered backend structure with `controller`, `dto`, `entity`, `mapper`, `service`, and `service.impl`.
3. JWT-based authentication with BCrypt password encryption.
4. Integrated local AI services for image search and spatiotemporal prediction.
5. Includes schema, seed data, run guide, and system design documents.

## Suggested Demo Flow

1. Import `database.sql` into MySQL.
2. Start the backend and confirm `/api/health` is reachable.
3. Start the frontend and open the home page.
4. Log in with the user account and demonstrate publishing, search, and message flows.
5. Log in with an admin account and demonstrate dashboard, review, user management, and configuration flows.
6. Start the AI services and demonstrate smart matching if needed.

## Future Improvements

1. Add more realistic datasets and larger test coverage.
2. Improve search and dashboard queries to avoid full-table in-memory processing.
3. Add stronger deployment configuration and environment isolation.
4. Add more complete interaction flows such as threaded comments and retrieval confirmation.
5. Add charts and export features for reports and statistics.
