package ru.histone;

import com.fasterxml.jackson.core.JsonFactory;
import ru.histone.evaluator.Evaluator;
import ru.histone.evaluator.nodes.NodeFactory;
import ru.histone.optimizer.AstImportResolver;
import ru.histone.optimizer.AstInlineOptimizer;
import ru.histone.optimizer.AstMarker;
import ru.histone.optimizer.AstOptimizer;
import ru.histone.parser.Parser;

public class HistoneBootstrap {
    private  Parser parser;
    private  Evaluator evaluator;
    private NodeFactory nodeFactory;
    private AstOptimizer astAstOptimizer;
    private AstImportResolver astImportResolver;
    private AstMarker astMarker;
    private AstInlineOptimizer astInlineOptimizer;

    public Parser getParser() {
        return parser;
    }

    public void setParser(Parser parser) {
        this.parser = parser;
    }

    public Evaluator getEvaluator() {
        return evaluator;
    }

    public void setEvaluator(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    public NodeFactory getNodeFactory() {
        return nodeFactory;
    }

    public void setNodeFactory(NodeFactory nodeFactory) {
        this.nodeFactory = nodeFactory;
    }

    public AstOptimizer getAstAstOptimizer() {
        return astAstOptimizer;
    }

    public void setAstAstOptimizer(AstOptimizer astAstOptimizer) {
        this.astAstOptimizer = astAstOptimizer;
    }

    public AstImportResolver getAstImportResolver() {
        return astImportResolver;
    }

    public void setAstImportResolver(AstImportResolver astImportResolver) {
        this.astImportResolver = astImportResolver;
    }

    public AstMarker getAstMarker() {
        return astMarker;
    }

    public void setAstMarker(AstMarker astMarker) {
        this.astMarker = astMarker;
    }

    public AstInlineOptimizer getAstInlineOptimizer() {
        return astInlineOptimizer;
    }

    public void setAstInlineOptimizer(AstInlineOptimizer astInlineOptimizer) {
        this.astInlineOptimizer = astInlineOptimizer;
    }
}