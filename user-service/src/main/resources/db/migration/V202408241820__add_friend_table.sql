CREATE TABLE friend(
    friend_id CHAR(26) PRIMARY KEY,
    from_member_id INT,
    to_member_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX friend_idx_1 (from_member_id, to_member_id),
    INDEX friend_idx_2 (to_member_id, from_member_id)
);
