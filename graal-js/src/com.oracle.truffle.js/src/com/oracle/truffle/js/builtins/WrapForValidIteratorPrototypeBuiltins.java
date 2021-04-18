package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.access.*;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSIterator;
import com.oracle.truffle.js.runtime.builtins.JSWrapForValidIterator;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

import com.oracle.truffle.js.builtins.WrapForValidIteratorPrototypeBuiltinsFactory.JSWrapForValidIteratorNextNodeGen;
import com.oracle.truffle.js.builtins.WrapForValidIteratorPrototypeBuiltinsFactory.JSWrapForValidIteratorThrowNodeGen;
import com.oracle.truffle.js.builtins.WrapForValidIteratorPrototypeBuiltinsFactory.JSWrapForValidIteratorReturnNodeGen;

public final class WrapForValidIteratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<WrapForValidIteratorPrototypeBuiltins.WrapForValidIteratorPrototype> {
    public static final JSBuiltinsContainer BUILTINS = new WrapForValidIteratorPrototypeBuiltins();

    private static void requireIteratedInternalSlot(DynamicObject obj, Node node) {
        if (!JSObjectUtil.hasHiddenProperty(obj, JSIterator.ITERATED)) {
            throw Errors.createTypeErrorObjectIsNotIterated(obj, node);
        }
    }

    private static IteratorRecord getIteratedInternalSlot(DynamicObject obj) {
        return (IteratorRecord) JSObjectUtil.getHiddenProperty(obj, JSIterator.ITERATED);
    }

    protected WrapForValidIteratorPrototypeBuiltins() {
        super(JSWrapForValidIterator.PROTOTYPE_NAME, WrapForValidIteratorPrototype.class);
    }

    public enum WrapForValidIteratorPrototype implements BuiltinEnum<WrapForValidIteratorPrototype> {
        next(1),
        return_(1),
        throw_(1);

        private final int length;

        WrapForValidIteratorPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, WrapForValidIteratorPrototype builtinEnum) {
        switch (builtinEnum) {
            case next:
                return JSWrapForValidIteratorNextNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
            case return_:
                return JSWrapForValidIteratorReturnNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case throw_:
                return JSWrapForValidIteratorThrowNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSWrapForValidIteratorNextNode extends JSBuiltinNode {
        @Child private IteratorNextNode iteratorNextNode;

        private final ConditionProfile isValuePresent = ConditionProfile.createBinaryProfile();

        public JSWrapForValidIteratorNextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.iteratorNextNode = IteratorNextNode.create();
        }

        @Specialization
        protected DynamicObject next(DynamicObject thisObj, Object... values) {
            WrapForValidIteratorPrototypeBuiltins.requireIteratedInternalSlot(thisObj, this);

            IteratorRecord iterated = WrapForValidIteratorPrototypeBuiltins.getIteratedInternalSlot(thisObj);
            Object value = JSRuntime.getArgOrUndefined(values, 0);
            if (isValuePresent.profile(!JSGuards.isUndefined(value))) {
                return iteratorNextNode.execute(iterated, value);
            } else {
                return iteratorNextNode.execute(iterated);
            }
        }
    }

    public abstract static class JSWrapForValidIteratorReturnNode extends JSBuiltinNode {
        @Child private IteratorCloseNode iteratorCloseNode;
        @Child private CreateIterResultObjectNode createIterResultObjectNode;

        public JSWrapForValidIteratorReturnNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.iteratorCloseNode = IteratorCloseNode.create(context);
            this.createIterResultObjectNode = CreateIterResultObjectNode.create(context);
        }

        @Specialization
        protected DynamicObject return_(VirtualFrame frame, DynamicObject thisObj, Object value) {
            WrapForValidIteratorPrototypeBuiltins.requireIteratedInternalSlot(thisObj, this);

            IteratorRecord iterated = WrapForValidIteratorPrototypeBuiltins.getIteratedInternalSlot(thisObj);
            Object result = iteratorCloseNode.execute(iterated.getIterator(), value);

            return createIterResultObjectNode.execute(frame, result,true);
        }
    }

    public abstract static class JSWrapForValidIteratorThrowNode extends JSBuiltinNode {
        @Child private GetMethodNode getIteratorThrowMethodNode;
        @Child private JSFunctionCallNode callThrowMethodNode;

        private final ConditionProfile isThrowMethodPresent = ConditionProfile.createBinaryProfile();

        public JSWrapForValidIteratorThrowNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getIteratorThrowMethodNode = GetMethodNode.create(context, null, "throw");
            this.callThrowMethodNode = JSFunctionCallNode.createCall();
        }

        @Specialization
        protected DynamicObject throw_(DynamicObject thisObj, DynamicObject value) {
            WrapForValidIteratorPrototypeBuiltins.requireIteratedInternalSlot(thisObj, this);

            IteratorRecord iterated = WrapForValidIteratorPrototypeBuiltins.getIteratedInternalSlot(thisObj);
            DynamicObject iterator = iterated.getIterator();

            Object throwMethod = getIteratorThrowMethodNode.executeWithTarget(iterator);

            if (this.isThrowMethodPresent.profile(!JSGuards.isUndefined(throwMethod))) {
                return (DynamicObject) callThrowMethodNode.executeCall(JSArguments.createOneArg(iterator, throwMethod, value));
            }

            return value;
        }
    }
}
