package jabaclass.user.auth.application.service;

import org.springframework.stereotype.Service;

@Service
public class ReportTheftPageService {

    public String buildPage(String token) {
        return """
            <!DOCTYPE html>
            <html lang="ko">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>보안 알림 - 잡아클래스</title>
              <style>
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body {
                  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                  background: #f3f6fb;
                  min-height: 100vh;
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  padding: 16px;
                }
                .card {
                  background: #ffffff;
                  border-radius: 20px;
                  box-shadow: 0 10px 30px rgba(15,23,42,0.08);
                  max-width: 480px;
                  width: 100%%;
                  overflow: hidden;
                  border: 1px solid #e5e7eb;
                }
                .header {
                  background: linear-gradient(135deg, #111827 0%%, #1f2937 100%%);
                  padding: 36px 32px 28px;
                }
                .badge {
                  display: inline-block;
                  padding: 6px 12px;
                  background: rgba(255,255,255,0.12);
                  border-radius: 999px;
                  font-size: 12px;
                  font-weight: 700;
                  letter-spacing: 0.4px;
                  color: #f9fafb;
                  margin-bottom: 16px;
                }
                .header h1 {
                  font-size: 24px;
                  font-weight: 800;
                  color: #ffffff;
                  line-height: 1.3;
                }
                .header p {
                  margin-top: 10px;
                  font-size: 14px;
                  color: #d1d5db;
                  line-height: 1.6;
                }
                .body { padding: 32px; }
                .device-box {
                  background: linear-gradient(180deg, #f9fbff 0%%, #f3f6fb 100%%);
                  border: 1px solid #dbe3f0;
                  border-radius: 16px;
                  padding: 20px 24px;
                  margin-bottom: 28px;
                  display: flex;
                  align-items: center;
                  gap: 16px;
                }
                .device-icon { font-size: 38px; line-height: 1; }
                .device-name { font-size: 16px; font-weight: 700; color: #111827; }
                .device-sub { font-size: 13px; color: #6b7280; margin-top: 4px; }
                .question {
                  font-size: 15px;
                  font-weight: 600;
                  color: #374151;
                  margin-bottom: 16px;
                }
                .btn {
                  display: block;
                  width: 100%%;
                  padding: 14px 20px;
                  border-radius: 12px;
                  font-size: 15px;
                  font-weight: 700;
                  cursor: pointer;
                  border: none;
                  transition: background 0.2s;
                  margin-bottom: 12px;
                }
                .btn:last-child { margin-bottom: 0; }
                .btn-danger { background: #ef4444; color: #ffffff; }
                .btn-danger:hover { background: #dc2626; }
                .btn-safe {
                  background: #f3f4f6;
                  color: #374151;
                  border: 1px solid #e5e7eb;
                }
                .btn-safe:hover { background: #e5e7eb; }
                .result {
                  display: none;
                  text-align: center;
                  padding: 16px 0 8px;
                }
                .result-icon { font-size: 44px; margin-bottom: 14px; }
                .result-title {
                  font-size: 20px;
                  font-weight: 800;
                  color: #111827;
                  margin-bottom: 10px;
                }
                .result-sub {
                  font-size: 14px;
                  color: #6b7280;
                  line-height: 1.7;
                }
                .footer {
                  padding: 18px 32px;
                  background: #fafafa;
                  border-top: 1px solid #e5e7eb;
                  font-size: 12px;
                  color: #9ca3af;
                }
              </style>
            </head>
            <body>
              <div class="card">
                <div class="header">
                  <span class="badge">SECURITY ALERT</span>
                  <h1>새 기기 로그인 감지</h1>
                  <p>회원님의 계정에 새로운 기기에서 로그인이 시도되었습니다.</p>
                </div>
                <div class="body">
                  <div class="device-box">
                    <div class="device-icon" id="deviceIcon">💻</div>
                    <div>
                      <div class="device-name" id="deviceName">알 수 없는 기기</div>
                      <div class="device-sub" id="deviceSub">기기 정보 없음</div>
                    </div>
                  </div>

                  <div id="actionSection">
                    <div class="question">이 로그인이 본인의 활동인가요?</div>
                    <button class="btn btn-danger" onclick="handleNo()">
                      아니요, 계정을 보호해 주세요
                    </button>
                    <button class="btn btn-safe" onclick="handleYes()">
                      예, 본인의 활동입니다
                    </button>
                  </div>

                  <div class="result" id="resultSection">
                    <div class="result-icon" id="resultIcon">🔒</div>
                    <div class="result-title" id="resultTitle"></div>
                    <div class="result-sub" id="resultSub"></div>
                  </div>
                </div>
                <div class="footer">© Jaba Class. All rights reserved.</div>
              </div>

              <script>
                const TOKEN = '%s';

                (function detectDevice() {
                  const ua = navigator.userAgent;
                  let icon, name, sub;
                  if (/iPhone/.test(ua)) {
                    icon = '📱'; name = 'iPhone'; sub = 'iOS 기기';
                  } else if (/iPad/.test(ua)) {
                    icon = '📱'; name = 'iPad'; sub = 'iPadOS 기기';
                  } else if (/SM-|Samsung/.test(ua)) {
                    icon = '📱'; name = 'Samsung Galaxy'; sub = 'Android 기기';
                  } else if (/Android/.test(ua)) {
                    icon = '📱'; name = 'Android 기기'; sub = 'Android';
                  } else if (/Windows/.test(ua)) {
                    icon = '🖥️'; name = 'Windows PC'; sub = '데스크탑';
                  } else if (/Macintosh/.test(ua)) {
                    icon = '💻'; name = 'Mac'; sub = '데스크탑';
                  } else {
                    icon = '💻'; name = '알 수 없는 기기'; sub = '기기 정보 없음';
                  }
                  document.getElementById('deviceIcon').textContent = icon;
                  document.getElementById('deviceName').textContent = name;
                  document.getElementById('deviceSub').textContent = sub;
                })();

                function showResult(icon, title, sub) {
                  document.getElementById('actionSection').style.display = 'none';
                  document.getElementById('resultIcon').textContent = icon;
                  document.getElementById('resultTitle').textContent = title;
                  document.getElementById('resultSub').textContent = sub;
                  document.getElementById('resultSection').style.display = 'block';
                }

                async function handleNo() {
                  try {
                    const res = await fetch('/api/v1/auth/report-theft?token=' + encodeURIComponent(TOKEN), {
                      method: 'POST'
                    });
                    if (res.ok) {
                      showResult('🔒', '계정이 보호됐습니다', '모든 세션이 종료되었습니다.\\n안전한 비밀번호로 변경해 주세요.');
                    } else {
                      showResult('⚠️', '오류가 발생했습니다', '잠시 후 다시 시도해 주세요.');
                    }
                  } catch {
                    showResult('⚠️', '오류가 발생했습니다', '잠시 후 다시 시도해 주세요.');
                  }
                }

                function handleYes() {
                  showResult('✅', '감사합니다', '본인 확인이 완료되었습니다.');
                }
              </script>
            </body>
            </html>
            """.formatted(token);
    }
}