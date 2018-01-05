/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.parser.ast;

/**
 * A common supertype for all {@link RegexASTNode}s except {@link Sequence}s.
 * <p>
 * Roughly corresponds to the goal symbol <em>Term</em> in the ECMAScript RegExp syntax. A
 * <em>Term</em> ({@link Term}) can be either an <em>Assertion</em> ({@link PositionAssertion} or
 * {@link RegexASTSubtreeRootNode}) or an <em>Atom</em> ({@link CharacterClass},
 * {@link BackReference} or {@link Group}). <em>Quantifier</em>s are handled by the
 * {@link Group#isLoop()} flag of {@link Group}s.
 */
public abstract class Term extends RegexASTNode {

    private short seqIndex = 0;

    Term() {
    }

    Term(Term copy) {
        super(copy);
    }

    @Override
    public abstract Term copy(RegexAST ast);

    public int getSeqIndex() {
        return seqIndex;
    }

    public void setSeqIndex(int seqIndex) {
        this.seqIndex = (short) seqIndex;
    }

    @Override
    public RegexASTSubtreeRootNode getSubTreeParent() {
        RegexASTNode current = this;
        while (current.getParent() != null) {
            assert current instanceof Term;
            if (current.getParent() instanceof RegexASTSubtreeRootNode) {
                return (RegexASTSubtreeRootNode) current.getParent();
            }
            // structure is always Group -> Sequence -> Term
            current = current.getParent().getParent();
        }
        // this should only be reached by nodes generated by RegexAST#createNFAInitialStates()!
        return null;
    }

    /**
     * Marks the node as dead, i.e. unmatchable.
     * <p>
     * Note that using this setter also traverses the ancestors of this node and updates their
     * "dead" status as well.
     */
    @Override
    public void markAsDead() {
        super.markAsDead();
        if (getParent() != null && !getParent().isDead()) {
            getParent().markAsDead();
        }
    }
}
