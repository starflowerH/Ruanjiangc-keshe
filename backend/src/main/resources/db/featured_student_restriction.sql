-- 高中生内容不可设为精华（幂等迁移，可重复执行）
-- 历史数据收敛：把高中生已设为精华的帖子取消精华
UPDATE Youtube
SET is_featured = 0, featured_by_type = NULL, featured_by_id = NULL, featured_at = NULL
WHERE author_type = 'student' AND is_featured = 1;
