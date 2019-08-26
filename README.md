# Compuware ISPW Operations Plugin

This plugin 
* sends an ISPW rest API request to a CES rest endpoint.
* integrates GIT with ISPW.

## ISPW Rest API Features

The following features are available in both Pipeline and traditional
project types:

* ISPW Actions: CreateAssignment, GetAssignmentInfo, GetAssignmentTaskList, GenerateTasksInAssignment, PromoteAssignment, DeployAssignment,RegressAssignment, GetReleaseInfo, GetReleaseTaskList, CreateRelease, GenerateTasksInRelease, GetReleaseTaskGenerateListing, GetReleaseTaskInfo, PromoteRelease, DeployRelease, RegressRelease, GetSetInfoAction, GetSetTaskLlist

### Basic build

The following features are only present in the non-pipeline version of
the plugin. For the Pipeline version, these features are available
programmatically.

* You can send the build parameters as key-value property list
* You can configure CES URL in global configuration and CES token as secret text in Credentials
* You can chose to wait for end of action by polling style (freestyle and pipeline)
* You can chose to wait for end of action by webhook callback (pipeline only)

### Pipeline build

Suppose you want to generate all DEV2 tasks in assignment PLAY000313,
you can create the rest API request to CES server programmatically like so:

```groovy
ispwOperation connectionId: 'c56dcf99-f26c-4b4e-a826-1ae47cc4c9b4', consoleLogResponseBody: true, credentialsId: '733e1263-4334-4852-b4cc-27ebc4685b94', ispwAction: 'GenerateTasksInAssignment', ispwRequestBody: '''assignmentId=PLAY000313
level=DEV2
runtimeConfiguration=TPZP'''
```

Also in a Pipeline job, you can use webhook callback if you don't want polling for status:

```groovy
hook = ispwRegisterWebhook()
echo "...created ISPW webhook - ${hook.getURL()}"

ispwOperation connectionId: 'c56dcf99-f26c-4b4e-a826-1ae47cc4c9b4', consoleLogResponseBody: true, credentialsId: '733e1263-4334-4852-b4cc-27ebc4685b94', ispwAction: 'GenerateTasksInAssignment', ispwRequestBody: '''assignmentId=PLAY000313
level=DEV2
runtimeConfiguration=TPZP
events.name=Completed
events.body=Generated
events.httpHeaders=Jenkins-Crumb:no-crumb
events.credentials=admin:library'''

echo "...waiting ISPW webhook callback - ${hook.getURL()}"
data = ispwWaitForWebhook hook
echo "...ISPW Webhook called back with message - ${data}"
```

## GIT to ISPW Integration Features

![GIT to ISPW design](https://github.com/jenkinsci/compuware-ispw-operations-plugin/blob/CWE-150569-ISPW-Git-Integration---Pipeline-build/ispw%20git%20integration.png)

### Pipeline build example

```
pipeline {

    agent any

    triggers {
        GenericTrigger(
            genericVariables: [
                [key: 'ref', value: '$.changes[0].ref.displayId', expressionType: 'JSONPath', regexpFilter: '^(refs/heads/\\|refs/remotes/origin/)'],
                [key: 'toHash', value: '$.changes[0].toHash', expressionType: 'JSONPath', regexpFilter: '^(refs/heads/\\|refs/remotes/origin/)'],
                [key: 'fromHash', value: '$.changes[0].fromHash', expressionType: 'JSONPath', regexpFilter: '^(refs/heads/\\|refs/remotes/origin/)'],
                [key: 'refId', value: '$.changes[0].ref.id', expressionType: 'JSONPath', regexpFilter: '^(refs/heads/\\|refs/remotes/origin/)'],
            ],
     
            causeString: 'Triggered on $ref',
            token: 'mytokenPipeline',
            printContributedVariables: true,
            printPostContent: true,
            silentResponse: false
        )
    }

    stages {
        stage("git to ispw") {
            steps {
                gitToIspwIntegration app: 'PLAY', branchMapping: '''**/dev1 => DEV1, per-commit
                **/dev2 => DEV2, per-branch
                **/dev3 => DEV3, custom, description

                ''', connectionId: '94d914d9-ea8d-472c-90e4-4b5c007c64d4', credentialsId: '702482ac-de07-4e55-92b3-fcfecbd4fcd7', gitCredentialsId: '6d38ac8e-2d78-446d-9c84-f6072d896013', gitRepoUrl: 'http://10.211.55.3:7990/bitbucket/scm/proj/gitrepo2.git', runtimeConfig: 'TPZP', stream: 'PLAY'

            }
        }
    }
}
```
