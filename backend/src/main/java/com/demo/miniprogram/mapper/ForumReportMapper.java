package com.demo.miniprogram.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.miniprogram.entity.ForumReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
@Mapper
public interface ForumReportMapper extends BaseMapper<ForumReport> {
    @Select("<script>SELECT fr.*, qa.title AS post_title, qa.question_content AS post_content, "
            + "fc.content AS comment_content, COALESCE(cv.name, cs.nickname, cs.name) AS comment_author_name "
            + "FROM forum_report fr "
            + "LEFT JOIN Youtube qa ON qa.id = fr.post_id "
            + "LEFT JOIN forum_comment fc ON fr.target_type = 'comment' AND fc.id = fr.comment_id "
            + "LEFT JOIN volunteer cv ON fc.user_type = 'volunteer' AND cv.id = fc.user_id "
            + "LEFT JOIN high_school_student cs ON fc.user_type = 'student' AND cs.id = fc.user_id "
            + "WHERE fr.status = 'PENDING' "
            + "<if test='universityId != null'>AND qa.university_id = #{universityId}</if> "
            + "ORDER BY fr.created_at ASC</script>")
    IPage<ForumReport> selectPendingPage(Page<ForumReport> page, @Param("universityId") Integer universityId);
}
