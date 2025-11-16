# CI 적용 가이드

이 문서는 GitHub Actions로 "푸시/PR 마다 자동 빌드·테스트"를 실행하는 방법을 쉽게 설명합니다. 
이 레포는 Testcontainers를 사용하므로, CI 환경에서도 실제 MySQL 컨테이너가 자동으로 뜹니다.

## 1) 준비물 확인
- 이 레포에는 다음이 이미 준비되어 있습니다.
  - Gradle 래퍼: `./gradlew`
  - 테스트 프로필: `src/test/resources/application-test.properties`
  - Testcontainers 설정(통합 테스트에서 `@Import(TestContainersConfiguration.class)` 사용)
- GitHub Actions 워크플로우가 추가되었습니다.
  - 파일: `.github/workflows/ci.yml`

## 2) GitHub Actions 켜기
- GitHub 저장소로 이동 → 상단의 `Actions` 탭 클릭
- 워크플로우 목록에서 `CI`가 보이면 활성화 완료
- 만약 꺼져 있으면 "Enable GitHub Actions" 버튼으로 활성화

## 3) 워크플로우가 하는 일(요약)
- 트리거: `main`, `develop` 브랜치에 대한 `push`와 `pull_request`
- 실행 환경: `ubuntu-latest` (Docker 사용 가능)
- 단계
  1) 체크아웃: `actions/checkout@v4`
  2) JDK 17 설치 + Gradle 캐시: `actions/setup-java@v4`
  3) Gradlew 실행 권한 부여: `chmod +x gradlew`
  4) 빌드·테스트: `./gradlew clean test --no-daemon` (프로필 `test`)
  5) 테스트 리포트 업로드(항상): XML/HTML 리포트 아티팩트 업로드

## 4) 직접 확인 방법
- PR을 올리거나, `main`/`develop`에 커밋을 푸시합니다.
- `Actions` 탭에서 `CI` 워크플로우가 실행되는지 확인
- 실행 완료 후 `Artifacts`에서 `test-reports` 다운로드 → HTML 리포트 확인(`build/reports/tests/test/index.html`)

## 5) 실패했을 때 확인 방법
- 실패한 단계 옆의 로그를 펼쳐 에러 메시지 확인
- 테스트 실패라면 업로드된 `test-reports` 아티팩트에서 XML/HTML 리포트를 열어 어느 테스트가 실패했는지 확인
- 수정 후 다시 푸시하면 자동으로 재실행됩니다

## 6) 자주 하는 질문(FAQ)
- Q. "Actions 러너에서 Testcontainers가 진짜로 MySQL을 띄울 수 있나요?"
  - A. 예. `ubuntu-latest` 호스트 러너에는 Docker가 기본 제공되어 컨테이너가 자동으로 기동·종료됩니다.
- Q. "로컬과 결과가 다를 수 있나요?"
  - A. 의존 버전/OS가 달라 소소한 차이는 있을 수 있습니다. 그래도 Testcontainers로 환경이 표준화되어 격차가 아주 작습니다.
- Q. "속도가 느립니다."
  - A. Gradle 캐시가 켜져 있고, Testcontainers 재사용 옵션을 추가로 구성할 수 있습니다(필수는 아님). 워크플로우를 복수 Job으로 나눠 병렬 실행할 수도 있습니다.

## 7) 선택적 고급 설정(원할 때만)
- JaCoCo 커버리지 보고서
  - `build.gradle.kts`에 `jacoco` 플러그인 추가
  - CI 단계에 `./gradlew jacocoTestReport` 추가 후 리포트 아티팩트 업로드
- 정적 분석/포매터
  - Spotless/Checkstyle/PMD 등을 Gradle에 추가하고 CI에서 실행
- 캐시/동시성
  - Gradle 캐시 외에 `actions/cache`로 `.gradle` 또는 Testcontainers 이미지 캐시 전략을 고도화할 수 있습니다

## 8) Jenkins를 쓰고 싶다면?
- Jenkinsfile 예시를 레포지토리 루트에 추가하고, Jenkins에서 해당 레포를 멀티브랜치 파이프라인으로 구성하세요.
- 최소 단계: Checkout → `chmod +x gradlew` → `./gradlew clean test -Dspring.profiles.active=test` → `junit`/`archiveArtifacts`로 리포트 수집
- 에이전트 노드에 Docker 사용 권한/JDK 17이 필요합니다

## 9) 로컬에서 "CI처럼" 돌려보기
- 터미널에서 `./gradlew clean test -Dspring.profiles.active=test` 실행
- `build/reports/tests/test/index.html` 열어 결과 확인

## 10) 요약
- 이미 `.github/workflows/ci.yml`이 있어 푸시/PR 마다 자동 빌드·테스트가 실행됩니다.
- 추가 코드는 필요 없습니다(앱 코드 수정 X). CI는 워크플로우 파일만으로 동작합니다.
- Testcontainers 덕분에 CI에서도 운영과 비슷한 DB 환경으로 신뢰성 있는 통합 테스트가 가능합니다.
