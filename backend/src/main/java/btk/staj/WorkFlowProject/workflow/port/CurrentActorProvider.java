package btk.staj.WorkFlowProject.workflow.port;

import btk.staj.WorkFlowProject.workflow.model.CurrentActor;

/** Supplies the authenticated actor without coupling workflow to Spring Security. */
public interface CurrentActorProvider {

    CurrentActor currentActor();
}
