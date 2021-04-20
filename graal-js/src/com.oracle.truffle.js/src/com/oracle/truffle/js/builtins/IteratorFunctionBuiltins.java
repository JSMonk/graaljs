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
import com.oracle.truffle.api.profiles.ConditionProfile;

import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.access.*;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.builtins.JSIterator;
import com.oracle.truffle.js.runtime.builtins.JSWrapForValidIterator;
import com.oracle.truffle.js.nodes.binary.InstanceofNode;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.builtins.IteratorFunctionBuiltinsFactory.JSIteratorFromNodeGen;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for {@linkplain JSIterator} function (constructor).
 */
public final class IteratorFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<IteratorFunctionBuiltins.IteratorFunction> {

    public static final JSBuiltinsContainer BUILTINS = new IteratorFunctionBuiltins();

    protected IteratorFunctionBuiltins() {
        super(JSIterator.CLASS_NAME, IteratorFunction.class);
    }

    public enum IteratorFunction implements BuiltinEnum<IteratorFunction> {
        from(1);

        private final int length;

        IteratorFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, IteratorFunction builtinEnum) {
        switch (builtinEnum) {
            case from:
                return JSIteratorFromNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSIteratorFromNode extends JSBuiltinNode {
        @Child private GetMethodNode getIteratorMethodNode;
        @Child private JSFunctionCallNode callIteratorMethodNode;
        @Child private IsJSObjectNode isObjectNode;
        @Child private PropertyGetNode getNextMethodNode;
        @Child private InstanceofNode.OrdinaryHasInstanceNode ordinaryHasInstanceNode;
        @Child private CreateObjectNode.CreateObjectWithPrototypeNode createObjectNode;

        private final ConditionProfile isIterable = ConditionProfile.createBinaryProfile();

        public JSIteratorFromNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.createObjectNode = CreateObjectNode.createOrdinaryWithPrototype(context);
            this.getIteratorMethodNode = GetMethodNode.create(context, null, Symbol.SYMBOL_ITERATOR);
            this.ordinaryHasInstanceNode = InstanceofNode.OrdinaryHasInstanceNode.create(context);
        }

        @Specialization
        protected DynamicObject from(Object thisObj, Object target) {
            Object usingIterator = getIteratorMethodNode.executeWithTarget(target);
            IteratorRecord iteratorRecord;

            if (isIterable.profile(usingIterator != Undefined.instance)) {
                iteratorRecord = getIterator(target, usingIterator);
                DynamicObject iterator = iteratorRecord.getIterator();
                boolean hasInstance = ordinaryHasInstanceNode.executeBoolean(thisObj, iterator);
                if (hasInstance) {
                    return iterator;
                }
            } else {
                iteratorRecord = getIteratorDirect(target);
            }

            return createWrapForValidIteratorPrototype(iteratorRecord);
        }

        private DynamicObject createWrapForValidIteratorPrototype(IteratorRecord iteratorRecord) {
            return JSWrapForValidIterator.createWithIteratorRecord(getContext(), iteratorRecord);
        }

        private IteratorRecord getIterator(Object object, Object usingIterator) {
            if (callIteratorMethodNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callIteratorMethodNode = insert(JSFunctionCallNode.createCall());
            }
            if (isObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isObjectNode = insert(IsJSObjectNode.create());
            }
            if (getNextMethodNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getNextMethodNode = insert(PropertyGetNode.create(JSRuntime.NEXT, getContext()));
            }

            return GetIteratorNode.getIterator(object, usingIterator, callIteratorMethodNode, isObjectNode, getNextMethodNode, this);
        }

        private IteratorRecord getIteratorDirect(Object object) {
            if (isObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isObjectNode = insert(IsJSObjectNode.create());
            }
            if (getNextMethodNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getNextMethodNode = insert(PropertyGetNode.create(JSRuntime.NEXT, getContext()));
            }

            if (isObjectNode.executeBoolean(object)) {
                return IteratorRecord.create((DynamicObject) object, getNextMethodNode.getValue(object), false);
            }

            throw Errors.createTypeErrorNotAnObject(object, this);
        }
    }
}
