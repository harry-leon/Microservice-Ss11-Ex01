# [Bài tập] Caching danh mục thuốc (Redis Cache) - Ss11-Ex01

Dự án triển khai mô hình kiến trúc **Microservice Caching** sử dụng **Spring Boot Starter Data Redis** và **Cache-Aside Pattern** theo yêu cầu:
- Tăng tốc độ truy vấn danh mục thuốc, giảm tải cho Database (PostgreSQL/MySQL).
- Cấu hình Redis làm Cache Provider trong `application.properties`.
- Sử dụng annotation `@Cacheable(value = "medicines", key = "#id")` tại hàm lấy chi tiết thuốc.
- Dữ liệu thuốc được lưu vào Redis dưới dạng JSON (`GenericJackson2JsonRedisSerializer`).

---

## 1. Cấu trúc thư mục

```text
Ss11-Ex01/
├── src/
│   ├── main/
│   │   ├── java/com/rikkei/microservice/pharmacyservice/
│   │   │   ├── config/
│   │   │   │   └── RedisConfig.java                 # Cấu hình RedisCacheManager & JSON Serializer
│   │   │   ├── controller/
│   │   │   │   └── MedicineController.java           # REST Controller (/api/medicines)
│   │   │   ├── entity/
│   │   │   │   └── Medicine.java                     # Entity Medicine (implements Serializable)
│   │   │   ├── repository/
│   │   │   │   └── MedicineRepository.java           # Spring Data JPA Repository
│   │   │   ├── service/
│   │   │   │   ├── MedicineService.java              # Service Interface
│   │   │   │   └── impl/MedicineServiceImpl.java     # Service triển khai @Cacheable, @CachePut, @CacheEvict
│   │   │   └── PharmacyServiceApplication.java       # Main class & Seed Data ban đầu
│   │   └── resources/
│   │       └── application.properties                # Cấu hình kết nối Redis & Database
│   └── test/
│       └── java/com/rikkei/microservice/pharmacyservice/
│           └── MedicineCacheTest.java                # Unit/Integration test kiểm chứng Cache-aside pattern
├── docker-compose.yml                                # Redis & PostgreSQL containers
├── pom.xml                                           # Maven dependencies (Redis, Cache, JPA, Web, Jackson)
├── .gitignore
└── README.md
```

---

## 2. Nguyên lý hoạt động Cache-aside Pattern

1. **Lần 1 truy vấn chi tiết thuốc (`GET /api/medicines/{id}`):**
   - Hệ thống kiểm tra trong Redis Cache xem có key `medicines::{id}` chưa.
   - **Cache MISS**: Key chưa tồn tại -> Service thực thi truy vấn câu lệnh SQL vào Database (`MedicineRepository.findById(id)`).
   - Hibernate in log câu lệnh SQL: `select m1_0.id, m1_0.name, ... from medicines m1_0 where m1_0.id=?`.
   - Kết quả được serialize dưới dạng JSON và lưu vào Redis với TTL là 10 phút.
   - Thời gian phản hồi lần đầu bao gồm thời gian truy vấn DB.

2. **Lần 2 truy vấn cùng thuốc (`GET /api/medicines/{id}`):**
   - Hệ thống kiểm tra thấy key `medicines::{id}` đã tồn tại trong Redis (**Cache HIT**).
   - Trả về ngay lập tức dữ liệu từ Redis Cache.
   - Phương thức bên dưới không được gọi -> **Hoàn toàn không có câu lệnh SQL nào được sinh ra**.
   - Tốc độ phản hồi cực nhanh (**< 10ms**).

---

## 3. Cấu hình trong `application.properties`

```properties
# Redis Cache Provider Configuration
spring.cache.type=redis
spring.cache.redis.time-to-live=600000
spring.cache.redis.cache-null-values=false

# Spring Data Redis Connection
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.timeout=5000ms

# Database Configuration
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:pharmacy_db}
spring.datasource.username=${DB_USER:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

---

## 4. Hướng dẫn chạy và kiểm thử

### Bước 1: Khởi động Redis và PostgreSQL (Docker)
```bash
docker-compose up -d
```

### Bước 2: Build và chạy ứng dụng
```bash
mvn clean package -DskipTests
mvn spring-boot:run
```

### Bước 3: Kiểm tra API Caching

1. **Gọi lấy chi tiết thuốc lần 1 (ID = 1):**
   ```bash
   curl -i http://localhost:8081/api/medicines/1
   ```
   - Log console ứng dụng:
     ```text
     >>> CACHE MISS: Querying database for medicine with id: 1
     Hibernate: select m1_0.id, ... from medicines m1_0 where m1_0.id=?
     API GET /api/medicines/1 executed in 35 ms
     ```

2. **Gọi lấy chi tiết thuốc lần 2 (ID = 1):**
   ```bash
   curl -i http://localhost:8081/api/medicines/1
   ```
   - Log console ứng dụng:
     ```text
     API GET /api/medicines/1 executed in 2 ms
     ```
   - **Kết quả**: Không có log `CACHE MISS` và không có câu lệnh Hibernate SQL nào xuất hiện trong log; tốc độ phản hồi < 10ms.

3. **Kiểm tra dữ liệu được lưu trong Redis:**
   ```bash
   redis-cli
   KEYS *
   # Output: "medicines::1"
   GET "medicines::1"
   ```
   Dữ liệu được lưu trữ chuẩn JSON định dạng `GenericJackson2JsonRedisSerializer`.

### Bước 4: Chạy Automated Test
```bash
mvn test
```
Test suite `MedicineCacheTest` tự động xác minh:
- Lần 1 gọi: repository `findById` được invoke 1 lần.
- Lần 2 gọi: kết quả trả về tương đương nhưng repository `findById` không bị gọi thêm lần nào (vẫn `times(1)`).
- Cache "medicines" chứa key tương ứng.
