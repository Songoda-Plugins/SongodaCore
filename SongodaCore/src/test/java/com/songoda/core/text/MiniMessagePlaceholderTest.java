package com.songoda.core.text;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class MiniMessagePlaceholderTest {

    @Test
    public void replacesDollarSignsAndBackslashesLiterally() {
        MiniMessagePlaceholder placeholder = new MiniMessagePlaceholder("value", "$100 C:\\kits $1 \\1");
        assertEquals("$100 C:\\kits $1 \\1 / $100 C:\\kits $1 \\1", placeholder.replace("<value> / <value>"));
        assertEquals(List.of("Price: $100 C:\\kits $1 \\1", "unchanged"), placeholder.replace(List.of("Price: <value>", "unchanged")));
    }

    @Test
    public void treatsPlaceholderNamesLiterally() {
        MiniMessagePlaceholder placeholder = new MiniMessagePlaceholder("price.total", 100);
        assertEquals("100 <priceXtotal>", placeholder.replace("<price.total> <priceXtotal>"));
        assertEquals("", new MiniMessagePlaceholder("value", (String) null).replace("<value>"));
    }
}
