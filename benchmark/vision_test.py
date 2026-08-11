"""
=============================================================
🔬 Vision AI 모델 벤치마크 테스트 (2단계)
=============================================================

[목적]
식단 사진에서 식재료 추출 + 영양소 피드백까지
전체 MEAL API 흐름에 최적화된 Vision 모델 선정

[비교 모델]
- GPT-4o           (OpenAI)
- Gemini 3.5 Flash (Google)
- Claude Sonnet 4  (Anthropic)

[테스트 조건]
- 조건 A: 사진만
- 조건 B: 사진 + 음식 이름 + 설명

[테스트 흐름]
1단계 - 식재료 추출 (Vision)
  사진 (+ 텍스트 힌트) → [{"name": "두부", "amount_g": 150}, ...]

2단계 - 영양소 피드백
  추출 결과 + DB 수치(mock) → 비건 관점 분석 + 개선 제안

[비교 항목]
1. 재료 추출 수        : 많을수록 ingredient 테이블 매핑 가능성 높아짐
2. JSON 파싱 성공 여부 : 형식 준수율
3. 피드백 품질         : 비건 핵심 영양소(B12/D/오메가3) 언급 여부 + 구체성
4. 응답 시간 (ms)      : 빠를수록 UX 유리
5. 토큰 수 + 비용      : 운영 비용 비교

[모델별 토큰 가격]
- GPT-4o           : 입력 $2.50 / 출력 $10.00 (per 1M tokens)
- Gemini 3.5 Flash : 입력 $0.15 / 출력 $0.60  (per 1M tokens)
- Claude Sonnet 4  : 입력 $3.00 / 출력 $15.00 (per 1M tokens)

[결과]
benchmark_result.json 으로 저장
=============================================================
"""

import os
import base64
import time
import json
from pathlib import Path
from dotenv import load_dotenv
from google import genai
from google.genai import types
import openai
import anthropic

# .env 로드
load_dotenv(dotenv_path=Path(__file__).parent.parent / ".env")

OPENAI_API_KEY    = os.getenv("OPENAI_API_KEY")
ANTHROPIC_API_KEY = os.getenv("ANTHROPIC_API_KEY")
GEMINI_API_KEY    = os.getenv("GEMINI_API_KEY")

# 토큰 가격 (per 1M tokens, USD)
PRICING = {
    "gpt-4o":            {"input": 2.50,  "output": 10.00},
    "gemini-3.5-flash":  {"input": 0.15,  "output": 0.60},
    "claude-sonnet-4-5": {"input": 3.00,  "output": 15.00},
}

# =============================================================
# 이미지 파일명 → 음식 정보 매핑
# =============================================================
FOOD_INFO = {
    "images.jpeg":     {"name": "비건 샐러드",  "description": "옥수수, 루꼴라, 방울토마토가 들어간 비건 샐러드"},
    "images (1).jpeg": {"name": "일반 도시락",  "description": "소시지, 닭강정, 밥이 포함된 일반 도시락"},
    "images (2).jpeg": {"name": "육회 비빔밥",  "description": "육회와 계란노른자가 올라간 비빔밥"},
    "images (3).jpeg": {"name": "된장찌개",     "description": "두부, 감자, 애호박이 들어간 된장찌개"},
    "images (4).jpeg": {"name": "두부 스테이크","description": "두부로 만든 스테이크에 소스와 채소가 곁들여진 요리"},
}

# =============================================================
# 프롬프트
# =============================================================
EXTRACT_PROMPT = """이 음식 사진에서 보이는 식재료와 예상 중량을 추출해줘.
JSON 배열로만 반환해. 설명 없이 JSON만.
예시: [{"name": "두부", "amount_g": 150}, {"name": "브로콜리", "amount_g": 100}]"""

def build_extract_prompt_with_info(food_name: str, food_description: str) -> str:
    return f"""음식 이름: {food_name}
음식 설명: {food_description}

이 음식 사진에서 보이는 식재료와 예상 중량을 추출해줘.
음식 이름과 설명을 참고해서 더 정확하게 추출해줘.
JSON 배열로만 반환해. 설명 없이 JSON만.
예시: [{{"name": "두부", "amount_g": 150}}, {{"name": "브로콜리", "amount_g": 100}}]"""

