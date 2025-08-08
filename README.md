# Temporal Java Workshop

This project walks through starting a brand new Temporal Project and incrementally adding functionality. It includes

* Workflows & Activities
* Tests
* Signals and Queries

For more detailed courses, project based tutorials and example applications, check out our [courses](https://learn.temporal.io/)

## Prerequisites

* [Temporal CLI](https://docs.temporal.io/cli#install)
* Java JDK 8 or higher
* [Apache Maven](https://formulae.brew.sh/formula/maven) 

## Create a new Temporal Java Project

In a terminal window, create a new folder and switch to the new directory:

```bash
mkdir java-workshop
cd java-workshop
```

Create a new Java project 

### Maven 
Run the following Maven command to create a new project:

```bash
mvn -B archetype:generate \
-DgroupId=helloworldapp \
-DartifactId=app \
-DarchetypeArtifactId=maven-archetype-quickstart \
-DarchetypeVersion=1.4
```

This command creates a directory name ```app``` which contans your Java application ```helloworldapp```

## Update the application dependencies

Inside your app/pom.xml you need to make sure that the Java Version is set to 1.8. 

```xml
  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <maven.compiler.source>1.8</maven.compiler.source>
    <maven.compiler.target>1.8</maven.compiler.target>
  </properties>
```

Now add the Temporal SDK as a dependency, along with a few other libraries for testing and logging. 

Change the dependencies section to look like this:

```xml
  <dependencies>
    <dependency>
      <groupId>io.temporal</groupId>
      <artifactId>temporal-sdk</artifactId>
      <version>1.19.0</version>
    </dependency>

    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-nop</artifactId>
      <version>2.0.6</version>
    </dependency>

    <dependency>
      <groupId>io.temporal</groupId>
      <artifactId>temporal-testing</artifactId>
      <version>1.19.0</version>
      <scope>test</scope>
    </dependency>  

    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>4.11</version>
      <scope>test</scope>
    </dependency>
    
    <dependency>
      <groupId>org.mockito</groupId>
      <artifactId>mockito-core</artifactId>
      <version>5.1.1</version>
      <scope>test</scope>
    </dependency>  
  </dependencies>
```

Finally, if you are using an IDE like IntelliJ or Visual Studio Code, add a maven compiler plugin immediately after the ```</pluginManagement>``` tag. 
If you plan on only running this from a terminal window, you can skip this step.

```xml
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
          <source>8</source>
          <target>8</target>
        </configuration>
      </plugin>
    </plugins>
```

## Build the application

Build the application and make sure it successfully compiles

```bash
cd app
mvn compile 
```

The maven command that was executed earlier, creates an app.java file. This won't be needed for this workshop, so let's delete it

```bash
rm -f src/main/java/helloworldapp/App.java
```

Now that you have the project configured correctly, let's create workflows and 
activities.

## Create a Workflow

A workflow is a sequence of steps for a business process. Workflows are code, 
which executes effectively once to completion. Workflows must be deterministic, 
meaning that if you pass in the same parameters to the workflow, Temporal expects
it to take the same code path each and every time.

To create a Temporal Workflow in Java, you must first create an interface. 
That interface is then annotated with ```@WorkflowInterface```. Inside the 
interface is a method that is used to kick off the workflow. It is annotated 
with ```@WorkflowMethod```.

Let's create the WorkflowInterface. 

Create a new file ```HelloWorldWorkflow.java``` in the following location 
```app/srce/main/java/helloworldapp/``` and add the following code to create 
the interface definition:

```java
package helloworldapp;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface HelloWorldWorkflow {

    /**
     * This is the method that is executed when the Workflow Execution is started. The Workflow
     * Execution completes when this method finishes execution.
     */
    @WorkflowMethod
    String getGreeting(String name);
}
```

Notice the ```@WorkflowInterface``` annotation that lets the Temporal SDK 
know this is a Temporal Workflow. In order to run this worklow, the 
```getGreeting()``` method is annotated with  ```@WorkflowMethod```

Create ```HelloWorldWorkflowImpl.java``` in the same folder and add the following 
code:

```java
package helloworldapp;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;

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

    // This is the entry point to the Workflow.
    @Override
    public String getGreeting(String name) {

        /**   
         * If there were other Activity methods they would be orchestrated here or from within other Activities.
         * This is a blocking call that returns only after the activity has completed.
         */
        return activity.composeGreeting(name);
    }
}
```

There are few things to notice in the code above. First, we have specified a 
Start-to-Close Timeout for your activity will be one minute, which means that 
your Activity has a maximum of one minute to finish before it times out. Be 
sure to always set an activity's Start-to-Close Timeout. 

Another thing to notice is that we create a ```HelloWorldActivities``` stub 
that acts as a proxy for calling activities. In particular, the Activity Stub 
is generated on the **interface** and not the implementation. 

Lastly, the ```HelloWorldWorkflowImpl``` implements the ```getGreeting()``` 
method which is the entrypoint for our workflow.

## Create an Activity

Activities in Temporal represent anything that may be prone to failure. This 
includes things like calling APIs, reading and writing databases, file I/O, 
and more. By default, activities are retried until they succeed. Unlike 
Workflows, Activities are not required to be deterministic. We do recommend 
that Activities are idempotent.

Similar to the way we create workflows, activities follow the same pattern. 
We first define an interface and then implement that interface. 

Create a new file ```HelloWorldActivities.java``` in the following location 
```app/srce/main/java/helloworldapp/``` and add the following code to create
the interface definition: 

```java
package helloworldapp;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface HelloWorldActivities {

    // Define your activity methods which can be called during workflow execution
    String composeGreeting(String name);
    
}
```

Notice the ```@ActivityInterface``` annotation that lets the Temporal SDK know 
this is a Temporal Activity. In this example, we only have one activity method 
```composeGreeting()```. In more complex workflows, there could be many activity 
methods.

Create ```HelloWorldActivitiesImpl.java``` in the same folder and add the 
following code:

```java
package helloworldapp;

public class HelloWorldActivitiesImpl implements HelloWorldActivities {

    @Override
    public String composeGreeting(String name) {
        return "Hello " + name + "!";
    }

}
```

This class implements the single method from the interface ```composeGreeting()``` 
that returns a "Hello" message using the passed in parameter.

For both workflows and activities, there are restrictions on the types for both 
parameters and return values. They must be seralizable and you should avoid 
passing in large amounts of data. Practically speaking, large means more than 1 MB. 

## Create a Worker

A worker is a standalone process that can host Workflows and Activities to 
execute your code. The Temporal Service orchestrates the execution of code 
by adding Tasks to a Task Queue. Workers poll task queues looking for work 
to be done. When a worker accepts a Task, it will execute the necessary code 
and report back the result (or error) back to the Temporal Service. After the 
worker runs the code, it communicates the results back to the Temporal Server.

When you start a workflow, you specify the name of the Task Queue the Workflow uses. A Worker listens and polls on the Task Queue looking for work to do.

To configure a Worker process using the Java SDK, you create an instance of 
```Worker``` and give it the name of the Task Queue to poll.

You'll connect to the Temporal Service using a Temporal Client, which provides 
a set of APIs to communicate with a Temporal Service. You'll use Clients to 
interact with existing Workflows or to start new ones.

Since you'll use the Task Queue name in multiple places in your project, create 
the file ```Shared.java``` in ```app/src/main/java/helloworldapp``` and define 
the Task Queue name there:

```java
package helloworldapp;

public interface Shared {

    // Define the task queue name
    final String HELLO_WORLD_TASK_QUEUE = "HelloWorldTaskQueue";

}
```

Let's create the Worker process. We'll create a standalone Worker program to 
see how all of the components work together.

Create the file ```HelloWorldWorker.java``` in ```app/src/main/java/helloworldapp``` and add the following code:

```java
package helloworldapp;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class HelloWorldWorker {

    public static void main(String[] args) {

        // Get a Workflow service stub.
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

        /*
        * Get a Workflow service client which can be used to start, Signal, and Query Workflow Executions.
        */
        WorkflowClient client = WorkflowClient.newInstance(service);

        /*
        * Define the workflow factory. It is used to create workflow workers that poll specific Task Queues.
        */
        WorkerFactory factory = WorkerFactory.newInstance(client);

        /*
        * Define the workflow worker. Workflow workers listen to a defined task queue and process
        * workflows and activities.
        */
        Worker worker = factory.newWorker(Shared.HELLO_WORLD_TASK_QUEUE);

        /*
        * Register our workflow implementation with the worker.
        * Workflow implementations must be known to the worker at runtime in
        * order to dispatch workflow tasks.
        */
        worker.registerWorkflowImplementationTypes(HelloWorldWorkflowImpl.class);

        /*
        * Register our Activity Types with the Worker. Since Activities are stateless and thread-safe,
        * the Activity Type is a shared instance.
        */
        worker.registerActivitiesImplementations(new HelloWorldActivitiesImpl());

        /*
        * Start all the workers registered for a specific task queue.
        * The started workers then start polling for workflows and activities.
        */
        factory.start();

    }
}
```

This program implements a service stub to be used when instantiating 
the client. The code first instantiates a factory and then creates a new 
worker that listens on a Task Queue. This worker will only process workflows 
and activities from this Task Queue. You register the Workflow and Activity 
with the Worker and then start the worker using ```factory.start()```.

## Starting a Workflow Execution 

You can start a Workflow Execution by using the Temporal CLI or by writing 
code using the Temporal SDK. Let's use the Temporal SDK to start the Workflow, 
which is how most real-world applications work.

Starting a Workflow Execution using the Temporal SDK involves connecting to 
the Temporal Server, specifying the Task Queue the Workflow should use, and 
starting the Workflow with the input parameters it expects. In a real 
application, you may invoke this code when someone submits a form, presses 
a button, or visits a certain URL. We'll create a separate Java class that 
starts the Workflow Execution.

Create ```InitiateHelloWorld.java``` in ```app/src/main/java/helloworldapp/``` 
and add the following code to the file to connect to the server and start 
the Workflow:

```java
package helloworldapp;

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
         */
        WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setWorkflowId(WORKFLOW_ID)
                    .setTaskQueue(Shared.HELLO_WORLD_TASK_QUEUE)
                    .build();

        // Create the workflow client stub. It is used to start our workflow execution.
        HelloWorldWorkflow workflow = client.newWorkflowStub(HelloWorldWorkflow.class, options);

        /*
         * Execute our workflow and wait for it to complete. The call to our getGreeting method is
         * synchronous.
         * 
         * Replace the parameter "World" in the call to getGreeting() with your name.
         */
        String greeting = workflow.getGreeting("World");

        String workflowId = WorkflowStub.fromTyped(workflow).getExecution().getWorkflowId();
        // Display workflow execution results
        System.out.println(workflowId + " " + greeting);
        System.exit(0);
    }
}
```

Like the Worker you created, this program uses stubs and a client to connect 
to the Temporal server. It then specifies a Workflow ID for the Workflow, as 
well as the Task Queue. The Worker you configured is looking for tasks on that 
Task Queue.

A Workflow ID is a unique identifier in a namespace. We strongly recommend 
to use an identifier that is meaningful to the business process or entity. 
For example, you might consider using a customer ID as part of the Workflow 
ID, if you create a workflow for each customer. This would make it easier 
to find all of the Workflow Executions related to that customer later.

The program then creates a stubbed instance of your Workflow, 
taking the interface class of your workflow along with the options you 
have set as parameters. This stub looks like an implementation of the 
interface, but is used to communicate with the Temporal Server under the hood. 

You can get the results from your Workflow right away, or you can get 
the results at a later time. This implementation stores the results in 
the ```greeting``` variable after the ```getGreeting()``` method is called, 
which blocks the program's execution until the Workflow Execution completes.

You have a Workflow, an Activity, a Worker, and a way to start a Workflow 
Execution. It's time to run the Workflow.

## Run the application

For this part of the workshop, we'll need three different terminal windows.

In the first window start the temporal development server:

```bash
temporal server start-dev
```

Now start the worker
```bash
cd app
mvn compile exec:java -Dexec.mainClass="helloworldapp.HelloWorldWorker"
```

And finally, start the Workflow Execution:

```bash
cd app
mvn exec:java -Dexec.mainClass="helloworldapp.InitiateHelloWorld"
```

Alternatively, you can use the Temporal CLI to start the workflow:

```bash
temporal workflow start --type HelloWorldWorkflow --task-queue HelloWorldTaskQueue --input '"World"'
```

Let's explore the Temporal UI to see the workflow and explore the history.

In a browser open up the following address [http://localhost:8233](http://localhost:8233]). 
Click on HelloWorldWorkflowID and notice all of the information available to help you 
identify potential issues. Be sure to click on ComposeGreeting in the Event History so you can see the input and results of the activity. Experiment with the tabs at the bottom of the UI labled "All" and "Compact". What do you notice? 

Stop the worker by using <CTRL-C> in the terminal window where the worker 
is running. You can leave the Temporal Server running.

## Testing

The Temporal Java SDK includes classes and methods that help you test your Workflow executions. 
Let's add a basic unit test to the application to make sure the Workflow works as expected.

You'll use JUnit 4 build your test cases to test your Workflow and Activity. You'll test the 
integration of the Activity and the Workflow by using Temporal's built in Test Environment. 
You'll then mock the Activity so you can test the Workflow in isolation.

Let's add a few unit tests to our application to make sure things are working as expected. 
Test code lives in ```app/src/test/java/helloworldapp```. Your build tool generates a default 
```AppTest.java``` in that location. Delete it:

```bash
rm -f src/test/java/helloworldapp/AppTest.java
```

Create a new file called ```HelloWorldWorkflowTest.java``` in ```app/src/test/java/``` that 
contains the following code:

```java
package helloworldapp;

import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class HelloWorldWorkflowTest {

    @Rule
    public TestWorkflowRule testWorkflowRule =
            TestWorkflowRule.newBuilder()
                    .setWorkflowTypes(HelloWorldWorkflowImpl.class)
                    .setDoNotStart(true)
                    .build();

    @Test
    public void testIntegrationGetGreeting() {
        testWorkflowRule.getWorker().registerActivitiesImplementations(new HelloWorldActivitiesImpl());
        testWorkflowRule.getTestEnvironment().start();

        HelloWorldWorkflow workflow =
                testWorkflowRule
                        .getWorkflowClient()
                        .newWorkflowStub(
                                HelloWorldWorkflow.class,
                                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
        String greeting = workflow.getGreeting("John");
        assertEquals("Hello John!", greeting);
        testWorkflowRule.getTestEnvironment().shutdown();
    }

    @Test
    public void testMockedGetGreeting() {
        HelloWorldActivities formatActivities = mock(HelloWorldActivities.class, withSettings().withoutAnnotations());
        when(formatActivities.composeGreeting(anyString())).thenReturn("Hello World!");
        testWorkflowRule.getWorker().registerActivitiesImplementations(formatActivities);
        testWorkflowRule.getTestEnvironment().start();

        HelloWorldWorkflow workflow =
                testWorkflowRule
                        .getWorkflowClient()
                        .newWorkflowStub(
                                HelloWorldWorkflow.class,
                                WorkflowOptions.newBuilder().setTaskQueue(testWorkflowRule.getTaskQueue()).build());
        String greeting = workflow.getGreeting("World");
        assertEquals("Hello World!", greeting);
        testWorkflowRule.getTestEnvironment().shutdown();
    }
}
```

The first test, ```testIntegrationGetGreeting```, creates a test execution environment to test the 
integration between the Activity and the Workflow. The second test, ```testMockedGetGreeting```, mocks 
the Activity implementation so it returns a successful execution. The test then executes the Workflow 
in the test environment and checks for a successful execution. Finally, the tests ensures the Workflow's 
return value returns the expected value.

Run the following command from the project root (e.g. the app folder) to execute the unit tests:

```bash
mvn compile test
```

You'll see output similar to the following that shows that the test was successful. 

```bash
... removed other build output 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running helloworldapp.HelloWorldWorkflowTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.879 s - in helloworldapp.HelloWorldWorkflowTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

You have a working application and a test to ensure the Workflow executes as expected.

## Adding Signal and Query to the workflow

In the ```HelloWorldWorkflow.java``` (located in the ```app/src/main/java/helloworldapp/``` folder) file,
let's add a signal and query handler. Change the code to look like this:

```java
package helloworldapp;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface HelloWorldWorkflow {

    /**
     * This is the method that is executed when the Workflow Execution is started. The Workflow
     * Execution completes when this method finishes execution.
     */
    @WorkflowMethod
    String getGreeting(String name);
    
    @SignalMethod
    void approve();
    
    @QueryMethod
    String currentState();
}
```

We added two additional methods: ```approve()``` which uses a Signal to change the state of the
workflow and ```currentState()``` which uses a Query to return the state of the workflow execution.

Next, we need to update the ```HelloWorldWorkflowImpl.java``` to add these methods and add some
additional code that will support these changes.

```java
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
```

After saving this file, run the tests again and notice that the test is now broken:

```bash
mvn compile test
```

If you look at the errors, notice this line:

```text

[ERROR] Tests run: 2, Failures: 0, Errors: 2, Skipped: 0, Time elapsed: 0.928 s <<< FAILURE! - in helloworldapp.HelloWorldWorkflowTest
[ERROR] testMockedGetGreeting(helloworldapp.HelloWorldWorkflowTest)  Time elapsed: 0.679 s  <<< ERROR!
io.temporal.client.WorkflowFailedException: Workflow execution {workflowId='be1fec43-f2a1-469c-bbb6-447371361c67', runId='', workflowType='HelloWorldWorkflow'} failed. Metadata: {closeEventType='EVENT_TYPE_WORKFLOW_EXECUTION_FAILED', retryState='RETRY_STATE_UNSPECIFIED', workflowTaskCompletedEventId=14'}
        at helloworldapp.HelloWorldWorkflowTest.testMockedGetGreeting(HelloWorldWorkflowTest.java:49)
Caused by: io.temporal.failure.ApplicationFailure: message='Approval not received within 30 seconds', type='Operation Timed Out', nonRetryable=false

[ERROR] testIntegrationGetGreeting(helloworldapp.HelloWorldWorkflowTest)  Time elapsed: 0.013 s  <<< ERROR!
io.temporal.client.WorkflowFailedException: Workflow execution {workflowId='28c1ad0a-34c6-478c-b21a-33e307134f5e', runId='', workflowType='HelloWorldWorkflow'} failed. Metadata: {closeEventType='EVENT_TYPE_WORKFLOW_EXECUTION_FAILED', retryState='RETRY_STATE_UNSPECIFIED', workflowTaskCompletedEventId=14'}
        at helloworldapp.HelloWorldWorkflowTest.testIntegrationGetGreeting(HelloWorldWorkflowTest.java:31)
Caused by: io.temporal.failure.ApplicationFailure: message='Approval not received within 30 seconds', type='Operation Timed Out', nonRetryable=false
```

Both are failing because the approval was not sent. Let's modify the tests so that they pass:

```java
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
```

After you have saved these changes, re-run the tests:

```bash
mvn compile test
```

And now all tests pass. 

We now need to modify the Workflow Starter found in ```InitiateHelloWorld```. We'll add code 
to retrieve the current state of the workflow using a query, and approve the workflow by 
sending a signal. 

Notice how we are using Workflow Stubs for starting the workflow asynchronously as well as 
performing the signal and queries.

```java
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
```

If your worker was previously running, stop it and restart it:

```bash
mvn compile exec:java -Dexec.mainClass="helloworldapp.HelloWorldWorker"
```

Now let's start the workflow:

```bash
mvn compile exec:java -Dexec.mainClass="helloworldapp.InitiateHelloWorld"
```

The output should look similar to this:

```text
The state of the workflow is Starting
After approval, the state is Complete!
WorkflowID is HelloWorldWorkflowID and the greeting is Hello World!
```

If you want to run the workflow from the command line, you can start it with this command:

```bash
temporal workflow start --type HelloWorldWorkflow --task-queue HelloWorldTaskQueue --workflow-id HelloWorldWorkflowID --input '"World"'
```

And then signal the workflow using the Temporal CLI with this command:

```bash
temporal workflow signal --workflow-id HelloWorldWorkflowID --name approve
```

To view the results, look in the [Temporal UI](http://localhost:8233) or you can get the results from
the command line like this:

```bash
temporal workflow describe --workflow-id HelloWorldWorkflowID
```

The output will look similar to this:

```text
Execution Info:
  WorkflowId            HelloWorldWorkflowID
  RunId                 01988691-008a-7c87-b53d-d428d0930602
  Type                  HelloWorldWorkflow
  Namespace             default
  TaskQueue             HelloWorldTaskQueue
  AssignedBuildId        
  StartTime             2 minutes ago
  CloseTime             2 minutes ago
  ExecutionTime         2 minutes ago
  SearchAttributes      map[BuildIds:metadata:{key:"encoding"  value:"json/plain"}  metadata:{key:"type"  value:"KeywordList"}  data:"[\"unversioned\"]"]
  StateTransitionCount  10
  HistoryLength         16
  HistorySize           1975
  RootWorkflowId        HelloWorldWorkflowID
  RootRunId             01988691-008a-7c87-b53d-d428d0930602
Extended Execution Info:
  CancelRequested    false
  OriginalStartTime  2 minutes ago

Results:
  RunTime         7.51s
  Status          COMPLETED
  Result          "Hello World!"
  ResultEncoding  json/plain
```


