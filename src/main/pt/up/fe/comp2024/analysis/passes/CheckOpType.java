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

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable symTable) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected method to be set");
        // Retrieve the left and right operands
        JmmNode leftOperand = binaryExpr.getChild(0);
        JmmNode rightOperand = binaryExpr.getChild(1);
        String leftType = "";
        String rightType = "";
        String operator = binaryExpr.get("op");

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

        for (var local: symTable.getLocalVariables(currentMethod)){
            if (leftType.isEmpty() && local.getName().equals(leftOperand.get("name"))){
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
                leftType = local.getType().getName();
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
                    if(symTable.getImports().contains(caller)){
                        rightType = null; // fix
                    }
                    rightType = symTable.getReturnType(rightOperand.getChild(1).get("name")).getName();
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
