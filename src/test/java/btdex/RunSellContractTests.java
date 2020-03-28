package btdex;

import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.List;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;


public class RunSellContractTests {
    public static void main(String[] args) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(
                        selectPackage("btdex.dispute"),
                        selectClass(TestTakeRetake.class),
                        selectClass(TestInvalidTakeTake.class),
                        selectClass(TestInvalidTakeReopenWithdraw.class)
                )
                .build();

        Launcher launcher = LauncherFactory.create();
        // Register a listener
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);

        //TestPlan testPlan = launcher.discover(request);

        launcher.execute(request);

        TestExecutionSummary summary = listener.getSummary();
        printReport(summary);


    }
    private static void printReport(TestExecutionSummary summary) {
        System.out.println(
                "\n------------------------------------------" +
                        "\nTests started: " + summary.getTestsStartedCount() +
                        "\nTests failed: " + summary.getTestsFailedCount() +
                        "\nTests succeeded: " + summary.getTestsSucceededCount() +
                        "\n------------------------------------------"
        );

        if(summary.getTestsFailedCount() > 0) {
            for(TestExecutionSummary.Failure f: summary.getFailures()){
                System.out.println(f.getTestIdentifier().getSource() +
                                    "\nException " + f.getException());
            }
        }
    }

}
