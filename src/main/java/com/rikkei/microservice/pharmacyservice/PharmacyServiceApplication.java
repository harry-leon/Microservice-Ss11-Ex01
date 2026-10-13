package com.rikkei.microservice.pharmacyservice;

import com.rikkei.microservice.pharmacyservice.entity.Medicine;
import com.rikkei.microservice.pharmacyservice.repository.MedicineRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.util.List;

@SpringBootApplication
@EnableCaching
@Slf4j
public class PharmacyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PharmacyServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(MedicineRepository medicineRepository) {
        return args -> {
            if (medicineRepository.count() == 0) {
                log.info("Initializing sample medicines in database...");
                medicineRepository.saveAll(List.of(
                        Medicine.builder()
                                .name("Paracetamol 500mg")
                                .activeIngredient("Paracetamol")
                                .quantity(1500)
                                .price(new BigDecimal("25000.00"))
                                .manufacturer("Dược Hậu Giang")
                                .build(),
                        Medicine.builder()
                                .name("Amoxicillin 500mg")
                                .activeIngredient("Amoxicillin Trihydrate")
                                .quantity(800)
                                .price(new BigDecimal("45000.00"))
                                .manufacturer("Imexpharm")
                                .build(),
                        Medicine.builder()
                                .name("Ibuprofen 400mg")
                                .activeIngredient("Ibuprofen")
                                .quantity(600)
                                .price(new BigDecimal("35000.00"))
                                .manufacturer("Stada")
                                .build(),
                        Medicine.builder()
                                .name("Berberin 100mg")
                                .activeIngredient("Berberin Clorid")
                                .quantity(2000)
                                .price(new BigDecimal("15000.00"))
                                .manufacturer("Dược Phẩm OPC")
                                .build(),
                        Medicine.builder()
                                .name("Panadol Extra")
                                .activeIngredient("Paracetamol, Caffeine")
                                .quantity(1200)
                                .price(new BigDecimal("30000.00"))
                                .manufacturer("GSK")
                                .build()
                ));
                log.info("Sample medicines initialized successfully.");
            }
        };
    }
}
