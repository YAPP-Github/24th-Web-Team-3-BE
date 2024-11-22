package kr.mafoo.user.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Table("friend_invitation")
public class FriendInvitationEntity implements Persistable<String> {
    @Id
    @Column("invitation_id")
    private String id;

    @Column("from_member_id")
    private String fromMemberId;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @Transient
    private boolean isNew = false;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        FriendInvitationEntity that = (FriendInvitationEntity) obj;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static FriendInvitationEntity newInvitation(String id, String fromMemberId) {
        FriendInvitationEntity friend = new FriendInvitationEntity();
        friend.id = id;
        friend.fromMemberId = fromMemberId;
        friend.isNew = true;
        return friend;
    }
}
