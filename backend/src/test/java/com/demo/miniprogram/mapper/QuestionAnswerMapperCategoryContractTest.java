package com.demo.miniprogram.mapper;

import com.demo.miniprogram.vo.QAVO;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionAnswerMapperCategoryContractTest {

    @Test
    void forumPostsExposeCategoryForClientSideRendering() throws Exception {
        assertDoesNotThrow(() -> QAVO.class.getDeclaredField("category"));

        Method method = QuestionAnswerMapper.class.getMethod(
                "selectForumPosts",
                com.baomidou.mybatisplus.extension.plugins.pagination.Page.class,
                String.class, String.class, Integer.class, Integer.class, String.class
        );
        String sql = String.join(" ", Arrays.asList(method.getAnnotation(Select.class).value()));

        assertTrue(sql.contains("qa.category"));
    }
}
