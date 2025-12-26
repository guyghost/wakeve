#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class SharedAccommodation, SharedAccommodationQueries, SharedAccommodationRequest, SharedAccommodationRequestCompanion, SharedAccommodationService, SharedAccommodationType, SharedAccommodationTypeCompanion, SharedAccommodationWithRooms, SharedAccommodationWithRoomsCompanion, SharedAccommodation_, SharedAccommodation_Companion, SharedActivitiesByDate, SharedActivitiesByDateCompanion, SharedActivity, SharedActivityManager, SharedActivityParticipant, SharedActivityParticipantCompanion, SharedActivityParticipantQueries, SharedActivityQueries, SharedActivityRegistrationRequest, SharedActivityRegistrationRequestCompanion, SharedActivityRequest, SharedActivityRequestCompanion, SharedActivitySchedule, SharedActivityScheduleCompanion, SharedActivityWithStats, SharedActivityWithStatsCompanion, SharedActivity_, SharedActivity_Companion, SharedActivity_participant, SharedAddParticipantRequest, SharedAddParticipantRequestCompanion, SharedAddVoteRequest, SharedAddVoteRequestCompanion, SharedApiResponse<T>, SharedApiResponseCompanion, SharedAuthContext, SharedAuthState, SharedAuthStateAuthenticated, SharedAuthStateError, SharedAuthStateLoading, SharedAuthStateUnauthenticated, SharedAutoMealPlanRequest, SharedAutoMealPlanRequestCompanion, SharedBookingStatus, SharedBookingStatusCompanion, SharedBudget, SharedBudgetCalculator, SharedBudgetCategory, SharedBudgetCategoryCompanion, SharedBudgetCategoryDetails, SharedBudgetItem, SharedBudgetItemQueries, SharedBudgetItem_, SharedBudgetItem_Companion, SharedBudgetQueries, SharedBudgetRange, SharedBudgetRangeCompanion, SharedBudgetWithItems, SharedBudget_, SharedBudget_Companion, SharedCalendarEvent, SharedCalendarEventCompanion, SharedCalendarInvite, SharedCalendarInviteCompanion, SharedClientAuthenticationService, SharedConfirmedDate, SharedConfirmedDateQueries, SharedCountAssignedParticipants, SharedCountByStatus, SharedCountMealsByStatus, SharedCountMealsByType, SharedCountRestrictionsByType, SharedCreateEventRequest, SharedCreateEventRequestCompanion, SharedCreateScenarioRequest, SharedCreateScenarioRequestCompanion, SharedCreateTimeSlotRequest, SharedCreateTimeSlotRequestCompanion, SharedDailyMealSchedule, SharedDailyMealScheduleCompanion, SharedDatabaseEventRepository, SharedDatabaseProvider, SharedDeviceData, SharedDevice_fingerprint, SharedDietaryRestriction, SharedDietaryRestrictionCompanion, SharedDietaryRestrictionRequest, SharedDietaryRestrictionRequestCompanion, SharedEquipmentByCategory, SharedEquipmentByCategoryCompanion, SharedEquipmentCategory, SharedEquipmentCategoryCompanion, SharedEquipmentChecklist, SharedEquipmentChecklistCompanion, SharedEquipmentItem, SharedEquipmentItemCompanion, SharedEquipmentItemQueries, SharedEquipmentItemRequest, SharedEquipmentItemRequestCompanion, SharedEquipmentManager, SharedEquipment_item, SharedErrorCode, SharedErrorResponse, SharedErrorResponseCompanion, SharedEvent, SharedEventCompanion, SharedEventQueries, SharedEventResponse, SharedEventResponseCompanion, SharedEventStatus, SharedEventStatusCompanion, SharedEvent_, SharedGenerateChecklistRequest, SharedGenerateChecklistRequestCompanion, SharedGetParticipantsWithMultipleRestrictions, SharedGetRoomOccupancyStats, SharedGetTotalAccommodationCost, SharedGetTotalActualCost, SharedGetTotalConfirmedCapacity, SharedGetTotalEstimatedCost, SharedItemStatus, SharedItemStatusCompanion, SharedJwt_blacklist, SharedKotlinAbstractCoroutineContextElement, SharedKotlinAbstractCoroutineContextKey<B, E>, SharedKotlinArray<T>, SharedKotlinByteArray, SharedKotlinByteIterator, SharedKotlinCancellationException, SharedKotlinEnum<E>, SharedKotlinEnumCompanion, SharedKotlinException, SharedKotlinIllegalStateException, SharedKotlinKTypeProjection, SharedKotlinKTypeProjectionCompanion, SharedKotlinKVariance, SharedKotlinNothing, SharedKotlinPair<__covariant A, __covariant B>, SharedKotlinRuntimeException, SharedKotlinThrowable, SharedKotlinTriple<__covariant A, __covariant B, __covariant C>, SharedKotlinUnit, SharedKotlinx_coroutines_coreCoroutineDispatcher, SharedKotlinx_coroutines_coreCoroutineDispatcherKey, SharedKotlinx_io_coreBuffer, SharedKotlinx_serialization_coreSerialKind, SharedKotlinx_serialization_coreSerializersModule, SharedKtor_client_coreHttpClient, SharedKtor_client_coreHttpClientCall, SharedKtor_client_coreHttpClientCallCompanion, SharedKtor_client_coreHttpClientConfig<T>, SharedKtor_client_coreHttpClientEngineConfig, SharedKtor_client_coreHttpReceivePipeline, SharedKtor_client_coreHttpReceivePipelinePhases, SharedKtor_client_coreHttpRequestBuilder, SharedKtor_client_coreHttpRequestBuilderCompanion, SharedKtor_client_coreHttpRequestData, SharedKtor_client_coreHttpRequestPipeline, SharedKtor_client_coreHttpRequestPipelinePhases, SharedKtor_client_coreHttpResponse, SharedKtor_client_coreHttpResponseContainer, SharedKtor_client_coreHttpResponseData, SharedKtor_client_coreHttpResponsePipeline, SharedKtor_client_coreHttpResponsePipelinePhases, SharedKtor_client_coreHttpSendPipeline, SharedKtor_client_coreHttpSendPipelinePhases, SharedKtor_client_coreProxyConfig, SharedKtor_eventsEventDefinition<T>, SharedKtor_eventsEvents, SharedKtor_httpContentType, SharedKtor_httpContentTypeCompanion, SharedKtor_httpHeaderValueParam, SharedKtor_httpHeaderValueWithParameters, SharedKtor_httpHeaderValueWithParametersCompanion, SharedKtor_httpHeadersBuilder, SharedKtor_httpHttpMethod, SharedKtor_httpHttpMethodCompanion, SharedKtor_httpHttpProtocolVersion, SharedKtor_httpHttpProtocolVersionCompanion, SharedKtor_httpHttpStatusCode, SharedKtor_httpHttpStatusCodeCompanion, SharedKtor_httpOutgoingContent, SharedKtor_httpURLBuilder, SharedKtor_httpURLBuilderCompanion, SharedKtor_httpURLProtocol, SharedKtor_httpURLProtocolCompanion, SharedKtor_httpUrl, SharedKtor_httpUrlCompanion, SharedKtor_utilsAttributeKey<T>, SharedKtor_utilsGMTDate, SharedKtor_utilsGMTDateCompanion, SharedKtor_utilsMonth, SharedKtor_utilsMonthCompanion, SharedKtor_utilsPipeline<TSubject, TContext>, SharedKtor_utilsPipelinePhase, SharedKtor_utilsStringValuesBuilderImpl, SharedKtor_utilsTypeInfo, SharedKtor_utilsWeekDay, SharedKtor_utilsWeekDayCompanion, SharedLastSyncTime, SharedLocation, SharedLocationCompanion, SharedMeal, SharedMealPlanner, SharedMealPlanningSummary, SharedMealPlanningSummaryCompanion, SharedMealQueries, SharedMealRequest, SharedMealRequestCompanion, SharedMealStatus, SharedMealStatusCompanion, SharedMealType, SharedMealTypeCompanion, SharedMealWithRestrictions, SharedMealWithRestrictionsCompanion, SharedMeal_, SharedMeal_Companion, SharedNotificationMessage, SharedNotificationMessageCompanion, SharedNotificationPreferences, SharedNotificationType, SharedNotificationTypeCompanion, SharedNotification_preference, SharedOAuthLoginRequest, SharedOAuthLoginRequestCompanion, SharedOAuthLoginResponse, SharedOAuthLoginResponseCompanion, SharedOAuthProvider, SharedOptimizationType, SharedOptimizationTypeCompanion, SharedParticipant, SharedParticipantAccommodation, SharedParticipantAccommodationCompanion, SharedParticipantActivityStats, SharedParticipantActivityStatsCompanion, SharedParticipantBudgetShare, SharedParticipantDietaryRestriction, SharedParticipantDietaryRestrictionCompanion, SharedParticipantDietaryRestrictionQueries, SharedParticipantEquipmentStats, SharedParticipantEquipmentStatsCompanion, SharedParticipantQueries, SharedParticipant_dietary_restriction, SharedPermission, SharedPermissionCompanion, SharedPoll, SharedPollLogic, SharedPollLogicSlotScore, SharedPollResponse, SharedPollResponseCompanion, SharedPushToken, SharedPushTokenCompanion, SharedRBACClaims, SharedRecommendation, SharedRecommendationCompanion, SharedRecommendationType, SharedRecommendationTypeCompanion, SharedRegistrationResult, SharedRegistrationResultAlreadyRegistered, SharedRegistrationResultFull, SharedRegistrationResultSuccess, SharedRolePermissions, SharedRoomAssignment, SharedRoomAssignmentCompanion, SharedRoomAssignmentQueries, SharedRoomAssignmentRequest, SharedRoomAssignmentRequestCompanion, SharedRoom_assignment, SharedRoute, SharedRouteCompanion, SharedRuntimeAfterVersion, SharedRuntimeBaseTransacterImpl, SharedRuntimeExecutableQuery<__covariant RowType>, SharedRuntimeQuery<__covariant RowType>, SharedRuntimeTransacterImpl, SharedRuntimeTransacterTransaction, SharedScenario, SharedScenarioLogic, SharedScenarioQueries, SharedScenarioResponse, SharedScenarioResponseCompanion, SharedScenarioStatus, SharedScenarioStatusCompanion, SharedScenarioVote, SharedScenarioVoteCompanion, SharedScenarioVoteQueries, SharedScenarioVoteRequest, SharedScenarioVoteRequestCompanion, SharedScenarioVoteResponse, SharedScenarioVoteResponseCompanion, SharedScenarioVoteType, SharedScenarioVoteTypeCompanion, SharedScenarioVotingResult, SharedScenarioVotingResultResponse, SharedScenarioVotingResultResponseCompanion, SharedScenarioWithVotes, SharedScenarioWithVotesResponse, SharedScenarioWithVotesResponseCompanion, SharedScenario_, SharedScenario_Companion, SharedScenario_vote, SharedSelectActivitiesByDateGrouped, SharedSelectEquipmentOverallStats, SharedSelectEquipmentStatsByAssignee, SharedSelectEquipmentStatsByCategory, SharedSelectVotesByTimeslot, SharedSelectVotesForEventTimeslots, SharedSelectVotingResultByScenarioId, SharedSelectWithTimeslotDetails, SharedSession, SharedSessionData, SharedSessionQueries, SharedSessionRepository, SharedSyncChange, SharedSyncChangeCompanion, SharedSyncConflict, SharedSyncConflictCompanion, SharedSyncEventData, SharedSyncEventDataCompanion, SharedSyncManager, SharedSyncMetadata, SharedSyncMetadataQueries, SharedSyncMetadata_, SharedSyncOperation, SharedSyncParticipantData, SharedSyncParticipantDataCompanion, SharedSyncRequest, SharedSyncRequestCompanion, SharedSyncResponse, SharedSyncResponseCompanion, SharedSyncStats, SharedSyncStatus, SharedSyncStatusError, SharedSyncStatusIdle, SharedSyncStatusSyncing, SharedSyncVoteData, SharedSyncVoteDataCompanion, SharedSync_metadata, SharedTimeSlot, SharedTimeSlotCompanion, SharedTimeSlotQueries, SharedTimeSlotResponse, SharedTimeSlotResponseCompanion, SharedTimeSlot_, SharedTokenRefreshRequest, SharedTokenRefreshRequestCompanion, SharedTokenRefreshResponse, SharedTokenRefreshResponseCompanion, SharedTransportMode, SharedTransportModeCompanion, SharedTransportOption, SharedTransportOptionCompanion, SharedTransportPlan, SharedTransportPlanCompanion, SharedUpdateEventStatusRequest, SharedUpdateEventStatusRequestCompanion, SharedUpdateScenarioRequest, SharedUpdateScenarioRequestCompanion, SharedUser, SharedUserPreferences, SharedUserPreferencesCompanion, SharedUserPreferencesQueries, SharedUserPreferencesRepository, SharedUserQueries, SharedUserRepository, SharedUserResponse, SharedUserResponseCompanion, SharedUserRole, SharedUserRoleCompanion, SharedUserToken, SharedUser_, SharedUser_preferences, SharedUser_token, SharedValidationResult, SharedValidationResult_, SharedVote, SharedVoteQueries, SharedVote_, SharedWakevDbCompanion;

@protocol SharedCalendarService, SharedDatabaseFactory, SharedEventRepositoryInterface, SharedKotlinAnnotation, SharedKotlinAutoCloseable, SharedKotlinComparable, SharedKotlinContinuation, SharedKotlinContinuationInterceptor, SharedKotlinCoroutineContext, SharedKotlinCoroutineContextElement, SharedKotlinCoroutineContextKey, SharedKotlinFunction, SharedKotlinIterator, SharedKotlinKAnnotatedElement, SharedKotlinKClass, SharedKotlinKClassifier, SharedKotlinKDeclarationContainer, SharedKotlinKType, SharedKotlinMapEntry, SharedKotlinSequence, SharedKotlinSuspendFunction0, SharedKotlinSuspendFunction1, SharedKotlinSuspendFunction2, SharedKotlinx_coroutines_coreChildHandle, SharedKotlinx_coroutines_coreChildJob, SharedKotlinx_coroutines_coreCoroutineScope, SharedKotlinx_coroutines_coreDisposableHandle, SharedKotlinx_coroutines_coreFlow, SharedKotlinx_coroutines_coreFlowCollector, SharedKotlinx_coroutines_coreJob, SharedKotlinx_coroutines_coreParentJob, SharedKotlinx_coroutines_coreRunnable, SharedKotlinx_coroutines_coreSelectClause, SharedKotlinx_coroutines_coreSelectClause0, SharedKotlinx_coroutines_coreSelectInstance, SharedKotlinx_coroutines_coreSharedFlow, SharedKotlinx_coroutines_coreStateFlow, SharedKotlinx_io_coreRawSink, SharedKotlinx_io_coreRawSource, SharedKotlinx_io_coreSink, SharedKotlinx_io_coreSource, SharedKotlinx_serialization_coreCompositeDecoder, SharedKotlinx_serialization_coreCompositeEncoder, SharedKotlinx_serialization_coreDecoder, SharedKotlinx_serialization_coreDeserializationStrategy, SharedKotlinx_serialization_coreEncoder, SharedKotlinx_serialization_coreKSerializer, SharedKotlinx_serialization_coreSerialDescriptor, SharedKotlinx_serialization_coreSerializationStrategy, SharedKotlinx_serialization_coreSerializersModuleCollector, SharedKtor_client_coreHttpClientEngine, SharedKtor_client_coreHttpClientEngineCapability, SharedKtor_client_coreHttpClientPlugin, SharedKtor_client_coreHttpRequest, SharedKtor_httpHeaders, SharedKtor_httpHttpMessage, SharedKtor_httpHttpMessageBuilder, SharedKtor_httpParameters, SharedKtor_httpParametersBuilder, SharedKtor_ioByteReadChannel, SharedKtor_ioCloseable, SharedKtor_ioJvmSerializable, SharedKtor_utilsAttributes, SharedKtor_utilsStringValues, SharedKtor_utilsStringValuesBuilder, SharedNetworkStatusDetector, SharedNotificationService, SharedPlatform, SharedRuntimeCloseable, SharedRuntimeQueryListener, SharedRuntimeQueryResult, SharedRuntimeSqlCursor, SharedRuntimeSqlDriver, SharedRuntimeSqlPreparedStatement, SharedRuntimeSqlSchema, SharedRuntimeTransacter, SharedRuntimeTransacterBase, SharedRuntimeTransactionCallbacks, SharedRuntimeTransactionWithReturn, SharedRuntimeTransactionWithoutReturn, SharedSecureTokenStorage, SharedSuggestionEngine, SharedSyncAlertManager, SharedSyncHttpClient, SharedSyncMetrics, SharedTransportService, SharedWakevDb;

NS_ASSUME_NONNULL_BEGIN
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunknown-warning-option"
#pragma clang diagnostic ignored "-Wincompatible-property-type"
#pragma clang diagnostic ignored "-Wnullability"

#pragma push_macro("_Nullable_result")
#if !__has_feature(nullability_nullable_result)
#undef _Nullable_result
#define _Nullable_result _Nullable
#endif

__attribute__((swift_name("KotlinBase")))
@interface SharedBase : NSObject
- (instancetype)init __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
+ (void)initialize __attribute__((objc_requires_super));
@end

@interface SharedBase (SharedBaseCopying) <NSCopying>
@end

__attribute__((swift_name("KotlinMutableSet")))
@interface SharedMutableSet<ObjectType> : NSMutableSet<ObjectType>
@end

__attribute__((swift_name("KotlinMutableDictionary")))
@interface SharedMutableDictionary<KeyType, ObjectType> : NSMutableDictionary<KeyType, ObjectType>
@end

@interface NSError (NSErrorSharedKotlinException)
@property (readonly) id _Nullable kotlinException;
@end

__attribute__((swift_name("KotlinNumber")))
@interface SharedNumber : NSNumber
- (instancetype)initWithChar:(char)value __attribute__((unavailable));
- (instancetype)initWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
- (instancetype)initWithShort:(short)value __attribute__((unavailable));
- (instancetype)initWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
- (instancetype)initWithInt:(int)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
- (instancetype)initWithLong:(long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
- (instancetype)initWithLongLong:(long long)value __attribute__((unavailable));
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
- (instancetype)initWithFloat:(float)value __attribute__((unavailable));
- (instancetype)initWithDouble:(double)value __attribute__((unavailable));
- (instancetype)initWithBool:(BOOL)value __attribute__((unavailable));
- (instancetype)initWithInteger:(NSInteger)value __attribute__((unavailable));
- (instancetype)initWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
+ (instancetype)numberWithChar:(char)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
+ (instancetype)numberWithShort:(short)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
+ (instancetype)numberWithInt:(int)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
+ (instancetype)numberWithLong:(long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
+ (instancetype)numberWithLongLong:(long long)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
+ (instancetype)numberWithFloat:(float)value __attribute__((unavailable));
+ (instancetype)numberWithDouble:(double)value __attribute__((unavailable));
+ (instancetype)numberWithBool:(BOOL)value __attribute__((unavailable));
+ (instancetype)numberWithInteger:(NSInteger)value __attribute__((unavailable));
+ (instancetype)numberWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
@end

__attribute__((swift_name("KotlinByte")))
@interface SharedByte : SharedNumber
- (instancetype)initWithChar:(char)value;
+ (instancetype)numberWithChar:(char)value;
@end

__attribute__((swift_name("KotlinUByte")))
@interface SharedUByte : SharedNumber
- (instancetype)initWithUnsignedChar:(unsigned char)value;
+ (instancetype)numberWithUnsignedChar:(unsigned char)value;
@end

__attribute__((swift_name("KotlinShort")))
@interface SharedShort : SharedNumber
- (instancetype)initWithShort:(short)value;
+ (instancetype)numberWithShort:(short)value;
@end

__attribute__((swift_name("KotlinUShort")))
@interface SharedUShort : SharedNumber
- (instancetype)initWithUnsignedShort:(unsigned short)value;
+ (instancetype)numberWithUnsignedShort:(unsigned short)value;
@end

__attribute__((swift_name("KotlinInt")))
@interface SharedInt : SharedNumber
- (instancetype)initWithInt:(int)value;
+ (instancetype)numberWithInt:(int)value;
@end

__attribute__((swift_name("KotlinUInt")))
@interface SharedUInt : SharedNumber
- (instancetype)initWithUnsignedInt:(unsigned int)value;
+ (instancetype)numberWithUnsignedInt:(unsigned int)value;
@end

__attribute__((swift_name("KotlinLong")))
@interface SharedLong : SharedNumber
- (instancetype)initWithLongLong:(long long)value;
+ (instancetype)numberWithLongLong:(long long)value;
@end

__attribute__((swift_name("KotlinULong")))
@interface SharedULong : SharedNumber
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value;
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value;
@end

__attribute__((swift_name("KotlinFloat")))
@interface SharedFloat : SharedNumber
- (instancetype)initWithFloat:(float)value;
+ (instancetype)numberWithFloat:(float)value;
@end

__attribute__((swift_name("KotlinDouble")))
@interface SharedDouble : SharedNumber
- (instancetype)initWithDouble:(double)value;
+ (instancetype)numberWithDouble:(double)value;
@end

__attribute__((swift_name("KotlinBoolean")))
@interface SharedBoolean : SharedNumber
- (instancetype)initWithBool:(BOOL)value;
+ (instancetype)numberWithBool:(BOOL)value;
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Accommodation")))
@interface SharedAccommodation : SharedBase
- (instancetype)initWithId:(NSString *)id event_id:(NSString *)event_id name:(NSString *)name type:(NSString *)type address:(NSString *)address capacity:(int64_t)capacity price_per_night:(int64_t)price_per_night total_nights:(int64_t)total_nights total_cost:(int64_t)total_cost booking_status:(NSString *)booking_status booking_url:(NSString * _Nullable)booking_url check_in_date:(NSString *)check_in_date check_out_date:(NSString *)check_out_date notes:(NSString * _Nullable)notes created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("init(id:event_id:name:type:address:capacity:price_per_night:total_nights:total_cost:booking_status:booking_url:check_in_date:check_out_date:notes:created_at:updated_at:)"))) __attribute__((objc_designated_initializer));
- (SharedAccommodation *)doCopyId:(NSString *)id event_id:(NSString *)event_id name:(NSString *)name type:(NSString *)type address:(NSString *)address capacity:(int64_t)capacity price_per_night:(int64_t)price_per_night total_nights:(int64_t)total_nights total_cost:(int64_t)total_cost booking_status:(NSString *)booking_status booking_url:(NSString * _Nullable)booking_url check_in_date:(NSString *)check_in_date check_out_date:(NSString *)check_out_date notes:(NSString * _Nullable)notes created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("doCopy(id:event_id:name:type:address:capacity:price_per_night:total_nights:total_cost:booking_status:booking_url:check_in_date:check_out_date:notes:created_at:updated_at:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *address __attribute__((swift_name("address")));
@property (readonly) NSString *booking_status __attribute__((swift_name("booking_status")));
@property (readonly) NSString * _Nullable booking_url __attribute__((swift_name("booking_url")));
@property (readonly) int64_t capacity __attribute__((swift_name("capacity")));
@property (readonly) NSString *check_in_date __attribute__((swift_name("check_in_date")));
@property (readonly) NSString *check_out_date __attribute__((swift_name("check_out_date")));
@property (readonly) NSString *created_at __attribute__((swift_name("created_at")));
@property (readonly) NSString *event_id __attribute__((swift_name("event_id")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) NSString * _Nullable notes __attribute__((swift_name("notes")));
@property (readonly) int64_t price_per_night __attribute__((swift_name("price_per_night")));
@property (readonly) int64_t total_cost __attribute__((swift_name("total_cost")));
@property (readonly) int64_t total_nights __attribute__((swift_name("total_nights")));
@property (readonly) NSString *type __attribute__((swift_name("type")));
@property (readonly) NSString *updated_at __attribute__((swift_name("updated_at")));
@end

__attribute__((swift_name("RuntimeBaseTransacterImpl")))
@interface SharedRuntimeBaseTransacterImpl : SharedBase
- (instancetype)initWithDriver:(id<SharedRuntimeSqlDriver>)driver __attribute__((swift_name("init(driver:)"))) __attribute__((objc_designated_initializer));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (NSString *)createArgumentsCount:(int32_t)count __attribute__((swift_name("createArguments(count:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)notifyQueriesIdentifier:(int32_t)identifier tableProvider:(void (^)(SharedKotlinUnit *(^)(NSString *)))tableProvider __attribute__((swift_name("notifyQueries(identifier:tableProvider:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (id _Nullable)postTransactionCleanupTransaction:(SharedRuntimeTransacterTransaction *)transaction enclosing:(SharedRuntimeTransacterTransaction * _Nullable)enclosing thrownException:(SharedKotlinThrowable * _Nullable)thrownException returnValue:(id _Nullable)returnValue __attribute__((swift_name("postTransactionCleanup(transaction:enclosing:thrownException:returnValue:)")));

/**
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property (readonly) id<SharedRuntimeSqlDriver> driver __attribute__((swift_name("driver")));
@end

__attribute__((swift_name("RuntimeTransacterBase")))
@protocol SharedRuntimeTransacterBase
@required
@end

__attribute__((swift_name("RuntimeTransacter")))
@protocol SharedRuntimeTransacter <SharedRuntimeTransacterBase>
@required
- (void)transactionNoEnclosing:(BOOL)noEnclosing body:(void (^)(id<SharedRuntimeTransactionWithoutReturn>))body __attribute__((swift_name("transaction(noEnclosing:body:)")));
- (id _Nullable)transactionWithResultNoEnclosing:(BOOL)noEnclosing bodyWithReturn:(id _Nullable (^)(id<SharedRuntimeTransactionWithReturn>))bodyWithReturn __attribute__((swift_name("transactionWithResult(noEnclosing:bodyWithReturn:)")));
@end

__attribute__((swift_name("RuntimeTransacterImpl")))
@interface SharedRuntimeTransacterImpl : SharedRuntimeBaseTransacterImpl <SharedRuntimeTransacter>
- (instancetype)initWithDriver:(id<SharedRuntimeSqlDriver>)driver __attribute__((swift_name("init(driver:)"))) __attribute__((objc_designated_initializer));
- (void)transactionNoEnclosing:(BOOL)noEnclosing body:(void (^)(id<SharedRuntimeTransactionWithoutReturn>))body __attribute__((swift_name("transaction(noEnclosing:body:)")));
- (id _Nullable)transactionWithResultNoEnclosing:(BOOL)noEnclosing bodyWithReturn:(id _Nullable (^)(id<SharedRuntimeTransactionWithReturn>))bodyWithReturn __attribute__((swift_name("transactionWithResult(noEnclosing:bodyWithReturn:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AccommodationQueries")))
@interface SharedAccommodationQueries : SharedRuntimeTransacterImpl
- (instancetype)initWithDriver:(id<SharedRuntimeSqlDriver>)driver __attribute__((swift_name("init(driver:)"))) __attribute__((objc_designated_initializer));
- (SharedRuntimeQuery<SharedCountByStatus *> *)countByStatusEvent_id:(NSString *)event_id __attribute__((swift_name("countByStatus(event_id:)")));
- (SharedRuntimeQuery<id> *)countByStatusEvent_id:(NSString *)event_id mapper:(id (^)(NSString *, SharedLong *))mapper __attribute__((swift_name("countByStatus(event_id:mapper:)")));
- (void)deleteAccommodationId:(NSString *)id __attribute__((swift_name("deleteAccommodation(id:)")));
- (void)deleteAccommodationsByEventIdEvent_id:(NSString *)event_id __attribute__((swift_name("deleteAccommodationsByEventId(event_id:)")));
- (SharedRuntimeQuery<SharedAccommodation *> *)getAccommodationByIdId:(NSString *)id __attribute__((swift_name("getAccommodationById(id:)")));
- (SharedRuntimeQuery<id> *)getAccommodationByIdId:(NSString *)id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, SharedLong *, SharedLong *, SharedLong *, SharedLong *, NSString *, NSString * _Nullable, NSString *, NSString *, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("getAccommodationById(id:mapper:)")));
- (SharedRuntimeQuery<SharedAccommodation *> *)getAccommodationsByEventIdEvent_id:(NSString *)event_id __attribute__((swift_name("getAccommodationsByEventId(event_id:)")));
- (SharedRuntimeQuery<id> *)getAccommodationsByEventIdEvent_id:(NSString *)event_id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, SharedLong *, SharedLong *, SharedLong *, SharedLong *, NSString *, NSString * _Nullable, NSString *, NSString *, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("getAccommodationsByEventId(event_id:mapper:)")));
- (SharedRuntimeQuery<SharedAccommodation *> *)getAccommodationsByStatusEvent_id:(NSString *)event_id booking_status:(NSString *)booking_status __attribute__((swift_name("getAccommodationsByStatus(event_id:booking_status:)")));
- (SharedRuntimeQuery<id> *)getAccommodationsByStatusEvent_id:(NSString *)event_id booking_status:(NSString *)booking_status mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, SharedLong *, SharedLong *, SharedLong *, SharedLong *, NSString *, NSString * _Nullable, NSString *, NSString *, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("getAccommodationsByStatus(event_id:booking_status:mapper:)")));
- (SharedRuntimeQuery<SharedAccommodation *> *)getConfirmedAccommodationsEvent_id:(NSString *)event_id __attribute__((swift_name("getConfirmedAccommodations(event_id:)")));
- (SharedRuntimeQuery<id> *)getConfirmedAccommodationsEvent_id:(NSString *)event_id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, SharedLong *, SharedLong *, SharedLong *, SharedLong *, NSString *, NSString * _Nullable, NSString *, NSString *, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("getConfirmedAccommodations(event_id:mapper:)")));
- (SharedRuntimeQuery<SharedGetTotalAccommodationCost *> *)getTotalAccommodationCostEvent_id:(NSString *)event_id __attribute__((swift_name("getTotalAccommodationCost(event_id:)")));
- (SharedRuntimeQuery<id> *)getTotalAccommodationCostEvent_id:(NSString *)event_id mapper:(id (^)(SharedLong * _Nullable))mapper __attribute__((swift_name("getTotalAccommodationCost(event_id:mapper:)")));
- (SharedRuntimeQuery<SharedGetTotalConfirmedCapacity *> *)getTotalConfirmedCapacityEvent_id:(NSString *)event_id __attribute__((swift_name("getTotalConfirmedCapacity(event_id:)")));
- (SharedRuntimeQuery<id> *)getTotalConfirmedCapacityEvent_id:(NSString *)event_id mapper:(id (^)(SharedLong * _Nullable))mapper __attribute__((swift_name("getTotalConfirmedCapacity(event_id:mapper:)")));
- (void)insertAccommodationId:(NSString *)id event_id:(NSString *)event_id name:(NSString *)name type:(NSString *)type address:(NSString *)address capacity:(int64_t)capacity price_per_night:(int64_t)price_per_night total_nights:(int64_t)total_nights total_cost:(int64_t)total_cost booking_status:(NSString *)booking_status booking_url:(NSString * _Nullable)booking_url check_in_date:(NSString *)check_in_date check_out_date:(NSString *)check_out_date notes:(NSString * _Nullable)notes created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("insertAccommodation(id:event_id:name:type:address:capacity:price_per_night:total_nights:total_cost:booking_status:booking_url:check_in_date:check_out_date:notes:created_at:updated_at:)")));
- (void)updateAccommodationName:(NSString *)name type:(NSString *)type address:(NSString *)address capacity:(int64_t)capacity price_per_night:(int64_t)price_per_night total_nights:(int64_t)total_nights total_cost:(int64_t)total_cost booking_status:(NSString *)booking_status booking_url:(NSString * _Nullable)booking_url check_in_date:(NSString *)check_in_date check_out_date:(NSString *)check_out_date notes:(NSString * _Nullable)notes updated_at:(NSString *)updated_at id:(NSString *)id __attribute__((swift_name("updateAccommodation(name:type:address:capacity:price_per_night:total_nights:total_cost:booking_status:booking_url:check_in_date:check_out_date:notes:updated_at:id:)")));
- (void)updateBookingStatusBooking_status:(NSString *)booking_status updated_at:(NSString *)updated_at id:(NSString *)id __attribute__((swift_name("updateBookingStatus(booking_status:updated_at:id:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Activity")))
@interface SharedActivity : SharedBase
- (instancetype)initWithId:(NSString *)id event_id:(NSString *)event_id scenario_id:(NSString * _Nullable)scenario_id name:(NSString *)name description:(NSString *)description date:(NSString * _Nullable)date time:(NSString * _Nullable)time duration:(int64_t)duration location:(NSString * _Nullable)location cost:(SharedLong * _Nullable)cost max_participants:(SharedLong * _Nullable)max_participants organizer_id:(NSString *)organizer_id notes:(NSString * _Nullable)notes created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("init(id:event_id:scenario_id:name:description:date:time:duration:location:cost:max_participants:organizer_id:notes:created_at:updated_at:)"))) __attribute__((objc_designated_initializer));
- (SharedActivity *)doCopyId:(NSString *)id event_id:(NSString *)event_id scenario_id:(NSString * _Nullable)scenario_id name:(NSString *)name description:(NSString *)description date:(NSString * _Nullable)date time:(NSString * _Nullable)time duration:(int64_t)duration location:(NSString * _Nullable)location cost:(SharedLong * _Nullable)cost max_participants:(SharedLong * _Nullable)max_participants organizer_id:(NSString *)organizer_id notes:(NSString * _Nullable)notes created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("doCopy(id:event_id:scenario_id:name:description:date:time:duration:location:cost:max_participants:organizer_id:notes:created_at:updated_at:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedLong * _Nullable cost __attribute__((swift_name("cost")));
@property (readonly) NSString *created_at __attribute__((swift_name("created_at")));
@property (readonly) NSString * _Nullable date __attribute__((swift_name("date")));
@property (readonly) NSString *description_ __attribute__((swift_name("description_")));
@property (readonly) int64_t duration __attribute__((swift_name("duration")));
@property (readonly) NSString *event_id __attribute__((swift_name("event_id")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString * _Nullable location __attribute__((swift_name("location")));
@property (readonly) SharedLong * _Nullable max_participants __attribute__((swift_name("max_participants")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) NSString * _Nullable notes __attribute__((swift_name("notes")));
@property (readonly) NSString *organizer_id __attribute__((swift_name("organizer_id")));
@property (readonly) NSString * _Nullable scenario_id __attribute__((swift_name("scenario_id")));
@property (readonly) NSString * _Nullable time __attribute__((swift_name("time")));
@property (readonly) NSString *updated_at __attribute__((swift_name("updated_at")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ActivityParticipantQueries")))
@interface SharedActivityParticipantQueries : SharedRuntimeTransacterImpl
- (instancetype)initWithDriver:(id<SharedRuntimeSqlDriver>)driver __attribute__((swift_name("init(driver:)"))) __attribute__((objc_designated_initializer));
- (SharedRuntimeQuery<SharedLong *> *)countActivitiesByParticipantParticipant_id:(NSString *)participant_id __attribute__((swift_name("countActivitiesByParticipant(participant_id:)")));
- (SharedRuntimeQuery<SharedLong *> *)countParticipantsByActivityActivity_id:(NSString *)activity_id __attribute__((swift_name("countParticipantsByActivity(activity_id:)")));
- (void)deleteActivityParticipantId:(NSString *)id __attribute__((swift_name("deleteActivityParticipant(id:)")));
- (void)deleteActivityParticipantByActivityAndParticipantActivity_id:(NSString *)activity_id participant_id:(NSString *)participant_id __attribute__((swift_name("deleteActivityParticipantByActivityAndParticipant(activity_id:participant_id:)")));
- (void)deleteParticipantsByActivityActivity_id:(NSString *)activity_id __attribute__((swift_name("deleteParticipantsByActivity(activity_id:)")));
- (void)insertActivityParticipantId:(NSString *)id activity_id:(NSString *)activity_id participant_id:(NSString *)participant_id registered_at:(NSString *)registered_at notes:(NSString * _Nullable)notes __attribute__((swift_name("insertActivityParticipant(id:activity_id:participant_id:registered_at:notes:)")));
- (SharedRuntimeQuery<SharedBoolean *> *)isParticipantRegisteredActivity_id:(NSString *)activity_id participant_id:(NSString *)participant_id __attribute__((swift_name("isParticipantRegistered(activity_id:participant_id:)")));
- (SharedRuntimeQuery<SharedActivity_participant *> *)selectActivitiesByParticipantParticipant_id:(NSString *)participant_id __attribute__((swift_name("selectActivitiesByParticipant(participant_id:)")));
- (SharedRuntimeQuery<id> *)selectActivitiesByParticipantParticipant_id:(NSString *)participant_id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString * _Nullable))mapper __attribute__((swift_name("selectActivitiesByParticipant(participant_id:mapper:)")));
- (SharedRuntimeQuery<NSString *> *)selectActivityIdsByParticipantParticipant_id:(NSString *)participant_id __attribute__((swift_name("selectActivityIdsByParticipant(participant_id:)")));
- (SharedRuntimeQuery<SharedActivity_participant *> *)selectActivityParticipantByIdId:(NSString *)id __attribute__((swift_name("selectActivityParticipantById(id:)")));
- (SharedRuntimeQuery<id> *)selectActivityParticipantByIdId:(NSString *)id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString * _Nullable))mapper __attribute__((swift_name("selectActivityParticipantById(id:mapper:)")));
- (SharedRuntimeQuery<NSString *> *)selectParticipantIdsByActivityActivity_id:(NSString *)activity_id __attribute__((swift_name("selectParticipantIdsByActivity(activity_id:)")));
- (SharedRuntimeQuery<SharedActivity_participant *> *)selectParticipantsByActivityActivity_id:(NSString *)activity_id __attribute__((swift_name("selectParticipantsByActivity(activity_id:)")));
- (SharedRuntimeQuery<id> *)selectParticipantsByActivityActivity_id:(NSString *)activity_id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString * _Nullable))mapper __attribute__((swift_name("selectParticipantsByActivity(activity_id:mapper:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ActivityQueries")))
@interface SharedActivityQueries : SharedRuntimeTransacterImpl
- (instancetype)initWithDriver:(id<SharedRuntimeSqlDriver>)driver __attribute__((swift_name("init(driver:)"))) __attribute__((objc_designated_initializer));
- (SharedRuntimeQuery<SharedBoolean *> *)activityExistsId:(NSString *)id __attribute__((swift_name("activityExists(id:)")));
- (SharedRuntimeQuery<SharedLong *> *)countActivitiesByEventEvent_id:(NSString *)event_id __attribute__((swift_name("countActivitiesByEvent(event_id:)")));
- (SharedRuntimeQuery<SharedLong *> *)countActivitiesByEventAndDateEvent_id:(NSString *)event_id date:(NSString * _Nullable)date __attribute__((swift_name("countActivitiesByEventAndDate(event_id:date:)")));
- (void)deleteActivitiesByEventEvent_id:(NSString *)event_id __attribute__((swift_name("deleteActivitiesByEvent(event_id:)")));
- (void)deleteActivitiesByScenarioScenario_id:(NSString * _Nullable)scenario_id __attribute__((swift_name("deleteActivitiesByScenario(scenario_id:)")));
- (void)deleteActivityId:(NSString *)id __attribute__((swift_name("deleteActivity(id:)")));
- (void)insertActivityId:(NSString *)id event_id:(NSString *)event_id scenario_id:(NSString * _Nullable)scenario_id name:(NSString *)name description:(NSString *)description date:(NSString * _Nullable)date time:(NSString * _Nullable)time duration:(int64_t)duration location:(NSString * _Nullable)location cost:(SharedLong * _Nullable)cost max_participants:(SharedLong * _Nullable)max_participants organizer_id:(NSString *)organizer_id notes:(NSString * _Nullable)notes created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("insertActivity(id:event_id:scenario_id:name:description:date:time:duration:location:cost:max_participants:organizer_id:notes:created_at:updated_at:)")));
- (SharedRuntimeQuery<SharedSelectActivitiesByDateGrouped *> *)selectActivitiesByDateGroupedEvent_id:(NSString *)event_id __attribute__((swift_name("selectActivitiesByDateGrouped(event_id:)")));
- (SharedRuntimeQuery<id> *)selectActivitiesByDateGroupedEvent_id:(NSString *)event_id mapper:(id (^)(NSString *, SharedLong *, SharedDouble *))mapper __attribute__((swift_name("selectActivitiesByDateGrouped(event_id:mapper:)")));
- (SharedRuntimeQuery<SharedActivity *> *)selectActivitiesByEventEvent_id:(NSString *)event_id __attribute__((swift_name("selectActivitiesByEvent(event_id:)")));
- (SharedRuntimeQuery<id> *)selectActivitiesByEventEvent_id:(NSString *)event_id mapper:(id (^)(NSString *, NSString *, NSString * _Nullable, NSString *, NSString *, NSString * _Nullable, NSString * _Nullable, SharedLong *, NSString * _Nullable, SharedLong * _Nullable, SharedLong * _Nullable, NSString *, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("selectActivitiesByEvent(event_id:mapper:)")));
- (SharedRuntimeQuery<SharedActivity *> *)selectActivitiesByEventAndDateEvent_id:(NSString *)event_id date:(NSString * _Nullable)date __attribute__((swift_name("selectActivitiesByEventAndDate(event_id:date:)")));
- (SharedRuntimeQuery<id> *)selectActivitiesByEventAndDateEvent_id:(NSString *)event_id date:(NSString * _Nullable)date mapper:(id (^)(NSString *, NSString *, NSString * _Nullable, NSString *, NSString *, NSString * _Nullable, NSString * _Nullable, SharedLong *, NSString * _Nullable, SharedLong * _Nullable, SharedLong * _Nullable, NSString *, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("selectActivitiesByEventAndDate(event_id:date:mapper:)")));
- (SharedRuntimeQuery<SharedActivity *> *)selectActivitiesByOrganizerEvent_id:(NSString *)event_id organizer_id:(NSString *)organizer_id __attribute__((swift_name("selectActivitiesByOrganizer(event_id:organizer_id:)")));
- (SharedRuntimeQuery<id> *)selectActivitiesByOrganizerEvent_id:(NSString *)event_id organizer_id:(NSString *)organizer_id mapper:(id (^)(NSString *, NSString *, NSString * _Nullable, NSString *, NSString *, NSString * _Nullable, NSString * _Nullable, SharedLong *, NSString * _Nullable, SharedLong * _Nullable, SharedLong * _Nullable, NSString *, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("selectActivitiesByOrganizer(event_id:organizer_id:mapper:)")));
- (SharedRuntimeQuery<SharedActivity *> *)selectActivitiesByScenarioEvent_id:(NSString *)event_id scenario_id:(NSString * _Nullable)scenario_id __attribute__((swift_name("selectActivitiesByScenario(event_id:scenario_id:)")));
- (SharedRuntimeQuery<id> *)selectActivitiesByScenarioEvent_id:(NSString *)event_id scenario_id:(NSString * _Nullable)scenario_id mapper:(id (^)(NSString *, NSString *, NSString * _Nullable, NSString *, NSString *, NSString * _Nullable, NSString * _Nullable, SharedLong *, NSString * _Nullable, SharedLong * _Nullable, SharedLong * _Nullable, NSString *, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("selectActivitiesByScenario(event_id:scenario_id:mapper:)")));
- (SharedRuntimeQuery<SharedActivity *> *)selectActivitiesWithoutDateEvent_id:(NSString *)event_id __attribute__((swift_name("selectActivitiesWithoutDate(event_id:)")));
- (SharedRuntimeQuery<id> *)selectActivitiesWithoutDateEvent_id:(NSString *)event_id mapper:(id (^)(NSString *, NSString *, NSString * _Nullable, NSString *, NSString *, NSString * _Nullable, NSString * _Nullable, SharedLong *, NSString * _Nullable, SharedLong * _Nullable, SharedLong * _Nullable, NSString *, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("selectActivitiesWithoutDate(event_id:mapper:)")));
- (SharedRuntimeQuery<SharedActivity *> *)selectActivityByIdId:(NSString *)id __attribute__((swift_name("selectActivityById(id:)")));
- (SharedRuntimeQuery<id> *)selectActivityByIdId:(NSString *)id mapper:(id (^)(NSString *, NSString *, NSString * _Nullable, NSString *, NSString *, NSString * _Nullable, NSString * _Nullable, SharedLong *, NSString * _Nullable, SharedLong * _Nullable, SharedLong * _Nullable, NSString *, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("selectActivityById(id:mapper:)")));
- (SharedRuntimeQuery<SharedDouble *> *)sumActivityCostByDateEvent_id:(NSString *)event_id date:(NSString * _Nullable)date __attribute__((swift_name("sumActivityCostByDate(event_id:date:)")));
- (SharedRuntimeQuery<SharedDouble *> *)sumActivityCostByEventEvent_id:(NSString *)event_id __attribute__((swift_name("sumActivityCostByEvent(event_id:)")));
- (void)updateActivityScenario_id:(NSString * _Nullable)scenario_id name:(NSString *)name description:(NSString *)description date:(NSString * _Nullable)date time:(NSString * _Nullable)time duration:(int64_t)duration location:(NSString * _Nullable)location cost:(SharedLong * _Nullable)cost max_participants:(SharedLong * _Nullable)max_participants organizer_id:(NSString *)organizer_id notes:(NSString * _Nullable)notes updated_at:(NSString *)updated_at id:(NSString *)id __attribute__((swift_name("updateActivity(scenario_id:name:description:date:time:duration:location:cost:max_participants:organizer_id:notes:updated_at:id:)")));
- (void)updateActivityCapacityMax_participants:(SharedLong * _Nullable)max_participants updated_at:(NSString *)updated_at id:(NSString *)id __attribute__((swift_name("updateActivityCapacity(max_participants:updated_at:id:)")));
- (void)updateActivityDateDate:(NSString * _Nullable)date time:(NSString * _Nullable)time updated_at:(NSString *)updated_at id:(NSString *)id __attribute__((swift_name("updateActivityDate(date:time:updated_at:id:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Activity_participant")))
@interface SharedActivity_participant : SharedBase
- (instancetype)initWithId:(NSString *)id activity_id:(NSString *)activity_id participant_id:(NSString *)participant_id registered_at:(NSString *)registered_at notes:(NSString * _Nullable)notes __attribute__((swift_name("init(id:activity_id:participant_id:registered_at:notes:)"))) __attribute__((objc_designated_initializer));
- (SharedActivity_participant *)doCopyId:(NSString *)id activity_id:(NSString *)activity_id participant_id:(NSString *)participant_id registered_at:(NSString *)registered_at notes:(NSString * _Nullable)notes __attribute__((swift_name("doCopy(id:activity_id:participant_id:registered_at:notes:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *activity_id __attribute__((swift_name("activity_id")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString * _Nullable notes __attribute__((swift_name("notes")));
@property (readonly) NSString *participant_id __attribute__((swift_name("participant_id")));
@property (readonly) NSString *registered_at __attribute__((swift_name("registered_at")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Budget")))
@interface SharedBudget : SharedBase
- (instancetype)initWithId:(NSString *)id eventId:(NSString *)eventId totalEstimated:(double)totalEstimated totalActual:(double)totalActual transportEstimated:(double)transportEstimated transportActual:(double)transportActual accommodationEstimated:(double)accommodationEstimated accommodationActual:(double)accommodationActual mealsEstimated:(double)mealsEstimated mealsActual:(double)mealsActual activitiesEstimated:(double)activitiesEstimated activitiesActual:(double)activitiesActual equipmentEstimated:(double)equipmentEstimated equipmentActual:(double)equipmentActual otherEstimated:(double)otherEstimated otherActual:(double)otherActual createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("init(id:eventId:totalEstimated:totalActual:transportEstimated:transportActual:accommodationEstimated:accommodationActual:mealsEstimated:mealsActual:activitiesEstimated:activitiesActual:equipmentEstimated:equipmentActual:otherEstimated:otherActual:createdAt:updatedAt:)"))) __attribute__((objc_designated_initializer));
- (SharedBudget *)doCopyId:(NSString *)id eventId:(NSString *)eventId totalEstimated:(double)totalEstimated totalActual:(double)totalActual transportEstimated:(double)transportEstimated transportActual:(double)transportActual accommodationEstimated:(double)accommodationEstimated accommodationActual:(double)accommodationActual mealsEstimated:(double)mealsEstimated mealsActual:(double)mealsActual activitiesEstimated:(double)activitiesEstimated activitiesActual:(double)activitiesActual equipmentEstimated:(double)equipmentEstimated equipmentActual:(double)equipmentActual otherEstimated:(double)otherEstimated otherActual:(double)otherActual createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("doCopy(id:eventId:totalEstimated:totalActual:transportEstimated:transportActual:accommodationEstimated:accommodationActual:mealsEstimated:mealsActual:activitiesEstimated:activitiesActual:equipmentEstimated:equipmentActual:otherEstimated:otherActual:createdAt:updatedAt:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) double accommodationActual __attribute__((swift_name("accommodationActual")));
@property (readonly) double accommodationEstimated __attribute__((swift_name("accommodationEstimated")));
@property (readonly) double activitiesActual __attribute__((swift_name("activitiesActual")));
@property (readonly) double activitiesEstimated __attribute__((swift_name("activitiesEstimated")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) double equipmentActual __attribute__((swift_name("equipmentActual")));
@property (readonly) double equipmentEstimated __attribute__((swift_name("equipmentEstimated")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) double mealsActual __attribute__((swift_name("mealsActual")));
@property (readonly) double mealsEstimated __attribute__((swift_name("mealsEstimated")));
@property (readonly) double otherActual __attribute__((swift_name("otherActual")));
@property (readonly) double otherEstimated __attribute__((swift_name("otherEstimated")));
@property (readonly) double totalActual __attribute__((swift_name("totalActual")));
@property (readonly) double totalEstimated __attribute__((swift_name("totalEstimated")));
@property (readonly) double transportActual __attribute__((swift_name("transportActual")));
@property (readonly) double transportEstimated __attribute__((swift_name("transportEstimated")));
@property (readonly) NSString *updatedAt __attribute__((swift_name("updatedAt")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BudgetItem")))
@interface SharedBudgetItem : SharedBase
- (instancetype)initWithId:(NSString *)id budgetId:(NSString *)budgetId category:(NSString *)category name:(NSString *)name description:(NSString *)description estimatedCost:(double)estimatedCost actualCost:(double)actualCost isPaid:(int64_t)isPaid paidBy:(NSString * _Nullable)paidBy sharedBy:(NSString *)sharedBy notes:(NSString *)notes createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("init(id:budgetId:category:name:description:estimatedCost:actualCost:isPaid:paidBy:sharedBy:notes:createdAt:updatedAt:)"))) __attribute__((objc_designated_initializer));
- (SharedBudgetItem *)doCopyId:(NSString *)id budgetId:(NSString *)budgetId category:(NSString *)category name:(NSString *)name description:(NSString *)description estimatedCost:(double)estimatedCost actualCost:(double)actualCost isPaid:(int64_t)isPaid paidBy:(NSString * _Nullable)paidBy sharedBy:(NSString *)sharedBy notes:(NSString *)notes createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("doCopy(id:budgetId:category:name:description:estimatedCost:actualCost:isPaid:paidBy:sharedBy:notes:createdAt:updatedAt:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) double actualCost __attribute__((swift_name("actualCost")));
@property (readonly) NSString *budgetId __attribute__((swift_name("budgetId")));
@property (readonly) NSString *category __attribute__((swift_name("category")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *description_ __attribute__((swift_name("description_")));
@property (readonly) double estimatedCost __attribute__((swift_name("estimatedCost")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) int64_t isPaid __attribute__((swift_name("isPaid")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) NSString *notes __attribute__((swift_name("notes")));
@property (readonly) NSString * _Nullable paidBy __attribute__((swift_name("paidBy")));
@property (readonly) NSString *sharedBy __attribute__((swift_name("sharedBy")));
@property (readonly) NSString *updatedAt __attribute__((swift_name("updatedAt")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BudgetItemQueries")))
@interface SharedBudgetItemQueries : SharedRuntimeTransacterImpl
- (instancetype)initWithDriver:(id<SharedRuntimeSqlDriver>)driver __attribute__((swift_name("init(driver:)"))) __attribute__((objc_designated_initializer));
- (SharedRuntimeQuery<SharedLong *> *)countByBudgetIdBudgetId:(NSString *)budgetId __attribute__((swift_name("countByBudgetId(budgetId:)")));
- (SharedRuntimeQuery<SharedLong *> *)countByCategoryBudgetId:(NSString *)budgetId category:(NSString *)category __attribute__((swift_name("countByCategory(budgetId:category:)")));
- (SharedRuntimeQuery<SharedLong *> *)countPaidItemsBudgetId:(NSString *)budgetId __attribute__((swift_name("countPaidItems(budgetId:)")));
- (void)deleteBudgetItemId:(NSString *)id __attribute__((swift_name("deleteBudgetItem(id:)")));
- (void)deleteByBudgetIdBudgetId:(NSString *)budgetId __attribute__((swift_name("deleteByBudgetId(budgetId:)")));
- (void)insertBudgetItemId:(NSString *)id budgetId:(NSString *)budgetId category:(NSString *)category name:(NSString *)name description:(NSString *)description estimatedCost:(double)estimatedCost actualCost:(double)actualCost isPaid:(int64_t)isPaid paidBy:(NSString * _Nullable)paidBy sharedBy:(NSString *)sharedBy notes:(NSString *)notes createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("insertBudgetItem(id:budgetId:category:name:description:estimatedCost:actualCost:isPaid:paidBy:sharedBy:notes:createdAt:updatedAt:)")));
- (void)markAsPaidActualCost:(double)actualCost paidBy:(NSString * _Nullable)paidBy updatedAt:(NSString *)updatedAt id:(NSString *)id __attribute__((swift_name("markAsPaid(actualCost:paidBy:updatedAt:id:)")));
- (SharedRuntimeQuery<SharedBudgetItem *> *)selectAll __attribute__((swift_name("selectAll()")));
- (SharedRuntimeQuery<id> *)selectAllMapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, SharedDouble *, SharedDouble *, SharedLong *, NSString * _Nullable, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectAll(mapper:)")));
- (SharedRuntimeQuery<SharedBudgetItem *> *)selectByBudgetIdBudgetId:(NSString *)budgetId __attribute__((swift_name("selectByBudgetId(budgetId:)")));
- (SharedRuntimeQuery<id> *)selectByBudgetIdBudgetId:(NSString *)budgetId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, SharedDouble *, SharedDouble *, SharedLong *, NSString * _Nullable, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectByBudgetId(budgetId:mapper:)")));
- (SharedRuntimeQuery<SharedBudgetItem *> *)selectByBudgetIdAndCategoryBudgetId:(NSString *)budgetId category:(NSString *)category __attribute__((swift_name("selectByBudgetIdAndCategory(budgetId:category:)")));
- (SharedRuntimeQuery<id> *)selectByBudgetIdAndCategoryBudgetId:(NSString *)budgetId category:(NSString *)category mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, SharedDouble *, SharedDouble *, SharedLong *, NSString * _Nullable, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectByBudgetIdAndCategory(budgetId:category:mapper:)")));
- (SharedRuntimeQuery<SharedBudgetItem *> *)selectByIdId:(NSString *)id __attribute__((swift_name("selectById(id:)")));
- (SharedRuntimeQuery<id> *)selectByIdId:(NSString *)id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, SharedDouble *, SharedDouble *, SharedLong *, NSString * _Nullable, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectById(id:mapper:)")));
- (SharedRuntimeQuery<SharedBudgetItem *> *)selectItemsPaidByBudgetId:(NSString *)budgetId paidBy:(NSString * _Nullable)paidBy __attribute__((swift_name("selectItemsPaidBy(budgetId:paidBy:)")));
- (SharedRuntimeQuery<id> *)selectItemsPaidByBudgetId:(NSString *)budgetId paidBy:(NSString * _Nullable)paidBy mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, SharedDouble *, SharedDouble *, SharedLong *, NSString * _Nullable, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectItemsPaidBy(budgetId:paidBy:mapper:)")));
- (SharedRuntimeQuery<SharedBudgetItem *> *)selectItemsSharedByParticipantBudgetId:(NSString *)budgetId value_:(NSString *)value_ __attribute__((swift_name("selectItemsSharedByParticipant(budgetId:value_:)")));
- (SharedRuntimeQuery<id> *)selectItemsSharedByParticipantBudgetId:(NSString *)budgetId value:(NSString *)value mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, SharedDouble *, SharedDouble *, SharedLong *, NSString * _Nullable, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectItemsSharedByParticipant(budgetId:value:mapper:)")));
- (SharedRuntimeQuery<SharedBudgetItem *> *)selectPaidItemsBudgetId:(NSString *)budgetId __attribute__((swift_name("selectPaidItems(budgetId:)")));
- (SharedRuntimeQuery<id> *)selectPaidItemsBudgetId:(NSString *)budgetId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, SharedDouble *, SharedDouble *, SharedLong *, NSString * _Nullable, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectPaidItems(budgetId:mapper:)")));
- (SharedRuntimeQuery<SharedBudgetItem *> *)selectUnpaidItemsBudgetId:(NSString *)budgetId __attribute__((swift_name("selectUnpaidItems(budgetId:)")));
- (SharedRuntimeQuery<id> *)selectUnpaidItemsBudgetId:(NSString *)budgetId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, SharedDouble *, SharedDouble *, SharedLong *, NSString * _Nullable, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectUnpaidItems(budgetId:mapper:)")));
- (SharedRuntimeQuery<SharedDouble *> *)sumActualByCategoryBudgetId:(NSString *)budgetId category:(NSString *)category __attribute__((swift_name("sumActualByCategory(budgetId:category:)")));
- (SharedRuntimeQuery<SharedDouble *> *)sumEstimatedByCategoryBudgetId:(NSString *)budgetId category:(NSString *)category __attribute__((swift_name("sumEstimatedByCategory(budgetId:category:)")));
- (SharedRuntimeQuery<SharedDouble *> *)sumTotalActualBudgetId:(NSString *)budgetId __attribute__((swift_name("sumTotalActual(budgetId:)")));
- (SharedRuntimeQuery<SharedDouble *> *)sumTotalEstimatedBudgetId:(NSString *)budgetId __attribute__((swift_name("sumTotalEstimated(budgetId:)")));
- (void)updateBudgetItemCategory:(NSString *)category name:(NSString *)name description:(NSString *)description estimatedCost:(double)estimatedCost actualCost:(double)actualCost isPaid:(int64_t)isPaid paidBy:(NSString * _Nullable)paidBy sharedBy:(NSString *)sharedBy notes:(NSString *)notes updatedAt:(NSString *)updatedAt id:(NSString *)id __attribute__((swift_name("updateBudgetItem(category:name:description:estimatedCost:actualCost:isPaid:paidBy:sharedBy:notes:updatedAt:id:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BudgetQueries")))
@interface SharedBudgetQueries : SharedRuntimeTransacterImpl
- (instancetype)initWithDriver:(id<SharedRuntimeSqlDriver>)driver __attribute__((swift_name("init(driver:)"))) __attribute__((objc_designated_initializer));
- (void)deleteBudgetId:(NSString *)id __attribute__((swift_name("deleteBudget(id:)")));
- (void)deleteByEventIdEventId:(NSString *)eventId __attribute__((swift_name("deleteByEventId(eventId:)")));
- (void)insertBudgetId:(NSString *)id eventId:(NSString *)eventId totalEstimated:(double)totalEstimated totalActual:(double)totalActual transportEstimated:(double)transportEstimated transportActual:(double)transportActual accommodationEstimated:(double)accommodationEstimated accommodationActual:(double)accommodationActual mealsEstimated:(double)mealsEstimated mealsActual:(double)mealsActual activitiesEstimated:(double)activitiesEstimated activitiesActual:(double)activitiesActual equipmentEstimated:(double)equipmentEstimated equipmentActual:(double)equipmentActual otherEstimated:(double)otherEstimated otherActual:(double)otherActual createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("insertBudget(id:eventId:totalEstimated:totalActual:transportEstimated:transportActual:accommodationEstimated:accommodationActual:mealsEstimated:mealsActual:activitiesEstimated:activitiesActual:equipmentEstimated:equipmentActual:otherEstimated:otherActual:createdAt:updatedAt:)")));
- (SharedRuntimeQuery<SharedBudget *> *)selectAll __attribute__((swift_name("selectAll()")));
- (SharedRuntimeQuery<id> *)selectAllMapper:(id (^)(NSString *, NSString *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, NSString *, NSString *))mapper __attribute__((swift_name("selectAll(mapper:)")));
- (SharedRuntimeQuery<SharedBudget *> *)selectByEventIdEventId:(NSString *)eventId __attribute__((swift_name("selectByEventId(eventId:)")));
- (SharedRuntimeQuery<id> *)selectByEventIdEventId:(NSString *)eventId mapper:(id (^)(NSString *, NSString *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, NSString *, NSString *))mapper __attribute__((swift_name("selectByEventId(eventId:mapper:)")));
- (SharedRuntimeQuery<SharedBudget *> *)selectByIdId:(NSString *)id __attribute__((swift_name("selectById(id:)")));
- (SharedRuntimeQuery<id> *)selectByIdId:(NSString *)id mapper:(id (^)(NSString *, NSString *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, SharedDouble *, NSString *, NSString *))mapper __attribute__((swift_name("selectById(id:mapper:)")));
- (void)updateBudgetTotalEstimated:(double)totalEstimated totalActual:(double)totalActual transportEstimated:(double)transportEstimated transportActual:(double)transportActual accommodationEstimated:(double)accommodationEstimated accommodationActual:(double)accommodationActual mealsEstimated:(double)mealsEstimated mealsActual:(double)mealsActual activitiesEstimated:(double)activitiesEstimated activitiesActual:(double)activitiesActual equipmentEstimated:(double)equipmentEstimated equipmentActual:(double)equipmentActual otherEstimated:(double)otherEstimated otherActual:(double)otherActual updatedAt:(NSString *)updatedAt id:(NSString *)id __attribute__((swift_name("updateBudget(totalEstimated:totalActual:transportEstimated:transportActual:accommodationEstimated:accommodationActual:mealsEstimated:mealsActual:activitiesEstimated:activitiesActual:equipmentEstimated:equipmentActual:otherEstimated:otherActual:updatedAt:id:)")));
- (void)updateCategoryActualValue:(NSString *)value transportActual:(double)transportActual value_:(NSString *)value_ accommodationActual:(double)accommodationActual value__:(NSString *)value__ mealsActual:(double)mealsActual value___:(NSString *)value___ activitiesActual:(double)activitiesActual value____:(NSString *)value____ equipmentActual:(double)equipmentActual value_____:(NSString *)value_____ otherActual:(double)otherActual totalActual:(double)totalActual updatedAt:(NSString *)updatedAt id:(NSString *)id __attribute__((swift_name("updateCategoryActual(value:transportActual:value_:accommodationActual:value__:mealsActual:value___:activitiesActual:value____:equipmentActual:value_____:otherActual:totalActual:updatedAt:id:)")));
@end

__attribute__((swift_name("CalendarService")))
@protocol SharedCalendarService
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)addEventToCalendarEvent:(SharedCalendarEvent *)event completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("addEventToCalendar(event:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)generateICSInviteEvent:(SharedCalendarEvent *)event completionHandler:(void (^)(SharedCalendarInvite * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("generateICSInvite(event:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)removeCalendarEventCalendarEventId:(NSString *)calendarEventId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("removeCalendarEvent(calendarEventId:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)updateCalendarEventCalendarEventId:(NSString *)calendarEventId event:(SharedCalendarEvent *)event completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("updateCalendarEvent(calendarEventId:event:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ConfirmedDate")))
@interface SharedConfirmedDate : SharedBase
- (instancetype)initWithId:(NSString *)id eventId:(NSString *)eventId timeslotId:(NSString *)timeslotId confirmedByOrganizerId:(NSString *)confirmedByOrganizerId confirmedAt:(NSString *)confirmedAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("init(id:eventId:timeslotId:confirmedByOrganizerId:confirmedAt:updatedAt:)"))) __attribute__((objc_designated_initializer));
- (SharedConfirmedDate *)doCopyId:(NSString *)id eventId:(NSString *)eventId timeslotId:(NSString *)timeslotId confirmedByOrganizerId:(NSString *)confirmedByOrganizerId confirmedAt:(NSString *)confirmedAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("doCopy(id:eventId:timeslotId:confirmedByOrganizerId:confirmedAt:updatedAt:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *confirmedAt __attribute__((swift_name("confirmedAt")));
@property (readonly) NSString *confirmedByOrganizerId __attribute__((swift_name("confirmedByOrganizerId")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *timeslotId __attribute__((swift_name("timeslotId")));
@property (readonly) NSString *updatedAt __attribute__((swift_name("updatedAt")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ConfirmedDateQueries")))
@interface SharedConfirmedDateQueries : SharedRuntimeTransacterImpl
- (instancetype)initWithDriver:(id<SharedRuntimeSqlDriver>)driver __attribute__((swift_name("init(driver:)"))) __attribute__((objc_designated_initializer));
- (void)deleteByEventIdEventId:(NSString *)eventId __attribute__((swift_name("deleteByEventId(eventId:)")));
- (SharedRuntimeQuery<SharedConfirmedDate *> *)existsByEventIdEventId:(NSString *)eventId __attribute__((swift_name("existsByEventId(eventId:)")));
- (SharedRuntimeQuery<id> *)existsByEventIdEventId:(NSString *)eventId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("existsByEventId(eventId:mapper:)")));
- (void)insertConfirmedDateId:(NSString *)id eventId:(NSString *)eventId timeslotId:(NSString *)timeslotId confirmedByOrganizerId:(NSString *)confirmedByOrganizerId confirmedAt:(NSString *)confirmedAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("insertConfirmedDate(id:eventId:timeslotId:confirmedByOrganizerId:confirmedAt:updatedAt:)")));
- (SharedRuntimeQuery<SharedConfirmedDate *> *)selectAll __attribute__((swift_name("selectAll()")));
- (SharedRuntimeQuery<id> *)selectAllMapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectAll(mapper:)")));
- (SharedRuntimeQuery<SharedConfirmedDate *> *)selectByEventIdEventId:(NSString *)eventId __attribute__((swift_name("selectByEventId(eventId:)")));
- (SharedRuntimeQuery<id> *)selectByEventIdEventId:(NSString *)eventId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectByEventId(eventId:mapper:)")));
- (SharedRuntimeQuery<SharedConfirmedDate *> *)selectByIdId:(NSString *)id __attribute__((swift_name("selectById(id:)")));
- (SharedRuntimeQuery<id> *)selectByIdId:(NSString *)id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectById(id:mapper:)")));
- (SharedRuntimeQuery<SharedSelectWithTimeslotDetails *> *)selectWithTimeslotDetailsEventId:(NSString *)eventId __attribute__((swift_name("selectWithTimeslotDetails(eventId:)")));
- (SharedRuntimeQuery<id> *)selectWithTimeslotDetailsEventId:(NSString *)eventId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectWithTimeslotDetails(eventId:mapper:)")));
- (void)updateConfirmedDateTimeslotId:(NSString *)timeslotId updatedAt:(NSString *)updatedAt eventId:(NSString *)eventId __attribute__((swift_name("updateConfirmedDate(timeslotId:updatedAt:eventId:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CountAssignedParticipants")))
@interface SharedCountAssignedParticipants : SharedBase
- (instancetype)initWithAccommodation_id:(NSString *)accommodation_id COUNT:(int64_t)COUNT __attribute__((swift_name("init(accommodation_id:COUNT:)"))) __attribute__((objc_designated_initializer));
- (SharedCountAssignedParticipants *)doCopyAccommodation_id:(NSString *)accommodation_id COUNT:(int64_t)COUNT __attribute__((swift_name("doCopy(accommodation_id:COUNT:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t COUNT __attribute__((swift_name("COUNT")));
@property (readonly) NSString *accommodation_id __attribute__((swift_name("accommodation_id")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CountByStatus")))
@interface SharedCountByStatus : SharedBase
- (instancetype)initWithBooking_status:(NSString *)booking_status COUNT:(int64_t)COUNT __attribute__((swift_name("init(booking_status:COUNT:)"))) __attribute__((objc_designated_initializer));
- (SharedCountByStatus *)doCopyBooking_status:(NSString *)booking_status COUNT:(int64_t)COUNT __attribute__((swift_name("doCopy(booking_status:COUNT:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t COUNT __attribute__((swift_name("COUNT")));
@property (readonly) NSString *booking_status __attribute__((swift_name("booking_status")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CountMealsByStatus")))
@interface SharedCountMealsByStatus : SharedBase
- (instancetype)initWithStatus:(NSString *)status COUNT:(int64_t)COUNT __attribute__((swift_name("init(status:COUNT:)"))) __attribute__((objc_designated_initializer));
- (SharedCountMealsByStatus *)doCopyStatus:(NSString *)status COUNT:(int64_t)COUNT __attribute__((swift_name("doCopy(status:COUNT:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t COUNT __attribute__((swift_name("COUNT")));
@property (readonly) NSString *status __attribute__((swift_name("status")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CountMealsByType")))
@interface SharedCountMealsByType : SharedBase
- (instancetype)initWithType:(NSString *)type COUNT:(int64_t)COUNT __attribute__((swift_name("init(type:COUNT:)"))) __attribute__((objc_designated_initializer));
- (SharedCountMealsByType *)doCopyType:(NSString *)type COUNT:(int64_t)COUNT __attribute__((swift_name("doCopy(type:COUNT:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t COUNT __attribute__((swift_name("COUNT")));
@property (readonly) NSString *type __attribute__((swift_name("type")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CountRestrictionsByType")))
@interface SharedCountRestrictionsByType : SharedBase
- (instancetype)initWithRestriction:(NSString *)restriction COUNT:(int64_t)COUNT __attribute__((swift_name("init(restriction:COUNT:)"))) __attribute__((objc_designated_initializer));
- (SharedCountRestrictionsByType *)doCopyRestriction:(NSString *)restriction COUNT:(int64_t)COUNT __attribute__((swift_name("doCopy(restriction:COUNT:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t COUNT __attribute__((swift_name("COUNT")));
@property (readonly) NSString *restriction __attribute__((swift_name("restriction")));
@end

__attribute__((swift_name("EventRepositoryInterface")))
@protocol SharedEventRepositoryInterface
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)addParticipantEventId:(NSString *)eventId participantId:(NSString *)participantId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("addParticipant(eventId:participantId:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)addVoteEventId:(NSString *)eventId participantId:(NSString *)participantId slotId:(NSString *)slotId vote:(SharedVote *)vote completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("addVote(eventId:participantId:slotId:vote:completionHandler:)")));
- (BOOL)canModifyEventEventId:(NSString *)eventId userId:(NSString *)userId __attribute__((swift_name("canModifyEvent(eventId:userId:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)createEventEvent:(SharedEvent *)event completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("createEvent(event:completionHandler:)")));
- (NSArray<SharedEvent *> *)getAllEvents __attribute__((swift_name("getAllEvents()")));
- (SharedEvent * _Nullable)getEventId:(NSString *)id __attribute__((swift_name("getEvent(id:)")));
- (NSArray<NSString *> * _Nullable)getParticipantsEventId:(NSString *)eventId __attribute__((swift_name("getParticipants(eventId:)")));
- (SharedPoll * _Nullable)getPollEventId:(NSString *)eventId __attribute__((swift_name("getPoll(eventId:)")));
- (BOOL)isDeadlinePassedDeadline:(NSString *)deadline __attribute__((swift_name("isDeadlinePassed(deadline:)")));
- (BOOL)isOrganizerEventId:(NSString *)eventId userId:(NSString *)userId __attribute__((swift_name("isOrganizer(eventId:userId:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)updateEventEvent:(SharedEvent *)event completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("updateEvent(event:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)updateEventStatusId:(NSString *)id status:(SharedEventStatus *)status finalDate:(NSString * _Nullable)finalDate completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("updateEventStatus(id:status:finalDate:completionHandler:)")));
@end


/**
 * Database-backed event repository using SQLDelight for persistence.
 * Mirrors the EventRepository interface but stores data in SQLite.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DatabaseEventRepository")))
@interface SharedDatabaseEventRepository : SharedBase <SharedEventRepositoryInterface>
- (instancetype)initWithDb:(id<SharedWakevDb>)db syncManager:(SharedSyncManager * _Nullable)syncManager __attribute__((swift_name("init(db:syncManager:)"))) __attribute__((objc_designated_initializer));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)addParticipantEventId:(NSString *)eventId participantId:(NSString *)participantId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("addParticipant(eventId:participantId:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)addVoteEventId:(NSString *)eventId participantId:(NSString *)participantId slotId:(NSString *)slotId vote:(SharedVote *)vote completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("addVote(eventId:participantId:slotId:vote:completionHandler:)")));
- (BOOL)canModifyEventEventId:(NSString *)eventId userId:(NSString *)userId __attribute__((swift_name("canModifyEvent(eventId:userId:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)createEventEvent:(SharedEvent *)event completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("createEvent(event:completionHandler:)")));
- (NSArray<SharedEvent *> *)getAllEvents __attribute__((swift_name("getAllEvents()")));
- (SharedEvent * _Nullable)getEventId:(NSString *)id __attribute__((swift_name("getEvent(id:)")));
- (NSArray<NSString *> * _Nullable)getParticipantsEventId:(NSString *)eventId __attribute__((swift_name("getParticipants(eventId:)")));
- (SharedPoll * _Nullable)getPollEventId:(NSString *)eventId __attribute__((swift_name("getPoll(eventId:)")));
- (BOOL)isDeadlinePassedDeadline:(NSString *)deadline __attribute__((swift_name("isDeadlinePassed(deadline:)")));
- (BOOL)isOrganizerEventId:(NSString *)eventId userId:(NSString *)userId __attribute__((swift_name("isOrganizer(eventId:userId:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)updateEventEvent:(SharedEvent *)event completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("updateEvent(event:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)updateEventStatusId:(NSString *)id status:(SharedEventStatus *)status finalDate:(NSString * _Nullable)finalDate completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("updateEventStatus(id:status:finalDate:completionHandler:)")));
@end


/**
 * Factory for creating and managing the Wakev database instance.
 * Platform-specific implementations handle driver creation.
 */
__attribute__((swift_name("DatabaseFactory")))
@protocol SharedDatabaseFactory
@required
- (id<SharedRuntimeSqlDriver>)createDriver __attribute__((swift_name("createDriver()")));
@end


/**
 * Creates a singleton instance of the WakevDb database.
 * Handles initialization and driver setup for the current platform.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DatabaseProvider")))
@interface SharedDatabaseProvider : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Creates a singleton instance of the WakevDb database.
 * Handles initialization and driver setup for the current platform.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)databaseProvider __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedDatabaseProvider *shared __attribute__((swift_name("shared")));
- (id<SharedWakevDb>)getDatabaseFactory:(id<SharedDatabaseFactory>)factory __attribute__((swift_name("getDatabase(factory:)")));
- (void)resetDatabase __attribute__((swift_name("resetDatabase()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DefaultCalendarService")))
@interface SharedDefaultCalendarService : SharedBase <SharedCalendarService>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)addEventToCalendarEvent:(SharedCalendarEvent *)event completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("addEventToCalendar(event:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)generateICSInviteEvent:(SharedCalendarEvent *)event completionHandler:(void (^)(SharedCalendarInvite * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("generateICSInvite(event:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)removeCalendarEventCalendarEventId:(NSString *)calendarEventId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("removeCalendarEvent(calendarEventId:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)updateCalendarEventCalendarEventId:(NSString *)calendarEventId event:(SharedCalendarEvent *)event completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("updateCalendarEvent(calendarEventId:event:completionHandler:)")));
@end

__attribute__((swift_name("NotificationService")))
@protocol SharedNotificationService
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getUnreadNotificationsUserId:(NSString *)userId completionHandler:(void (^)(NSArray<SharedNotificationMessage *> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getUnreadNotifications(userId:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)markAsReadNotificationId:(NSString *)notificationId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("markAsRead(notificationId:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)registerPushTokenToken:(SharedPushToken *)token completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("registerPushToken(token:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)sendNotificationMessage:(SharedNotificationMessage *)message completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("sendNotification(message:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)unregisterPushTokenUserId:(NSString *)userId deviceId:(NSString *)deviceId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("unregisterPushToken(userId:deviceId:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DefaultNotificationService")))
@interface SharedDefaultNotificationService : SharedBase <SharedNotificationService>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getUnreadNotificationsUserId:(NSString *)userId completionHandler:(void (^)(NSArray<SharedNotificationMessage *> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getUnreadNotifications(userId:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)markAsReadNotificationId:(NSString *)notificationId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("markAsRead(notificationId:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)registerPushTokenToken:(SharedPushToken *)token completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("registerPushToken(token:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)sendNotificationMessage:(SharedNotificationMessage *)message completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("sendNotification(message:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)unregisterPushTokenUserId:(NSString *)userId deviceId:(NSString *)deviceId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("unregisterPushToken(userId:deviceId:completionHandler:)")));
@end

__attribute__((swift_name("SuggestionEngine")))
@protocol SharedSuggestionEngine
@required
- (NSArray<SharedRecommendation *> *)suggestActivitiesEvent:(SharedEvent *)event preferences:(SharedUserPreferences *)preferences __attribute__((swift_name("suggestActivities(event:preferences:)")));
- (NSArray<SharedRecommendation *> *)suggestDatesEvent:(SharedEvent *)event preferences:(SharedUserPreferences *)preferences __attribute__((swift_name("suggestDates(event:preferences:)")));
- (NSArray<SharedRecommendation *> *)suggestLocationsEvent:(SharedEvent *)event preferences:(SharedUserPreferences *)preferences __attribute__((swift_name("suggestLocations(event:preferences:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DefaultSuggestionEngine")))
@interface SharedDefaultSuggestionEngine : SharedBase <SharedSuggestionEngine>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSArray<SharedRecommendation *> *)suggestActivitiesEvent:(SharedEvent *)event preferences:(SharedUserPreferences *)preferences __attribute__((swift_name("suggestActivities(event:preferences:)")));
- (NSArray<SharedRecommendation *> *)suggestDatesEvent:(SharedEvent *)event preferences:(SharedUserPreferences *)preferences __attribute__((swift_name("suggestDates(event:preferences:)")));
- (NSArray<SharedRecommendation *> *)suggestLocationsEvent:(SharedEvent *)event preferences:(SharedUserPreferences *)preferences __attribute__((swift_name("suggestLocations(event:preferences:)")));
@end

__attribute__((swift_name("TransportService")))
@protocol SharedTransportService
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)findGroupMeetingPointsRoutes:(NSDictionary<NSString *, SharedRoute *> *)routes maxWaitTimeMinutes:(int32_t)maxWaitTimeMinutes completionHandler:(void (^)(NSArray<NSString *> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("findGroupMeetingPoints(routes:maxWaitTimeMinutes:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getTransportOptionsFrom:(SharedLocation *)from to:(SharedLocation *)to departureTime:(NSString *)departureTime mode:(SharedTransportMode * _Nullable)mode completionHandler:(void (^)(NSArray<SharedTransportOption *> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getTransportOptions(from:to:departureTime:mode:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)optimizeRoutesParticipants:(NSDictionary<NSString *, SharedLocation *> *)participants destination:(SharedLocation *)destination eventTime:(NSString *)eventTime optimizationType:(SharedOptimizationType *)optimizationType completionHandler:(void (^)(SharedTransportPlan * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("optimizeRoutes(participants:destination:eventTime:optimizationType:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DefaultTransportService")))
@interface SharedDefaultTransportService : SharedBase <SharedTransportService>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)findGroupMeetingPointsRoutes:(NSDictionary<NSString *, SharedRoute *> *)routes maxWaitTimeMinutes:(int32_t)maxWaitTimeMinutes completionHandler:(void (^)(NSArray<NSString *> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("findGroupMeetingPoints(routes:maxWaitTimeMinutes:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getTransportOptionsFrom:(SharedLocation *)from to:(SharedLocation *)to departureTime:(NSString *)departureTime mode:(SharedTransportMode * _Nullable)mode completionHandler:(void (^)(NSArray<SharedTransportOption *> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getTransportOptions(from:to:departureTime:mode:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)optimizeRoutesParticipants:(NSDictionary<NSString *, SharedLocation *> *)participants destination:(SharedLocation *)destination eventTime:(NSString *)eventTime optimizationType:(SharedOptimizationType *)optimizationType completionHandler:(void (^)(SharedTransportPlan * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("optimizeRoutes(participants:destination:eventTime:optimizationType:completionHandler:)")));
@end


/**
 * Device fingerprint data model.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DeviceData")))
@interface SharedDeviceData : SharedBase
- (instancetype)initWithId:(NSString *)id userId:(NSString *)userId deviceId:(NSString *)deviceId deviceName:(NSString *)deviceName deviceType:(NSString * _Nullable)deviceType fingerprintHash:(NSString *)fingerprintHash firstSeen:(NSString *)firstSeen lastSeen:(NSString *)lastSeen trusted:(BOOL)trusted createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("init(id:userId:deviceId:deviceName:deviceType:fingerprintHash:firstSeen:lastSeen:trusted:createdAt:updatedAt:)"))) __attribute__((objc_designated_initializer));
- (SharedDeviceData *)doCopyId:(NSString *)id userId:(NSString *)userId deviceId:(NSString *)deviceId deviceName:(NSString *)deviceName deviceType:(NSString * _Nullable)deviceType fingerprintHash:(NSString *)fingerprintHash firstSeen:(NSString *)firstSeen lastSeen:(NSString *)lastSeen trusted:(BOOL)trusted createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("doCopy(id:userId:deviceId:deviceName:deviceType:fingerprintHash:firstSeen:lastSeen:trusted:createdAt:updatedAt:)")));

/**
 * Device fingerprint data model.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Device fingerprint data model.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Device fingerprint data model.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *deviceId __attribute__((swift_name("deviceId")));
@property (readonly) NSString *deviceName __attribute__((swift_name("deviceName")));
@property (readonly) NSString * _Nullable deviceType __attribute__((swift_name("deviceType")));
@property (readonly) NSString *fingerprintHash __attribute__((swift_name("fingerprintHash")));
@property (readonly) NSString *firstSeen __attribute__((swift_name("firstSeen")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *lastSeen __attribute__((swift_name("lastSeen")));
@property (readonly) BOOL trusted __attribute__((swift_name("trusted")));
@property (readonly) NSString *updatedAt __attribute__((swift_name("updatedAt")));
@property (readonly) NSString *userId __attribute__((swift_name("userId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Device_fingerprint")))
@interface SharedDevice_fingerprint : SharedBase
- (instancetype)initWithId:(NSString *)id user_id:(NSString *)user_id device_id:(NSString *)device_id device_name:(NSString *)device_name device_type:(NSString * _Nullable)device_type fingerprint_hash:(NSString *)fingerprint_hash first_seen:(NSString *)first_seen last_seen:(NSString *)last_seen trusted:(int64_t)trusted created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("init(id:user_id:device_id:device_name:device_type:fingerprint_hash:first_seen:last_seen:trusted:created_at:updated_at:)"))) __attribute__((objc_designated_initializer));
- (SharedDevice_fingerprint *)doCopyId:(NSString *)id user_id:(NSString *)user_id device_id:(NSString *)device_id device_name:(NSString *)device_name device_type:(NSString * _Nullable)device_type fingerprint_hash:(NSString *)fingerprint_hash first_seen:(NSString *)first_seen last_seen:(NSString *)last_seen trusted:(int64_t)trusted created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("doCopy(id:user_id:device_id:device_name:device_type:fingerprint_hash:first_seen:last_seen:trusted:created_at:updated_at:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *created_at __attribute__((swift_name("created_at")));
@property (readonly) NSString *device_id __attribute__((swift_name("device_id")));
@property (readonly) NSString *device_name __attribute__((swift_name("device_name")));
@property (readonly) NSString * _Nullable device_type __attribute__((swift_name("device_type")));
@property (readonly) NSString *fingerprint_hash __attribute__((swift_name("fingerprint_hash")));
@property (readonly) NSString *first_seen __attribute__((swift_name("first_seen")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *last_seen __attribute__((swift_name("last_seen")));
@property (readonly) int64_t trusted __attribute__((swift_name("trusted")));
@property (readonly) NSString *updated_at __attribute__((swift_name("updated_at")));
@property (readonly) NSString *user_id __attribute__((swift_name("user_id")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EquipmentItemQueries")))
@interface SharedEquipmentItemQueries : SharedRuntimeTransacterImpl
- (instancetype)initWithDriver:(id<SharedRuntimeSqlDriver>)driver __attribute__((swift_name("init(driver:)"))) __attribute__((objc_designated_initializer));
- (SharedRuntimeQuery<SharedLong *> *)countEquipmentItemsByEventEvent_id:(NSString *)event_id __attribute__((swift_name("countEquipmentItemsByEvent(event_id:)")));
- (SharedRuntimeQuery<SharedLong *> *)countEquipmentItemsByEventAndCategoryEvent_id:(NSString *)event_id category:(NSString *)category __attribute__((swift_name("countEquipmentItemsByEventAndCategory(event_id:category:)")));
- (SharedRuntimeQuery<SharedLong *> *)countEquipmentItemsByEventAndStatusEvent_id:(NSString *)event_id status:(NSString *)status __attribute__((swift_name("countEquipmentItemsByEventAndStatus(event_id:status:)")));
- (void)deleteEquipmentItemId:(NSString *)id __attribute__((swift_name("deleteEquipmentItem(id:)")));
- (void)deleteEquipmentItemsByEventEvent_id:(NSString *)event_id __attribute__((swift_name("deleteEquipmentItemsByEvent(event_id:)")));
- (SharedRuntimeQuery<SharedBoolean *> *)equipmentItemExistsId:(NSString *)id __attribute__((swift_name("equipmentItemExists(id:)")));
- (void)insertEquipmentItemId:(NSString *)id event_id:(NSString *)event_id name:(NSString *)name category:(NSString *)category quantity:(int64_t)quantity assigned_to:(NSString * _Nullable)assigned_to status:(NSString *)status shared_cost:(SharedLong * _Nullable)shared_cost notes:(NSString * _Nullable)notes created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("insertEquipmentItem(id:event_id:name:category:quantity:assigned_to:status:shared_cost:notes:created_at:updated_at:)")));
- (SharedRuntimeQuery<SharedEquipment_item *> *)selectEquipmentItemByIdId:(NSString *)id __attribute__((swift_name("selectEquipmentItemById(id:)")));
- (SharedRuntimeQuery<id> *)selectEquipmentItemByIdId:(NSString *)id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, SharedLong *, NSString * _Nullable, NSString *, SharedLong * _Nullable, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("selectEquipmentItemById(id:mapper:)")));
- (SharedRuntimeQuery<SharedEquipment_item *> *)selectEquipmentItemsByAssigneeEvent_id:(NSString *)event_id assigned_to:(NSString * _Nullable)assigned_to __attribute__((swift_name("selectEquipmentItemsByAssignee(event_id:assigned_to:)")));
- (SharedRuntimeQuery<id> *)selectEquipmentItemsByAssigneeEvent_id:(NSString *)event_id assigned_to:(NSString * _Nullable)assigned_to mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, SharedLong *, NSString * _Nullable, NSString *, SharedLong * _Nullable, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("selectEquipmentItemsByAssignee(event_id:assigned_to:mapper:)")));
- (SharedRuntimeQuery<SharedEquipment_item *> *)selectEquipmentItemsByEventEvent_id:(NSString *)event_id __attribute__((swift_name("selectEquipmentItemsByEvent(event_id:)")));
- (SharedRuntimeQuery<id> *)selectEquipmentItemsByEventEvent_id:(NSString *)event_id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, SharedLong *, NSString * _Nullable, NSString *, SharedLong * _Nullable, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("selectEquipmentItemsByEvent(event_id:mapper:)")));
- (SharedRuntimeQuery<SharedEquipment_item *> *)selectEquipmentItemsByEventAndCategoryEvent_id:(NSString *)event_id category:(NSString *)category __attribute__((swift_name("selectEquipmentItemsByEventAndCategory(event_id:category:)")));
- (SharedRuntimeQuery<id> *)selectEquipmentItemsByEventAndCategoryEvent_id:(NSString *)event_id category:(NSString *)category mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, SharedLong *, NSString * _Nullable, NSString *, SharedLong * _Nullable, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("selectEquipmentItemsByEventAndCategory(event_id:category:mapper:)")));
- (SharedRuntimeQuery<SharedEquipment_item *> *)selectEquipmentItemsByEventAndStatusEvent_id:(NSString *)event_id status:(NSString *)status __attribute__((swift_name("selectEquipmentItemsByEventAndStatus(event_id:status:)")));
- (SharedRuntimeQuery<id> *)selectEquipmentItemsByEventAndStatusEvent_id:(NSString *)event_id status:(NSString *)status mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, SharedLong *, NSString * _Nullable, NSString *, SharedLong * _Nullable, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("selectEquipmentItemsByEventAndStatus(event_id:status:mapper:)")));
- (SharedRuntimeQuery<SharedSelectEquipmentOverallStats *> *)selectEquipmentOverallStatsEvent_id:(NSString *)event_id __attribute__((swift_name("selectEquipmentOverallStats(event_id:)")));
- (SharedRuntimeQuery<id> *)selectEquipmentOverallStatsEvent_id:(NSString *)event_id mapper:(id (^)(SharedLong *, SharedLong *, SharedLong *, SharedLong *, SharedDouble *))mapper __attribute__((swift_name("selectEquipmentOverallStats(event_id:mapper:)")));
- (SharedRuntimeQuery<SharedSelectEquipmentStatsByAssignee *> *)selectEquipmentStatsByAssigneeEvent_id:(NSString *)event_id __attribute__((swift_name("selectEquipmentStatsByAssignee(event_id:)")));
- (SharedRuntimeQuery<id> *)selectEquipmentStatsByAssigneeEvent_id:(NSString *)event_id mapper:(id (^)(NSString *, SharedLong *, SharedLong *, SharedLong *, SharedDouble *))mapper __attribute__((swift_name("selectEquipmentStatsByAssignee(event_id:mapper:)")));
- (SharedRuntimeQuery<SharedSelectEquipmentStatsByCategory *> *)selectEquipmentStatsByCategoryEvent_id:(NSString *)event_id __attribute__((swift_name("selectEquipmentStatsByCategory(event_id:)")));
- (SharedRuntimeQuery<id> *)selectEquipmentStatsByCategoryEvent_id:(NSString *)event_id mapper:(id (^)(NSString *, SharedLong *, SharedLong *, SharedLong *, SharedLong *, SharedDouble *))mapper __attribute__((swift_name("selectEquipmentStatsByCategory(event_id:mapper:)")));
- (SharedRuntimeQuery<NSString *> *)selectItemNamesByAssigneeEvent_id:(NSString *)event_id assigned_to:(NSString * _Nullable)assigned_to __attribute__((swift_name("selectItemNamesByAssignee(event_id:assigned_to:)")));
- (SharedRuntimeQuery<SharedEquipment_item *> *)selectUnassignedItemsEvent_id:(NSString *)event_id __attribute__((swift_name("selectUnassignedItems(event_id:)")));
- (SharedRuntimeQuery<id> *)selectUnassignedItemsEvent_id:(NSString *)event_id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, SharedLong *, NSString * _Nullable, NSString *, SharedLong * _Nullable, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("selectUnassignedItems(event_id:mapper:)")));
- (SharedRuntimeQuery<SharedDouble *> *)sumEquipmentCostByAssigneeEvent_id:(NSString *)event_id assigned_to:(NSString * _Nullable)assigned_to __attribute__((swift_name("sumEquipmentCostByAssignee(event_id:assigned_to:)")));
- (SharedRuntimeQuery<SharedDouble *> *)sumEquipmentCostByEventEvent_id:(NSString *)event_id __attribute__((swift_name("sumEquipmentCostByEvent(event_id:)")));
- (SharedRuntimeQuery<SharedDouble *> *)sumEquipmentCostByEventAndCategoryEvent_id:(NSString *)event_id category:(NSString *)category __attribute__((swift_name("sumEquipmentCostByEventAndCategory(event_id:category:)")));
- (void)updateEquipmentItemName:(NSString *)name category:(NSString *)category quantity:(int64_t)quantity assigned_to:(NSString * _Nullable)assigned_to status:(NSString *)status shared_cost:(SharedLong * _Nullable)shared_cost notes:(NSString * _Nullable)notes updated_at:(NSString *)updated_at id:(NSString *)id __attribute__((swift_name("updateEquipmentItem(name:category:quantity:assigned_to:status:shared_cost:notes:updated_at:id:)")));
- (void)updateEquipmentItemAssignmentAssigned_to:(NSString * _Nullable)assigned_to status:(NSString *)status updated_at:(NSString *)updated_at id:(NSString *)id __attribute__((swift_name("updateEquipmentItemAssignment(assigned_to:status:updated_at:id:)")));
- (void)updateEquipmentItemStatusStatus:(NSString *)status updated_at:(NSString *)updated_at id:(NSString *)id __attribute__((swift_name("updateEquipmentItemStatus(status:updated_at:id:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Equipment_item")))
@interface SharedEquipment_item : SharedBase
- (instancetype)initWithId:(NSString *)id event_id:(NSString *)event_id name:(NSString *)name category:(NSString *)category quantity:(int64_t)quantity assigned_to:(NSString * _Nullable)assigned_to status:(NSString *)status shared_cost:(SharedLong * _Nullable)shared_cost notes:(NSString * _Nullable)notes created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("init(id:event_id:name:category:quantity:assigned_to:status:shared_cost:notes:created_at:updated_at:)"))) __attribute__((objc_designated_initializer));
- (SharedEquipment_item *)doCopyId:(NSString *)id event_id:(NSString *)event_id name:(NSString *)name category:(NSString *)category quantity:(int64_t)quantity assigned_to:(NSString * _Nullable)assigned_to status:(NSString *)status shared_cost:(SharedLong * _Nullable)shared_cost notes:(NSString * _Nullable)notes created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("doCopy(id:event_id:name:category:quantity:assigned_to:status:shared_cost:notes:created_at:updated_at:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable assigned_to __attribute__((swift_name("assigned_to")));
@property (readonly) NSString *category __attribute__((swift_name("category")));
@property (readonly) NSString *created_at __attribute__((swift_name("created_at")));
@property (readonly) NSString *event_id __attribute__((swift_name("event_id")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) NSString * _Nullable notes __attribute__((swift_name("notes")));
@property (readonly) int64_t quantity __attribute__((swift_name("quantity")));
@property (readonly) SharedLong * _Nullable shared_cost __attribute__((swift_name("shared_cost")));
@property (readonly) NSString *status __attribute__((swift_name("status")));
@property (readonly) NSString *updated_at __attribute__((swift_name("updated_at")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Event_")))
@interface SharedEvent_ : SharedBase
- (instancetype)initWithId:(NSString *)id organizerId:(NSString *)organizerId title:(NSString *)title description:(NSString *)description status:(NSString *)status deadline:(NSString *)deadline createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt version:(int64_t)version __attribute__((swift_name("init(id:organizerId:title:description:status:deadline:createdAt:updatedAt:version:)"))) __attribute__((objc_designated_initializer));
- (SharedEvent_ *)doCopyId:(NSString *)id organizerId:(NSString *)organizerId title:(NSString *)title description:(NSString *)description status:(NSString *)status deadline:(NSString *)deadline createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt version:(int64_t)version __attribute__((swift_name("doCopy(id:organizerId:title:description:status:deadline:createdAt:updatedAt:version:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *deadline __attribute__((swift_name("deadline")));
@property (readonly) NSString *description_ __attribute__((swift_name("description_")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *organizerId __attribute__((swift_name("organizerId")));
@property (readonly) NSString *status __attribute__((swift_name("status")));
@property (readonly) NSString *title __attribute__((swift_name("title")));
@property (readonly) NSString *updatedAt __attribute__((swift_name("updatedAt")));
@property (readonly) int64_t version __attribute__((swift_name("version")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EventQueries")))
@interface SharedEventQueries : SharedRuntimeTransacterImpl
- (instancetype)initWithDriver:(id<SharedRuntimeSqlDriver>)driver __attribute__((swift_name("init(driver:)"))) __attribute__((objc_designated_initializer));
- (void)deleteEventId:(NSString *)id __attribute__((swift_name("deleteEvent(id:)")));
- (void)insertEventId:(NSString *)id organizerId:(NSString *)organizerId title:(NSString *)title description:(NSString *)description status:(NSString *)status deadline:(NSString *)deadline createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt version:(int64_t)version __attribute__((swift_name("insertEvent(id:organizerId:title:description:status:deadline:createdAt:updatedAt:version:)")));
- (SharedRuntimeQuery<SharedEvent_ *> *)selectAll __attribute__((swift_name("selectAll()")));
- (SharedRuntimeQuery<id> *)selectAllMapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, SharedLong *))mapper __attribute__((swift_name("selectAll(mapper:)")));
- (SharedRuntimeQuery<SharedEvent_ *> *)selectByIdId:(NSString *)id __attribute__((swift_name("selectById(id:)")));
- (SharedRuntimeQuery<id> *)selectByIdId:(NSString *)id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, SharedLong *))mapper __attribute__((swift_name("selectById(id:mapper:)")));
- (SharedRuntimeQuery<SharedEvent_ *> *)selectByOrganizerIdOrganizerId:(NSString *)organizerId __attribute__((swift_name("selectByOrganizerId(organizerId:)")));
- (SharedRuntimeQuery<id> *)selectByOrganizerIdOrganizerId:(NSString *)organizerId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, SharedLong *))mapper __attribute__((swift_name("selectByOrganizerId(organizerId:mapper:)")));
- (SharedRuntimeQuery<SharedEvent_ *> *)selectByStatusStatus:(NSString *)status __attribute__((swift_name("selectByStatus(status:)")));
- (SharedRuntimeQuery<id> *)selectByStatusStatus:(NSString *)status mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, SharedLong *))mapper __attribute__((swift_name("selectByStatus(status:mapper:)")));
- (void)updateEventTitle:(NSString *)title description:(NSString *)description status:(NSString *)status deadline:(NSString *)deadline updatedAt:(NSString *)updatedAt id:(NSString *)id __attribute__((swift_name("updateEvent(title:description:status:deadline:updatedAt:id:)")));
- (void)updateEventStatusStatus:(NSString *)status updatedAt:(NSString *)updatedAt id:(NSString *)id __attribute__((swift_name("updateEventStatus(status:updatedAt:id:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EventRepository")))
@interface SharedEventRepository : SharedBase <SharedEventRepositoryInterface>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)addParticipantEventId:(NSString *)eventId participantId:(NSString *)participantId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("addParticipant(eventId:participantId:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)addVoteEventId:(NSString *)eventId participantId:(NSString *)participantId slotId:(NSString *)slotId vote:(SharedVote *)vote completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("addVote(eventId:participantId:slotId:vote:completionHandler:)")));
- (BOOL)canModifyEventEventId:(NSString *)eventId userId:(NSString *)userId __attribute__((swift_name("canModifyEvent(eventId:userId:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)createEventEvent:(SharedEvent *)event completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("createEvent(event:completionHandler:)")));
- (NSArray<SharedEvent *> *)getAllEvents __attribute__((swift_name("getAllEvents()")));
- (SharedEvent * _Nullable)getEventId:(NSString *)id __attribute__((swift_name("getEvent(id:)")));
- (NSArray<NSString *> * _Nullable)getParticipantsEventId:(NSString *)eventId __attribute__((swift_name("getParticipants(eventId:)")));
- (SharedPoll * _Nullable)getPollEventId:(NSString *)eventId __attribute__((swift_name("getPoll(eventId:)")));
- (BOOL)isDeadlinePassedDeadline:(NSString *)deadline __attribute__((swift_name("isDeadlinePassed(deadline:)")));
- (BOOL)isOrganizerEventId:(NSString *)eventId userId:(NSString *)userId __attribute__((swift_name("isOrganizer(eventId:userId:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)updateEventEvent:(SharedEvent *)event completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("updateEvent(event:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)updateEventStatusId:(NSString *)id status:(SharedEventStatus *)status finalDate:(NSString * _Nullable)finalDate completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("updateEventStatus(id:status:finalDate:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GetParticipantsWithMultipleRestrictions")))
@interface SharedGetParticipantsWithMultipleRestrictions : SharedBase
- (instancetype)initWithParticipant_id:(NSString *)participant_id restriction_count:(int64_t)restriction_count __attribute__((swift_name("init(participant_id:restriction_count:)"))) __attribute__((objc_designated_initializer));
- (SharedGetParticipantsWithMultipleRestrictions *)doCopyParticipant_id:(NSString *)participant_id restriction_count:(int64_t)restriction_count __attribute__((swift_name("doCopy(participant_id:restriction_count:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *participant_id __attribute__((swift_name("participant_id")));
@property (readonly) int64_t restriction_count __attribute__((swift_name("restriction_count")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GetRoomOccupancyStats")))
@interface SharedGetRoomOccupancyStats : SharedBase
- (instancetype)initWithAccommodation_id:(NSString *)accommodation_id COUNT:(int64_t)COUNT SUM:(SharedLong * _Nullable)SUM __attribute__((swift_name("init(accommodation_id:COUNT:SUM:)"))) __attribute__((objc_designated_initializer));
- (SharedGetRoomOccupancyStats *)doCopyAccommodation_id:(NSString *)accommodation_id COUNT:(int64_t)COUNT SUM:(SharedLong * _Nullable)SUM __attribute__((swift_name("doCopy(accommodation_id:COUNT:SUM:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t COUNT __attribute__((swift_name("COUNT")));
@property (readonly) SharedLong * _Nullable SUM __attribute__((swift_name("SUM")));
@property (readonly) NSString *accommodation_id __attribute__((swift_name("accommodation_id")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GetTotalAccommodationCost")))
@interface SharedGetTotalAccommodationCost : SharedBase
- (instancetype)initWithSUM:(SharedLong * _Nullable)SUM __attribute__((swift_name("init(SUM:)"))) __attribute__((objc_designated_initializer));
- (SharedGetTotalAccommodationCost *)doCopySUM:(SharedLong * _Nullable)SUM __attribute__((swift_name("doCopy(SUM:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedLong * _Nullable SUM __attribute__((swift_name("SUM")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GetTotalActualCost")))
@interface SharedGetTotalActualCost : SharedBase
- (instancetype)initWithSUM:(SharedDouble * _Nullable)SUM __attribute__((swift_name("init(SUM:)"))) __attribute__((objc_designated_initializer));
- (SharedGetTotalActualCost *)doCopySUM:(SharedDouble * _Nullable)SUM __attribute__((swift_name("doCopy(SUM:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedDouble * _Nullable SUM __attribute__((swift_name("SUM")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GetTotalConfirmedCapacity")))
@interface SharedGetTotalConfirmedCapacity : SharedBase
- (instancetype)initWithSUM:(SharedLong * _Nullable)SUM __attribute__((swift_name("init(SUM:)"))) __attribute__((objc_designated_initializer));
- (SharedGetTotalConfirmedCapacity *)doCopySUM:(SharedLong * _Nullable)SUM __attribute__((swift_name("doCopy(SUM:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedLong * _Nullable SUM __attribute__((swift_name("SUM")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GetTotalEstimatedCost")))
@interface SharedGetTotalEstimatedCost : SharedBase
- (instancetype)initWithSUM:(SharedLong * _Nullable)SUM __attribute__((swift_name("init(SUM:)"))) __attribute__((objc_designated_initializer));
- (SharedGetTotalEstimatedCost *)doCopySUM:(SharedLong * _Nullable)SUM __attribute__((swift_name("doCopy(SUM:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedLong * _Nullable SUM __attribute__((swift_name("SUM")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Greeting")))
@interface SharedGreeting : SharedBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSString *)greet __attribute__((swift_name("greet()")));
@end

__attribute__((swift_name("Platform")))
@protocol SharedPlatform
@required
@property (readonly) NSString *name __attribute__((swift_name("name")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("IOSPlatform")))
@interface SharedIOSPlatform : SharedBase <SharedPlatform>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@end


/**
 * iOS-specific database factory using the native SQLite driver.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("IosDatabaseFactory")))
@interface SharedIosDatabaseFactory : SharedBase <SharedDatabaseFactory>

/**
 * iOS-specific database factory using the native SQLite driver.
 */
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));

/**
 * iOS-specific database factory using the native SQLite driver.
 */
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (id<SharedRuntimeSqlDriver>)createDriver __attribute__((swift_name("createDriver()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Jwt_blacklist")))
@interface SharedJwt_blacklist : SharedBase
- (instancetype)initWithToken_hash:(NSString *)token_hash user_id:(NSString *)user_id revoked_at:(NSString *)revoked_at reason:(NSString * _Nullable)reason expires_at:(NSString *)expires_at __attribute__((swift_name("init(token_hash:user_id:revoked_at:reason:expires_at:)"))) __attribute__((objc_designated_initializer));
- (SharedJwt_blacklist *)doCopyToken_hash:(NSString *)token_hash user_id:(NSString *)user_id revoked_at:(NSString *)revoked_at reason:(NSString * _Nullable)reason expires_at:(NSString *)expires_at __attribute__((swift_name("doCopy(token_hash:user_id:revoked_at:reason:expires_at:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *expires_at __attribute__((swift_name("expires_at")));
@property (readonly) NSString * _Nullable reason __attribute__((swift_name("reason")));
@property (readonly) NSString *revoked_at __attribute__((swift_name("revoked_at")));
@property (readonly) NSString *token_hash __attribute__((swift_name("token_hash")));
@property (readonly) NSString *user_id __attribute__((swift_name("user_id")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LastSyncTime")))
@interface SharedLastSyncTime : SharedBase
- (instancetype)initWithMAX:(NSString * _Nullable)MAX __attribute__((swift_name("init(MAX:)"))) __attribute__((objc_designated_initializer));
- (SharedLastSyncTime *)doCopyMAX:(NSString * _Nullable)MAX __attribute__((swift_name("doCopy(MAX:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable MAX __attribute__((swift_name("MAX")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Meal")))
@interface SharedMeal : SharedBase
- (instancetype)initWithId:(NSString *)id event_id:(NSString *)event_id type:(NSString *)type name:(NSString *)name date:(NSString *)date time:(NSString *)time location:(NSString * _Nullable)location responsible_participant_ids:(NSString *)responsible_participant_ids estimated_cost:(int64_t)estimated_cost actual_cost:(SharedLong * _Nullable)actual_cost servings:(int64_t)servings status:(NSString *)status notes:(NSString * _Nullable)notes created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("init(id:event_id:type:name:date:time:location:responsible_participant_ids:estimated_cost:actual_cost:servings:status:notes:created_at:updated_at:)"))) __attribute__((objc_designated_initializer));
- (SharedMeal *)doCopyId:(NSString *)id event_id:(NSString *)event_id type:(NSString *)type name:(NSString *)name date:(NSString *)date time:(NSString *)time location:(NSString * _Nullable)location responsible_participant_ids:(NSString *)responsible_participant_ids estimated_cost:(int64_t)estimated_cost actual_cost:(SharedLong * _Nullable)actual_cost servings:(int64_t)servings status:(NSString *)status notes:(NSString * _Nullable)notes created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("doCopy(id:event_id:type:name:date:time:location:responsible_participant_ids:estimated_cost:actual_cost:servings:status:notes:created_at:updated_at:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedLong * _Nullable actual_cost __attribute__((swift_name("actual_cost")));
@property (readonly) NSString *created_at __attribute__((swift_name("created_at")));
@property (readonly) NSString *date __attribute__((swift_name("date")));
@property (readonly) int64_t estimated_cost __attribute__((swift_name("estimated_cost")));
@property (readonly) NSString *event_id __attribute__((swift_name("event_id")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString * _Nullable location __attribute__((swift_name("location")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) NSString * _Nullable notes __attribute__((swift_name("notes")));
@property (readonly) NSString *responsible_participant_ids __attribute__((swift_name("responsible_participant_ids")));
@property (readonly) int64_t servings __attribute__((swift_name("servings")));
@property (readonly) NSString *status __attribute__((swift_name("status")));
@property (readonly) NSString *time __attribute__((swift_name("time")));
@property (readonly) NSString *type __attribute__((swift_name("type")));
@property (readonly) NSString *updated_at __attribute__((swift_name("updated_at")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MealQueries")))
@interface SharedMealQueries : SharedRuntimeTransacterImpl
- (instancetype)initWithDriver:(id<SharedRuntimeSqlDriver>)driver __attribute__((swift_name("init(driver:)"))) __attribute__((objc_designated_initializer));
- (SharedRuntimeQuery<SharedLong *> *)countCompletedMealsEvent_id:(NSString *)event_id __attribute__((swift_name("countCompletedMeals(event_id:)")));
- (SharedRuntimeQuery<SharedCountMealsByStatus *> *)countMealsByStatusEvent_id:(NSString *)event_id __attribute__((swift_name("countMealsByStatus(event_id:)")));
- (SharedRuntimeQuery<id> *)countMealsByStatusEvent_id:(NSString *)event_id mapper:(id (^)(NSString *, SharedLong *))mapper __attribute__((swift_name("countMealsByStatus(event_id:mapper:)")));
- (SharedRuntimeQuery<SharedCountMealsByType *> *)countMealsByTypeEvent_id:(NSString *)event_id __attribute__((swift_name("countMealsByType(event_id:)")));
- (SharedRuntimeQuery<id> *)countMealsByTypeEvent_id:(NSString *)event_id mapper:(id (^)(NSString *, SharedLong *))mapper __attribute__((swift_name("countMealsByType(event_id:mapper:)")));
- (void)deleteMealId:(NSString *)id __attribute__((swift_name("deleteMeal(id:)")));
- (void)deleteMealsByEventIdEvent_id:(NSString *)event_id __attribute__((swift_name("deleteMealsByEventId(event_id:)")));
- (SharedRuntimeQuery<SharedMeal *> *)getMealByIdId:(NSString *)id __attribute__((swift_name("getMealById(id:)")));
- (SharedRuntimeQuery<id> *)getMealByIdId:(NSString *)id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString *, SharedLong *, SharedLong * _Nullable, SharedLong *, NSString *, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("getMealById(id:mapper:)")));
- (SharedRuntimeQuery<SharedMeal *> *)getMealsByDateEvent_id:(NSString *)event_id date:(NSString *)date __attribute__((swift_name("getMealsByDate(event_id:date:)")));
- (SharedRuntimeQuery<id> *)getMealsByDateEvent_id:(NSString *)event_id date:(NSString *)date mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString *, SharedLong *, SharedLong * _Nullable, SharedLong *, NSString *, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("getMealsByDate(event_id:date:mapper:)")));
- (SharedRuntimeQuery<SharedMeal *> *)getMealsByDateRangeEvent_id:(NSString *)event_id date:(NSString *)date date_:(NSString *)date_ __attribute__((swift_name("getMealsByDateRange(event_id:date:date_:)")));
- (SharedRuntimeQuery<id> *)getMealsByDateRangeEvent_id:(NSString *)event_id date:(NSString *)date date_:(NSString *)date_ mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString *, SharedLong *, SharedLong * _Nullable, SharedLong *, NSString *, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("getMealsByDateRange(event_id:date:date_:mapper:)")));
- (SharedRuntimeQuery<SharedMeal *> *)getMealsByEventIdEvent_id:(NSString *)event_id __attribute__((swift_name("getMealsByEventId(event_id:)")));
- (SharedRuntimeQuery<id> *)getMealsByEventIdEvent_id:(NSString *)event_id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString *, SharedLong *, SharedLong * _Nullable, SharedLong *, NSString *, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("getMealsByEventId(event_id:mapper:)")));
- (SharedRuntimeQuery<SharedMeal *> *)getMealsByStatusEvent_id:(NSString *)event_id status:(NSString *)status __attribute__((swift_name("getMealsByStatus(event_id:status:)")));
- (SharedRuntimeQuery<id> *)getMealsByStatusEvent_id:(NSString *)event_id status:(NSString *)status mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString *, SharedLong *, SharedLong * _Nullable, SharedLong *, NSString *, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("getMealsByStatus(event_id:status:mapper:)")));
- (SharedRuntimeQuery<SharedMeal *> *)getMealsByTypeEvent_id:(NSString *)event_id type:(NSString *)type __attribute__((swift_name("getMealsByType(event_id:type:)")));
- (SharedRuntimeQuery<id> *)getMealsByTypeEvent_id:(NSString *)event_id type:(NSString *)type mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString *, SharedLong *, SharedLong * _Nullable, SharedLong *, NSString *, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("getMealsByType(event_id:type:mapper:)")));
- (SharedRuntimeQuery<SharedMeal *> *)getMealsForParticipantEvent_id:(NSString *)event_id value_:(NSString *)value_ __attribute__((swift_name("getMealsForParticipant(event_id:value_:)")));
- (SharedRuntimeQuery<id> *)getMealsForParticipantEvent_id:(NSString *)event_id value:(NSString *)value mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString *, SharedLong *, SharedLong * _Nullable, SharedLong *, NSString *, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("getMealsForParticipant(event_id:value:mapper:)")));
- (SharedRuntimeQuery<SharedGetTotalActualCost *> *)getTotalActualCostEvent_id:(NSString *)event_id __attribute__((swift_name("getTotalActualCost(event_id:)")));
- (SharedRuntimeQuery<id> *)getTotalActualCostEvent_id:(NSString *)event_id mapper:(id (^)(SharedDouble * _Nullable))mapper __attribute__((swift_name("getTotalActualCost(event_id:mapper:)")));
- (SharedRuntimeQuery<SharedGetTotalEstimatedCost *> *)getTotalEstimatedCostEvent_id:(NSString *)event_id __attribute__((swift_name("getTotalEstimatedCost(event_id:)")));
- (SharedRuntimeQuery<id> *)getTotalEstimatedCostEvent_id:(NSString *)event_id mapper:(id (^)(SharedLong * _Nullable))mapper __attribute__((swift_name("getTotalEstimatedCost(event_id:mapper:)")));
- (SharedRuntimeQuery<SharedMeal *> *)getUpcomingMealsEvent_id:(NSString *)event_id __attribute__((swift_name("getUpcomingMeals(event_id:)")));
- (SharedRuntimeQuery<id> *)getUpcomingMealsEvent_id:(NSString *)event_id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString *, SharedLong *, SharedLong * _Nullable, SharedLong *, NSString *, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("getUpcomingMeals(event_id:mapper:)")));
- (void)insertMealId:(NSString *)id event_id:(NSString *)event_id type:(NSString *)type name:(NSString *)name date:(NSString *)date time:(NSString *)time location:(NSString * _Nullable)location responsible_participant_ids:(NSString *)responsible_participant_ids estimated_cost:(int64_t)estimated_cost actual_cost:(SharedLong * _Nullable)actual_cost servings:(int64_t)servings status:(NSString *)status notes:(NSString * _Nullable)notes created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("insertMeal(id:event_id:type:name:date:time:location:responsible_participant_ids:estimated_cost:actual_cost:servings:status:notes:created_at:updated_at:)")));
- (void)updateMealType:(NSString *)type name:(NSString *)name date:(NSString *)date time:(NSString *)time location:(NSString * _Nullable)location responsible_participant_ids:(NSString *)responsible_participant_ids estimated_cost:(int64_t)estimated_cost actual_cost:(SharedLong * _Nullable)actual_cost servings:(int64_t)servings status:(NSString *)status notes:(NSString * _Nullable)notes updated_at:(NSString *)updated_at id:(NSString *)id __attribute__((swift_name("updateMeal(type:name:date:time:location:responsible_participant_ids:estimated_cost:actual_cost:servings:status:notes:updated_at:id:)")));
- (void)updateMealActualCostActual_cost:(SharedLong * _Nullable)actual_cost updated_at:(NSString *)updated_at id:(NSString *)id __attribute__((swift_name("updateMealActualCost(actual_cost:updated_at:id:)")));
- (void)updateMealStatusStatus:(NSString *)status updated_at:(NSString *)updated_at id:(NSString *)id __attribute__((swift_name("updateMealStatus(status:updated_at:id:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Notification_preference")))
@interface SharedNotification_preference : SharedBase
- (instancetype)initWithId:(NSString *)id user_id:(NSString *)user_id deadline_reminder:(int64_t)deadline_reminder event_update:(int64_t)event_update vote_close_reminder:(int64_t)vote_close_reminder timezone:(NSString *)timezone created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("init(id:user_id:deadline_reminder:event_update:vote_close_reminder:timezone:created_at:updated_at:)"))) __attribute__((objc_designated_initializer));
- (SharedNotification_preference *)doCopyId:(NSString *)id user_id:(NSString *)user_id deadline_reminder:(int64_t)deadline_reminder event_update:(int64_t)event_update vote_close_reminder:(int64_t)vote_close_reminder timezone:(NSString *)timezone created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("doCopy(id:user_id:deadline_reminder:event_update:vote_close_reminder:timezone:created_at:updated_at:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *created_at __attribute__((swift_name("created_at")));
@property (readonly) int64_t deadline_reminder __attribute__((swift_name("deadline_reminder")));
@property (readonly) int64_t event_update __attribute__((swift_name("event_update")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *timezone __attribute__((swift_name("timezone")));
@property (readonly) NSString *updated_at __attribute__((swift_name("updated_at")));
@property (readonly) NSString *user_id __attribute__((swift_name("user_id")));
@property (readonly) int64_t vote_close_reminder __attribute__((swift_name("vote_close_reminder")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Participant")))
@interface SharedParticipant : SharedBase
- (instancetype)initWithId:(NSString *)id eventId:(NSString *)eventId userId:(NSString *)userId role:(NSString *)role hasValidatedDate:(int64_t)hasValidatedDate joinedAt:(NSString *)joinedAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("init(id:eventId:userId:role:hasValidatedDate:joinedAt:updatedAt:)"))) __attribute__((objc_designated_initializer));
- (SharedParticipant *)doCopyId:(NSString *)id eventId:(NSString *)eventId userId:(NSString *)userId role:(NSString *)role hasValidatedDate:(int64_t)hasValidatedDate joinedAt:(NSString *)joinedAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("doCopy(id:eventId:userId:role:hasValidatedDate:joinedAt:updatedAt:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) int64_t hasValidatedDate __attribute__((swift_name("hasValidatedDate")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *joinedAt __attribute__((swift_name("joinedAt")));
@property (readonly) NSString *role __attribute__((swift_name("role")));
@property (readonly) NSString *updatedAt __attribute__((swift_name("updatedAt")));
@property (readonly) NSString *userId __attribute__((swift_name("userId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ParticipantDietaryRestrictionQueries")))
@interface SharedParticipantDietaryRestrictionQueries : SharedRuntimeTransacterImpl
- (instancetype)initWithDriver:(id<SharedRuntimeSqlDriver>)driver __attribute__((swift_name("init(driver:)"))) __attribute__((objc_designated_initializer));
- (SharedRuntimeQuery<SharedCountRestrictionsByType *> *)countRestrictionsByTypeEvent_id:(NSString *)event_id __attribute__((swift_name("countRestrictionsByType(event_id:)")));
- (SharedRuntimeQuery<id> *)countRestrictionsByTypeEvent_id:(NSString *)event_id mapper:(id (^)(NSString *, SharedLong *))mapper __attribute__((swift_name("countRestrictionsByType(event_id:mapper:)")));
- (SharedRuntimeQuery<SharedLong *> *)countTotalRestrictionsEvent_id:(NSString *)event_id __attribute__((swift_name("countTotalRestrictions(event_id:)")));
- (void)deleteDietaryRestrictionId:(NSString *)id __attribute__((swift_name("deleteDietaryRestriction(id:)")));
- (void)deleteRestrictionsForEventEvent_id:(NSString *)event_id __attribute__((swift_name("deleteRestrictionsForEvent(event_id:)")));
- (void)deleteRestrictionsForParticipantParticipant_id:(NSString *)participant_id event_id:(NSString *)event_id __attribute__((swift_name("deleteRestrictionsForParticipant(participant_id:event_id:)")));
- (SharedRuntimeQuery<SharedParticipant_dietary_restriction *> *)getDietaryRestrictionByIdId:(NSString *)id __attribute__((swift_name("getDietaryRestrictionById(id:)")));
- (SharedRuntimeQuery<id> *)getDietaryRestrictionByIdId:(NSString *)id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString *))mapper __attribute__((swift_name("getDietaryRestrictionById(id:mapper:)")));
- (SharedRuntimeQuery<SharedGetParticipantsWithMultipleRestrictions *> *)getParticipantsWithMultipleRestrictionsEvent_id:(NSString *)event_id __attribute__((swift_name("getParticipantsWithMultipleRestrictions(event_id:)")));
- (SharedRuntimeQuery<id> *)getParticipantsWithMultipleRestrictionsEvent_id:(NSString *)event_id mapper:(id (^)(NSString *, SharedLong *))mapper __attribute__((swift_name("getParticipantsWithMultipleRestrictions(event_id:mapper:)")));
- (SharedRuntimeQuery<NSString *> *)getParticipantsWithRestrictionEvent_id:(NSString *)event_id restriction:(NSString *)restriction __attribute__((swift_name("getParticipantsWithRestriction(event_id:restriction:)")));
- (SharedRuntimeQuery<SharedParticipant_dietary_restriction *> *)getRestrictionsByTypeEvent_id:(NSString *)event_id restriction:(NSString *)restriction __attribute__((swift_name("getRestrictionsByType(event_id:restriction:)")));
- (SharedRuntimeQuery<id> *)getRestrictionsByTypeEvent_id:(NSString *)event_id restriction:(NSString *)restriction mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString *))mapper __attribute__((swift_name("getRestrictionsByType(event_id:restriction:mapper:)")));
- (SharedRuntimeQuery<SharedParticipant_dietary_restriction *> *)getRestrictionsForEventEvent_id:(NSString *)event_id __attribute__((swift_name("getRestrictionsForEvent(event_id:)")));
- (SharedRuntimeQuery<id> *)getRestrictionsForEventEvent_id:(NSString *)event_id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString *))mapper __attribute__((swift_name("getRestrictionsForEvent(event_id:mapper:)")));
- (SharedRuntimeQuery<SharedParticipant_dietary_restriction *> *)getRestrictionsForParticipantParticipant_id:(NSString *)participant_id event_id:(NSString *)event_id __attribute__((swift_name("getRestrictionsForParticipant(participant_id:event_id:)")));
- (SharedRuntimeQuery<id> *)getRestrictionsForParticipantParticipant_id:(NSString *)participant_id event_id:(NSString *)event_id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString *))mapper __attribute__((swift_name("getRestrictionsForParticipant(participant_id:event_id:mapper:)")));
- (SharedRuntimeQuery<SharedBoolean *> *)hasRestrictionParticipant_id:(NSString *)participant_id event_id:(NSString *)event_id restriction:(NSString *)restriction __attribute__((swift_name("hasRestriction(participant_id:event_id:restriction:)")));
- (void)insertDietaryRestrictionId:(NSString *)id participant_id:(NSString *)participant_id event_id:(NSString *)event_id restriction:(NSString *)restriction notes:(NSString * _Nullable)notes created_at:(NSString *)created_at __attribute__((swift_name("insertDietaryRestriction(id:participant_id:event_id:restriction:notes:created_at:)")));
- (void)updateRestrictionNotesNotes:(NSString * _Nullable)notes id:(NSString *)id __attribute__((swift_name("updateRestrictionNotes(notes:id:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ParticipantQueries")))
@interface SharedParticipantQueries : SharedRuntimeTransacterImpl
- (instancetype)initWithDriver:(id<SharedRuntimeSqlDriver>)driver __attribute__((swift_name("init(driver:)"))) __attribute__((objc_designated_initializer));
- (void)deleteByEventIdEventId:(NSString *)eventId __attribute__((swift_name("deleteByEventId(eventId:)")));
- (void)deleteParticipantId:(NSString *)id __attribute__((swift_name("deleteParticipant(id:)")));
- (void)insertParticipantId:(NSString *)id eventId:(NSString *)eventId userId:(NSString *)userId role:(NSString *)role hasValidatedDate:(int64_t)hasValidatedDate joinedAt:(NSString *)joinedAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("insertParticipant(id:eventId:userId:role:hasValidatedDate:joinedAt:updatedAt:)")));
- (SharedRuntimeQuery<SharedParticipant *> *)selectAll __attribute__((swift_name("selectAll()")));
- (SharedRuntimeQuery<id> *)selectAllMapper:(id (^)(NSString *, NSString *, NSString *, NSString *, SharedLong *, NSString *, NSString *))mapper __attribute__((swift_name("selectAll(mapper:)")));
- (SharedRuntimeQuery<SharedParticipant *> *)selectByEventIdEventId:(NSString *)eventId __attribute__((swift_name("selectByEventId(eventId:)")));
- (SharedRuntimeQuery<id> *)selectByEventIdEventId:(NSString *)eventId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, SharedLong *, NSString *, NSString *))mapper __attribute__((swift_name("selectByEventId(eventId:mapper:)")));
- (SharedRuntimeQuery<SharedParticipant *> *)selectByEventIdAndUserIdEventId:(NSString *)eventId userId:(NSString *)userId __attribute__((swift_name("selectByEventIdAndUserId(eventId:userId:)")));
- (SharedRuntimeQuery<id> *)selectByEventIdAndUserIdEventId:(NSString *)eventId userId:(NSString *)userId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, SharedLong *, NSString *, NSString *))mapper __attribute__((swift_name("selectByEventIdAndUserId(eventId:userId:mapper:)")));
- (SharedRuntimeQuery<SharedParticipant *> *)selectByIdId:(NSString *)id __attribute__((swift_name("selectById(id:)")));
- (SharedRuntimeQuery<id> *)selectByIdId:(NSString *)id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, SharedLong *, NSString *, NSString *))mapper __attribute__((swift_name("selectById(id:mapper:)")));
- (SharedRuntimeQuery<SharedParticipant *> *)selectByRoleEventId:(NSString *)eventId role:(NSString *)role __attribute__((swift_name("selectByRole(eventId:role:)")));
- (SharedRuntimeQuery<id> *)selectByRoleEventId:(NSString *)eventId role:(NSString *)role mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, SharedLong *, NSString *, NSString *))mapper __attribute__((swift_name("selectByRole(eventId:role:mapper:)")));
- (SharedRuntimeQuery<SharedParticipant *> *)selectValidatedEventId:(NSString *)eventId __attribute__((swift_name("selectValidated(eventId:)")));
- (SharedRuntimeQuery<id> *)selectValidatedEventId:(NSString *)eventId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, SharedLong *, NSString *, NSString *))mapper __attribute__((swift_name("selectValidated(eventId:mapper:)")));
- (void)updateParticipantRole:(NSString *)role hasValidatedDate:(int64_t)hasValidatedDate updatedAt:(NSString *)updatedAt id:(NSString *)id __attribute__((swift_name("updateParticipant(role:hasValidatedDate:updatedAt:id:)")));
- (void)updateValidationHasValidatedDate:(int64_t)hasValidatedDate updatedAt:(NSString *)updatedAt id:(NSString *)id __attribute__((swift_name("updateValidation(hasValidatedDate:updatedAt:id:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Participant_dietary_restriction")))
@interface SharedParticipant_dietary_restriction : SharedBase
- (instancetype)initWithId:(NSString *)id participant_id:(NSString *)participant_id event_id:(NSString *)event_id restriction:(NSString *)restriction notes:(NSString * _Nullable)notes created_at:(NSString *)created_at __attribute__((swift_name("init(id:participant_id:event_id:restriction:notes:created_at:)"))) __attribute__((objc_designated_initializer));
- (SharedParticipant_dietary_restriction *)doCopyId:(NSString *)id participant_id:(NSString *)participant_id event_id:(NSString *)event_id restriction:(NSString *)restriction notes:(NSString * _Nullable)notes created_at:(NSString *)created_at __attribute__((swift_name("doCopy(id:participant_id:event_id:restriction:notes:created_at:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *created_at __attribute__((swift_name("created_at")));
@property (readonly) NSString *event_id __attribute__((swift_name("event_id")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString * _Nullable notes __attribute__((swift_name("notes")));
@property (readonly) NSString *participant_id __attribute__((swift_name("participant_id")));
@property (readonly) NSString *restriction __attribute__((swift_name("restriction")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PollLogic")))
@interface SharedPollLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)pollLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedPollLogic *shared __attribute__((swift_name("shared")));
- (SharedTimeSlot * _Nullable)calculateBestSlotPoll:(SharedPoll *)poll slots:(NSArray<SharedTimeSlot *> *)slots __attribute__((swift_name("calculateBestSlot(poll:slots:)")));
- (SharedKotlinPair<SharedTimeSlot *, SharedPollLogicSlotScore *> * _Nullable)getBestSlotWithScorePoll:(SharedPoll *)poll slots:(NSArray<SharedTimeSlot *> *)slots __attribute__((swift_name("getBestSlotWithScore(poll:slots:)")));
- (NSArray<SharedPollLogicSlotScore *> *)getSlotScoresPoll:(SharedPoll *)poll slots:(NSArray<SharedTimeSlot *> *)slots __attribute__((swift_name("getSlotScores(poll:slots:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PollLogic.SlotScore")))
@interface SharedPollLogicSlotScore : SharedBase
- (instancetype)initWithSlotId:(NSString *)slotId yesCount:(int32_t)yesCount maybeCount:(int32_t)maybeCount noCount:(int32_t)noCount totalScore:(int32_t)totalScore __attribute__((swift_name("init(slotId:yesCount:maybeCount:noCount:totalScore:)"))) __attribute__((objc_designated_initializer));
- (SharedPollLogicSlotScore *)doCopySlotId:(NSString *)slotId yesCount:(int32_t)yesCount maybeCount:(int32_t)maybeCount noCount:(int32_t)noCount totalScore:(int32_t)totalScore __attribute__((swift_name("doCopy(slotId:yesCount:maybeCount:noCount:totalScore:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t maybeCount __attribute__((swift_name("maybeCount")));
@property (readonly) int32_t noCount __attribute__((swift_name("noCount")));
@property (readonly) NSString *slotId __attribute__((swift_name("slotId")));
@property (readonly) int32_t totalScore __attribute__((swift_name("totalScore")));
@property (readonly) int32_t yesCount __attribute__((swift_name("yesCount")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecommendationService")))
@interface SharedRecommendationService : SharedBase
- (instancetype)initWithSuggestionEngine:(id<SharedSuggestionEngine>)suggestionEngine userPreferencesRepository:(SharedUserPreferencesRepository *)userPreferencesRepository __attribute__((swift_name("init(suggestionEngine:userPreferencesRepository:)"))) __attribute__((objc_designated_initializer));
- (NSArray<SharedRecommendation *> *)getActivityRecommendationsEvent:(SharedEvent *)event userId:(NSString *)userId __attribute__((swift_name("getActivityRecommendations(event:userId:)")));
- (NSArray<SharedRecommendation *> *)getDateRecommendationsEvent:(SharedEvent *)event userId:(NSString *)userId __attribute__((swift_name("getDateRecommendations(event:userId:)")));
- (NSArray<SharedRecommendation *> *)getLocationRecommendationsEvent:(SharedEvent *)event userId:(NSString *)userId __attribute__((swift_name("getLocationRecommendations(event:userId:)")));
- (void)updateUserPreferencesUserId:(NSString *)userId preferences:(SharedUserPreferences *)preferences __attribute__((swift_name("updateUserPreferences(userId:preferences:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RoomAssignmentQueries")))
@interface SharedRoomAssignmentQueries : SharedRuntimeTransacterImpl
- (instancetype)initWithDriver:(id<SharedRuntimeSqlDriver>)driver __attribute__((swift_name("init(driver:)"))) __attribute__((objc_designated_initializer));
- (SharedRuntimeQuery<SharedCountAssignedParticipants *> *)countAssignedParticipantsAccommodation_id:(NSString *)accommodation_id __attribute__((swift_name("countAssignedParticipants(accommodation_id:)")));
- (SharedRuntimeQuery<id> *)countAssignedParticipantsAccommodation_id:(NSString *)accommodation_id mapper:(id (^)(NSString *, SharedLong *))mapper __attribute__((swift_name("countAssignedParticipants(accommodation_id:mapper:)")));
- (void)deleteRoomAssignmentId:(NSString *)id __attribute__((swift_name("deleteRoomAssignment(id:)")));
- (void)deleteRoomAssignmentsByAccommodationIdAccommodation_id:(NSString *)accommodation_id __attribute__((swift_name("deleteRoomAssignmentsByAccommodationId(accommodation_id:)")));
- (SharedRuntimeQuery<SharedRoom_assignment *> *)getAvailableRoomsAccommodation_id:(NSString *)accommodation_id __attribute__((swift_name("getAvailableRooms(accommodation_id:)")));
- (SharedRuntimeQuery<id> *)getAvailableRoomsAccommodation_id:(NSString *)accommodation_id mapper:(id (^)(NSString *, NSString *, NSString *, SharedLong *, NSString *, SharedLong *, NSString *, NSString *))mapper __attribute__((swift_name("getAvailableRooms(accommodation_id:mapper:)")));
- (SharedRuntimeQuery<SharedRoom_assignment *> *)getRoomAssignmentByIdId:(NSString *)id __attribute__((swift_name("getRoomAssignmentById(id:)")));
- (SharedRuntimeQuery<id> *)getRoomAssignmentByIdId:(NSString *)id mapper:(id (^)(NSString *, NSString *, NSString *, SharedLong *, NSString *, SharedLong *, NSString *, NSString *))mapper __attribute__((swift_name("getRoomAssignmentById(id:mapper:)")));
- (SharedRuntimeQuery<SharedRoom_assignment *> *)getRoomAssignmentByRoomNumberAccommodation_id:(NSString *)accommodation_id room_number:(NSString *)room_number __attribute__((swift_name("getRoomAssignmentByRoomNumber(accommodation_id:room_number:)")));
- (SharedRuntimeQuery<id> *)getRoomAssignmentByRoomNumberAccommodation_id:(NSString *)accommodation_id room_number:(NSString *)room_number mapper:(id (^)(NSString *, NSString *, NSString *, SharedLong *, NSString *, SharedLong *, NSString *, NSString *))mapper __attribute__((swift_name("getRoomAssignmentByRoomNumber(accommodation_id:room_number:mapper:)")));
- (SharedRuntimeQuery<SharedRoom_assignment *> *)getRoomAssignmentsByAccommodationIdAccommodation_id:(NSString *)accommodation_id __attribute__((swift_name("getRoomAssignmentsByAccommodationId(accommodation_id:)")));
- (SharedRuntimeQuery<id> *)getRoomAssignmentsByAccommodationIdAccommodation_id:(NSString *)accommodation_id mapper:(id (^)(NSString *, NSString *, NSString *, SharedLong *, NSString *, SharedLong *, NSString *, NSString *))mapper __attribute__((swift_name("getRoomAssignmentsByAccommodationId(accommodation_id:mapper:)")));
- (SharedRuntimeQuery<SharedRoom_assignment *> *)getRoomAssignmentsByParticipantValue_:(NSString *)value_ __attribute__((swift_name("getRoomAssignmentsByParticipant(value_:)")));
- (SharedRuntimeQuery<id> *)getRoomAssignmentsByParticipantValue:(NSString *)value mapper:(id (^)(NSString *, NSString *, NSString *, SharedLong *, NSString *, SharedLong *, NSString *, NSString *))mapper __attribute__((swift_name("getRoomAssignmentsByParticipant(value:mapper:)")));
- (SharedRuntimeQuery<SharedGetRoomOccupancyStats *> *)getRoomOccupancyStatsAccommodation_id:(NSString *)accommodation_id __attribute__((swift_name("getRoomOccupancyStats(accommodation_id:)")));
- (SharedRuntimeQuery<id> *)getRoomOccupancyStatsAccommodation_id:(NSString *)accommodation_id mapper:(id (^)(NSString *, SharedLong *, SharedLong * _Nullable))mapper __attribute__((swift_name("getRoomOccupancyStats(accommodation_id:mapper:)")));
- (void)insertRoomAssignmentId:(NSString *)id accommodation_id:(NSString *)accommodation_id room_number:(NSString *)room_number capacity:(int64_t)capacity assigned_participants:(NSString *)assigned_participants price_share:(int64_t)price_share created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("insertRoomAssignment(id:accommodation_id:room_number:capacity:assigned_participants:price_share:created_at:updated_at:)")));
- (void)updateAssignedParticipantsAssigned_participants:(NSString *)assigned_participants updated_at:(NSString *)updated_at id:(NSString *)id __attribute__((swift_name("updateAssignedParticipants(assigned_participants:updated_at:id:)")));
- (void)updateRoomAssignmentRoom_number:(NSString *)room_number capacity:(int64_t)capacity assigned_participants:(NSString *)assigned_participants price_share:(int64_t)price_share updated_at:(NSString *)updated_at id:(NSString *)id __attribute__((swift_name("updateRoomAssignment(room_number:capacity:assigned_participants:price_share:updated_at:id:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Room_assignment")))
@interface SharedRoom_assignment : SharedBase
- (instancetype)initWithId:(NSString *)id accommodation_id:(NSString *)accommodation_id room_number:(NSString *)room_number capacity:(int64_t)capacity assigned_participants:(NSString *)assigned_participants price_share:(int64_t)price_share created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("init(id:accommodation_id:room_number:capacity:assigned_participants:price_share:created_at:updated_at:)"))) __attribute__((objc_designated_initializer));
- (SharedRoom_assignment *)doCopyId:(NSString *)id accommodation_id:(NSString *)accommodation_id room_number:(NSString *)room_number capacity:(int64_t)capacity assigned_participants:(NSString *)assigned_participants price_share:(int64_t)price_share created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("doCopy(id:accommodation_id:room_number:capacity:assigned_participants:price_share:created_at:updated_at:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *accommodation_id __attribute__((swift_name("accommodation_id")));
@property (readonly) NSString *assigned_participants __attribute__((swift_name("assigned_participants")));
@property (readonly) int64_t capacity __attribute__((swift_name("capacity")));
@property (readonly) NSString *created_at __attribute__((swift_name("created_at")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) int64_t price_share __attribute__((swift_name("price_share")));
@property (readonly) NSString *room_number __attribute__((swift_name("room_number")));
@property (readonly) NSString *updated_at __attribute__((swift_name("updated_at")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Scenario")))
@interface SharedScenario : SharedBase
- (instancetype)initWithId:(NSString *)id eventId:(NSString *)eventId name:(NSString *)name dateOrPeriod:(NSString *)dateOrPeriod location:(NSString *)location duration:(int64_t)duration estimatedParticipants:(int64_t)estimatedParticipants estimatedBudgetPerPerson:(double)estimatedBudgetPerPerson description:(NSString *)description status:(NSString *)status createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("init(id:eventId:name:dateOrPeriod:location:duration:estimatedParticipants:estimatedBudgetPerPerson:description:status:createdAt:updatedAt:)"))) __attribute__((objc_designated_initializer));
- (SharedScenario *)doCopyId:(NSString *)id eventId:(NSString *)eventId name:(NSString *)name dateOrPeriod:(NSString *)dateOrPeriod location:(NSString *)location duration:(int64_t)duration estimatedParticipants:(int64_t)estimatedParticipants estimatedBudgetPerPerson:(double)estimatedBudgetPerPerson description:(NSString *)description status:(NSString *)status createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("doCopy(id:eventId:name:dateOrPeriod:location:duration:estimatedParticipants:estimatedBudgetPerPerson:description:status:createdAt:updatedAt:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *dateOrPeriod __attribute__((swift_name("dateOrPeriod")));
@property (readonly) NSString *description_ __attribute__((swift_name("description_")));
@property (readonly) int64_t duration __attribute__((swift_name("duration")));
@property (readonly) double estimatedBudgetPerPerson __attribute__((swift_name("estimatedBudgetPerPerson")));
@property (readonly) int64_t estimatedParticipants __attribute__((swift_name("estimatedParticipants")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *location __attribute__((swift_name("location")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) NSString *status __attribute__((swift_name("status")));
@property (readonly) NSString *updatedAt __attribute__((swift_name("updatedAt")));
@end


/**
 * Business logic for scenario voting and ranking.
 * Similar to PollLogic but for planning scenarios instead of time slots.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScenarioLogic")))
@interface SharedScenarioLogic : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Business logic for scenario voting and ranking.
 * Similar to PollLogic but for planning scenarios instead of time slots.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)scenarioLogic __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedScenarioLogic *shared __attribute__((swift_name("shared")));

/**
 * Calculate the best scenario based on participant votes.
 * Scoring: PREFER = 2 points, NEUTRAL = 1 point, AGAINST = -1 point
 *
 * @param scenarios List of scenarios to evaluate
 * @param votes List of all votes cast on these scenarios
 * @return The scenario with the highest score, or null if no scenarios provided
 */
- (SharedScenario_ * _Nullable)calculateBestScenarioScenarios:(NSArray<SharedScenario_ *> *)scenarios votes:(NSArray<SharedScenarioVote *> *)votes __attribute__((swift_name("calculateBestScenario(scenarios:votes:)")));

/**
 * Get the best scenario along with its voting details.
 *
 * @param scenarios List of scenarios to evaluate
 * @param votes List of all votes cast on these scenarios
 * @return Pair of best scenario and its voting result, or null if no scenarios
 */
- (SharedKotlinPair<SharedScenario_ *, SharedScenarioVotingResult *> * _Nullable)getBestScenarioWithScoreScenarios:(NSArray<SharedScenario_ *> *)scenarios votes:(NSArray<SharedScenarioVote *> *)votes __attribute__((swift_name("getBestScenarioWithScore(scenarios:votes:)")));

/**
 * Get voting results for all scenarios.
 *
 * @param scenarios List of scenarios to evaluate
 * @param votes List of all votes cast on these scenarios
 * @return List of voting results with counts and scores
 */
- (NSArray<SharedScenarioVotingResult *> *)getScenarioVotingResultsScenarios:(NSArray<SharedScenario_ *> *)scenarios votes:(NSArray<SharedScenarioVote *> *)votes __attribute__((swift_name("getScenarioVotingResults(scenarios:votes:)")));

/**
 * Rank scenarios by their vote scores in descending order.
 *
 * @param scenarios List of scenarios to rank
 * @param votes List of all votes cast on these scenarios
 * @return List of scenarios with their votes, sorted by score (highest first)
 */
- (NSArray<SharedScenarioWithVotes *> *)rankScenariosByScoreScenarios:(NSArray<SharedScenario_ *> *)scenarios votes:(NSArray<SharedScenarioVote *> *)votes __attribute__((swift_name("rankScenariosByScore(scenarios:votes:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScenarioQueries")))
@interface SharedScenarioQueries : SharedRuntimeTransacterImpl
- (instancetype)initWithDriver:(id<SharedRuntimeSqlDriver>)driver __attribute__((swift_name("init(driver:)"))) __attribute__((objc_designated_initializer));
- (SharedRuntimeQuery<SharedLong *> *)countByEventIdEventId:(NSString *)eventId __attribute__((swift_name("countByEventId(eventId:)")));
- (SharedRuntimeQuery<SharedLong *> *)countByEventIdAndStatusEventId:(NSString *)eventId status:(NSString *)status __attribute__((swift_name("countByEventIdAndStatus(eventId:status:)")));
- (void)deleteByEventIdEventId:(NSString *)eventId __attribute__((swift_name("deleteByEventId(eventId:)")));
- (void)deleteScenarioId:(NSString *)id __attribute__((swift_name("deleteScenario(id:)")));
- (void)insertScenarioId:(NSString *)id eventId:(NSString *)eventId name:(NSString *)name dateOrPeriod:(NSString *)dateOrPeriod location:(NSString *)location duration:(int64_t)duration estimatedParticipants:(int64_t)estimatedParticipants estimatedBudgetPerPerson:(double)estimatedBudgetPerPerson description:(NSString *)description status:(NSString *)status createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("insertScenario(id:eventId:name:dateOrPeriod:location:duration:estimatedParticipants:estimatedBudgetPerPerson:description:status:createdAt:updatedAt:)")));
- (SharedRuntimeQuery<SharedScenario *> *)selectAll __attribute__((swift_name("selectAll()")));
- (SharedRuntimeQuery<id> *)selectAllMapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, SharedLong *, SharedLong *, SharedDouble *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectAll(mapper:)")));
- (SharedRuntimeQuery<SharedScenario *> *)selectByEventIdEventId:(NSString *)eventId __attribute__((swift_name("selectByEventId(eventId:)")));
- (SharedRuntimeQuery<id> *)selectByEventIdEventId:(NSString *)eventId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, SharedLong *, SharedLong *, SharedDouble *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectByEventId(eventId:mapper:)")));
- (SharedRuntimeQuery<SharedScenario *> *)selectByEventIdAndStatusEventId:(NSString *)eventId status:(NSString *)status __attribute__((swift_name("selectByEventIdAndStatus(eventId:status:)")));
- (SharedRuntimeQuery<id> *)selectByEventIdAndStatusEventId:(NSString *)eventId status:(NSString *)status mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, SharedLong *, SharedLong *, SharedDouble *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectByEventIdAndStatus(eventId:status:mapper:)")));
- (SharedRuntimeQuery<SharedScenario *> *)selectByIdId:(NSString *)id __attribute__((swift_name("selectById(id:)")));
- (SharedRuntimeQuery<id> *)selectByIdId:(NSString *)id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, SharedLong *, SharedLong *, SharedDouble *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectById(id:mapper:)")));
- (SharedRuntimeQuery<SharedScenario *> *)selectProposedByEventIdEventId:(NSString *)eventId __attribute__((swift_name("selectProposedByEventId(eventId:)")));
- (SharedRuntimeQuery<id> *)selectProposedByEventIdEventId:(NSString *)eventId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, SharedLong *, SharedLong *, SharedDouble *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectProposedByEventId(eventId:mapper:)")));
- (SharedRuntimeQuery<SharedScenario *> *)selectSelectedByEventIdEventId:(NSString *)eventId __attribute__((swift_name("selectSelectedByEventId(eventId:)")));
- (SharedRuntimeQuery<id> *)selectSelectedByEventIdEventId:(NSString *)eventId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, SharedLong *, SharedLong *, SharedDouble *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectSelectedByEventId(eventId:mapper:)")));
- (void)updateScenarioName:(NSString *)name dateOrPeriod:(NSString *)dateOrPeriod location:(NSString *)location duration:(int64_t)duration estimatedParticipants:(int64_t)estimatedParticipants estimatedBudgetPerPerson:(double)estimatedBudgetPerPerson description:(NSString *)description updatedAt:(NSString *)updatedAt id:(NSString *)id __attribute__((swift_name("updateScenario(name:dateOrPeriod:location:duration:estimatedParticipants:estimatedBudgetPerPerson:description:updatedAt:id:)")));
- (void)updateScenarioStatusStatus:(NSString *)status updatedAt:(NSString *)updatedAt id:(NSString *)id __attribute__((swift_name("updateScenarioStatus(status:updatedAt:id:)")));
@end


/**
 * Repository for managing scenarios and scenario votes in the database.
 * Provides CRUD operations and voting functionality for event planning scenarios.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScenarioRepository")))
@interface SharedScenarioRepository : SharedBase
- (instancetype)initWithDb:(id<SharedWakevDb>)db __attribute__((swift_name("init(db:)"))) __attribute__((objc_designated_initializer));

/**
 * Add or update a vote for a scenario.
 * If the participant has already voted, their vote is updated.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)addVoteVote:(SharedScenarioVote *)vote completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("addVote(vote:completionHandler:)")));

/**
 * Count scenarios for an event.
 */
- (int64_t)countScenariosEventId:(NSString *)eventId __attribute__((swift_name("countScenarios(eventId:)")));

/**
 * Count scenarios by status.
 */
- (int64_t)countScenariosByStatusEventId:(NSString *)eventId status:(SharedScenarioStatus *)status __attribute__((swift_name("countScenariosByStatus(eventId:status:)")));

/**
 * Create a new scenario in the database.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)createScenarioScenario:(SharedScenario_ *)scenario completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("createScenario(scenario:completionHandler:)")));

/**
 * Delete a scenario (cascade deletes votes).
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)deleteScenarioScenarioId:(NSString *)scenarioId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("deleteScenario(scenarioId:completionHandler:)")));

/**
 * Delete a vote.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)deleteVoteScenarioId:(NSString *)scenarioId participantId:(NSString *)participantId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("deleteVote(scenarioId:participantId:completionHandler:)")));

/**
 * Get a scenario by its ID.
 */
- (SharedScenario_ * _Nullable)getScenarioByIdId:(NSString *)id __attribute__((swift_name("getScenarioById(id:)")));

/**
 * Get all scenarios for a specific event.
 */
- (NSArray<SharedScenario_ *> *)getScenariosByEventIdEventId:(NSString *)eventId __attribute__((swift_name("getScenariosByEventId(eventId:)")));

/**
 * Get scenarios by event ID and status.
 */
- (NSArray<SharedScenario_ *> *)getScenariosByEventIdAndStatusEventId:(NSString *)eventId status:(SharedScenarioStatus *)status __attribute__((swift_name("getScenariosByEventIdAndStatus(eventId:status:)")));

/**
 * Get scenarios with their votes and voting results.
 */
- (NSArray<SharedScenarioWithVotes *> *)getScenariosWithVotesEventId:(NSString *)eventId __attribute__((swift_name("getScenariosWithVotes(eventId:)")));

/**
 * Get the selected scenario for an event (if any).
 */
- (SharedScenario_ * _Nullable)getSelectedScenarioEventId:(NSString *)eventId __attribute__((swift_name("getSelectedScenario(eventId:)")));

/**
 * Get all votes for a scenario.
 */
- (NSArray<SharedScenarioVote *> *)getVotesByScenarioIdScenarioId:(NSString *)scenarioId __attribute__((swift_name("getVotesByScenarioId(scenarioId:)")));

/**
 * Get voting result for a scenario.
 */
- (SharedScenarioVotingResult * _Nullable)getVotingResultScenarioId:(NSString *)scenarioId __attribute__((swift_name("getVotingResult(scenarioId:)")));

/**
 * Update an existing scenario.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)updateScenarioScenario:(SharedScenario_ *)scenario completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("updateScenario(scenario:completionHandler:)")));

/**
 * Update the status of a scenario.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)updateScenarioStatusScenarioId:(NSString *)scenarioId status:(SharedScenarioStatus *)status completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("updateScenarioStatus(scenarioId:status:completionHandler:)")));

/**
 * Update an existing vote.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)updateVoteVote:(SharedScenarioVote *)vote completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("updateVote(vote:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScenarioVoteQueries")))
@interface SharedScenarioVoteQueries : SharedRuntimeTransacterImpl
- (instancetype)initWithDriver:(id<SharedRuntimeSqlDriver>)driver __attribute__((swift_name("init(driver:)"))) __attribute__((objc_designated_initializer));
- (SharedRuntimeQuery<SharedLong *> *)countByScenarioIdScenarioId:(NSString *)scenarioId __attribute__((swift_name("countByScenarioId(scenarioId:)")));
- (SharedRuntimeQuery<SharedLong *> *)countByScenarioIdAndVoteScenarioId:(NSString *)scenarioId vote:(NSString *)vote __attribute__((swift_name("countByScenarioIdAndVote(scenarioId:vote:)")));
- (void)deleteByParticipantIdParticipantId:(NSString *)participantId __attribute__((swift_name("deleteByParticipantId(participantId:)")));
- (void)deleteByScenarioIdScenarioId:(NSString *)scenarioId __attribute__((swift_name("deleteByScenarioId(scenarioId:)")));
- (void)deleteByScenarioIdAndParticipantIdScenarioId:(NSString *)scenarioId participantId:(NSString *)participantId __attribute__((swift_name("deleteByScenarioIdAndParticipantId(scenarioId:participantId:)")));
- (void)deleteScenarioVoteId:(NSString *)id __attribute__((swift_name("deleteScenarioVote(id:)")));
- (void)insertScenarioVoteId:(NSString *)id scenarioId:(NSString *)scenarioId participantId:(NSString *)participantId vote:(NSString *)vote createdAt:(NSString *)createdAt __attribute__((swift_name("insertScenarioVote(id:scenarioId:participantId:vote:createdAt:)")));
- (SharedRuntimeQuery<SharedScenario_vote *> *)selectAll __attribute__((swift_name("selectAll()")));
- (SharedRuntimeQuery<id> *)selectAllMapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectAll(mapper:)")));
- (SharedRuntimeQuery<SharedScenario_vote *> *)selectByIdId:(NSString *)id __attribute__((swift_name("selectById(id:)")));
- (SharedRuntimeQuery<id> *)selectByIdId:(NSString *)id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectById(id:mapper:)")));
- (SharedRuntimeQuery<SharedScenario_vote *> *)selectByParticipantIdParticipantId:(NSString *)participantId __attribute__((swift_name("selectByParticipantId(participantId:)")));
- (SharedRuntimeQuery<id> *)selectByParticipantIdParticipantId:(NSString *)participantId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectByParticipantId(participantId:mapper:)")));
- (SharedRuntimeQuery<SharedScenario_vote *> *)selectByScenarioIdScenarioId:(NSString *)scenarioId __attribute__((swift_name("selectByScenarioId(scenarioId:)")));
- (SharedRuntimeQuery<id> *)selectByScenarioIdScenarioId:(NSString *)scenarioId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectByScenarioId(scenarioId:mapper:)")));
- (SharedRuntimeQuery<SharedScenario_vote *> *)selectByScenarioIdAndParticipantIdScenarioId:(NSString *)scenarioId participantId:(NSString *)participantId __attribute__((swift_name("selectByScenarioIdAndParticipantId(scenarioId:participantId:)")));
- (SharedRuntimeQuery<id> *)selectByScenarioIdAndParticipantIdScenarioId:(NSString *)scenarioId participantId:(NSString *)participantId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectByScenarioIdAndParticipantId(scenarioId:participantId:mapper:)")));
- (SharedRuntimeQuery<SharedSelectVotingResultByScenarioId *> *)selectVotingResultByScenarioIdScenarioId:(NSString *)scenarioId __attribute__((swift_name("selectVotingResultByScenarioId(scenarioId:)")));
- (SharedRuntimeQuery<id> *)selectVotingResultByScenarioIdScenarioId:(NSString *)scenarioId mapper:(id (^)(NSString *, SharedLong * _Nullable, SharedLong * _Nullable, SharedLong * _Nullable, SharedLong *))mapper __attribute__((swift_name("selectVotingResultByScenarioId(scenarioId:mapper:)")));
- (void)updateScenarioVoteVote:(NSString *)vote scenarioId:(NSString *)scenarioId participantId:(NSString *)participantId __attribute__((swift_name("updateScenarioVote(vote:scenarioId:participantId:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Scenario_vote")))
@interface SharedScenario_vote : SharedBase
- (instancetype)initWithId:(NSString *)id scenarioId:(NSString *)scenarioId participantId:(NSString *)participantId vote:(NSString *)vote createdAt:(NSString *)createdAt __attribute__((swift_name("init(id:scenarioId:participantId:vote:createdAt:)"))) __attribute__((objc_designated_initializer));
- (SharedScenario_vote *)doCopyId:(NSString *)id scenarioId:(NSString *)scenarioId participantId:(NSString *)participantId vote:(NSString *)vote createdAt:(NSString *)createdAt __attribute__((swift_name("doCopy(id:scenarioId:participantId:vote:createdAt:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *participantId __attribute__((swift_name("participantId")));
@property (readonly) NSString *scenarioId __attribute__((swift_name("scenarioId")));
@property (readonly) NSString *vote __attribute__((swift_name("vote")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SelectActivitiesByDateGrouped")))
@interface SharedSelectActivitiesByDateGrouped : SharedBase
- (instancetype)initWithDate:(NSString *)date activityCount:(int64_t)activityCount totalCost:(double)totalCost __attribute__((swift_name("init(date:activityCount:totalCost:)"))) __attribute__((objc_designated_initializer));
- (SharedSelectActivitiesByDateGrouped *)doCopyDate:(NSString *)date activityCount:(int64_t)activityCount totalCost:(double)totalCost __attribute__((swift_name("doCopy(date:activityCount:totalCost:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t activityCount __attribute__((swift_name("activityCount")));
@property (readonly) NSString *date __attribute__((swift_name("date")));
@property (readonly) double totalCost __attribute__((swift_name("totalCost")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SelectEquipmentOverallStats")))
@interface SharedSelectEquipmentOverallStats : SharedBase
- (instancetype)initWithTotalItems:(int64_t)totalItems assignedItems:(int64_t)assignedItems confirmedItems:(int64_t)confirmedItems packedItems:(int64_t)packedItems totalCost:(double)totalCost __attribute__((swift_name("init(totalItems:assignedItems:confirmedItems:packedItems:totalCost:)"))) __attribute__((objc_designated_initializer));
- (SharedSelectEquipmentOverallStats *)doCopyTotalItems:(int64_t)totalItems assignedItems:(int64_t)assignedItems confirmedItems:(int64_t)confirmedItems packedItems:(int64_t)packedItems totalCost:(double)totalCost __attribute__((swift_name("doCopy(totalItems:assignedItems:confirmedItems:packedItems:totalCost:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t assignedItems __attribute__((swift_name("assignedItems")));
@property (readonly) int64_t confirmedItems __attribute__((swift_name("confirmedItems")));
@property (readonly) int64_t packedItems __attribute__((swift_name("packedItems")));
@property (readonly) double totalCost __attribute__((swift_name("totalCost")));
@property (readonly) int64_t totalItems __attribute__((swift_name("totalItems")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SelectEquipmentStatsByAssignee")))
@interface SharedSelectEquipmentStatsByAssignee : SharedBase
- (instancetype)initWithAssigned_to:(NSString *)assigned_to itemCount:(int64_t)itemCount confirmedCount:(int64_t)confirmedCount packedCount:(int64_t)packedCount totalValue:(double)totalValue __attribute__((swift_name("init(assigned_to:itemCount:confirmedCount:packedCount:totalValue:)"))) __attribute__((objc_designated_initializer));
- (SharedSelectEquipmentStatsByAssignee *)doCopyAssigned_to:(NSString *)assigned_to itemCount:(int64_t)itemCount confirmedCount:(int64_t)confirmedCount packedCount:(int64_t)packedCount totalValue:(double)totalValue __attribute__((swift_name("doCopy(assigned_to:itemCount:confirmedCount:packedCount:totalValue:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *assigned_to __attribute__((swift_name("assigned_to")));
@property (readonly) int64_t confirmedCount __attribute__((swift_name("confirmedCount")));
@property (readonly) int64_t itemCount __attribute__((swift_name("itemCount")));
@property (readonly) int64_t packedCount __attribute__((swift_name("packedCount")));
@property (readonly) double totalValue __attribute__((swift_name("totalValue")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SelectEquipmentStatsByCategory")))
@interface SharedSelectEquipmentStatsByCategory : SharedBase
- (instancetype)initWithCategory:(NSString *)category itemCount:(int64_t)itemCount assignedCount:(int64_t)assignedCount confirmedCount:(int64_t)confirmedCount packedCount:(int64_t)packedCount totalCost:(double)totalCost __attribute__((swift_name("init(category:itemCount:assignedCount:confirmedCount:packedCount:totalCost:)"))) __attribute__((objc_designated_initializer));
- (SharedSelectEquipmentStatsByCategory *)doCopyCategory:(NSString *)category itemCount:(int64_t)itemCount assignedCount:(int64_t)assignedCount confirmedCount:(int64_t)confirmedCount packedCount:(int64_t)packedCount totalCost:(double)totalCost __attribute__((swift_name("doCopy(category:itemCount:assignedCount:confirmedCount:packedCount:totalCost:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t assignedCount __attribute__((swift_name("assignedCount")));
@property (readonly) NSString *category __attribute__((swift_name("category")));
@property (readonly) int64_t confirmedCount __attribute__((swift_name("confirmedCount")));
@property (readonly) int64_t itemCount __attribute__((swift_name("itemCount")));
@property (readonly) int64_t packedCount __attribute__((swift_name("packedCount")));
@property (readonly) double totalCost __attribute__((swift_name("totalCost")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SelectVotesByTimeslot")))
@interface SharedSelectVotesByTimeslot : SharedBase
- (instancetype)initWithId:(NSString *)id eventId:(NSString *)eventId timeslotId:(NSString *)timeslotId participantId:(NSString *)participantId vote:(NSString *)vote createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt userId:(NSString *)userId __attribute__((swift_name("init(id:eventId:timeslotId:participantId:vote:createdAt:updatedAt:userId:)"))) __attribute__((objc_designated_initializer));
- (SharedSelectVotesByTimeslot *)doCopyId:(NSString *)id eventId:(NSString *)eventId timeslotId:(NSString *)timeslotId participantId:(NSString *)participantId vote:(NSString *)vote createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt userId:(NSString *)userId __attribute__((swift_name("doCopy(id:eventId:timeslotId:participantId:vote:createdAt:updatedAt:userId:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *participantId __attribute__((swift_name("participantId")));
@property (readonly) NSString *timeslotId __attribute__((swift_name("timeslotId")));
@property (readonly) NSString *updatedAt __attribute__((swift_name("updatedAt")));
@property (readonly) NSString *userId __attribute__((swift_name("userId")));
@property (readonly) NSString *vote __attribute__((swift_name("vote")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SelectVotesForEventTimeslots")))
@interface SharedSelectVotesForEventTimeslots : SharedBase
- (instancetype)initWithId:(NSString *)id eventId:(NSString *)eventId timeslotId:(NSString *)timeslotId participantId:(NSString *)participantId vote:(NSString *)vote createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt eventId_:(NSString *)eventId_ userId:(NSString *)userId __attribute__((swift_name("init(id:eventId:timeslotId:participantId:vote:createdAt:updatedAt:eventId_:userId:)"))) __attribute__((objc_designated_initializer));
- (SharedSelectVotesForEventTimeslots *)doCopyId:(NSString *)id eventId:(NSString *)eventId timeslotId:(NSString *)timeslotId participantId:(NSString *)participantId vote:(NSString *)vote createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt eventId_:(NSString *)eventId_ userId:(NSString *)userId __attribute__((swift_name("doCopy(id:eventId:timeslotId:participantId:vote:createdAt:updatedAt:eventId_:userId:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *eventId_ __attribute__((swift_name("eventId_")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *participantId __attribute__((swift_name("participantId")));
@property (readonly) NSString *timeslotId __attribute__((swift_name("timeslotId")));
@property (readonly) NSString *updatedAt __attribute__((swift_name("updatedAt")));
@property (readonly) NSString *userId __attribute__((swift_name("userId")));
@property (readonly) NSString *vote __attribute__((swift_name("vote")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SelectVotingResultByScenarioId")))
@interface SharedSelectVotingResultByScenarioId : SharedBase
- (instancetype)initWithScenarioId:(NSString *)scenarioId preferCount:(SharedLong * _Nullable)preferCount neutralCount:(SharedLong * _Nullable)neutralCount againstCount:(SharedLong * _Nullable)againstCount totalVotes:(int64_t)totalVotes __attribute__((swift_name("init(scenarioId:preferCount:neutralCount:againstCount:totalVotes:)"))) __attribute__((objc_designated_initializer));
- (SharedSelectVotingResultByScenarioId *)doCopyScenarioId:(NSString *)scenarioId preferCount:(SharedLong * _Nullable)preferCount neutralCount:(SharedLong * _Nullable)neutralCount againstCount:(SharedLong * _Nullable)againstCount totalVotes:(int64_t)totalVotes __attribute__((swift_name("doCopy(scenarioId:preferCount:neutralCount:againstCount:totalVotes:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedLong * _Nullable againstCount __attribute__((swift_name("againstCount")));
@property (readonly) SharedLong * _Nullable neutralCount __attribute__((swift_name("neutralCount")));
@property (readonly) SharedLong * _Nullable preferCount __attribute__((swift_name("preferCount")));
@property (readonly) NSString *scenarioId __attribute__((swift_name("scenarioId")));
@property (readonly) int64_t totalVotes __attribute__((swift_name("totalVotes")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SelectWithTimeslotDetails")))
@interface SharedSelectWithTimeslotDetails : SharedBase
- (instancetype)initWithId:(NSString *)id eventId:(NSString *)eventId timeslotId:(NSString *)timeslotId confirmedByOrganizerId:(NSString *)confirmedByOrganizerId confirmedAt:(NSString *)confirmedAt updatedAt:(NSString *)updatedAt startTime:(NSString *)startTime endTime:(NSString *)endTime timezone:(NSString *)timezone __attribute__((swift_name("init(id:eventId:timeslotId:confirmedByOrganizerId:confirmedAt:updatedAt:startTime:endTime:timezone:)"))) __attribute__((objc_designated_initializer));
- (SharedSelectWithTimeslotDetails *)doCopyId:(NSString *)id eventId:(NSString *)eventId timeslotId:(NSString *)timeslotId confirmedByOrganizerId:(NSString *)confirmedByOrganizerId confirmedAt:(NSString *)confirmedAt updatedAt:(NSString *)updatedAt startTime:(NSString *)startTime endTime:(NSString *)endTime timezone:(NSString *)timezone __attribute__((swift_name("doCopy(id:eventId:timeslotId:confirmedByOrganizerId:confirmedAt:updatedAt:startTime:endTime:timezone:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *confirmedAt __attribute__((swift_name("confirmedAt")));
@property (readonly) NSString *confirmedByOrganizerId __attribute__((swift_name("confirmedByOrganizerId")));
@property (readonly) NSString *endTime __attribute__((swift_name("endTime")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *startTime __attribute__((swift_name("startTime")));
@property (readonly) NSString *timeslotId __attribute__((swift_name("timeslotId")));
@property (readonly) NSString *timezone __attribute__((swift_name("timezone")));
@property (readonly) NSString *updatedAt __attribute__((swift_name("updatedAt")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Session")))
@interface SharedSession : SharedBase
- (instancetype)initWithId:(NSString *)id user_id:(NSString *)user_id device_id:(NSString *)device_id device_name:(NSString *)device_name jwt_token_hash:(NSString *)jwt_token_hash refresh_token_hash:(NSString *)refresh_token_hash ip_address:(NSString * _Nullable)ip_address user_agent:(NSString * _Nullable)user_agent created_at:(NSString *)created_at last_accessed:(NSString *)last_accessed expires_at:(NSString *)expires_at status:(NSString *)status __attribute__((swift_name("init(id:user_id:device_id:device_name:jwt_token_hash:refresh_token_hash:ip_address:user_agent:created_at:last_accessed:expires_at:status:)"))) __attribute__((objc_designated_initializer));
- (SharedSession *)doCopyId:(NSString *)id user_id:(NSString *)user_id device_id:(NSString *)device_id device_name:(NSString *)device_name jwt_token_hash:(NSString *)jwt_token_hash refresh_token_hash:(NSString *)refresh_token_hash ip_address:(NSString * _Nullable)ip_address user_agent:(NSString * _Nullable)user_agent created_at:(NSString *)created_at last_accessed:(NSString *)last_accessed expires_at:(NSString *)expires_at status:(NSString *)status __attribute__((swift_name("doCopy(id:user_id:device_id:device_name:jwt_token_hash:refresh_token_hash:ip_address:user_agent:created_at:last_accessed:expires_at:status:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *created_at __attribute__((swift_name("created_at")));
@property (readonly) NSString *device_id __attribute__((swift_name("device_id")));
@property (readonly) NSString *device_name __attribute__((swift_name("device_name")));
@property (readonly) NSString *expires_at __attribute__((swift_name("expires_at")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString * _Nullable ip_address __attribute__((swift_name("ip_address")));
@property (readonly) NSString *jwt_token_hash __attribute__((swift_name("jwt_token_hash")));
@property (readonly) NSString *last_accessed __attribute__((swift_name("last_accessed")));
@property (readonly) NSString *refresh_token_hash __attribute__((swift_name("refresh_token_hash")));
@property (readonly) NSString *status __attribute__((swift_name("status")));
@property (readonly) NSString * _Nullable user_agent __attribute__((swift_name("user_agent")));
@property (readonly) NSString *user_id __attribute__((swift_name("user_id")));
@end


/**
 * Session data model for application use.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SessionData")))
@interface SharedSessionData : SharedBase
- (instancetype)initWithId:(NSString *)id userId:(NSString *)userId deviceId:(NSString *)deviceId deviceName:(NSString *)deviceName jwtTokenHash:(NSString *)jwtTokenHash refreshTokenHash:(NSString *)refreshTokenHash ipAddress:(NSString * _Nullable)ipAddress userAgent:(NSString * _Nullable)userAgent createdAt:(NSString *)createdAt lastAccessed:(NSString *)lastAccessed expiresAt:(NSString *)expiresAt status:(NSString *)status __attribute__((swift_name("init(id:userId:deviceId:deviceName:jwtTokenHash:refreshTokenHash:ipAddress:userAgent:createdAt:lastAccessed:expiresAt:status:)"))) __attribute__((objc_designated_initializer));
- (SharedSessionData *)doCopyId:(NSString *)id userId:(NSString *)userId deviceId:(NSString *)deviceId deviceName:(NSString *)deviceName jwtTokenHash:(NSString *)jwtTokenHash refreshTokenHash:(NSString *)refreshTokenHash ipAddress:(NSString * _Nullable)ipAddress userAgent:(NSString * _Nullable)userAgent createdAt:(NSString *)createdAt lastAccessed:(NSString *)lastAccessed expiresAt:(NSString *)expiresAt status:(NSString *)status __attribute__((swift_name("doCopy(id:userId:deviceId:deviceName:jwtTokenHash:refreshTokenHash:ipAddress:userAgent:createdAt:lastAccessed:expiresAt:status:)")));

/**
 * Session data model for application use.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Session data model for application use.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Session data model for application use.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *deviceId __attribute__((swift_name("deviceId")));
@property (readonly) NSString *deviceName __attribute__((swift_name("deviceName")));
@property (readonly) NSString *expiresAt __attribute__((swift_name("expiresAt")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString * _Nullable ipAddress __attribute__((swift_name("ipAddress")));
@property (readonly) NSString *jwtTokenHash __attribute__((swift_name("jwtTokenHash")));
@property (readonly) NSString *lastAccessed __attribute__((swift_name("lastAccessed")));
@property (readonly) NSString *refreshTokenHash __attribute__((swift_name("refreshTokenHash")));
@property (readonly) NSString *status __attribute__((swift_name("status")));
@property (readonly) NSString * _Nullable userAgent __attribute__((swift_name("userAgent")));
@property (readonly) NSString *userId __attribute__((swift_name("userId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SessionQueries")))
@interface SharedSessionQueries : SharedRuntimeTransacterImpl
- (instancetype)initWithDriver:(id<SharedRuntimeSqlDriver>)driver __attribute__((swift_name("init(driver:)"))) __attribute__((objc_designated_initializer));
- (void)cleanupExpiredBlacklistExpires_at:(NSString *)expires_at __attribute__((swift_name("cleanupExpiredBlacklist(expires_at:)")));
- (SharedRuntimeQuery<SharedLong *> *)countActiveSessionsByUserIdUser_id:(NSString *)user_id expires_at:(NSString *)expires_at __attribute__((swift_name("countActiveSessionsByUserId(user_id:expires_at:)")));
- (void)deleteAllUserDevicesUser_id:(NSString *)user_id __attribute__((swift_name("deleteAllUserDevices(user_id:)")));
- (void)deleteDeviceId:(NSString *)id __attribute__((swift_name("deleteDevice(id:)")));
- (void)deleteOldSessionsCreated_at:(NSString *)created_at __attribute__((swift_name("deleteOldSessions(created_at:)")));
- (void)insertBlacklistedTokenToken_hash:(NSString *)token_hash user_id:(NSString *)user_id revoked_at:(NSString *)revoked_at reason:(NSString * _Nullable)reason expires_at:(NSString *)expires_at __attribute__((swift_name("insertBlacklistedToken(token_hash:user_id:revoked_at:reason:expires_at:)")));
- (void)insertDeviceId:(NSString *)id user_id:(NSString *)user_id device_id:(NSString *)device_id device_name:(NSString *)device_name device_type:(NSString * _Nullable)device_type fingerprint_hash:(NSString *)fingerprint_hash first_seen:(NSString *)first_seen last_seen:(NSString *)last_seen trusted:(int64_t)trusted created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("insertDevice(id:user_id:device_id:device_name:device_type:fingerprint_hash:first_seen:last_seen:trusted:created_at:updated_at:)")));
- (void)insertSessionId:(NSString *)id user_id:(NSString *)user_id device_id:(NSString *)device_id device_name:(NSString *)device_name jwt_token_hash:(NSString *)jwt_token_hash refresh_token_hash:(NSString *)refresh_token_hash ip_address:(NSString * _Nullable)ip_address user_agent:(NSString * _Nullable)user_agent created_at:(NSString *)created_at last_accessed:(NSString *)last_accessed expires_at:(NSString *)expires_at status:(NSString *)status __attribute__((swift_name("insertSession(id:user_id:device_id:device_name:jwt_token_hash:refresh_token_hash:ip_address:user_agent:created_at:last_accessed:expires_at:status:)")));
- (SharedRuntimeQuery<SharedBoolean *> *)isTokenBlacklistedToken_hash:(NSString *)token_hash __attribute__((swift_name("isTokenBlacklisted(token_hash:)")));
- (void)markExpiredSessionsExpires_at:(NSString *)expires_at __attribute__((swift_name("markExpiredSessions(expires_at:)")));
- (void)revokeAllOtherSessionsUser_id:(NSString *)user_id id:(NSString *)id __attribute__((swift_name("revokeAllOtherSessions(user_id:id:)")));
- (void)revokeAllUserSessionsUser_id:(NSString *)user_id __attribute__((swift_name("revokeAllUserSessions(user_id:)")));
- (void)revokeSessionId:(NSString *)id __attribute__((swift_name("revokeSession(id:)")));
- (SharedRuntimeQuery<SharedSession *> *)selectActiveSessionsByUserIdUser_id:(NSString *)user_id expires_at:(NSString *)expires_at __attribute__((swift_name("selectActiveSessionsByUserId(user_id:expires_at:)")));
- (SharedRuntimeQuery<id> *)selectActiveSessionsByUserIdUser_id:(NSString *)user_id expires_at:(NSString *)expires_at mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString * _Nullable, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectActiveSessionsByUserId(user_id:expires_at:mapper:)")));
- (SharedRuntimeQuery<SharedJwt_blacklist *> *)selectBlacklistedTokensByUserIdUser_id:(NSString *)user_id __attribute__((swift_name("selectBlacklistedTokensByUserId(user_id:)")));
- (SharedRuntimeQuery<id> *)selectBlacklistedTokensByUserIdUser_id:(NSString *)user_id mapper:(id (^)(NSString *, NSString *, NSString *, NSString * _Nullable, NSString *))mapper __attribute__((swift_name("selectBlacklistedTokensByUserId(user_id:mapper:)")));
- (SharedRuntimeQuery<SharedDevice_fingerprint *> *)selectDeviceByFingerprintFingerprint_hash:(NSString *)fingerprint_hash __attribute__((swift_name("selectDeviceByFingerprint(fingerprint_hash:)")));
- (SharedRuntimeQuery<id> *)selectDeviceByFingerprintFingerprint_hash:(NSString *)fingerprint_hash mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString *, NSString *, NSString *, SharedLong *, NSString *, NSString *))mapper __attribute__((swift_name("selectDeviceByFingerprint(fingerprint_hash:mapper:)")));
- (SharedRuntimeQuery<SharedDevice_fingerprint *> *)selectDeviceByIdId:(NSString *)id __attribute__((swift_name("selectDeviceById(id:)")));
- (SharedRuntimeQuery<id> *)selectDeviceByIdId:(NSString *)id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString *, NSString *, NSString *, SharedLong *, NSString *, NSString *))mapper __attribute__((swift_name("selectDeviceById(id:mapper:)")));
- (SharedRuntimeQuery<SharedDevice_fingerprint *> *)selectDevicesByUserIdUser_id:(NSString *)user_id __attribute__((swift_name("selectDevicesByUserId(user_id:)")));
- (SharedRuntimeQuery<id> *)selectDevicesByUserIdUser_id:(NSString *)user_id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString *, NSString *, NSString *, SharedLong *, NSString *, NSString *))mapper __attribute__((swift_name("selectDevicesByUserId(user_id:mapper:)")));
- (SharedRuntimeQuery<SharedSession *> *)selectSessionByDeviceIdDevice_id:(NSString *)device_id __attribute__((swift_name("selectSessionByDeviceId(device_id:)")));
- (SharedRuntimeQuery<id> *)selectSessionByDeviceIdDevice_id:(NSString *)device_id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString * _Nullable, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectSessionByDeviceId(device_id:mapper:)")));
- (SharedRuntimeQuery<SharedSession *> *)selectSessionByIdId:(NSString *)id __attribute__((swift_name("selectSessionById(id:)")));
- (SharedRuntimeQuery<id> *)selectSessionByIdId:(NSString *)id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString * _Nullable, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectSessionById(id:mapper:)")));
- (SharedRuntimeQuery<SharedSession *> *)selectSessionByTokenHashJwt_token_hash:(NSString *)jwt_token_hash __attribute__((swift_name("selectSessionByTokenHash(jwt_token_hash:)")));
- (SharedRuntimeQuery<id> *)selectSessionByTokenHashJwt_token_hash:(NSString *)jwt_token_hash mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString * _Nullable, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectSessionByTokenHash(jwt_token_hash:mapper:)")));
- (SharedRuntimeQuery<SharedDevice_fingerprint *> *)selectTrustedDevicesByUserIdUser_id:(NSString *)user_id __attribute__((swift_name("selectTrustedDevicesByUserId(user_id:)")));
- (SharedRuntimeQuery<id> *)selectTrustedDevicesByUserIdUser_id:(NSString *)user_id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString *, NSString *, NSString *, SharedLong *, NSString *, NSString *))mapper __attribute__((swift_name("selectTrustedDevicesByUserId(user_id:mapper:)")));
- (void)updateDeviceLastSeenLast_seen:(NSString *)last_seen updated_at:(NSString *)updated_at id:(NSString *)id __attribute__((swift_name("updateDeviceLastSeen(last_seen:updated_at:id:)")));
- (void)updateDeviceTrustTrusted:(int64_t)trusted updated_at:(NSString *)updated_at id:(NSString *)id __attribute__((swift_name("updateDeviceTrust(trusted:updated_at:id:)")));
- (void)updateSessionLastAccessedLast_accessed:(NSString *)last_accessed id:(NSString *)id __attribute__((swift_name("updateSessionLastAccessed(last_accessed:id:)")));
- (void)updateSessionTokensJwt_token_hash:(NSString *)jwt_token_hash refresh_token_hash:(NSString *)refresh_token_hash expires_at:(NSString *)expires_at last_accessed:(NSString *)last_accessed id:(NSString *)id __attribute__((swift_name("updateSessionTokens(jwt_token_hash:refresh_token_hash:expires_at:last_accessed:id:)")));
@end


/**
 * Repository for managing user sessions, JWT blacklist, and device fingerprints.
 *
 * This repository handles:
 * - Multi-device session tracking
 * - JWT token blacklisting for revoked tokens
 * - Device fingerprinting for security
 * - Session lifecycle management (creation, validation, revocation)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SessionRepository")))
@interface SharedSessionRepository : SharedBase
- (instancetype)initWithDb:(id<SharedWakevDb>)db __attribute__((swift_name("init(db:)"))) __attribute__((objc_designated_initializer));

/**
 * Cleanup expired blacklist entries.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)cleanupExpiredBlacklistWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("cleanupExpiredBlacklist(completionHandler:)")));

/**
 * Cleanup old sessions (delete expired/revoked sessions older than 30 days).
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)cleanupOldSessionsWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("cleanupOldSessions(completionHandler:)")));

/**
 * Count active sessions for a user.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)countActiveSessionsUserId:(NSString *)userId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("countActiveSessions(userId:completionHandler:)")));

/**
 * Create a new session.
 *
 * @param userId The user ID
 * @param deviceId Unique device identifier
 * @param deviceName Human-readable device name
 * @param jwtToken The JWT access token
 * @param refreshToken The refresh token
 * @param ipAddress Optional IP address
 * @param userAgent Optional user agent string
 * @param expiresAt Session expiration timestamp
 * @return The created session ID
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)createSessionUserId:(NSString *)userId deviceId:(NSString *)deviceId deviceName:(NSString *)deviceName jwtToken:(NSString *)jwtToken refreshToken:(NSString *)refreshToken ipAddress:(NSString * _Nullable)ipAddress userAgent:(NSString * _Nullable)userAgent expiresAt:(NSString *)expiresAt completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("createSession(userId:deviceId:deviceName:jwtToken:refreshToken:ipAddress:userAgent:expiresAt:completionHandler:)")));

/**
 * Get all active sessions for a user.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getActiveSessionsForUserUserId:(NSString *)userId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getActiveSessionsForUser(userId:completionHandler:)")));

/**
 * Get all devices for a user.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getDevicesForUserUserId:(NSString *)userId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getDevicesForUser(userId:completionHandler:)")));

/**
 * Get session by ID.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getSessionByIdSessionId:(NSString *)sessionId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getSessionById(sessionId:completionHandler:)")));

/**
 * Get session by JWT token hash.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getSessionByTokenJwtToken:(NSString *)jwtToken completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getSessionByToken(jwtToken:completionHandler:)")));

/**
 * Check if a JWT token is blacklisted.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)isTokenBlacklistedJwtToken:(NSString *)jwtToken completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("isTokenBlacklisted(jwtToken:completionHandler:)")));

/**
 * Register or update a device fingerprint.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)registerDeviceUserId:(NSString *)userId deviceId:(NSString *)deviceId deviceName:(NSString *)deviceName deviceType:(NSString * _Nullable)deviceType fingerprintHash:(NSString *)fingerprintHash trusted:(BOOL)trusted completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("registerDevice(userId:deviceId:deviceName:deviceType:fingerprintHash:trusted:completionHandler:)")));

/**
 * Revoke all other sessions except the current one.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)revokeAllOtherSessionsUserId:(NSString *)userId currentSessionId:(NSString *)currentSessionId reason:(NSString *)reason completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("revokeAllOtherSessions(userId:currentSessionId:reason:completionHandler:)")));

/**
 * Revoke all sessions for a user (logout from all devices).
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)revokeAllUserSessionsUserId:(NSString *)userId reason:(NSString *)reason completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("revokeAllUserSessions(userId:reason:completionHandler:)")));

/**
 * Revoke a specific session (logout from one device).
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)revokeSessionSessionId:(NSString *)sessionId reason:(NSString *)reason completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("revokeSession(sessionId:reason:completionHandler:)")));

/**
 * Update device trust status.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)updateDeviceTrustDeviceId:(NSString *)deviceId trusted:(BOOL)trusted completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("updateDeviceTrust(deviceId:trusted:completionHandler:)")));

/**
 * Update session last accessed time.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)updateSessionLastAccessedSessionId:(NSString *)sessionId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("updateSessionLastAccessed(sessionId:completionHandler:)")));

/**
 * Update session tokens (after token refresh).
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)updateSessionTokensSessionId:(NSString *)sessionId newJwtToken:(NSString *)newJwtToken newRefreshToken:(NSString *)newRefreshToken newExpiresAt:(NSString *)newExpiresAt completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("updateSessionTokens(sessionId:newJwtToken:newRefreshToken:newExpiresAt:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SyncMetadata")))
@interface SharedSyncMetadata : SharedBase
- (instancetype)initWithId:(NSString *)id entityType:(NSString *)entityType entityId:(NSString *)entityId operation:(NSString *)operation timestamp:(NSString *)timestamp synced:(int64_t)synced __attribute__((swift_name("init(id:entityType:entityId:operation:timestamp:synced:)"))) __attribute__((objc_designated_initializer));
- (SharedSyncMetadata *)doCopyId:(NSString *)id entityType:(NSString *)entityType entityId:(NSString *)entityId operation:(NSString *)operation timestamp:(NSString *)timestamp synced:(int64_t)synced __attribute__((swift_name("doCopy(id:entityType:entityId:operation:timestamp:synced:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *entityId __attribute__((swift_name("entityId")));
@property (readonly) NSString *entityType __attribute__((swift_name("entityType")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *operation __attribute__((swift_name("operation")));
@property (readonly) int64_t synced __attribute__((swift_name("synced")));
@property (readonly) NSString *timestamp __attribute__((swift_name("timestamp")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SyncMetadataQueries")))
@interface SharedSyncMetadataQueries : SharedRuntimeTransacterImpl
- (instancetype)initWithDriver:(id<SharedRuntimeSqlDriver>)driver __attribute__((swift_name("init(driver:)"))) __attribute__((objc_designated_initializer));
- (void)deleteByEntityEntityType:(NSString *)entityType entityId:(NSString *)entityId __attribute__((swift_name("deleteByEntity(entityType:entityId:)")));
- (void)insertSyncMetadataId:(NSString *)id entityType:(NSString *)entityType entityId:(NSString *)entityId operation:(NSString *)operation timestamp:(NSString *)timestamp synced:(int64_t)synced __attribute__((swift_name("insertSyncMetadata(id:entityType:entityId:operation:timestamp:synced:)")));
- (SharedRuntimeQuery<SharedLastSyncTime *> *)lastSyncTime __attribute__((swift_name("lastSyncTime()")));
- (SharedRuntimeQuery<id> *)lastSyncTimeMapper:(id (^)(NSString * _Nullable))mapper __attribute__((swift_name("lastSyncTime(mapper:)")));
- (void)markSyncedId:(NSString *)id __attribute__((swift_name("markSynced(id:)")));
- (void)markSyncedByEntityEntityType:(NSString *)entityType entityId:(NSString *)entityId timestamp:(NSString *)timestamp __attribute__((swift_name("markSyncedByEntity(entityType:entityId:timestamp:)")));
- (SharedRuntimeQuery<SharedSyncMetadata *> *)selectAll __attribute__((swift_name("selectAll()")));
- (SharedRuntimeQuery<id> *)selectAllMapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, SharedLong *))mapper __attribute__((swift_name("selectAll(mapper:)")));
- (SharedRuntimeQuery<SharedSyncMetadata *> *)selectByEntityEntityType:(NSString *)entityType entityId:(NSString *)entityId __attribute__((swift_name("selectByEntity(entityType:entityId:)")));
- (SharedRuntimeQuery<id> *)selectByEntityEntityType:(NSString *)entityType entityId:(NSString *)entityId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, SharedLong *))mapper __attribute__((swift_name("selectByEntity(entityType:entityId:mapper:)")));
- (SharedRuntimeQuery<SharedSyncMetadata *> *)selectByIdId:(NSString *)id __attribute__((swift_name("selectById(id:)")));
- (SharedRuntimeQuery<id> *)selectByIdId:(NSString *)id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, SharedLong *))mapper __attribute__((swift_name("selectById(id:mapper:)")));
- (SharedRuntimeQuery<SharedSyncMetadata *> *)selectPending __attribute__((swift_name("selectPending()")));
- (SharedRuntimeQuery<id> *)selectPendingMapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, SharedLong *))mapper __attribute__((swift_name("selectPending(mapper:)")));
- (SharedRuntimeQuery<SharedSyncMetadata *> *)selectSinceLastTimestamp:(NSString *)timestamp __attribute__((swift_name("selectSinceLast(timestamp:)")));
- (SharedRuntimeQuery<id> *)selectSinceLastTimestamp:(NSString *)timestamp mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, SharedLong *))mapper __attribute__((swift_name("selectSinceLast(timestamp:mapper:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Sync_metadata")))
@interface SharedSync_metadata : SharedBase
- (instancetype)initWithId:(NSString *)id table_name:(NSString *)table_name record_id:(NSString *)record_id operation:(NSString *)operation timestamp:(NSString *)timestamp user_id:(NSString *)user_id synced:(SharedLong * _Nullable)synced retry_count:(SharedLong * _Nullable)retry_count last_error:(NSString * _Nullable)last_error __attribute__((swift_name("init(id:table_name:record_id:operation:timestamp:user_id:synced:retry_count:last_error:)"))) __attribute__((objc_designated_initializer));
- (SharedSync_metadata *)doCopyId:(NSString *)id table_name:(NSString *)table_name record_id:(NSString *)record_id operation:(NSString *)operation timestamp:(NSString *)timestamp user_id:(NSString *)user_id synced:(SharedLong * _Nullable)synced retry_count:(SharedLong * _Nullable)retry_count last_error:(NSString * _Nullable)last_error __attribute__((swift_name("doCopy(id:table_name:record_id:operation:timestamp:user_id:synced:retry_count:last_error:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString * _Nullable last_error __attribute__((swift_name("last_error")));
@property (readonly) NSString *operation __attribute__((swift_name("operation")));
@property (readonly) NSString *record_id __attribute__((swift_name("record_id")));
@property (readonly) SharedLong * _Nullable retry_count __attribute__((swift_name("retry_count")));
@property (readonly) SharedLong * _Nullable synced __attribute__((swift_name("synced")));
@property (readonly) NSString *table_name __attribute__((swift_name("table_name")));
@property (readonly) NSString *timestamp __attribute__((swift_name("timestamp")));
@property (readonly) NSString *user_id __attribute__((swift_name("user_id")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TimeSlot_")))
@interface SharedTimeSlot_ : SharedBase
- (instancetype)initWithId:(NSString *)id eventId:(NSString *)eventId startTime:(NSString *)startTime endTime:(NSString *)endTime timezone:(NSString *)timezone proposedByParticipantId:(NSString * _Nullable)proposedByParticipantId createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("init(id:eventId:startTime:endTime:timezone:proposedByParticipantId:createdAt:updatedAt:)"))) __attribute__((objc_designated_initializer));
- (SharedTimeSlot_ *)doCopyId:(NSString *)id eventId:(NSString *)eventId startTime:(NSString *)startTime endTime:(NSString *)endTime timezone:(NSString *)timezone proposedByParticipantId:(NSString * _Nullable)proposedByParticipantId createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("doCopy(id:eventId:startTime:endTime:timezone:proposedByParticipantId:createdAt:updatedAt:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *endTime __attribute__((swift_name("endTime")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString * _Nullable proposedByParticipantId __attribute__((swift_name("proposedByParticipantId")));
@property (readonly) NSString *startTime __attribute__((swift_name("startTime")));
@property (readonly) NSString *timezone __attribute__((swift_name("timezone")));
@property (readonly) NSString *updatedAt __attribute__((swift_name("updatedAt")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TimeSlotQueries")))
@interface SharedTimeSlotQueries : SharedRuntimeTransacterImpl
- (instancetype)initWithDriver:(id<SharedRuntimeSqlDriver>)driver __attribute__((swift_name("init(driver:)"))) __attribute__((objc_designated_initializer));
- (void)deleteByEventIdEventId:(NSString *)eventId __attribute__((swift_name("deleteByEventId(eventId:)")));
- (void)deleteTimeSlotId:(NSString *)id __attribute__((swift_name("deleteTimeSlot(id:)")));
- (void)insertTimeSlotId:(NSString *)id eventId:(NSString *)eventId startTime:(NSString *)startTime endTime:(NSString *)endTime timezone:(NSString *)timezone proposedByParticipantId:(NSString * _Nullable)proposedByParticipantId createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("insertTimeSlot(id:eventId:startTime:endTime:timezone:proposedByParticipantId:createdAt:updatedAt:)")));
- (SharedRuntimeQuery<SharedTimeSlot_ *> *)selectAll __attribute__((swift_name("selectAll()")));
- (SharedRuntimeQuery<id> *)selectAllMapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("selectAll(mapper:)")));
- (SharedRuntimeQuery<SharedTimeSlot_ *> *)selectByEventIdEventId:(NSString *)eventId __attribute__((swift_name("selectByEventId(eventId:)")));
- (SharedRuntimeQuery<id> *)selectByEventIdEventId:(NSString *)eventId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("selectByEventId(eventId:mapper:)")));
- (SharedRuntimeQuery<SharedTimeSlot_ *> *)selectByEventIdAndProposerEventId:(NSString *)eventId proposedByParticipantId:(NSString * _Nullable)proposedByParticipantId __attribute__((swift_name("selectByEventIdAndProposer(eventId:proposedByParticipantId:)")));
- (SharedRuntimeQuery<id> *)selectByEventIdAndProposerEventId:(NSString *)eventId proposedByParticipantId:(NSString * _Nullable)proposedByParticipantId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("selectByEventIdAndProposer(eventId:proposedByParticipantId:mapper:)")));
- (SharedRuntimeQuery<SharedTimeSlot_ *> *)selectByIdId:(NSString *)id __attribute__((swift_name("selectById(id:)")));
- (SharedRuntimeQuery<id> *)selectByIdId:(NSString *)id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("selectById(id:mapper:)")));
- (void)updateTimeSlotEndTime:(NSString *)endTime updatedAt:(NSString *)updatedAt id:(NSString *)id __attribute__((swift_name("updateTimeSlot(endTime:updatedAt:id:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("User")))
@interface SharedUser : SharedBase
- (instancetype)initWithId:(NSString *)id provider_id:(NSString *)provider_id email:(NSString *)email name:(NSString *)name avatar_url:(NSString * _Nullable)avatar_url provider:(NSString *)provider role:(NSString *)role created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("init(id:provider_id:email:name:avatar_url:provider:role:created_at:updated_at:)"))) __attribute__((objc_designated_initializer));
- (SharedUser *)doCopyId:(NSString *)id provider_id:(NSString *)provider_id email:(NSString *)email name:(NSString *)name avatar_url:(NSString * _Nullable)avatar_url provider:(NSString *)provider role:(NSString *)role created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("doCopy(id:provider_id:email:name:avatar_url:provider:role:created_at:updated_at:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable avatar_url __attribute__((swift_name("avatar_url")));
@property (readonly) NSString *created_at __attribute__((swift_name("created_at")));
@property (readonly) NSString *email __attribute__((swift_name("email")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) NSString *provider __attribute__((swift_name("provider")));
@property (readonly) NSString *provider_id __attribute__((swift_name("provider_id")));
@property (readonly) NSString *role __attribute__((swift_name("role")));
@property (readonly) NSString *updated_at __attribute__((swift_name("updated_at")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UserPreferencesQueries")))
@interface SharedUserPreferencesQueries : SharedRuntimeTransacterImpl
- (instancetype)initWithDriver:(id<SharedRuntimeSqlDriver>)driver __attribute__((swift_name("init(driver:)"))) __attribute__((objc_designated_initializer));
- (void)deletePreferencesUser_id:(NSString *)user_id __attribute__((swift_name("deletePreferences(user_id:)")));
- (void)insertPreferencesUser_id:(NSString *)user_id preferred_days_of_week:(NSString *)preferred_days_of_week preferred_times:(NSString *)preferred_times preferred_locations:(NSString *)preferred_locations preferred_activities:(NSString *)preferred_activities budget_range:(NSString * _Nullable)budget_range group_size_preference:(SharedLong * _Nullable)group_size_preference last_updated:(NSString *)last_updated __attribute__((swift_name("insertPreferences(user_id:preferred_days_of_week:preferred_times:preferred_locations:preferred_activities:budget_range:group_size_preference:last_updated:)")));
- (SharedRuntimeQuery<SharedUser_preferences *> *)selectPreferencesByUserIdUser_id:(NSString *)user_id __attribute__((swift_name("selectPreferencesByUserId(user_id:)")));
- (SharedRuntimeQuery<id> *)selectPreferencesByUserIdUser_id:(NSString *)user_id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, SharedLong * _Nullable, NSString *))mapper __attribute__((swift_name("selectPreferencesByUserId(user_id:mapper:)")));
- (void)updatePreferencesPreferred_days_of_week:(NSString *)preferred_days_of_week preferred_times:(NSString *)preferred_times preferred_locations:(NSString *)preferred_locations preferred_activities:(NSString *)preferred_activities budget_range:(NSString * _Nullable)budget_range group_size_preference:(SharedLong * _Nullable)group_size_preference last_updated:(NSString *)last_updated user_id:(NSString *)user_id __attribute__((swift_name("updatePreferences(preferred_days_of_week:preferred_times:preferred_locations:preferred_activities:budget_range:group_size_preference:last_updated:user_id:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UserPreferencesRepository")))
@interface SharedUserPreferencesRepository : SharedBase
- (instancetype)initWithDatabase:(id<SharedWakevDb>)database __attribute__((swift_name("init(database:)"))) __attribute__((objc_designated_initializer));
- (void)deletePreferencesUserId:(NSString *)userId __attribute__((swift_name("deletePreferences(userId:)")));
- (SharedUserPreferences * _Nullable)getPreferencesUserId:(NSString *)userId __attribute__((swift_name("getPreferences(userId:)")));
- (void)savePreferencesPreferences:(SharedUserPreferences *)preferences __attribute__((swift_name("savePreferences(preferences:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UserQueries")))
@interface SharedUserQueries : SharedRuntimeTransacterImpl
- (instancetype)initWithDriver:(id<SharedRuntimeSqlDriver>)driver __attribute__((swift_name("init(driver:)"))) __attribute__((objc_designated_initializer));
- (void)cleanupOldSyncMetadataTimestamp:(NSString *)timestamp __attribute__((swift_name("cleanupOldSyncMetadata(timestamp:)")));
- (void)deleteExpiredTokensExpires_at:(NSString *)expires_at __attribute__((swift_name("deleteExpiredTokens(expires_at:)")));
- (void)deleteSyncMetadataId:(NSString *)id __attribute__((swift_name("deleteSyncMetadata(id:)")));
- (void)deleteTokenUser_id:(NSString *)user_id __attribute__((swift_name("deleteToken(user_id:)")));
- (void)deleteUserId:(NSString *)id __attribute__((swift_name("deleteUser(id:)")));
- (void)insertPreferencesId:(NSString *)id user_id:(NSString *)user_id deadline_reminder:(int64_t)deadline_reminder event_update:(int64_t)event_update vote_close_reminder:(int64_t)vote_close_reminder timezone:(NSString *)timezone created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("insertPreferences(id:user_id:deadline_reminder:event_update:vote_close_reminder:timezone:created_at:updated_at:)")));
- (void)insertSyncMetadataId:(NSString *)id table_name:(NSString *)table_name record_id:(NSString *)record_id operation:(NSString *)operation timestamp:(NSString *)timestamp user_id:(NSString *)user_id synced:(SharedLong * _Nullable)synced retry_count:(SharedLong * _Nullable)retry_count last_error:(NSString * _Nullable)last_error __attribute__((swift_name("insertSyncMetadata(id:table_name:record_id:operation:timestamp:user_id:synced:retry_count:last_error:)")));
- (void)insertTokenId:(NSString *)id user_id:(NSString *)user_id access_token:(NSString *)access_token refresh_token:(NSString * _Nullable)refresh_token token_type:(NSString *)token_type expires_at:(NSString *)expires_at scope:(NSString * _Nullable)scope created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("insertToken(id:user_id:access_token:refresh_token:token_type:expires_at:scope:created_at:updated_at:)")));
- (void)insertUserId:(NSString *)id provider_id:(NSString *)provider_id email:(NSString *)email name:(NSString *)name avatar_url:(NSString * _Nullable)avatar_url provider:(NSString *)provider role:(NSString *)role created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("insertUser(id:provider_id:email:name:avatar_url:provider:role:created_at:updated_at:)")));
- (SharedRuntimeQuery<SharedUser *> *)selectAllUsers __attribute__((swift_name("selectAllUsers()")));
- (SharedRuntimeQuery<id> *)selectAllUsersMapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectAllUsers(mapper:)")));
- (SharedRuntimeQuery<SharedSync_metadata *> *)selectPendingSync __attribute__((swift_name("selectPendingSync()")));
- (SharedRuntimeQuery<id> *)selectPendingSyncMapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, SharedLong * _Nullable, SharedLong * _Nullable, NSString * _Nullable))mapper __attribute__((swift_name("selectPendingSync(mapper:)")));
- (SharedRuntimeQuery<SharedNotification_preference *> *)selectPreferencesByUserIdUser_id:(NSString *)user_id __attribute__((swift_name("selectPreferencesByUserId(user_id:)")));
- (SharedRuntimeQuery<id> *)selectPreferencesByUserIdUser_id:(NSString *)user_id mapper:(id (^)(NSString *, NSString *, SharedLong *, SharedLong *, SharedLong *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectPreferencesByUserId(user_id:mapper:)")));
- (SharedRuntimeQuery<SharedSync_metadata *> *)selectSyncByTableAndRecordTable_name:(NSString *)table_name record_id:(NSString *)record_id __attribute__((swift_name("selectSyncByTableAndRecord(table_name:record_id:)")));
- (SharedRuntimeQuery<id> *)selectSyncByTableAndRecordTable_name:(NSString *)table_name record_id:(NSString *)record_id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, SharedLong * _Nullable, SharedLong * _Nullable, NSString * _Nullable))mapper __attribute__((swift_name("selectSyncByTableAndRecord(table_name:record_id:mapper:)")));
- (SharedRuntimeQuery<SharedUser_token *> *)selectTokenByIdId:(NSString *)id __attribute__((swift_name("selectTokenById(id:)")));
- (SharedRuntimeQuery<id> *)selectTokenByIdId:(NSString *)id mapper:(id (^)(NSString *, NSString *, NSString *, NSString * _Nullable, NSString *, NSString *, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("selectTokenById(id:mapper:)")));
- (SharedRuntimeQuery<SharedUser_token *> *)selectTokenByRefreshTokenRefresh_token:(NSString * _Nullable)refresh_token __attribute__((swift_name("selectTokenByRefreshToken(refresh_token:)")));
- (SharedRuntimeQuery<id> *)selectTokenByRefreshTokenRefresh_token:(NSString * _Nullable)refresh_token mapper:(id (^)(NSString *, NSString *, NSString *, NSString * _Nullable, NSString *, NSString *, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("selectTokenByRefreshToken(refresh_token:mapper:)")));
- (SharedRuntimeQuery<SharedUser_token *> *)selectTokenByUserIdUser_id:(NSString *)user_id __attribute__((swift_name("selectTokenByUserId(user_id:)")));
- (SharedRuntimeQuery<id> *)selectTokenByUserIdUser_id:(NSString *)user_id mapper:(id (^)(NSString *, NSString *, NSString *, NSString * _Nullable, NSString *, NSString *, NSString * _Nullable, NSString *, NSString *))mapper __attribute__((swift_name("selectTokenByUserId(user_id:mapper:)")));
- (SharedRuntimeQuery<SharedUser *> *)selectUserByEmailEmail:(NSString *)email __attribute__((swift_name("selectUserByEmail(email:)")));
- (SharedRuntimeQuery<id> *)selectUserByEmailEmail:(NSString *)email mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectUserByEmail(email:mapper:)")));
- (SharedRuntimeQuery<SharedUser *> *)selectUserByIdId:(NSString *)id __attribute__((swift_name("selectUserById(id:)")));
- (SharedRuntimeQuery<id> *)selectUserByIdId:(NSString *)id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectUserById(id:mapper:)")));
- (SharedRuntimeQuery<SharedUser *> *)selectUserByProviderIdProvider_id:(NSString *)provider_id provider:(NSString *)provider __attribute__((swift_name("selectUserByProviderId(provider_id:provider:)")));
- (SharedRuntimeQuery<id> *)selectUserByProviderIdProvider_id:(NSString *)provider_id provider:(NSString *)provider mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString * _Nullable, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectUserByProviderId(provider_id:provider:mapper:)")));
- (void)updatePreferencesDeadline_reminder:(int64_t)deadline_reminder event_update:(int64_t)event_update vote_close_reminder:(int64_t)vote_close_reminder timezone:(NSString *)timezone updated_at:(NSString *)updated_at user_id:(NSString *)user_id __attribute__((swift_name("updatePreferences(deadline_reminder:event_update:vote_close_reminder:timezone:updated_at:user_id:)")));
- (void)updateSyncMetadataSynced:(SharedLong * _Nullable)synced retry_count:(SharedLong * _Nullable)retry_count last_error:(NSString * _Nullable)last_error id:(NSString *)id __attribute__((swift_name("updateSyncMetadata(synced:retry_count:last_error:id:)")));
- (void)updateTokenAccess_token:(NSString *)access_token refresh_token:(NSString * _Nullable)refresh_token expires_at:(NSString *)expires_at updated_at:(NSString *)updated_at id:(NSString *)id __attribute__((swift_name("updateToken(access_token:refresh_token:expires_at:updated_at:id:)")));
- (void)updateTokenExpiryExpires_at:(NSString *)expires_at updated_at:(NSString *)updated_at id:(NSString *)id __attribute__((swift_name("updateTokenExpiry(expires_at:updated_at:id:)")));
- (void)updateUserName:(NSString *)name avatar_url:(NSString * _Nullable)avatar_url updated_at:(NSString *)updated_at id:(NSString *)id __attribute__((swift_name("updateUser(name:avatar_url:updated_at:id:)")));
- (void)updateUserRoleRole:(NSString *)role updated_at:(NSString *)updated_at id:(NSString *)id __attribute__((swift_name("updateUserRole(role:updated_at:id:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UserRepository")))
@interface SharedUserRepository : SharedBase
- (instancetype)initWithDb:(id<SharedWakevDb>)db __attribute__((swift_name("init(db:)"))) __attribute__((objc_designated_initializer));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)addSyncMetadataId:(NSString *)id tableName:(NSString *)tableName recordId:(NSString *)recordId operation:(SharedSyncOperation *)operation timestamp:(NSString *)timestamp userId:(NSString *)userId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("addSyncMetadata(id:tableName:recordId:operation:timestamp:userId:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)cleanupExpiredTokensWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("cleanupExpiredTokens(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)cleanupOldSyncMetadataOlderThan:(NSString *)olderThan completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("cleanupOldSyncMetadata(olderThan:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)createTokenUserId:(NSString *)userId accessToken:(NSString *)accessToken refreshToken:(NSString * _Nullable)refreshToken tokenType:(NSString *)tokenType expiresAt:(NSString *)expiresAt scope:(NSString * _Nullable)scope completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("createToken(userId:accessToken:refreshToken:tokenType:expiresAt:scope:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)createUserProviderId:(NSString *)providerId email:(NSString *)email name:(NSString *)name avatarUrl:(NSString * _Nullable)avatarUrl provider:(SharedOAuthProvider *)provider completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("createUser(providerId:email:name:avatarUrl:provider:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)deleteTokensForUserUserId:(NSString *)userId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("deleteTokensForUser(userId:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getNotificationPreferencesUserId:(NSString *)userId completionHandler:(void (^)(SharedNotificationPreferences * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getNotificationPreferences(userId:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getPendingSyncChangesWithCompletionHandler:(void (^)(NSArray<SharedSyncMetadata_ *> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getPendingSyncChanges(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getTokenByUserIdUserId:(NSString *)userId completionHandler:(void (^)(SharedUserToken * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getTokenByUserId(userId:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getUserByEmailEmail:(NSString *)email completionHandler:(void (^)(SharedUser_ * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getUserByEmail(email:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getUserByIdUserId:(NSString *)userId completionHandler:(void (^)(SharedUser_ * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getUserById(userId:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getUserByProviderIdProviderId:(NSString *)providerId provider:(SharedOAuthProvider *)provider completionHandler:(void (^)(SharedUser_ * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getUserByProviderId(providerId:provider:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getUserTokenByRefreshTokenRefreshToken:(NSString *)refreshToken completionHandler:(void (^)(SharedUserToken * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getUserTokenByRefreshToken(refreshToken:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)removeSyncMetadataSyncId:(NSString *)syncId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("removeSyncMetadata(syncId:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)updateNotificationPreferencesUserId:(NSString *)userId deadlineReminder:(BOOL)deadlineReminder eventUpdate:(BOOL)eventUpdate voteCloseReminder:(BOOL)voteCloseReminder timezone:(NSString *)timezone completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("updateNotificationPreferences(userId:deadlineReminder:eventUpdate:voteCloseReminder:timezone:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)updateSyncStatusSyncId:(NSString *)syncId synced:(BOOL)synced retryCount:(int32_t)retryCount error:(NSString * _Nullable)error completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("updateSyncStatus(syncId:synced:retryCount:error:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)updateTokenTokenId:(NSString *)tokenId accessToken:(NSString *)accessToken refreshToken:(NSString * _Nullable)refreshToken expiresAt:(NSString *)expiresAt completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("updateToken(tokenId:accessToken:refreshToken:expiresAt:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)updateTokenExpiryTokenId:(NSString *)tokenId expiresAt:(NSString *)expiresAt completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("updateTokenExpiry(tokenId:expiresAt:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)updateUserUserId:(NSString *)userId name:(NSString *)name avatarUrl:(NSString * _Nullable)avatarUrl completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("updateUser(userId:name:avatarUrl:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("User_preferences")))
@interface SharedUser_preferences : SharedBase
- (instancetype)initWithUser_id:(NSString *)user_id preferred_days_of_week:(NSString *)preferred_days_of_week preferred_times:(NSString *)preferred_times preferred_locations:(NSString *)preferred_locations preferred_activities:(NSString *)preferred_activities budget_range:(NSString * _Nullable)budget_range group_size_preference:(SharedLong * _Nullable)group_size_preference last_updated:(NSString *)last_updated __attribute__((swift_name("init(user_id:preferred_days_of_week:preferred_times:preferred_locations:preferred_activities:budget_range:group_size_preference:last_updated:)"))) __attribute__((objc_designated_initializer));
- (SharedUser_preferences *)doCopyUser_id:(NSString *)user_id preferred_days_of_week:(NSString *)preferred_days_of_week preferred_times:(NSString *)preferred_times preferred_locations:(NSString *)preferred_locations preferred_activities:(NSString *)preferred_activities budget_range:(NSString * _Nullable)budget_range group_size_preference:(SharedLong * _Nullable)group_size_preference last_updated:(NSString *)last_updated __attribute__((swift_name("doCopy(user_id:preferred_days_of_week:preferred_times:preferred_locations:preferred_activities:budget_range:group_size_preference:last_updated:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable budget_range __attribute__((swift_name("budget_range")));
@property (readonly) SharedLong * _Nullable group_size_preference __attribute__((swift_name("group_size_preference")));
@property (readonly) NSString *last_updated __attribute__((swift_name("last_updated")));
@property (readonly) NSString *preferred_activities __attribute__((swift_name("preferred_activities")));
@property (readonly) NSString *preferred_days_of_week __attribute__((swift_name("preferred_days_of_week")));
@property (readonly) NSString *preferred_locations __attribute__((swift_name("preferred_locations")));
@property (readonly) NSString *preferred_times __attribute__((swift_name("preferred_times")));
@property (readonly) NSString *user_id __attribute__((swift_name("user_id")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("User_token")))
@interface SharedUser_token : SharedBase
- (instancetype)initWithId:(NSString *)id user_id:(NSString *)user_id access_token:(NSString *)access_token refresh_token:(NSString * _Nullable)refresh_token token_type:(NSString *)token_type expires_at:(NSString *)expires_at scope:(NSString * _Nullable)scope created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("init(id:user_id:access_token:refresh_token:token_type:expires_at:scope:created_at:updated_at:)"))) __attribute__((objc_designated_initializer));
- (SharedUser_token *)doCopyId:(NSString *)id user_id:(NSString *)user_id access_token:(NSString *)access_token refresh_token:(NSString * _Nullable)refresh_token token_type:(NSString *)token_type expires_at:(NSString *)expires_at scope:(NSString * _Nullable)scope created_at:(NSString *)created_at updated_at:(NSString *)updated_at __attribute__((swift_name("doCopy(id:user_id:access_token:refresh_token:token_type:expires_at:scope:created_at:updated_at:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *access_token __attribute__((swift_name("access_token")));
@property (readonly) NSString *created_at __attribute__((swift_name("created_at")));
@property (readonly) NSString *expires_at __attribute__((swift_name("expires_at")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString * _Nullable refresh_token __attribute__((swift_name("refresh_token")));
@property (readonly) NSString * _Nullable scope __attribute__((swift_name("scope")));
@property (readonly) NSString *token_type __attribute__((swift_name("token_type")));
@property (readonly) NSString *updated_at __attribute__((swift_name("updated_at")));
@property (readonly) NSString *user_id __attribute__((swift_name("user_id")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Vote_")))
@interface SharedVote_ : SharedBase
- (instancetype)initWithId:(NSString *)id eventId:(NSString *)eventId timeslotId:(NSString *)timeslotId participantId:(NSString *)participantId vote:(NSString *)vote createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("init(id:eventId:timeslotId:participantId:vote:createdAt:updatedAt:)"))) __attribute__((objc_designated_initializer));
- (SharedVote_ *)doCopyId:(NSString *)id eventId:(NSString *)eventId timeslotId:(NSString *)timeslotId participantId:(NSString *)participantId vote:(NSString *)vote createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("doCopy(id:eventId:timeslotId:participantId:vote:createdAt:updatedAt:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *participantId __attribute__((swift_name("participantId")));
@property (readonly) NSString *timeslotId __attribute__((swift_name("timeslotId")));
@property (readonly) NSString *updatedAt __attribute__((swift_name("updatedAt")));
@property (readonly) NSString *vote __attribute__((swift_name("vote")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("VoteQueries")))
@interface SharedVoteQueries : SharedRuntimeTransacterImpl
- (instancetype)initWithDriver:(id<SharedRuntimeSqlDriver>)driver __attribute__((swift_name("init(driver:)"))) __attribute__((objc_designated_initializer));
- (void)deleteByEventIdEventId:(NSString *)eventId __attribute__((swift_name("deleteByEventId(eventId:)")));
- (void)deleteByTimeslotIdTimeslotId:(NSString *)timeslotId __attribute__((swift_name("deleteByTimeslotId(timeslotId:)")));
- (void)deleteVoteId:(NSString *)id __attribute__((swift_name("deleteVote(id:)")));
- (void)insertVoteId:(NSString *)id eventId:(NSString *)eventId timeslotId:(NSString *)timeslotId participantId:(NSString *)participantId vote:(NSString *)vote createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("insertVote(id:eventId:timeslotId:participantId:vote:createdAt:updatedAt:)")));
- (SharedRuntimeQuery<SharedVote_ *> *)selectAll __attribute__((swift_name("selectAll()")));
- (SharedRuntimeQuery<id> *)selectAllMapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectAll(mapper:)")));
- (SharedRuntimeQuery<SharedVote_ *> *)selectByEventIdEventId:(NSString *)eventId __attribute__((swift_name("selectByEventId(eventId:)")));
- (SharedRuntimeQuery<id> *)selectByEventIdEventId:(NSString *)eventId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectByEventId(eventId:mapper:)")));
- (SharedRuntimeQuery<SharedVote_ *> *)selectByIdId:(NSString *)id __attribute__((swift_name("selectById(id:)")));
- (SharedRuntimeQuery<id> *)selectByIdId:(NSString *)id mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectById(id:mapper:)")));
- (SharedRuntimeQuery<SharedVote_ *> *)selectByParticipantIdParticipantId:(NSString *)participantId __attribute__((swift_name("selectByParticipantId(participantId:)")));
- (SharedRuntimeQuery<id> *)selectByParticipantIdParticipantId:(NSString *)participantId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectByParticipantId(participantId:mapper:)")));
- (SharedRuntimeQuery<SharedVote_ *> *)selectByTimeslotAndParticipantTimeslotId:(NSString *)timeslotId participantId:(NSString *)participantId __attribute__((swift_name("selectByTimeslotAndParticipant(timeslotId:participantId:)")));
- (SharedRuntimeQuery<id> *)selectByTimeslotAndParticipantTimeslotId:(NSString *)timeslotId participantId:(NSString *)participantId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectByTimeslotAndParticipant(timeslotId:participantId:mapper:)")));
- (SharedRuntimeQuery<SharedVote_ *> *)selectByTimeslotIdTimeslotId:(NSString *)timeslotId __attribute__((swift_name("selectByTimeslotId(timeslotId:)")));
- (SharedRuntimeQuery<id> *)selectByTimeslotIdTimeslotId:(NSString *)timeslotId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectByTimeslotId(timeslotId:mapper:)")));
- (SharedRuntimeQuery<SharedVote_ *> *)selectMaybeByTimeslotTimeslotId:(NSString *)timeslotId __attribute__((swift_name("selectMaybeByTimeslot(timeslotId:)")));
- (SharedRuntimeQuery<id> *)selectMaybeByTimeslotTimeslotId:(NSString *)timeslotId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectMaybeByTimeslot(timeslotId:mapper:)")));
- (SharedRuntimeQuery<SharedVote_ *> *)selectNoByTimeslotTimeslotId:(NSString *)timeslotId __attribute__((swift_name("selectNoByTimeslot(timeslotId:)")));
- (SharedRuntimeQuery<id> *)selectNoByTimeslotTimeslotId:(NSString *)timeslotId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectNoByTimeslot(timeslotId:mapper:)")));
- (SharedRuntimeQuery<SharedSelectVotesByTimeslot *> *)selectVotesByTimeslotTimeslotId:(NSString *)timeslotId __attribute__((swift_name("selectVotesByTimeslot(timeslotId:)")));
- (SharedRuntimeQuery<id> *)selectVotesByTimeslotTimeslotId:(NSString *)timeslotId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectVotesByTimeslot(timeslotId:mapper:)")));
- (SharedRuntimeQuery<SharedSelectVotesForEventTimeslots *> *)selectVotesForEventTimeslotsEventId:(NSString *)eventId __attribute__((swift_name("selectVotesForEventTimeslots(eventId:)")));
- (SharedRuntimeQuery<id> *)selectVotesForEventTimeslotsEventId:(NSString *)eventId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectVotesForEventTimeslots(eventId:mapper:)")));
- (SharedRuntimeQuery<SharedVote_ *> *)selectYesByTimeslotTimeslotId:(NSString *)timeslotId __attribute__((swift_name("selectYesByTimeslot(timeslotId:)")));
- (SharedRuntimeQuery<id> *)selectYesByTimeslotTimeslotId:(NSString *)timeslotId mapper:(id (^)(NSString *, NSString *, NSString *, NSString *, NSString *, NSString *, NSString *))mapper __attribute__((swift_name("selectYesByTimeslot(timeslotId:mapper:)")));
- (void)updateVoteVote:(NSString *)vote updatedAt:(NSString *)updatedAt id:(NSString *)id __attribute__((swift_name("updateVote(vote:updatedAt:id:)")));
@end


/**
 * Service for managing accommodations and room assignments.
 *
 * This service provides business logic for:
 * - Creating and managing accommodations
 * - Assigning participants to rooms
 * - Calculating costs per person
 * - Validating capacity constraints
 * - Automatic room distribution algorithms
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AccommodationService")))
@interface SharedAccommodationService : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Service for managing accommodations and room assignments.
 *
 * This service provides business logic for:
 * - Creating and managing accommodations
 * - Assigning participants to rooms
 * - Calculating costs per person
 * - Validating capacity constraints
 * - Automatic room distribution algorithms
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)accommodationService __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAccommodationService *shared __attribute__((swift_name("shared")));

/**
 * Automatically distribute participants into rooms
 *
 * This algorithm tries to:
 * 1. Fill rooms efficiently (prefer filling rooms completely)
 * 2. Minimize the number of partially-filled rooms
 * 3. Respect room capacity constraints
 *
 * @param participants List of participant IDs to assign
 * @param roomCapacities Map of room numbers to their capacities
 * @return Map of room numbers to assigned participant lists
 */
- (NSDictionary<NSString *, NSArray<NSString *> *> *)autoAssignRoomsParticipants:(NSArray<NSString *> *)participants roomCapacities:(NSDictionary<NSString *, SharedInt *> *)roomCapacities __attribute__((swift_name("autoAssignRooms(participants:roomCapacities:)")));

/**
 * Calculate statistics for accommodation with rooms
 */
- (SharedAccommodationWithRooms *)calculateAccommodationStatsAccommodation:(SharedAccommodation_ *)accommodation roomAssignments:(NSArray<SharedRoomAssignment *> *)roomAssignments __attribute__((swift_name("calculateAccommodationStats(accommodation:roomAssignments:)")));

/**
 * Calculate the average cost per person for an accommodation
 *
 * @param totalCost Total cost of accommodation in cents
 * @param participantCount Number of participants sharing the cost
 * @return Cost per person in cents
 */
- (int64_t)calculateCostPerPersonTotalCost:(int64_t)totalCost participantCount:(int32_t)participantCount __attribute__((swift_name("calculateCostPerPerson(totalCost:participantCount:)")));

/**
 * Calculate remaining capacity
 */
- (int32_t)calculateRemainingCapacityAccommodationCapacity:(int32_t)accommodationCapacity assignedCount:(int32_t)assignedCount __attribute__((swift_name("calculateRemainingCapacity(accommodationCapacity:assignedCount:)")));

/**
 * Calculate the price share for a room assignment
 *
 * @param accommodationTotalCost Total cost of the accommodation
 * @param roomCapacity Capacity of this specific room
 * @param totalAccommodationCapacity Total capacity of the accommodation
 * @param assignedParticipants Number of participants assigned to this room
 * @return Cost per person in this room in cents
 */
- (int64_t)calculateRoomPriceShareAccommodationTotalCost:(int64_t)accommodationTotalCost roomCapacity:(int32_t)roomCapacity totalAccommodationCapacity:(int32_t)totalAccommodationCapacity assignedParticipants:(int32_t)assignedParticipants __attribute__((swift_name("calculateRoomPriceShare(accommodationTotalCost:roomCapacity:totalAccommodationCapacity:assignedParticipants:)")));

/**
 * Calculate the total cost of an accommodation
 */
- (int64_t)calculateTotalCostPricePerNight:(int64_t)pricePerNight totalNights:(int32_t)totalNights __attribute__((swift_name("calculateTotalCost(pricePerNight:totalNights:)")));

/**
 * Find unassigned participants
 *
 * @param allParticipants All event participants
 * @param roomAssignments Current room assignments
 * @return List of participant IDs not assigned to any room
 */
- (NSArray<NSString *> *)findUnassignedParticipantsAllParticipants:(NSArray<NSString *> *)allParticipants roomAssignments:(NSArray<SharedRoomAssignment *> *)roomAssignments __attribute__((swift_name("findUnassignedParticipants(allParticipants:roomAssignments:)")));

/**
 * Get current UTC timestamp in ISO 8601 format
 */
- (NSString *)getCurrentUtcIsoString __attribute__((swift_name("getCurrentUtcIsoString()")));

/**
 * Get the room assignment for a specific participant
 */
- (SharedRoomAssignment * _Nullable)getRoomForParticipantParticipantId:(NSString *)participantId roomAssignments:(NSArray<SharedRoomAssignment *> *)roomAssignments __attribute__((swift_name("getRoomForParticipant(participantId:roomAssignments:)")));

/**
 * Check if accommodation has remaining capacity
 */
- (BOOL)hasRemainingCapacityAccommodationCapacity:(int32_t)accommodationCapacity assignedCount:(int32_t)assignedCount __attribute__((swift_name("hasRemainingCapacity(accommodationCapacity:assignedCount:)")));

/**
 * Check if a participant is assigned to any room in an accommodation
 */
- (BOOL)isParticipantAssignedParticipantId:(NSString *)participantId roomAssignments:(NSArray<SharedRoomAssignment *> *)roomAssignments __attribute__((swift_name("isParticipantAssigned(participantId:roomAssignments:)")));

/**
 * Optimize room assignments to minimize partially-filled rooms
 *
 * This is a more advanced algorithm that tries to balance room occupancy.
 * Use this when you want to avoid having one person alone in a large room.
 *
 * @param participants List of participant IDs to assign
 * @param roomCapacities Map of room numbers to their capacities
 * @return Map of room numbers to assigned participant lists
 */
- (NSDictionary<NSString *, NSArray<NSString *> *> *)optimizeRoomAssignmentsParticipants:(NSArray<NSString *> *)participants roomCapacities:(NSDictionary<NSString *, SharedInt *> *)roomCapacities __attribute__((swift_name("optimizeRoomAssignments(participants:roomCapacities:)")));

/**
 * Validate that accommodation data is correct
 *
 * @return Validation error message, or null if valid
 */
- (NSString * _Nullable)validateAccommodationName:(NSString *)name capacity:(int32_t)capacity pricePerNight:(int64_t)pricePerNight totalNights:(int32_t)totalNights checkInDate:(NSString *)checkInDate checkOutDate:(NSString *)checkOutDate __attribute__((swift_name("validateAccommodation(name:capacity:pricePerNight:totalNights:checkInDate:checkOutDate:)")));

/**
 * Validate that room assignment data is correct
 *
 * @return Validation error message, or null if valid
 */
- (NSString * _Nullable)validateRoomAssignmentRoomNumber:(NSString *)roomNumber capacity:(int32_t)capacity assignedParticipants:(NSArray<NSString *> *)assignedParticipants __attribute__((swift_name("validateRoomAssignment(roomNumber:capacity:assignedParticipants:)")));

/**
 * Validate that total cost matches price per night * total nights
 */
- (BOOL)validateTotalCostPricePerNight:(int64_t)pricePerNight totalNights:(int32_t)totalNights totalCost:(int64_t)totalCost __attribute__((swift_name("validateTotalCost(pricePerNight:totalNights:totalCost:)")));
@end


/**
 * Service for activity management
 *
 * This service provides business logic for:
 * - Creating and validating activities
 * - Managing participant registration
 * - Checking capacity constraints
 * - Calculating activity statistics
 * - Validating activity data
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ActivityManager")))
@interface SharedActivityManager : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Service for activity management
 *
 * This service provides business logic for:
 * - Creating and validating activities
 * - Managing participant registration
 * - Checking capacity constraints
 * - Calculating activity statistics
 * - Validating activity data
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)activityManager __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedActivityManager *shared __attribute__((swift_name("shared")));

/**
 * Calculate activity statistics
 */
- (SharedActivityWithStats *)calculateActivityStatsActivity:(SharedActivity_ *)activity __attribute__((swift_name("calculateActivityStats(activity:)")));

/**
 * Calculate participant activity statistics
 */
- (SharedParticipantActivityStats *)calculateParticipantStatsActivities:(NSArray<SharedActivity_ *> *)activities participantId:(NSString *)participantId __attribute__((swift_name("calculateParticipantStats(activities:participantId:)")));

/**
 * Check if activity has available capacity
 */
- (BOOL)checkCapacityActivity:(SharedActivity_ *)activity __attribute__((swift_name("checkCapacity(activity:)")));

/**
 * Create a new activity
 */
- (SharedActivity_ *)createActivityEventId:(NSString *)eventId request:(SharedActivityRequest *)request __attribute__((swift_name("createActivity(eventId:request:)")));

/**
 * Group activities by date
 */
- (NSArray<SharedActivitiesByDate *> *)groupActivitiesByDateActivities:(NSArray<SharedActivity_ *> *)activities __attribute__((swift_name("groupActivitiesByDate(activities:)")));

/**
 * Register a participant to an activity
 *
 * @return Updated activity with participant added, or null if registration failed
 */
- (SharedRegistrationResult *)registerParticipantActivity:(SharedActivity_ *)activity participantId:(NSString *)participantId notes:(NSString * _Nullable)notes __attribute__((swift_name("registerParticipant(activity:participantId:notes:)")));

/**
 * Unregister a participant from an activity
 */
- (SharedActivity_ *)unregisterParticipantActivity:(SharedActivity_ *)activity participantId:(NSString *)participantId __attribute__((swift_name("unregisterParticipant(activity:participantId:)")));

/**
 * Validate activity data
 */
- (SharedValidationResult *)validateActivityRequest:(SharedActivityRequest *)request __attribute__((swift_name("validateActivity(request:)")));
@end


/**
 * Activity Repository - Manages activity and participant registration persistence.
 *
 * Responsibilities:
 * - CRUD operations for activities
 * - CRUD operations for activity participants (registrations)
 * - Activity queries and filtering
 * - Statistics and aggregations
 * - Map between SQLDelight entities and Kotlin models
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ActivityRepository")))
@interface SharedActivityRepository : SharedBase
- (instancetype)initWithDb:(id<SharedWakevDb>)db __attribute__((swift_name("init(db:)"))) __attribute__((objc_designated_initializer));

/**
 * Check if an activity exists.
 */
- (BOOL)activityExistsActivityId:(NSString *)activityId __attribute__((swift_name("activityExists(activityId:)")));

/**
 * Count activities for an event.
 */
- (int64_t)countActivitiesByEventEventId:(NSString *)eventId __attribute__((swift_name("countActivitiesByEvent(eventId:)")));

/**
 * Count activities by event and date.
 */
- (int64_t)countActivitiesByEventAndDateEventId:(NSString *)eventId date:(NSString *)date __attribute__((swift_name("countActivitiesByEventAndDate(eventId:date:)")));

/**
 * Count activities a participant is registered for.
 */
- (int64_t)countActivitiesByParticipantParticipantId:(NSString *)participantId __attribute__((swift_name("countActivitiesByParticipant(participantId:)")));

/**
 * Count registered participants for an activity.
 */
- (int64_t)countParticipantsByActivityActivityId:(NSString *)activityId __attribute__((swift_name("countParticipantsByActivity(activityId:)")));

/**
 * Create a new activity.
 *
 * @param activity Activity to create
 * @return Created Activity
 */
- (SharedActivity_ *)createActivityActivity:(SharedActivity_ *)activity __attribute__((swift_name("createActivity(activity:)")));

/**
 * Delete all activities for an event.
 */
- (void)deleteActivitiesByEventEventId:(NSString *)eventId __attribute__((swift_name("deleteActivitiesByEvent(eventId:)")));

/**
 * Delete all activities for a scenario.
 */
- (void)deleteActivitiesByScenarioScenarioId:(NSString *)scenarioId __attribute__((swift_name("deleteActivitiesByScenario(scenarioId:)")));

/**
 * Delete an activity.
 */
- (void)deleteActivityActivityId:(NSString *)activityId __attribute__((swift_name("deleteActivity(activityId:)")));

/**
 * Delete all registrations for an activity.
 */
- (void)deleteParticipantsByActivityActivityId:(NSString *)activityId __attribute__((swift_name("deleteParticipantsByActivity(activityId:)")));

/**
 * Get activities grouped by date with statistics.
 */
- (NSArray<SharedActivitiesByDate *> *)getActivitiesByDateGroupedEventId:(NSString *)eventId __attribute__((swift_name("getActivitiesByDateGrouped(eventId:)")));

/**
 * Get activities by event and date.
 */
- (NSArray<SharedActivity_ *> *)getActivitiesByEventAndDateEventId:(NSString *)eventId date:(NSString *)date __attribute__((swift_name("getActivitiesByEventAndDate(eventId:date:)")));

/**
 * Get all activities for an event.
 */
- (NSArray<SharedActivity_ *> *)getActivitiesByEventIdEventId:(NSString *)eventId __attribute__((swift_name("getActivitiesByEventId(eventId:)")));

/**
 * Get activities by organizer.
 */
- (NSArray<SharedActivity_ *> *)getActivitiesByOrganizerEventId:(NSString *)eventId organizerId:(NSString *)organizerId __attribute__((swift_name("getActivitiesByOrganizer(eventId:organizerId:)")));

/**
 * Get all activities a participant is registered for.
 */
- (NSArray<SharedActivity_ *> *)getActivitiesByParticipantEventId:(NSString *)eventId participantId:(NSString *)participantId __attribute__((swift_name("getActivitiesByParticipant(eventId:participantId:)")));

/**
 * Get activities by scenario.
 */
- (NSArray<SharedActivity_ *> *)getActivitiesByScenarioEventId:(NSString *)eventId scenarioId:(NSString *)scenarioId __attribute__((swift_name("getActivitiesByScenario(eventId:scenarioId:)")));

/**
 * Get activities without a date set.
 */
- (NSArray<SharedActivity_ *> *)getActivitiesWithoutDateEventId:(NSString *)eventId __attribute__((swift_name("getActivitiesWithoutDate(eventId:)")));

/**
 * Get activity by ID.
 */
- (SharedActivity_ * _Nullable)getActivityByIdActivityId:(NSString *)activityId __attribute__((swift_name("getActivityById(activityId:)")));

/**
 * Get activity with statistics.
 */
- (SharedActivityWithStats * _Nullable)getActivityWithStatsActivityId:(NSString *)activityId __attribute__((swift_name("getActivityWithStats(activityId:)")));

/**
 * Get participant IDs for an activity.
 */
- (NSArray<NSString *> *)getParticipantIdsByActivityActivityId:(NSString *)activityId __attribute__((swift_name("getParticipantIdsByActivity(activityId:)")));

/**
 * Get all participants registered for an activity.
 */
- (NSArray<SharedActivityParticipant *> *)getParticipantsByActivityActivityId:(NSString *)activityId __attribute__((swift_name("getParticipantsByActivity(activityId:)")));

/**
 * Check if participant is already registered.
 */
- (BOOL)isParticipantRegisteredActivityId:(NSString *)activityId participantId:(NSString *)participantId __attribute__((swift_name("isParticipantRegistered(activityId:participantId:)")));

/**
 * Register a participant to an activity.
 */
- (SharedActivityParticipant *)registerParticipantRegistration:(SharedActivityParticipant *)registration __attribute__((swift_name("registerParticipant(registration:)")));

/**
 * Sum activity cost by date.
 */
- (int64_t)sumActivityCostByDateEventId:(NSString *)eventId date:(NSString *)date __attribute__((swift_name("sumActivityCostByDate(eventId:date:)")));

/**
 * Sum activity cost by event.
 */
- (int64_t)sumActivityCostByEventEventId:(NSString *)eventId __attribute__((swift_name("sumActivityCostByEvent(eventId:)")));

/**
 * Unregister a participant from an activity.
 */
- (void)unregisterParticipantActivityId:(NSString *)activityId participantId:(NSString *)participantId __attribute__((swift_name("unregisterParticipant(activityId:participantId:)")));

/**
 * Update an existing activity.
 *
 * @param activity Activity with updated fields
 * @return Updated Activity
 */
- (SharedActivity_ *)updateActivityActivity:(SharedActivity_ *)activity __attribute__((swift_name("updateActivity(activity:)")));

/**
 * Update activity capacity.
 */
- (SharedActivity_ * _Nullable)updateActivityCapacityActivityId:(NSString *)activityId maxParticipants:(SharedInt * _Nullable)maxParticipants __attribute__((swift_name("updateActivityCapacity(activityId:maxParticipants:)")));

/**
 * Update activity date and time.
 */
- (SharedActivity_ * _Nullable)updateActivityDateActivityId:(NSString *)activityId date:(NSString * _Nullable)date time:(NSString * _Nullable)time __attribute__((swift_name("updateActivityDate(activityId:date:time:)")));
@end


/**
 * Result of a registration attempt
 */
__attribute__((swift_name("RegistrationResult")))
@interface SharedRegistrationResult : SharedBase
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RegistrationResult.AlreadyRegistered")))
@interface SharedRegistrationResultAlreadyRegistered : SharedRegistrationResult
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)alreadyRegistered __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedRegistrationResultAlreadyRegistered *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RegistrationResult.Full")))
@interface SharedRegistrationResultFull : SharedRegistrationResult
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)full __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedRegistrationResultFull *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RegistrationResult.Success")))
@interface SharedRegistrationResultSuccess : SharedRegistrationResult
- (instancetype)initWithActivity:(SharedActivity_ *)activity registration:(SharedActivityParticipant *)registration __attribute__((swift_name("init(activity:registration:)"))) __attribute__((objc_designated_initializer));
- (SharedRegistrationResultSuccess *)doCopyActivity:(SharedActivity_ *)activity registration:(SharedActivityParticipant *)registration __attribute__((swift_name("doCopy(activity:registration:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedActivity_ *activity __attribute__((swift_name("activity")));
@property (readonly) SharedActivityParticipant *registration __attribute__((swift_name("registration")));
@end


/**
 * Validation result data class
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ValidationResult")))
@interface SharedValidationResult : SharedBase
- (instancetype)initWithIsValid:(BOOL)isValid errors:(NSArray<NSString *> *)errors __attribute__((swift_name("init(isValid:errors:)"))) __attribute__((objc_designated_initializer));
- (SharedValidationResult *)doCopyIsValid:(BOOL)isValid errors:(NSArray<NSString *> *)errors __attribute__((swift_name("doCopy(isValid:errors:)")));

/**
 * Validation result data class
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Validation result data class
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Validation result data class
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<NSString *> *errors __attribute__((swift_name("errors")));
@property (readonly) BOOL isValid __attribute__((swift_name("isValid")));
@end


/**
 * Authentication state sealed class.
 *
 * Represents all possible authentication states in the application.
 */
__attribute__((swift_name("AuthState")))
@interface SharedAuthState : SharedBase
@end


/**
 * User is successfully authenticated.
 *
 * @property userId The unique user identifier
 * @property user The user profile information
 * @property sessionId The current session identifier
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AuthState.Authenticated")))
@interface SharedAuthStateAuthenticated : SharedAuthState
- (instancetype)initWithUserId:(NSString *)userId user:(SharedUserResponse *)user sessionId:(NSString *)sessionId __attribute__((swift_name("init(userId:user:sessionId:)"))) __attribute__((objc_designated_initializer));
- (SharedAuthStateAuthenticated *)doCopyUserId:(NSString *)userId user:(SharedUserResponse *)user sessionId:(NSString *)sessionId __attribute__((swift_name("doCopy(userId:user:sessionId:)")));

/**
 * User is successfully authenticated.
 *
 * @property userId The unique user identifier
 * @property user The user profile information
 * @property sessionId The current session identifier
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * User is successfully authenticated.
 *
 * @property userId The unique user identifier
 * @property user The user profile information
 * @property sessionId The current session identifier
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * User is successfully authenticated.
 *
 * @property userId The unique user identifier
 * @property user The user profile information
 * @property sessionId The current session identifier
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *sessionId __attribute__((swift_name("sessionId")));
@property (readonly) SharedUserResponse *user __attribute__((swift_name("user")));
@property (readonly) NSString *userId __attribute__((swift_name("userId")));
@end


/**
 * Authentication error occurred.
 *
 * @property message User-friendly error message
 * @property code Error code for debugging
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AuthState.Error")))
@interface SharedAuthStateError : SharedAuthState
- (instancetype)initWithMessage:(NSString *)message code:(SharedErrorCode *)code __attribute__((swift_name("init(message:code:)"))) __attribute__((objc_designated_initializer));
- (SharedAuthStateError *)doCopyMessage:(NSString *)message code:(SharedErrorCode *)code __attribute__((swift_name("doCopy(message:code:)")));

/**
 * Authentication error occurred.
 *
 * @property message User-friendly error message
 * @property code Error code for debugging
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Authentication error occurred.
 *
 * @property message User-friendly error message
 * @property code Error code for debugging
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Authentication error occurred.
 *
 * @property message User-friendly error message
 * @property code Error code for debugging
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedErrorCode *code __attribute__((swift_name("code")));
@property (readonly) NSString *message __attribute__((swift_name("message")));
@end


/**
 * Initial state while checking for existing authentication.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AuthState.Loading")))
@interface SharedAuthStateLoading : SharedAuthState
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Initial state while checking for existing authentication.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)loading __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAuthStateLoading *shared __attribute__((swift_name("shared")));
@end


/**
 * User is not authenticated.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AuthState.Unauthenticated")))
@interface SharedAuthStateUnauthenticated : SharedAuthState
+ (instancetype)alloc __attribute__((unavailable));

/**
 * User is not authenticated.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)unauthenticated __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAuthStateUnauthenticated *shared __attribute__((swift_name("shared")));
@end


/**
 * Central authentication state manager.
 *
 * This class manages the authentication state across the entire application using
 * StateFlow for reactive updates. It handles:
 * - Initial authentication check on app startup
 * - OAuth login flow coordination
 * - Token refresh and expiry handling
 * - Logout and session cleanup
 * - Feature flag integration for progressive rollout
 *
 * Usage:
 * ```kotlin
 * val authStateManager = AuthStateManager(secureStorage, authService)
 * val authState by authStateManager.authState.collectAsState()
 *
 * when (authState) {
 *     is AuthState.Loading -> LoadingScreen()
 *     is AuthState.Unauthenticated -> LoginScreen()
 *     is AuthState.Authenticated -> MainApp()
 *     is AuthState.Error -> ErrorScreen()
 * }
 * ```
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AuthStateManager")))
@interface SharedAuthStateManager : SharedBase
- (instancetype)initWithSecureStorage:(id<SharedSecureTokenStorage>)secureStorage authService:(SharedClientAuthenticationService *)authService enableOAuth:(BOOL)enableOAuth __attribute__((swift_name("init(secureStorage:authService:enableOAuth:)"))) __attribute__((objc_designated_initializer));

/**
 * Clean up resources.
 */
- (void)dispose __attribute__((swift_name("dispose()")));

/**
 * Get the current access token (if authenticated).
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getCurrentAccessTokenWithCompletionHandler:(void (^)(NSString * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getCurrentAccessToken(completionHandler:)")));

/**
 * Get the current user ID (if authenticated).
 */
- (NSString * _Nullable)getCurrentUserId __attribute__((swift_name("getCurrentUserId()")));

/**
 * Handle token expiry (typically called from API error handlers).
 *
 * Attempts to refresh the token. If refresh fails, logs out the user.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)handleTokenExpiredWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("handleTokenExpired(completionHandler:)")));

/**
 * Initialize authentication state.
 *
 * This should be called when the app starts. It checks for existing
 * authentication and validates the stored tokens.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)initializeWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("initialize(completionHandler:)")));

/**
 * Login with OAuth authorization code.
 *
 * @param authCode The authorization code from OAuth provider
 * @param provider The OAuth provider (Google, Apple)
 * @return Result indicating success or failure
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)loginAuthCode:(NSString *)authCode provider:(SharedOAuthProvider *)provider completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("login(authCode:provider:completionHandler:)")));

/**
 * Logout the current user.
 *
 * Clears all stored tokens and resets state to Unauthenticated.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)logoutWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("logout(completionHandler:)")));

/**
 * Refresh token if needed.
 *
 * This proactively refreshes the access token before it expires.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)refreshTokenIfNeededWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("refreshTokenIfNeeded(completionHandler:)")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> authState __attribute__((swift_name("authState")));
@end


/**
 * Common authentication service interface for client-side OAuth2
 */
__attribute__((swift_name("ClientAuthenticationService")))
@interface SharedClientAuthenticationService : SharedBase
- (instancetype)initWithSecureStorage:(id<SharedSecureTokenStorage>)secureStorage baseUrl:(NSString *)baseUrl __attribute__((swift_name("init(secureStorage:baseUrl:)"))) __attribute__((objc_designated_initializer));

/**
 * Get stored access token
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getStoredAccessTokenWithCompletionHandler:(void (^)(NSString * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getStoredAccessToken(completionHandler:)")));

/**
 * Check if user is logged in
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)isLoggedInWithCompletionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("isLoggedIn(completionHandler:)")));

/**
 * Login with Apple OAuth2
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)loginWithAppleAuthorizationCode:(NSString *)authorizationCode userInfo:(NSString * _Nullable)userInfo completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("loginWithApple(authorizationCode:userInfo:completionHandler:)")));

/**
 * Login with Google OAuth2
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)loginWithGoogleAuthorizationCode:(NSString *)authorizationCode completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("loginWithGoogle(authorizationCode:completionHandler:)")));

/**
 * Logout and clear stored tokens
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)logoutWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("logout(completionHandler:)")));

/**
 * Helper method to make OAuth login request to server
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)performLoginRequestRequest:(SharedOAuthLoginRequest *)request completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("performLoginRequest(request:completionHandler:)")));

/**
 * Refresh the current access token
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)refreshTokenWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("refreshToken(completionHandler:)")));

/**
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property (readonly) NSString *baseUrl __attribute__((swift_name("baseUrl")));

/**
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property (readonly) id<SharedSecureTokenStorage> secureStorage __attribute__((swift_name("secureStorage")));
@end

__attribute__((swift_name("KotlinComparable")))
@protocol SharedKotlinComparable
@required
- (int32_t)compareToOther:(id _Nullable)other __attribute__((swift_name("compareTo(other:)")));
@end

__attribute__((swift_name("KotlinEnum")))
@interface SharedKotlinEnum<E> : SharedBase <SharedKotlinComparable>
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKotlinEnumCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(E)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) int32_t ordinal __attribute__((swift_name("ordinal")));
@end


/**
 * Error codes for authentication failures.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ErrorCode")))
@interface SharedErrorCode : SharedKotlinEnum<SharedErrorCode *>
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Error codes for authentication failures.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedErrorCode *unknown __attribute__((swift_name("unknown")));
@property (class, readonly) SharedErrorCode *networkError __attribute__((swift_name("networkError")));
@property (class, readonly) SharedErrorCode *invalidCredentials __attribute__((swift_name("invalidCredentials")));
@property (class, readonly) SharedErrorCode *tokenExpired __attribute__((swift_name("tokenExpired")));
@property (class, readonly) SharedErrorCode *serverError __attribute__((swift_name("serverError")));
@property (class, readonly) SharedErrorCode *userCancelled __attribute__((swift_name("userCancelled")));
+ (SharedKotlinArray<SharedErrorCode *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedErrorCode *> *entries __attribute__((swift_name("entries")));
@end


/**
 * Fine-grained permissions for specific actions.
 *
 * Permissions are checked against user's roles to determine authorization.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Permission")))
@interface SharedPermission : SharedKotlinEnum<SharedPermission *>
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Fine-grained permissions for specific actions.
 *
 * Permissions are checked against user's roles to determine authorization.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedPermissionCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) SharedPermission *eventCreate __attribute__((swift_name("eventCreate")));
@property (class, readonly) SharedPermission *eventRead __attribute__((swift_name("eventRead")));
@property (class, readonly) SharedPermission *eventUpdateOwn __attribute__((swift_name("eventUpdateOwn")));
@property (class, readonly) SharedPermission *eventUpdateAny __attribute__((swift_name("eventUpdateAny")));
@property (class, readonly) SharedPermission *eventDeleteOwn __attribute__((swift_name("eventDeleteOwn")));
@property (class, readonly) SharedPermission *eventDeleteAny __attribute__((swift_name("eventDeleteAny")));
@property (class, readonly) SharedPermission *participantInvite __attribute__((swift_name("participantInvite")));
@property (class, readonly) SharedPermission *participantRemoveOwn __attribute__((swift_name("participantRemoveOwn")));
@property (class, readonly) SharedPermission *participantRemoveAny __attribute__((swift_name("participantRemoveAny")));
@property (class, readonly) SharedPermission *voteCreate __attribute__((swift_name("voteCreate")));
@property (class, readonly) SharedPermission *voteUpdateOwn __attribute__((swift_name("voteUpdateOwn")));
@property (class, readonly) SharedPermission *voteUpdateAny __attribute__((swift_name("voteUpdateAny")));
@property (class, readonly) SharedPermission *voteDeleteOwn __attribute__((swift_name("voteDeleteOwn")));
@property (class, readonly) SharedPermission *voteDeleteAny __attribute__((swift_name("voteDeleteAny")));
@property (class, readonly) SharedPermission *userRead __attribute__((swift_name("userRead")));
@property (class, readonly) SharedPermission *userUpdateOwn __attribute__((swift_name("userUpdateOwn")));
@property (class, readonly) SharedPermission *userUpdateAny __attribute__((swift_name("userUpdateAny")));
@property (class, readonly) SharedPermission *userDeleteOwn __attribute__((swift_name("userDeleteOwn")));
@property (class, readonly) SharedPermission *userDeleteAny __attribute__((swift_name("userDeleteAny")));
@property (class, readonly) SharedPermission *userBan __attribute__((swift_name("userBan")));
@property (class, readonly) SharedPermission *sessionReadOwn __attribute__((swift_name("sessionReadOwn")));
@property (class, readonly) SharedPermission *sessionReadAny __attribute__((swift_name("sessionReadAny")));
@property (class, readonly) SharedPermission *sessionRevokeOwn __attribute__((swift_name("sessionRevokeOwn")));
@property (class, readonly) SharedPermission *sessionRevokeAny __attribute__((swift_name("sessionRevokeAny")));
@property (class, readonly) SharedPermission *systemSettings __attribute__((swift_name("systemSettings")));
@property (class, readonly) SharedPermission *systemMetrics __attribute__((swift_name("systemMetrics")));
@property (class, readonly) SharedPermission *systemLogs __attribute__((swift_name("systemLogs")));
+ (SharedKotlinArray<SharedPermission *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedPermission *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Permission.Companion")))
@interface SharedPermissionCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedPermissionCompanion *shared __attribute__((swift_name("shared")));

/**
 * Parse permission from string (case-insensitive).
 */
- (SharedPermission * _Nullable)fromStringPermission:(NSString *)permission __attribute__((swift_name("fromString(permission:)")));
@end


/**
 * JWT claims for roles and permissions.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RBACClaims")))
@interface SharedRBACClaims : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * JWT claims for roles and permissions.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)rBACClaims __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedRBACClaims *shared __attribute__((swift_name("shared")));
@property (readonly) NSString *PERMISSIONS __attribute__((swift_name("PERMISSIONS")));
@property (readonly) NSString *ROLES __attribute__((swift_name("ROLES")));
@end


/**
 * Role-based permission mappings.
 *
 * Defines which permissions are granted to each role.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RolePermissions")))
@interface SharedRolePermissions : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Role-based permission mappings.
 *
 * Defines which permissions are granted to each role.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)rolePermissions __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedRolePermissions *shared __attribute__((swift_name("shared")));

/**
 * Get all permissions for a given role.
 */
- (NSSet<SharedPermission *> *)getPermissionsRole:(SharedUserRole *)role __attribute__((swift_name("getPermissions(role:)")));

/**
 * Check if a role has a specific permission.
 */
- (BOOL)hasPermissionRole:(SharedUserRole *)role permission:(SharedPermission *)permission __attribute__((swift_name("hasPermission(role:permission:)")));

/**
 * Check if any of the given roles has a specific permission.
 */
- (BOOL)hasPermissionRoles:(NSSet<SharedUserRole *> *)roles permission:(SharedPermission *)permission __attribute__((swift_name("hasPermission(roles:permission:)")));
@end


/**
 * Session management with automatic token rotation.
 *
 * This manager handles:
 * - Token lifecycle management
 * - Automatic token refresh
 * - Session validation and cleanup
 * - Multi-device session coordination
 *
 * Usage:
 * ```kotlin
 * val sessionManager = SessionManager(sessionRepository, authService)
 * sessionManager.startSession(userId, deviceId, deviceName, jwtToken, refreshToken)
 * sessionManager.startTokenRotation() // Automatic refresh
 * ```
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SessionManager")))
@interface SharedSessionManager : SharedBase
- (instancetype)initWithSessionRepository:(SharedSessionRepository *)sessionRepository scope:(id<SharedKotlinx_coroutines_coreCoroutineScope>)scope __attribute__((swift_name("init(sessionRepository:scope:)"))) __attribute__((objc_designated_initializer));

/**
 * Cleanup expired JWT blacklist entries.
 *
 * Should be called periodically in background.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)cleanupExpiredBlacklistWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("cleanupExpiredBlacklist(completionHandler:)")));

/**
 * Cleanup old sessions (remove expired/revoked sessions older than 30 days).
 *
 * Should be called periodically in background.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)cleanupOldSessionsWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("cleanupOldSessions(completionHandler:)")));

/**
 * Count active sessions for a user.
 *
 * @param userId The user ID
 * @return Result with session count
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)countActiveUserSessionsUserId:(NSString *)userId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("countActiveUserSessions(userId:completionHandler:)")));

/**
 * Clean up resources.
 */
- (void)dispose __attribute__((swift_name("dispose()")));

/**
 * End all other sessions except the current one.
 *
 * Useful for "logout from other devices" functionality.
 *
 * @param userId The user ID
 * @param reason Reason for ending sessions (default: "logout_others")
 * @return Result indicating success or failure
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)endAllOtherSessionsUserId:(NSString *)userId reason:(NSString *)reason completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("endAllOtherSessions(userId:reason:completionHandler:)")));

/**
 * End all sessions for a user (logout from all devices).
 *
 * @param userId The user ID
 * @param reason Reason for ending sessions (default: "logout_all")
 * @return Result indicating success or failure
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)endAllUserSessionsUserId:(NSString *)userId reason:(NSString *)reason completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("endAllUserSessions(userId:reason:completionHandler:)")));

/**
 * End the current session (logout).
 *
 * @param reason Reason for ending session (default: "logout")
 * @return Result indicating success or failure
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)endCurrentSessionReason:(NSString *)reason completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("endCurrentSession(reason:completionHandler:)")));

/**
 * Get all active sessions for a user.
 *
 * @param userId The user ID
 * @return Result with list of sessions
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getActiveUserSessionsUserId:(NSString *)userId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getActiveUserSessions(userId:completionHandler:)")));

/**
 * Start a new session.
 *
 * Creates a session record and starts token rotation monitoring.
 *
 * @param userId The user ID
 * @param deviceId Unique device identifier
 * @param deviceName Human-readable device name
 * @param jwtToken The JWT access token
 * @param refreshToken The refresh token
 * @param ipAddress Optional IP address
 * @param userAgent Optional user agent
 * @param tokenExpiryMs Token expiry time in milliseconds from epoch
 * @return Result with session ID on success
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)startSessionUserId:(NSString *)userId deviceId:(NSString *)deviceId deviceName:(NSString *)deviceName jwtToken:(NSString *)jwtToken refreshToken:(NSString *)refreshToken ipAddress:(NSString * _Nullable)ipAddress userAgent:(NSString * _Nullable)userAgent tokenExpiryMs:(int64_t)tokenExpiryMs completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("startSession(userId:deviceId:deviceName:jwtToken:refreshToken:ipAddress:userAgent:tokenExpiryMs:completionHandler:)")));

/**
 * Start automatic token rotation.
 *
 * Monitors token expiry and triggers refresh callback when token is about to expire.
 *
 * @param sessionId The session ID to monitor
 * @param tokenExpiryMs Token expiry time in milliseconds from epoch
 * @param refreshThresholdMs Refresh token when this many milliseconds remain (default: 5 minutes)
 * @param onTokenRefreshNeeded Callback when token needs refresh
 */
- (void)startTokenRotationSessionId:(NSString *)sessionId tokenExpiryMs:(int64_t)tokenExpiryMs refreshThresholdMs:(int64_t)refreshThresholdMs onTokenRefreshNeeded:(id<SharedKotlinSuspendFunction1>)onTokenRefreshNeeded __attribute__((swift_name("startTokenRotation(sessionId:tokenExpiryMs:refreshThresholdMs:onTokenRefreshNeeded:)")));

/**
 * Stop token rotation monitoring.
 */
- (void)stopTokenRotation __attribute__((swift_name("stopTokenRotation()")));

/**
 * Update session with new tokens after refresh.
 *
 * @param sessionId The session ID
 * @param newJwtToken New JWT access token
 * @param newRefreshToken New refresh token
 * @param newTokenExpiryMs New token expiry time in milliseconds
 * @return Result indicating success or failure
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)updateSessionTokensSessionId:(NSString *)sessionId newJwtToken:(NSString *)newJwtToken newRefreshToken:(NSString *)newRefreshToken newTokenExpiryMs:(int64_t)newTokenExpiryMs completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("updateSessionTokens(sessionId:newJwtToken:newRefreshToken:newTokenExpiryMs:completionHandler:)")));

/**
 * Validate if a JWT token is valid (not blacklisted).
 *
 * @param jwtToken The JWT token to validate
 * @return Result with true if valid, false if blacklisted
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)validateTokenJwtToken:(NSString *)jwtToken completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("validateToken(jwtToken:completionHandler:)")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> currentSessionId __attribute__((swift_name("currentSessionId")));
@end


/**
 * System roles for Role-Based Access Control (RBAC).
 *
 * Roles define broad categories of users with predefined sets of permissions.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UserRole")))
@interface SharedUserRole : SharedKotlinEnum<SharedUserRole *>
+ (instancetype)alloc __attribute__((unavailable));

/**
 * System roles for Role-Based Access Control (RBAC).
 *
 * Roles define broad categories of users with predefined sets of permissions.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedUserRoleCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) SharedUserRole *user __attribute__((swift_name("user")));
@property (class, readonly) SharedUserRole *organizer __attribute__((swift_name("organizer")));
@property (class, readonly) SharedUserRole *moderator __attribute__((swift_name("moderator")));
@property (class, readonly) SharedUserRole *admin __attribute__((swift_name("admin")));
+ (SharedKotlinArray<SharedUserRole *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedUserRole *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UserRole.Companion")))
@interface SharedUserRoleCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUserRoleCompanion *shared __attribute__((swift_name("shared")));

/**
 * Get default role for new users.
 */
- (SharedUserRole *)default __attribute__((swift_name("default()")));

/**
 * Parse role from string (case-insensitive).
 */
- (SharedUserRole * _Nullable)fromStringRole:(NSString *)role __attribute__((swift_name("fromString(role:)")));
@end


/**
 * Budget Calculator - Business logic for budget calculations.
 *
 * Handles:
 * - Automatic budget aggregation from items
 * - Category-wise calculations
 * - Per-person cost splitting
 * - Balance calculations between participants
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BudgetCalculator")))
@interface SharedBudgetCalculator : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Budget Calculator - Business logic for budget calculations.
 *
 * Handles:
 * - Automatic budget aggregation from items
 * - Category-wise calculations
 * - Per-person cost splitting
 * - Balance calculations between participants
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)budgetCalculator __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBudgetCalculator *shared __attribute__((swift_name("shared")));

/**
 * Calculate balances between all participants.
 * Positive balance = owes money, Negative balance = is owed money.
 *
 * @param items List of all budget items
 * @return Map of participantId to balance
 */
- (NSDictionary<NSString *, SharedDouble *> *)calculateBalancesItems:(NSArray<SharedBudgetItem_ *> *)items __attribute__((swift_name("calculateBalances(items:)")));

/**
 * Calculate budget breakdown for all categories.
 *
 * @param items List of budget items
 * @param totalEstimated Total estimated budget (for percentage calculation)
 * @return List of BudgetCategoryDetails for each category
 */
- (NSArray<SharedBudgetCategoryDetails *> *)calculateCategoryBreakdownItems:(NSArray<SharedBudgetItem_ *> *)items totalEstimated:(double)totalEstimated __attribute__((swift_name("calculateCategoryBreakdown(items:totalEstimated:)")));

/**
 * Calculate budget totals by category.
 *
 * @param items List of budget items
 * @param category Category to calculate for
 * @return Pair of (estimated, actual) for the category
 */
- (SharedKotlinPair<SharedDouble *, SharedDouble *> *)calculateCategoryBudgetItems:(NSArray<SharedBudgetItem_ *> *)items category:(SharedBudgetCategory *)category __attribute__((swift_name("calculateCategoryBudget(items:category:)")));

/**
 * Calculate budget usage percentage by category.
 *
 * @param budget Budget entity
 * @return Map of BudgetCategory to usage percentage
 */
- (NSDictionary<SharedBudgetCategory *, SharedDouble *> *)calculateCategoryUsagePercentagesBudget:(SharedBudget_ *)budget __attribute__((swift_name("calculateCategoryUsagePercentages(budget:)")));

/**
 * Calculate how much each participant owes for a specific item.
 *
 * @param item Budget item
 * @return Map of participantId to amount owed
 */
- (NSDictionary<NSString *, SharedDouble *> *)calculateItemSharePerParticipantItem:(SharedBudgetItem_ *)item __attribute__((swift_name("calculateItemSharePerParticipant(item:)")));

/**
 * Calculate budget share details for a specific participant.
 *
 * @param participantId ID of the participant
 * @param items List of all budget items
 * @return ParticipantBudgetShare with complete details
 */
- (SharedParticipantBudgetShare *)calculateParticipantBudgetShareParticipantId:(NSString *)participantId items:(NSArray<SharedBudgetItem_ *> *)items __attribute__((swift_name("calculateParticipantBudgetShare(participantId:items:)")));

/**
 * Calculate total amount each participant has paid.
 *
 * @param items List of all budget items
 * @return Map of participantId to total amount paid
 */
- (NSDictionary<NSString *, SharedDouble *> *)calculateParticipantPaymentsItems:(NSArray<SharedBudgetItem_ *> *)items __attribute__((swift_name("calculateParticipantPayments(items:)")));

/**
 * Calculate total amount each participant owes.
 *
 * @param items List of all budget items
 * @return Map of participantId to total amount owed
 */
- (NSDictionary<NSString *, SharedDouble *> *)calculateParticipantSharesItems:(NSArray<SharedBudgetItem_ *> *)items __attribute__((swift_name("calculateParticipantShares(items:)")));

/**
 * Calculate per-person budget for an event.
 *
 * @param budget Budget entity
 * @param participantCount Number of participants
 * @return Pair of (estimatedPerPerson, actualPerPerson)
 */
- (SharedKotlinPair<SharedDouble *, SharedDouble *> *)calculatePerPersonBudgetBudget:(SharedBudget_ *)budget participantCount:(int32_t)participantCount __attribute__((swift_name("calculatePerPersonBudget(budget:participantCount:)")));

/**
 * Calculate simplified debt settlements using a greedy algorithm.
 * Minimizes the number of transactions needed to settle all debts.
 *
 * @param items List of all budget items
 * @return List of (from, to, amount) tuples representing settlements
 */
- (NSArray<SharedKotlinTriple<NSString *, NSString *, SharedDouble *> *> *)calculateSettlementsItems:(NSArray<SharedBudgetItem_ *> *)items __attribute__((swift_name("calculateSettlements(items:)")));

/**
 * Calculate total budget from a list of budget items.
 *
 * @param items List of budget items
 * @return Pair of (totalEstimated, totalActual)
 */
- (SharedKotlinPair<SharedDouble *, SharedDouble *> *)calculateTotalBudgetItems:(NSArray<SharedBudgetItem_ *> *)items __attribute__((swift_name("calculateTotalBudget(items:)")));

/**
 * Find categories that are over budget.
 *
 * @param budget Budget entity
 * @return List of categories that exceeded their estimated budget
 */
- (NSArray<SharedBudgetCategory *> *)findOverBudgetCategoriesBudget:(SharedBudget_ *)budget __attribute__((swift_name("findOverBudgetCategories(budget:)")));

/**
 * Generate a budget summary report.
 *
 * @param budget Budget entity
 * @param items List of budget items
 * @param participantCount Number of participants
 * @return Human-readable summary string
 */
- (NSString *)generateBudgetSummaryBudget:(SharedBudget_ *)budget items:(NSArray<SharedBudgetItem_ *> *)items participantCount:(int32_t)participantCount __attribute__((swift_name("generateBudgetSummary(budget:items:participantCount:)")));

/**
 * Check if budget is within limits (not over budget).
 *
 * @param budget Budget to check
 * @return true if within budget, false if over budget
 */
- (BOOL)isWithinBudgetBudget:(SharedBudget_ *)budget __attribute__((swift_name("isWithinBudget(budget:)")));

/**
 * Update budget entity with new values calculated from items.
 * This creates a new Budget instance with updated totals.
 *
 * @param budget Current budget
 * @param items List of all budget items
 * @param updatedAt New timestamp for updatedAt field
 * @return Updated Budget instance
 */
- (SharedBudget_ *)updateBudgetFromItemsBudget:(SharedBudget_ *)budget items:(NSArray<SharedBudgetItem_ *> *)items updatedAt:(NSString *)updatedAt __attribute__((swift_name("updateBudgetFromItems(budget:items:updatedAt:)")));

/**
 * Validate a budget before creation/update.
 *
 * @param budget Budget to validate
 * @return List of validation errors (empty if valid)
 */
- (NSArray<NSString *> *)validateBudgetBudget:(SharedBudget_ *)budget __attribute__((swift_name("validateBudget(budget:)")));

/**
 * Validate a budget item before creation/update.
 *
 * @param item Budget item to validate
 * @return List of validation errors (empty if valid)
 */
- (NSArray<NSString *> *)validateBudgetItemItem:(SharedBudgetItem_ *)item __attribute__((swift_name("validateBudgetItem(item:)")));
@end


/**
 * Budget Repository - Manages budget and budget items persistence.
 *
 * Responsibilities:
 * - CRUD operations for budgets and budget items
 * - Auto-update budget totals when items change
 * - Aggregate calculations from database
 * - Map between SQLDelight entities and Kotlin models
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BudgetRepository")))
@interface SharedBudgetRepository : SharedBase
- (instancetype)initWithDb:(id<SharedWakevDb>)db __attribute__((swift_name("init(db:)"))) __attribute__((objc_designated_initializer));

/**
 * Count items in a budget.
 */
- (int64_t)countItemsBudgetId:(NSString *)budgetId __attribute__((swift_name("countItems(budgetId:)")));

/**
 * Count paid items.
 */
- (int64_t)countPaidItemsBudgetId:(NSString *)budgetId __attribute__((swift_name("countPaidItems(budgetId:)")));

/**
 * Create a new budget for an event.
 *
 * @param eventId ID of the event
 * @return Created Budget model
 */
- (SharedBudget_ *)createBudgetEventId:(NSString *)eventId __attribute__((swift_name("createBudget(eventId:)")));

/**
 * Create a new budget item.
 */
- (SharedBudgetItem_ *)createBudgetItemBudgetId:(NSString *)budgetId category:(SharedBudgetCategory *)category name:(NSString *)name description:(NSString *)description estimatedCost:(double)estimatedCost sharedBy:(NSArray<NSString *> *)sharedBy notes:(NSString *)notes __attribute__((swift_name("createBudgetItem(budgetId:category:name:description:estimatedCost:sharedBy:notes:)")));

/**
 * Delete budget and all its items (CASCADE).
 */
- (void)deleteBudgetBudgetId:(NSString *)budgetId __attribute__((swift_name("deleteBudget(budgetId:)")));

/**
 * Delete budget item.
 */
- (void)deleteBudgetItemItemId:(NSString *)itemId __attribute__((swift_name("deleteBudgetItem(itemId:)")));

/**
 * Get budget for an event.
 */
- (SharedBudget_ * _Nullable)getBudgetByEventIdEventId:(NSString *)eventId __attribute__((swift_name("getBudgetByEventId(eventId:)")));

/**
 * Get budget by ID.
 */
- (SharedBudget_ * _Nullable)getBudgetByIdBudgetId:(NSString *)budgetId __attribute__((swift_name("getBudgetById(budgetId:)")));

/**
 * Get budget item by ID.
 */
- (SharedBudgetItem_ * _Nullable)getBudgetItemByIdItemId:(NSString *)itemId __attribute__((swift_name("getBudgetItemById(itemId:)")));

/**
 * Get all items for a budget.
 */
- (NSArray<SharedBudgetItem_ *> *)getBudgetItemsBudgetId:(NSString *)budgetId __attribute__((swift_name("getBudgetItems(budgetId:)")));

/**
 * Get items by category.
 */
- (NSArray<SharedBudgetItem_ *> *)getBudgetItemsByCategoryBudgetId:(NSString *)budgetId category:(SharedBudgetCategory *)category __attribute__((swift_name("getBudgetItemsByCategory(budgetId:category:)")));

/**
 * Get budget with all its items.
 */
- (SharedBudgetWithItems * _Nullable)getBudgetWithItemsBudgetId:(NSString *)budgetId __attribute__((swift_name("getBudgetWithItems(budgetId:)")));

/**
 * Get items paid by a participant.
 */
- (NSArray<SharedBudgetItem_ *> *)getItemsPaidByBudgetId:(NSString *)budgetId participantId:(NSString *)participantId __attribute__((swift_name("getItemsPaidBy(budgetId:participantId:)")));

/**
 * Get items shared by a participant.
 */
- (NSArray<SharedBudgetItem_ *> *)getItemsSharedByParticipantBudgetId:(NSString *)budgetId participantId:(NSString *)participantId __attribute__((swift_name("getItemsSharedByParticipant(budgetId:participantId:)")));

/**
 * Get paid items.
 */
- (NSArray<SharedBudgetItem_ *> *)getPaidItemsBudgetId:(NSString *)budgetId __attribute__((swift_name("getPaidItems(budgetId:)")));

/**
 * Get balances for all participants in a budget.
 */
- (NSDictionary<NSString *, SharedDouble *> *)getParticipantBalancesBudgetId:(NSString *)budgetId __attribute__((swift_name("getParticipantBalances(budgetId:)")));

/**
 * Get budget share details for a participant.
 */
- (SharedParticipantBudgetShare *)getParticipantBudgetShareBudgetId:(NSString *)budgetId participantId:(NSString *)participantId __attribute__((swift_name("getParticipantBudgetShare(budgetId:participantId:)")));

/**
 * Get settlement suggestions for a budget.
 */
- (NSArray<SharedKotlinTriple<NSString *, NSString *, SharedDouble *> *> *)getSettlementsBudgetId:(NSString *)budgetId __attribute__((swift_name("getSettlements(budgetId:)")));

/**
 * Get unpaid items.
 */
- (NSArray<SharedBudgetItem_ *> *)getUnpaidItemsBudgetId:(NSString *)budgetId __attribute__((swift_name("getUnpaidItems(budgetId:)")));

/**
 * Mark item as paid.
 */
- (SharedBudgetItem_ *)markItemAsPaidItemId:(NSString *)itemId actualCost:(double)actualCost paidBy:(NSString *)paidBy __attribute__((swift_name("markItemAsPaid(itemId:actualCost:paidBy:)")));

/**
 * Recalculate and update budget totals from items.
 * Called after any item is added/updated/deleted.
 */
- (SharedBudget_ * _Nullable)recalculateBudgetBudgetId:(NSString *)budgetId __attribute__((swift_name("recalculateBudget(budgetId:)")));

/**
 * Sum actual costs by category.
 */
- (double)sumActualByCategoryBudgetId:(NSString *)budgetId category:(SharedBudgetCategory *)category __attribute__((swift_name("sumActualByCategory(budgetId:category:)")));

/**
 * Sum estimated costs by category.
 */
- (double)sumEstimatedByCategoryBudgetId:(NSString *)budgetId category:(SharedBudgetCategory *)category __attribute__((swift_name("sumEstimatedByCategory(budgetId:category:)")));

/**
 * Update budget.
 */
- (SharedBudget_ *)updateBudgetBudget:(SharedBudget_ *)budget __attribute__((swift_name("updateBudget(budget:)")));

/**
 * Update budget item.
 */
- (SharedBudgetItem_ *)updateBudgetItemItem:(SharedBudgetItem_ *)item __attribute__((swift_name("updateBudgetItem(item:)")));
@end

__attribute__((swift_name("WakevDb")))
@protocol SharedWakevDb <SharedRuntimeTransacter>
@required
@property (readonly) SharedAccommodationQueries *accommodationQueries __attribute__((swift_name("accommodationQueries")));
@property (readonly) SharedActivityParticipantQueries *activityParticipantQueries __attribute__((swift_name("activityParticipantQueries")));
@property (readonly) SharedActivityQueries *activityQueries __attribute__((swift_name("activityQueries")));
@property (readonly) SharedBudgetItemQueries *budgetItemQueries __attribute__((swift_name("budgetItemQueries")));
@property (readonly) SharedBudgetQueries *budgetQueries __attribute__((swift_name("budgetQueries")));
@property (readonly) SharedConfirmedDateQueries *confirmedDateQueries __attribute__((swift_name("confirmedDateQueries")));
@property (readonly) SharedEquipmentItemQueries *equipmentItemQueries __attribute__((swift_name("equipmentItemQueries")));
@property (readonly) SharedEventQueries *eventQueries __attribute__((swift_name("eventQueries")));
@property (readonly) SharedMealQueries *mealQueries __attribute__((swift_name("mealQueries")));
@property (readonly) SharedParticipantDietaryRestrictionQueries *participantDietaryRestrictionQueries __attribute__((swift_name("participantDietaryRestrictionQueries")));
@property (readonly) SharedParticipantQueries *participantQueries __attribute__((swift_name("participantQueries")));
@property (readonly) SharedRoomAssignmentQueries *roomAssignmentQueries __attribute__((swift_name("roomAssignmentQueries")));
@property (readonly) SharedScenarioQueries *scenarioQueries __attribute__((swift_name("scenarioQueries")));
@property (readonly) SharedScenarioVoteQueries *scenarioVoteQueries __attribute__((swift_name("scenarioVoteQueries")));
@property (readonly) SharedSessionQueries *sessionQueries __attribute__((swift_name("sessionQueries")));
@property (readonly) SharedSyncMetadataQueries *syncMetadataQueries __attribute__((swift_name("syncMetadataQueries")));
@property (readonly) SharedTimeSlotQueries *timeSlotQueries __attribute__((swift_name("timeSlotQueries")));
@property (readonly) SharedUserPreferencesQueries *userPreferencesQueries __attribute__((swift_name("userPreferencesQueries")));
@property (readonly) SharedUserQueries *userQueries __attribute__((swift_name("userQueries")));
@property (readonly) SharedVoteQueries *voteQueries __attribute__((swift_name("voteQueries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WakevDbCompanion")))
@interface SharedWakevDbCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedWakevDbCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedWakevDb>)invokeDriver:(id<SharedRuntimeSqlDriver>)driver __attribute__((swift_name("invoke(driver:)")));
@property (readonly) id<SharedRuntimeSqlSchema> Schema __attribute__((swift_name("Schema")));
@end


/**
 * Service for equipment checklist management
 *
 * This service provides business logic for:
 * - Creating equipment items
 * - Auto-generating checklists by event type
 * - Assigning items to participants
 * - Tracking equipment status (NEEDED → ASSIGNED → CONFIRMED → PACKED)
 * - Calculating checklist statistics
 * - Validating equipment data
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EquipmentManager")))
@interface SharedEquipmentManager : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Service for equipment checklist management
 *
 * This service provides business logic for:
 * - Creating equipment items
 * - Auto-generating checklists by event type
 * - Assigning items to participants
 * - Tracking equipment status (NEEDED → ASSIGNED → CONFIRMED → PACKED)
 * - Calculating checklist statistics
 * - Validating equipment data
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)equipmentManager __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedEquipmentManager *shared __attribute__((swift_name("shared")));

/**
 * Assign equipment item to a participant
 */
- (SharedEquipmentItem *)assignEquipmentItem:(SharedEquipmentItem *)item participantId:(NSString *)participantId updateStatus:(BOOL)updateStatus __attribute__((swift_name("assignEquipment(item:participantId:updateStatus:)")));

/**
 * Calculate checklist statistics
 */
- (SharedEquipmentChecklist *)calculateChecklistStatsEventId:(NSString *)eventId items:(NSArray<SharedEquipmentItem *> *)items __attribute__((swift_name("calculateChecklistStats(eventId:items:)")));

/**
 * Calculate participant equipment statistics
 */
- (SharedParticipantEquipmentStats *)calculateParticipantStatsItems:(NSArray<SharedEquipmentItem *> *)items participantId:(NSString *)participantId __attribute__((swift_name("calculateParticipantStats(items:participantId:)")));

/**
 * Auto-generate equipment checklist based on event type
 *
 * Creates a comprehensive equipment list tailored to the event type.
 *
 * @param eventId Event ID
 * @param eventType Type of event
 * @return List of generated equipment items
 */
- (NSArray<SharedEquipmentItem *> *)createChecklistEventId:(NSString *)eventId eventType:(NSString *)eventType __attribute__((swift_name("createChecklist(eventId:eventType:)")));

/**
 * Create a new equipment item
 */
- (SharedEquipmentItem *)createEquipmentItemEventId:(NSString *)eventId name:(NSString *)name category:(SharedEquipmentCategory *)category quantity:(int32_t)quantity assignedTo:(NSString * _Nullable)assignedTo status:(SharedItemStatus *)status sharedCost:(SharedLong * _Nullable)sharedCost notes:(NSString * _Nullable)notes __attribute__((swift_name("createEquipmentItem(eventId:name:category:quantity:assignedTo:status:sharedCost:notes:)")));

/**
 * Group equipment by category
 */
- (NSArray<SharedEquipmentByCategory *> *)groupByCategoryItems:(NSArray<SharedEquipmentItem *> *)items __attribute__((swift_name("groupByCategory(items:)")));

/**
 * Track equipment status through lifecycle
 */
- (SharedEquipmentItem *)trackEquipmentStatusItem:(SharedEquipmentItem *)item newStatus:(SharedItemStatus *)newStatus __attribute__((swift_name("trackEquipmentStatus(item:newStatus:)")));

/**
 * Unassign equipment item
 */
- (SharedEquipmentItem *)unassignEquipmentItem:(SharedEquipmentItem *)item __attribute__((swift_name("unassignEquipment(item:)")));

/**
 * Validate equipment item data
 */
- (SharedValidationResult_ *)validateEquipmentItemName:(NSString *)name quantity:(int32_t)quantity sharedCost:(SharedLong * _Nullable)sharedCost __attribute__((swift_name("validateEquipmentItem(name:quantity:sharedCost:)")));
@end


/**
 * Equipment Repository - Manages equipment item persistence.
 *
 * Responsibilities:
 * - CRUD operations for equipment items
 * - Equipment queries and filtering
 * - Statistics and aggregations
 * - Map between SQLDelight entities and Kotlin models
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EquipmentRepository")))
@interface SharedEquipmentRepository : SharedBase
- (instancetype)initWithDb:(id<SharedWakevDb>)db __attribute__((swift_name("init(db:)"))) __attribute__((objc_designated_initializer));

/**
 * Count equipment items by category.
 */
- (int64_t)countEquipmentItemsByCategoryEventId:(NSString *)eventId category:(SharedEquipmentCategory *)category __attribute__((swift_name("countEquipmentItemsByCategory(eventId:category:)")));

/**
 * Count equipment items for an event.
 */
- (int64_t)countEquipmentItemsByEventEventId:(NSString *)eventId __attribute__((swift_name("countEquipmentItemsByEvent(eventId:)")));

/**
 * Count equipment items by status.
 */
- (int64_t)countEquipmentItemsByStatusEventId:(NSString *)eventId status:(SharedItemStatus *)status __attribute__((swift_name("countEquipmentItemsByStatus(eventId:status:)")));

/**
 * Create a new equipment item.
 *
 * @param item Equipment item to create
 * @return Created EquipmentItem
 */
- (SharedEquipmentItem *)createEquipmentItemItem:(SharedEquipmentItem *)item __attribute__((swift_name("createEquipmentItem(item:)")));

/**
 * Delete an equipment item.
 */
- (void)deleteEquipmentItemItemId:(NSString *)itemId __attribute__((swift_name("deleteEquipmentItem(itemId:)")));

/**
 * Delete all equipment items for an event.
 */
- (void)deleteEquipmentItemsByEventEventId:(NSString *)eventId __attribute__((swift_name("deleteEquipmentItemsByEvent(eventId:)")));

/**
 * Check if an equipment item exists.
 */
- (BOOL)equipmentItemExistsItemId:(NSString *)itemId __attribute__((swift_name("equipmentItemExists(itemId:)")));

/**
 * Get overall equipment checklist statistics.
 */
- (SharedEquipmentChecklist *)getEquipmentChecklistEventId:(NSString *)eventId __attribute__((swift_name("getEquipmentChecklist(eventId:)")));

/**
 * Get equipment item by ID.
 */
- (SharedEquipmentItem * _Nullable)getEquipmentItemByIdItemId:(NSString *)itemId __attribute__((swift_name("getEquipmentItemById(itemId:)")));

/**
 * Get equipment items assigned to a participant.
 */
- (NSArray<SharedEquipmentItem *> *)getEquipmentItemsByAssigneeEventId:(NSString *)eventId participantId:(NSString *)participantId __attribute__((swift_name("getEquipmentItemsByAssignee(eventId:participantId:)")));

/**
 * Get equipment items by category.
 */
- (NSArray<SharedEquipmentItem *> *)getEquipmentItemsByCategoryEventId:(NSString *)eventId category:(SharedEquipmentCategory *)category __attribute__((swift_name("getEquipmentItemsByCategory(eventId:category:)")));

/**
 * Get all equipment items for an event.
 */
- (NSArray<SharedEquipmentItem *> *)getEquipmentItemsByEventIdEventId:(NSString *)eventId __attribute__((swift_name("getEquipmentItemsByEventId(eventId:)")));

/**
 * Get equipment items by status.
 */
- (NSArray<SharedEquipmentItem *> *)getEquipmentItemsByStatusEventId:(NSString *)eventId status:(SharedItemStatus *)status __attribute__((swift_name("getEquipmentItemsByStatus(eventId:status:)")));

/**
 * Get equipment statistics by assignee.
 */
- (NSArray<SharedParticipantEquipmentStats *> *)getEquipmentStatsByAssigneeEventId:(NSString *)eventId __attribute__((swift_name("getEquipmentStatsByAssignee(eventId:)")));

/**
 * Get equipment statistics by category.
 */
- (NSArray<SharedEquipmentByCategory *> *)getEquipmentStatsByCategoryEventId:(NSString *)eventId __attribute__((swift_name("getEquipmentStatsByCategory(eventId:)")));

/**
 * Get unassigned equipment items.
 */
- (NSArray<SharedEquipmentItem *> *)getUnassignedItemsEventId:(NSString *)eventId __attribute__((swift_name("getUnassignedItems(eventId:)")));

/**
 * Sum equipment cost by assignee.
 */
- (int64_t)sumEquipmentCostByAssigneeEventId:(NSString *)eventId participantId:(NSString *)participantId __attribute__((swift_name("sumEquipmentCostByAssignee(eventId:participantId:)")));

/**
 * Sum equipment cost by category.
 */
- (int64_t)sumEquipmentCostByCategoryEventId:(NSString *)eventId category:(SharedEquipmentCategory *)category __attribute__((swift_name("sumEquipmentCostByCategory(eventId:category:)")));

/**
 * Sum equipment cost by event.
 */
- (int64_t)sumEquipmentCostByEventEventId:(NSString *)eventId __attribute__((swift_name("sumEquipmentCostByEvent(eventId:)")));

/**
 * Update an existing equipment item.
 *
 * @param item Equipment item with updated fields
 * @return Updated EquipmentItem
 */
- (SharedEquipmentItem *)updateEquipmentItemItem:(SharedEquipmentItem *)item __attribute__((swift_name("updateEquipmentItem(item:)")));

/**
 * Update equipment item assignment.
 */
- (SharedEquipmentItem * _Nullable)updateEquipmentItemAssignmentItemId:(NSString *)itemId participantId:(NSString * _Nullable)participantId status:(SharedItemStatus *)status __attribute__((swift_name("updateEquipmentItemAssignment(itemId:participantId:status:)")));

/**
 * Update equipment item status.
 */
- (SharedEquipmentItem * _Nullable)updateEquipmentItemStatusItemId:(NSString *)itemId status:(SharedItemStatus *)status __attribute__((swift_name("updateEquipmentItemStatus(itemId:status:)")));
@end


/**
 * Validation result data class
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ValidationResult_")))
@interface SharedValidationResult_ : SharedBase
- (instancetype)initWithIsValid:(BOOL)isValid errors:(NSArray<NSString *> *)errors __attribute__((swift_name("init(isValid:errors:)"))) __attribute__((objc_designated_initializer));
- (SharedValidationResult_ *)doCopyIsValid:(BOOL)isValid errors:(NSArray<NSString *> *)errors __attribute__((swift_name("doCopy(isValid:errors:)")));

/**
 * Validation result data class
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Validation result data class
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Validation result data class
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<NSString *> *errors __attribute__((swift_name("errors")));
@property (readonly) BOOL isValid __attribute__((swift_name("isValid")));
@end


/**
 * Service for meal planning and management
 *
 * This service provides business logic for:
 * - Creating and managing meals
 * - Auto-generating meal plans
 * - Managing dietary restrictions
 * - Calculating meal costs
 * - Assigning responsibilities
 * - Validating meal data
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MealPlanner")))
@interface SharedMealPlanner : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Service for meal planning and management
 *
 * This service provides business logic for:
 * - Creating and managing meals
 * - Auto-generating meal plans
 * - Managing dietary restrictions
 * - Calculating meal costs
 * - Assigning responsibilities
 * - Validating meal data
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)mealPlanner __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMealPlanner *shared __attribute__((swift_name("shared")));

/**
 * Check if meals cover dietary restrictions
 *
 * Identifies which restrictions are not being accommodated.
 *
 * @param meals List of planned meals
 * @param restrictions List of dietary restrictions
 * @return Map of restriction to count of participants with that restriction
 */
- (NSDictionary<SharedDietaryRestriction *, SharedInt *> *)analyzeRestrictionCoverageMeals:(NSArray<SharedMeal_ *> *)meals restrictions:(NSArray<SharedParticipantDietaryRestriction *> *)restrictions __attribute__((swift_name("analyzeRestrictionCoverage(meals:restrictions:)")));

/**
 * Auto-generate meals for an event
 *
 * Creates a complete meal plan from start to end date.
 *
 * @param request Auto-meal plan configuration
 * @return List of generated meals
 */
- (NSArray<SharedMeal_ *> *)autoGenerateMealsRequest:(SharedAutoMealPlanRequest *)request __attribute__((swift_name("autoGenerateMeals(request:)")));

/**
 * Calculate cost per person for meals
 *
 * @param totalCost Total meal cost in cents
 * @param participantCount Number of participants
 * @return Cost per person in cents
 */
- (int64_t)calculateCostPerPersonTotalCost:(int64_t)totalCost participantCount:(int32_t)participantCount __attribute__((swift_name("calculateCostPerPerson(totalCost:participantCount:)")));

/**
 * Calculate meal statistics
 */
- (NSDictionary<NSString *, id> *)calculateMealStatsMeals:(NSArray<SharedMeal_ *> *)meals __attribute__((swift_name("calculateMealStats(meals:)")));

/**
 * Calculate total meal cost for an event
 *
 * @param meals List of meals
 * @param useActual If true, use actual costs where available
 * @return Total cost in cents
 */
- (int64_t)calculateTotalMealCostMeals:(NSArray<SharedMeal_ *> *)meals useActual:(BOOL)useActual __attribute__((swift_name("calculateTotalMealCost(meals:useActual:)")));

/**
 * Count meals by participant
 *
 * @return Map of participant ID to number of meals assigned
 */
- (NSDictionary<NSString *, SharedInt *> *)countMealsByParticipantMeals:(NSArray<SharedMeal_ *> *)meals __attribute__((swift_name("countMealsByParticipant(meals:)")));

/**
 * Find meal conflicts (same date/time)
 */
- (NSArray<SharedKotlinPair<SharedMeal_ *, SharedMeal_ *> *> *)findMealConflictsMeals:(NSArray<SharedMeal_ *> *)meals __attribute__((swift_name("findMealConflicts(meals:)")));

/**
 * Generate meal planning summary
 */
- (SharedMealPlanningSummary *)generateMealSummaryMeals:(NSArray<SharedMeal_ *> *)meals __attribute__((swift_name("generateMealSummary(meals:)")));

/**
 * Get completed meals
 */
- (NSArray<SharedMeal_ *> *)getCompletedMealsMeals:(NSArray<SharedMeal_ *> *)meals __attribute__((swift_name("getCompletedMeals(meals:)")));

/**
 * Get current UTC timestamp in ISO 8601 format
 */
- (NSString *)getCurrentUtcIsoString __attribute__((swift_name("getCurrentUtcIsoString()")));

/**
 * Get default name for a meal type
 */
- (NSString *)getDefaultMealNameType:(SharedMealType *)type date:(NSString *)date __attribute__((swift_name("getDefaultMealName(type:date:)")));

/**
 * Get default time for a meal type
 */
- (NSString *)getDefaultMealTimeType:(SharedMealType *)type __attribute__((swift_name("getDefaultMealTime(type:)")));

/**
 * Get meals assigned to a participant
 */
- (NSArray<SharedMeal_ *> *)getMealsForParticipantMeals:(NSArray<SharedMeal_ *> *)meals participantId:(NSString *)participantId __attribute__((swift_name("getMealsForParticipant(meals:participantId:)")));

/**
 * Get meals that need assignment
 *
 * Returns meals with no responsible participants assigned.
 */
- (NSArray<SharedMeal_ *> *)getMealsNeedingAssignmentMeals:(NSArray<SharedMeal_ *> *)meals __attribute__((swift_name("getMealsNeedingAssignment(meals:)")));

/**
 * Get upcoming meals (not completed or cancelled)
 */
- (NSArray<SharedMeal_ *> *)getUpcomingMealsMeals:(NSArray<SharedMeal_ *> *)meals __attribute__((swift_name("getUpcomingMeals(meals:)")));

/**
 * Group meals by date
 *
 * @param meals List of meals
 * @return Map of date to meals for that date
 */
- (NSArray<SharedDailyMealSchedule *> *)groupMealsByDateMeals:(NSArray<SharedMeal_ *> *)meals __attribute__((swift_name("groupMealsByDate(meals:)")));

/**
 * Check if two meals overlap in time
 */
- (BOOL)mealsOverlapMeal1:(SharedMeal_ *)meal1 meal2:(SharedMeal_ *)meal2 __attribute__((swift_name("mealsOverlap(meal1:meal2:)")));

/**
 * Suggest meal assignments based on workload balance
 *
 * Distributes meal responsibilities evenly among participants.
 *
 * @param meals Meals to assign
 * @param participantIds Available participants
 * @param currentAssignments Current assignments to consider
 * @return Map of meal ID to suggested participant IDs
 */
- (NSDictionary<NSString *, NSArray<NSString *> *> *)suggestMealAssignmentsMeals:(NSArray<SharedMeal_ *> *)meals participantIds:(NSArray<NSString *> *)participantIds currentAssignments:(NSDictionary<NSString *, SharedInt *> *)currentAssignments __attribute__((swift_name("suggestMealAssignments(meals:participantIds:currentAssignments:)")));

/**
 * Validate dietary restriction
 */
- (NSString * _Nullable)validateDietaryRestrictionParticipantId:(NSString *)participantId eventId:(NSString *)eventId restriction:(SharedDietaryRestriction *)restriction __attribute__((swift_name("validateDietaryRestriction(participantId:eventId:restriction:)")));

/**
 * Validate meal data
 *
 * @return Validation error message, or null if valid
 */
- (NSString * _Nullable)validateMealName:(NSString *)name date:(NSString *)date time:(NSString *)time servings:(int32_t)servings estimatedCost:(int64_t)estimatedCost __attribute__((swift_name("validateMeal(name:date:time:servings:estimatedCost:)")));
@end


/**
 * Meal Repository - Manages meal and dietary restriction persistence.
 *
 * Responsibilities:
 * - CRUD operations for meals
 * - CRUD operations for dietary restrictions
 * - Meal queries and filtering
 * - Statistics and aggregations
 * - Map between SQLDelight entities and Kotlin models
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MealRepository")))
@interface SharedMealRepository : SharedBase
- (instancetype)initWithDb:(id<SharedWakevDb>)db __attribute__((swift_name("init(db:)"))) __attribute__((objc_designated_initializer));

/**
 * Add dietary restriction for a participant.
 */
- (SharedParticipantDietaryRestriction *)addDietaryRestrictionRequest:(SharedDietaryRestrictionRequest *)request __attribute__((swift_name("addDietaryRestriction(request:)")));

/**
 * Count meals by status.
 */
- (NSDictionary<SharedMealStatus *, SharedInt *> *)countMealsByStatusEventId:(NSString *)eventId __attribute__((swift_name("countMealsByStatus(eventId:)")));

/**
 * Count meals by type.
 */
- (NSDictionary<SharedMealType *, SharedInt *> *)countMealsByTypeEventId:(NSString *)eventId __attribute__((swift_name("countMealsByType(eventId:)")));

/**
 * Create a new meal.
 *
 * @param request Meal creation request
 * @return Created Meal
 */
- (SharedMeal_ *)createMealRequest:(SharedMealRequest *)request __attribute__((swift_name("createMeal(request:)")));

/**
 * Delete a dietary restriction.
 */
- (void)deleteDietaryRestrictionRestrictionId:(NSString *)restrictionId __attribute__((swift_name("deleteDietaryRestriction(restrictionId:)")));

/**
 * Delete all dietary restrictions for a participant.
 */
- (void)deleteDietaryRestrictionsByParticipantEventId:(NSString *)eventId participantId:(NSString *)participantId __attribute__((swift_name("deleteDietaryRestrictionsByParticipant(eventId:participantId:)")));

/**
 * Delete a meal.
 */
- (void)deleteMealMealId:(NSString *)mealId __attribute__((swift_name("deleteMeal(mealId:)")));

/**
 * Delete all meals for an event.
 */
- (void)deleteMealsByEventIdEventId:(NSString *)eventId __attribute__((swift_name("deleteMealsByEventId(eventId:)")));

/**
 * Get meals grouped by date (daily schedule).
 */
- (NSArray<SharedDailyMealSchedule *> *)getDailyMealScheduleEventId:(NSString *)eventId __attribute__((swift_name("getDailyMealSchedule(eventId:)")));

/**
 * Get count of participants with each dietary restriction.
 */
- (NSDictionary<SharedDietaryRestriction *, SharedInt *> *)getDietaryRestrictionCountsEventId:(NSString *)eventId __attribute__((swift_name("getDietaryRestrictionCounts(eventId:)")));

/**
 * Get all dietary restrictions for an event.
 */
- (NSArray<SharedParticipantDietaryRestriction *> *)getDietaryRestrictionsByEventIdEventId:(NSString *)eventId __attribute__((swift_name("getDietaryRestrictionsByEventId(eventId:)")));

/**
 * Get dietary restrictions for a specific participant.
 */
- (NSArray<SharedParticipantDietaryRestriction *> *)getDietaryRestrictionsByParticipantEventId:(NSString *)eventId participantId:(NSString *)participantId __attribute__((swift_name("getDietaryRestrictionsByParticipant(eventId:participantId:)")));

/**
 * Get meal by ID.
 */
- (SharedMeal_ * _Nullable)getMealByIdMealId:(NSString *)mealId __attribute__((swift_name("getMealById(mealId:)")));

/**
 * Calculate meal planning summary.
 */
- (SharedMealPlanningSummary *)getMealPlanningSummaryEventId:(NSString *)eventId __attribute__((swift_name("getMealPlanningSummary(eventId:)")));

/**
 * Get meal with dietary restrictions to consider.
 */
- (SharedMealWithRestrictions * _Nullable)getMealWithRestrictionsMealId:(NSString *)mealId __attribute__((swift_name("getMealWithRestrictions(mealId:)")));

/**
 * Get meals for a specific date.
 */
- (NSArray<SharedMeal_ *> *)getMealsByDateEventId:(NSString *)eventId date:(NSString *)date __attribute__((swift_name("getMealsByDate(eventId:date:)")));

/**
 * Get all meals for an event.
 */
- (NSArray<SharedMeal_ *> *)getMealsByEventIdEventId:(NSString *)eventId __attribute__((swift_name("getMealsByEventId(eventId:)")));

/**
 * Get meals by status.
 */
- (NSArray<SharedMeal_ *> *)getMealsByStatusEventId:(NSString *)eventId status:(SharedMealStatus *)status __attribute__((swift_name("getMealsByStatus(eventId:status:)")));

/**
 * Get meals by type.
 */
- (NSArray<SharedMeal_ *> *)getMealsByTypeEventId:(NSString *)eventId type:(SharedMealType *)type __attribute__((swift_name("getMealsByType(eventId:type:)")));

/**
 * Get total actual cost for completed meals.
 */
- (int64_t)getTotalActualCostEventId:(NSString *)eventId __attribute__((swift_name("getTotalActualCost(eventId:)")));

/**
 * Get total estimated cost for all meals.
 */
- (int64_t)getTotalEstimatedCostEventId:(NSString *)eventId __attribute__((swift_name("getTotalEstimatedCost(eventId:)")));

/**
 * Get upcoming meals (sorted by date, time).
 */
- (NSArray<SharedMeal_ *> *)getUpcomingMealsEventId:(NSString *)eventId limit:(int64_t)limit __attribute__((swift_name("getUpcomingMeals(eventId:limit:)")));

/**
 * Update an existing meal.
 */
- (SharedMeal_ *)updateMealMeal:(SharedMeal_ *)meal __attribute__((swift_name("updateMeal(meal:)")));
@end


/**
 * Accommodation for an event
 *
 * Represents a place where participants will stay during the event.
 * Can be a hotel, Airbnb, camping site, etc.
 *
 * @property id Unique identifier
 * @property eventId Event this accommodation belongs to
 * @property name Name of the accommodation (e.g., "Hotel California")
 * @property type Type of accommodation
 * @property address Full address
 * @property capacity Maximum number of people it can host
 * @property pricePerNight Price per night (in cents to avoid float precision issues)
 * @property totalNights Number of nights booked
 * @property totalCost Total cost (pricePerNight * totalNights in cents)
 * @property bookingStatus Current booking status
 * @property bookingUrl URL to booking page or confirmation
 * @property checkInDate Check-in date (ISO 8601 format)
 * @property checkOutDate Check-out date (ISO 8601 format)
 * @property notes Additional notes or special requirements
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Accommodation_")))
@interface SharedAccommodation_ : SharedBase
- (instancetype)initWithId:(NSString *)id eventId:(NSString *)eventId name:(NSString *)name type:(SharedAccommodationType *)type address:(NSString *)address capacity:(int32_t)capacity pricePerNight:(int64_t)pricePerNight totalNights:(int32_t)totalNights totalCost:(int64_t)totalCost bookingStatus:(SharedBookingStatus *)bookingStatus bookingUrl:(NSString * _Nullable)bookingUrl checkInDate:(NSString *)checkInDate checkOutDate:(NSString *)checkOutDate notes:(NSString * _Nullable)notes createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("init(id:eventId:name:type:address:capacity:pricePerNight:totalNights:totalCost:bookingStatus:bookingUrl:checkInDate:checkOutDate:notes:createdAt:updatedAt:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedAccommodation_Companion *companion __attribute__((swift_name("companion")));
- (SharedAccommodation_ *)doCopyId:(NSString *)id eventId:(NSString *)eventId name:(NSString *)name type:(SharedAccommodationType *)type address:(NSString *)address capacity:(int32_t)capacity pricePerNight:(int64_t)pricePerNight totalNights:(int32_t)totalNights totalCost:(int64_t)totalCost bookingStatus:(SharedBookingStatus *)bookingStatus bookingUrl:(NSString * _Nullable)bookingUrl checkInDate:(NSString *)checkInDate checkOutDate:(NSString *)checkOutDate notes:(NSString * _Nullable)notes createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("doCopy(id:eventId:name:type:address:capacity:pricePerNight:totalNights:totalCost:bookingStatus:bookingUrl:checkInDate:checkOutDate:notes:createdAt:updatedAt:)")));

/**
 * Accommodation for an event
 *
 * Represents a place where participants will stay during the event.
 * Can be a hotel, Airbnb, camping site, etc.
 *
 * @property id Unique identifier
 * @property eventId Event this accommodation belongs to
 * @property name Name of the accommodation (e.g., "Hotel California")
 * @property type Type of accommodation
 * @property address Full address
 * @property capacity Maximum number of people it can host
 * @property pricePerNight Price per night (in cents to avoid float precision issues)
 * @property totalNights Number of nights booked
 * @property totalCost Total cost (pricePerNight * totalNights in cents)
 * @property bookingStatus Current booking status
 * @property bookingUrl URL to booking page or confirmation
 * @property checkInDate Check-in date (ISO 8601 format)
 * @property checkOutDate Check-out date (ISO 8601 format)
 * @property notes Additional notes or special requirements
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Accommodation for an event
 *
 * Represents a place where participants will stay during the event.
 * Can be a hotel, Airbnb, camping site, etc.
 *
 * @property id Unique identifier
 * @property eventId Event this accommodation belongs to
 * @property name Name of the accommodation (e.g., "Hotel California")
 * @property type Type of accommodation
 * @property address Full address
 * @property capacity Maximum number of people it can host
 * @property pricePerNight Price per night (in cents to avoid float precision issues)
 * @property totalNights Number of nights booked
 * @property totalCost Total cost (pricePerNight * totalNights in cents)
 * @property bookingStatus Current booking status
 * @property bookingUrl URL to booking page or confirmation
 * @property checkInDate Check-in date (ISO 8601 format)
 * @property checkOutDate Check-out date (ISO 8601 format)
 * @property notes Additional notes or special requirements
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Accommodation for an event
 *
 * Represents a place where participants will stay during the event.
 * Can be a hotel, Airbnb, camping site, etc.
 *
 * @property id Unique identifier
 * @property eventId Event this accommodation belongs to
 * @property name Name of the accommodation (e.g., "Hotel California")
 * @property type Type of accommodation
 * @property address Full address
 * @property capacity Maximum number of people it can host
 * @property pricePerNight Price per night (in cents to avoid float precision issues)
 * @property totalNights Number of nights booked
 * @property totalCost Total cost (pricePerNight * totalNights in cents)
 * @property bookingStatus Current booking status
 * @property bookingUrl URL to booking page or confirmation
 * @property checkInDate Check-in date (ISO 8601 format)
 * @property checkOutDate Check-out date (ISO 8601 format)
 * @property notes Additional notes or special requirements
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *address __attribute__((swift_name("address")));
@property (readonly) SharedBookingStatus *bookingStatus __attribute__((swift_name("bookingStatus")));
@property (readonly) NSString * _Nullable bookingUrl __attribute__((swift_name("bookingUrl")));
@property (readonly) int32_t capacity __attribute__((swift_name("capacity")));
@property (readonly) NSString *checkInDate __attribute__((swift_name("checkInDate")));
@property (readonly) NSString *checkOutDate __attribute__((swift_name("checkOutDate")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) NSString * _Nullable notes __attribute__((swift_name("notes")));
@property (readonly) int64_t pricePerNight __attribute__((swift_name("pricePerNight")));
@property (readonly) int64_t totalCost __attribute__((swift_name("totalCost")));
@property (readonly) int32_t totalNights __attribute__((swift_name("totalNights")));
@property (readonly) SharedAccommodationType *type __attribute__((swift_name("type")));
@property (readonly) NSString *updatedAt __attribute__((swift_name("updatedAt")));
@end


/**
 * Accommodation for an event
 *
 * Represents a place where participants will stay during the event.
 * Can be a hotel, Airbnb, camping site, etc.
 *
 * @property id Unique identifier
 * @property eventId Event this accommodation belongs to
 * @property name Name of the accommodation (e.g., "Hotel California")
 * @property type Type of accommodation
 * @property address Full address
 * @property capacity Maximum number of people it can host
 * @property pricePerNight Price per night (in cents to avoid float precision issues)
 * @property totalNights Number of nights booked
 * @property totalCost Total cost (pricePerNight * totalNights in cents)
 * @property bookingStatus Current booking status
 * @property bookingUrl URL to booking page or confirmation
 * @property checkInDate Check-in date (ISO 8601 format)
 * @property checkOutDate Check-out date (ISO 8601 format)
 * @property notes Additional notes or special requirements
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Accommodation_.Companion")))
@interface SharedAccommodation_Companion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Accommodation for an event
 *
 * Represents a place where participants will stay during the event.
 * Can be a hotel, Airbnb, camping site, etc.
 *
 * @property id Unique identifier
 * @property eventId Event this accommodation belongs to
 * @property name Name of the accommodation (e.g., "Hotel California")
 * @property type Type of accommodation
 * @property address Full address
 * @property capacity Maximum number of people it can host
 * @property pricePerNight Price per night (in cents to avoid float precision issues)
 * @property totalNights Number of nights booked
 * @property totalCost Total cost (pricePerNight * totalNights in cents)
 * @property bookingStatus Current booking status
 * @property bookingUrl URL to booking page or confirmation
 * @property checkInDate Check-in date (ISO 8601 format)
 * @property checkOutDate Check-out date (ISO 8601 format)
 * @property notes Additional notes or special requirements
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAccommodation_Companion *shared __attribute__((swift_name("shared")));

/**
 * Accommodation for an event
 *
 * Represents a place where participants will stay during the event.
 * Can be a hotel, Airbnb, camping site, etc.
 *
 * @property id Unique identifier
 * @property eventId Event this accommodation belongs to
 * @property name Name of the accommodation (e.g., "Hotel California")
 * @property type Type of accommodation
 * @property address Full address
 * @property capacity Maximum number of people it can host
 * @property pricePerNight Price per night (in cents to avoid float precision issues)
 * @property totalNights Number of nights booked
 * @property totalCost Total cost (pricePerNight * totalNights in cents)
 * @property bookingStatus Current booking status
 * @property bookingUrl URL to booking page or confirmation
 * @property checkInDate Check-in date (ISO 8601 format)
 * @property checkOutDate Check-out date (ISO 8601 format)
 * @property notes Additional notes or special requirements
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Request to create or update an accommodation
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AccommodationRequest")))
@interface SharedAccommodationRequest : SharedBase
- (instancetype)initWithEventId:(NSString *)eventId name:(NSString *)name type:(SharedAccommodationType *)type address:(NSString *)address capacity:(int32_t)capacity pricePerNight:(int64_t)pricePerNight totalNights:(int32_t)totalNights bookingStatus:(SharedBookingStatus *)bookingStatus bookingUrl:(NSString * _Nullable)bookingUrl checkInDate:(NSString *)checkInDate checkOutDate:(NSString *)checkOutDate notes:(NSString * _Nullable)notes __attribute__((swift_name("init(eventId:name:type:address:capacity:pricePerNight:totalNights:bookingStatus:bookingUrl:checkInDate:checkOutDate:notes:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedAccommodationRequestCompanion *companion __attribute__((swift_name("companion")));
- (SharedAccommodationRequest *)doCopyEventId:(NSString *)eventId name:(NSString *)name type:(SharedAccommodationType *)type address:(NSString *)address capacity:(int32_t)capacity pricePerNight:(int64_t)pricePerNight totalNights:(int32_t)totalNights bookingStatus:(SharedBookingStatus *)bookingStatus bookingUrl:(NSString * _Nullable)bookingUrl checkInDate:(NSString *)checkInDate checkOutDate:(NSString *)checkOutDate notes:(NSString * _Nullable)notes __attribute__((swift_name("doCopy(eventId:name:type:address:capacity:pricePerNight:totalNights:bookingStatus:bookingUrl:checkInDate:checkOutDate:notes:)")));

/**
 * Request to create or update an accommodation
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Request to create or update an accommodation
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Request to create or update an accommodation
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *address __attribute__((swift_name("address")));
@property (readonly) SharedBookingStatus *bookingStatus __attribute__((swift_name("bookingStatus")));
@property (readonly) NSString * _Nullable bookingUrl __attribute__((swift_name("bookingUrl")));
@property (readonly) int32_t capacity __attribute__((swift_name("capacity")));
@property (readonly) NSString *checkInDate __attribute__((swift_name("checkInDate")));
@property (readonly) NSString *checkOutDate __attribute__((swift_name("checkOutDate")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) NSString * _Nullable notes __attribute__((swift_name("notes")));
@property (readonly) int64_t pricePerNight __attribute__((swift_name("pricePerNight")));
@property (readonly) int32_t totalNights __attribute__((swift_name("totalNights")));
@property (readonly) SharedAccommodationType *type __attribute__((swift_name("type")));
@end


/**
 * Request to create or update an accommodation
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AccommodationRequest.Companion")))
@interface SharedAccommodationRequestCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Request to create or update an accommodation
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAccommodationRequestCompanion *shared __attribute__((swift_name("shared")));

/**
 * Request to create or update an accommodation
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Type of accommodation
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AccommodationType")))
@interface SharedAccommodationType : SharedKotlinEnum<SharedAccommodationType *>
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Type of accommodation
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedAccommodationTypeCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) SharedAccommodationType *hotel __attribute__((swift_name("hotel")));
@property (class, readonly) SharedAccommodationType *airbnb __attribute__((swift_name("airbnb")));
@property (class, readonly) SharedAccommodationType *camping __attribute__((swift_name("camping")));
@property (class, readonly) SharedAccommodationType *hostel __attribute__((swift_name("hostel")));
@property (class, readonly) SharedAccommodationType *vacationRental __attribute__((swift_name("vacationRental")));
@property (class, readonly) SharedAccommodationType *other __attribute__((swift_name("other")));
+ (SharedKotlinArray<SharedAccommodationType *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedAccommodationType *> *entries __attribute__((swift_name("entries")));
@end


/**
 * Type of accommodation
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AccommodationType.Companion")))
@interface SharedAccommodationTypeCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Type of accommodation
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAccommodationTypeCompanion *shared __attribute__((swift_name("shared")));

/**
 * Type of accommodation
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));

/**
 * Type of accommodation
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializerTypeParamsSerializers:(SharedKotlinArray<id<SharedKotlinx_serialization_coreKSerializer>> *)typeParamsSerializers __attribute__((swift_name("serializer(typeParamsSerializers:)")));
@end


/**
 * Summary of accommodation with room assignments
 *
 * Used to present a complete view of accommodation with all rooms and assignments.
 *
 * @property accommodation The accommodation details
 * @property roomAssignments List of room assignments for this accommodation
 * @property totalAssignedParticipants Count of assigned participants
 * @property remainingCapacity Number of spots still available
 * @property averageCostPerPerson Average cost per assigned participant (in cents)
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AccommodationWithRooms")))
@interface SharedAccommodationWithRooms : SharedBase
- (instancetype)initWithAccommodation:(SharedAccommodation_ *)accommodation roomAssignments:(NSArray<SharedRoomAssignment *> *)roomAssignments totalAssignedParticipants:(int32_t)totalAssignedParticipants remainingCapacity:(int32_t)remainingCapacity averageCostPerPerson:(int64_t)averageCostPerPerson __attribute__((swift_name("init(accommodation:roomAssignments:totalAssignedParticipants:remainingCapacity:averageCostPerPerson:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedAccommodationWithRoomsCompanion *companion __attribute__((swift_name("companion")));
- (SharedAccommodationWithRooms *)doCopyAccommodation:(SharedAccommodation_ *)accommodation roomAssignments:(NSArray<SharedRoomAssignment *> *)roomAssignments totalAssignedParticipants:(int32_t)totalAssignedParticipants remainingCapacity:(int32_t)remainingCapacity averageCostPerPerson:(int64_t)averageCostPerPerson __attribute__((swift_name("doCopy(accommodation:roomAssignments:totalAssignedParticipants:remainingCapacity:averageCostPerPerson:)")));

/**
 * Summary of accommodation with room assignments
 *
 * Used to present a complete view of accommodation with all rooms and assignments.
 *
 * @property accommodation The accommodation details
 * @property roomAssignments List of room assignments for this accommodation
 * @property totalAssignedParticipants Count of assigned participants
 * @property remainingCapacity Number of spots still available
 * @property averageCostPerPerson Average cost per assigned participant (in cents)
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Summary of accommodation with room assignments
 *
 * Used to present a complete view of accommodation with all rooms and assignments.
 *
 * @property accommodation The accommodation details
 * @property roomAssignments List of room assignments for this accommodation
 * @property totalAssignedParticipants Count of assigned participants
 * @property remainingCapacity Number of spots still available
 * @property averageCostPerPerson Average cost per assigned participant (in cents)
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Summary of accommodation with room assignments
 *
 * Used to present a complete view of accommodation with all rooms and assignments.
 *
 * @property accommodation The accommodation details
 * @property roomAssignments List of room assignments for this accommodation
 * @property totalAssignedParticipants Count of assigned participants
 * @property remainingCapacity Number of spots still available
 * @property averageCostPerPerson Average cost per assigned participant (in cents)
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedAccommodation_ *accommodation __attribute__((swift_name("accommodation")));
@property (readonly) int64_t averageCostPerPerson __attribute__((swift_name("averageCostPerPerson")));
@property (readonly) int32_t remainingCapacity __attribute__((swift_name("remainingCapacity")));
@property (readonly) NSArray<SharedRoomAssignment *> *roomAssignments __attribute__((swift_name("roomAssignments")));
@property (readonly) int32_t totalAssignedParticipants __attribute__((swift_name("totalAssignedParticipants")));
@end


/**
 * Summary of accommodation with room assignments
 *
 * Used to present a complete view of accommodation with all rooms and assignments.
 *
 * @property accommodation The accommodation details
 * @property roomAssignments List of room assignments for this accommodation
 * @property totalAssignedParticipants Count of assigned participants
 * @property remainingCapacity Number of spots still available
 * @property averageCostPerPerson Average cost per assigned participant (in cents)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AccommodationWithRooms.Companion")))
@interface SharedAccommodationWithRoomsCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Summary of accommodation with room assignments
 *
 * Used to present a complete view of accommodation with all rooms and assignments.
 *
 * @property accommodation The accommodation details
 * @property roomAssignments List of room assignments for this accommodation
 * @property totalAssignedParticipants Count of assigned participants
 * @property remainingCapacity Number of spots still available
 * @property averageCostPerPerson Average cost per assigned participant (in cents)
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAccommodationWithRoomsCompanion *shared __attribute__((swift_name("shared")));

/**
 * Summary of accommodation with room assignments
 *
 * Used to present a complete view of accommodation with all rooms and assignments.
 *
 * @property accommodation The accommodation details
 * @property roomAssignments List of room assignments for this accommodation
 * @property totalAssignedParticipants Count of assigned participants
 * @property remainingCapacity Number of spots still available
 * @property averageCostPerPerson Average cost per assigned participant (in cents)
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Activities grouped by date
 *
 * @property date Date string (ISO 8601 format)
 * @property activities List of activities for this date
 * @property totalActivities Total count
 * @property totalCost Sum of all activity costs in cents
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ActivitiesByDate")))
@interface SharedActivitiesByDate : SharedBase
- (instancetype)initWithDate:(NSString *)date activities:(NSArray<SharedActivity_ *> *)activities totalActivities:(int32_t)totalActivities totalCost:(int64_t)totalCost __attribute__((swift_name("init(date:activities:totalActivities:totalCost:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedActivitiesByDateCompanion *companion __attribute__((swift_name("companion")));
- (SharedActivitiesByDate *)doCopyDate:(NSString *)date activities:(NSArray<SharedActivity_ *> *)activities totalActivities:(int32_t)totalActivities totalCost:(int64_t)totalCost __attribute__((swift_name("doCopy(date:activities:totalActivities:totalCost:)")));

/**
 * Activities grouped by date
 *
 * @property date Date string (ISO 8601 format)
 * @property activities List of activities for this date
 * @property totalActivities Total count
 * @property totalCost Sum of all activity costs in cents
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Activities grouped by date
 *
 * @property date Date string (ISO 8601 format)
 * @property activities List of activities for this date
 * @property totalActivities Total count
 * @property totalCost Sum of all activity costs in cents
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Activities grouped by date
 *
 * @property date Date string (ISO 8601 format)
 * @property activities List of activities for this date
 * @property totalActivities Total count
 * @property totalCost Sum of all activity costs in cents
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<SharedActivity_ *> *activities __attribute__((swift_name("activities")));
@property (readonly) NSString *date __attribute__((swift_name("date")));
@property (readonly) int32_t totalActivities __attribute__((swift_name("totalActivities")));
@property (readonly) int64_t totalCost __attribute__((swift_name("totalCost")));
@end


/**
 * Activities grouped by date
 *
 * @property date Date string (ISO 8601 format)
 * @property activities List of activities for this date
 * @property totalActivities Total count
 * @property totalCost Sum of all activity costs in cents
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ActivitiesByDate.Companion")))
@interface SharedActivitiesByDateCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Activities grouped by date
 *
 * @property date Date string (ISO 8601 format)
 * @property activities List of activities for this date
 * @property totalActivities Total count
 * @property totalCost Sum of all activity costs in cents
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedActivitiesByDateCompanion *shared __attribute__((swift_name("shared")));

/**
 * Activities grouped by date
 *
 * @property date Date string (ISO 8601 format)
 * @property activities List of activities for this date
 * @property totalActivities Total count
 * @property totalCost Sum of all activity costs in cents
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Activity for an event
 *
 * Represents a planned activity with participant registration.
 *
 * @property id Unique identifier
 * @property eventId Event this activity belongs to
 * @property scenarioId Optional link to a scenario
 * @property name Name of the activity (e.g., "Hike to the lake", "Beach volleyball")
 * @property description Detailed description
 * @property date Date of the activity (ISO 8601 date, optional)
 * @property time Time of the activity (HH:MM format, optional)
 * @property duration Duration in minutes
 * @property location Where the activity takes place (optional)
 * @property cost Cost per participant in cents (optional)
 * @property maxParticipants Maximum number of participants (null for unlimited)
 * @property registeredParticipantIds List of registered participant IDs
 * @property organizerId Participant ID who organizes this activity
 * @property notes Additional notes
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Activity_")))
@interface SharedActivity_ : SharedBase
- (instancetype)initWithId:(NSString *)id eventId:(NSString *)eventId scenarioId:(NSString * _Nullable)scenarioId name:(NSString *)name description:(NSString *)description date:(NSString * _Nullable)date time:(NSString * _Nullable)time duration:(int32_t)duration location:(NSString * _Nullable)location cost:(SharedLong * _Nullable)cost maxParticipants:(SharedInt * _Nullable)maxParticipants registeredParticipantIds:(NSArray<NSString *> *)registeredParticipantIds organizerId:(NSString *)organizerId notes:(NSString * _Nullable)notes createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("init(id:eventId:scenarioId:name:description:date:time:duration:location:cost:maxParticipants:registeredParticipantIds:organizerId:notes:createdAt:updatedAt:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedActivity_Companion *companion __attribute__((swift_name("companion")));
- (SharedActivity_ *)doCopyId:(NSString *)id eventId:(NSString *)eventId scenarioId:(NSString * _Nullable)scenarioId name:(NSString *)name description:(NSString *)description date:(NSString * _Nullable)date time:(NSString * _Nullable)time duration:(int32_t)duration location:(NSString * _Nullable)location cost:(SharedLong * _Nullable)cost maxParticipants:(SharedInt * _Nullable)maxParticipants registeredParticipantIds:(NSArray<NSString *> *)registeredParticipantIds organizerId:(NSString *)organizerId notes:(NSString * _Nullable)notes createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("doCopy(id:eventId:scenarioId:name:description:date:time:duration:location:cost:maxParticipants:registeredParticipantIds:organizerId:notes:createdAt:updatedAt:)")));

/**
 * Activity for an event
 *
 * Represents a planned activity with participant registration.
 *
 * @property id Unique identifier
 * @property eventId Event this activity belongs to
 * @property scenarioId Optional link to a scenario
 * @property name Name of the activity (e.g., "Hike to the lake", "Beach volleyball")
 * @property description Detailed description
 * @property date Date of the activity (ISO 8601 date, optional)
 * @property time Time of the activity (HH:MM format, optional)
 * @property duration Duration in minutes
 * @property location Where the activity takes place (optional)
 * @property cost Cost per participant in cents (optional)
 * @property maxParticipants Maximum number of participants (null for unlimited)
 * @property registeredParticipantIds List of registered participant IDs
 * @property organizerId Participant ID who organizes this activity
 * @property notes Additional notes
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Activity for an event
 *
 * Represents a planned activity with participant registration.
 *
 * @property id Unique identifier
 * @property eventId Event this activity belongs to
 * @property scenarioId Optional link to a scenario
 * @property name Name of the activity (e.g., "Hike to the lake", "Beach volleyball")
 * @property description Detailed description
 * @property date Date of the activity (ISO 8601 date, optional)
 * @property time Time of the activity (HH:MM format, optional)
 * @property duration Duration in minutes
 * @property location Where the activity takes place (optional)
 * @property cost Cost per participant in cents (optional)
 * @property maxParticipants Maximum number of participants (null for unlimited)
 * @property registeredParticipantIds List of registered participant IDs
 * @property organizerId Participant ID who organizes this activity
 * @property notes Additional notes
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Activity for an event
 *
 * Represents a planned activity with participant registration.
 *
 * @property id Unique identifier
 * @property eventId Event this activity belongs to
 * @property scenarioId Optional link to a scenario
 * @property name Name of the activity (e.g., "Hike to the lake", "Beach volleyball")
 * @property description Detailed description
 * @property date Date of the activity (ISO 8601 date, optional)
 * @property time Time of the activity (HH:MM format, optional)
 * @property duration Duration in minutes
 * @property location Where the activity takes place (optional)
 * @property cost Cost per participant in cents (optional)
 * @property maxParticipants Maximum number of participants (null for unlimited)
 * @property registeredParticipantIds List of registered participant IDs
 * @property organizerId Participant ID who organizes this activity
 * @property notes Additional notes
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedLong * _Nullable cost __attribute__((swift_name("cost")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString * _Nullable date __attribute__((swift_name("date")));
@property (readonly) NSString *description_ __attribute__((swift_name("description_")));
@property (readonly) int32_t duration __attribute__((swift_name("duration")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString * _Nullable location __attribute__((swift_name("location")));
@property (readonly) SharedInt * _Nullable maxParticipants __attribute__((swift_name("maxParticipants")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) NSString * _Nullable notes __attribute__((swift_name("notes")));
@property (readonly) NSString *organizerId __attribute__((swift_name("organizerId")));
@property (readonly) NSArray<NSString *> *registeredParticipantIds __attribute__((swift_name("registeredParticipantIds")));
@property (readonly) NSString * _Nullable scenarioId __attribute__((swift_name("scenarioId")));
@property (readonly) NSString * _Nullable time __attribute__((swift_name("time")));
@property (readonly) NSString *updatedAt __attribute__((swift_name("updatedAt")));
@end


/**
 * Activity for an event
 *
 * Represents a planned activity with participant registration.
 *
 * @property id Unique identifier
 * @property eventId Event this activity belongs to
 * @property scenarioId Optional link to a scenario
 * @property name Name of the activity (e.g., "Hike to the lake", "Beach volleyball")
 * @property description Detailed description
 * @property date Date of the activity (ISO 8601 date, optional)
 * @property time Time of the activity (HH:MM format, optional)
 * @property duration Duration in minutes
 * @property location Where the activity takes place (optional)
 * @property cost Cost per participant in cents (optional)
 * @property maxParticipants Maximum number of participants (null for unlimited)
 * @property registeredParticipantIds List of registered participant IDs
 * @property organizerId Participant ID who organizes this activity
 * @property notes Additional notes
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Activity_.Companion")))
@interface SharedActivity_Companion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Activity for an event
 *
 * Represents a planned activity with participant registration.
 *
 * @property id Unique identifier
 * @property eventId Event this activity belongs to
 * @property scenarioId Optional link to a scenario
 * @property name Name of the activity (e.g., "Hike to the lake", "Beach volleyball")
 * @property description Detailed description
 * @property date Date of the activity (ISO 8601 date, optional)
 * @property time Time of the activity (HH:MM format, optional)
 * @property duration Duration in minutes
 * @property location Where the activity takes place (optional)
 * @property cost Cost per participant in cents (optional)
 * @property maxParticipants Maximum number of participants (null for unlimited)
 * @property registeredParticipantIds List of registered participant IDs
 * @property organizerId Participant ID who organizes this activity
 * @property notes Additional notes
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedActivity_Companion *shared __attribute__((swift_name("shared")));

/**
 * Activity for an event
 *
 * Represents a planned activity with participant registration.
 *
 * @property id Unique identifier
 * @property eventId Event this activity belongs to
 * @property scenarioId Optional link to a scenario
 * @property name Name of the activity (e.g., "Hike to the lake", "Beach volleyball")
 * @property description Detailed description
 * @property date Date of the activity (ISO 8601 date, optional)
 * @property time Time of the activity (HH:MM format, optional)
 * @property duration Duration in minutes
 * @property location Where the activity takes place (optional)
 * @property cost Cost per participant in cents (optional)
 * @property maxParticipants Maximum number of participants (null for unlimited)
 * @property registeredParticipantIds List of registered participant IDs
 * @property organizerId Participant ID who organizes this activity
 * @property notes Additional notes
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Activity registration entry
 *
 * Links a participant to an activity.
 *
 * @property id Unique identifier
 * @property activityId Activity ID
 * @property participantId Participant ID
 * @property registeredAt Registration timestamp (ISO 8601 UTC)
 * @property notes Optional notes from participant
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ActivityParticipant")))
@interface SharedActivityParticipant : SharedBase
- (instancetype)initWithId:(NSString *)id activityId:(NSString *)activityId participantId:(NSString *)participantId registeredAt:(NSString *)registeredAt notes:(NSString * _Nullable)notes __attribute__((swift_name("init(id:activityId:participantId:registeredAt:notes:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedActivityParticipantCompanion *companion __attribute__((swift_name("companion")));
- (SharedActivityParticipant *)doCopyId:(NSString *)id activityId:(NSString *)activityId participantId:(NSString *)participantId registeredAt:(NSString *)registeredAt notes:(NSString * _Nullable)notes __attribute__((swift_name("doCopy(id:activityId:participantId:registeredAt:notes:)")));

/**
 * Activity registration entry
 *
 * Links a participant to an activity.
 *
 * @property id Unique identifier
 * @property activityId Activity ID
 * @property participantId Participant ID
 * @property registeredAt Registration timestamp (ISO 8601 UTC)
 * @property notes Optional notes from participant
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Activity registration entry
 *
 * Links a participant to an activity.
 *
 * @property id Unique identifier
 * @property activityId Activity ID
 * @property participantId Participant ID
 * @property registeredAt Registration timestamp (ISO 8601 UTC)
 * @property notes Optional notes from participant
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Activity registration entry
 *
 * Links a participant to an activity.
 *
 * @property id Unique identifier
 * @property activityId Activity ID
 * @property participantId Participant ID
 * @property registeredAt Registration timestamp (ISO 8601 UTC)
 * @property notes Optional notes from participant
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *activityId __attribute__((swift_name("activityId")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString * _Nullable notes __attribute__((swift_name("notes")));
@property (readonly) NSString *participantId __attribute__((swift_name("participantId")));
@property (readonly) NSString *registeredAt __attribute__((swift_name("registeredAt")));
@end


/**
 * Activity registration entry
 *
 * Links a participant to an activity.
 *
 * @property id Unique identifier
 * @property activityId Activity ID
 * @property participantId Participant ID
 * @property registeredAt Registration timestamp (ISO 8601 UTC)
 * @property notes Optional notes from participant
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ActivityParticipant.Companion")))
@interface SharedActivityParticipantCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Activity registration entry
 *
 * Links a participant to an activity.
 *
 * @property id Unique identifier
 * @property activityId Activity ID
 * @property participantId Participant ID
 * @property registeredAt Registration timestamp (ISO 8601 UTC)
 * @property notes Optional notes from participant
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedActivityParticipantCompanion *shared __attribute__((swift_name("shared")));

/**
 * Activity registration entry
 *
 * Links a participant to an activity.
 *
 * @property id Unique identifier
 * @property activityId Activity ID
 * @property participantId Participant ID
 * @property registeredAt Registration timestamp (ISO 8601 UTC)
 * @property notes Optional notes from participant
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Request to register a participant to an activity
 *
 * @property participantId Participant ID
 * @property notes Optional notes
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ActivityRegistrationRequest")))
@interface SharedActivityRegistrationRequest : SharedBase
- (instancetype)initWithParticipantId:(NSString *)participantId notes:(NSString * _Nullable)notes __attribute__((swift_name("init(participantId:notes:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedActivityRegistrationRequestCompanion *companion __attribute__((swift_name("companion")));
- (SharedActivityRegistrationRequest *)doCopyParticipantId:(NSString *)participantId notes:(NSString * _Nullable)notes __attribute__((swift_name("doCopy(participantId:notes:)")));

/**
 * Request to register a participant to an activity
 *
 * @property participantId Participant ID
 * @property notes Optional notes
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Request to register a participant to an activity
 *
 * @property participantId Participant ID
 * @property notes Optional notes
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Request to register a participant to an activity
 *
 * @property participantId Participant ID
 * @property notes Optional notes
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable notes __attribute__((swift_name("notes")));
@property (readonly) NSString *participantId __attribute__((swift_name("participantId")));
@end


/**
 * Request to register a participant to an activity
 *
 * @property participantId Participant ID
 * @property notes Optional notes
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ActivityRegistrationRequest.Companion")))
@interface SharedActivityRegistrationRequestCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Request to register a participant to an activity
 *
 * @property participantId Participant ID
 * @property notes Optional notes
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedActivityRegistrationRequestCompanion *shared __attribute__((swift_name("shared")));

/**
 * Request to register a participant to an activity
 *
 * @property participantId Participant ID
 * @property notes Optional notes
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Request to create or update an activity
 *
 * @property name Activity name
 * @property description Description
 * @property date Date (optional, ISO 8601)
 * @property time Time (optional, HH:MM)
 * @property duration Duration in minutes
 * @property location Location (optional)
 * @property cost Cost per participant in cents (optional)
 * @property maxParticipants Max capacity (optional, null = unlimited)
 * @property organizerId Organizer participant ID
 * @property notes Additional notes (optional)
 * @property scenarioId Link to scenario (optional)
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ActivityRequest")))
@interface SharedActivityRequest : SharedBase
- (instancetype)initWithName:(NSString *)name description:(NSString *)description date:(NSString * _Nullable)date time:(NSString * _Nullable)time duration:(int32_t)duration location:(NSString * _Nullable)location cost:(SharedLong * _Nullable)cost maxParticipants:(SharedInt * _Nullable)maxParticipants organizerId:(NSString *)organizerId notes:(NSString * _Nullable)notes scenarioId:(NSString * _Nullable)scenarioId __attribute__((swift_name("init(name:description:date:time:duration:location:cost:maxParticipants:organizerId:notes:scenarioId:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedActivityRequestCompanion *companion __attribute__((swift_name("companion")));
- (SharedActivityRequest *)doCopyName:(NSString *)name description:(NSString *)description date:(NSString * _Nullable)date time:(NSString * _Nullable)time duration:(int32_t)duration location:(NSString * _Nullable)location cost:(SharedLong * _Nullable)cost maxParticipants:(SharedInt * _Nullable)maxParticipants organizerId:(NSString *)organizerId notes:(NSString * _Nullable)notes scenarioId:(NSString * _Nullable)scenarioId __attribute__((swift_name("doCopy(name:description:date:time:duration:location:cost:maxParticipants:organizerId:notes:scenarioId:)")));

/**
 * Request to create or update an activity
 *
 * @property name Activity name
 * @property description Description
 * @property date Date (optional, ISO 8601)
 * @property time Time (optional, HH:MM)
 * @property duration Duration in minutes
 * @property location Location (optional)
 * @property cost Cost per participant in cents (optional)
 * @property maxParticipants Max capacity (optional, null = unlimited)
 * @property organizerId Organizer participant ID
 * @property notes Additional notes (optional)
 * @property scenarioId Link to scenario (optional)
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Request to create or update an activity
 *
 * @property name Activity name
 * @property description Description
 * @property date Date (optional, ISO 8601)
 * @property time Time (optional, HH:MM)
 * @property duration Duration in minutes
 * @property location Location (optional)
 * @property cost Cost per participant in cents (optional)
 * @property maxParticipants Max capacity (optional, null = unlimited)
 * @property organizerId Organizer participant ID
 * @property notes Additional notes (optional)
 * @property scenarioId Link to scenario (optional)
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Request to create or update an activity
 *
 * @property name Activity name
 * @property description Description
 * @property date Date (optional, ISO 8601)
 * @property time Time (optional, HH:MM)
 * @property duration Duration in minutes
 * @property location Location (optional)
 * @property cost Cost per participant in cents (optional)
 * @property maxParticipants Max capacity (optional, null = unlimited)
 * @property organizerId Organizer participant ID
 * @property notes Additional notes (optional)
 * @property scenarioId Link to scenario (optional)
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedLong * _Nullable cost __attribute__((swift_name("cost")));
@property (readonly) NSString * _Nullable date __attribute__((swift_name("date")));
@property (readonly) NSString *description_ __attribute__((swift_name("description_")));
@property (readonly) int32_t duration __attribute__((swift_name("duration")));
@property (readonly) NSString * _Nullable location __attribute__((swift_name("location")));
@property (readonly) SharedInt * _Nullable maxParticipants __attribute__((swift_name("maxParticipants")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) NSString * _Nullable notes __attribute__((swift_name("notes")));
@property (readonly) NSString *organizerId __attribute__((swift_name("organizerId")));
@property (readonly) NSString * _Nullable scenarioId __attribute__((swift_name("scenarioId")));
@property (readonly) NSString * _Nullable time __attribute__((swift_name("time")));
@end


/**
 * Request to create or update an activity
 *
 * @property name Activity name
 * @property description Description
 * @property date Date (optional, ISO 8601)
 * @property time Time (optional, HH:MM)
 * @property duration Duration in minutes
 * @property location Location (optional)
 * @property cost Cost per participant in cents (optional)
 * @property maxParticipants Max capacity (optional, null = unlimited)
 * @property organizerId Organizer participant ID
 * @property notes Additional notes (optional)
 * @property scenarioId Link to scenario (optional)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ActivityRequest.Companion")))
@interface SharedActivityRequestCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Request to create or update an activity
 *
 * @property name Activity name
 * @property description Description
 * @property date Date (optional, ISO 8601)
 * @property time Time (optional, HH:MM)
 * @property duration Duration in minutes
 * @property location Location (optional)
 * @property cost Cost per participant in cents (optional)
 * @property maxParticipants Max capacity (optional, null = unlimited)
 * @property organizerId Organizer participant ID
 * @property notes Additional notes (optional)
 * @property scenarioId Link to scenario (optional)
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedActivityRequestCompanion *shared __attribute__((swift_name("shared")));

/**
 * Request to create or update an activity
 *
 * @property name Activity name
 * @property description Description
 * @property date Date (optional, ISO 8601)
 * @property time Time (optional, HH:MM)
 * @property duration Duration in minutes
 * @property location Location (optional)
 * @property cost Cost per participant in cents (optional)
 * @property maxParticipants Max capacity (optional, null = unlimited)
 * @property organizerId Organizer participant ID
 * @property notes Additional notes (optional)
 * @property scenarioId Link to scenario (optional)
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Activity schedule overview
 *
 * @property eventId Event ID
 * @property activitiesByDate Activities grouped by date
 * @property totalActivities Total number of activities
 * @property totalCost Total cost of all activities in cents
 * @property participationRate Average participation rate (0.0 to 1.0)
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ActivitySchedule")))
@interface SharedActivitySchedule : SharedBase
- (instancetype)initWithEventId:(NSString *)eventId activitiesByDate:(NSArray<SharedActivitiesByDate *> *)activitiesByDate totalActivities:(int32_t)totalActivities totalCost:(int64_t)totalCost participationRate:(double)participationRate __attribute__((swift_name("init(eventId:activitiesByDate:totalActivities:totalCost:participationRate:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedActivityScheduleCompanion *companion __attribute__((swift_name("companion")));
- (SharedActivitySchedule *)doCopyEventId:(NSString *)eventId activitiesByDate:(NSArray<SharedActivitiesByDate *> *)activitiesByDate totalActivities:(int32_t)totalActivities totalCost:(int64_t)totalCost participationRate:(double)participationRate __attribute__((swift_name("doCopy(eventId:activitiesByDate:totalActivities:totalCost:participationRate:)")));

/**
 * Activity schedule overview
 *
 * @property eventId Event ID
 * @property activitiesByDate Activities grouped by date
 * @property totalActivities Total number of activities
 * @property totalCost Total cost of all activities in cents
 * @property participationRate Average participation rate (0.0 to 1.0)
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Activity schedule overview
 *
 * @property eventId Event ID
 * @property activitiesByDate Activities grouped by date
 * @property totalActivities Total number of activities
 * @property totalCost Total cost of all activities in cents
 * @property participationRate Average participation rate (0.0 to 1.0)
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Activity schedule overview
 *
 * @property eventId Event ID
 * @property activitiesByDate Activities grouped by date
 * @property totalActivities Total number of activities
 * @property totalCost Total cost of all activities in cents
 * @property participationRate Average participation rate (0.0 to 1.0)
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<SharedActivitiesByDate *> *activitiesByDate __attribute__((swift_name("activitiesByDate")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) double participationRate __attribute__((swift_name("participationRate")));
@property (readonly) int32_t totalActivities __attribute__((swift_name("totalActivities")));
@property (readonly) int64_t totalCost __attribute__((swift_name("totalCost")));
@end


/**
 * Activity schedule overview
 *
 * @property eventId Event ID
 * @property activitiesByDate Activities grouped by date
 * @property totalActivities Total number of activities
 * @property totalCost Total cost of all activities in cents
 * @property participationRate Average participation rate (0.0 to 1.0)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ActivitySchedule.Companion")))
@interface SharedActivityScheduleCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Activity schedule overview
 *
 * @property eventId Event ID
 * @property activitiesByDate Activities grouped by date
 * @property totalActivities Total number of activities
 * @property totalCost Total cost of all activities in cents
 * @property participationRate Average participation rate (0.0 to 1.0)
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedActivityScheduleCompanion *shared __attribute__((swift_name("shared")));

/**
 * Activity schedule overview
 *
 * @property eventId Event ID
 * @property activitiesByDate Activities grouped by date
 * @property totalActivities Total number of activities
 * @property totalCost Total cost of all activities in cents
 * @property participationRate Average participation rate (0.0 to 1.0)
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Activity with additional metadata
 *
 * @property activity The activity
 * @property registeredCount Number of registered participants
 * @property spotsRemaining Remaining spots (null if unlimited)
 * @property isFull Whether the activity is full
 * @property totalCost Total cost for all registered participants in cents
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ActivityWithStats")))
@interface SharedActivityWithStats : SharedBase
- (instancetype)initWithActivity:(SharedActivity_ *)activity registeredCount:(int32_t)registeredCount spotsRemaining:(SharedInt * _Nullable)spotsRemaining isFull:(BOOL)isFull totalCost:(int64_t)totalCost __attribute__((swift_name("init(activity:registeredCount:spotsRemaining:isFull:totalCost:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedActivityWithStatsCompanion *companion __attribute__((swift_name("companion")));
- (SharedActivityWithStats *)doCopyActivity:(SharedActivity_ *)activity registeredCount:(int32_t)registeredCount spotsRemaining:(SharedInt * _Nullable)spotsRemaining isFull:(BOOL)isFull totalCost:(int64_t)totalCost __attribute__((swift_name("doCopy(activity:registeredCount:spotsRemaining:isFull:totalCost:)")));

/**
 * Activity with additional metadata
 *
 * @property activity The activity
 * @property registeredCount Number of registered participants
 * @property spotsRemaining Remaining spots (null if unlimited)
 * @property isFull Whether the activity is full
 * @property totalCost Total cost for all registered participants in cents
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Activity with additional metadata
 *
 * @property activity The activity
 * @property registeredCount Number of registered participants
 * @property spotsRemaining Remaining spots (null if unlimited)
 * @property isFull Whether the activity is full
 * @property totalCost Total cost for all registered participants in cents
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Activity with additional metadata
 *
 * @property activity The activity
 * @property registeredCount Number of registered participants
 * @property spotsRemaining Remaining spots (null if unlimited)
 * @property isFull Whether the activity is full
 * @property totalCost Total cost for all registered participants in cents
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedActivity_ *activity __attribute__((swift_name("activity")));
@property (readonly) BOOL isFull __attribute__((swift_name("isFull")));
@property (readonly) int32_t registeredCount __attribute__((swift_name("registeredCount")));
@property (readonly) SharedInt * _Nullable spotsRemaining __attribute__((swift_name("spotsRemaining")));
@property (readonly) int64_t totalCost __attribute__((swift_name("totalCost")));
@end


/**
 * Activity with additional metadata
 *
 * @property activity The activity
 * @property registeredCount Number of registered participants
 * @property spotsRemaining Remaining spots (null if unlimited)
 * @property isFull Whether the activity is full
 * @property totalCost Total cost for all registered participants in cents
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ActivityWithStats.Companion")))
@interface SharedActivityWithStatsCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Activity with additional metadata
 *
 * @property activity The activity
 * @property registeredCount Number of registered participants
 * @property spotsRemaining Remaining spots (null if unlimited)
 * @property isFull Whether the activity is full
 * @property totalCost Total cost for all registered participants in cents
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedActivityWithStatsCompanion *shared __attribute__((swift_name("shared")));

/**
 * Activity with additional metadata
 *
 * @property activity The activity
 * @property registeredCount Number of registered participants
 * @property spotsRemaining Remaining spots (null if unlimited)
 * @property isFull Whether the activity is full
 * @property totalCost Total cost for all registered participants in cents
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AddParticipantRequest")))
@interface SharedAddParticipantRequest : SharedBase
- (instancetype)initWithEventId:(NSString *)eventId participantId:(NSString *)participantId __attribute__((swift_name("init(eventId:participantId:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedAddParticipantRequestCompanion *companion __attribute__((swift_name("companion")));
- (SharedAddParticipantRequest *)doCopyEventId:(NSString *)eventId participantId:(NSString *)participantId __attribute__((swift_name("doCopy(eventId:participantId:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *participantId __attribute__((swift_name("participantId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AddParticipantRequest.Companion")))
@interface SharedAddParticipantRequestCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAddParticipantRequestCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AddVoteRequest")))
@interface SharedAddVoteRequest : SharedBase
- (instancetype)initWithEventId:(NSString *)eventId participantId:(NSString *)participantId slotId:(NSString *)slotId vote:(NSString *)vote __attribute__((swift_name("init(eventId:participantId:slotId:vote:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedAddVoteRequestCompanion *companion __attribute__((swift_name("companion")));
- (SharedAddVoteRequest *)doCopyEventId:(NSString *)eventId participantId:(NSString *)participantId slotId:(NSString *)slotId vote:(NSString *)vote __attribute__((swift_name("doCopy(eventId:participantId:slotId:vote:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *participantId __attribute__((swift_name("participantId")));
@property (readonly) NSString *slotId __attribute__((swift_name("slotId")));
@property (readonly) NSString *vote __attribute__((swift_name("vote")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AddVoteRequest.Companion")))
@interface SharedAddVoteRequestCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAddVoteRequestCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ApiResponse")))
@interface SharedApiResponse<T> : SharedBase
- (instancetype)initWithSuccess:(BOOL)success data:(T _Nullable)data error:(SharedErrorResponse * _Nullable)error __attribute__((swift_name("init(success:data:error:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedApiResponseCompanion *companion __attribute__((swift_name("companion")));
- (SharedApiResponse<T> *)doCopySuccess:(BOOL)success data:(T _Nullable)data error:(SharedErrorResponse * _Nullable)error __attribute__((swift_name("doCopy(success:data:error:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) T _Nullable data __attribute__((swift_name("data")));
@property (readonly) SharedErrorResponse * _Nullable error __attribute__((swift_name("error")));
@property (readonly) BOOL success __attribute__((swift_name("success")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ApiResponseCompanion")))
@interface SharedApiResponseCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedApiResponseCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializerTypeParamsSerializers:(SharedKotlinArray<id<SharedKotlinx_serialization_coreKSerializer>> *)typeParamsSerializers __attribute__((swift_name("serializer(typeParamsSerializers:)")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializerTypeSerial0:(id<SharedKotlinx_serialization_coreKSerializer>)typeSerial0 __attribute__((swift_name("serializer(typeSerial0:)")));
@end


/**
 * Authentication middleware context
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AuthContext")))
@interface SharedAuthContext : SharedBase
- (instancetype)initWithUserId:(NSString *)userId user:(SharedUser_ * _Nullable)user token:(SharedUserToken * _Nullable)token __attribute__((swift_name("init(userId:user:token:)"))) __attribute__((objc_designated_initializer));
- (SharedAuthContext *)doCopyUserId:(NSString *)userId user:(SharedUser_ * _Nullable)user token:(SharedUserToken * _Nullable)token __attribute__((swift_name("doCopy(userId:user:token:)")));

/**
 * Authentication middleware context
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Authentication middleware context
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Authentication middleware context
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedUserToken * _Nullable token __attribute__((swift_name("token")));
@property (readonly) SharedUser_ * _Nullable user __attribute__((swift_name("user")));
@property (readonly) NSString *userId __attribute__((swift_name("userId")));
@end


/**
 * Auto-generated meal plan request
 *
 * Used to automatically generate meals for an event.
 *
 * @property eventId Event ID
 * @property startDate Start date (ISO 8601)
 * @property endDate End date (ISO 8601)
 * @property participantCount Number of participants
 * @property includeMealTypes Which meal types to plan
 * @property estimatedCostPerMeal Average cost per meal per person in cents
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AutoMealPlanRequest")))
@interface SharedAutoMealPlanRequest : SharedBase
- (instancetype)initWithEventId:(NSString *)eventId startDate:(NSString *)startDate endDate:(NSString *)endDate participantCount:(int32_t)participantCount includeMealTypes:(NSArray<SharedMealType *> *)includeMealTypes estimatedCostPerMeal:(int64_t)estimatedCostPerMeal __attribute__((swift_name("init(eventId:startDate:endDate:participantCount:includeMealTypes:estimatedCostPerMeal:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedAutoMealPlanRequestCompanion *companion __attribute__((swift_name("companion")));
- (SharedAutoMealPlanRequest *)doCopyEventId:(NSString *)eventId startDate:(NSString *)startDate endDate:(NSString *)endDate participantCount:(int32_t)participantCount includeMealTypes:(NSArray<SharedMealType *> *)includeMealTypes estimatedCostPerMeal:(int64_t)estimatedCostPerMeal __attribute__((swift_name("doCopy(eventId:startDate:endDate:participantCount:includeMealTypes:estimatedCostPerMeal:)")));

/**
 * Auto-generated meal plan request
 *
 * Used to automatically generate meals for an event.
 *
 * @property eventId Event ID
 * @property startDate Start date (ISO 8601)
 * @property endDate End date (ISO 8601)
 * @property participantCount Number of participants
 * @property includeMealTypes Which meal types to plan
 * @property estimatedCostPerMeal Average cost per meal per person in cents
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Auto-generated meal plan request
 *
 * Used to automatically generate meals for an event.
 *
 * @property eventId Event ID
 * @property startDate Start date (ISO 8601)
 * @property endDate End date (ISO 8601)
 * @property participantCount Number of participants
 * @property includeMealTypes Which meal types to plan
 * @property estimatedCostPerMeal Average cost per meal per person in cents
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Auto-generated meal plan request
 *
 * Used to automatically generate meals for an event.
 *
 * @property eventId Event ID
 * @property startDate Start date (ISO 8601)
 * @property endDate End date (ISO 8601)
 * @property participantCount Number of participants
 * @property includeMealTypes Which meal types to plan
 * @property estimatedCostPerMeal Average cost per meal per person in cents
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *endDate __attribute__((swift_name("endDate")));
@property (readonly) int64_t estimatedCostPerMeal __attribute__((swift_name("estimatedCostPerMeal")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSArray<SharedMealType *> *includeMealTypes __attribute__((swift_name("includeMealTypes")));
@property (readonly) int32_t participantCount __attribute__((swift_name("participantCount")));
@property (readonly) NSString *startDate __attribute__((swift_name("startDate")));
@end


/**
 * Auto-generated meal plan request
 *
 * Used to automatically generate meals for an event.
 *
 * @property eventId Event ID
 * @property startDate Start date (ISO 8601)
 * @property endDate End date (ISO 8601)
 * @property participantCount Number of participants
 * @property includeMealTypes Which meal types to plan
 * @property estimatedCostPerMeal Average cost per meal per person in cents
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AutoMealPlanRequest.Companion")))
@interface SharedAutoMealPlanRequestCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Auto-generated meal plan request
 *
 * Used to automatically generate meals for an event.
 *
 * @property eventId Event ID
 * @property startDate Start date (ISO 8601)
 * @property endDate End date (ISO 8601)
 * @property participantCount Number of participants
 * @property includeMealTypes Which meal types to plan
 * @property estimatedCostPerMeal Average cost per meal per person in cents
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAutoMealPlanRequestCompanion *shared __attribute__((swift_name("shared")));

/**
 * Auto-generated meal plan request
 *
 * Used to automatically generate meals for an event.
 *
 * @property eventId Event ID
 * @property startDate Start date (ISO 8601)
 * @property endDate End date (ISO 8601)
 * @property participantCount Number of participants
 * @property includeMealTypes Which meal types to plan
 * @property estimatedCostPerMeal Average cost per meal per person in cents
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Booking status for accommodation
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BookingStatus")))
@interface SharedBookingStatus : SharedKotlinEnum<SharedBookingStatus *>
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Booking status for accommodation
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedBookingStatusCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) SharedBookingStatus *searching __attribute__((swift_name("searching")));
@property (class, readonly) SharedBookingStatus *reserved __attribute__((swift_name("reserved")));
@property (class, readonly) SharedBookingStatus *confirmed __attribute__((swift_name("confirmed")));
@property (class, readonly) SharedBookingStatus *cancelled __attribute__((swift_name("cancelled")));
+ (SharedKotlinArray<SharedBookingStatus *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedBookingStatus *> *entries __attribute__((swift_name("entries")));
@end


/**
 * Booking status for accommodation
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BookingStatus.Companion")))
@interface SharedBookingStatusCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Booking status for accommodation
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBookingStatusCompanion *shared __attribute__((swift_name("shared")));

/**
 * Booking status for accommodation
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));

/**
 * Booking status for accommodation
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializerTypeParamsSerializers:(SharedKotlinArray<id<SharedKotlinx_serialization_coreKSerializer>> *)typeParamsSerializers __attribute__((swift_name("serializer(typeParamsSerializers:)")));
@end


/**
 * Main budget entity for an event.
 * Tracks total budget with breakdown by category.
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Budget_")))
@interface SharedBudget_ : SharedBase
- (instancetype)initWithId:(NSString *)id eventId:(NSString *)eventId totalEstimated:(double)totalEstimated totalActual:(double)totalActual transportEstimated:(double)transportEstimated transportActual:(double)transportActual accommodationEstimated:(double)accommodationEstimated accommodationActual:(double)accommodationActual mealsEstimated:(double)mealsEstimated mealsActual:(double)mealsActual activitiesEstimated:(double)activitiesEstimated activitiesActual:(double)activitiesActual equipmentEstimated:(double)equipmentEstimated equipmentActual:(double)equipmentActual otherEstimated:(double)otherEstimated otherActual:(double)otherActual createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("init(id:eventId:totalEstimated:totalActual:transportEstimated:transportActual:accommodationEstimated:accommodationActual:mealsEstimated:mealsActual:activitiesEstimated:activitiesActual:equipmentEstimated:equipmentActual:otherEstimated:otherActual:createdAt:updatedAt:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedBudget_Companion *companion __attribute__((swift_name("companion")));
- (SharedBudget_ *)doCopyId:(NSString *)id eventId:(NSString *)eventId totalEstimated:(double)totalEstimated totalActual:(double)totalActual transportEstimated:(double)transportEstimated transportActual:(double)transportActual accommodationEstimated:(double)accommodationEstimated accommodationActual:(double)accommodationActual mealsEstimated:(double)mealsEstimated mealsActual:(double)mealsActual activitiesEstimated:(double)activitiesEstimated activitiesActual:(double)activitiesActual equipmentEstimated:(double)equipmentEstimated equipmentActual:(double)equipmentActual otherEstimated:(double)otherEstimated otherActual:(double)otherActual createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("doCopy(id:eventId:totalEstimated:totalActual:transportEstimated:transportActual:accommodationEstimated:accommodationActual:mealsEstimated:mealsActual:activitiesEstimated:activitiesActual:equipmentEstimated:equipmentActual:otherEstimated:otherActual:createdAt:updatedAt:)")));

/**
 * Main budget entity for an event.
 * Tracks total budget with breakdown by category.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Get actual budget for a specific category.
 */
- (double)getActualForCategoryCategory:(SharedBudgetCategory *)category __attribute__((swift_name("getActualForCategory(category:)")));

/**
 * Calculate percentage of budget used for a category.
 */
- (double)getCategoryPercentageCategory:(SharedBudgetCategory *)category __attribute__((swift_name("getCategoryPercentage(category:)")));

/**
 * Get estimated budget for a specific category.
 */
- (double)getEstimatedForCategoryCategory:(SharedBudgetCategory *)category __attribute__((swift_name("getEstimatedForCategory(category:)")));

/**
 * Main budget entity for an event.
 * Tracks total budget with breakdown by category.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Main budget entity for an event.
 * Tracks total budget with breakdown by category.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) double accommodationActual __attribute__((swift_name("accommodationActual")));
@property (readonly) double accommodationEstimated __attribute__((swift_name("accommodationEstimated")));
@property (readonly) double activitiesActual __attribute__((swift_name("activitiesActual")));
@property (readonly) double activitiesEstimated __attribute__((swift_name("activitiesEstimated")));

/**
 * Calculate overall budget usage percentage.
 */
@property (readonly) double budgetUsagePercentage __attribute__((swift_name("budgetUsagePercentage")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) double equipmentActual __attribute__((swift_name("equipmentActual")));
@property (readonly) double equipmentEstimated __attribute__((swift_name("equipmentEstimated")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *id __attribute__((swift_name("id")));

/**
 * Check if budget is exceeded.
 */
@property (readonly) BOOL isOverBudget __attribute__((swift_name("isOverBudget")));
@property (readonly) double mealsActual __attribute__((swift_name("mealsActual")));
@property (readonly) double mealsEstimated __attribute__((swift_name("mealsEstimated")));
@property (readonly) double otherActual __attribute__((swift_name("otherActual")));
@property (readonly) double otherEstimated __attribute__((swift_name("otherEstimated")));

/**
 * Calculate remaining budget.
 */
@property (readonly) double remainingBudget __attribute__((swift_name("remainingBudget")));
@property (readonly) double totalActual __attribute__((swift_name("totalActual")));
@property (readonly) double totalEstimated __attribute__((swift_name("totalEstimated")));
@property (readonly) double transportActual __attribute__((swift_name("transportActual")));
@property (readonly) double transportEstimated __attribute__((swift_name("transportEstimated")));
@property (readonly) NSString *updatedAt __attribute__((swift_name("updatedAt")));
@end


/**
 * Main budget entity for an event.
 * Tracks total budget with breakdown by category.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Budget_.Companion")))
@interface SharedBudget_Companion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Main budget entity for an event.
 * Tracks total budget with breakdown by category.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBudget_Companion *shared __attribute__((swift_name("shared")));

/**
 * Main budget entity for an event.
 * Tracks total budget with breakdown by category.
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Budget categories for event planning.
 * Organized by main expense types.
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BudgetCategory")))
@interface SharedBudgetCategory : SharedKotlinEnum<SharedBudgetCategory *>
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Budget categories for event planning.
 * Organized by main expense types.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedBudgetCategoryCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) SharedBudgetCategory *transport __attribute__((swift_name("transport")));
@property (class, readonly) SharedBudgetCategory *accommodation __attribute__((swift_name("accommodation")));
@property (class, readonly) SharedBudgetCategory *meals __attribute__((swift_name("meals")));
@property (class, readonly) SharedBudgetCategory *activities __attribute__((swift_name("activities")));
@property (class, readonly) SharedBudgetCategory *equipment __attribute__((swift_name("equipment")));
@property (class, readonly) SharedBudgetCategory *other __attribute__((swift_name("other")));
+ (SharedKotlinArray<SharedBudgetCategory *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedBudgetCategory *> *entries __attribute__((swift_name("entries")));
@end


/**
 * Budget categories for event planning.
 * Organized by main expense types.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BudgetCategory.Companion")))
@interface SharedBudgetCategoryCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Budget categories for event planning.
 * Organized by main expense types.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBudgetCategoryCompanion *shared __attribute__((swift_name("shared")));

/**
 * Budget categories for event planning.
 * Organized by main expense types.
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));

/**
 * Budget categories for event planning.
 * Organized by main expense types.
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializerTypeParamsSerializers:(SharedKotlinArray<id<SharedKotlinx_serialization_coreKSerializer>> *)typeParamsSerializers __attribute__((swift_name("serializer(typeParamsSerializers:)")));
@end


/**
 * Category details for budget breakdown.
 * Used for UI display and analysis.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BudgetCategoryDetails")))
@interface SharedBudgetCategoryDetails : SharedBase
- (instancetype)initWithCategory:(SharedBudgetCategory *)category estimated:(double)estimated actual:(double)actual itemCount:(int32_t)itemCount paidItemCount:(int32_t)paidItemCount percentage:(double)percentage __attribute__((swift_name("init(category:estimated:actual:itemCount:paidItemCount:percentage:)"))) __attribute__((objc_designated_initializer));
- (SharedBudgetCategoryDetails *)doCopyCategory:(SharedBudgetCategory *)category estimated:(double)estimated actual:(double)actual itemCount:(int32_t)itemCount paidItemCount:(int32_t)paidItemCount percentage:(double)percentage __attribute__((swift_name("doCopy(category:estimated:actual:itemCount:paidItemCount:percentage:)")));

/**
 * Category details for budget breakdown.
 * Used for UI display and analysis.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Category details for budget breakdown.
 * Used for UI display and analysis.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Category details for budget breakdown.
 * Used for UI display and analysis.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) double actual __attribute__((swift_name("actual")));
@property (readonly) SharedBudgetCategory *category __attribute__((swift_name("category")));
@property (readonly) double estimated __attribute__((swift_name("estimated")));
@property (readonly) BOOL isOverBudget __attribute__((swift_name("isOverBudget")));
@property (readonly) int32_t itemCount __attribute__((swift_name("itemCount")));
@property (readonly) int32_t paidItemCount __attribute__((swift_name("paidItemCount")));
@property (readonly) double percentage __attribute__((swift_name("percentage")));
@property (readonly) double remaining __attribute__((swift_name("remaining")));
@property (readonly) double usagePercentage __attribute__((swift_name("usagePercentage")));
@end


/**
 * Individual budget item (expense).
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BudgetItem_")))
@interface SharedBudgetItem_ : SharedBase
- (instancetype)initWithId:(NSString *)id budgetId:(NSString *)budgetId category:(SharedBudgetCategory *)category name:(NSString *)name description:(NSString *)description estimatedCost:(double)estimatedCost actualCost:(double)actualCost isPaid:(BOOL)isPaid paidBy:(NSString * _Nullable)paidBy sharedBy:(NSArray<NSString *> *)sharedBy notes:(NSString *)notes createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("init(id:budgetId:category:name:description:estimatedCost:actualCost:isPaid:paidBy:sharedBy:notes:createdAt:updatedAt:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedBudgetItem_Companion *companion __attribute__((swift_name("companion")));
- (SharedBudgetItem_ *)doCopyId:(NSString *)id budgetId:(NSString *)budgetId category:(SharedBudgetCategory *)category name:(NSString *)name description:(NSString *)description estimatedCost:(double)estimatedCost actualCost:(double)actualCost isPaid:(BOOL)isPaid paidBy:(NSString * _Nullable)paidBy sharedBy:(NSArray<NSString *> *)sharedBy notes:(NSString *)notes createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("doCopy(id:budgetId:category:name:description:estimatedCost:actualCost:isPaid:paidBy:sharedBy:notes:createdAt:updatedAt:)")));

/**
 * Individual budget item (expense).
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Individual budget item (expense).
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Individual budget item (expense).
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) double actualCost __attribute__((swift_name("actualCost")));
@property (readonly) NSString *budgetId __attribute__((swift_name("budgetId")));
@property (readonly) SharedBudgetCategory *category __attribute__((swift_name("category")));

/**
 * Calculate cost per person for this item.
 */
@property (readonly) double costPerPerson __attribute__((swift_name("costPerPerson")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *description_ __attribute__((swift_name("description_")));
@property (readonly) double estimatedCost __attribute__((swift_name("estimatedCost")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) BOOL isPaid __attribute__((swift_name("isPaid")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) NSString *notes __attribute__((swift_name("notes")));
@property (readonly) NSString * _Nullable paidBy __attribute__((swift_name("paidBy")));

/**
 * Get the relevant cost (actual if paid, estimated otherwise).
 */
@property (readonly) double relevantCost __attribute__((swift_name("relevantCost")));
@property (readonly) NSArray<NSString *> *sharedBy __attribute__((swift_name("sharedBy")));
@property (readonly) NSString *updatedAt __attribute__((swift_name("updatedAt")));
@end


/**
 * Individual budget item (expense).
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BudgetItem_.Companion")))
@interface SharedBudgetItem_Companion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Individual budget item (expense).
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBudgetItem_Companion *shared __attribute__((swift_name("shared")));

/**
 * Individual budget item (expense).
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BudgetRange")))
@interface SharedBudgetRange : SharedKotlinEnum<SharedBudgetRange *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedBudgetRangeCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) SharedBudgetRange *low __attribute__((swift_name("low")));
@property (class, readonly) SharedBudgetRange *medium __attribute__((swift_name("medium")));
@property (class, readonly) SharedBudgetRange *high __attribute__((swift_name("high")));
+ (SharedKotlinArray<SharedBudgetRange *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedBudgetRange *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BudgetRange.Companion")))
@interface SharedBudgetRangeCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedBudgetRangeCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializerTypeParamsSerializers:(SharedKotlinArray<id<SharedKotlinx_serialization_coreKSerializer>> *)typeParamsSerializers __attribute__((swift_name("serializer(typeParamsSerializers:)")));
@end


/**
 * Budget with all its items.
 * Used for complete budget view.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("BudgetWithItems")))
@interface SharedBudgetWithItems : SharedBase
- (instancetype)initWithBudget:(SharedBudget_ *)budget items:(NSArray<SharedBudgetItem_ *> *)items categoryBreakdown:(NSArray<SharedBudgetCategoryDetails *> *)categoryBreakdown __attribute__((swift_name("init(budget:items:categoryBreakdown:)"))) __attribute__((objc_designated_initializer));
- (SharedBudgetWithItems *)doCopyBudget:(SharedBudget_ *)budget items:(NSArray<SharedBudgetItem_ *> *)items categoryBreakdown:(NSArray<SharedBudgetCategoryDetails *> *)categoryBreakdown __attribute__((swift_name("doCopy(budget:items:categoryBreakdown:)")));

/**
 * Budget with all its items.
 * Used for complete budget view.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Budget with all its items.
 * Used for complete budget view.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Budget with all its items.
 * Used for complete budget view.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedBudget_ *budget __attribute__((swift_name("budget")));
@property (readonly) NSArray<SharedBudgetCategoryDetails *> *categoryBreakdown __attribute__((swift_name("categoryBreakdown")));
@property (readonly) NSArray<SharedBudgetItem_ *> *items __attribute__((swift_name("items")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CalendarEvent")))
@interface SharedCalendarEvent : SharedBase
- (instancetype)initWithId:(NSString *)id title:(NSString *)title description:(NSString *)description startTime:(NSString *)startTime endTime:(NSString *)endTime timezone:(NSString *)timezone location:(NSString * _Nullable)location attendees:(NSArray<NSString *> *)attendees organizer:(NSString *)organizer eventId:(NSString *)eventId __attribute__((swift_name("init(id:title:description:startTime:endTime:timezone:location:attendees:organizer:eventId:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedCalendarEventCompanion *companion __attribute__((swift_name("companion")));
- (SharedCalendarEvent *)doCopyId:(NSString *)id title:(NSString *)title description:(NSString *)description startTime:(NSString *)startTime endTime:(NSString *)endTime timezone:(NSString *)timezone location:(NSString * _Nullable)location attendees:(NSArray<NSString *> *)attendees organizer:(NSString *)organizer eventId:(NSString *)eventId __attribute__((swift_name("doCopy(id:title:description:startTime:endTime:timezone:location:attendees:organizer:eventId:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<NSString *> *attendees __attribute__((swift_name("attendees")));
@property (readonly) NSString *description_ __attribute__((swift_name("description_")));
@property (readonly) NSString *endTime __attribute__((swift_name("endTime")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString * _Nullable location __attribute__((swift_name("location")));
@property (readonly) NSString *organizer __attribute__((swift_name("organizer")));
@property (readonly) NSString *startTime __attribute__((swift_name("startTime")));
@property (readonly) NSString *timezone __attribute__((swift_name("timezone")));
@property (readonly) NSString *title __attribute__((swift_name("title")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CalendarEvent.Companion")))
@interface SharedCalendarEventCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedCalendarEventCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CalendarInvite")))
@interface SharedCalendarInvite : SharedBase
- (instancetype)initWithEventId:(NSString *)eventId icsContent:(NSString *)icsContent generatedAt:(NSString *)generatedAt __attribute__((swift_name("init(eventId:icsContent:generatedAt:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedCalendarInviteCompanion *companion __attribute__((swift_name("companion")));
- (SharedCalendarInvite *)doCopyEventId:(NSString *)eventId icsContent:(NSString *)icsContent generatedAt:(NSString *)generatedAt __attribute__((swift_name("doCopy(eventId:icsContent:generatedAt:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *generatedAt __attribute__((swift_name("generatedAt")));
@property (readonly) NSString *icsContent __attribute__((swift_name("icsContent")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CalendarInvite.Companion")))
@interface SharedCalendarInviteCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedCalendarInviteCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((swift_name("CalendarService_")))
@protocol SharedCalendarService_
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)addEventToCalendarEvent:(SharedCalendarEvent *)event completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("addEventToCalendar(event:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)generateICSInviteEvent:(SharedCalendarEvent *)event completionHandler:(void (^)(SharedCalendarInvite * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("generateICSInvite(event:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)removeCalendarEventCalendarEventId:(NSString *)calendarEventId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("removeCalendarEvent(calendarEventId:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)updateCalendarEventCalendarEventId:(NSString *)calendarEventId event:(SharedCalendarEvent *)event completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("updateCalendarEvent(calendarEventId:event:completionHandler:)")));
@end


/**
 * API Request/Response models for the Wakev backend.
 * These are separate from domain models to allow for API evolution independent of domain changes.
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CreateEventRequest")))
@interface SharedCreateEventRequest : SharedBase
- (instancetype)initWithTitle:(NSString *)title description:(NSString *)description organizerId:(NSString *)organizerId deadline:(NSString *)deadline proposedSlots:(NSArray<SharedCreateTimeSlotRequest *> *)proposedSlots __attribute__((swift_name("init(title:description:organizerId:deadline:proposedSlots:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedCreateEventRequestCompanion *companion __attribute__((swift_name("companion")));
- (SharedCreateEventRequest *)doCopyTitle:(NSString *)title description:(NSString *)description organizerId:(NSString *)organizerId deadline:(NSString *)deadline proposedSlots:(NSArray<SharedCreateTimeSlotRequest *> *)proposedSlots __attribute__((swift_name("doCopy(title:description:organizerId:deadline:proposedSlots:)")));

/**
 * API Request/Response models for the Wakev backend.
 * These are separate from domain models to allow for API evolution independent of domain changes.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * API Request/Response models for the Wakev backend.
 * These are separate from domain models to allow for API evolution independent of domain changes.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * API Request/Response models for the Wakev backend.
 * These are separate from domain models to allow for API evolution independent of domain changes.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *deadline __attribute__((swift_name("deadline")));
@property (readonly) NSString *description_ __attribute__((swift_name("description_")));
@property (readonly) NSString *organizerId __attribute__((swift_name("organizerId")));
@property (readonly) NSArray<SharedCreateTimeSlotRequest *> *proposedSlots __attribute__((swift_name("proposedSlots")));
@property (readonly) NSString *title __attribute__((swift_name("title")));
@end


/**
 * API Request/Response models for the Wakev backend.
 * These are separate from domain models to allow for API evolution independent of domain changes.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CreateEventRequest.Companion")))
@interface SharedCreateEventRequestCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * API Request/Response models for the Wakev backend.
 * These are separate from domain models to allow for API evolution independent of domain changes.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedCreateEventRequestCompanion *shared __attribute__((swift_name("shared")));

/**
 * API Request/Response models for the Wakev backend.
 * These are separate from domain models to allow for API evolution independent of domain changes.
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CreateScenarioRequest")))
@interface SharedCreateScenarioRequest : SharedBase
- (instancetype)initWithEventId:(NSString *)eventId name:(NSString *)name dateOrPeriod:(NSString *)dateOrPeriod location:(NSString *)location duration:(int32_t)duration estimatedParticipants:(int32_t)estimatedParticipants estimatedBudgetPerPerson:(double)estimatedBudgetPerPerson description:(NSString *)description __attribute__((swift_name("init(eventId:name:dateOrPeriod:location:duration:estimatedParticipants:estimatedBudgetPerPerson:description:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedCreateScenarioRequestCompanion *companion __attribute__((swift_name("companion")));
- (SharedCreateScenarioRequest *)doCopyEventId:(NSString *)eventId name:(NSString *)name dateOrPeriod:(NSString *)dateOrPeriod location:(NSString *)location duration:(int32_t)duration estimatedParticipants:(int32_t)estimatedParticipants estimatedBudgetPerPerson:(double)estimatedBudgetPerPerson description:(NSString *)description __attribute__((swift_name("doCopy(eventId:name:dateOrPeriod:location:duration:estimatedParticipants:estimatedBudgetPerPerson:description:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *dateOrPeriod __attribute__((swift_name("dateOrPeriod")));
@property (readonly) NSString *description_ __attribute__((swift_name("description_")));
@property (readonly) int32_t duration __attribute__((swift_name("duration")));
@property (readonly) double estimatedBudgetPerPerson __attribute__((swift_name("estimatedBudgetPerPerson")));
@property (readonly) int32_t estimatedParticipants __attribute__((swift_name("estimatedParticipants")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *location __attribute__((swift_name("location")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CreateScenarioRequest.Companion")))
@interface SharedCreateScenarioRequestCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedCreateScenarioRequestCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CreateTimeSlotRequest")))
@interface SharedCreateTimeSlotRequest : SharedBase
- (instancetype)initWithId:(NSString *)id start:(NSString *)start end:(NSString *)end timezone:(NSString *)timezone __attribute__((swift_name("init(id:start:end:timezone:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedCreateTimeSlotRequestCompanion *companion __attribute__((swift_name("companion")));
- (SharedCreateTimeSlotRequest *)doCopyId:(NSString *)id start:(NSString *)start end:(NSString *)end timezone:(NSString *)timezone __attribute__((swift_name("doCopy(id:start:end:timezone:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *end __attribute__((swift_name("end")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *start __attribute__((swift_name("start")));
@property (readonly) NSString *timezone __attribute__((swift_name("timezone")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("CreateTimeSlotRequest.Companion")))
@interface SharedCreateTimeSlotRequestCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedCreateTimeSlotRequestCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Meal schedule for an event
 *
 * Groups meals by day for easy visualization.
 *
 * @property date The date
 * @property meals List of meals for this date
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DailyMealSchedule")))
@interface SharedDailyMealSchedule : SharedBase
- (instancetype)initWithDate:(NSString *)date meals:(NSArray<SharedMeal_ *> *)meals __attribute__((swift_name("init(date:meals:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedDailyMealScheduleCompanion *companion __attribute__((swift_name("companion")));
- (SharedDailyMealSchedule *)doCopyDate:(NSString *)date meals:(NSArray<SharedMeal_ *> *)meals __attribute__((swift_name("doCopy(date:meals:)")));

/**
 * Meal schedule for an event
 *
 * Groups meals by day for easy visualization.
 *
 * @property date The date
 * @property meals List of meals for this date
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Meal schedule for an event
 *
 * Groups meals by day for easy visualization.
 *
 * @property date The date
 * @property meals List of meals for this date
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Meal schedule for an event
 *
 * Groups meals by day for easy visualization.
 *
 * @property date The date
 * @property meals List of meals for this date
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *date __attribute__((swift_name("date")));
@property (readonly) NSArray<SharedMeal_ *> *meals __attribute__((swift_name("meals")));
@end


/**
 * Meal schedule for an event
 *
 * Groups meals by day for easy visualization.
 *
 * @property date The date
 * @property meals List of meals for this date
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DailyMealSchedule.Companion")))
@interface SharedDailyMealScheduleCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Meal schedule for an event
 *
 * Groups meals by day for easy visualization.
 *
 * @property date The date
 * @property meals List of meals for this date
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedDailyMealScheduleCompanion *shared __attribute__((swift_name("shared")));

/**
 * Meal schedule for an event
 *
 * Groups meals by day for easy visualization.
 *
 * @property date The date
 * @property meals List of meals for this date
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Common dietary restrictions
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DietaryRestriction")))
@interface SharedDietaryRestriction : SharedKotlinEnum<SharedDietaryRestriction *>
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Common dietary restrictions
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedDietaryRestrictionCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) SharedDietaryRestriction *vegetarian __attribute__((swift_name("vegetarian")));
@property (class, readonly) SharedDietaryRestriction *vegan __attribute__((swift_name("vegan")));
@property (class, readonly) SharedDietaryRestriction *glutenFree __attribute__((swift_name("glutenFree")));
@property (class, readonly) SharedDietaryRestriction *lactoseIntolerant __attribute__((swift_name("lactoseIntolerant")));
@property (class, readonly) SharedDietaryRestriction *nutAllergy __attribute__((swift_name("nutAllergy")));
@property (class, readonly) SharedDietaryRestriction *shellfishAllergy __attribute__((swift_name("shellfishAllergy")));
@property (class, readonly) SharedDietaryRestriction *kosher __attribute__((swift_name("kosher")));
@property (class, readonly) SharedDietaryRestriction *halal __attribute__((swift_name("halal")));
@property (class, readonly) SharedDietaryRestriction *diabetic __attribute__((swift_name("diabetic")));
@property (class, readonly) SharedDietaryRestriction *other __attribute__((swift_name("other")));
+ (SharedKotlinArray<SharedDietaryRestriction *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedDietaryRestriction *> *entries __attribute__((swift_name("entries")));
@end


/**
 * Common dietary restrictions
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DietaryRestriction.Companion")))
@interface SharedDietaryRestrictionCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Common dietary restrictions
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedDietaryRestrictionCompanion *shared __attribute__((swift_name("shared")));

/**
 * Common dietary restrictions
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));

/**
 * Common dietary restrictions
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializerTypeParamsSerializers:(SharedKotlinArray<id<SharedKotlinx_serialization_coreKSerializer>> *)typeParamsSerializers __attribute__((swift_name("serializer(typeParamsSerializers:)")));
@end


/**
 * Request to add dietary restriction for a participant
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DietaryRestrictionRequest")))
@interface SharedDietaryRestrictionRequest : SharedBase
- (instancetype)initWithParticipantId:(NSString *)participantId eventId:(NSString *)eventId restriction:(SharedDietaryRestriction *)restriction notes:(NSString * _Nullable)notes __attribute__((swift_name("init(participantId:eventId:restriction:notes:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedDietaryRestrictionRequestCompanion *companion __attribute__((swift_name("companion")));
- (SharedDietaryRestrictionRequest *)doCopyParticipantId:(NSString *)participantId eventId:(NSString *)eventId restriction:(SharedDietaryRestriction *)restriction notes:(NSString * _Nullable)notes __attribute__((swift_name("doCopy(participantId:eventId:restriction:notes:)")));

/**
 * Request to add dietary restriction for a participant
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Request to add dietary restriction for a participant
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Request to add dietary restriction for a participant
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString * _Nullable notes __attribute__((swift_name("notes")));
@property (readonly) NSString *participantId __attribute__((swift_name("participantId")));
@property (readonly) SharedDietaryRestriction *restriction __attribute__((swift_name("restriction")));
@end


/**
 * Request to add dietary restriction for a participant
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("DietaryRestrictionRequest.Companion")))
@interface SharedDietaryRestrictionRequestCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Request to add dietary restriction for a participant
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedDietaryRestrictionRequestCompanion *shared __attribute__((swift_name("shared")));

/**
 * Request to add dietary restriction for a participant
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Equipment items grouped by category
 *
 * @property category Equipment category
 * @property items List of items in this category
 * @property itemCount Total items in category
 * @property assignedCount Number of assigned items
 * @property totalCost Total cost for this category in cents
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EquipmentByCategory")))
@interface SharedEquipmentByCategory : SharedBase
- (instancetype)initWithCategory:(SharedEquipmentCategory *)category items:(NSArray<SharedEquipmentItem *> *)items itemCount:(int32_t)itemCount assignedCount:(int32_t)assignedCount totalCost:(int64_t)totalCost __attribute__((swift_name("init(category:items:itemCount:assignedCount:totalCost:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedEquipmentByCategoryCompanion *companion __attribute__((swift_name("companion")));
- (SharedEquipmentByCategory *)doCopyCategory:(SharedEquipmentCategory *)category items:(NSArray<SharedEquipmentItem *> *)items itemCount:(int32_t)itemCount assignedCount:(int32_t)assignedCount totalCost:(int64_t)totalCost __attribute__((swift_name("doCopy(category:items:itemCount:assignedCount:totalCost:)")));

/**
 * Equipment items grouped by category
 *
 * @property category Equipment category
 * @property items List of items in this category
 * @property itemCount Total items in category
 * @property assignedCount Number of assigned items
 * @property totalCost Total cost for this category in cents
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Equipment items grouped by category
 *
 * @property category Equipment category
 * @property items List of items in this category
 * @property itemCount Total items in category
 * @property assignedCount Number of assigned items
 * @property totalCost Total cost for this category in cents
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Equipment items grouped by category
 *
 * @property category Equipment category
 * @property items List of items in this category
 * @property itemCount Total items in category
 * @property assignedCount Number of assigned items
 * @property totalCost Total cost for this category in cents
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t assignedCount __attribute__((swift_name("assignedCount")));
@property (readonly) SharedEquipmentCategory *category __attribute__((swift_name("category")));
@property (readonly) int32_t itemCount __attribute__((swift_name("itemCount")));
@property (readonly) NSArray<SharedEquipmentItem *> *items __attribute__((swift_name("items")));
@property (readonly) int64_t totalCost __attribute__((swift_name("totalCost")));
@end


/**
 * Equipment items grouped by category
 *
 * @property category Equipment category
 * @property items List of items in this category
 * @property itemCount Total items in category
 * @property assignedCount Number of assigned items
 * @property totalCost Total cost for this category in cents
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EquipmentByCategory.Companion")))
@interface SharedEquipmentByCategoryCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Equipment items grouped by category
 *
 * @property category Equipment category
 * @property items List of items in this category
 * @property itemCount Total items in category
 * @property assignedCount Number of assigned items
 * @property totalCost Total cost for this category in cents
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedEquipmentByCategoryCompanion *shared __attribute__((swift_name("shared")));

/**
 * Equipment items grouped by category
 *
 * @property category Equipment category
 * @property items List of items in this category
 * @property itemCount Total items in category
 * @property assignedCount Number of assigned items
 * @property totalCost Total cost for this category in cents
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Equipment category
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EquipmentCategory")))
@interface SharedEquipmentCategory : SharedKotlinEnum<SharedEquipmentCategory *>
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Equipment category
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedEquipmentCategoryCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) SharedEquipmentCategory *camping __attribute__((swift_name("camping")));
@property (class, readonly) SharedEquipmentCategory *sports __attribute__((swift_name("sports")));
@property (class, readonly) SharedEquipmentCategory *cooking __attribute__((swift_name("cooking")));
@property (class, readonly) SharedEquipmentCategory *electronics __attribute__((swift_name("electronics")));
@property (class, readonly) SharedEquipmentCategory *safety __attribute__((swift_name("safety")));
@property (class, readonly) SharedEquipmentCategory *other __attribute__((swift_name("other")));
+ (SharedKotlinArray<SharedEquipmentCategory *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedEquipmentCategory *> *entries __attribute__((swift_name("entries")));
@end


/**
 * Equipment category
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EquipmentCategory.Companion")))
@interface SharedEquipmentCategoryCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Equipment category
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedEquipmentCategoryCompanion *shared __attribute__((swift_name("shared")));

/**
 * Equipment category
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));

/**
 * Equipment category
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializerTypeParamsSerializers:(SharedKotlinArray<id<SharedKotlinx_serialization_coreKSerializer>> *)typeParamsSerializers __attribute__((swift_name("serializer(typeParamsSerializers:)")));
@end


/**
 * Equipment checklist with statistics
 *
 * Provides an overview of equipment items grouped by category.
 *
 * @property eventId Event ID
 * @property items List of all equipment items
 * @property totalItems Total number of items
 * @property assignedItems Number of assigned items
 * @property confirmedItems Number of confirmed items
 * @property packedItems Number of packed items
 * @property totalCost Total shared cost in cents
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EquipmentChecklist")))
@interface SharedEquipmentChecklist : SharedBase
- (instancetype)initWithEventId:(NSString *)eventId items:(NSArray<SharedEquipmentItem *> *)items totalItems:(int32_t)totalItems assignedItems:(int32_t)assignedItems confirmedItems:(int32_t)confirmedItems packedItems:(int32_t)packedItems totalCost:(int64_t)totalCost __attribute__((swift_name("init(eventId:items:totalItems:assignedItems:confirmedItems:packedItems:totalCost:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedEquipmentChecklistCompanion *companion __attribute__((swift_name("companion")));
- (SharedEquipmentChecklist *)doCopyEventId:(NSString *)eventId items:(NSArray<SharedEquipmentItem *> *)items totalItems:(int32_t)totalItems assignedItems:(int32_t)assignedItems confirmedItems:(int32_t)confirmedItems packedItems:(int32_t)packedItems totalCost:(int64_t)totalCost __attribute__((swift_name("doCopy(eventId:items:totalItems:assignedItems:confirmedItems:packedItems:totalCost:)")));

/**
 * Equipment checklist with statistics
 *
 * Provides an overview of equipment items grouped by category.
 *
 * @property eventId Event ID
 * @property items List of all equipment items
 * @property totalItems Total number of items
 * @property assignedItems Number of assigned items
 * @property confirmedItems Number of confirmed items
 * @property packedItems Number of packed items
 * @property totalCost Total shared cost in cents
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Equipment checklist with statistics
 *
 * Provides an overview of equipment items grouped by category.
 *
 * @property eventId Event ID
 * @property items List of all equipment items
 * @property totalItems Total number of items
 * @property assignedItems Number of assigned items
 * @property confirmedItems Number of confirmed items
 * @property packedItems Number of packed items
 * @property totalCost Total shared cost in cents
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Equipment checklist with statistics
 *
 * Provides an overview of equipment items grouped by category.
 *
 * @property eventId Event ID
 * @property items List of all equipment items
 * @property totalItems Total number of items
 * @property assignedItems Number of assigned items
 * @property confirmedItems Number of confirmed items
 * @property packedItems Number of packed items
 * @property totalCost Total shared cost in cents
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t assignedItems __attribute__((swift_name("assignedItems")));
@property (readonly) int32_t confirmedItems __attribute__((swift_name("confirmedItems")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSArray<SharedEquipmentItem *> *items __attribute__((swift_name("items")));
@property (readonly) int32_t packedItems __attribute__((swift_name("packedItems")));
@property (readonly) int64_t totalCost __attribute__((swift_name("totalCost")));
@property (readonly) int32_t totalItems __attribute__((swift_name("totalItems")));
@end


/**
 * Equipment checklist with statistics
 *
 * Provides an overview of equipment items grouped by category.
 *
 * @property eventId Event ID
 * @property items List of all equipment items
 * @property totalItems Total number of items
 * @property assignedItems Number of assigned items
 * @property confirmedItems Number of confirmed items
 * @property packedItems Number of packed items
 * @property totalCost Total shared cost in cents
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EquipmentChecklist.Companion")))
@interface SharedEquipmentChecklistCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Equipment checklist with statistics
 *
 * Provides an overview of equipment items grouped by category.
 *
 * @property eventId Event ID
 * @property items List of all equipment items
 * @property totalItems Total number of items
 * @property assignedItems Number of assigned items
 * @property confirmedItems Number of confirmed items
 * @property packedItems Number of packed items
 * @property totalCost Total shared cost in cents
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedEquipmentChecklistCompanion *shared __attribute__((swift_name("shared")));

/**
 * Equipment checklist with statistics
 *
 * Provides an overview of equipment items grouped by category.
 *
 * @property eventId Event ID
 * @property items List of all equipment items
 * @property totalItems Total number of items
 * @property assignedItems Number of assigned items
 * @property confirmedItems Number of confirmed items
 * @property packedItems Number of packed items
 * @property totalCost Total shared cost in cents
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Equipment item for an event
 *
 * Represents an item needed for an event with assignment tracking.
 *
 * @property id Unique identifier
 * @property eventId Event this item belongs to
 * @property name Name of the item (e.g., "Tent 4 places", "Barbecue grill")
 * @property category Category of equipment
 * @property quantity Number of items needed
 * @property assignedTo Participant ID who will bring this item (null if unassigned)
 * @property status Current status of the item
 * @property sharedCost Cost shared among participants (in cents, null if no cost)
 * @property notes Additional notes or specifications
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EquipmentItem")))
@interface SharedEquipmentItem : SharedBase
- (instancetype)initWithId:(NSString *)id eventId:(NSString *)eventId name:(NSString *)name category:(SharedEquipmentCategory *)category quantity:(int32_t)quantity assignedTo:(NSString * _Nullable)assignedTo status:(SharedItemStatus *)status sharedCost:(SharedLong * _Nullable)sharedCost notes:(NSString * _Nullable)notes createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("init(id:eventId:name:category:quantity:assignedTo:status:sharedCost:notes:createdAt:updatedAt:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedEquipmentItemCompanion *companion __attribute__((swift_name("companion")));
- (SharedEquipmentItem *)doCopyId:(NSString *)id eventId:(NSString *)eventId name:(NSString *)name category:(SharedEquipmentCategory *)category quantity:(int32_t)quantity assignedTo:(NSString * _Nullable)assignedTo status:(SharedItemStatus *)status sharedCost:(SharedLong * _Nullable)sharedCost notes:(NSString * _Nullable)notes createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("doCopy(id:eventId:name:category:quantity:assignedTo:status:sharedCost:notes:createdAt:updatedAt:)")));

/**
 * Equipment item for an event
 *
 * Represents an item needed for an event with assignment tracking.
 *
 * @property id Unique identifier
 * @property eventId Event this item belongs to
 * @property name Name of the item (e.g., "Tent 4 places", "Barbecue grill")
 * @property category Category of equipment
 * @property quantity Number of items needed
 * @property assignedTo Participant ID who will bring this item (null if unassigned)
 * @property status Current status of the item
 * @property sharedCost Cost shared among participants (in cents, null if no cost)
 * @property notes Additional notes or specifications
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Equipment item for an event
 *
 * Represents an item needed for an event with assignment tracking.
 *
 * @property id Unique identifier
 * @property eventId Event this item belongs to
 * @property name Name of the item (e.g., "Tent 4 places", "Barbecue grill")
 * @property category Category of equipment
 * @property quantity Number of items needed
 * @property assignedTo Participant ID who will bring this item (null if unassigned)
 * @property status Current status of the item
 * @property sharedCost Cost shared among participants (in cents, null if no cost)
 * @property notes Additional notes or specifications
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Equipment item for an event
 *
 * Represents an item needed for an event with assignment tracking.
 *
 * @property id Unique identifier
 * @property eventId Event this item belongs to
 * @property name Name of the item (e.g., "Tent 4 places", "Barbecue grill")
 * @property category Category of equipment
 * @property quantity Number of items needed
 * @property assignedTo Participant ID who will bring this item (null if unassigned)
 * @property status Current status of the item
 * @property sharedCost Cost shared among participants (in cents, null if no cost)
 * @property notes Additional notes or specifications
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable assignedTo __attribute__((swift_name("assignedTo")));
@property (readonly) SharedEquipmentCategory *category __attribute__((swift_name("category")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) NSString * _Nullable notes __attribute__((swift_name("notes")));
@property (readonly) int32_t quantity __attribute__((swift_name("quantity")));
@property (readonly) SharedLong * _Nullable sharedCost __attribute__((swift_name("sharedCost")));
@property (readonly) SharedItemStatus *status __attribute__((swift_name("status")));
@property (readonly) NSString *updatedAt __attribute__((swift_name("updatedAt")));
@end


/**
 * Equipment item for an event
 *
 * Represents an item needed for an event with assignment tracking.
 *
 * @property id Unique identifier
 * @property eventId Event this item belongs to
 * @property name Name of the item (e.g., "Tent 4 places", "Barbecue grill")
 * @property category Category of equipment
 * @property quantity Number of items needed
 * @property assignedTo Participant ID who will bring this item (null if unassigned)
 * @property status Current status of the item
 * @property sharedCost Cost shared among participants (in cents, null if no cost)
 * @property notes Additional notes or specifications
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EquipmentItem.Companion")))
@interface SharedEquipmentItemCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Equipment item for an event
 *
 * Represents an item needed for an event with assignment tracking.
 *
 * @property id Unique identifier
 * @property eventId Event this item belongs to
 * @property name Name of the item (e.g., "Tent 4 places", "Barbecue grill")
 * @property category Category of equipment
 * @property quantity Number of items needed
 * @property assignedTo Participant ID who will bring this item (null if unassigned)
 * @property status Current status of the item
 * @property sharedCost Cost shared among participants (in cents, null if no cost)
 * @property notes Additional notes or specifications
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedEquipmentItemCompanion *shared __attribute__((swift_name("shared")));

/**
 * Equipment item for an event
 *
 * Represents an item needed for an event with assignment tracking.
 *
 * @property id Unique identifier
 * @property eventId Event this item belongs to
 * @property name Name of the item (e.g., "Tent 4 places", "Barbecue grill")
 * @property category Category of equipment
 * @property quantity Number of items needed
 * @property assignedTo Participant ID who will bring this item (null if unassigned)
 * @property status Current status of the item
 * @property sharedCost Cost shared among participants (in cents, null if no cost)
 * @property notes Additional notes or specifications
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Request to create or update an equipment item
 *
 * @property name Item name
 * @property category Category
 * @property quantity Quantity needed
 * @property assignedTo Assigned participant ID (optional)
 * @property status Item status
 * @property sharedCost Shared cost in cents (optional)
 * @property notes Additional notes (optional)
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EquipmentItemRequest")))
@interface SharedEquipmentItemRequest : SharedBase
- (instancetype)initWithName:(NSString *)name category:(SharedEquipmentCategory *)category quantity:(int32_t)quantity assignedTo:(NSString * _Nullable)assignedTo status:(SharedItemStatus *)status sharedCost:(SharedLong * _Nullable)sharedCost notes:(NSString * _Nullable)notes __attribute__((swift_name("init(name:category:quantity:assignedTo:status:sharedCost:notes:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedEquipmentItemRequestCompanion *companion __attribute__((swift_name("companion")));
- (SharedEquipmentItemRequest *)doCopyName:(NSString *)name category:(SharedEquipmentCategory *)category quantity:(int32_t)quantity assignedTo:(NSString * _Nullable)assignedTo status:(SharedItemStatus *)status sharedCost:(SharedLong * _Nullable)sharedCost notes:(NSString * _Nullable)notes __attribute__((swift_name("doCopy(name:category:quantity:assignedTo:status:sharedCost:notes:)")));

/**
 * Request to create or update an equipment item
 *
 * @property name Item name
 * @property category Category
 * @property quantity Quantity needed
 * @property assignedTo Assigned participant ID (optional)
 * @property status Item status
 * @property sharedCost Shared cost in cents (optional)
 * @property notes Additional notes (optional)
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Request to create or update an equipment item
 *
 * @property name Item name
 * @property category Category
 * @property quantity Quantity needed
 * @property assignedTo Assigned participant ID (optional)
 * @property status Item status
 * @property sharedCost Shared cost in cents (optional)
 * @property notes Additional notes (optional)
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Request to create or update an equipment item
 *
 * @property name Item name
 * @property category Category
 * @property quantity Quantity needed
 * @property assignedTo Assigned participant ID (optional)
 * @property status Item status
 * @property sharedCost Shared cost in cents (optional)
 * @property notes Additional notes (optional)
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable assignedTo __attribute__((swift_name("assignedTo")));
@property (readonly) SharedEquipmentCategory *category __attribute__((swift_name("category")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) NSString * _Nullable notes __attribute__((swift_name("notes")));
@property (readonly) int32_t quantity __attribute__((swift_name("quantity")));
@property (readonly) SharedLong * _Nullable sharedCost __attribute__((swift_name("sharedCost")));
@property (readonly) SharedItemStatus *status __attribute__((swift_name("status")));
@end


/**
 * Request to create or update an equipment item
 *
 * @property name Item name
 * @property category Category
 * @property quantity Quantity needed
 * @property assignedTo Assigned participant ID (optional)
 * @property status Item status
 * @property sharedCost Shared cost in cents (optional)
 * @property notes Additional notes (optional)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EquipmentItemRequest.Companion")))
@interface SharedEquipmentItemRequestCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Request to create or update an equipment item
 *
 * @property name Item name
 * @property category Category
 * @property quantity Quantity needed
 * @property assignedTo Assigned participant ID (optional)
 * @property status Item status
 * @property sharedCost Shared cost in cents (optional)
 * @property notes Additional notes (optional)
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedEquipmentItemRequestCompanion *shared __attribute__((swift_name("shared")));

/**
 * Request to create or update an equipment item
 *
 * @property name Item name
 * @property category Category
 * @property quantity Quantity needed
 * @property assignedTo Assigned participant ID (optional)
 * @property status Item status
 * @property sharedCost Shared cost in cents (optional)
 * @property notes Additional notes (optional)
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ErrorResponse")))
@interface SharedErrorResponse : SharedBase
- (instancetype)initWithError:(NSString *)error message:(NSString *)message __attribute__((swift_name("init(error:message:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedErrorResponseCompanion *companion __attribute__((swift_name("companion")));
- (SharedErrorResponse *)doCopyError:(NSString *)error message:(NSString *)message __attribute__((swift_name("doCopy(error:message:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *error __attribute__((swift_name("error")));
@property (readonly) NSString *message __attribute__((swift_name("message")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ErrorResponse.Companion")))
@interface SharedErrorResponseCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedErrorResponseCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Event")))
@interface SharedEvent : SharedBase
- (instancetype)initWithId:(NSString *)id title:(NSString *)title description:(NSString *)description organizerId:(NSString *)organizerId participants:(NSArray<NSString *> *)participants proposedSlots:(NSArray<SharedTimeSlot *> *)proposedSlots deadline:(NSString *)deadline status:(SharedEventStatus *)status finalDate:(NSString * _Nullable)finalDate createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("init(id:title:description:organizerId:participants:proposedSlots:deadline:status:finalDate:createdAt:updatedAt:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedEventCompanion *companion __attribute__((swift_name("companion")));
- (SharedEvent *)doCopyId:(NSString *)id title:(NSString *)title description:(NSString *)description organizerId:(NSString *)organizerId participants:(NSArray<NSString *> *)participants proposedSlots:(NSArray<SharedTimeSlot *> *)proposedSlots deadline:(NSString *)deadline status:(SharedEventStatus *)status finalDate:(NSString * _Nullable)finalDate createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("doCopy(id:title:description:organizerId:participants:proposedSlots:deadline:status:finalDate:createdAt:updatedAt:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *deadline __attribute__((swift_name("deadline")));
@property (readonly) NSString *description_ __attribute__((swift_name("description_")));
@property (readonly) NSString * _Nullable finalDate __attribute__((swift_name("finalDate")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *organizerId __attribute__((swift_name("organizerId")));
@property (readonly) NSArray<NSString *> *participants __attribute__((swift_name("participants")));
@property (readonly) NSArray<SharedTimeSlot *> *proposedSlots __attribute__((swift_name("proposedSlots")));
@property (readonly) SharedEventStatus *status __attribute__((swift_name("status")));
@property (readonly) NSString *title __attribute__((swift_name("title")));
@property (readonly) NSString *updatedAt __attribute__((swift_name("updatedAt")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Event.Companion")))
@interface SharedEventCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedEventCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EventResponse")))
@interface SharedEventResponse : SharedBase
- (instancetype)initWithId:(NSString *)id title:(NSString *)title description:(NSString *)description organizerId:(NSString *)organizerId participants:(NSArray<NSString *> *)participants deadline:(NSString *)deadline status:(NSString *)status proposedSlots:(NSArray<SharedTimeSlotResponse *> *)proposedSlots finalDate:(NSString * _Nullable)finalDate __attribute__((swift_name("init(id:title:description:organizerId:participants:deadline:status:proposedSlots:finalDate:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedEventResponseCompanion *companion __attribute__((swift_name("companion")));
- (SharedEventResponse *)doCopyId:(NSString *)id title:(NSString *)title description:(NSString *)description organizerId:(NSString *)organizerId participants:(NSArray<NSString *> *)participants deadline:(NSString *)deadline status:(NSString *)status proposedSlots:(NSArray<SharedTimeSlotResponse *> *)proposedSlots finalDate:(NSString * _Nullable)finalDate __attribute__((swift_name("doCopy(id:title:description:organizerId:participants:deadline:status:proposedSlots:finalDate:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *deadline __attribute__((swift_name("deadline")));
@property (readonly) NSString *description_ __attribute__((swift_name("description_")));
@property (readonly) NSString * _Nullable finalDate __attribute__((swift_name("finalDate")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *organizerId __attribute__((swift_name("organizerId")));
@property (readonly) NSArray<NSString *> *participants __attribute__((swift_name("participants")));
@property (readonly) NSArray<SharedTimeSlotResponse *> *proposedSlots __attribute__((swift_name("proposedSlots")));
@property (readonly) NSString *status __attribute__((swift_name("status")));
@property (readonly) NSString *title __attribute__((swift_name("title")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EventResponse.Companion")))
@interface SharedEventResponseCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedEventResponseCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EventStatus")))
@interface SharedEventStatus : SharedKotlinEnum<SharedEventStatus *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedEventStatusCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) SharedEventStatus *draft __attribute__((swift_name("draft")));
@property (class, readonly) SharedEventStatus *polling __attribute__((swift_name("polling")));
@property (class, readonly) SharedEventStatus *comparing __attribute__((swift_name("comparing")));
@property (class, readonly) SharedEventStatus *confirmed __attribute__((swift_name("confirmed")));
@property (class, readonly) SharedEventStatus *organizing __attribute__((swift_name("organizing")));
@property (class, readonly) SharedEventStatus *finalized __attribute__((swift_name("finalized")));
+ (SharedKotlinArray<SharedEventStatus *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedEventStatus *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("EventStatus.Companion")))
@interface SharedEventStatusCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedEventStatusCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializerTypeParamsSerializers:(SharedKotlinArray<id<SharedKotlinx_serialization_coreKSerializer>> *)typeParamsSerializers __attribute__((swift_name("serializer(typeParamsSerializers:)")));
@end


/**
 * Request to generate a default equipment checklist
 *
 * @property eventType Type of event (e.g., "camping", "beach", "ski", "indoor")
 * @property participantCount Number of participants
 * @property duration Duration in days
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GenerateChecklistRequest")))
@interface SharedGenerateChecklistRequest : SharedBase
- (instancetype)initWithEventType:(NSString *)eventType participantCount:(int32_t)participantCount duration:(int32_t)duration __attribute__((swift_name("init(eventType:participantCount:duration:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedGenerateChecklistRequestCompanion *companion __attribute__((swift_name("companion")));
- (SharedGenerateChecklistRequest *)doCopyEventType:(NSString *)eventType participantCount:(int32_t)participantCount duration:(int32_t)duration __attribute__((swift_name("doCopy(eventType:participantCount:duration:)")));

/**
 * Request to generate a default equipment checklist
 *
 * @property eventType Type of event (e.g., "camping", "beach", "ski", "indoor")
 * @property participantCount Number of participants
 * @property duration Duration in days
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Request to generate a default equipment checklist
 *
 * @property eventType Type of event (e.g., "camping", "beach", "ski", "indoor")
 * @property participantCount Number of participants
 * @property duration Duration in days
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Request to generate a default equipment checklist
 *
 * @property eventType Type of event (e.g., "camping", "beach", "ski", "indoor")
 * @property participantCount Number of participants
 * @property duration Duration in days
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t duration __attribute__((swift_name("duration")));
@property (readonly) NSString *eventType __attribute__((swift_name("eventType")));
@property (readonly) int32_t participantCount __attribute__((swift_name("participantCount")));
@end


/**
 * Request to generate a default equipment checklist
 *
 * @property eventType Type of event (e.g., "camping", "beach", "ski", "indoor")
 * @property participantCount Number of participants
 * @property duration Duration in days
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("GenerateChecklistRequest.Companion")))
@interface SharedGenerateChecklistRequestCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Request to generate a default equipment checklist
 *
 * @property eventType Type of event (e.g., "camping", "beach", "ski", "indoor")
 * @property participantCount Number of participants
 * @property duration Duration in days
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedGenerateChecklistRequestCompanion *shared __attribute__((swift_name("shared")));

/**
 * Request to generate a default equipment checklist
 *
 * @property eventType Type of event (e.g., "camping", "beach", "ski", "indoor")
 * @property participantCount Number of participants
 * @property duration Duration in days
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Equipment item status
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ItemStatus")))
@interface SharedItemStatus : SharedKotlinEnum<SharedItemStatus *>
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Equipment item status
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedItemStatusCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) SharedItemStatus *needed __attribute__((swift_name("needed")));
@property (class, readonly) SharedItemStatus *assigned __attribute__((swift_name("assigned")));
@property (class, readonly) SharedItemStatus *confirmed __attribute__((swift_name("confirmed")));
@property (class, readonly) SharedItemStatus *packed __attribute__((swift_name("packed")));
@property (class, readonly) SharedItemStatus *cancelled __attribute__((swift_name("cancelled")));
+ (SharedKotlinArray<SharedItemStatus *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedItemStatus *> *entries __attribute__((swift_name("entries")));
@end


/**
 * Equipment item status
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ItemStatus.Companion")))
@interface SharedItemStatusCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Equipment item status
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedItemStatusCompanion *shared __attribute__((swift_name("shared")));

/**
 * Equipment item status
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));

/**
 * Equipment item status
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializerTypeParamsSerializers:(SharedKotlinArray<id<SharedKotlinx_serialization_coreKSerializer>> *)typeParamsSerializers __attribute__((swift_name("serializer(typeParamsSerializers:)")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Location")))
@interface SharedLocation : SharedBase
- (instancetype)initWithName:(NSString *)name address:(NSString * _Nullable)address latitude:(SharedDouble * _Nullable)latitude longitude:(SharedDouble * _Nullable)longitude iataCode:(NSString * _Nullable)iataCode __attribute__((swift_name("init(name:address:latitude:longitude:iataCode:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedLocationCompanion *companion __attribute__((swift_name("companion")));
- (SharedLocation *)doCopyName:(NSString *)name address:(NSString * _Nullable)address latitude:(SharedDouble * _Nullable)latitude longitude:(SharedDouble * _Nullable)longitude iataCode:(NSString * _Nullable)iataCode __attribute__((swift_name("doCopy(name:address:latitude:longitude:iataCode:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable address __attribute__((swift_name("address")));
@property (readonly) NSString * _Nullable iataCode __attribute__((swift_name("iataCode")));
@property (readonly) SharedDouble * _Nullable latitude __attribute__((swift_name("latitude")));
@property (readonly) SharedDouble * _Nullable longitude __attribute__((swift_name("longitude")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Location.Companion")))
@interface SharedLocationCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedLocationCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Meal for an event
 *
 * Represents a planned meal with assignments and dietary considerations.
 *
 * @property id Unique identifier
 * @property eventId Event this meal belongs to
 * @property type Type of meal
 * @property name Name/description of the meal (e.g., "Barbecue du samedi soir")
 * @property date Date of the meal (ISO 8601 date)
 * @property time Time of the meal (HH:MM format)
 * @property location Where the meal takes place (optional)
 * @property responsibleParticipantIds List of participant IDs responsible for this meal
 * @property estimatedCost Estimated cost in cents
 * @property actualCost Actual cost in cents (null if not yet incurred)
 * @property servings Number of people to serve
 * @property status Current status of meal planning
 * @property notes Additional notes or menu details
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Meal_")))
@interface SharedMeal_ : SharedBase
- (instancetype)initWithId:(NSString *)id eventId:(NSString *)eventId type:(SharedMealType *)type name:(NSString *)name date:(NSString *)date time:(NSString *)time location:(NSString * _Nullable)location responsibleParticipantIds:(NSArray<NSString *> *)responsibleParticipantIds estimatedCost:(int64_t)estimatedCost actualCost:(SharedLong * _Nullable)actualCost servings:(int32_t)servings status:(SharedMealStatus *)status notes:(NSString * _Nullable)notes createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("init(id:eventId:type:name:date:time:location:responsibleParticipantIds:estimatedCost:actualCost:servings:status:notes:createdAt:updatedAt:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedMeal_Companion *companion __attribute__((swift_name("companion")));
- (SharedMeal_ *)doCopyId:(NSString *)id eventId:(NSString *)eventId type:(SharedMealType *)type name:(NSString *)name date:(NSString *)date time:(NSString *)time location:(NSString * _Nullable)location responsibleParticipantIds:(NSArray<NSString *> *)responsibleParticipantIds estimatedCost:(int64_t)estimatedCost actualCost:(SharedLong * _Nullable)actualCost servings:(int32_t)servings status:(SharedMealStatus *)status notes:(NSString * _Nullable)notes createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("doCopy(id:eventId:type:name:date:time:location:responsibleParticipantIds:estimatedCost:actualCost:servings:status:notes:createdAt:updatedAt:)")));

/**
 * Meal for an event
 *
 * Represents a planned meal with assignments and dietary considerations.
 *
 * @property id Unique identifier
 * @property eventId Event this meal belongs to
 * @property type Type of meal
 * @property name Name/description of the meal (e.g., "Barbecue du samedi soir")
 * @property date Date of the meal (ISO 8601 date)
 * @property time Time of the meal (HH:MM format)
 * @property location Where the meal takes place (optional)
 * @property responsibleParticipantIds List of participant IDs responsible for this meal
 * @property estimatedCost Estimated cost in cents
 * @property actualCost Actual cost in cents (null if not yet incurred)
 * @property servings Number of people to serve
 * @property status Current status of meal planning
 * @property notes Additional notes or menu details
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Meal for an event
 *
 * Represents a planned meal with assignments and dietary considerations.
 *
 * @property id Unique identifier
 * @property eventId Event this meal belongs to
 * @property type Type of meal
 * @property name Name/description of the meal (e.g., "Barbecue du samedi soir")
 * @property date Date of the meal (ISO 8601 date)
 * @property time Time of the meal (HH:MM format)
 * @property location Where the meal takes place (optional)
 * @property responsibleParticipantIds List of participant IDs responsible for this meal
 * @property estimatedCost Estimated cost in cents
 * @property actualCost Actual cost in cents (null if not yet incurred)
 * @property servings Number of people to serve
 * @property status Current status of meal planning
 * @property notes Additional notes or menu details
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Meal for an event
 *
 * Represents a planned meal with assignments and dietary considerations.
 *
 * @property id Unique identifier
 * @property eventId Event this meal belongs to
 * @property type Type of meal
 * @property name Name/description of the meal (e.g., "Barbecue du samedi soir")
 * @property date Date of the meal (ISO 8601 date)
 * @property time Time of the meal (HH:MM format)
 * @property location Where the meal takes place (optional)
 * @property responsibleParticipantIds List of participant IDs responsible for this meal
 * @property estimatedCost Estimated cost in cents
 * @property actualCost Actual cost in cents (null if not yet incurred)
 * @property servings Number of people to serve
 * @property status Current status of meal planning
 * @property notes Additional notes or menu details
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedLong * _Nullable actualCost __attribute__((swift_name("actualCost")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *date __attribute__((swift_name("date")));
@property (readonly) int64_t estimatedCost __attribute__((swift_name("estimatedCost")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString * _Nullable location __attribute__((swift_name("location")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) NSString * _Nullable notes __attribute__((swift_name("notes")));
@property (readonly) NSArray<NSString *> *responsibleParticipantIds __attribute__((swift_name("responsibleParticipantIds")));
@property (readonly) int32_t servings __attribute__((swift_name("servings")));
@property (readonly) SharedMealStatus *status __attribute__((swift_name("status")));
@property (readonly) NSString *time __attribute__((swift_name("time")));
@property (readonly) SharedMealType *type __attribute__((swift_name("type")));
@property (readonly) NSString *updatedAt __attribute__((swift_name("updatedAt")));
@end


/**
 * Meal for an event
 *
 * Represents a planned meal with assignments and dietary considerations.
 *
 * @property id Unique identifier
 * @property eventId Event this meal belongs to
 * @property type Type of meal
 * @property name Name/description of the meal (e.g., "Barbecue du samedi soir")
 * @property date Date of the meal (ISO 8601 date)
 * @property time Time of the meal (HH:MM format)
 * @property location Where the meal takes place (optional)
 * @property responsibleParticipantIds List of participant IDs responsible for this meal
 * @property estimatedCost Estimated cost in cents
 * @property actualCost Actual cost in cents (null if not yet incurred)
 * @property servings Number of people to serve
 * @property status Current status of meal planning
 * @property notes Additional notes or menu details
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Meal_.Companion")))
@interface SharedMeal_Companion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Meal for an event
 *
 * Represents a planned meal with assignments and dietary considerations.
 *
 * @property id Unique identifier
 * @property eventId Event this meal belongs to
 * @property type Type of meal
 * @property name Name/description of the meal (e.g., "Barbecue du samedi soir")
 * @property date Date of the meal (ISO 8601 date)
 * @property time Time of the meal (HH:MM format)
 * @property location Where the meal takes place (optional)
 * @property responsibleParticipantIds List of participant IDs responsible for this meal
 * @property estimatedCost Estimated cost in cents
 * @property actualCost Actual cost in cents (null if not yet incurred)
 * @property servings Number of people to serve
 * @property status Current status of meal planning
 * @property notes Additional notes or menu details
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMeal_Companion *shared __attribute__((swift_name("shared")));

/**
 * Meal for an event
 *
 * Represents a planned meal with assignments and dietary considerations.
 *
 * @property id Unique identifier
 * @property eventId Event this meal belongs to
 * @property type Type of meal
 * @property name Name/description of the meal (e.g., "Barbecue du samedi soir")
 * @property date Date of the meal (ISO 8601 date)
 * @property time Time of the meal (HH:MM format)
 * @property location Where the meal takes place (optional)
 * @property responsibleParticipantIds List of participant IDs responsible for this meal
 * @property estimatedCost Estimated cost in cents
 * @property actualCost Actual cost in cents (null if not yet incurred)
 * @property servings Number of people to serve
 * @property status Current status of meal planning
 * @property notes Additional notes or menu details
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Meal planning summary
 *
 * @property totalMeals Total number of planned meals
 * @property totalEstimatedCost Total estimated cost in cents
 * @property totalActualCost Total actual cost in cents (completed meals only)
 * @property mealsCompleted Number of completed meals
 * @property mealsRemaining Number of meals not yet completed
 * @property mealsByType Count of meals by type
 * @property mealsByStatus Count of meals by status
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MealPlanningSummary")))
@interface SharedMealPlanningSummary : SharedBase
- (instancetype)initWithTotalMeals:(int32_t)totalMeals totalEstimatedCost:(int64_t)totalEstimatedCost totalActualCost:(int64_t)totalActualCost mealsCompleted:(int32_t)mealsCompleted mealsRemaining:(int32_t)mealsRemaining mealsByType:(NSDictionary<SharedMealType *, SharedInt *> *)mealsByType mealsByStatus:(NSDictionary<SharedMealStatus *, SharedInt *> *)mealsByStatus __attribute__((swift_name("init(totalMeals:totalEstimatedCost:totalActualCost:mealsCompleted:mealsRemaining:mealsByType:mealsByStatus:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedMealPlanningSummaryCompanion *companion __attribute__((swift_name("companion")));
- (SharedMealPlanningSummary *)doCopyTotalMeals:(int32_t)totalMeals totalEstimatedCost:(int64_t)totalEstimatedCost totalActualCost:(int64_t)totalActualCost mealsCompleted:(int32_t)mealsCompleted mealsRemaining:(int32_t)mealsRemaining mealsByType:(NSDictionary<SharedMealType *, SharedInt *> *)mealsByType mealsByStatus:(NSDictionary<SharedMealStatus *, SharedInt *> *)mealsByStatus __attribute__((swift_name("doCopy(totalMeals:totalEstimatedCost:totalActualCost:mealsCompleted:mealsRemaining:mealsByType:mealsByStatus:)")));

/**
 * Meal planning summary
 *
 * @property totalMeals Total number of planned meals
 * @property totalEstimatedCost Total estimated cost in cents
 * @property totalActualCost Total actual cost in cents (completed meals only)
 * @property mealsCompleted Number of completed meals
 * @property mealsRemaining Number of meals not yet completed
 * @property mealsByType Count of meals by type
 * @property mealsByStatus Count of meals by status
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Meal planning summary
 *
 * @property totalMeals Total number of planned meals
 * @property totalEstimatedCost Total estimated cost in cents
 * @property totalActualCost Total actual cost in cents (completed meals only)
 * @property mealsCompleted Number of completed meals
 * @property mealsRemaining Number of meals not yet completed
 * @property mealsByType Count of meals by type
 * @property mealsByStatus Count of meals by status
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Meal planning summary
 *
 * @property totalMeals Total number of planned meals
 * @property totalEstimatedCost Total estimated cost in cents
 * @property totalActualCost Total actual cost in cents (completed meals only)
 * @property mealsCompleted Number of completed meals
 * @property mealsRemaining Number of meals not yet completed
 * @property mealsByType Count of meals by type
 * @property mealsByStatus Count of meals by status
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSDictionary<SharedMealStatus *, SharedInt *> *mealsByStatus __attribute__((swift_name("mealsByStatus")));
@property (readonly) NSDictionary<SharedMealType *, SharedInt *> *mealsByType __attribute__((swift_name("mealsByType")));
@property (readonly) int32_t mealsCompleted __attribute__((swift_name("mealsCompleted")));
@property (readonly) int32_t mealsRemaining __attribute__((swift_name("mealsRemaining")));
@property (readonly) int64_t totalActualCost __attribute__((swift_name("totalActualCost")));
@property (readonly) int64_t totalEstimatedCost __attribute__((swift_name("totalEstimatedCost")));
@property (readonly) int32_t totalMeals __attribute__((swift_name("totalMeals")));
@end


/**
 * Meal planning summary
 *
 * @property totalMeals Total number of planned meals
 * @property totalEstimatedCost Total estimated cost in cents
 * @property totalActualCost Total actual cost in cents (completed meals only)
 * @property mealsCompleted Number of completed meals
 * @property mealsRemaining Number of meals not yet completed
 * @property mealsByType Count of meals by type
 * @property mealsByStatus Count of meals by status
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MealPlanningSummary.Companion")))
@interface SharedMealPlanningSummaryCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Meal planning summary
 *
 * @property totalMeals Total number of planned meals
 * @property totalEstimatedCost Total estimated cost in cents
 * @property totalActualCost Total actual cost in cents (completed meals only)
 * @property mealsCompleted Number of completed meals
 * @property mealsRemaining Number of meals not yet completed
 * @property mealsByType Count of meals by type
 * @property mealsByStatus Count of meals by status
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMealPlanningSummaryCompanion *shared __attribute__((swift_name("shared")));

/**
 * Meal planning summary
 *
 * @property totalMeals Total number of planned meals
 * @property totalEstimatedCost Total estimated cost in cents
 * @property totalActualCost Total actual cost in cents (completed meals only)
 * @property mealsCompleted Number of completed meals
 * @property mealsRemaining Number of meals not yet completed
 * @property mealsByType Count of meals by type
 * @property mealsByStatus Count of meals by status
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Request to create or update a meal
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MealRequest")))
@interface SharedMealRequest : SharedBase
- (instancetype)initWithEventId:(NSString *)eventId type:(SharedMealType *)type name:(NSString *)name date:(NSString *)date time:(NSString *)time location:(NSString * _Nullable)location responsibleParticipantIds:(NSArray<NSString *> *)responsibleParticipantIds estimatedCost:(int64_t)estimatedCost actualCost:(SharedLong * _Nullable)actualCost servings:(int32_t)servings status:(SharedMealStatus *)status notes:(NSString * _Nullable)notes __attribute__((swift_name("init(eventId:type:name:date:time:location:responsibleParticipantIds:estimatedCost:actualCost:servings:status:notes:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedMealRequestCompanion *companion __attribute__((swift_name("companion")));
- (SharedMealRequest *)doCopyEventId:(NSString *)eventId type:(SharedMealType *)type name:(NSString *)name date:(NSString *)date time:(NSString *)time location:(NSString * _Nullable)location responsibleParticipantIds:(NSArray<NSString *> *)responsibleParticipantIds estimatedCost:(int64_t)estimatedCost actualCost:(SharedLong * _Nullable)actualCost servings:(int32_t)servings status:(SharedMealStatus *)status notes:(NSString * _Nullable)notes __attribute__((swift_name("doCopy(eventId:type:name:date:time:location:responsibleParticipantIds:estimatedCost:actualCost:servings:status:notes:)")));

/**
 * Request to create or update a meal
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Request to create or update a meal
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Request to create or update a meal
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedLong * _Nullable actualCost __attribute__((swift_name("actualCost")));
@property (readonly) NSString *date __attribute__((swift_name("date")));
@property (readonly) int64_t estimatedCost __attribute__((swift_name("estimatedCost")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString * _Nullable location __attribute__((swift_name("location")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) NSString * _Nullable notes __attribute__((swift_name("notes")));
@property (readonly) NSArray<NSString *> *responsibleParticipantIds __attribute__((swift_name("responsibleParticipantIds")));
@property (readonly) int32_t servings __attribute__((swift_name("servings")));
@property (readonly) SharedMealStatus *status __attribute__((swift_name("status")));
@property (readonly) NSString *time __attribute__((swift_name("time")));
@property (readonly) SharedMealType *type __attribute__((swift_name("type")));
@end


/**
 * Request to create or update a meal
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MealRequest.Companion")))
@interface SharedMealRequestCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Request to create or update a meal
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMealRequestCompanion *shared __attribute__((swift_name("shared")));

/**
 * Request to create or update a meal
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Status of meal planning
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MealStatus")))
@interface SharedMealStatus : SharedKotlinEnum<SharedMealStatus *>
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Status of meal planning
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedMealStatusCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) SharedMealStatus *planned __attribute__((swift_name("planned")));
@property (class, readonly) SharedMealStatus *assigned __attribute__((swift_name("assigned")));
@property (class, readonly) SharedMealStatus *inProgress __attribute__((swift_name("inProgress")));
@property (class, readonly) SharedMealStatus *completed __attribute__((swift_name("completed")));
@property (class, readonly) SharedMealStatus *cancelled __attribute__((swift_name("cancelled")));
+ (SharedKotlinArray<SharedMealStatus *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedMealStatus *> *entries __attribute__((swift_name("entries")));
@end


/**
 * Status of meal planning
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MealStatus.Companion")))
@interface SharedMealStatusCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Status of meal planning
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMealStatusCompanion *shared __attribute__((swift_name("shared")));

/**
 * Status of meal planning
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));

/**
 * Status of meal planning
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializerTypeParamsSerializers:(SharedKotlinArray<id<SharedKotlinx_serialization_coreKSerializer>> *)typeParamsSerializers __attribute__((swift_name("serializer(typeParamsSerializers:)")));
@end


/**
 * Type of meal
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MealType")))
@interface SharedMealType : SharedKotlinEnum<SharedMealType *>
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Type of meal
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedMealTypeCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) SharedMealType *breakfast __attribute__((swift_name("breakfast")));
@property (class, readonly) SharedMealType *lunch __attribute__((swift_name("lunch")));
@property (class, readonly) SharedMealType *dinner __attribute__((swift_name("dinner")));
@property (class, readonly) SharedMealType *snack __attribute__((swift_name("snack")));
@property (class, readonly) SharedMealType *aperitif __attribute__((swift_name("aperitif")));
+ (SharedKotlinArray<SharedMealType *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedMealType *> *entries __attribute__((swift_name("entries")));
@end


/**
 * Type of meal
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MealType.Companion")))
@interface SharedMealTypeCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Type of meal
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMealTypeCompanion *shared __attribute__((swift_name("shared")));

/**
 * Type of meal
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));

/**
 * Type of meal
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializerTypeParamsSerializers:(SharedKotlinArray<id<SharedKotlinx_serialization_coreKSerializer>> *)typeParamsSerializers __attribute__((swift_name("serializer(typeParamsSerializers:)")));
@end


/**
 * Meal with associated restrictions
 *
 * Combines meal details with relevant dietary restrictions to consider.
 *
 * @property meal The meal details
 * @property relevantRestrictions Dietary restrictions of participants attending
 * @property restrictionCounts Count of each type of restriction
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MealWithRestrictions")))
@interface SharedMealWithRestrictions : SharedBase
- (instancetype)initWithMeal:(SharedMeal_ *)meal relevantRestrictions:(NSArray<SharedParticipantDietaryRestriction *> *)relevantRestrictions restrictionCounts:(NSDictionary<SharedDietaryRestriction *, SharedInt *> *)restrictionCounts __attribute__((swift_name("init(meal:relevantRestrictions:restrictionCounts:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedMealWithRestrictionsCompanion *companion __attribute__((swift_name("companion")));
- (SharedMealWithRestrictions *)doCopyMeal:(SharedMeal_ *)meal relevantRestrictions:(NSArray<SharedParticipantDietaryRestriction *> *)relevantRestrictions restrictionCounts:(NSDictionary<SharedDietaryRestriction *, SharedInt *> *)restrictionCounts __attribute__((swift_name("doCopy(meal:relevantRestrictions:restrictionCounts:)")));

/**
 * Meal with associated restrictions
 *
 * Combines meal details with relevant dietary restrictions to consider.
 *
 * @property meal The meal details
 * @property relevantRestrictions Dietary restrictions of participants attending
 * @property restrictionCounts Count of each type of restriction
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Meal with associated restrictions
 *
 * Combines meal details with relevant dietary restrictions to consider.
 *
 * @property meal The meal details
 * @property relevantRestrictions Dietary restrictions of participants attending
 * @property restrictionCounts Count of each type of restriction
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Meal with associated restrictions
 *
 * Combines meal details with relevant dietary restrictions to consider.
 *
 * @property meal The meal details
 * @property relevantRestrictions Dietary restrictions of participants attending
 * @property restrictionCounts Count of each type of restriction
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedMeal_ *meal __attribute__((swift_name("meal")));
@property (readonly) NSArray<SharedParticipantDietaryRestriction *> *relevantRestrictions __attribute__((swift_name("relevantRestrictions")));
@property (readonly) NSDictionary<SharedDietaryRestriction *, SharedInt *> *restrictionCounts __attribute__((swift_name("restrictionCounts")));
@end


/**
 * Meal with associated restrictions
 *
 * Combines meal details with relevant dietary restrictions to consider.
 *
 * @property meal The meal details
 * @property relevantRestrictions Dietary restrictions of participants attending
 * @property restrictionCounts Count of each type of restriction
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("MealWithRestrictions.Companion")))
@interface SharedMealWithRestrictionsCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Meal with associated restrictions
 *
 * Combines meal details with relevant dietary restrictions to consider.
 *
 * @property meal The meal details
 * @property relevantRestrictions Dietary restrictions of participants attending
 * @property restrictionCounts Count of each type of restriction
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedMealWithRestrictionsCompanion *shared __attribute__((swift_name("shared")));

/**
 * Meal with associated restrictions
 *
 * Combines meal details with relevant dietary restrictions to consider.
 *
 * @property meal The meal details
 * @property relevantRestrictions Dietary restrictions of participants attending
 * @property restrictionCounts Count of each type of restriction
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NotificationMessage")))
@interface SharedNotificationMessage : SharedBase
- (instancetype)initWithId:(NSString *)id userId:(NSString *)userId type:(SharedNotificationType *)type title:(NSString *)title body:(NSString *)body data:(NSDictionary<NSString *, NSString *> *)data sentAt:(NSString * _Nullable)sentAt readAt:(NSString * _Nullable)readAt __attribute__((swift_name("init(id:userId:type:title:body:data:sentAt:readAt:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedNotificationMessageCompanion *companion __attribute__((swift_name("companion")));
- (SharedNotificationMessage *)doCopyId:(NSString *)id userId:(NSString *)userId type:(SharedNotificationType *)type title:(NSString *)title body:(NSString *)body data:(NSDictionary<NSString *, NSString *> *)data sentAt:(NSString * _Nullable)sentAt readAt:(NSString * _Nullable)readAt __attribute__((swift_name("doCopy(id:userId:type:title:body:data:sentAt:readAt:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *body __attribute__((swift_name("body")));
@property (readonly) NSDictionary<NSString *, NSString *> *data __attribute__((swift_name("data")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString * _Nullable readAt __attribute__((swift_name("readAt")));
@property (readonly) NSString * _Nullable sentAt __attribute__((swift_name("sentAt")));
@property (readonly) NSString *title __attribute__((swift_name("title")));
@property (readonly) SharedNotificationType *type __attribute__((swift_name("type")));
@property (readonly) NSString *userId __attribute__((swift_name("userId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NotificationMessage.Companion")))
@interface SharedNotificationMessageCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedNotificationMessageCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Notification preferences domain model
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NotificationPreferences")))
@interface SharedNotificationPreferences : SharedBase
- (instancetype)initWithId:(NSString *)id userId:(NSString *)userId deadlineReminder:(BOOL)deadlineReminder eventUpdate:(BOOL)eventUpdate voteCloseReminder:(BOOL)voteCloseReminder timezone:(NSString *)timezone createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("init(id:userId:deadlineReminder:eventUpdate:voteCloseReminder:timezone:createdAt:updatedAt:)"))) __attribute__((objc_designated_initializer));
- (SharedNotificationPreferences *)doCopyId:(NSString *)id userId:(NSString *)userId deadlineReminder:(BOOL)deadlineReminder eventUpdate:(BOOL)eventUpdate voteCloseReminder:(BOOL)voteCloseReminder timezone:(NSString *)timezone createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("doCopy(id:userId:deadlineReminder:eventUpdate:voteCloseReminder:timezone:createdAt:updatedAt:)")));

/**
 * Notification preferences domain model
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Notification preferences domain model
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Notification preferences domain model
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) BOOL deadlineReminder __attribute__((swift_name("deadlineReminder")));
@property (readonly) BOOL eventUpdate __attribute__((swift_name("eventUpdate")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *timezone __attribute__((swift_name("timezone")));
@property (readonly) NSString *updatedAt __attribute__((swift_name("updatedAt")));
@property (readonly) NSString *userId __attribute__((swift_name("userId")));
@property (readonly) BOOL voteCloseReminder __attribute__((swift_name("voteCloseReminder")));
@end

__attribute__((swift_name("NotificationService_")))
@protocol SharedNotificationService_
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getUnreadNotificationsUserId:(NSString *)userId completionHandler:(void (^)(NSArray<SharedNotificationMessage *> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getUnreadNotifications(userId:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)markAsReadNotificationId:(NSString *)notificationId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("markAsRead(notificationId:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)registerPushTokenToken:(SharedPushToken *)token completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("registerPushToken(token:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)sendNotificationMessage:(SharedNotificationMessage *)message completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("sendNotification(message:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)unregisterPushTokenUserId:(NSString *)userId deviceId:(NSString *)deviceId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("unregisterPushToken(userId:deviceId:completionHandler:)")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NotificationType")))
@interface SharedNotificationType : SharedKotlinEnum<SharedNotificationType *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedNotificationTypeCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) SharedNotificationType *deadlineReminder __attribute__((swift_name("deadlineReminder")));
@property (class, readonly) SharedNotificationType *eventUpdate __attribute__((swift_name("eventUpdate")));
@property (class, readonly) SharedNotificationType *voteCloseReminder __attribute__((swift_name("voteCloseReminder")));
@property (class, readonly) SharedNotificationType *eventConfirmed __attribute__((swift_name("eventConfirmed")));
@property (class, readonly) SharedNotificationType *participantJoined __attribute__((swift_name("participantJoined")));
@property (class, readonly) SharedNotificationType *voteSubmitted __attribute__((swift_name("voteSubmitted")));
+ (SharedKotlinArray<SharedNotificationType *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedNotificationType *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("NotificationType.Companion")))
@interface SharedNotificationTypeCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedNotificationTypeCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializerTypeParamsSerializers:(SharedKotlinArray<id<SharedKotlinx_serialization_coreKSerializer>> *)typeParamsSerializers __attribute__((swift_name("serializer(typeParamsSerializers:)")));
@end


/**
 * OAuth login request model
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OAuthLoginRequest")))
@interface SharedOAuthLoginRequest : SharedBase
- (instancetype)initWithProvider:(NSString *)provider idToken:(NSString * _Nullable)idToken accessToken:(NSString * _Nullable)accessToken authorizationCode:(NSString * _Nullable)authorizationCode refreshToken:(NSString * _Nullable)refreshToken __attribute__((swift_name("init(provider:idToken:accessToken:authorizationCode:refreshToken:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedOAuthLoginRequestCompanion *companion __attribute__((swift_name("companion")));
- (SharedOAuthLoginRequest *)doCopyProvider:(NSString *)provider idToken:(NSString * _Nullable)idToken accessToken:(NSString * _Nullable)accessToken authorizationCode:(NSString * _Nullable)authorizationCode refreshToken:(NSString * _Nullable)refreshToken __attribute__((swift_name("doCopy(provider:idToken:accessToken:authorizationCode:refreshToken:)")));

/**
 * OAuth login request model
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * OAuth login request model
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * OAuth login request model
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable accessToken __attribute__((swift_name("accessToken")));
@property (readonly) NSString * _Nullable authorizationCode __attribute__((swift_name("authorizationCode")));
@property (readonly) NSString * _Nullable idToken __attribute__((swift_name("idToken")));
@property (readonly) NSString *provider __attribute__((swift_name("provider")));
@property (readonly) NSString * _Nullable refreshToken __attribute__((swift_name("refreshToken")));
@end


/**
 * OAuth login request model
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OAuthLoginRequest.Companion")))
@interface SharedOAuthLoginRequestCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * OAuth login request model
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedOAuthLoginRequestCompanion *shared __attribute__((swift_name("shared")));

/**
 * OAuth login request model
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * OAuth login response model
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OAuthLoginResponse")))
@interface SharedOAuthLoginResponse : SharedBase
- (instancetype)initWithUser:(SharedUserResponse *)user accessToken:(NSString *)accessToken refreshToken:(NSString * _Nullable)refreshToken tokenType:(NSString *)tokenType expiresIn:(int64_t)expiresIn scope:(NSString * _Nullable)scope __attribute__((swift_name("init(user:accessToken:refreshToken:tokenType:expiresIn:scope:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedOAuthLoginResponseCompanion *companion __attribute__((swift_name("companion")));
- (SharedOAuthLoginResponse *)doCopyUser:(SharedUserResponse *)user accessToken:(NSString *)accessToken refreshToken:(NSString * _Nullable)refreshToken tokenType:(NSString *)tokenType expiresIn:(int64_t)expiresIn scope:(NSString * _Nullable)scope __attribute__((swift_name("doCopy(user:accessToken:refreshToken:tokenType:expiresIn:scope:)")));

/**
 * OAuth login response model
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * OAuth login response model
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * OAuth login response model
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *accessToken __attribute__((swift_name("accessToken")));
@property (readonly) int64_t expiresIn __attribute__((swift_name("expiresIn")));
@property (readonly) NSString * _Nullable refreshToken __attribute__((swift_name("refreshToken")));
@property (readonly) NSString * _Nullable scope __attribute__((swift_name("scope")));
@property (readonly) NSString *tokenType __attribute__((swift_name("tokenType")));
@property (readonly) SharedUserResponse *user __attribute__((swift_name("user")));
@end


/**
 * OAuth login response model
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OAuthLoginResponse.Companion")))
@interface SharedOAuthLoginResponseCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * OAuth login response model
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedOAuthLoginResponseCompanion *shared __attribute__((swift_name("shared")));

/**
 * OAuth login response model
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OAuthProvider")))
@interface SharedOAuthProvider : SharedKotlinEnum<SharedOAuthProvider *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedOAuthProvider *google __attribute__((swift_name("google")));
@property (class, readonly) SharedOAuthProvider *apple __attribute__((swift_name("apple")));
+ (SharedKotlinArray<SharedOAuthProvider *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedOAuthProvider *> *entries __attribute__((swift_name("entries")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OptimizationType")))
@interface SharedOptimizationType : SharedKotlinEnum<SharedOptimizationType *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedOptimizationTypeCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) SharedOptimizationType *costMinimize __attribute__((swift_name("costMinimize")));
@property (class, readonly) SharedOptimizationType *timeMinimize __attribute__((swift_name("timeMinimize")));
@property (class, readonly) SharedOptimizationType *balanced __attribute__((swift_name("balanced")));
+ (SharedKotlinArray<SharedOptimizationType *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedOptimizationType *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("OptimizationType.Companion")))
@interface SharedOptimizationTypeCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedOptimizationTypeCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializerTypeParamsSerializers:(SharedKotlinArray<id<SharedKotlinx_serialization_coreKSerializer>> *)typeParamsSerializers __attribute__((swift_name("serializer(typeParamsSerializers:)")));
@end


/**
 * Participant accommodation details
 *
 * Shows where a specific participant is staying.
 *
 * @property participantId The participant's ID
 * @property accommodation The accommodation they're assigned to
 * @property roomAssignment The specific room assignment
 * @property costShare Their share of the accommodation cost (in cents)
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ParticipantAccommodation")))
@interface SharedParticipantAccommodation : SharedBase
- (instancetype)initWithParticipantId:(NSString *)participantId accommodation:(SharedAccommodation_ *)accommodation roomAssignment:(SharedRoomAssignment *)roomAssignment costShare:(int64_t)costShare __attribute__((swift_name("init(participantId:accommodation:roomAssignment:costShare:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedParticipantAccommodationCompanion *companion __attribute__((swift_name("companion")));
- (SharedParticipantAccommodation *)doCopyParticipantId:(NSString *)participantId accommodation:(SharedAccommodation_ *)accommodation roomAssignment:(SharedRoomAssignment *)roomAssignment costShare:(int64_t)costShare __attribute__((swift_name("doCopy(participantId:accommodation:roomAssignment:costShare:)")));

/**
 * Participant accommodation details
 *
 * Shows where a specific participant is staying.
 *
 * @property participantId The participant's ID
 * @property accommodation The accommodation they're assigned to
 * @property roomAssignment The specific room assignment
 * @property costShare Their share of the accommodation cost (in cents)
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Participant accommodation details
 *
 * Shows where a specific participant is staying.
 *
 * @property participantId The participant's ID
 * @property accommodation The accommodation they're assigned to
 * @property roomAssignment The specific room assignment
 * @property costShare Their share of the accommodation cost (in cents)
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Participant accommodation details
 *
 * Shows where a specific participant is staying.
 *
 * @property participantId The participant's ID
 * @property accommodation The accommodation they're assigned to
 * @property roomAssignment The specific room assignment
 * @property costShare Their share of the accommodation cost (in cents)
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedAccommodation_ *accommodation __attribute__((swift_name("accommodation")));
@property (readonly) int64_t costShare __attribute__((swift_name("costShare")));
@property (readonly) NSString *participantId __attribute__((swift_name("participantId")));
@property (readonly) SharedRoomAssignment *roomAssignment __attribute__((swift_name("roomAssignment")));
@end


/**
 * Participant accommodation details
 *
 * Shows where a specific participant is staying.
 *
 * @property participantId The participant's ID
 * @property accommodation The accommodation they're assigned to
 * @property roomAssignment The specific room assignment
 * @property costShare Their share of the accommodation cost (in cents)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ParticipantAccommodation.Companion")))
@interface SharedParticipantAccommodationCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Participant accommodation details
 *
 * Shows where a specific participant is staying.
 *
 * @property participantId The participant's ID
 * @property accommodation The accommodation they're assigned to
 * @property roomAssignment The specific room assignment
 * @property costShare Their share of the accommodation cost (in cents)
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedParticipantAccommodationCompanion *shared __attribute__((swift_name("shared")));

/**
 * Participant accommodation details
 *
 * Shows where a specific participant is staying.
 *
 * @property participantId The participant's ID
 * @property accommodation The accommodation they're assigned to
 * @property roomAssignment The specific room assignment
 * @property costShare Their share of the accommodation cost (in cents)
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Participant activity statistics
 *
 * @property participantId Participant ID
 * @property registeredCount Number of activities registered
 * @property totalCost Total cost for this participant in cents
 * @property activityNames List of activity names
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ParticipantActivityStats")))
@interface SharedParticipantActivityStats : SharedBase
- (instancetype)initWithParticipantId:(NSString *)participantId registeredCount:(int32_t)registeredCount totalCost:(int64_t)totalCost activityNames:(NSArray<NSString *> *)activityNames __attribute__((swift_name("init(participantId:registeredCount:totalCost:activityNames:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedParticipantActivityStatsCompanion *companion __attribute__((swift_name("companion")));
- (SharedParticipantActivityStats *)doCopyParticipantId:(NSString *)participantId registeredCount:(int32_t)registeredCount totalCost:(int64_t)totalCost activityNames:(NSArray<NSString *> *)activityNames __attribute__((swift_name("doCopy(participantId:registeredCount:totalCost:activityNames:)")));

/**
 * Participant activity statistics
 *
 * @property participantId Participant ID
 * @property registeredCount Number of activities registered
 * @property totalCost Total cost for this participant in cents
 * @property activityNames List of activity names
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Participant activity statistics
 *
 * @property participantId Participant ID
 * @property registeredCount Number of activities registered
 * @property totalCost Total cost for this participant in cents
 * @property activityNames List of activity names
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Participant activity statistics
 *
 * @property participantId Participant ID
 * @property registeredCount Number of activities registered
 * @property totalCost Total cost for this participant in cents
 * @property activityNames List of activity names
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<NSString *> *activityNames __attribute__((swift_name("activityNames")));
@property (readonly) NSString *participantId __attribute__((swift_name("participantId")));
@property (readonly) int32_t registeredCount __attribute__((swift_name("registeredCount")));
@property (readonly) int64_t totalCost __attribute__((swift_name("totalCost")));
@end


/**
 * Participant activity statistics
 *
 * @property participantId Participant ID
 * @property registeredCount Number of activities registered
 * @property totalCost Total cost for this participant in cents
 * @property activityNames List of activity names
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ParticipantActivityStats.Companion")))
@interface SharedParticipantActivityStatsCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Participant activity statistics
 *
 * @property participantId Participant ID
 * @property registeredCount Number of activities registered
 * @property totalCost Total cost for this participant in cents
 * @property activityNames List of activity names
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedParticipantActivityStatsCompanion *shared __attribute__((swift_name("shared")));

/**
 * Participant activity statistics
 *
 * @property participantId Participant ID
 * @property registeredCount Number of activities registered
 * @property totalCost Total cost for this participant in cents
 * @property activityNames List of activity names
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Participant's share of the budget.
 * Used for cost splitting and tracking.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ParticipantBudgetShare")))
@interface SharedParticipantBudgetShare : SharedBase
- (instancetype)initWithParticipantId:(NSString *)participantId totalOwed:(double)totalOwed totalPaid:(double)totalPaid itemsShared:(NSArray<SharedBudgetItem_ *> *)itemsShared itemsPaid:(NSArray<SharedBudgetItem_ *> *)itemsPaid __attribute__((swift_name("init(participantId:totalOwed:totalPaid:itemsShared:itemsPaid:)"))) __attribute__((objc_designated_initializer));
- (SharedParticipantBudgetShare *)doCopyParticipantId:(NSString *)participantId totalOwed:(double)totalOwed totalPaid:(double)totalPaid itemsShared:(NSArray<SharedBudgetItem_ *> *)itemsShared itemsPaid:(NSArray<SharedBudgetItem_ *> *)itemsPaid __attribute__((swift_name("doCopy(participantId:totalOwed:totalPaid:itemsShared:itemsPaid:)")));

/**
 * Participant's share of the budget.
 * Used for cost splitting and tracking.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Participant's share of the budget.
 * Used for cost splitting and tracking.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Participant's share of the budget.
 * Used for cost splitting and tracking.
 */
- (NSString *)description __attribute__((swift_name("description()")));

/**
 * Calculate balance (positive = owes money, negative = is owed money).
 */
@property (readonly) double balance __attribute__((swift_name("balance")));
@property (readonly) BOOL isBalanced __attribute__((swift_name("isBalanced")));
@property (readonly) BOOL isOwed __attribute__((swift_name("isOwed")));
@property (readonly) NSArray<SharedBudgetItem_ *> *itemsPaid __attribute__((swift_name("itemsPaid")));
@property (readonly) NSArray<SharedBudgetItem_ *> *itemsShared __attribute__((swift_name("itemsShared")));
@property (readonly) BOOL owesMore __attribute__((swift_name("owesMore")));
@property (readonly) NSString *participantId __attribute__((swift_name("participantId")));
@property (readonly) double totalOwed __attribute__((swift_name("totalOwed")));
@property (readonly) double totalPaid __attribute__((swift_name("totalPaid")));
@end


/**
 * Participant dietary restrictions mapping
 *
 * Associates dietary restrictions with participants.
 *
 * @property id Unique identifier
 * @property participantId Participant ID
 * @property eventId Event ID (for scoping)
 * @property restriction Type of dietary restriction
 * @property notes Additional details about the restriction
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ParticipantDietaryRestriction")))
@interface SharedParticipantDietaryRestriction : SharedBase
- (instancetype)initWithId:(NSString *)id participantId:(NSString *)participantId eventId:(NSString *)eventId restriction:(SharedDietaryRestriction *)restriction notes:(NSString * _Nullable)notes createdAt:(NSString *)createdAt __attribute__((swift_name("init(id:participantId:eventId:restriction:notes:createdAt:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedParticipantDietaryRestrictionCompanion *companion __attribute__((swift_name("companion")));
- (SharedParticipantDietaryRestriction *)doCopyId:(NSString *)id participantId:(NSString *)participantId eventId:(NSString *)eventId restriction:(SharedDietaryRestriction *)restriction notes:(NSString * _Nullable)notes createdAt:(NSString *)createdAt __attribute__((swift_name("doCopy(id:participantId:eventId:restriction:notes:createdAt:)")));

/**
 * Participant dietary restrictions mapping
 *
 * Associates dietary restrictions with participants.
 *
 * @property id Unique identifier
 * @property participantId Participant ID
 * @property eventId Event ID (for scoping)
 * @property restriction Type of dietary restriction
 * @property notes Additional details about the restriction
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Participant dietary restrictions mapping
 *
 * Associates dietary restrictions with participants.
 *
 * @property id Unique identifier
 * @property participantId Participant ID
 * @property eventId Event ID (for scoping)
 * @property restriction Type of dietary restriction
 * @property notes Additional details about the restriction
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Participant dietary restrictions mapping
 *
 * Associates dietary restrictions with participants.
 *
 * @property id Unique identifier
 * @property participantId Participant ID
 * @property eventId Event ID (for scoping)
 * @property restriction Type of dietary restriction
 * @property notes Additional details about the restriction
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString * _Nullable notes __attribute__((swift_name("notes")));
@property (readonly) NSString *participantId __attribute__((swift_name("participantId")));
@property (readonly) SharedDietaryRestriction *restriction __attribute__((swift_name("restriction")));
@end


/**
 * Participant dietary restrictions mapping
 *
 * Associates dietary restrictions with participants.
 *
 * @property id Unique identifier
 * @property participantId Participant ID
 * @property eventId Event ID (for scoping)
 * @property restriction Type of dietary restriction
 * @property notes Additional details about the restriction
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ParticipantDietaryRestriction.Companion")))
@interface SharedParticipantDietaryRestrictionCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Participant dietary restrictions mapping
 *
 * Associates dietary restrictions with participants.
 *
 * @property id Unique identifier
 * @property participantId Participant ID
 * @property eventId Event ID (for scoping)
 * @property restriction Type of dietary restriction
 * @property notes Additional details about the restriction
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedParticipantDietaryRestrictionCompanion *shared __attribute__((swift_name("shared")));

/**
 * Participant dietary restrictions mapping
 *
 * Associates dietary restrictions with participants.
 *
 * @property id Unique identifier
 * @property participantId Participant ID
 * @property eventId Event ID (for scoping)
 * @property restriction Type of dietary restriction
 * @property notes Additional details about the restriction
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Equipment statistics per participant
 *
 * @property participantId Participant ID
 * @property assignedItemsCount Number of items assigned to this participant
 * @property itemNames List of item names assigned
 * @property totalValue Total value of assigned items in cents
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ParticipantEquipmentStats")))
@interface SharedParticipantEquipmentStats : SharedBase
- (instancetype)initWithParticipantId:(NSString *)participantId assignedItemsCount:(int32_t)assignedItemsCount itemNames:(NSArray<NSString *> *)itemNames totalValue:(int64_t)totalValue __attribute__((swift_name("init(participantId:assignedItemsCount:itemNames:totalValue:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedParticipantEquipmentStatsCompanion *companion __attribute__((swift_name("companion")));
- (SharedParticipantEquipmentStats *)doCopyParticipantId:(NSString *)participantId assignedItemsCount:(int32_t)assignedItemsCount itemNames:(NSArray<NSString *> *)itemNames totalValue:(int64_t)totalValue __attribute__((swift_name("doCopy(participantId:assignedItemsCount:itemNames:totalValue:)")));

/**
 * Equipment statistics per participant
 *
 * @property participantId Participant ID
 * @property assignedItemsCount Number of items assigned to this participant
 * @property itemNames List of item names assigned
 * @property totalValue Total value of assigned items in cents
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Equipment statistics per participant
 *
 * @property participantId Participant ID
 * @property assignedItemsCount Number of items assigned to this participant
 * @property itemNames List of item names assigned
 * @property totalValue Total value of assigned items in cents
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Equipment statistics per participant
 *
 * @property participantId Participant ID
 * @property assignedItemsCount Number of items assigned to this participant
 * @property itemNames List of item names assigned
 * @property totalValue Total value of assigned items in cents
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t assignedItemsCount __attribute__((swift_name("assignedItemsCount")));
@property (readonly) NSArray<NSString *> *itemNames __attribute__((swift_name("itemNames")));
@property (readonly) NSString *participantId __attribute__((swift_name("participantId")));
@property (readonly) int64_t totalValue __attribute__((swift_name("totalValue")));
@end


/**
 * Equipment statistics per participant
 *
 * @property participantId Participant ID
 * @property assignedItemsCount Number of items assigned to this participant
 * @property itemNames List of item names assigned
 * @property totalValue Total value of assigned items in cents
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ParticipantEquipmentStats.Companion")))
@interface SharedParticipantEquipmentStatsCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Equipment statistics per participant
 *
 * @property participantId Participant ID
 * @property assignedItemsCount Number of items assigned to this participant
 * @property itemNames List of item names assigned
 * @property totalValue Total value of assigned items in cents
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedParticipantEquipmentStatsCompanion *shared __attribute__((swift_name("shared")));

/**
 * Equipment statistics per participant
 *
 * @property participantId Participant ID
 * @property assignedItemsCount Number of items assigned to this participant
 * @property itemNames List of item names assigned
 * @property totalValue Total value of assigned items in cents
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Poll")))
@interface SharedPoll : SharedBase
- (instancetype)initWithId:(NSString *)id eventId:(NSString *)eventId votes:(NSDictionary<NSString *, NSDictionary<NSString *, SharedVote *> *> *)votes __attribute__((swift_name("init(id:eventId:votes:)"))) __attribute__((objc_designated_initializer));
- (SharedPoll *)doCopyId:(NSString *)id eventId:(NSString *)eventId votes:(NSDictionary<NSString *, NSDictionary<NSString *, SharedVote *> *> *)votes __attribute__((swift_name("doCopy(id:eventId:votes:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSDictionary<NSString *, NSDictionary<NSString *, SharedVote *> *> *votes __attribute__((swift_name("votes")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PollResponse")))
@interface SharedPollResponse : SharedBase
- (instancetype)initWithEventId:(NSString *)eventId votes:(NSDictionary<NSString *, NSDictionary<NSString *, NSString *> *> *)votes __attribute__((swift_name("init(eventId:votes:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedPollResponseCompanion *companion __attribute__((swift_name("companion")));
- (SharedPollResponse *)doCopyEventId:(NSString *)eventId votes:(NSDictionary<NSString *, NSDictionary<NSString *, NSString *> *> *)votes __attribute__((swift_name("doCopy(eventId:votes:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSDictionary<NSString *, NSDictionary<NSString *, NSString *> *> *votes __attribute__((swift_name("votes")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PollResponse.Companion")))
@interface SharedPollResponseCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedPollResponseCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PushToken")))
@interface SharedPushToken : SharedBase
- (instancetype)initWithUserId:(NSString *)userId token:(NSString *)token platform:(NSString *)platform deviceId:(NSString *)deviceId registeredAt:(NSString *)registeredAt __attribute__((swift_name("init(userId:token:platform:deviceId:registeredAt:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedPushTokenCompanion *companion __attribute__((swift_name("companion")));
- (SharedPushToken *)doCopyUserId:(NSString *)userId token:(NSString *)token platform:(NSString *)platform deviceId:(NSString *)deviceId registeredAt:(NSString *)registeredAt __attribute__((swift_name("doCopy(userId:token:platform:deviceId:registeredAt:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *deviceId __attribute__((swift_name("deviceId")));
@property (readonly) NSString *platform __attribute__((swift_name("platform")));
@property (readonly) NSString *registeredAt __attribute__((swift_name("registeredAt")));
@property (readonly) NSString *token __attribute__((swift_name("token")));
@property (readonly) NSString *userId __attribute__((swift_name("userId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("PushToken.Companion")))
@interface SharedPushTokenCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedPushTokenCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Recommendation")))
@interface SharedRecommendation : SharedBase
- (instancetype)initWithId:(NSString *)id type:(SharedRecommendationType *)type eventId:(NSString *)eventId content:(NSString *)content score:(double)score reason:(NSString *)reason createdAt:(NSString *)createdAt __attribute__((swift_name("init(id:type:eventId:content:score:reason:createdAt:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedRecommendationCompanion *companion __attribute__((swift_name("companion")));
- (SharedRecommendation *)doCopyId:(NSString *)id type:(SharedRecommendationType *)type eventId:(NSString *)eventId content:(NSString *)content score:(double)score reason:(NSString *)reason createdAt:(NSString *)createdAt __attribute__((swift_name("doCopy(id:type:eventId:content:score:reason:createdAt:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *content __attribute__((swift_name("content")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *reason __attribute__((swift_name("reason")));
@property (readonly) double score __attribute__((swift_name("score")));
@property (readonly) SharedRecommendationType *type __attribute__((swift_name("type")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Recommendation.Companion")))
@interface SharedRecommendationCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedRecommendationCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecommendationType")))
@interface SharedRecommendationType : SharedKotlinEnum<SharedRecommendationType *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedRecommendationTypeCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) SharedRecommendationType *date __attribute__((swift_name("date")));
@property (class, readonly) SharedRecommendationType *location __attribute__((swift_name("location")));
@property (class, readonly) SharedRecommendationType *activity __attribute__((swift_name("activity")));
+ (SharedKotlinArray<SharedRecommendationType *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedRecommendationType *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RecommendationType.Companion")))
@interface SharedRecommendationTypeCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedRecommendationTypeCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializerTypeParamsSerializers:(SharedKotlinArray<id<SharedKotlinx_serialization_coreKSerializer>> *)typeParamsSerializers __attribute__((swift_name("serializer(typeParamsSerializers:)")));
@end


/**
 * Room assignment within an accommodation
 *
 * Assigns participants to specific rooms within an accommodation.
 * Helps organize who sleeps where and calculate per-person costs.
 *
 * @property id Unique identifier
 * @property accommodationId Accommodation this room belongs to
 * @property roomNumber Room number or identifier (e.g., "101", "Room A")
 * @property capacity Maximum occupancy of this room
 * @property assignedParticipants List of participant IDs assigned to this room
 * @property priceShare Cost per person for this room (in cents)
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RoomAssignment")))
@interface SharedRoomAssignment : SharedBase
- (instancetype)initWithId:(NSString *)id accommodationId:(NSString *)accommodationId roomNumber:(NSString *)roomNumber capacity:(int32_t)capacity assignedParticipants:(NSArray<NSString *> *)assignedParticipants priceShare:(int64_t)priceShare createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("init(id:accommodationId:roomNumber:capacity:assignedParticipants:priceShare:createdAt:updatedAt:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedRoomAssignmentCompanion *companion __attribute__((swift_name("companion")));
- (SharedRoomAssignment *)doCopyId:(NSString *)id accommodationId:(NSString *)accommodationId roomNumber:(NSString *)roomNumber capacity:(int32_t)capacity assignedParticipants:(NSArray<NSString *> *)assignedParticipants priceShare:(int64_t)priceShare createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("doCopy(id:accommodationId:roomNumber:capacity:assignedParticipants:priceShare:createdAt:updatedAt:)")));

/**
 * Room assignment within an accommodation
 *
 * Assigns participants to specific rooms within an accommodation.
 * Helps organize who sleeps where and calculate per-person costs.
 *
 * @property id Unique identifier
 * @property accommodationId Accommodation this room belongs to
 * @property roomNumber Room number or identifier (e.g., "101", "Room A")
 * @property capacity Maximum occupancy of this room
 * @property assignedParticipants List of participant IDs assigned to this room
 * @property priceShare Cost per person for this room (in cents)
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Room assignment within an accommodation
 *
 * Assigns participants to specific rooms within an accommodation.
 * Helps organize who sleeps where and calculate per-person costs.
 *
 * @property id Unique identifier
 * @property accommodationId Accommodation this room belongs to
 * @property roomNumber Room number or identifier (e.g., "101", "Room A")
 * @property capacity Maximum occupancy of this room
 * @property assignedParticipants List of participant IDs assigned to this room
 * @property priceShare Cost per person for this room (in cents)
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Room assignment within an accommodation
 *
 * Assigns participants to specific rooms within an accommodation.
 * Helps organize who sleeps where and calculate per-person costs.
 *
 * @property id Unique identifier
 * @property accommodationId Accommodation this room belongs to
 * @property roomNumber Room number or identifier (e.g., "101", "Room A")
 * @property capacity Maximum occupancy of this room
 * @property assignedParticipants List of participant IDs assigned to this room
 * @property priceShare Cost per person for this room (in cents)
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *accommodationId __attribute__((swift_name("accommodationId")));
@property (readonly) NSArray<NSString *> *assignedParticipants __attribute__((swift_name("assignedParticipants")));
@property (readonly) int32_t capacity __attribute__((swift_name("capacity")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) int64_t priceShare __attribute__((swift_name("priceShare")));
@property (readonly) NSString *roomNumber __attribute__((swift_name("roomNumber")));
@property (readonly) NSString *updatedAt __attribute__((swift_name("updatedAt")));
@end


/**
 * Room assignment within an accommodation
 *
 * Assigns participants to specific rooms within an accommodation.
 * Helps organize who sleeps where and calculate per-person costs.
 *
 * @property id Unique identifier
 * @property accommodationId Accommodation this room belongs to
 * @property roomNumber Room number or identifier (e.g., "101", "Room A")
 * @property capacity Maximum occupancy of this room
 * @property assignedParticipants List of participant IDs assigned to this room
 * @property priceShare Cost per person for this room (in cents)
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RoomAssignment.Companion")))
@interface SharedRoomAssignmentCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Room assignment within an accommodation
 *
 * Assigns participants to specific rooms within an accommodation.
 * Helps organize who sleeps where and calculate per-person costs.
 *
 * @property id Unique identifier
 * @property accommodationId Accommodation this room belongs to
 * @property roomNumber Room number or identifier (e.g., "101", "Room A")
 * @property capacity Maximum occupancy of this room
 * @property assignedParticipants List of participant IDs assigned to this room
 * @property priceShare Cost per person for this room (in cents)
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedRoomAssignmentCompanion *shared __attribute__((swift_name("shared")));

/**
 * Room assignment within an accommodation
 *
 * Assigns participants to specific rooms within an accommodation.
 * Helps organize who sleeps where and calculate per-person costs.
 *
 * @property id Unique identifier
 * @property accommodationId Accommodation this room belongs to
 * @property roomNumber Room number or identifier (e.g., "101", "Room A")
 * @property capacity Maximum occupancy of this room
 * @property assignedParticipants List of participant IDs assigned to this room
 * @property priceShare Cost per person for this room (in cents)
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Request to create or update a room assignment
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RoomAssignmentRequest")))
@interface SharedRoomAssignmentRequest : SharedBase
- (instancetype)initWithAccommodationId:(NSString *)accommodationId roomNumber:(NSString *)roomNumber capacity:(int32_t)capacity assignedParticipants:(NSArray<NSString *> *)assignedParticipants __attribute__((swift_name("init(accommodationId:roomNumber:capacity:assignedParticipants:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedRoomAssignmentRequestCompanion *companion __attribute__((swift_name("companion")));
- (SharedRoomAssignmentRequest *)doCopyAccommodationId:(NSString *)accommodationId roomNumber:(NSString *)roomNumber capacity:(int32_t)capacity assignedParticipants:(NSArray<NSString *> *)assignedParticipants __attribute__((swift_name("doCopy(accommodationId:roomNumber:capacity:assignedParticipants:)")));

/**
 * Request to create or update a room assignment
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Request to create or update a room assignment
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Request to create or update a room assignment
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *accommodationId __attribute__((swift_name("accommodationId")));
@property (readonly) NSArray<NSString *> *assignedParticipants __attribute__((swift_name("assignedParticipants")));
@property (readonly) int32_t capacity __attribute__((swift_name("capacity")));
@property (readonly) NSString *roomNumber __attribute__((swift_name("roomNumber")));
@end


/**
 * Request to create or update a room assignment
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RoomAssignmentRequest.Companion")))
@interface SharedRoomAssignmentRequestCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Request to create or update a room assignment
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedRoomAssignmentRequestCompanion *shared __attribute__((swift_name("shared")));

/**
 * Request to create or update a room assignment
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Route")))
@interface SharedRoute : SharedBase
- (instancetype)initWithId:(NSString *)id segments:(NSArray<SharedTransportOption *> *)segments totalDurationMinutes:(int32_t)totalDurationMinutes totalCost:(double)totalCost currency:(NSString *)currency score:(double)score __attribute__((swift_name("init(id:segments:totalDurationMinutes:totalCost:currency:score:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedRouteCompanion *companion __attribute__((swift_name("companion")));
- (SharedRoute *)doCopyId:(NSString *)id segments:(NSArray<SharedTransportOption *> *)segments totalDurationMinutes:(int32_t)totalDurationMinutes totalCost:(double)totalCost currency:(NSString *)currency score:(double)score __attribute__((swift_name("doCopy(id:segments:totalDurationMinutes:totalCost:currency:score:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *currency __attribute__((swift_name("currency")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) double score __attribute__((swift_name("score")));
@property (readonly) NSArray<SharedTransportOption *> *segments __attribute__((swift_name("segments")));
@property (readonly) double totalCost __attribute__((swift_name("totalCost")));
@property (readonly) int32_t totalDurationMinutes __attribute__((swift_name("totalDurationMinutes")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Route.Companion")))
@interface SharedRouteCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedRouteCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Represents a planning scenario for an event.
 * A scenario combines date, location, duration and budget estimates
 * to provide different options for participants to vote on.
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Scenario_")))
@interface SharedScenario_ : SharedBase
- (instancetype)initWithId:(NSString *)id eventId:(NSString *)eventId name:(NSString *)name dateOrPeriod:(NSString *)dateOrPeriod location:(NSString *)location duration:(int32_t)duration estimatedParticipants:(int32_t)estimatedParticipants estimatedBudgetPerPerson:(double)estimatedBudgetPerPerson description:(NSString *)description status:(SharedScenarioStatus *)status createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("init(id:eventId:name:dateOrPeriod:location:duration:estimatedParticipants:estimatedBudgetPerPerson:description:status:createdAt:updatedAt:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedScenario_Companion *companion __attribute__((swift_name("companion")));
- (SharedScenario_ *)doCopyId:(NSString *)id eventId:(NSString *)eventId name:(NSString *)name dateOrPeriod:(NSString *)dateOrPeriod location:(NSString *)location duration:(int32_t)duration estimatedParticipants:(int32_t)estimatedParticipants estimatedBudgetPerPerson:(double)estimatedBudgetPerPerson description:(NSString *)description status:(SharedScenarioStatus *)status createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("doCopy(id:eventId:name:dateOrPeriod:location:duration:estimatedParticipants:estimatedBudgetPerPerson:description:status:createdAt:updatedAt:)")));

/**
 * Represents a planning scenario for an event.
 * A scenario combines date, location, duration and budget estimates
 * to provide different options for participants to vote on.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Represents a planning scenario for an event.
 * A scenario combines date, location, duration and budget estimates
 * to provide different options for participants to vote on.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Represents a planning scenario for an event.
 * A scenario combines date, location, duration and budget estimates
 * to provide different options for participants to vote on.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *dateOrPeriod __attribute__((swift_name("dateOrPeriod")));
@property (readonly) NSString *description_ __attribute__((swift_name("description_")));
@property (readonly) int32_t duration __attribute__((swift_name("duration")));
@property (readonly) double estimatedBudgetPerPerson __attribute__((swift_name("estimatedBudgetPerPerson")));
@property (readonly) int32_t estimatedParticipants __attribute__((swift_name("estimatedParticipants")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *location __attribute__((swift_name("location")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) SharedScenarioStatus *status __attribute__((swift_name("status")));
@property (readonly) NSString *updatedAt __attribute__((swift_name("updatedAt")));
@end


/**
 * Represents a planning scenario for an event.
 * A scenario combines date, location, duration and budget estimates
 * to provide different options for participants to vote on.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Scenario_.Companion")))
@interface SharedScenario_Companion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Represents a planning scenario for an event.
 * A scenario combines date, location, duration and budget estimates
 * to provide different options for participants to vote on.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedScenario_Companion *shared __attribute__((swift_name("shared")));

/**
 * Represents a planning scenario for an event.
 * A scenario combines date, location, duration and budget estimates
 * to provide different options for participants to vote on.
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScenarioResponse")))
@interface SharedScenarioResponse : SharedBase
- (instancetype)initWithId:(NSString *)id eventId:(NSString *)eventId name:(NSString *)name dateOrPeriod:(NSString *)dateOrPeriod location:(NSString *)location duration:(int32_t)duration estimatedParticipants:(int32_t)estimatedParticipants estimatedBudgetPerPerson:(double)estimatedBudgetPerPerson description:(NSString *)description status:(NSString *)status createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("init(id:eventId:name:dateOrPeriod:location:duration:estimatedParticipants:estimatedBudgetPerPerson:description:status:createdAt:updatedAt:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedScenarioResponseCompanion *companion __attribute__((swift_name("companion")));
- (SharedScenarioResponse *)doCopyId:(NSString *)id eventId:(NSString *)eventId name:(NSString *)name dateOrPeriod:(NSString *)dateOrPeriod location:(NSString *)location duration:(int32_t)duration estimatedParticipants:(int32_t)estimatedParticipants estimatedBudgetPerPerson:(double)estimatedBudgetPerPerson description:(NSString *)description status:(NSString *)status createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("doCopy(id:eventId:name:dateOrPeriod:location:duration:estimatedParticipants:estimatedBudgetPerPerson:description:status:createdAt:updatedAt:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *dateOrPeriod __attribute__((swift_name("dateOrPeriod")));
@property (readonly) NSString *description_ __attribute__((swift_name("description_")));
@property (readonly) int32_t duration __attribute__((swift_name("duration")));
@property (readonly) double estimatedBudgetPerPerson __attribute__((swift_name("estimatedBudgetPerPerson")));
@property (readonly) int32_t estimatedParticipants __attribute__((swift_name("estimatedParticipants")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *location __attribute__((swift_name("location")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) NSString *status __attribute__((swift_name("status")));
@property (readonly) NSString *updatedAt __attribute__((swift_name("updatedAt")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScenarioResponse.Companion")))
@interface SharedScenarioResponseCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedScenarioResponseCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Status of a scenario in the voting process.
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScenarioStatus")))
@interface SharedScenarioStatus : SharedKotlinEnum<SharedScenarioStatus *>
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Status of a scenario in the voting process.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedScenarioStatusCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) SharedScenarioStatus *proposed __attribute__((swift_name("proposed")));
@property (class, readonly) SharedScenarioStatus *selected __attribute__((swift_name("selected")));
@property (class, readonly) SharedScenarioStatus *rejected __attribute__((swift_name("rejected")));
+ (SharedKotlinArray<SharedScenarioStatus *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedScenarioStatus *> *entries __attribute__((swift_name("entries")));
@end


/**
 * Status of a scenario in the voting process.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScenarioStatus.Companion")))
@interface SharedScenarioStatusCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Status of a scenario in the voting process.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedScenarioStatusCompanion *shared __attribute__((swift_name("shared")));

/**
 * Status of a scenario in the voting process.
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));

/**
 * Status of a scenario in the voting process.
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializerTypeParamsSerializers:(SharedKotlinArray<id<SharedKotlinx_serialization_coreKSerializer>> *)typeParamsSerializers __attribute__((swift_name("serializer(typeParamsSerializers:)")));
@end


/**
 * Represents a participant's vote on a scenario.
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScenarioVote")))
@interface SharedScenarioVote : SharedBase
- (instancetype)initWithId:(NSString *)id scenarioId:(NSString *)scenarioId participantId:(NSString *)participantId vote:(SharedScenarioVoteType *)vote createdAt:(NSString *)createdAt __attribute__((swift_name("init(id:scenarioId:participantId:vote:createdAt:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedScenarioVoteCompanion *companion __attribute__((swift_name("companion")));
- (SharedScenarioVote *)doCopyId:(NSString *)id scenarioId:(NSString *)scenarioId participantId:(NSString *)participantId vote:(SharedScenarioVoteType *)vote createdAt:(NSString *)createdAt __attribute__((swift_name("doCopy(id:scenarioId:participantId:vote:createdAt:)")));

/**
 * Represents a participant's vote on a scenario.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Represents a participant's vote on a scenario.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Represents a participant's vote on a scenario.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *participantId __attribute__((swift_name("participantId")));
@property (readonly) NSString *scenarioId __attribute__((swift_name("scenarioId")));
@property (readonly) SharedScenarioVoteType *vote __attribute__((swift_name("vote")));
@end


/**
 * Represents a participant's vote on a scenario.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScenarioVote.Companion")))
@interface SharedScenarioVoteCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Represents a participant's vote on a scenario.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedScenarioVoteCompanion *shared __attribute__((swift_name("shared")));

/**
 * Represents a participant's vote on a scenario.
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScenarioVoteRequest")))
@interface SharedScenarioVoteRequest : SharedBase
- (instancetype)initWithParticipantId:(NSString *)participantId vote:(NSString *)vote __attribute__((swift_name("init(participantId:vote:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedScenarioVoteRequestCompanion *companion __attribute__((swift_name("companion")));
- (SharedScenarioVoteRequest *)doCopyParticipantId:(NSString *)participantId vote:(NSString *)vote __attribute__((swift_name("doCopy(participantId:vote:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *participantId __attribute__((swift_name("participantId")));
@property (readonly) NSString *vote __attribute__((swift_name("vote")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScenarioVoteRequest.Companion")))
@interface SharedScenarioVoteRequestCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedScenarioVoteRequestCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScenarioVoteResponse")))
@interface SharedScenarioVoteResponse : SharedBase
- (instancetype)initWithId:(NSString *)id scenarioId:(NSString *)scenarioId participantId:(NSString *)participantId vote:(NSString *)vote createdAt:(NSString *)createdAt __attribute__((swift_name("init(id:scenarioId:participantId:vote:createdAt:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedScenarioVoteResponseCompanion *companion __attribute__((swift_name("companion")));
- (SharedScenarioVoteResponse *)doCopyId:(NSString *)id scenarioId:(NSString *)scenarioId participantId:(NSString *)participantId vote:(NSString *)vote createdAt:(NSString *)createdAt __attribute__((swift_name("doCopy(id:scenarioId:participantId:vote:createdAt:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *participantId __attribute__((swift_name("participantId")));
@property (readonly) NSString *scenarioId __attribute__((swift_name("scenarioId")));
@property (readonly) NSString *vote __attribute__((swift_name("vote")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScenarioVoteResponse.Companion")))
@interface SharedScenarioVoteResponseCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedScenarioVoteResponseCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Type of vote for a scenario.
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScenarioVoteType")))
@interface SharedScenarioVoteType : SharedKotlinEnum<SharedScenarioVoteType *>
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Type of vote for a scenario.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedScenarioVoteTypeCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) SharedScenarioVoteType *prefer __attribute__((swift_name("prefer")));
@property (class, readonly) SharedScenarioVoteType *neutral __attribute__((swift_name("neutral")));
@property (class, readonly) SharedScenarioVoteType *against __attribute__((swift_name("against")));
+ (SharedKotlinArray<SharedScenarioVoteType *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedScenarioVoteType *> *entries __attribute__((swift_name("entries")));
@end


/**
 * Type of vote for a scenario.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScenarioVoteType.Companion")))
@interface SharedScenarioVoteTypeCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Type of vote for a scenario.
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedScenarioVoteTypeCompanion *shared __attribute__((swift_name("shared")));

/**
 * Type of vote for a scenario.
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));

/**
 * Type of vote for a scenario.
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializerTypeParamsSerializers:(SharedKotlinArray<id<SharedKotlinx_serialization_coreKSerializer>> *)typeParamsSerializers __attribute__((swift_name("serializer(typeParamsSerializers:)")));
@end


/**
 * Aggregated voting results for a scenario.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScenarioVotingResult")))
@interface SharedScenarioVotingResult : SharedBase
- (instancetype)initWithScenarioId:(NSString *)scenarioId preferCount:(int32_t)preferCount neutralCount:(int32_t)neutralCount againstCount:(int32_t)againstCount totalVotes:(int32_t)totalVotes score:(int32_t)score __attribute__((swift_name("init(scenarioId:preferCount:neutralCount:againstCount:totalVotes:score:)"))) __attribute__((objc_designated_initializer));
- (SharedScenarioVotingResult *)doCopyScenarioId:(NSString *)scenarioId preferCount:(int32_t)preferCount neutralCount:(int32_t)neutralCount againstCount:(int32_t)againstCount totalVotes:(int32_t)totalVotes score:(int32_t)score __attribute__((swift_name("doCopy(scenarioId:preferCount:neutralCount:againstCount:totalVotes:score:)")));

/**
 * Aggregated voting results for a scenario.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Aggregated voting results for a scenario.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Aggregated voting results for a scenario.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t againstCount __attribute__((swift_name("againstCount")));
@property (readonly) double againstPercentage __attribute__((swift_name("againstPercentage")));
@property (readonly) int32_t neutralCount __attribute__((swift_name("neutralCount")));
@property (readonly) double neutralPercentage __attribute__((swift_name("neutralPercentage")));
@property (readonly) int32_t preferCount __attribute__((swift_name("preferCount")));
@property (readonly) double preferPercentage __attribute__((swift_name("preferPercentage")));
@property (readonly) NSString *scenarioId __attribute__((swift_name("scenarioId")));
@property (readonly) int32_t score __attribute__((swift_name("score")));
@property (readonly) int32_t totalVotes __attribute__((swift_name("totalVotes")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScenarioVotingResultResponse")))
@interface SharedScenarioVotingResultResponse : SharedBase
- (instancetype)initWithScenarioId:(NSString *)scenarioId preferCount:(int32_t)preferCount neutralCount:(int32_t)neutralCount againstCount:(int32_t)againstCount totalVotes:(int32_t)totalVotes score:(int32_t)score preferPercentage:(double)preferPercentage neutralPercentage:(double)neutralPercentage againstPercentage:(double)againstPercentage __attribute__((swift_name("init(scenarioId:preferCount:neutralCount:againstCount:totalVotes:score:preferPercentage:neutralPercentage:againstPercentage:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedScenarioVotingResultResponseCompanion *companion __attribute__((swift_name("companion")));
- (SharedScenarioVotingResultResponse *)doCopyScenarioId:(NSString *)scenarioId preferCount:(int32_t)preferCount neutralCount:(int32_t)neutralCount againstCount:(int32_t)againstCount totalVotes:(int32_t)totalVotes score:(int32_t)score preferPercentage:(double)preferPercentage neutralPercentage:(double)neutralPercentage againstPercentage:(double)againstPercentage __attribute__((swift_name("doCopy(scenarioId:preferCount:neutralCount:againstCount:totalVotes:score:preferPercentage:neutralPercentage:againstPercentage:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t againstCount __attribute__((swift_name("againstCount")));
@property (readonly) double againstPercentage __attribute__((swift_name("againstPercentage")));
@property (readonly) int32_t neutralCount __attribute__((swift_name("neutralCount")));
@property (readonly) double neutralPercentage __attribute__((swift_name("neutralPercentage")));
@property (readonly) int32_t preferCount __attribute__((swift_name("preferCount")));
@property (readonly) double preferPercentage __attribute__((swift_name("preferPercentage")));
@property (readonly) NSString *scenarioId __attribute__((swift_name("scenarioId")));
@property (readonly) int32_t score __attribute__((swift_name("score")));
@property (readonly) int32_t totalVotes __attribute__((swift_name("totalVotes")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScenarioVotingResultResponse.Companion")))
@interface SharedScenarioVotingResultResponseCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedScenarioVotingResultResponseCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Scenario with its associated votes.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScenarioWithVotes")))
@interface SharedScenarioWithVotes : SharedBase
- (instancetype)initWithScenario:(SharedScenario_ *)scenario votes:(NSArray<SharedScenarioVote *> *)votes votingResult:(SharedScenarioVotingResult *)votingResult __attribute__((swift_name("init(scenario:votes:votingResult:)"))) __attribute__((objc_designated_initializer));
- (SharedScenarioWithVotes *)doCopyScenario:(SharedScenario_ *)scenario votes:(NSArray<SharedScenarioVote *> *)votes votingResult:(SharedScenarioVotingResult *)votingResult __attribute__((swift_name("doCopy(scenario:votes:votingResult:)")));

/**
 * Scenario with its associated votes.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Scenario with its associated votes.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Scenario with its associated votes.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedScenario_ *scenario __attribute__((swift_name("scenario")));
@property (readonly) NSArray<SharedScenarioVote *> *votes __attribute__((swift_name("votes")));
@property (readonly) SharedScenarioVotingResult *votingResult __attribute__((swift_name("votingResult")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScenarioWithVotesResponse")))
@interface SharedScenarioWithVotesResponse : SharedBase
- (instancetype)initWithScenario:(SharedScenarioResponse *)scenario votes:(NSArray<SharedScenarioVoteResponse *> *)votes result:(SharedScenarioVotingResultResponse *)result __attribute__((swift_name("init(scenario:votes:result:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedScenarioWithVotesResponseCompanion *companion __attribute__((swift_name("companion")));
- (SharedScenarioWithVotesResponse *)doCopyScenario:(SharedScenarioResponse *)scenario votes:(NSArray<SharedScenarioVoteResponse *> *)votes result:(SharedScenarioVotingResultResponse *)result __attribute__((swift_name("doCopy(scenario:votes:result:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedScenarioVotingResultResponse *result __attribute__((swift_name("result")));
@property (readonly) SharedScenarioResponse *scenario __attribute__((swift_name("scenario")));
@property (readonly) NSArray<SharedScenarioVoteResponse *> *votes __attribute__((swift_name("votes")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ScenarioWithVotesResponse.Companion")))
@interface SharedScenarioWithVotesResponseCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedScenarioWithVotesResponseCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Sync change record
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SyncChange")))
@interface SharedSyncChange : SharedBase
- (instancetype)initWithId:(NSString *)id table:(NSString *)table operation:(NSString *)operation recordId:(NSString *)recordId data:(NSString *)data timestamp:(NSString *)timestamp userId:(NSString *)userId __attribute__((swift_name("init(id:table:operation:recordId:data:timestamp:userId:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedSyncChangeCompanion *companion __attribute__((swift_name("companion")));
- (SharedSyncChange *)doCopyId:(NSString *)id table:(NSString *)table operation:(NSString *)operation recordId:(NSString *)recordId data:(NSString *)data timestamp:(NSString *)timestamp userId:(NSString *)userId __attribute__((swift_name("doCopy(id:table:operation:recordId:data:timestamp:userId:)")));

/**
 * Sync change record
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Sync change record
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Sync change record
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *data __attribute__((swift_name("data")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *operation __attribute__((swift_name("operation")));
@property (readonly) NSString *recordId __attribute__((swift_name("recordId")));
@property (readonly) NSString *table __attribute__((swift_name("table")));
@property (readonly) NSString *timestamp __attribute__((swift_name("timestamp")));
@property (readonly) NSString *userId __attribute__((swift_name("userId")));
@end


/**
 * Sync change record
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SyncChange.Companion")))
@interface SharedSyncChangeCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Sync change record
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSyncChangeCompanion *shared __attribute__((swift_name("shared")));

/**
 * Sync change record
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Sync conflict information
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SyncConflict")))
@interface SharedSyncConflict : SharedBase
- (instancetype)initWithChangeId:(NSString *)changeId table:(NSString *)table recordId:(NSString *)recordId clientData:(NSString *)clientData serverData:(NSString *)serverData resolution:(NSString *)resolution __attribute__((swift_name("init(changeId:table:recordId:clientData:serverData:resolution:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedSyncConflictCompanion *companion __attribute__((swift_name("companion")));
- (SharedSyncConflict *)doCopyChangeId:(NSString *)changeId table:(NSString *)table recordId:(NSString *)recordId clientData:(NSString *)clientData serverData:(NSString *)serverData resolution:(NSString *)resolution __attribute__((swift_name("doCopy(changeId:table:recordId:clientData:serverData:resolution:)")));

/**
 * Sync conflict information
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Sync conflict information
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Sync conflict information
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *changeId __attribute__((swift_name("changeId")));
@property (readonly) NSString *clientData __attribute__((swift_name("clientData")));
@property (readonly) NSString *recordId __attribute__((swift_name("recordId")));
@property (readonly) NSString *resolution __attribute__((swift_name("resolution")));
@property (readonly) NSString *serverData __attribute__((swift_name("serverData")));
@property (readonly) NSString *table __attribute__((swift_name("table")));
@end


/**
 * Sync conflict information
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SyncConflict.Companion")))
@interface SharedSyncConflictCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Sync conflict information
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSyncConflictCompanion *shared __attribute__((swift_name("shared")));

/**
 * Sync conflict information
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Data structures for sync operations
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SyncEventData")))
@interface SharedSyncEventData : SharedBase
- (instancetype)initWithId:(NSString *)id title:(NSString *)title description:(NSString *)description organizerId:(NSString *)organizerId deadline:(NSString *)deadline timezone:(NSString *)timezone __attribute__((swift_name("init(id:title:description:organizerId:deadline:timezone:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedSyncEventDataCompanion *companion __attribute__((swift_name("companion")));
- (SharedSyncEventData *)doCopyId:(NSString *)id title:(NSString *)title description:(NSString *)description organizerId:(NSString *)organizerId deadline:(NSString *)deadline timezone:(NSString *)timezone __attribute__((swift_name("doCopy(id:title:description:organizerId:deadline:timezone:)")));

/**
 * Data structures for sync operations
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Data structures for sync operations
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Data structures for sync operations
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *deadline __attribute__((swift_name("deadline")));
@property (readonly) NSString *description_ __attribute__((swift_name("description_")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *organizerId __attribute__((swift_name("organizerId")));
@property (readonly) NSString *timezone __attribute__((swift_name("timezone")));
@property (readonly) NSString *title __attribute__((swift_name("title")));
@end


/**
 * Data structures for sync operations
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SyncEventData.Companion")))
@interface SharedSyncEventDataCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Data structures for sync operations
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSyncEventDataCompanion *shared __attribute__((swift_name("shared")));

/**
 * Data structures for sync operations
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Sync metadata for tracking changes
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SyncMetadata_")))
@interface SharedSyncMetadata_ : SharedBase
- (instancetype)initWithId:(NSString *)id tableName:(NSString *)tableName recordId:(NSString *)recordId operation:(SharedSyncOperation *)operation timestamp:(NSString *)timestamp userId:(NSString *)userId synced:(BOOL)synced retryCount:(int32_t)retryCount lastError:(NSString * _Nullable)lastError __attribute__((swift_name("init(id:tableName:recordId:operation:timestamp:userId:synced:retryCount:lastError:)"))) __attribute__((objc_designated_initializer));
- (SharedSyncMetadata_ *)doCopyId:(NSString *)id tableName:(NSString *)tableName recordId:(NSString *)recordId operation:(SharedSyncOperation *)operation timestamp:(NSString *)timestamp userId:(NSString *)userId synced:(BOOL)synced retryCount:(int32_t)retryCount lastError:(NSString * _Nullable)lastError __attribute__((swift_name("doCopy(id:tableName:recordId:operation:timestamp:userId:synced:retryCount:lastError:)")));

/**
 * Sync metadata for tracking changes
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Sync metadata for tracking changes
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Sync metadata for tracking changes
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString * _Nullable lastError __attribute__((swift_name("lastError")));
@property (readonly) SharedSyncOperation *operation __attribute__((swift_name("operation")));
@property (readonly) NSString *recordId __attribute__((swift_name("recordId")));
@property (readonly) int32_t retryCount __attribute__((swift_name("retryCount")));
@property (readonly) BOOL synced __attribute__((swift_name("synced")));
@property (readonly) NSString *tableName __attribute__((swift_name("tableName")));
@property (readonly) NSString *timestamp __attribute__((swift_name("timestamp")));
@property (readonly) NSString *userId __attribute__((swift_name("userId")));
@end


/**
 * Sync operation types
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SyncOperation")))
@interface SharedSyncOperation : SharedKotlinEnum<SharedSyncOperation *>
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Sync operation types
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedSyncOperation *create __attribute__((swift_name("create")));
@property (class, readonly) SharedSyncOperation *update __attribute__((swift_name("update")));
@property (class, readonly) SharedSyncOperation *delete_ __attribute__((swift_name("delete_")));
+ (SharedKotlinArray<SharedSyncOperation *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedSyncOperation *> *entries __attribute__((swift_name("entries")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SyncParticipantData")))
@interface SharedSyncParticipantData : SharedBase
- (instancetype)initWithEventId:(NSString *)eventId userId:(NSString *)userId __attribute__((swift_name("init(eventId:userId:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedSyncParticipantDataCompanion *companion __attribute__((swift_name("companion")));
- (SharedSyncParticipantData *)doCopyEventId:(NSString *)eventId userId:(NSString *)userId __attribute__((swift_name("doCopy(eventId:userId:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *userId __attribute__((swift_name("userId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SyncParticipantData.Companion")))
@interface SharedSyncParticipantDataCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSyncParticipantDataCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Sync request payload
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SyncRequest")))
@interface SharedSyncRequest : SharedBase
- (instancetype)initWithChanges:(NSArray<SharedSyncChange *> *)changes lastSyncTimestamp:(NSString * _Nullable)lastSyncTimestamp __attribute__((swift_name("init(changes:lastSyncTimestamp:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedSyncRequestCompanion *companion __attribute__((swift_name("companion")));
- (SharedSyncRequest *)doCopyChanges:(NSArray<SharedSyncChange *> *)changes lastSyncTimestamp:(NSString * _Nullable)lastSyncTimestamp __attribute__((swift_name("doCopy(changes:lastSyncTimestamp:)")));

/**
 * Sync request payload
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Sync request payload
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Sync request payload
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSArray<SharedSyncChange *> *changes __attribute__((swift_name("changes")));
@property (readonly) NSString * _Nullable lastSyncTimestamp __attribute__((swift_name("lastSyncTimestamp")));
@end


/**
 * Sync request payload
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SyncRequest.Companion")))
@interface SharedSyncRequestCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Sync request payload
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSyncRequestCompanion *shared __attribute__((swift_name("shared")));

/**
 * Sync request payload
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Sync response payload
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SyncResponse")))
@interface SharedSyncResponse : SharedBase
- (instancetype)initWithSuccess:(BOOL)success appliedChanges:(int32_t)appliedChanges conflicts:(NSArray<SharedSyncConflict *> *)conflicts serverTimestamp:(NSString *)serverTimestamp message:(NSString * _Nullable)message __attribute__((swift_name("init(success:appliedChanges:conflicts:serverTimestamp:message:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedSyncResponseCompanion *companion __attribute__((swift_name("companion")));
- (SharedSyncResponse *)doCopySuccess:(BOOL)success appliedChanges:(int32_t)appliedChanges conflicts:(NSArray<SharedSyncConflict *> *)conflicts serverTimestamp:(NSString *)serverTimestamp message:(NSString * _Nullable)message __attribute__((swift_name("doCopy(success:appliedChanges:conflicts:serverTimestamp:message:)")));

/**
 * Sync response payload
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Sync response payload
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Sync response payload
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t appliedChanges __attribute__((swift_name("appliedChanges")));
@property (readonly) NSArray<SharedSyncConflict *> *conflicts __attribute__((swift_name("conflicts")));
@property (readonly) NSString * _Nullable message __attribute__((swift_name("message")));
@property (readonly) NSString *serverTimestamp __attribute__((swift_name("serverTimestamp")));
@property (readonly) BOOL success __attribute__((swift_name("success")));
@end


/**
 * Sync response payload
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SyncResponse.Companion")))
@interface SharedSyncResponseCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Sync response payload
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSyncResponseCompanion *shared __attribute__((swift_name("shared")));

/**
 * Sync response payload
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SyncVoteData")))
@interface SharedSyncVoteData : SharedBase
- (instancetype)initWithEventId:(NSString *)eventId participantId:(NSString *)participantId slotId:(NSString *)slotId preference:(NSString *)preference __attribute__((swift_name("init(eventId:participantId:slotId:preference:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedSyncVoteDataCompanion *companion __attribute__((swift_name("companion")));
- (SharedSyncVoteData *)doCopyEventId:(NSString *)eventId participantId:(NSString *)participantId slotId:(NSString *)slotId preference:(NSString *)preference __attribute__((swift_name("doCopy(eventId:participantId:slotId:preference:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString *participantId __attribute__((swift_name("participantId")));
@property (readonly) NSString *preference __attribute__((swift_name("preference")));
@property (readonly) NSString *slotId __attribute__((swift_name("slotId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SyncVoteData.Companion")))
@interface SharedSyncVoteDataCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSyncVoteDataCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TimeSlot")))
@interface SharedTimeSlot : SharedBase
- (instancetype)initWithId:(NSString *)id start:(NSString *)start end:(NSString *)end timezone:(NSString *)timezone __attribute__((swift_name("init(id:start:end:timezone:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedTimeSlotCompanion *companion __attribute__((swift_name("companion")));
- (SharedTimeSlot *)doCopyId:(NSString *)id start:(NSString *)start end:(NSString *)end timezone:(NSString *)timezone __attribute__((swift_name("doCopy(id:start:end:timezone:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *end __attribute__((swift_name("end")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *start __attribute__((swift_name("start")));
@property (readonly) NSString *timezone __attribute__((swift_name("timezone")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TimeSlot.Companion")))
@interface SharedTimeSlotCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedTimeSlotCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TimeSlotResponse")))
@interface SharedTimeSlotResponse : SharedBase
- (instancetype)initWithId:(NSString *)id start:(NSString *)start end:(NSString *)end timezone:(NSString *)timezone __attribute__((swift_name("init(id:start:end:timezone:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedTimeSlotResponseCompanion *companion __attribute__((swift_name("companion")));
- (SharedTimeSlotResponse *)doCopyId:(NSString *)id start:(NSString *)start end:(NSString *)end timezone:(NSString *)timezone __attribute__((swift_name("doCopy(id:start:end:timezone:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *end __attribute__((swift_name("end")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *start __attribute__((swift_name("start")));
@property (readonly) NSString *timezone __attribute__((swift_name("timezone")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TimeSlotResponse.Companion")))
@interface SharedTimeSlotResponseCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedTimeSlotResponseCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Token refresh request
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TokenRefreshRequest")))
@interface SharedTokenRefreshRequest : SharedBase
- (instancetype)initWithRefreshToken:(NSString *)refreshToken __attribute__((swift_name("init(refreshToken:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedTokenRefreshRequestCompanion *companion __attribute__((swift_name("companion")));
- (SharedTokenRefreshRequest *)doCopyRefreshToken:(NSString *)refreshToken __attribute__((swift_name("doCopy(refreshToken:)")));

/**
 * Token refresh request
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Token refresh request
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Token refresh request
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *refreshToken __attribute__((swift_name("refreshToken")));
@end


/**
 * Token refresh request
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TokenRefreshRequest.Companion")))
@interface SharedTokenRefreshRequestCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Token refresh request
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedTokenRefreshRequestCompanion *shared __attribute__((swift_name("shared")));

/**
 * Token refresh request
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Token refresh response
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TokenRefreshResponse")))
@interface SharedTokenRefreshResponse : SharedBase
- (instancetype)initWithAccessToken:(NSString *)accessToken tokenType:(NSString *)tokenType expiresIn:(int64_t)expiresIn scope:(NSString * _Nullable)scope __attribute__((swift_name("init(accessToken:tokenType:expiresIn:scope:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedTokenRefreshResponseCompanion *companion __attribute__((swift_name("companion")));
- (SharedTokenRefreshResponse *)doCopyAccessToken:(NSString *)accessToken tokenType:(NSString *)tokenType expiresIn:(int64_t)expiresIn scope:(NSString * _Nullable)scope __attribute__((swift_name("doCopy(accessToken:tokenType:expiresIn:scope:)")));

/**
 * Token refresh response
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Token refresh response
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Token refresh response
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *accessToken __attribute__((swift_name("accessToken")));
@property (readonly) int64_t expiresIn __attribute__((swift_name("expiresIn")));
@property (readonly) NSString * _Nullable scope __attribute__((swift_name("scope")));
@property (readonly) NSString *tokenType __attribute__((swift_name("tokenType")));
@end


/**
 * Token refresh response
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TokenRefreshResponse.Companion")))
@interface SharedTokenRefreshResponseCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Token refresh response
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedTokenRefreshResponseCompanion *shared __attribute__((swift_name("shared")));

/**
 * Token refresh response
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TransportMode")))
@interface SharedTransportMode : SharedKotlinEnum<SharedTransportMode *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedTransportModeCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) SharedTransportMode *flight __attribute__((swift_name("flight")));
@property (class, readonly) SharedTransportMode *train __attribute__((swift_name("train")));
@property (class, readonly) SharedTransportMode *bus __attribute__((swift_name("bus")));
@property (class, readonly) SharedTransportMode *car __attribute__((swift_name("car")));
@property (class, readonly) SharedTransportMode *rideshare __attribute__((swift_name("rideshare")));
@property (class, readonly) SharedTransportMode *taxi __attribute__((swift_name("taxi")));
@property (class, readonly) SharedTransportMode *walking __attribute__((swift_name("walking")));
+ (SharedKotlinArray<SharedTransportMode *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedTransportMode *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TransportMode.Companion")))
@interface SharedTransportModeCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedTransportModeCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializerTypeParamsSerializers:(SharedKotlinArray<id<SharedKotlinx_serialization_coreKSerializer>> *)typeParamsSerializers __attribute__((swift_name("serializer(typeParamsSerializers:)")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TransportOption")))
@interface SharedTransportOption : SharedBase
- (instancetype)initWithId:(NSString *)id mode:(SharedTransportMode *)mode provider:(NSString *)provider departure:(SharedLocation *)departure arrival:(SharedLocation *)arrival departureTime:(NSString *)departureTime arrivalTime:(NSString *)arrivalTime durationMinutes:(int32_t)durationMinutes cost:(double)cost currency:(NSString *)currency stops:(NSArray<SharedLocation *> *)stops bookingUrl:(NSString * _Nullable)bookingUrl __attribute__((swift_name("init(id:mode:provider:departure:arrival:departureTime:arrivalTime:durationMinutes:cost:currency:stops:bookingUrl:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedTransportOptionCompanion *companion __attribute__((swift_name("companion")));
- (SharedTransportOption *)doCopyId:(NSString *)id mode:(SharedTransportMode *)mode provider:(NSString *)provider departure:(SharedLocation *)departure arrival:(SharedLocation *)arrival departureTime:(NSString *)departureTime arrivalTime:(NSString *)arrivalTime durationMinutes:(int32_t)durationMinutes cost:(double)cost currency:(NSString *)currency stops:(NSArray<SharedLocation *> *)stops bookingUrl:(NSString * _Nullable)bookingUrl __attribute__((swift_name("doCopy(id:mode:provider:departure:arrival:departureTime:arrivalTime:durationMinutes:cost:currency:stops:bookingUrl:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedLocation *arrival __attribute__((swift_name("arrival")));
@property (readonly) NSString *arrivalTime __attribute__((swift_name("arrivalTime")));
@property (readonly) NSString * _Nullable bookingUrl __attribute__((swift_name("bookingUrl")));
@property (readonly) double cost __attribute__((swift_name("cost")));
@property (readonly) NSString *currency __attribute__((swift_name("currency")));
@property (readonly) SharedLocation *departure __attribute__((swift_name("departure")));
@property (readonly) NSString *departureTime __attribute__((swift_name("departureTime")));
@property (readonly) int32_t durationMinutes __attribute__((swift_name("durationMinutes")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) SharedTransportMode *mode __attribute__((swift_name("mode")));
@property (readonly) NSString *provider __attribute__((swift_name("provider")));
@property (readonly) NSArray<SharedLocation *> *stops __attribute__((swift_name("stops")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TransportOption.Companion")))
@interface SharedTransportOptionCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedTransportOptionCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TransportPlan")))
@interface SharedTransportPlan : SharedBase
- (instancetype)initWithEventId:(NSString *)eventId participantRoutes:(NSDictionary<NSString *, SharedRoute *> *)participantRoutes groupArrivals:(NSArray<NSString *> *)groupArrivals totalGroupCost:(double)totalGroupCost optimizationType:(SharedOptimizationType *)optimizationType createdAt:(NSString *)createdAt __attribute__((swift_name("init(eventId:participantRoutes:groupArrivals:totalGroupCost:optimizationType:createdAt:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedTransportPlanCompanion *companion __attribute__((swift_name("companion")));
- (SharedTransportPlan *)doCopyEventId:(NSString *)eventId participantRoutes:(NSDictionary<NSString *, SharedRoute *> *)participantRoutes groupArrivals:(NSArray<NSString *> *)groupArrivals totalGroupCost:(double)totalGroupCost optimizationType:(SharedOptimizationType *)optimizationType createdAt:(NSString *)createdAt __attribute__((swift_name("doCopy(eventId:participantRoutes:groupArrivals:totalGroupCost:optimizationType:createdAt:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSArray<NSString *> *groupArrivals __attribute__((swift_name("groupArrivals")));
@property (readonly) SharedOptimizationType *optimizationType __attribute__((swift_name("optimizationType")));
@property (readonly) NSDictionary<NSString *, SharedRoute *> *participantRoutes __attribute__((swift_name("participantRoutes")));
@property (readonly) double totalGroupCost __attribute__((swift_name("totalGroupCost")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("TransportPlan.Companion")))
@interface SharedTransportPlanCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedTransportPlanCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UpdateEventStatusRequest")))
@interface SharedUpdateEventStatusRequest : SharedBase
- (instancetype)initWithEventId:(NSString *)eventId status:(NSString *)status finalDate:(NSString * _Nullable)finalDate __attribute__((swift_name("init(eventId:status:finalDate:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedUpdateEventStatusRequestCompanion *companion __attribute__((swift_name("companion")));
- (SharedUpdateEventStatusRequest *)doCopyEventId:(NSString *)eventId status:(NSString *)status finalDate:(NSString * _Nullable)finalDate __attribute__((swift_name("doCopy(eventId:status:finalDate:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *eventId __attribute__((swift_name("eventId")));
@property (readonly) NSString * _Nullable finalDate __attribute__((swift_name("finalDate")));
@property (readonly) NSString *status __attribute__((swift_name("status")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UpdateEventStatusRequest.Companion")))
@interface SharedUpdateEventStatusRequestCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUpdateEventStatusRequestCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UpdateScenarioRequest")))
@interface SharedUpdateScenarioRequest : SharedBase
- (instancetype)initWithName:(NSString * _Nullable)name dateOrPeriod:(NSString * _Nullable)dateOrPeriod location:(NSString * _Nullable)location duration:(SharedInt * _Nullable)duration estimatedParticipants:(SharedInt * _Nullable)estimatedParticipants estimatedBudgetPerPerson:(SharedDouble * _Nullable)estimatedBudgetPerPerson description:(NSString * _Nullable)description status:(NSString * _Nullable)status __attribute__((swift_name("init(name:dateOrPeriod:location:duration:estimatedParticipants:estimatedBudgetPerPerson:description:status:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedUpdateScenarioRequestCompanion *companion __attribute__((swift_name("companion")));
- (SharedUpdateScenarioRequest *)doCopyName:(NSString * _Nullable)name dateOrPeriod:(NSString * _Nullable)dateOrPeriod location:(NSString * _Nullable)location duration:(SharedInt * _Nullable)duration estimatedParticipants:(SharedInt * _Nullable)estimatedParticipants estimatedBudgetPerPerson:(SharedDouble * _Nullable)estimatedBudgetPerPerson description:(NSString * _Nullable)description status:(NSString * _Nullable)status __attribute__((swift_name("doCopy(name:dateOrPeriod:location:duration:estimatedParticipants:estimatedBudgetPerPerson:description:status:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable dateOrPeriod __attribute__((swift_name("dateOrPeriod")));
@property (readonly) NSString * _Nullable description_ __attribute__((swift_name("description_")));
@property (readonly) SharedInt * _Nullable duration __attribute__((swift_name("duration")));
@property (readonly) SharedDouble * _Nullable estimatedBudgetPerPerson __attribute__((swift_name("estimatedBudgetPerPerson")));
@property (readonly) SharedInt * _Nullable estimatedParticipants __attribute__((swift_name("estimatedParticipants")));
@property (readonly) NSString * _Nullable location __attribute__((swift_name("location")));
@property (readonly) NSString * _Nullable name __attribute__((swift_name("name")));
@property (readonly) NSString * _Nullable status __attribute__((swift_name("status")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UpdateScenarioRequest.Companion")))
@interface SharedUpdateScenarioRequestCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUpdateScenarioRequestCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * User domain model for authenticated users
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("User_")))
@interface SharedUser_ : SharedBase
- (instancetype)initWithId:(NSString *)id providerId:(NSString *)providerId email:(NSString *)email name:(NSString *)name avatarUrl:(NSString * _Nullable)avatarUrl provider:(SharedOAuthProvider *)provider role:(SharedUserRole *)role createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("init(id:providerId:email:name:avatarUrl:provider:role:createdAt:updatedAt:)"))) __attribute__((objc_designated_initializer));
- (SharedUser_ *)doCopyId:(NSString *)id providerId:(NSString *)providerId email:(NSString *)email name:(NSString *)name avatarUrl:(NSString * _Nullable)avatarUrl provider:(SharedOAuthProvider *)provider role:(SharedUserRole *)role createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("doCopy(id:providerId:email:name:avatarUrl:provider:role:createdAt:updatedAt:)")));

/**
 * User domain model for authenticated users
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * User domain model for authenticated users
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * User domain model for authenticated users
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable avatarUrl __attribute__((swift_name("avatarUrl")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *email __attribute__((swift_name("email")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) SharedOAuthProvider *provider __attribute__((swift_name("provider")));
@property (readonly) NSString *providerId __attribute__((swift_name("providerId")));
@property (readonly) SharedUserRole *role __attribute__((swift_name("role")));
@property (readonly) NSString *updatedAt __attribute__((swift_name("updatedAt")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UserPreferences")))
@interface SharedUserPreferences : SharedBase
- (instancetype)initWithUserId:(NSString *)userId preferredDaysOfWeek:(NSArray<NSString *> *)preferredDaysOfWeek preferredTimes:(NSArray<NSString *> *)preferredTimes preferredLocations:(NSArray<NSString *> *)preferredLocations preferredActivities:(NSArray<NSString *> *)preferredActivities budgetRange:(SharedBudgetRange * _Nullable)budgetRange groupSizePreference:(SharedLong * _Nullable)groupSizePreference lastUpdated:(NSString *)lastUpdated __attribute__((swift_name("init(userId:preferredDaysOfWeek:preferredTimes:preferredLocations:preferredActivities:budgetRange:groupSizePreference:lastUpdated:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedUserPreferencesCompanion *companion __attribute__((swift_name("companion")));
- (SharedUserPreferences *)doCopyUserId:(NSString *)userId preferredDaysOfWeek:(NSArray<NSString *> *)preferredDaysOfWeek preferredTimes:(NSArray<NSString *> *)preferredTimes preferredLocations:(NSArray<NSString *> *)preferredLocations preferredActivities:(NSArray<NSString *> *)preferredActivities budgetRange:(SharedBudgetRange * _Nullable)budgetRange groupSizePreference:(SharedLong * _Nullable)groupSizePreference lastUpdated:(NSString *)lastUpdated __attribute__((swift_name("doCopy(userId:preferredDaysOfWeek:preferredTimes:preferredLocations:preferredActivities:budgetRange:groupSizePreference:lastUpdated:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedBudgetRange * _Nullable budgetRange __attribute__((swift_name("budgetRange")));
@property (readonly) SharedLong * _Nullable groupSizePreference __attribute__((swift_name("groupSizePreference")));
@property (readonly) NSString *lastUpdated __attribute__((swift_name("lastUpdated")));
@property (readonly) NSArray<NSString *> *preferredActivities __attribute__((swift_name("preferredActivities")));
@property (readonly) NSArray<NSString *> *preferredDaysOfWeek __attribute__((swift_name("preferredDaysOfWeek")));
@property (readonly) NSArray<NSString *> *preferredLocations __attribute__((swift_name("preferredLocations")));
@property (readonly) NSArray<NSString *> *preferredTimes __attribute__((swift_name("preferredTimes")));
@property (readonly) NSString *userId __attribute__((swift_name("userId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UserPreferences.Companion")))
@interface SharedUserPreferencesCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUserPreferencesCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * User API response model
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UserResponse")))
@interface SharedUserResponse : SharedBase
- (instancetype)initWithId:(NSString *)id email:(NSString *)email name:(NSString *)name avatarUrl:(NSString * _Nullable)avatarUrl provider:(NSString *)provider role:(NSString *)role createdAt:(NSString *)createdAt __attribute__((swift_name("init(id:email:name:avatarUrl:provider:role:createdAt:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedUserResponseCompanion *companion __attribute__((swift_name("companion")));
- (SharedUserResponse *)doCopyId:(NSString *)id email:(NSString *)email name:(NSString *)name avatarUrl:(NSString * _Nullable)avatarUrl provider:(NSString *)provider role:(NSString *)role createdAt:(NSString *)createdAt __attribute__((swift_name("doCopy(id:email:name:avatarUrl:provider:role:createdAt:)")));

/**
 * User API response model
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * User API response model
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * User API response model
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString * _Nullable avatarUrl __attribute__((swift_name("avatarUrl")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *email __attribute__((swift_name("email")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) NSString *provider __attribute__((swift_name("provider")));
@property (readonly) NSString *role __attribute__((swift_name("role")));
@end


/**
 * User API response model
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UserResponse.Companion")))
@interface SharedUserResponseCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * User API response model
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedUserResponseCompanion *shared __attribute__((swift_name("shared")));

/**
 * User API response model
 */
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * User token domain model
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UserToken")))
@interface SharedUserToken : SharedBase
- (instancetype)initWithId:(NSString *)id userId:(NSString *)userId accessToken:(NSString *)accessToken refreshToken:(NSString * _Nullable)refreshToken tokenType:(NSString *)tokenType expiresAt:(NSString *)expiresAt scope:(NSString * _Nullable)scope createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("init(id:userId:accessToken:refreshToken:tokenType:expiresAt:scope:createdAt:updatedAt:)"))) __attribute__((objc_designated_initializer));
- (SharedUserToken *)doCopyId:(NSString *)id userId:(NSString *)userId accessToken:(NSString *)accessToken refreshToken:(NSString * _Nullable)refreshToken tokenType:(NSString *)tokenType expiresAt:(NSString *)expiresAt scope:(NSString * _Nullable)scope createdAt:(NSString *)createdAt updatedAt:(NSString *)updatedAt __attribute__((swift_name("doCopy(id:userId:accessToken:refreshToken:tokenType:expiresAt:scope:createdAt:updatedAt:)")));

/**
 * User token domain model
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * User token domain model
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * User token domain model
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *accessToken __attribute__((swift_name("accessToken")));
@property (readonly) NSString *createdAt __attribute__((swift_name("createdAt")));
@property (readonly) NSString *expiresAt __attribute__((swift_name("expiresAt")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) NSString * _Nullable refreshToken __attribute__((swift_name("refreshToken")));
@property (readonly) NSString * _Nullable scope __attribute__((swift_name("scope")));
@property (readonly) NSString *tokenType __attribute__((swift_name("tokenType")));
@property (readonly) NSString *updatedAt __attribute__((swift_name("updatedAt")));
@property (readonly) NSString *userId __attribute__((swift_name("userId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Vote")))
@interface SharedVote : SharedKotlinEnum<SharedVote *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedVote *yes __attribute__((swift_name("yes")));
@property (class, readonly) SharedVote *maybe __attribute__((swift_name("maybe")));
@property (class, readonly) SharedVote *no __attribute__((swift_name("no")));
+ (SharedKotlinArray<SharedVote *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedVote *> *entries __attribute__((swift_name("entries")));
@end


/**
 * Common interface for secure token storage across platforms
 */
__attribute__((swift_name("SecureTokenStorage")))
@protocol SharedSecureTokenStorage
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)clearAllTokensWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("clearAllTokens(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getAccessTokenWithCompletionHandler:(void (^)(NSString * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getAccessToken(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getRefreshTokenWithCompletionHandler:(void (^)(NSString * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getRefreshToken(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getTokenExpiryWithCompletionHandler:(void (^)(SharedLong * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getTokenExpiry(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getUserIdWithCompletionHandler:(void (^)(NSString * _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("getUserId(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)hasValidTokenWithCompletionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("hasValidToken(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)isTokenExpiredWithCompletionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("isTokenExpired(completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)storeAccessTokenToken:(NSString *)token completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("storeAccessToken(token:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)storeRefreshTokenToken:(NSString *)token completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("storeRefreshToken(token:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)storeTokenExpiryExpiryTimestamp:(int64_t)expiryTimestamp completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("storeTokenExpiry(expiryTimestamp:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)storeUserIdUserId:(NSString *)userId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("storeUserId(userId:completionHandler:)")));
@end

__attribute__((swift_name("KotlinThrowable")))
@interface SharedKotlinThrowable : SharedBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));

/**
 * @note annotations
 *   kotlin.experimental.ExperimentalNativeApi
*/
- (SharedKotlinArray<NSString *> *)getStackTrace __attribute__((swift_name("getStackTrace()")));
- (void)printStackTrace __attribute__((swift_name("printStackTrace()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedKotlinThrowable * _Nullable cause __attribute__((swift_name("cause")));
@property (readonly) NSString * _Nullable message __attribute__((swift_name("message")));
- (NSError *)asError __attribute__((swift_name("asError()")));
@end

__attribute__((swift_name("KotlinException")))
@interface SharedKotlinException : SharedKotlinThrowable
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end


/**
 * Exception thrown when a resource is forbidden (HTTP 403)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ForbiddenException")))
@interface SharedForbiddenException : SharedKotlinException
- (instancetype)initWithMessage:(NSString *)message statusCode:(int32_t)statusCode __attribute__((swift_name("init(message:statusCode:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithCause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (readonly) int32_t statusCode __attribute__((swift_name("statusCode")));
@end


/**
 * Exception thrown for other HTTP errors
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("HttpException")))
@interface SharedHttpException : SharedKotlinException
- (instancetype)initWithStatusCode:(int32_t)statusCode message:(NSString *)message __attribute__((swift_name("init(statusCode:message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithCause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (readonly) int32_t statusCode __attribute__((swift_name("statusCode")));
@end


/**
 * Metrics collector for sync operations
 */
__attribute__((swift_name("SyncMetrics")))
@protocol SharedSyncMetrics
@required
- (SharedSyncStats *)getSyncStats __attribute__((swift_name("getSyncStats()")));
- (void)recordConflictResolvedTable:(NSString *)table strategy:(NSString *)strategy __attribute__((swift_name("recordConflictResolved(table:strategy:)")));
- (void)recordSyncFailureDurationMs:(int64_t)durationMs error:(NSString *)error __attribute__((swift_name("recordSyncFailure(durationMs:error:)")));
- (void)recordSyncStart __attribute__((swift_name("recordSyncStart()")));
- (void)recordSyncSuccessDurationMs:(int64_t)durationMs changesApplied:(int32_t)changesApplied __attribute__((swift_name("recordSyncSuccess(durationMs:changesApplied:)")));
@end


/**
 * Simple in-memory metrics implementation
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("InMemorySyncMetrics")))
@interface SharedInMemorySyncMetrics : SharedBase <SharedSyncMetrics>

/**
 * Simple in-memory metrics implementation
 */
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));

/**
 * Simple in-memory metrics implementation
 */
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (SharedSyncStats *)getSyncStats __attribute__((swift_name("getSyncStats()")));
- (void)recordConflictResolvedTable:(NSString *)table strategy:(NSString *)strategy __attribute__((swift_name("recordConflictResolved(table:strategy:)")));
- (void)recordSyncFailureDurationMs:(int64_t)durationMs error:(NSString *)error __attribute__((swift_name("recordSyncFailure(durationMs:error:)")));
- (void)recordSyncStart __attribute__((swift_name("recordSyncStart()")));
- (void)recordSyncSuccessDurationMs:(int64_t)durationMs changesApplied:(int32_t)changesApplied __attribute__((swift_name("recordSyncSuccess(durationMs:changesApplied:)")));
@end


/**
 * Platform-agnostic network status detector interface
 */
__attribute__((swift_name("NetworkStatusDetector")))
@protocol SharedNetworkStatusDetector
@required
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> isNetworkAvailable __attribute__((swift_name("isNetworkAvailable")));
@end


/**
 * iOS network status detector (simplified - always available for now)
 * TODO: Implement proper NWPathMonitor integration
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("IosNetworkStatusDetector")))
@interface SharedIosNetworkStatusDetector : SharedBase <SharedNetworkStatusDetector>

/**
 * iOS network status detector (simplified - always available for now)
 * TODO: Implement proper NWPathMonitor integration
 */
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));

/**
 * iOS network status detector (simplified - always available for now)
 * TODO: Implement proper NWPathMonitor integration
 */
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> isNetworkAvailable __attribute__((swift_name("isNetworkAvailable")));
@end


/**
 * HTTP client for sync operations
 */
__attribute__((swift_name("SyncHttpClient")))
@protocol SharedSyncHttpClient
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)syncRequestJson:(NSString *)requestJson authToken:(NSString *)authToken completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("sync(requestJson:authToken:completionHandler:)")));
@end


/**
 * Ktor-based HTTP client for sync operations (iOS implementation)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KtorSyncHttpClient")))
@interface SharedKtorSyncHttpClient : SharedBase <SharedSyncHttpClient>
- (instancetype)initWithBaseUrl:(NSString *)baseUrl httpClient:(SharedKtor_client_coreHttpClient *)httpClient __attribute__((swift_name("init(baseUrl:httpClient:)"))) __attribute__((objc_designated_initializer));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)syncRequestJson:(NSString *)requestJson authToken:(NSString *)authToken completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("sync(requestJson:authToken:completionHandler:)")));
@end


/**
 * Alert manager for sync operations
 */
__attribute__((swift_name("SyncAlertManager")))
@protocol SharedSyncAlertManager
@required
- (void)alertHighConflictRateConflicts:(int32_t)conflicts __attribute__((swift_name("alertHighConflictRate(conflicts:)")));
- (void)alertNetworkIssues __attribute__((swift_name("alertNetworkIssues()")));
- (void)alertSyncFailureError:(NSString *)error retryCount:(int32_t)retryCount __attribute__((swift_name("alertSyncFailure(error:retryCount:)")));
@end


/**
 * Simple logging-based alert manager
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("LoggingSyncAlertManager")))
@interface SharedLoggingSyncAlertManager : SharedBase <SharedSyncAlertManager>

/**
 * Simple logging-based alert manager
 */
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));

/**
 * Simple logging-based alert manager
 */
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)alertHighConflictRateConflicts:(int32_t)conflicts __attribute__((swift_name("alertHighConflictRate(conflicts:)")));
- (void)alertNetworkIssues __attribute__((swift_name("alertNetworkIssues()")));
- (void)alertSyncFailureError:(NSString *)error retryCount:(int32_t)retryCount __attribute__((swift_name("alertSyncFailure(error:retryCount:)")));
@end


/**
 * Client-side sync manager for offline-first synchronization
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SyncManager")))
@interface SharedSyncManager : SharedBase
- (instancetype)initWithDatabase:(id<SharedWakevDb>)database eventRepository:(SharedDatabaseEventRepository *)eventRepository userRepository:(SharedUserRepository *)userRepository networkDetector:(id<SharedNetworkStatusDetector>)networkDetector httpClient:(id<SharedSyncHttpClient>)httpClient authTokenProvider:(NSString * _Nullable (^)(void))authTokenProvider authTokenRefreshProvider:(id<SharedKotlinSuspendFunction0> _Nullable)authTokenRefreshProvider maxRetries:(int32_t)maxRetries baseRetryDelayMs:(int64_t)baseRetryDelayMs metrics:(id<SharedSyncMetrics>)metrics alertManager:(id<SharedSyncAlertManager>)alertManager __attribute__((swift_name("init(database:eventRepository:userRepository:networkDetector:httpClient:authTokenProvider:authTokenRefreshProvider:maxRetries:baseRetryDelayMs:metrics:alertManager:)"))) __attribute__((objc_designated_initializer));

/**
 * Clean up old sync metadata
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)cleanupOldSyncDataWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("cleanupOldSyncData(completionHandler:)")));

/**
 * Clean up resources
 */
- (void)dispose __attribute__((swift_name("dispose()")));

/**
 * Get all pending changes ready for sync
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)getPendingChangesForSyncWithCompletionHandler:(void (^)(NSArray<SharedSyncChange *> * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getPendingChangesForSync(completionHandler:)")));

/**
 * Get sync metrics for monitoring
 */
- (SharedSyncStats *)getSyncMetrics __attribute__((swift_name("getSyncMetrics()")));

/**
 * Check if there are pending changes to sync
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)hasPendingChangesWithCompletionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("hasPendingChanges(completionHandler:)")));

/**
 * Log current sync status for monitoring
 */
- (void)logSyncStatus __attribute__((swift_name("logSyncStatus()")));

/**
 * Record a local change for later synchronization
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)recordLocalChangeTable:(NSString *)table operation:(SharedSyncOperation *)operation recordId:(NSString *)recordId data:(NSString *)data userId:(NSString *)userId completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("recordLocalChange(table:operation:recordId:data:userId:completionHandler:)")));

/**
 * Schedule automatic retry for failed changes
 */
- (void)scheduleRetryForFailedChanges __attribute__((swift_name("scheduleRetryForFailedChanges()")));

/**
 * Trigger synchronization with server
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)triggerSyncWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("triggerSync(completionHandler:)")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> isNetworkAvailable __attribute__((swift_name("isNetworkAvailable")));
@property (readonly) id<SharedKotlinx_coroutines_coreStateFlow> syncStatus __attribute__((swift_name("syncStatus")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SyncStats")))
@interface SharedSyncStats : SharedBase
- (instancetype)initWithTotalSyncs:(int32_t)totalSyncs successfulSyncs:(int32_t)successfulSyncs failedSyncs:(int32_t)failedSyncs averageDurationMs:(int64_t)averageDurationMs totalConflictsResolved:(int32_t)totalConflictsResolved lastSyncTime:(int64_t)lastSyncTime __attribute__((swift_name("init(totalSyncs:successfulSyncs:failedSyncs:averageDurationMs:totalConflictsResolved:lastSyncTime:)"))) __attribute__((objc_designated_initializer));
- (SharedSyncStats *)doCopyTotalSyncs:(int32_t)totalSyncs successfulSyncs:(int32_t)successfulSyncs failedSyncs:(int32_t)failedSyncs averageDurationMs:(int64_t)averageDurationMs totalConflictsResolved:(int32_t)totalConflictsResolved lastSyncTime:(int64_t)lastSyncTime __attribute__((swift_name("doCopy(totalSyncs:successfulSyncs:failedSyncs:averageDurationMs:totalConflictsResolved:lastSyncTime:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t averageDurationMs __attribute__((swift_name("averageDurationMs")));
@property (readonly) int32_t failedSyncs __attribute__((swift_name("failedSyncs")));
@property (readonly) int64_t lastSyncTime __attribute__((swift_name("lastSyncTime")));
@property (readonly) int32_t successfulSyncs __attribute__((swift_name("successfulSyncs")));
@property (readonly) int32_t totalConflictsResolved __attribute__((swift_name("totalConflictsResolved")));
@property (readonly) int32_t totalSyncs __attribute__((swift_name("totalSyncs")));
@end


/**
 * Sync status for tracking synchronization state
 */
__attribute__((swift_name("SyncStatus")))
@interface SharedSyncStatus : SharedBase
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SyncStatus.Error")))
@interface SharedSyncStatusError : SharedSyncStatus
- (instancetype)initWithMessage:(NSString *)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (SharedSyncStatusError *)doCopyMessage:(NSString *)message __attribute__((swift_name("doCopy(message:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *message __attribute__((swift_name("message")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SyncStatus.Idle")))
@interface SharedSyncStatusIdle : SharedSyncStatus
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)idle __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSyncStatusIdle *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SyncStatus.Syncing")))
@interface SharedSyncStatusSyncing : SharedSyncStatus
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)syncing __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedSyncStatusSyncing *shared __attribute__((swift_name("shared")));
@end


/**
 * Exception thrown when authentication fails (HTTP 401)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("UnauthorizedException")))
@interface SharedUnauthorizedException : SharedKotlinException
- (instancetype)initWithMessage:(NSString *)message statusCode:(int32_t)statusCode __attribute__((swift_name("init(message:statusCode:)"))) __attribute__((objc_designated_initializer));
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithCause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (readonly) int32_t statusCode __attribute__((swift_name("statusCode")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("ConstantsKt")))
@interface SharedConstantsKt : SharedBase
@property (class, readonly) int32_t SERVER_PORT __attribute__((swift_name("SERVER_PORT")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KtorSyncHttpClientKt")))
@interface SharedKtorSyncHttpClientKt : SharedBase
+ (id<SharedNetworkStatusDetector>)createNetworkStatusDetector __attribute__((swift_name("createNetworkStatusDetector()")));
+ (id<SharedSyncHttpClient>)createSyncHttpClientBaseUrl:(NSString *)baseUrl __attribute__((swift_name("createSyncHttpClient(baseUrl:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Platform_iosKt")))
@interface SharedPlatform_iosKt : SharedBase
+ (id<SharedCalendarService>)getCalendarService __attribute__((swift_name("getCalendarService()")));
+ (int64_t)getCurrentTimeMillis __attribute__((swift_name("getCurrentTimeMillis()")));
+ (id<SharedNotificationService>)getNotificationService __attribute__((swift_name("getNotificationService()")));
+ (id<SharedPlatform>)getPlatform __attribute__((swift_name("getPlatform()")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("SessionRepository_iosKt")))
@interface SharedSessionRepository_iosKt : SharedBase

/**
 * iOS implementation of current time in milliseconds.
 */
+ (int64_t)currentTimeMillis __attribute__((swift_name("currentTimeMillis()")));

/**
 * iOS implementation of SHA-256 hashing.
 *
 * Note: This is a simplified implementation using Swift's native crypto.
 * For production, consider using CryptoKit interop.
 */
+ (NSString *)sha256HashInput:(NSString *)input __attribute__((swift_name("sha256Hash(input:)")));
@end

__attribute__((swift_name("RuntimeCloseable")))
@protocol SharedRuntimeCloseable
@required
- (void)close __attribute__((swift_name("close()")));
@end

__attribute__((swift_name("RuntimeSqlDriver")))
@protocol SharedRuntimeSqlDriver <SharedRuntimeCloseable>
@required
- (void)addListenerQueryKeys:(SharedKotlinArray<NSString *> *)queryKeys listener:(id<SharedRuntimeQueryListener>)listener __attribute__((swift_name("addListener(queryKeys:listener:)")));
- (SharedRuntimeTransacterTransaction * _Nullable)currentTransaction __attribute__((swift_name("currentTransaction()")));
- (id<SharedRuntimeQueryResult>)executeIdentifier:(SharedInt * _Nullable)identifier sql:(NSString *)sql parameters:(int32_t)parameters binders:(void (^ _Nullable)(id<SharedRuntimeSqlPreparedStatement>))binders __attribute__((swift_name("execute(identifier:sql:parameters:binders:)")));
- (id<SharedRuntimeQueryResult>)executeQueryIdentifier:(SharedInt * _Nullable)identifier sql:(NSString *)sql mapper:(id<SharedRuntimeQueryResult> (^)(id<SharedRuntimeSqlCursor>))mapper parameters:(int32_t)parameters binders:(void (^ _Nullable)(id<SharedRuntimeSqlPreparedStatement>))binders __attribute__((swift_name("executeQuery(identifier:sql:mapper:parameters:binders:)")));
- (id<SharedRuntimeQueryResult>)doNewTransaction __attribute__((swift_name("doNewTransaction()")));
- (void)notifyListenersQueryKeys:(SharedKotlinArray<NSString *> *)queryKeys __attribute__((swift_name("notifyListeners(queryKeys:)")));
- (void)removeListenerQueryKeys:(SharedKotlinArray<NSString *> *)queryKeys listener:(id<SharedRuntimeQueryListener>)listener __attribute__((swift_name("removeListener(queryKeys:listener:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinUnit")))
@interface SharedKotlinUnit : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)unit __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinUnit *shared __attribute__((swift_name("shared")));
- (NSString *)description __attribute__((swift_name("description()")));
@end

__attribute__((swift_name("RuntimeTransactionCallbacks")))
@protocol SharedRuntimeTransactionCallbacks
@required
- (void)afterCommitFunction:(void (^)(void))function __attribute__((swift_name("afterCommit(function:)")));
- (void)afterRollbackFunction:(void (^)(void))function __attribute__((swift_name("afterRollback(function:)")));
@end

__attribute__((swift_name("RuntimeTransacterTransaction")))
@interface SharedRuntimeTransacterTransaction : SharedBase <SharedRuntimeTransactionCallbacks>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)afterCommitFunction:(void (^)(void))function __attribute__((swift_name("afterCommit(function:)")));
- (void)afterRollbackFunction:(void (^)(void))function __attribute__((swift_name("afterRollback(function:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (id<SharedRuntimeQueryResult>)endTransactionSuccessful:(BOOL)successful __attribute__((swift_name("endTransaction(successful:)")));

/**
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property (readonly) SharedRuntimeTransacterTransaction * _Nullable enclosingTransaction __attribute__((swift_name("enclosingTransaction")));
@end

__attribute__((swift_name("RuntimeTransactionWithoutReturn")))
@protocol SharedRuntimeTransactionWithoutReturn <SharedRuntimeTransactionCallbacks>
@required
- (void)rollback __attribute__((swift_name("rollback()")));
- (void)transactionBody:(void (^)(id<SharedRuntimeTransactionWithoutReturn>))body __attribute__((swift_name("transaction(body:)")));
@end

__attribute__((swift_name("RuntimeTransactionWithReturn")))
@protocol SharedRuntimeTransactionWithReturn <SharedRuntimeTransactionCallbacks>
@required
- (void)rollbackReturnValue:(id _Nullable)returnValue __attribute__((swift_name("rollback(returnValue:)")));
- (id _Nullable)transactionBody_:(id _Nullable (^)(id<SharedRuntimeTransactionWithReturn>))body __attribute__((swift_name("transaction(body_:)")));
@end

__attribute__((swift_name("RuntimeExecutableQuery")))
@interface SharedRuntimeExecutableQuery<__covariant RowType> : SharedBase
- (instancetype)initWithMapper:(RowType (^)(id<SharedRuntimeSqlCursor>))mapper __attribute__((swift_name("init(mapper:)"))) __attribute__((objc_designated_initializer));
- (id<SharedRuntimeQueryResult>)executeMapper:(id<SharedRuntimeQueryResult> (^)(id<SharedRuntimeSqlCursor>))mapper __attribute__((swift_name("execute(mapper:)")));
- (NSArray<RowType> *)executeAsList __attribute__((swift_name("executeAsList()")));
- (RowType)executeAsOne __attribute__((swift_name("executeAsOne()")));
- (RowType _Nullable)executeAsOneOrNull __attribute__((swift_name("executeAsOneOrNull()")));
@property (readonly) RowType (^mapper)(id<SharedRuntimeSqlCursor>) __attribute__((swift_name("mapper")));
@end

__attribute__((swift_name("RuntimeQuery")))
@interface SharedRuntimeQuery<__covariant RowType> : SharedRuntimeExecutableQuery<RowType>
- (instancetype)initWithMapper:(RowType (^)(id<SharedRuntimeSqlCursor>))mapper __attribute__((swift_name("init(mapper:)"))) __attribute__((objc_designated_initializer));
- (void)addListenerListener:(id<SharedRuntimeQueryListener>)listener __attribute__((swift_name("addListener(listener:)")));
- (void)removeListenerListener:(id<SharedRuntimeQueryListener>)listener __attribute__((swift_name("removeListener(listener:)")));
@end

__attribute__((swift_name("KotlinRuntimeException")))
@interface SharedKotlinRuntimeException : SharedKotlinException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((swift_name("KotlinIllegalStateException")))
@interface SharedKotlinIllegalStateException : SharedKotlinRuntimeException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.4")
*/
__attribute__((swift_name("KotlinCancellationException")))
@interface SharedKotlinCancellationException : SharedKotlinIllegalStateException
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithMessage:(NSString * _Nullable)message __attribute__((swift_name("init(message:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(cause:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithMessage:(NSString * _Nullable)message cause:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("init(message:cause:)"))) __attribute__((objc_designated_initializer));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinPair")))
@interface SharedKotlinPair<__covariant A, __covariant B> : SharedBase
- (instancetype)initWithFirst:(A _Nullable)first second:(B _Nullable)second __attribute__((swift_name("init(first:second:)"))) __attribute__((objc_designated_initializer));
- (SharedKotlinPair<A, B> *)doCopyFirst:(A _Nullable)first second:(B _Nullable)second __attribute__((swift_name("doCopy(first:second:)")));
- (BOOL)equalsOther:(id _Nullable)other __attribute__((swift_name("equals(other:)")));
- (int32_t)hashCode __attribute__((swift_name("hashCode()")));
- (NSString *)toString __attribute__((swift_name("toString()")));
@property (readonly) A _Nullable first __attribute__((swift_name("first")));
@property (readonly) B _Nullable second __attribute__((swift_name("second")));
@end

__attribute__((swift_name("Kotlinx_coroutines_coreFlow")))
@protocol SharedKotlinx_coroutines_coreFlow
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)collectCollector:(id<SharedKotlinx_coroutines_coreFlowCollector>)collector completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("collect(collector:completionHandler:)")));
@end

__attribute__((swift_name("Kotlinx_coroutines_coreSharedFlow")))
@protocol SharedKotlinx_coroutines_coreSharedFlow <SharedKotlinx_coroutines_coreFlow>
@required
@property (readonly) NSArray<id> *replayCache __attribute__((swift_name("replayCache")));
@end

__attribute__((swift_name("Kotlinx_coroutines_coreStateFlow")))
@protocol SharedKotlinx_coroutines_coreStateFlow <SharedKotlinx_coroutines_coreSharedFlow>
@required
@property (readonly) id _Nullable value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinEnumCompanion")))
@interface SharedKotlinEnumCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinEnumCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinArray")))
@interface SharedKotlinArray<T> : SharedBase
+ (instancetype)arrayWithSize:(int32_t)size init:(T _Nullable (^)(SharedInt *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (T _Nullable)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (id<SharedKotlinIterator>)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(T _Nullable)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((swift_name("Kotlinx_coroutines_coreCoroutineScope")))
@protocol SharedKotlinx_coroutines_coreCoroutineScope
@required
@property (readonly) id<SharedKotlinCoroutineContext> coroutineContext __attribute__((swift_name("coroutineContext")));
@end

__attribute__((swift_name("KotlinFunction")))
@protocol SharedKotlinFunction
@required
@end

__attribute__((swift_name("KotlinSuspendFunction1")))
@protocol SharedKotlinSuspendFunction1 <SharedKotlinFunction>
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeP1:(id _Nullable)p1 completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(p1:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinTriple")))
@interface SharedKotlinTriple<__covariant A, __covariant B, __covariant C> : SharedBase
- (instancetype)initWithFirst:(A _Nullable)first second:(B _Nullable)second third:(C _Nullable)third __attribute__((swift_name("init(first:second:third:)"))) __attribute__((objc_designated_initializer));
- (SharedKotlinTriple<A, B, C> *)doCopyFirst:(A _Nullable)first second:(B _Nullable)second third:(C _Nullable)third __attribute__((swift_name("doCopy(first:second:third:)")));
- (BOOL)equalsOther:(id _Nullable)other __attribute__((swift_name("equals(other:)")));
- (int32_t)hashCode __attribute__((swift_name("hashCode()")));
- (NSString *)toString __attribute__((swift_name("toString()")));
@property (readonly) A _Nullable first __attribute__((swift_name("first")));
@property (readonly) B _Nullable second __attribute__((swift_name("second")));
@property (readonly) C _Nullable third __attribute__((swift_name("third")));
@end

__attribute__((swift_name("RuntimeSqlSchema")))
@protocol SharedRuntimeSqlSchema
@required
- (id<SharedRuntimeQueryResult>)createDriver:(id<SharedRuntimeSqlDriver>)driver __attribute__((swift_name("create(driver:)")));
- (id<SharedRuntimeQueryResult>)migrateDriver:(id<SharedRuntimeSqlDriver>)driver oldVersion:(int64_t)oldVersion newVersion:(int64_t)newVersion callbacks:(SharedKotlinArray<SharedRuntimeAfterVersion *> *)callbacks __attribute__((swift_name("migrate(driver:oldVersion:newVersion:callbacks:)")));
@property (readonly) int64_t version __attribute__((swift_name("version")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreSerializationStrategy")))
@protocol SharedKotlinx_serialization_coreSerializationStrategy
@required
- (void)serializeEncoder:(id<SharedKotlinx_serialization_coreEncoder>)encoder value:(id _Nullable)value __attribute__((swift_name("serialize(encoder:value:)")));
@property (readonly) id<SharedKotlinx_serialization_coreSerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreDeserializationStrategy")))
@protocol SharedKotlinx_serialization_coreDeserializationStrategy
@required
- (id _Nullable)deserializeDecoder:(id<SharedKotlinx_serialization_coreDecoder>)decoder __attribute__((swift_name("deserialize(decoder:)")));
@property (readonly) id<SharedKotlinx_serialization_coreSerialDescriptor> descriptor __attribute__((swift_name("descriptor")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreKSerializer")))
@protocol SharedKotlinx_serialization_coreKSerializer <SharedKotlinx_serialization_coreSerializationStrategy, SharedKotlinx_serialization_coreDeserializationStrategy>
@required
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="2.0")
*/
__attribute__((swift_name("KotlinAutoCloseable")))
@protocol SharedKotlinAutoCloseable
@required
- (void)close __attribute__((swift_name("close()")));
@end

__attribute__((swift_name("Ktor_ioCloseable")))
@protocol SharedKtor_ioCloseable <SharedKotlinAutoCloseable>
@required
@end


/**
 * A multiplatform asynchronous HTTP client that allows you to make requests, handle responses,
 * and extend its functionality with plugins such as authentication, JSON serialization, and more.
 *
 * # Creating client
 * To create a new client, you can call:
 * ```kotlin
 * val client = HttpClient()
 * ```
 * You can create as many clients as you need.
 *
 * If you no longer need the client, please consider closing it to release resources:
 * ```
 * client.close()
 * ```
 *
 * To learn more on how to create and configure an [HttpClient] see the tutorial page:
 * [Creating and configuring a client](https://ktor.io/docs/create-client.html).
 *
 * # Making API Requests
 * For every HTTP method (GET, POST, PUT, etc.), there is a corresponding function:
 * ```kotlin
 * val response: HttpResponse = client.get("https://ktor.io/")
 * val body = response.bodyAsText()
 * ```
 * See [Making HTTP requests](https://ktor.io/docs/client-requests.html) for more details.
 *
 * # Query Parameters
 * Add query parameters to your request using the `parameter` function:
 * ```kotlin
 * val response = client.get("https://google.com/search") {
 *     url {
 *         parameter("q", "REST API with Ktor")
 *     }
 * }
 * ```
 * For more information, refer to [Passing request parameters](https://ktor.io/docs/client-requests.html#parameters).
 *
 * # Adding Headers
 * Include headers in your request using the `headers` builder or the `header` function:
 * ```kotlin
 * val response = client.get("https://httpbin.org/bearer") {
 *     headers {
 *         append("Authorization", "Bearer your_token_here")
 *         append("Accept", "application/json")
 *     }
 * }
 * ```
 * Learn more at [Adding headers to a request](https://ktor.io/docs/client-requests.html#headers).
 *
 * # JSON Serialization
 * Add dependencies:
 * - io.ktor:ktor-client-content-negotiation:3.+
 * - io.ktor:ktor-serialization-kotlinx-json:3.+
 * Add Gradle plugin:
 * ```
 * plugins {
 *     kotlin("plugin.serialization")
 * }
 * ```
 *
 * Send and receive JSON data by installing the `ContentNegotiation` plugin with `kotlinx.serialization`:
 * ```kotlin
 * val client = HttpClient {
 *     install(ContentNegotiation) {
 *         json()
 *     }
 * }
 *
 * @Serializable
 * data class MyRequestType(val someData: String)
 *
 * @Serializable
 * data class MyResponseType(val someResponseData: String)
 *
 * val response: MyResponseType = client.post("https://api.example.com/data") {
 *     contentType(ContentType.Application.Json)
 *     setBody(MyRequestType(someData = "value"))
 * }.body()
 * ```
 * See [Serializing JSON data](https://ktor.io/docs/client-serialization.html) for maven configuration and other details.
 *
 * # Submitting Forms
 * Submit form data using `FormDataContent` or the `submitForm` function:
 * ```kotlin
 * // Using FormDataContent
 * val response = client.post("https://example.com/submit") {
 *     setBody(FormDataContent(Parameters.build {
 *         append("username", "user")
 *         append("password", "pass")
 *     }))
 * }
 *
 * // Or using submitForm
 * val response = client.submitForm(
 *     url = "https://example.com/submit",
 *     formParameters = Parameters.build {
 *         append("username", "user")
 *         append("password", "pass")
 *     }
 * )
 * ```
 * More information is available at [Submitting form parameters](https://ktor.io/docs/client-requests.html#form_parameters).
 *
 * # Handling Authentication
 * Add dependency: io.ktor:ktor-client-auth:3.+
 *
 * Use the `Auth` plugin to handle various authentication schemes like Basic or Bearer token authentication:
 * ```kotlin
 * val client = HttpClient {
 *     install(Auth) {
 *         bearer {
 *             loadTokens {
 *                 BearerTokens(accessToken = "your_access_token", refreshToken = "your_refresh_token")
 *             }
 *         }
 *     }
 * }
 *
 * val response = client.get("https://api.example.com/protected")
 * ```
 * Refer to [Client authentication](https://ktor.io/docs/client-auth.html) for more details.
 *
 * # Setting Timeouts and Retries
 * Configure timeouts and implement retry logic for your requests:
 * ```kotlin
 * val client = HttpClient {
 *     install(HttpTimeout) {
 *         requestTimeoutMillis = 10000
 *         connectTimeoutMillis = 5000
 *         socketTimeoutMillis = 15000
 *     }
 * }
 * ```
 *
 * For the request timeout:
 * ```kotlin
 * client.get("") {
 *     timeout {
 *         requestTimeoutMillis = 1000
 *     }
 * }
 * ```
 * See [Timeout](https://ktor.io/docs/client-timeout.html) for more information.
 *
 * # Handling Cookies
 *
 * Manage cookies automatically by installing the `HttpCookies` plugin:
 * ```kotlin
 * val client = HttpClient {
 *     install(HttpCookies) {
 *         storage = AcceptAllCookiesStorage()
 *     }
 * }
 *
 * // Accessing cookies
 * val cookies: List<Cookie> = client.cookies("https://example.com")
 * ```
 * Learn more at [Cookies](https://ktor.io/docs/client-cookies.html).
 *
 * # Uploading Files
 * Upload files using multipart/form-data requests:
 * ```kotlin
 * client.submitFormWithBinaryData(
 *      url = "https://example.com/upload",
 *      formData = formData {
 *          append("description", "File upload example")
 *          append("file", {
 *              File("path/to/file.txt").readChannel()
 *          })
 *      }
 *  )
 *
 * See [Uploading data](https://ktor.io/docs/client-requests.html#upload_file) for details.
 *
 * # Using WebSockets
 *
 * Communicate over WebSockets using the `webSocket` function:
 * ```kotlin
 * client.webSocket("wss://echo.websocket.org") {
 *     send(Frame.Text("Hello, WebSocket!"))
 *     val frame = incoming.receive()
 *     if (frame is Frame.Text) {
 *         println("Received: ${frame.readText()}")
 *     }
 * }
 * ```
 * Learn more at [Client WebSockets](https://ktor.io/docs/client-websockets.html).
 *
 * # Error Handling
 * Handle exceptions and HTTP error responses gracefully:
 * val client = HttpClient {
 *     HttpResponseValidator {
 *         validateResponse { response ->
 *             val statusCode = response.status.value
 *             when (statusCode) {
 *                 in 300..399 -> error("Redirects are not allowed")
 *             }
 *         }
 *     }
 * }
 * See [Error handling](https://ktor.io/docs/client-response-validation.html) for more information.
 *
 * # Configuring SSL/TLS
 *
 * Customize SSL/TLS settings for secure connections is engine-specific. Please refer to the following page for
 * the details: [Client SSL/TLS](https://ktor.io/docs/client-ssl.html).
 *
 * # Using Proxies
 * Route requests through an HTTP or SOCKS proxy:
 * ```kotlin
 * val client = HttpClient() {
 *     engine {
 *         proxy = ProxyBuilder.http("http://proxy.example.com:8080")
 *         // For a SOCKS proxy:
 *         // proxy = ProxyBuilder.socks(host = "proxy.example.com", port = 1080)
 *     }
 * }
 * ```
 * See [Using a proxy](https://ktor.io/docs/client-proxy.html) for details.
 *
 * # Streaming Data
 *
 * Stream large data efficiently without loading it entirely into memory.
 *
 * Stream request:
 * ```kotlin
 * val response = client.post("https://example.com/upload") {
 *      setBody(object: OutgoingContent.WriteChannelContent() {
 *          override suspend fun writeTo(channel: ByteWriteChannel) {
 *              repeat(1000) {
 *                  channel.writeString("Hello!")
 *              }
 *          }
 *      })
 * }
 * ```
 *
 * Stream response:
 * ```kotlin
 * client.prepareGet("https://example.com/largefile.zip").execute { response ->
 *     val channel: ByteReadChannel = response.bodyAsChannel()
 *
 *     while (!channel.exhausted()) {
 *         val chunk = channel.readBuffer()
 *         // ...
 *     }
 * }
 * ```
 * Learn more at [Streaming data](https://ktor.io/docs/client-responses.html#streaming).
 *
 * # Using SSE
 * Server-Sent Events (SSE) is a technology that allows a server to continuously push events to a client over an HTTP
 * connection. It's particularly useful in cases where the server needs to send event-based updates without requiring
 * the client to repeatedly poll the server.
 *
 * Install the plugin:
 * ```kotlin
 * val client = HttpClient(CIO) {
 *     install(SSE)
 * }
 * ```
 *
 * ```
 * client.sse(host = "0.0.0.0", port = 8080, path = "/events") {
 *     while (true) {
 *         for (event in incoming) {
 *             println("Event from server:")
 *             println(event)
 *         }
 *     }
 * }
 * ```
 *
 * Visit [Using SSE](https://ktor.io/docs/client-server-sent-events.html#install_plugin) to learn more.
 *
 * # Customizing a client with plugins
 * To extend out-of-the-box functionality, you can install plugins for a Ktor client:
 * ```kotlin
 * val client = HttpClient {
 *     install(ContentNegotiation) {
 *         json()
 *     }
 * }
 * ```
 *
 * There are many plugins available out of the box, and you can write your own. See
 * [Create custom client plugins](https://ktor.io/docs/client-custom-plugins.html) to learn more.
 *
 * # Service Loader and Default Engine
 * On JVM, calling `HttpClient()` without specifying an engine uses a service loader mechanism to
 * determine the appropriate default engine. This can introduce a performance overhead, especially on
 * slower devices, such as Android.
 *
 * **Performance Note**: If you are targeting platforms where initialization speed is critical,
 * consider explicitly specifying an engine to avoid the service loader lookup.
 *
 * Example with manual engine specification:
 * ```
 * val client = HttpClient(Apache) // Explicitly uses Apache engine, bypassing service loader
 * ```
 *
 * By directly setting the engine (e.g., `Apache`, `OkHttp`), you can optimize startup performance
 * by preventing the default service loader mechanism.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.HttpClient)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_client_coreHttpClient")))
@interface SharedKtor_client_coreHttpClient : SharedBase <SharedKotlinx_coroutines_coreCoroutineScope, SharedKtor_ioCloseable>
- (instancetype)initWithEngine:(id<SharedKtor_client_coreHttpClientEngine>)engine userConfig:(SharedKtor_client_coreHttpClientConfig<SharedKtor_client_coreHttpClientEngineConfig *> *)userConfig __attribute__((swift_name("init(engine:userConfig:)"))) __attribute__((objc_designated_initializer));

/**
 * Initiates the shutdown process for the `HttpClient`. This is a non-blocking call, which
 * means it returns immediately and begins the client closure in the background.
 *
 * ## Usage
 * ```
 * val client = HttpClient()
 * client.close()
 * client.coroutineContext.job.join() // Waits for complete termination if necessary
 * ```
 *
 * ## Important Notes
 * - **Non-blocking**: `close()` only starts the closing process and does not wait for it to complete.
 * - **Coroutine Context**: To wait for all client resources to be freed, use `client.coroutineContext.job.join()`
 *   or `client.coroutineContext.cancel()` to terminate ongoing tasks.
 * - **Manual Engine Management**: If a custom `engine` was manually created, it must be closed explicitly
 *   after calling `client.close()` to release all resources.
 *
 * Example with custom engine management:
 * ```
 * val engine = HttpClientEngine() // Custom engine instance
 * val client = HttpClient(engine)
 *
 * client.close()
 * engine.close() // Ensure manually created engine is also closed
 * ```
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.HttpClient.close)
 */
- (void)close __attribute__((swift_name("close()")));

/**
 * Returns a new [HttpClient] by copying this client's configuration
 * and additionally configured by the [block] parameter.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.HttpClient.config)
 */
- (SharedKtor_client_coreHttpClient *)configBlock:(void (^)(SharedKtor_client_coreHttpClientConfig<id> *))block __attribute__((swift_name("config(block:)")));

/**
 * Checks if the specified [capability] is supported by this client.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.HttpClient.isSupported)
 */
- (BOOL)isSupportedCapability:(id<SharedKtor_client_coreHttpClientEngineCapability>)capability __attribute__((swift_name("isSupported(capability:)")));
- (NSString *)description __attribute__((swift_name("description()")));

/**
 * Typed attributes used as a lightweight container for this client.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.HttpClient.attributes)
 */
@property (readonly) id<SharedKtor_utilsAttributes> attributes __attribute__((swift_name("attributes")));
@property (readonly) id<SharedKotlinCoroutineContext> coroutineContext __attribute__((swift_name("coroutineContext")));
@property (readonly) id<SharedKtor_client_coreHttpClientEngine> engine __attribute__((swift_name("engine")));

/**
 * Provides access to the client's engine configuration.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.HttpClient.engineConfig)
 */
@property (readonly) SharedKtor_client_coreHttpClientEngineConfig *engineConfig __attribute__((swift_name("engineConfig")));

/**
 * Provides access to the events of the client's lifecycle.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.HttpClient.monitor)
 */
@property (readonly) SharedKtor_eventsEvents *monitor __attribute__((swift_name("monitor")));

/**
 * A pipeline used for receiving a request.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.HttpClient.receivePipeline)
 */
@property (readonly) SharedKtor_client_coreHttpReceivePipeline *receivePipeline __attribute__((swift_name("receivePipeline")));

/**
 * A pipeline used for processing all requests sent by this client.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.HttpClient.requestPipeline)
 */
@property (readonly) SharedKtor_client_coreHttpRequestPipeline *requestPipeline __attribute__((swift_name("requestPipeline")));

/**
 * A pipeline used for processing all responses sent by the server.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.HttpClient.responsePipeline)
 */
@property (readonly) SharedKtor_client_coreHttpResponsePipeline *responsePipeline __attribute__((swift_name("responsePipeline")));

/**
 * A pipeline used for sending a request.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.HttpClient.sendPipeline)
 */
@property (readonly) SharedKtor_client_coreHttpSendPipeline *sendPipeline __attribute__((swift_name("sendPipeline")));
@end

__attribute__((swift_name("KotlinSuspendFunction0")))
@protocol SharedKotlinSuspendFunction0 <SharedKotlinFunction>
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(completionHandler:)")));
@end

__attribute__((swift_name("RuntimeQueryListener")))
@protocol SharedRuntimeQueryListener
@required
- (void)queryResultsChanged __attribute__((swift_name("queryResultsChanged()")));
@end

__attribute__((swift_name("RuntimeQueryResult")))
@protocol SharedRuntimeQueryResult
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)awaitWithCompletionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("await(completionHandler:)")));
@property (readonly) id _Nullable value __attribute__((swift_name("value")));
@end

__attribute__((swift_name("RuntimeSqlPreparedStatement")))
@protocol SharedRuntimeSqlPreparedStatement
@required
- (void)bindBooleanIndex:(int32_t)index boolean:(SharedBoolean * _Nullable)boolean __attribute__((swift_name("bindBoolean(index:boolean:)")));
- (void)bindBytesIndex:(int32_t)index bytes:(SharedKotlinByteArray * _Nullable)bytes __attribute__((swift_name("bindBytes(index:bytes:)")));
- (void)bindDoubleIndex:(int32_t)index double:(SharedDouble * _Nullable)double_ __attribute__((swift_name("bindDouble(index:double:)")));
- (void)bindLongIndex:(int32_t)index long:(SharedLong * _Nullable)long_ __attribute__((swift_name("bindLong(index:long:)")));
- (void)bindStringIndex:(int32_t)index string:(NSString * _Nullable)string __attribute__((swift_name("bindString(index:string:)")));
@end

__attribute__((swift_name("RuntimeSqlCursor")))
@protocol SharedRuntimeSqlCursor
@required
- (SharedBoolean * _Nullable)getBooleanIndex:(int32_t)index __attribute__((swift_name("getBoolean(index:)")));
- (SharedKotlinByteArray * _Nullable)getBytesIndex:(int32_t)index __attribute__((swift_name("getBytes(index:)")));
- (SharedDouble * _Nullable)getDoubleIndex:(int32_t)index __attribute__((swift_name("getDouble(index:)")));
- (SharedLong * _Nullable)getLongIndex:(int32_t)index __attribute__((swift_name("getLong(index:)")));
- (NSString * _Nullable)getStringIndex:(int32_t)index __attribute__((swift_name("getString(index:)")));
- (id<SharedRuntimeQueryResult>)next __attribute__((swift_name("next()")));
@end

__attribute__((swift_name("Kotlinx_coroutines_coreFlowCollector")))
@protocol SharedKotlinx_coroutines_coreFlowCollector
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)emitValue:(id _Nullable)value completionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("emit(value:completionHandler:)")));
@end

__attribute__((swift_name("KotlinIterator")))
@protocol SharedKotlinIterator
@required
- (BOOL)hasNext __attribute__((swift_name("hasNext()")));
- (id _Nullable)next __attribute__((swift_name("next()")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
__attribute__((swift_name("KotlinCoroutineContext")))
@protocol SharedKotlinCoroutineContext
@required
- (id _Nullable)foldInitial:(id _Nullable)initial operation:(id _Nullable (^)(id _Nullable, id<SharedKotlinCoroutineContextElement>))operation __attribute__((swift_name("fold(initial:operation:)")));
- (id<SharedKotlinCoroutineContextElement> _Nullable)getKey:(id<SharedKotlinCoroutineContextKey>)key __attribute__((swift_name("get(key:)")));
- (id<SharedKotlinCoroutineContext>)minusKeyKey:(id<SharedKotlinCoroutineContextKey>)key __attribute__((swift_name("minusKey(key:)")));
- (id<SharedKotlinCoroutineContext>)plusContext:(id<SharedKotlinCoroutineContext>)context __attribute__((swift_name("plus(context:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("RuntimeAfterVersion")))
@interface SharedRuntimeAfterVersion : SharedBase
- (instancetype)initWithAfterVersion:(int64_t)afterVersion block:(void (^)(id<SharedRuntimeSqlDriver>))block __attribute__((swift_name("init(afterVersion:block:)"))) __attribute__((objc_designated_initializer));
@property (readonly) int64_t afterVersion __attribute__((swift_name("afterVersion")));
@property (readonly) void (^block)(id<SharedRuntimeSqlDriver>) __attribute__((swift_name("block")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreEncoder")))
@protocol SharedKotlinx_serialization_coreEncoder
@required
- (id<SharedKotlinx_serialization_coreCompositeEncoder>)beginCollectionDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor collectionSize:(int32_t)collectionSize __attribute__((swift_name("beginCollection(descriptor:collectionSize:)")));
- (id<SharedKotlinx_serialization_coreCompositeEncoder>)beginStructureDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("beginStructure(descriptor:)")));
- (void)encodeBooleanValue:(BOOL)value __attribute__((swift_name("encodeBoolean(value:)")));
- (void)encodeByteValue:(int8_t)value __attribute__((swift_name("encodeByte(value:)")));
- (void)encodeCharValue:(unichar)value __attribute__((swift_name("encodeChar(value:)")));
- (void)encodeDoubleValue:(double)value __attribute__((swift_name("encodeDouble(value:)")));
- (void)encodeEnumEnumDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)enumDescriptor index:(int32_t)index __attribute__((swift_name("encodeEnum(enumDescriptor:index:)")));
- (void)encodeFloatValue:(float)value __attribute__((swift_name("encodeFloat(value:)")));
- (id<SharedKotlinx_serialization_coreEncoder>)encodeInlineDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("encodeInline(descriptor:)")));
- (void)encodeIntValue:(int32_t)value __attribute__((swift_name("encodeInt(value:)")));
- (void)encodeLongValue:(int64_t)value __attribute__((swift_name("encodeLong(value:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNotNullMark __attribute__((swift_name("encodeNotNullMark()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNull __attribute__((swift_name("encodeNull()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNullableSerializableValueSerializer:(id<SharedKotlinx_serialization_coreSerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeNullableSerializableValue(serializer:value:)")));
- (void)encodeSerializableValueSerializer:(id<SharedKotlinx_serialization_coreSerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeSerializableValue(serializer:value:)")));
- (void)encodeShortValue:(int16_t)value __attribute__((swift_name("encodeShort(value:)")));
- (void)encodeStringValue:(NSString *)value __attribute__((swift_name("encodeString(value:)")));
@property (readonly) SharedKotlinx_serialization_coreSerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreSerialDescriptor")))
@protocol SharedKotlinx_serialization_coreSerialDescriptor
@required
- (NSArray<id<SharedKotlinAnnotation>> *)getElementAnnotationsIndex:(int32_t)index __attribute__((swift_name("getElementAnnotations(index:)")));
- (id<SharedKotlinx_serialization_coreSerialDescriptor>)getElementDescriptorIndex:(int32_t)index __attribute__((swift_name("getElementDescriptor(index:)")));
- (int32_t)getElementIndexName:(NSString *)name __attribute__((swift_name("getElementIndex(name:)")));
- (NSString *)getElementNameIndex:(int32_t)index __attribute__((swift_name("getElementName(index:)")));
- (BOOL)isElementOptionalIndex:(int32_t)index __attribute__((swift_name("isElementOptional(index:)")));
@property (readonly) NSArray<id<SharedKotlinAnnotation>> *annotations __attribute__((swift_name("annotations")));
@property (readonly) int32_t elementsCount __attribute__((swift_name("elementsCount")));
@property (readonly) BOOL isInline __attribute__((swift_name("isInline")));
@property (readonly) BOOL isNullable __attribute__((swift_name("isNullable")));
@property (readonly) SharedKotlinx_serialization_coreSerialKind *kind __attribute__((swift_name("kind")));
@property (readonly) NSString *serialName __attribute__((swift_name("serialName")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreDecoder")))
@protocol SharedKotlinx_serialization_coreDecoder
@required
- (id<SharedKotlinx_serialization_coreCompositeDecoder>)beginStructureDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("beginStructure(descriptor:)")));
- (BOOL)decodeBoolean __attribute__((swift_name("decodeBoolean()")));
- (int8_t)decodeByte __attribute__((swift_name("decodeByte()")));
- (unichar)decodeChar __attribute__((swift_name("decodeChar()")));
- (double)decodeDouble __attribute__((swift_name("decodeDouble()")));
- (int32_t)decodeEnumEnumDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)enumDescriptor __attribute__((swift_name("decodeEnum(enumDescriptor:)")));
- (float)decodeFloat __attribute__((swift_name("decodeFloat()")));
- (id<SharedKotlinx_serialization_coreDecoder>)decodeInlineDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("decodeInline(descriptor:)")));
- (int32_t)decodeInt __attribute__((swift_name("decodeInt()")));
- (int64_t)decodeLong __attribute__((swift_name("decodeLong()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (BOOL)decodeNotNullMark __attribute__((swift_name("decodeNotNullMark()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (SharedKotlinNothing * _Nullable)decodeNull __attribute__((swift_name("decodeNull()")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id _Nullable)decodeNullableSerializableValueDeserializer:(id<SharedKotlinx_serialization_coreDeserializationStrategy>)deserializer __attribute__((swift_name("decodeNullableSerializableValue(deserializer:)")));
- (id _Nullable)decodeSerializableValueDeserializer:(id<SharedKotlinx_serialization_coreDeserializationStrategy>)deserializer __attribute__((swift_name("decodeSerializableValue(deserializer:)")));
- (int16_t)decodeShort __attribute__((swift_name("decodeShort()")));
- (NSString *)decodeString __attribute__((swift_name("decodeString()")));
@property (readonly) SharedKotlinx_serialization_coreSerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end


/**
 * Serves as the base interface for an [HttpClient]'s engine.
 *
 * An `HttpClientEngine` represents the underlying network implementation that
 * performs HTTP requests and handles responses.
 * Developers can implement this interface to create custom engines for use with [HttpClient].
 *
 * This interface provides a set of properties and methods that define the
 * contract for configuring, executing, and managing HTTP requests within the engine.
 *
 * For a base implementation that handles common engine functionality, see [HttpClientEngineBase].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.engine.HttpClientEngine)
 */
__attribute__((swift_name("Ktor_client_coreHttpClientEngine")))
@protocol SharedKtor_client_coreHttpClientEngine <SharedKotlinx_coroutines_coreCoroutineScope, SharedKtor_ioCloseable>
@required

/**
 * Executes an HTTP request and produces an HTTP response.
 *
 * This function takes [HttpRequestData], which contains all details of the HTTP request,
 * and returns [HttpResponseData] with the server's response, including headers, status code, and body.
 *
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.engine.HttpClientEngine.execute)
 *
 * @param data The [HttpRequestData] representing the request to be executed.
 * @return An [HttpResponseData] object containing the server's response.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)executeData:(SharedKtor_client_coreHttpRequestData *)data completionHandler:(void (^)(SharedKtor_client_coreHttpResponseData * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("execute(data:completionHandler:)")));

/**
 * Installs the engine into an [HttpClient].
 *
 * This method is called when the engine is being set up within an `HttpClient`.
 * Use it to register interceptors, validate configuration, or prepare the engine
 * for use with the client.
 *
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.engine.HttpClientEngine.install)
 *
 * @param client The [HttpClient] instance to which the engine is being installed.
 */
- (void)installClient:(SharedKtor_client_coreHttpClient *)client __attribute__((swift_name("install(client:)")));

/**
 * Provides access to the engine's configuration via [HttpClientEngineConfig].
 *
 * The [config] object stores user-defined parameters or settings that control
 * how the engine operates. When creating a custom engine, this property
 * should return the specific configuration implementation.
 *
 * Example:
 * ```kotlin
 * override val config: HttpClientEngineConfig = CustomEngineConfig()
 * ```
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.engine.HttpClientEngine.config)
 */
@property (readonly) SharedKtor_client_coreHttpClientEngineConfig *config __attribute__((swift_name("config")));

/**
 * Specifies the [CoroutineDispatcher] for I/O operations in the engine.
 *
 * This dispatcher is used for all network-related operations, such as
 * sending requests and receiving responses.
 * By default, it should be optimized for I/O tasks.
 *
 * Example:
 * ```kotlin
 * override val dispatcher: CoroutineDispatcher = Dispatchers.IO
 * ```
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.engine.HttpClientEngine.dispatcher)
 */
@property (readonly) SharedKotlinx_coroutines_coreCoroutineDispatcher *dispatcher __attribute__((swift_name("dispatcher")));

/**
 * Specifies the set of capabilities supported by this HTTP client engine.
 *
 * Capabilities provide a mechanism for plugins and other components to
 * determine whether the engine supports specific features such as timeouts,
 * WebSocket communication, HTTP/2, HTTP/3, or other advanced networking
 * capabilities. This allows seamless integration of features based on the
 * engine's functionality.
 *
 * Each capability is represented as an instance of [HttpClientEngineCapability],
 * which can carry additional metadata or configurations for the capability.
 *
 * Example:
 * ```kotlin
 * override val supportedCapabilities: Set<HttpClientEngineCapability<*>> = setOf(
 *     WebSocketCapability,
 *     Http2Capability,
 *     TimeoutCapability
 * )
 * ```
 *
 * **Usage in Plugins**:
 * Plugins can check if the engine supports a specific capability before
 * applying behavior:
 * ```kotlin
 * if (engine.supportedCapabilities.contains(WebSocketCapability)) {
 *     // Configure WebSocket-specific settings
 * }
 * ```
 *
 * When implementing a custom engine, ensure this property accurately reflects
 * the engine's abilities to avoid unexpected plugin behavior or runtime errors.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.engine.HttpClientEngine.supportedCapabilities)
 */
@property (readonly) NSSet<id<SharedKtor_client_coreHttpClientEngineCapability>> *supportedCapabilities __attribute__((swift_name("supportedCapabilities")));
@end


/**
 * Base configuration for [HttpClientEngine].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.engine.HttpClientEngineConfig)
 */
__attribute__((swift_name("Ktor_client_coreHttpClientEngineConfig")))
@interface SharedKtor_client_coreHttpClientEngineConfig : SharedBase

/**
 * Base configuration for [HttpClientEngine].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.engine.HttpClientEngineConfig)
 */
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));

/**
 * Base configuration for [HttpClientEngine].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.engine.HttpClientEngineConfig)
 */
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * Allow specifying the coroutine dispatcher to use for IO operations.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.engine.HttpClientEngineConfig.dispatcher)
 */
@property SharedKotlinx_coroutines_coreCoroutineDispatcher * _Nullable dispatcher __attribute__((swift_name("dispatcher")));

/**
 * Enables HTTP pipelining advice.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.engine.HttpClientEngineConfig.pipelining)
 */
@property BOOL pipelining __attribute__((swift_name("pipelining")));

/**
 * Specifies a proxy address to use.
 * Uses a system proxy by default.
 *
 * You can learn more from [Proxy](https://ktor.io/docs/proxy.html).
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.engine.HttpClientEngineConfig.proxy)
 */
@property SharedKtor_client_coreProxyConfig * _Nullable proxy __attribute__((swift_name("proxy")));

/**
 * Specifies network threads count advice.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.engine.HttpClientEngineConfig.threadsCount)
 */
@property int32_t threadsCount __attribute__((swift_name("threadsCount"))) __attribute__((unavailable("The [threadsCount] property is deprecated. Consider setting [dispatcher] instead.")));
@end


/**
 * A mutable [HttpClient] configuration used to adjust settings, install plugins and interceptors.
 *
 * This configuration can be provided as a lambda in the [HttpClient] constructor or the [HttpClient.config] builder:
 * ```kotlin
 * val client = HttpClient { // HttpClientConfig<Engine>()
 *     // Configure engine settings
 *     engine { // HttpClientEngineConfig
 *         threadsCount = 4
 *         pipelining = true
 *     }
 *
 *     // Install and configure plugins
 *     install(ContentNegotiation) {
 *         json()
 *     }
 *
 *     // Configure default request parameters
 *     defaultRequest {
 *         url("https://api.example.com")
 *         header("X-Custom-Header", "value")
 *     }
 *
 *     // Configure client-wide settings
 *     expectSuccess = true
 *     followRedirects = true
 * }
 * ```
 * ## Configuring [HttpClientEngine]
 *
 * If the engine is specified explicitly, engine-specific properties will be available in the `engine` block:
 * ```kotlin
 * val client = HttpClient(CIO) { // HttpClientConfig<CIOEngineConfig>.() -> Unit
 *     engine { // CIOEngineConfig.() -> Unit
 *         // engine specific properties
 *     }
 * }
 * ```
 *
 * Learn more about the client's configuration from
 * [Creating and configuring a client](https://ktor.io/docs/create-client.html).
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.HttpClientConfig)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_client_coreHttpClientConfig")))
@interface SharedKtor_client_coreHttpClientConfig<T> : SharedBase

/**
 * A mutable [HttpClient] configuration used to adjust settings, install plugins and interceptors.
 *
 * This configuration can be provided as a lambda in the [HttpClient] constructor or the [HttpClient.config] builder:
 * ```kotlin
 * val client = HttpClient { // HttpClientConfig<Engine>()
 *     // Configure engine settings
 *     engine { // HttpClientEngineConfig
 *         threadsCount = 4
 *         pipelining = true
 *     }
 *
 *     // Install and configure plugins
 *     install(ContentNegotiation) {
 *         json()
 *     }
 *
 *     // Configure default request parameters
 *     defaultRequest {
 *         url("https://api.example.com")
 *         header("X-Custom-Header", "value")
 *     }
 *
 *     // Configure client-wide settings
 *     expectSuccess = true
 *     followRedirects = true
 * }
 * ```
 * ## Configuring [HttpClientEngine]
 *
 * If the engine is specified explicitly, engine-specific properties will be available in the `engine` block:
 * ```kotlin
 * val client = HttpClient(CIO) { // HttpClientConfig<CIOEngineConfig>.() -> Unit
 *     engine { // CIOEngineConfig.() -> Unit
 *         // engine specific properties
 *     }
 * }
 * ```
 *
 * Learn more about the client's configuration from
 * [Creating and configuring a client](https://ktor.io/docs/create-client.html).
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.HttpClientConfig)
 */
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));

/**
 * A mutable [HttpClient] configuration used to adjust settings, install plugins and interceptors.
 *
 * This configuration can be provided as a lambda in the [HttpClient] constructor or the [HttpClient.config] builder:
 * ```kotlin
 * val client = HttpClient { // HttpClientConfig<Engine>()
 *     // Configure engine settings
 *     engine { // HttpClientEngineConfig
 *         threadsCount = 4
 *         pipelining = true
 *     }
 *
 *     // Install and configure plugins
 *     install(ContentNegotiation) {
 *         json()
 *     }
 *
 *     // Configure default request parameters
 *     defaultRequest {
 *         url("https://api.example.com")
 *         header("X-Custom-Header", "value")
 *     }
 *
 *     // Configure client-wide settings
 *     expectSuccess = true
 *     followRedirects = true
 * }
 * ```
 * ## Configuring [HttpClientEngine]
 *
 * If the engine is specified explicitly, engine-specific properties will be available in the `engine` block:
 * ```kotlin
 * val client = HttpClient(CIO) { // HttpClientConfig<CIOEngineConfig>.() -> Unit
 *     engine { // CIOEngineConfig.() -> Unit
 *         // engine specific properties
 *     }
 * }
 * ```
 *
 * Learn more about the client's configuration from
 * [Creating and configuring a client](https://ktor.io/docs/create-client.html).
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.HttpClientConfig)
 */
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * Clones this [HttpClientConfig] by duplicating all the [plugins] and [customInterceptors].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.HttpClientConfig.clone)
 */
- (SharedKtor_client_coreHttpClientConfig<T> *)clone __attribute__((swift_name("clone()")));

/**
 * A builder for configuring engine-specific settings in [HttpClientEngineConfig],
 * such as dispatcher, thread count, proxy, and more.
 *
 * ```kotlin
 * val client = HttpClient(CIO) { // HttpClientConfig<CIOEngineConfig>
 *     engine { // CIOEngineConfig.() -> Unit
 *         proxy = ProxyBuilder.http("proxy.example.com", 8080)
 *     }
 * ```
 *
 * You can learn more from [Engines](https://ktor.io/docs/http-client-engines.html).
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.HttpClientConfig.engine)
 */
- (void)engineBlock:(void (^)(T))block __attribute__((swift_name("engine(block:)")));

/**
 * Applies all the installed [plugins] and [customInterceptors] from this configuration
 * into the specified [client].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.HttpClientConfig.install)
 */
- (void)installClient:(SharedKtor_client_coreHttpClient *)client __attribute__((swift_name("install(client:)")));

/**
 * Installs the specified [plugin] and optionally configures it using the [configure] block.
 *
 * ```kotlin
 * val client = HttpClient {
 *     install(ContentNegotiation) {
 *         // configuration block
 *         json()
 *     }
 * }
 * ```
 *
 * If the plugin is already installed, the configuration block will be applied to the existing configuration class.
 *
 * Learn more from [Plugins](https://ktor.io/docs/http-client-plugins.html).
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.HttpClientConfig.install)
 */
- (void)installPlugin:(id<SharedKtor_client_coreHttpClientPlugin>)plugin configure:(void (^)(id))configure __attribute__((swift_name("install(plugin:configure:)")));

/**
 * Installs an interceptor defined by [block].
 * The [key] parameter is used as a unique name, that also prevents installing duplicated interceptors.
 *
 * If the [key] is already used, the new interceptor will replace the old one.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.HttpClientConfig.install)
 */
- (void)installKey:(NSString *)key block:(void (^)(SharedKtor_client_coreHttpClient *))block __attribute__((swift_name("install(key:block:)")));

/**
 * Installs the plugin from the [other] client's configuration.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.HttpClientConfig.plusAssign)
 */
- (void)plusAssignOther:(SharedKtor_client_coreHttpClientConfig<T> *)other __attribute__((swift_name("plusAssign(other:)")));

/**
 * Development mode is no longer required all functionality is enabled by default. The property is safe to remove.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.HttpClientConfig.developmentMode)
 */
@property BOOL developmentMode __attribute__((swift_name("developmentMode"))) __attribute__((deprecated("Development mode is no longer required. The property will be removed in the future.")));

/**
 * Terminates [HttpClient.receivePipeline] if the status code is not successful (>=300).
 * Learn more from [Response validation](https://ktor.io/docs/response-validation.html).
 *
 * For more details, see the [HttpCallValidator] documentation.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.HttpClientConfig.expectSuccess)
 */
@property BOOL expectSuccess __attribute__((swift_name("expectSuccess")));

/**
 * Specifies whether the client redirects to URLs provided in the `Location` header.
 * You can disable redirections by setting this property to `false`.
 *
 * For an advanced redirection configuration, use the [HttpRedirect] plugin.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.HttpClientConfig.followRedirects)
 */
@property BOOL followRedirects __attribute__((swift_name("followRedirects")));

/**
 * Enables body transformations for many common types like [String], [ByteArray], [ByteReadChannel], etc.
 * These transformations are applied to the request and response bodies.
 *
 * The transformers will be used when the response body is received with a type:
 * ```kotlin
 * val client = HttpClient()
 * val bytes = client.get("https://ktor.io")
 *                   .body<ByteArray>()
 * ```
 *
 * This flag is enabled by default.
 * You might want to disable it if you want to write your own transformers or handle body manually.
 *
 * For more details, see the [defaultTransformers] documentation.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.HttpClientConfig.useDefaultTransformers)
 */
@property BOOL useDefaultTransformers __attribute__((swift_name("useDefaultTransformers")));
@end


/**
 * Represents a capability that an [HttpClientEngine] can support, with [T] representing the type
 * of configuration or metadata associated with the capability.
 *
 * Capabilities are used to declare optional features or behaviors that an engine may support,
 * such as WebSocket communication, HTTP/2, or custom timeouts. They enable plugins and request
 * builders to configure engine-specific functionality by associating a capability with a
 * specific configuration.
 *
 * Capabilities can be set on a per-request basis using the `HttpRequestBuilder.setCapability` method,
 * allowing users to configure engine-specific behavior for individual requests.
 *
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.engine.HttpClientEngineCapability)
 *
 * @param T The type of the configuration or metadata associated with this capability.
 *
 * Example:
 * Suppose you have a custom capability for WebSocket support that requires a specific configuration:
 * ```kotlin
 * object WebSocketCapability : HttpClientEngineCapability<WebSocketConfig>
 *
 * data class WebSocketConfig(val maxFrameSize: Int, val pingIntervalMillis: Long)
 * ```
 *
 * Setting a capability in a request:
 * ```kotlin
 * client.request {
 *     setCapability(WebSocketCapability, WebSocketConfig(
 *         maxFrameSize = 65536,
 *         pingIntervalMillis = 30000
 *     ))
 * }
 * ```
 *
 * Engine Example:
 * A custom engine implementation can declare support for specific capabilities in its `supportedCapabilities` property:
 * ```kotlin
 * override val supportedCapabilities: Set<HttpClientEngineCapability<*>> = setOf(WebSocketCapability)
 * ```
 *
 * Plugin Integration Example:
 * Plugins use capabilities to interact with engine-specific features. For example:
 * ```kotlin
 * if (engine.supportedCapabilities.contains(WebSocketCapability)) {
 *     // Configure WebSocket behavior if supported by the engine
 * }
 * ```
 *
 * When creating a custom capability:
 * - Define a singleton object implementing `HttpClientEngineCapability`.
 * - Use the type parameter [T] to provide the associated configuration type or metadata.
 * - Ensure that engines supporting the capability handle the associated configuration properly.
 */
__attribute__((swift_name("Ktor_client_coreHttpClientEngineCapability")))
@protocol SharedKtor_client_coreHttpClientEngineCapability
@required
@end


/**
 * Map of attributes accessible by [AttributeKey] in a typed manner
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.Attributes)
 */
__attribute__((swift_name("Ktor_utilsAttributes")))
@protocol SharedKtor_utilsAttributes
@required

/**
 * Gets a value of the attribute for the specified [key], or calls supplied [block] to compute its value
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.Attributes.computeIfAbsent)
 */
- (id)computeIfAbsentKey:(SharedKtor_utilsAttributeKey<id> *)key block:(id (^)(void))block __attribute__((swift_name("computeIfAbsent(key:block:)")));

/**
 * Checks if an attribute with the specified [key] exists
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.Attributes.contains)
 */
- (BOOL)containsKey:(SharedKtor_utilsAttributeKey<id> *)key __attribute__((swift_name("contains(key:)")));

/**
 * Gets a value of the attribute for the specified [key], or throws an exception if an attribute doesn't exist
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.Attributes.get)
 */
- (id)getKey_:(SharedKtor_utilsAttributeKey<id> *)key __attribute__((swift_name("get(key_:)")));

/**
 * Gets a value of the attribute for the specified [key], or return `null` if an attribute doesn't exist
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.Attributes.getOrNull)
 */
- (id _Nullable)getOrNullKey:(SharedKtor_utilsAttributeKey<id> *)key __attribute__((swift_name("getOrNull(key:)")));

/**
 * Creates or changes an attribute with the specified [key] using [value]
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.Attributes.put)
 */
- (void)putKey:(SharedKtor_utilsAttributeKey<id> *)key value:(id)value __attribute__((swift_name("put(key:value:)")));

/**
 * Removes an attribute with the specified [key]
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.Attributes.remove)
 */
- (void)removeKey:(SharedKtor_utilsAttributeKey<id> *)key __attribute__((swift_name("remove(key:)")));

/**
 * Creates or changes an attribute with the specified [key] using [value]
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.Attributes.set)
 */
- (void)setKey:(SharedKtor_utilsAttributeKey<id> *)key value:(id)value __attribute__((swift_name("set(key:value:)")));

/**
 * Removes an attribute with the specified [key] and returns its current value, throws an exception if an attribute doesn't exist
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.Attributes.take)
 */
- (id)takeKey:(SharedKtor_utilsAttributeKey<id> *)key __attribute__((swift_name("take(key:)")));

/**
 * Removes an attribute with the specified [key] and returns its current value, returns `null` if an attribute doesn't exist
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.Attributes.takeOrNull)
 */
- (id _Nullable)takeOrNullKey:(SharedKtor_utilsAttributeKey<id> *)key __attribute__((swift_name("takeOrNull(key:)")));

/**
 * Returns [List] of all [AttributeKey] instances in this map
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.Attributes.allKeys)
 */
@property (readonly) NSArray<SharedKtor_utilsAttributeKey<id> *> *allKeys __attribute__((swift_name("allKeys")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_eventsEvents")))
@interface SharedKtor_eventsEvents : SharedBase
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));

/**
 * Raises the event specified by [definition] with the [value] and calls all handlers.
 *
 * Handlers are called in order of subscriptions.
 * If some handler throws an exception, all remaining handlers will still run. The exception will eventually be re-thrown.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.events.Events.raise)
 */
- (void)raiseDefinition:(SharedKtor_eventsEventDefinition<id> *)definition value:(id _Nullable)value __attribute__((swift_name("raise(definition:value:)")));

/**
 * Subscribe [handler] to an event specified by [definition]
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.events.Events.subscribe)
 */
- (id<SharedKotlinx_coroutines_coreDisposableHandle>)subscribeDefinition:(SharedKtor_eventsEventDefinition<id> *)definition handler:(void (^)(id _Nullable))handler __attribute__((swift_name("subscribe(definition:handler:)")));

/**
 * Unsubscribe [handler] from an event specified by [definition]
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.events.Events.unsubscribe)
 */
- (void)unsubscribeDefinition:(SharedKtor_eventsEventDefinition<id> *)definition handler:(void (^)(id _Nullable))handler __attribute__((swift_name("unsubscribe(definition:handler:)")));
@end


/**
 * Represents an execution pipeline for asynchronous extensible computations
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.pipeline.Pipeline)
 */
__attribute__((swift_name("Ktor_utilsPipeline")))
@interface SharedKtor_utilsPipeline<TSubject, TContext> : SharedBase
- (instancetype)initWithPhases:(SharedKotlinArray<SharedKtor_utilsPipelinePhase *> *)phases __attribute__((swift_name("init(phases:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithPhase:(SharedKtor_utilsPipelinePhase *)phase interceptors:(NSArray<id<SharedKotlinSuspendFunction2>> *)interceptors __attribute__((swift_name("init(phase:interceptors:)"))) __attribute__((objc_designated_initializer));

/**
 * Adds [phase] to the end of this pipeline
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.pipeline.Pipeline.addPhase)
 */
- (void)addPhasePhase:(SharedKtor_utilsPipelinePhase *)phase __attribute__((swift_name("addPhase(phase:)")));

/**
 * Invoked after an interceptor has been installed
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.pipeline.Pipeline.afterIntercepted)
 */
- (void)afterIntercepted __attribute__((swift_name("afterIntercepted()")));

/**
 * Executes this pipeline in the given [context] and with the given [subject]
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.pipeline.Pipeline.execute)
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)executeContext:(TContext)context subject:(TSubject)subject completionHandler:(void (^)(TSubject _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("execute(context:subject:completionHandler:)")));

/**
 * Inserts [phase] after the [reference] phase. If there are other phases inserted after [reference], then [phase]
 * will be inserted after them.
 * Example:
 * ```
 * val pipeline = Pipeline<String, String>(a)
 * pipeline.insertPhaseAfter(a, b)
 * pipeline.insertPhaseAfter(a, c)
 * assertEquals(listOf(a, b, c), pipeline.items)
 * ```
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.pipeline.Pipeline.insertPhaseAfter)
 */
- (void)insertPhaseAfterReference:(SharedKtor_utilsPipelinePhase *)reference phase:(SharedKtor_utilsPipelinePhase *)phase __attribute__((swift_name("insertPhaseAfter(reference:phase:)")));

/**
 * Inserts [phase] before the [reference] phase.
 * Example:
 * ```
 * val pipeline = Pipeline<String, String>(c)
 * pipeline.insertPhaseBefore(c, a)
 * pipeline.insertPhaseBefore(c, b)
 * assertEquals(listOf(a, b, c), pipeline.items)
 * ```
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.pipeline.Pipeline.insertPhaseBefore)
 */
- (void)insertPhaseBeforeReference:(SharedKtor_utilsPipelinePhase *)reference phase:(SharedKtor_utilsPipelinePhase *)phase __attribute__((swift_name("insertPhaseBefore(reference:phase:)")));

/**
 * Adds [block] to the [phase] of this pipeline
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.pipeline.Pipeline.intercept)
 */
- (void)interceptPhase:(SharedKtor_utilsPipelinePhase *)phase block:(id<SharedKotlinSuspendFunction2>)block __attribute__((swift_name("intercept(phase:block:)")));
- (NSArray<id<SharedKotlinSuspendFunction2>> *)interceptorsForPhasePhase:(SharedKtor_utilsPipelinePhase *)phase __attribute__((swift_name("interceptorsForPhase(phase:)")));

/**
 * Merges another pipeline into this pipeline, maintaining relative phases order
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.pipeline.Pipeline.merge)
 */
- (void)mergeFrom:(SharedKtor_utilsPipeline<TSubject, TContext> *)from __attribute__((swift_name("merge(from:)")));
- (void)mergePhasesFrom:(SharedKtor_utilsPipeline<TSubject, TContext> *)from __attribute__((swift_name("mergePhases(from:)")));

/**
 * Reset current pipeline from other.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.pipeline.Pipeline.resetFrom)
 */
- (void)resetFromFrom:(SharedKtor_utilsPipeline<TSubject, TContext> *)from __attribute__((swift_name("resetFrom(from:)")));
- (NSString *)description __attribute__((swift_name("description()")));

/**
 * Provides common place to store pipeline attributes
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.pipeline.Pipeline.attributes)
 */
@property (readonly) id<SharedKtor_utilsAttributes> attributes __attribute__((swift_name("attributes")));

/**
 * Indicated if debug mode is enabled. In debug mode users will get more details in the stacktrace.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.pipeline.Pipeline.developmentMode)
 */
@property (readonly) BOOL developmentMode __attribute__((swift_name("developmentMode")));

/**
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.pipeline.Pipeline.isEmpty)
 *
 * @return `true` if there are no interceptors installed regardless number of phases
 */
@property (readonly) BOOL isEmpty __attribute__((swift_name("isEmpty")));

/**
 * Phases of this pipeline
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.pipeline.Pipeline.items)
 */
@property (readonly) NSArray<SharedKtor_utilsPipelinePhase *> *items __attribute__((swift_name("items")));
@end


/**
 * [HttpClient] Pipeline used for receiving [HttpResponse] without any processing.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.statement.HttpReceivePipeline)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_client_coreHttpReceivePipeline")))
@interface SharedKtor_client_coreHttpReceivePipeline : SharedKtor_utilsPipeline<SharedKtor_client_coreHttpResponse *, SharedKotlinUnit *>
- (instancetype)initWithDevelopmentMode:(BOOL)developmentMode __attribute__((swift_name("init(developmentMode:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithPhases:(SharedKotlinArray<SharedKtor_utilsPipelinePhase *> *)phases __attribute__((swift_name("init(phases:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithPhase:(SharedKtor_utilsPipelinePhase *)phase interceptors:(NSArray<id<SharedKotlinSuspendFunction2>> *)interceptors __attribute__((swift_name("init(phase:interceptors:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedKtor_client_coreHttpReceivePipelinePhases *companion __attribute__((swift_name("companion")));
@property (readonly) BOOL developmentMode __attribute__((swift_name("developmentMode")));
@end


/**
 * An [HttpClient]'s pipeline used for executing [HttpRequest].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestPipeline)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_client_coreHttpRequestPipeline")))
@interface SharedKtor_client_coreHttpRequestPipeline : SharedKtor_utilsPipeline<id, SharedKtor_client_coreHttpRequestBuilder *>
- (instancetype)initWithDevelopmentMode:(BOOL)developmentMode __attribute__((swift_name("init(developmentMode:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithPhases:(SharedKotlinArray<SharedKtor_utilsPipelinePhase *> *)phases __attribute__((swift_name("init(phases:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithPhase:(SharedKtor_utilsPipelinePhase *)phase interceptors:(NSArray<id<SharedKotlinSuspendFunction2>> *)interceptors __attribute__((swift_name("init(phase:interceptors:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedKtor_client_coreHttpRequestPipelinePhases *companion __attribute__((swift_name("companion")));
@property (readonly) BOOL developmentMode __attribute__((swift_name("developmentMode")));
@end


/**
 * [HttpClient] Pipeline used for executing [HttpResponse].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.statement.HttpResponsePipeline)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_client_coreHttpResponsePipeline")))
@interface SharedKtor_client_coreHttpResponsePipeline : SharedKtor_utilsPipeline<SharedKtor_client_coreHttpResponseContainer *, SharedKtor_client_coreHttpClientCall *>
- (instancetype)initWithDevelopmentMode:(BOOL)developmentMode __attribute__((swift_name("init(developmentMode:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithPhases:(SharedKotlinArray<SharedKtor_utilsPipelinePhase *> *)phases __attribute__((swift_name("init(phases:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithPhase:(SharedKtor_utilsPipelinePhase *)phase interceptors:(NSArray<id<SharedKotlinSuspendFunction2>> *)interceptors __attribute__((swift_name("init(phase:interceptors:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedKtor_client_coreHttpResponsePipelinePhases *companion __attribute__((swift_name("companion")));
@property (readonly) BOOL developmentMode __attribute__((swift_name("developmentMode")));
@end


/**
 * An [HttpClient]'s pipeline used for sending [HttpRequest] to a remote server.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpSendPipeline)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_client_coreHttpSendPipeline")))
@interface SharedKtor_client_coreHttpSendPipeline : SharedKtor_utilsPipeline<id, SharedKtor_client_coreHttpRequestBuilder *>
- (instancetype)initWithDevelopmentMode:(BOOL)developmentMode __attribute__((swift_name("init(developmentMode:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithPhases:(SharedKotlinArray<SharedKtor_utilsPipelinePhase *> *)phases __attribute__((swift_name("init(phases:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (instancetype)initWithPhase:(SharedKtor_utilsPipelinePhase *)phase interceptors:(NSArray<id<SharedKotlinSuspendFunction2>> *)interceptors __attribute__((swift_name("init(phase:interceptors:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedKtor_client_coreHttpSendPipelinePhases *companion __attribute__((swift_name("companion")));
@property (readonly) BOOL developmentMode __attribute__((swift_name("developmentMode")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinByteArray")))
@interface SharedKotlinByteArray : SharedBase
+ (instancetype)arrayWithSize:(int32_t)size __attribute__((swift_name("init(size:)")));
+ (instancetype)arrayWithSize:(int32_t)size init:(SharedByte *(^)(SharedInt *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (int8_t)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (SharedKotlinByteIterator *)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(int8_t)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((swift_name("KotlinCoroutineContextElement")))
@protocol SharedKotlinCoroutineContextElement <SharedKotlinCoroutineContext>
@required
@property (readonly) id<SharedKotlinCoroutineContextKey> key __attribute__((swift_name("key")));
@end

__attribute__((swift_name("KotlinCoroutineContextKey")))
@protocol SharedKotlinCoroutineContextKey
@required
@end

__attribute__((swift_name("Kotlinx_serialization_coreCompositeEncoder")))
@protocol SharedKotlinx_serialization_coreCompositeEncoder
@required
- (void)encodeBooleanElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(BOOL)value __attribute__((swift_name("encodeBooleanElement(descriptor:index:value:)")));
- (void)encodeByteElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(int8_t)value __attribute__((swift_name("encodeByteElement(descriptor:index:value:)")));
- (void)encodeCharElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(unichar)value __attribute__((swift_name("encodeCharElement(descriptor:index:value:)")));
- (void)encodeDoubleElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(double)value __attribute__((swift_name("encodeDoubleElement(descriptor:index:value:)")));
- (void)encodeFloatElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(float)value __attribute__((swift_name("encodeFloatElement(descriptor:index:value:)")));
- (id<SharedKotlinx_serialization_coreEncoder>)encodeInlineElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("encodeInlineElement(descriptor:index:)")));
- (void)encodeIntElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(int32_t)value __attribute__((swift_name("encodeIntElement(descriptor:index:value:)")));
- (void)encodeLongElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(int64_t)value __attribute__((swift_name("encodeLongElement(descriptor:index:value:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)encodeNullableSerializableElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index serializer:(id<SharedKotlinx_serialization_coreSerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeNullableSerializableElement(descriptor:index:serializer:value:)")));
- (void)encodeSerializableElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index serializer:(id<SharedKotlinx_serialization_coreSerializationStrategy>)serializer value:(id _Nullable)value __attribute__((swift_name("encodeSerializableElement(descriptor:index:serializer:value:)")));
- (void)encodeShortElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(int16_t)value __attribute__((swift_name("encodeShortElement(descriptor:index:value:)")));
- (void)encodeStringElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index value:(NSString *)value __attribute__((swift_name("encodeStringElement(descriptor:index:value:)")));
- (void)endStructureDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("endStructure(descriptor:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (BOOL)shouldEncodeElementDefaultDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("shouldEncodeElementDefault(descriptor:index:)")));
@property (readonly) SharedKotlinx_serialization_coreSerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreSerializersModule")))
@interface SharedKotlinx_serialization_coreSerializersModule : SharedBase

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (void)dumpToCollector:(id<SharedKotlinx_serialization_coreSerializersModuleCollector>)collector __attribute__((swift_name("dumpTo(collector:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<SharedKotlinx_serialization_coreKSerializer> _Nullable)getContextualKClass:(id<SharedKotlinKClass>)kClass typeArgumentsSerializers:(NSArray<id<SharedKotlinx_serialization_coreKSerializer>> *)typeArgumentsSerializers __attribute__((swift_name("getContextual(kClass:typeArgumentsSerializers:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<SharedKotlinx_serialization_coreSerializationStrategy> _Nullable)getPolymorphicBaseClass:(id<SharedKotlinKClass>)baseClass value:(id)value __attribute__((swift_name("getPolymorphic(baseClass:value:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id<SharedKotlinx_serialization_coreDeserializationStrategy> _Nullable)getPolymorphicBaseClass:(id<SharedKotlinKClass>)baseClass serializedClassName:(NSString * _Nullable)serializedClassName __attribute__((swift_name("getPolymorphic(baseClass:serializedClassName:)")));
@end

__attribute__((swift_name("KotlinAnnotation")))
@protocol SharedKotlinAnnotation
@required
@end

__attribute__((swift_name("Kotlinx_serialization_coreSerialKind")))
@interface SharedKotlinx_serialization_coreSerialKind : SharedBase
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@end

__attribute__((swift_name("Kotlinx_serialization_coreCompositeDecoder")))
@protocol SharedKotlinx_serialization_coreCompositeDecoder
@required
- (BOOL)decodeBooleanElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeBooleanElement(descriptor:index:)")));
- (int8_t)decodeByteElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeByteElement(descriptor:index:)")));
- (unichar)decodeCharElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeCharElement(descriptor:index:)")));
- (int32_t)decodeCollectionSizeDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("decodeCollectionSize(descriptor:)")));
- (double)decodeDoubleElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeDoubleElement(descriptor:index:)")));
- (int32_t)decodeElementIndexDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("decodeElementIndex(descriptor:)")));
- (float)decodeFloatElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeFloatElement(descriptor:index:)")));
- (id<SharedKotlinx_serialization_coreDecoder>)decodeInlineElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeInlineElement(descriptor:index:)")));
- (int32_t)decodeIntElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeIntElement(descriptor:index:)")));
- (int64_t)decodeLongElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeLongElement(descriptor:index:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (id _Nullable)decodeNullableSerializableElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index deserializer:(id<SharedKotlinx_serialization_coreDeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeNullableSerializableElement(descriptor:index:deserializer:previousValue:)")));

/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
- (BOOL)decodeSequentially __attribute__((swift_name("decodeSequentially()")));
- (id _Nullable)decodeSerializableElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index deserializer:(id<SharedKotlinx_serialization_coreDeserializationStrategy>)deserializer previousValue:(id _Nullable)previousValue __attribute__((swift_name("decodeSerializableElement(descriptor:index:deserializer:previousValue:)")));
- (int16_t)decodeShortElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeShortElement(descriptor:index:)")));
- (NSString *)decodeStringElementDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor index:(int32_t)index __attribute__((swift_name("decodeStringElement(descriptor:index:)")));
- (void)endStructureDescriptor:(id<SharedKotlinx_serialization_coreSerialDescriptor>)descriptor __attribute__((swift_name("endStructure(descriptor:)")));
@property (readonly) SharedKotlinx_serialization_coreSerializersModule *serializersModule __attribute__((swift_name("serializersModule")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinNothing")))
@interface SharedKotlinNothing : SharedBase
@end


/**
 * Actual data of the [HttpRequest], including [url], [method], [headers], [body] and [executionContext].
 * Built by [HttpRequestBuilder].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestData)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_client_coreHttpRequestData")))
@interface SharedKtor_client_coreHttpRequestData : SharedBase
- (instancetype)initWithUrl:(SharedKtor_httpUrl *)url method:(SharedKtor_httpHttpMethod *)method headers:(id<SharedKtor_httpHeaders>)headers body:(SharedKtor_httpOutgoingContent *)body executionContext:(id<SharedKotlinx_coroutines_coreJob>)executionContext attributes:(id<SharedKtor_utilsAttributes>)attributes __attribute__((swift_name("init(url:method:headers:body:executionContext:attributes:)"))) __attribute__((objc_designated_initializer));

/**
 * Retrieve extension by its key.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestData.getCapabilityOrNull)
 */
- (id _Nullable)getCapabilityOrNullKey:(id<SharedKtor_client_coreHttpClientEngineCapability>)key __attribute__((swift_name("getCapabilityOrNull(key:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) id<SharedKtor_utilsAttributes> attributes __attribute__((swift_name("attributes")));
@property (readonly) SharedKtor_httpOutgoingContent *body __attribute__((swift_name("body")));
@property (readonly) id<SharedKotlinx_coroutines_coreJob> executionContext __attribute__((swift_name("executionContext")));
@property (readonly) id<SharedKtor_httpHeaders> headers __attribute__((swift_name("headers")));
@property (readonly) SharedKtor_httpHttpMethod *method __attribute__((swift_name("method")));
@property (readonly) SharedKtor_httpUrl *url __attribute__((swift_name("url")));
@end


/**
 * Data prepared for [HttpResponse].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpResponseData)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_client_coreHttpResponseData")))
@interface SharedKtor_client_coreHttpResponseData : SharedBase
- (instancetype)initWithStatusCode:(SharedKtor_httpHttpStatusCode *)statusCode requestTime:(SharedKtor_utilsGMTDate *)requestTime headers:(id<SharedKtor_httpHeaders>)headers version:(SharedKtor_httpHttpProtocolVersion *)version body:(id)body callContext:(id<SharedKotlinCoroutineContext>)callContext __attribute__((swift_name("init(statusCode:requestTime:headers:version:body:callContext:)"))) __attribute__((objc_designated_initializer));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) id body __attribute__((swift_name("body")));
@property (readonly) id<SharedKotlinCoroutineContext> callContext __attribute__((swift_name("callContext")));
@property (readonly) id<SharedKtor_httpHeaders> headers __attribute__((swift_name("headers")));
@property (readonly) SharedKtor_utilsGMTDate *requestTime __attribute__((swift_name("requestTime")));
@property (readonly) SharedKtor_utilsGMTDate *responseTime __attribute__((swift_name("responseTime")));
@property (readonly) SharedKtor_httpHttpStatusCode *statusCode __attribute__((swift_name("statusCode")));
@property (readonly) SharedKtor_httpHttpProtocolVersion *version __attribute__((swift_name("version")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
__attribute__((swift_name("KotlinAbstractCoroutineContextElement")))
@interface SharedKotlinAbstractCoroutineContextElement : SharedBase <SharedKotlinCoroutineContextElement>
- (instancetype)initWithKey:(id<SharedKotlinCoroutineContextKey>)key __attribute__((swift_name("init(key:)"))) __attribute__((objc_designated_initializer));
@property (readonly) id<SharedKotlinCoroutineContextKey> key __attribute__((swift_name("key")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
__attribute__((swift_name("KotlinContinuationInterceptor")))
@protocol SharedKotlinContinuationInterceptor <SharedKotlinCoroutineContextElement>
@required
- (id<SharedKotlinContinuation>)interceptContinuationContinuation:(id<SharedKotlinContinuation>)continuation __attribute__((swift_name("interceptContinuation(continuation:)")));
- (void)releaseInterceptedContinuationContinuation:(id<SharedKotlinContinuation>)continuation __attribute__((swift_name("releaseInterceptedContinuation(continuation:)")));
@end

__attribute__((swift_name("Kotlinx_coroutines_coreCoroutineDispatcher")))
@interface SharedKotlinx_coroutines_coreCoroutineDispatcher : SharedKotlinAbstractCoroutineContextElement <SharedKotlinContinuationInterceptor>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (instancetype)initWithKey:(id<SharedKotlinCoroutineContextKey>)key __attribute__((swift_name("init(key:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedKotlinx_coroutines_coreCoroutineDispatcherKey *companion __attribute__((swift_name("companion")));
- (void)dispatchContext:(id<SharedKotlinCoroutineContext>)context block:(id<SharedKotlinx_coroutines_coreRunnable>)block __attribute__((swift_name("dispatch(context:block:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
- (void)dispatchYieldContext:(id<SharedKotlinCoroutineContext>)context block:(id<SharedKotlinx_coroutines_coreRunnable>)block __attribute__((swift_name("dispatchYield(context:block:)")));
- (id<SharedKotlinContinuation>)interceptContinuationContinuation:(id<SharedKotlinContinuation>)continuation __attribute__((swift_name("interceptContinuation(continuation:)")));
- (BOOL)isDispatchNeededContext:(id<SharedKotlinCoroutineContext>)context __attribute__((swift_name("isDispatchNeeded(context:)")));
- (SharedKotlinx_coroutines_coreCoroutineDispatcher *)limitedParallelismParallelism:(int32_t)parallelism name:(NSString * _Nullable)name __attribute__((swift_name("limitedParallelism(parallelism:name:)")));
- (SharedKotlinx_coroutines_coreCoroutineDispatcher *)plusOther:(SharedKotlinx_coroutines_coreCoroutineDispatcher *)other __attribute__((swift_name("plus(other:)"))) __attribute__((unavailable("Operator '+' on two CoroutineDispatcher objects is meaningless. CoroutineDispatcher is a coroutine context element and `+` is a set-sum operator for coroutine contexts. The dispatcher to the right of `+` just replaces the dispatcher to the left.")));
- (void)releaseInterceptedContinuationContinuation:(id<SharedKotlinContinuation>)continuation __attribute__((swift_name("releaseInterceptedContinuation(continuation:)")));
- (NSString *)description __attribute__((swift_name("description()")));
@end


/**
 * Proxy configuration.
 *
 * See [ProxyBuilder] to create proxy.
 *
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.engine.ProxyConfig)
 *
 * @param url: proxy url address.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_client_coreProxyConfig")))
@interface SharedKtor_client_coreProxyConfig : SharedBase
- (instancetype)initWithUrl:(SharedKtor_httpUrl *)url __attribute__((swift_name("init(url:)"))) __attribute__((objc_designated_initializer));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedKtor_httpUrl *url __attribute__((swift_name("url")));
@end


/**
 * Base interface representing a [HttpClient] plugin.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.plugins.HttpClientPlugin)
 */
__attribute__((swift_name("Ktor_client_coreHttpClientPlugin")))
@protocol SharedKtor_client_coreHttpClientPlugin
@required

/**
 * Installs the [plugin] class for a [HttpClient] defined at [scope].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.plugins.HttpClientPlugin.install)
 */
- (void)installPlugin:(id)plugin scope:(SharedKtor_client_coreHttpClient *)scope __attribute__((swift_name("install(plugin:scope:)")));

/**
 * Builds a [TPlugin] by calling the [block] with a [TConfig] config instance as receiver.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.plugins.HttpClientPlugin.prepare)
 */
- (id)prepareBlock:(void (^)(id))block __attribute__((swift_name("prepare(block:)")));

/**
 * The [AttributeKey] for this plugin.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.plugins.HttpClientPlugin.key)
 */
@property (readonly) SharedKtor_utilsAttributeKey<id> *key __attribute__((swift_name("key")));
@end


/**
 * Specifies a key for an attribute in [Attributes]
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.AttributeKey)
 *
 * @param T is a type of the value stored in the attribute
 * @property name is a name of the attribute for diagnostic purposes. Can't be blank
 * @property type the recorded kotlin type of T
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_utilsAttributeKey")))
@interface SharedKtor_utilsAttributeKey<T> : SharedBase

/**
 * @note annotations
 *   kotlin.jvm.JvmOverloads
*/
- (instancetype)initWithName:(NSString *)name type:(SharedKtor_utilsTypeInfo *)type __attribute__((swift_name("init(name:type:)"))) __attribute__((objc_designated_initializer));
- (SharedKtor_utilsAttributeKey<T> *)doCopyName:(NSString *)name type:(SharedKtor_utilsTypeInfo *)type __attribute__((swift_name("doCopy(name:type:)")));

/**
 * Specifies a key for an attribute in [Attributes]
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.AttributeKey)
 *
 * @param T is a type of the value stored in the attribute
 * @property name is a name of the attribute for diagnostic purposes. Can't be blank
 * @property type the recorded kotlin type of T
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Specifies a key for an attribute in [Attributes]
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.AttributeKey)
 *
 * @param T is a type of the value stored in the attribute
 * @property name is a name of the attribute for diagnostic purposes. Can't be blank
 * @property type the recorded kotlin type of T
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@end


/**
 * Definition of an event.
 * Event is used as a key so both [hashCode] and [equals] need to be implemented properly.
 * Inheriting of this class is an experimental feature.
 * Instantiate directly if inheritance not necessary.
 *
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.events.EventDefinition)
 *
 * @param T specifies what is a type of value passed to the event
 */
__attribute__((swift_name("Ktor_eventsEventDefinition")))
@interface SharedKtor_eventsEventDefinition<T> : SharedBase

/**
 * Definition of an event.
 * Event is used as a key so both [hashCode] and [equals] need to be implemented properly.
 * Inheriting of this class is an experimental feature.
 * Instantiate directly if inheritance not necessary.
 *
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.events.EventDefinition)
 *
 * @param T specifies what is a type of value passed to the event
 */
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));

/**
 * Definition of an event.
 * Event is used as a key so both [hashCode] and [equals] need to be implemented properly.
 * Inheriting of this class is an experimental feature.
 * Instantiate directly if inheritance not necessary.
 *
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.events.EventDefinition)
 *
 * @param T specifies what is a type of value passed to the event
 */
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@end

__attribute__((swift_name("Kotlinx_coroutines_coreDisposableHandle")))
@protocol SharedKotlinx_coroutines_coreDisposableHandle
@required
- (void)dispose __attribute__((swift_name("dispose()")));
@end


/**
 * Represents a phase in a pipeline
 *
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.pipeline.PipelinePhase)
 *
 * @param name a name for this phase
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_utilsPipelinePhase")))
@interface SharedKtor_utilsPipelinePhase : SharedBase
- (instancetype)initWithName:(NSString *)name __attribute__((swift_name("init(name:)"))) __attribute__((objc_designated_initializer));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@end

__attribute__((swift_name("KotlinSuspendFunction2")))
@protocol SharedKotlinSuspendFunction2 <SharedKotlinFunction>
@required

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)invokeP1:(id _Nullable)p1 p2:(id _Nullable)p2 completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("invoke(p1:p2:completionHandler:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_client_coreHttpReceivePipeline.Phases")))
@interface SharedKtor_client_coreHttpReceivePipelinePhases : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)phases __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKtor_client_coreHttpReceivePipelinePhases *shared __attribute__((swift_name("shared")));

/**
 * Latest response pipeline phase
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.statement.HttpReceivePipeline.Phases.After)
 */
@property (readonly) SharedKtor_utilsPipelinePhase *After __attribute__((swift_name("After")));

/**
 * The earliest phase that happens before any other
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.statement.HttpReceivePipeline.Phases.Before)
 */
@property (readonly) SharedKtor_utilsPipelinePhase *Before __attribute__((swift_name("Before")));

/**
 * Use this phase to store request shared state
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.statement.HttpReceivePipeline.Phases.State)
 */
@property (readonly) SharedKtor_utilsPipelinePhase *State __attribute__((swift_name("State")));
@end


/**
 * A message either from the client or the server,
 * that has [headers] associated.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HttpMessage)
 */
__attribute__((swift_name("Ktor_httpHttpMessage")))
@protocol SharedKtor_httpHttpMessage
@required

/**
 * Message [Headers]
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HttpMessage.headers)
 */
@property (readonly) id<SharedKtor_httpHeaders> headers __attribute__((swift_name("headers")));
@end


/**
 * An [HttpClient]'s response, a second part of [HttpClientCall].
 *
 * Learn more from [Receiving responses](https://ktor.io/docs/response.html).
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.statement.HttpResponse)
 */
__attribute__((swift_name("Ktor_client_coreHttpResponse")))
@interface SharedKtor_client_coreHttpResponse : SharedBase <SharedKtor_httpHttpMessage, SharedKotlinx_coroutines_coreCoroutineScope>

/**
 * An [HttpClient]'s response, a second part of [HttpClientCall].
 *
 * Learn more from [Receiving responses](https://ktor.io/docs/response.html).
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.statement.HttpResponse)
 */
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));

/**
 * An [HttpClient]'s response, a second part of [HttpClientCall].
 *
 * Learn more from [Receiving responses](https://ktor.io/docs/response.html).
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.statement.HttpResponse)
 */
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (NSString *)description __attribute__((swift_name("description()")));

/**
 * The associated [HttpClientCall] containing both
 * the underlying [HttpClientCall.request] and [HttpClientCall.response].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.statement.HttpResponse.call)
 */
@property (readonly) SharedKtor_client_coreHttpClientCall *call __attribute__((swift_name("call")));

/**
 * Provides a raw [ByteReadChannel] to the response content as it is read from the network.
 * This content can be still compressed or encoded.
 *
 * This content doesn't go through any interceptors from [HttpResponsePipeline].
 *
 * If you need to read the content as decoded bytes, use the [bodyAsChannel] method instead.
 *
 * This property produces a new channel every time it's accessed.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.statement.HttpResponse.rawContent)
 */
@property (readonly) id<SharedKtor_ioByteReadChannel> rawContent __attribute__((swift_name("rawContent")));

/**
 * [GMTDate] of the request start.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.statement.HttpResponse.requestTime)
 */
@property (readonly) SharedKtor_utilsGMTDate *requestTime __attribute__((swift_name("requestTime")));

/**
 * [GMTDate] of the response start.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.statement.HttpResponse.responseTime)
 */
@property (readonly) SharedKtor_utilsGMTDate *responseTime __attribute__((swift_name("responseTime")));

/**
 * The [HttpStatusCode] returned by the server. It includes both,
 * the [HttpStatusCode.description] and the [HttpStatusCode.value] (code).
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.statement.HttpResponse.status)
 */
@property (readonly) SharedKtor_httpHttpStatusCode *status __attribute__((swift_name("status")));

/**
 * HTTP version. Usually [HttpProtocolVersion.HTTP_1_1] or [HttpProtocolVersion.HTTP_2_0].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.statement.HttpResponse.version)
 */
@property (readonly) SharedKtor_httpHttpProtocolVersion *version_ __attribute__((swift_name("version_")));
@end


/**
 * All interceptors accept payload as [subject] and try to convert it to [OutgoingContent].
 * Last phase should proceed with [HttpClientCall].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestPipeline.Phases)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_client_coreHttpRequestPipeline.Phases")))
@interface SharedKtor_client_coreHttpRequestPipelinePhases : SharedBase
+ (instancetype)alloc __attribute__((unavailable));

/**
 * All interceptors accept payload as [subject] and try to convert it to [OutgoingContent].
 * Last phase should proceed with [HttpClientCall].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestPipeline.Phases)
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)phases __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKtor_client_coreHttpRequestPipelinePhases *shared __attribute__((swift_name("shared")));

/**
 * The earliest phase that happens before any other.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestPipeline.Phases.Before)
 */
@property (readonly) SharedKtor_utilsPipelinePhase *Before __attribute__((swift_name("Before")));

/**
 * Encode a request body to [OutgoingContent].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestPipeline.Phases.Render)
 */
@property (readonly) SharedKtor_utilsPipelinePhase *Render __attribute__((swift_name("Render")));

/**
 * A phase for the [HttpSend] plugin.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestPipeline.Phases.Send)
 */
@property (readonly) SharedKtor_utilsPipelinePhase *Send __attribute__((swift_name("Send")));

/**
 * Use this phase to modify a request with a shared state.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestPipeline.Phases.State)
 */
@property (readonly) SharedKtor_utilsPipelinePhase *State __attribute__((swift_name("State")));

/**
 * Transform a request body to supported render format.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestPipeline.Phases.Transform)
 */
@property (readonly) SharedKtor_utilsPipelinePhase *Transform __attribute__((swift_name("Transform")));
@end


/**
 * A builder message either for the client or the server,
 * that has a [headers] builder associated.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HttpMessageBuilder)
 */
__attribute__((swift_name("Ktor_httpHttpMessageBuilder")))
@protocol SharedKtor_httpHttpMessageBuilder
@required

/**
 * MessageBuilder [HeadersBuilder]
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HttpMessageBuilder.headers)
 */
@property (readonly) SharedKtor_httpHeadersBuilder *headers __attribute__((swift_name("headers")));
@end


/**
 * Contains parameters used to make an HTTP request.
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestBuilder)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_client_coreHttpRequestBuilder")))
@interface SharedKtor_client_coreHttpRequestBuilder : SharedBase <SharedKtor_httpHttpMessageBuilder>

/**
 * Contains parameters used to make an HTTP request.
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestBuilder)
 */
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));

/**
 * Contains parameters used to make an HTTP request.
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestBuilder)
 */
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
@property (class, readonly, getter=companion) SharedKtor_client_coreHttpRequestBuilderCompanion *companion __attribute__((swift_name("companion")));

/**
 * Creates immutable [HttpRequestData].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestBuilder.build)
 */
- (SharedKtor_client_coreHttpRequestData *)build __attribute__((swift_name("build()")));

/**
 * Retrieves capability by the key.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestBuilder.getCapabilityOrNull)
 */
- (id _Nullable)getCapabilityOrNullKey:(id<SharedKtor_client_coreHttpClientEngineCapability>)key __attribute__((swift_name("getCapabilityOrNull(key:)")));

/**
 * Sets request-specific attributes specified by [block].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestBuilder.setAttributes)
 */
- (void)setAttributesBlock:(void (^)(id<SharedKtor_utilsAttributes>))block __attribute__((swift_name("setAttributes(block:)")));

/**
 * Sets capability configuration.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestBuilder.setCapability)
 */
- (void)setCapabilityKey:(id<SharedKtor_client_coreHttpClientEngineCapability>)key capability:(id)capability __attribute__((swift_name("setCapability(key:capability:)")));

/**
 * Mutates [this] by copying all the data but execution context from another [builder] using it as the base.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestBuilder.takeFrom)
 */
- (SharedKtor_client_coreHttpRequestBuilder *)takeFromBuilder:(SharedKtor_client_coreHttpRequestBuilder *)builder __attribute__((swift_name("takeFrom(builder:)")));

/**
 * Mutates [this] copying all the data from another [builder] using it as the base.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestBuilder.takeFromWithExecutionContext)
 */
- (SharedKtor_client_coreHttpRequestBuilder *)takeFromWithExecutionContextBuilder:(SharedKtor_client_coreHttpRequestBuilder *)builder __attribute__((swift_name("takeFromWithExecutionContext(builder:)")));

/**
 * Executes a [block] that configures the [URLBuilder] associated to this request.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestBuilder.url)
 */
- (void)urlBlock:(void (^)(SharedKtor_httpURLBuilder *, SharedKtor_httpURLBuilder *))block __attribute__((swift_name("url(block:)")));

/**
 * Provides access to attributes specific for this request.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestBuilder.attributes)
 */
@property (readonly) id<SharedKtor_utilsAttributes> attributes __attribute__((swift_name("attributes")));

/**
 * The [body] for this request. Initially [EmptyContent].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestBuilder.body)
 */
@property id body __attribute__((swift_name("body")));

/**
 * The [KType] of [body] for this request. Null for default types that don't need serialization.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestBuilder.bodyType)
 */
@property SharedKtor_utilsTypeInfo * _Nullable bodyType __attribute__((swift_name("bodyType")));

/**
 * A deferred used to control the execution of this request.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestBuilder.executionContext)
 */
@property (readonly) id<SharedKotlinx_coroutines_coreJob> executionContext __attribute__((swift_name("executionContext")));

/**
 * [HeadersBuilder] to configure the headers for this request.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestBuilder.headers)
 */
@property (readonly) SharedKtor_httpHeadersBuilder *headers __attribute__((swift_name("headers")));

/**
 * [HttpMethod] used by this request. [HttpMethod.Get] by default.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestBuilder.method)
 */
@property SharedKtor_httpHttpMethod *method __attribute__((swift_name("method")));

/**
 * [URLBuilder] to configure the URL for this request.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequestBuilder.url)
 */
@property (readonly) SharedKtor_httpURLBuilder *url __attribute__((swift_name("url")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_client_coreHttpResponsePipeline.Phases")))
@interface SharedKtor_client_coreHttpResponsePipelinePhases : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)phases __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKtor_client_coreHttpResponsePipelinePhases *shared __attribute__((swift_name("shared")));

/**
 * Latest response pipeline phase
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.statement.HttpResponsePipeline.Phases.After)
 */
@property (readonly) SharedKtor_utilsPipelinePhase *After __attribute__((swift_name("After")));

/**
 * Decode response body
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.statement.HttpResponsePipeline.Phases.Parse)
 */
@property (readonly) SharedKtor_utilsPipelinePhase *Parse __attribute__((swift_name("Parse")));

/**
 * The earliest phase that happens before any other
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.statement.HttpResponsePipeline.Phases.Receive)
 */
@property (readonly) SharedKtor_utilsPipelinePhase *Receive __attribute__((swift_name("Receive")));

/**
 * Use this phase to store request shared state
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.statement.HttpResponsePipeline.Phases.State)
 */
@property (readonly) SharedKtor_utilsPipelinePhase *State __attribute__((swift_name("State")));

/**
 * Transform response body to expected format
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.statement.HttpResponsePipeline.Phases.Transform)
 */
@property (readonly) SharedKtor_utilsPipelinePhase *Transform __attribute__((swift_name("Transform")));
@end


/**
 * Class representing a typed [response] with an attached [expectedType].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.statement.HttpResponseContainer)
 *
 * @param expectedType: information about expected type.
 * @param response: current response state.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_client_coreHttpResponseContainer")))
@interface SharedKtor_client_coreHttpResponseContainer : SharedBase
- (instancetype)initWithExpectedType:(SharedKtor_utilsTypeInfo *)expectedType response:(id)response __attribute__((swift_name("init(expectedType:response:)"))) __attribute__((objc_designated_initializer));
- (SharedKtor_client_coreHttpResponseContainer *)doCopyExpectedType:(SharedKtor_utilsTypeInfo *)expectedType response:(id)response __attribute__((swift_name("doCopy(expectedType:response:)")));

/**
 * Class representing a typed [response] with an attached [expectedType].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.statement.HttpResponseContainer)
 *
 * @param expectedType: information about expected type.
 * @param response: current response state.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Class representing a typed [response] with an attached [expectedType].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.statement.HttpResponseContainer)
 *
 * @param expectedType: information about expected type.
 * @param response: current response state.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Class representing a typed [response] with an attached [expectedType].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.statement.HttpResponseContainer)
 *
 * @param expectedType: information about expected type.
 * @param response: current response state.
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) SharedKtor_utilsTypeInfo *expectedType __attribute__((swift_name("expectedType")));
@property (readonly) id response __attribute__((swift_name("response")));
@end


/**
 * A pair of a [request] and [response] for a specific [HttpClient].
 *
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.call.HttpClientCall)
 *
 * @property client the client that executed the call.
 */
__attribute__((swift_name("Ktor_client_coreHttpClientCall")))
@interface SharedKtor_client_coreHttpClientCall : SharedBase <SharedKotlinx_coroutines_coreCoroutineScope>
- (instancetype)initWithClient:(SharedKtor_client_coreHttpClient *)client __attribute__((swift_name("init(client:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithClient:(SharedKtor_client_coreHttpClient *)client requestData:(SharedKtor_client_coreHttpRequestData *)requestData responseData:(SharedKtor_client_coreHttpResponseData *)responseData __attribute__((swift_name("init(client:requestData:responseData:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKtor_client_coreHttpClientCallCompanion *companion __attribute__((swift_name("companion")));

/**
 * Tries to receive the payload of the [response] as a specific expected type provided in [info].
 * Returns [response] if [info] corresponds to [HttpResponse].
 *
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.call.HttpClientCall.body)
 *
 * @throws NoTransformationFoundException If no transformation is found for the type [info].
 * @throws DoubleReceiveException If already called [body].
 * @throws NullPointerException If content is `null`.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)bodyInfo:(SharedKtor_utilsTypeInfo *)info completionHandler:(void (^)(id _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("body(info:completionHandler:)")));

/**
 * Tries to receive the payload of the [response] as a specific expected type provided in [info].
 * Returns [response] if [info] corresponds to [HttpResponse].
 *
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.call.HttpClientCall.bodyNullable)
 *
 * @throws NoTransformationFoundException If no transformation is found for the type [info].
 * @throws DoubleReceiveException If already called [body].
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)bodyNullableInfo:(SharedKtor_utilsTypeInfo *)info completionHandler:(void (^)(id _Nullable_result, NSError * _Nullable))completionHandler __attribute__((swift_name("bodyNullable(info:completionHandler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)getResponseContentWithCompletionHandler:(void (^)(id<SharedKtor_ioByteReadChannel> _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("getResponseContent(completionHandler:)")));
- (NSString *)description __attribute__((swift_name("description()")));

/**
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property (readonly) BOOL allowDoubleReceive __attribute__((swift_name("allowDoubleReceive")));

/**
 * Typed [Attributes] associated to this call serving as a lightweight container.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.call.HttpClientCall.attributes)
 */
@property (readonly) id<SharedKtor_utilsAttributes> attributes __attribute__((swift_name("attributes")));
@property (readonly) SharedKtor_client_coreHttpClient *client __attribute__((swift_name("client")));
@property (readonly) id<SharedKotlinCoroutineContext> coroutineContext __attribute__((swift_name("coroutineContext")));

/**
 * The [request] sent by the client.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.call.HttpClientCall.request)
 */
@property id<SharedKtor_client_coreHttpRequest> request __attribute__((swift_name("request")));

/**
 * The [response] sent by the server.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.call.HttpClientCall.response)
 */
@property SharedKtor_client_coreHttpResponse *response __attribute__((swift_name("response")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_client_coreHttpSendPipeline.Phases")))
@interface SharedKtor_client_coreHttpSendPipelinePhases : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)phases __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKtor_client_coreHttpSendPipelinePhases *shared __attribute__((swift_name("shared")));

/**
 * The earliest phase that happens before any other.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpSendPipeline.Phases.Before)
 */
@property (readonly) SharedKtor_utilsPipelinePhase *Before __attribute__((swift_name("Before")));

/**
 * Send a request to a remote server.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpSendPipeline.Phases.Engine)
 */
@property (readonly) SharedKtor_utilsPipelinePhase *Engine __attribute__((swift_name("Engine")));

/**
 * Use this phase for logging and other actions that don't modify a request or shared data.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpSendPipeline.Phases.Monitoring)
 */
@property (readonly) SharedKtor_utilsPipelinePhase *Monitoring __attribute__((swift_name("Monitoring")));

/**
 * Receive a pipeline execution phase.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpSendPipeline.Phases.Receive)
 */
@property (readonly) SharedKtor_utilsPipelinePhase *Receive __attribute__((swift_name("Receive")));

/**
 * Use this phase to modify request with a shared state.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpSendPipeline.Phases.State)
 */
@property (readonly) SharedKtor_utilsPipelinePhase *State __attribute__((swift_name("State")));
@end

__attribute__((swift_name("KotlinByteIterator")))
@interface SharedKotlinByteIterator : SharedBase <SharedKotlinIterator>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (SharedByte *)next __attribute__((swift_name("next()")));
- (int8_t)nextByte __attribute__((swift_name("nextByte()")));
@end


/**
 * @note annotations
 *   kotlinx.serialization.ExperimentalSerializationApi
*/
__attribute__((swift_name("Kotlinx_serialization_coreSerializersModuleCollector")))
@protocol SharedKotlinx_serialization_coreSerializersModuleCollector
@required
- (void)contextualKClass:(id<SharedKotlinKClass>)kClass provider:(id<SharedKotlinx_serialization_coreKSerializer> (^)(NSArray<id<SharedKotlinx_serialization_coreKSerializer>> *))provider __attribute__((swift_name("contextual(kClass:provider:)")));
- (void)contextualKClass:(id<SharedKotlinKClass>)kClass serializer:(id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("contextual(kClass:serializer:)")));
- (void)polymorphicBaseClass:(id<SharedKotlinKClass>)baseClass actualClass:(id<SharedKotlinKClass>)actualClass actualSerializer:(id<SharedKotlinx_serialization_coreKSerializer>)actualSerializer __attribute__((swift_name("polymorphic(baseClass:actualClass:actualSerializer:)")));
- (void)polymorphicDefaultBaseClass:(id<SharedKotlinKClass>)baseClass defaultDeserializerProvider:(id<SharedKotlinx_serialization_coreDeserializationStrategy> _Nullable (^)(NSString * _Nullable))defaultDeserializerProvider __attribute__((swift_name("polymorphicDefault(baseClass:defaultDeserializerProvider:)"))) __attribute__((deprecated("Deprecated in favor of function with more precise name: polymorphicDefaultDeserializer")));
- (void)polymorphicDefaultDeserializerBaseClass:(id<SharedKotlinKClass>)baseClass defaultDeserializerProvider:(id<SharedKotlinx_serialization_coreDeserializationStrategy> _Nullable (^)(NSString * _Nullable))defaultDeserializerProvider __attribute__((swift_name("polymorphicDefaultDeserializer(baseClass:defaultDeserializerProvider:)")));
- (void)polymorphicDefaultSerializerBaseClass:(id<SharedKotlinKClass>)baseClass defaultSerializerProvider:(id<SharedKotlinx_serialization_coreSerializationStrategy> _Nullable (^)(id))defaultSerializerProvider __attribute__((swift_name("polymorphicDefaultSerializer(baseClass:defaultSerializerProvider:)")));
@end

__attribute__((swift_name("KotlinKDeclarationContainer")))
@protocol SharedKotlinKDeclarationContainer
@required
@end

__attribute__((swift_name("KotlinKAnnotatedElement")))
@protocol SharedKotlinKAnnotatedElement
@required
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
__attribute__((swift_name("KotlinKClassifier")))
@protocol SharedKotlinKClassifier
@required
@end

__attribute__((swift_name("KotlinKClass")))
@protocol SharedKotlinKClass <SharedKotlinKDeclarationContainer, SharedKotlinKAnnotatedElement, SharedKotlinKClassifier>
@required

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
- (BOOL)isInstanceValue:(id _Nullable)value __attribute__((swift_name("isInstance(value:)")));
@property (readonly) NSString * _Nullable qualifiedName __attribute__((swift_name("qualifiedName")));
@property (readonly) NSString * _Nullable simpleName __attribute__((swift_name("simpleName")));
@end

__attribute__((swift_name("Ktor_ioJvmSerializable")))
@protocol SharedKtor_ioJvmSerializable
@required
@end


/**
 * Represents an immutable URL
 *
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.Url)
 *
 * @property protocol
 * @property host name without port (domain)
 * @property port the specified port or protocol default port
 * @property specifiedPort port number that was specified to override protocol's default
 * @property encodedPath encoded path without query string
 * @property parameters URL query parameters
 * @property fragment URL fragment (anchor name)
 * @property user username part of URL
 * @property password password part of URL
 * @property trailingQuery keep trailing question character even if there are no query parameters
 *
 * @note annotations
 *   kotlinx.serialization.Serializable(with=NormalClass(value=io/ktor/http/UrlSerializer))
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_httpUrl")))
@interface SharedKtor_httpUrl : SharedBase <SharedKtor_ioJvmSerializable>
@property (class, readonly, getter=companion) SharedKtor_httpUrlCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *encodedFragment __attribute__((swift_name("encodedFragment")));
@property (readonly) NSString * _Nullable encodedPassword __attribute__((swift_name("encodedPassword")));
@property (readonly) NSString *encodedPath __attribute__((swift_name("encodedPath")));
@property (readonly) NSString *encodedPathAndQuery __attribute__((swift_name("encodedPathAndQuery")));
@property (readonly) NSString *encodedQuery __attribute__((swift_name("encodedQuery")));
@property (readonly) NSString * _Nullable encodedUser __attribute__((swift_name("encodedUser")));
@property (readonly) NSString *fragment __attribute__((swift_name("fragment")));
@property (readonly) NSString *host __attribute__((swift_name("host")));
@property (readonly) id<SharedKtor_httpParameters> parameters __attribute__((swift_name("parameters")));
@property (readonly) NSString * _Nullable password __attribute__((swift_name("password")));

/**
 * A list containing the segments of the URL path.
 *
 * This property was designed to distinguish between absolute and relative paths,
 * so it will have an empty segment at the beginning for URLs with a hostname
 * and an empty segment at the end for URLs with a trailing slash.
 *
 * ```kotlin
 * val fullUrl = Url("http://ktor.io/docs/")
 * fullUrl.pathSegments == listOf("", "docs", "")
 *
 * val absolute = Url("/docs/")
 * absolute.pathSegments == listOf("", "docs", "")
 *
 * val relative = Url("docs")
 * relative.pathSegments == listOf("docs")
 * ```
 *
 * This behaviour may not be ideal if you're working only with full URLs.
 * If you don't require the specific handling of empty segments, consider using the [segments] property instead:
 *
 * ```kotlin
 * val fullUrl = Url("http://ktor.io/docs/")
 * fullUrl.segments == listOf("docs")
 *
 * val absolute = Url("/docs/")
 * absolute.segments == listOf("docs")
 *
 * val relative = Url("docs")
 * relative.segments == listOf("docs")
 * ```
 *
 * To address this issue, the current [pathSegments] property will be renamed to [rawSegments].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.Url.pathSegments)
 */
@property (readonly) NSArray<NSString *> *pathSegments __attribute__((swift_name("pathSegments"))) __attribute__((deprecated("\n        `pathSegments` is deprecated.\n\n        This property will contain an empty path segment at the beginning for URLs with a hostname,\n        and an empty path segment at the end for the URLs with a trailing slash. If you need to keep this behaviour please\n        use [rawSegments]. If you only need to access the meaningful parts of the path, consider using [segments] instead.\n             \n        Please decide if you need [rawSegments] or [segments] explicitly.\n        ")));
@property (readonly) int32_t port __attribute__((swift_name("port")));
@property (readonly) SharedKtor_httpURLProtocol *protocol __attribute__((swift_name("protocol")));
@property (readonly) SharedKtor_httpURLProtocol * _Nullable protocolOrNull __attribute__((swift_name("protocolOrNull")));

/**
 * A list containing the segments of the URL path.
 *
 * This property is designed to distinguish between absolute and relative paths,
 * so it will have an empty segment at the beginning for URLs with a hostname
 * and an empty segment at the end for URLs with a trailing slash.
 *
 * ```kotlin
 * val fullUrl = Url("http://ktor.io/docs/")
 * fullUrl.rawSegments == listOf("", "docs", "")
 *
 * val absolute = Url("/docs/")
 * absolute.rawSegments == listOf("", "docs", "")
 *
 * val relative = Url("docs")
 * relative.rawSegments == listOf("docs")
 * ```
 *
 * This behaviour may not be ideal if you're working only with full URLs.
 * If you don't require the specific handling of empty segments, consider using the [segments] property instead:
 *
 * ```kotlin
 * val fullUrl = Url("http://ktor.io/docs/")
 * fullUrl.segments == listOf("docs")
 *
 * val absolute = Url("/docs/")
 * absolute.segments == listOf("docs")
 *
 * val relative = Url("docs")
 * relative.segments == listOf("docs")
 * ```
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.Url.rawSegments)
 */
@property (readonly) NSArray<NSString *> *rawSegments __attribute__((swift_name("rawSegments")));

/**
 * A list of path segments derived from the URL, excluding any leading
 * and trailing empty segments.
 *
 * ```kotlin
 * val fullUrl = Url("http://ktor.io/docs/")
 * fullUrl.segments == listOf("docs")
 *
 * val absolute = Url("/docs/")
 * absolute.segments == listOf("docs")
 * val relative = Url("docs")
 * relative.segments == listOf("docs")
 * ```
 *
 * If you need to check for trailing slash and relative/absolute paths, please check the [rawSegments] property.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.Url.segments)
 **/
@property (readonly) NSArray<NSString *> *segments __attribute__((swift_name("segments")));
@property (readonly) int32_t specifiedPort __attribute__((swift_name("specifiedPort")));
@property (readonly) BOOL trailingQuery __attribute__((swift_name("trailingQuery")));
@property (readonly) NSString * _Nullable user __attribute__((swift_name("user")));
@end


/**
 * Represents an HTTP method (verb)
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HttpMethod)
 *
 * @property value contains method name
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_httpHttpMethod")))
@interface SharedKtor_httpHttpMethod : SharedBase
- (instancetype)initWithValue:(NSString *)value __attribute__((swift_name("init(value:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKtor_httpHttpMethodCompanion *companion __attribute__((swift_name("companion")));
- (SharedKtor_httpHttpMethod *)doCopyValue:(NSString *)value __attribute__((swift_name("doCopy(value:)")));

/**
 * Represents an HTTP method (verb)
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HttpMethod)
 *
 * @property value contains method name
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Represents an HTTP method (verb)
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HttpMethod)
 *
 * @property value contains method name
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *value __attribute__((swift_name("value")));
@end


/**
 * Provides data structure for associating a [String] with a [List] of Strings
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.StringValues)
 */
__attribute__((swift_name("Ktor_utilsStringValues")))
@protocol SharedKtor_utilsStringValues
@required

/**
 * Checks if the given [name] exists in the map
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.StringValues.contains)
 */
- (BOOL)containsName:(NSString *)name __attribute__((swift_name("contains(name:)")));

/**
 * Checks if the given [name] and [value] pair exists in the map
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.StringValues.contains)
 */
- (BOOL)containsName:(NSString *)name value:(NSString *)value __attribute__((swift_name("contains(name:value:)")));

/**
 * Gets all entries from the map
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.StringValues.entries)
 */
- (NSSet<id<SharedKotlinMapEntry>> *)entries __attribute__((swift_name("entries()")));

/**
 * Iterates over all entries in this map and calls [body] for each pair
 *
 * Can be optimized in implementations
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.StringValues.forEach)
 */
- (void)forEachBody:(void (^)(NSString *, NSArray<NSString *> *))body __attribute__((swift_name("forEach(body:)")));

/**
 * Gets first value from the list of values associated with a [name], or null if the name is not present
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.StringValues.get)
 */
- (NSString * _Nullable)getName:(NSString *)name __attribute__((swift_name("get(name:)")));

/**
 * Gets all values associated with the [name], or null if the name is not present
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.StringValues.getAll)
 */
- (NSArray<NSString *> * _Nullable)getAllName:(NSString *)name __attribute__((swift_name("getAll(name:)")));

/**
 * Checks if this map is empty
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.StringValues.isEmpty)
 */
- (BOOL)isEmpty_ __attribute__((swift_name("isEmpty()")));

/**
 * Gets all names from the map
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.StringValues.names)
 */
- (NSSet<NSString *> *)names __attribute__((swift_name("names()")));

/**
 * Specifies if map has case-sensitive or case-insensitive names
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.StringValues.caseInsensitiveName)
 */
@property (readonly) BOOL caseInsensitiveName __attribute__((swift_name("caseInsensitiveName")));
@end


/**
 * Represents HTTP headers as a map from case-insensitive names to collection of [String] values
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.Headers)
 */
__attribute__((swift_name("Ktor_httpHeaders")))
@protocol SharedKtor_httpHeaders <SharedKtor_utilsStringValues>
@required
@end


/**
 * Information about the content to be sent to the peer, recognized by a client or server engine
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.content.OutgoingContent)
 */
__attribute__((swift_name("Ktor_httpOutgoingContent")))
@interface SharedKtor_httpOutgoingContent : SharedBase

/**
 * Gets an extension property for this content
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.content.OutgoingContent.getProperty)
 */
- (id _Nullable)getPropertyKey:(SharedKtor_utilsAttributeKey<id> *)key __attribute__((swift_name("getProperty(key:)")));

/**
 * Sets an extension property for this content
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.content.OutgoingContent.setProperty)
 */
- (void)setPropertyKey:(SharedKtor_utilsAttributeKey<id> *)key value:(id _Nullable)value __attribute__((swift_name("setProperty(key:value:)")));

/**
 * Trailers to set when sending this content, will be ignored if request is not in HTTP2 mode
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.content.OutgoingContent.trailers)
 */
- (id<SharedKtor_httpHeaders> _Nullable)trailers __attribute__((swift_name("trailers()")));

/**
 * Specifies content length in bytes for this resource.
 *
 * If null, the resources will be sent as `Transfer-Encoding: chunked`
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.content.OutgoingContent.contentLength)
 */
@property (readonly) SharedLong * _Nullable contentLength __attribute__((swift_name("contentLength")));

/**
 * Specifies [ContentType] for this resource.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.content.OutgoingContent.contentType)
 */
@property (readonly) SharedKtor_httpContentType * _Nullable contentType __attribute__((swift_name("contentType")));

/**
 * Headers to set when sending this content
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.content.OutgoingContent.headers)
 */
@property (readonly) id<SharedKtor_httpHeaders> headers __attribute__((swift_name("headers")));

/**
 * Status code to set when sending this content
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.content.OutgoingContent.status)
 */
@property (readonly) SharedKtor_httpHttpStatusCode * _Nullable status __attribute__((swift_name("status")));
@end

__attribute__((swift_name("Kotlinx_coroutines_coreJob")))
@protocol SharedKotlinx_coroutines_coreJob <SharedKotlinCoroutineContextElement>
@required

/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
- (id<SharedKotlinx_coroutines_coreChildHandle>)attachChildChild:(id<SharedKotlinx_coroutines_coreChildJob>)child __attribute__((swift_name("attachChild(child:)")));
- (void)cancelCause:(SharedKotlinCancellationException * _Nullable)cause __attribute__((swift_name("cancel(cause:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
- (SharedKotlinCancellationException *)getCancellationException __attribute__((swift_name("getCancellationException()")));
- (id<SharedKotlinx_coroutines_coreDisposableHandle>)invokeOnCompletionHandler:(void (^)(SharedKotlinThrowable * _Nullable))handler __attribute__((swift_name("invokeOnCompletion(handler:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
- (id<SharedKotlinx_coroutines_coreDisposableHandle>)invokeOnCompletionOnCancelling:(BOOL)onCancelling invokeImmediately:(BOOL)invokeImmediately handler:(void (^)(SharedKotlinThrowable * _Nullable))handler __attribute__((swift_name("invokeOnCompletion(onCancelling:invokeImmediately:handler:)")));

/**
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)joinWithCompletionHandler:(void (^)(NSError * _Nullable))completionHandler __attribute__((swift_name("join(completionHandler:)")));
- (id<SharedKotlinx_coroutines_coreJob>)plusOther_:(id<SharedKotlinx_coroutines_coreJob>)other __attribute__((swift_name("plus(other_:)"))) __attribute__((unavailable("Operator '+' on two Job objects is meaningless. Job is a coroutine context element and `+` is a set-sum operator for coroutine contexts. The job to the right of `+` just replaces the job the left of `+`.")));
- (BOOL)start __attribute__((swift_name("start()")));
@property (readonly) id<SharedKotlinSequence> children __attribute__((swift_name("children")));
@property (readonly) BOOL isActive __attribute__((swift_name("isActive")));
@property (readonly) BOOL isCancelled __attribute__((swift_name("isCancelled")));
@property (readonly) BOOL isCompleted __attribute__((swift_name("isCompleted")));
@property (readonly) id<SharedKotlinx_coroutines_coreSelectClause0> onJoin __attribute__((swift_name("onJoin")));

/**
 * @note annotations
 *   kotlinx.coroutines.ExperimentalCoroutinesApi
*/
@property (readonly) id<SharedKotlinx_coroutines_coreJob> _Nullable parent __attribute__((swift_name("parent")));
@end


/**
 * Represents an HTTP status code and description.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HttpStatusCode)
 *
 * @param value is a numeric code.
 * @param description is free form description of a status.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_httpHttpStatusCode")))
@interface SharedKtor_httpHttpStatusCode : SharedBase <SharedKotlinComparable>
- (instancetype)initWithValue:(int32_t)value description:(NSString *)description __attribute__((swift_name("init(value:description:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKtor_httpHttpStatusCodeCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(SharedKtor_httpHttpStatusCode *)other __attribute__((swift_name("compareTo(other:)")));
- (SharedKtor_httpHttpStatusCode *)doCopyValue:(int32_t)value description:(NSString *)description __attribute__((swift_name("doCopy(value:description:)")));

/**
 * Returns a copy of `this` code with a description changed to [value].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HttpStatusCode.description)
 */
- (SharedKtor_httpHttpStatusCode *)descriptionValue:(NSString *)value __attribute__((swift_name("description(value:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *description_ __attribute__((swift_name("description_")));
@property (readonly) int32_t value __attribute__((swift_name("value")));
@end


/**
 * Date in GMT timezone
 *
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.date.GMTDate)
 *
 * @property seconds: seconds from 0 to 60(last is for leap second)
 * @property minutes: minutes from 0 to 59
 * @property hours: hours from 0 to 23
 * @property dayOfWeek an instance of the corresponding day of week
 * @property dayOfMonth: day of month from 1 to 31
 * @property dayOfYear: day of year from 1 to 366
 * @property month an instance of the corresponding month
 * @property year: year in common era(CE: https://en.wikipedia.org/wiki/Common_Era)
 *
 * @property timestamp is a number of epoch milliseconds
 *
 * @note annotations
 *   kotlinx.serialization.Serializable
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_utilsGMTDate")))
@interface SharedKtor_utilsGMTDate : SharedBase <SharedKotlinComparable>
- (instancetype)initWithSeconds:(int32_t)seconds minutes:(int32_t)minutes hours:(int32_t)hours dayOfWeek:(SharedKtor_utilsWeekDay *)dayOfWeek dayOfMonth:(int32_t)dayOfMonth dayOfYear:(int32_t)dayOfYear month:(SharedKtor_utilsMonth *)month year:(int32_t)year timestamp:(int64_t)timestamp __attribute__((swift_name("init(seconds:minutes:hours:dayOfWeek:dayOfMonth:dayOfYear:month:year:timestamp:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKtor_utilsGMTDateCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(SharedKtor_utilsGMTDate *)other __attribute__((swift_name("compareTo(other:)")));
- (SharedKtor_utilsGMTDate *)doCopy __attribute__((swift_name("doCopy()")));
- (SharedKtor_utilsGMTDate *)doCopySeconds:(int32_t)seconds minutes:(int32_t)minutes hours:(int32_t)hours dayOfWeek:(SharedKtor_utilsWeekDay *)dayOfWeek dayOfMonth:(int32_t)dayOfMonth dayOfYear:(int32_t)dayOfYear month:(SharedKtor_utilsMonth *)month year:(int32_t)year timestamp:(int64_t)timestamp __attribute__((swift_name("doCopy(seconds:minutes:hours:dayOfWeek:dayOfMonth:dayOfYear:month:year:timestamp:)")));

/**
 * Date in GMT timezone
 *
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.date.GMTDate)
 *
 * @property seconds: seconds from 0 to 60(last is for leap second)
 * @property minutes: minutes from 0 to 59
 * @property hours: hours from 0 to 23
 * @property dayOfWeek an instance of the corresponding day of week
 * @property dayOfMonth: day of month from 1 to 31
 * @property dayOfYear: day of year from 1 to 366
 * @property month an instance of the corresponding month
 * @property year: year in common era(CE: https://en.wikipedia.org/wiki/Common_Era)
 *
 * @property timestamp is a number of epoch milliseconds
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Date in GMT timezone
 *
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.date.GMTDate)
 *
 * @property seconds: seconds from 0 to 60(last is for leap second)
 * @property minutes: minutes from 0 to 59
 * @property hours: hours from 0 to 23
 * @property dayOfWeek an instance of the corresponding day of week
 * @property dayOfMonth: day of month from 1 to 31
 * @property dayOfYear: day of year from 1 to 366
 * @property month an instance of the corresponding month
 * @property year: year in common era(CE: https://en.wikipedia.org/wiki/Common_Era)
 *
 * @property timestamp is a number of epoch milliseconds
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Date in GMT timezone
 *
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.date.GMTDate)
 *
 * @property seconds: seconds from 0 to 60(last is for leap second)
 * @property minutes: minutes from 0 to 59
 * @property hours: hours from 0 to 23
 * @property dayOfWeek an instance of the corresponding day of week
 * @property dayOfMonth: day of month from 1 to 31
 * @property dayOfYear: day of year from 1 to 366
 * @property month an instance of the corresponding month
 * @property year: year in common era(CE: https://en.wikipedia.org/wiki/Common_Era)
 *
 * @property timestamp is a number of epoch milliseconds
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t dayOfMonth __attribute__((swift_name("dayOfMonth")));
@property (readonly) SharedKtor_utilsWeekDay *dayOfWeek __attribute__((swift_name("dayOfWeek")));
@property (readonly) int32_t dayOfYear __attribute__((swift_name("dayOfYear")));
@property (readonly) int32_t hours __attribute__((swift_name("hours")));
@property (readonly) int32_t minutes __attribute__((swift_name("minutes")));
@property (readonly) SharedKtor_utilsMonth *month __attribute__((swift_name("month")));
@property (readonly) int32_t seconds __attribute__((swift_name("seconds")));
@property (readonly) int64_t timestamp __attribute__((swift_name("timestamp")));
@property (readonly) int32_t year __attribute__((swift_name("year")));
@end


/**
 * Represents an HTTP protocol version.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HttpProtocolVersion)
 *
 * @property name specifies name of the protocol, e.g. "HTTP".
 * @property major specifies protocol major version.
 * @property minor specifies protocol minor version.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_httpHttpProtocolVersion")))
@interface SharedKtor_httpHttpProtocolVersion : SharedBase
- (instancetype)initWithName:(NSString *)name major:(int32_t)major minor:(int32_t)minor __attribute__((swift_name("init(name:major:minor:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKtor_httpHttpProtocolVersionCompanion *companion __attribute__((swift_name("companion")));
- (SharedKtor_httpHttpProtocolVersion *)doCopyName:(NSString *)name major:(int32_t)major minor:(int32_t)minor __attribute__((swift_name("doCopy(name:major:minor:)")));

/**
 * Represents an HTTP protocol version.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HttpProtocolVersion)
 *
 * @property name specifies name of the protocol, e.g. "HTTP".
 * @property major specifies protocol major version.
 * @property minor specifies protocol minor version.
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Represents an HTTP protocol version.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HttpProtocolVersion)
 *
 * @property name specifies name of the protocol, e.g. "HTTP".
 * @property major specifies protocol major version.
 * @property minor specifies protocol minor version.
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t major __attribute__((swift_name("major")));
@property (readonly) int32_t minor __attribute__((swift_name("minor")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
*/
__attribute__((swift_name("KotlinContinuation")))
@protocol SharedKotlinContinuation
@required
- (void)resumeWithResult:(id _Nullable)result __attribute__((swift_name("resumeWith(result:)")));
@property (readonly) id<SharedKotlinCoroutineContext> context __attribute__((swift_name("context")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.3")
 *   kotlin.ExperimentalStdlibApi
*/
__attribute__((swift_name("KotlinAbstractCoroutineContextKey")))
@interface SharedKotlinAbstractCoroutineContextKey<B, E> : SharedBase <SharedKotlinCoroutineContextKey>
- (instancetype)initWithBaseKey:(id<SharedKotlinCoroutineContextKey>)baseKey safeCast:(E _Nullable (^)(id<SharedKotlinCoroutineContextElement>))safeCast __attribute__((swift_name("init(baseKey:safeCast:)"))) __attribute__((objc_designated_initializer));
@end


/**
 * @note annotations
 *   kotlin.ExperimentalStdlibApi
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_coroutines_coreCoroutineDispatcher.Key")))
@interface SharedKotlinx_coroutines_coreCoroutineDispatcherKey : SharedKotlinAbstractCoroutineContextKey<id<SharedKotlinContinuationInterceptor>, SharedKotlinx_coroutines_coreCoroutineDispatcher *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithBaseKey:(id<SharedKotlinCoroutineContextKey>)baseKey safeCast:(id<SharedKotlinCoroutineContextElement> _Nullable (^)(id<SharedKotlinCoroutineContextElement>))safeCast __attribute__((swift_name("init(baseKey:safeCast:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
+ (instancetype)key __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinx_coroutines_coreCoroutineDispatcherKey *shared __attribute__((swift_name("shared")));
@end

__attribute__((swift_name("Kotlinx_coroutines_coreRunnable")))
@protocol SharedKotlinx_coroutines_coreRunnable
@required
- (void)run __attribute__((swift_name("run()")));
@end


/**
 * Ktor type information.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.reflect.TypeInfo)
 *
 * @property type Source KClass<*>
 * @property kotlinType Kotlin reified type with all generic type parameters.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_utilsTypeInfo")))
@interface SharedKtor_utilsTypeInfo : SharedBase
- (instancetype)initWithType:(id<SharedKotlinKClass>)type kotlinType:(id<SharedKotlinKType> _Nullable)kotlinType __attribute__((swift_name("init(type:kotlinType:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithType:(id<SharedKotlinKClass>)type reifiedType:(id<SharedKotlinKType>)reifiedType kotlinType:(id<SharedKotlinKType> _Nullable)kotlinType __attribute__((swift_name("init(type:reifiedType:kotlinType:)"))) __attribute__((objc_designated_initializer)) __attribute__((deprecated("Use constructor without reifiedType parameter.")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) id<SharedKotlinKType> _Nullable kotlinType __attribute__((swift_name("kotlinType")));
@property (readonly) id<SharedKotlinKClass> type __attribute__((swift_name("type")));
@end


/**
 * Channel for asynchronous reading of sequences of bytes.
 * This is a **single-reader channel**.
 *
 * Operations on this channel cannot be invoked concurrently.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.utils.io.ByteReadChannel)
 */
__attribute__((swift_name("Ktor_ioByteReadChannel")))
@protocol SharedKtor_ioByteReadChannel
@required

/**
 * Suspend the channel until it has [min] bytes or gets closed. Throws exception if the channel was closed with an
 * error. If there are bytes available in the channel, this function returns immediately.
 *
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.utils.io.ByteReadChannel.awaitContent)
 *
 * @return return `false` eof is reached, otherwise `true`.
 *
 * @note This method converts instances of CancellationException to errors.
 * Other uncaught Kotlin exceptions are fatal.
*/
- (void)awaitContentMin:(int32_t)min completionHandler:(void (^)(SharedBoolean * _Nullable, NSError * _Nullable))completionHandler __attribute__((swift_name("awaitContent(min:completionHandler:)")));
- (void)cancelCause_:(SharedKotlinThrowable * _Nullable)cause __attribute__((swift_name("cancel(cause_:)")));
@property (readonly) SharedKotlinThrowable * _Nullable closedCause __attribute__((swift_name("closedCause")));
@property (readonly) BOOL isClosedForRead __attribute__((swift_name("isClosedForRead")));
@property (readonly) id<SharedKotlinx_io_coreSource> readBuffer __attribute__((swift_name("readBuffer")));
@end

__attribute__((swift_name("Ktor_utilsStringValuesBuilder")))
@protocol SharedKtor_utilsStringValuesBuilder
@required
- (void)appendName:(NSString *)name value:(NSString *)value __attribute__((swift_name("append(name:value:)")));
- (void)appendAllStringValues:(id<SharedKtor_utilsStringValues>)stringValues __attribute__((swift_name("appendAll(stringValues:)")));
- (void)appendAllName:(NSString *)name values:(id)values __attribute__((swift_name("appendAll(name:values:)")));
- (void)appendMissingStringValues:(id<SharedKtor_utilsStringValues>)stringValues __attribute__((swift_name("appendMissing(stringValues:)")));
- (void)appendMissingName:(NSString *)name values:(id)values __attribute__((swift_name("appendMissing(name:values:)")));
- (id<SharedKtor_utilsStringValues>)build __attribute__((swift_name("build()")));
- (void)clear __attribute__((swift_name("clear()")));
- (BOOL)containsName:(NSString *)name __attribute__((swift_name("contains(name:)")));
- (BOOL)containsName:(NSString *)name value:(NSString *)value __attribute__((swift_name("contains(name:value:)")));
- (NSSet<id<SharedKotlinMapEntry>> *)entries __attribute__((swift_name("entries()")));
- (NSString * _Nullable)getName:(NSString *)name __attribute__((swift_name("get(name:)")));
- (NSArray<NSString *> * _Nullable)getAllName:(NSString *)name __attribute__((swift_name("getAll(name:)")));
- (BOOL)isEmpty_ __attribute__((swift_name("isEmpty()")));
- (NSSet<NSString *> *)names __attribute__((swift_name("names()")));
- (void)removeName:(NSString *)name __attribute__((swift_name("remove(name:)")));
- (BOOL)removeName:(NSString *)name value:(NSString *)value __attribute__((swift_name("remove(name:value:)")));
- (void)removeKeysWithNoEntries __attribute__((swift_name("removeKeysWithNoEntries()")));
- (void)setName:(NSString *)name value:(NSString *)value __attribute__((swift_name("set(name:value:)")));
@property (readonly) BOOL caseInsensitiveName __attribute__((swift_name("caseInsensitiveName")));
@end

__attribute__((swift_name("Ktor_utilsStringValuesBuilderImpl")))
@interface SharedKtor_utilsStringValuesBuilderImpl : SharedBase <SharedKtor_utilsStringValuesBuilder>
- (instancetype)initWithCaseInsensitiveName:(BOOL)caseInsensitiveName size:(int32_t)size __attribute__((swift_name("init(caseInsensitiveName:size:)"))) __attribute__((objc_designated_initializer));
- (void)appendName:(NSString *)name value:(NSString *)value __attribute__((swift_name("append(name:value:)")));
- (void)appendAllStringValues:(id<SharedKtor_utilsStringValues>)stringValues __attribute__((swift_name("appendAll(stringValues:)")));
- (void)appendAllName:(NSString *)name values:(id)values __attribute__((swift_name("appendAll(name:values:)")));
- (void)appendMissingStringValues:(id<SharedKtor_utilsStringValues>)stringValues __attribute__((swift_name("appendMissing(stringValues:)")));
- (void)appendMissingName:(NSString *)name values:(id)values __attribute__((swift_name("appendMissing(name:values:)")));
- (id<SharedKtor_utilsStringValues>)build __attribute__((swift_name("build()")));
- (void)clear __attribute__((swift_name("clear()")));
- (BOOL)containsName:(NSString *)name __attribute__((swift_name("contains(name:)")));
- (BOOL)containsName:(NSString *)name value:(NSString *)value __attribute__((swift_name("contains(name:value:)")));
- (NSSet<id<SharedKotlinMapEntry>> *)entries __attribute__((swift_name("entries()")));
- (NSString * _Nullable)getName:(NSString *)name __attribute__((swift_name("get(name:)")));
- (NSArray<NSString *> * _Nullable)getAllName:(NSString *)name __attribute__((swift_name("getAll(name:)")));
- (BOOL)isEmpty_ __attribute__((swift_name("isEmpty()")));
- (NSSet<NSString *> *)names __attribute__((swift_name("names()")));
- (void)removeName:(NSString *)name __attribute__((swift_name("remove(name:)")));
- (BOOL)removeName:(NSString *)name value:(NSString *)value __attribute__((swift_name("remove(name:value:)")));
- (void)removeKeysWithNoEntries __attribute__((swift_name("removeKeysWithNoEntries()")));
- (void)setName:(NSString *)name value:(NSString *)value __attribute__((swift_name("set(name:value:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)validateNameName:(NSString *)name __attribute__((swift_name("validateName(name:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)validateValueValue:(NSString *)value __attribute__((swift_name("validateValue(value:)")));
@property (readonly) BOOL caseInsensitiveName __attribute__((swift_name("caseInsensitiveName")));

/**
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property (readonly) SharedMutableDictionary<NSString *, NSMutableArray<NSString *> *> *values __attribute__((swift_name("values")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_httpHeadersBuilder")))
@interface SharedKtor_httpHeadersBuilder : SharedKtor_utilsStringValuesBuilderImpl
- (instancetype)initWithSize:(int32_t)size __attribute__((swift_name("init(size:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithCaseInsensitiveName:(BOOL)caseInsensitiveName size:(int32_t)size __attribute__((swift_name("init(caseInsensitiveName:size:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
- (id<SharedKtor_httpHeaders>)build __attribute__((swift_name("build()")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)validateNameName:(NSString *)name __attribute__((swift_name("validateName(name:)")));

/**
 * @note This method has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
- (void)validateValueValue:(NSString *)value __attribute__((swift_name("validateValue(value:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_client_coreHttpRequestBuilder.Companion")))
@interface SharedKtor_client_coreHttpRequestBuilderCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKtor_client_coreHttpRequestBuilderCompanion *shared __attribute__((swift_name("shared")));
@end


/**
 * A URL builder with all mutable components
 *
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.URLBuilder)
 *
 * @property protocol URL protocol (scheme)
 * @property host name without port (domain)
 * @property port port number
 * @property user username part (optional)
 * @property password password part (optional)
 * @property pathSegments URL path without query
 * @property parameters URL query parameters
 * @property fragment URL fragment (anchor name)
 * @property trailingQuery keep a trailing question character even if there are no query parameters
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_httpURLBuilder")))
@interface SharedKtor_httpURLBuilder : SharedBase
- (instancetype)initWithProtocol:(SharedKtor_httpURLProtocol * _Nullable)protocol host:(NSString *)host port:(int32_t)port user:(NSString * _Nullable)user password:(NSString * _Nullable)password pathSegments:(NSArray<NSString *> *)pathSegments parameters:(id<SharedKtor_httpParameters>)parameters fragment:(NSString *)fragment trailingQuery:(BOOL)trailingQuery __attribute__((swift_name("init(protocol:host:port:user:password:pathSegments:parameters:fragment:trailingQuery:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKtor_httpURLBuilderCompanion *companion __attribute__((swift_name("companion")));

/**
 * Build a [Url] instance (everything is copied to a new instance)
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.URLBuilder.build)
 */
- (SharedKtor_httpUrl *)build __attribute__((swift_name("build()")));

/**
 * Build a URL string
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.URLBuilder.buildString)
 */
- (NSString *)buildString __attribute__((swift_name("buildString()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property NSString *encodedFragment __attribute__((swift_name("encodedFragment")));
@property id<SharedKtor_httpParametersBuilder> encodedParameters __attribute__((swift_name("encodedParameters")));
@property NSString * _Nullable encodedPassword __attribute__((swift_name("encodedPassword")));
@property NSArray<NSString *> *encodedPathSegments __attribute__((swift_name("encodedPathSegments")));
@property NSString * _Nullable encodedUser __attribute__((swift_name("encodedUser")));
@property NSString *fragment __attribute__((swift_name("fragment")));
@property NSString *host __attribute__((swift_name("host")));
@property (readonly) id<SharedKtor_httpParametersBuilder> parameters __attribute__((swift_name("parameters")));
@property NSString * _Nullable password __attribute__((swift_name("password")));
@property NSArray<NSString *> *pathSegments __attribute__((swift_name("pathSegments")));
@property int32_t port __attribute__((swift_name("port")));
@property SharedKtor_httpURLProtocol *protocol __attribute__((swift_name("protocol")));
@property SharedKtor_httpURLProtocol * _Nullable protocolOrNull __attribute__((swift_name("protocolOrNull")));
@property BOOL trailingQuery __attribute__((swift_name("trailingQuery")));
@property NSString * _Nullable user __attribute__((swift_name("user")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_client_coreHttpClientCall.Companion")))
@interface SharedKtor_client_coreHttpClientCallCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKtor_client_coreHttpClientCallCompanion *shared __attribute__((swift_name("shared")));
@end


/**
 * A request for [HttpClient], first part of [HttpClientCall].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequest)
 */
__attribute__((swift_name("Ktor_client_coreHttpRequest")))
@protocol SharedKtor_client_coreHttpRequest <SharedKtor_httpHttpMessage, SharedKotlinx_coroutines_coreCoroutineScope>
@required

/**
 * Typed [Attributes] associated to this call serving as a lightweight container.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequest.attributes)
 */
@property (readonly) id<SharedKtor_utilsAttributes> attributes __attribute__((swift_name("attributes")));

/**
 * The associated [HttpClientCall] containing both
 * the underlying [HttpClientCall.request] and [HttpClientCall.response].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequest.call)
 */
@property (readonly) SharedKtor_client_coreHttpClientCall *call __attribute__((swift_name("call")));

/**
 * An [OutgoingContent] representing the request body
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequest.content)
 */
@property (readonly) SharedKtor_httpOutgoingContent *content __attribute__((swift_name("content")));

/**
 * The [HttpMethod] or HTTP VERB used for this request.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequest.method)
 */
@property (readonly) SharedKtor_httpHttpMethod *method __attribute__((swift_name("method")));

/**
 * The [Url] representing the endpoint and the uri for this request.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.client.request.HttpRequest.url)
 */
@property (readonly) SharedKtor_httpUrl *url __attribute__((swift_name("url")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_httpUrl.Companion")))
@interface SharedKtor_httpUrlCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKtor_httpUrlCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));
@end


/**
 * Represents HTTP parameters as a map from case-insensitive names to collection of [String] values
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.Parameters)
 */
__attribute__((swift_name("Ktor_httpParameters")))
@protocol SharedKtor_httpParameters <SharedKtor_utilsStringValues>
@required
@end


/**
 * Represents URL protocol
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.URLProtocol)
 *
 * @property name of protocol (schema)
 * @property defaultPort default port for protocol or `-1` if not known
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_httpURLProtocol")))
@interface SharedKtor_httpURLProtocol : SharedBase <SharedKtor_ioJvmSerializable>
- (instancetype)initWithName:(NSString *)name defaultPort:(int32_t)defaultPort __attribute__((swift_name("init(name:defaultPort:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKtor_httpURLProtocolCompanion *companion __attribute__((swift_name("companion")));
- (SharedKtor_httpURLProtocol *)doCopyName:(NSString *)name defaultPort:(int32_t)defaultPort __attribute__((swift_name("doCopy(name:defaultPort:)")));

/**
 * Represents URL protocol
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.URLProtocol)
 *
 * @property name of protocol (schema)
 * @property defaultPort default port for protocol or `-1` if not known
 */
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));

/**
 * Represents URL protocol
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.URLProtocol)
 *
 * @property name of protocol (schema)
 * @property defaultPort default port for protocol or `-1` if not known
 */
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Represents URL protocol
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.URLProtocol)
 *
 * @property name of protocol (schema)
 * @property defaultPort default port for protocol or `-1` if not known
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int32_t defaultPort __attribute__((swift_name("defaultPort")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_httpHttpMethod.Companion")))
@interface SharedKtor_httpHttpMethodCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKtor_httpHttpMethodCompanion *shared __attribute__((swift_name("shared")));

/**
 * Parse HTTP method by [method] string
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HttpMethod.Companion.parse)
 */
- (SharedKtor_httpHttpMethod *)parseMethod:(NSString *)method __attribute__((swift_name("parse(method:)")));

/**
 * A list of default HTTP methods
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HttpMethod.Companion.DefaultMethods)
 */
@property (readonly) NSArray<SharedKtor_httpHttpMethod *> *DefaultMethods __attribute__((swift_name("DefaultMethods")));
@property (readonly) SharedKtor_httpHttpMethod *Delete __attribute__((swift_name("Delete")));
@property (readonly) SharedKtor_httpHttpMethod *Get __attribute__((swift_name("Get")));
@property (readonly) SharedKtor_httpHttpMethod *Head __attribute__((swift_name("Head")));
@property (readonly) SharedKtor_httpHttpMethod *Options __attribute__((swift_name("Options")));
@property (readonly) SharedKtor_httpHttpMethod *Patch __attribute__((swift_name("Patch")));
@property (readonly) SharedKtor_httpHttpMethod *Post __attribute__((swift_name("Post")));
@property (readonly) SharedKtor_httpHttpMethod *Put __attribute__((swift_name("Put")));
@end

__attribute__((swift_name("KotlinMapEntry")))
@protocol SharedKotlinMapEntry
@required
@property (readonly) id _Nullable key __attribute__((swift_name("key")));
@property (readonly) id _Nullable value __attribute__((swift_name("value")));
@end


/**
 * Represents a header value that consist of [content] followed by [parameters].
 * Useful for headers such as `Content-Type`, `Content-Disposition` and so on.
 *
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HeaderValueWithParameters)
 *
 * @property content header's content without parameters
 * @property parameters
 */
__attribute__((swift_name("Ktor_httpHeaderValueWithParameters")))
@interface SharedKtor_httpHeaderValueWithParameters : SharedBase
- (instancetype)initWithContent:(NSString *)content parameters:(NSArray<SharedKtor_httpHeaderValueParam *> *)parameters __attribute__((swift_name("init(content:parameters:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKtor_httpHeaderValueWithParametersCompanion *companion __attribute__((swift_name("companion")));

/**
 * The first value for the parameter with [name] comparing case-insensitively or `null` if no such parameters found
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HeaderValueWithParameters.parameter)
 */
- (NSString * _Nullable)parameterName:(NSString *)name __attribute__((swift_name("parameter(name:)")));
- (NSString *)description __attribute__((swift_name("description()")));

/**
 * @note This property has protected visibility in Kotlin source and is intended only for use by subclasses.
*/
@property (readonly) NSString *content __attribute__((swift_name("content")));
@property (readonly) NSArray<SharedKtor_httpHeaderValueParam *> *parameters __attribute__((swift_name("parameters")));
@end


/**
 * Represents a value for a `Content-Type` header.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.ContentType)
 *
 * @property contentType represents a type part of the media type.
 * @property contentSubtype represents a subtype part of the media type.
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_httpContentType")))
@interface SharedKtor_httpContentType : SharedKtor_httpHeaderValueWithParameters
- (instancetype)initWithContentType:(NSString *)contentType contentSubtype:(NSString *)contentSubtype parameters:(NSArray<SharedKtor_httpHeaderValueParam *> *)parameters __attribute__((swift_name("init(contentType:contentSubtype:parameters:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithContent:(NSString *)content parameters:(NSArray<SharedKtor_httpHeaderValueParam *> *)parameters __attribute__((swift_name("init(content:parameters:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedKtor_httpContentTypeCompanion *companion __attribute__((swift_name("companion")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Checks if `this` type matches a [pattern] type taking into account placeholder symbols `*` and parameters.
 * The `this` type must be a more specific type than the [pattern] type. In other words:
 *
 * ```kotlin
 * ContentType("a", "b").match(ContentType("a", "b").withParameter("foo", "bar")) === false
 * ContentType("a", "b").withParameter("foo", "bar").match(ContentType("a", "b")) === true
 * ContentType("a", "*").match(ContentType("a", "b")) === false
 * ContentType("a", "b").match(ContentType("a", "*")) === true
 * ```
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.ContentType.match)
 */
- (BOOL)matchPattern:(SharedKtor_httpContentType *)pattern __attribute__((swift_name("match(pattern:)")));

/**
 * Checks if `this` type matches a [pattern] type taking into account placeholder symbols `*` and parameters.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.ContentType.match)
 */
- (BOOL)matchPattern_:(NSString *)pattern __attribute__((swift_name("match(pattern_:)")));

/**
 * Creates a copy of `this` type with the added parameter with the [name] and [value].
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.ContentType.withParameter)
 */
- (SharedKtor_httpContentType *)withParameterName:(NSString *)name value:(NSString *)value __attribute__((swift_name("withParameter(name:value:)")));

/**
 * Creates a copy of `this` type without any parameters
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.ContentType.withoutParameters)
 */
- (SharedKtor_httpContentType *)withoutParameters __attribute__((swift_name("withoutParameters()")));
@property (readonly) NSString *contentSubtype __attribute__((swift_name("contentSubtype")));
@property (readonly) NSString *contentType __attribute__((swift_name("contentType")));
@end


/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
__attribute__((swift_name("Kotlinx_coroutines_coreChildHandle")))
@protocol SharedKotlinx_coroutines_coreChildHandle <SharedKotlinx_coroutines_coreDisposableHandle>
@required

/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
- (BOOL)childCancelledCause:(SharedKotlinThrowable *)cause __attribute__((swift_name("childCancelled(cause:)")));

/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
@property (readonly) id<SharedKotlinx_coroutines_coreJob> _Nullable parent __attribute__((swift_name("parent")));
@end


/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
__attribute__((swift_name("Kotlinx_coroutines_coreChildJob")))
@protocol SharedKotlinx_coroutines_coreChildJob <SharedKotlinx_coroutines_coreJob>
@required

/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
- (void)parentCancelledParentJob:(id<SharedKotlinx_coroutines_coreParentJob>)parentJob __attribute__((swift_name("parentCancelled(parentJob:)")));
@end

__attribute__((swift_name("KotlinSequence")))
@protocol SharedKotlinSequence
@required
- (id<SharedKotlinIterator>)iterator __attribute__((swift_name("iterator()")));
@end


/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
__attribute__((swift_name("Kotlinx_coroutines_coreSelectClause")))
@protocol SharedKotlinx_coroutines_coreSelectClause
@required
@property (readonly) id clauseObject __attribute__((swift_name("clauseObject")));
@property (readonly) SharedKotlinUnit *(^(^ _Nullable onCancellationConstructor)(id<SharedKotlinx_coroutines_coreSelectInstance>, id _Nullable, id _Nullable))(SharedKotlinThrowable *, id _Nullable, id<SharedKotlinCoroutineContext>) __attribute__((swift_name("onCancellationConstructor")));
@property (readonly) id _Nullable (^processResFunc)(id, id _Nullable, id _Nullable) __attribute__((swift_name("processResFunc")));
@property (readonly) void (^regFunc)(id, id<SharedKotlinx_coroutines_coreSelectInstance>, id _Nullable) __attribute__((swift_name("regFunc")));
@end

__attribute__((swift_name("Kotlinx_coroutines_coreSelectClause0")))
@protocol SharedKotlinx_coroutines_coreSelectClause0 <SharedKotlinx_coroutines_coreSelectClause>
@required
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_httpHttpStatusCode.Companion")))
@interface SharedKtor_httpHttpStatusCodeCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKtor_httpHttpStatusCodeCompanion *shared __attribute__((swift_name("shared")));

/**
 * Creates an instance of [HttpStatusCode] with the given numeric value.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HttpStatusCode.Companion.fromValue)
 */
- (SharedKtor_httpHttpStatusCode *)fromValueValue:(int32_t)value __attribute__((swift_name("fromValue(value:)")));
@property (readonly) SharedKtor_httpHttpStatusCode *Accepted __attribute__((swift_name("Accepted")));
@property (readonly) SharedKtor_httpHttpStatusCode *BadGateway __attribute__((swift_name("BadGateway")));
@property (readonly) SharedKtor_httpHttpStatusCode *BadRequest __attribute__((swift_name("BadRequest")));
@property (readonly) SharedKtor_httpHttpStatusCode *Conflict __attribute__((swift_name("Conflict")));
@property (readonly) SharedKtor_httpHttpStatusCode *Continue __attribute__((swift_name("Continue")));
@property (readonly) SharedKtor_httpHttpStatusCode *Created __attribute__((swift_name("Created")));
@property (readonly) SharedKtor_httpHttpStatusCode *ExpectationFailed __attribute__((swift_name("ExpectationFailed")));
@property (readonly) SharedKtor_httpHttpStatusCode *FailedDependency __attribute__((swift_name("FailedDependency")));
@property (readonly) SharedKtor_httpHttpStatusCode *Forbidden __attribute__((swift_name("Forbidden")));
@property (readonly) SharedKtor_httpHttpStatusCode *Found __attribute__((swift_name("Found")));
@property (readonly) SharedKtor_httpHttpStatusCode *GatewayTimeout __attribute__((swift_name("GatewayTimeout")));
@property (readonly) SharedKtor_httpHttpStatusCode *Gone __attribute__((swift_name("Gone")));
@property (readonly) SharedKtor_httpHttpStatusCode *InsufficientStorage __attribute__((swift_name("InsufficientStorage")));
@property (readonly) SharedKtor_httpHttpStatusCode *InternalServerError __attribute__((swift_name("InternalServerError")));
@property (readonly) SharedKtor_httpHttpStatusCode *LengthRequired __attribute__((swift_name("LengthRequired")));
@property (readonly) SharedKtor_httpHttpStatusCode *Locked __attribute__((swift_name("Locked")));
@property (readonly) SharedKtor_httpHttpStatusCode *MethodNotAllowed __attribute__((swift_name("MethodNotAllowed")));
@property (readonly) SharedKtor_httpHttpStatusCode *MovedPermanently __attribute__((swift_name("MovedPermanently")));
@property (readonly) SharedKtor_httpHttpStatusCode *MultiStatus __attribute__((swift_name("MultiStatus")));
@property (readonly) SharedKtor_httpHttpStatusCode *MultipleChoices __attribute__((swift_name("MultipleChoices")));
@property (readonly) SharedKtor_httpHttpStatusCode *NoContent __attribute__((swift_name("NoContent")));
@property (readonly) SharedKtor_httpHttpStatusCode *NonAuthoritativeInformation __attribute__((swift_name("NonAuthoritativeInformation")));
@property (readonly) SharedKtor_httpHttpStatusCode *NotAcceptable __attribute__((swift_name("NotAcceptable")));
@property (readonly) SharedKtor_httpHttpStatusCode *NotFound __attribute__((swift_name("NotFound")));
@property (readonly) SharedKtor_httpHttpStatusCode *NotImplemented __attribute__((swift_name("NotImplemented")));
@property (readonly) SharedKtor_httpHttpStatusCode *NotModified __attribute__((swift_name("NotModified")));
@property (readonly) SharedKtor_httpHttpStatusCode *OK __attribute__((swift_name("OK")));
@property (readonly) SharedKtor_httpHttpStatusCode *PartialContent __attribute__((swift_name("PartialContent")));
@property (readonly) SharedKtor_httpHttpStatusCode *PayloadTooLarge __attribute__((swift_name("PayloadTooLarge")));
@property (readonly) SharedKtor_httpHttpStatusCode *PaymentRequired __attribute__((swift_name("PaymentRequired")));
@property (readonly) SharedKtor_httpHttpStatusCode *PermanentRedirect __attribute__((swift_name("PermanentRedirect")));
@property (readonly) SharedKtor_httpHttpStatusCode *PreconditionFailed __attribute__((swift_name("PreconditionFailed")));
@property (readonly) SharedKtor_httpHttpStatusCode *Processing __attribute__((swift_name("Processing")));
@property (readonly) SharedKtor_httpHttpStatusCode *ProxyAuthenticationRequired __attribute__((swift_name("ProxyAuthenticationRequired")));
@property (readonly) SharedKtor_httpHttpStatusCode *RequestHeaderFieldTooLarge __attribute__((swift_name("RequestHeaderFieldTooLarge")));
@property (readonly) SharedKtor_httpHttpStatusCode *RequestTimeout __attribute__((swift_name("RequestTimeout")));
@property (readonly) SharedKtor_httpHttpStatusCode *RequestURITooLong __attribute__((swift_name("RequestURITooLong")));
@property (readonly) SharedKtor_httpHttpStatusCode *RequestedRangeNotSatisfiable __attribute__((swift_name("RequestedRangeNotSatisfiable")));
@property (readonly) SharedKtor_httpHttpStatusCode *ResetContent __attribute__((swift_name("ResetContent")));
@property (readonly) SharedKtor_httpHttpStatusCode *SeeOther __attribute__((swift_name("SeeOther")));
@property (readonly) SharedKtor_httpHttpStatusCode *ServiceUnavailable __attribute__((swift_name("ServiceUnavailable")));
@property (readonly) SharedKtor_httpHttpStatusCode *SwitchProxy __attribute__((swift_name("SwitchProxy")));
@property (readonly) SharedKtor_httpHttpStatusCode *SwitchingProtocols __attribute__((swift_name("SwitchingProtocols")));
@property (readonly) SharedKtor_httpHttpStatusCode *TemporaryRedirect __attribute__((swift_name("TemporaryRedirect")));
@property (readonly) SharedKtor_httpHttpStatusCode *TooEarly __attribute__((swift_name("TooEarly")));
@property (readonly) SharedKtor_httpHttpStatusCode *TooManyRequests __attribute__((swift_name("TooManyRequests")));
@property (readonly) SharedKtor_httpHttpStatusCode *Unauthorized __attribute__((swift_name("Unauthorized")));
@property (readonly) SharedKtor_httpHttpStatusCode *UnprocessableEntity __attribute__((swift_name("UnprocessableEntity")));
@property (readonly) SharedKtor_httpHttpStatusCode *UnsupportedMediaType __attribute__((swift_name("UnsupportedMediaType")));
@property (readonly) SharedKtor_httpHttpStatusCode *UpgradeRequired __attribute__((swift_name("UpgradeRequired")));
@property (readonly) SharedKtor_httpHttpStatusCode *UseProxy __attribute__((swift_name("UseProxy")));
@property (readonly) SharedKtor_httpHttpStatusCode *VariantAlsoNegotiates __attribute__((swift_name("VariantAlsoNegotiates")));
@property (readonly) SharedKtor_httpHttpStatusCode *VersionNotSupported __attribute__((swift_name("VersionNotSupported")));

/**
 * All known status codes
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HttpStatusCode.Companion.allStatusCodes)
 */
@property (readonly) NSArray<SharedKtor_httpHttpStatusCode *> *allStatusCodes __attribute__((swift_name("allStatusCodes")));
@end


/**
 * Day of week
 * [value] is 3 letter shortcut
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.date.WeekDay)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_utilsWeekDay")))
@interface SharedKtor_utilsWeekDay : SharedKotlinEnum<SharedKtor_utilsWeekDay *>
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Day of week
 * [value] is 3 letter shortcut
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.date.WeekDay)
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedKtor_utilsWeekDayCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) SharedKtor_utilsWeekDay *monday __attribute__((swift_name("monday")));
@property (class, readonly) SharedKtor_utilsWeekDay *tuesday __attribute__((swift_name("tuesday")));
@property (class, readonly) SharedKtor_utilsWeekDay *wednesday __attribute__((swift_name("wednesday")));
@property (class, readonly) SharedKtor_utilsWeekDay *thursday __attribute__((swift_name("thursday")));
@property (class, readonly) SharedKtor_utilsWeekDay *friday __attribute__((swift_name("friday")));
@property (class, readonly) SharedKtor_utilsWeekDay *saturday __attribute__((swift_name("saturday")));
@property (class, readonly) SharedKtor_utilsWeekDay *sunday __attribute__((swift_name("sunday")));
+ (SharedKotlinArray<SharedKtor_utilsWeekDay *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedKtor_utilsWeekDay *> *entries __attribute__((swift_name("entries")));
@property (readonly) NSString *value __attribute__((swift_name("value")));
@end


/**
 * Month
 * [value] is 3 letter shortcut
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.date.Month)
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_utilsMonth")))
@interface SharedKtor_utilsMonth : SharedKotlinEnum<SharedKtor_utilsMonth *>
+ (instancetype)alloc __attribute__((unavailable));

/**
 * Month
 * [value] is 3 letter shortcut
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.date.Month)
 */
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly, getter=companion) SharedKtor_utilsMonthCompanion *companion __attribute__((swift_name("companion")));
@property (class, readonly) SharedKtor_utilsMonth *january __attribute__((swift_name("january")));
@property (class, readonly) SharedKtor_utilsMonth *february __attribute__((swift_name("february")));
@property (class, readonly) SharedKtor_utilsMonth *march __attribute__((swift_name("march")));
@property (class, readonly) SharedKtor_utilsMonth *april __attribute__((swift_name("april")));
@property (class, readonly) SharedKtor_utilsMonth *may __attribute__((swift_name("may")));
@property (class, readonly) SharedKtor_utilsMonth *june __attribute__((swift_name("june")));
@property (class, readonly) SharedKtor_utilsMonth *july __attribute__((swift_name("july")));
@property (class, readonly) SharedKtor_utilsMonth *august __attribute__((swift_name("august")));
@property (class, readonly) SharedKtor_utilsMonth *september __attribute__((swift_name("september")));
@property (class, readonly) SharedKtor_utilsMonth *october __attribute__((swift_name("october")));
@property (class, readonly) SharedKtor_utilsMonth *november __attribute__((swift_name("november")));
@property (class, readonly) SharedKtor_utilsMonth *december __attribute__((swift_name("december")));
+ (SharedKotlinArray<SharedKtor_utilsMonth *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedKtor_utilsMonth *> *entries __attribute__((swift_name("entries")));
@property (readonly) NSString *value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_utilsGMTDate.Companion")))
@interface SharedKtor_utilsGMTDateCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKtor_utilsGMTDateCompanion *shared __attribute__((swift_name("shared")));
- (id<SharedKotlinx_serialization_coreKSerializer>)serializer __attribute__((swift_name("serializer()")));

/**
 * An instance of [GMTDate] corresponding to the epoch beginning
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.date.GMTDate.Companion.START)
 */
@property (readonly) SharedKtor_utilsGMTDate *START __attribute__((swift_name("START")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_httpHttpProtocolVersion.Companion")))
@interface SharedKtor_httpHttpProtocolVersionCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKtor_httpHttpProtocolVersionCompanion *shared __attribute__((swift_name("shared")));

/**
 * Creates an instance of [HttpProtocolVersion] from the given parameters.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HttpProtocolVersion.Companion.fromValue)
 */
- (SharedKtor_httpHttpProtocolVersion *)fromValueName:(NSString *)name major:(int32_t)major minor:(int32_t)minor __attribute__((swift_name("fromValue(name:major:minor:)")));

/**
 * Create an instance of [HttpProtocolVersion] from http string representation.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HttpProtocolVersion.Companion.parse)
 */
- (SharedKtor_httpHttpProtocolVersion *)parseValue:(id)value __attribute__((swift_name("parse(value:)")));

/**
 * HTTP/1.0 version.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HttpProtocolVersion.Companion.HTTP_1_0)
 */
@property (readonly) SharedKtor_httpHttpProtocolVersion *HTTP_1_0 __attribute__((swift_name("HTTP_1_0")));

/**
 * HTTP/1.1 version.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HttpProtocolVersion.Companion.HTTP_1_1)
 */
@property (readonly) SharedKtor_httpHttpProtocolVersion *HTTP_1_1 __attribute__((swift_name("HTTP_1_1")));

/**
 * HTTP/2.0 version.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HttpProtocolVersion.Companion.HTTP_2_0)
 */
@property (readonly) SharedKtor_httpHttpProtocolVersion *HTTP_2_0 __attribute__((swift_name("HTTP_2_0")));

/**
 * HTTP/3.0 version.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HttpProtocolVersion.Companion.HTTP_3_0)
 */
@property (readonly) SharedKtor_httpHttpProtocolVersion *HTTP_3_0 __attribute__((swift_name("HTTP_3_0")));

/**
 * QUIC/1.0 version.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HttpProtocolVersion.Companion.QUIC)
 */
@property (readonly) SharedKtor_httpHttpProtocolVersion *QUIC __attribute__((swift_name("QUIC")));

/**
 * SPDY/3.0 version.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HttpProtocolVersion.Companion.SPDY_3)
 */
@property (readonly) SharedKtor_httpHttpProtocolVersion *SPDY_3 __attribute__((swift_name("SPDY_3")));
@end

__attribute__((swift_name("KotlinKType")))
@protocol SharedKotlinKType
@required

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
@property (readonly) NSArray<SharedKotlinKTypeProjection *> *arguments __attribute__((swift_name("arguments")));

/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
@property (readonly) id<SharedKotlinKClassifier> _Nullable classifier __attribute__((swift_name("classifier")));
@property (readonly) BOOL isMarkedNullable __attribute__((swift_name("isMarkedNullable")));
@end

__attribute__((swift_name("Kotlinx_io_coreRawSource")))
@protocol SharedKotlinx_io_coreRawSource <SharedKotlinAutoCloseable>
@required
- (int64_t)readAtMostToSink:(SharedKotlinx_io_coreBuffer *)sink byteCount:(int64_t)byteCount __attribute__((swift_name("readAtMostTo(sink:byteCount:)")));
@end

__attribute__((swift_name("Kotlinx_io_coreSource")))
@protocol SharedKotlinx_io_coreSource <SharedKotlinx_io_coreRawSource>
@required
- (BOOL)exhausted __attribute__((swift_name("exhausted()")));
- (id<SharedKotlinx_io_coreSource>)peek __attribute__((swift_name("peek()")));
- (int32_t)readAtMostToSink:(SharedKotlinByteArray *)sink startIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("readAtMostTo(sink:startIndex:endIndex:)")));
- (int8_t)readByte __attribute__((swift_name("readByte()")));
- (int32_t)readInt __attribute__((swift_name("readInt()")));
- (int64_t)readLong __attribute__((swift_name("readLong()")));
- (int16_t)readShort __attribute__((swift_name("readShort()")));
- (void)readToSink:(id<SharedKotlinx_io_coreRawSink>)sink byteCount:(int64_t)byteCount __attribute__((swift_name("readTo(sink:byteCount:)")));
- (BOOL)requestByteCount:(int64_t)byteCount __attribute__((swift_name("request(byteCount:)")));
- (void)requireByteCount:(int64_t)byteCount __attribute__((swift_name("require(byteCount:)")));
- (void)skipByteCount:(int64_t)byteCount __attribute__((swift_name("skip(byteCount:)")));
- (int64_t)transferToSink:(id<SharedKotlinx_io_coreRawSink>)sink __attribute__((swift_name("transferTo(sink:)")));

/**
 * @note annotations
 *   kotlinx.io.InternalIoApi
*/
@property (readonly) SharedKotlinx_io_coreBuffer *buffer __attribute__((swift_name("buffer")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_httpURLBuilder.Companion")))
@interface SharedKtor_httpURLBuilderCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKtor_httpURLBuilderCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((swift_name("Ktor_httpParametersBuilder")))
@protocol SharedKtor_httpParametersBuilder <SharedKtor_utilsStringValuesBuilder>
@required
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_httpURLProtocol.Companion")))
@interface SharedKtor_httpURLProtocolCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKtor_httpURLProtocolCompanion *shared __attribute__((swift_name("shared")));

/**
 * Create an instance by [name] or use already existing instance
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.URLProtocol.Companion.createOrDefault)
 */
- (SharedKtor_httpURLProtocol *)createOrDefaultName:(NSString *)name __attribute__((swift_name("createOrDefault(name:)")));

/**
 * HTTP with port 80
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.URLProtocol.Companion.HTTP)
 */
@property (readonly) SharedKtor_httpURLProtocol *HTTP __attribute__((swift_name("HTTP")));

/**
 * secure HTTPS with port 443
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.URLProtocol.Companion.HTTPS)
 */
@property (readonly) SharedKtor_httpURLProtocol *HTTPS __attribute__((swift_name("HTTPS")));

/**
 * Socks proxy url protocol.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.URLProtocol.Companion.SOCKS)
 */
@property (readonly) SharedKtor_httpURLProtocol *SOCKS __attribute__((swift_name("SOCKS")));

/**
 * Web socket over HTTP on port 80
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.URLProtocol.Companion.WS)
 */
@property (readonly) SharedKtor_httpURLProtocol *WS __attribute__((swift_name("WS")));

/**
 * Web socket over secure HTTPS on port 443
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.URLProtocol.Companion.WSS)
 */
@property (readonly) SharedKtor_httpURLProtocol *WSS __attribute__((swift_name("WSS")));

/**
 * Protocols by names map
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.URLProtocol.Companion.byName)
 */
@property (readonly) NSDictionary<NSString *, SharedKtor_httpURLProtocol *> *byName __attribute__((swift_name("byName")));
@end


/**
 * Represents a single value parameter
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HeaderValueParam)
 *
 * @property name of parameter
 * @property value of parameter
 * @property escapeValue specifies if the value should be escaped
 */
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_httpHeaderValueParam")))
@interface SharedKtor_httpHeaderValueParam : SharedBase
- (instancetype)initWithName:(NSString *)name value:(NSString *)value __attribute__((swift_name("init(name:value:)"))) __attribute__((objc_designated_initializer));
- (instancetype)initWithName:(NSString *)name value:(NSString *)value escapeValue:(BOOL)escapeValue __attribute__((swift_name("init(name:value:escapeValue:)"))) __attribute__((objc_designated_initializer));
- (SharedKtor_httpHeaderValueParam *)doCopyName:(NSString *)name value:(NSString *)value escapeValue:(BOOL)escapeValue __attribute__((swift_name("doCopy(name:value:escapeValue:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));

/**
 * Represents a single value parameter
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HeaderValueParam)
 *
 * @property name of parameter
 * @property value of parameter
 * @property escapeValue specifies if the value should be escaped
 */
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL escapeValue __attribute__((swift_name("escapeValue")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) NSString *value __attribute__((swift_name("value")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_httpHeaderValueWithParameters.Companion")))
@interface SharedKtor_httpHeaderValueWithParametersCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKtor_httpHeaderValueWithParametersCompanion *shared __attribute__((swift_name("shared")));

/**
 * Parse header with parameter and pass it to [init] function to instantiate particular type
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.HeaderValueWithParameters.Companion.parse)
 */
- (id _Nullable)parseValue:(NSString *)value init:(id _Nullable (^)(NSString *, NSArray<SharedKtor_httpHeaderValueParam *> *))init __attribute__((swift_name("parse(value:init:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_httpContentType.Companion")))
@interface SharedKtor_httpContentTypeCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKtor_httpContentTypeCompanion *shared __attribute__((swift_name("shared")));

/**
 * Parses a string representing a `Content-Type` header into a [ContentType] instance.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.ContentType.Companion.parse)
 */
- (SharedKtor_httpContentType *)parseValue:(NSString *)value __attribute__((swift_name("parse(value:)")));

/**
 * Represents a pattern `* / *` to match any content type.
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.http.ContentType.Companion.Any)
 */
@property (readonly) SharedKtor_httpContentType *Any __attribute__((swift_name("Any")));
@end


/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
__attribute__((swift_name("Kotlinx_coroutines_coreParentJob")))
@protocol SharedKotlinx_coroutines_coreParentJob <SharedKotlinx_coroutines_coreJob>
@required

/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
- (SharedKotlinCancellationException *)getChildJobCancellationCause __attribute__((swift_name("getChildJobCancellationCause()")));
@end


/**
 * @note annotations
 *   kotlinx.coroutines.InternalCoroutinesApi
*/
__attribute__((swift_name("Kotlinx_coroutines_coreSelectInstance")))
@protocol SharedKotlinx_coroutines_coreSelectInstance
@required
- (void)disposeOnCompletionDisposableHandle:(id<SharedKotlinx_coroutines_coreDisposableHandle>)disposableHandle __attribute__((swift_name("disposeOnCompletion(disposableHandle:)")));
- (void)selectInRegistrationPhaseInternalResult:(id _Nullable)internalResult __attribute__((swift_name("selectInRegistrationPhase(internalResult:)")));
- (BOOL)trySelectClauseObject:(id)clauseObject result:(id _Nullable)result __attribute__((swift_name("trySelect(clauseObject:result:)")));
@property (readonly) id<SharedKotlinCoroutineContext> context __attribute__((swift_name("context")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_utilsWeekDay.Companion")))
@interface SharedKtor_utilsWeekDayCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKtor_utilsWeekDayCompanion *shared __attribute__((swift_name("shared")));

/**
 * Lookup an instance by [ordinal]
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.date.WeekDay.Companion.from)
 */
- (SharedKtor_utilsWeekDay *)fromOrdinal:(int32_t)ordinal __attribute__((swift_name("from(ordinal:)")));

/**
 * Lookup an instance by short week day name [WeekDay.value]
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.date.WeekDay.Companion.from)
 */
- (SharedKtor_utilsWeekDay *)fromValue:(NSString *)value __attribute__((swift_name("from(value:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Ktor_utilsMonth.Companion")))
@interface SharedKtor_utilsMonthCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKtor_utilsMonthCompanion *shared __attribute__((swift_name("shared")));

/**
 * Lookup an instance by [ordinal]
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.date.Month.Companion.from)
 */
- (SharedKtor_utilsMonth *)fromOrdinal:(int32_t)ordinal __attribute__((swift_name("from(ordinal:)")));

/**
 * Lookup an instance by short month name [Month.value]
 *
 * [Report a problem](https://ktor.io/feedback/?fqname=io.ktor.util.date.Month.Companion.from)
 */
- (SharedKtor_utilsMonth *)fromValue:(NSString *)value __attribute__((swift_name("from(value:)")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinKTypeProjection")))
@interface SharedKotlinKTypeProjection : SharedBase
- (instancetype)initWithVariance:(SharedKotlinKVariance * _Nullable)variance type:(id<SharedKotlinKType> _Nullable)type __attribute__((swift_name("init(variance:type:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedKotlinKTypeProjectionCompanion *companion __attribute__((swift_name("companion")));
- (SharedKotlinKTypeProjection *)doCopyVariance:(SharedKotlinKVariance * _Nullable)variance type:(id<SharedKotlinKType> _Nullable)type __attribute__((swift_name("doCopy(variance:type:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) id<SharedKotlinKType> _Nullable type __attribute__((swift_name("type")));
@property (readonly) SharedKotlinKVariance * _Nullable variance __attribute__((swift_name("variance")));
@end

__attribute__((swift_name("Kotlinx_io_coreRawSink")))
@protocol SharedKotlinx_io_coreRawSink <SharedKotlinAutoCloseable>
@required
- (void)flush __attribute__((swift_name("flush()")));
- (void)writeSource:(SharedKotlinx_io_coreBuffer *)source byteCount:(int64_t)byteCount __attribute__((swift_name("write(source:byteCount:)")));
@end

__attribute__((swift_name("Kotlinx_io_coreSink")))
@protocol SharedKotlinx_io_coreSink <SharedKotlinx_io_coreRawSink>
@required
- (void)emit __attribute__((swift_name("emit()")));

/**
 * @note annotations
 *   kotlinx.io.InternalIoApi
*/
- (void)hintEmit __attribute__((swift_name("hintEmit()")));
- (int64_t)transferFromSource:(id<SharedKotlinx_io_coreRawSource>)source __attribute__((swift_name("transferFrom(source:)")));
- (void)writeSource:(id<SharedKotlinx_io_coreRawSource>)source byteCount_:(int64_t)byteCount __attribute__((swift_name("write(source:byteCount_:)")));
- (void)writeSource:(SharedKotlinByteArray *)source startIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("write(source:startIndex:endIndex:)")));
- (void)writeByteByte:(int8_t)byte __attribute__((swift_name("writeByte(byte:)")));
- (void)writeIntInt:(int32_t)int_ __attribute__((swift_name("writeInt(int:)")));
- (void)writeLongLong:(int64_t)long_ __attribute__((swift_name("writeLong(long:)")));
- (void)writeShortShort:(int16_t)short_ __attribute__((swift_name("writeShort(short:)")));

/**
 * @note annotations
 *   kotlinx.io.InternalIoApi
*/
@property (readonly) SharedKotlinx_io_coreBuffer *buffer __attribute__((swift_name("buffer")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Kotlinx_io_coreBuffer")))
@interface SharedKotlinx_io_coreBuffer : SharedBase <SharedKotlinx_io_coreSource, SharedKotlinx_io_coreSink>
- (instancetype)init __attribute__((swift_name("init()"))) __attribute__((objc_designated_initializer));
+ (instancetype)new __attribute__((availability(swift, unavailable, message="use object initializers instead")));
- (void)clear __attribute__((swift_name("clear()")));
- (void)close __attribute__((swift_name("close()")));
- (SharedKotlinx_io_coreBuffer *)doCopy __attribute__((swift_name("doCopy()")));
- (void)doCopyToOut:(SharedKotlinx_io_coreBuffer *)out startIndex:(int64_t)startIndex endIndex:(int64_t)endIndex __attribute__((swift_name("doCopyTo(out:startIndex:endIndex:)")));
- (void)emit __attribute__((swift_name("emit()")));
- (BOOL)exhausted __attribute__((swift_name("exhausted()")));
- (void)flush __attribute__((swift_name("flush()")));
- (int8_t)getPosition:(int64_t)position __attribute__((swift_name("get(position:)")));

/**
 * @note annotations
 *   kotlinx.io.InternalIoApi
*/
- (void)hintEmit __attribute__((swift_name("hintEmit()")));
- (id<SharedKotlinx_io_coreSource>)peek __attribute__((swift_name("peek()")));
- (int64_t)readAtMostToSink:(SharedKotlinx_io_coreBuffer *)sink byteCount:(int64_t)byteCount __attribute__((swift_name("readAtMostTo(sink:byteCount:)")));
- (int32_t)readAtMostToSink:(SharedKotlinByteArray *)sink startIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("readAtMostTo(sink:startIndex:endIndex:)")));
- (int8_t)readByte __attribute__((swift_name("readByte()")));
- (int32_t)readInt __attribute__((swift_name("readInt()")));
- (int64_t)readLong __attribute__((swift_name("readLong()")));
- (int16_t)readShort __attribute__((swift_name("readShort()")));
- (void)readToSink:(id<SharedKotlinx_io_coreRawSink>)sink byteCount:(int64_t)byteCount __attribute__((swift_name("readTo(sink:byteCount:)")));
- (BOOL)requestByteCount:(int64_t)byteCount __attribute__((swift_name("request(byteCount:)")));
- (void)requireByteCount:(int64_t)byteCount __attribute__((swift_name("require(byteCount:)")));
- (void)skipByteCount:(int64_t)byteCount __attribute__((swift_name("skip(byteCount:)")));
- (NSString *)description __attribute__((swift_name("description()")));
- (int64_t)transferFromSource:(id<SharedKotlinx_io_coreRawSource>)source __attribute__((swift_name("transferFrom(source:)")));
- (int64_t)transferToSink:(id<SharedKotlinx_io_coreRawSink>)sink __attribute__((swift_name("transferTo(sink:)")));
- (void)writeSource:(SharedKotlinx_io_coreBuffer *)source byteCount:(int64_t)byteCount __attribute__((swift_name("write(source:byteCount:)")));
- (void)writeSource:(id<SharedKotlinx_io_coreRawSource>)source byteCount_:(int64_t)byteCount __attribute__((swift_name("write(source:byteCount_:)")));
- (void)writeSource:(SharedKotlinByteArray *)source startIndex:(int32_t)startIndex endIndex:(int32_t)endIndex __attribute__((swift_name("write(source:startIndex:endIndex:)")));
- (void)writeByteByte:(int8_t)byte __attribute__((swift_name("writeByte(byte:)")));
- (void)writeIntInt:(int32_t)int_ __attribute__((swift_name("writeInt(int:)")));
- (void)writeLongLong:(int64_t)long_ __attribute__((swift_name("writeLong(long:)")));
- (void)writeShortShort:(int16_t)short_ __attribute__((swift_name("writeShort(short:)")));

/**
 * @note annotations
 *   kotlinx.io.InternalIoApi
*/
@property (readonly) SharedKotlinx_io_coreBuffer *buffer __attribute__((swift_name("buffer")));
@property (readonly) int64_t size __attribute__((swift_name("size")));
@end


/**
 * @note annotations
 *   kotlin.SinceKotlin(version="1.1")
*/
__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinKVariance")))
@interface SharedKotlinKVariance : SharedKotlinEnum<SharedKotlinKVariance *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedKotlinKVariance *invariant __attribute__((swift_name("invariant")));
@property (class, readonly) SharedKotlinKVariance *in __attribute__((swift_name("in")));
@property (class, readonly) SharedKotlinKVariance *out __attribute__((swift_name("out")));
+ (SharedKotlinArray<SharedKotlinKVariance *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedKotlinKVariance *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinKTypeProjection.Companion")))
@interface SharedKotlinKTypeProjectionCompanion : SharedBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedKotlinKTypeProjectionCompanion *shared __attribute__((swift_name("shared")));

/**
 * @note annotations
 *   kotlin.jvm.JvmStatic
*/
- (SharedKotlinKTypeProjection *)contravariantType:(id<SharedKotlinKType>)type __attribute__((swift_name("contravariant(type:)")));

/**
 * @note annotations
 *   kotlin.jvm.JvmStatic
*/
- (SharedKotlinKTypeProjection *)covariantType:(id<SharedKotlinKType>)type __attribute__((swift_name("covariant(type:)")));

/**
 * @note annotations
 *   kotlin.jvm.JvmStatic
*/
- (SharedKotlinKTypeProjection *)invariantType:(id<SharedKotlinKType>)type __attribute__((swift_name("invariant(type:)")));
@property (readonly) SharedKotlinKTypeProjection *STAR __attribute__((swift_name("STAR")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
