package com.esendex.jenkinsci.plugins.graphite;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class GraphiteEventNotifier extends Notifier
{
    private final String tags;
    private final String whatTemplate;
    private final String dataTemplate;
    private final String host;

    @DataBoundConstructor
    public GraphiteEventNotifier(String tags, String whatTemplate, String dataTemplate, String host)
    {
        super();
        this.tags = tags;
        this.whatTemplate = whatTemplate;
        this.dataTemplate = dataTemplate;
        this.host = host != null && !host.isEmpty() && !host.trim().isEmpty() ? host : null;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    public String getTags() {
        return tags;
    }

    public String getWhatTemplate() { return whatTemplate; }

    public String getDataTemplate() { return dataTemplate; }

    public String getHost() { return host; }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        if (build.getResult() != Result.SUCCESS) return true;

        JSONObject postData = generatePostData(build, listener);

        final DescriptorImpl descriptor = getDescriptor();
        URL url = new URL(String.format("%s://%s/events/",
                descriptor.getUseSSL() ? "https" : "http",
                host == null ? descriptor.getDefaultHost() : expand(build, listener, host)));
        listener.getLogger().println("Publishing event to: '" + url.toString() + "' data: '" + postData.toString() + "'");

        try {
            publish(url, postData, listener);
        } catch (Exception ex) {
            listener.getLogger().println("Exception publishing Graphite event: " + ex.getMessage());
        }
        return true; // Never fail the build
    }

    private JSONObject generatePostData(AbstractBuild<?, ?> build, BuildListener listener) {
        String what = expand(build, listener, whatTemplate);
        String data = expand(build, listener, dataTemplate);
        String tags = "jenkins-graphite";
        if (this.tags != null && this.tags.length() > 0)
            tags += "," + this.tags;

        JSONObject postData = new JSONObject();
        postData.put("what", what);
        postData.put("data", data);
        postData.put("tags", tags);
        return postData;
    }

    private static String expand(AbstractBuild<?, ?> build, BuildListener listener, String template) {
        try {
            return TokenMacro.expandAll(build, listener, template, false, null);
        } catch (MacroEvaluationException mee) {
            throw new RuntimeException(mee);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    private static void publish(URL url, JSONObject postData, BuildListener listener) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestMethod("POST");
        OutputStream os = connection.getOutputStream();
        os.write(postData.toString().getBytes("UTF-8"));
        os.close();

        int result = connection.getResponseCode();
        if (result >= HttpURLConnection.HTTP_OK && result < HttpURLConnection.HTTP_MULT_CHOICE)
            return;

        listener.getLogger().println("Unexpected result while publishing event to Graphite: " + connection.getResponseMessage());
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher>
    {
        private String defaultHost = "localhost";
        private boolean useSSL = false;

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Graphite Event Publisher";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            return super.configure(req,formData);
        }

        public String getDefaultHost() {
            return defaultHost;
        }

        public void setDefaultHost(String defaultHost) {
            this.defaultHost = defaultHost;
        }

        public FormValidation doCheckDefaultHost(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set the host");
            return FormValidation.ok();
        }

        public boolean getUseSSL() {
            return useSSL;
        }

        public void setUseSSL(boolean useSSL) {
            this.useSSL = useSSL;
        }

        public static String defaultWhatTemplate() {
            return "$JOB_NAME successful";
        }
        public static String defaultDataTemplate() {
            return "<a href=\"$JOB_URL\"><strong>$JOB_NAME</strong></a> completed <a href=\"$BUILD_URL\"><em>$BUILD_NUMBER</em></a> successfully<br />$CURRENT_TIME";
        }
    }
}