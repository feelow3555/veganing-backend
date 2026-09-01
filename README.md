🌱 Veganing Backend

<div align="center">

비건 라이프스타일 챌린지 플랫폼 — Spring Boot Backend

Vision AI 식단 분석 · pgvector 기반 RAG 추천 · 동시성 제어 · AWS 배포

Frontend: React 19 + Vite
Backend: Spring Boot 기반 전체 시스템 단독 설계 및 구현

</div>

⸻

💡 프로젝트 소개

Veganing은 비건 식단을 시작하고 지속하는 과정을 돕는 라이프스타일 챌린지 플랫폼입니다.

사용자가 식단 사진을 업로드하면 Claude Sonnet 4 Vision이 식재료를 추출하고, DB에 저장된 영양소와 탄소 데이터를 기반으로 식단을 분석합니다.

커뮤니티에서 선정된 레시피는 Voyage AI + pgvector를 통해 임베딩되어 RAG 추천 데이터로 다시 활용되며, 추천 식재료는 쇼핑 기능까지 연결됩니다.

프로젝트 한눈에 보기

항목	내용
개발 기간	2026.07 ~ 2026.08
담당	Backend Solo — 설계부터 배포까지 전체 담당
Backend	Java 21 · Spring Boot 4.1
Database	PostgreSQL 16 · pgvector · Redis
AI	Claude Sonnet 4 Vision · Voyage AI
Infra	AWS EC2 · RDS · S3
DevOps	GitHub Actions · Flyway · systemd
규모	15개 테이블 · 8개 주요 도메인

주요 구현

* 기존 Node.js 프록시 서버를 Spring Boot 기반 풀 백엔드로 재설계
* Vision AI → 정량 데이터 분석 → RAG 추천으로 이어지는 AI 파이프라인 구축
* 좋아요와 상품 재고에 낙관적 락 / 비관적 락을 구분 적용
* @Async 기반 식단 분석 비동기 처리
* JWT + Refresh Token Rotation 기반 인증 구현
* EC2 · RDS · S3 배포 및 GitHub Actions CI/CD 구축
* Flyway 기반 운영 DB 마이그레이션 관리

⸻

🔄 서비스 흐름

비건 챌린지 시작
        ↓
식단 사진 업로드
        ↓
Vision AI 식재료 분석
        ↓
영양소 / 탄소 절감량 계산
        ↓
RAG 기반 개인 맞춤 식단 추천
        ↓
추천 식재료 기반 쇼핑 상품 연결
커뮤니티 레시피
        ↓
좋아요 상위 레시피 선정
        ↓
Voyage AI Embedding
        ↓
pgvector 저장
        ↓
RAG 추천 데이터로 재활용

커뮤니티에서 생성된 레시피가 다시 AI 추천 데이터가 되는 선순환 구조를 설계했습니다.

⸻

✨ 핵심 기능

기능	설명
AI 식단 분석	Claude Vision으로 식재료를 추출하고 DB 기반 영양소·탄소 데이터 분석
RAG 식단 추천	pgvector 코사인 유사도 검색 + Claude 기반 개인화 추천
탄소 대시보드	비건 식단의 일별·누적 탄소 절감량 집계
비건 챌린지	30일 챌린지 및 포인트·레벨 시스템
커뮤니티	레시피 공유, 좋아요, 댓글, 지역별 랭킹
비건 쇼핑	상품 추천, 장바구니, 재고 동시성 제어 기반 주문

⸻

🏗 시스템 아키텍처

Vision AI 식단 분석

Client
   ↓
S3 Presigned URL 직접 업로드
   ↓
POST /api/meal
   ↓
202 Accepted + mealId 반환
   ↓
@Async Background Processing
   ↓
Claude Sonnet 4 Vision
   ↓
식재료 목록 추출
   ↓
Ingredient DB
   ↓
영양소 + CO2 정량 데이터 조회
   ↓
Claude Feedback
   ↓
Meal 저장 / Carbon 집계

AI API의 응답이 5~15초까지 소요될 수 있어 요청을 동기적으로 대기시키지 않고 202 Accepted + Polling 방식으로 설계했습니다.

이미지는 백엔드를 거치지 않고 Presigned URL을 통해 Client → S3로 직접 업로드하여 서버의 메모리와 네트워크 사용을 줄였습니다.

⸻

RAG Pipeline

[Recipe Indexing]
좋아요 상위 레시피
       ↓
Recipe Text
       ↓
Voyage AI voyage-3-lite
       ↓
