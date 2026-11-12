pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
    }

    stages {
        stage('Checkout') {
            steps {
                deleteDir()
                checkout scm
            }
        }

        stage('Preparation') {
            steps {
                sh 'chmod +x gradlew'
                sh 'java -version'
                sh './gradlew --version'
            }
        }

        stage('Compilation') {
            steps {
                sh './gradlew --no-daemon clean classes'
            }
        }

        stage('Tests et couverture') {
            steps {
                sh './gradlew --no-daemon test jacocoTestReport'
            }
        }

        /*stage('Analyse SonarQube') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        set +x
                        export SONAR_TOKEN="$SONAR_AUTH_TOKEN"

                        ./gradlew --no-daemon sonar \
                          -Dsonar.projectKey=eventpro \
                          -Dsonar.projectName=EventPro \
                          -Dsonar.coverage.jacoco.xmlReportPaths=build/reports/jacoco/test/jacocoTestReport.xml
                    '''
                }
            }
        }


        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
*/
        stage('Packaging') {
            steps {
                sh './gradlew --no-daemon bootJar'
            }
        }

        stage('Archivage') {
            steps {
                archiveArtifacts(
                    artifacts: 'build/libs/*.jar',
                    excludes: 'build/libs/*-plain.jar',
                    fingerprint: true
                )
            }
        }
    }

    post {
        always {
            junit(
                testResults: 'build/test-results/test/*.xml',
                allowEmptyResults: true
            )

            archiveArtifacts(
                artifacts: 'build/reports/tests/test/**,build/reports/jacoco/test/**',
                allowEmptyArchive: true
            )
        }

        success {
            echo 'EventPro : pipeline termine avec succes.'
        }

        failure {
            echo 'EventPro : echec du pipeline. Consulter les logs.'
        }
    }
}