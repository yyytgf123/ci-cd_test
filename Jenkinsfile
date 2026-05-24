pipeline {
  agent any

  environment {
    CI                    = 'true'
    GRADLE_USER_HOME      = "${WORKSPACE}/.gradle"
    // MinIO (S3-compatible) 설정
    S3_ENDPOINT           = 'http://172.17.0.3:9000'
    AWS_ACCESS_KEY_ID     = 'minioadmin'
    AWS_SECRET_ACCESS_KEY = 'minioadmin'
    AWS_DEFAULT_REGION    = 'us-east-1'
    DEP_CACHE             = "s3://my-ci-cache/gradle-deps/${env.JOB_NAME}"
  }

  stages {
    stage('Restore deps') {
      steps { sh '''
        KEY=$(sha256sum build.gradle settings.gradle gradle/wrapper/gradle-wrapper.properties 2>/dev/null | sha256sum | cut -c1-16)
        mkdir -p "${GRADLE_USER_HOME}"
        if aws --endpoint-url "${S3_ENDPOINT}" s3 cp "${DEP_CACHE}/${KEY}.tar.zst" - 2>/dev/null | zstd -d | tar x -C "${GRADLE_USER_HOME}"; then
          echo "deps cache HIT: ${KEY}"
        else
          echo "deps cache MISS: ${KEY}"
        fi
      ''' }
    }

    stage('Unit Test') {
      steps {
        sh 'chmod +x gradlew'
        sh './gradlew test --build-cache'
      }
      post {
        always {
          // exec 파일 보존 (Integration Test가 같은 test.exec를 덮어쓰지 않도록)
          sh '''
            find . -path "*/build/jacoco/test.exec" | while read f; do
              cp "$f" "$(dirname "$f")/unit-test.exec"
            done
          '''
          stash name: 'unit-exec', includes: '**/build/jacoco/unit-test.exec', allowEmpty: true
        }
      }
    }

    stage('Integration Test') {
      steps {
        sh './gradlew test -Ptags=Integration --build-cache'
      }
      post {
        always {
          stash name: 'it-exec', includes: '**/build/jacoco/test.exec', allowEmpty: true
        }
      }
    }

    stage('Coverage Report') {
      steps {
        unstash 'unit-exec'
        unstash 'it-exec'
        sh './gradlew jacocoMergeReport'
      }
    }

    stage('Sonar') {
      steps {
        sh './gradlew sonarqube --build-cache'
      }
    }

    stage('Save deps') {
      steps { sh '''
        KEY=$(sha256sum build.gradle settings.gradle gradle/wrapper/gradle-wrapper.properties 2>/dev/null | sha256sum | cut -c1-16)
        if ! aws --endpoint-url "${S3_ENDPOINT}" s3 ls "${DEP_CACHE}/${KEY}.tar.zst" 2>/dev/null; then
          tar c -C "${GRADLE_USER_HOME}" caches/modules-2 | zstd | aws --endpoint-url "${S3_ENDPOINT}" s3 cp - "${DEP_CACHE}/${KEY}.tar.zst"
          echo "deps cache SAVED: ${KEY}"
        else
          echo "deps cache already exists: ${KEY}"
        fi
      ''' }
    }
  }
}
