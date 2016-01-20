package ru.histone.v2.parser;

import ru.histone.HistoneException;
import ru.histone.v2.exceptions.UnknownAstTypeException;
import ru.histone.v2.parser.node.AstNode;
import ru.histone.v2.parser.node.AstType;
import ru.histone.v2.utils.ParserUtils;

import java.util.*;

/**
 *
 * Created by gali.alykoff on 20/01/16.
 */
public class Marker {

    // TODO
    public void markReferences(
            AstNode astNode, Deque<Map<String, Integer>> scopeChain
    ) throws HistoneException {
        if (scopeChain == null) {
            scopeChain = new LinkedList<>();
        }
        final int astNodeType = astNode.getType();
        final AstType type = AstType.fromId(astNodeType);
        if (type == null) {
            throw new UnknownAstTypeException(astNodeType);
        }
        switch (type) {
            case AST_REF:
                String nameOfVar = ParserUtils.getValueFromStringNode(astNode.getNode(0));
                final AstNode refNode = getReference(nameOfVar, scopeChain);
                astNode.setNodes(Collections.singletonList(refNode));
                break;
            case AST_VAR:
                markReferences(astNode.getNode(0), scopeChain);
                nameOfVar = ParserUtils.getValueFromStringNode(astNode.getNode(1));
                final String name = setReference(nameOfVar, scopeChain);

                break;
            case AST_IF: break;
            case AST_FOR: break;
            case AST_MACRO: break;
            case AST_NODES: break;
            default: break;
        }

    }

    private AstNode getReference(String name, Deque<Map<String, Integer>> scopeChain) {
        int scopeIndex = scopeChain.size();
        final int currentScope = scopeIndex - 1;
        final Iterator<Map<String, Integer>> iterator = scopeChain.descendingIterator();
        while (iterator.hasNext()) {
            scopeIndex--;
            final Map<String, Integer> scope = iterator.next();
            final Integer variableIndex = scope.get(name);
            if (variableIndex != null) {
                final int scopeDeep = currentScope - scopeIndex;;
                final AstNode deepOfVarDefinitionNode = new AstNode(AstNode.LEAF_NODE_TYPE_ID)
                        .setValue(scopeDeep);
                final AstNode varIndexNode = new AstNode(AstNode.LEAF_NODE_TYPE_ID)
                        .setValue(variableIndex);
                return new AstNode(AstType.AST_REF)
                        .add(deepOfVarDefinitionNode)
                        .add(varIndexNode);
            }
        }

        final AstNode globalNode = new AstNode(AstType.AST_GLOBAL);
        final AstNode nameOfGlobalVarNode = new AstNode(AstNode.LEAF_NODE_TYPE_ID)
                .setValue(name);
        return new AstNode(AstType.AST_METHOD)
                .add(globalNode)
                .add(nameOfGlobalVarNode);
    }

    private Integer setReference(String name, Deque<Map<String, Integer>> scopeChain) {
        final Map<String, Integer> lastScope = scopeChain.getLast();
        if (!lastScope.containsKey(name)) {
            lastScope.put(name, lastScope.keySet().size());
        }
        return lastScope.get(name);
    }
}
