package btk.staj.WorkFlowProject.workflow.port;

import btk.staj.WorkFlowProject.workflow.model.CurrentActor;

/** Supplies the trusted non-HTTP actor used by automatic workflow transitions. */
public interface SystemActorProvider {

    CurrentActor systemActor();
}
