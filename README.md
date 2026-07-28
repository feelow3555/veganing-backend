# 🌱 Veganing Backend

<div align="center">

**비건 라이프스타일 챌린지 플랫폼 - Spring Boot 백엔드**

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white)](https://redis.io/)
[![AWS](https://img.shields.io/badge/AWS-S3%20%7C%20Bedrock-FF9900?style=flat-square&logo=amazonaws&logoColor=white)](https://aws.amazon.com/)

> 프론트엔드 레포: [veganing (React 19)](https://github.com/feelow3555/veganing)

</div>

---

## 📑 목차
- [프로젝트 소개](#-프로젝트-소개)
- [핵심 BM 흐름](#-핵심-bm-흐름)
- [기술 스택](#-기술-스택)
- [시스템 아키텍처](#-시스템-아키텍처)
- [패키지 구조](#-패키지-구조)
- [ERD](#-erd)
- [API 목록](#-api-목록)
- [기술적 의사결정](#-기술적-의사결정)
- [개발 환경](#-개발-환경)
- [개발 진행상황](#-개발-진행상황)

---

## 💡 프로젝트 소개

**Veganing**은 비건 라이프스타일을 시작하고 유지할 수 있도록 돕는 챌린지 플랫폼입니다.

사용자는 매일 식단 사진을 업로드하면 GPT-4 Vision이 식재료를 분석하고, 부족한 영양소와 탄소 절감량을 계산해줍니다. 커뮤니티에 레시피를 공유하고, AI가 개인 맞춤 식단을 추천하며, 비건 식재료 쇼핑까지 한 번에 해결할 수 있습니다.

### 개발 배경
- 비건 입문자가 영양 불균형 없이 식단을 유지하기 어려운 문제
- 탄소 절감 효과를 수치로 확인하며 동기부여를 높이고자 함
- 커뮤니티 레시피 → AI 추천 → 쇼핑 연결로 비건 생활의 진입 장벽을 낮춤

### 진행 기간
**2026.07 ~ 진행 중** (4주 계획)

---

## 🔄 핵심 BM 흐름

```
식단 사진 업로드
      ↓
GPT-4 Vision → 식재료 추출
      ↓
ingredient 테이블 조회 → 정확한 CO2 + 영양소 수치 계산
      ↓
부족한 영양소 파악 → 식단 추천 (RAG 기반)
      ↓
추천 식단 기반 쇼핑몰 상품 연결 → 장바구니 → 주문
      ↓
커뮤니티에 레시피 등록 → 좋아요 누적
      ↓
좋아요 상위 레시피 → 오늘의 레시피 선정 (S3 저장)
      ↓
AWS Bedrock Knowledge Base 자동 인덱싱
      ↓
AI 식단 추천 시 RAG 컨텍스트로 재활용 (루프)
```

---

## 🛠 기술 스택

### Backend
<img src="https://img.shields.io/badge/Java%2021-ED8B00?style=flat-square&logo=openjdk&logoColor=white"/> <img src="https://img.shields.io/badge/Spring%20Boot%204.1-6DB33F?style=flat-square&logo=springboot&logoColor=white"/> <img src="https://img.shields.io/badge/Spring%20Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white"/> <img src="https://img.shields.io/badge/JPA%2FHibernate-59666C?style=flat-square&logo=hibernate&logoColor=white"/>

### Database & Cache
<img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white"/> <img src="https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white"/>

### AI & Cloud
<img src="https://img.shields.io/badge/OpenAI%20GPT--4%20Vision-412991?style=flat-square&logo=openai&logoColor=white"/> <img src="https://img.shields.io/badge/AWS%20S3-569A31?style=flat-square&logo=amazons3&logoColor=white"/> <img src="https://img.shields.io/badge/AWS%20Bedrock-FF9900?style=flat-square&logo=amazonaws&logoColor=white"/>

### DevOps
<img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white"/> <img src="https://img.shields.io/badge/GitHub%20Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white"/>

### 기술 선택 이유

| 기술 | 선택 이유 |
|------|----------|
| **Spring Boot** | 실무 표준, 풍부한 생태계, 포트폴리오 경쟁력 |
| **JPA / Hibernate** | 객체지향적 DB 접근, 복잡한 연관관계 관리 |
| **Spring Security + JWT** | Stateless 인증, Refresh Token으로 보안 강화 |
| **Redis** | Refresh Token 저장 + 오늘의 레시피 캐싱 (TTL 기반) |
| **AWS Bedrock** | 벡터 DB 직접 구축 대신 관리형 서비스로 RAG 구현 복잡도 감소 |

---

## 🏗 시스템 아키텍처

```
[React 19 Frontend : 5173]
        ↓ HTTP Request
[Spring Boot Backend : 8080]
        ↓
  ┌─────────────┐
  │  Controller │  ← 요청 받고 응답 반환
  └──────┬──────┘
         ↓
  ┌─────────────┐
  │   Service   │  ← 비즈니스 로직
  └──────┬──────┘
         ↓
  ┌─────────────┐
  │ Repository  │  ← DB 접근 (JPA)
  └──────┬──────┘
         ↓
  ┌─────────────┐
  │ PostgreSQL  │
  └─────────────┘

  + Redis              (JWT Refresh Token + 오늘의 레시피 캐싱)
  + AWS S3             (식단 이미지 + 레시피 텍스트 저장)
  + AWS Bedrock        (Knowledge Base RAG)
  + OpenAI API         (식단 AI 분석 + 추천)
```

### RAG 흐름

```
[스케줄러 - 매일 자정]
좋아요 상위 레시피 조회 (DB)
        ↓
레시피 텍스트 → S3 업로드
        ↓
Bedrock Knowledge Base 자동 임베딩 + 인덱싱
        ↓
Redis 캐시 갱신 (오늘의 레시피 목록)

[AI 식단 추천 요청 시]
사용자 식단 분석 결과
        ↓
Bedrock Knowledge Base 쿼리 (관련 레시피 검색)
        ↓
검색된 레시피 컨텍스트 + 사용자 데이터 → OpenAI 프롬프트
        ↓
개인화된 식단 추천 반환
```

---

## 📁 패키지 구조

```
com.veganing
├── domain                  # 기능별 비즈니스 로직
│   ├── auth                # 회원가입, 로그인, JWT
│   ├── challenge           # 30일 챌린지
│   ├── meal                # 식단 업로드 + AI 분석
│   ├── carbon              # 탄소 절감량 계산
│   ├── ingredient          # 식재료 CO2 + 영양소 DB
│   ├── community           # 게시물, 댓글, 좋아요
│   ├── recipe              # 오늘의 레시피 선정
│   ├── product             # 쇼핑몰 상품
│   ├── cart                # 장바구니
│   └── order               # 주문
│
└── global                  # 전역 공통 모듈
    ├── auth                # JWT 필터
    ├── config              # Security, CORS, Redis 설정
    ├── error               # 전역 예외 처리
    ├── common              # ApiResponse 공통 응답 포맷
    ├── scheduler           # 오늘의 레시피 자동 선정 (매일 자정)
    └── infra
        ├── openai          # GPT-4 Vision 연동
        ├── bedrock         # AWS Knowledge Base 연동
        └── s3              # AWS S3 연동
```

> **도메인형 구조를 선택한 이유**
> 계층형은 파일이 많아질수록 한 폴더에 클래스가 몰려 관리가 어렵고,
> 도메인형은 기능 단위로 응집도가 높아 확장과 유지보수가 용이합니다.

---

## 🗃 ERD

### 테이블 목록 (15개)

| 테이블 | 도메인 | 설명 |
|--------|--------|------|
| `users` | AUTH | 회원 정보 + 누적 포인트/레벨 + 지역 |
| `challenges` | CHALLENGE | 챌린지 진행 상태 |
| `point_history` | CHALLENGE | 포인트 적립 내역 (랭킹 집계용) |
| `ingredients` | MEAL | 식재료별 CO2 + 영양소 기준 데이터 |
| `meals` | MEAL | 식단 업로드 + AI 분석 결과 |
| `carbon_daily` | CARBON | 일별 탄소 절감 집계 |
| `community_posts` | COMMUNITY | 게시물 (version 컬럼 = 낙관적 락) |
| `post_likes` | COMMUNITY | 좋아요 중복 방지 |
| `comments` | COMMUNITY | 댓글 |
| `recipes` | RECIPE | 오늘의 레시피 선정 + S3 URL |
| `products` | PRODUCT | 쇼핑몰 상품 |
| `carts` | CART | 장바구니 (user당 1개) |
| `cart_items` | CART | 장바구니 상품 목록 |
| `orders` | ORDER | 주문 |
| `order_items` | ORDER | 주문 상품 목록 (가격 스냅샷) |

```
users ──< challenges ──< meals
  │              │         └── (ingredients jsonb snapshot, FK 없음)
  │         point_history
  │
  ├──< carbon_daily
  │
  ├──< community_posts ──< post_likes
  │          │ ──< comments
  │          └──> recipes (스케줄러가 좋아요 상위 선정)
  │
  ├──── carts ──< cart_items >── products
  │
  └──< orders ──< order_items

ingredients  ← 식재료 기준 데이터 (조회용, meals와 FK 관계 없음)
```

---

## 📋 API 목록

### AUTH
| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/auth/signup` | 회원가입 | ❌ |
| POST | `/api/auth/login` | 로그인 | ❌ |
| POST | `/api/auth/logout` | 로그아웃 | ✅ |
| POST | `/api/auth/refresh` | 토큰 재발급 | ❌ |
| GET | `/api/auth/me` | 내 프로필 조회 | ✅ |

### CHALLENGE
| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/challenge/start` | 챌린지 시작 | ✅ |
| GET | `/api/challenge/current` | 진행중인 챌린지 조회 | ✅ |
| GET | `/api/challenge/history` | 챌린지 히스토리 | ✅ |
| PUT | `/api/challenge/{id}/quit` | 챌린지 포기 | ✅ |

### MEAL
| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/meal/upload-url` | S3 Presigned URL 발급 | ✅ |
| POST | `/api/meal` | 식단 분석 요청 (202 즉시 반환, 비동기) | ✅ |
| GET | `/api/meal/{id}` | 식단 상세 + 분석 상태 폴링 | ✅ |
| GET | `/api/meal/recommend` | RAG 기반 식단 추천 | ✅ |

### CARBON
| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/carbon/stats` | 전체 누적 통계 | ✅ |
| GET | `/api/carbon/today` | 오늘 절감량 | ✅ |
| GET | `/api/carbon/history?days=7` | 기간별 일별 집계 | ✅ |

### COMMUNITY
| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/community/posts` | 게시물 목록 | ❌ |
| POST | `/api/community/posts` | 게시물 작성 | ✅ |
| POST | `/api/community/posts/{id}/like` | 좋아요 (낙관적 락) | ✅ |
| GET | `/api/community/ranking` | 지역별 랭킹 | ✅ |

### PRODUCT / CART / ORDER
| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/product` | 상품 목록 | ❌ |
| POST | `/api/cart` | 장바구니 담기 | ✅ |
| POST | `/api/order` | 주문 생성 (비관적 락) | ✅ |

---

## 🧠 기술적 의사결정

### 1. 탄소/영양소 계산 - LLM 대신 DB 기반

```
GPT-4 Vision → 식재료 목록 추출
      ↓
ingredient 테이블 조회 (CO2 + 영양소 정확한 수치)
      ↓
수치를 GPT 프롬프트에 삽입
      ↓
GPT는 계산 + 피드백만 담당
```

> LLM hallucination 방지 + 정확한 수치 보장을 위해 수치 계산은 DB에서 직접 처리

### 2. 좋아요 - 낙관적 락 (Optimistic Lock)

```java
@Version
private Long version; // 충돌 감지용
```

> 좋아요는 충돌 빈도가 낮고 성능이 중요 → 비관적 락 대신 낙관적 락 + 재시도 로직

### 3. 주문/재고 - 비관적 락 (Pessimistic Lock)

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Product findProductById(Long id); // SELECT FOR UPDATE
```

> 재고 차감은 정합성 손실이 치명적 → 반드시 비관적 락으로 정확성 보장

### 4. 식단 분석 - 비동기 처리 (202 즉시 반환)

```
POST /api/meal → 즉시 202 반환 (meal_id 포함)
      ↓ (백그라운드)
GPT-4 Vision 분석 (5~15초)
      ↓
GET /api/meal/{id} 폴링으로 상태 확인
ANALYZING → DONE → FAILED
```

> GPT-4 Vision 응답 지연(5~15초) 대응, 타임아웃 방지

### 5. meals.ingredients - jsonb 스냅샷 (FK 없음)

```json
[
  { "ingredient_id": 12, "name": "두부", "amount_g": 150,
    "co2": 3.0, "calories": 117, "protein": 12.0 }
]
```

> 분석 시점의 수치 고정. 이후 ingredients 데이터가 수정돼도 과거 기록에 영향 없음

### 6. Redis 활용

| 용도 | 방식 |
|------|------|
| Refresh Token | TTL 자동 만료 + 로그아웃 시 즉시 삭제 |
| 오늘의 레시피 | 매일 자정 스케줄러 갱신, TTL 24시간 캐싱 |

---

## ⚙️ 개발 환경

| 항목 | 값 |
|------|-----|
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Build Tool | Gradle (Groovy) |
| IDE | IntelliJ IDEA |
| Database | PostgreSQL (개발: H2) |
| Cache | Redis |
| Packaging | Jar |
| Config | YAML |

---

## 📊 개발 진행상황

| 단계 | 내용 | 상태 |
|------|------|------|
| Phase 1 | 프로젝트 세팅, 의존성, 서버 실행 확인 | ✅ 완료 |
| Phase 2 | 도메인형 패키지 구조 생성 | ✅ 완료 |
| Phase 3 | PostgreSQL + Redis 연결 | 🔜 예정 |
| Phase 4 | Entity 작성 (15개 테이블) | 🔜 예정 |
| Phase 5 | JWT + Spring Security | 🔜 예정 |
| Phase 6 | 핵심 비즈니스 로직 (Meal AI, Carbon) | 🔜 예정 |
| Phase 7 | RAG + 커머스 (S3, Bedrock, 주문) | 🔜 예정 |
| Phase 8 | Docker + EC2 배포 + CI/CD | 🔜 예정 |

---

<div align="center">

**⭐ Star를 눌러주시면 개발에 큰 힘이 됩니다!**

Made with 🌱 by feelow3555

</div>
