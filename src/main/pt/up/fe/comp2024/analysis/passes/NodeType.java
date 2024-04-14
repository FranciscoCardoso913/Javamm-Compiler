package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static pt.up.fe.comp2024.ast.TypeUtils.getExprType;

public class NodeType extends AnalysisVisitor {

    Pattern array_pattern = Pattern.compile("([a-zA-Z0-9]+)(_array)?(_ellipse)?");

    @Override
    protected void buildVisitor() {
        addVisit(Kind.METHOD_EXPR, this::visitMethodExpr);
        addVisit(Kind.THIS, this::visitThis);
        addVisit(Kind.NEW_OBJ_EXPR, this::visitNewObjectExpression);
        addVisit(Kind.INIT_ARRAY_EXPR, this::visitInitArrayExpression);
        addVisit(Kind.NEW_ARRAY_EXPR, this::visitNewArrayExpression);
        addVisit(Kind.METHOD, this::visitMethod);
        addVisit(Kind.LENGTH_ATTR_EXPR, this::visitLengthAttributeExpression);
        addVisit(Kind.PARENTH_EXPR, this::visitParenthExpression);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpression);
        addVisit(Kind.ARRAY_EXPR, this::visitArrayExpression);
        addVisit(Kind.INTEGER_LITERAL, this::visitIntegerLiteral);
        addVisit(Kind.BOOL_LITERAL, this::visitBooleanLiteral);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRef);
        addVisit(Kind.NEG_EXPR, this::visitNegationExpression);
    }

    private Void visitBinaryExpression(JmmNode node, SymbolTable table) {
        String node_type = getExprType(node, table).getName();
        node.put("node_type", node_type);
        return null;
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

    private Void visitParenthExpression(JmmNode node, SymbolTable table) {
        JmmNode child = node.getChild(0);
        visit(child, table);
        node.put("node_type", child.get("node_type"));
        return null;
    }

    private Void visitArrayExpression(JmmNode node, SymbolTable table) {

        var left = node.getChild(0);
        visit(left, table);
        Matcher matcher = array_pattern.matcher(left.get("node_type"));
        if (!(matcher.find() && (matcher.group(2) != null || matcher.group(3) !=null))) {
            String message = String.format(
                    "Array expected, got %s instead",
                    left.get("node_type")
            );
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null)
            );
            return null;
        }
        node.put("node_type", matcher.group(1));
        return null;
    }

    private Void visitLengthAttributeExpression(JmmNode node, SymbolTable table) {
        node.put("node_type", "int");
        return null;
    }

    private Void visitMethod(JmmNode node, SymbolTable table) {
        currentMethod = node.get("name");
        var method_type = node.getChild(0).get("name");
        String isArray = Boolean.parseBoolean(node.getChild(0).get("isArray"))?"_array":"";
        node.put("node_type", method_type +isArray);
        return null;
    }

    private Void visitNegationExpression(JmmNode node, SymbolTable table) {
        node.put("node_type", "boolean");
        return null;
    }

    private Void visitNewArrayExpression(JmmNode node, SymbolTable table) {
        node.put("node_type", "int_array");
        return null;
    }

    private Void visitInitArrayExpression(JmmNode node, SymbolTable table) {
        visit(node.getChild(0), table);
        var type = node.getChild(0).get("node_type");
        node.put("node_type", type + "_array");
        return null;
    }

    private Void visitNewObjectExpression(JmmNode node, SymbolTable table) {

        node.put("node_type", node.get("name"));

        return null;
    }

    private Void visitThis(JmmNode node, SymbolTable table) {
        node.put("node_type", table.getClassName());
        return null;
    }

    private Void visitMethodExpr(JmmNode node, SymbolTable table) {
        //Check if method belongs to object
        var object = node.getChild(0);
        for(var child: node.getChildren())
            visit(child, table);
        if(table.getImports().contains(object.get("node_type"))){
            node.put("node_type", "unknown");
            return null;
        }
        if (object.get("node_type").equals(table.getClassName())) {
            if(table.getSuper() != null){
                node.put("node_type", "unknown");
                return null;
            }
            if (table.getMethods().contains(node.get("name"))) {
                var return_type = table.getReturnType(node.get("name"));
                node.put("node_type", return_type.getName() + (return_type.isArray() ? "_array" : ""));
                return null;
            }
            String message = String.format("%s does not contain method %s.", object.get("node_type"), node.get("name"));
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null)
            );

        }
        return null;
    }



}
