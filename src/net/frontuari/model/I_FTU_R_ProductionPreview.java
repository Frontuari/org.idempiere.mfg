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
package net.frontuari.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for FTU_R_ProductionPreview
 *  @author iDempiere (generated) 
 *  @version Release 7.1
 */
@SuppressWarnings("all")
public interface I_FTU_R_ProductionPreview 
{

    /** TableName=FTU_R_ProductionPreview */
    public static final String Table_Name = "FTU_R_ProductionPreview";

    /** AD_Table_ID=1000043 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 3 - Client - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(3);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Client.
	  * Client/Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Organization.
	  * Organizational entity within client
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Organization.
	  * Organizational entity within client
	  */
	public int getAD_Org_ID();

    /** Column name AD_PInstance_ID */
    public static final String COLUMNNAME_AD_PInstance_ID = "AD_PInstance_ID";

	/** Set Process Instance.
	  * Instance of the process
	  */
	public void setAD_PInstance_ID (int AD_PInstance_ID);

	/** Get Process Instance.
	  * Instance of the process
	  */
	public int getAD_PInstance_ID();

	public org.compiere.model.I_AD_PInstance getAD_PInstance() throws RuntimeException;

    /** Column name bom_uombase_id */
    public static final String COLUMNNAME_bom_uombase_id = "bom_uombase_id";

	/** Set bom_uombase_id	  */
	public void setbom_uombase_id (int bom_uombase_id);

	/** Get bom_uombase_id	  */
	public int getbom_uombase_id();

	public org.compiere.model.I_C_UOM getbom_uombase() throws RuntimeException;

    /** Column name bom_uom_id */
    public static final String COLUMNNAME_bom_uom_id = "bom_uom_id";

	/** Set bom_uom_id	  */
	public void setbom_uom_id (int bom_uom_id);

	/** Get bom_uom_id	  */
	public int getbom_uom_id();

	public org.compiere.model.I_C_UOM getbom_uom() throws RuntimeException;

    /** Column name bom_uom_name */
    public static final String COLUMNNAME_bom_uom_name = "bom_uom_name";

	/** Set bom_uom_name	  */
	public void setbom_uom_name (String bom_uom_name);

	/** Get bom_uom_name	  */
	public String getbom_uom_name();

    /** Column name bom_uomsymbol */
    public static final String COLUMNNAME_bom_uomsymbol = "bom_uomsymbol";

	/** Set bom_uomsymbol	  */
	public void setbom_uomsymbol (String bom_uomsymbol);

	/** Get bom_uomsymbol	  */
	public String getbom_uomsymbol();

    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Created.
	  * Date this record was created
	  */
	public Timestamp getCreated();

    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Get Created By.
	  * User who created this records
	  */
	public int getCreatedBy();

    /** Column name DivideRate */
    public static final String COLUMNNAME_DivideRate = "DivideRate";

	/** Set Divide Rate.
	  * To convert Source number to Target number, the Source is divided
	  */
	public void setDivideRate (BigDecimal DivideRate);

	/** Get Divide Rate.
	  * To convert Source number to Target number, the Source is divided
	  */
	public BigDecimal getDivideRate();

    /** Column name DocumentNo */
    public static final String COLUMNNAME_DocumentNo = "DocumentNo";

	/** Set Document No.
	  * Document sequence number of the document
	  */
	public void setDocumentNo (String DocumentNo);

	/** Get Document No.
	  * Document sequence number of the document
	  */
	public String getDocumentNo();

    /** Column name FTU_R_ProductionPreview_ID */
    public static final String COLUMNNAME_FTU_R_ProductionPreview_ID = "FTU_R_ProductionPreview_ID";

	/** Set ProductionPreview	  */
	public void setFTU_R_ProductionPreview_ID (int FTU_R_ProductionPreview_ID);

	/** Get ProductionPreview	  */
	public int getFTU_R_ProductionPreview_ID();

