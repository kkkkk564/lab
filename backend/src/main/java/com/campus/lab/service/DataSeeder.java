package com.campus.lab.service;

import com.campus.lab.entity.Announcement;
import com.campus.lab.entity.Device;
import com.campus.lab.entity.Lab;
import com.campus.lab.entity.Reservation;
import com.campus.lab.entity.User;
import com.campus.lab.repository.AnnouncementRepository;
import com.campus.lab.repository.DeviceRepository;
import com.campus.lab.repository.LabRepository;
import com.campus.lab.repository.ReservationRepository;
import com.campus.lab.repository.UserRepository;
import com.campus.lab.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * 首次启动时写入演示数据：9 个用户、6 个实验室、24 台设备、
 * 14 天历史预约 + 今日/未来预约、4 条公告。
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final LabRepository labRepository;
    private final DeviceRepository deviceRepository;
    private final ReservationRepository reservationRepository;
    private final AnnouncementRepository announcementRepository;

    public DataSeeder(UserRepository userRepository, LabRepository labRepository,
                      DeviceRepository deviceRepository, ReservationRepository reservationRepository,
                      AnnouncementRepository announcementRepository) {
        this.userRepository = userRepository;
        this.labRepository = labRepository;
        this.deviceRepository = deviceRepository;
        this.reservationRepository = reservationRepository;
        this.announcementRepository = announcementRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        log.info("首次启动，正在初始化演示数据 ...");

        Map<String, Long> userIds = seedUsers();
        Map<String, Long> labIds = seedLabs();
        seedDevices(labIds);
        seedReservations(userIds, labIds);
        seedAnnouncements();

        log.info("演示数据初始化完成：{} 用户，{} 实验室，{} 设备，{} 预约，{} 公告",
                userRepository.count(), labRepository.count(), deviceRepository.count(),
                reservationRepository.count(), announcementRepository.count());
    }

    private Map<String, Long> seedUsers() {
        String[][] users = {
                {"admin", "王建国", "ADMIN", "实验室与设备管理处", "13800000001", "admin@campus.edu.cn"},
                {"teacher1", "张明", "TEACHER", "计算机学院", "13800000002", "zhangming@campus.edu.cn"},
                {"teacher2", "李红", "TEACHER", "电子信息学院", "13800000003", "lihong@campus.edu.cn"},
                {"student1", "陈晓", "STUDENT", "计算机2201班", "13800000004", "chenxiao@stu.campus.edu.cn"},
                {"student2", "刘洋", "STUDENT", "计算机2202班", "13800000005", "liuyang@stu.campus.edu.cn"},
                {"student3", "赵磊", "STUDENT", "电子2201班", "13800000006", "zhaolei@stu.campus.edu.cn"},
                {"student4", "孙悦", "STUDENT", "电子2202班", "13800000007", "sunyue@stu.campus.edu.cn"},
                {"student5", "周婷", "STUDENT", "自动化2301班", "13800000008", "zhouting@stu.campus.edu.cn"},
                {"student6", "吴强", "STUDENT", "软件2302班", "13800000009", "wuqiang@stu.campus.edu.cn"},
        };
        Map<String, Long> ids = new HashMap<>();
        for (String[] u : users) {
            User user = new User();
            user.setUsername(u[0]);
            user.setPassword(PasswordUtil.hash("123456"));
            user.setName(u[1]);
            user.setRole(u[2]);
            user.setDepartment(u[3]);
            user.setPhone(u[4]);
            user.setEmail(u[5]);
            user.setActive(true);
            ids.put(u[0], userRepository.save(user).getId());
        }
        return ids;
    }

    private Map<String, Long> seedLabs() {
        Object[][] labs = {
                {"LAB-101", "程序设计实验室", "实验楼A", "A-101", "计算机", 48, "张明", "021-66130001", "08:00", "22:00", "OPEN",
                        "面向程序设计类课程上机与课外自主练习，预装主流开发环境与课程实验框架。",
                        "戴尔 OptiPlex 台式机 ×48、千兆交换机、投影仪、教师工作站"},
                {"LAB-102", "网络安全实验室", "实验楼A", "A-102", "计算机", 40, "张明", "021-66130002", "08:00", "22:00", "OPEN",
                        "网络安全攻防演练与课程实验，含隔离靶场环境。",
                        "防火墙、核心/接入交换机、安全审计终端 ×40"},
                {"LAB-201", "人工智能实验室", "实验楼B", "B-201", "计算机", 30, "张明", "021-66130003", "08:00", "22:00", "OPEN",
                        "机器学习/深度学习实验与科研训练，支持夜间批处理任务。",
                        "A100/A800 GPU 工作站、深度学习训练服务器、CUDA 开发终端"},
                {"LAB-202", "嵌入式实验室", "实验楼B", "B-202", "电子信息", 32, "李红", "021-66130004", "08:00", "22:00", "MAINTENANCE",
                        "嵌入式系统课程设计与竞赛训练（当前空调管路维修中，暂闭）。",
                        "嵌入式开发板套件、数字示波器、信号发生器、逻辑分析仪"},
                {"LAB-301", "通信原理实验室", "实验楼C", "C-301", "电子信息", 36, "李红", "021-66130005", "08:00", "22:00", "OPEN",
                        "通信原理与射频课程实验。",
                        "频谱分析仪、矢量网络分析仪、射频信号源、数字示波器"},
                {"LAB-302", "创新实践基地", "实验楼C", "C-302", "综合", 20, "王建国", "021-66130006", "00:00", "23:59", "OPEN",
                        "24 小时开放的创客空间，支持门禁刷卡进入，用于学科竞赛与自主创新项目。",
                        "3D 打印机、激光切割机、焊台、高性能创客终端"},
        };
        Map<String, Long> ids = new HashMap<>();
        for (Object[] l : labs) {
            Lab lab = new Lab();
            lab.setCode((String) l[0]);
            lab.setName((String) l[1]);
            lab.setBuilding((String) l[2]);
            lab.setRoom((String) l[3]);
            lab.setCategory((String) l[4]);
            lab.setCapacity((Integer) l[5]);
            lab.setManager((String) l[6]);
            lab.setPhone((String) l[7]);
            lab.setOpenTime((String) l[8]);
            lab.setCloseTime((String) l[9]);
            lab.setStatus((String) l[10]);
            lab.setDescription((String) l[11]);
            lab.setEquipment((String) l[12]);
            ids.put((String) l[0], labRepository.save(lab).getId());
        }
        return ids;
    }

    private void seedDevices(Map<String, Long> labIds) {
        Long l101 = labIds.get("LAB-101");
        Long l102 = labIds.get("LAB-102");
        Long l201 = labIds.get("LAB-201");
        Long l202 = labIds.get("LAB-202");
        Long l301 = labIds.get("LAB-301");
        Long l302 = labIds.get("LAB-302");

        device(l101, "台式计算机-Dell-01", "OptiPlex 7090", "NORMAL", "2023-09", 6800.0, null);
        device(l101, "台式计算机-Dell-02", "OptiPlex 7090", "NORMAL", "2023-09", 6800.0, null);
        device(l101, "千兆交换机", "H3C S1850V2", "NORMAL", "2023-09", 2200.0, null);
        device(l101, "投影仪", "Epson CB-FH52", "NORMAL", "2022-06", 5500.0, null);

        device(l102, "网络防火墙", "H3C SecPath F100", "NORMAL", "2024-03", 15800.0, null);
        device(l102, "核心交换机-01", "H3C S5560S", "NORMAL", "2024-03", 8600.0, null);
        device(l102, "接入交换机-02", "H3C S1850V2", "NORMAL", "2024-03", 2200.0, null);
        device(l102, "台式计算机-Lenovo-01", "ThinkCentre M760", "NORMAL", "2024-03", 7200.0, null);

        device(l201, "GPU工作站-A100", "NVIDIA DGX Station", "NORMAL", "2024-01", 98000.0, "深度学习训练专用");
        device(l201, "GPU工作站-A800", "NVIDIA A800 80G", "NORMAL", "2024-01", 120000.0, "支持夜间批处理任务");
        device(l201, "深度学习训练服务器", "Dell R750xa", "NORMAL", "2024-01", 85000.0, null);
        device(l201, "台式计算机-RTX-01", "RTX 4090 工作站", "NORMAL", "2024-01", 21000.0, null);

        device(l202, "嵌入式开发板套件", "STM32F4 + 传感器包", "NORMAL", "2023-05", 1800.0, null);
        device(l202, "数字示波器", "RIGOL DS1054Z", "NORMAL", "2023-05", 3580.0, null);
        device(l202, "信号发生器", "RIGOL DG1022Z", "NORMAL", "2023-05", 2980.0, null);
        device(l202, "逻辑分析仪", "Kingst LA5016", "FAULTY", "2023-05", 2600.0, "屏幕无显示，无法采集数据");

        device(l301, "频谱分析仪", "RIGOL RSA5054N", "NORMAL", "2022-11", 46000.0, null);
        device(l301, "矢量网络分析仪", "NanoVNA V2", "FAULTY", "2022-11", 3200.0, "开机报错 ERR-03，无法自校准");
        device(l301, "射频信号源", "RIGOL DSG815", "NORMAL", "2022-11", 23000.0, null);
        device(l301, "数字示波器-02", "RIGOL DS1104Z", "NORMAL", "2022-11", 4680.0, null);

        device(l302, "3D打印机", "Creality CR-10 Smart", "NORMAL", "2023-12", 4200.0, null);
        device(l302, "激光切割机", "Glowforge Pro", "REPAIRING", "2023-12", 32000.0, "激光模块更换中");
        device(l302, "台式计算机-创客-01", "i9 组装机", "NORMAL", "2023-12", 15000.0, null);
        device(l302, "焊台套装", "白光 T12 ×8", "NORMAL", "2023-12", 2400.0, null);
    }

    private void device(Long labId, String name, String model, String status, String purchaseDate, Double price, String fault) {
        Device d = new Device();
        d.setLabId(labId);
        d.setName(name);
        d.setModel(model);
        d.setStatus(status);
        d.setPurchaseDate(purchaseDate);
        d.setPrice(price);
        if (fault != null) {
            d.setFaultDesc(fault);
            d.setReportBy("孙悦");
            d.setReportAt(LocalDateTime.now().minusDays(2));
        }
        deviceRepository.save(d);
    }

    private void seedReservations(Map<String, Long> userIds, Map<String, Long> labIds) {
        Long l101 = labIds.get("LAB-101");
        Long l102 = labIds.get("LAB-102");
        Long l201 = labIds.get("LAB-201");
        Long l202 = labIds.get("LAB-202");
        Long l301 = labIds.get("LAB-301");
        Long l302 = labIds.get("LAB-302");

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);

        // ===== 过去 14 天历史预约（形成趋势数据） =====
        for (int i = 1; i <= 14; i++) {
            LocalDate d = today.minusDays(i);
            res(l101, userIds.get("student1"), "数据结构课程实验", "完成二叉树与图的上机实验", d, "08:00", "10:00", 2, "APPROVED");
            if (i % 3 == 0) {
                res(l201, userIds.get("teacher1"), "机器学习课程实验", "模型训练与超参数调优", d, "14:00", "17:00", 25, "APPROVED");
            }
            if (i % 4 == 0) {
                res(l102, userIds.get("student2"), "CTF 课外训练", "备战省级网络安全竞赛", d, "19:00", "21:00", 4, "APPROVED");
                res(l301, userIds.get("student3"), "通信仿真实验", "调制解调 MATLAB 仿真", d, "10:00", "12:00", 2, "CANCELLED");
            }
            if (i % 5 == 0) {
                res(l202, userIds.get("student4"), "嵌入式课程设计", "智能小车循迹调试", d, "15:00", "17:00", 6, "REJECTED");
            }
            if (i % 7 == 0) {
                res(l302, userIds.get("student5"), "开放自习", "考研复习", d, "20:00", "23:59", 1, "APPROVED");
            }
        }

        // ===== 今日：24 小时实验室当前使用中（覆盖当前时刻） =====
        LocalTime covStart;
        LocalTime covEnd;
        if (now.isBefore(LocalTime.of(2, 0))) {
            covStart = LocalTime.MIDNIGHT;
            covEnd = LocalTime.of(8, 0);
        } else {
            covStart = now.minusMinutes(now.getMinute());
            covEnd = covStart.plusHours(2);
            if (covEnd.isAfter(LocalTime.of(23, 59))) {
                covEnd = LocalTime.of(23, 59);
            }
        }
        res(l302, userIds.get("teacher1"), "深度学习模型训练（夜间批处理）", "使用 A100 服务器批量训练视觉模型",
                today, fmt(covStart), fmt(covEnd), 1, "APPROVED");

        // ===== 今日：白天时段的「使用中 / 即将开始」 =====
        if (!now.isBefore(LocalTime.of(9, 0)) && !now.isAfter(LocalTime.of(19, 0))) {
            LocalTime s2 = now.minusMinutes(now.getMinute());
            res(l101, userIds.get("student2"), "Java 程序设计实践", "课程综合实验", today, fmt(s2), fmt(s2.plusHours(2)), 30, "APPROVED");
            LocalTime s3 = now.plusMinutes(40);
            if (s3.isBefore(LocalTime.of(21, 30))) {
                res(l201, userIds.get("student1"), "算法竞赛集训", "ICPC 区域赛赛前集训", today, fmt(s3), fmt(s3.plusHours(2)), 8, "APPROVED");
            }
        }

        // ===== 今日上午已完成（当前时间已过 10 点时） =====
        if (now.isAfter(LocalTime.of(10, 0))) {
            res(l101, userIds.get("student6"), "毕业设计调试", "Spring Boot 项目联调", today, "08:00", "10:00", 1, "APPROVED");
        }

        // ===== 明日：待审批队列 + 已批准 =====
        LocalDate tomorrow = today.plusDays(1);
        res(l101, userIds.get("student3"), "操作系统课程实验", "进程调度模拟实验", tomorrow, "19:00", "21:00", 2, "PENDING");
        res(l201, userIds.get("teacher2"), "深度学习课程实验", "卷积神经网络实践", tomorrow, "09:00", "11:00", 28, "PENDING");
        res(l102, userIds.get("student4"), "网络安全技能训练", "靶场环境渗透练习", tomorrow, "14:00", "16:00", 3, "PENDING");
        res(l302, userIds.get("student6"), "创客项目开发", "智能小车调试", tomorrow, "19:00", "21:00", 2, "PENDING");
        res(l101, userIds.get("teacher1"), "Java 程序设计课程上机", "第 5 周上机课", tomorrow, "10:00", "12:00", 45, "APPROVED");

        // ===== 未来一周已批准 =====
        res(l301, userIds.get("student5"), "通信原理课程实验", "调制解调仿真", today.plusDays(2), "13:00", "15:00", 2, "APPROVED");
        res(l102, userIds.get("student1"), "密码学实验", "AES/RSA 算法实现", today.plusDays(3), "16:00", "18:00", 1, "APPROVED");
        res(l302, userIds.get("student2"), "开放自习", "考研复习", today.plusDays(4), "20:00", "22:00", 1, "APPROVED");
        res(l201, userIds.get("teacher1"), "科研组会", "论文复现讨论", today.plusDays(5), "09:00", "11:00", 10, "APPROVED");
        res(l101, userIds.get("student6"), "课程设计冲刺", "数据库课程设计", today.plusDays(6), "08:00", "10:00", 2, "APPROVED");
        res(l301, userIds.get("teacher2"), "通信原理课程上机", "第 6 周上机课", today.plusDays(7), "10:00", "12:00", 34, "APPROVED");
    }

    private void res(Long labId, Long userId, String title, String purpose, LocalDate date,
                     String startTime, String endTime, int headcount, String status) {
        Reservation r = new Reservation();
        r.setLabId(labId);
        r.setUserId(userId);
        r.setTitle(title);
        r.setPurpose(purpose);
        r.setDate(date);
        r.setStartTime(startTime);
        r.setEndTime(endTime);
        r.setHeadcount(headcount);
        r.setStatus(status);
        r.setCreatedAt(LocalDateTime.now().minusDays(date.isBefore(LocalDate.now()) ? 3 : 1).withSecond(0).withNano(0));
        if ("APPROVED".equals(status)) {
            r.setReviewedBy("王建国");
            r.setReviewedAt(r.getCreatedAt().plusHours(2));
        } else if ("REJECTED".equals(status)) {
            r.setReviewedBy("王建国");
            r.setReviewedAt(r.getCreatedAt().plusHours(2));
            r.setReviewComment("与课程实验时间冲突，请改期后重新提交");
        } else if ("CANCELLED".equals(status)) {
            r.setReviewComment("申请人因课程调整主动取消");
        }
        reservationRepository.save(r);
    }

    private void seedAnnouncements() {
        announcement("2026-2027 学年秋季学期实验室开放公告",
                "各实验室自第 1 教学周起正常开放，开放时间以实验室详情页为准。请通过本系统提前预约，"
                        + "进入实验室请刷卡签到并遵守实验室安全管理规定。创新实践基地（LAB-302）全天 24 小时开放。",
                "实验室与设备管理处", true, 14);
        announcement("实验室安全管理规定（必读）",
                "一、禁止携带食物饮品进入实验室；二、实验结束后请关闭设备电源并整理台面；"
                        + "三、发现设备异常请立即停止使用并在系统内提交报修；四、贵重设备使用需经指导教师批准。",
                "实验室与设备管理处", true, 12);
        announcement("嵌入式实验室（LAB-202）维护通知",
                "嵌入式实验室因空调管路维修暂停开放，预计一周内恢复。已提交该实验室预约申请的同学请改约其他实验室，"
                        + "给您带来不便敬请谅解。",
                "李红", false, 3);
        announcement("设备报修流程说明",
                "登录系统后在「设备管理」页面找到故障设备，点击「报修」并填写故障描述，管理员将统一安排维修。"
                        + "维修完成后设备状态将恢复为正常。",
                "实验室与设备管理处", false, 7);
    }

    private void announcement(String title, String content, String publisher, boolean pinned, int daysAgo) {
        Announcement a = new Announcement();
        a.setTitle(title);
        a.setContent(content);
        a.setPublisher(publisher);
        a.setPinned(pinned);
        a.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        announcementRepository.save(a);
    }

    private String fmt(LocalTime t) {
        return t.truncatedTo(ChronoUnit.MINUTES).toString();
    }
}
