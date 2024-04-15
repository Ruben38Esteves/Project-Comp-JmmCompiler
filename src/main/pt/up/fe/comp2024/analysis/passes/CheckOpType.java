package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

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
        System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        System.out.println(leftOperand);
        // Perform type checking based on the operator
        String operator = binaryExpr.get("op");

        // Retrieve the types of the operands
        String leftType = "aa";
        String rightType = "aa";

        // Perform type checking based on the operator
        switch (operator) {
            case "+":
            case "-":
            case "*":
            case "/":
                // Check if both operands have numeric types (int or float)
                if (!isNumericType(leftType) || !isNumericType(rightType)) {
                    String errorMessage = "Operands of the " + operator + " operator must have numeric types";
                    //addReport(Report.error(Stage.SEMANTIC, binaryExpr, errorMessage));
                }
                break;
            // Add cases for other operators as needed
        }

        return null;
    }

    // Helper method to check if a type is numeric (e.g., int or float)
    private boolean isNumericType(String type) {
        return type.equals("int") || type.equals("float");
    }
}
