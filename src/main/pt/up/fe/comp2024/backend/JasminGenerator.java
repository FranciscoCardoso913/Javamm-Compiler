package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.*;
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
    private boolean shouldPop = true;

    List<Report> reports;

    String code;

    Method currentMethod;
    int maxStack;
    int currentStack;

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
        generators.put(SingleOpCondInstruction.class, this::generateSingleOpCond);
        generators.put(OpCondInstruction.class, this::generateOpCond);
        generators.put(GotoInstruction.class, this::generateGoto);
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
        if (classUnit.getSuperClass() != null) {
            if (classPathMap.containsKey(classUnit.getSuperClass()))
                code.append(classPathMap.get(classUnit.getSuperClass())).append(NL);
            else if (classUnit.getSuperClass().equals("Object"))
                code.append("java/lang/Object").append(NL);
            else
                code.append(classUnit.getSuperClass()).append(NL);
        }
        else
            code.append("java/lang/Object").append(NL); //

        for (var field : ollirResult.getOllirClass().getFields()) {
            code.append(generators.apply(field));
        }

        StringBuilder defaultConstructor = new StringBuilder("""
                ;constructor
                .method public <init>()V
                    aload_0
                    invokespecial""");
        defaultConstructor.append((" "));

        if (classUnit.getSuperClass() != null) {
            if (classPathMap.containsKey(classUnit.getSuperClass()))
                defaultConstructor.append(classPathMap.get(classUnit.getSuperClass()));
            else if (classUnit.getSuperClass().equals("Object"))
                defaultConstructor.append("java/lang/Object");
            else
                defaultConstructor.append(classUnit.getSuperClass());
        }
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
        System.out.println(" JASMIN CODE: ");
        System.out.println(code);
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
        maxStack = 0;
        currentStack = 0;

        StringBuilder instructionsCode = new StringBuilder();

        for (var inst : method.getInstructions()) {

            for (Map.Entry<String, Instruction> label : currentMethod.getLabels().entrySet())
                if (label.getValue().equals(inst))
                    instructionsCode.append(label.getKey()).append(":").append(NL);

            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            instructionsCode.append(instCode);
        }

        code.append(TAB).append(".limit stack ").append(maxStack).append(NL);
        // TODO: start with params.size() and increase?
        code.append(TAB).append(".limit locals ").append(method.getVarTable().size()).append(NL);

        code.append(instructionsCode);

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
            case ARRAYREF -> {
                // cast to ArrayType
                ArrayType arrayType = (ArrayType) type;
                yield "[" + getType(arrayType.getElementType());
            }
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
        currentStack++;
        maxStack = Math.max(maxStack, currentStack);

        return "aload 0" + NL + // push this to stack
                "getfield " + currentMethod.getOllirClass().getClassName() + "/" +
                getFieldInstruction.getField().getName() + " " +
                getType(getFieldInstruction.getField().getType()) + NL;
    }

    private String generatePutField(PutFieldInstruction putFieldInstruction) {
        currentStack+=2; // this + value
        maxStack = Math.max(maxStack, currentStack);

        currentStack-=2; // putfield pops 2 values

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
        Type typeInstance = callInstruction.getReturnType();
        if (typeInstance instanceof ClassType classTypeInstance) {
            String className = classPathMap.getOrDefault(classTypeInstance.getName(), classTypeInstance.getName());
            code.append("new ").append(className).append(NL);
            shouldPop = false;
        } else if (typeInstance instanceof ArrayType arrayTypeInstance){
            code.append("newarray ");
            switch (arrayTypeInstance.getElementType().getTypeOfElement()) {
                case INT32 -> code.append("int");
                case BOOLEAN -> code.append("boolean");
                case OBJECTREF -> code.append("java/lang/Object");
                case STRING -> code.append("java/lang/String");
                case CLASS -> code.append("java/lang/Object"); // TODO: expand
                default -> throw new NotImplementedException(arrayTypeInstance.getTypeOfElement());
            }
            code.append(NL);
        } else {
            throw new NotImplementedException(typeInstance.getClass());
        }

        currentStack++;

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

            String returnType = getType(callInstruction.getReturnType());

            code.append("invokevirtual ")
                    .append(className).append("/")
                    .append(methodName)
                    .append("(")
                    .append(callInstruction.getArguments().stream()
                            .map(arg -> getType(arg.getType()))
                            .collect(Collectors.joining()))
                    .append(")");
            code.append(returnType).append(NL);

            currentStack-=(callInstruction.getArguments().size()+1);
            currentStack++;

            if (shouldPop && !callInstruction.getReturnType().getTypeOfElement().equals(ElementType.VOID)) {
                code.append("pop").append(NL);
                currentStack--;
            }
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

            currentStack-=(callInstruction.getArguments().size());
            currentStack++;

            if (shouldPop && !callInstruction.getReturnType().getTypeOfElement().equals(ElementType.VOID)) {
                code.append("pop").append(NL);
                currentStack--;
            }
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

            String returnType = getType(callInstruction.getReturnType());
            code.append("invokespecial ")
                    .append(className).append("/")
                    .append(methodName).append("(")
                    .append(callInstruction.getArguments().stream()
                            .map(arg -> getType(arg.getType()))
                            .collect(Collectors.joining()))
                    .append(")")
                    .append(returnType).append(NL);

            currentStack-=(callInstruction.getArguments().size()+1);
            currentStack++;

            if (shouldPop && !callInstruction.getReturnType().getTypeOfElement().equals(ElementType.VOID)) {
                code.append("pop").append(NL);
                currentStack--;
                shouldPop = true;
            }

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

        // if RHS is an invoke, we don't want to pop the result
        shouldPop = false;
        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        shouldPop = true;

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand operand)) {
            System.out.println("lhs: " + lhs.getClass() + " is not an Operand");
            throw new NotImplementedException(lhs.getClass());
        }

        // get register
        String operandName = operand.getName();
        code.append(generateStore(operandName));

        return code.toString();
    }

    private String generateStore(String name) {
        var code = new StringBuilder();
        var regName = currentMethod.getVarTable().get(name);
        var reg = regName.getVirtualReg();

        switch (regName.getVarType().getTypeOfElement()) {
            case INT32, BOOLEAN -> code.append("istore ").append(reg).append(NL);
            case OBJECTREF, STRING, CLASS -> code.append("astore ").append(reg).append(NL);
        }
        currentStack--;

        return code.toString();
    }

    private String generateLiteral(LiteralElement literal) {
        currentStack++; // push literal to stack
        maxStack = Math.max(maxStack, currentStack);

        // TODO: iconst here?
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        var code = new StringBuilder();
        // get register
        switch (operand.getType().getTypeOfElement()) {
            case INT32, BOOLEAN -> {
                var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
                code.append("iload ").append(reg).append(NL);
            }
            case OBJECTREF, CLASS, STRING -> {
                if ("this".equals(operand.getName())) {
                    return "aload 0" + NL;
                }
                System.out.println(operand.getName());
                var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
                code.append("aload ").append(reg).append(NL);
            }
            case THIS -> code.append("aload 0").append(NL);
        }

        currentStack++;
        maxStack = Math.max(maxStack, currentStack);

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }


    private String generateUnaryOp(UnaryOpInstruction unaryOpInstruction) {
        var code = new StringBuilder();

        code.append(generators.apply(unaryOpInstruction.getOperand()));

        if (unaryOpInstruction.getOperation().getOpType() == OperationType.NOTB)
            code.append("ifeq ").append(generateLabels(unaryOpInstruction)).append(NL);

        return code.toString();
    }

    private String generateSingleOpCond(SingleOpCondInstruction singleOpCondInstruction) {
        return generators.apply(singleOpCondInstruction.getCondition()) +
                "ifne " + singleOpCondInstruction.getLabel() + NL;
    }

    private String generateOpCond(OpCondInstruction opCondInstruction) {
        return generators.apply(opCondInstruction.getCondition()) +
                "ifne " + opCondInstruction.getLabel() + NL; // "if not equals zero" means true
    }

    private String generateGoto(GotoInstruction gotoInstruction) {
        return "goto " + gotoInstruction.getLabel() + NL;
    }

    private String generateLabels(Instruction instruction) {
        String label = String.valueOf(currentMethod.getLabels().size());
        String labelTrue = "LabelTrue" + label;
        String labelEnd = "LabelEnd" + label;

        currentMethod.addLabel(labelTrue, instruction);
        currentMethod.addLabel(labelEnd, instruction);

        return " " + labelTrue + NL
                + "iconst_0" + NL // false
                + "goto " + labelEnd + NL
                +  labelTrue + ": " + NL
                + "iconst_1" + NL
                + labelEnd + ":";
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
            case ANDB-> "iand";
            case LTH -> "if_icmplt";
            default ->{
                System.out.println("Operation not implemented: " + binaryOp.getOperation().getOpType());
                throw new NotImplementedException(binaryOp.getOperation().getOpType());
            }
        };

        currentStack--; // pop two values and push result

        var labelCode = switch (binaryOp.getOperation().getOpType()){
            case LTH, GTH, EQ, NEQ, LTE, GTE -> generateLabels(binaryOp);
            default -> "";
        };

        code.append(op).append(labelCode).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        ElementType type = returnInst.getReturnType().getTypeOfElement();
        switch (type) {
            case INT32, BOOLEAN -> {
                code.append(generators.apply(returnInst.getOperand()));
                currentStack--;
                code.append("ireturn").append(NL);
            }
            case OBJECTREF, STRING, CLASS -> {
                code.append(generators.apply(returnInst.getOperand()));
                currentStack--;
                code.append("areturn").append(NL);
            }
            case VOID -> code.append("return").append(NL);
        }

        return code.toString();
    }

}
