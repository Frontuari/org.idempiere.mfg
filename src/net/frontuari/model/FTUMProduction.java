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
import org.compiere.model.MOrderLine;
import org.compiere.model.MPeriod;
import org.compiere.model.MProduct;
import org.compiere.model.MProductCategory;
import org.compiere.model.MProduction;
import org.compiere.model.MProductionLine;
import org.compiere.model.MProductionLineMA;
import org.compiere.model.MProductionPlan;
import org.compiere.model.MProject;
import org.compiere.model.MProjectLine;
import org.compiere.model.MStorageOnHand;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUOM;
import org.compiere.model.MUOMConversion;
import org.compiere.model.MWarehouse;
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
		if (M_Production_ID == 0) {
			setDocStatus(DOCSTATUS_Drafted);
			setDocAction (DOCACTION_Prepare);
		}
	}
	
	public FTUMProduction(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	public FTUMProduction( MOrderLine line ) {
		super( line.getCtx(), 0, line.get_TrxName());
		setAD_Client_ID(line.getAD_Client_ID());
		setAD_Org_ID(line.getAD_Org_ID());
		setMovementDate( line.getDatePromised() );
	}

	public FTUMProduction( MProjectLine line ) {
		super( line.getCtx(), 0, line.get_TrxName());
		MProject project = new MProject(line.getCtx(), line.getC_Project_ID(), line.get_TrxName());
		MWarehouse wh = new MWarehouse(line.getCtx(), project.getM_Warehouse_ID(), line.get_TrxName());
		
		MLocator M_Locator = null;
		int M_Locator_ID = 0;

		if (wh != null)
		{
			M_Locator = wh.getDefaultLocator();
			M_Locator_ID = M_Locator.getM_Locator_ID();
		}
		setAD_Client_ID(line.getAD_Client_ID());
		setAD_Org_ID(line.getAD_Org_ID());
		setM_Product_ID(line.getM_Product_ID());
		setProductionQty(line.getPlannedQty());
		setM_Locator_ID(M_Locator_ID);
		setDescription(project.getValue()+"_"+project.getName()+" Line: "+line.getLine()+" (project)");
		setC_Project_ID(line.getC_Project_ID());
		setC_BPartner_ID(project.getC_BPartner_ID());
		setC_Campaign_ID(project.getC_Campaign_ID());
		setAD_OrgTrx_ID(project.getAD_OrgTrx_ID());
		setC_Activity_ID(project.getC_Activity_ID());
		setC_ProjectPhase_ID(line.getC_ProjectPhase_ID());
		setC_ProjectTask_ID(line.getC_ProjectTask_ID());
		setMovementDate( Env.getContextAsDate(p_ctx, "#Date"));
	}
	
	private static final String ProductionQuantity = "PQ";
	
	@Override
	public String prepareIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;

		//	Std Period open?
		MPeriod.testPeriodOpen(getCtx(), getMovementDate(), MDocType.DOCBASETYPE_MaterialProduction, getAD_Org_ID());
		//	Valide Product Parent
		ValidateExistanceOfPricipalProductInLines();
		
		boolean isTransformation = get_ValueAsString("TrxType").equalsIgnoreCase("T");
		if ( getIsCreated().equals("N") )
		{
			//	m_processMsg = "Not created";
			if(isTransformation && !get_ValueAsBoolean("IsManual")){
				m_processMsg = "Debe crear las lineas o actualizarlas(Recrearlas) en caso de haber cambiado la cantidad a transformar";
				return DocAction.STATUS_Invalid; 
			}else if (!isTransformation) {
				m_processMsg = "Debe crear las lineas o actualizarlas(Recrearlas) en caso de haber cambiado la cantidad a producir";
				return DocAction.STATUS_Invalid; 
			} 
		}
		//	Validate Stock on Transformation Transaction
		if(isTransformation){
			if(!get_ValueAsBoolean("IsManual"))
			{
				String sql = "SELECT COALESCE(SUM(QtyOnHand-QtyReserved),0) FROM M_Storage  WHERE M_Product_ID = "+getM_Product_ID()+" AND AD_Org_ID = "+getAD_Org_ID()+" AND M_Locator_ID="+getM_Locator_ID();
				BigDecimal qty = DB.getSQLValueBD(get_TrxName(), sql);
				if(qty!=null){
					if(qty.compareTo(getProductionQty())<0){
						m_processMsg = "La cantidad a transformar es menor a la cantidad disponible en el almacen y la ubicacion seleccionada";
						return DocAction.STATUS_Invalid; 
					}
				}
				//	Validate Production
				ValidateProduction((MProduct)getM_Product());
			}
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
		//int materials = DB.getSQLValue(get_TrxName(), "SELECT count(PP_Product_BOM_ID) FROM PP_Product_BOM WHERE M_Product_ID = ?", M_Product_ID);
		int materials = 0;
		String sql = "";
		if(get_ValueAsString("TrxType").equalsIgnoreCase("T"))
			sql = "SELECT count(M_Product_BOM_ID) FROM M_Product_BOM WHERE M_Product_ID = "+M_Product_ID+" AND (AD_Org_ID = 0 OR AD_Org_ID ="+ getAD_Org_ID()+")";
		else if(get_ValueAsString("TrxType").equalsIgnoreCase("P"))
			sql = "SELECT count(PP_Product_BOMLine_ID) FROM PP_Product_BOMLine WHERE PP_Product_BOM_ID = "+ get_ValueAsInt("PP_Product_BOM_ID");
		materials = DB.getSQLValue(get_TrxName(),sql);
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
	

	
	public FTUMProductionLine[] getLines(String whereClause) {
		ArrayList<FTUMProductionLine> list = new ArrayList<FTUMProductionLine>();
		
		String sql = "SELECT pl.M_ProductionLine_ID "
			+ "FROM M_ProductionLine pl "
			+ "WHERE pl.M_Production_ID = ?";
		
		if(whereClause != "" && !whereClause.isEmpty())
		{
			sql = sql+whereClause;
		}
		
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
	
	/**
	 * Validate Parent Product exists on Line
	 * @author Jorge Colmenarez, 2021-03-02 14:37, jcolmenarez@frontuari.net, frontuari.net
	 */
	public void ValidateExistanceOfPricipalProductInLines() {
		String sql="SELECT M_ProductionLine_ID FROM M_ProductionLine WHERE M_Production_ID="+getM_Production_ID()+" AND M_Product_ID="+getM_Product_ID();
		int M_ProductionLine_ID = DB.getSQLValue(get_TrxName(), sql);
		if(M_ProductionLine_ID<=0)
			throw new AdempiereException("No hay una linea que corresponda al producto del encabezado");
	}
	
	public void ValidateProduction (MProduct prod) {
		String ProductionValidationMethod = prod.get_ValueAsString("ProductionValidationMethod");
		
		if(ProductionValidationMethod.equalsIgnoreCase(ProductionQuantity)) {			
			String sql = "SELECT COUNT(DISTINCT prod.C_UOM_ID) FROM M_ProductionLine pl"
					+ " JOIN M_Product prod ON prod.M_Product_ID=pl.M_Product_ID"
					+ " WHERE pl.M_Production_ID = "+getM_Production_ID()+"";
			int uomqty = DB.getSQLValueEx(get_TrxName(), sql); 
			if (uomqty>1)
				throw new AdempiereException("Las unidades base de los productos son diferentes");
			
			sql = " SELECT SUM(pl.QtyUsed) FROM M_ProductionLine pl WHERE M_Production_ID="+getM_Production_ID()+" AND M_Product_ID<>"+getM_Product_ID();
			BigDecimal ProductionQty = getProductionQty();
			BigDecimal LinesQty = DB.getSQLValueBDEx(get_TrxName(), sql);
			ProductionQty = ProductionQty.subtract(LinesQty).setScale(MUOM.getPrecision(getCtx(), get_ValueAsInt("C_UOM_ID")), RoundingMode.HALF_UP);
			if(ProductionQty.signum()>0)
				throw new AdempiereException("Las cantidades usadas en las lineas no corresponden a las unidades a procesar, faltan: "+ProductionQty.toString());
			else if(ProductionQty.signum()<0)
				throw new AdempiereException("Las cantidades usadas en las lineas no corresponden a las unidades a procesar, sobran: "+ProductionQty.negate().toString());
		}	
	}
	
	/**
	 * Create Transformation Lines
	 * @param mustBeStocked
	 * @return lines
	 */
	public int createTransformationLines(boolean mustBeStocked) {
		
		lineno = 10;

		count = 0;

		// product to be produced
		MProduct finishedProduct = new MProduct(getCtx(), getM_Product_ID(), get_TrxName());
		
		BigDecimal MovementQty = getProductionQty();
		
		MStorageOnHand[] storages = null;
		
		MLocator finishedLocator = MLocator.get(getCtx(), getM_Locator_ID());

		int C_UOM_ID = get_ValueAsInt("C_UOM_ID");
		
		BigDecimal ConversionRate = BigDecimal.ZERO;
		
		if(finishedProduct.getC_UOM_ID()!=C_UOM_ID) {
			MovementQty = MUOMConversion.convertProductFrom(getCtx(), finishedProduct.get_ID(), C_UOM_ID, MovementQty);
			ConversionRate = MUOMConversion.getProductRateFrom(getCtx(), finishedProduct.get_ID(), C_UOM_ID);
			if(MovementQty.signum()==0 || ConversionRate==null)
				throw new AdempiereException("El producto "+finishedProduct.getValue()+"-"+finishedProduct.getName()+" no tiene conversion de unidades de medida");
		}
				
		BigDecimal ProductionQty = MovementQty;
		
		int M_Warehouse_ID = finishedLocator.getM_Warehouse_ID();
		
		int asi = 0;
		
		MClient client = MClient.get(getCtx());
		MProductCategory pc = MProductCategory.get(getCtx(),
				finishedProduct.getM_Product_Category_ID());
		String MMPolicy = pc.getMMPolicy();
		if (MMPolicy == null || MMPolicy.length() == 0) { 
			MMPolicy = client.getMMPolicy();
		}

		storages = MStorageOnHand.getWarehouse(getCtx(), M_Warehouse_ID, finishedProduct.get_ID(), 0, null,
				MProductCategory.MMPOLICY_FiFo.equals(MMPolicy), true, 0, get_TrxName());
		

		FTUMProductionLine Line = null;
		int prevLoc = -1;
		int previousAttribSet = -1;
		// Create lines from storage until qty is reached
		for (int sl = 0; sl < storages.length; sl++) {
			BigDecimal lineQty = storages[sl].getQtyOnHand();
			if (lineQty.signum() != 0) {
				if (lineQty.compareTo(MovementQty) > 0)
					lineQty = MovementQty;

				int loc = storages[sl].getM_Locator_ID();
				int slASI = storages[sl].getM_AttributeSetInstance_ID();
				int locAttribSet = new MAttributeSetInstance(getCtx(), asi,
						get_TrxName()).getM_AttributeSet_ID();

				// roll up costing attributes if in the same locator
				if (locAttribSet == 0 && previousAttribSet == 0
						&& prevLoc == loc) {
					Line.setQtyUsed(Line.getQtyUsed()
							.add(lineQty));
					Line.setPlannedQty(Line.getQtyUsed());
					Line.setMovementQty(Line.getQtyUsed().negate());
					Line.saveEx(get_TrxName());

				}
				// otherwise create new line
				else {
					Line = new FTUMProductionLine(this);
					Line.setLine(lineno);
					Line.setM_Product_ID(finishedProduct.get_ID());
					Line.setM_Locator_ID(loc);
					Line.setQtyUsed(ConversionRate.compareTo(BigDecimal.ZERO)>0?lineQty.divide(ConversionRate,6, RoundingMode.HALF_UP):lineQty);
					Line.setPlannedQty(lineQty);
					Line.setMovementQty(lineQty.negate());
					Line.setIsEndProduct(false);
					if(C_UOM_ID>0)
						Line.set_ValueOfColumn("C_UOM_ID", C_UOM_ID);
					if (slASI != 0 && locAttribSet != 0)  // ie non costing attribute
						Line.setM_AttributeSetInstance_ID(slASI);
					Line.saveEx(get_TrxName());

				}
				prevLoc = loc;
				previousAttribSet = locAttribSet;
				// enough ?
				MovementQty = MovementQty.subtract(lineQty);
				if (MovementQty.signum() == 0)
					break;
			}
		}
		if(storages.length == 0) {
			throw new AdempiereException("El producto ["+finishedProduct.getName()+"] no tiene cantidad disponible en la ubicacion ["+finishedLocator.getValue()+"]");
		}
		
		count ++;
		
		createTransformationLines(mustBeStocked, finishedProduct, ProductionQty);
		
		return count;
	}

	@SuppressWarnings("resource")
	/**
	 * Create Transformation Lines
	 * @param mustBeStocked
	 * @param finishedProduct
	 * @param requiredQty
	 * @return lines
	 */
	public int createTransformationLines(boolean mustBeStocked, MProduct finishedProduct, BigDecimal requiredQty) {
		
		lineno = lineno + 10;
		
		int defaultLocator = 0;
		
		// products used in production
		String sql = " SELECT M_ProductBom_ID, BOMQty, COALESCE(Scrap,0) as Scrap, C_UOM_ID,IsQtyPercentage FROM M_Product_BOM "
				+ " WHERE M_Product_ID=" + finishedProduct.getM_Product_ID() + " AND isactive='Y' AND (AD_Org_ID = 0 OR AD_Org_ID = " + getAD_Org_ID() + ") "
				+ " ORDER BY Line";

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());
			rs = pstmt.executeQuery();
			while (rs.next()) {		
				int BOMProduct_ID = rs.getInt(1);
				BigDecimal BOMQty = rs.getBigDecimal(2);
				int C_UOM_ID = rs.getInt(4);
				boolean isQtyPercentage = rs.getBoolean("IsQtyPercentage");
				if(isQtyPercentage)
					BOMQty= BOMQty.divide(new BigDecimal(100),2, RoundingMode.HALF_UP);
				
				BigDecimal BOMMovementQty = BOMQty.multiply(requiredQty);
				
				MProduct bomproduct = new MProduct(Env.getCtx(), BOMProduct_ID, get_TrxName());
				

				BigDecimal factor = rs.getBigDecimal(3);
				BigDecimal qtyUsed = new BigDecimal(0);
				
				if(!(factor==null)){
					factor = BigDecimal.ONE.subtract(factor.divide(new BigDecimal(100),4, RoundingMode.HALF_UP));
					qtyUsed= BOMMovementQty.multiply(factor);
				}else{
					throw new AdempiereException("El Producto hijo no tiene factor de conversión");
				}
				
				// BOM stock info
				MProduct usedProduct = MProduct.get(getCtx(), BOMProduct_ID);
				defaultLocator = usedProduct.getM_Locator_ID();
				if ( defaultLocator == 0 )
					defaultLocator = getM_Locator_ID();
				if (usedProduct == null || usedProduct.get_ID() == 0)
					return 0;
				
				if (!bomproduct.isStocked() || BOMMovementQty.signum() == 0) {		
					FTUMProductionLine line = new FTUMProductionLine(this);
					line.setLine( lineno );
					line.setM_Product_ID(BOMProduct_ID);
					line.setM_Locator_ID(defaultLocator);
					line.setMovementQty(BOMMovementQty);
					line.setPlannedQty(BOMMovementQty);
					line.setQtyUsed(BOMMovementQty);
					line.set_ValueOfColumn("MultiplyRate", factor);
					if(C_UOM_ID>0)
						line.set_ValueOfColumn("C_UOM_ID", C_UOM_ID);
					line.setIsEndProduct(true);
					line.saveEx();
					lineno = lineno + 10;
					count++;
				
				} else {					
					FTUMProductionLine line = new FTUMProductionLine( this );
					line.setLine( lineno );
					line.setM_Product_ID(BOMProduct_ID);
					line.setM_Locator_ID(defaultLocator);
					line.setMovementQty(qtyUsed);
					line.setPlannedQty(BOMMovementQty);
					line.setQtyUsed(BOMMovementQty);
					line.set_ValueOfColumn("MultiplyRate", factor);
					if(C_UOM_ID>0)
						line.set_ValueOfColumn("C_UOM_ID", C_UOM_ID);
					line.setIsEndProduct(true);
					
					line.saveEx();
					lineno = lineno + 10;
					count++;
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
	
	public int createLines(boolean mustBeStocked) {
		
		lineno = 100;

		count = 0;

		// product to be produced
		MProduct finishedProduct = new MProduct(getCtx(), getM_Product_ID(), get_TrxName());
		

		FTUMProductionLine line = new FTUMProductionLine( this );
		line.setLine( lineno );
		line.setM_Product_ID( finishedProduct.get_ID() );
		line.setM_Locator_ID( getM_Locator_ID() );
		line.setMovementQty( getProductionQty());
		line.setPlannedQty(getProductionQty());
		
		line.saveEx();
		count++;
		
		createLines(mustBeStocked, finishedProduct, getProductionQty());
		
		return count;
	}

	protected int createLines(boolean mustBeStocked, MProduct finishedProduct, BigDecimal requiredQty) {
		
		int defaultLocator = 0;
		
		MLocator finishedLocator = MLocator.get(getCtx(), getM_Locator_ID());
		
		int M_Warehouse_ID = finishedLocator.getM_Warehouse_ID();
		
		int asi = 0;

		// products used in production
		String sql = "SELECT M_ProductBom_ID, BOMQty" + " FROM M_Product_BOM"
				+ " WHERE M_Product_ID=" + finishedProduct.getM_Product_ID() + " ORDER BY Line";

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());

			rs = pstmt.executeQuery();
			while (rs.next()) {
				
				lineno = lineno + 10;
				int BOMProduct_ID = rs.getInt(1);
				BigDecimal BOMQty = rs.getBigDecimal(2);
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

					if (!bomproduct.isStocked())
					{					
						FTUMProductionLine BOMLine = null;
						BOMLine = new FTUMProductionLine( this );
						BOMLine.setLine( lineno );
						BOMLine.setM_Product_ID( BOMProduct_ID );
						BOMLine.setM_Locator_ID( defaultLocator );  
						BOMLine.setQtyUsed(BOMMovementQty );
						BOMLine.setPlannedQty( BOMMovementQty );
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
						BOMLine.saveEx(get_TrxName());

						lineno = lineno + 10;
						count++;
					}
					else
					{

						// BOM stock info
						MStorageOnHand[] storages = null;
						MProduct usedProduct = MProduct.get(getCtx(), BOMProduct_ID);
						defaultLocator = usedProduct.getM_Locator_ID();
						if ( defaultLocator == 0 )
							defaultLocator = getM_Locator_ID();
						if (usedProduct == null || usedProduct.get_ID() == 0)
							return 0;

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
									BOMLine.setPlannedQty(BOMLine.getQtyUsed());
									BOMLine.saveEx(get_TrxName());

								}
								// otherwise create new line
								else {
									BOMLine = new FTUMProductionLine( this );
									BOMLine.setLine( lineno );
									BOMLine.setM_Product_ID( BOMProduct_ID );
									BOMLine.setM_Locator_ID( loc );
									BOMLine.setQtyUsed( lineQty);
									BOMLine.setPlannedQty( lineQty);
									if ( slASI != 0 && locAttribSet != 0 )  // ie non costing attribute
										BOMLine.setM_AttributeSetInstance_ID(slASI);
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
									BOMLine.setPlannedQty( BOMMovementQty);
									BOMLine.saveEx(get_TrxName());

									lineno = lineno + 10;
									count++;
								}

							}
							else
							{
								DB.close(rs, pstmt);
								throw new AdempiereUserError("Not enough stock of " + BOMProduct_ID);
							}
						}
					}
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
	
	@Override
	protected boolean beforeSave(boolean newRecord) {
		if (getM_Product_ID() > 0) {
			if (isUseProductionPlan()) {
				setIsUseProductionPlan(false);
			}
		} else { 
			if (!isUseProductionPlan()) {
				setIsUseProductionPlan(true);
			}
		}
		
		if(is_ValueChanged(COLUMNNAME_ProductionQty)||is_ValueChanged(COLUMNNAME_M_Product_ID)) {
			setIsCreated("N");
		}
			
		return true;
	}
	
	@Override
	protected boolean beforeDelete() {
		deleteLines(get_TrxName());
		return true;
	}
	
	public void deleteLines(String trxName) {

		for (FTUMProductionLine line : getLines())
		{
			line.deleteEx(true);
		}

	}// deleteLines
	
	protected BigDecimal verifyTransformationQty(MProductionLine[] lines){
		
		BigDecimal usedQty = new BigDecimal(0);
				
		BigDecimal requiredQty = getProductionQty();
		
		
		
		for(MProductionLine line : lines){
			
			if(line.isEndProduct() && line.isActive()){
				usedQty = usedQty.add(line.getQtyUsed());
			}
			
		}
		
		if(usedQty.compareTo(requiredQty) == 0){
			return requiredQty.subtract(usedQty);
		}
		
		return BigDecimal.ZERO;
	}
}
