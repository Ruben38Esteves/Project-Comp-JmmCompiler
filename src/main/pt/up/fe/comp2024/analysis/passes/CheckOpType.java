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

        // Perform type checking based on the operator
        String operator = binaryExpr.get("op");

        for (var local: symTable.getLocalVariables(currentMethod)){
            if (local.getName().equals(leftOperand.get("name"))){
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
            }
            if (local.getName().equals(rightOperand.get("name"))){
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
                rightType = local.getType().getName();
            }
        }


        // Perform type checking based on the operator
        switch (operator) {
            case "*", "+", "-":
                if (rightType.equals("boolean") || leftType.equals("boolean")){
                    var message = String.format("Cannot perform '%s' on '%s'", operator, rightType);
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(binaryExpr),
                            NodeUtils.getColumn(binaryExpr),
                            message,
                            null)
                    );
                }
                break;
            case "/": // separei para adicionar divisao por zero
                if (rightType.equals("boolean") || leftType.equals("boolean")){
                    var message = String.format("Cannot perform '%s' on '%s'", operator, rightType);
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(binaryExpr),
                            NodeUtils.getColumn(binaryExpr),
                            message,
                            null)
                    );
                }
                break;
        }

        return null;
    }

    // Helper method to check if a type is numeric (e.g., int or float)
    private boolean isNumericType(String type) {
        return type.equals("int") || type.equals("float");
    }
}
