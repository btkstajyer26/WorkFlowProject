package btk.staj.WorkFlowProject.auth.security;

/** Supplies the existing system-role identity for record visibility readers. */
@FunctionalInterface
public interface CurrentVisibilityActorProvider {
    VisibilityActor currentVisibilityActor();
}
