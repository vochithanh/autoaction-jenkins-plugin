package chl.jenkins.builder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/*
 * Loader for libs
 */
public class ChlLoadLib{
	public final static String JAR_EXT = ".jar";	

	public ChlLoadLib() {	
	}	
	
	protected static Map<String, Path> scanPath(String path, Predicate<Path> excludePattern){
		Map<String,Path> result = new HashMap<>(); 
		try {
			Files.walk(Paths.get(path), FileVisitOption.FOLLOW_LINKS).forEach(item ->{
				if( !excludePattern.test(item)) {
					result.put(item.toFile().getAbsolutePath(), item);
				}				
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}	
		
	protected URL[] loadLibUrl(String rootPath) {		
		Map<String, Path> resourceMap = scanPath(rootPath, path -> !path.toFile().getName().endsWith(JAR_EXT));
		return resourceMap.values().stream().map( path -> {
			try {
				return path.toUri().toURL();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			return null;
		}).collect(Collectors.toSet()).toArray(new URL[]{});
	}	
	
	public Object run(String libPath, String workspacePath, String browserString, String scriptContent, boolean isRunScriptOnly) {		
			
		final URLClassLoader loader = new URLClassLoader(loadLibUrl(libPath));
		try {			
	    	//GatePass pass = new GatePass(browserString,workspacePath,content,runScriptOnly);
	    	//ChlTestSuite result = GateBoss.run(pass);

			Class<?> gpClass = loader.loadClass("chl.gate.GatePass");
			Object gatepass = gpClass.getConstructor(String.class,String.class,String.class,boolean.class).newInstance(browserString,workspacePath,scriptContent,isRunScriptOnly);
			
			Class<?> gbClass = loader.loadClass("chl.gate.GateBoss");
			return gbClass.getMethod("run", gpClass).invoke(null, gatepass);			
		} catch (ClassNotFoundException|NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException  e) {
			e.printStackTrace();
		}finally {
			try {
				loader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}	
}
