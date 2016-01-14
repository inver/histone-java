package ru.histone.v2.parser.node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by alexey.nevinsky on 24.12.2015.
 */
public class AstNode implements Serializable {
    private final int type;
    private List<AstNode> nodes = new ArrayList<>();
    private List<Object> values = new ArrayList<>();

    public AstNode(int type) {
        this.type = type;
    }

    public AstNode(AstType type) {
        this(type.getId());
    }

    public AstNode(AstType type, AstNode res) {
        this(type.getId());
        add(res);
    }

    public static AstNode forValue(Object object) {
        AstNode node = new AstNode(Integer.MIN_VALUE);
        node.addValue(object);
        return node;
    }

    public AstNode add(AstNode node) {
        nodes.add(node);
        return this;
    }

    public AstNode addValue(Object node) {
        values.add(node);
        return this;
    }

    public List<Object> getValues() {
        return values;
    }

    public AstNode add(AstNode... nodes) {
        this.nodes.addAll(Arrays.asList(nodes));
        return this;
    }

    public int getType() {
        return type;
    }

    public List<AstNode> getNodes() {
        return nodes;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AstNode{");
        sb.append("type=");
        AstType t = AstType.fromId(type);
        if (t == null) {
            sb.append(type);
        } else {
            sb.append(t.name()).append("(").append(type).append(")");
        }
        sb.append(", nodes=").append(nodes);
        sb.append(", values=").append(values);
        sb.append('}');
        return sb.toString();
    }

    public AstNode escaped() {
        //todo
        return this;
    }
}