512차원 Embedding
       ↓
PostgreSQL + pgvector
[Recommendation]
사용자 식단 분석
       ↓
부족 영양소 + 비건 피드백
       ↓
Voyage AI Query Embedding
       ↓
pgvector Cosine Similarity
       ↓
관련 레시피 Top N
       ↓
Claude Prompt Context
       ↓
개인화 식단 추천

AWS Bedrock Knowledge Base 같은 관리형 서비스를 사용하지 않고 임베딩 생성 → 저장 → 유사도 검색 → LLM Context 구성 과정을 직접 구현했습니다.

⸻

🧠 핵심 기술적 의사결정

1. Vision AI — 3개 모델 직접 비교

문제

식단 이미지에서 다양한 식재료를 안정적으로 추출할 Vision 모델이 필요했습니다.

비교

* Claude Sonnet 4
* GPT-4o
* Gemini

동일한 식단 이미지를 입력하고 식재료 추출 개수, 정확도, JSON 응답 안정성을 비교했습니다.

결정

Claude Sonnet 4

테스트 결과 식재료 추출 정확도와 응답 안정성이 가장 우수해 최종 선택했습니다.

⸻

2. RAG — Bedrock Knowledge Base 대신 pgvector

후보

AWS Bedrock Knowledge Base

* 관리가 편리함
* 임베딩과 검색 과정이 추상화됨

PostgreSQL + pgvector

* 직접 구현 필요
* 기존 PostgreSQL 인프라 활용 가능
* 임베딩과 검색 과정을 코드 레벨에서 제어 가능

결정

pgvector + Voyage AI

별도의 Vector DB를 추가하지 않고 기존 PostgreSQL을 활용하면서 RAG 전체 흐름을 직접 제어하기 위해 선택했습니다.

⸻

3. AI에게 정량 데이터를 맡기지 않은 이유

Vision AI가 식재료를 찾더라도 영양소와 탄소 수치를 LLM이 직접 생성하도록 하지 않았습니다.

Vision AI
   ↓
식재료 추출
   ↓
Ingredient DB
   ↓
CO2 / 영양소 데이터 조회
   ↓
Claude Prompt에 실제 수치 제공
   ↓
분석 및 피드백

LLM은 식재료 인식과 피드백에 집중시키고, 정확성이 필요한 수치는 DB에서 관리하여 Hallucination으로 인한 수치 오류 가능성을 줄였습니다.

⸻

4. 좋아요와 주문에 서로 다른 Lock 적용

좋아요

충돌 가능성보다 읽기/쓰기 성능이 중요하다고 판단하여 Optimistic Lock을 적용했습니다.

@Version
private Long version;

주문

재고 초과 판매는 데이터 정합성 문제로 이어지기 때문에 Pessimistic Lock을 적용했습니다.

@Lock(LockModeType.PESSIMISTIC_WRITE)
Product findProductById(Long id);

동일한 동시성 문제라도 데이터 특성과 충돌 비용에 따라 다른 전략을 적용했습니다.

⸻

⚡ 동시성 / 부하 테스트

K6 테스트 완료 후 실측 결과 추가 예정

Optimistic Lock

* 동일 게시물 동시 좋아요 요청
* Lock 충돌 횟수
* Retry 결과
* 최종 Like Count 정합성

Pessimistic Lock

* 제한된 재고에 대한 동시 주문
* 주문 성공 / 실패 요청 수
* 최종 재고
* Overselling 발생 여부

⸻

🔧 Troubleshooting

1. @Async가 동작하지 않았던 문제

문제

동일 클래스 내부에서 비동기 메서드를 호출하자 @Async가 적용되지 않고 동기적으로 실행되었습니다.

원인

Spring AOP가 Proxy 기반으로 동작하기 때문에 Self Invocation은 Proxy를 우회한다는 것을 확인했습니다.

해결

MealService
     ↓
MealAsyncService
     ↓
@Async Method

비동기 로직을 별도의 Spring Bean으로 분리해 Proxy를 경유하도록 변경했습니다.

이후 @Transactional에서도 같은 원리가 적용된다는 것을 확인하고 트랜잭션 경계가 필요한 로직 역시 별도의 Service로 분리했습니다.

배운 점

@Async, @Transactional, @Cacheable과 같은 AOP 기반 기능을 사용할 때 호출이 Proxy를 경유하는지 먼저 확인하는 기준을 갖게 되었습니다.

⸻

2. JPA와 pgvector 타입 매핑 문제

