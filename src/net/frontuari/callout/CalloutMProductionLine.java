package net.frontuari.callout;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.X_M_ProductionLine;
import org.compiere.util.DB;

import net.frontuari.model.FTUMProduction;

/**
 * 
 * @author Argenis Rodríguez
 *
 */
public class CalloutMProductionLine implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab
			, GridField mField, Object value, Object oldValue) {
		
		if (X_M_ProductionLine.COLUMNNAME_QtyUsed.equals(mField.getColumnName()))
			return updateMovementQty(ctx, WindowNo, mTab
					, mField, value, oldValue);
		
		return null;
	}
	
	private String updateMovementQty(Properties ctx, int WindowNo, GridTab mTab
			, GridField mField, Object value, Object oldValue) {
		
		BigDecimal qtyUsed = Optional.ofNullable((BigDecimal) value)
				.orElse(BigDecimal.ZERO);
		
		mTab.setValue(X_M_ProductionLine.COLUMNNAME_MovementQty, qtyUsed.negate());
		
		int product_ID = (int) mTab.getValue("M_Product_ID");
		int production_ID = (int) mTab.getValue("M_Production_ID");
		FTUMProduction p = new FTUMProduction(ctx, production_ID, null);
		
		//	Write PriceActual and LineNetAmt
		String sql = "SELECT pp.PriceStd "
				+ "FROM M_PriceList_Version plv "
				+ "JOIN M_ProductPrice pp ON plv.M_PriceList_Version_ID = pp.M_PriceList_Version_ID "
				+ "WHERE plv.M_PriceList_ID=? "						//	1
				+ " AND plv.ValidFrom <= ? AND pp.M_Product_ID = ?"
				+ "ORDER BY plv.ValidFrom DESC";
		
		BigDecimal price = DB.getSQLValueBD(null, sql, new Object[] {p.get_ValueAsInt("M_PriceList_ID"),p.getMovementDate(),product_ID});
		
		if(price != null)
		{
			mTab.setValue("PriceActual", price);
			mTab.setValue("LineNetAmt", price.multiply(qtyUsed));
		}
		
		return null;
	}
}
