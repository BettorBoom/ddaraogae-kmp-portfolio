"""Render the two-page submission PDF from the same screenshots as README."""
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas
from reportlab.platypus import Paragraph


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "output/pdf/ddaraogae-kmp-portfolio.pdf"
FONT = "/System/Library/Fonts/Supplemental/AppleGothic.ttf"
W, H = 595.28, 841.89
INK = colors.HexColor("#292929")
MUTED = colors.HexColor("#77716d")
BRAND = colors.HexColor("#a3441d")
CARD = colors.HexColor("#f0e6de")
PAPER = colors.HexColor("#fffaf6")


def para(c, value, x, top, width, size=10, leading=None, color=INK):
    style = ParagraphStyle("body", fontName="Korean", fontSize=size,
                           leading=leading or size * 1.55, textColor=color,
                           alignment=TA_LEFT, spaceAfter=0)
    item = Paragraph(value, style)
    _, height = item.wrap(width, H)
    item.drawOn(c, x, top - height)
    return top - height


def label(c, value, x, y, size=10, color=INK):
    c.setFillColor(color)
    c.setFont("Korean", size)
    c.drawString(x, y, value)


def rounded(c, x, y, width, height, fill=CARD, radius=14):
    c.setFillColor(fill)
    c.roundRect(x, y, width, height, radius, fill=1, stroke=0)


def image_fit(c, path, x, y, width, height):
    picture = ImageReader(str(path))
    pw, ph = picture.getSize()
    ratio = min(width / pw, height / ph)
    drawn_w, drawn_h = pw * ratio, ph * ratio
    c.drawImage(picture, x + (width - drawn_w) / 2, y + (height - drawn_h) / 2,
                drawn_w, drawn_h, mask="auto")


def page_frame(c, page):
    c.setFillColor(PAPER)
    c.rect(0, 0, W, H, fill=1, stroke=0)
    label(c, "DDARAOGAE  /  KOTLIN MULTIPLATFORM", 42, H - 44, 8.5, BRAND)
    c.setStrokeColor(CARD)
    c.line(42, H - 57, W - 42, H - 57)
    label(c, "2026.10  ·  채용 지원용 기술 포트폴리오", 42, 34, 8, MUTED)
    label(c, f"{page} / 2", W - 68, 34, 8, MUTED)


def row(c, title, detail, y):
    rounded(c, 42, y, W - 84, 46)
    label(c, title, 56, y + 18, 10, BRAND)
    para(c, detail, 126, y + 31, W - 183, 9.4)


def main():
    pdfmetrics.registerFont(TTFont("Korean", FONT))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    c = canvas.Canvas(str(OUT), pagesize=(W, H))
    c.setTitle("따라오개 - Android 우선 KMP 마이그레이션 포트폴리오")
    c.setAuthor("BettorBoom")

    page_frame(c, 1)
    label(c, "따라오개", 42, H - 112, 30, BRAND)
    label(c, "산책 기록의 핵심 흐름을 Android와 iOS에 다시 만들었습니다.", 42, H - 141, 12)
    para(c, "기능별 KMP 모듈로 상태와 저장을 공유하고, 위치 권한과 앱 수명은 각 플랫폼에서 처리합니다. "
         "완료한 범위와 미완료한 범위를 구분합니다.", 42, H - 162, W - 84, 9.4, color=MUTED)
    para(c, "원본은 nbc-final-team2/ddaraogae 팀 프로젝트입니다. 공개 로고와 고정 샘플 지도 자산을 재사용했고, "
         "화면과 데이터 수명은 공통 Compose·Room 구조로 다시 구현했습니다.", 42, H - 198, W - 84, 8.1,
         color=MUTED)
    row(c, "Android", "앱 사용 중 위치·거리·경로를 Room에 기록", H - 279)
    row(c, "iOS", "Core Location 위치·거리·경로를 같은 산책 계약으로 기록", H - 333)
    row(c, "공통", "Room v1→v4 보존 migration·산책 상태·이동선", H - 387)
    label(c, "실제 실행 화면", 42, H - 431, 11)
    para(c, "Android 에뮬레이터에 좌표를 주입해 21m, iOS Simulator의 Core Location에 경로를 주입해 "
         "저장된 거리를 확인했습니다. 야외 실측이나 실기기 결과는 아닙니다.", 42, H - 445,
         W - 84, 8.6, color=MUTED)
    for x, path, caption in (
        (62, ROOT / "docs/evidence/submission/android-record.png", "Android · 저장된 이동선"),
        (325, ROOT / "docs/evidence/submission/ios-gps-record.png", "iOS · Core Location 기록"),
    ):
        rounded(c, x, 68, 208, 283, colors.white)
        image_fit(c, path, x + 12, 73, 184, 273)
        label(c, caption, x, 55, 8.5, MUTED)
    c.showPage()

    page_frame(c, 2)
    label(c, "설계 선택과 검증", 42, H - 105, 25, BRAND)
    para(c, "작은 기능 경계, 명시적인 플랫폼 책임, 실패를 재현하는 검사로 유지보수 비용을 관리했습니다.",
         42, H - 126, W - 84, 9.4, color=MUTED)
    label(c, "의존성 방향", 42, H - 174, 11)
    for index, (title, detail) in enumerate((
        ("Android / iOS host", "권한 · 수명 · 공식 네이티브 SDK"),
        ("sharedApp", "Navigation 3 · 사용자 범위 조립"),
        ("feature:dogs / walk / history", "화면과 상호작용"),
        ("domain + data", "산책 계약 · Room · 좌표 검증"),
    )):
        y = H - 226 - index * 54
        rounded(c, 42, y, W - 84, 43)
        label(c, title, 56, y + 15, 9, BRAND)
        label(c, detail, 312, y + 15, 8.7)
    label(c, "의사결정의 근거", 42, H - 424, 11)
    y = H - 447
    for title, detail in (
        ("Koin + 생성자 주입", "기능별 등록과 사용자 수명을 한곳에서 검증"),
        ("Room v1→v4", "과거 산책 보존, 위치·거리 원자적 저장"),
        ("Swift Core Location", "권한과 앱 활성 상태는 host, 저장 검증은 KMP"),
        ("Debug 합성 경로", "제품 DB·Release와 분리해 시연 좌표를 구별"),
    ):
        label(c, "• " + title, 50, y, 8.7, BRAND)
        label(c, detail, 242, y, 8.2)
        y -= 29
    label(c, "검증된 범위", 42, H - 581, 11)
    para(c, "공통 Kotlin 39개 · Android 제품 1개와 시나리오 3개 · iOS 제품 2개와 시나리오 5개, "
         "Core Location 주입 1개. Android/iOS 각 39개 상태별 PNG와 Release fixture 차단을 확인했습니다.",
         42, H - 596, W - 84, 8.8)
    rounded(c, 42, 113, W - 84, 72)
    para(c, "<b>제한</b>  로그인·클라우드 동기화, 양 플랫폼 백그라운드 추적, 실제 지도 타일, "
         "서명 배포/TestFlight·실기기 검증은 아직 완료하지 않았습니다. "
         "화면 지도는 실제 좌표와 맞지 않는 고정 샘플임을 표시합니다.",
         55, 167, W - 110, 8.7)
    label(c, "코드·시연·결정 기록", 42, 76, 8.5, MUTED)
    label(c, "github.com/BettorBoom/ddaraogae-kmp-portfolio", 176, 76, 8.5, BRAND)
    c.showPage()
    c.save()
    print(OUT)


if __name__ == "__main__":
    main()
