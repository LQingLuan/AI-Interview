package com.university.smartinterview.service;

import com.university.smartinterview.dto.request.LoginRequest;
import com.university.smartinterview.dto.response.LoginResponse;
import com.university.smartinterview.dto.request.RegisterRequest;
import com.university.smartinterview.entity.SmartInterviewUser;
import com.university.smartinterview.repository.SmartInterviewUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final SmartInterviewUserRepository userRepository;

    /**
     * 用户登录
     */
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress) {
        String username = request.getUsername();

        // 1. 查询用户
        SmartInterviewUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        // 2. 检查账户状态
        if ("LOCKED".equals(user.getStatus())) {
            throw new RuntimeException("账户已被锁定");
        }
        if ("DELETED".equals(user.getStatus())) {
            throw new RuntimeException("账户已被删除");
        }
        if (user.getAccountLockedUntil() != null &&
                user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("账户临时锁定，请稍后再试");
        }

        // 3. 验证密码（使用 jBCrypt）
        if (!BCrypt.checkpw(request.getPassword(), user.getPasswordHash())) {
            // 密码错误，增加失败次数
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

            // 如果失败次数达到5次，锁定账户30分钟
            if (user.getFailedLoginAttempts() >= 5) {
                user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(30));
                user.setStatus("LOCKED");
                log.warn("用户 {} 因连续登录失败被锁定", username);
            }

            userRepository.save(user);
            throw new RuntimeException("用户名或密码错误");
        }

        // 4. 登录成功，更新登录信息
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);

        log.info("用户登录成功: {}", username);

        // 5. 返回响应（使用Long类型的ID，但LoginResponse中改为Integer）
        return LoginResponse.builder()
                .success(true)
                .message("登录成功")
                .username(user.getUsername())
                .userId(user.getId().intValue())  // 转换为Integer类型
                .userType(user.getUserType())
                .build();
    }

    /**
     * 用户注册
     */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        // 1. 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        // 2. 检查邮箱是否已存在（如果有邮箱）
        // 关键修改：将空字符串转换为null
        String email = request.getEmail();
        if (email != null && email.trim().isEmpty()) {
            email = null;
        }

        if (email != null && userRepository.existsByEmail(email)) {
            throw new RuntimeException("邮箱已被注册");
        }

        // 3. 同样处理手机号
        String phone = request.getPhone();
        if (phone != null && phone.trim().isEmpty()) {
            phone = null;
        }

        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new RuntimeException("手机号已被注册");
        }

        // 4. 生成业务ID
        String userId = "USER_" + System.currentTimeMillis() +
                String.format("%06d", (int)(Math.random() * 1000000));

        // 5. 使用 jBCrypt 加密密码
        String hashedPassword = BCrypt.hashpw(request.getPassword(), BCrypt.gensalt());

        SmartInterviewUser user = SmartInterviewUser.builder()
                .userId(userId)  // 设置业务ID
                .username(request.getUsername())
                .passwordHash(hashedPassword)
                .email(email)  // 使用转换后的email
                .phone(phone)  // 使用转换后的phone
                .userType("REGULAR")
                .status("ACTIVE")
                .build();

        userRepository.save(user);
        log.info("用户注册成功: {}", request.getUsername());

        // 6. 返回响应（使用Long类型的ID，但转换为Integer）
        return LoginResponse.builder()
                .success(true)
                .message("注册成功")
                .username(user.getUsername())
                .userId(user.getId().intValue())  // 转换为Integer类型
                .userType(user.getUserType())
                .build();
    }

    /**
     * 检查用户名是否可用
     */
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    /**
     * 检查邮箱是否可用
     */
    public boolean isEmailAvailable(String email) {
        if (email == null || email.trim().isEmpty()) {
            return true;  // 空邮箱被认为是可用的
        }
        return !userRepository.existsByEmail(email);
    }

    /**
     * 获取用户信息（根据用户名）
     */
    public SmartInterviewUser getUserInfo(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    /**
     * 根据用户ID获取用户信息
     */
    public SmartInterviewUser getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    /**
     * 根据用户ID获取用户信息（返回Object类型，为了兼容性）
     */
    public Object getUser(Long userId) {
        return getUserById(userId);
    }

    /**
     * 更新用户最后活动时间
     */
    @Transactional
    public void updateLastActivity(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastActivityAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }
}