pipeline {
  agent any

  environment {
    CI                      = 'true'
    CI_IGNORE_TEST_FAILURES = 'true'
    GRADLE_USER_HOME        = "${WORKSPACE}/.gradle"
    AWS_DEFAULT_REGION      = 'ap-northeast-2'
    DEP_CACHE               = "s3://my-ci-cache/gradle-deps/${env.JOB_NAME}"
    // Jenkins credentials 참조
    AWS_CREDS               = credentials('aws-credentials')
    SONAR_TOKEN             = credentials('sonar-token')
    SONAR_HOST_URL          = 'http://sonarqube:9000'
  }

  stages {
    stage('Restore deps') {
      steps { sh '''
        export AWS_ACCESS_KEY_ID="${AWS_CREDS_USR}"
        export AWS_SECRET_ACCESS_KEY="${AWS_CREDS_PSW}"
        KEY=$(sha256sum build.gradle settings.gradle gradle/wrapper/gradle-wrapper.properties 2>/dev/null | sha256sum | cut -c1-16)
        mkdir -p "${GRADLE_USER_HOME}"
        if aws s3 cp "${DEP_CACHE}/${KEY}.tar.zst" - 2>/dev/null | zstd -d | tar x -C "${GRADLE_USER_HOME}"; then
          echo "deps cache HIT: ${KEY}"
        else
          echo "deps cache MISS: ${KEY}"
        fi
      ''' }
    }

    stage('Unit Test') {
      steps {
        sh 'chmod +x gradlew'
        sh '''
          export AWS_ACCESS_KEY_ID="${AWS_CREDS_USR}"
          export AWS_SECRET_ACCESS_KEY="${AWS_CREDS_PSW}"
          ./gradlew test --build-cache
        '''
      }
      post {
        always {
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
        sh '''
          export AWS_ACCESS_KEY_ID="${AWS_CREDS_USR}"
          export AWS_SECRET_ACCESS_KEY="${AWS_CREDS_PSW}"
          ./gradlew test -Ptags=Integration --build-cache
        '''
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
        sh '''
          export AWS_ACCESS_KEY_ID="${AWS_CREDS_USR}"
          export AWS_SECRET_ACCESS_KEY="${AWS_CREDS_PSW}"
          ./gradlew jacocoMergeReport
        '''
      }
    }

    stage('Sonar') {
      steps {
        sh '''
          export AWS_ACCESS_KEY_ID="${AWS_CREDS_USR}"
          export AWS_SECRET_ACCESS_KEY="${AWS_CREDS_PSW}"
          ./gradlew sonar --build-cache
        '''
      }
    }

    stage('Save deps') {
      steps { sh '''
        export AWS_ACCESS_KEY_ID="${AWS_CREDS_USR}"
        export AWS_SECRET_ACCESS_KEY="${AWS_CREDS_PSW}"
        KEY=$(sha256sum build.gradle settings.gradle gradle/wrapper/gradle-wrapper.properties 2>/dev/null | sha256sum | cut -c1-16)
        if ! aws s3 ls "${DEP_CACHE}/${KEY}.tar.zst" 2>/dev/null; then
          tar c -C "${GRADLE_USER_HOME}" caches/modules-2 | zstd | aws s3 cp - "${DEP_CACHE}/${KEY}.tar.zst"
          echo "deps cache SAVED: ${KEY}"
        else
          echo "deps cache already exists: ${KEY}"
        fi
      ''' }
    }
  }
}
