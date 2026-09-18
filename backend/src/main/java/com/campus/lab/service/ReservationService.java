package com.campus.lab.service;

import com.campus.lab.common.BizException;
import com.campus.lab.dto.ReservationCreateRequest;
import com.campus.lab.dto.ReservationDTO;
import com.campus.lab.dto.ReviewRequest;
import com.campus.lab.entity.Lab;
import com.campus.lab.entity.Reservation;
import com.campus.lab.entity.User;
import com.campus.lab.repository.LabRepository;
import com.campus.lab.repository.ReservationRepository;
import com.campus.lab.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final LabRepository labRepository;
    private final UserRepository userRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              LabRepository labRepository,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.labRepository = labRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReservationDTO create(Long userId, ReservationCreateRequest req) {
        // 先对实验室行加悲观写锁：同一实验室的预约创建/审批在此串行化，消除"先查后写"竞态
        Lab lab = labRepository.findByIdForUpdate(req.labId())
                .orElseThrow(() -> new BizException("实验室不存在"));
        if (!"OPEN".equals(lab.getStatus())) {
            throw new BizException("实验室当前不可预约（已关闭或维护中）");
        }
        if (req.date().isBefore(LocalDate.now())) {
            throw new BizException("预约日期不能早于今天");
        }
        LocalTime start = parse(req.startTime());
        LocalTime end = parse(req.endTime());
        if (!start.isBefore(end)) {
            throw new BizException("结束时间必须晚于开始时间");
        }
        LocalTime open = parse(lab.getOpenTime());
        LocalTime close = parse(lab.getCloseTime());
        if (start.isBefore(open) || end.isAfter(close)) {
            throw new BizException("预约时间须在开放时间 " + lab.getOpenTime() + " - " + lab.getCloseTime() + " 之内");
        }
        if (req.date().equals(LocalDate.now()) && !start.isAfter(LocalTime.now())) {
            throw new BizException("当天的预约开始时间必须晚于当前时间");
        }
        if (lab.getCapacity() != null && req.headcount() > lab.getCapacity()) {
            throw new BizException("使用人数超过实验室容量（" + lab.getCapacity() + " 人）");
        }
        checkConflict(lab.getId(), req.date(), start, end, null);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException("用户不存在"));
        Reservation r = new Reservation();
        r.setLabId(lab.getId());
        r.setUserId(userId);
        r.setTitle(req.title());
        r.setPurpose(req.purpose());
        r.setDate(req.date());
        r.setStartTime(req.startTime());
        r.setEndTime(req.endTime());
        r.setHeadcount(req.headcount());
        r.setStatus("PENDING");
        r = reservationRepository.save(r);
        return ReservationDTO.of(r, lab, user);
    }

    public List<ReservationDTO> list(Long userId, String role, String scope, String status, LocalDate date) {
        List<Reservation> list;
        if ("all".equals(scope) && "ADMIN".equals(role)) {
            list = reservationRepository.findAllByOrderByDateDescStartTimeDesc();
        } else {
            list = reservationRepository.findByUserIdOrderByDateDescStartTimeDesc(userId);
        }
        Map<Long, Lab> labs = labRepository.findAll().stream()
                .collect(Collectors.toMap(Lab::getId, Function.identity()));
        Map<Long, User> users = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return list.stream()
                .filter(r -> date == null || date.equals(r.getDate()))
                .filter(r -> {
                    if (status == null || status.isBlank()) {
                        return true;
                    }
                    if ("FINISHED".equals(status)) {
                        return "APPROVED".equals(r.getStatus()) && endBeforeNow(r);
                    }
                    return status.equals(r.getStatus());
                })
                .map(r -> ReservationDTO.of(r, labs.get(r.getLabId()), users.get(r.getUserId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancel(Long userId, String role, Long id) {
        Reservation r = reservationRepository.findById(id)
                .orElseThrow(() -> new BizException("预约不存在"));
        if (!"ADMIN".equals(role) && !r.getUserId().equals(userId)) {
            throw new BizException("只能取消自己的预约");
        }
        if (!"PENDING".equals(r.getStatus()) && !"APPROVED".equals(r.getStatus())) {
            throw new BizException("当前状态不可取消");
        }
        if (r.getDate().isBefore(LocalDate.now())) {
            throw new BizException("预约日期已过，无法取消");
        }
        if (r.getDate().equals(LocalDate.now()) && !LocalTime.now().isBefore(parse(r.getStartTime()))) {
            throw new BizException("预约已开始或已结束，无法取消");
        }
        r.setStatus("CANCELLED");
        reservationRepository.save(r);
    }

    @Transactional
    public void review(String adminName, Long id, ReviewRequest req) {
        boolean approve = "approve".equals(req.action());
        if (!approve && !"reject".equals(req.action())) {
            throw new BizException("action 仅支持 approve / reject");
        }
        Reservation r = reservationRepository.findById(id)
                .orElseThrow(() -> new BizException("预约不存在"));
        // 与 create 保持相同加锁顺序（先实验室行、后预约行），避免死锁；
        // 实验室行锁保证同一实验室的审批互相串行，锁内重读可看到刚提交的冲突预约
        Lab lab = labRepository.findByIdForUpdate(r.getLabId())
                .orElseThrow(() -> new BizException("关联实验室不存在"));
        Reservation locked = reservationRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BizException("预约不存在"));
        if (!"PENDING".equals(locked.getStatus())) {
            throw new BizException("仅待审批的预约可以操作");
        }
        if (approve) {
            if (!"OPEN".equals(lab.getStatus())) {
                throw new BizException("实验室当前不可用（已关闭或维护中），建议驳回");
            }
            checkConflict(locked.getLabId(), locked.getDate(),
                    parse(locked.getStartTime()), parse(locked.getEndTime()), locked.getId());
            locked.setStatus("APPROVED");
        } else {
            locked.setStatus("REJECTED");
        }
        locked.setReviewComment(req.comment());
        locked.setReviewedBy(adminName);
        locked.setReviewedAt(LocalDateTime.now());
        reservationRepository.save(locked);
    }

    private void checkConflict(Long labId, LocalDate date, LocalTime start, LocalTime end, Long excludeId) {
        // 加锁读（SELECT ... FOR UPDATE）：读取最新已提交数据并在索引区间上加锁，
        // 即使并发事务刚插入冲突预约，这里也能看到，防止双占位落库
        List<Reservation> list = reservationRepository
                .findActiveByLabAndDateForUpdate(labId, date, List.of("PENDING", "APPROVED"));
        for (Reservation r : list) {
            if (excludeId != null && excludeId.equals(r.getId())) {
                continue;
            }
            LocalTime s = parse(r.getStartTime());
            LocalTime e = parse(r.getEndTime());
            if (start.isBefore(e) && s.isBefore(end)) {
                throw new BizException("时段冲突：该实验室 " + r.getStartTime() + " - " + r.getEndTime()
                        + " 已被占用（" + r.getTitle() + "）");
            }
        }
    }

    private boolean endBeforeNow(Reservation r) {
        return LocalDateTime.of(r.getDate(), parse(r.getEndTime())).isBefore(LocalDateTime.now());
    }

    private LocalTime parse(String time) {
        try {
            return LocalTime.parse(time);
        } catch (Exception e) {
            throw new BizException("时间格式不正确，应为 HH:mm");
        }
    }
}
