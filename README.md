# 🌱 Veganing Backend

<div align="center">

![배너](docs/images/banner.png)

**비건 라이프스타일 챌린지 플랫폼 - Spring Boot 백엔드**

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white)](https://redis.io/)
[![AWS](https://img.shields.io/badge/AWS-EC2%20%7C%20RDS%20%7C%20S3-FF9900?style=flat-square&logo=amazonaws&logoColor=white)](https://aws.amazon.com/)
[![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white)](https://github.com/features/actions)
[![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=flat-square&logo=swagger&logoColor=black)](http://13.55.162.161:8080/swagger-ui/index.html)

> 프론트엔드 레포: [Veganing Frontend (React 19)](https://github.com/feelow3555/Veganing-frontend) · 🌐 [라이브 데모](https://veganing-frontend.vercel.app)

</div>

---

## 📑 목차
- [프로젝트 소개](#-프로젝트-소개)
- [핵심 기능](#-핵심-기능)
- [기술 스택](#-기술-스택)
- [시스템 아키텍처](#-시스템-아키텍처)
- [ERD](#-erd)
- [API 목록](#-api-목록)
- [기술적 의사결정](#-기술적-의사결정)
- [트러블슈팅 하이라이트](#-트러블슈팅-하이라이트)
- [개발 진행상황](#-개발-진행상황)

---

## 💡 프로젝트 소개

**Veganing**은 비건 라이프스타일을 시작하고 유지할 수 있도록 돕는 챌린지 플랫폼입니다.

매일 식단 사진을 업로드하면 **Claude Sonnet 4 Vision AI**가 식재료를 분석하고, 부족한 영양소와 탄소 절감량을 계산합니다. 커뮤니티에 레시피를 공유하고, **RAG 기반 AI**가 개인 맞춤 식단을 추천하며, 비건 식재료 쇼핑까지 한 번에 해결할 수 있습니다.

### 서비스 흐름

```
비건 챌린지 시작
      ↓
식단 사진 업로드 → Vision AI 분석 (식재료 추출, 영양소, 탄소 계산)
      ↓
RAG 기반 맞춤 식단 추천 → 추천 식재료 기반 쇼핑 상품 연결
      ↓
커뮤니티에 레시피 공유 → 좋아요 상위 레시피 → 오늘의 레시피 선정
      ↓
선정된 레시피 → Voyage AI 임베딩 → pgvector 저장 → RAG 컨텍스트로 재활용
```

> 커뮤니티 레시피가 RAG 추천의 데이터 소스가 되는 선순환 구조

### 개발 배경
- 비건 입문자가 영양 불균형 없이 식단을 유지하기 어려운 문제
- 탄소 절감 효과를 수치로 확인하며 동기부여를 높이고자 함
- 커뮤니티 레시피 → AI 추천 → 쇼핑 연결로 비건 생활의 진입 장벽을 낮춤

### 진행 기간
**2026.07 ~ 2026.08** (Solo, 백엔드 전담)

- 프론트엔드: 팀 프로젝트 기반 (React 19 + Vite) — 기획, 챌린지, 쇼핑몰 파트 담당
- 백엔드: Spring Boot 풀스택 Solo 구축 (본 레포, 포트폴리오 주요 결과물)

---

## ✨ 핵심 기능

| 기능 | 스크린샷 |
|------|----------|
| **AI 식단 분석** — Claude Sonnet 4 Vision으로 식재료 추출, DB 기반 영양소/탄소 계산 | ![식단분석](docs/images/screenshot_meal.png) |
| **RAG 식단 추천** — pgvector 코사인 유사도 검색 + Claude로 개인 맞춤 추천 | ![추천](docs/images/screenshot_recommend.png) |
| **탄소 대시보드** — 일별 탄소 절감량 시각화, 비건 식단만 집계 | ![탄소](docs/images/screenshot_carbon.png) |
| **비건 챌린지** — 30일 챌린지, 포인트/레벨 시스템 | ![챌린지](docs/images/screenshot_challenge.png) |
| **커뮤니티** — 레시피 공유, 좋아요(낙관적 락), 지역별 랭킹 | ![커뮤니티](docs/images/screenshot_community.png) |
| **비건 쇼핑** — 분석 식재료 기반 상품 추천, 장바구니/주문(비관적 락) | ![쇼핑](docs/images/screenshot_shopping.png) |

---

## 🛠 기술 스택

### Backend
<img src="https://img.shields.io/badge/Java%2021-ED8B00?style=flat-square&logo=openjdk&logoColor=white"/> <img src="https://img.shields.io/badge/Spring%20Boot%204.1-6DB33F?style=flat-square&logo=springboot&logoColor=white"/> <img src="https://img.shields.io/badge/Spring%20Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white"/> <img src="https://img.shields.io/badge/JPA%2FHibernate-59666C?style=flat-square&logo=hibernate&logoColor=white"/> <img src="https://img.shields.io/badge/Flyway-CC0200?style=flat-square&logo=flyway&logoColor=white"/> <img src="https://img.shields.io/badge/Swagger-85EA2D?style=flat-square&logo=swagger&logoColor=black"/>

### Database & Cache
<img src="https://img.shields.io/badge/PostgreSQL%2016-4169E1?style=flat-square&logo=postgresql&logoColor=white"/> <img src="https://img.shields.io/badge/pgvector-4169E1?style=flat-square&logo=postgresql&logoColor=white"/> <img src="https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white"/>

### AI & Cloud
<img src="https://img.shields.io/badge/Claude%20Sonnet%204-CC785C?style=flat-square&logo=anthropic&logoColor=white"/> <img src="https://img.shields.io/badge/Voyage%20AI-412991?style=flat-square&logoColor=white"/> <img src="https://img.shields.io/badge/AWS%20EC2-FF9900?style=flat-square&logo=amazonec2&logoColor=white"/> <img src="https://img.shields.io/badge/AWS%20RDS-527FFF?style=flat-square&logo=amazonrds&logoColor=white"/> <img src="https://img.shields.io/badge/AWS%20S3-569A31?style=flat-square&logo=amazons3&logoColor=white"/>

### DevOps
<img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white"/> <img src="https://img.shields.io/badge/GitHub%20Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white"/> <img src="https://img.shields.io/badge/Cloudflare%20Tunnel-F38020?style=flat-square&logo=cloudflare&logoColor=white"/> <img src="https://img.shields.io/badge/Vercel-000000?style=flat-square&logo=vercel&logoColor=white"/>

### 기술 선택 이유

| 기술 | 선택 이유 |
|------|----------|
| **Spring Boot 4.1.0 | 실무 표준, 풍부한 생태계, 포트폴리오 경쟁력 |
| **JPA / Hibernate** | 객체지향적 DB 접근, 복잡한 연관관계 관리 |
| **Spring Security + JWT** | Stateless 인증, Refresh Token으로 보안 강화 |
| **Redis** | Refresh Token TTL 자동 만료 + 오늘의 레시피 캐싱으로 DB 부하 감소 |
| **pgvector** | PostgreSQL 확장으로 별도 벡터 DB 없이 임베딩~검색 전과정 코드 레벨 제어 |
| **Claude Sonnet 4** | GPT-4o, Gemini 3사 벤치마크 결과 식재료 추출 정확도 최다 |
| **Voyage AI voyage-3-lite** | OpenAI, Bedrock Titan 대비 벤치마크 성능 우수, 가격 동등 |
| **Flyway** | 운영 환경 DDL 버전 관리, `ddl-auto: none`으로 안전한 스키마 관리 |

---

## 🏗 시스템 아키텍처

![아키텍처](docs/images/architecture.png)

### RAG 흐름

```
[스케줄러 - 매일 자정]
좋아요 상위 레시피 조회 (DB)
        ↓
레시피 텍스트 → S3 업로드
        ↓
Voyage AI voyage-3-lite → 512차원 벡터 임베딩 생성
        ↓
recipes.embedding (pgvector) 저장
        ↓
Redis 캐시 갱신 (오늘의 레시피 목록, TTL 24시간)

[AI 식단 추천 요청 시]
사용자 식단 분석 결과 (부족 영양소 + 비건 피드백)
        ↓
Voyage AI 임베딩 → 쿼리 벡터 생성
        ↓
pgvector 코사인 유사도 검색 → 관련 레시피 Top N 추출
        ↓
검색된 레시피 컨텍스트 + 사용자 데이터 → Claude 프롬프트
        ↓
개인화된 식단 추천 반환
```

### 식단 분석 흐름

```
[사용자 식단 사진 업로드 → S3 Presigned URL 직접 업로드]
        ↓
POST /api/meal → 즉시 202 반환 (meal_id)
        ↓ (비동기 - @Async + CompletableFuture)
Claude Sonnet 4 Vision → 식재료 목록 추출
        ↓
ingredient 테이블 조회 (CO2 + 영양소 정확한 수치)
        ↓
수치를 Claude 프롬프트에 삽입 → 피드백 + 부족 영양소 분석
        ↓
meal 저장 + 비건 식단이면 carbon_daily upsert
        ↓
GET /api/meal/{id} 폴링: ANALYZING → DONE
```

---

## 📁 패키지 구조

```
com.veganing
├── domain
│   ├── auth          # 회원가입, 로그인, JWT
│   ├── challenge     # 30일 챌린지, 포인트/레벨
│   ├── meal          # 식단 업로드 + Vision AI 분석
│   ├── carbon        # 탄소 절감량 집계
│   ├── ingredient    # 식재료 CO2 + 영양소 기준 데이터
│   ├── community     # 게시물, 댓글, 좋아요, 랭킹
│   ├── recipe        # 오늘의 레시피 선정
│   ├── product       # 쇼핑몰 상품
│   ├── cart          # 장바구니
│   └── order         # 주문
│
└── global
    ├── auth          # JWT 필터, CustomUserDetails
    ├── config        # Security, CORS, Redis, S3 설정
    ├── error         # GlobalExceptionHandler, CustomException
    ├── common        # ApiResponse<T> 공통 응답 포맷
    ├── scheduler     # 오늘의 레시피 자동 선정 (매일 자정)
    └── infra
        ├── vision    # Claude Sonnet 4 Vision AI 연동
        ├── voyage    # Voyage AI 임베딩 연동
        └── s3        # AWS S3 Presigned URL + 이미지 업로드
```

---

## 🗃 ERD

![ERD](docs/images/erd.png)

### 테이블 목록 (15개)

| 테이블 | 도메인 | 설명 |
|--------|--------|------|
| `users` | AUTH | 회원 정보 + 누적 포인트/레벨 + 지역 |
| `challenges` | CHALLENGE | 챌린지 진행 상태 |
| `point_history` | CHALLENGE | 포인트 적립 내역 (랭킹 집계용) |
| `ingredients` | MEAL | 식재료별 CO2 + 영양소 기준 데이터 (meals와 FK 없음) |
| `meals` | MEAL | 식단 업로드 + AI 분석 결과 |
| `carbon_daily` | CARBON | 일별 탄소 절감 집계 (비건 식단만) |
| `community_posts` | COMMUNITY | 게시물 (version 컬럼 = 낙관적 락) |
| `post_likes` | COMMUNITY | 좋아요 중복 방지 |
| `comments` | COMMUNITY | 댓글 |
| `recipes` | RECIPE | 오늘의 레시피 선정 + S3 URL + pgvector 임베딩 |
| `products` | PRODUCT | 쇼핑몰 상품 |
| `carts` | CART | 장바구니 (user당 1개, 1:1) |
| `cart_items` | CART | 장바구니 상품 목록 |
| `orders` | ORDER | 주문 |
| `order_items` | ORDER | 주문 상품 목록 (price_at_order 가격 스냅샷) |

---

## 📋 API 목록

### AUTH
| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/auth/signup` | 회원가입 | ❌ |
| POST | `/api/auth/login` | 로그인 | ❌ |
| POST | `/api/auth/logout` | 로그아웃 | ✅ |
| POST | `/api/auth/refresh` | 토큰 재발급 (Rotation) | ❌ |
| GET | `/api/auth/me` | 내 프로필 조회 | ✅ |
| PUT | `/api/auth/profile` | 프로필 수정 | ✅ |

### CHALLENGE
| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/challenge/start` | 챌린지 시작 | ✅ |
| GET | `/api/challenge/current` | 진행중인 챌린지 조회 | ✅ |
| GET | `/api/challenge/history` | 챌린지 히스토리 | ✅ |
| GET | `/api/challenge/stats` | 챌린지 통계 | ✅ |
| PUT | `/api/challenge/{id}/quit` | 챌린지 포기 | ✅ |
| POST | `/api/challenge/add-points` | 포인트 추가 | ✅ |

### MEAL
| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/meal/upload-url` | S3 Presigned URL 발급 | ✅ |
| POST | `/api/meal` | 식단 분석 요청 (202 즉시 반환, 비동기) | ✅ |
| GET | `/api/meal/history` | 식단 기록 조회 | ✅ |
| GET | `/api/meal/{id}` | 식단 상세 + 분석 상태 폴링 | ✅ |
| GET | `/api/meal/recommend` | RAG 기반 식단 추천 | ✅ |

### CARBON
| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/carbon/stats` | 전체 누적 통계 | ✅ |
| GET | `/api/carbon/today` | 오늘 절감량 | ✅ |
| GET | `/api/carbon/history?days=N` | 기간별 일별 집계 | ✅ |

### COMMUNITY
| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/community/posts` | 게시물 목록 | ❌ |
| POST | `/api/community/posts` | 게시물 작성 | ✅ |
| GET | `/api/community/posts/{id}` | 게시물 상세 | ❌ |
| PUT | `/api/community/posts/{id}` | 게시물 수정 | ✅ |
| DELETE | `/api/community/posts/{id}` | 게시물 삭제 | ✅ |
| POST | `/api/community/posts/{id}/like` | 좋아요 (낙관적 락) | ✅ |
| GET | `/api/community/posts/{id}/comments` | 댓글 목록 | ❌ |
| POST | `/api/community/posts/{id}/comments` | 댓글 작성 | ✅ |
| DELETE | `/api/community/comments/{id}` | 댓글 삭제 | ✅ |
| GET | `/api/community/ranking` | 지역별 랭킹 (all/week/month) | ✅ |

### RECIPE
| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/recipe/today` | 오늘의 레시피 목록 (Redis 캐시) | ❌ |

### PRODUCT / CART / ORDER
| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/product` | 상품 목록 | ❌ |
| GET | `/api/product/{id}` | 상품 상세 | ❌ |
| GET | `/api/cart` | 장바구니 조회 | ✅ |
| POST | `/api/cart` | 상품 담기 | ✅ |
| PUT | `/api/cart/{cartItemId}` | 수량 변경 | ✅ |
| DELETE | `/api/cart/{cartItemId}` | 상품 제거 | ✅ |
| POST | `/api/order` | 주문 생성 (비관적 락) | ✅ |
| GET | `/api/order` | 내 주문 목록 | ✅ |
| GET | `/api/order/{orderId}` | 주문 상세 | ✅ |

---

## 🧠 기술적 의사결정

### 1. Vision AI — Claude Sonnet 4 선정 (3사 벤치마크)

GPT-4o, Gemini, Claude Sonnet 4 세 모델로 동일한 식단 이미지를 분석해 식재료 추출 정확도를 비교했습니다.

> Claude Sonnet 4가 식재료 추출 개수와 정확도에서 가장 우수한 결과를 보여 최종 선정.

### 2. RAG — pgvector + Voyage AI (AWS Bedrock KB 대신)

AWS Bedrock Knowledge Base는 콘솔 설정 중심으로 임베딩~검색 전과정을 코드 레벨에서 제어할 수 없었습니다.

임베딩 모델은 OpenAI `text-embedding-3-small`, AWS Bedrock Titan, Voyage AI `voyage-3-lite` 세 모델을 벤치마크 비교한 결과 Voyage AI가 성능 우수, 가격 동등 수준으로 최종 선정했습니다.

> pgvector + Voyage AI로 직접 구현해 임베딩 생성, 저장, 코사인 유사도 검색 전 과정을 코드로 관리. 기존 PostgreSQL에 확장만 추가해 별도 인프라 불필요.

### 3. 탄소/영양소 계산 — DB 기반 (LLM 대신)

```
Claude Vision → 식재료 목록 추출
      ↓
ingredient 테이블 조회 (정확한 CO2 + 영양소 수치)
      ↓
수치를 Claude 프롬프트에 삽입
      ↓
Claude는 계산 + 피드백만 담당
```

> LLM hallucination 방지. 수치는 식약처 DB + Our World in Data 기반으로 직접 관리.

### 4. 좋아요 — 낙관적 락 vs 주문 — 비관적 락

```java
// 좋아요: 낙관적 락 (충돌 빈도 낮음, 성능 우선)
@Version
private Long version;

// 주문/재고: 비관적 락 (정합성 손실 치명적)
@Lock(LockModeType.PESSIMISTIC_WRITE)
Product findProductById(Long id);
```

### 5. 식단 분석 — 비동기 처리 (202 즉시 반환)

Claude Vision 응답 지연(5~15초) 대응. `@Async` + `CompletableFuture`로 백그라운드 처리 후 폴링 방식으로 상태 확인.

> `@Async` 프록시 우회 문제로 `MealAsyncService` 별도 클래스로 분리.

### 6. Presigned URL — 서버 메모리 절약

이미지가 백엔드를 거치지 않고 클라이언트 → S3 직접 업로드. 서버 메모리/대역폭 절약.

### 7. carbon_daily — 집계 테이블 별도 운영

Meal 저장마다 upsert → Carbon API 조회 시 전체 집계 쿼리 불필요. 비건 식단만 집계해 챌린지 동기부여 강화.

---

## 🔧 트러블슈팅 하이라이트

### #016 — @Async 프록시 우회 문제

**문제** 같은 클래스 내 `this.메서드()` 호출 시 Spring AOP 프록시를 우회해 `@Async` 무시됨.

**해결** `MealAsyncService` 별도 클래스 분리. `@Transactional`도 같은 원리로 `RecipeIndexService` 분리.

**교훈** `@Async`, `@Transactional` 등 AOP 어노테이션은 반드시 별도 빈을 통해 호출해야 함.

---

### #057 — JPA 메서드명 파싱 오류

**문제** `findTopByLikeCount(pageable)`이 "상위 N개"가 아닌 `WHERE like_count = ?` 조건으로 잘못 해석됨.

**해결** `@Query("SELECT p FROM CommunityPost p ORDER BY p.likeCount DESC")` JPQL로 교체.

**교훈** `findTopN`은 상위 N개가 아님. 정렬 기반 상위 N개는 반드시 `@Query` + `Pageable` 조합.

---

### #059 — pgvector 임베딩 skip 로직 오류

**문제** `existsByPostId`로 skip 판단 → recipes row만 있어도 embedding 없이 skip됨.

**해결** embedding 컬럼은 JPA 미매핑이라 네이티브 쿼리로 `existsByPostIdAndHasEmbedding` 추가.

**교훈** JPA 미매핑 컬럼 조건은 파생 쿼리 불가. 반드시 `@Query(nativeQuery=true)` 사용.

---

### #075 — Spring Boot 4.x Flyway 자동설정 미동작

**문제** `flyway-core`만 추가 시 클래스패스에는 있지만 Spring이 자동으로 연결 안 됨 (silent failure).

**해결** `spring-boot-starter-flyway` 스타터로 교체.

**교훈** Spring Boot 4.x 모듈화 아키텍처에서 raw 의존성만 추가하면 자동설정 안 됨. 반드시 `spring-boot-starter-*` 형태 사용.

---

### #082 — Refresh Token 미전달로 토큰 갱신 실패

**문제** 로그인 응답에 refreshToken을 포함하지 않아 프론트가 accessToken만 저장. 401 발생 시 refreshToken 대신 accessToken으로 재발급 요청 → 15분 후 갱신 불가.

**해결** `AuthResponse`에 refreshToken 필드 추가. 재발급 시마다 새 refreshToken 발급 후 Redis 갱신 (Refresh Token Rotation 적용).

**교훈** 로그인 응답 설계 시 accessToken만 내려보내면 안 됨. refreshToken도 함께 내려보내고 프론트가 별도 저장해야 갱신 루프 완성.

---

## 📊 개발 진행상황

| 단계 | 내용 | 상태 |
|------|------|------|
| Phase 1 | 프로젝트 세팅, 의존성, 서버 실행 확인 | ✅ 완료 |
| Phase 2 | 도메인형 패키지 구조 생성 | ✅ 완료 |
| Phase 3 | PostgreSQL 16 + Redis 7 Docker 연결, 프로필 분리 | ✅ 완료 |
| Phase 4 | Entity 작성 (15개 테이블) | ✅ 완료 |
| Phase 5 | JWT + Spring Security, CustomUserDetails | ✅ 완료 |
| Phase 6 | 전체 도메인 API (AUTH/CHALLENGE/COMMUNITY/MEAL/CARBON/PRODUCT/CART/ORDER) | ✅ 완료 |
| Phase 7 | Claude Sonnet 4 Vision AI + pgvector RAG + Voyage AI 임베딩, 단위테스트 48개 | ✅ 완료 |
| Phase 8 | EC2 배포, RDS, GitHub Actions CI/CD, Flyway, Vercel, Cloudflare Tunnel | ✅ 완료 |

---

<div align="center">

**⭐ Star를 눌러주시면 개발에 큰 힘이 됩니다!**

Made with 🌱 by feelow3555

</div>
