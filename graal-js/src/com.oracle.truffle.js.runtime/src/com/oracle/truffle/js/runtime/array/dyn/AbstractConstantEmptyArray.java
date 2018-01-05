/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.array.dyn;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.array.ArrayAllocationSite;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class AbstractConstantEmptyArray extends AbstractConstantArray {

    protected AbstractConstantEmptyArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    protected static void setCapacity(DynamicObject object, long length) {
        JSArray.arraySetLength(object, length);
    }

    protected static long getCapacity(DynamicObject object) {
        return JSArray.arrayGetLength(object);
    }

    protected static long getCapacity(DynamicObject object, boolean condition) {
        return JSArray.arrayGetLength(object, condition);
    }

    @Override
    public Object getElementInBounds(DynamicObject object, int index, boolean condition) {
        return Undefined.instance;
    }

    @Override
    public int lengthInt(DynamicObject object, boolean condition) {
        return (int) getCapacity(object, condition);
    }

    @Override
    public Object[] toArray(DynamicObject object) {
        int cap = (int) getCapacity(object);
        Object[] arr = new Object[cap];
        Arrays.fill(arr, Undefined.instance);
        return arr;
    }

    @Override
    public boolean hasElement(DynamicObject object, long index, boolean condition) {
        return false;
    }

    @Override
    public ScriptArray deleteElementImpl(DynamicObject object, long index, boolean strict, boolean condition) {
        return this;
    }

    @Override
    public long firstElementIndex(DynamicObject object, boolean condition) {
        return length(object, condition); // there is no element in this array
    }

    @Override
    public long lastElementIndex(DynamicObject object, boolean condition) {
        return -1; // there is no element in this array
    }

    @Override
    public long nextElementIndex(DynamicObject object, long index, boolean condition) {
        return JSRuntime.MAX_SAFE_INTEGER_LONG;
    }

    @Override
    public long previousElementIndex(DynamicObject object, long index, boolean condition) {
        return -1;
    }

    @Override
    public AbstractIntArray createWriteableInt(DynamicObject object, long index, int value, ProfileHolder profile) {
        assert index >= 0; // corner case, length would not be int then
        int capacity = lengthInt(object);
        int[] initialArray = new int[calcNewArraySize(capacity, profile)];
        AbstractIntArray newArray;
        if (CREATE_WRITABLE_PROFILE.indexZero(profile, index == 0)) {
            newArray = ZeroBasedIntArray.makeZeroBasedIntArray(object, capacity, 0, initialArray, integrityLevel);
        } else {
            newArray = createWritableIntContiguous(object, capacity, index, initialArray, profile);
        }
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        notifyAllocationSite(object, newArray);
        return newArray;
    }

    private AbstractIntArray createWritableIntContiguous(DynamicObject object, int capacity, long index, int[] initialArray, ProfileHolder profile) {
        long length = Math.max(index + 1, capacity);
        int arrayOffset = 0;
        long indexOffset = index;
        if (CREATE_WRITABLE_PROFILE.indexLessThanLength(profile, index < initialArray.length)) {
            arrayOffset = (int) index;
            indexOffset = 0;
        }
        return ContiguousIntArray.makeContiguousIntArray(object, length, initialArray, indexOffset, arrayOffset, 0, integrityLevel);
    }

    private static int calcNewArraySize(int capacity, ProfileHolder profile) {
        if (CREATE_WRITABLE_PROFILE.lengthZero(profile, capacity == 0)) {
            return JSTruffleOptions.InitialArraySize;
        } else if (CREATE_WRITABLE_PROFILE.lengthBelowLimit(profile, capacity < JSTruffleOptions.MaxFlatArraySize)) {
            return capacity;
        } else {
            return JSTruffleOptions.InitialArraySize;
        }
    }

    @Override
    public AbstractDoubleArray createWriteableDouble(DynamicObject object, long index, double value, ProfileHolder profile) {
        int capacity = lengthInt(object);
        double[] initialArray = new double[calcNewArraySize(capacity, profile)];
        AbstractDoubleArray newArray;
        if (CREATE_WRITABLE_PROFILE.indexZero(profile, index == 0)) {
            newArray = ZeroBasedDoubleArray.makeZeroBasedDoubleArray(object, capacity, 0, initialArray, integrityLevel);
        } else {
            newArray = createWritableDoubleContiguous(object, capacity, index, initialArray, profile);
        }
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        notifyAllocationSite(object, newArray);
        return newArray;
    }

    private AbstractDoubleArray createWritableDoubleContiguous(DynamicObject object, int capacity, long index, double[] initialArray, ProfileHolder profile) {
        long length = Math.max(index + 1, capacity);
        int arrayOffset = 0;
        long indexOffset = index;
        if (CREATE_WRITABLE_PROFILE.indexLessThanLength(profile, index < initialArray.length)) {
            arrayOffset = (int) index;
            indexOffset = 0;
        }
        return ContiguousDoubleArray.makeContiguousDoubleArray(object, length, initialArray, indexOffset, arrayOffset, 0, integrityLevel);
    }

    @Override
    public AbstractJSObjectArray createWriteableJSObject(DynamicObject object, long index, DynamicObject value, ProfileHolder profile) {
        int capacity = lengthInt(object);
        DynamicObject[] initialArray = new DynamicObject[calcNewArraySize(capacity, profile)];
        AbstractJSObjectArray newArray;
        if (CREATE_WRITABLE_PROFILE.indexZero(profile, index == 0)) {
            newArray = ZeroBasedJSObjectArray.makeZeroBasedJSObjectArray(object, capacity, 0, initialArray, integrityLevel);
        } else {
            newArray = createWritableJSObjectContiguous(object, capacity, index, initialArray, profile);
        }
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        notifyAllocationSite(object, newArray);
        return newArray;
    }

    private AbstractJSObjectArray createWritableJSObjectContiguous(DynamicObject object, int capacity, long index, DynamicObject[] initialArray, ProfileHolder profile) {
        long length = Math.max(index + 1, capacity);
        int arrayOffset = 0;
        long indexOffset = index;
        if (CREATE_WRITABLE_PROFILE.indexLessThanLength(profile, index < initialArray.length)) {
            arrayOffset = (int) index;
            indexOffset = 0;
        }
        return ContiguousJSObjectArray.makeContiguousJSObjectArray(object, length, initialArray, indexOffset, arrayOffset, 0, integrityLevel);
    }

    @Override
    public AbstractObjectArray createWriteableObject(DynamicObject object, long index, Object value, ProfileHolder profile) {
        int capacity = lengthInt(object);
        Object[] initialArray = new Object[calcNewArraySize(capacity, profile)];
        AbstractObjectArray newArray;
        if (CREATE_WRITABLE_PROFILE.indexZero(profile, index == 0)) {
            newArray = ZeroBasedObjectArray.makeZeroBasedObjectArray(object, capacity, 0, initialArray, integrityLevel);
        } else {
            newArray = createWritableObjectContiguous(object, capacity, index, initialArray, profile);
        }
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        notifyAllocationSite(object, newArray);
        return newArray;
    }

    private AbstractObjectArray createWritableObjectContiguous(DynamicObject object, int capacity, long index, Object[] initialArray, ProfileHolder profile) {
        long length = Math.max(index + 1, capacity);
        int arrayOffset = 0;
        long indexOffset = index;
        if (CREATE_WRITABLE_PROFILE.indexLessThanLength(profile, index < initialArray.length)) {
            arrayOffset = (int) index;
            indexOffset = 0;
        }
        return ContiguousObjectArray.makeContiguousObjectArray(object, length, initialArray, indexOffset, arrayOffset, 0, integrityLevel);
    }

    @Override
    public boolean isHolesType() {
        return true;
    }

    @Override
    public boolean hasHoles(DynamicObject object, boolean condition) {
        return getCapacity(object, condition) != 0;
    }

    private void notifyAllocationSite(DynamicObject object, ScriptArray newArray) {
        if (JSTruffleOptions.TrackArrayAllocationSites && CompilerDirectives.inInterpreter()) {
            ArrayAllocationSite site = JSAbstractArray.arrayGetAllocationSite(object);
            if (site != null) {
                site.notifyArrayTransition(newArray, lengthInt(object));
            }
        }
    }
}
