package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

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
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(METHOD_EXPR, this::visitMethodExpr);
        addVisit(NEW_OBJ_EXPR, this::visitNewObjExpr);

        setDefaultVisit(this::defaultVisit);
    }


    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
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


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        // TODO: Change this whole method when the AST is annotated
        StringBuilder code = new StringBuilder();
        Optional<JmmNode> method = node.getAncestor(METHOD_DECL);
        Optional<JmmNode> returnStmt = node.getAncestor(RETURN_STMT);

        String id = node.get("name");

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

        String ollirType = OptUtils.toOllirType(node);
        code.append(id).append(ollirType);

        return new OllirExprResult(code.toString());
    }

    private OllirExprResult visitMethodExpr(JmmNode node, Void unused) {
        // TODO: Refactor this method
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        String ollirMethod = OptUtils.getOllirMethod(node.getChild(0), table);
        String methodName = node.get("name");
        String returnType;

        if (table.getReturnType(methodName) == null) {
            // TODO: With the annotated tree the return type isn't simply void.
            returnType = ".V";
        }
        else {
            String tmpVar = OptUtils.getTemp();
            returnType = OptUtils.toOllirType(table.getReturnType(methodName));
            code.append(tmpVar).append(returnType);
            computation.append(tmpVar).append(returnType).append(SPACE).append(ASSIGN)
                    .append(returnType).append(SPACE);
        }

        computation.append(ollirMethod);

        computation.append(", ").append("\"").append(methodName).append("\"");

        for (int i = 1; i < node.getChildren().size(); i++) {
            JmmNode param = node.getChild(i);
            OllirExprResult res = visit(param);
            computation.append(", ").append(res.getCode());
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

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

}
