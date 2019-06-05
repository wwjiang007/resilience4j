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

import org.reactivestreams.Publisher;

import io.vavr.CheckedFunction0;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * fallbackMethod decorator for {@link Flux} and {@link Mono}
 */
public class ReactorFallbackDecorator implements FallbackDecorator {
    private static final Set<Class<?>> REACTORS_SUPPORTED_TYPES = newHashSet(Mono.class, Flux.class);

    @Override
    public boolean supports(Class<?> target) {
        return REACTORS_SUPPORTED_TYPES.stream().anyMatch(it -> it.isAssignableFrom(target));
    }

    @SuppressWarnings("unchecked")
    @Override
    public CheckedFunction0<Object> decorate(FallbackMethod fallbackMethod, CheckedFunction0<Object> supplier) {
        return supplier.andThen(returnValue -> {
            if (Flux.class.isAssignableFrom(returnValue.getClass())) {
                Flux fluxReturnValue = (Flux) returnValue;
                return fluxReturnValue.onErrorResume(reactorOnErrorResume(fallbackMethod, Flux::error));
            } else if (Mono.class.isAssignableFrom(returnValue.getClass())) {
                Mono monoReturnValue = (Mono) returnValue;
                return monoReturnValue.onErrorResume(reactorOnErrorResume(fallbackMethod, Mono::error));
            } else {
                return returnValue;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T> Function<? super Throwable, ? extends Publisher<? extends T>> reactorOnErrorResume(FallbackMethod recoveryMethod, Function<? super Throwable, ? extends Publisher<? extends T>> errorFunction) {
        return throwable -> {
            try {
                return (Publisher<? extends T>) recoveryMethod.fallback(throwable);
            } catch (Throwable recoverThrowable) {
                return errorFunction.apply(recoverThrowable);
            }
        };
    }
}
