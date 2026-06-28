package com.payflux.payment_orchestrator.infrastructure.persistence;

import com.payflux.payment_orchestrator.domain.OutboxEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OutboxEventMapper {
    int insert(OutboxEvent event);
}
