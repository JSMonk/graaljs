/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nfa;

import com.oracle.truffle.regex.tregex.buffer.CompilationBuffer;
import com.oracle.truffle.regex.tregex.parser.ast.CharacterClass;
import com.oracle.truffle.regex.tregex.parser.ast.Group;
import com.oracle.truffle.regex.tregex.parser.ast.LookAheadAssertion;
import com.oracle.truffle.regex.tregex.parser.ast.LookBehindAssertion;
import com.oracle.truffle.regex.tregex.parser.ast.MatchFound;
import com.oracle.truffle.regex.tregex.parser.ast.PositionAssertion;
import com.oracle.truffle.regex.tregex.parser.ast.RegexAST;
import com.oracle.truffle.regex.tregex.parser.ast.RegexASTNode;
import com.oracle.truffle.regex.tregex.parser.ast.Term;
import com.oracle.truffle.regex.tregex.parser.ast.visitors.NFATraversalRegexASTVisitor;
import com.oracle.truffle.regex.util.CompilationFinalBitSet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Regex AST visitor that will find convert all NFA successors of a given {@link Term} to
 * {@link ASTTransition}s (by calculating their respective {@link GroupBoundaries}) and annotate for
 * every successor which {@link LookAheadAssertion}s and/or {@link LookBehindAssertion}s it should
 * be merged with. For example, when starting from Term "a" in the expression
 * {@code /a(b|(?=c)d)(?<=e)/}, it will find the successors "b" and "d", where "d" must be merged
 * with the successors of the look-ahead assertion ("c"), and both successors may be merged with the
 * successors of the look-behind assertion ("e").
 *
 * @see NFATraversalRegexASTVisitor
 */
public final class ASTStepVisitor extends NFATraversalRegexASTVisitor {

    private ASTStep stepCur;
    private final Map<LookAheadAssertion, ASTStep> lookAheadMap = new HashMap<>();
    private final Map<LookAheadAssertion, ASTStep> lookAheadMapWithCaret = new HashMap<>();
    private final List<ASTStep> curLookAheads = new ArrayList<>();
    private final List<ASTStep> curLookBehinds = new ArrayList<>();
    private final Deque<ASTStep> lookAroundExpansionQueue = new ArrayDeque<>();
    private final CompilationFinalBitSet updateStarts;
    private final CompilationFinalBitSet updateEnds;
    private final CompilationFinalBitSet clearStarts;
    private final CompilationFinalBitSet clearEnds;

    private final CompilationBuffer compilationBuffer;

    public ASTStepVisitor(RegexAST ast, CompilationBuffer compilationBuffer) {
        super(ast);
        this.compilationBuffer = compilationBuffer;
        updateStarts = new CompilationFinalBitSet(ast.getNumberOfCaptureGroups());
        updateEnds = new CompilationFinalBitSet(ast.getNumberOfCaptureGroups());
        clearStarts = new CompilationFinalBitSet(ast.getNumberOfCaptureGroups());
        clearEnds = new CompilationFinalBitSet(ast.getNumberOfCaptureGroups());
    }

    public ASTStep step(NFAState expandState) {
        ASTStep stepRoot = null;
        assert curLookAheads.isEmpty();
        assert curLookBehinds.isEmpty();
        assert lookAroundExpansionQueue.isEmpty();
        for (RegexASTNode t : expandState.getStateSet()) {
            if (t.isInLookAheadAssertion()) {
                ASTStep laStep = new ASTStep(t);
                curLookAheads.add(laStep);
                lookAroundExpansionQueue.push(laStep);
            } else if (t.isInLookBehindAssertion()) {
                ASTStep lbStep = new ASTStep(t);
                curLookBehinds.add(lbStep);
                lookAroundExpansionQueue.push(lbStep);
            } else {
                assert stepRoot == null;
                stepRoot = new ASTStep(t);
            }
        }
        if (stepRoot == null) {
            assert !curLookAheads.isEmpty();
            stepRoot = curLookAheads.get(curLookAheads.size() - 1);
            curLookAheads.remove(curLookAheads.size() - 1);
            lookAroundExpansionQueue.remove(stepRoot);
        }
        stepCur = stepRoot;
        Term root = (Term) stepRoot.getRoot();
        setTraversableLookBehindAssertions(expandState instanceof NFAMatcherState ? ((NFAMatcherState) expandState).getFinishedLookBehinds() : Collections.emptySet());
        setCanTraverseCaret(root instanceof PositionAssertion && ast.getNfaAnchoredInitialStates().contains(root));
        run(root);
        curLookAheads.clear();
        curLookBehinds.clear();
        while (!lookAroundExpansionQueue.isEmpty()) {
            stepCur = lookAroundExpansionQueue.pop();
            root = (Term) stepCur.getRoot();
            run(root);
        }
        return stepRoot;
    }

