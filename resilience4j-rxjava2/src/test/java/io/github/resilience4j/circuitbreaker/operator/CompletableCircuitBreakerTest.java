package io.github.resilience4j.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.reactivex.Completable;
import org.junit.Test;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Unit test for {@link CompletableCircuitBreaker}.
 */
public class CompletableCircuitBreakerTest extends BaseCircuitBreakerTest {

    @Test
    public void shouldSubscribeToCompletable() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);

        Completable.complete()
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertSubscribed()
            .assertComplete();

        verify(circuitBreaker, times(1)).onSuccess(anyLong());
        verify(circuitBreaker, never()).onError(anyLong(), any(Throwable.class));
    }

    @Test
    public void shouldPropagateAndMarkError() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);

        Completable.error(new IOException("BAM!"))
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertSubscribed()
            .assertError(IOException.class)
            .assertNotComplete();

        verify(circuitBreaker, times(1)).onError(anyLong(), any(IOException.class));
        verify(circuitBreaker, never()).onSuccess(anyLong());
    }

    @Test
    public void shouldEmitErrorWithCallNotPermittedException() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(false);

        Completable.complete()
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertSubscribed()
            .assertError(CallNotPermittedException.class)
            .assertNotComplete();

        verify(circuitBreaker, never()).onSuccess(anyLong());
        verify(circuitBreaker, never()).onError(anyLong(), any(Throwable.class));
    }
}
