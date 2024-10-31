pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh 'mvn -B -Dskiptests clean package'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn -Dtest=OpenapiGeneratortTest verify'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
    }
}
