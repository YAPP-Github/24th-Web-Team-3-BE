package kr.mafoo.user.exception;

public class CannotBeFriendMyselfException extends DomainException {
    public CannotBeFriendMyselfException() {
        super(ErrorCode.CANNOT_MAKE_FRIEND_MYSELF);
    }
}
