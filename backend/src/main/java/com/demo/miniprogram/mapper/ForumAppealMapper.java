package com.demo.miniprogram.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.miniprogram.entity.ForumAppeal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
@Mapper
public interface ForumAppealMapper extends BaseMapper<ForumAppeal> {
    @Select("<script>SELECT fa.*, fr.target_type AS target_type FROM forum_appeal fa "
            + "INNER JOIN forum_penalty fp ON fp.id = fa.penalty_id "
            + "INNER JOIN forum_report fr ON fr.id = fp.report_id "
            + "INNER JOIN Youtube qa ON qa.id = fr.post_id "
            + "WHERE fa.status = 'PENDING' "
            + "<if test='universityId != null'>AND qa.university_id = #{universityId}</if> "
            + "ORDER BY fa.created_at ASC</script>")
    IPage<ForumAppeal> selectPendingPage(Page<ForumAppeal> page, @Param("universityId") Integer universityId);
}
