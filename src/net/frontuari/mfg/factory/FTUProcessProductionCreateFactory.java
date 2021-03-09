package net.frontuari.mfg.factory;

import org.adempiere.base.IProcessFactory;
import org.compiere.process.ProcessCall;

import net.frontuari.process.ProcessFTUProductionCreate;
import net.frontuari.process.ProductionCreate;
import net.frontuari.process.ProductionPreview;
import net.frontuari.process.TransformationCreate;


public class FTUProcessProductionCreateFactory implements IProcessFactory{

	@Override
	public ProcessCall newProcessInstance(String className) {
		if(className.equals("net.frontuari.process.ProcessFTUProductionCreate"))
			return new ProcessFTUProductionCreate();
		if(className.equals("net.frontuari.process.ProductionCreate"))
			return new ProductionCreate();
		if(className.equals("net.frontuari.process.ProductionPreview"))
			return new ProductionPreview();
		if(className.equals("net.frontuari.process.TransformationCreate"))
			return new TransformationCreate();
		return null;
	}

}
