package com.esendex.jenkinsci.plugins.graphite;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

@Extension(optional=true)
public class CurrentTimeMacro extends DataBoundTokenMacro {
    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals("CURRENT_TIME");
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName) throws MacroEvaluationException, IOException, InterruptedException {
        return DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.LONG).format(new Date());
    }
}
