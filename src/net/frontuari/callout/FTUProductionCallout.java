package net.frontuari.callout;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MProduct;
import org.compiere.util.DB;

import net.frontuari.model.FTUMProduction;

public class FTUProductionCallout implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab
			, GridField mField, Object value, Object oldValue) {
		
		if (FTUMProduction.COLUMNNAME_M_Product_ID.equals(mField.getColumnName()))
		{
			int M_Product_ID = (int)value;
			if(M_Product_ID>0) {
				MProduct prod = new MProduct(ctx,M_Product_ID,null);
				mTab.setValue("C_UOM_ID", prod.getC_UOM_ID());
			}
		}
		
		if ("Discount".equals(mField.getColumnName()))
		{
			int M_Product_ID = mTab.getValue("M_Product_ID")!=null?(int)mTab.getValue("M_Product_ID"):0;
			int M_Production_ID = mTab.getValue("M_Production_ID")!=null?(int)mTab.getValue("M_Production_ID"):0;
			BigDecimal discount = value!=null?(BigDecimal)value:BigDecimal.ZERO;
			
			if(discount.signum()!=0) 
				discount = (new BigDecimal(100).subtract(discount)).divide(new BigDecimal(100),5,RoundingMode.HALF_UP);
			else
				discount = BigDecimal.ONE ;
				//Update End Product Cost
			if (M_Product_ID>0) {
				String sqlU = "UPDATE M_ProductionLine pl SET PriceCost = (pf.PriceCost/pf.productionqty)*"+discount+" FROM"
						+ " (SELECT SUM(ppl.PriceCost*(ppl.movementqty*-1)) AS PriceCost, pp.productionqty AS productionqty,"
							+ " CASE WHEN Discount>0 THEN ((100-Discount)/100) ELSE 1 END AS Discount FROM M_ProductionLine ppl "
						+ " JOIN M_Production pp ON ppl.M_Production_ID=pp.M_Production_ID"
						+ " WHERE ppl.M_Product_ID <> "+ M_Product_ID +" AND ppl.M_Production_ID = "+ M_Production_ID +" "
								+ "GROUP BY pp.M_Production_ID,pp.productionqty,pp.Discount) pf"
						+ " WHERE pl.M_Product_ID = "+ M_Product_ID +" AND pl.M_Production_ID = " + M_Production_ID;
				DB.executeUpdate(sqlU,null);
			}
		}
		
		
		return null;
	}
}