    @Override
    protected void visit(ArrayList<PathElement> path) {
        ASTSuccessor successor = new ASTSuccessor(compilationBuffer);
        ASTTransition transition = new ASTTransition();
        PositionAssertion dollar = null;
        updateStarts.clear();
        updateEnds.clear();
        clearStarts.clear();
        clearEnds.clear();
        Group outerPassThrough = null;
        for (PathElement element : path) {
            final RegexASTNode node = element.getNode();
            if (node instanceof Group) {
                Group group = (Group) node;
                if (element.isGroupEnter()) {
                    if (outerPassThrough == null) {
                        if (element.isGroupPassThrough() && group.isExpandedQuantifier()) {
                            outerPassThrough = group;
                        }
                        if (group.isCapturing() && (!element.isGroupPassThrough() || !group.isExpandedQuantifier())) {
                            updateStarts.set(group.getGroupNumber());
                            clearStarts.clear(group.getGroupNumber());
                        }
                        if (!element.isGroupPassThrough() && (group.isLoop() || group.isExpandedQuantifier())) {
                            for (int i = group.getEnclosedCaptureGroupsLow(); i < group.getEnclosedCaptureGroupsHigh(); i++) {
                                if (!updateStarts.get(i)) {
                                    clearStarts.set(i);
                                }
                                if (!updateEnds.get(i)) {
                                    clearEnds.set(i);
                                }
                            }
                        }
                    }
                } else {
                    assert element.isGroupExit();
                    if (outerPassThrough == null) {
                        if (group.isCapturing() && (!element.isGroupPassThrough() || !group.isExpandedQuantifier())) {
                            updateEnds.set(group.getGroupNumber());
                            clearEnds.clear(group.getGroupNumber());
                        }
                    } else if (outerPassThrough == group) {
                        outerPassThrough = null;
                    }
                }
            } else if (node instanceof PositionAssertion && ((PositionAssertion) node).type == PositionAssertion.Type.DOLLAR) {
                dollar = (PositionAssertion) node;
            }
        }
        transition.getGroupBoundaries().setIndices(updateStarts, updateEnds, clearStarts, clearEnds);
        final RegexASTNode lastNode = path.get(path.size() - 1).getNode();
        if (dollar == null) {
            if (lastNode instanceof CharacterClass) {
                final CharacterClass charClass = (CharacterClass) lastNode;
                ArrayList<ASTStep> newLookBehinds = new ArrayList<>();
                for (Group g : charClass.getLookBehindEntries()) {
                    final ASTStep lbAstStep = new ASTStep(g);
                    assert g.isLiteral();
                    lbAstStep.addSuccessor(new ASTSuccessor(compilationBuffer, new ASTTransition(g.getAlternatives().get(0).getFirstTerm())));
                    newLookBehinds.add(lbAstStep);
                }
                transition.setTarget(charClass);
                successor.setLookBehinds(newLookBehinds);
            } else {
                assert lastNode instanceof MatchFound;
                transition.setTarget((MatchFound) lastNode);
            }
        } else {
            assert lastNode instanceof MatchFound;
            transition.setTarget(dollar);
        }
        successor.addInitialTransition(transition);
        if (!curLookAheads.isEmpty()) {
            successor.setLookAheads(new ArrayList<>(curLookAheads));
        }
        if (!curLookBehinds.isEmpty()) {
            successor.addLookBehinds(curLookBehinds);
        }
        stepCur.addSuccessor(successor);
    }

    @Override
    protected void enterLookAhead(LookAheadAssertion assertion) {
        curLookAheads.add((canTraverseCaret() ? lookAheadMapWithCaret : lookAheadMap).computeIfAbsent(assertion, x -> {
            ASTStep laStep = new ASTStep(assertion.getGroup());
            lookAroundExpansionQueue.push(laStep);
            return laStep;
        }));
    }

    @Override
    protected void leaveLookAhead(LookAheadAssertion assertion) {
        assert curLookAheads.get(curLookAheads.size() - 1).getRoot().getParent() == assertion;
        curLookAheads.remove(curLookAheads.size() - 1);
    }
}
