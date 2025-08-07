package helloworldapp;

import io.temporal.activity.ActivityOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

public class HelloWorldWorkflowImpl implements HelloWorldWorkflow {

    /* 
     * At least one of the following options needs to be defined:
     * - setStartToCloseTimeout
     * - setScheduleToCloseTimeout
     */
    ActivityOptions options = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(60))
            .build();

    /*
     * Define the HelloWorldActivity stub. Activity stubs are proxies for activity invocations that
     * are executed outside of the workflow thread on the activity worker, that can be on a
     * different host. Temporal is going to dispatch the activity results back to the workflow and
     * unblock the stub as soon as activity is completed on the activity worker.
     * 
     * The activity options that were defined above are passed in as a parameter.
     */
    private final HelloWorldActivities activity = Workflow.newActivityStub(HelloWorldActivities.class, options);
    private String currentStep = "Starting";
    private boolean approved = false;
    private long approvalTime = 30;

    private static final Logger log = Workflow.getLogger(HelloWorldWorkflowImpl.class);

    // This is the entry point to the Workflow.
    @Override
    public String getGreeting(String name) {
        log.info("GetGreeting called {}", name);
        /**   
         * If there were other Activity methods they would be orchestrated here or from within other Activities.
         * This is a blocking call that returns only after the activity has completed.
         */
        String result =  activity.composeGreeting(name);

        log.info("Result from activity is {}. Waiting for approval", result);

        currentStep = "Awaiting Approval";
        // wait for either the signal to arrive or time out
        boolean receivedApproval = Workflow.await(Duration.ofSeconds(approvalTime), () -> approved);
        if (receivedApproval) {
            currentStep = "The workflow has been approved!";
            log.info("Approved!");
        }
        else {
            log.info("Not approved. Timed out");
            currentStep = "The workflow timed out while waiting to be approved";
            // if we wanted to fail the workflow
            // we can throw an exception
            throw ApplicationFailure.newFailure(String.format("Approval not received within %d seconds", approvalTime),"Operation Timed Out");
        }

        currentStep = "Complete!";
        log.info("Returning {}", result);
        return result;
    }

    @Override
    public void approve() {
        approved = true;
    }

    @Override
    public String currentState() {
        return currentStep;
    }
}