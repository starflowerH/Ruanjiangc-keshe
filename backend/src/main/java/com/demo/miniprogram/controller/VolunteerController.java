package com.demo.miniprogram.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.demo.miniprogram.common.Result;
import com.demo.miniprogram.dto.PageQueryDTO;
import com.demo.miniprogram.dto.VolunteerApplyDTO;
import com.demo.miniprogram.entity.Volunteer;
import com.demo.miniprogram.security.RequireRole;
import com.demo.miniprogram.service.VolunteerService;
import com.demo.miniprogram.vo.MemberVO;
import com.demo.miniprogram.vo.VolunteerPublicVO;
import com.demo.miniprogram.vo.VolunteerVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 志愿者接口 - 报名、查询、我的报名
 *
 * @author demo
 */
@RestController
@RequestMapping("/api/volunteer")
@RequiredArgsConstructor
public class VolunteerController {

    private final VolunteerService volunteerService;

    /**
     * 大学生提交报名（阶段1-报名期）
     *
     * 请求示例：
     *   POST /api/volunteer/apply
     *   Header: Authorization: Bearer {token}
     *   Body: {"name":"张三","phone":"138...","wechatId":"wx_zhang",...}
     *
     * @param openid 从JWT中获取的当前用户openid
     * @param dto    报名表单数据
     * @return 报名记录
     */
    @PostMapping("/apply")
    public Result<Volunteer> apply(@RequestAttribute("openid") String openid,
                                   @Valid @RequestBody VolunteerApplyDTO dto) {
        Volunteer volunteer = volunteerService.apply(openid, dto);
        return Result.ok("报名成功，请等待审核", volunteer);
    }

    /**
     * 高中生端 → 查看来本校宣传的"学长学姐"列表
     *
     * 无需登录态，通过高中生注册时选的高中ID查询
     *
     * 请求示例：
     *   GET /api/volunteer/by-high-school?highSchoolId=1
     *
     * @param highSchoolId 高中ID
     * @return 该高中对应的已确认志愿者列表（按大学分组）
     */
    @GetMapping("/by-high-school")
    public Result<List<VolunteerPublicVO>> listByHighSchool(@RequestParam Integer highSchoolId) {
        // 公开接口，只返回姓名/大学/专业，不返回手机号和微信号
        List<VolunteerPublicVO> list = volunteerService.listPublicByHighSchool(highSchoolId);
        return Result.ok(list);
    }

    /**
     * 查询当前用户的报名状态（个人中心）
     *
     * 请求示例：
     *   GET /api/volunteer/my-apply
     *   Header: Authorization: Bearer {token}
     *
     * @param openid 当前用户openid
     * @return 最近一次报名记录（含审核状态）
     */
    @GetMapping("/my-apply")
    public Result<IPage<VolunteerVO>> myApply(@RequestAttribute("openid") String openid,
                                              @Valid PageQueryDTO queryDTO) {
        // 用openid精确查询，不会看到别人的数据
        return Result.ok(volunteerService.listMyApply(openid, queryDTO.getPage(), queryDTO.getSize()));
    }

    /* ==================== 队长专属接口 ==================== */

    /**
     * 队长通讯录 - 查看其他队长列表
     *
     * 权限：仅队长可访问
     * 返回：其他队长的姓名、大学、队伍、目标高中和其主动填写的通讯方式
     *
     * 请求示例：
     *   GET /api/volunteer/captain-contacts
     *   Header: Authorization: Bearer {token}
     */
    @GetMapping("/captain-contacts")
    @RequireRole("CAPTAIN")
    public Result<List<MemberVO>> getCaptainContacts(
            @RequestAttribute("relatedId") Integer volunteerId) {
        return Result.ok(volunteerService.getCaptainContacts(volunteerId));
    }

    /**
     * 本队队员列表 - 队长查看自己队伍的成员
     *
     * 权限：仅队长可访问
     * 返回：本队队员的姓名+大学（不返回联系方式）
     *
     * 请求示例：
     *   GET /api/volunteer/my-team-members
     *   Header: Authorization: Bearer {token}
     */
    @GetMapping("/my-team-members")
    @RequireRole("CAPTAIN")
    public Result<List<MemberVO>> getMyTeamMembers(
            @RequestAttribute("relatedId") Integer volunteerId) {
        return Result.ok(volunteerService.getMyTeamMembers(volunteerId));
    }
}