    /** Column name FTU_R_ProductionPreview_UU */
    public static final String COLUMNNAME_FTU_R_ProductionPreview_UU = "FTU_R_ProductionPreview_UU";

	/** Set FTU_R_ProductionPreview_UU	  */
	public void setFTU_R_ProductionPreview_UU (String FTU_R_ProductionPreview_UU);

	/** Get FTU_R_ProductionPreview_UU	  */
	public String getFTU_R_ProductionPreview_UU();

    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Active.
	  * The record is active in the system
	  */
	public void setIsActive (boolean IsActive);

	/** Get Active.
	  * The record is active in the system
	  */
	public boolean isActive();

    /** Column name IsBOM */
    public static final String COLUMNNAME_IsBOM = "IsBOM";

	/** Set Bill of Materials.
	  * Bill of Materials
	  */
	public void setIsBOM (boolean IsBOM);

	/** Get Bill of Materials.
	  * Bill of Materials
	  */
	public boolean isBOM();

    /** Column name LineTotalAmt */
    public static final String COLUMNNAME_LineTotalAmt = "LineTotalAmt";

	/** Set Line Total.
	  * Total line amount incl. Tax
	  */
	public void setLineTotalAmt (BigDecimal LineTotalAmt);

	/** Get Line Total.
	  * Total line amount incl. Tax
	  */
	public BigDecimal getLineTotalAmt();

    /** Column name locator_value */
    public static final String COLUMNNAME_locator_value = "locator_value";

	/** Set locator_value	  */
	public void setlocator_value (String locator_value);

	/** Get locator_value	  */
	public String getlocator_value();

    /** Column name M_Locator_ID */
    public static final String COLUMNNAME_M_Locator_ID = "M_Locator_ID";

	/** Set Locator.
	  * Warehouse Locator
	  */
	public void setM_Locator_ID (int M_Locator_ID);

	/** Get Locator.
	  * Warehouse Locator
	  */
	public int getM_Locator_ID();

	public I_M_Locator getM_Locator() throws RuntimeException;

    /** Column name MovementQty */
    public static final String COLUMNNAME_MovementQty = "MovementQty";

	/** Set Movement Quantity.
	  * Quantity of a product moved.
	  */
	public void setMovementQty (BigDecimal MovementQty);

	/** Get Movement Quantity.
	  * Quantity of a product moved.
	  */
	public BigDecimal getMovementQty();

    /** Column name M_ProductBOM_ID */
    public static final String COLUMNNAME_M_ProductBOM_ID = "M_ProductBOM_ID";

	/** Set BOM Product.
	  * Bill of Material Component Product
	  */
	public void setM_ProductBOM_ID (int M_ProductBOM_ID);

	/** Get BOM Product.
	  * Bill of Material Component Product
	  */
	public int getM_ProductBOM_ID();

	public org.compiere.model.I_M_Product getM_ProductBOM() throws RuntimeException;

    /** Column name M_Product_ID */
    public static final String COLUMNNAME_M_Product_ID = "M_Product_ID";

	/** Set Product.
	  * Product, Service, Item
	  */
	public void setM_Product_ID (int M_Product_ID);

	/** Get Product.
	  * Product, Service, Item
	  */
	public int getM_Product_ID();

	public org.compiere.model.I_M_Product getM_Product() throws RuntimeException;

    /** Column name M_Production_ID */
    public static final String COLUMNNAME_M_Production_ID = "M_Production_ID";

	/** Set Production.
	  * Plan for producing a product
	  */
	public void setM_Production_ID (int M_Production_ID);

	/** Get Production.
	  * Plan for producing a product
	  */
	public int getM_Production_ID();

	public org.compiere.model.I_M_Production getM_Production() throws RuntimeException;

    /** Column name M_Warehouse_ID */
    public static final String COLUMNNAME_M_Warehouse_ID = "M_Warehouse_ID";

	/** Set Warehouse.
	  * Storage Warehouse and Service Point
	  */
	public void setM_Warehouse_ID (int M_Warehouse_ID);

