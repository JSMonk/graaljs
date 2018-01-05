/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

import java.io.File;
import java.io.IOException;

import org.graalvm.polyglot.Context;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;

public abstract class AbstractJavaScriptLanguage extends TruffleLanguage<JSContext> {
    public static final String TEXT_MIME_TYPE = "text/javascript";
    public static final String APPLICATION_MIME_TYPE = "application/javascript";
    public static final String MODULE_SOURCE_NAME_PREFIX = "module:";

    public static final String VERSION_NUMBER = "0.9";
    public static final String NAME = "JavaScript";
    public static final String ID = "js";

    protected static final ThreadLocal<JSContext> contextHolder = new ThreadLocal<>();
    protected static final String GET_JSCONTEXT_NAME = "<get graal-js context>";

    public abstract JSContext findContext();

    public static Source sourceFromFileName(String fileName) throws IOException {
        return Source.newBuilder(new File(fileName)).name(fileName).mimeType(APPLICATION_MIME_TYPE).build();
    }

    public static org.graalvm.polyglot.Source newSourceFromFileName(String fileName) throws IOException {
        return org.graalvm.polyglot.Source.newBuilder(ID, new File(fileName)).build();
    }

    public static JSContext getJSContext(PolyglotEngine engine) {
        try {
            Source source = Source.newBuilder("this").name(GET_JSCONTEXT_NAME).mimeType(APPLICATION_MIME_TYPE).build();
            engine.eval(source);
            JSContext jsContext = contextHolder.get();
            contextHolder.remove();
            return jsContext;
        } catch (Exception e) {
            // This should never happen
            throw new RuntimeException(e);
        }
    }

    public static JSContext getJSContext(Context context) {
        org.graalvm.polyglot.Source source = org.graalvm.polyglot.Source.newBuilder("js", "this", GET_JSCONTEXT_NAME).buildLiteral();
        context.eval(source);
        JSContext jsContext = contextHolder.get();
        contextHolder.remove();
        return jsContext;
    }
}
