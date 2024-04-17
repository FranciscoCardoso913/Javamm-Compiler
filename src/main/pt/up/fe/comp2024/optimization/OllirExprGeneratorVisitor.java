package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */

public class OllirExprGeneratorVisitor extends AJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private static final String END_STMT = ";\n";
    private static final String NEW = "new";
    private static final String L_BRACKET = "(";
    private static final String R_BRACKET = ")";
    private static final String COMMA = ",";
    private static final String INIT = "\"<init>\"";
    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(NEG_EXPR, this::visitNegExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(BOOL_LITERAL, this::visitBoolean);
        addVisit(METHOD_EXPR, this::visitMethodExpr);
        addVisit(NEW_OBJ_EXPR, this::visitNewObjExpr);
        addVisit(THIS, this::visitThis);

        setDefaultVisit(this::defaultVisit);
    }


    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        Type intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitBoolean(JmmNode node, Void unused) {
        Type boolType = new Type(TypeUtils.getBoolTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(boolType);
        String ollirValue = node.get("value").equals("true") ? "1" : "0";

        String code = ollirValue + ollirIntType;
        return new OllirExprResult(code);
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {
        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        String resOllirType = OptUtils.toOllirType(node);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        computation.append(node.get("op")).append(OptUtils.toOllirType(node)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitNegExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        JmmNode exprNode = node.getChild(0);

        OllirExprResult exprResult = visit(exprNode);

        String nextTmp = OptUtils.getTemp();
        String exprType = OptUtils.toOllirType(exprNode);
        code.append(nextTmp).append(exprType);
        computation.append(exprResult.getComputation());
        computation.append(code).append(SPACE).append(ASSIGN).append(exprType).append(SPACE)
                .append("!").append(exprType).append(SPACE).append(exprResult.getCode()).append(END_STMT);

        return new OllirExprResult(code.toString(), computation.toString());
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        Optional<JmmNode> method = node.getAncestor(METHOD_DECL);

        String id = node.get("name");

        if (method.isPresent() && NodeUtils.isFieldRef(id, table, method.get().get("name")))
            return buildGetField(node);

        return buildCommonField(node);
    }

    private OllirExprResult buildGetField(JmmNode node) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        String varType = OptUtils.toOllirType(node);

        String test = OptUtils.getTemp();
        code.append(test).append(varType);
        computation.append(code).append(SPACE).append(ASSIGN).append(varType).append(SPACE).append("getfield(this, ")
                .append(node.get("name")).append(varType).append(")").append(varType).append(END_STMT);

        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult buildCommonField(JmmNode node) {
        StringBuilder code = new StringBuilder();
        // TODO: Maybe annotate the information below?
        Optional<JmmNode> method = node.getAncestor(METHOD_DECL);
        Optional<JmmNode> returnStmt = node.getAncestor(RETURN_STMT);

        String id = node.get("name");

        // TODO: Maybe annotate node to know if it param?
        // TODO: This is extra as it only adds the $, which isn't mandatory
        if (method.isPresent() && returnStmt.isEmpty()) {
            String methodName = method.get().get("name");
            List<Symbol> params = table.getParameters(methodName);

            for (int i = 1; i <= params.size(); i++) {
                if (params.get(i - 1).getName().equals(id)) {
                    // TODO: Check if '$' makes the tests be wrong or have problems with jasmin
                    code.append("$").append(i).append(".");
                    break;
                }
            }
        }

        code.append(id);

        // TODO: Remove when tree is fully annotated, that is, imports are annotated with empty string
        if (!NodeUtils.isImported(id, table))
            code.append(OptUtils.toOllirType(node));

        return new OllirExprResult(code.toString());
    }

    private OllirExprResult visitMethodExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        List<String> tmpVars = new ArrayList<>();

        // Visit params as they are expressions as well
        for (int i = 1; i < node.getChildren().size(); i++) {
            JmmNode param = node.getChild(i);
            OllirExprResult res = visit(param);
            computation.append(res.getComputation());
            tmpVars.add(res.getCode());
        }

        // visit lhs expr to get its ollir representation
        var object = visit(node.getChild(0));
        computation.append(object.getComputation());

        String ollirMethod = OptUtils.getOllirMethod(node.getChild(0), table, object.getCode());
        String methodName = node.get("name");
        String returnType = OptUtils.toOllirType(node);

        if (!returnType.equals(".V")) {
            String tmpVar = OptUtils.getTemp();
            code.append(tmpVar).append(returnType);
            computation.append(tmpVar).append(returnType).append(SPACE).append(ASSIGN)
                    .append(returnType).append(SPACE);
        }

        computation.append(ollirMethod);

        computation.append(", ").append("\"").append(methodName).append("\"");

        for (String tmpVar: tmpVars) {
            computation.append(", ").append(tmpVar);
        }

        computation.append(")").append(returnType).append(END_STMT);


        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitNewObjExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        String nextTemp = OptUtils.getTemp();
        String objectClass = node.get("name");
        String exprType = "." + objectClass;

        computation.append(nextTemp).append(exprType).append(SPACE).append(ASSIGN).append(exprType).append(SPACE);
        computation.append(NEW).append(L_BRACKET).append(objectClass).append(R_BRACKET).append(exprType).append(END_STMT);
        computation.append("invokespecial").append(L_BRACKET).append(nextTemp).append(exprType).append(COMMA)
                .append(SPACE).append(INIT).append(R_BRACKET).append(".V").append(END_STMT);

        code.append(nextTemp).append(exprType);

        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitThis(JmmNode node, Void unused) {
        return new OllirExprResult("this." + node.get("node_type"));
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        for (var child : node.getChildren()) {
            OllirExprResult res = visit(child);
            code.append(res.getCode());
            computation.append(res.getComputation());
        }

        return new OllirExprResult(code.toString(), computation.toString());
    }

}
