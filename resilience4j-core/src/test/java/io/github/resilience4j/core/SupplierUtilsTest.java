package io.github.resilience4j.core;

import org.junit.Test;

import javax.xml.ws.WebServiceException;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class SupplierUtilsTest {

    @Test
    public void shouldChainSupplierAndResultHandler() {

        Supplier<String> supplier = () -> "BLA";
        //When
        Supplier<String> supplierWithRecovery = SupplierUtils.andThen(supplier, result -> "Bla");

        String result = supplierWithRecovery.get();

        //Then
        assertThat(result).isEqualTo("Bla");
    }

    @Test
    public void shouldChainSupplierAndRecoverWithHandler() {

        Supplier<String> supplier = () -> {
            throw new RuntimeException("BAM!");
        };
        //When
        Supplier<String> supplierWithRecovery = SupplierUtils.andThen(supplier, (result, ex) -> "Bla");

        String result = supplierWithRecovery.get();

        //Then
        assertThat(result).isEqualTo("Bla");
    }

    @Test
    public void shouldChainSupplierAndRecoverWithErrorHandler() {

        Supplier<String> supplier = () -> {
            throw new RuntimeException("BAM!");
        };
        //When
        Supplier<String> supplierWithRecovery = SupplierUtils.andThen(supplier, (result) -> result, ex -> "Bla");

        String result = supplierWithRecovery.get();

        //Then
        assertThat(result).isEqualTo("Bla");
    }


    @Test
    public void shouldRecoverSupplierFromException() {

        Supplier<String> supplier = () -> {
            throw new RuntimeException("BAM!");
        };
        //When
        Supplier<String> supplierWithRecovery = SupplierUtils.recover(supplier, (ex) -> "Bla");

        String result = supplierWithRecovery.get();

        //Then
        assertThat(result).isEqualTo("Bla");
    }

    @Test(expected = WebServiceException.class)
    public void shouldRethrowException() {

        Supplier<String> supplier = () -> {
            throw new RuntimeException("BAM!");
        };
        //When
        Supplier<String> supplierWithRecovery = SupplierUtils.recover(supplier, (ex) -> {
            throw new WebServiceException();
        });

        supplierWithRecovery.get();
    }
}
