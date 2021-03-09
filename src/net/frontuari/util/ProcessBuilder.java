package net.frontuari.util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MPInstance;
import org.compiere.model.MPInstancePara;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ServerProcessCtl;
import org.compiere.process.SvrProcess;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;

/**
 * 
 * @author Argenis Rodríguez arodriguez@frontuari.net
 *
 */
public class ProcessBuilder {

	private int AD_Process_ID = 0;
	private int Record_ID = 0;
	private int Table_ID = 0;
	private String trxName = null;
	private String processMsg = null;
	private ProcessInfo pi = null;
	
	private CLogger logger = CLogger.getCLogger(ProcessBuilder.class);
	
	/** The paramaters in Format Key <-> Value String <-> Object*/
	private HashMap<String, Object> params = new HashMap<String, Object>();
	
	private ProcessBuilder(int AD_Process_ID) {
		this.AD_Process_ID = AD_Process_ID;
	}
	
	/**
	 * 
	 * @param AD_Process_ID
	 * @param Record_ID
	 */
	private ProcessBuilder(int AD_Process_ID, int Record_ID) {
		this.AD_Process_ID = AD_Process_ID;
		this.Record_ID = Record_ID;
	}
	
	public int getProcess_ID() {
		return AD_Process_ID;
	}
	
	public ProcessBuilder setTable_ID(int Table_ID) {
		this.Table_ID = Table_ID;
		return this;
	}
	
	public int getTable_ID() {
		return Table_ID;
	}
	
	public int getRecord_ID() {
		return Record_ID;
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param clazz
	 * @return
	 */
	public static ProcessBuilder build(Class<? extends SvrProcess> clazz) {
		
		int AD_Process_ID = getProcessId(clazz.getCanonicalName());
		
		if (AD_Process_ID <= 0)
			throw new AdempiereException("@FillMandatory@ @AD_Process_ID@");
		
		ProcessBuilder retVal = new ProcessBuilder(AD_Process_ID);
		
		return retVal;
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @return success
	 */
	public boolean execute() {
		
		MPInstance instance = new MPInstance(Env.getCtx(), AD_Process_ID, Record_ID);
		
		if (!instance.save())
		{
			processMsg = Msg.getMsg(Env.getCtx(), "ProcessNoInstance");
			return false;
		}
		
		pi = new ProcessInfo("", AD_Process_ID, Table_ID, Record_ID);
		pi.setAD_PInstance_ID(instance.get_ID());
		
		if (Table_ID > 0 && Record_ID > 0)
		{
			MTable table = new MTable(Env.getCtx(), Table_ID, trxName);
			
			if (table == null || table.get_ID() == 0)
			{
				processMsg = Msg.parseTranslation(Env.getCtx(), "@NotFound@ @AD_Table_ID@ = " + Table_ID);
				return false;
			}
			
			PO po = table.getPO(Record_ID, trxName);
			
			if (po == null || po.get_ID() == 0)
			{
				processMsg = Msg.parseTranslation(Env.getCtx(), "@NotFound@ @Record_ID@ = " + Record_ID);
				return false;
			}
			
			pi.setPO(po);
		}
		
		//Add Parameters
		Set<Entry<String, Object>> entries = params.entrySet();
		int seqNo = 10;
		
		for (Entry<String, Object> entry: entries)
		{
			MPInstancePara instancePara = new MPInstancePara(instance, seqNo);
			setParameter(instancePara, entry.getKey(), entry.getValue());
			
			if (!instancePara.save())
			{
				processMsg = "No could Save Parameter " + entry.getKey();
				logger.log(Level.SEVERE, processMsg);
				return false;
			}
			seqNo += 10;
		}
		
		//Execute Process
		Trx m_trx = trxName != null ? Trx.get(trxName, false) : null;
		ServerProcessCtl worker = new ServerProcessCtl(pi, m_trx);
		worker.run();
		
		return true;
	}
	
	public boolean isError() {
		return pi != null ? pi.isError(): false;
	}
	
	public String getProcessMsg() {
		
		if (processMsg == null || processMsg.isEmpty())
			processMsg = pi.getSummary();
		
		return processMsg;
	}
	
	private static void setParameter(MPInstancePara instancePara, String name, Object value) {
		
		if (value == null)
			instancePara.setParameterName(name);
		else if (value instanceof BigDecimal)
			instancePara.setParameter(name, (BigDecimal) value);
		else if (value instanceof String)
			instancePara.setParameter(name, (String) value);
		else if (value instanceof Integer)
			instancePara.setParameter(name, ((Integer) value).intValue());
		else if (value instanceof Timestamp)
			instancePara.setParameter(name, (Timestamp) value);
		else if (value instanceof Boolean)
			instancePara.setParameter(name, ((Boolean) value).booleanValue());
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param AD_Process_ID
	 * @return
	 */
	public static ProcessBuilder build(int AD_Process_ID) {
		
		if (AD_Process_ID <= 0)
			throw new AdempiereException("@FillMandatory@ @AD_Process_ID@");
		
		ProcessBuilder retVal = new ProcessBuilder(AD_Process_ID);
		
		return retVal;
	}
	
	public ProcessBuilder withTrxName(String trxName) {
		this.trxName = trxName;
		return this;
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param name
	 * @param value
	 */
	public ProcessBuilder withParameter(String name, Object value) {
		params.put(name, value);
		return this;
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param Record_ID
	 */
	public ProcessBuilder setRecord_ID(int Record_ID) {
		this.Record_ID = Record_ID;
		return this;
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @param className
	 * @return
	 */
	private static int getProcessId(String className) {
		
		int AD_Process_ID = DB.getSQLValue(null, "SELECT AD_Process_ID FROM AD_Process WHERE ClassName = ?", new Object[] {
			className
		});
		
		return AD_Process_ID;
	}
}
