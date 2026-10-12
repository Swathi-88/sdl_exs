# URL Shortener Projects (`url_shortner`)

This repository contains two Spring Boot URL shortener implementations:

---

## 📁 Repository Structure

```
url_shortner/
├── url-shortener-api/       # Implementation 1: Spring Boot REST API with Swagger & Redis/DB caching
└── url-shortner-api-2/      # Implementation 2: Spring Boot URL shortener with Frontend UI
```

---

## 1. `url-shortener-api`
- **Tech Stack**: Java, Spring Boot, Spring Data JPA, Swagger / OpenAPI
- **Features**: RESTful API endpoints to encode/decode URLs, track clicks, and swagger documentation.
- **Run**:
  ```bash
  cd url-shortener-api
  ./mvnw spring-boot:run
  ```

---

## 2. `url-shortner-api-2`
- **Tech Stack**: Java, Spring Boot, Thymeleaf / HTML Frontend
- **Features**: Web frontend and backend service for shortening URLs and redirection.
- **Run**:
  ```bash
  cd url-shortner-api-2
  mvn spring-boot:run
  ```
