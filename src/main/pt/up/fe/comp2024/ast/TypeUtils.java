package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static pt.up.fe.comp2024.ast.Kind.METHOD_DECL;
import static pt.up.fe.comp2024.ast.Kind.THIS;

public class TypeUtils {
    private static Pattern  array_pattern = Pattern.compile("([a-zA-Z0-9]+)(_array)?(_ellipse)?");
    private static final String INT_TYPE_NAME = "int";
    private static final String BOOL_TYPE_NAME = "boolean";

    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }

    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded

        var kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case VAR_REF_EXPR -> getVarExprType(expr, table);
            case INTEGER_LITERAL -> new Type(INT_TYPE_NAME, false);
            case METHOD_EXPR -> getMethodExprType(expr, table);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
    }

    public static Type getExprOperands(JmmNode expr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded

        String operator = expr.get("op");

        Type type = switch (operator) {
            case "+", "*","-", "/", "<" -> new Type(INT_TYPE_NAME, false);
            case "&&", "!" -> new Type(BOOL_TYPE_NAME, false);
            default -> throw new UnsupportedOperationException("Expression does not contain operands");
        };

        return type;
    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        // TODO: Simple implementation that needs to be expanded

        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "*","-", "/" -> new Type(INT_TYPE_NAME, false);
            case "&&", "<", "!" -> new Type(BOOL_TYPE_NAME, false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }

    // TODO: Remove this after having the annotated tree
    private static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {
        if (varRefExpr.getAncestor(METHOD_DECL).isEmpty())
            return null;

        JmmNode methodNode = varRefExpr.getAncestor(METHOD_DECL).get();
        String methodName = methodNode.get("name");
        String varName = varRefExpr.get("name");

        for (Symbol param: table.getParameters(methodName)) {
            if (param.getName().equals(varName))
                return param.getType();
        }

        for (Symbol local: table.getLocalVariables(methodName)) {
            if (local.getName().equals(varName))
                return local.getType();
        }

        for (Symbol field: table.getFields()) {
            if (field.getName().equals(varName))
                return field.getType();
        }

        return null;
    }

    private static Type getMethodExprType(JmmNode node, SymbolTable table) {
        String methodName = node.get("name");

        if (node.getChild(0).isInstance(THIS)) {
            return table.getReturnType(methodName);
        }
        else {
            return new Type("unknown", false);
        }
    }


    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(String sourceType, String destinationType, SymbolTable table) {
        return sourceType.equals(destinationType)
                || sourceType.equals("unknown")
                || (sourceType.equals(table.getClassName()) && destinationType.equals(table.getSuper()))
                || table.getImports().contains(sourceType);
    }

    public static boolean isEllipse (String type){
        Matcher matcher = array_pattern.matcher(type);
        return (matcher.find() && matcher.group(3) != null);
    }
    public static boolean isArray (String type){
        Matcher matcher = array_pattern.matcher(type);
        return (matcher.find() && matcher.group(2) != null);
    }
    public static String  getType( Type a){
        String isArray = a.isArray()?"_array":"";
        return  a.getName() + isArray;
    }
}
