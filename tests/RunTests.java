import javax.tools.ToolProvider;
import java.net.URLClassLoader;
import java.nio.file.*;
public class RunTests { public static void main(String[] args)throws Exception {
 Path out=Files.createTempDirectory("ghost-tests");var compiler=ToolProvider.getSystemJavaCompiler();if(compiler==null)throw new IllegalStateException("JDK required");
 int result=compiler.run(null,null,null,"-encoding","UTF-8","-d",out.toString(),"app/src/main/java/ge/venice/ghostcardlab/Protocol.java","tests/ProtocolTest.java");if(result!=0)throw new AssertionError("Compilation failed");
 try(var loader=new URLClassLoader(new java.net.URL[]{out.toUri().toURL()})){loader.loadClass("ge.venice.ghostcardlab.ProtocolTest").getMethod("main",String[].class).invoke(null,(Object)new String[0]);}
 }}
