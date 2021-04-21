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
import com.oracle.truffle.js.builtins.IteratorPrototypeBuiltinsFactory.JSIteratorToArrayNodeGen;
import com.oracle.truffle.js.nodes.access.GetIteratorDirectNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSIterator;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;
import com.oracle.truffle.js.runtime.util.UnmodifiableArrayList;

import java.util.LinkedList;
import java.util.List;

/**
 * Contains builtins for {@linkplain JSIterator}.prototype.
 */
public final class IteratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<IteratorPrototypeBuiltins.IteratorPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new IteratorPrototypeBuiltins();

    protected IteratorPrototypeBuiltins() {
        super(JSIterator.PROTOTYPE_NAME, IteratorPrototype.class);
    }

    public enum IteratorPrototype implements BuiltinEnum<IteratorPrototype> {
        toArray(0);

        private final int length;

        IteratorPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, IteratorPrototype builtinEnum) {
        switch (builtinEnum) {
            case toArray:
                return JSIteratorToArrayNodeGen.create(context, builtin, args().withThis().fixedArgs(0).createArgumentNodes(context));
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
     * Implementation of the Iterator.prototype.has().
     */
    public abstract static class JSIteratorToArrayNode extends JSBuiltinNode {
        @Child private GetIteratorDirectNode getIteratorDirectNode;
        @Child private IteratorStepNode iteratorStepNode;
        @Child private IteratorValueNode getIteratorValueNode;

        private final BranchProfile growProfile = BranchProfile.create();

        public JSIteratorToArrayNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getIteratorDirectNode = GetIteratorDirectNode.create(context);
        }

        @Specialization()
        protected DynamicObject toArray(Object thisObj) {
            IteratorRecord iterated = getIteratorDirectNode.execute(thisObj);
            SimpleArrayList<Object> items = new SimpleArrayList<>();
            while (true) {
                Object next = iteratorStep(iterated);
                if (next == Boolean.FALSE) {
                    break;
                }
                Object nextValue = getIteratorValue((DynamicObject) next);
                items.add(nextValue, growProfile);
            }
            return JSRuntime.createArrayFromList(getContext(), new UnmodifiableArrayList<>(items.toArray()));
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
}
