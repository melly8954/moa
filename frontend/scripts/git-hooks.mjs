// git hook 켜기: npm install 때(prepare) 저장소 루트에 git 템플릿의 .githooks가 있으면 core.hooksPath를 맞춘다
// git 저장소가 아니거나 .githooks가 없거나 hooksPath를 이미 다른 곳에 두었으면 아무것도 하지 않는다
// 설치를 막지 않도록 어떤 경우에도 실패로 끝내지 않는다 (CONTRACT.md 1절 "저장소 템플릿")
import { execFileSync } from "node:child_process"
import { existsSync } from "node:fs"
import { join } from "node:path"

function git(...args) {
  try {
    return execFileSync("git", args, { encoding: "utf8", stdio: ["ignore", "pipe", "ignore"] }).trim()
  } catch {
    return null
  }
}

const top = git("rev-parse", "--show-toplevel")
if (top && existsSync(join(top, ".githooks"))) {
  const current = git("-C", top, "config", "--get", "core.hooksPath")
  if (!current) {
    if (git("-C", top, "config", "core.hooksPath", ".githooks") !== null) {
      console.log("git hook을 켰다 (core.hooksPath=.githooks)")
    }
  } else if (current !== ".githooks") {
    console.log(`git hook을 켜지 않았다: core.hooksPath가 이미 ${current}이다`)
  }
}
