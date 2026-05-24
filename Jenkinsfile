pipeline {
  agent {
    kubernetes {
      yaml '''
        spec:
          serviceAccountName: jenkins-ci
          containers:
          - name: gradle
            image: gradle:8.12-jdk21
            command: ['sleep']
            args: ['infinity']
      '''
    }
  }

  environment {
    CI = 'true'
    GRADLE_USER_HOME = "${WORKSPACE}/.gradle"
    DEP_CACHE = "s3://my-ci-cache/gradle-deps/${env.JOB_NAME}"
  }

  stages {
    stage('Restore deps') {
      steps { container('gradle') { sh '''
        KEY=$(sha256sum build.gradle settings.gradle gradle/wrapper/gradle-wrapper.properties 2>/dev/null | sha256sum | cut -c1-16)
        if aws s3 cp "${DEP_CACHE}/${KEY}.tar.zst" - 2>/dev/null | zstd -d | tar x -C "${GRADLE_USER_HOME}"; then
          echo "deps cache hit: ${KEY}"
        else
          echo "deps cache miss: ${KEY}"
        fi
      ''' } }
    }

    stage('Unit Test') {
      steps { container('gradle') {
        sh './gradlew test --build-cache'
        // exec 파일 보존 (Integration Test가 덮어쓰지 않도록)
        sh 'find . -path "*/build/jacoco/test.exec" -exec cp {} {}.unit \\;'
        sh 'find . -name "test.exec.unit" | while read f; do cp "$f" "$(dirname "$f")/unit-test.exec"; rm "$f"; done'
      } }
    }

    stage('Integration Test') {
      steps { container('gradle') {
        sh './gradlew test -Ptags=Integration --build-cache'
      } }
    }

    stage('Coverage Report') {
      steps { container('gradle') {
        sh './gradlew jacocoMergeReport'
      } }
    }

    stage('Sonar') {
      steps { container('gradle') {
        sh './gradlew sonarqube --build-cache'
      } }
    }

    stage('Save deps') {
      steps { container('gradle') { sh '''
        KEY=$(sha256sum build.gradle settings.gradle gradle/wrapper/gradle-wrapper.properties 2>/dev/null | sha256sum | cut -c1-16)
        if ! aws s3 ls "${DEP_CACHE}/${KEY}.tar.zst" 2>/dev/null; then
          tar c -C "${GRADLE_USER_HOME}" caches/modules-2 | zstd | aws s3 cp - "${DEP_CACHE}/${KEY}.tar.zst"
        fi
      ''' } }
    }
  }
}
