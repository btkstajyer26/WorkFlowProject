package btk.staj.WorkFlowProject.auth.security;

import java.util.UUID;

/** Authenticated identity for operations that do not require a workflow role. */
public interface CurrentUserProvider {
    UUID currentUserId();
}
