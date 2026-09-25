package btk.staj.WorkFlowProject.workflow.port;

import btk.staj.WorkFlowProject.workflow.model.TransitionRuleRecord;

import java.util.List;

/**
 * Aktif workflow gecis satirlarini persistence teknolojisinden bagimsiz bir
 * projection olarak okuyan port.
 *
 * <p>SM-7A kapsaminda bu portun gercek JPA adapter'i yoktur. Gelecekteki
 * adapter, yalnizca aktif satirlari bu sozlesmeye map ederek dondurecektir.
 */
public interface TransitionRuleRecordReader {

    List<TransitionRuleRecord> findAllActive();
}
