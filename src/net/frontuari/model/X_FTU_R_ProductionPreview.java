/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2012 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package net.frontuari.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for FTU_R_ProductionPreview
 *  @author iDempiere (generated) 
 *  @version Release 7.1 - $Id$ */
public class X_FTU_R_ProductionPreview extends PO implements I_FTU_R_ProductionPreview, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20200812L;

    /** Standard Constructor */
    public X_FTU_R_ProductionPreview (Properties ctx, int FTU_R_ProductionPreview_ID, String trxName)
    {
      super (ctx, FTU_R_ProductionPreview_ID, trxName);
      /** if (FTU_R_ProductionPreview_ID == 0)
        {
			setAD_PInstance_ID (0);
			setFTU_R_ProductionPreview_ID (0);
        } */
    }

    /** Load Constructor */
    public X_FTU_R_ProductionPreview (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 3 - Client - Org 
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuffer sb = new StringBuffer ("X_FTU_R_ProductionPreview[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_AD_PInstance getAD_PInstance() throws RuntimeException
    {
		return (org.compiere.model.I_AD_PInstance)MTable.get(getCtx(), org.compiere.model.I_AD_PInstance.Table_Name)
			.getPO(getAD_PInstance_ID(), get_TrxName());	}

	/** Set Process Instance.
		@param AD_PInstance_ID 
		Instance of the process
	  */
	public void setAD_PInstance_ID (int AD_PInstance_ID)
	{
		if (AD_PInstance_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_AD_PInstance_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_AD_PInstance_ID, Integer.valueOf(AD_PInstance_ID));
	}

	/** Get Process Instance.
		@return Instance of the process
	  */
	public int getAD_PInstance_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_PInstance_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_UOM getbom_uombase() throws RuntimeException
    {
		return (org.compiere.model.I_C_UOM)MTable.get(getCtx(), org.compiere.model.I_C_UOM.Table_Name)
			.getPO(getbom_uombase_id(), get_TrxName());	}

	/** Set bom_uombase_id.
		@param bom_uombase_id bom_uombase_id	  */
	public void setbom_uombase_id (int bom_uombase_id)
	{
		set_ValueNoCheck (COLUMNNAME_bom_uombase_id, Integer.valueOf(bom_uombase_id));
	}

	/** Get bom_uombase_id.
		@return bom_uombase_id	  */
	public int getbom_uombase_id () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_bom_uombase_id);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_UOM getbom_uom() throws RuntimeException
    {
		return (org.compiere.model.I_C_UOM)MTable.get(getCtx(), org.compiere.model.I_C_UOM.Table_Name)
			.getPO(getbom_uom_id(), get_TrxName());	}

	/** Set bom_uom_id.
		@param bom_uom_id bom_uom_id	  */
	public void setbom_uom_id (int bom_uom_id)
	{
		set_ValueNoCheck (COLUMNNAME_bom_uom_id, Integer.valueOf(bom_uom_id));
	}

	/** Get bom_uom_id.
		@return bom_uom_id	  */
	public int getbom_uom_id () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_bom_uom_id);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set bom_uom_name.
		@param bom_uom_name bom_uom_name	  */
	public void setbom_uom_name (String bom_uom_name)
	{
		set_ValueNoCheck (COLUMNNAME_bom_uom_name, bom_uom_name);
	}

	/** Get bom_uom_name.
		@return bom_uom_name	  */
	public String getbom_uom_name () 
	{
		return (String)get_Value(COLUMNNAME_bom_uom_name);
	}

	/** Set bom_uomsymbol.
		@param bom_uomsymbol bom_uomsymbol	  */
	public void setbom_uomsymbol (String bom_uomsymbol)
	{
		set_ValueNoCheck (COLUMNNAME_bom_uomsymbol, bom_uomsymbol);
	}

	/** Get bom_uomsymbol.
		@return bom_uomsymbol	  */
	public String getbom_uomsymbol () 
	{
		return (String)get_Value(COLUMNNAME_bom_uomsymbol);
	}

	/** Set Divide Rate.
		@param DivideRate 
		To convert Source number to Target number, the Source is divided
	  */
	public void setDivideRate (BigDecimal DivideRate)
	{
		set_ValueNoCheck (COLUMNNAME_DivideRate, DivideRate);
	}

	/** Get Divide Rate.
		@return To convert Source number to Target number, the Source is divided
	  */
	public BigDecimal getDivideRate () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_DivideRate);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Document No.
		@param DocumentNo 
		Document sequence number of the document
	  */
	public void setDocumentNo (String DocumentNo)
	{
		set_ValueNoCheck (COLUMNNAME_DocumentNo, DocumentNo);
	}

	/** Get Document No.
		@return Document sequence number of the document
	  */
	public String getDocumentNo () 
	{
		return (String)get_Value(COLUMNNAME_DocumentNo);
	}

	/** Set ProductionPreview.
		@param FTU_R_ProductionPreview_ID ProductionPreview	  */
	public void setFTU_R_ProductionPreview_ID (int FTU_R_ProductionPreview_ID)
	{
		if (FTU_R_ProductionPreview_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_FTU_R_ProductionPreview_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_FTU_R_ProductionPreview_ID, Integer.valueOf(FTU_R_ProductionPreview_ID));
	}

	/** Get ProductionPreview.
		@return ProductionPreview	  */
	public int getFTU_R_ProductionPreview_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_FTU_R_ProductionPreview_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set FTU_R_ProductionPreview_UU.
		@param FTU_R_ProductionPreview_UU FTU_R_ProductionPreview_UU	  */
	public void setFTU_R_ProductionPreview_UU (String FTU_R_ProductionPreview_UU)
	{
		set_ValueNoCheck (COLUMNNAME_FTU_R_ProductionPreview_UU, FTU_R_ProductionPreview_UU);
	}

	/** Get FTU_R_ProductionPreview_UU.
		@return FTU_R_ProductionPreview_UU	  */
	public String getFTU_R_ProductionPreview_UU () 
	{
		return (String)get_Value(COLUMNNAME_FTU_R_ProductionPreview_UU);
	}

	/** Set Bill of Materials.
		@param IsBOM 
		Bill of Materials
	  */
	public void setIsBOM (boolean IsBOM)
	{
		set_ValueNoCheck (COLUMNNAME_IsBOM, Boolean.valueOf(IsBOM));
	}

	/** Get Bill of Materials.
		@return Bill of Materials
	  */
	public boolean isBOM () 
	{
		Object oo = get_Value(COLUMNNAME_IsBOM);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Line Total.
		@param LineTotalAmt 
		Total line amount incl. Tax
	  */
	public void setLineTotalAmt (BigDecimal LineTotalAmt)
	{
		set_Value (COLUMNNAME_LineTotalAmt, LineTotalAmt);
	}

	/** Get Line Total.
		@return Total line amount incl. Tax
	  */
	public BigDecimal getLineTotalAmt () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_LineTotalAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set locator_value.
		@param locator_value locator_value	  */
	public void setlocator_value (String locator_value)
	{
		set_ValueNoCheck (COLUMNNAME_locator_value, locator_value);
	}

	/** Get locator_value.
		@return locator_value	  */
	public String getlocator_value () 
	{
		return (String)get_Value(COLUMNNAME_locator_value);
	}

	public I_M_Locator getM_Locator() throws RuntimeException
    {
		return (I_M_Locator)MTable.get(getCtx(), I_M_Locator.Table_Name)
			.getPO(getM_Locator_ID(), get_TrxName());	}

	/** Set Locator.
		@param M_Locator_ID 
		Warehouse Locator
	  */
	public void setM_Locator_ID (int M_Locator_ID)
	{
		if (M_Locator_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_Locator_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_Locator_ID, Integer.valueOf(M_Locator_ID));
	}

	/** Get Locator.
		@return Warehouse Locator
	  */
	public int getM_Locator_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Locator_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Movement Quantity.
		@param MovementQty 
		Quantity of a product moved.
	  */
	public void setMovementQty (BigDecimal MovementQty)
	{
		set_ValueNoCheck (COLUMNNAME_MovementQty, MovementQty);
	}

	/** Get Movement Quantity.
		@return Quantity of a product moved.
	  */
	public BigDecimal getMovementQty () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_MovementQty);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	public org.compiere.model.I_M_Product getM_ProductBOM() throws RuntimeException
    {
		return (org.compiere.model.I_M_Product)MTable.get(getCtx(), org.compiere.model.I_M_Product.Table_Name)
			.getPO(getM_ProductBOM_ID(), get_TrxName());	}

	/** Set BOM Product.
		@param M_ProductBOM_ID 
		Bill of Material Component Product
	  */
	public void setM_ProductBOM_ID (int M_ProductBOM_ID)
	{
		if (M_ProductBOM_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_ProductBOM_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_ProductBOM_ID, Integer.valueOf(M_ProductBOM_ID));
	}

	/** Get BOM Product.
		@return Bill of Material Component Product
	  */
	public int getM_ProductBOM_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_ProductBOM_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_M_Product getM_Product() throws RuntimeException
    {
		return (org.compiere.model.I_M_Product)MTable.get(getCtx(), org.compiere.model.I_M_Product.Table_Name)
			.getPO(getM_Product_ID(), get_TrxName());	}

	/** Set Product.
		@param M_Product_ID 
		Product, Service, Item
	  */
	public void setM_Product_ID (int M_Product_ID)
	{
		if (M_Product_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_Product_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_Product_ID, Integer.valueOf(M_Product_ID));
	}

	/** Get Product.
		@return Product, Service, Item
	  */
	public int getM_Product_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Product_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_M_Production getM_Production() throws RuntimeException
    {
		return (org.compiere.model.I_M_Production)MTable.get(getCtx(), org.compiere.model.I_M_Production.Table_Name)
			.getPO(getM_Production_ID(), get_TrxName());	}

	/** Set Production.
		@param M_Production_ID 
		Plan for producing a product
	  */
	public void setM_Production_ID (int M_Production_ID)
	{
		if (M_Production_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_Production_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_Production_ID, Integer.valueOf(M_Production_ID));
	}

	/** Get Production.
		@return Plan for producing a product
	  */
	public int getM_Production_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Production_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_M_Warehouse getM_Warehouse() throws RuntimeException
    {
		return (org.compiere.model.I_M_Warehouse)MTable.get(getCtx(), org.compiere.model.I_M_Warehouse.Table_Name)
			.getPO(getM_Warehouse_ID(), get_TrxName());	}

	/** Set Warehouse.
		@param M_Warehouse_ID 
		Storage Warehouse and Service Point
	  */
	public void setM_Warehouse_ID (int M_Warehouse_ID)
	{
		if (M_Warehouse_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_Warehouse_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_Warehouse_ID, Integer.valueOf(M_Warehouse_ID));
	}

	/** Get Warehouse.
		@return Storage Warehouse and Service Point
	  */
	public int getM_Warehouse_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Warehouse_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set org_name.
		@param org_name org_name	  */
	public void setorg_name (String org_name)
	{
		set_ValueNoCheck (COLUMNNAME_org_name, org_name);
	}

	/** Get org_name.
		@return org_name	  */
	public String getorg_name () 
	{
		return (String)get_Value(COLUMNNAME_org_name);
	}

	public org.eevolution.model.I_PP_Product_BOM getPP_Product_BOM() throws RuntimeException
    {
		return (org.eevolution.model.I_PP_Product_BOM)MTable.get(getCtx(), org.eevolution.model.I_PP_Product_BOM.Table_Name)
			.getPO(getPP_Product_BOM_ID(), get_TrxName());	}

	/** Set BOM & Formula.
		@param PP_Product_BOM_ID 
		BOM & Formula
	  */
	public void setPP_Product_BOM_ID (int PP_Product_BOM_ID)
	{
		if (PP_Product_BOM_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_PP_Product_BOM_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_PP_Product_BOM_ID, Integer.valueOf(PP_Product_BOM_ID));
	}

	/** Get BOM & Formula.
		@return BOM & Formula
	  */
	public int getPP_Product_BOM_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PP_Product_BOM_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set pp_product_bom_name.
		@param pp_product_bom_name pp_product_bom_name	  */
	public void setpp_product_bom_name (String pp_product_bom_name)
	{
		set_ValueNoCheck (COLUMNNAME_pp_product_bom_name, pp_product_bom_name);
	}

	/** Get pp_product_bom_name.
		@return pp_product_bom_name	  */
	public String getpp_product_bom_name () 
	{
		return (String)get_Value(COLUMNNAME_pp_product_bom_name);
	}

	/** Set pp_product_bom_value.
		@param pp_product_bom_value pp_product_bom_value	  */
	public void setpp_product_bom_value (String pp_product_bom_value)
	{
		set_ValueNoCheck (COLUMNNAME_pp_product_bom_value, pp_product_bom_value);
	}

	/** Get pp_product_bom_value.
		@return pp_product_bom_value	  */
	public String getpp_product_bom_value () 
	{
		return (String)get_Value(COLUMNNAME_pp_product_bom_value);
	}

	/** Set Unit Price.
		@param PriceActual 
		Actual Price 
	  */
	public void setPriceActual (BigDecimal PriceActual)
	{
		set_ValueNoCheck (COLUMNNAME_PriceActual, PriceActual);
	}

	/** Get Unit Price.
		@return Actual Price 
	  */
	public BigDecimal getPriceActual () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_PriceActual);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set product_bom_name.
		@param product_bom_name product_bom_name	  */
	public void setproduct_bom_name (String product_bom_name)
	{
		set_ValueNoCheck (COLUMNNAME_product_bom_name, product_bom_name);
	}

	/** Get product_bom_name.
		@return product_bom_name	  */
	public String getproduct_bom_name () 
	{
		return (String)get_Value(COLUMNNAME_product_bom_name);
	}

	/** Set product_bom_sku.
		@param product_bom_sku product_bom_sku	  */
	public void setproduct_bom_sku (String product_bom_sku)
	{
		set_ValueNoCheck (COLUMNNAME_product_bom_sku, product_bom_sku);
	}

	/** Get product_bom_sku.
		@return product_bom_sku	  */
	public String getproduct_bom_sku () 
	{
		return (String)get_Value(COLUMNNAME_product_bom_sku);
	}

	/** Set Product BOM Key.
		@param Product_BOM_Value 
		Key of Product BOM
	  */
	public void setProduct_BOM_Value (String Product_BOM_Value)
	{
		set_ValueNoCheck (COLUMNNAME_Product_BOM_Value, Product_BOM_Value);
	}

	/** Get Product BOM Key.
		@return Key of Product BOM
	  */
	public String getProduct_BOM_Value () 
	{
		return (String)get_Value(COLUMNNAME_Product_BOM_Value);
	}

	/** Production = P */
	public static final String PRODUCTIONMETHOD_Production = "P";
	/** Transformation = T */
	public static final String PRODUCTIONMETHOD_Transformation = "T";
	/** Set Method of production.
		@param ProductionMethod Method of production	  */
	public void setProductionMethod (String ProductionMethod)
	{

		set_ValueNoCheck (COLUMNNAME_ProductionMethod, ProductionMethod);
	}

	/** Get Method of production.
		@return Method of production	  */
	public String getProductionMethod () 
	{
		return (String)get_Value(COLUMNNAME_ProductionMethod);
	}

	/** Set product_name.
		@param product_name product_name	  */
	public void setproduct_name (String product_name)
	{
		set_ValueNoCheck (COLUMNNAME_product_name, product_name);
	}

	/** Get product_name.
		@return product_name	  */
	public String getproduct_name () 
	{
		return (String)get_Value(COLUMNNAME_product_name);
	}

	public org.compiere.model.I_M_Product getproductuom() throws RuntimeException
    {
		return (org.compiere.model.I_M_Product)MTable.get(getCtx(), org.compiere.model.I_M_Product.Table_Name)
			.getPO(getproductuom_id(), get_TrxName());	}

	/** Set productuom_id.
		@param productuom_id productuom_id	  */
	public void setproductuom_id (int productuom_id)
	{
		set_ValueNoCheck (COLUMNNAME_productuom_id, Integer.valueOf(productuom_id));
	}

	/** Get productuom_id.
		@return productuom_id	  */
	public int getproductuom_id () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_productuom_id);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set product_value.
		@param product_value product_value	  */
	public void setproduct_value (String product_value)
	{
		set_ValueNoCheck (COLUMNNAME_product_value, product_value);
	}

	/** Get product_value.
		@return product_value	  */
	public String getproduct_value () 
	{
		return (String)get_Value(COLUMNNAME_product_value);
	}

	/** Set Available Quantity.
		@param QtyAvailable 
		Available Quantity (On Hand - Reserved)
	  */
	public void setQtyAvailable (BigDecimal QtyAvailable)
	{
		set_ValueNoCheck (COLUMNNAME_QtyAvailable, QtyAvailable);
	}

	/** Get Available Quantity.
		@return Available Quantity (On Hand - Reserved)
	  */
	public BigDecimal getQtyAvailable () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyAvailable);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set On Hand Quantity.
		@param QtyOnHand 
		On Hand Quantity
	  */
	public void setQtyOnHand (BigDecimal QtyOnHand)
	{
		set_ValueNoCheck (COLUMNNAME_QtyOnHand, QtyOnHand);
	}

	/** Get On Hand Quantity.
		@return On Hand Quantity
	  */
	public BigDecimal getQtyOnHand () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyOnHand);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Ordered Quantity.
		@param QtyOrdered 
		Ordered Quantity
	  */
	public void setQtyOrdered (BigDecimal QtyOrdered)
	{
		set_ValueNoCheck (COLUMNNAME_QtyOrdered, QtyOrdered);
	}

	/** Get Ordered Quantity.
		@return Ordered Quantity
	  */
	public BigDecimal getQtyOrdered () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyOrdered);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Reserved Quantity.
		@param QtyReserved 
		Reserved Quantity
	  */
	public void setQtyReserved (BigDecimal QtyReserved)
	{
		set_ValueNoCheck (COLUMNNAME_QtyReserved, QtyReserved);
	}

	/** Get Reserved Quantity.
		@return Reserved Quantity
	  */
	public BigDecimal getQtyReserved () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyReserved);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Quantity Used.
		@param QtyUsed Quantity Used	  */
	public void setQtyUsed (BigDecimal QtyUsed)
	{
		set_ValueNoCheck (COLUMNNAME_QtyUsed, QtyUsed);
	}

	/** Get Quantity Used.
		@return Quantity Used	  */
	public BigDecimal getQtyUsed () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyUsed);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Scrap %.
		@param Scrap 
		Indicate the Scrap %  for calculate the Scrap Quantity
	  */
	public void setScrap (BigDecimal Scrap)
	{
		set_ValueNoCheck (COLUMNNAME_Scrap, Scrap);
	}

	/** Get Scrap %.
		@return Indicate the Scrap %  for calculate the Scrap Quantity
	  */
	public BigDecimal getScrap () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Scrap);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Scrapped Quantity.
		@param ScrappedQty 
		The Quantity scrapped due to QA issues
	  */
	public void setScrappedQty (BigDecimal ScrappedQty)
	{
		set_Value (COLUMNNAME_ScrappedQty, ScrappedQty);
	}

	/** Get Scrapped Quantity.
		@return The Quantity scrapped due to QA issues
	  */
	public BigDecimal getScrappedQty () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ScrappedQty);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set SKU.
		@param SKU 
		Stock Keeping Unit
	  */
	public void setSKU (String SKU)
	{
		set_ValueNoCheck (COLUMNNAME_SKU, SKU);
	}

	/** Get SKU.
		@return Stock Keeping Unit
	  */
	public String getSKU () 
	{
		return (String)get_Value(COLUMNNAME_SKU);
	}

	/** Set Total Amount.
		@param TotalAmt 
		Total Amount
	  */
	public void setTotalAmt (BigDecimal TotalAmt)
	{
		set_ValueNoCheck (COLUMNNAME_TotalAmt, TotalAmt);
	}

	/** Get Total Amount.
		@return Total Amount
	  */
	public BigDecimal getTotalAmt () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_TotalAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Total Price.
		@param TotalPrice Total Price	  */
	public void setTotalPrice (BigDecimal TotalPrice)
	{
		set_ValueNoCheck (COLUMNNAME_TotalPrice, TotalPrice);
	}

	/** Get Total Price.
		@return Total Price	  */
	public BigDecimal getTotalPrice () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_TotalPrice);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set warehouse_name.
		@param warehouse_name warehouse_name	  */
	public void setwarehouse_name (String warehouse_name)
	{
		set_ValueNoCheck (COLUMNNAME_warehouse_name, warehouse_name);
	}

	/** Get warehouse_name.
		@return warehouse_name	  */
	public String getwarehouse_name () 
	{
		return (String)get_Value(COLUMNNAME_warehouse_name);
	}
}