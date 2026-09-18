package com.campus.lab.service;

import com.campus.lab.common.BizException;
import com.campus.lab.dto.LabDTO;
import com.campus.lab.dto.ReservationDTO;
import com.campus.lab.entity.Lab;
import com.campus.lab.entity.Reservation;
import com.campus.lab.repository.DeviceRepository;
import com.campus.lab.repository.LabRepository;
import com.campus.lab.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class LabService {

    private final LabRepository labRepository;
    private final DeviceRepository deviceRepository;
    private final ReservationRepository reservationRepository;

    public LabService(LabRepository labRepository, DeviceRepository deviceRepository,
                      ReservationRepository reservationRepository) {
        this.labRepository = labRepository;
        this.deviceRepository = deviceRepository;
        this.reservationRepository = reservationRepository;
    }

    public List<LabDTO> list(String keyword, String category, String status) {
        LocalDate today = LocalDate.now();
        return labRepository.findAll().stream()
                .filter(l -> status == null || status.isBlank() || status.equals(l.getStatus()))
                .filter(l -> category == null || category.isBlank() || category.equals(l.getCategory()))
                .filter(l -> {
                    if (keyword == null || keyword.isBlank()) {
                        return true;
                    }
                    String k = keyword.trim().toLowerCase(Locale.ROOT);
                    return contains(l.getName(), k) || contains(l.getCode(), k)
                            || contains(l.getBuilding(), k) || contains(l.getRoom(), k);
                })
                .map(l -> toDTO(l, today))
                .sorted(Comparator.comparing(LabDTO::id))
                .collect(Collectors.toList());
    }

    public LabDTO detail(Long id) {
        Lab lab = labRepository.findById(id)
                .orElseThrow(() -> new BizException("实验室不存在"));
        return toDTO(lab, LocalDate.now());
    }

    public List<ReservationDTO> timetable(Long labId, LocalDate date) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new BizException("实验室不存在"));
        return reservationRepository.findByLabIdAndDateAndStatusIn(labId, date, List.of("PENDING", "APPROVED"))
                .stream()
                .map(r -> ReservationDTO.of(r, lab, null))
                .collect(Collectors.toList());
    }

    @Transactional
    public LabDTO create(Lab lab) {
        checkCode(lab.getCode(), null);
        fillDefault(lab);
        return toDTO(labRepository.save(lab), LocalDate.now());
    }

    @Transactional
    public LabDTO update(Long id, Lab body) {
        Lab lab = labRepository.findById(id)
                .orElseThrow(() -> new BizException("实验室不存在"));
        checkCode(body.getCode(), id);
        lab.setCode(body.getCode());
        lab.setName(body.getName());
        lab.setBuilding(body.getBuilding());
        lab.setRoom(body.getRoom());
        lab.setCategory(body.getCategory());
        lab.setCapacity(body.getCapacity() == null ? 40 : body.getCapacity());
        lab.setManager(body.getManager());
        lab.setPhone(body.getPhone());
        lab.setOpenTime(body.getOpenTime() == null ? "08:00" : body.getOpenTime());
        lab.setCloseTime(body.getCloseTime() == null ? "22:00" : body.getCloseTime());
        lab.setStatus(body.getStatus() == null ? "OPEN" : body.getStatus());
        lab.setDescription(body.getDescription());
        lab.setEquipment(body.getEquipment());
        return toDTO(labRepository.save(lab), LocalDate.now());
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        if (!"OPEN".equals(status) && !"CLOSED".equals(status) && !"MAINTENANCE".equals(status)) {
            throw new BizException("状态不合法，仅支持 OPEN / CLOSED / MAINTENANCE");
        }
        Lab lab = labRepository.findById(id)
                .orElseThrow(() -> new BizException("实验室不存在"));
        lab.setStatus(status);
        labRepository.save(lab);
    }

    @Transactional
    public void delete(Long id) {
        if (!labRepository.existsById(id)) {
            throw new BizException("实验室不存在");
        }
        // 全量校验（含历史与未来日期），防止删除后预约悬挂
        long resCount = reservationRepository.countByLabId(id);
        if (resCount > 0) {
            throw new BizException("该实验室存在 " + resCount + " 条预约记录，不允许删除");
        }
        if (deviceRepository.countByLabId(id) > 0) {
            throw new BizException("该实验室下存在设备，请先转移或删除设备");
        }
        labRepository.deleteById(id);
    }

    /**
     * 状态聚合引擎：根据实验室配置与当日预约，计算当前实时状态。
     */
    public String currentStatus(Lab lab) {
        if (!"OPEN".equals(lab.getStatus())) {
            return "MAINTENANCE".equals(lab.getStatus()) ? "维护中" : "已关闭";
        }
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        List<Reservation> list = reservationRepository
                .findByLabIdAndDateAndStatusIn(lab.getId(), today, List.of("PENDING", "APPROVED"));
        for (Reservation r : list) {
            LocalTime s = LocalTime.parse(r.getStartTime());
            LocalTime e = LocalTime.parse(r.getEndTime());
            if (!now.isBefore(s) && now.isBefore(e)) {
                return "使用中";
            }
        }
        for (Reservation r : list) {
            LocalTime s = LocalTime.parse(r.getStartTime());
            long minutes = Duration.between(now, s).toMinutes();
            if (minutes >= 0 && minutes <= 60) {
                return "即将开始";
            }
        }
        return "空闲";
    }

    private LabDTO toDTO(Lab l, LocalDate today) {
        long deviceCount = deviceRepository.countByLabId(l.getId());
        long faulty = deviceRepository.countByLabIdAndStatus(l.getId(), "FAULTY")
                + deviceRepository.countByLabIdAndStatus(l.getId(), "REPAIRING");
        long todayRes = reservationRepository
                .findByLabIdAndDateAndStatusIn(l.getId(), today, List.of("PENDING", "APPROVED")).size();
        return new LabDTO(l.getId(), l.getCode(), l.getName(), l.getBuilding(), l.getRoom(), l.getCategory(),
                l.getCapacity(), l.getManager(), l.getPhone(), l.getOpenTime(), l.getCloseTime(),
                l.getStatus(), l.getDescription(), l.getEquipment(),
                deviceCount, faulty, todayRes, currentStatus(l));
    }

    private void checkCode(String code, Long excludeId) {
        if (code == null || code.isBlank()) {
            throw new BizException("实验室编号不能为空");
        }
        labRepository.findAll().stream()
                .filter(l -> code.equals(l.getCode()) && !l.getId().equals(excludeId))
                .findAny()
                .ifPresent(l -> {
                    throw new BizException("实验室编号已存在");
                });
    }

    private void fillDefault(Lab lab) {
        if (lab.getCapacity() == null) {
            lab.setCapacity(40);
        }
        if (lab.getOpenTime() == null) {
            lab.setOpenTime("08:00");
        }
        if (lab.getCloseTime() == null) {
            lab.setCloseTime("22:00");
        }
        if (lab.getStatus() == null) {
            lab.setStatus("OPEN");
        }
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
