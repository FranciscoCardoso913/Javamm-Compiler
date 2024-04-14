package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.Instruction;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.List;
import java.util.MissingFormatArgumentException;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.*;

public class OptUtils {
    private static int tempNumber = -1;

    private final static String VIRTUAL_FUNC = "invokevirtual";
    private final static String STATIC_FUNC = "invokestatic";
    private final static String SPECIAL_FUNC = "invokespecial";

    public static String getTemp() {

        return getTemp("tmp");
    }

    public static String getTemp(String prefix) {

        return prefix + getNextTempNum();
    }

    public static int getNextTempNum() {

        tempNumber += 1;
        return tempNumber;
    }

    public static String toOllirType(JmmNode typeNode) {
        if (!Kind.fromString(typeNode.getKind()).isType())
            throw new RuntimeException("Node '" + typeNode + "' is not a type");
        String typeName = typeNode.get("name");

        return toOllirType(typeName, NodeUtils.getBooleanAttribute(typeNode, "isArray", "false"));
    }

    public static String toOllirType(Type type) {
        return toOllirType(type.getName(), type.isArray());
    }

    private static String toOllirType(String typeName, boolean isArray) {
        String type = (isArray ? ".array" : "");

        type = type + "." + switch (typeName) {
            case "int" -> "i32";
            case "boolean" -> "bool";
            case "String" -> "String";
            default -> typeName;
        };

        return type;
    }

    public static String getOllirMethod(JmmNode node) {
        StringBuilder code = new StringBuilder();

        if (node.isInstance(THIS)) {
            code.append(VIRTUAL_FUNC).append("(this");
        } else {
            code.append(STATIC_FUNC).append("(").append(node.get("name"));
        }

        return code.toString();
    }

    // TODO: Remove this when tree is annotated
    public static Type getAssignType(JmmNode assignNode, SymbolTable table) {
        if (assignNode.getAncestor(METHOD_DECL).isEmpty())
            return null;

        JmmNode methodNode = assignNode.getAncestor(METHOD_DECL).get();
        String methodName = methodNode.get("name");
        String varName = assignNode.get("name");

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
}
