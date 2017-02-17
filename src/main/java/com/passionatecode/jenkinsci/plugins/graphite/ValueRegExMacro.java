package com.passionatecode.jenkinsci.plugins.graphite;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Extension
public class ValueRegExMacro extends DataBoundTokenMacro {

    @Parameter(required=true)
    public String value = null;

    @Parameter(required=true)
    public String regex = null;

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals("VALUE_REGEX");
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName) throws MacroEvaluationException, IOException, InterruptedException {
        if (value == null || regex == null) {
            return value == null ? "" : value;
        }

        Matcher matcher = Pattern.compile(regex).matcher(value);
        return matcher.find() ? matcher.group() : value;
    }
}
