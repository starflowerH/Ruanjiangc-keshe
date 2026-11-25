package com.demo.miniprogram.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.miniprogram.common.BusinessException;
import com.demo.miniprogram.common.ResultCode;
import com.demo.miniprogram.dto.AdminLoginDTO;
import com.demo.miniprogram.dto.TeacherRegisterDTO;
import com.demo.miniprogram.dto.WxLoginDTO;
import com.demo.miniprogram.entity.*;
import com.demo.miniprogram.mapper.*;
import com.demo.miniprogram.util.JwtUtil;
import com.demo.miniprogram.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证服务 - 处理微信登录、管理员登录、手机号绑定
 *
 * @author demo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserWechatMapper userWechatMapper;
    private final AdminUserMapper adminUserMapper;
    private final VolunteerMapper volunteerMapper;
    private final HighSchoolStudentMapper highSchoolStudentMapper;
    private final PromoTeamMapper promoTeamMapper;
    private final UniversityMapper universityMapper;
    private final HighSchoolMapper highSchoolMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${wechat.miniapp-appid:wx0000000000000000}")
    private String miniappAppid;

    @Value("${wechat.miniapp-secret:your_miniapp_secret_here}")
    private String miniappSecret;

    private static final String WX_LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session";
    private static final String WX_PHONE_URL = "https://api.weixin.qq.com/wxa/business/getuserphonenumber";

    /**
     * 微信小程序登录
     *
     * 流程：
     *   1. 用 code 换 openid + session_key
     *   2. 如果传了 phoneCode，调用微信接口获取手机号
     *   3. 创建或更新用户记录
     *   4. 生成 JWT 返回
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginVO wxLogin(WxLoginDTO dto) {
        // 步骤1：用 code 换 openid
        JSONObject wxResult = code2Session(dto.getCode());
        String openid = wxResult.getStr("openid");
        String sessionKey = wxResult.getStr("session_key");

        if (StrUtil.isBlank(openid)) {
            throw new BusinessException(ResultCode.WX_LOGIN_ERROR, "微信登录失败，请重试");
        }

        // 步骤2：如果传了 phoneCode，获取手机号
        String phone = null;
        if (StrUtil.isNotBlank(dto.getPhoneCode())) {
            phone = getPhoneNumber(dto.getPhoneCode(), sessionKey);
        }

        // 步骤3：查找或创建用户
        UserWechat user = userWechatMapper.selectById(openid);
        if (user == null) {
            // 新用户
            user = new UserWechat();
            user.setOpenid(openid);
            user.setUnionid(wxResult.getStr("unionid"));
            user.setNickname(dto.getNickname());
            user.setAvatarUrl(dto.getAvatarUrl());
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userWechatMapper.insert(user);
            log.info("新用户注册: openid={}", openid);
        } else {
            // 老用户，更新信息
            boolean needUpdate = false;
            if (StrUtil.isNotBlank(dto.getNickname()) && !dto.getNickname().equals(user.getNickname())) {
                user.setNickname(dto.getNickname());
                needUpdate = true;
            }
            if (StrUtil.isNotBlank(dto.getAvatarUrl()) && !dto.getAvatarUrl().equals(user.getAvatarUrl())) {
                user.setAvatarUrl(dto.getAvatarUrl());
                needUpdate = true;
            }
            if (needUpdate) {
                user.setUpdatedAt(LocalDateTime.now());
                userWechatMapper.updateById(user);
            }
        }

        // 按用户在登录页选择的身份返回对应资料。未指定时兼容旧版，
        // 优先已注册的志愿者身份（含被取消/待审核等任意状态，防止仅剩非APPROVED
        // 记录时被误判为"无身份新用户"导致界面进不去）
        String identityRole = normalizeIdentityRole(dto.getIdentityRole());
        if (identityRole == null) {
            identityRole = findLatestVolunteerAny(openid) != null ? "volunteer" : "student";
        }
        return buildWechatIdentityLogin(user, identityRole, phone);
    }

    /** 在同一微信账号的高中生/志愿者身份之间切换。 */
    public LoginVO switchIdentity(String openid, String identityRole) {
        String normalizedRole = normalizeIdentityRole(identityRole);
        if (normalizedRole == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "身份只能是 student 或 volunteer");
        }
        UserWechat user = userWechatMapper.selectById(openid);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "微信账号不存在，请重新登录");
        }
        return buildWechatIdentityLogin(user, normalizedRole, null);
    }

    private LoginVO buildWechatIdentityLogin(UserWechat user, String identityRole, String loginPhone) {
        String openid = user.getOpenid();
        HighSchoolStudent student = null;
        Volunteer volunteer = null;

        if ("student".equals(identityRole)) {
            student = highSchoolStudentMapper.selectOne(
                    new LambdaQueryWrapper<HighSchoolStudent>()
                            .eq(HighSchoolStudent::getOpenid, openid)
                            .last("LIMIT 1")
            );
        } else {
            // 不限状态取最新报名记录：被取消(CANCELLED)/待审核(PENDING)/已拒绝(REJECTED)
            // 的用户也应获得其身份上下文(relatedId/status)，否则会被当成"无身份新用户"，
            // 前端触发 403 误报"服务异常"且要求重填昵称。写操作仍由 Service 层校验 APPROVED。
            volunteer = findLatestVolunteerAny(openid);
        }

        Integer relatedId = student != null ? student.getId() : volunteer != null ? volunteer.getId() : null;
        String resolvedType = relatedId == null ? "new_user" : identityRole;
        String token = jwtUtil.generateToken(openid, "wechat", relatedId, identityRole, null);

        LoginVO loginVO = LoginVO.builder()
                .token(token)
                .expiresIn(jwtUtil.getExpireSeconds())
                .openid(openid)
                .avatarUrl(user.getAvatarUrl())
                .userType(resolvedType)
                .relatedId(relatedId)
                .build();

        if (student != null) {
            loginVO.setNickname(identityNickname(student.getNickname(), student.getName(), user.getNickname()));
            loginVO.setName(student.getName());
            loginVO.setPhone(student.getPhone() != null ? student.getPhone() : loginPhone);
            loginVO.setHighSchoolId(student.getHighSchoolId());
            HighSchool highSchool = highSchoolMapper.selectById(student.getHighSchoolId());
            loginVO.setHighSchoolName(highSchool != null ? highSchool.getName() : null);
        } else if (volunteer != null) {
            loginVO.setNickname(volunteer.getName());
            loginVO.setName(volunteer.getName());
            loginVO.setPhone(volunteer.getPhone() != null ? volunteer.getPhone() : loginPhone);
            loginVO.setWechatId(volunteer.getWechatId());
            loginVO.setUniversityId(volunteer.getUniversityId());
            loginVO.setGrade(volunteer.getGrade());
            loginVO.setMajor(volunteer.getMajor());
            loginVO.setStatus(volunteer.getStatus());
            loginVO.setTargetHighSchoolId(volunteer.getTargetHighSchoolId());

            University university = universityMapper.selectById(volunteer.getUniversityId());
            loginVO.setUniversityName(university != null ? university.getName() : null);
            HighSchool targetHighSchool = highSchoolMapper.selectById(volunteer.getTargetHighSchoolId());
            loginVO.setTargetHighSchoolName(targetHighSchool != null ? targetHighSchool.getName() : null);
            loginVO.setIsCaptain(promoTeamMapper.exists(
                    new LambdaQueryWrapper<PromoTeam>()
                            .eq(PromoTeam::getCaptainVolunteerId, volunteer.getId())
            ));
        } else {
            // 仅用于新身份注册时预填，注册完成后昵称保存在对应身份表。
            loginVO.setNickname(user.getNickname());
            loginVO.setPhone(loginPhone);
            loginVO.setIsCaptain(false);
        }
        return loginVO;
    }

    private String identityNickname(String nickname, String name, String wechatNickname) {
        if (StrUtil.isNotBlank(nickname)) return nickname;
        if (StrUtil.isNotBlank(name)) return name;
        return wechatNickname;
    }

    /** 仅查 APPROVED 的最新报名记录（身份切换/资格校验等严格场景用） */
    private Volunteer findLatestVolunteer(String openid) {
        List<Volunteer> volunteers = volunteerMapper.selectList(
                new LambdaQueryWrapper<Volunteer>()
                        .eq(Volunteer::getOpenid, openid)
                        .eq(Volunteer::getStatus, "APPROVED")
                        .orderByDesc(Volunteer::getUpdatedAt)
                        .orderByDesc(Volunteer::getId)
        );
        return volunteers.isEmpty() ? null : volunteers.get(0);
    }

    /** 不限状态取最新报名记录（登录身份上下文恢复用：含 CANCELLED/PENDING/REJECTED） */
    private Volunteer findLatestVolunteerAny(String openid) {
        List<Volunteer> volunteers = volunteerMapper.selectList(
                new LambdaQueryWrapper<Volunteer>()
                        .eq(Volunteer::getOpenid, openid)
                        .orderByDesc(Volunteer::getUpdatedAt)
                        .orderByDesc(Volunteer::getId)
                        .last("LIMIT 1")
        );
        return volunteers.isEmpty() ? null : volunteers.get(0);
    }

    private String normalizeIdentityRole(String identityRole) {
        return "student".equals(identityRole) || "volunteer".equals(identityRole)
                ? identityRole
                : null;
    }

    /**
     * 管理员登录（后台管理系统）
     */
    public LoginVO adminLogin(AdminLoginDTO dto) {
        // 查找管理员
        AdminUser admin = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>()
                        .eq(AdminUser::getUsername, dto.getUsername())
        );

        if (admin == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 校验密码
        if (!passwordEncoder.matches(dto.getPassword(), admin.getPasswordHash())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 检查状态
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }

        // 生成JWT
        String token = jwtUtil.generateToken(
                "admin_" + admin.getId(),
                "admin",
                admin.getId(),
                admin.getRole(),
                admin.getUniversityId()
        );

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setOpenid("admin_" + admin.getId());
        loginVO.setNickname(admin.getName());
        loginVO.setUserType("admin");
        loginVO.setRole(admin.getRole());
        loginVO.setRelatedId(admin.getId());
        loginVO.setPermissions(admin.getPermissions());
        loginVO.setUniversityId(admin.getUniversityId());

        // 如果是老师，加载大学名称
        if ("TEACHER".equals(admin.getRole()) && admin.getUniversityId() != null) {
            University university = universityMapper.selectById(admin.getUniversityId());
            if (university != null) {
                loginVO.setUniversityName(university.getName());
            }
        }

        return loginVO;
    }

    /**
     * 绑定手机号（已登录用户单独绑定）
     */
    @Transactional(rollbackFor = Exception.class)
    public String bindPhone(String openid, String phoneCode) {
        String phone = getPhoneNumber(phoneCode, null);

        UserWechat user = userWechatMapper.selectById(openid);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }

        // 这里可以更新用户手机号到相关表
        // 具体逻辑根据业务需求实现

        log.info("手机号绑定成功: openid={}, phone={}", openid, phone);
        return phone;
    }

    /**
     * 老师注册（微信授权登录，无需密令）
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginVO teacherRegister(TeacherRegisterDTO dto) {
        // 步骤1：用 code 换 openid
        JSONObject wxResult = code2Session(dto.getCode());
        String openid = wxResult.getStr("openid");

        if (StrUtil.isBlank(openid)) {
            throw new BusinessException(ResultCode.WX_LOGIN_ERROR, "微信登录失败，请重试");
        }

        // 步骤2：检查该微信是否已注册为老师
        AdminUser existingAdmin = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>()
                        .eq(AdminUser::getUsername, "teacher_" + openid)
        );
        if (existingAdmin != null) {
            throw new BusinessException("该微信已注册为老师");
        }

        // 步骤3：验证大学是否存在
        University university = universityMapper.selectById(dto.getUniversityId());
        if (university == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "所属大学不存在");
        }

        // 步骤4：创建管理员账号（角色为TEACHER）
        AdminUser admin = new AdminUser();
        admin.setUsername("teacher_" + openid);
        admin.setPasswordHash(passwordEncoder.encode(openid)); // 微信openid作为密码凭证
        admin.setRole("TEACHER");
        admin.setUniversityId(dto.getUniversityId());
        admin.setName(dto.getName());
        admin.setPhone(dto.getPhone());
        admin.setPermissions("{\"teacherVolunteerAudit\":false,\"teacherTeamManage\":false,\"teacherPromotionManage\":false,\"teacherForumModeration\":false}");
        // 安全修复：注册后默认禁用（status=0），须由超管审核启用后才可登录，防止任意人自封大学老师
        admin.setStatus(0);
        admin.setCreatedAt(LocalDateTime.now());
        adminUserMapper.insert(admin);

        // 步骤5：不签发可用JWT——账号待审核，登录由 teacher-login 在校验 status 后放行
        LoginVO loginVO = new LoginVO();
        loginVO.setOpenid("admin_" + admin.getId());
        loginVO.setNickname(admin.getName());
        loginVO.setUserType("admin");
        loginVO.setRole(admin.getRole());
        loginVO.setRelatedId(admin.getId());
        loginVO.setPermissions(admin.getPermissions());
        loginVO.setUniversityId(admin.getUniversityId());
        loginVO.setUniversityName(university.getName());

        log.info("老师注册成功(待审核启用): name={}, university={}", dto.getName(), university.getName());
        return loginVO;
    }

    /**
     * 老师登录（微信授权登录，无需密令）
     */
    public LoginVO teacherLogin(String code) {
        // 步骤1：用 code 换 openid
        JSONObject wxResult = code2Session(code);
        String openid = wxResult.getStr("openid");

        if (StrUtil.isBlank(openid)) {
            throw new BusinessException(ResultCode.WX_LOGIN_ERROR, "微信登录失败，请重试");
        }

        // 步骤2：查找老师账号
        AdminUser admin = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>()
                        .eq(AdminUser::getUsername, "teacher_" + openid)
                        .eq(AdminUser::getRole, "TEACHER")
        );

        if (admin == null) {
            throw new BusinessException("该微信未注册为老师，请先注册");
        }

        // 检查状态
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }

        // 步骤3：生成JWT
        String token = jwtUtil.generateToken(
                "admin_" + admin.getId(),
                "admin",
                admin.getId(),
                admin.getRole(),
                admin.getUniversityId()
        );

        // 步骤4：构建返回值
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setOpenid("admin_" + admin.getId());
        loginVO.setNickname(admin.getName());
        loginVO.setUserType("admin");
        loginVO.setRole(admin.getRole());
        loginVO.setRelatedId(admin.getId());
        loginVO.setUniversityId(admin.getUniversityId());

        // 加载大学名称
        if (admin.getUniversityId() != null) {
            University university = universityMapper.selectById(admin.getUniversityId());
            if (university != null) {
                loginVO.setUniversityName(university.getName());
            }
        }

        return loginVO;
    }

    /* ========== 微信 access_token 缓存（线程安全） ========== */

    /** 缓存的 access_token */
    private volatile String cachedAccessToken;

    /** token 过期时间戳（毫秒），提前5分钟过期以留出缓冲 */
    private volatile long tokenExpireTime = 0;

    /** 缓存锁对象 */
    private final Object tokenLock = new Object();

    /** access_token 有效期（毫秒），微信默认7200秒，这里提前300秒（5分钟）刷新 */
    private static final long TOKEN_TTL_MS = (7200 - 300) * 1000L;

    /**
     * 获取微信接口调用凭证 access_token（带线程安全缓存）
     *
     * 使用 volatile + synchronized 双重检查锁保证：
     *   - 多线程并发调用时只请求一次微信 API
     *   - token 提前5分钟刷新，避免临界时段的调用失败
     *   - 缓存失效时自动重新获取
     */
    private String getAccessToken() {
        // 第一次检查（无锁，快速路径）：缓存有效直接返回
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return cachedAccessToken;
        }

        // 缓存失效，加锁重新获取
        synchronized (tokenLock) {
            // 第二次检查（有锁）：防止多个线程同时进入临界区后重复请求
            if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpireTime) {
                return cachedAccessToken;
            }

            // 请求微信 API 获取新 token
            String url = String.format(
                    "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
                    miniappAppid, miniappSecret);

            HttpResponse response = HttpRequest.get(url).execute();
            JSONObject result = JSONUtil.parseObj(response.body());

            if (result.containsKey("errcode") && result.getInt("errcode") != 0) {
                log.error("获取access_token失败: {}", result);
                throw new BusinessException("获取微信access_token失败");
            }

            // 更新缓存
            cachedAccessToken = result.getStr("access_token");
            tokenExpireTime = System.currentTimeMillis() + TOKEN_TTL_MS;
            log.info("access_token 已刷新，有效期至 {}",
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(tokenExpireTime + 300_000L)));

            return cachedAccessToken;
        }
    }

    /* ========== 私有辅助方法 ========== */

    /**
     * 用 code 换取 openid 和 session_key
     */
    private JSONObject code2Session(String code) {
        String url = String.format("%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                WX_LOGIN_URL, miniappAppid, miniappSecret, code);

        HttpResponse response = HttpRequest.get(url).execute();
        JSONObject result = JSONUtil.parseObj(response.body());

        if (result.containsKey("errcode") && result.getInt("errcode") != 0) {
            log.error("微信登录接口异常: {}", result);
            throw new BusinessException(ResultCode.WX_LOGIN_ERROR,
                    "微信登录失败: " + result.getStr("errmsg"));
        }

        return result;
    }

    /**
     * 通过 phoneCode 获取手机号（新版接口）
     */
    private String getPhoneNumber(String phoneCode, String sessionKey) {
        String accessToken = getAccessToken();

        String url = WX_PHONE_URL + "?access_token=" + accessToken;
        JSONObject body = new JSONObject();
        body.set("code", phoneCode);

        HttpResponse response = HttpRequest.post(url)
                .body(body.toString())
                .execute();
        JSONObject result = JSONUtil.parseObj(response.body());

        if (result.containsKey("errcode") && result.getInt("errcode") != 0) {
            log.error("获取手机号失败: {}", result);
            throw new BusinessException(ResultCode.WX_LOGIN_ERROR, "获取手机号失败");
        }

        JSONObject phoneInfo = result.getJSONObject("phone_info");
        if (phoneInfo == null) {
            log.error("微信返回的phone_info为空: {}", result);
            throw new BusinessException(ResultCode.WX_LOGIN_ERROR, "获取手机号失败");
        }
        return phoneInfo.getStr("purePhoneNumber");
    }
}
