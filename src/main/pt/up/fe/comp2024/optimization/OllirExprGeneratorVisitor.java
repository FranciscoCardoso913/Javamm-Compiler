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
    private final String END_LABEL = ":\n";
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
        addVisit(LENGTH_ATTR_EXPR, this::visitLengthAttrExpr);
        addVisit(ARRAY_EXPR, this::visitArrayExpr);
        addVisit(NEW_ARRAY_EXPR, this::visitNewArrayExpr);
        addVisit(INIT_ARRAY_EXPR, this::visitInitArrayExpr);

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
        String ollirBoolType = OptUtils.toOllirType(boolType);
        String ollirValue = node.get("value").equals("true") ? "1" : "0";

        String code = ollirValue + ollirBoolType;
        return new OllirExprResult(code);
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {
        if (node.get("op").equals("&&"))
            return visitShortCircuitAnd(node);
        return visitRegularBinExpr(node);
    }

    private OllirExprResult visitRegularBinExpr(JmmNode node) {
        OllirExprResult lhs = visit(node.getJmmChild(0));
        OllirExprResult rhs = visit(node.getJmmChild(1));

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

    private OllirExprResult visitShortCircuitAnd (JmmNode node) {
        int andIdx = OptUtils.getNextAndNumber();
        final String AND_RHS_LABEL = "AND_RHS_" + andIdx;
        final String AND_END_LABEL = "AND_END_" + andIdx;

        OllirExprResult lhs = visit(node.getJmmChild(0));
        OllirExprResult rhs = visit(node.getJmmChild(1));
        String resOllirType = OptUtils.toOllirType(node);
        String code = OptUtils.getTemp() + resOllirType;

        StringBuilder computation = new StringBuilder();

        computation.append(lhs.getComputation());

        // short-circuit LHS evaluation
        computation.append("if (").append(lhs.getCode()).append(") goto ").append(AND_RHS_LABEL).append(END_STMT);
        computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE).append("0.bool").append(END_STMT);
        computation.append("goto ").append(AND_END_LABEL).append(END_STMT);

        // short-citcuit RHS evaluation
        computation.append(AND_RHS_LABEL).append(END_LABEL);
        computation.append(rhs.getComputation());
        computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE).append(rhs.getCode()).append(END_STMT);
        computation.append(AND_END_LABEL).append(END_LABEL);

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitNegExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        JmmNode exprNode = node.getChild(0);

        OllirExprResult exprResult = visit(exprNode);

        String exprType = OptUtils.toOllirType(exprNode);
        code.append(OptUtils.getTemp()).append(exprType);
        computation.append(exprResult.getComputation());
        computation.append(code).append(SPACE).append(ASSIGN).append(exprType).append(SPACE)
                .append("!").append(exprType).append(SPACE).append(exprResult.getCode()).append(END_STMT);

        return new OllirExprResult(code.toString(), computation.toString());
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        Optional<JmmNode> method = node.getAncestor(METHOD_DECL);

        String id = node.get("name");

        if (NodeUtils.isFieldRef(id, table, method.get().get("name")))
            return buildGetField(node);

        return buildCommonField(node);
    }

    private OllirExprResult buildGetField(JmmNode node) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        String varType = OptUtils.toOllirType(node);

        code.append(OptUtils.getTemp()).append(varType);
        computation.append(code).append(SPACE).append(ASSIGN).append(varType).append(SPACE).append("getfield(this, ")
                .append(node.get("name")).append(varType).append(")").append(varType).append(END_STMT);

        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult buildCommonField(JmmNode node) {
        StringBuilder code = new StringBuilder();
        Optional<JmmNode> method = node.getAncestor(METHOD_DECL);
        Optional<JmmNode> returnStmt = node.getAncestor(RETURN_STMT);

        String id = node.get("name");

        // This is extra as it only adds the $ before the use of paramaters, which isn't mandatory
        if (returnStmt.isEmpty()) {
            String methodName = method.get().get("name");
            List<Symbol> params = table.getParameters(methodName);

            for (int i = 1; i <= params.size(); i++) {
                if (params.get(i - 1).getName().equals(id)) {
                    code.append("$").append(i).append(".");
                    break;
                }
            }
        }

        code.append(id);

        if (!NodeUtils.isImported(id, table))
            code.append(OptUtils.toOllirType(node));

        return new OllirExprResult(code.toString());
    }

    private OllirExprResult visitMethodExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        List<String> tmpVars = new ArrayList<>();
        int nParams = -1;
        int nArguments = node.getChildren().size() - 1;
        boolean isLastParamEllipsis = false;
        String ellipsisArrayTmp = "";

        // visit lhs expr to get its ollir representation
        var object = visit(node.getChild(0));
        computation.append(object.getComputation());

        String ollirMethod = OptUtils.getOllirMethod(table, object.getCode());
        String methodName = node.get("name");
        String returnType = OptUtils.toOllirType(node);


        // Visit params as they are expressions as well
        for (int i = 1; i < node.getChildren().size(); i++) {
            JmmNode param = node.getChild(i);
            OllirExprResult res = visit(param);
            computation.append(res.getComputation());
            tmpVars.add(res.getCode());
        }

        if (!NodeUtils.isImported(object.getCode(), table) && !NodeUtils.isImported(OptUtils.getOllirBaseType(object.getCode()), table) && !NodeUtils.isImported(OptUtils.getOllirBaseType(object.getCode()), table) && !table.getParameters(methodName).isEmpty()) {
            List<Symbol> methodParams = table.getParameters(methodName);
            nParams = methodParams.size();
            Symbol lastParam = methodParams.get(nParams - 1);
            isLastParamEllipsis = lastParam.getType().getObject("isEllipse", Boolean.class);

            if (isLastParamEllipsis && (tmpVars.isEmpty() || !(OptUtils.isOllirArray(tmpVars.get(nArguments - 1)) && nArguments == nParams) )) {
                int newListSize = nArguments - nParams + 1;
                OllirExprResult newList = generateOllirArray(OptUtils.toOllirType(lastParam.getType()), newListSize);
                ellipsisArrayTmp = newList.getCode();
                String arrayOllirType = "." + OptUtils.getOllirBaseType(ellipsisArrayTmp);
                computation.append(newList.getComputation());

                for (int i = 0; i < newListSize; i++) {
                    computation.append(ellipsisArrayTmp).append("[").append(i).append(".i32]").append(arrayOllirType)
                            .append(SPACE).append(ASSIGN).append(arrayOllirType).append(SPACE)
                            .append(tmpVars.get(i + nParams - 1)).append(END_STMT);
                }
            }
            else
                isLastParamEllipsis = false;
        }

        if (!returnType.equals(".V") && !node.getParent().isInstance(EXPR_STMT)) {
            String tmpVar = OptUtils.getTemp();
            code.append(tmpVar).append(returnType);
            computation.append(code).append(SPACE).append(ASSIGN)
                    .append(returnType).append(SPACE);
        }

        computation.append(ollirMethod);

        computation.append(", ").append("\"").append(methodName).append("\"");

        if (isLastParamEllipsis) {
            for (int i = 0; i < nParams - 1; i++) {
                computation.append(", ").append(tmpVars.get(i));
            }
            computation.append(", ").append(ellipsisArrayTmp);
        }
        else {
            for (String tmpVar : tmpVars) {
                computation.append(", ").append(tmpVar);
            }
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

    private OllirExprResult visitLengthAttrExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        OllirExprResult exprRes = visit(node.getChild(0));
        String intTypeOllir = OptUtils.toOllirType(new Type("int", false));

        code.append(OptUtils.getTemp()).append(intTypeOllir);
        computation.append(exprRes.getComputation());
        computation.append(code).append(SPACE).append(ASSIGN).append(intTypeOllir).append(SPACE).append("arraylength(")
                .append(exprRes.getCode()).append(")").append(intTypeOllir).append(END_STMT);

        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitArrayExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        String ollirType = OptUtils.toOllirType(node);

        OllirExprResult arrayExpr = visit(node.getChild(0));
        OllirExprResult arrayIdx = visit(node.getChild(1));

        String nextTmp = OptUtils.getTemp();
        code.append(nextTmp).append(ollirType);
        computation.append(arrayExpr.getComputation()).append(arrayIdx.getComputation());
        computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE);
        computation.append(OptUtils.removeOllirType(arrayExpr.getCode())).append("[").append(arrayIdx.getCode()).append("]").append(ollirType)
                .append(END_STMT);


        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitNewArrayExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        String ollirType = OptUtils.toOllirType(node);

        OllirExprResult exprRes = visit(node.getChild(0));
        code.append(OptUtils.getTemp()).append(ollirType);
        computation.append(exprRes.getComputation());
        computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE);
        computation.append("new(array, ").append(exprRes.getCode()).append(")").append(ollirType).append(END_STMT);

        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitInitArrayExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        String nextTmp = OptUtils.getTemp();
        String ollirType = OptUtils.toOllirType(node);
        String arrayOllirType = OptUtils.toOllirType(node, false);
        List<JmmNode> arrExprs = node.getChildren();

        code.append(nextTmp).append(ollirType);

        // Create new array in temporary variable
        computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE);
        computation.append("new(array, ").append(arrExprs.size()).append(".i32)").append(ollirType).append(END_STMT);

        // Store elements of array initializer in the array that was created
        for (int i = 0; i < arrExprs.size(); i++) {
            OllirExprResult exprRes = visit(arrExprs.get(i));
            computation.append(exprRes.getComputation());
            computation.append(nextTmp).append("[").append(i).append(".i32]").append(arrayOllirType).append(SPACE).append(ASSIGN)
                    .append(arrayOllirType).append(SPACE).append(exprRes.getCode()).append(END_STMT);
        }

        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult generateOllirArray(String ollirType, int elems) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        code.append(OptUtils.getTemp()).append(ollirType);
        computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE);
        computation.append("new(array, ").append(elems).append(".i32)").append(ollirType).append(END_STMT);

        return new OllirExprResult(code.toString(), computation.toString());
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
