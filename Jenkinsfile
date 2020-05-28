@Library('jenkins-shared-libraries') _

def SERVER_ID = 'carlspring'
def SNAPSHOT_SERVER_URL = 'https://repo.carlspring.org/content/repositories/carlspring-oss-snapshots'
def RELEASE_SERVER_URL = 'https://repo.carlspring.org/content/repositories/carlspring-oss-releases'
def PR_SERVER_URL = 'https://repo.carlspring.org/content/repositories/carlspring-oss-pull-requests'

// Notification settings for "master" and "branch/pr"
def notifyMaster = [notifyAdmins: true, recipients: [culprits(), requestor()]]
def notifyBranch = [recipients: [brokenTestsSuspects(), requestor()]]
def isMasterBranch = 'master'.equals(env.BRANCH_NAME);

pipeline {
    agent {
        node {
            label 'alpine-jdk8-mvn-3.5'
        }
    }
    parameters {
        booleanParam(defaultValue: true, description: 'Trigger strongbox? (has no effect on branches/prs)', name: 'TRIGGER_STRONGBOX')
        booleanParam(defaultValue: true, description: 'Send email notification?', name: 'NOTIFY_EMAIL')
    }
    environment {
        // Use Pipeline Utility Steps plugin to read information from pom.xml into env variables
        GROUP_ID = readMavenPom().getGroupId()
        ARTIFACT_ID = readMavenPom().getArtifactId()
        VERSION = readMavenPom().getVersion()
    }
    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '50', daysToKeepStr: '', numToKeepStr: '1000')
        disableResume()
        durabilityHint 'PERFORMANCE_OPTIMIZED'
        disableConcurrentBuilds()
        skipStagesAfterUnstable()
    }
    triggers {
        cron(isMasterBranch ? '' : 'H * * * */6')
    }
    stages {
        stage('Node') {
            steps {
                container('maven') {
                    nodeInfo("mvn")
                }
            }
        }
        stage('Building') {
            steps {
                container('maven') {
                    withMavenPlus(timestamps: true, mavenLocalRepo: workspace().getM2LocalRepoPath(), mavenSettingsConfig: '67aaee2b-ca74-4ae1-8eb9-c8f16eb5e534')
                        {
                            sh "mvn -U clean install -Dprepare.revision"
                        }
                }
            }
        }
        stage('Deploy') {
            when {
                expression {
                    isMasterBranch || isDeployableTempVersion()
                }
            }
            steps {
                script {
                    container('maven') {
                        withMavenPlus(mavenLocalRepo: workspace().getM2LocalRepoPath(), mavenSettingsConfig: 'a5452263-40e5-4d71-a5aa-4fc94a0e6833') {
                            echo "Deploying " + GROUP_ID + ":" + ARTIFACT_ID + ":" + VERSION

                            if (BRANCH_NAME == 'master')
                            {
                                // We are temporarily reverting back.
                                sh "mvn deploy" +
                                   " -DskipTests" +
                                   " -DaltDeploymentRepository=${SERVER_ID}::default::${SNAPSHOT_SERVER_URL}"

                                // This will be used in the future.
                                // def APPROVE_RELEASE=false
                                // def APPROVED_BY=""

                                //try {
                                //    timeout(time: 115, unit: 'MINUTES')
                                //    {
                                //        rocketSend attachments: [[
                                //             authorIcon: 'https://jenkins.carlspring.org/static/fd850815/images/headshot.png',
                                //             authorName: 'Jenkins',
                                //             color: '#f4bc0d',
                                //             text: 'Job is pending release approval! If no action is taken within an hour, it will abort releasing.',
                                //             title: env.JOB_NAME + ' #' + env.BUILD_NUMBER,
                                //             titleLink: env.BUILD_URL
                                //        ]], message: '', rawMessage: true, channel: '#strongbox-devs'
                                //
                                //        APPROVE_RELEASE = input message: 'Do you want to release and deploy this version?',
                                //                                submitter: 'administrators,strongbox,strongbox-pro'
                                //    }
                                //}
                                //catch(err)
                                //{
                                //    APPROVE_RELEASE = false
                                //}

                                //if(APPROVE_RELEASE == true || APPROVE_RELEASE.equals(null))
                                //{
                                //    echo "Set upstream branch..."
                                //    sh "git branch --set-upstream-to=origin/master master"

                                //    echo "Preparing release and tag..."
                                //    sh "mvn -B release:clean release:prepare"

                                //    def releaseProperties = readProperties(file: "release.properties");
                                //    def RELEASE_VERSION = releaseProperties["scm.tag"]

                                //    echo "Deploying " + RELEASE_VERSION

                                //    sh "mvn -B release:perform -DserverId=${SERVER_ID} -DdeployUrl=${RELEASE_SERVER_URL}"
                                //}
                                //else
                                //{
                                //    echo "Deployment has been skipped, because it was not approved."
                                //}
                            }
                            else
                            {
                                sh "mvn deploy" +
                                   " -DskipTests" +
                                   " -DaltDeploymentRepository=${SERVER_ID}::default::${PR_SERVER_URL}"
                            }
                        }
                    }
                }
            }
        }
    }
    post {
        success {
            script {
                if (BRANCH_NAME == 'master' && params.TRIGGER_STRONGBOX)
                {
                    build job: '/strongbox/builds/strongbox/master',
                          wait: false,
                          parameters: [
                              string(name: 'INTEGRATION_TESTS_BRANCH', value: 'master'),
                              booleanParam(name: 'RUN_ONLY_SMOKE', value: false),
                              booleanParam(name: 'SKIP_TESTS', value: false),
                              booleanParam(name: 'DEPLOY', value: true),
                              booleanParam(name: 'NOTIFY_EMAIL', value: true)
                          ]
                }
            }
        }
        failure {
            script {
                if (params.NOTIFY_EMAIL)
                {
                    notifyFailed((BRANCH_NAME == "master") ? notifyMaster : notifyBranch)
                }
            }
        }
        unstable {
            script {
                if (params.NOTIFY_EMAIL)
                {
                    notifyUnstable((BRANCH_NAME == "master") ? notifyMaster : notifyBranch)
                }
            }
        }
        fixed {
            script {
                if (params.NOTIFY_EMAIL)
                {
                    notifyFixed((BRANCH_NAME == "master") ? notifyMaster : notifyBranch)
                }
            }
        }
        cleanup {
            script {
                container('maven') {
                    workspace().clean()
                }
            }
        }
    }
}
