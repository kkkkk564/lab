package com.campus.lab.service;

import com.campus.lab.dto.LabUsage;
import com.campus.lab.dto.Overview;
import com.campus.lab.dto.StatusCount;
import com.campus.lab.dto.TrendPoint;
import com.campus.lab.entity.Lab;
import com.campus.lab.entity.Reservation;
import com.campus.lab.repository.DeviceRepository;
import com.campus.lab.repository.LabRepository;
import com.campus.lab.repository.ReservationRepository;
import com.campus.lab.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatsService {

    private final LabRepository labRepository;
    private final DeviceRepository deviceRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final LabService labService;

    public StatsService(LabRepository labRepository, DeviceRepository deviceRepository,
                        ReservationRepository reservationRepository, UserRepository userRepository,
                        LabService labService) {
        this.labRepository = labRepository;
        this.deviceRepository = deviceRepository;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.labService = labService;
    }

    public Overview overview() {
        List<Lab> labs = labRepository.findAll();
        long openLabs = labs.stream().filter(l -> "OPEN".equals(l.getStatus())).count();
        long inUseLabs = labs.stream()
                .filter(l -> "OPEN".equals(l.getStatus()) && "使用中".equals(labService.currentStatus(l)))
                .count();
        LocalDate today = LocalDate.now();
        List<Reservation> todayList = reservationRepository.findByDate(today);
        long todayApproved = todayList.stream().filter(r -> "APPROVED".equals(r.getStatus())).count();
        long todayPending = todayList.stream().filter(r -> "PENDING".equals(r.getStatus())).count();
        return new Overview(
                labs.size(), openLabs, inUseLabs,
                deviceRepository.count(),
                deviceRepository.countByStatus("NORMAL"),
                deviceRepository.countByStatus("FAULTY"),
                deviceRepository.countByStatus("REPAIRING"),
                todayList.size(), todayApproved, todayPending,
                userRepository.countByActiveTrue(),
                reservationRepository.countByStatus("PENDING"));
    }

    public List<TrendPoint> trends(int days) {
        LocalDate start = LocalDate.now().minusDays(days - 1L);
        List<Reservation> list = reservationRepository.findByDateBetweenOrderByDateAsc(start, LocalDate.now());
        Map<LocalDate, List<Reservation>> byDate = list.stream()
                .collect(Collectors.groupingBy(Reservation::getDate));
        List<TrendPoint> out = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            List<Reservation> rs = byDate.getOrDefault(d, List.of());
            long approved = rs.stream().filter(r -> "APPROVED".equals(r.getStatus())).count();
            out.add(new TrendPoint(d.toString(), rs.size(), approved));
        }
        return out;
    }

    public List<LabUsage> labUsage(int days) {
        LocalDate start = LocalDate.now().minusDays(days - 1L);
        List<Reservation> list = reservationRepository.findByDateBetweenOrderByDateAsc(start, LocalDate.now());
        Map<Long, List<Reservation>> byLab = list.stream()
                .filter(r -> "APPROVED".equals(r.getStatus()))
                .collect(Collectors.groupingBy(Reservation::getLabId));
        double totalAvailable = days * 14.0;
        List<LabUsage> out = new ArrayList<>();
        for (Lab lab : labRepository.findAll()) {
            List<Reservation> rs = byLab.getOrDefault(lab.getId(), List.of());
            double hours = rs.stream()
                    .mapToDouble(r -> hours(r))
                    .sum();
            double util = totalAvailable > 0 ? Math.min(100.0, hours / totalAvailable * 100.0) : 0;
            out.add(new LabUsage(lab.getId(), lab.getName(), rs.size(),
                    Math.round(hours * 10) / 10.0, Math.round(util * 10) / 10.0));
        }
        return out.stream()
                .sorted(Comparator.comparingDouble(LabUsage::hours).reversed())
                .collect(Collectors.toList());
    }

    public List<StatusCount> statusDistribution() {
        List<Reservation> all = reservationRepository.findAll();
        Map<String, Long> raw = all.stream()
                .collect(Collectors.groupingBy(Reservation::getStatus, Collectors.counting()));
        long finished = 0;
        long activeApproved = 0;
        for (Reservation r : all) {
            if ("APPROVED".equals(r.getStatus())) {
                LocalDateTime end = LocalDateTime.of(r.getDate(), safeParse(r.getEndTime()));
                if (end.isBefore(LocalDateTime.now())) {
                    finished++;
                } else {
                    activeApproved++;
                }
            }
        }
        List<StatusCount> out = new ArrayList<>();
        out.add(new StatusCount("已完成", finished));
        out.add(new StatusCount("已生效", activeApproved));
        out.add(new StatusCount("待审批", raw.getOrDefault("PENDING", 0L)));
        out.add(new StatusCount("已驳回", raw.getOrDefault("REJECTED", 0L)));
        out.add(new StatusCount("已取消", raw.getOrDefault("CANCELLED", 0L)));
        return out;
    }

    private double hours(Reservation r) {
        try {
            return Duration.between(LocalTime.parse(r.getStartTime()), LocalTime.parse(r.getEndTime()))
                    .toMinutes() / 60.0;
        } catch (Exception e) {
            return 0;
        }
    }

    private LocalTime safeParse(String time) {
        try {
            return LocalTime.parse(time);
        } catch (Exception e) {
            return LocalTime.MIDNIGHT;
        }
    }
}
