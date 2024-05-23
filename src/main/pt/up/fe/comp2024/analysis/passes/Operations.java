package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static pt.up.fe.comp2024.ast.TypeUtils.getExprOperands;
import static pt.up.fe.comp2024.ast.TypeUtils.getExprType;

public class Operations extends AnalysisVisitor {
    @Override
    protected void buildVisitor() {
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpression);
        addVisit(Kind.NEG_EXPR, this::visitNegationExpression);
    }

    private Void visitBinaryExpression(JmmNode node, SymbolTable table) {
        String op_type = getExprOperands(node, table).getName();
        var left = node.getChild(0);
        var right = node.getChild(1);
        List<String> validTypes = new ArrayList<>(Arrays.asList(op_type, "unknown"));
        if(validTypes.contains(left.get("node_type")) && validTypes.contains(right.get("node_type")))
            return null;

        String message = String.format(
                "Expected both operands of type %s, got %s and %s instead",
                op_type,
                left.get("node_type"),
                right.get("node_type")
        );
        addSemanticReport(node, message);
        return null;
    }

    private Void visitNegationExpression(JmmNode node, SymbolTable table) {
        var expr = node.getChild(0);
        if (expr.get("node_type").equals("boolean")) {
            return null;
        } else {
            addSemanticReport(node, String.format(
                    "Negation requires operand of type boolean, got %s instead.",
                    expr.get("node_type")
                    )
            );
        }
        return null;
    }















}
