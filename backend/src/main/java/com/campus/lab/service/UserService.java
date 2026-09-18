package com.campus.lab.service;

import com.campus.lab.common.BizException;
import com.campus.lab.dto.UserInfo;
import com.campus.lab.dto.UserSaveRequest;
import com.campus.lab.entity.User;
import com.campus.lab.repository.ReservationRepository;
import com.campus.lab.repository.UserRepository;
import com.campus.lab.util.Auths;
import com.campus.lab.util.PasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;

    public UserService(UserRepository userRepository, ReservationRepository reservationRepository) {
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
    }

    public List<UserInfo> list(String keyword, String role) {
        return userRepository.findAll().stream()
                .filter(u -> role == null || role.isBlank() || role.equals(u.getRole()))
                .filter(u -> {
                    if (keyword == null || keyword.isBlank()) {
                        return true;
                    }
                    String k = keyword.trim().toLowerCase(Locale.ROOT);
                    return contains(u.getName(), k) || contains(u.getUsername(), k) || contains(u.getDepartment(), k);
                })
                .map(UserInfo::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserInfo create(UserSaveRequest req) {
        if (userRepository.findByUsername(req.username()).isPresent()) {
            throw new BizException("用户名已存在");
        }
        validateRole(req.role());
        User u = new User();
        u.setUsername(req.username());
        String pwd = req.password();
        u.setPassword(PasswordUtil.hash(pwd == null || pwd.isBlank() ? "123456" : pwd));
        u.setName(req.name());
        u.setRole(req.role());
        u.setDepartment(req.department());
        u.setPhone(req.phone());
        u.setEmail(req.email());
        u.setActive(req.active() == null || req.active());
        return UserInfo.from(userRepository.save(u));
    }

    @Transactional
    public UserInfo update(Long id, UserSaveRequest req) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new BizException("用户不存在"));
        validateRole(req.role());
        u.setName(req.name());
        u.setRole(req.role());
        u.setDepartment(req.department());
        u.setPhone(req.phone());
        u.setEmail(req.email());
        if (req.active() != null) {
            u.setActive(req.active());
        }
        return UserInfo.from(userRepository.save(u));
    }

    @Transactional
    public void delete(Long id, Long operatorId) {
        if (id.equals(operatorId)) {
            throw new BizException("不能删除当前登录账号");
        }
        if (!userRepository.existsById(id)) {
            throw new BizException("用户不存在");
        }
        // 全量校验，防止删除后该用户的预约悬挂
        long resCount = reservationRepository.countByUserId(id);
        if (resCount > 0) {
            throw new BizException("该用户名下存在 " + resCount + " 条预约记录，不允许删除");
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public void resetPassword(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new BizException("用户不存在"));
        u.setPassword(PasswordUtil.hash("123456"));
        userRepository.save(u);
    }

    private void validateRole(String role) {
        if (!Auths.ROLE_ADMIN.equals(role) && !Auths.ROLE_TEACHER.equals(role) && !Auths.ROLE_STUDENT.equals(role)) {
            throw new BizException("角色不合法，仅支持 STUDENT / TEACHER / ADMIN");
        }
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
