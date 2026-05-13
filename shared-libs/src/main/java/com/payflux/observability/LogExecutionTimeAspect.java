package com.payflux.observability;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

@Aspect
public class LogExecutionTimeAspect {
    private final ObservationRegistry registry;

    public LogExecutionTimeAspect(ObservationRegistry registry) {
        this.registry = registry;
    }

    @Around("@annotation(com.payflux.annotation.LogExecutionTime) "
            + "|| @within(com.payflux.annotation.LogExecutionTime)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        String name = pjp.getSignature().toShortString();
        return Observation.createNotStarted(name, registry)
                .lowCardinalityKeyValue("class", pjp.getTarget().getClass().getSimpleName())
                .observeChecked((Observation.CheckedCallable<Object, Throwable>) pjp::proceed);
    }
}
