package net.masterthought.jenkins;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.nio.charset.Charset;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import javax.annotation.Nonnull;
import jenkins.tasks.SimpleBuildStep;
import org.apache.tools.ant.DirectoryScanner;
import org.kohsuke.stapler.DataBoundConstructor;

//import net.masterthought.cucumber.Configuration;
//import net.masterthought.cucumber.ReportBuilder;
//import net.masterthought.cucumber.Reportable;

public class SafeReportPublisher extends Publisher implements SimpleBuildStep {

//    private final static String DEFAULT_FILE_INCLUDE_PATTERN = "**/*.json";
//	private final static String DEFAULT_FILE_INCLUDE_PATTERN = "**/*.*";

//    private final static String TRENDS_DIR = "benchmarkReport";
//    private final static String TRENDS_FILE = "cucumber-trends.json";

    public final String archivedReportDirectory;
    public final String htmlIndexPage;
//    public final String fileExcludePattern;
//    public final int trendsLimit;

//    public final int failedStepsNumber;
//    public final int skippedStepsNumber;
//    public final int pendingStepsNumber;
//    public final int undefinedStepsNumber;
//    public final int failedScenariosNumber;
//    public final int failedFeaturesNumber;
//    public final boolean parallelTesting;

    public List<Classification> classifications = Collections.emptyList();

    public final Result buildStatus;
    
    public static final String BASE_DIRECTORY = "benchmarkReport";
//    public static final String HOME_PAGE = "network/NetworkIndex.html";
    public static final String WRAPPER_NAME = "safereport-wrapper.html";
    
	@DataBoundConstructor
	public SafeReportPublisher(String archivedReportDirectory, String htmlIndexPage, String buildStatus,
			List<Classification> classifications) {

		this.archivedReportDirectory = archivedReportDirectory;
		this.htmlIndexPage = htmlIndexPage;
		this.buildStatus = buildStatus == null ? null : Result.fromString(buildStatus);
		// don't store the classifications if there was no element provided
		if (classifications != null) {
			this.classifications = classifications;
		}
	}
	
//    @DataBoundConstructor
//    public SafeReportPublisher(String jsonReportDirectory, String fileIncludePattern, String fileExcludePattern,
//                                   int trendsLimit, int failedStepsNumber, int skippedStepsNumber, int pendingStepsNumber,
//                                   int undefinedStepsNumber, int failedScenariosNumber, int failedFeaturesNumber,
//                                   String buildStatus, boolean parallelTesting, List<Classification> classifications) {
//
//        this.jsonReportDirectory = jsonReportDirectory;
//        this.fileIncludePattern = fileIncludePattern;
////        this.fileExcludePattern = fileExcludePattern;
////        this.trendsLimit = trendsLimit;
////        this.failedStepsNumber = failedStepsNumber;
////        this.skippedStepsNumber = skippedStepsNumber;
////        this.pendingStepsNumber = pendingStepsNumber;
////        this.undefinedStepsNumber = undefinedStepsNumber;
////        this.failedScenariosNumber = failedScenariosNumber;
////        this.failedFeaturesNumber = failedFeaturesNumber;
//        this.buildStatus = buildStatus == null ? null : Result.fromString(buildStatus);
////        this.parallelTesting = parallelTesting;
//        // don't store the classifications if there was no element provided
//        if (classifications != null) {
//            this.classifications = classifications;
//        }
//    }

    public List<Classification> getClassifications() {
        return classifications;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener)
            throws InterruptedException, IOException {

        generateReport(run, workspace, listener);

//        SafeArchiveServingRunAction caa = new SafeArchiveServingRunAction(new File(run.getRootDir(), ReportBuilder.BASE_DIRECTORY),
//                ReportBuilder.BASE_DIRECTORY, ReportBuilder.HOME_PAGE, CucumberReportBaseAction.ICON_NAME, Messages.SidePanel_DisplayName());
        SafeArchiveServingRunAction caa = new SafeArchiveServingRunAction(new File(run.getRootDir(), BASE_DIRECTORY),
              BASE_DIRECTORY, htmlIndexPage, SafeReportBaseAction.ICON_NAME, Messages.SidePanel_DisplayName());
        run.addAction(caa);
    }
    
