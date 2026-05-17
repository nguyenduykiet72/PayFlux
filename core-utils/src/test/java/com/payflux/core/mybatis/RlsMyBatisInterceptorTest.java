package com.payflux.core.mybatis;

import com.payflux.core.tenant.TenantContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RlsMyBatisInterceptorTest {

    @Test
    void setsCurrentMerchantWhenTenantBound() throws Exception {
        var interceptor = new RlsMyBatisInterceptor();
        UUID merchantId = UUID.randomUUID();

        Executor executor = mock(Executor.class);
        Transaction tx = mock(Transaction.class);
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        MappedStatement ms = mock(MappedStatement.class);

        when(executor.getTransaction()).thenReturn(tx);
        when(tx.getConnection()).thenReturn(conn);
        when(conn.prepareStatement("SELECT set_config('app.current_merchant', ?, true)")).thenReturn(ps);

        var invocation = new Invocation(executor, Executor.class.getMethod(
                "update", MappedStatement.class, Object.class), new Object[]{ms, null});

        TenantContext.runWhere(merchantId, () -> {
            try {
                interceptor.intercept(invocation);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });

        verify(ps).setString(1, merchantId.toString());
        verify(ps).execute();
        verify(ps).close();
    }

    @Test
    void proceedsWithoutSettingWhenTenantNotBound() throws Exception {
        var interceptor = new RlsMyBatisInterceptor();

        Executor executor = mock(Executor.class);
        MappedStatement ms = mock(MappedStatement.class);

        var invocation = new Invocation(executor, Executor.class.getMethod(
                "update", MappedStatement.class, Object.class), new Object[]{ms, null});

        // Should not throw and should not attempt to get connection
        assertThatNoException().isThrownBy(() -> interceptor.intercept(invocation));
        verify(executor, never()).getTransaction();
    }
}
