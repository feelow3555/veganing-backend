package com.veganing.domain.challenge.entity;

public enum ChallengeType {
    FLEXITARIAN,    // 적색육/백색육/어패류/난류/유지류/채소/과일 모두 허용
    POLLO_PESCO,    // 적색육 제외
    PESCO,          // 적색육/백색육 제외
    POLLO,          // 적색육/어패류 제외가자
    LACTO_OVO,      // 적색육/백색육/어패류 제외
    LACTO,          // 적색육/백색육/어패류/난류 제외
    OVO,            // 적색육/백색육/어패류/유지류 제외
    VEGAN,          // 적색육/백색육/어패류/난류/유지류 제외
    FRUITARIAN      // 과일류만 허용
}