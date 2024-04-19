package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.ClassUnit;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;

public class JasminBackendImpl implements JasminBackend {

    ClassUnit classUnit;
    StringBuilder jasminCode;
    String superClass;
    int limit_stack = 99;
    int limit_locals = 99;

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {

        var jasminGenerator = new JasminGenerator(ollirResult);
        var jasminCode = jasminGenerator.build();

        return new JasminResult(ollirResult, jasminCode, jasminGenerator.getReports());
    }

}
