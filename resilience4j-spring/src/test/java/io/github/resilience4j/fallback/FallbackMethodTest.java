/*
 * Copyright 2019 Kyuhyen Hwang , Mahmoud Romih
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

import java.lang.reflect.Method;

import org.junit.Test;

@SuppressWarnings({"WeakerAccess", "unused"})
public class FallbackMethodTest {
    @Test
    public void recoverRuntimeExceptionTest() throws Throwable {
        FallbackMethodTest target = new FallbackMethodTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);
        FallbackMethod recoveryMethod = FallbackMethod.create("fallbackMethod", testMethod, new Object[]{"test"}, target);
        assertThat(recoveryMethod.fallback(new RuntimeException("err"))).isEqualTo("recovered-RuntimeException");
    }

    @Test
    public void recoverGlobalExceptionWithSameMethodReturnType() throws Throwable {
        FallbackMethodTest target = new FallbackMethodTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);
        FallbackMethod recoveryMethod = FallbackMethod.create("fallbackMethod", testMethod, new Object[]{"test"}, target);
        assertThat(recoveryMethod.fallback(new IllegalStateException("err"))).isEqualTo("recovered-IllegalStateException");
    }

    @Test
    public void recoverClosestSuperclassExceptionTest() throws Throwable {
        FallbackMethodTest target = new FallbackMethodTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);
        FallbackMethod recoveryMethod = FallbackMethod.create("fallbackMethod", testMethod, new Object[]{"test"}, target);
        assertThat(recoveryMethod.fallback(new NumberFormatException("err"))).isEqualTo("recovered-IllegalArgumentException");
    }

    @Test
    public void shouldThrowUnrecoverableThrowable() throws Throwable {
        FallbackMethodTest target = new FallbackMethodTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);
        FallbackMethod recoveryMethod = FallbackMethod.create("fallbackMethod", testMethod, new Object[]{"test"}, target);
        Throwable unrecoverableThrown = new Throwable("err");
        assertThatThrownBy(() -> recoveryMethod.fallback(unrecoverableThrown)).isEqualTo(unrecoverableThrown);
    }

    @Test
    public void shouldCallPrivateRecoveryMethod() throws Throwable {
        FallbackMethodTest target = new FallbackMethodTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);
        FallbackMethod recoveryMethod = FallbackMethod.create("privateRecovery", testMethod, new Object[]{"test"}, target);
        assertThat(recoveryMethod.fallback(new RuntimeException("err"))).isEqualTo("recovered-privateMethod");
    }

    @Test
    public void mismatchReturnType_shouldThrowNoSuchMethodException() throws Throwable {
        FallbackMethodTest target = new FallbackMethodTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);

        assertThatThrownBy(() -> FallbackMethod.create("duplicateException", testMethod, new Object[]{"test"}, target))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You have more that one fallback method that cover the same exception type java.lang.IllegalArgumentException");
    }

    @Test
    public void shouldFailIf2FallBackMethodsHandleSameException() throws Throwable {
        FallbackMethodTest target = new FallbackMethodTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);
        assertThatThrownBy(() -> FallbackMethod.create("returnMismatchRecovery", testMethod, new Object[]{"test"}, target))
                .isInstanceOf(NoSuchMethodException.class)
                .hasMessage("class java.lang.String class io.github.resilience4j.fallback.FallbackMethodTest.returnMismatchRecovery(class java.lang.String,class java.lang.Throwable)");
    }

    @Test
    public void notFoundRecoveryMethod_shouldThrowsNoSuchMethodException() throws Throwable {
        FallbackMethodTest target = new FallbackMethodTest();
        Method testMethod = target.getClass().getMethod("testMethod", String.class);
        assertThatThrownBy(() -> FallbackMethod.create("noMethod", testMethod, new Object[]{"test"}, target))
                .isInstanceOf(NoSuchMethodException.class)
                .hasMessage("class java.lang.String class io.github.resilience4j.fallback.FallbackMethodTest.noMethod(class java.lang.String,class java.lang.Throwable)");
    }

    public String testMethod(String parameter) {
        return null;
    }

    public String fallbackMethod(String parameter, RuntimeException exception) {
        return "recovered-RuntimeException";
    }

    public String fallbackMethod(IllegalStateException exception) {
        return "recovered-IllegalStateException";
    }

    public String fallbackMethod(String parameter, IllegalArgumentException exception) {
        return "recovered-IllegalArgumentException";
    }

    public Object returnMismatchRecovery(String parameter, RuntimeException exception) {
        return "recovered";
    }

    private String privateRecovery(String parameter, RuntimeException exception) {
        return "recovered-privateMethod";
    }

    public String duplicateException(String parameter, IllegalArgumentException exception) {
        return "recovered-IllegalArgumentException";
    }

    public String duplicateException(IllegalArgumentException exception) {
        return "recovered-IllegalArgumentException";
    }
}