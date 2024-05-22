package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.ArrayList;
import java.util.List;

public class CheckOpType extends AnalysisVisitor {
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit(Kind.IF_STMT, this::visitIfStmt);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable symTable){
        currentMethod = method.get("name");
        return null;
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
            }
        }
        return null;
        //imports

    }

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable symTable) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected method to be set");

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
                return null;
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
                return null;
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


        /*
        // Retrieve the left and right operands
        JmmNode leftOperand = binaryExpr.getChild(0);
        System.out.println(visit(leftOperand,symTable));
        JmmNode rightOperand = binaryExpr.getChild(1);
        var params = symTable.getParameters(currentMethod);
        List<String> param_types = new ArrayList<>();
        var locals = symTable.getLocalVariables(currentMethod);
        List<String> locals_types = new ArrayList<>();
        var fields = symTable.getFields();
        List<String> fields_types = new ArrayList<>();
        String leftType = "";
        String rightType = "";
        String operator = binaryExpr.get("op");

        for(var param : params){
            param_types.add(param.getName());
        }

        for(var local : locals){
            locals_types.add(local.getName());
        }

        for(var field : fields){
            fields_types.add(field.getName());
        }

        if(!leftOperand.getKind().equals("VarRefExpr")){
            switch (leftOperand.getKind()){
                case "IntegerLiteral":{
                    leftType = "int";
                    break;
                }
                case "BooleanLiteral":{
                    leftType = "boolean";
                    break;
                }
            }
        }
        if(leftType.isEmpty()){
            if(locals_types.contains(leftOperand.get("name"))){
                for(var local : locals){
                    if(local.getName().equals(leftOperand.get("name"))){
                        if(local.getType().isArray()){
                            var message = String.format("Cannot perform '%s' on '%s'", operator, rightType);
                            addReport(Report.newError(
                                    Stage.SEMANTIC,
                                    NodeUtils.getLine(binaryExpr),
                                    NodeUtils.getColumn(binaryExpr),
                                    message,
                                    null)
                            );
                        }
                        leftType = local.getType().getName();
                        break;
                    }
                }
            } else if (param_types.contains(leftOperand.get("name"))) {
                for(var param : params){
                    if(param.getName().equals(leftOperand.get("name"))){
                        if(param.getType().isArray()){
                            var message = String.format("Cannot perform '%s' on '%s'", operator, rightType);
                            addReport(Report.newError(
                                    Stage.SEMANTIC,
                                    NodeUtils.getLine(binaryExpr),
                                    NodeUtils.getColumn(binaryExpr),
                                    message,
                                    null)
                            );
                        }
                        leftType = param.getType().getName();
                        break;
                    }
                }
            } else if (fields_types.contains(leftOperand.get("name"))) {
                for(var field : fields){
                    if(field.getName().equals(leftOperand.get("name"))){
                        if(field.getType().isArray()){
                            var message = String.format("Cannot perform '%s' on '%s'", operator, rightType);
                            addReport(Report.newError(
                                    Stage.SEMANTIC,
                                    NodeUtils.getLine(binaryExpr),
                                    NodeUtils.getColumn(binaryExpr),
                                    message,
                                    null)
                            );
                        }
                        leftType = field.getType().getName();
                        break;
                    }
                }
            }
        }

        if(!rightOperand.getKind().equals("VarRefExpr")){
            switch (rightOperand.getKind()){
                case "IntegerLiteral":{
                    rightType = "int";
                    break;
                }
                case "BooleanLiteral":{
                    rightType = "boolean";
                    break;
                }
                case "MethodExpr":{
                    String caller = rightOperand.getChild(0).get("name");
                    String called = rightOperand.getChild(1).get("name");
                    if(caller.equals("this")){
                        if(symTable.getMethods().contains(called)){
                            rightType = symTable.getReturnType(called).getName();
                            break;
                        }
                    }else if(symTable.getImports().contains(caller)){
                        rightType = leftType;
                        break;
                    } else if(param_types.contains(caller)){
                        for(var param : params){
                            if(param.getName().equals(caller)){
                                rightType = param.getType().getName();
                                break;
                            }
                        }
                    }else if(locals_types.contains(caller)){
                        for(var local : locals){
                            if(local.getName().equals(caller)){
                                rightType = local.getType().getName();
                                break;
                            }
                        }
                    }else if(fields_types.contains(caller)){
                        for(var field : fields){
                            if(field.getName().equals(caller)){
                                rightType = field.getType().getName();
                                break;
                            }
                        }
                    }
                    if(symTable.getImports().contains(rightType)){
                        rightType = leftType;
                        break;
                    }
                    if(symTable.getClassName().equals(rightType) && symTable.getMethods().contains(caller)){
                        rightType = symTable.getReturnType(caller).getName();
                        break;
                    }
                    var message = String.format("'%s' cant be called", called);
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(binaryExpr),
                            NodeUtils.getColumn(binaryExpr),
                            message,
                            null)
                    );
                    return null;
                }
            }
        }
        // Perform type checking based on the operator

        for (var local: symTable.getLocalVariables(currentMethod)){
            if (rightType.isEmpty() && local.getName().equals(rightOperand.get("name"))){
                if(local.getType().isArray()){
                    var message = String.format("Cannot perform '%s' on '%s'", operator, rightType);
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(binaryExpr),
                            NodeUtils.getColumn(binaryExpr),
                            message,
                            null)
                    );
                    return null;
                }
                rightType = local.getType().getName();
            }
        }


        // Perform type checking based on the operator
        switch (operator) {
            case "*", "+", "-":
                if (!rightType.equals("int") || !leftType.equals("int")){
                    var message = String.format("Cannot perform '%s' on '%s'", operator, rightType);
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(binaryExpr),
                            NodeUtils.getColumn(binaryExpr),
                            message,
                            null)
                    );
                    return null;
                }
                break;
            case "/": // separei para adicionar divisao por zero
                if (!rightType.equals("int") || !leftType.equals("int")){
                    var message = String.format("Cannot perform '%s' on '%s'", operator, rightType);
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(binaryExpr),
                            NodeUtils.getColumn(binaryExpr),
                            message,
                            null)
                    );
                    return null;
                }
                break;
        }

        return null;

         */
        return null;
    }

    private Void visitIfStmt(JmmNode ifStmt, SymbolTable symTable){
        JmmNode expr = ifStmt.getChild(0);

            if (!expr.getKind().equals("BooleanExpr") || !expr.getKind().equals("BooleanLiteral")){
                var message = String.format("Invalid if statement");
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(ifStmt),
                        NodeUtils.getColumn(ifStmt),
                        message,
                        null)
                );
                return null;
            }
        return null;
    }

}
