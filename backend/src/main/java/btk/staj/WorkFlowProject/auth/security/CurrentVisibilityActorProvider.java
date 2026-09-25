package btk.staj.WorkFlowProject.auth.security;

/** Supplies current role identity and permissions for record visibility readers. */
@FunctionalInterface
public interface CurrentVisibilityActorProvider {
    VisibilityActor currentVisibilityActor();
}
