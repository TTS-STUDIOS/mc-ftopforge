package com.ttsstudio.ftopforge.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MoneyFormatTest {
    @Test
    void zeroIsZero() { assertEquals("0", MoneyFormat.shortFmt(0)); }
    @Test
    void belowThousandIsLiteral() { assertEquals("999", MoneyFormat.shortFmt(999)); }
    @Test
    void thousandsUseK() { assertEquals("1.5K", MoneyFormat.shortFmt(1500)); }
    @Test
    void millionsUseM() { assertEquals("2.3M", MoneyFormat.shortFmt(2_300_000)); }
    @Test
    void billionsUseB() { assertEquals("4.5B", MoneyFormat.shortFmt(4_500_000_000L)); }
    @Test
    void trillionsUseT() { assertEquals("1.2T", MoneyFormat.shortFmt(1_200_000_000_000L)); }
    @Test
    void roundsToOneDecimal() { assertEquals("1.0K", MoneyFormat.shortFmt(1049)); assertEquals("1.1K", MoneyFormat.shortFmt(1050)); }
    @Test
    void negativeIsPrefixed() { assertEquals("-1.5K", MoneyFormat.shortFmt(-1500)); }
}
