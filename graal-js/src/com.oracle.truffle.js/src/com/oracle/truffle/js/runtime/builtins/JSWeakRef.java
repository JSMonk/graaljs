/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.WeakReference;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.builtins.WeakRefPrototypeBuiltins;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.JSBasicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public final class JSWeakRef extends JSBuiltinObject implements JSConstructorFactory.Default, PrototypeSupplier {

    public static final JSWeakRef INSTANCE = new JSWeakRef();

    public static final String CLASS_NAME = "WeakRef";
    public static final String PROTOTYPE_NAME = "WeakRef.prototype";

    public static class WeakRefImpl extends JSBasicObject {
        private final TruffleWeakReference<Object> weakReference;

        protected WeakRefImpl(JSRealm realm, JSObjectFactory factory, TruffleWeakReference<Object> weakReference) {
            super(realm, factory);
            this.weakReference = weakReference;
        }

        protected WeakRefImpl(Shape shape, TruffleWeakReference<Object> weakReference) {
            super(shape);
            this.weakReference = weakReference;
        }

        public TruffleWeakReference<Object> getWeakReference() {
            return weakReference;
        }

        public static WeakRefImpl create(JSRealm realm, JSObjectFactory factory, TruffleWeakReference<Object> weakReference) {
            return new WeakRefImpl(realm, factory, weakReference);
        }
    }

    private JSWeakRef() {
    }

    public static DynamicObject create(JSContext context, Object referent) {
        TruffleWeakReference<Object> weakReference = new TruffleWeakReference<>(referent);
        DynamicObject obj = WeakRefImpl.create(context.getRealm(), context.getWeakRefFactory(), weakReference);
        assert isJSWeakRef(obj);
        // Used for KeepDuringJob(target) in the specification
        context.addWeakRefTargetToSet(referent);
        return context.trackAllocation(obj);
    }

    public static TruffleWeakReference<?> getInternalWeakRef(DynamicObject obj) {
        assert isJSWeakRef(obj);
        return ((WeakRefImpl) obj).getWeakReference();
    }

    @Override
    public DynamicObject createPrototype(final JSRealm realm, DynamicObject ctor) {
        JSContext ctx = realm.getContext();
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(ctx, prototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, WeakRefPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, CLASS_NAME);
        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSWeakRef.INSTANCE, context);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
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
    public String toDisplayStringImpl(DynamicObject obj, int depth, boolean allowSideEffects, JSContext context) {
        return "[" + getClassName() + "]";
    }

    public static boolean isJSWeakRef(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSWeakRef((DynamicObject) obj);
    }

    public static boolean isJSWeakRef(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getWeakRefPrototype();
    }

    public static final class TruffleWeakReference<T> extends WeakReference<T> {

        public TruffleWeakReference(T t) {
            super(t);
        }

    }
}
