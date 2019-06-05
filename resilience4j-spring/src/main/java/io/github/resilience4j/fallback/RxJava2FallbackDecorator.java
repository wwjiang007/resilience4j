/*
 * Copyright 2019 Kyuhyen Hwang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.fallback;

import static io.github.resilience4j.utils.AspectUtil.newHashSet;

import java.util.Set;
import java.util.function.Function;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.vavr.CheckedFunction0;

/**
 * fallbackMethod decorator for {@link ObservableSource}, {@link SingleSource}, {@link CompletableSource}, {@link MaybeSource} and {@link Flowable}.
 */
public class RxJava2FallbackDecorator implements FallbackDecorator {
    private static final Set<Class<?>> RX_SUPPORTED_TYPES = newHashSet(ObservableSource.class, SingleSource.class, CompletableSource.class, MaybeSource.class, Flowable.class);

    @Override
    public boolean supports(Class<?> target) {
        return RX_SUPPORTED_TYPES.stream().anyMatch(it -> it.isAssignableFrom(target));
    }

    @Override
    public CheckedFunction0<Object> decorate(FallbackMethod fallbackMethod, CheckedFunction0<Object> supplier) {
        return supplier.andThen(request -> {
            if (request instanceof ObservableSource) {
                Observable<?> observable = (Observable<?>) request;
                return observable.onErrorResumeNext(rxJava2OnErrorResumeNext(fallbackMethod, Observable::error));
            } else if (request instanceof SingleSource) {
                Single<?> single = (Single) request;
                return single.onErrorResumeNext(rxJava2OnErrorResumeNext(fallbackMethod, Single::error));
            } else if (request instanceof CompletableSource) {
                Completable completable = (Completable) request;
                return completable.onErrorResumeNext(rxJava2OnErrorResumeNext(fallbackMethod, Completable::error));
            } else if (request instanceof MaybeSource) {
                Maybe<?> maybe = (Maybe) request;
                return maybe.onErrorResumeNext(rxJava2OnErrorResumeNext(fallbackMethod, Maybe::error));
            } else if (request instanceof Flowable) {
                Flowable<?> flowable = (Flowable) request;
                return flowable.onErrorResumeNext(rxJava2OnErrorResumeNext(fallbackMethod, Flowable::error));
            } else {
                return request;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T> io.reactivex.functions.Function<Throwable, T> rxJava2OnErrorResumeNext(FallbackMethod recoveryMethod, Function<? super Throwable, ? extends T> errorFunction) {
        return throwable -> {
            try {
                return (T) recoveryMethod.fallback(throwable);
            } catch (Throwable recoverThrowable) {
                return (T) errorFunction.apply(recoverThrowable);
            }
        };
    }
}
