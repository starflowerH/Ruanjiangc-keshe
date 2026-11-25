package com.demo.miniprogram.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.miniprogram.common.BusinessException;
import com.demo.miniprogram.common.Result;
import com.demo.miniprogram.common.ResultCode;
import com.demo.miniprogram.dto.HighSchoolStudentRegisterDTO;
import com.demo.miniprogram.entity.HighSchool;
import com.demo.miniprogram.entity.HighSchoolStudent;
import com.demo.miniprogram.entity.UserWechat;
import com.demo.miniprogram.mapper.HighSchoolMapper;
import com.demo.miniprogram.mapper.HighSchoolStudentMapper;
import com.demo.miniprogram.mapper.UserWechatMapper;
import com.demo.miniprogram.util.JwtUtil;
import com.demo.miniprogram.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 高中生接口 - 注册、个人信息
 *
 * @author demo
 */
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final HighSchoolStudentMapper studentMapper;
    private final HighSchoolMapper highSchoolMapper;
    private final UserWechatMapper userWechatMapper;
    private final JwtUtil jwtUtil;

    /**
     * 高中生注册
     *
     * 流程：扫码关注公众号 → 填写手机号 + 选择所在高中 → 注册完成
     * 注册后系统自动匹配来过本校宣传的大学生
     *
     * 请求示例：
     *   POST /api/student/register
     *   Header: Authorization: Bearer {token}
     *   Body: {"phone": "138xxxx", "highSchoolId": 1}
     *
     * @param openid 当前微信用户OpenID（从JWT中获取）
     */
    @PostMapping("/register")
    public Result<LoginVO> register(@RequestAttribute("openid") String openid,
                                    @Valid @RequestBody HighSchoolStudentRegisterDTO dto) {
        // 校验：一个微信只能注册一个高中生身份
        HighSchoolStudent existing = studentMapper.selectOne(
                new LambdaQueryWrapper<HighSchoolStudent>().eq(HighSchoolStudent::getOpenid, openid)
        );
        if (existing != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS, "您已注册过高中生身份");
        }

        // 校验高中存在
        HighSchool highSchool = highSchoolMapper.selectById(dto.getHighSchoolId());
        if (highSchool == null) {
            throw new BusinessException("所选高中不存在");
        }

        // 创建高中生记录
        HighSchoolStudent student = new HighSchoolStudent();
        student.setOpenid(openid);
        student.setName(dto.getName());
        // 昵称兜底规则：优先用户填写 → 微信昵称 → 真实姓名
        // （避免高中生未设昵称时直接暴露真实姓名，保护隐私）
        String fallbackNickname = dto.getName();
        if (dto.getNickname() == null || dto.getNickname().isBlank()) {
            UserWechat wechat = userWechatMapper.selectById(openid);
            if (wechat != null && wechat.getNickname() != null && !wechat.getNickname().isBlank()) {
                fallbackNickname = wechat.getNickname();
            }
        } else {
            fallbackNickname = dto.getNickname().trim();
        }
        student.setNickname(fallbackNickname);
        student.setAvatarUrl(dto.getAvatarUrl());
        student.setPhone(dto.getPhone());
        student.setHighSchoolId(dto.getHighSchoolId());
        studentMapper.insert(student);

        // 身份绑定改变了 JWT 中的 relatedId，注册成功后必须签发新 Token。
        LoginVO loginVO = LoginVO.builder()
                .token(jwtUtil.generateToken(openid, "wechat", student.getId(), "student", null))
                .expiresIn(jwtUtil.getExpireSeconds())
                .openid(openid)
                .userType("student")
                .relatedId(student.getId())
                .nickname(student.getNickname())
                .name(student.getName())
                .avatarUrl(student.getAvatarUrl())
                .phone(student.getPhone())
                .highSchoolId(student.getHighSchoolId())
                .highSchoolName(highSchool.getName())
                .build();
        return Result.ok("注册成功", loginVO);
    }

    /**
     * 获取当前高中生的注册信息
     *
     * GET /api/student/my-info
     */
    @GetMapping("/my-info")
    public Result<HighSchoolStudent> myInfo(@RequestAttribute("openid") String openid) {
        HighSchoolStudent student = studentMapper.selectOne(
                new LambdaQueryWrapper<HighSchoolStudent>().eq(HighSchoolStudent::getOpenid, openid)
        );
        if (student == null) {
            return Result.fail(ResultCode.NOT_FOUND, "请先注册高中生身份");
        }
        HighSchool highSchool = highSchoolMapper.selectById(student.getHighSchoolId());
        if (highSchool != null) {
            student.setHighSchoolName(highSchool.getName());
        }
        return Result.ok(student);
    }
}
