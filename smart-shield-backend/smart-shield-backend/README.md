# Smart Shield Backend (Spring Boot + MySQL)

Smart Shield frontend (`index.html`) ka backend. Abhi sirf **project setup + DB connection** hai, koi feature nahi.

## Zaroori cheezein
- Java 17 ya usse upar
- Maven 3.9+ (ya IntelliJ / Eclipse / VS Code ka built-in Maven)
- MySQL 8 (localhost:3306 pe chalu)

## Setup
1. `src/main/resources/application.properties` kholo.
2. `spring.datasource.password` mein apna MySQL password daalo (username agar root nahi hai to wo bhi badlo).
3. Database `smart_shield` khud ban jayega.

## Run
```
mvn spring-boot:run
```

## Check karo
Browser mein kholo: http://localhost:8080/api/health

Aisa kuch dikhna chahiye:
```
{ "app": "Smart Shield Backend", "status": "UP", "time": "...", "database": "CONNECTED" }
```

## Folder structure
```
com.smartshield
├── SmartShieldBackendApplication.java   main entry point
├── config/CorsConfig.java               frontend ke fetch() ke liye CORS
├── controller/HealthController.java     setup check
├── service/                             business logic (khaali)
├── repository/                          DB access (khaali)
├── model/                               entities (khaali)
└── dto/                                 request/response objects (khaali)
```

## Roadmap (user side pehle)
1. Login / Signup
2. Complaints (raise, list, status)
3. Evidence upload
4. Route safety rating + auto-complaint
5. SOS alert
6. Admin dashboard APIs (baad mein)
