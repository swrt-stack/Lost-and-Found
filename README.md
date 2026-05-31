# Campus Lost and Found Platform

This repository is a graduation-design project with a separated frontend and backend architecture for campus lost-and-found management.

## Modules

- `backend/`: Spring Boot backend, authentication, item publishing, review flow, admin APIs
- `frontend/`: Vue 3 + Vite frontend for user-side pages and admin-side pages
- `python-image-search/`: image retrieval service
- `python-spatiotemporal/`: spatiotemporal prediction service
- `docs/`: run and design documents
- `database.sql`: schema and seed data

## Tech Stack

- Backend: Spring Boot 3, Spring Security, MyBatis-Flex, JWT, MySQL 8
- Frontend: Vue 3, Vite, Vue Router, Axios, Element Plus
- AI Services: FastAPI, PyTorch, FAISS

## Current Scope

### User Side

- Login and registration
- Home page and public item list
- Lost-item publishing
- Found-item publishing
- Text search and image-based smart matching
- Item detail and claim submission
- Personal center and message center

### Admin Side

- Review-admin dashboard, pending review list, review history
- System-admin dashboard
- Lost-item management
- Found-item management
- Pending item management
- Claim management
- User management
- Announcement management
- Admin-account management

## Quick Start

1. Create a MySQL database named `lost_found` with `utf8mb4`.
2. Import `database.sql`.
3. Start the backend from `backend/`.
4. Start the frontend from `frontend/`.
5. Start the Python services only if you need AI search or spatiotemporal prediction.

Detailed setup steps are in [docs/run-guide.md](/D:/GraduationDesign/untitled/docs/run-guide.md).

## Demo Data

- The repository ships with seeded demo users `demo`, `reviewer`, and `sysadmin`.
- Usernames are stable, but passwords in your local database should be treated as environment data rather than hard-coded documentation.
- If you want a fully clean demo environment, re-import `database.sql` into a fresh `lost_found` database.

## Notes

- Backend and database character set are configured for UTF-8 / `utf8mb4`.
- Recent cleanup removed temporary smoke-test data and repaired historical `created_at` null records.
- The frontend and backend both compile successfully in the current workspace.
