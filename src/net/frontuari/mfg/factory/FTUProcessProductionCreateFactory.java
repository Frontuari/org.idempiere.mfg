package net.frontuari.mfg.factory;

import org.adempiere.base.IProcessFactory;
import org.compiere.process.ProcessCall;

import net.frontuari.process.ProcessFTUProductionCreate;


public class FTUProcessProductionCreateFactory implements IProcessFactory{

	@Override
	public ProcessCall newProcessInstance(String className) {
		if(className.equals("net.frontuari.process.ProcessFTUProductionCreate"))
			return new ProcessFTUProductionCreate();
		return null;
	}

}
