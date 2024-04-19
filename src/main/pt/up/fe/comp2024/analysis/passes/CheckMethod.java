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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

public class CheckMethod extends AnalysisVisitor {
    public String currentClass;
    public String currentMethod;
    @Override
    public void buildVisitor() {
        addVisit(Kind.CLASS_DECL, this::visitClassDecl);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.METHOD_EXPR, this::visitMethodExpr);
        addVisit(Kind.RETURN_STMT, this::visitReturnStmt);
    }

    private Void visitClassDecl(JmmNode currClass, SymbolTable symTable){

        // Check for duplicate imports
        List<String> imports = symTable.getImports();
        Set<String> seenImports = new HashSet<>();

        for (String import_ : imports) {
            if (!seenImports.add(import_)) {
                var message = String.format("%s already imported", import_);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        1,
                        1,
                        message,
                        null));
            }
        }


        currentClass = currClass.get("name");
        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable symTable){
        currentMethod = method.get("name");
        return null;
    }

    private Void visitMethodExpr(JmmNode methodExpr, SymbolTable symTable){

        List<Symbol> localVars = symTable.getLocalVariables(currentMethod);
        List<String> imports = symTable.getImports();
        List<String> methods = symTable.getMethods();



        var object =  methodExpr.getChild(0);
        String varType = "";
        if (object.get("name").equals("this")){ // check
            //If "this", takes Class name
            varType = currentClass;
            if(!methods.contains(methodExpr.getChild(1).get("name"))){ //check
                var message = String.format("");
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(methodExpr),
                        NodeUtils.getColumn(methodExpr),
                        message,
                        null));
                return null;

            }
        } else {
            if(imports.contains(object.get("name"))){
                varType = object.get("name");
            }
            for (var localVar : localVars) {
                if (localVar.getName().equals(object.get("name"))) {
                    varType = localVar.getType().getName();
                }
            }
        }



        if (!varType.equals(currentClass)){
            // If imported, no need to check method
            if(imports.contains(varType)){
                return null;
            }
            var message = String.format("");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(methodExpr),
                    NodeUtils.getColumn(methodExpr),
                    message,
                    null));
            return null;
        }else if (imports.contains(symTable.getSuper())){
            return null;
        }



        var expr = methodExpr;
        expr = expr.getChild(1);


        while(!expr.getKind().equals("CallMethod")){
            expr = expr.getChild(1);
        }

        // Check method declaration
        if (!methods.contains(expr.get("name"))){
            var message = String.format("%s was not declared", expr.get("name"));
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(expr),
                    NodeUtils.getColumn(expr),
                    message,
                    null));
            return null;

        }
        List<Symbol> params = symTable.getParameters(expr.get("name"));
        var params_in_call = expr.getChild(0).getChildren();


        List<String> paramsInCallType = new ArrayList<>();
        for (var param: params_in_call){
            if (param.getKind().equals("VarRefExpr")){
                for (var localvar: localVars){
                    if (param.get("name").equals(localvar.getName())){ // check
                        paramsInCallType.add(localvar.getType().getName());
                    }
                }
            } else {
                switch (param.getKind()){
                    case "IntegerLiteral":
                        paramsInCallType.add("int");
                        break;
                    case "StringLiteral":
                        paramsInCallType.add("string");
                        break;
                    case "CharLiteral":
                        paramsInCallType.add("char");
                        break;
                    case "BooleanLiteral":
                        paramsInCallType.add("bool");
                        break;
                }

            }
        }


        // Checks if varArg is last and throws report otherwise
        for (var param: params) {
            if (param.getType().getName().equals("int...") && !param.equals(params.get(params.size()-1))){
                var message = String.format("Varargs must be last element of method expression");
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(expr),
                        NodeUtils.getColumn(expr),
                        message,
                        null));
                return null;
            }
        }


        // Checks if the arguments on call related to vararg are integers
        if(params_in_call != null){
            if(params_in_call.size() != params.size()){
                if (params.get(params.size()-1).getType().getName().equals("int...")){
                    for (int i = params.size()-1; i < params_in_call.size(); i++){
                        if (!paramsInCallType.get(i).equals("int")){
                            var message = String.format("Varargs must be composed of integers");
                            addReport(Report.newError(
                                    Stage.SEMANTIC,
                                    NodeUtils.getLine(expr),
                                    NodeUtils.getColumn(expr),
                                    message,
                                    null));
                            return null;
                        }
                    }

                }else {
                    var message = String.format("Incorrect argument amount");
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(expr),
                            NodeUtils.getColumn(expr),
                            message,
                            null));
                    return null;
                }

            }
        }


        // Already checked if vararg is last , checks only params
        int i = 0;
        for (var param: params){
            if (param.getType().getName().equals("int...")) break;
            if (!param.getType().getName().equals(paramsInCallType.get(i))){
                var message = String.format("%s expected, received %s", param.getType().getName(), paramsInCallType.get(i));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(expr),
                        NodeUtils.getColumn(expr),
                        message,
                        null));
                return null;
            }
        }


        // Check return



    return null;
    }

    private Void visitReturnStmt(JmmNode returnStmt, SymbolTable symTable){
        System.out.println("AA");
        String returnType = "";

        // fetch expeccted return type
        var parent = returnStmt.getJmmParent();
        while (!parent.getKind().equals("MethodDecl")){
            parent = parent.getJmmParent();
        }
        String methodName = parent.get("name");
        String expectedReturnType = symTable.getReturnType(methodName).getName();


        // check what it's trying to return
        switch(returnStmt.getChild(0).getKind()){
            case "BinaryExpr":
            case "IntegerLiteral":
            case "ArrayAccessExpr":
                returnType = "int";
                break;
            case "BooleanExpr":
            case "BooleanLiteral":
                returnType = "bool";
                break;
            case "VarRefExpr":
                var name = returnStmt.getChild(0).get("name");
                var params = symTable.getParameters(methodName);
                var fields = symTable.getFields();
                var localVars = symTable.getLocalVariables(methodName);
                List<Symbol> allVars = new ArrayList<>();
                allVars.addAll(params);
                allVars.addAll(fields);
                allVars.addAll(localVars);
                for (var sym: allVars){
                    if (sym.getName().equals(name)){
                        returnType = sym.getType().getName();
                        break;
                    }
                }
                break;
        }

        if (!returnType.equals(expectedReturnType)){
            var message = String.format("%s is returning %s, expected %s", methodName, returnType, expectedReturnType);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(returnStmt),
                    NodeUtils.getColumn(returnStmt),
                    message,
                    null));
        }

        return null;
    }
}

