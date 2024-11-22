package kr.mafoo.user.exception;

public class InvitationNotValidException extends DomainException {
    public InvitationNotValidException() {
        super(ErrorCode.INVITATION_NOT_VALID);
    }
}