def build_feedback_prompt(ingredients: list) -> str:
    mock_nutrition = {
        "두부":   {"calories": 117, "protein": 12.0, "vitamin_b12": 0.0, "vitamin_d": 0.0, "omega3": 0.3},
        "브로콜리": {"calories": 34,  "protein": 2.8,  "vitamin_b12": 0.0, "vitamin_d": 0.0, "omega3": 0.0},
        "현미밥": {"calories": 218, "protein": 4.5,  "vitamin_b12": 0.0, "vitamin_d": 0.0, "omega3": 0.0},
        "참기름": {"calories": 120, "protein": 0.0,  "vitamin_b12": 0.0, "vitamin_d": 0.0, "omega3": 0.3},
        "시금치": {"calories": 23,  "protein": 2.9,  "vitamin_b12": 0.0, "vitamin_d": 0.0, "omega3": 0.1},
    }
    lines = []
    for item in ingredients:
        name     = item.get("name", "")
        amount_g = item.get("amount_g", 100)
        n        = mock_nutrition.get(name, {"calories": 50, "protein": 1.0, "vitamin_b12": 0.0, "vitamin_d": 0.0, "omega3": 0.0})
        r        = amount_g / 100
        lines.append(
            f"{name} {amount_g}g: 칼로리 {n['calories']*r:.0f}kcal, "
            f"단백질 {n['protein']*r:.1f}g, 비타민B12 {n['vitamin_b12']*r:.1f}μg, "
            f"비타민D {n['vitamin_d']*r:.1f}μg, 오메가3 {n['omega3']*r:.1f}g"
        )
    nutrition_text = "\n".join(lines) if lines else "식재료 정보 없음"
    return f"""아래는 비건 식단 분석 결과야.

{nutrition_text}

비건 관점에서 다음을 분석해줘:
1. 부족한 영양소 (특히 비타민B12, 비타민D, 오메가3)
2. 구체적인 개선 방법
3. 전반적인 식단 평가

한국어로 간결하게 답해줘."""


def encode_image(image_path: str) -> str:
    with open(image_path, "rb") as f:
        return base64.b64encode(f.read()).decode("utf-8")


def calc_cost(model_key: str, input_tokens: int, output_tokens: int) -> float:
    p = PRICING[model_key]
    return (input_tokens / 1_000_000 * p["input"]) + (output_tokens / 1_000_000 * p["output"])


# =============================================================
# GPT-4o
# =============================================================
def test_openai(image_path: str, extract_prompt: str) -> dict:
    client       = openai.OpenAI(api_key=OPENAI_API_KEY)
    base64_image = encode_image(image_path)

    # 1단계: 추출
    start = time.time()
    extract_res = client.chat.completions.create(
        model="gpt-4o",
        messages=[{
            "role": "user",
            "content": [
                {"type": "text", "text": extract_prompt},
                {"type": "image_url", "image_url": {"url": f"data:image/jpeg;base64,{base64_image}"}},
            ],
        }],
        max_tokens=500,
    )
    extract_time = int((time.time() - start) * 1000)

    raw = extract_res.choices[0].message.content.strip()
    raw = raw.replace("```json", "").replace("```", "").strip()
    try:
        ingredients   = json.loads(raw)
        parse_success = True
    except:
        ingredients   = []
        parse_success = False

    # 2단계: 피드백
    start = time.time()
    feedback_res = client.chat.completions.create(
        model="gpt-4o",
        messages=[{"role": "user", "content": build_feedback_prompt(ingredients)}],
        max_tokens=500,
    )
    feedback_time = int((time.time() - start) * 1000)

    input_tokens  = extract_res.usage.prompt_tokens     + feedback_res.usage.prompt_tokens
    output_tokens = extract_res.usage.completion_tokens + feedback_res.usage.completion_tokens

    return {
        "model":              "gpt-4o",
        "ingredients":        ingredients,
        "ingredient_count":   len(ingredients),
        "json_parse_success": parse_success,
        "extract_time_ms":    extract_time,
        "feedback":           feedback_res.choices[0].message.content.strip(),
        "feedback_time_ms":   feedback_time,
        "total_time_ms":      extract_time + feedback_time,
        "input_tokens":       input_tokens,
        "output_tokens":      output_tokens,
        "estimated_cost_usd": round(calc_cost("gpt-4o", input_tokens, output_tokens), 6),
    }