	/** Get Warehouse.
	  * Storage Warehouse and Service Point
	  */
	public int getM_Warehouse_ID();

	public org.compiere.model.I_M_Warehouse getM_Warehouse() throws RuntimeException;

    /** Column name org_name */
    public static final String COLUMNNAME_org_name = "org_name";

	/** Set org_name	  */
	public void setorg_name (String org_name);

	/** Get org_name	  */
	public String getorg_name();

    /** Column name PP_Product_BOM_ID */
    public static final String COLUMNNAME_PP_Product_BOM_ID = "PP_Product_BOM_ID";

	/** Set BOM & Formula.
	  * BOM & Formula
	  */
	public void setPP_Product_BOM_ID (int PP_Product_BOM_ID);

	/** Get BOM & Formula.
	  * BOM & Formula
	  */
	public int getPP_Product_BOM_ID();

	public org.eevolution.model.I_PP_Product_BOM getPP_Product_BOM() throws RuntimeException;

    /** Column name pp_product_bom_name */
    public static final String COLUMNNAME_pp_product_bom_name = "pp_product_bom_name";

	/** Set pp_product_bom_name	  */
	public void setpp_product_bom_name (String pp_product_bom_name);

	/** Get pp_product_bom_name	  */
	public String getpp_product_bom_name();

    /** Column name pp_product_bom_value */
    public static final String COLUMNNAME_pp_product_bom_value = "pp_product_bom_value";

	/** Set pp_product_bom_value	  */
	public void setpp_product_bom_value (String pp_product_bom_value);

	/** Get pp_product_bom_value	  */
	public String getpp_product_bom_value();

    /** Column name PriceActual */
    public static final String COLUMNNAME_PriceActual = "PriceActual";

	/** Set Unit Price.
	  * Actual Price 
	  */
	public void setPriceActual (BigDecimal PriceActual);

	/** Get Unit Price.
	  * Actual Price 
	  */
	public BigDecimal getPriceActual();

    /** Column name product_bom_name */
    public static final String COLUMNNAME_product_bom_name = "product_bom_name";

	/** Set product_bom_name	  */
	public void setproduct_bom_name (String product_bom_name);

	/** Get product_bom_name	  */
	public String getproduct_bom_name();

    /** Column name product_bom_sku */
    public static final String COLUMNNAME_product_bom_sku = "product_bom_sku";

	/** Set product_bom_sku	  */
	public void setproduct_bom_sku (String product_bom_sku);

	/** Get product_bom_sku	  */
	public String getproduct_bom_sku();

    /** Column name Product_BOM_Value */
    public static final String COLUMNNAME_Product_BOM_Value = "Product_BOM_Value";

	/** Set Product BOM Key.
	  * Key of Product BOM
	  */
	public void setProduct_BOM_Value (String Product_BOM_Value);

	/** Get Product BOM Key.
	  * Key of Product BOM
	  */
	public String getProduct_BOM_Value();

    /** Column name ProductionMethod */
    public static final String COLUMNNAME_ProductionMethod = "ProductionMethod";

	/** Set Method of production	  */
	public void setProductionMethod (String ProductionMethod);

	/** Get Method of production	  */
	public String getProductionMethod();

    /** Column name product_name */
    public static final String COLUMNNAME_product_name = "product_name";

	/** Set product_name	  */
	public void setproduct_name (String product_name);

	/** Get product_name	  */
	public String getproduct_name();

    /** Column name productuom_id */
    public static final String COLUMNNAME_productuom_id = "productuom_id";

	/** Set productuom_id	  */
	public void setproductuom_id (int productuom_id);

	/** Get productuom_id	  */
	public int getproductuom_id();

	public org.compiere.model.I_M_Product getproductuom() throws RuntimeException;

    /** Column name product_value */
    public static final String COLUMNNAME_product_value = "product_value";

	/** Set product_value	  */
	public void setproduct_value (String product_value);

	/** Get product_value	  */
	public String getproduct_value();

    /** Column name QtyAvailable */
    public static final String COLUMNNAME_QtyAvailable = "QtyAvailable";

