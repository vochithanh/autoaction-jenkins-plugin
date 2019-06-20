package chl.jenkins.builder;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.base.Strings;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Map;

import jenkins.YesNoMaybe;
import jenkins.tasks.SimpleBuildStep;

public class ChlAutoActionStep extends Builder implements SimpleBuildStep {

    private String content = "Open http://www.google.com";
    private String browserString = "firefox";
    private Boolean runScriptOnly = Boolean.TRUE;
    private String rootPath = "";
    private String libPath = "";
    
    public ChlAutoActionStep(){
    }
    
    @DataBoundConstructor
    public ChlAutoActionStep(String content, String browserString, Boolean runScriptOnly, String rootPath,String libPath) {
        this.content = content;
        this.browserString = browserString;
        this.runScriptOnly = runScriptOnly;
        this.rootPath = rootPath;
        this.libPath = libPath;
    }

    public String getContent() {
        return content;
    }
    
    public String getBrowserString() {
    	return browserString;
    }
    
	public Boolean getRunScriptOnly() {
		return runScriptOnly;
	}

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
    	String workspacePath = workspace.getRemote();
    	if( !Strings.isNullOrEmpty(rootPath)) {
    		if( rootPath.startsWith(".")) {
    			workspacePath += rootPath.substring(1);
    		}else {
    			workspacePath = rootPath;
    		}
    	}
    	
    	ChlLoadLib loadlib = new ChlLoadLib();
    	Object result = loadlib.run(libPath,workspacePath,browserString,content,runScriptOnly);
    	
    	if( result != null) {    		    	
	    	// Save result
	    	File artifactsDir = run.getArtifactsDir();
	        if (!artifactsDir.isDirectory()) {
	            boolean success = artifactsDir.mkdirs();
	            if (!success) {
	                listener.getLogger().println("Can't create artifacts directory at "
	                  + artifactsDir.getAbsolutePath());
	            }
	        }
	        String path = String.format("%s/AutoAction-%s.log",artifactsDir.getCanonicalPath(),Calendar.getInstance().getTimeInMillis()+"");
	        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path),
	          StandardCharsets.UTF_8))) {
	            //writer.write(result.report(null));
	        	writer.write(result.getClass().getMethod("report", Map.class).invoke(result, new Object[] {null}).toString());
	        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
					| SecurityException e) {
				e.printStackTrace();
			}
    	}
    }

    @Extension(dynamicLoadable=YesNoMaybe.YES)
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Run AutoAction";
        }

    }

	public String getRootPath() {
		return rootPath;
	}

	public String getLibPath() {
		return libPath;
	}
}
