CREATE OR REPLACE FUNCTION adempiere.BomQtyReservedASIByLocator(p_product_id numeric, attributesetinstance_id numeric, p_locator_id numeric)
 RETURNS numeric
 LANGUAGE plpgsql
AS $function$
DECLARE
	v_Quantity numeric := 99999;
	v_IsBOM CHAR(1);
	v_IsStocked CHAR(1);
	v_ProductType CHAR(1);
	v_ProductQty numeric;
	v_StdPrecision int;
	bom record;
BEGIN
	BEGIN 
		SELECT
			IsBOM, ProductType, IsStocked
				INTO
			v_IsBOM, v_ProductType, v_IsStocked
		FROM M_PRODUCT
		WHERE M_Product_ID=p_Product_ID;
		EXCEPTION WHEN OTHERS THEN
			RETURN 0;
	END;
	IF (v_IsBOM='N' AND (v_ProductType<>'I' OR v_IsStocked='N')) THEN
		RETURN 0;
	ELSIF (v_IsStocked='Y') THEN
		SELECT
			COALESCE(SUM(QtyReserved), 0)
				INTO
			v_ProductQty
		FROM M_STORAGE s
		WHERE M_Product_ID=p_Product_ID
		AND s.M_Locator_ID = p_locator_id
		AND (s.M_AttributeSetInstance_ID = AttributeSetInstance_ID
			OR COALESCE(AttributeSetInstance_ID,0) = 0);
		RETURN v_ProductQty;
	END IF;
	FOR bom IN
		SELECT
			bl.M_Product_ID AS M_ProductBOM_ID
			, CASE WHEN bl.IsQtyPercentage = 'N' THEN bl.QtyBOM ELSE bl.QtyBatch / 100 END AS BomQty
			, p.IsBOM
			, p.IsStocked
			, p.ProductType
		FROM PP_PRODUCT_BOM b
		INNER JOIN M_PRODUCT p ON (p.M_Product_ID=b.M_Product_ID)
	INNER JOIN PP_PRODUCT_BOMLINE bl ON (bl.PP_Product_BOM_ID=b.PP_Product_BOM_ID)
		WHERE b.M_Product_ID = p_Product_ID
	LOOP
		IF (bom.ProductType = 'I' AND bom.IsStocked = 'Y') THEN
			SELECT
				COALESCE(SUM(QtyReserved), 0)
					INTO
			v_ProductQty
			FROM M_STORAGE s
			WHERE M_Product_ID=bom.M_ProductBOM_ID
			AND s.M_Locator_ID = p_locator_id
			AND (s.M_AttributeSetInstance_ID = AttributeSetInstance_ID
				OR COALESCE(AttributeSetInstance_ID,0) = 0);
			
			SELECT
				COALESCE(MAX(u.StdPrecision), 0)
					INTO
				v_StdPrecision
			FROM C_UOM u, M_PRODUCT p
			WHERE u.C_UOM_ID=p.C_UOM_ID
			AND p.M_Product_ID=bom.M_ProductBOM_ID;
			v_ProductQty := ROUND (v_ProductQty/bom.BOMQty, v_StdPrecision);
			
			IF (v_ProductQty < v_Quantity) THEN
				v_Quantity := v_ProductQty;
			END IF;
		ELSIF (bom.IsBOM = 'Y') THEN
			v_ProductQty := BomQtyReservedASIByLocator (bom.M_ProductBOM_ID, AttributeSetInstance_ID, p_Locator_ID);
			IF (v_ProductQty < v_Quantity) THEN
				v_Quantity := v_ProductQty;
			END IF;
		END IF;
	END LOOP;
	IF (v_Quantity = 99999) THEN
		RETURN 0;
	END IF;
	IF (v_Quantity > 0) THEN
		SELECT
			COALESCE(MAX(u.StdPrecision), 0)
			INTO
			v_StdPrecision
		FROM C_UOM u, M_PRODUCT p
		WHERE u.C_UOM_ID=p.C_UOM_ID
		AND p.M_Product_ID=p_Product_ID;
		RETURN ROUND (v_Quantity, v_StdPrecision );
	END IF;
	RETURN 0;
END;
$function$

