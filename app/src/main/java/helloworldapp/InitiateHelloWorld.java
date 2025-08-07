package helloworldapp;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class InitiateHelloWorld {

    public static void main(String[] args) throws Exception {

        // This gRPC stubs wrapper talks to the local docker instance of the Temporal service.
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

        // WorkflowClient can be used to start, signal, query, cancel, and terminate Workflows.
        WorkflowClient client = WorkflowClient.newInstance(service);

        // Define our workflow unique id
        final String WORKFLOW_ID = "HelloWorldWorkflowID";

        /*
         * Set Workflow options such as WorkflowId and Task Queue so the worker knows where to list and which workflows to execute.
        // This gRPC stubs wrapper talks to the local docker instance of the Temporal service.
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

        // WorkflowClient can be used to start, signal, query, cancel, and terminate Workflows.
        WorkflowClient client = WorkflowClient.newInstance(service);

        // Define our workflow unique id
        final String WORKFLOW_ID = "HelloWorldWorkflowID";

        /*
         * Set Workflow options such as WorkflowId and Task Queue so the worker knows where to list and which workflows to execute.
         */
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(Shared.HELLO_WORLD_TASK_QUEUE)
                .build();

        // Create the workflow client stub. It is used to start our workflow execution.
        WorkflowStub workflowStub = client.newUntypedWorkflowStub(
                "HelloWorldWorkflow",
                options);

        /*
         * Execute our workflow and DO NOT WAIT for it to complete.
         * 
         * Replace the parameter "World" in the call to getGreeting() with your name.
         */
        WorkflowExecution workflowExecution = workflowStub.start("World");

        // get the state of the workflow
        String state = workflowStub.query("currentState", String.class);
        System.out.println("The state of the workflow is " + state);

        // wait a bit before signaling approval
        Thread.sleep(3000);

        workflowStub.signal("approve");

        String state2 = workflowStub.query("currentState", String.class);
        System.out.println("After approval, the state is " + state2);

        // Get the result of the workflow
        String greeting = workflowStub.getResult(String.class);

        // Get the workflow ID
        String workflowId = workflowExecution.getWorkflowId();
        // Display workflow execution results

        System.out.println("WorkflowID is " + workflowId + " and the greeting is " + greeting);
        System.exit(0);
    }
}