문제

Hibernate가 pgvector embedding 컬럼을 일반적인 JPA 타입처럼 처리하지 못하면서 타입 변환 문제가 발생했습니다.

시도

AttributeConverter를 이용한 매핑을 시도했지만 Vector Similarity Query에서 문제가 지속되었습니다.

해결

Embedding 컬럼을 일반 JPA Entity 매핑에서 제외하고 Native Query를 통해 벡터 저장 및 검색을 처리했습니다.

배운 점

모든 데이터 접근을 JPA로 통일하기보다 ORM의 추상화가 적합하지 않은 영역에서는 SQL을 직접 사용하는 것이 더 명확할 수 있다는 점을 경험했습니다.

⸻

3. Spring Boot 4 Flyway 자동 설정 문제

문제

flyway-core 의존성을 추가했지만 Flyway Migration이 실행되지 않았고 별도의 명확한 오류도 발생하지 않았습니다.

해결

Spring Boot 4 환경에 맞게 spring-boot-starter-flyway로 변경하여 자동 설정을 정상화했습니다.

배운 점

라이브러리가 Classpath에 존재하는 것과 Spring Boot Auto Configuration이 활성화되는 것은 별개의 문제임을 확인했습니다.

⸻

🗃 ERD

총 15개 테이블을 AUTH, CHALLENGE, MEAL, CARBON, COMMUNITY, RECIPE, PRODUCT, CART, ORDER 도메인으로 분리했습니다.

주요 설계:

* community_posts.version → Optimistic Lock
* order_items.price_at_order → 주문 당시 가격 Snapshot
* carbon_daily → 조회 성능을 위한 일별 집계
* recipes.embedding → pgvector 기반 RAG 검색
* post_likes → 사용자별 중복 좋아요 방지

⸻

📁 Package Structure

com.veganing
├── domain
│   ├── auth
│   ├── challenge
│   ├── meal
│   ├── carbon
│   ├── ingredient
│   ├── community
│   ├── recipe
│   ├── product
│   ├── cart
│   └── order
│
└── global
    ├── auth
    ├── config
    ├── error
    ├── common
    ├── scheduler
    └── infra
        ├── vision
        ├── voyage
        └── s3

기능 중심의 계층형 구조가 아니라 도메인 중심 패키지 구조를 사용해 관련 Controller, Service, Repository가 같은 도메인 안에서 관리되도록 구성했습니다.

⸻

📋 API

<details>
<summary><b>전체 API 목록 보기</b></summary>

AUTH

회원가입 · 로그인 · 로그아웃 · Token Refresh · Profile

CHALLENGE

챌린지 시작 · 조회 · 히스토리 · 통계 · 포인트

MEAL

Presigned URL · 식단 분석 · 분석 상태 · 기록 · RAG 추천

CARBON

누적 통계 · 오늘 절감량 · 기간별 통계

COMMUNITY

게시물 · 댓글 · 좋아요 · 지역별 랭킹

RECIPE

오늘의 레시피

SHOPPING

상품 · 장바구니 · 주문

상세 Endpoint는 Swagger API Documentation에서 확인할 수 있습니다.

</details>

⸻

🛠 Tech Stack

Backend

Java 21 · Spring Boot 4.1 · Spring Security · JPA/Hibernate · Flyway

Database

PostgreSQL 16 · pgvector · Redis

AI

Claude Sonnet 4 Vision · Voyage AI voyage-3-lite

Infrastructure

AWS EC2 · RDS · S3 · Cloudflare Tunnel

DevOps

Docker · GitHub Actions · systemd

Frontend

React 19 · Vite · Vercel

⸻

📊 Project Result

항목	결과
개발	Backend Solo
기간	약 3~4주
Database	15 Tables
주요 도메인	8+
Test	단위 테스트 48개
AI	Vision + Embedding + RAG
Deployment	EC2 + RDS + S3
CI/CD	GitHub Actions
DB Migration	Flyway
Troubleshooting	81건 문서화

프로젝트를 통해 얻은 것

단순히 API 기능을 구현하는 것을 넘어 기술을 선택하는 기준, Spring Framework의 동작 원리, 데이터 정합성을 고려한 동시성 제어, AI Pipeline 설계, 배포 이후 운영 환경까지 백엔드 개발의 전체 흐름을 직접 경험했습니다.

⸻

<div align="center">

Veganing Backend

Spring Boot · PostgreSQL · Redis · AWS · Claude · RAG

Made with 🌱 by feelow3555

</div>
