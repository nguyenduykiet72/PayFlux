package com.payflux.core.mybatis;

import com.payflux.core.tenant.TenantContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * MyBatis interceptor that sets Postgres session variable before every SQL
 * statement, enabling Row Level Security to filter by tenant.
 *
 * Uses set_config('app.current_merchant', ?, true) instead of SET LOCAL
 * because SET LOCAL does not support parameterized queries — concatenating
 * the UUID string would open SQL injection risk.
 *
 * The third parameter (true) means LOCAL scope = current transaction only.
 * When the transaction commits/rolls back, the value resets. This prevents
 * connection pool reuse from leaking one tenant's context to another request.
 *
 * Consequence: all MyBatis calls must run inside @Transactional (even reads).
 */
@Intercepts({
    @Signature(type = Executor.class, method = "update",
               args = {MappedStatement.class, Object.class}),
    @Signature(type = Executor.class, method = "query",
               args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class RlsMyBatisInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(RlsMyBatisInterceptor.class);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!TenantContext.isBound()) {
            return invocation.proceed();
        }

        Executor executor = (Executor) invocation.getTarget();
        Connection conn = executor.getTransaction().getConnection();

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT set_config('app.current_merchant', ?, true)")) {
            ps.setString(1, TenantContext.get().toString());
            ps.execute();
        }
        log.debug("RLS set app.current_merchant={}", TenantContext.get());
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
}
