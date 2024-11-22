CREATE TABLE friend_invitation(
                       invitation_id VARCHAR(26) PRIMARY KEY,
                       from_member_id INT,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       INDEX friend_invitation_idx_1 (from_member_id)
);
