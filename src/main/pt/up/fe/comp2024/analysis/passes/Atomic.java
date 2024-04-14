package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

public class Atomic extends AnalysisVisitor {
    @Override
    protected void buildVisitor() {
        addVisit(Kind.THIS, this::visitThis);
        addVisit(Kind.INTEGER_LITERAL, this::visitIntegerLiteral);
        addVisit(Kind.BOOL_LITERAL, this::visitBooleanLiteral);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRef);
    }

    private Void visitIntegerLiteral(JmmNode node, SymbolTable table) {
        node.put("node_type", "int");
        return null;
    }

    private Void visitBooleanLiteral(JmmNode node, SymbolTable table) {
        node.put("node_type", "boolean");
        return null;
    }

    private Void visitVarRef(JmmNode node, SymbolTable table) {
        String varRefName = node.get("name");
        node.put("node_type", NodeUtils.getLocalVariableType(varRefName, currentMethod, table));
        return null;
    }

    private Void visitThis(JmmNode node, SymbolTable table) {
        node.put("node_type", table.getClassName());
        return null;
    }
}
