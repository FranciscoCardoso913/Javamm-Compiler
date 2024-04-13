package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import static pt.up.fe.comp2024.ast.TypeUtils.getExprType;

public class Operations extends AnalysisVisitor {
    private String currentMethod = "foo";
    @Override
    protected void buildVisitor() {
        addVisit(Kind.PARENTH_EXPR, this::visitParenthExpression);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpression);
        addVisit(Kind.ARRAY_EXPR, this::visitArrayExpression);
        addVisit(Kind.INTEGER_LITERAL, this::visitIntegerLiteral );
        addVisit(Kind.BOOL_LITERAL, this::visitBooleanLiteral );
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRef);
    }
    private Void visitBinaryExpression(JmmNode node, SymbolTable table){
        String node_type = getExprType(node, table).getName();
        var left = node.getChild(0);
        var right = node.getChild(1);
        visit(left,table);
        visit(right,table);
        String message = "";
        if(node.get("op").equals( "<")){
            if(left.get("node_type").equals(right.get("node_type")) && left.get("node_type").equals("int")){
                node.put("node_type", node_type);
                return null;
            }else{
                message = String.format(
                        "Binary Expression of type '%s' expected two %s operands, instead got types %s %s",
                        node.get("op"),
                        "int",
                        left.get("node_type"),
                        right.get("node_type")
                );
            }
        }
        else if( left.get("node_type").equals(right.get("node_type")) && left.get("node_type").equals(node_type)){
                node.put("node_type", node_type);
                return null;
        }
        else {
            message = String.format(
                    "Binary Expression of type '%s' expected two %s operands, instead got types %s %s",
                    node.get("op"),
                    node_type,
                    left.get("node_type"),
                    right.get("node_type")
            );
        }
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(node),
                NodeUtils.getColumn(node),
                message,
                null)
        );
        node.put("node_type","null");
        return null;
    }

    private Void visitIntegerLiteral(JmmNode node, SymbolTable table){
        node.put("node_type", "int" );
        return null;
    }
    private Void visitBooleanLiteral(JmmNode node, SymbolTable table){
        node.put("node_type", "boolean" );
        return null;
    }
    private Void visitVarRef(JmmNode node, SymbolTable table){


        String varRefName = node.get("name");

        for(int i =0; i< table.getLocalVariables(currentMethod).size();i++){
            var variable = table.getLocalVariables(currentMethod).get(i);
            if(variable.getName().equals(varRefName)){
                node.put("node_type",variable.getType().getName());
                return null;
            }
        }
        for(int i =0; i< table.getParameters(currentMethod).size();i++){
            var variable = table.getParameters(currentMethod).get(i);
            if(variable.getName().equals(varRefName)){
                node.put("node_type",variable.getType().getName());
                return null;
            }
        }
        for(int i =0; i< table.getFields().size();i++){
            var variable = table.getFields().get(i);
            if(variable.getName().equals(varRefName)){
                node.put("node_type",variable.getType().getName());
                return null;
            }
        }
        return null;
    }

    private Void visitParenthExpression(JmmNode node, SymbolTable table){
        JmmNode child = node.getChild(0);
        visit(child,table);
        node.put("node_type",child.get("node_type"));
        return null;
    }

    private Void visitArrayExpression(JmmNode node, SymbolTable table){
        var left = node.getChild(0);
        var right = node.getChild(1);
        visit(left,table);
        visit(right,table);
        if(!right.get("node_type").equals("int")){
            String message = String.format(
                    "Array index must be of type int, got %s instead",
                    right.get("node_type")
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
        node.put("node_type",left.get("node_type"));
        return null;
    }

}
