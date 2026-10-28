pipeline {
    agent any

    environment {
        DOCKER_IMAGE  = 'ani3h/calculator'
        NOTIFY_EMAIL  = 'anishkanteti3@gmail.com'
        DOCKERHUB_CREDS = 'dockerhub-creds'
        IMAGE_TAG     = "${env.BUILD_NUMBER}"
        // Homebrew / Docker Desktop paths
        PATH = "/opt/homebrew/bin:/usr/local/bin:${env.PATH}"
    }

    triggers {
        // Fires on GitHub webhook pushes
        githubPush()
        // Fallback if the webhook can't reach Jenkins: poll every 2 minutes
        pollSCM('H/2 * * * *')
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker build -t $DOCKER_IMAGE:$IMAGE_TAG -t $DOCKER_IMAGE:latest .'
            }
        }

        stage('Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: env.DOCKERHUB_CREDS,
                                                  usernameVariable: 'DH_USER',
                                                  passwordVariable: 'DH_PASS')]) {
                    sh '''
                        echo "$DH_PASS" | docker login -u "$DH_USER" --password-stdin
                        docker push $DOCKER_IMAGE:$IMAGE_TAG
                        docker push $DOCKER_IMAGE:latest
                    '''
                }
            }
            post {
                always {
                    sh 'docker logout || true'
                }
            }
        }

        stage('Deploy') {
            steps {
                // Recreates only containers whose image changed, so re-runs are safe
                sh 'docker compose up -d --no-build --remove-orphans'
                sh '''
                    for i in $(seq 1 30); do
                        if curl -fs http://localhost:8081/actuator/health | grep -q UP; then
                            echo "Deployment healthy"
                            curl -s "http://localhost:8081/api/v1/add?a=2&b=3"; echo
                            exit 0
                        fi
                        sleep 2
                    done
                    echo "Health check failed"
                    docker compose ps
                    docker compose logs --tail=50
                    exit 1
                '''
            }
        }
    }

    post {
        success {
            mail to: "${env.NOTIFY_EMAIL}",
                 subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                 body: "Build succeeded and ${env.DOCKER_IMAGE}:${env.IMAGE_TAG} is deployed.\n\n${env.BUILD_URL}"
        }
        failure {
            mail to: "${env.NOTIFY_EMAIL}",
                 subject: "FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                 body: "Build failed. Console output:\n\n${env.BUILD_URL}console"
        }
    }
}
