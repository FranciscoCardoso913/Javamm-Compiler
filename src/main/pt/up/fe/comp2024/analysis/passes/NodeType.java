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

import static pt.up.fe.comp2024.ast.NodeUtils.isImported;
import static pt.up.fe.comp2024.ast.TypeUtils.getExprType;

public class NodeType extends AnalysisVisitor {

    Pattern array_pattern = Pattern.compile("([a-zA-Z0-9]+)(\narray)?(\nellipse)?");

    @Override
    protected void buildVisitor() {
        addVisit(Kind.LIST_ASSIGN_STMT,this::visitAssignStatement);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStatement);
        addVisit(Kind.PARAM, this::visitParam);
        addVisit(Kind.VAR_DECL, this::visitVarDeclaration);
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
        String message = NodeUtils.getLocalVariableType(varRefName, currentMethod, table) ;
        if(message==null) {
            addSemanticReport(node, "Field "+ node.get("name") + " cannot be accessed");
            node.put("node_type", "undefined");
            return null;
        }
        node.put("node_type",message );
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

        if( Boolean.parseBoolean(node.get("isMain")) && !node.getDescendants(Kind.THIS).isEmpty() ){
            addSemanticReport(node, "this nuts");
        }
        var method_type = node.getChild(0).get("name");
        String isArray = Boolean.parseBoolean(node.getChild(0).get("isArray"))?"\narray":"";
        node.put("node_type", method_type +isArray);
        return null;
    }

    private Void visitNegationExpression(JmmNode node, SymbolTable table) {
        node.put("node_type", "boolean");
        return null;
    }

    private Void visitNewArrayExpression(JmmNode node, SymbolTable table) {
        node.put("node_type", "int\narray");
        return null;
    }

    private Void visitInitArrayExpression(JmmNode node, SymbolTable table) {

        if(node.getChildren().isEmpty()){

            node.put("node_type",  "int\narray");

            return null;
        }

        visit(node.getChild(0), table);

        var type = node.getChild(0).get("node_type");
        node.put("node_type", type + "\narray");
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
        if(isImported(object.get("node_type"),table) || object.get("node_type").equals("unknown")){
            node.put("node_type", "unknown");
            return null;
        }
        if (object.get("node_type").equals(table.getClassName())) {

            if (table.getMethods().contains(node.get("name"))) {
                var return_type = table.getReturnType(node.get("name"));
                node.put("node_type", return_type.getName() + (return_type.isArray() ? "\narray" : ""));
                return null;
            }
            if(table.getSuper() != null){

                node.put("node_type", "unknown");
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
        node.put("node_type", "undefined");
        return null;
    }
    private Void visitVarDeclaration(JmmNode node, SymbolTable table){
        var type = node.getChild(0);
        List<String> validTypes = new ArrayList<>(Arrays.asList("int", "boolean", "String", table.getClassName()));

        List<String> imports = table.getImports();
        if (imports != null) {
            for (String imported_path : imports) {
                String[] parts = imported_path.split("\\.");
                String lastPart = parts[parts.length - 1];
                validTypes.add(lastPart);
            }
        }



        if(!validTypes.contains(type.get("name")))addSemanticReport(node, "Invalid type");
        else if( Boolean.parseBoolean(type.get("isEllipse"))) addSemanticReport(node, "Variables cannot be declared as ellipses");
        else if( type.get("name").equals("void")) addSemanticReport(node, "Variables cannot be declared as void");
        else node.put("node_type", type.get("name") + (Boolean.parseBoolean(type.get("isArray"))?"\narray":"") );
        return null;
    }

    private Void visitParam(JmmNode node, SymbolTable table){
        var type = node.getChild(0);
        if( type.get("name").equals("void")) addSemanticReport(node, "Parameters cannot be declared as void");
        else node.put(
                "node_type",
                type.get("name") +
                        (Boolean.parseBoolean(type.get("isArray"))?"\narray":"") +
                        (Boolean.parseBoolean(type.get("isEllipse"))?"\nellipse":"")
                );
        return null;
    }

    private Void visitAssignStatement(JmmNode node ,SymbolTable table){
        var variable = node.get("name");
        String variable_type =  NodeUtils.getLocalVariableType(variable, currentMethod, table);
        if(variable_type == null) {
            addSemanticReport(node, "Static method cannot use non static fields");
            node.put("node_type", "undefined");
        }
        else
            node.put("node_type", variable_type);
        return null;
    }




}
