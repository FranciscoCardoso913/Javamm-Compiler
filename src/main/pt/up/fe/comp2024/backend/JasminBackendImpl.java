package pt.up.fe.comp2024.backend;

import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;

public class JasminBackendImpl implements JasminBackend {

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        // converts an OLLIR ClassUnit
        // to a String representing Jasmin bytecode
        var jasminGenerator = new JasminGenerator(ollirResult);
        var jasminCode = jasminGenerator.build();

        return new JasminResult(ollirResult, jasminCode, jasminGenerator.getReports());
    }

}
