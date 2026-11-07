package com.moa.api.application.v1.auth.command;

public record LoginCommand(String loginId, String password) {
}
