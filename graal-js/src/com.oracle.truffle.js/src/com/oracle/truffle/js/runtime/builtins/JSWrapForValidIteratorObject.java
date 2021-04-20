package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.objects.JSNonProxyObject;

public final class JSWrapForValidIteratorObject extends JSNonProxyObject {
    protected JSWrapForValidIteratorObject(Shape shape) {
        super(shape);
    }
}