    private void generateHTMLIndexPage(File path, @Nonnull TaskListener listener) throws IOException{
    	listener.getLogger().println("***** Generating wrappedReport.html");
    	BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), Charset.defaultCharset()));
		try {
//			bw.write("<meta http-equiv=\"refresh\" content=\"0; url=network/NetworkIndex.html\" />");
			bw.write("<meta http-equiv=\"refresh\" content=\"0; url=" + htmlIndexPage + "\" />");
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    }

    private void generateReport(Run<?, ?> build, FilePath workspace, TaskListener listener) throws InterruptedException, IOException {
        log(listener, "Preparing Ralph Benchmark Reports");

//        // create directory where trends will be stored
//        final File trendsDir = new File(build.getParent().getRootDir(), TRENDS_DIR);
//        if (!trendsDir.exists()) {
//            if (!trendsDir.mkdir()) {
//                throw new IllegalStateException("Could not create directory for trends: " + trendsDir);
//            }
//        }

        // source directory (possibly on slave)
        FilePath inputDirectory = new FilePath(workspace, archivedReportDirectory);
        if (!inputDirectory.exists()) {
        	throw new IllegalStateException("Could not find source directory: " + inputDirectory);
        }

        File directoryForReport = build.getRootDir();
//        File directoryJsonCache = new File(directoryForReport, ReportBuilder.BASE_DIRECTORY + File.separatorChar + ".cache");
        File directoryJsonCache = new File(directoryForReport, BASE_DIRECTORY);
        
//        int copiedFiles = inputDirectory.copyRecursiveTo(DEFAULT_FILE_INCLUDE_PATTERN, new FilePath(directoryJsonCache));
        int copiedFiles = inputDirectory.copyRecursiveTo(new FilePath(directoryJsonCache));
        log(listener, String.format("Copied %d report files from workspace \"%s\" to reports directory \"%s\"",
                copiedFiles, inputDirectory.getRemote(), directoryJsonCache));
        
        log(listener, "directoryJsonCache: "+ directoryJsonCache);
        generateHTMLIndexPage(new File(directoryJsonCache, WRAPPER_NAME),  listener);
//
//        // exclude JSONs that should be skipped (as configured by the user)
//        String[] jsonReportFiles = findJsonFiles(directoryJsonCache, fileIncludePattern, fileExcludePattern);
//        List<String> jsonFilesToProcess = fullPathToJsonFiles(jsonReportFiles, directoryJsonCache);
//        log(listener, String.format("Filtered out %d json files:", jsonReportFiles.length));
//        for (String jsonFile : jsonFilesToProcess) {
//            log(listener, jsonFile);
//        }
//
//        String buildNumber = Integer.toString(build.getNumber());
//        // this works for normal and multi-config/matrix jobs
//        // for matrix jobs, this will include the matrix job name and the specific
//        // configuration/permutation name as well. this also includes the '/' so
//        // we don't have to modify how the cucumber plugin report generator's links
//        String projectName = build.getParent().getDisplayName();
//
//        Configuration configuration = new Configuration(directoryForReport, projectName);
//        configuration.setParallelTesting(parallelTesting);
//        configuration.setRunWithJenkins(true);
//        configuration.setBuildNumber(buildNumber);
//        configuration.setTrends(new File(trendsDir, TRENDS_FILE), trendsLimit);
//        for (Classification classification : classifications) {
//            configuration.addClassifications(classification.key, classification.value);
//        }
//
//        ReportBuilder reportBuilder = new ReportBuilder(jsonFilesToProcess, configuration);
//        Reportable result = reportBuilder.generateReports();
//
//        if (hasReportFailed(result, listener)) {
//            // redefine build result if it was provided by plugin configuration
//            if (buildStatus != null) {
//                log(listener, "Build status is changed to " + buildStatus.toString());
//                build.setResult(buildStatus);
//            } else {
//                log(listener, "Build status is left unchanged");
//            }
//        }
    }

//    private String[] findJsonFiles(File targetDirectory, String fileIncludePattern, String fileExcludePattern) {
//        DirectoryScanner scanner = new DirectoryScanner();
//
//        if (fileIncludePattern == null || fileIncludePattern.isEmpty()) {
//            scanner.setIncludes(new String[]{DEFAULT_FILE_INCLUDE_PATTERN});
//        } else {
//            scanner.setIncludes(new String[]{fileIncludePattern});
//        }
//        if (fileExcludePattern != null) {
//            scanner.setExcludes(new String[]{fileExcludePattern});
//        }
//        scanner.setBasedir(targetDirectory);
//        scanner.scan();
//        return scanner.getIncludedFiles();
//    }

//    private List<String> fullPathToJsonFiles(String[] jsonFiles, File targetBuildDirectory) {
//        List<String> fullPathList = new ArrayList<>();
//        for (String file : jsonFiles) {
//            fullPathList.add(new File(targetBuildDirectory, file).getAbsolutePath());
//        }
//        return fullPathList;
//    }
//
//    boolean hasReportFailed(Reportable result, TaskListener listener) {
//        // happens when the resport could not be generated
//        if (result == null) {
//            log(listener, "Missing report result - report was not successfully completed");
//            return true;
//        }
//
//        if (result.getFailedSteps() > failedStepsNumber) {
//            log(listener, String.format("Found %d failed steps, while expected not more than %d",
//                    result.getFailedSteps(), failedStepsNumber));
//            return true;
//        }
//        if (result.getSkippedSteps() > skippedStepsNumber) {
//            log(listener, String.format("Found %d skipped steps, while expected not more than %d",
//                    result.getSkippedSteps(), skippedStepsNumber));
//            return true;
//        }
//        if (result.getPendingSteps() > pendingStepsNumber) {
//            log(listener, String.format("Found %d pending steps, while expected not more than %d",
//                    result.getPendingSteps(), pendingStepsNumber));
//            return true;
//        }
//        if (result.getUndefinedSteps() > undefinedStepsNumber) {
//            log(listener, String.format("Found %d undefined steps, while expected not more than %d",
//                    result.getUndefinedSteps(), undefinedStepsNumber));
//            return true;
//        }
//
//        if (result.getFailedScenarios() > failedScenariosNumber) {
//            log(listener, String.format("Found %d failed scenarios, while expected not more than %d",
//                    result.getFailedScenarios(), failedScenariosNumber));
//            return true;
//        }
//        if (result.getFailedFeatures() > failedFeaturesNumber) {
//            log(listener, String.format("Found %d failed features, while expected not more than %d",
//                    result.getFailedFeatures(), failedFeaturesNumber));
//            return true;
//        }
//
//        return false;
//    }

    private static void log(TaskListener listener, String message) {
        listener.getLogger().println("[RalphBenchmarkReport] " + message);
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        return new SafeReportProjectAction(project);
    }

    public static class Classification extends AbstractDescribableImpl<Classification> {

        String key;
        String value;

        @DataBoundConstructor
        public Classification(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Extension
        public static class DescriptorImpl extends Descriptor<Classification> {

            @Override
            public String getDisplayName() {
                return "";
            }
        }
    }

    @Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		@Override
		public String getDisplayName() {
			return Messages.Plugin_DisplayName();
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}
	}
}
