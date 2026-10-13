package com.rikkei.microservice.pharmacyservice;

import com.rikkei.microservice.pharmacyservice.entity.Medicine;
import com.rikkei.microservice.pharmacyservice.repository.MedicineRepository;
import com.rikkei.microservice.pharmacyservice.service.MedicineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.cache.type=simple",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class MedicineCacheTest {

    @Autowired
    private MedicineService medicineService;

    @SpyBean
    private MedicineRepository medicineRepository;

    @Autowired
    private CacheManager cacheManager;

    private Long testMedicineId;

    @BeforeEach
    void setUp() {
        Objects.requireNonNull(cacheManager.getCache("medicines")).clear();
        medicineRepository.deleteAll();

        Medicine saved = medicineRepository.save(Medicine.builder()
                .name("Test Panadol")
                .activeIngredient("Paracetamol")
                .quantity(100)
                .price(new BigDecimal("20000.00"))
                .manufacturer("Sanofi")
                .build());

        testMedicineId = saved.getId();
        reset(medicineRepository);
    }

    @Test
    @DisplayName("Lần 1 gọi getMedicineById phải query DB, lần 2 gọi cùng id phải lấy từ Cache và không query DB")
    void testCacheAsidePattern() {
        // Lần 1: Gọi hàm lấy chi tiết thuốc -> Cache Miss -> query DB
        Medicine firstCallResult = medicineService.getMedicineById(testMedicineId);
        assertNotNull(firstCallResult);
        assertEquals("Test Panadol", firstCallResult.getName());
        verify(medicineRepository, times(1)).findById(testMedicineId);

        // Lần 2: Gọi lại cùng id -> Cache Hit -> không chạy lại phương thức repo
        Medicine secondCallResult = medicineService.getMedicineById(testMedicineId);
        assertNotNull(secondCallResult);
        assertEquals("Test Panadol", secondCallResult.getName());
        // findById trong repository vẫn chỉ được gọi đúng 1 lần từ lần trước!
        verify(medicineRepository, times(1)).findById(testMedicineId);

        // Kiểm tra đối tượng có tồn tại trong Cache "medicines"
        assertNotNull(cacheManager.getCache("medicines"));
        assertNotNull(cacheManager.getCache("medicines").get(testMedicineId));
    }
}
