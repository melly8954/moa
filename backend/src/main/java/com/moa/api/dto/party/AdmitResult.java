package com.moa.api.dto.party;

import java.util.List;

/**
 * 멤버가 된 결과 (contracts/party-api.md "멤버가 되는 모든 길")
 *
 * @param joined 남긴 JOINED 안내
 * @param cancelledPartyIds 함께 취소된 대기 신청의 파티들. 그 방장들의 신청 목록이 바뀐다
 */
public record AdmitResult(PartyMessageDto joined, List<Long> cancelledPartyIds) {
}
