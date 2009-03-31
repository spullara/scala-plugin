package org.jetbrains.plugins.scala.compiler;

import com.intellij.compiler.CompilerConfigurationImpl;
import com.intellij.compiler.OutputParser;
import com.intellij.compiler.impl.javaCompiler.DependencyProcessor;
import com.intellij.compiler.impl.javaCompiler.ExternalCompiler;
import com.intellij.compiler.impl.javaCompiler.ModuleChunk;
import com.intellij.compiler.impl.javaCompiler.javac.JavacSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.MockJdkWrapper;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.scala.ScalaBundle;
import org.jetbrains.plugins.scala.ScalaFileType;
import org.jetbrains.plugins.scala.compiler.rt.ScalacRunner;
import org.jetbrains.plugins.scala.config.ScalaCompilerUtil;
import org.jetbrains.plugins.scala.config.ScalaConfigUtils;
import org.jetbrains.plugins.scala.util.ScalaUtils;

import java.io.*;
import java.util.*;

/**
 * @author ilyas
 */
public class ScalacBackendCompiler extends ExternalCompiler {

  private static final Logger LOG = Logger.getInstance("#org.jetbrains.plugins.scala.compiler.ScalacBackendCompiler");

  private final Project myProject;
  private final List<File> myTempFiles = new ArrayList<File>();

  // VM properties
  @NonNls
  private final String XSS_COMPILER_PROPERTY = "-Xss128m";

  // Scalac parameters
  @NonNls
  private static final String DEBUG_INFO_LEVEL_PROPEERTY = "-g:vars";
  @NonNls
  private static final String VERBOSE_PROPERTY = "-verbose";
  @NonNls
  private static final String DESTINATION_COMPILER_PROPERTY = "-d";
  @NonNls
  private static final String DEBUG_PROPERTY = "-Ydebug";

  private final static HashSet<FileType> COMPILABLE_FILE_TYPES = new HashSet<FileType>(Arrays.asList(ScalaFileType.SCALA_FILE_TYPE, StdFileTypes.JAVA));

  public ScalacBackendCompiler(Project project) {
    myProject = project;
  }

  public boolean checkCompiler(CompileScope scope) {
    VirtualFile[] files = scope.getFiles(ScalaFileType.SCALA_FILE_TYPE, true);
    if (files.length == 0) return true;

    final ProjectFileIndex index = ProjectRootManager.getInstance(myProject).getFileIndex();
    Set<Module> modules = new HashSet<Module>();
    for (VirtualFile file : files) {
      Module module = index.getModuleForFile(file);
      if (module != null) {
        modules.add(module);
      }
    }

    boolean hasJava = false;
    for (Module module : modules) {
      if (ScalaUtils.isSuitableModule(module)) {
        hasJava = true;
      }
    }
    if (!hasJava) return false; //this compiler work with only Java modules, so we don't need to continue.

    for (Module module : modules) {
      if (!(ScalaCompilerUtil.isScalaCompilerSetUpForModule(module) && ScalaUtils.isSuitableModule(module))) {
        Messages.showErrorDialog(myProject, ScalaBundle.message("cannot.compile.scala.files.no.compiler", module.getName()), ScalaBundle.message("cannot.compile"));
        return false;
      }
    }

    Set<Module> nojdkModules = new HashSet<Module>();
    for (Module module : scope.getAffectedModules()) {
      if (!(ScalaUtils.isSuitableModule(module))) continue;
      Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
      if (sdk == null || !(sdk.getSdkType() instanceof JavaSdkType)) {
        nojdkModules.add(module);
      }
    }

    if (!nojdkModules.isEmpty()) {
      final Module[] noJdkArray = nojdkModules.toArray(new Module[nojdkModules.size()]);
      if (noJdkArray.length == 1) {
        Messages.showErrorDialog(myProject, ScalaBundle.message("cannot.compile.scala.files.no.sdk", noJdkArray[0].getName()), ScalaBundle.message("cannot.compile"));
      } else {
        StringBuffer modulesList = new StringBuffer();
        for (int i = 0; i < noJdkArray.length; i++) {
          if (i > 0) modulesList.append(", ");
          modulesList.append(noJdkArray[i].getName());
        }
        Messages.showErrorDialog(myProject, ScalaBundle.message("cannot.compile.scala.files.no.sdk.mult", modulesList.toString()), ScalaBundle.message("cannot.compile"));
      }
      return false;
    }
    return true;
  }

  @NotNull
  public String getId() {
    return "Scalac";
  }

  @NotNull
  public String getPresentableName() {
    return ScalaBundle.message("scalac.compiler.name");
  }

  @NotNull
  public Configurable createConfigurable() {
    return null;
  }

  public OutputParser createErrorParser(String outputDir) {
    return new ScalacOutputParser();
  }

  public OutputParser createOutputParser(String outputDir) {
    return new OutputParser() {
      @Override
      public boolean processMessageLine(Callback callback) {
        if (super.processMessageLine(callback)) {
          return true;
        }
        return callback.getCurrentLine() != null;
      }
    };
  }

