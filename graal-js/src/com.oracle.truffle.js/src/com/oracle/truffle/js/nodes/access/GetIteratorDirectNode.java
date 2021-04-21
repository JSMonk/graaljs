package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;

import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.*;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;

public class GetIteratorDirectNode extends JavaScriptNode {
    @Child protected IsJSObjectNode isObjectNode;
    @Child protected PropertyGetNode getNextMethodNode;

    protected final JSContext context;

    protected GetIteratorDirectNode(JSContext context) {
        this.context = context;
        this.isObjectNode = IsJSObjectNode.create();
        this.getNextMethodNode = PropertyGetNode.create(JSRuntime.NEXT, context);
    }

    public static GetIteratorDirectNode create(JSContext context) {
        return new GetIteratorDirectNode(context);
    }

    protected JSContext getContext() {
        return context;
    }

    @Override
    public IteratorRecord execute(VirtualFrame frame) {
        return this.execute((Object)null);
    }

    public IteratorRecord execute(Object iteratedObject) {
        if (isObjectNode.executeBoolean(iteratedObject)) {
            return IteratorRecord.create((DynamicObject) iteratedObject, getNextMethodNode.getValue(iteratedObject), false);
        }
        throw Errors.createTypeErrorNotAnObject(iteratedObject, this);
    }
}
