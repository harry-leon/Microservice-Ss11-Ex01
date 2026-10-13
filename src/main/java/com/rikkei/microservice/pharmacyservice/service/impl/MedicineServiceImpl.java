package com.rikkei.microservice.pharmacyservice.service.impl;

import com.rikkei.microservice.pharmacyservice.entity.Medicine;
import com.rikkei.microservice.pharmacyservice.repository.MedicineRepository;
import com.rikkei.microservice.pharmacyservice.service.MedicineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicineServiceImpl implements MedicineService {

    private final MedicineRepository medicineRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Medicine> getAllMedicines() {
        log.info("Fetching all medicines from database");
        return medicineRepository.findAll();
    }

    /**
     * Cache-Aside Pattern:
     * Kiểm tra trong Redis cache với cache name 'medicines' và key '#id'.
     * - Nếu có trong cache: Trả về trực tiếp từ cache, không chạy method này (không query DB).
     * - Nếu chưa có: Thực thi method, truy vấn database và lưu kết quả vào Redis cache.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "medicines", key = "#id")
    public Medicine getMedicineById(Long id) {
        log.info(">>> CACHE MISS: Querying database for medicine with id: {}", id);
        return medicineRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Medicine not found with id: " + id));
    }

    @Override
    @Transactional
    public Medicine createMedicine(Medicine medicine) {
        log.info("Saving new medicine into database: {}", medicine.getName());
        return medicineRepository.save(medicine);
    }

    @Override
    @Transactional
    @CachePut(value = "medicines", key = "#id")
    public Medicine updateMedicine(Long id, Medicine medicine) {
        log.info("Updating medicine in database and refreshing cache for id: {}", id);
        Medicine existing = medicineRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Medicine not found with id: " + id));

        existing.setName(medicine.getName());
        existing.setActiveIngredient(medicine.getActiveIngredient());
        existing.setQuantity(medicine.getQuantity());
        existing.setPrice(medicine.getPrice());
        existing.setManufacturer(medicine.getManufacturer());

        return medicineRepository.save(existing);
    }

    @Override
    @Transactional
    @CacheEvict(value = "medicines", key = "#id")
    public void deleteMedicine(Long id) {
        log.info("Deleting medicine from database and evicting cache for id: {}", id);
        if (!medicineRepository.existsById(id)) {
            throw new NoSuchElementException("Medicine not found with id: " + id);
        }
        medicineRepository.deleteById(id);
    }
}