  @NotNull
  public String[] createStartupCommand(final ModuleChunk chunk, CompileContext context, final String outputPath) throws IOException, IllegalArgumentException {
    final ArrayList<String> commandLine = new ArrayList<String>();
    final Exception[] ex = new Exception[]{null};
    ApplicationManager.getApplication().runReadAction(new Runnable() {
      public void run() {
        try {
          createStartupCommandImpl(chunk, commandLine, outputPath);
        }
        catch (IllegalArgumentException e) {
          ex[0] = e;
        }
        catch (IOException e) {
          ex[0] = e;
        }
      }
    });
    if (ex[0] != null) {
      if (ex[0] instanceof IOException) {
        throw (IOException) ex[0];
      } else if (ex[0] instanceof IllegalArgumentException) {
        throw (IllegalArgumentException) ex[0];
      } else {
        LOG.error(ex[0]);
      }
    }
    return commandLine.toArray(new String[commandLine.size()]);
  }

  @NotNull
  @Override
  public Set<FileType> getCompilableFileTypes() {
    return COMPILABLE_FILE_TYPES;
  }

  @Override
  public DependencyProcessor getDependencyProcessor() {
    return new ScalacDependencyProcessor();
  }

  private void createStartupCommandImpl(ModuleChunk chunk, ArrayList<String> commandLine, String outputPath) throws IOException {
    final Sdk jdk = getJdkForStartupCommand(chunk);
    final String versionString = jdk.getVersionString();
    if (versionString == null || "".equals(versionString)) {
      throw new IllegalArgumentException(ScalaBundle.message("javac.error.unknown.jdk.version", jdk.getName()));
    }
    final JavaSdkType sdkType = (JavaSdkType) jdk.getSdkType();

    final String toolsJarPath = sdkType.getToolsPath(jdk);
    if (toolsJarPath == null) {
      throw new IllegalArgumentException(ScalaBundle.message("javac.error.tools.jar.missing", jdk.getName()));
    }
    if (!allModulesHaveSameScalaSdk(chunk)) {
      throw new IllegalArgumentException(ScalaBundle.message("different.scala.sdk.in.modules"));
    }

    String javaExecutablePath = sdkType.getVMExecutablePath(jdk);
    commandLine.add(javaExecutablePath);

    ScalacSettings settings = ScalacSettings.getInstance(myProject);
    commandLine.add(XSS_COMPILER_PROPERTY);
    commandLine.add("-Xmx" + settings.MAXIMUM_HEAP_SIZE + "m");
    commandLine.add("-cp");


    // For debug

//    commandLine.add("-Xdebug");
//    commandLine.add("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=127.0.0.1:5448");

    String rtJarPath = PathUtil.getJarPathForClass(ScalacRunner.class);
    final StringBuilder classPathBuilder = new StringBuilder();
    classPathBuilder.append(rtJarPath).append(File.pathSeparator);
    classPathBuilder.append(sdkType.getToolsPath(jdk)).append(File.pathSeparator);

    String scalaSdkJarPath = "";
    String scalaCompilerJarPath = "";

    final Module[] modules = chunk.getModules();
    for (Module module : modules) {
      if (ScalaCompilerUtil.isScalaCompilerSetUpForModule(module)) {
        scalaCompilerJarPath = ScalaCompilerUtil.getScalaCompilerJarPath(module);
      }
    }

    for (Module module : modules) {
      scalaSdkJarPath = ScalaConfigUtils.getScalaSdkJarPath(module);
      if (scalaSdkJarPath.length() > 0) {
        break;
      }
    }

    //Add Scala SDK jar
    if (scalaSdkJarPath.length() > 0) {
      classPathBuilder.append(scalaSdkJarPath);
      classPathBuilder.append(File.pathSeparator);
    }

    // Special check to compile scala language library
    if (ScalaCompilerUtil.isJarFileContainsClassFile(scalaCompilerJarPath, ScalaCompilerUtil.LAMP_PATCKAGE_PATH)) {
      classPathBuilder.append(scalaCompilerJarPath);
      classPathBuilder.append(File.pathSeparator);
    } else {
      final Module module = ContainerUtil.find(modules, new Condition<Module>() {
        public boolean value(Module module) {
          return ScalaCompilerUtil.isScalaCompilerSetUpForModule(module);
        }
      });

      assert module != null;

      final Library[] libraries = ScalaCompilerUtil.getScalaCompilerLibrariesByModule(module);
      if (libraries.length > 0) {
        for (VirtualFile file : libraries[0].getFiles(OrderRootType.CLASSES)) {
          classPathBuilder.append(StringUtil.trimEnd(file.getPath(), "!/"));
          classPathBuilder.append(File.pathSeparator);
        }
      }
    }


    commandLine.add(classPathBuilder.toString());
    commandLine.add(ScalacRunner.class.getName());


    try {
      File fileWithParams = File.createTempFile("scalac", ".tmp");
      fillFileWithScalacParams(chunk, fileWithParams, outputPath, myProject);

      commandLine.add(fileWithParams.getPath());
    } catch (IOException e) {
      LOG.error(e);
    }
  }


