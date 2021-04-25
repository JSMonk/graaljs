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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.access.GetIteratorDirectNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.control.YieldNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.*;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSIterator;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.Pair;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;
import com.oracle.truffle.js.runtime.util.UnmodifiableArrayList;
import com.oracle.truffle.js.builtins.IteratorPrototypeBuiltinsFactory.JSIteratorMapNodeGen;
import com.oracle.truffle.js.builtins.IteratorPrototypeBuiltinsFactory.JSIteratorReduceNodeGen;
import com.oracle.truffle.js.builtins.IteratorPrototypeBuiltinsFactory.JSIteratorToArrayNodeGen;
import com.oracle.truffle.js.builtins.IteratorPrototypeBuiltinsFactory.JSIteratorForEachNodeGen;
import com.oracle.truffle.js.builtins.IteratorPrototypeBuiltinsFactory.JSIteratorSomeNodeGen;
import com.oracle.truffle.js.builtins.IteratorPrototypeBuiltinsFactory.JSIteratorEveryNodeGen;
import com.oracle.truffle.js.builtins.IteratorPrototypeBuiltinsFactory.JSIteratorFindNodeGen;

import java.util.function.Consumer;

/**
 * Contains builtins for {@linkplain JSIterator}.prototype.
 */
public final class IteratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<IteratorPrototypeBuiltins.IteratorPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new IteratorPrototypeBuiltins();

    protected IteratorPrototypeBuiltins() {
        super(JSIterator.PROTOTYPE_NAME, IteratorPrototype.class);
    }

    public enum IteratorPrototype implements BuiltinEnum<IteratorPrototype> {
        map(1, true),
        /**Final operations*/
        reduce(2),
        toArray(0),
        forEach(1),
        some(1),
        every(1),
        find(1);

        private final int length;
        private final boolean generator;

        IteratorPrototype(int length) {
            this(length, false);
        }
        IteratorPrototype(int length, boolean generator) {
            this.length = length;
            this.generator = generator;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isGenerator() {
            return generator;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, IteratorPrototype builtinEnum) {
        switch (builtinEnum) {
            case map:
                return JSIteratorMapNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case reduce:
                return JSIteratorReduceNodeGen.create(context, builtin, args().withThis().fixedArgs(1).varArgs().createArgumentNodes(context));
            case toArray:
                return JSIteratorToArrayNodeGen.create(context, builtin, args().withThis().fixedArgs(0).createArgumentNodes(context));
            case forEach:
                return JSIteratorForEachNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case some:
                return JSIteratorSomeNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case every:
                return JSIteratorEveryNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case find:
                return JSIteratorFindNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    /** Dummy value to associate with a key in the backing map. */
    protected static final Object PRESENT = new Object();

    protected static RuntimeException typeErrorKeyIsNotObject() {
        throw Errors.createTypeError("Iterator key must be an object");
    }

    protected static RuntimeException typeErrorIteratorExpected() {
        throw Errors.createTypeError("Iterator expected");
    }
    /**
     Abstract final operation implementation
     */
    public abstract static class JSAbstractIteratorFinalOperationNode extends JSBuiltinNode {
        @Child protected GetIteratorDirectNode getIteratorDirectNode;
        @Child protected IteratorStepNode iteratorStepNode;
        @Child protected IteratorValueNode getIteratorValueNode;

        protected JSAbstractIteratorFinalOperationNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected void iterateOver(Object thisObj, Consumer<Object> callback) {
            IteratorRecord iterated = getIteratorDirect(thisObj);
            while (true) {
                Object next = iteratorStep(iterated);
                if (next == Boolean.FALSE) {
                    break;
                }
                Object nextValue = getIteratorValue((DynamicObject) next);
                callback.accept(nextValue);
            }
        }

        protected IteratorRecord getIteratorDirect(Object obj) {
            if (getIteratorDirectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getIteratorDirectNode = insert(GetIteratorDirectNode.create(getContext()));
            }
            return getIteratorDirectNode.execute(obj);
        }

        protected Object iteratorStep(IteratorRecord iteratorRecord) {
            if (iteratorStepNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                iteratorStepNode = insert(IteratorStepNode.create(getContext()));
            }
            return iteratorStepNode.execute(iteratorRecord);
        }

        protected Object getIteratorValue(DynamicObject iteratorResult) {
            if (getIteratorValueNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getIteratorValueNode = insert(IteratorValueNode.create(getContext()));
            }
            return getIteratorValueNode.execute(iteratorResult);
        }
    }

    /**
     Abstract final operation with call implementation
     */
    public abstract static class JSAbstractIteratorWithCallNode extends JSAbstractIteratorFinalOperationNode {
        @Child private IsCallableNode isCallableNode;
        @Child private JSFunctionCallNode callReducerFnNode;
        @Child private IteratorCloseNode iteratorCloseNode;

        protected final BranchProfile errorBranch = BranchProfile.create();

        protected JSAbstractIteratorWithCallNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected final Object checkCallbackIsFunction(Object callback) {
            if (!isCallable(callback)) {
                errorBranch.enter();
                throw Errors.createTypeErrorNotAFunction(callback, this);
            }
            return callback;
        }

        protected final Object callFunction(DynamicObject function, Object... userArguments) {
            if (callReducerFnNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callReducerFnNode = insert(JSFunctionCallNode.createCall());
            }
            return callReducerFnNode.executeCall(JSArguments.create(Undefined.instance, function, userArguments));
        }

        protected final boolean isCallable(Object callback) {
            if (isCallableNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isCallableNode = insert(IsCallableNode.create());
            }
            return isCallableNode.executeBoolean(callback);
        }

        protected Object iteratorCloseAbrupt(DynamicObject iterator, Object value) {
            if (iteratorCloseNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                iteratorCloseNode = insert(IteratorCloseNode.create(getContext()));
            }
            return iteratorCloseNode.executeAbrupt(iterator, value);
        }
    }
    /**
     * Implementation of the Iterator.prototype.reduce(fn).
     */
    public abstract static class JSIteratorReduceNode extends JSAbstractIteratorWithCallNode {
        private final BranchProfile findInitialValueBranch = BranchProfile.create();

        public JSIteratorReduceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization()
        protected Object reduce(Object thisObj, Object callback, Object[] initialValueOpt) {
            IteratorRecord iterated = getIteratorDirect(thisObj);

            callback = checkCallbackIsFunction(callback);
            Object accumulator = JSRuntime.getArg(initialValueOpt, 0, null);

            if (accumulator == null) {
                findInitialValueBranch.enter();
                Object next = iteratorStep(iterated);
                if (next == Boolean.FALSE) {
                    errorBranch.enter();
                    throw reduceNoInitialValueError();
                }
                accumulator = getIteratorValue((DynamicObject) next);
            }

            while (true) {
                Object next = iteratorStep(iterated);
                if (next == Boolean.FALSE) {
                    break;
                }
                Object nextValue = getIteratorValue((DynamicObject) next);
                accumulator = callFunction((DynamicObject) callback, accumulator, nextValue);
            }

            return iteratorCloseAbrupt((DynamicObject) thisObj, accumulator);
        }

        @CompilerDirectives.TruffleBoundary
        protected static RuntimeException reduceNoInitialValueError() {
            throw Errors.createTypeError("Reduce of empty Iterator with no initial value");
        }
    }

    /**
     * Implementation of the Iterator.prototype.toArray().
     */
    public abstract static class JSIteratorToArrayNode extends JSAbstractIteratorFinalOperationNode {
        private final BranchProfile growProfile = BranchProfile.create();

        public JSIteratorToArrayNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization()
        protected DynamicObject toArray(Object thisObj) {
            SimpleArrayList<Object> items = new SimpleArrayList<>();
            iterateOver(thisObj, (Object nextValue) -> {
                items.add(nextValue, growProfile);
            });
            return JSRuntime.createArrayFromList(getContext(), new UnmodifiableArrayList<>(items.toArray()));
        }
    }

    /**
     * Implementation of the Iterator.prototype.forEach(fn).
     */
    public abstract static class JSIteratorForEachNode extends JSAbstractIteratorWithCallNode {
        public JSIteratorForEachNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization()
        protected DynamicObject forEach(Object thisObj, Object callback) {
            Object fn = checkCallbackIsFunction(callback);

            iterateOver(thisObj, (Object nextValue) -> {
                Object result = callFunction((DynamicObject) fn, nextValue);
                iteratorCloseAbrupt( (DynamicObject) thisObj, result);
            });

            return Undefined.instance;
        }
    }

    /**
     * Implementation of the Iterator.prototype.some(fn).
     */
    public abstract static class JSIteratorSomeNode extends JSAbstractIteratorWithCallNode {
        @Child private JSToBooleanNode toBooleanNode;

        public JSIteratorSomeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization()
        protected Object some(Object thisObj, Object callback) {
            IteratorRecord iterated = getIteratorDirect(thisObj);
            callback = checkCallbackIsFunction(callback);

            while (true) {
                Object next = iteratorStep(iterated);
                if (next == Boolean.FALSE) {
                    break;
                }
                Object nextValue = getIteratorValue((DynamicObject) next);
                Object result = callFunction((DynamicObject) callback, nextValue);
                iteratorCloseAbrupt((DynamicObject) thisObj, result);
                if (toBoolean(result) == Boolean.TRUE) {
                   return Boolean.TRUE;
                }
            }

            return Boolean.FALSE;
        }

        protected Object toBoolean(Object value) {
            if (toBooleanNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toBooleanNode = insert(JSToBooleanNode.create());
            }
            return toBooleanNode.executeBoolean(value);
        }
    }

    /**
     * Implementation of the Iterator.prototype.every(fn).
     */
    public abstract static class JSIteratorEveryNode extends JSAbstractIteratorWithCallNode {
        @Child private JSToBooleanNode toBooleanNode;

        public JSIteratorEveryNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization()
        protected Object every(Object thisObj, Object callback) {
            IteratorRecord iterated = getIteratorDirect(thisObj);
            callback = checkCallbackIsFunction(callback);

            while (true) {
                Object next = iteratorStep(iterated);
                if (next == Boolean.FALSE) {
                    break;
                }
                Object nextValue = getIteratorValue((DynamicObject) next);
                Object result = callFunction((DynamicObject) callback, nextValue);
                iteratorCloseAbrupt((DynamicObject) thisObj, result);
                if (toBoolean(result) == Boolean.FALSE) {
                    return Boolean.FALSE;
                }
            }

            return Boolean.TRUE;
        }

        protected Object toBoolean(Object value) {
            if (toBooleanNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toBooleanNode = insert(JSToBooleanNode.create());
            }
            return toBooleanNode.executeBoolean(value);
        }
    }

    /**
     * Implementation of the Iterator.prototype.find(fn).
     */
    public abstract static class JSIteratorFindNode extends JSAbstractIteratorWithCallNode {
        @Child private JSToBooleanNode toBooleanNode;

        public JSIteratorFindNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization()
        protected Object find(Object thisObj, Object callback) {
            IteratorRecord iterated = getIteratorDirect(thisObj);
            callback = checkCallbackIsFunction(callback);

            while (true) {
                Object next = iteratorStep(iterated);
                if (next == Boolean.FALSE) {
                    break;
                }
                Object nextValue = getIteratorValue((DynamicObject) next);
                Object result = callFunction((DynamicObject) callback, nextValue);
                iteratorCloseAbrupt((DynamicObject) thisObj, result);
                if (toBoolean(result) == Boolean.TRUE) {
                    return nextValue;
                }
            }

            return Undefined.instance;
        }

        protected Object toBoolean(Object value) {
            if (toBooleanNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toBooleanNode = insert(JSToBooleanNode.create());
            }
            return toBooleanNode.executeBoolean(value);
        }
    }
    /**
     * Implementation of the Iterator.prototype.map(fn).
     */
    public abstract static class JSIteratorMapNode extends JSBuiltinNode {
        @Child private IsCallableNode isCallableNode;
        @Child private GetIteratorDirectNode getIteratorDirectNode;
        @Child private IteratorStepNode iteratorStepNode;
        @Child private IteratorValueNode getIteratorValueNode;
        @Child private JSFunctionCallNode callMapFnNode;
        @Child private YieldNode yieldNode;

        protected final BranchProfile errorBranch = BranchProfile.create();

        public JSIteratorMapNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getIteratorDirectNode = GetIteratorDirectNode.create(context);
        }

        @Specialization()
        protected DynamicObject map(Object thisObj, Object callback) {
//            IteratorRecord iterated = getIteratorDirectNode.execute(thisObj);
//            callback = checkCallbackIsFunction(callback);
//            DynamicObject lastValue = Undefined.instance;
//
//            while (true) {
//                Object next = iteratorStep(iterated);
//                if (next == Boolean.FALSE) {
//                    return (DynamicObject) Undefined.instance;
//                }
//                Object value = getIteratorValue((DynamicObject) next);
//                Object mapped = callMapFn((DynamicObject) callback, value);
//                lastValue = yield(mapped);
//            }
//
//            return lastValue;
            return (DynamicObject) Undefined.instance;
        }

        protected final Object callMapFn(Object function, Object... userArguments) {
            if (callMapFnNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callMapFnNode = insert(JSFunctionCallNode.createCall());
            }
            return callMapFnNode.executeCall(JSArguments.create(function, userArguments));
        }

        protected final Object checkCallbackIsFunction(Object callback) {
            if (!isCallable(callback)) {
                errorBranch.enter();
                throw Errors.createTypeErrorNotAFunction(callback, this);
            }
            return callback;
        }

        protected Object iteratorStep(IteratorRecord iteratorRecord) {
            if (iteratorStepNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                iteratorStepNode = insert(IteratorStepNode.create(getContext()));
            }
            return iteratorStepNode.execute(iteratorRecord);
        }
        protected Object getIteratorValue(DynamicObject iteratorResult) {
            if (getIteratorValueNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getIteratorValueNode = insert(IteratorValueNode.create(getContext()));
            }
            return getIteratorValueNode.execute(iteratorResult);
        }

        protected final boolean isCallable(Object callback) {
            if (isCallableNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isCallableNode = insert(IsCallableNode.create());
            }
            return isCallableNode.executeBoolean(callback);
        }
    }
}
