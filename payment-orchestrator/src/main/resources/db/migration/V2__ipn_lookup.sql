CREATE
OR REPLACE FUNCTION payflux_find_payment_by_txn_ref(p_txn_ref TEXT)
RETURNS SETOF t_payments
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT *
    FROM t_payments
    WHERE replace(id::text, '-', '') = p_txn_ref LIMIT 1;
$$;

REVOKE ALL ON FUNCTION payflux_find_payment_by_txn_ref(TEXT) FROM public;
GRANT EXECUTE ON FUNCTION payflux_find_payment_by_txn_ref(TEXT) TO payflux_app;
GRANT EXECUTE ON FUNCTION payflux_find_payment_by_txn_ref(TEXT) TO postgres;
