package com.oj.onlinejudge.service.checker.c;

import com.oj.onlinejudge.service.checker.CheckerCaller;
import com.oj.onlinejudge.service.checker.cpp.CppChecker;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class CCheckerCore extends CheckerCaller {
    private String root = System.getenv("BJUT_OJ_HOME");

    private String debugInfo;

    public CCheckerCore(String submissionUUID, String debugInfo, String targetProblem) {
        super();
        String fname = submissionUUID + "_main.c";
        if (root != null) {
            root += "\\files";
        } else {
            throw new RuntimeException("Cannot locate file storage");
        }
        setChecker(new CChecker(root, submissionUUID, targetProblem));
        this.debugInfo = debugInfo;
    }

    public Map<String, String> checkSubmission() throws IOException, InterruptedException, SQLException {

        Map<String, String> compileResult = Checker.compileAndRunFile(debugInfo);

        Map<String, String> resultPacket = new HashMap<>();

        if (("Accepted").equals(compileResult.get("RuntimeStatus"))) { // No compile error.
            Map<String, String> runResult;
            if (debugInfo == null) {
                runResult = Checker.checker();
                resultPacket.put("SubmissionStatus", runResult.get("JudgerStatus"));
            } else {
                runResult = Checker.debugger();
                resultPacket.put("SubmissionStatus", "Finished");
                resultPacket.put("debugOutcome", runResult.get("DebuggerStatus"));
            }
        } else {
            resultPacket.put("SubmissionStatus", compileResult.get("RuntimeStatus"));
        }

        resultPacket.put("TimeElapsed", compileResult.get("TimeElapsed"));
        Checker.clearUps();

        return resultPacket;
    }
}