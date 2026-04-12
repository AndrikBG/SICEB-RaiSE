package com.siceb.shared;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void creationEnforcesScale() {
        Money m = Money.of(new BigDecimal("100.123456789"));
        assertEquals(new BigDecimal("100.1235"), m.amount());
    }

    @Test
    void bankersRoundingRoundsHalfToEven() {
        // 0.12345 → round at 4th decimal: 5th digit is 5, 4th digit is 4 (even) → rounds down
        assertEquals(new BigDecimal("0.1234"), Money.of(new BigDecimal("0.12345")).amount());
        // 0.12355 → round at 4th decimal: 5th digit is 5, 4th digit is 5 (odd) → rounds up
        assertEquals(new BigDecimal("0.1236"), Money.of(new BigDecimal("0.12355")).amount());
    }

    @Test
    void noFloatingPointErrors() {
        Money a = Money.of(new BigDecimal("0.1"));
        Money b = Money.of(new BigDecimal("0.2"));
        Money sum = a.add(b);
        assertEquals(Money.of(new BigDecimal("0.3")), sum);
    }

    @Test
    void addition() {
        Money a = Money.of(new BigDecimal("100.50"));
        Money b = Money.of(new BigDecimal("50.25"));
        assertEquals(new BigDecimal("150.7500"), a.add(b).amount());
    }

    @Test
    void subtraction() {
        Money a = Money.of(new BigDecimal("100.50"));
        Money b = Money.of(new BigDecimal("50.25"));
        assertEquals(new BigDecimal("50.2500"), a.subtract(b).amount());
    }

    @Test
    void multiplication() {
        Money price = Money.of(new BigDecimal("100"));
        BigDecimal taxRate = new BigDecimal("0.16");
        Money tax = price.multiply(taxRate);
        assertEquals(new BigDecimal("16.0000"), tax.amount());
    }

    @Test
    void multiplicationWithIvaPrecision() {
        Money price = Money.of(new BigDecimal("333.33"));
        BigDecimal ivaRate = new BigDecimal("0.16");
        Money iva = price.multiply(ivaRate);
        assertEquals(new BigDecimal("53.3328"), iva.amount());
    }

    @Test
    void zero() {
        Money zero = Money.zero();
        assertTrue(zero.isZero());
        assertFalse(zero.isPositive());
        assertFalse(zero.isNegative());
    }

    @Test
    void negation() {
        Money positive = Money.of(new BigDecimal("100"));
        Money negative = positive.negate();
        assertTrue(negative.isNegative());
        assertEquals(Money.zero(), positive.add(negative));
    }

    @Test
    void comparison() {
        Money a = Money.of(new BigDecimal("100"));
        Money b = Money.of(new BigDecimal("200"));
        assertTrue(a.isLessThan(b));
        assertTrue(b.isGreaterThan(a));
    }

    @Test
    void equalityByValue() {
        Money a = Money.of(new BigDecimal("100.00"));
        Money b = Money.of(new BigDecimal("100.0000"));
        assertEquals(a, b);
    }

    @Test
    void nullRejected() {
        assertThrows(NullPointerException.class, () -> Money.of((BigDecimal) null));
    }

    @Test
    void toStringIncludesCurrency() {
        Money m = Money.of(new BigDecimal("1234.56"));
        assertEquals("1234.5600 MXN", m.toString());
    }
}
