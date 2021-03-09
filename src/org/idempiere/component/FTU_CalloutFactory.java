package org.idempiere.component;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.IColumnCalloutFactory;
import org.compiere.model.X_M_Production;
import org.compiere.model.X_M_ProductionLine;

import net.frontuari.callout.CalloutMProductionLine;
import net.frontuari.callout.FTUProductionCallout;

/**
 * 
 * @author Argenis Rodríguez
 *
 */
public class FTU_CalloutFactory implements IColumnCalloutFactory {

	@Override
	public IColumnCallout[] getColumnCallouts(String tableName, String columnName) {
		
		if (X_M_ProductionLine.Table_Name.equals(tableName)
				&& X_M_ProductionLine.COLUMNNAME_QtyUsed.equals(columnName))
			return new IColumnCallout[] {new CalloutMProductionLine()};
		
		if (X_M_Production.Table_Name.equals(tableName)
				&& X_M_Production.COLUMNNAME_M_Product_ID.equals(columnName))
			return new IColumnCallout[] {new FTUProductionCallout()};
		
		if (X_M_Production.Table_Name.equals(tableName)
				&& "Discount".equals(columnName))
			return new IColumnCallout[] {new FTUProductionCallout()};
		
		return null;
	}

}
