// 앱 이름, 시간대, 화폐. apply.sh --name이 name을 바꾼다. 화면 언어는 src/i18n/config.ts
//   timeZone: 시각을 보일 시간대. 브라우저 시간대가 아니라 서비스 시간대로 고정한다
//             (서버 그림과 브라우저 그림이 같고, 해외에서 열어도 같은 시각이 보인다)
//   currency: formatCurrency 기본 화폐
export const SITE = {
  name: "moa",
  timeZone: "Asia/Seoul",
  currency: "KRW",
}
