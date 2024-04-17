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
import java.util.function.BiFunction;

public class CheckMethod extends AnalysisVisitor {
    public String currentClass;
    public String currentMethod;
    @Override
    public void buildVisitor() {
        addVisit(Kind.CLASS_DECL, this::visitClassDecl);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.METHOD_EXPR, this::visitMethodExpr);
    }

    private Void visitClassDecl(JmmNode currClass, SymbolTable symTable){
        currentClass = currClass.get("name");
        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable symTable){
        currentMethod = method.get("name");
        return null;
    }

    private Void visitMethodExpr(JmmNode methodExpr, SymbolTable symTable){
        boolean exists = false;
        boolean localAux = false;
        List<Symbol> localVars = symTable.getLocalVariables(currentMethod);
        List<String> imports = symTable.getImports();
        List<String> methods = symTable.getMethods();

        //se tiver imports passa
        //se tiver numa classe que exrtende o import, passa

        var object =  methodExpr.getChild(0);
        String varType = "";
        if (object.get("name").equals("this")){
            varType = currentClass;
            if(!methods.contains(methodExpr.getChild(1).get("name"))){
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
            for (var localVar : localVars) {
                if (localVar.getName().equals(object.get("name"))) {
                    varType = localVar.getType().getName();
                }
            }
        }

        if (!varType.equals(currentClass)){
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

        //todo checks nothing along path
        while(!expr.getKind().equals("CallMethod")){
            expr = expr.getChild(1);
        }

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

        if(params_in_call != null){
            if(params_in_call.size() != params.size()){
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

        for (int i = 0; i < expr.getChild(0).getChildren().size(); i++){
            var currentArgument = expr.getChild(0).getChild(i);
            for (var localVar: localVars){
                if (localVar.getName().equals(currentArgument.get("name"))){
                    if (!localVar.getType().getName().equals(params.get(i).getType().getName())){
                        var message = String.format("%s does not take %s as an argument", expr.get("name"), localVar.getType().getName());
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(currentArgument),
                                NodeUtils.getColumn(currentArgument),
                                message,
                                null));
                        return null;
                    }
                }
            }

        }
    return null;
    }
}

