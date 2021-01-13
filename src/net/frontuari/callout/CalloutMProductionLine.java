package net.frontuari.callout;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.X_M_ProductionLine;

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
		
		return null;
	}
}
