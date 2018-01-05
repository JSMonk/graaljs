/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.array.dyn;

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArray;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetLength;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetUsedLength;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.array.ScriptArray;

public final class ZeroBasedObjectArray extends AbstractObjectArray {

    private static final ZeroBasedObjectArray ZERO_BASED_OBJECT_ARRAY = new ZeroBasedObjectArray(INTEGRITY_LEVEL_NONE, createCache());

    public static ZeroBasedObjectArray makeZeroBasedObjectArray(DynamicObject object, int length, int usedLength, Object[] array, int integrityLevel) {
        ZeroBasedObjectArray arrayType = createZeroBasedObjectArray().setIntegrityLevel(object, integrityLevel);
        arraySetLength(object, length);
        arraySetUsedLength(object, usedLength);
        arraySetArray(object, array);
        return arrayType;
    }

    public static ZeroBasedObjectArray createZeroBasedObjectArray() {
        return ZERO_BASED_OBJECT_ARRAY;
    }

    private ZeroBasedObjectArray(int integrityLevel, DynamicArrayCache cache) {
        super(integrityLevel, cache);
    }

    @Override
    public boolean isSupported(DynamicObject object, long index, boolean condition) {
        return isSupportedZeroBased(object, (int) index, condition);
    }

    @Override
    public Object getInBoundsFastObject(DynamicObject object, int index, boolean condition) {
        return castNonNull(getArray(object, condition)[index]);
    }

    @Override
    public void setInBoundsFast(DynamicObject object, int index, Object value, boolean condition) {
        getArray(object, condition)[index] = checkNonNull(value);
        if (JSTruffleOptions.TraceArrayWrites) {
            traceWriteValue("InBoundsFast", index, value);
        }
    }

    @Override
    protected int prepareInBoundsFast(DynamicObject object, long index, boolean condition) {
        return (int) index;
    }

    @Override
    protected int prepareInBounds(DynamicObject object, int index, boolean condition, ProfileHolder profile) {
        prepareInBoundsZeroBased(object, index, condition, profile);
        return index;
    }

    @Override
    protected int prepareSupported(DynamicObject object, int index, boolean condition, ProfileHolder profile) {
        prepareSupportedZeroBased(object, index, condition, profile);
        return index;
    }

    @Override
    protected void setLengthLess(DynamicObject object, long length, boolean condition, ProfileHolder profile) {
        setLengthLessZeroBased(object, length, condition, profile);
    }

    @Override
    public ContiguousObjectArray toContiguous(DynamicObject object, long index, Object value, boolean condition) {
        Object[] array = getArray(object, condition);
        int length = lengthInt(object, condition);
        int usedLength = getUsedLength(object, condition);

        ContiguousObjectArray newArray = ContiguousObjectArray.makeContiguousObjectArray(object, length, array, 0, 0, usedLength, integrityLevel);
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public HolesObjectArray toHoles(DynamicObject object, long index, Object value, boolean condition) {
        Object[] array = getArray(object, condition);
        int length = lengthInt(object, condition);
        int usedLength = getUsedLength(object, condition);

        HolesObjectArray newArray = HolesObjectArray.makeHolesObjectArray(object, length, array, 0, 0, usedLength, 0, integrityLevel);
        if (JSTruffleOptions.TraceArrayTransitions) {
            traceArrayTransition(this, newArray, index, value);
        }
        return newArray;
    }

    @Override
    public Object[] toArray(DynamicObject object) {
        return toArrayZeroBased(object);
    }

    @Override
    public long firstElementIndex(DynamicObject object, boolean condition) {
        return 0;
    }

    @Override
    public long lastElementIndex(DynamicObject object, boolean condition) {
        return getUsedLength(object, condition) - 1;
    }

    @Override
    public ScriptArray removeRangeImpl(DynamicObject object, long start, long end) {
        Object[] array = getArray(object);
        int usedLength = getUsedLength(object);
        System.arraycopy(array, (int) end, array, (int) start, Math.max(0, (int) (usedLength - end)));
        return this;
    }

    @Override
    public ScriptArray addRangeImpl(DynamicObject object, long offset, int size) {
        return addRangeImplZeroBased(object, offset, size);
    }

    @Override
    public boolean hasHoles(DynamicObject object, boolean condition) {
        int length = lengthInt(object, condition);
        int usedLength = getUsedLength(object, condition);
        return usedLength < length;
    }

    @Override
    protected ZeroBasedObjectArray withIntegrityLevel(int newIntegrityLevel) {
        return new ZeroBasedObjectArray(newIntegrityLevel, cache);
    }
}
