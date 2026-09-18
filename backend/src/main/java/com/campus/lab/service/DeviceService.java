package com.campus.lab.service;

import com.campus.lab.common.BizException;
import com.campus.lab.dto.DeviceDTO;
import com.campus.lab.dto.DeviceReportRequest;
import com.campus.lab.entity.Device;
import com.campus.lab.entity.Lab;
import com.campus.lab.repository.DeviceRepository;
import com.campus.lab.repository.LabRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final LabRepository labRepository;

    public DeviceService(DeviceRepository deviceRepository, LabRepository labRepository) {
        this.deviceRepository = deviceRepository;
        this.labRepository = labRepository;
    }

    public List<DeviceDTO> list(String keyword, String status, Long labId) {
        Map<Long, Lab> labs = labRepository.findAll().stream()
                .collect(Collectors.toMap(Lab::getId, Function.identity()));
        return deviceRepository.findAll().stream()
                .filter(d -> labId == null || labId.equals(d.getLabId()))
                .filter(d -> status == null || status.isBlank() || status.equals(d.getStatus()))
                .filter(d -> {
                    if (keyword == null || keyword.isBlank()) {
                        return true;
                    }
                    String k = keyword.trim().toLowerCase(Locale.ROOT);
                    Lab lab = labs.get(d.getLabId());
                    return contains(d.getName(), k) || contains(d.getModel(), k)
                            || (lab != null && contains(lab.getName(), k));
                })
                .map(d -> DeviceDTO.of(d, labs.get(d.getLabId())))
                .sorted(Comparator.comparing(DeviceDTO::id))
                .collect(Collectors.toList());
    }

    @Transactional
    public DeviceDTO create(Device d) {
        checkLab(d.getLabId());
        fillDefault(d);
        return DeviceDTO.of(deviceRepository.save(d), labRepository.findById(d.getLabId()).orElse(null));
    }

    @Transactional
    public DeviceDTO update(Long id, Device body) {
        Device d = deviceRepository.findById(id)
                .orElseThrow(() -> new BizException("设备不存在"));
        checkLab(body.getLabId());
        d.setLabId(body.getLabId());
        d.setName(body.getName());
        d.setModel(body.getModel());
        if (body.getStatus() != null) {
            validateStatus(body.getStatus());
            d.setStatus(body.getStatus());
        }
        d.setPurchaseDate(body.getPurchaseDate());
        d.setPrice(body.getPrice());
        d.setRemark(body.getRemark());
        return DeviceDTO.of(deviceRepository.save(d), labRepository.findById(d.getLabId()).orElse(null));
    }

    @Transactional
    public void delete(Long id) {
        if (!deviceRepository.existsById(id)) {
            throw new BizException("设备不存在");
        }
        deviceRepository.deleteById(id);
    }

    @Transactional
    public void report(String reporter, Long id, DeviceReportRequest req) {
        Device d = deviceRepository.findById(id)
                .orElseThrow(() -> new BizException("设备不存在"));
        if ("SCRAPPED".equals(d.getStatus())) {
            throw new BizException("该设备已报废，无需报修");
        }
        if ("FAULTY".equals(d.getStatus()) || "REPAIRING".equals(d.getStatus())) {
            throw new BizException("该设备已在维修流程中");
        }
        d.setStatus("FAULTY");
        d.setFaultDesc(req.description());
        d.setReportBy(reporter);
        d.setReportAt(LocalDateTime.now());
        deviceRepository.save(d);
    }

    @Transactional
    public void repair(Long id) {
        Device d = deviceRepository.findById(id)
                .orElseThrow(() -> new BizException("设备不存在"));
        d.setStatus("NORMAL");
        d.setFaultDesc(null);
        d.setReportBy(null);
        d.setReportAt(null);
        deviceRepository.save(d);
    }

    private void checkLab(Long labId) {
        if (labId == null || labRepository.findById(labId).isEmpty()) {
            throw new BizException("所属实验室不存在");
        }
    }

    private void validateStatus(String status) {
        if (!List.of("NORMAL", "FAULTY", "REPAIRING", "SCRAPPED").contains(status)) {
            throw new BizException("设备状态不合法，仅支持 NORMAL / FAULTY / REPAIRING / SCRAPPED");
        }
    }

    private void fillDefault(Device d) {
        if (d.getStatus() == null) {
            d.setStatus("NORMAL");
        } else {
            validateStatus(d.getStatus());
        }
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
