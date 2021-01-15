CREATE FUNCTION getReservedQty(p_M_ProductionLine_ID NUMERIC) RETURNS NUMERIC AS
	'SELECT
		COALESCE(pob.qtyreserved, 0)
	FROM pp_order_bomline pob 
	INNER JOIN m_productionline mp ON mp.pp_order_bomline_id = pob.pp_order_bomline_id 
	WHERE mp.m_productionline_id = $1;'
LANGUAGE SQL;