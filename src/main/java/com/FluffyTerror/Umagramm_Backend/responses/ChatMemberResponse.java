package com.FluffyTerror.Umagramm_Backend.responses;


public record ChatMemberResponse(String userId,
                                 String role, // "ADMIN" or "MEMBER"
                                 String username,
                                 String firstName,
                                 String lastName,
                                 Long profilePhotoId,
                                 String invitedBy) {

}
