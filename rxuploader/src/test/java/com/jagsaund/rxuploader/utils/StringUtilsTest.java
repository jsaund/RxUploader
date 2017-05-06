package com.jagsaund.rxuploader.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@RunWith(JUnit4.class)
public class StringUtilsTest {
    @Test
    public void testIsNull() throws Exception {
        assertThat(StringUtils.isNullOrEmpty(null), is(true));
    }

    @Test
    public void testIsNotNull() throws Exception {
        assertThat(StringUtils.isNullOrEmpty("not null"), is(false));
    }

    @Test
    public void testIsEmpty() throws Exception {
        assertThat(StringUtils.isNullOrEmpty(""), is(true));
    }

    @Test
    public void testIsNotEmpty() throws Exception {
        assertThat(StringUtils.isNullOrEmpty("not empty"), is(false));
    }

    @Test
    public void testGetNonEmptyString() throws Exception {
        assertThat(StringUtils.getOrDefault("not empty", "invalid"), is(equalTo("not empty")));
    }

    @Test
    public void testGetEmptyString() throws Exception {
        assertThat(StringUtils.getOrDefault("", "invalid"), is(equalTo("")));
    }

    @Test
    public void testGetDefaultString() throws Exception {
        assertThat(StringUtils.getOrDefault(null, "default"), is(equalTo("default")));
    }

    @Test
    public void testGetEmptyStringForNull() throws Exception {
        assertThat(StringUtils.getOrEmpty(null), is(equalTo("")));
    }

    @Test
    public void testGetEmptyStringForNonNull() throws Exception {
        assertThat(StringUtils.getOrEmpty("not empty"), is(equalTo("not empty")));
    }
}