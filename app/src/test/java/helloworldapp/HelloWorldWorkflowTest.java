package helloworldapp;

import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class HelloWorldWorkflowTest {

    @Rule
    public TestWorkflowRule testWorkflowRule =
            TestWorkflowRule.newBuilder()
                    .setWorkflowTypes(HelloWorldWorkflowImpl.class)
                    .setDoNotStart(true)
// If you want to use temporal server locally, set the following properties:
//                    .setUseExternalService(true)
//                    .setTarget("localhost:7233")
//                    .setNamespace("default")
                    .build();


    @Test
    public void testIntegrationGetGreeting() {
        testWorkflowRule.getWorker().registerActivitiesImplementations(new HelloWorldActivitiesImpl());
        testWorkflowRule.getTestEnvironment().start();

        WorkflowStub workflowStub =
                testWorkflowRule
                        .getWorkflowClient()
                        .newUntypedWorkflowStub(
                                "HelloWorldWorkflow",
                                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());

        // Start the workflow but don't wait for it to complete
        workflowStub.start("John");

        // wait for a few seconds before proceeding with the approval
        testWorkflowRule.getTestEnvironment().sleep(Duration.ofSeconds(3));

        workflowStub.signal("approve");

        String greeting = workflowStub.getResult(String.class);
        assertEquals("Hello John!", greeting);
        testWorkflowRule.getTestEnvironment().shutdown();
    }

    @Test
    public void testMockedGetGreeting() {
        HelloWorldActivities formatActivities = mock(HelloWorldActivities.class, withSettings().withoutAnnotations());
        when(formatActivities.composeGreeting(anyString())).thenReturn("Hello World!");
        testWorkflowRule.getWorker().registerActivitiesImplementations(formatActivities);
        testWorkflowRule.getTestEnvironment().start();

        WorkflowStub workflowStub =
                testWorkflowRule
                        .getWorkflowClient()
                        .newUntypedWorkflowStub(
                                "HelloWorldWorkflow",
                                WorkflowOptions.newBuilder()
                                        .setTaskQueue(testWorkflowRule.getTaskQueue()).build());

        // Start the workflow but don't wait for it to complete
        workflowStub.start("World");

        // wait for a few seconds before proceeding with the approval
        testWorkflowRule.getTestEnvironment().sleep(Duration.ofSeconds(3));

        workflowStub.signal("approve");

        String greeting = workflowStub.getResult(String.class);
        assertEquals("Hello World!", greeting);
        testWorkflowRule.getTestEnvironment().shutdown();
    }
}