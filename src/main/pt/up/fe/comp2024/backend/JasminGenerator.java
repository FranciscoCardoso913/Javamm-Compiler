package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;
    private final Map<String, String> classPathMap;

    List<Report> reports;

    String code;

    Method currentMethod;

    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        classPathMap = new HashMap<>();
        for (String importEntry : ollirResult.getOllirClass().getImports()) {
            String[] parts = importEntry.split("\\.");
            String simpleName = parts[parts.length - 1];
            classPathMap.put(simpleName, importEntry.replace('.', '/'));
        }

        // print all contents of classPathMap
        for (Map.Entry<String, String> entry : classPathMap.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }


        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Field.class, this::generateField);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(UnaryOpInstruction.class, this::generateUnaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(PutFieldInstruction.class, this::generatePutField);
        generators.put(GetFieldInstruction.class, this::generateGetField);
        generators.put(CallInstruction.class, this::generateCall);
    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        return code;
    }


    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();

        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        // TODO: could be other than public?
        code.append(".class public ").append(className).append(NL).append(NL);

        code.append(".super ");
        if (classUnit.getSuperClass() != null)
            code.append(classUnit.getSuperClass()).append(NL);
        else
            code.append("java/lang/Object").append(NL); //

        for (var field : ollirResult.getOllirClass().getFields()) {
            code.append(generators.apply(field));
        }

        StringBuilder defaultConstructor = new StringBuilder("""
                ;default constructor
                .method public <init>()V
                    aload_0
                    invokespecial""");
        defaultConstructor.append((" "));

        if (classUnit.getSuperClass() != null)
            defaultConstructor.append(classUnit.getSuperClass());
        else
            defaultConstructor.append("java/lang/Object");

        defaultConstructor.append("/<init>()V").append(NL);
        defaultConstructor.append("""
                    return
                .end method
                """);

        code.append(defaultConstructor);

        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                continue;
            }

            code.append(generators.apply(method));
        }

        return code.toString();
    }

    private String generateField(Field field) {
        var code = new StringBuilder();
        code.append(".field");
        switch (field.getFieldAccessModifier()) {
            case PUBLIC -> code.append(" public ");
            case PRIVATE -> code.append(" private ");
            case PROTECTED -> code.append(" protected ");
            case DEFAULT -> code.append(" ");
        }
        code.append(field.getFieldName()).append(" ");
        code.append(getType(field.getFieldType())).append(NL);
        return code.toString();
    }

    private String generateMethod(Method method) {

        // set method
        currentMethod = method;

        var code = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        // TODO: deal with final, constructors, etc
        if (method.isStaticMethod()) {
            modifier += "static ";
        }

        var methodName = method.getMethodName();

        // get params
        StringBuilder paramsTypes = new StringBuilder("(");
        for (var param : method.getParams()) {
            paramsTypes.append(getType(param.getType()));
        }
        paramsTypes.append(")");

        var retType = getType(method.getReturnType());
        code.append("\n.method ").append(modifier).append(methodName).append(paramsTypes).append(retType).append(NL);

        // Add limits
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
        }

        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

    private String getType(Type type) {

        ElementType elementType = type.getTypeOfElement();
        return switch (elementType) {
            case INT32 -> "I";
            case BOOLEAN -> "Z";
            case ARRAYREF -> // TODO: get type of arrau; next checkpoint?
                    "[Ljava/lang/String" + ";";
            case OBJECTREF -> {
                String className = ((ClassType) type).getName();
                String fullPath = classPathMap.getOrDefault(className, className);
                yield "L" + fullPath + ";";
            }
            case CLASS -> "L" + currentMethod.getClass().getName().toLowerCase() + ";";
            case THIS -> "L" + currentMethod.getOllirClass().getClassName() + ";";
            case STRING -> "Ljava/lang/String;";
            case VOID -> "V";
        };
    }

    private String generateGetField(GetFieldInstruction getFieldInstruction) {
        return "aload 0" + NL + // push this to stack
                "getfield " + currentMethod.getOllirClass().getClassName() + "/" +
                getFieldInstruction.getField().getName() + " " +
                getType(getFieldInstruction.getField().getType()) + NL;
    }

    private String generatePutField(PutFieldInstruction putFieldInstruction) {
        return "aload 0" + NL + generators.apply(putFieldInstruction.getValue()) +
                "putfield " + currentMethod.getOllirClass().getClassName() + "/" +
                putFieldInstruction.getField().getName() + " " +
                getType(putFieldInstruction.getField().getType()) +
                NL;
    }

    private String generateCall(CallInstruction callInstruction) {
        return switch (callInstruction.getInvocationType()) {
            case invokespecial -> generateInvokeSpecial(callInstruction);
            case invokestatic -> generateInvokeStatic(callInstruction);
            case invokevirtual -> generateInvokeVirtual(callInstruction);
            case NEW -> generateNew(callInstruction);
            default -> throw new NotImplementedException(callInstruction.getInvocationType());
        };
    }

    private String generateNew(CallInstruction callInstruction) {
        StringBuilder code = new StringBuilder();
        Type typeInstance = callInstruction.getCaller().getType();
        if (typeInstance instanceof ClassType classTypeInstance) {
            String className = classPathMap.getOrDefault(classTypeInstance.getName(), classTypeInstance.getName());
            code.append("new ").append(className).append(NL);
        } else {
            throw new NotImplementedException(typeInstance.getClass());
        }
        return code.toString();
    }

