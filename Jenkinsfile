def update = ""
def gate = ""
pipeline{
    agent any
    options {
        disableConcurrentBuilds()
        timeout(time: 1, unit: "HOURS")
    }
    parameters {
        string(name: 'DEPENDENCY', defaultValue: '', description: 'dependency to update')
        string(name: 'VERSION', defaultValue: '', description: 'version of the dependency')
        choice(name: 'TYPE', choices: ['none', 'snapshot', 'release'], description: 'type of version to update')
    }
    environment {
        MVN_SETTING_PROVIDER = "3ec57b41-efe6-4628-a6c7-8be5f1c26d77"
        COMPONENT = "projet-isa-devops-20-team-b-20-schedule-component"
    }
    stages {
        stage("Snapshot revision") {
            environment {
                NEW_VERSION = """${sh(
                                    returnStdout: true,
                                    script: 'mvn help:evaluate -Dexpression=versions.${DEPENDENCY} -q -DforceStdout'
                                )}"""
            }
            when {
                allOf {
                    expression { params.DEPENDENCY != '' }
                    expression { params.TYPE == 'snapshot' }
                }
            }
            stages {
                stage("Check Update") {
                    when {
                        expression { params.VERSION != NEW_VERSION }
                    }
                    steps {
                        sh "mvn versions:use-latest-versions -DallowSnapshots=true -DprocessParent=false -Dincludes=fr.unice.polytech.isadevops.dronedelivery:${params.DEPENDENCY}"
                        script {
                            update = "\n - Component need fix before update : ${params.DEPENDENCY}[latest snapshot]"
                        }
                    }
                }
            }
        }
        stage("Compile") {
            steps {
                configFileProvider([configFile(fileId: MVN_SETTING_PROVIDER, variable: "MAVEN_SETTINGS")]) {
					echo "Compile module"
					sh "mvn -s $MAVEN_SETTINGS clean compile"
                }
            }
        }
		stage("Tests") {
			steps {
				echo "Unit tests module"
				sh "mvn test"
			}
		}
        stage('Mutations') {
            steps {
                echo 'PiTest Mutation'
                sh 'mvn org.pitest:pitest-maven:mutationCoverage'
            }
        }
		stage("Deploy") {
			steps {
				configFileProvider([configFile(fileId: MVN_SETTING_PROVIDER, variable: "MAVEN_SETTINGS")]) {
						echo "Deployment into artifactory"
						sh "mvn -s $MAVEN_SETTINGS deploy"
				}
			}
		}
        stage('Sonarqube') {
            steps {
                withSonarQubeEnv('Sonarqube_env') {
                    echo 'Sonar Analysis'
                    sh 'mvn package sonar:sonar -Dsonar.pitest.mode=reuseReport'
                }
            }
        }
        stage('Quality Gate') {
            steps {
                catchError(buildResult: "SUCCESS", stageResult: "FAILURE") {
                    timeout(time: 1, unit: "HOURS") {
                        waitForQualityGate true
                    }
                }
            }
            post{
                success {
                    script {
                        gate = "\n - Quality gate was successful"
                    }
                }
                failure {
                    script {
                        gate = "\n - Quality gate was failed"
                    }
                }
            }
        }
        stage('Update snapshot dependencies') {
            environment {
                CURRENT_VERSION = """${sh(
                                    returnStdout: true,
                                    script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout'
                                )}"""
            }
            when {
                allOf {
                    not { branch 'master' }
                    expression { params.DEPENDENCY == '' }
                }
            }
            steps {
                script {
                    def components = ['projet-isa-devops-20-team-b-20-shipment-component']
                    for (int i = 0; i < components.size(); ++i) {
                        echo "Check dependency on ${components[i]}"
                        build job: "${components[i]}/develop",
                            parameters: [string(name: 'DEPENDENCY', value: "${COMPONENT}"),
                            string(name: 'VERSION', value: "${CURRENT_VERSION}"),
                            string(name: 'TYPE', value: 'snapshot')],
                            propagate: false,
                            wait: false
                    }
                }
            }
        }
        stage('Commit with new version') {
            when {
                allOf {
                    expression { params.DEPENDENCY != '' }
                    expression { params.VERSION == 'snapshot' }
                }
            }
            steps {
                script {
                    update = "\n - Component can be update : ${params.DEPENDENCY}[latest snapshot]"
                }
            }
        }
    }
    post{
        always {
            archiveArtifacts artifacts: 'target/**/*', fingerprint: true
            junit 'target/surefire-reports/*.xml'
            echo '======== pipeline archived ========'
        }
        success {
            slackSend(
            channel: 'projet-isa-devops-ci',
            notifyCommitters: true,
            failOnError: true,
            color: 'good',
            token: env.SLACK_TOKEN,
            message: 'Job: ' + env.JOB_NAME + ' with buildnumber ' + env.BUILD_NUMBER + ' was successful' + update + gate,
            baseUrl: env.SLACK_WEBHOOK)
            echo "======== pipeline executed successfully ========"
            sh 'mvn versions:commit'
        }
        failure {
            slackSend(
            channel: 'projet-isa-devops-ci',
            notifyCommitters: true,
            failOnError: true,
            color: 'danger',
            token: env.SLACK_TOKEN,
            message: 'Job: ' + env.JOB_NAME + ' with buildnumber ' + env.BUILD_NUMBER + ' was failed',
            baseUrl: env.SLACK_WEBHOOK)
            echo "======== pipeline execution failed========"
            sh 'mvn versions:revert'
        }
    }
}