  private static void fillFileWithScalacParams(ModuleChunk chunk, File fileWithParameters, String outputPath, Project myProject)
      throws FileNotFoundException {

    PrintStream printer = new PrintStream(new FileOutputStream(fileWithParameters));

    ScalacSettings settings = ScalacSettings.getInstance(myProject);
    StringTokenizer tokenizer = new StringTokenizer(settings.getOptionsString(), " ");
    while (tokenizer.hasMoreTokens()) {
      printer.println(tokenizer.nextToken());
    }
    printer.println(VERBOSE_PROPERTY);
    printer.println(DEBUG_PROPERTY);
    //printer.println(WARNINGS_PROPERTY);
    printer.println(DEBUG_INFO_LEVEL_PROPEERTY);
    printer.println(DESTINATION_COMPILER_PROPERTY);
    printer.println(outputPath.replace('/', File.separatorChar));

    //write classpath
    printer.println("-cp");

    final Module[] chunkModules = chunk.getModules();
    final List<Module> modules = Arrays.asList(chunkModules);
    final Set<VirtualFile> sourceDependencies = new HashSet<VirtualFile>();
    final boolean isTestChunk = isTestChunk(chunk);

    for (Module module : modules) {
      if (ScalaUtils.isSuitableModule(module)) {
        ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
        OrderEntry[] entries = moduleRootManager.getOrderEntries();
        Set<VirtualFile> cpVFiles = new HashSet<VirtualFile>();
        for (OrderEntry orderEntry : entries) {
          cpVFiles.addAll(Arrays.asList(orderEntry.getFiles(OrderRootType.COMPILATION_CLASSES)));
          // Add Java dependencies
          if (orderEntry instanceof ModuleOrderEntry && !(modules.contains(((ModuleOrderEntry) orderEntry).getModule()))) {
            sourceDependencies.addAll(Arrays.asList(orderEntry.getFiles(OrderRootType.SOURCES)));
          }
          if (isTestChunk &&
              (orderEntry instanceof ModuleSourceOrderEntry) &&
              orderEntry.getOwnerModule() == module) {
            //add Java sources for test compilation
            sourceDependencies.addAll(Arrays.asList(orderEntry.getFiles(OrderRootType.SOURCES)));
          }
        }
        for (VirtualFile file : cpVFiles) {
          String path = file.getPath();
          int jarSeparatorIndex = path.indexOf(JarFileSystem.JAR_SEPARATOR);
          if (jarSeparatorIndex > 0) {
            path = path.substring(0, jarSeparatorIndex);
          }
          printer.print(path);
          printer.print(File.pathSeparator);
        }
      }
    }
    printer.println();

    for (VirtualFile file : chunk.getFilesToCompile()) {
      printer.println(file.getPath());
    }
    for (VirtualFile sourceDependency : sourceDependencies) {
      addJavaSourceFiles(printer, sourceDependency);
    }
    printer.close();
  }

  private static boolean isTestChunk(ModuleChunk chunk) {
    boolean isTestChunk = false;
    final Module[] modules = chunk.getModules();
    if (modules.length > 0) {
      ProjectRootManager rm = ProjectRootManager.getInstance(modules[0].getProject());
      for (VirtualFile file : chunk.getSourceRoots()) {
        if (file != null && rm.getFileIndex().isInTestSourceContent(file)) {
          isTestChunk = true;
          break;
        }
      }
    }
    return isTestChunk;
  }

  private static void addJavaSourceFiles(PrintStream stream, VirtualFile src) {
    if (src.getPath().contains("!/")) return;
    if (src.isDirectory()) {
      for (VirtualFile file : src.getChildren()) {
        addJavaSourceFiles(stream, file);
      }
    } else if (src.getFileType() == StdFileTypes.JAVA) {
      stream.println(src.getPath());
    }
  }


  private boolean allModulesHaveSameScalaSdk(ModuleChunk chunk) {
    if (chunk.getModuleCount() == 0) return false;
    final Module[] modules = chunk.getModules();
    final Module first = modules[0];
    final String firstVersion = ScalaCompilerUtil.getScalaCompilerVersion(ScalaCompilerUtil.getScalaCompilerJarPath(first));

    for (Module module : modules) {
      final String path = ScalaCompilerUtil.getScalaCompilerJarPath(module);
      final String version = ScalaCompilerUtil.getScalaCompilerVersion(path);
      if (!version.equals(firstVersion)) return false;
    }
    return true;
  }

  public void compileFinished() {
    FileUtil.asyncDelete(myTempFiles);
  }

  private Sdk getJdkForStartupCommand(final ModuleChunk chunk) {
    final Sdk jdk = chunk.getJdk();
    if (ApplicationManager.getApplication().isUnitTestMode() && JavacSettings.getInstance(myProject).isTestsUseExternalCompiler()) {
      final String jdkHomePath = CompilerConfigurationImpl.getTestsExternalCompilerHome();
      if (jdkHomePath == null) {
        throw new IllegalArgumentException("[TEST-MODE] Cannot determine home directory for JDK to use javac from");
      }
      // when running under Mock JDK use VM executable from the JDK on which the tests run
      return new MockJdkWrapper(jdkHomePath, jdk);
    }
    return jdk;
  }

}