	/** Set Available Quantity.
	  * Available Quantity (On Hand - Reserved)
	  */
	public void setQtyAvailable (BigDecimal QtyAvailable);

	/** Get Available Quantity.
	  * Available Quantity (On Hand - Reserved)
	  */
	public BigDecimal getQtyAvailable();

    /** Column name QtyOnHand */
    public static final String COLUMNNAME_QtyOnHand = "QtyOnHand";

	/** Set On Hand Quantity.
	  * On Hand Quantity
	  */
	public void setQtyOnHand (BigDecimal QtyOnHand);

	/** Get On Hand Quantity.
	  * On Hand Quantity
	  */
	public BigDecimal getQtyOnHand();

    /** Column name QtyOrdered */
    public static final String COLUMNNAME_QtyOrdered = "QtyOrdered";

	/** Set Ordered Quantity.
	  * Ordered Quantity
	  */
	public void setQtyOrdered (BigDecimal QtyOrdered);

	/** Get Ordered Quantity.
	  * Ordered Quantity
	  */
	public BigDecimal getQtyOrdered();

    /** Column name QtyReserved */
    public static final String COLUMNNAME_QtyReserved = "QtyReserved";

	/** Set Reserved Quantity.
	  * Reserved Quantity
	  */
	public void setQtyReserved (BigDecimal QtyReserved);

	/** Get Reserved Quantity.
	  * Reserved Quantity
	  */
	public BigDecimal getQtyReserved();

    /** Column name QtyUsed */
    public static final String COLUMNNAME_QtyUsed = "QtyUsed";

	/** Set Quantity Used	  */
	public void setQtyUsed (BigDecimal QtyUsed);

	/** Get Quantity Used	  */
	public BigDecimal getQtyUsed();

    /** Column name Scrap */
    public static final String COLUMNNAME_Scrap = "Scrap";

	/** Set Scrap %.
	  * Indicate the Scrap %  for calculate the Scrap Quantity
	  */
	public void setScrap (BigDecimal Scrap);

	/** Get Scrap %.
	  * Indicate the Scrap %  for calculate the Scrap Quantity
	  */
	public BigDecimal getScrap();

    /** Column name ScrappedQty */
    public static final String COLUMNNAME_ScrappedQty = "ScrappedQty";

	/** Set Scrapped Quantity.
	  * The Quantity scrapped due to QA issues
	  */
	public void setScrappedQty (BigDecimal ScrappedQty);

	/** Get Scrapped Quantity.
	  * The Quantity scrapped due to QA issues
	  */
	public BigDecimal getScrappedQty();

    /** Column name SKU */
    public static final String COLUMNNAME_SKU = "SKU";

	/** Set SKU.
	  * Stock Keeping Unit
	  */
	public void setSKU (String SKU);

	/** Get SKU.
	  * Stock Keeping Unit
	  */
	public String getSKU();

    /** Column name TotalAmt */
    public static final String COLUMNNAME_TotalAmt = "TotalAmt";

	/** Set Total Amount.
	  * Total Amount
	  */
	public void setTotalAmt (BigDecimal TotalAmt);

	/** Get Total Amount.
	  * Total Amount
	  */
	public BigDecimal getTotalAmt();

    /** Column name TotalPrice */
    public static final String COLUMNNAME_TotalPrice = "TotalPrice";

	/** Set Total Price	  */
	public void setTotalPrice (BigDecimal TotalPrice);

	/** Get Total Price	  */
	public BigDecimal getTotalPrice();

    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Updated.
	  * Date this record was updated
	  */
	public Timestamp getUpdated();

    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/** Get Updated By.
	  * User who updated this records
	  */
	public int getUpdatedBy();

    /** Column name warehouse_name */
    public static final String COLUMNNAME_warehouse_name = "warehouse_name";

	/** Set warehouse_name	  */
	public void setwarehouse_name (String warehouse_name);

	/** Get warehouse_name	  */
	public String getwarehouse_name();
}