# =============================================================
# Gemini 3.5 Flash
# =============================================================
def test_gemini(image_path: str, extract_prompt: str) -> dict:
    client = genai.Client(api_key=GEMINI_API_KEY)

    with open(image_path, "rb") as f:
        image_data = f.read()

    # 1단계: 추출
    start = time.time()
    extract_res = client.models.generate_content(
        model="gemini-3.5-flash",
        contents=[
            types.Part.from_bytes(data=image_data, mime_type="image/jpeg"),
            types.Part.from_text(text=extract_prompt),
        ],
    )
    extract_time = int((time.time() - start) * 1000)

    raw = extract_res.text.strip()
    raw = raw.replace("```json", "").replace("```", "").strip()
    try:
        ingredients   = json.loads(raw)
        parse_success = True
    except:
        ingredients   = []
        parse_success = False

    # 2단계: 피드백
    start = time.time()
    feedback_res = client.models.generate_content(
        model="gemini-3.5-flash",
        contents=build_feedback_prompt(ingredients),
    )
    feedback_time = int((time.time() - start) * 1000)

    input_tokens  = (extract_res.usage_metadata.prompt_token_count +
                     feedback_res.usage_metadata.prompt_token_count)
    output_tokens = (extract_res.usage_metadata.candidates_token_count +
                     feedback_res.usage_metadata.candidates_token_count)

    return {
        "model":              "gemini-3.5-flash",
        "ingredients":        ingredients,
        "ingredient_count":   len(ingredients),
        "json_parse_success": parse_success,
        "extract_time_ms":    extract_time,
        "feedback":           feedback_res.text.strip(),
        "feedback_time_ms":   feedback_time,
        "total_time_ms":      extract_time + feedback_time,
        "input_tokens":       input_tokens,
        "output_tokens":      output_tokens,
        "estimated_cost_usd": round(calc_cost("gemini-3.5-flash", input_tokens, output_tokens), 6),
    }


# =============================================================
# Claude Sonnet 4
# =============================================================
def test_claude(image_path: str, extract_prompt: str) -> dict:
    client       = anthropic.Anthropic(api_key=ANTHROPIC_API_KEY)
    base64_image = encode_image(image_path)

    # 1단계: 추출
    start = time.time()
    extract_res = client.messages.create(
        model="claude-sonnet-4-5",
        max_tokens=500,
        messages=[{
            "role": "user",
            "content": [
                {"type": "image", "source": {"type": "base64", "media_type": "image/jpeg", "data": base64_image}},
                {"type": "text", "text": extract_prompt},
            ],
        }],
    )
    extract_time = int((time.time() - start) * 1000)

    raw = extract_res.content[0].text.strip()
    raw = raw.replace("```json", "").replace("```", "").strip()
    try:
        ingredients   = json.loads(raw)
        parse_success = True
    except:
        ingredients   = []
        parse_success = False

    # 2단계: 피드백
    start = time.time()
    feedback_res = client.messages.create(
        model="claude-sonnet-4-5",
        max_tokens=500,
        messages=[{"role": "user", "content": build_feedback_prompt(ingredients)}],
    )
    feedback_time = int((time.time() - start) * 1000)

    input_tokens  = extract_res.usage.input_tokens  + feedback_res.usage.input_tokens
    output_tokens = extract_res.usage.output_tokens + feedback_res.usage.output_tokens

    return {
        "model":              "claude-sonnet-4-5",
        "ingredients":        ingredients,
        "ingredient_count":   len(ingredients),
        "json_parse_success": parse_success,
        "extract_time_ms":    extract_time,
        "feedback":           feedback_res.content[0].text.strip(),
        "feedback_time_ms":   feedback_time,
        "total_time_ms":      extract_time + feedback_time,
        "input_tokens":       input_tokens,
        "output_tokens":      output_tokens,
        "estimated_cost_usd": round(calc_cost("claude-sonnet-4-5", input_tokens, output_tokens), 6),
    }


# =============================================================
# 요약 출력
# =============================================================
def print_summary(label: str, all_results: list):
    print(f"\n{'=' * 60}")
    print(f"📊 {label} - 전체 요약")
    print("=" * 60)

    model_stats = {}
    for item in all_results:
        for r in item["results"]:
            m = r["model"]
            if m not in model_stats:
                model_stats[m] = {
                    "total_ingredients":   0,
                    "total_time":          0,
                    "parse_success":       0,
                    "total_input_tokens":  0,
                    "total_output_tokens": 0,
                    "total_cost":          0,
                    "count":               0,
                }
            model_stats[m]["total_ingredients"]   += r["ingredient_count"]
            model_stats[m]["total_time"]          += r["total_time_ms"]
            model_stats[m]["parse_success"]       += 1 if r["json_parse_success"] else 0
            model_stats[m]["total_input_tokens"]  += r.get("input_tokens", 0)
            model_stats[m]["total_output_tokens"] += r.get("output_tokens", 0)
            model_stats[m]["total_cost"]          += r.get("estimated_cost_usd", 0)
            model_stats[m]["count"]               += 1

    for model, s in model_stats.items():
        c = s["count"]
        print(f"\n[ {model} ]")
        print(f"  평균 재료 추출 수  : {s['total_ingredients'] / c:.1f}개")
        print(f"  평균 총 응답시간   : {s['total_time'] / c:.0f}ms")
        print(f"  JSON 파싱 성공률   : {s['parse_success']}/{c}")
        print(f"  총 입력 토큰       : {s['total_input_tokens']:,}")
        print(f"  총 출력 토큰       : {s['total_output_tokens']:,}")
        print(f"  5장 총 비용        : ${s['total_cost']:.6f}")
        print(f"  장당 평균 비용     : ${s['total_cost'] / c:.6f}")


