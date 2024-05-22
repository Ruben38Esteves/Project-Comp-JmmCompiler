package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.List;
public class CheckEqualType extends AnalysisVisitor{
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
    }

    private String getMethodCallType(JmmNode methodCall, SymbolTable table){
        String callerType = getVarRefType(methodCall.getChild(0), table);
        if(callerType == null){
            var message = String.format("Error getting Variable");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(methodCall),
                    NodeUtils.getColumn(methodCall),
                    message,
                    null));
            return null;
        }
        if(table.getImports().contains(callerType)){
            return "assume_correct";
        }
        String methodName = methodCall.getChild(1).get("name");
        if(callerType.equals(table.getClassName())){
            return table.getReturnType(methodName).getName();
        }
        return null;
    }

    private String getVarRefType(JmmNode varRefExpr, SymbolTable table){
        String varName = varRefExpr.get("name");
        //this
        if(varName.equals("this")){
            return table.getClassName();
        }
        //locals
        for(Symbol sym: table.getLocalVariables(currentMethod)){
            if(sym.getName().equals(varName)){
                if(sym.getType().isArray()){
                    return sym.getType().getName() + "_array";
                }
                return sym.getType().getName();
            }
        }
        //params
        for(Symbol sym: table.getParameters(currentMethod)){
            if(sym.getName().equals(varName)){
                if(sym.getType().isArray()){
                    return sym.getType().getName() + "_array";
                }
                return sym.getType().getName();
            }
        }
        //fields
        for(Symbol sym: table.getFields()){
            if(sym.getName().equals(varName)){
                if(sym.getType().isArray()){
                    return sym.getType().getName() + " array";
                }
                return sym.getType().getName();
            }
        }

        for(String imported : table.getImports()){
            if(imported.equals(varName)){
                return varName;
            }else{
                var message = String.format("'%s' is not imported",varName);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(varRefExpr),
                        NodeUtils.getColumn(varRefExpr),
                        message,
                        null));
                return null;
            }
        }
        return null;
        //imports

    }

    private String getBinaryExprType(JmmNode binaryExpr, SymbolTable symTable){
        JmmNode leftOperand = binaryExpr.getChild(0);
        JmmNode rightOperand = binaryExpr.getChild(1);
        String leftType = "";
        String rightType = "";

        switch (leftOperand.getKind()){
            case "IntegerLiteral":{
                leftType = "int";
                break;
            }
            case "BooleanLiteral":{
                leftType = "boolean";
                break;
            }
            case "VarRefExpr":{
                leftType = getVarRefType(leftOperand, symTable);
                break;
            }
            case "MethodExpr":{
                leftType = getMethodCallType(leftOperand, symTable);
                break;
            }
        }

        switch (rightOperand.getKind()){
            case "IntegerLiteral":{
                rightType = "int";
                break;
            }
            case "BooleanLiteral":{
                rightType = "boolean";
                break;
            }
            case "VarRefExpr":{
                rightType = getVarRefType(rightOperand,symTable);
                break;
            }
            case "MethodExpr":{
                rightType = getMethodCallType(rightOperand, symTable);
                break;
            }
        }

        if(leftType.equals("assume_correct")){
            if(rightType.equals("assume_correct")){
                return null;
            }else{
                leftType = rightType;
            }
        }else{
            if(rightType.equals("assume_correct")){
                rightType = leftType;
            }
        }

        String op =binaryExpr.get("op");
        if(op.equals("*") || op.equals("/") || op.equals("+") || op.equals("-")){
            if(leftType.equals("int") && rightType.equals("int")){
                return "int";
            }else{
                var message = String.format("Cannot perform '%s' on '%s'", op, rightType);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(binaryExpr),
                        NodeUtils.getColumn(binaryExpr),
                        message,
                        null)
                );
            }
        }
        if(op.equals("||") || op.equals("&&") || op.equals("<") || op.equals(">")){
            if(leftType.equals("boolean") && rightType.equals("boolean")){
                return "boolean";
            }else{
                var message = String.format("Cannot perform '%s' on '%s'", op, rightType);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(binaryExpr),
                        NodeUtils.getColumn(binaryExpr),
                        message,
                        null)
                );
            }
        }
        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable symTable){
        currentMethod = method.get("name");
        return null;
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable symTable){
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected method to be set");
        JmmNode leftOperand = assignStmt.getChild(0);
        JmmNode rightOperand = assignStmt.getChild(1);

        if(!leftOperand.getKind().equals("VarRefExpr")){
            var message = String.format("Left is not a variable");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignStmt),
                    NodeUtils.getColumn(assignStmt),
                    message,
                    null));
            return null;
        }

        String leftType = getVarRefType(leftOperand, symTable);
        if(leftType == null){
            var message = String.format("Left is not propperly defined");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignStmt),
                    NodeUtils.getColumn(assignStmt),
                    message,
                    null));
            return null;
        }
        String rightType = "";

        switch (rightOperand.getKind()){
            case "IntegerLiteral":{
                rightType = "int";
                break;
            }
            case "BooleanLiteral":{
                rightType = "boolean";
                break;
            }
            case "VarRefExpr":{
                rightType = getVarRefType(rightOperand,symTable);
                if(rightType == null){
                    var message = String.format("Left is not a variable");
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(assignStmt),
                            NodeUtils.getColumn(assignStmt),
                            message,
                            null));
                    return null;
                }
                break;
            }
            case "MethodExpr":{
                rightType = getMethodCallType(rightOperand, symTable);
                if(rightType == null){
                    var message = String.format("Left is not a variable");
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(assignStmt),
                            NodeUtils.getColumn(assignStmt),
                            message,
                            null));
                    return null;
                }
                break;
            }
            case "ClassInstance":{
                rightType = rightOperand.get("name");
                if(symTable.getImports().contains(rightType) || symTable.getClassName().equals(rightType)){
                    break;
                }else{
                    var message = String.format("'%s' was not imported",rightType);
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(assignStmt),
                            NodeUtils.getColumn(assignStmt),
                            message,
                            null));
                    return null;
                }
            }
            case "ArrayInitialization":{
                // vai ser preciso mudar caso arrays nao sejam so de ints
                rightType = "int_array";
                break;
            }
            case "BinaryExpr":{
                rightType = getBinaryExprType(rightOperand, symTable);
                if(rightType == null){
                    var message = String.format("Binary Expression is not correct");
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(rightOperand),
                            NodeUtils.getColumn(rightOperand),
                            message,
                            null));
                    return null;
                }
            }
        }

        if(symTable.getImports().contains(rightType)){
            rightType = leftType;
        }

        if(rightType.equals("assume_correct")){
            rightType = leftType;
        }


        if(leftType.equals(rightType)){
            return null;
        }else{
            if(rightType.equals(symTable.getClassName()) && leftType.equals(symTable.getSuper()) && symTable.getImports().contains(symTable.getSuper())){
                return null;
            }

            var message = String.format("Cannot attribute '%s to '%s'",rightType, leftType);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignStmt),
                    NodeUtils.getColumn(assignStmt),
                    message,
                    null));
            return null;
        }

        /*
        List<Symbol> symList = symTable.getLocalVariables(currentMethod);
        List<String> imports = symTable.getImports();
        String rightType = "";
        String leftType = "";
        boolean isLeftArray = false;
        boolean isRightArray = false;

        for (var sym: symList){
            if (sym.getName().equals(left.get("name"))) {
                leftType = sym.getType().getName();
                isLeftArray = sym.getType().isArray();
            }
        }

        if(right.getKind().equals("NewArray")){
            rightType = right.getChild(0).get("name");
            right = right.getChild(1);
        }

        if(right.getKind().equals("ClassInstance")){
            rightType = right.get("name");
        }

        //adaptar para chain de methods
        if(right.getKind().equals("MethodExpr")){
            String var_name = right.getChild(0).get("name");
            Type return_type = symTable.getReturnType(right.getChild(1).get("name"));
            if(return_type != null){
                rightType = return_type.getName();
            }else{
                for(var sym : symList){
                    if(sym.getName().equals(var_name) && imports.contains(sym.getType().getName())){
                        return null;
                    }
                    if(sym.getName().equals(var_name) && symTable.getSuper() != null ){
                        if(sym.getType().getName().equals(symTable.getSuper()) && imports.contains(symTable.getSuper())){
                            return null;
                        }
                    }
                }
                var message = String.format("Class %s not imported", var_name);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignStmt),
                        NodeUtils.getColumn(assignStmt),
                        message,
                        null));
                return null;
            }
        }

        if(right.getKind().equals("IntegerLiteral")){
            if(leftType.equals("int")){
                return null;
            }else {
                var message = String.format("Cannot attribute int to '%s'", leftType);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignStmt),
                        NodeUtils.getColumn(assignStmt),
                        message,
                        null));
                return null;
            }
        }

        if(right.getKind().equals("BooleanLiteral")){
            if(leftType.equals("boolean")){
                return null;
            }else{
                var message = String.format("Cannot attribute boolean to '%s'", leftType);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignStmt),
                        NodeUtils.getColumn(assignStmt),
                        message,
                        null));
                return null;
            }
        }

        if(right.getKind().equals("ArrayInitialization")){
            if(isLeftArray){
                return null;
            }else {
                var message = String.format("Cannot attribute array to '%s'", leftType);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignStmt),
                        NodeUtils.getColumn(assignStmt),
                        message,
                        null));
                return null;
            }
        }

        //----------------------------------------------
        if (right.getKind().equals("BinaryExpr")){
            if (leftType.equals("int")){
                return null;
            }else {
                var message = String.format("Cannot attribute boolean to '%s'", leftType);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignStmt),
                        NodeUtils.getColumn(assignStmt),
                        message,
                        null));
                return null;
            }

        }
        //--------------------------------------------

        if(rightType.isEmpty()){
            for (var sym: symList){
                if (sym.getName().equals(right.get("name"))) {
                    rightType = sym.getType().getName();
                    isRightArray = sym.getType().isArray();
                }
            }
        }


        if(!leftType.equals(rightType)){
            if(imports.contains(leftType)){
                if (imports.contains(rightType)){
                    return null;
                }
                if(leftType.equals(symTable.getSuper()) && rightType.equals(symTable.getClassName())){
                    return null;
                }
            }
            var message = String.format("Cannot attribute '%s' to '%s'", rightType, leftType);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignStmt),
                    NodeUtils.getColumn(assignStmt),
                    message,
                    null));
            return null;
        } else if (isLeftArray != isRightArray) {
            var message = String.format("Cannot attribute '%s' to '%s' beacause of them is not an array", rightType, leftType);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignStmt),
                    NodeUtils.getColumn(assignStmt),
                    message,
                    null));
            return null;

        }
        return null;
        */
    }

}
