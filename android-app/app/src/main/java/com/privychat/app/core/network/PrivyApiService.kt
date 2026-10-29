package com.privychat.app.core.network

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*
import java.util.UUID

interface PrivyApiService {

    // Auth
    @POST("auth/register")
    suspend fun register(@Body req: RegisterRequestDto): Response<UserProfileDto>

    @POST("auth/verify-email-otp")
    suspend fun verifyEmailOtp(@Body req: OtpVerificationDto): Response<Map<String, Boolean>>

    @POST("auth/verify-phone-otp")
    suspend fun verifyPhoneOtp(@Body req: OtpVerificationDto): Response<Map<String, Boolean>>

    @POST("auth/login")
    suspend fun login(@Body req: LoginRequestDto): Response<AuthResponseDto>

    @POST("auth/refresh")
    suspend fun refresh(@Body req: RefreshTokenDto): Response<AuthResponseDto>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    // Profile & Settings
    @GET("profile")
    suspend fun getProfile(): Response<UserProfileDto>

    @PUT("profile")
    suspend fun updateProfile(@Body req: UpdateProfileDto): Response<UserProfileDto>

    @GET("profile/privacy")
    suspend fun getPrivacySettings(): Response<PrivacySettingsDto>

    @PUT("profile/privacy")
    suspend fun updatePrivacySettings(@Body req: PrivacySettingsDto): Response<PrivacySettingsDto>

    // Contacts & Invites
    @POST("contacts/discover")
    suspend fun discoverContacts(@Body req: ContactLookupRequestDto): Response<List<ContactResponseDto>>

    @POST("invitations/send")
    suspend fun sendInvitation(@Body req: SendInvitationDto): Response<InvitationResponseDto>

    // Keys
    @POST("keys/bundle")
    suspend fun publishKeys(@Body req: PublishKeysDto): Response<Unit>

    @GET("keys/bundle/{recipientUserId}")
    suspend fun getPreKeyBundle(@Path("recipientUserId") recipientUserId: UUID): Response<PreKeyBundleDto>

    // Conversations & Messages
    @GET("conversations")
    suspend fun getConversations(): Response<List<ConversationResponseDto>>

    @POST("conversations/direct")
    suspend fun getOrCreateDirectConversation(@Body req: DirectConversationRequestDto): Response<ConversationResponseDto>

    @GET("conversations/{conversationId}/messages")
    suspend fun getMessages(
        @Path("conversationId") conversationId: UUID,
        @Query("before") before: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<List<MessageResponseDto>>

    @POST("messages")
    suspend fun sendMessage(@Body req: SendMessageDto): Response<MessageResponseDto>

    @POST("messages/receipts")
    suspend fun submitReceipts(@Body req: MessageReceiptDto): Response<Unit>

    @GET("messages/offline")
    suspend fun getOfflineMessages(): Response<List<MessageResponseDto>>

    // Attachments
    @POST("attachments/upload-request")
    suspend fun requestUpload(@Body req: AttachmentUploadRequestDto): Response<AttachmentUploadResponseDto>

    @PUT("attachments/{attachmentId}/upload")
    @Headers("Content-Type: application/octet-stream")
    suspend fun uploadCiphertextBytes(
        @Path("attachmentId") attachmentId: UUID,
        @Body body: RequestBody
    ): Response<Unit>

    @GET("attachments/{attachmentId}/download")
    suspend fun getDownloadInfo(@Path("attachmentId") attachmentId: UUID): Response<AttachmentDownloadResponseDto>

    @GET("attachments/{attachmentId}/file")
    @Streaming
    suspend fun downloadCiphertextBytes(@Path("attachmentId") attachmentId: UUID): Response<ResponseBody>

    // Calls
    @POST("calls/signal")
    suspend fun sendCallSignalRest(@Body signal: CallSignalDto): Response<Unit>

    @GET("calls/history")
    suspend fun getCallHistory(@Query("limit") limit: Int = 50): Response<List<CallSessionResponseDto>>

    // Block
    @POST("users/{userId}/block")
    suspend fun blockUser(@Path("userId") userId: UUID): Response<Unit>

    @DELETE("users/{userId}/block")
    suspend fun unblockUser(@Path("userId") userId: UUID): Response<Unit>

    // Account
    @DELETE("account")
    suspend fun deleteAccount(@Query("password") pass: String): Response<Unit>
}

