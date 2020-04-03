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
					dir('./projet-isa-devops-20-team-b-20-schedule-component/') {
						echo "Compile module"
						sh "mvn -s $MAVEN_SETTINGS -N deploy"
					}
                }
            }
        }
		stage("Tests") {
			steps {
				dir("./projet-isa-devops-20-team-b-20-schedule-component/") {
					echo "Unit tests module"
					sh "mvn test"
				}
			}
		}
		stage("Deploy") {
			configFileProvider([configFile(fileId: MVN_SETTING_PROVIDER, variable: "MAVEN_SETTINGS")]) {
					dir("./projet-isa-devops-20-team-b-20-schedule-component/") {
						echo "Deployment into artifactory"
						sh "mvn -s $MAVEN_SETTINGS deploy"
					}
				}
		}
    }
    post{
        success {
            echo "======== pipeline executed successfully ========"
        }
        failure {
            echo "======== pipeline execution failed========"
        }
    }
}
