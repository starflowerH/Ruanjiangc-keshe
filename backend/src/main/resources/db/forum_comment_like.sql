CREATE TABLE IF NOT EXISTS forum_comment_like (
  id INT NOT NULL AUTO_INCREMENT,
  comment_id INT NOT NULL,
  user_id INT NOT NULL,
  user_type VARCHAR(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_comment_liker (comment_id, user_id, user_type),
  KEY idx_comment_like_comment (comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