// invokevirtual
//  Utilizada para chamar métodos de instância não-privados,
//  não-estáticos e não-final (exceto construtores e métodos privados).
    private String generateInvokeVirtual(CallInstruction callInstruction) {
        StringBuilder code = new StringBuilder();
        Type typeInstance = callInstruction.getCaller().getType();
        if (typeInstance instanceof ClassType classTypeInstance) {
            String className = classPathMap.getOrDefault(classTypeInstance.getName(), classTypeInstance.getName());
            String methodName = getMethodName(callInstruction);

            code.append(generators.apply(callInstruction.getOperands().get(0)));

            for (Element arg : callInstruction.getArguments()) {
                code.append(generators.apply(arg));
            }

            code.append("invokevirtual ")
                    .append(className).append("/")
                    .append(methodName)
                    .append("(")
                    .append(callInstruction.getArguments().stream()
                            .map(arg -> getType(arg.getType()))
                            .collect(Collectors.joining()))
                    .append(")");
            code.append(getType(callInstruction.getReturnType())).append(NL);

        } else if (typeInstance instanceof ArrayType arrayTypeInstance) {
            throw new NotImplementedException(arrayTypeInstance);
        }
        return code.toString();
    }

    private String generateInvokeStatic(CallInstruction callInstruction) {
        StringBuilder code = new StringBuilder();
        if (callInstruction.getCaller() instanceof Operand operand) {

            String className = classPathMap.getOrDefault(operand.getName(), operand.getName());

            String methodName = getMethodName(callInstruction);

            for (Element arg : callInstruction.getArguments()) {
                code.append(generators.apply(arg));
            }

            code.append("invokestatic ")
                    .append(className).append("/")
                    .append(methodName)
                    .append("(")
                    .append(callInstruction.getArguments().stream()
                            .map(arg -> getType(arg.getType()))
                            .collect(Collectors.joining()))
                    .append(")");
            code.append(getType(callInstruction.getReturnType())).append(NL);
        } else {
            throw new NotImplementedException(callInstruction.getCaller().getClass());
        }
        return code.toString();
    }

    private String generateInvokeSpecial(CallInstruction callInstruction) {
        StringBuilder code = new StringBuilder();
        Type typeInstance = callInstruction.getCaller().getType();
        if (typeInstance instanceof ClassType classTypeInstance) {
            String className = classPathMap.getOrDefault(classTypeInstance.getName(), classTypeInstance.getName());
            String methodName = getMethodName(callInstruction);
            code.append(generators.apply(callInstruction.getOperands().get(0)));

            for (Element arg : callInstruction.getArguments()) {
                code.append(generators.apply(arg));
            }
            code.append("invokespecial ")
                    .append(className).append("/")
                    .append(methodName).append("(")
                    .append(callInstruction.getArguments().stream()
                            .map(arg -> getType(arg.getType()))
                            .collect(Collectors.joining()))
                    .append(")")
                    .append(getType(callInstruction.getReturnType())).append(NL);
        } else {
            throw new NotImplementedException(typeInstance.getClass());
        }
        return code.toString();
    }


    private String getMethodName(CallInstruction callInstruction) {
        return callInstruction.getMethodNameTry()
                .map(methodElement -> {
                    if (methodElement instanceof LiteralElement) {
                        String literal = ((LiteralElement) methodElement).getLiteral();
                        if (literal.startsWith("\"") && literal.endsWith("\"") && literal.length() > 1) {
                            return literal.substring(1, literal.length() - 1);
                        }
                        return literal;
                    }
                    return methodElement.toString();
                })
                .orElse("<init>");
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand operand)) {
            System.out.println("lhs: " + lhs.getClass() + " is not an Operand");
            throw new NotImplementedException(lhs.getClass());
        }

        // get register
        var name = currentMethod.getVarTable().get(operand.getName());
        var reg = name.getVirtualReg();

        switch (name.getVarType().getTypeOfElement()) {
            case INT32, BOOLEAN -> code.append("istore ").append(reg).append(NL);
            case OBJECTREF -> code.append("astore ").append(reg).append(NL);
            default -> throw new NotImplementedException(name.getVarType().getTypeOfElement());
        }

        return code.toString();
    }

    private String generateLiteral(LiteralElement literal) {
        // TODO: iconst here?
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        var code = new StringBuilder();
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        switch (currentMethod.getVarTable().get(operand.getName()).getVarType().getTypeOfElement()) {
            case INT32, BOOLEAN -> code.append("iload ").append(reg).append(NL);
            case OBJECTREF -> code.append("aload ").append(reg).append(NL);
            case THIS -> code.append("aload 0").append(NL);
            default -> throw new NotImplementedException(currentMethod.getVarTable().get(operand.getName()).getVarType().getTypeOfElement());
        }
        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }


    private String generateUnaryOp(UnaryOpInstruction unaryOpInstruction) {
        var code = new StringBuilder();

        code.append(generators.apply(unaryOpInstruction.getOperand()));

        var op = switch (unaryOpInstruction.getOperation().getOpType()) {
            case NOTB -> "ixor";
            case NOT -> "ineg";
            default -> {
                System.out.println("Not a single operation: " + unaryOpInstruction.getOperation());
                throw new NotImplementedException(unaryOpInstruction.getOperation().getOpType());
            }
        };

        code.append(op).append(NL);
        return code.toString();
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation

        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "iadd";
            case SUB -> "isub";
            case MUL -> "imul";
            case DIV -> "idiv";
            case SHR -> "ishr"; // signed shift right
            case SHL -> "ishl"; // signed shift left
            case SHRR -> "iushr"; // unsigned shift right, logical
            case XOR -> "ixor";
            case AND, ANDB -> "iand";
            case OR, ORB -> "ior";
            case LTH -> "if_icmplt"; // TODO: fix it
            case GTH -> "if_icmpgt";
            case EQ -> "if_icmpeq";
            case NEQ -> "if_icmpne";
            case LTE -> "if_icmple";
            case GTE -> "if_icmpge";
            default -> {
                System.out.println("Not a binary operation: " + binaryOp.getOperation().getOpType());
                throw new NotImplementedException(binaryOp.getOperation().getOpType());
            }
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        ElementType type = returnInst.getReturnType().getTypeOfElement();
        switch (type) {
            case INT32, BOOLEAN -> {
                code.append(generators.apply(returnInst.getOperand()));
                code.append("ireturn").append(NL);
            }
            case VOID -> code.append("return").append(NL);
            default -> throw new NotImplementedException(type);
        }

        return code.toString();
    }

}
