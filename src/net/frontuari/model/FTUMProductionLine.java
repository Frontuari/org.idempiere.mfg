package net.frontuari.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.I_M_AttributeSet;
import org.compiere.model.I_M_Cost;
import org.compiere.model.I_M_ProductionPlan;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MClientInfo;
import org.compiere.model.MCost;
import org.compiere.model.MLocator;
import org.compiere.model.MProduct;
import org.compiere.model.MProduction;
import org.compiere.model.MProductionLine;
import org.compiere.model.MProductionLineMA;
import org.compiere.model.MProductionPlan;
import org.compiere.model.MQualityTest;
import org.compiere.model.MStorageOnHand;
import org.compiere.model.MStorageReservation;
import org.compiere.model.MTransaction;
import org.compiere.model.MUOM;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.eevolution.model.X_PP_Order_BOMLine;
import org.libero.model.MPPOrder;
import org.libero.model.MPPOrderBOMLine;

public class FTUMProductionLine extends MProductionLine {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7413655891179840601L;
	
	public static final String COLUMNNAME_QtyOverReceipt = "QtyOverReceipt";

	public FTUMProductionLine(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	public FTUMProductionLine(Properties ctx, int M_ProductionLine_ID, String trxName) {
		super(ctx, M_ProductionLine_ID, trxName);
		if (M_ProductionLine_ID == 0)
		{
			setLine (0);
			setM_AttributeSetInstance_ID (0);
			setM_ProductionLine_ID (0);
			setM_Production_ID (0);
			setMovementQty (Env.ZERO);
			setProcessed (false);
		}
	}

	public FTUMProductionLine( FTUMProduction header ) {
		super( header.getCtx(), 0, header.get_TrxName() );
		setM_Production_ID( header.get_ID());
		setAD_Client_ID(header.getAD_Client_ID());
		setAD_Org_ID(header.getAD_Org_ID());
		productionParent = header;
	}
	
	public FTUMProductionLine( MProductionPlan header ) {
		super( header.getCtx(), 0, header.get_TrxName() );
		setM_ProductionPlan_ID( header.get_ID());
		setAD_Client_ID(header.getAD_Client_ID());
		setAD_Org_ID(header.getAD_Org_ID());
	}
	
	/**
	 * @author Argenis Rodríguez
	 * @return PP_Order_ID
	 */
	public int getPP_Order_ID() {
		
		return DB.getSQLValue(get_TrxName()
				, "SELECT PP_Order_ID FROM M_Production WHERE M_Production_ID=?"
				, getM_Production_ID());
	}
	
	public int getHeaderProduct_ID() {
		
		if (getM_Production_ID() > 0)
		{
			productionParent = productionParent != null ? productionParent
					: new FTUMProduction(getCtx(), getM_Production_ID(), get_TrxName());
			
			return productionParent.getM_Product_ID();
		}
		else if (getM_ProductionPlan_ID() > 0)
			return getM_ProductionPlan().getM_Product_ID();
		
		return 0;
	}
	
	/**
	 * 
	 * @param date
	 * @return "" for success, error string if failed
	 */
	public String createTransactions(Timestamp date, boolean mustBeStocked) {
		int reversalId = getProductionReversalId ();
		if (reversalId <= 0  )
		{
			// delete existing ASI records
			int deleted = deleteMA();
			if (log.isLoggable(Level.FINE))log.log(Level.FINE, "Deleted " + deleted + " attribute records ");
		}
		MProduct prod = new MProduct(getCtx(), getM_Product_ID(), get_TrxName());
		if (log.isLoggable(Level.FINE))log.log(Level.FINE,"Loaded Product " + prod.toString());
		
		if ( !prod.isStocked() || prod.getProductType().compareTo(MProduct.PRODUCTTYPE_Item ) != 0 )  {
			// no need to do any movements
			if (log.isLoggable(Level.FINE))log.log(Level.FINE, "Production Line " + getLine() + " does not require stock movement");
			return "";
		}
		StringBuilder errorString = new StringBuilder();
		
		MAttributeSetInstance asi = new MAttributeSetInstance(getCtx(), getM_AttributeSetInstance_ID(), get_TrxName());
		I_M_AttributeSet attributeset = prod.getM_AttributeSet();
		boolean isAutoGenerateLot = false;
		if (attributeset != null)
			isAutoGenerateLot = attributeset.isAutoGenerateLot();		
		String asiString = asi.getDescription();
		if ( asiString == null )
			asiString = "";
		
		if (log.isLoggable(Level.FINEST))	log.log(Level.FINEST, "asi Description is: " + asiString);
		// create transactions for finished goods
		if ( getM_Product_ID() == getEndProduct_ID()
				|| isEndProduct()) {
			if (reversalId <= 0  && isAutoGenerateLot && getM_AttributeSetInstance_ID() == 0)
			{
				asi = MAttributeSetInstance.generateLot(getCtx(), (MProduct)getM_Product(), get_TrxName());
				setM_AttributeSetInstance_ID(asi.getM_AttributeSetInstance_ID());
			} 
			Timestamp dateMPolicy = date;
			if(getM_AttributeSetInstance_ID()>0){
				Timestamp t = MStorageOnHand.getDateMaterialPolicy(getM_Product_ID(), getM_AttributeSetInstance_ID(), getM_Locator_ID(), get_TrxName());
				if (t != null)
					dateMPolicy = t;
			}
			
			dateMPolicy = Util.removeTime(dateMPolicy);
			//for reversal, keep the ma copy from original trx
			if (reversalId <= 0  ) 
			{
				MProductionLineMA lineMA = new MProductionLineMA( this,
						asi.get_ID(), getMovementQty(),dateMPolicy);
				if ( !lineMA.save(get_TrxName()) ) {
					log.log(Level.SEVERE, "Could not save MA for " + toString());
					errorString.append("Could not save MA for " + toString() + "\n" );
				}
			}
			MTransaction matTrx = new MTransaction (getCtx(), getAD_Org_ID(), 
					"P+", 
					getM_Locator_ID(), getM_Product_ID(), asi.get_ID(), 
					getMovementQty(), date, get_TrxName());
			matTrx.setM_ProductionLine_ID(get_ID());
			if ( !matTrx.save(get_TrxName()) ) {
				log.log(Level.SEVERE, "Could not save transaction for " + toString());
				errorString.append("Could not save transaction for " + toString() + "\n");
			}
			MStorageOnHand storage = MStorageOnHand.getCreate(getCtx(), getM_Locator_ID(),
					getM_Product_ID(), asi.get_ID(),dateMPolicy, get_TrxName());
			storage.addQtyOnHand(getMovementQty());
			
			//Update Reserved Qty if Have PP_Order_BOMLine_ID > 0
			if (get_ValueAsInt(X_PP_Order_BOMLine.COLUMNNAME_PP_Order_BOMLine_ID) > 0)
				errorString.append(updateReserve());
			else if (getM_Product_ID() == getHeaderProduct_ID())
				errorString.append(updateReservePPOrder(getPP_Order_ID()));
			
			//End By Argenis Rodríguez
			
			if (log.isLoggable(Level.FINE))log.log(Level.FINE, "Created finished goods line " + getLine());
			
			return errorString.toString();
		}
		
		// create transactions and update stock used in production
		MStorageOnHand[] storages = MStorageOnHand.getAll( getCtx(), getM_Product_ID(),
				getM_Locator_ID(), get_TrxName(), false, 0);
		
		MProductionLineMA lineMA = null;
		MTransaction matTrx = null;
		
		BigDecimal qtyToMove = getMovementQty().negate();

		if (qtyToMove.signum() > 0) {
			for (int sl = 0; sl < storages.length; sl++) {
	
				BigDecimal lineQty = storages[sl].getQtyOnHand();
				
				if (log.isLoggable(Level.FINE))log.log(Level.FINE, "QtyAvailable " + lineQty );
				if (lineQty.signum() > 0) 
				{
					if (lineQty.compareTo(qtyToMove ) > 0)
							lineQty = qtyToMove;
	
					MAttributeSetInstance slASI = new MAttributeSetInstance(getCtx(),
							storages[sl].getM_AttributeSetInstance_ID(),get_TrxName());
					String slASIString = slASI.getDescription();
					if (slASIString == null)
						slASIString = "";
					
					if (log.isLoggable(Level.FINEST))log.log(Level.FINEST,"slASI-Description =" + slASIString);
						
					if ( slASIString.compareTo(asiString) == 0
							|| asi.getM_AttributeSet_ID() == 0  )  
					//storage matches specified ASI or is a costing asi (inc. 0)
				    // This process will move negative stock on hand quantities
					{
						lineMA = MProductionLineMA.get(this,storages[sl].getM_AttributeSetInstance_ID(),storages[sl].getDateMaterialPolicy());
						lineMA.setMovementQty(lineMA.getMovementQty().add(lineQty.negate()));
						if ( !lineMA.save(get_TrxName()) ) {
							log.log(Level.SEVERE, "Could not save MA for " + toString());
							errorString.append("Could not save MA for " + toString() + "\n" );
						} else {
							if (log.isLoggable(Level.FINE))log.log(Level.FINE, "Saved MA for " + toString());
						}
						matTrx = new MTransaction (getCtx(), getAD_Org_ID(), 
								"P-", 
								getM_Locator_ID(), getM_Product_ID(), lineMA.getM_AttributeSetInstance_ID(), 
								lineQty.negate(), date, get_TrxName());
						matTrx.setM_ProductionLine_ID(get_ID());
						if ( !matTrx.save(get_TrxName()) ) {
							log.log(Level.SEVERE, "Could not save transaction for " + toString());
							errorString.append("Could not save transaction for " + toString() + "\n");
						} else {
							if (log.isLoggable(Level.FINE))log.log(Level.FINE, "Saved transaction for " + toString());
						}
						DB.getDatabase().forUpdate(storages[sl], 120);
						storages[sl].addQtyOnHand(lineQty.negate());
						qtyToMove = qtyToMove.subtract(lineQty);
						if (log.isLoggable(Level.FINE))log.log(Level.FINE, getLine() + " Qty moved = " + lineQty + ", Remaining = " + qtyToMove );
					}
				}
				
				if ( qtyToMove.signum() == 0 )			
					break;
				
			} // for available storages
		}
		else if (qtyToMove.signum() < 0 )
		{
		
			MClientInfo m_clientInfo = MClientInfo.get(getCtx(), getAD_Client_ID(), get_TrxName());
			MAcctSchema acctSchema = new MAcctSchema(getCtx(), m_clientInfo.getC_AcctSchema1_ID(), get_TrxName());				
			if (asi.get_ID() == 0 && MAcctSchema.COSTINGLEVEL_BatchLot.equals(prod.getCostingLevel(acctSchema)) )
			{
				//add quantity to last attributesetinstance
				String sqlWhere = "M_Product_ID=? AND M_Locator_ID=? AND M_AttributeSetInstance_ID > 0 ";
				MStorageOnHand storage = new Query(getCtx(), MStorageOnHand.Table_Name, sqlWhere, get_TrxName())
						.setParameters(getM_Product_ID(), getM_Locator_ID())
						.setOrderBy(MStorageOnHand.COLUMNNAME_DateMaterialPolicy+" DESC,"+ MStorageOnHand.COLUMNNAME_M_AttributeSetInstance_ID +" DESC")
						.first();
			
				if (storage != null)
				{
					setM_AttributeSetInstance_ID(storage.getM_AttributeSetInstance_ID());
					asi = new MAttributeSetInstance(getCtx(), storage.getM_AttributeSetInstance_ID(), get_TrxName());
					asiString = asi.getDescription();
				} 
				else
				{	
					String costingMethod = prod.getCostingMethod(acctSchema);
					StringBuilder localWhereClause = new StringBuilder("M_Product_ID =?" )
							.append(" AND C_AcctSchema_ID=?")
							.append(" AND ce.CostingMethod = ? ")
							.append(" AND CurrentCostPrice <> 0 ");
						MCost cost = new Query(getCtx(),I_M_Cost.Table_Name,localWhereClause.toString(),get_TrxName())
						.setParameters(getM_Product_ID(), acctSchema.get_ID(), costingMethod)
						.addJoinClause(" INNER JOIN M_CostElement ce ON (M_Cost.M_CostElement_ID =ce.M_CostElement_ID ) ")
						.setOrderBy("Updated DESC")
						.first();
					if (cost != null)
					{
						setM_AttributeSetInstance_ID(cost.getM_AttributeSetInstance_ID());
						asi = new MAttributeSetInstance(getCtx(), cost.getM_AttributeSetInstance_ID(), get_TrxName());
						asiString = asi.getDescription();
						
					} 
					else
					{
						log.log(Level.SEVERE, "Cannot retrieve cost of Product r " + prod.toString());
						errorString.append( "Cannot retrieve cost of Product " +prod.toString() ) ;
					}

				}			
			
			}
		}
		
		
		if ( !( qtyToMove.signum() == 0) ) {
			if (mustBeStocked && qtyToMove.signum() > 0)
			{
				MLocator loc = new MLocator(getCtx(), getM_Locator_ID(), get_TrxName());
				errorString.append( "Insufficient qty on hand of " + prod.toString() + " at "
						+ loc.toString() + "\n");
			}
			else
			{
				MStorageOnHand storage = MStorageOnHand.getCreate(Env.getCtx(), getM_Locator_ID(), getM_Product_ID(),
						asi.get_ID(), date, get_TrxName(), true);
				
				BigDecimal lineQty = qtyToMove;
				MAttributeSetInstance slASI = new MAttributeSetInstance(getCtx(),
						storage.getM_AttributeSetInstance_ID(),get_TrxName());
				String slASIString = slASI.getDescription();
				if (slASIString == null)
					slASIString = "";
				
				if (log.isLoggable(Level.FINEST))log.log(Level.FINEST,"slASI-Description =" + slASIString);
					
				if ( slASIString.compareTo(asiString) == 0
						|| asi.getM_AttributeSet_ID() == 0  )  
				//storage matches specified ASI or is a costing asi (inc. 0)
			    // This process will move negative stock on hand quantities
				{
					lineMA = MProductionLineMA.get(this,storage.getM_AttributeSetInstance_ID(),storage.getDateMaterialPolicy());
					lineMA.setMovementQty(lineMA.getMovementQty().add(lineQty.negate()));
					
					if ( !lineMA.save(get_TrxName()) ) {
						log.log(Level.SEVERE, "Could not save MA for " + toString());
						errorString.append("Could not save MA for " + toString() + "\n" );
					} else {
						if (log.isLoggable(Level.FINE))log.log(Level.FINE, "Saved MA for " + toString());
					}
					matTrx = new MTransaction (getCtx(), getAD_Org_ID(), 
							"P-", 
							getM_Locator_ID(), getM_Product_ID(), asi.get_ID(), 
							lineQty.negate(), date, get_TrxName());
					matTrx.setM_ProductionLine_ID(get_ID());
					if ( !matTrx.save(get_TrxName()) ) {
						log.log(Level.SEVERE, "Could not save transaction for " + toString());
						errorString.append("Could not save transaction for " + toString() + "\n");
					} else {
						if (log.isLoggable(Level.FINE))log.log(Level.FINE, "Saved transaction for " + toString());
					}
					storage.addQtyOnHand(lineQty.negate());
					qtyToMove = qtyToMove.subtract(lineQty);
					if (log.isLoggable(Level.FINE))log.log(Level.FINE, getLine() + " Qty moved = " + lineQty + ", Remaining = " + qtyToMove );
				} else {
					errorString.append( "Storage doesn't match ASI " + prod.toString() + " / "
							+ slASIString + " vs. " + asiString + "\n");
				}
				
			}
			
		}
		//Update Reserved Qty if Have PP_Order_BOMLine_ID > 0
		if (get_ValueAsInt(X_PP_Order_BOMLine.COLUMNNAME_PP_Order_BOMLine_ID) > 0)
			errorString.append(updateReserve());
		//End By Argenis Rodríguez
			
		return errorString.toString();
	}
	
	private String updateReservePPOrder(int PP_Order_ID) {
		
		if (PP_Order_ID <= 0)
			return "";
		
		boolean isReversal = getProductionReversalId() > 0;
		
		BigDecimal movementQty = !isReversal ? getMovementQty().abs()
				: getMovementQty().abs().negate();
		
		MPPOrder order = new MPPOrder(getCtx(), PP_Order_ID, get_TrxName());
		
		BigDecimal overUnderQty = BigDecimal.ZERO;
		
		if (!isReversal)
		{
			BigDecimal toDeliver = order.getQtyOrdered()
					.subtract(order.getQtyDelivered());
			
			if (movementQty.compareTo(toDeliver) > 0)
				overUnderQty = movementQty.subtract(toDeliver);
			
			if (BigDecimal.ZERO.compareTo(overUnderQty) != 0)
				set_ValueOfColumn(COLUMNNAME_QtyOverReceipt, overUnderQty);
		}
		else
			overUnderQty = Optional.ofNullable((BigDecimal) get_Value(COLUMNNAME_QtyOverReceipt))
				.orElse(BigDecimal.ZERO);
		
		BigDecimal qtyUpdate = movementQty.subtract(overUnderQty);
		
		if (!MStorageReservation.add(getCtx(), order.getM_Warehouse_ID()
				, getM_Product_ID(), getM_AttributeSetInstance_ID()
				, qtyUpdate.negate(), false, get_TrxName()))
			return "Storage Update  Error!\n";
		else
		{
			order.setQtyReserved(order.getQtyReserved().subtract(qtyUpdate));
			order.setQtyDelivered(order.getQtyDelivered().add(movementQty));
			order.saveEx();
		}
		
		return "";
	}
	
	private String updateReserve() {
		boolean isReversal = getProductionReversalId() > 0;
		
		BigDecimal movementQty = !isReversal ? getMovementQty().abs()
				: getMovementQty().abs().negate();
		
		MPPOrderBOMLine line = new MPPOrderBOMLine(getCtx()
				, get_ValueAsInt(X_PP_Order_BOMLine.COLUMNNAME_PP_Order_BOMLine_ID)
				, get_TrxName());
		
		//We Have Qty To Deliver
		BigDecimal overUnderQty = BigDecimal.ZERO;
		
		if (!isReversal)
		{
			BigDecimal toDeliver = line.getQtyRequired()
					.subtract(line.getQtyDelivered());
			
			if (movementQty.compareTo(toDeliver) > 0)
				overUnderQty = movementQty.subtract(toDeliver);
			
			if (BigDecimal.ZERO.compareTo(overUnderQty) != 0)
				set_ValueOfColumn(COLUMNNAME_QtyOverReceipt, overUnderQty);
		}
		else
			overUnderQty = Optional.ofNullable((BigDecimal) get_Value(COLUMNNAME_QtyOverReceipt))
							.orElse(BigDecimal.ZERO);
		
		BigDecimal qtyUpdate = movementQty.subtract(overUnderQty);
		
		if (!MStorageReservation.add(getCtx(), line.getM_Warehouse_ID()
				, line.getM_Product_ID(), line.getM_AttributeSetInstance_ID()
				, qtyUpdate.negate(), !line.get_ValueAsBoolean("IsDerivative"), get_TrxName()))
			return "Storage Update  Error!\n";
		else
		{
			line.setQtyReserved(line.getQtyReserved().subtract(qtyUpdate));
			line.setQtyDelivered(line.getQtyDelivered().add(movementQty));					
			line.saveEx();
		}
		return "";
	}
	
	/**
	 * Search EndProduct from PPOrder, Production Parent or Plan
	 * @author Jorge Colmenarez, <mailto:jcolmenarez@frontuari.net>, 2020-05-02 11:13
	 */
	protected int getEndProduct_ID() {
		//	Added by Jorge Colmenarez 2020-05-02 11:09 
		//	Check if Production have PPOrder
		FTUMProduction prd = (FTUMProduction) getM_Production();
		
		if(prd.get_ValueAsInt("PP_Order_ID")>0)
		{
			if(isEndProduct())
				return getM_Product_ID();
		}
		else {
			if (productionParent != null) {
				return productionParent.getM_Product_ID();
			} else if (getM_Production_ID() > 0) {
				return getM_Production().getM_Product_ID();
			} else {
				return getM_ProductionPlan().getM_Product_ID();
			}
		}
		return 0;
	}
	
	@Override
	protected boolean beforeSave(boolean newRecord) 
	{
		if (productionParent == null && getM_Production_ID() > 0)
			productionParent = new MProduction(getCtx(), getM_Production_ID(), get_TrxName());
		
		BigDecimal qtyused = getQtyUsed();
		if(qtyused==null||qtyused.compareTo(BigDecimal.ZERO)==0){
			setQtyUsed(BigDecimal.ZERO);
		}
		MProduct prod = new MProduct(getCtx(),getM_Product_ID(),get_TrxName());

		int C_UOM_ID = get_ValueAsInt("C_UOM_ID");
		if(C_UOM_ID>0)
			C_UOM_ID=prod.getC_UOM_ID();
		MUOM UOM = new MUOM (getCtx(),C_UOM_ID,get_TrxName());
		String trxType = productionParent.get_ValueAsString("TrxType");
		 
		boolean isTransformation = trxType.equalsIgnoreCase("T");
		
		BigDecimal cost = BigDecimal.ZERO;

		if (getM_Production_ID() > 0) 
		{
			if(isTransformation){
				BigDecimal movementQty = new BigDecimal(0);
				if ( productionParent.getM_Product_ID() == getM_Product_ID()) {
					
					movementQty = getQtyUsed().negate();
					if(C_UOM_ID>0){
						if(prod.getC_UOM_ID()!=C_UOM_ID) {
							String sql = "SELECT DivideRate FROM C_UOM_Conversion WHERE M_Product_ID="+prod.getM_Product_ID()+" AND C_UOM_ID="+prod.getC_UOM_ID()+" AND C_UOM_To_ID="+C_UOM_ID;
							BigDecimal multiplyrate = DB.getSQLValueBD(get_TrxName(), sql);
							if(multiplyrate.compareTo(BigDecimal.ZERO)>0) {						
								movementQty = movementQty.multiply(multiplyrate).setScale(UOM.getStdPrecision(), RoundingMode.HALF_UP);
								setMovementQty(movementQty);
							}
						}else
							setMovementQty(movementQty);
					}else
						setMovementQty(movementQty);

					if(is_ValueChanged("M_Product_ID")) {
						cost = getPurchaseProductCost(productionParent.getM_Product_ID(),getAD_Org_ID());
						set_ValueOfColumn("PriceCost", cost);
					}
				}else {		
					BigDecimal conversionFactor = get_Value("MultiplyRate")!=null?(BigDecimal)get_Value("MultiplyRate"):BigDecimal.ZERO ;
					
					BigDecimal qtyUsed = getQtyUsed();
					
					movementQty = conversionFactor.multiply(qtyUsed).setScale(UOM.getStdPrecision(), RoundingMode.HALF_UP);
					
					if(C_UOM_ID>0){
						if(prod.getC_UOM_ID()!=C_UOM_ID) {
							String sql = "SELECT DivideRate FROM C_UOM_Conversion WHERE M_Product_ID="+prod.getM_Product_ID()+" AND C_UOM_ID="+prod.getC_UOM_ID()+" AND C_UOM_To_ID="+C_UOM_ID;
							BigDecimal multiplyrate = DB.getSQLValueBD(get_TrxName(), sql);
							if(multiplyrate.compareTo(BigDecimal.ZERO)>0) {						
								movementQty = movementQty.multiply(multiplyrate).setScale(UOM.getStdPrecision(), RoundingMode.HALF_UP);
								setMovementQty(movementQty);
							}
						}else
							setMovementQty(movementQty);
					}else
						setMovementQty(movementQty);

					if(is_ValueChanged("M_Product_ID")) {
						cost = getPurchaseProductCost(productionParent.getM_Product_ID(),getAD_Org_ID());
						cost = cost.multiply(getQtyUsed()).setScale(UOM.getCostingPrecision(), RoundingMode.HALF_UP);
						cost = cost.divide(movementQty, UOM.getCostingPrecision(), RoundingMode.HALF_UP);						
						if(cost.compareTo(BigDecimal.ZERO)>0)
							set_ValueOfColumn("PriceCost", cost);
					}
					
				}
			}else {
				if(productionParent.get_ValueAsInt("PP_Order_ID") == 0)
				{
					if ( productionParent.getM_Product_ID() == getM_Product_ID() && productionParent.getProductionQty().signum() == getMovementQty().signum()) {
						if(C_UOM_ID>0){
							if(prod.getC_UOM_ID()!=C_UOM_ID) {
								String sql = "SELECT DivideRate FROM C_UOM_Conversion WHERE M_Product_ID="+prod.getM_Product_ID()+" AND C_UOM_ID="+prod.getC_UOM_ID()+" AND C_UOM_To_ID="+C_UOM_ID;
								BigDecimal multiplyrate = DB.getSQLValueBD(get_TrxName(), sql);
								if(multiplyrate.compareTo(BigDecimal.ZERO)>0) {
									BigDecimal base = getPlannedQty();
									base = base.multiply(multiplyrate).setScale(UOM.getStdPrecision(), RoundingMode.HALF_UP);
									setMovementQty(base);
								}
							}
						}
						setIsEndProduct(true);
					}else {
						if(C_UOM_ID>0){
							if(prod.getC_UOM_ID()!=C_UOM_ID) {
								String sql = "SELECT DivideRate FROM C_UOM_Conversion WHERE M_Product_ID="+prod.getM_Product_ID()+" AND C_UOM_ID="+prod.getC_UOM_ID()+" AND C_UOM_To_ID="+C_UOM_ID;
								BigDecimal multiplyrate = DB.getSQLValueBD(get_TrxName(), sql);
								if(multiplyrate.compareTo(BigDecimal.ZERO)>0) {
									BigDecimal base = getQtyUsed();
									base = base.multiply(multiplyrate).setScale(UOM.getStdPrecision(), RoundingMode.HALF_UP);
									setMovementQty(base.negate());
								}
							}else
								setMovementQty(getQtyUsed().negate());
						}else
							setMovementQty(getQtyUsed().negate());
						
						if(is_ValueChanged("M_Product_ID")) {
							cost = getPurchaseProductCost(getM_Product_ID(),getAD_Org_ID());
							set_ValueOfColumn("PriceCost", cost);
						}
						setIsEndProduct(false);
					}
					if(isTransformation)
					{
						//Update End Product Cost
						String sqlU = "UPDATE M_ProductionLine pl SET PriceCost = pf.PriceCost/pf.productionqty FROM "
								+ " (SELECT SUM(ppl.PriceCost*(ppl.movementqty*-1)) AS PriceCost, pp.productionqty AS productionqty, "
									+ " CASE WHEN Discount>0 THEN (Discount/100) ELSE 1 END AS Discount FROM M_ProductionLine ppl "
								+ " JOIN M_Production pp ON ppl.M_Production_ID=pp.M_Production_ID"
								+ " WHERE ppl.M_Product_ID <> "+ productionParent.getM_Product_ID() +" AND ppl.M_Production_ID = "+getM_Production_ID()+" "
										+ "GROUP BY pp.M_Production_ID,pp.productionqty,pp.Discount) pf"
								+ " WHERE pl.M_Product_ID = "+ productionParent.getM_Product_ID() +" AND pl.M_Production_ID = " + getM_Production_ID();
						if(is_ValueChanged("M_Product_ID")||is_ValueChanged("QtyUsed")||is_ValueChanged("movementqty")||is_new())
							DB.executeUpdate(sqlU, get_TrxName());
					}
				}
			}
		} 
		else 
		{
			I_M_ProductionPlan plan = getM_ProductionPlan();
			if (plan.getM_Product_ID() == getM_Product_ID() && plan.getProductionQty().signum() == getMovementQty().signum())
				setIsEndProduct(true);
			else 
				setIsEndProduct(false);
		}
		
		
		if ( isEndProduct() && getM_AttributeSetInstance_ID() != 0 )
		{
			String where = "M_QualityTest_ID IN (SELECT M_QualityTest_ID " +
			"FROM M_Product_QualityTest WHERE M_Product_ID=?) " +
			"AND M_QualityTest_ID NOT IN (SELECT M_QualityTest_ID " +
			"FROM M_QualityTestResult WHERE M_AttributeSetInstance_ID=?)";

			List<MQualityTest> tests = new Query(getCtx(), MQualityTest.Table_Name, where, get_TrxName())
			.setOnlyActiveRecords(true).setParameters(getM_Product_ID(), getM_AttributeSetInstance_ID()).list();
			// create quality control results
			for (MQualityTest test : tests)
			{
				test.createResult(getM_AttributeSetInstance_ID());
			}
		}
		
		if(getIsDerivative(getM_Product_ID()) == 1) {
			setIsEndProduct(true);
		}		
		
		return true;
	}
	
	public int getIsDerivative(int PP_Product_ID) {
		int PP_Product_BOM_ID = 0;
		
		MProduction mp = new MProduction(getCtx(), getM_Production_ID(), get_TrxName());
		PP_Product_BOM_ID = mp.get_ValueAsInt("PP_Product_BOM_ID");
		
		String sql = "SELECT (CASE WHEN IsDerivative = 'Y' THEN 1 ELSE 0 END) IsDerivative FROM PP_Product_BOMLine\n" + 
				"WHERE PP_Product_BOM_ID="+PP_Product_BOM_ID+" and m_product_id = "+PP_Product_ID;
		
		return DB.getSQLValueEx(get_TrxName(), sql);
	}
	
	public BigDecimal getPurchaseProductCost (int M_Product_ID, int AD_Org_ID) {
		MProduct p = new MProduct(getCtx(), M_Product_ID, get_TrxName());
		MClientInfo ci = MClientInfo.get(getCtx(), getAD_Client_ID());
		MCost c = p.getCostingRecord(ci.getMAcctSchema1(), AD_Org_ID, getM_AttributeSetInstance_ID());
		if(c!=null && c.getCurrentCostPrice().compareTo(BigDecimal.ZERO)>0)
			return c.getCurrentCostPrice();
		
		return BigDecimal.ZERO;
	}
	
	@Override
	protected boolean beforeDelete() {
		deleteMA();
		return true;
	}
}
