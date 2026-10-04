// 회원 정책 수치. 백엔드 MemberPolicy와 같은 값이다 (docs/design/conventions.md 4절)

export const NICKNAME_MAX_LENGTH = 12
export const NICKNAME_PATTERN = /^[가-힣A-Za-z0-9_]{2,12}$/
export const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*[0-9]).{8,20}$/
export const PHONE_NUMBER_PATTERN = /^01[016789][0-9]{7,8}$/
export const VERIFICATION_CODE_PATTERN = /^[0-9]{6}$/
export const EMAIL_MAX_LENGTH = 255
export const MIN_SIGN_UP_AGE = 14
export const PHONE_CODE_MAX_ATTEMPTS = 5
/** 이메일 인증 링크 유효 시간(분) */
export const EMAIL_LINK_TTL_MINUTES = 30
/** 메일·문자 다시 보내기 간격(초) */
export const RESEND_INTERVAL_SECONDS = 60
/** 한 휴대폰 번호에 하루 보낼 수 있는 문자 수 */
export const DAILY_SMS_LIMIT = 10

/** 서비스 지역(한국) 날짜로 만 나이를 센다 */
export function ageOn(birthDate: string, today = new Date()): number {
  const [year, month, day] = birthDate.split("-").map(Number)
  const parts = new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Seoul" }).format(today).split("-").map(Number)
  let age = parts[0] - year
  if (parts[1] < month || (parts[1] === month && parts[2] < day)) {
    age -= 1
  }
  return age
}
