/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public final class JSIntl {

    public static final String CLASS_NAME = "Intl";

    private JSIntl() {
    }

    public static DynamicObject create(JSRealm realm) {
        JSContext ctx = realm.getContext();
        DynamicObject obj = JSObject.create(realm, realm.getObjectPrototype(), JSUserObject.INSTANCE);
        JSObjectUtil.putDataProperty(ctx, obj, Symbol.SYMBOL_TO_STRING_TAG, CLASS_NAME, JSAttributes.configurableNotEnumerableNotWritable());
        JSObjectUtil.putFunctionsFromContainer(realm, obj, CLASS_NAME);
        return obj;
    }
}
