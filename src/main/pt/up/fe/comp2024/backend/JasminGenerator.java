package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

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
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(CallInstruction.class, this::generateCallInstr);
        generators.put(PutFieldInstruction.class, this::generatePutInstr);
        generators.put(GetFieldInstruction.class, this::generateGetInstr);
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

    private String getTypeJasmin(Type type) {
        return switch (type.getTypeOfElement()) {
            case INT32 -> "I";
            case BOOLEAN -> "Z";
            case ARRAYREF -> {
                ArrayType arrayType = (ArrayType) type;
                yield "[" + getTypeJasmin(arrayType.getElementType());
            }
            case OBJECTREF -> "Ljava/lang/Object";
            case CLASS -> {
                ClassType classType = (ClassType) type;
                yield "L" + getTypeJasmin(classType);
            }
            case THIS -> null;
            case STRING -> "Ljava/lang/String;";
            case VOID -> "V";
        };
    }


    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();

        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class ").append(className).append(NL).append(NL);

        String superClass = classUnit.getSuperClass();
        if (superClass == null) {
            superClass = " java/lang/Object";
        }
        code.append(".super ").append(superClass).append(NL);
        // generate a single constructor method
        var startConstructor = """
                .method public <init>()V
                    aload_0
                    invokespecial""";
        var endConstructor = """
                    return
                .end method
                """;
        code.append(startConstructor).append(" ").append(superClass).append("/<init>()V").append(NL).append(endConstructor).append(NL);

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


    private String generateMethod(Method method) {

        // set method
        currentMethod = method;

        var code = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        var methodName = method.getMethodName();

        if(method.isStaticMethod()){
            modifier += "static ";
        }

        code.append("\n.method ").append(modifier).append(methodName).append("(");
        for(var param : method.getParams()){
            code.append(getTypeJasmin(param.getType()));
        }
        code.append(")").append(getTypeJasmin(method.getReturnType())).append(NL);

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

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        Type type = operand.getType();

        switch (type.getTypeOfElement()){
            case INT32, BOOLEAN: code.append("istore ").append(reg).append(NL); break;
            case ARRAYREF,OBJECTREF,STRING: code.append("astore ").append(reg).append(NL); break;
            default: throw new NotImplementedException(type.getClass());
        }

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        return "iload " + reg + NL;
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
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();
        ElementType return_type = returnInst.getElementType();
        if(return_type != ElementType.VOID){
            code.append(generators.apply(returnInst.getOperand()));
        }
        switch (return_type) {
            case INT32,BOOLEAN -> code.append("ireturn").append(NL);
            case ARRAYREF,OBJECTREF,STRING -> code.append("areturn").append(NL);
            case VOID -> code.append("return").append(NL);

        }

        return code.toString();
    }

    private String generateCallInstr(CallInstruction call){
        var code = new StringBuilder();
        var className = call.getOperands().toString().split(" ")[1].split("\\.")[0];
        className = className.substring(0,1).toUpperCase() + className.substring(1);
        if(className.equals("This")){
            className = ollirResult.getOllirClass().getClassName();
        }
        switch (call.getInvocationType()){
            case invokestatic -> {
                code.append("invokestatic ").append(className).append("/");
                var methodName = call.getMethodName().toString().split("\"")[1];
                var args = call.getArguments();
                code.append(methodName).append("(");
                for(var arg : args){
                    code.append(getTypeJasmin(arg.getType()));
                }
                code.append(")").append(getTypeJasmin(call.getReturnType())).append(NL);
            }
            case invokespecial -> {
                code.append("invokespecial ").append(className).append("/").append("<init>()V").append(NL);
            }
            case invokevirtual -> {
                code.append("invokevirtual ").append(className).append("/");
                var methodName = call.getMethodName().toString().split("\"")[1];
                var args = call.getArguments();
                code.append(methodName).append("(");
                for(var arg : args){
                    code.append(getTypeJasmin(arg.getType()));
                }
            }
            case NEW -> {
                code.append("new ").append(className).append(NL);
                code.append("dup").append(NL);
            }
        }

        return code.toString();
    }

    private String generatePutInstr(PutFieldInstruction intr){
        var code = new StringBuilder();
        code.append("aload_0").append(NL);
        Operand field = intr.getField();
        String value = intr.getValue().toString().split(" ")[1].split("\\.")[0];
        code.append("ldc ").append(value).append(NL);
        String className = ollirResult.getOllirClass().getClassName();
        String fieldName = field.getName();
        Type fieldType = field.getType();
        code.append("putfield ").append(className).append("/").append(fieldName).append(" ").append(getTypeJasmin(fieldType)).append(NL);
        return code.toString();
    }

    private String generateGetInstr(GetFieldInstruction intr){
        var code = new StringBuilder();
        code.append("aload_0").append(NL);
        Operand field = intr.getField();
        String className = ollirResult.getOllirClass().getClassName();
        String fieldName = field.getName();
        Type fieldType = field.getType();
        code.append("getfield ").append(className).append("/").append(fieldName).append(" ").append(getTypeJasmin(fieldType)).append(NL);
        return code.toString();
    }

}
