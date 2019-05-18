package io.github.resilience4j.bulkhead.operator;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Unit test for {@link MaybeBulkhead} using {@link BulkheadOperator}.
 */
public class MaybeBulkheadTest {

    private Bulkhead bulkhead;

    @Before
    public void setUp(){
        bulkhead = Mockito.mock(Bulkhead.class);
    }

    @Test
    public void shouldEmitAllEvents() {
        given(bulkhead.tryAcquirePermission()).willReturn(true);

        Maybe.just(1)
            .compose(BulkheadOperator.of(bulkhead))
            .test()
            .assertResult(1);

        verify(bulkhead, times(1)).onComplete();
    }

    @Test
    public void shouldPropagateError() {
        given(bulkhead.tryAcquirePermission()).willReturn(true);

        Maybe.error(new IOException("BAM!"))
            .compose(BulkheadOperator.of(bulkhead))
            .test()
            .assertSubscribed()
            .assertError(IOException.class)
            .assertNotComplete();

        verify(bulkhead, times(1)).onComplete();
    }

    @Test
    public void shouldEmitErrorWithBulkheadFullException() {
        given(bulkhead.tryAcquirePermission()).willReturn(false);

        Maybe.just(1)
            .compose(BulkheadOperator.of(bulkhead))
            .test()
            .assertSubscribed()
            .assertError(BulkheadFullException.class)
            .assertNotComplete();

        verify(bulkhead, never()).onComplete();
    }

    @Test
    public void shouldReleaseBulkheadOnlyOnce() {
        given(bulkhead.tryAcquirePermission()).willReturn(true);

        Maybe.just(Arrays.asList(1, 2, 3))
            .compose(BulkheadOperator.of(bulkhead))
            .flatMapObservable(Observable::fromIterable)
            .take(2) //this with the previous line triggers an extra dispose
            .test()
            .assertResult(1, 2);

        verify(bulkhead, times(1)).onComplete();
    }
}
