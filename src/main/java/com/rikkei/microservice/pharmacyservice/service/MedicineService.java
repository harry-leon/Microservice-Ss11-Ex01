package com.rikkei.microservice.pharmacyservice.service;

import com.rikkei.microservice.pharmacyservice.entity.Medicine;

import java.util.List;

public interface MedicineService {

    List<Medicine> getAllMedicines();

    Medicine getMedicineById(Long id);

    Medicine createMedicine(Medicine medicine);

    Medicine updateMedicine(Long id, Medicine medicine);

    void deleteMedicine(Long id);
}
