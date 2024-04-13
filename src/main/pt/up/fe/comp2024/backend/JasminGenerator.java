package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.lang.constant.ConstantDesc;
import java.util.ArrayList;
import java.util.List;
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

    List<Report> reports;

    String code;

    Method currentMethod;

    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Field.class, this::generateField);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp); //
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand); //
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
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
        return switch (type.getTypeOfElement()) {
            case INT32 -> "I";
            case BOOLEAN -> "Z";
            case ARRAYREF -> // TODO: get type of arrau; next checkpoint?
                    "[Ljava/lang/String" + ";";
            case OBJECTREF, CLASS -> "L" + currentMethod.getClass().getName().toLowerCase() + ";";
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
        var code = new StringBuilder();
        switch (callInstruction.getInvocationType()) {
            case invokespecial -> {
                // might have more than one operand?
                Type typeInstance = callInstruction.getCaller().getType();
                if (typeInstance instanceof ClassType) {
                    ClassType classTypeInstance = (ClassType) typeInstance;
                    String name = classTypeInstance.getName();
                    code.append(generators.apply(callInstruction.getOperands().get(0)));
                    code.append("invokespecial ").append(name).append("/<init>()V").append(NL);
                } else {
                    throw new NotImplementedException(typeInstance.getClass());
                }
            }
            case NEW -> {
                Type typeInstance = callInstruction.getCaller().getType();
                if (typeInstance instanceof ClassType) {
                    ClassType classTypeInstance = (ClassType) typeInstance;
                    String name = classTypeInstance.getName();
                    code.append("new ").append(name).append(NL);
                } else {
                    throw new NotImplementedException(typeInstance.getClass());
                }
            }
            default -> throw new NotImplementedException(callInstruction.getInvocationType());
        }
        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {
            System.out.println("lhs: " + lhs.getClass() + " is not an Operand");
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;

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

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
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
            default -> throw new NotImplementedException(currentMethod.getVarTable().get(operand.getName()).getVarType().getTypeOfElement());
        }
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
            case MUL -> "imul";
            default -> {
                System.out.println("Operation not implemented: " + binaryOp.getOperation().getOpType());
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
