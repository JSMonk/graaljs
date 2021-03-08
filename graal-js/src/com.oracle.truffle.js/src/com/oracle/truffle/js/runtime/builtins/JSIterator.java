/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.builtins.IteratorFunctionBuiltins;
import com.oracle.truffle.js.builtins.IteratorPrototypeBuiltins;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public final class JSIterator extends JSNonProxy implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final JSIterator INSTANCE = new JSIterator();

    public static final String CLASS_NAME = "Iterator";
    public static final String PROTOTYPE_NAME = CLASS_NAME + ".prototype";

    private JSIterator() {}

    public static DynamicObject create(JSContext context) {
        JSRealm realm = context.getRealm();
        JSObjectFactory factory = context.getIteratorFactory();
        DynamicObject obj = factory.initProto(new JSIteratorObject(factory.getShape(realm)), realm);
        return context.trackAllocation(obj);
    }

    @Override
    public DynamicObject createPrototype(final JSRealm realm, DynamicObject ctor) {
        JSContext ctx = realm.getContext();
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putDataProperty(ctx, prototype, Symbol.SYMBOL_ITERATOR, createIteratorPrototypeSymbolIteratorFunction(realm), JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, IteratorPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, CLASS_NAME);
        return prototype;
    }

    private static DynamicObject createIteratorPrototypeSymbolIteratorFunction(JSRealm realm) {
        return JSFunction.create(realm, JSFunctionData.createCallOnly(realm.getContext(), realm.getContext().getSpeciesGetterFunctionCallTarget(), 0, "[Symbol.iterator]"));
    }


    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSIterator.INSTANCE, context);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, IteratorFunctionBuiltins.BUILTINS);
    }

    public static boolean isJSIterator(Object obj) {
        // TODO: Create Iterator Object
        return true;
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    public String getClassName(DynamicObject object) {
        return getClassName();
    }

    @Override
    @TruffleBoundary
    public String toDisplayStringImpl(DynamicObject obj, int depth, boolean allowSideEffects, JSContext context) {
        if (context.isOptionNashornCompatibilityMode()) {
            return "[" + getClassName() + "]";
        } else {
            return getClassName();
        }
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getIteratorPrototype();
    }
}
