package io.github.resilience4j.ratelimiter.operator;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.reactivex.Observable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.mockito.BDDMockito.given;

/**
 * Unit test for {@link ObserverRateLimiter}.
 */
public class ObserverRateLimiterTest {

    private RateLimiter rateLimiter;

    @Before
    public void setUp(){
        rateLimiter = Mockito.mock(RateLimiter.class);
    }

    @Test
    public void shouldEmitSingleEventWithSinglePermit() {
        given(rateLimiter.reservePermission()).willReturn(Duration.ofSeconds(0).toNanos());

        Observable.just(1)
            .compose(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertResult(1);
    }

    @Test
    public void shouldDelaySubscription() {
        given(rateLimiter.reservePermission()).willReturn(Duration.ofSeconds(1).toNanos());

        Observable.just(1)
                .compose(RateLimiterOperator.of(rateLimiter))
                .test()
                .awaitTerminalEvent(2, TimeUnit.SECONDS);
    }

    @Test
    public void shouldEmitAllEvents() {
        given(rateLimiter.reservePermission()).willReturn(Duration.ofSeconds(0).toNanos());

        Observable.fromArray(1, 2)
            .compose(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertResult(1, 2);
    }

    @Test
    public void shouldPropagateError() {
        given(rateLimiter.reservePermission()).willReturn(Duration.ofSeconds(0).toNanos());

        Observable.error(new IOException("BAM!"))
            .compose(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertSubscribed()
            .assertError(IOException.class)
            .assertNotComplete();
    }

    @Test
    public void shouldEmitErrorWithRequestNotPermittedException() {
        given(rateLimiter.reservePermission()).willReturn(-1L);

        Observable.just(1)
            .compose(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertSubscribed()
            .assertError(RequestNotPermitted.class)
            .assertNotComplete();
    }
}
