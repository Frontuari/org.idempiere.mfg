package net.frontuari.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.PeriodClosedException;
import org.compiere.acct.Doc;
import org.compiere.model.I_M_ProductionPlan;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MClient;
import org.compiere.model.MDocType;
import org.compiere.model.MLocator;
import org.compiere.model.MPeriod;
import org.compiere.model.MProduct;
import org.compiere.model.MProductCategory;
import org.compiere.model.MProduction;
import org.compiere.model.MProductionLineMA;
import org.compiere.model.MProductionPlan;
import org.compiere.model.MStorageOnHand;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUOMConversion;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

public class FTUMProduction extends MProduction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8926621246925337411L;

	public FTUMProduction(Properties ctx, int M_Production_ID, String trxName) {
		super(ctx, M_Production_ID, trxName);
	}
	
	public FTUMProduction(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	@Override
	public String prepareIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;

		//	Std Period open?
		MPeriod.testPeriodOpen(getCtx(), getMovementDate(), MDocType.DOCBASETYPE_MaterialProduction, getAD_Org_ID());

		if ( getIsCreated().equals("N") )
		{
			m_processMsg = "Not created";
			return DocAction.STATUS_Invalid; 
		}

		if (!isUseProductionPlan()) {
			m_processMsg = validateEndProduct(getM_Product_ID());			
			if (!Util.isEmpty(m_processMsg)) {
				return DocAction.STATUS_Invalid;
			}
		} else {
			Query planQuery = new Query(getCtx(), I_M_ProductionPlan.Table_Name, "M_ProductionPlan.M_Production_ID=?", get_TrxName());
			List<MProductionPlan> plans = planQuery.setParameters(getM_Production_ID()).list();
			for(MProductionPlan plan : plans) {
				m_processMsg = validateEndProduct(plan.getM_Product_ID());
				if (!Util.isEmpty(m_processMsg)) {
					return DocAction.STATUS_Invalid;
				}
			}
		}

		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;

		m_justPrepared = true;
		if (!DOCACTION_Complete.equals(getDocAction()))
			setDocAction(DOCACTION_Complete);
		return DocAction.STATUS_InProgress;
	}
	
	@Override
	public String completeIt()
	{
		// Re-Check
		if (!m_justPrepared)
		{
			String status = prepareIt();
			m_justPrepared = false;
			if (!DocAction.STATUS_InProgress.equals(status))
				return status;
		}

		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;

		StringBuilder errors = new StringBuilder();
		int processed = 0;
			
		if (!isUseProductionPlan()) {
			FTUMProductionLine[] lines = getLines();
			//IDEMPIERE-3107 Check if End Product in Production Lines exist
			if(!isHaveEndProduct(lines)) {
				m_processMsg = "Production does not contain End Product";
				return DocAction.STATUS_Invalid;
			}
			errors.append(processLines(lines));
			if (errors.length() > 0) {
				m_processMsg = errors.toString();
				return DocAction.STATUS_Invalid;
			}
			processed = processed + lines.length;
		} else {
			Query planQuery = new Query(Env.getCtx(), I_M_ProductionPlan.Table_Name, "M_ProductionPlan.M_Production_ID=?", get_TrxName());
			List<MProductionPlan> plans = planQuery.setParameters(getM_Production_ID()).list();
			for(MProductionPlan plan : plans) {
				FTUMProductionLine[] lines = (FTUMProductionLine[]) plan.getLines();
				
				//IDEMPIERE-3107 Check if End Product in Production Lines exist
				if(!isHaveEndProduct(lines)) {
					m_processMsg = String.format("Production plan (line %1$d id %2$d) does not contain End Product", plan.getLine(), plan.get_ID());
					return DocAction.STATUS_Invalid;
				}
				
				if (lines.length > 0) {
					errors.append(processLines(lines));
					if (errors.length() > 0) {
						m_processMsg = errors.toString();
						return DocAction.STATUS_Invalid;
					}
					processed = processed + lines.length;
				}
				plan.setProcessed(true);
				plan.saveEx();
			}
		}

		//		User Validation
		String valid = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
		if (valid != null)
		{
			m_processMsg = valid;
			return DocAction.STATUS_Invalid;
		}

		setProcessed(true);
		setDocAction(DOCACTION_Close);
		return DocAction.STATUS_Completed;
	}

	private boolean isHaveEndProduct(FTUMProductionLine[] lines) {
		
		for(FTUMProductionLine line : lines) {
			if(line.isEndProduct())
				return true; 
		}
		return false;
	}
	
	protected Object processLines(FTUMProductionLine[] lines) {
		StringBuilder errors = new StringBuilder();
		for ( int i = 0; i<lines.length; i++) {
			String error = lines[i].createTransactions(getMovementDate(), false);
			if (!Util.isEmpty(error)) {
				errors.append(error);
			} else { 
				lines[i].setProcessed( true );
				lines[i].saveEx(get_TrxName());
			}
		}

		return errors.toString();
	}
	
	@Override
	protected String validateEndProduct(int M_Product_ID) {
		String msg = isBom(M_Product_ID);
		if (!Util.isEmpty(msg))
			return msg;

		if (!costsOK(M_Product_ID)) {
			msg = "Excessive difference in standard costs";
			if (MSysConfig.getBooleanValue(MSysConfig.MFG_ValidateCostsDifferenceOnCreate, false, getAD_Client_ID())) {
				return msg;
			} else {
				log.warning(msg);
			}
		}

		return null;
	}
	
	@Override
	protected boolean costsOK(int M_Product_ID) throws AdempiereUserError {
		MProduct product = MProduct.get(getCtx(), M_Product_ID);
		String costingMethod=product.getCostingMethod(MClient.get(getCtx()).getAcctSchema());
		// will not work if non-standard costing is used
		if (MAcctSchema.COSTINGMETHOD_StandardCosting.equals(costingMethod))
		{			
			String sql = "SELECT ABS(((cc.currentcostprice-(SELECT SUM(c.currentcostprice*bom.qtybom)"
					+ " FROM m_cost c"
					+ " INNER JOIN pp_product_bom bom ON (c.m_product_id=bom.m_product_id)"
					+ " INNER JOIN pp_product_bomline boml ON (bom.pp_product_bom_id = boml.pp_product_bom_id ) "
					+ " INNER JOIN m_costelement ce ON (c.m_costelement_id = ce.m_costelement_id AND ce.costingmethod = 'S')"
					+ " WHERE bom.m_product_id = pp.m_product_id)"
					+ " )/cc.currentcostprice))"
					+ " FROM m_product pp"
					+ " INNER JOIN m_cost cc on (cc.m_product_id=pp.m_product_id)"
					+ " INNER JOIN m_costelement ce ON (cc.m_costelement_id=ce.m_costelement_id)"
					+ " WHERE cc.currentcostprice > 0 AND pp.M_Product_ID = ?"
					+ " AND ce.costingmethod='S'";

			BigDecimal costPercentageDiff = DB.getSQLValueBD(get_TrxName(), sql, M_Product_ID);

			if (costPercentageDiff == null)
			{
				costPercentageDiff = Env.ZERO;
				String msg = "Could not retrieve costs";
				if (MSysConfig.getBooleanValue(MSysConfig.MFG_ValidateCostsOnCreate, false, getAD_Client_ID())) {
					throw new AdempiereUserError(msg);
				} else {
					log.warning(msg);
				}
			}

			if ( (costPercentageDiff.compareTo(new BigDecimal("0.005")))< 0 )
				return true;

			return false;
		}
		else if (MAcctSchema.COSTINGMETHOD_AverageInvoice.equals(costingMethod))
		{			
			String sql = "SELECT ABS(((cc.currentcostprice-(SELECT SUM(c.currentcostprice*boml.qtybom)"
					+ " FROM m_cost c"
					+ " INNER JOIN pp_product_bom bom ON (c.m_product_id=bom.m_product_id)"
					+ " INNER JOIN pp_product_bomline boml ON (bom.pp_product_bom_id = boml.pp_product_bom_id ) "
					+ " INNER JOIN m_costelement ce ON (c.m_costelement_id = ce.m_costelement_id AND ce.costingmethod = 'I')"
					+ " WHERE bom.m_product_id = pp.m_product_id)"
					+ " )/cc.currentcostprice))"
					+ " FROM m_product pp"
					+ " INNER JOIN m_cost cc on (cc.m_product_id=pp.m_product_id)"
					+ " INNER JOIN m_costelement ce ON (cc.m_costelement_id=ce.m_costelement_id)"
					+ " WHERE cc.currentcostprice > 0 AND pp.M_Product_ID = ?"
					+ " AND ce.costingmethod='I'";

			BigDecimal costPercentageDiff = DB.getSQLValueBD(get_TrxName(), sql, M_Product_ID);

			if (costPercentageDiff == null)
			{
				costPercentageDiff = Env.ZERO;
				String msg = "Could not retrieve costs";
				if (MSysConfig.getBooleanValue(MSysConfig.MFG_ValidateCostsOnCreate, false, getAD_Client_ID())) {
					throw new AdempiereUserError(msg);
				} else {
					log.warning(msg);
				}
			}

			if ( (costPercentageDiff.compareTo(new BigDecimal("0.005")))< 0 )
				return true;

			return false;
		}
		return true;
	}
	
	@Override
	protected String isBom(int M_Product_ID)
	{
		String bom = DB.getSQLValueString(get_TrxName(), "SELECT isbom FROM M_Product WHERE M_Product_ID = ?", M_Product_ID);
		if ("N".compareTo(bom) == 0)
		{
			return "Attempt to create product line for Non Bill Of Materials";
		}
		int materials = DB.getSQLValue(get_TrxName(), "SELECT count(PP_Product_BOM_ID) FROM PP_Product_BOM WHERE M_Product_ID = ?", M_Product_ID);
		if (materials == 0)
		{
			return "Attempt to create product line for Bill Of Materials with no BOM Products";
		}
		return null;
	}
	
	@Override
	public boolean reverseCorrectIt() 
	{
		if (log.isLoggable(Level.INFO)) log.info(toString());
		// Before reverseCorrect
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSECORRECT);
		if (m_processMsg != null)
			return false;

		FTUMProduction reversal = reverse(false);
		if (reversal == null)
			return false;

		// After reverseCorrect
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSECORRECT);
		if (m_processMsg != null)
			return false;

		m_processMsg = reversal.getDocumentNo();

		return true;
	}
	
	protected FTUMProduction reverse(boolean accrual) {
		Timestamp reversalDate = accrual ? Env.getContextAsDate(getCtx(), "#Date") : getMovementDate();
		if (reversalDate == null) {
			reversalDate = new Timestamp(System.currentTimeMillis());
		}

		MPeriod.testPeriodOpen(getCtx(), reversalDate, Doc.DOCTYPE_MatProduction, getAD_Org_ID());
		FTUMProduction reversal = null;
		reversal = copyFrom (reversalDate);

		StringBuilder msgadd = new StringBuilder("{->").append(getDocumentNo()).append(")");
		reversal.addDescription(msgadd.toString());
		reversal.setReversal_ID(getM_Production_ID());
		reversal.saveEx(get_TrxName());
		
		// Reverse Line Qty
		FTUMProductionLine[] sLines = getLines();
		FTUMProductionLine[] tLines = reversal.getLines();
		for (int i = 0; i < sLines.length; i++)
		{		
			//	We need to copy MA
			if (sLines[i].getM_AttributeSetInstance_ID() == 0)
			{
				MProductionLineMA mas[] = MProductionLineMA.get(getCtx(), sLines[i].get_ID(), get_TrxName());
				for (int j = 0; j < mas.length; j++)
				{
					MProductionLineMA ma = new MProductionLineMA (tLines[i],
						mas[j].getM_AttributeSetInstance_ID(),
						mas[j].getMovementQty().negate(),mas[j].getDateMaterialPolicy());
					ma.saveEx(get_TrxName());					
				}
			}
		}

		if (!reversal.processIt(DocAction.ACTION_Complete))
		{
			m_processMsg = "Reversal ERROR: " + reversal.getProcessMsg();
			return null;
		}

		reversal.closeIt();
		reversal.setProcessing (false);
		reversal.setDocStatus(DOCSTATUS_Reversed);
		reversal.setDocAction(DOCACTION_None);
		reversal.saveEx(get_TrxName());

		msgadd = new StringBuilder("(").append(reversal.getDocumentNo()).append("<-)");
		addDescription(msgadd.toString());

		setProcessed(true);
		setReversal_ID(reversal.getM_Production_ID());
		setDocStatus(DOCSTATUS_Reversed);	//	may come from void
		setDocAction(DOCACTION_None);		
		saveEx(get_TrxName());
		
		return reversal;
	}
	
	protected FTUMProduction copyFrom(Timestamp reversalDate) {
		FTUMProduction to = new FTUMProduction(getCtx(), 0, get_TrxName());
		PO.copyValues (this, to, getAD_Client_ID(), getAD_Org_ID());

		to.set_ValueNoCheck ("DocumentNo", null);
		//
		to.setDocStatus (DOCSTATUS_Drafted);		//	Draft
		to.setDocAction(DOCACTION_Complete);
		to.setMovementDate(reversalDate);
		to.setIsComplete(false);
		to.setIsCreated("Y");
		to.setProcessing(false);
		to.setProcessed(false);
		to.setIsUseProductionPlan(isUseProductionPlan());
		if (isUseProductionPlan()) {
			to.saveEx();
			Query planQuery = new Query(Env.getCtx(), I_M_ProductionPlan.Table_Name, "M_ProductionPlan.M_Production_ID=?", get_TrxName());
			List<MProductionPlan> fplans = planQuery.setParameters(getM_Production_ID()).list();
			for(MProductionPlan fplan : fplans) {
				MProductionPlan tplan = new MProductionPlan(getCtx(), 0, get_TrxName());
				PO.copyValues (fplan, tplan, getAD_Client_ID(), getAD_Org_ID());
				tplan.setM_Production_ID(to.getM_Production_ID());
				tplan.setProductionQty(fplan.getProductionQty().negate());
				tplan.setProcessed(false);
				tplan.saveEx();

				FTUMProductionLine[] flines = (FTUMProductionLine[]) fplan.getLines();
				for(FTUMProductionLine fline : flines) {
					FTUMProductionLine tline = new FTUMProductionLine(tplan);
					
					BigDecimal qtyOverReceipt = Optional.ofNullable((BigDecimal) fline.get_Value(FTUMProductionLine.COLUMNNAME_QtyOverReceipt))
							.orElse(BigDecimal.ZERO)
							.negate();
					
					PO.copyValues (fline, tline, getAD_Client_ID(), getAD_Org_ID());
					tline.setM_ProductionPlan_ID(tplan.getM_ProductionPlan_ID());
					tline.setMovementQty(fline.getMovementQty().negate());
					tline.setPlannedQty(fline.getPlannedQty().negate());
					tline.setQtyUsed(fline.getQtyUsed().negate());
					tline.set_ValueOfColumn(FTUMProductionLine.COLUMNNAME_QtyOverReceipt, qtyOverReceipt);
					tline.saveEx();
				}
			}
		} else {
			to.setProductionQty(getProductionQty().negate());	
			to.saveEx();
			FTUMProductionLine[] flines = getLines();
			for(FTUMProductionLine fline : flines) {
				FTUMProductionLine tline = new FTUMProductionLine(to);
				
				BigDecimal qtyOverReceipt = Optional.ofNullable((BigDecimal) fline.get_Value(FTUMProductionLine.COLUMNNAME_QtyOverReceipt))
						.orElse(BigDecimal.ZERO)
						.negate();
				
				PO.copyValues (fline, tline, getAD_Client_ID(), getAD_Org_ID());
				tline.setM_Production_ID(to.getM_Production_ID());
				tline.setMovementQty(fline.getMovementQty().negate());
				tline.setPlannedQty(fline.getPlannedQty().negate());
				tline.setQtyUsed(fline.getQtyUsed().negate());
				tline.set_ValueOfColumn(FTUMProductionLine.COLUMNNAME_QtyOverReceipt, qtyOverReceipt);
				tline.saveEx();
			}
		}

		return to;
	}
	
	public FTUMProductionLine[] getLines() {
		ArrayList<FTUMProductionLine> list = new ArrayList<FTUMProductionLine>();
		
		String sql = "SELECT pl.M_ProductionLine_ID "
			+ "FROM M_ProductionLine pl "
			+ "WHERE pl.M_Production_ID = ?";
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, get_ID());
			rs = pstmt.executeQuery();
			while (rs.next())
				list.add( new FTUMProductionLine( getCtx(), rs.getInt(1), get_TrxName() ) );	
		}
		catch (SQLException ex)
		{
			throw new AdempiereException("Unable to load production lines", ex);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		
		FTUMProductionLine[] retValue = new FTUMProductionLine[list.size()];
		list.toArray(retValue);
		return retValue;
	}
	
	@Override
	public boolean voidIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());
		// Before Void
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_VOID);
		if (m_processMsg != null)
			return false;

		if (DOCSTATUS_Closed.equals(getDocStatus())
				|| DOCSTATUS_Reversed.equals(getDocStatus())
				|| DOCSTATUS_Voided.equals(getDocStatus()))
		{
			m_processMsg = "Document Closed: " + getDocStatus();
			setDocAction(DOCACTION_None);
			return false;
		}

		// Not Processed
		if (DOCSTATUS_Drafted.equals(getDocStatus())
				|| DOCSTATUS_Invalid.equals(getDocStatus())
				|| DOCSTATUS_InProgress.equals(getDocStatus())
				|| DOCSTATUS_Approved.equals(getDocStatus())
				|| DOCSTATUS_NotApproved.equals(getDocStatus()) )
		{
			setIsCreated("N");
			if (!isUseProductionPlan()) {
				deleteLines(get_TrxName());
				setProductionQty(BigDecimal.ZERO);
			} else {
				Query planQuery = new Query(Env.getCtx(), I_M_ProductionPlan.Table_Name, "M_ProductionPlan.M_Production_ID=?", get_TrxName());
				List<MProductionPlan> plans = planQuery.setParameters(getM_Production_ID()).list();
				for(MProductionPlan plan : plans) {
					plan.deleteLines(get_TrxName());
					plan.setProductionQty(BigDecimal.ZERO);
					plan.setProcessed(true);
					plan.saveEx();
				}
			}
		}
		else
		{
			boolean accrual = false;
			try 
			{
				MPeriod.testPeriodOpen(getCtx(), getMovementDate(), Doc.DOCTYPE_MatProduction, getAD_Org_ID());
			}
			catch (PeriodClosedException e) 
			{
				accrual = true;
			}

			if (accrual)
				return reverseAccrualIt();
			else
				return reverseCorrectIt();
		}

		// After Void
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_VOID);
		if (m_processMsg != null)
			return false;

		setProcessed(true);
		setDocAction(DOCACTION_None);
		return true; 
	}
	
	public int createProductionLines(boolean mustBeStocked,int PP_Product_BOM_ID) {
		
		set_ValueOfColumn("PP_Product_BOM_ID", PP_Product_BOM_ID);
		lineno = 10;

		count = 0;

		int C_UOM_ID = get_ValueAsInt("C_UOM_ID");
		
		BigDecimal ProductionQty = getProductionQty();
		
		
		// product to be produced
		MProduct finishedProduct = new MProduct(getCtx(), getM_Product_ID(), get_TrxName());
		
		if(finishedProduct.getC_UOM_ID()!=C_UOM_ID) {
			ProductionQty = MUOMConversion.convertProductTo(getCtx(), finishedProduct.get_ID(), C_UOM_ID, ProductionQty);
			if(ProductionQty.signum()==0)
				throw new AdempiereException("El producto "+finishedProduct.getValue()+"-"+finishedProduct.getName()+" no tiene conversion de unidades de medida");
		}
		

		FTUMProductionLine line = new FTUMProductionLine( this );
		line.setLine( lineno );
		line.setM_Product_ID( finishedProduct.get_ID() );
		line.setM_Locator_ID( getM_Locator_ID() );
		line.setMovementQty(ProductionQty);
		line.setPlannedQty(ProductionQty);
		if(C_UOM_ID>0)
			line.set_ValueOfColumn("C_UOM_ID", C_UOM_ID);
		
		line.saveEx(get_TrxName());
		count++;
		
		createProductionLines(mustBeStocked, finishedProduct, PP_Product_BOM_ID, ProductionQty);
		
		return count;
	}
	
	public int createProductionLines(boolean mustBeStocked, MProduct finishedProduct, int PP_Product_BOM_ID, BigDecimal requiredQty) {
		
		int defaultLocator = 0;
		
		MLocator finishedLocator = MLocator.get(getCtx(), getM_Locator_ID());
		
		int M_Warehouse_ID = finishedLocator.getM_Warehouse_ID();
		
		int asi = 0;

		// products used in production
		String sql = "SELECT M_Product_ID, QtyBOM, C_UOM_ID,IsQtyPercentage, IsDerivative" + " FROM PP_Product_BOMLine"
				+ " WHERE PP_Product_BOM_ID=" + PP_Product_BOM_ID + " ORDER BY Line";

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());

			rs = pstmt.executeQuery();
			while (rs.next()) {
				
				lineno = lineno + 10;
				int BOMProduct_ID = rs.getInt(1);
				BigDecimal BOMQty = rs.getBigDecimal(2);
				int C_UOM_ID = rs.getInt(3);
				boolean isQtyPercentage = rs.getBoolean("IsQtyPercentage");
				boolean IsDerivative = rs.getBoolean("IsDerivative");
				
				if(isQtyPercentage)
					BOMQty= BOMQty.divide(new BigDecimal(100),2, RoundingMode.HALF_UP);
				
				BigDecimal BOMMovementQty = BOMQty.multiply(requiredQty);
				
				MProduct bomproduct = new MProduct(Env.getCtx(), BOMProduct_ID, get_TrxName());
				

				if ( bomproduct.isBOM() && bomproduct.isPhantom() )
				{
					createLines(mustBeStocked, bomproduct, BOMMovementQty);
				}
				else
				{

					defaultLocator = bomproduct.getM_Locator_ID();
					if ( defaultLocator == 0 )
						defaultLocator = getM_Locator_ID();
					else
					{
						MLocator loc = new MLocator(getCtx(), defaultLocator, get_TrxName());
						M_Warehouse_ID = loc.getM_Warehouse_ID();
					}

					if (!bomproduct.isStocked())
					{					
						FTUMProductionLine BOMLine = null;
						BOMLine = new FTUMProductionLine( this );
						BOMLine.setLine( lineno );
						BOMLine.setM_Product_ID( BOMProduct_ID );
						BOMLine.setM_Locator_ID( defaultLocator );  
						BOMLine.setQtyUsed(BOMMovementQty );
						BOMLine.setPlannedQty( BOMMovementQty );
						if(C_UOM_ID>0)
							BOMLine.set_ValueOfColumn("C_UOM_ID", C_UOM_ID);
						BOMLine.saveEx(get_TrxName());

						lineno = lineno + 10;
						count++;					
					}
					else if (BOMMovementQty.signum() == 0) 
					{
						FTUMProductionLine BOMLine = null;
						BOMLine = new FTUMProductionLine( this );
						BOMLine.setLine( lineno );
						BOMLine.setM_Product_ID( BOMProduct_ID );
						BOMLine.setM_Locator_ID( defaultLocator );  
						BOMLine.setQtyUsed( BOMMovementQty );
						BOMLine.setPlannedQty( BOMMovementQty );
						if(C_UOM_ID>0)
							BOMLine.set_ValueOfColumn("C_UOM_ID", C_UOM_ID);
						BOMLine.saveEx(get_TrxName());

						lineno = lineno + 10;
						count++;
					}
					else if (IsDerivative)
						createDerivatedProductionLine(BOMProduct_ID, defaultLocator
								, M_Warehouse_ID, BOMMovementQty
								, asi, C_UOM_ID);
					else
						createNotDerivatedProdutionLine(BOMProduct_ID, defaultLocator
								, M_Warehouse_ID, BOMMovementQty
								, asi, C_UOM_ID, mustBeStocked);
				}
			} // for all bom products
		} catch (Exception e) {
			throw new AdempiereException("Failed to create production lines", e);
		}
		finally {
			DB.close(rs, pstmt);
		}

		return count;
	
	}
	
	private void createDerivatedProductionLine(int BOMProduct_ID, int defaultLocator, int M_Warehouse_ID
			, BigDecimal BOMMovementQty, int asi, int C_UOM_ID) {
		FTUMProductionLine line = new FTUMProductionLine(this);
		line.setLine(lineno);
		line.setM_Product_ID(BOMProduct_ID);
		line.setM_Locator_ID(defaultLocator);
		line.setMovementQty(BOMMovementQty);
		line.setPlannedQty(BOMMovementQty);
		line.set_ValueOfColumn("C_UOM_ID", C_UOM_ID);
		line.saveEx();
		
		lineno += 10;
		count++;
	}
	
	private void createNotDerivatedProdutionLine(int BOMProduct_ID, int defaultLocator, int M_Warehouse_ID
			, BigDecimal BOMMovementQty, int asi, int C_UOM_ID, boolean mustBeStocked) {
		
		// BOM stock info
		MStorageOnHand[] storages = null;
		MProduct usedProduct = MProduct.get(getCtx(), BOMProduct_ID);
		defaultLocator = usedProduct.getM_Locator_ID();
		if ( defaultLocator == 0 )
			defaultLocator = getM_Locator_ID();
		if (usedProduct == null || usedProduct.get_ID() == 0)
			return ;

		MClient client = MClient.get(getCtx());
		MProductCategory pc = MProductCategory.get(getCtx(),
				usedProduct.getM_Product_Category_ID());
		String MMPolicy = pc.getMMPolicy();
		if (MMPolicy == null || MMPolicy.length() == 0) 
		{ 
			MMPolicy = client.getMMPolicy();
		}

		storages = MStorageOnHand.getWarehouse(getCtx(), M_Warehouse_ID, BOMProduct_ID, 0, null,
				MProductCategory.MMPOLICY_FiFo.equals(MMPolicy), true, 0, get_TrxName());

		FTUMProductionLine BOMLine = null;
		int prevLoc = -1;
		int previousAttribSet = -1;
		// Create lines from storage until qty is reached
		for (int sl = 0; sl < storages.length; sl++) {

			BigDecimal lineQty = storages[sl].getQtyOnHand();
			if (lineQty.signum() != 0) {
				if (lineQty.compareTo(BOMMovementQty) > 0)
					lineQty = BOMMovementQty;


				int loc = storages[sl].getM_Locator_ID();
				int slASI = storages[sl].getM_AttributeSetInstance_ID();
				int locAttribSet = new MAttributeSetInstance(getCtx(), asi,
						get_TrxName()).getM_AttributeSet_ID();

				// roll up costing attributes if in the same locator
				if (locAttribSet == 0 && previousAttribSet == 0
						&& prevLoc == loc) {
					BOMLine.setQtyUsed(BOMLine.getQtyUsed()
							.add(lineQty));
					BOMLine.setMovementQty(BOMLine.getMovementQty()
							.add(lineQty.negate()));
					BOMLine.setPlannedQty(BOMLine.getQtyUsed());
					BOMLine.saveEx(get_TrxName());

				}
				// otherwise create new line
				else {
					BOMLine = new FTUMProductionLine( this );
					BOMLine.setLine( lineno );
					BOMLine.setM_Product_ID( BOMProduct_ID );
					BOMLine.setM_Locator_ID( loc );
					BOMLine.setQtyUsed( lineQty );
					BOMLine.setMovementQty( lineQty.negate() );
					BOMLine.setPlannedQty( lineQty);
					if ( slASI != 0 && locAttribSet != 0 )  // ie non costing attribute
						BOMLine.setM_AttributeSetInstance_ID(slASI);
					if(C_UOM_ID>0)
						BOMLine.set_ValueOfColumn("C_UOM_ID", C_UOM_ID);
					BOMLine.saveEx(get_TrxName());

					lineno = lineno + 10;
					count++;
				}
				prevLoc = loc;
				previousAttribSet = locAttribSet;
				// enough ?
				BOMMovementQty = BOMMovementQty.subtract(lineQty);
				if (BOMMovementQty.signum() == 0)
					break;
			}
		} // for available storages

		// fallback
		if (BOMMovementQty.signum() != 0 ) {
			if (!mustBeStocked)
			{

				// roll up costing attributes if in the same locator
				if ( previousAttribSet == 0
						&& prevLoc == defaultLocator) {
					BOMLine.setQtyUsed(BOMLine.getQtyUsed()
							.add(BOMMovementQty));
					BOMLine.setMovementQty(BOMLine.getMovementQty()
							.add(BOMMovementQty.negate()));
					BOMLine.setPlannedQty(BOMLine.getQtyUsed());
					BOMLine.saveEx(get_TrxName());

				}
				// otherwise create new line
				else {

					BOMLine = new FTUMProductionLine( this );
					BOMLine.setLine( lineno );
					BOMLine.setM_Product_ID( BOMProduct_ID );
					BOMLine.setM_Locator_ID( defaultLocator );
					BOMLine.setQtyUsed( BOMMovementQty);
					BOMLine.setMovementQty( BOMMovementQty.negate() );
					
					BOMLine.setPlannedQty( BOMMovementQty);
					
					if(C_UOM_ID>0)
						BOMLine.set_ValueOfColumn("C_UOM_ID", C_UOM_ID);
					BOMLine.saveEx(get_TrxName());

					lineno = lineno + 10;
					count++;
				}

			}
			else
			{
				throw new AdempiereUserError("Not enough stock of " + BOMProduct_ID);
			}
		}
	}
}
