package com.campus.lab.repository;

import com.campus.lab.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByLabId(Long labId);

    long countByLabId(Long labId);

    long countByLabIdAndStatus(Long labId, String status);

    long countByStatus(String status);
}