# =============================================================
# 벤치마크 실행
# =============================================================
def run_benchmark():
    images_dir  = Path(__file__).parent / "images"
    image_files = (
        list(images_dir.glob("*.jpg")) +
        list(images_dir.glob("*.jpeg")) +
        list(images_dir.glob("*.png"))
    )

    if not image_files:
        print("❌ benchmark/images/ 폴더에 사진이 없습니다.")
        return

    print(f"📸 테스트 사진 {len(image_files)}장 발견\n")

    results_a = []  # 조건 A: 사진만
    results_b = []  # 조건 B: 사진 + 음식 정보

    for image_path in image_files:
        food      = FOOD_INFO.get(image_path.name, {"name": "알 수 없음", "description": "정보 없음"})
        prompt_a  = EXTRACT_PROMPT
        prompt_b  = build_extract_prompt_with_info(food["name"], food["description"])

        # ── 조건 A ──────────────────────────────────────────
        print(f"\n{'=' * 60}")
        print(f"🅐 사진만 | [{image_path.name}] ({food['name']})")
        print("=" * 60)

        res_a = []
        for test_fn in [test_openai, test_gemini, test_claude]:
            try:
                r = test_fn(str(image_path), prompt_a)
                res_a.append(r)
                print(f"\n✅ {r['model']}")
                print(f"   재료 수    : {r['ingredient_count']}개  |  파싱: {'성공' if r['json_parse_success'] else '실패'}")
                print(f"   응답시간   : {r['total_time_ms']}ms  |  비용: ${r['estimated_cost_usd']:.6f}")
                print(f"   추출 재료  : {r['ingredients']}")
            except Exception as e:
                print(f"❌ {test_fn.__name__} 실패: {e}")

        results_a.append({"image": image_path.name, "food": food["name"], "results": res_a})

        # ── 조건 B ──────────────────────────────────────────
        print(f"\n{'=' * 60}")
        print(f"🅑 사진 + 음식정보 | [{image_path.name}] ({food['name']})")
        print(f"   힌트: {food['description']}")
        print("=" * 60)

        res_b = []
        for test_fn in [test_openai, test_gemini, test_claude]:
            try:
                r = test_fn(str(image_path), prompt_b)
                res_b.append(r)
                print(f"\n✅ {r['model']}")
                print(f"   재료 수    : {r['ingredient_count']}개  |  파싱: {'성공' if r['json_parse_success'] else '실패'}")
                print(f"   응답시간   : {r['total_time_ms']}ms  |  비용: ${r['estimated_cost_usd']:.6f}")
                print(f"   추출 재료  : {r['ingredients']}")
            except Exception as e:
                print(f"❌ {test_fn.__name__} 실패: {e}")

        results_b.append({"image": image_path.name, "food": food["name"], "results": res_b})

    # 요약
    print_summary("조건 A - 사진만", results_a)
    print_summary("조건 B - 사진 + 음식정보", results_b)

    # 조건별 비교
    print(f"\n{'=' * 60}")
    print("📊 조건 A vs 조건 B 비교")
    print("=" * 60)

    def get_avg_ingredients(results):
        counts = [r["ingredient_count"] for item in results for r in item["results"]]
        return sum(counts) / len(counts) if counts else 0

    print(f"\n  평균 재료 추출 수")
    print(f"  조건 A (사진만)       : {get_avg_ingredients(results_a):.1f}개")
    print(f"  조건 B (사진+음식정보): {get_avg_ingredients(results_b):.1f}개")

    # JSON 저장
    output_path = Path(__file__).parent / "benchmark_result.json"
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump({"condition_a": results_a, "condition_b": results_b}, f, ensure_ascii=False, indent=2)
    print(f"\n💾 결과 저장: {output_path}")


if __name__ == "__main__":
    run_benchmark()