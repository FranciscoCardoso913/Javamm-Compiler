package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.Instruction;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
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

    public static String toOllirType(JmmNode node) {
        String[] type = node.get("node_type").split("_");
        return toOllirType(type[0], type.length > 1);
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
            case "void" -> "V";
            default -> typeName;
        };

        return type;
    }

    public static String getOllirMethod(JmmNode node, SymbolTable table) {
        // TODO: Need to add "recursion" to get object name if it is temporary value
        // TODO: Visit the expr first, name is result.getCode(). Pass this value as a parameter or deal with this in visitor
        StringBuilder code = new StringBuilder();

        if (node.isInstance(THIS)) {
            code.append(VIRTUAL_FUNC).append("(this");
        } else {
            String objName = node.get("name");
            boolean isStatic = NodeUtils.isImported(objName, table) || objName.equals(table.getClassName());
            String funcType = isStatic ? STATIC_FUNC : VIRTUAL_FUNC;

            code.append(funcType).append("(").append(objName);
            if (funcType.equals(VIRTUAL_FUNC))
                code.append(OptUtils.toOllirType(node));
        }

        return code.toString();
    }
}
