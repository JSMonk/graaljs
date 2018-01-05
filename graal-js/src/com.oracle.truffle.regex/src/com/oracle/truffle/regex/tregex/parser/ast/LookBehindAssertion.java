/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.parser.ast;

import com.oracle.truffle.regex.tregex.util.DebugUtil;

/**
 * An assertion that succeeds depending on whether or not text preceding the current position
 * matches a given regular expression.
 * <p>
 * While look-behind assertions are not (yet) part of the ECMAScript specification, they are used
 * internally as a means to implement the word boundary assertions \b and \B.
 * <p>
 * Currently, the fragment of regular expressions that TRegex supports in look-behind assertions is
 * limited to so-called "literal" regular expressions, consisting only of concatenations and
 * character classes (which generalize literal characters). The method {@link #isLiteral} verifies
 * whether the body of the assertion ({@link #getGroup()}) is of this form.
 */
public class LookBehindAssertion extends RegexASTSubtreeRootNode {

    /**
     * Creates a new look-behind assertion AST node.
     * 
     * Note that for this node to be complete, {@link RegexASTSubtreeRootNode#setGroup(Group)} has
     * to be called with the {@link Group} that represents the contents of this look-ahead
     * assertion.
     */
    LookBehindAssertion() {
    }

    private LookBehindAssertion(LookBehindAssertion copy, RegexAST ast) {
        super(copy, ast);
    }

    @Override
    public LookBehindAssertion copy(RegexAST ast) {
        return ast.register(new LookBehindAssertion(this, ast));
    }

    /**
     * Verifies that the contents of this assertion ({@link #getGroup()}) are in "literal" form.
     * 
     * This means that there is only a single alternative which is composed of a sequence of
     * {@link CharacterClass} nodes and terminated by a {@link MatchFound} node.
     */
    public boolean isLiteral() {
        if (getGroup().getAlternatives().size() != 1) {
            return false;
        }
        for (Term t : getGroup().getAlternatives().get(0).getTerms()) {
            if (!(t instanceof CharacterClass)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the length of the words that can be matched by the body of this look-behind
     * assertion.
     * <p>
     * Because we restrict the regular expressions used in look-behind assertions to "literal"
     * regular expressions, all strings that match the body of the assertion are guaranteed to be of
     * the same length. This is critical to how look-behind is implemented, because it tells us how
     * much do we have to rewind when matching a regular expression with look-behind assertions.
     */
    public int getLength() {
        assert isLiteral();
        return getGroup().getAlternatives().get(0).getTerms().size();
    }

    @Override
    public String getPrefix() {
        return "?<=";
    }

    @Override
    public DebugUtil.Table toTable() {
        return toTable("LookBehindAssertion");
    }
}
