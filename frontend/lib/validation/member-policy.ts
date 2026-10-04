// 회원 정책 수치. 백엔드 MemberPolicy와 같은 값이다 (docs/design/conventions.md 4절)

export const NICKNAME_PATTERN = /^[가-힣A-Za-z0-9_]{2,12}$/
export const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*[0-9]).{8,20}$/
export const PHONE_NUMBER_PATTERN = /^01[016789][0-9]{7,8}$/
export const VERIFICATION_CODE_PATTERN = /^[0-9]{6}$/
export const EMAIL_MAX_LENGTH = 255
export const MIN_SIGN_UP_AGE = 14
export const PHONE_CODE_MAX_ATTEMPTS = 5

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
