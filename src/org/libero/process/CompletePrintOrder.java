/******************************************************************************
  * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2007 e-Evolution,SC. All Rights Reserved.               *
 * Contributor(s): Victor Perez www.e-evolution.com                           *
 *                 Teo Sarca, www.arhipac.ro                                  *
 *****************************************************************************/
package org.libero.process;


import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.FillMandatoryException;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MDocType;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.MProductCategory;
import org.compiere.model.MQuery;
import org.compiere.model.MStorageOnHand;
import org.compiere.model.MTable;
import org.compiere.model.MWarehouse;
import org.compiere.model.PrintInfo;
import org.compiere.print.MPrintFormat;
import org.compiere.print.ReportCtl;
import org.compiere.print.ReportEngine;
import org.compiere.process.ClientProcess;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.libero.model.MPPOrder;
import org.libero.model.MPPOrderBOMLine;

/**
 * Complete & Print Manufacturing Order
 * @author victor.perez@e-evolution.com
 * @author Teo Sarca, www.arhipac.ro
 */
public class CompletePrintOrder extends SvrProcess
implements ClientProcess
{
	/** The Order */
	private int p_PP_Order_ID = 0;
	private boolean p_IsPrintPickList = false;
	private boolean p_IsPrintWorkflow = false;
	@SuppressWarnings("unused")
	private boolean p_IsPrintPackList = false; // for future use
	private boolean p_IsComplete = false;
	private boolean p_IsBatch = false;

	/**
	 * Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		for (ProcessInfoParameter para : getParameter())
		{
			String name = para.getParameterName();
			if (para.getParameter() == null);
			else if (name.equals("PP_Order_ID"))
				p_PP_Order_ID = para.getParameterAsInt(); 
			else if (name.equals("IsPrintPickList"))
				p_IsPrintPickList = para.getParameterAsBoolean();				
			else if (name.equals("IsPrintWorkflow"))
				p_IsPrintWorkflow = para.getParameterAsBoolean();
			else if (name.equals("IsPrintPackingList"))
				p_IsPrintPackList = para.getParameterAsBoolean();
			else if (name.equals("IsComplete"))
				p_IsComplete = para.getParameterAsBoolean();
			else if (name.equals("IsBatch"))
				p_IsBatch = para.getParameterAsBoolean();
			else
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
		}
		
		
	} // prepare

	/**
	 * Perform process.
	 * 
	 * @return Message (clear text)
	 * @throws Exception
	 *             if not successful
	 */
	protected String doIt() throws Exception
	{

		if (p_PP_Order_ID == 0)
		{
			throw new FillMandatoryException(MPPOrder.COLUMNNAME_PP_Order_ID);
		}

		if (p_IsComplete)
		{
			
			MPPOrder order = new MPPOrder(getCtx(), p_PP_Order_ID, get_TrxName());
			if (!order.isAvailable())
			{
				throw new AdempiereException("@NoQtyAvailable@");
			}
			
			if(	order.isProductWithOutQty()) {
				String fields = order.crititalProductsWithOutInventory(order.get_ID());
				throw new AdempiereException("Los siguientes productos: "+fields+"son criticos y no tienen Inventario, para poder realizar la Operacion dichos productos deben tener Existencia **");
			}
			//
			// Process document
			boolean ok = order.processIt(MPPOrder.DOCACTION_Complete);
			
			if (order.getDateStart() == null)
				order.setDateStart(new Timestamp(System.currentTimeMillis()));
			
			order.saveEx(get_TrxName());
			if (!ok)
			{
				throw new AdempiereException(order.getProcessMsg());
			}
			
			//	Added by Jorge Colmenarez 2020-02-24 16:05
			//	Create Inventory Movement it's automatic by DocType selected
			if(order.get_ValueAsBoolean("IsMovementAutomatic"))
			{
				createMovement(order);
			}
			//	End Jorge Colmenarez
			
			//
			// Document Status should be completed
			if (!MPPOrder.DOCSTATUS_Completed.equals(order.getDocStatus()))
			{
				throw new AdempiereException(order.getProcessMsg());
			}
		}

		if (p_IsPrintPickList)
		{
			// Get Format & Data
			ReportEngine re = this.getReportEngine("Manufacturing_Order_BOM_Header ** TEMPLATE **","PP_Order_BOM_Header_v");
			if(re == null )
			{
				return "";
			}
			ReportCtl.preview(re);
			re.print(); // prints only original
		}
		if (p_IsPrintPackList)
		{
			// Get Format & Data
			ReportEngine re = this.getReportEngine("Manufacturing_Order_BOM_Header_Packing ** TEMPLATE **","PP_Order_BOM_Header_v");
			if(re == null )
			{
				return "";
			}
			ReportCtl.preview(re);
			re.print(); // prints only original
		}
		if (p_IsPrintWorkflow)
		{
			// Get Format & Data
			ReportEngine re = this.getReportEngine("Manufacturing_Order_Workflow_Header ** TEMPLATE **","PP_Order_Workflow_Header_v");
			if(re == null )
			{
				return "";
			}
			ReportCtl.preview(re);
			re.print(); // prints only original
		}

		return "@OK@";

	} // doIt
	
	/*
	 * get the a Report Engine Instance using the view table 
	 * @param tableName
	 */
	private ReportEngine getReportEngine(String formatName, String tableName)
	{
		// Get Format & Data
		int format_id= MPrintFormat.getPrintFormat_ID(formatName, MTable.getTable_ID(tableName), getAD_Client_ID());
		MPrintFormat format = MPrintFormat.get(getCtx(), format_id, true);
		if (format == null)
		{
			addLog("@NotFound@ @AD_PrintFormat_ID@");
			return null;
		}
		// query
		MQuery query = new MQuery(tableName);
		query.addRestriction("PP_Order_ID", MQuery.EQUAL, p_PP_Order_ID);
		// Engine
		PrintInfo info = new PrintInfo(tableName,  MTable.getTable_ID(tableName), p_PP_Order_ID);
		ReportEngine re = new ReportEngine(getCtx(), format, query, info);
		return re;
	}
	
	/**
	 * Create Inventory Movement when it's automatic selection
	 * @autor Carlos Vargas, cvargas@frontuari.net
	 * @param order PP_Order object
	 */
	private void createMovement(MPPOrder order)
	{
		// Para guardar el almacen anterior
		int tmp_warehouse = 0;
		/* Para guardar el anterior encabezado */
		MMovement tmp_m_movement = null;
		
		for(MPPOrderBOMLine line : order.getLines(false)) {
			
			if(!line.get_ValueAsBoolean("IsDerivative") && !line.get_ValueAsBoolean("IsRacking")) {
				
				if(order.isProductWithInventory(line.getM_Product_ID(),order.get_ID())) {
					MWarehouse w = new MWarehouse(getCtx(), line.get_ValueAsInt("M_WarehouseSource_ID"), get_TrxName());
					boolean IsManual = w.get_ValueAsBoolean("IsManual");
					
					BigDecimal qtyBom = (BigDecimal) line.get_Value("QtyBOM");
					BigDecimal MovementQty = BigDecimal.ZERO;
					if(qtyBom != null && qtyBom.compareTo(BigDecimal.ZERO) != 0)
						MovementQty = order.getQtyOrdered().multiply(qtyBom);
					else
						MovementQty = line.getQtyRequired();
					
					// si es diferente del anterior crea una nueva cabezera...
					if(tmp_warehouse != w.getM_Warehouse_ID()) {
						//	get DocType
						MDocType dt = new MDocType(getCtx(), order.getC_DocType_ID(), get_TrxName());
						
						// crea una nueva cabezera 
						MMovement m_movement = new MMovement(getCtx(), 0, get_TrxName());
						m_movement.setAD_Org_ID(order.getAD_Org_ID());
						m_movement.setAD_OrgTrx_ID(order.getAD_Org_ID());
						m_movement.setMovementDate(new Timestamp(System.currentTimeMillis()));
						m_movement.setC_DocType_ID(dt.get_ValueAsInt("C_DocTypeMovement_ID"));
						m_movement.setIsApproved(false);
						m_movement.setIsInTransit(false);
						m_movement.set_ValueOfColumn("IsManual", IsManual);
						m_movement.saveEx(get_TrxName());
						
						// guarda el objecto del movimiento
						tmp_m_movement = m_movement;
						MMovementLine Line = null;
						
						if(p_IsBatch)
						{
							//	Comprueba Stocks by ASI
							MProductCategory pc = MProductCategory.get(getCtx(),
									line.getM_Product().getM_Product_Category_ID());
							String MMPolicy = pc.getMMPolicy();
							MStorageOnHand[] storages = MStorageOnHand.getWarehouse(getCtx(), line.get_ValueAsInt("M_WarehouseSource_ID"), line.getM_Product_ID(), 0, null,
									MProductCategory.MMPOLICY_FiFo.equals(MMPolicy), true, 0, get_TrxName());
							
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
									int locAttribSet = new MAttributeSetInstance(getCtx(), slASI,
											get_TrxName()).getM_AttributeSet_ID();

									// roll up costing attributes if in the same locator
									if (locAttribSet == 0 && previousAttribSet == 0
											&& prevLoc == loc) {
										Line.setMovementQty(lineQty);
										Line.saveEx(get_TrxName());
									}
									// otherwise create new line
									else {
										// crea una nueva linea 
										Line = new MMovementLine(m_movement);
										Line.setAD_Org_ID(line.getAD_Org_ID());
										Line.setLine(line.getLine());
										Line.setM_Product_ID(line.getM_Product_ID());
										Line.setM_Locator_ID(line.get_ValueAsInt("M_LocatorFrom_ID"));
										Line.setM_LocatorTo_ID(line.getM_Locator_ID());
										Line.setMovementQty(lineQty);
										Line.saveEx(get_TrxName());
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
						}
						else
						{
							// crea una nueva linea 
							Line = new MMovementLine(m_movement);
							Line.setAD_Org_ID(line.getAD_Org_ID());
							Line.setLine(line.getLine());
							Line.setM_Product_ID(line.getM_Product_ID());
							Line.setM_Locator_ID(line.get_ValueAsInt("M_LocatorFrom_ID"));
							Line.setM_LocatorTo_ID(line.getM_Locator_ID());
							Line.setMovementQty(MovementQty);
							Line.saveEx(get_TrxName());
						}
					}else {						
						MMovementLine Line = null;
						if(p_IsBatch)
						{
							//	Comprueba Stocks by ASI
							MProductCategory pc = MProductCategory.get(getCtx(),
									line.getM_Product().getM_Product_Category_ID());
							String MMPolicy = pc.getMMPolicy();
							MStorageOnHand[] storages = MStorageOnHand.getWarehouse(getCtx(), line.get_ValueAsInt("M_WarehouseSource_ID"), line.getM_Product_ID(), 0, null,
									MProductCategory.MMPOLICY_FiFo.equals(MMPolicy), true, 0, get_TrxName());
							
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
									int locAttribSet = new MAttributeSetInstance(getCtx(), slASI,
											get_TrxName()).getM_AttributeSet_ID();

									// roll up costing attributes if in the same locator
									if (locAttribSet == 0 && previousAttribSet == 0
											&& prevLoc == loc) {
										Line.setMovementQty(lineQty);
										Line.saveEx(get_TrxName());
									}
									// otherwise create new line
									else {
										// crea una nueva linea 
										Line = new MMovementLine(tmp_m_movement);
										Line.setAD_Org_ID(line.getAD_Org_ID());
										Line.setLine(line.getLine());
										Line.setM_Product_ID(line.getM_Product_ID());
										Line.setM_Locator_ID(line.get_ValueAsInt("M_LocatorFrom_ID"));
										Line.setM_LocatorTo_ID(line.getM_Locator_ID());
										Line.setMovementQty(lineQty);
										Line.saveEx(get_TrxName());
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
						}
						else
						{
							// crea una nueva linea 
							Line = new MMovementLine(tmp_m_movement);
							Line.setAD_Org_ID(line.getAD_Org_ID());
							Line.setLine(line.getLine());
							Line.setM_Product_ID(line.getM_Product_ID());
							Line.setM_Locator_ID(line.get_ValueAsInt("M_LocatorFrom_ID"));
							Line.setM_LocatorTo_ID(line.getM_Locator_ID());
							Line.setMovementQty(MovementQty);
							Line.saveEx(get_TrxName());
						}
					}
					//Enviar la misma orden de manufactura
					if(!tmp_m_movement.processIt(MMovement.DOCACTION_Prepare) 
							&& !tmp_m_movement.get_ValueAsBoolean("IsManual"))
					{
						throw new AdempiereException(tmp_m_movement.getProcessMsg());
					}
					
					tmp_m_movement.set_ValueOfColumn("PP_Order_ID", order.get_ID());
					tmp_m_movement.saveEx(get_TrxName());
					
					tmp_warehouse = w.getM_Warehouse_ID();
			
				}
			}
		}
	}
	
	
} // CompletePrintOrder
