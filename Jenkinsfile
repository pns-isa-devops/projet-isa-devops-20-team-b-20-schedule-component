pipeline{
    agent any
    options {
        disableConcurrentBuilds()
        timeout(time: 1, unit: "HOURS")
    }
    environment {
        MVN_SETTING_PROVIDER = "3ec57b41-efe6-4628-a6c7-8be5f1c26d77"
    }
    stages {
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
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate true
                }
                echo 'passed'
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
            message: 'Job: ' + env.JOB_NAME + ' with buildnumber ' + env.BUILD_NUMBER + ' was successful',
            baseUrl: env.SLACK_WEBHOOK)
            echo "======== pipeline executed successfully ========"
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
        }
    }
}
