/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex;

import com.oracle.truffle.regex.tregex.dfa.DFAGenerator;
import com.oracle.truffle.regex.tregex.nfa.NFA;
import com.oracle.truffle.regex.tregex.nfa.NFAGenerator;
import com.oracle.truffle.regex.tregex.nfa.NFATraceFinderGenerator;
import com.oracle.truffle.regex.tregex.nodes.DFACaptureGroupPartialTransitionNode;
import com.oracle.truffle.regex.tregex.nodes.TRegexDFAExecutorNode;
import com.oracle.truffle.regex.tregex.nodes.TraceFinderDFAStateNode;
import com.oracle.truffle.regex.tregex.parser.RegexParser;
import com.oracle.truffle.regex.tregex.parser.ast.RegexAST;
import com.oracle.truffle.regex.tregex.util.DFANodeSplit;

public class TRegexOptions {

    /**
     * Try to pre-calculate results of tree-like expressions (see {@link NFATraceFinderGenerator}).
     * A regular expression is considered tree-like if it does not contain infinite loops (+ or *).
     * This option will increase performance at the cost of startup time and memory usage.
     */
    public static final boolean TRegexEnableTraceFinder = true;

    /**
     * Maximum number of pre-calculated results per TraceFinder DFA. This number must not be higher
     * than 254, because we compress the result indices to {@code byte} in
     * {@link TraceFinderDFAStateNode}, with 255 being reserved for "no result"!
     */
    public static final int TRegexTraceFinderMaxNumberOfResults = 254;

    /**
     * Try to make control flow through DFAs reducible by node splitting (see {@link DFANodeSplit}).
     * This option will increase performance at the cost of startup time and memory usage.
     */
    public static final boolean TRegexEnableNodeSplitter = false;

    /**
     * Maximum size of a DFA after being altered by {@link DFANodeSplit}.
     */
    public static final int TRegexMaxDFASizeAfterNodeSplitting = 10_000;

    /**
     * Bailout threshold for number of nodes in the parser tree ({@link RegexAST} generated by
     * {@link RegexParser}). This number must not be higher than {@link Short#MAX_VALUE}, because we
     * use {@code short} values for indexing AST nodes. The current setting is based on run times of
     * {@code graal/com.oracle.truffle.js.test/js/trufflejs/regexp/npm_extracted/hungry-regexp*.js}
     */
    public static final int TRegexMaxParseTreeSize = 4_000;

    /**
     * Bailout threshold for number of nodes in the NFA ({@link NFA} generated by
     * {@link NFAGenerator}). This number must not be higher than {@link Short#MAX_VALUE}, because
     * we use {@code short} values for indexing NFA nodes. The current setting is based on run times
     * of
     * {@code graal/com.oracle.truffle.js.test/js/trufflejs/regexp/npm_extracted/hungry-regexp*.js}
     */
    public static final int TRegexMaxNFASize = 3_500;

    /**
     * Bailout threshold for number of nodes in the DFA ({@link TRegexDFAExecutorNode} generated by
     * {@link DFAGenerator}). This number must not be higher than {@link Short#MAX_VALUE}, because
     * we use {@code short} values for indexing DFA nodes. The current setting is based on run times
     * of
     * {@code graal/com.oracle.truffle.js.test/js/trufflejs/regexp/npm_extracted/hungry-regexp*.js}
     */
    public static final int TRegexMaxDFASize = 3_000;

    /**
     * Maximum number of entries in the global compilation cache in
     * {@link com.oracle.truffle.regex.RegexLanguage}.
     */
    public static final int RegexMaxCacheSize = 1_000;

    /**
     * Bailout threshold for counted repetitions.
     */
    public static final int TRegexMaxCountedRepetition = 40;

    /**
     * Bailout threshold for number of capture groups. This number must not be higher than 127,
     * because we compress capture group boundary indices to {@code byte} in
     * {@link DFACaptureGroupPartialTransitionNode}!
     */
    public static final int TRegexMaxNumberOfCaptureGroups = 127;

    /**
     * Maximum number of NFA states contained in one DFA state. This number must not be higher that
     * 255, because the maximum number of NFA states in one DFA state determines the number of
     * simultaneously tracked result sets (arrays) in capture group tracking mode, which are
     * accessed over byte indices in {@link DFACaptureGroupPartialTransitionNode}.
     */
    public static final int TRegexMaxNumberOfNFAStatesInOneDFAState = 255;

    static {
        assert TRegexTraceFinderMaxNumberOfResults <= 254;
        assert TRegexMaxParseTreeSize <= Short.MAX_VALUE;
        assert TRegexMaxNFASize <= Short.MAX_VALUE;
        assert TRegexMaxDFASize <= Short.MAX_VALUE;
        assert TRegexMaxDFASizeAfterNodeSplitting <= Short.MAX_VALUE;
        assert TRegexMaxNumberOfCaptureGroups <= 127;
        assert TRegexMaxNumberOfNFAStatesInOneDFAState <= 255;
    }
}
