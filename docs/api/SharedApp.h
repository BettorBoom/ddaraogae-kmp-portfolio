#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSError.h>
#import <Foundation/NSObject.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSString.h>
#import <Foundation/NSValue.h>

@class SharedAppAppRuntime, SharedAppKotlinArray<T>, SharedAppKotlinEnum<E>, SharedAppKotlinEnumCompanion, SharedAppWalkSession, SharedAppWalkSessionState, SharedAppWalkStatus, UIViewController;

@protocol SharedAppActiveWalkObserver, SharedAppBackendUserObserver, SharedAppCancelHandle, SharedAppKotlinComparable, SharedAppKotlinIterator, SharedAppNativeBackend;

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
@interface SharedAppBase : NSObject
- (instancetype)init __attribute__((unavailable));
+ (instancetype)new __attribute__((unavailable));
+ (void)initialize __attribute__((objc_requires_super));
@end

@interface SharedAppBase (SharedAppBaseCopying) <NSCopying>
@end

__attribute__((swift_name("KotlinMutableSet")))
@interface SharedAppMutableSet<ObjectType> : NSMutableSet<ObjectType>
@end

__attribute__((swift_name("KotlinMutableDictionary")))
@interface SharedAppMutableDictionary<KeyType, ObjectType> : NSMutableDictionary<KeyType, ObjectType>
@end

@interface NSError (NSErrorSharedAppKotlinException)
@property (readonly) id _Nullable kotlinException;
@end

__attribute__((swift_name("KotlinNumber")))
@interface SharedAppNumber : NSNumber
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
@interface SharedAppByte : SharedAppNumber
- (instancetype)initWithChar:(char)value;
+ (instancetype)numberWithChar:(char)value;
@end

__attribute__((swift_name("KotlinUByte")))
@interface SharedAppUByte : SharedAppNumber
- (instancetype)initWithUnsignedChar:(unsigned char)value;
+ (instancetype)numberWithUnsignedChar:(unsigned char)value;
@end

__attribute__((swift_name("KotlinShort")))
@interface SharedAppShort : SharedAppNumber
- (instancetype)initWithShort:(short)value;
+ (instancetype)numberWithShort:(short)value;
@end

__attribute__((swift_name("KotlinUShort")))
@interface SharedAppUShort : SharedAppNumber
- (instancetype)initWithUnsignedShort:(unsigned short)value;
+ (instancetype)numberWithUnsignedShort:(unsigned short)value;
@end

__attribute__((swift_name("KotlinInt")))
@interface SharedAppInt : SharedAppNumber
- (instancetype)initWithInt:(int)value;
+ (instancetype)numberWithInt:(int)value;
@end

__attribute__((swift_name("KotlinUInt")))
@interface SharedAppUInt : SharedAppNumber
- (instancetype)initWithUnsignedInt:(unsigned int)value;
+ (instancetype)numberWithUnsignedInt:(unsigned int)value;
@end

__attribute__((swift_name("KotlinLong")))
@interface SharedAppLong : SharedAppNumber
- (instancetype)initWithLongLong:(long long)value;
+ (instancetype)numberWithLongLong:(long long)value;
@end

__attribute__((swift_name("KotlinULong")))
@interface SharedAppULong : SharedAppNumber
- (instancetype)initWithUnsignedLongLong:(unsigned long long)value;
+ (instancetype)numberWithUnsignedLongLong:(unsigned long long)value;
@end

__attribute__((swift_name("KotlinFloat")))
@interface SharedAppFloat : SharedAppNumber
- (instancetype)initWithFloat:(float)value;
+ (instancetype)numberWithFloat:(float)value;
@end

__attribute__((swift_name("KotlinDouble")))
@interface SharedAppDouble : SharedAppNumber
- (instancetype)initWithDouble:(double)value;
+ (instancetype)numberWithDouble:(double)value;
@end

__attribute__((swift_name("KotlinBoolean")))
@interface SharedAppBoolean : SharedAppNumber
- (instancetype)initWithBool:(BOOL)value;
+ (instancetype)numberWithBool:(BOOL)value;
@end

__attribute__((swift_name("BackendUserObserver")))
@protocol SharedAppBackendUserObserver
@required
- (void)onUserUserId:(NSString * _Nullable)userId __attribute__((swift_name("onUser(userId:)")));
@end

__attribute__((swift_name("CancelHandle")))
@protocol SharedAppCancelHandle
@required
- (void)cancel __attribute__((swift_name("cancel()")));
@end

__attribute__((swift_name("NativeBackend")))
@protocol SharedAppNativeBackend
@required
- (id<SharedAppCancelHandle>)observeUserObserver:(id<SharedAppBackendUserObserver>)observer __attribute__((swift_name("observeUser(observer:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WalkSession")))
@interface SharedAppWalkSession : SharedAppBase
- (instancetype)initWithId:(NSString *)id userId:(NSString *)userId startedAtMillis:(int64_t)startedAtMillis status:(SharedAppWalkStatus *)status endedAtMillis:(SharedAppLong * _Nullable)endedAtMillis dogName:(NSString * _Nullable)dogName distanceMeters:(double)distanceMeters activeDurationMillis:(int64_t)activeDurationMillis lastResumedAtMillis:(SharedAppLong * _Nullable)lastResumedAtMillis segment:(int32_t)segment segmentStartedAtMillis:(SharedAppLong * _Nullable)segmentStartedAtMillis __attribute__((swift_name("init(id:userId:startedAtMillis:status:endedAtMillis:dogName:distanceMeters:activeDurationMillis:lastResumedAtMillis:segment:segmentStartedAtMillis:)"))) __attribute__((objc_designated_initializer));
- (SharedAppWalkSession *)doCopyId:(NSString *)id userId:(NSString *)userId startedAtMillis:(int64_t)startedAtMillis status:(SharedAppWalkStatus *)status endedAtMillis:(SharedAppLong * _Nullable)endedAtMillis dogName:(NSString * _Nullable)dogName distanceMeters:(double)distanceMeters activeDurationMillis:(int64_t)activeDurationMillis lastResumedAtMillis:(SharedAppLong * _Nullable)lastResumedAtMillis segment:(int32_t)segment segmentStartedAtMillis:(SharedAppLong * _Nullable)segmentStartedAtMillis __attribute__((swift_name("doCopy(id:userId:startedAtMillis:status:endedAtMillis:dogName:distanceMeters:activeDurationMillis:lastResumedAtMillis:segment:segmentStartedAtMillis:)")));
- (int64_t)elapsedMillisNowMillis:(int64_t)nowMillis __attribute__((swift_name("elapsedMillis(nowMillis:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) int64_t activeDurationMillis __attribute__((swift_name("activeDurationMillis")));
@property (readonly) double distanceMeters __attribute__((swift_name("distanceMeters")));
@property (readonly) NSString * _Nullable dogName __attribute__((swift_name("dogName")));
@property (readonly) SharedAppLong * _Nullable endedAtMillis __attribute__((swift_name("endedAtMillis")));
@property (readonly) NSString *id __attribute__((swift_name("id")));
@property (readonly) SharedAppLong * _Nullable lastResumedAtMillis __attribute__((swift_name("lastResumedAtMillis")));
@property (readonly) int32_t segment __attribute__((swift_name("segment")));
@property (readonly) SharedAppLong * _Nullable segmentStartedAtMillis __attribute__((swift_name("segmentStartedAtMillis")));
@property (readonly) int64_t startedAtMillis __attribute__((swift_name("startedAtMillis")));
@property (readonly) SharedAppWalkStatus *status __attribute__((swift_name("status")));
@property (readonly) NSString *userId __attribute__((swift_name("userId")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WalkSessionState")))
@interface SharedAppWalkSessionState : SharedAppBase
- (instancetype)initWithSession:(SharedAppWalkSession * _Nullable)session loaded:(BOOL)loaded failed:(BOOL)failed __attribute__((swift_name("init(session:loaded:failed:)"))) __attribute__((objc_designated_initializer));
- (SharedAppWalkSessionState *)doCopySession:(SharedAppWalkSession * _Nullable)session loaded:(BOOL)loaded failed:(BOOL)failed __attribute__((swift_name("doCopy(session:loaded:failed:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) BOOL failed __attribute__((swift_name("failed")));
@property (readonly) BOOL loaded __attribute__((swift_name("loaded")));
@property (readonly) SharedAppWalkSession * _Nullable session __attribute__((swift_name("session")));
@end

__attribute__((swift_name("KotlinComparable")))
@protocol SharedAppKotlinComparable
@required
- (int32_t)compareToOther:(id _Nullable)other __attribute__((swift_name("compareTo(other:)")));
@end

__attribute__((swift_name("KotlinEnum")))
@interface SharedAppKotlinEnum<E> : SharedAppBase <SharedAppKotlinComparable>
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer));
@property (class, readonly, getter=companion) SharedAppKotlinEnumCompanion *companion __attribute__((swift_name("companion")));
- (int32_t)compareToOther:(E)other __attribute__((swift_name("compareTo(other:)")));
- (BOOL)isEqual:(id _Nullable)other __attribute__((swift_name("isEqual(_:)")));
- (NSUInteger)hash __attribute__((swift_name("hash()")));
- (NSString *)description __attribute__((swift_name("description()")));
@property (readonly) NSString *name __attribute__((swift_name("name")));
@property (readonly) int32_t ordinal __attribute__((swift_name("ordinal")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("WalkStatus")))
@interface SharedAppWalkStatus : SharedAppKotlinEnum<SharedAppWalkStatus *>
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (instancetype)initWithName:(NSString *)name ordinal:(int32_t)ordinal __attribute__((swift_name("init(name:ordinal:)"))) __attribute__((objc_designated_initializer)) __attribute__((unavailable));
@property (class, readonly) SharedAppWalkStatus *recording __attribute__((swift_name("recording")));
@property (class, readonly) SharedAppWalkStatus *completed __attribute__((swift_name("completed")));
@property (class, readonly) SharedAppWalkStatus *interrupted __attribute__((swift_name("interrupted")));
+ (SharedAppKotlinArray<SharedAppWalkStatus *> *)values __attribute__((swift_name("values()")));
@property (class, readonly) NSArray<SharedAppWalkStatus *> *entries __attribute__((swift_name("entries")));
@end

__attribute__((swift_name("ActiveWalkObserver")))
@protocol SharedAppActiveWalkObserver
@required
- (void)onActiveWalkWalkId:(NSString * _Nullable)walkId segment:(int32_t)segment __attribute__((swift_name("onActiveWalk(walkId:segment:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("AppRuntime")))
@interface SharedAppAppRuntime : SharedAppBase
- (void)close __attribute__((swift_name("close()")));
- (id<SharedAppCancelHandle>)observeActiveWalkObserver:(id<SharedAppActiveWalkObserver>)observer __attribute__((swift_name("observeActiveWalk(observer:)")));
- (void)pauseForBackground __attribute__((swift_name("pauseForBackground()")));
- (void)setLocationNoticeMessage:(NSString * _Nullable)message __attribute__((swift_name("setLocationNotice(message:)")));
- (void)submitNativeLocationWalkId:(NSString *)walkId segment:(int32_t)segment latitude:(double)latitude longitude:(double)longitude recordedAtMillis:(int64_t)recordedAtMillis accuracyMeters:(double)accuracyMeters __attribute__((swift_name("submitNativeLocation(walkId:segment:latitude:longitude:recordedAtMillis:accuracyMeters:)")));
@property (readonly) BOOL isClosed __attribute__((swift_name("isClosed")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("IosEntryKt")))
@interface SharedAppIosEntryKt : SharedAppBase
+ (SharedAppAppRuntime *)createIosRuntimeDatabasePath:(NSString *)databasePath backend:(id<SharedAppNativeBackend>)backend __attribute__((swift_name("createIosRuntime(databasePath:backend:)")));
+ (UIViewController *)mainViewControllerRuntime:(SharedAppAppRuntime *)runtime __attribute__((swift_name("mainViewController(runtime:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("Store_iosKt")))
@interface SharedAppStore_iosKt : SharedAppBase
+ (id)openIosWalkStorePath:(NSString *)path __attribute__((swift_name("openIosWalkStore(path:)")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinEnumCompanion")))
@interface SharedAppKotlinEnumCompanion : SharedAppBase
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
+ (instancetype)companion __attribute__((swift_name("init()")));
@property (class, readonly, getter=shared) SharedAppKotlinEnumCompanion *shared __attribute__((swift_name("shared")));
@end

__attribute__((objc_subclassing_restricted))
__attribute__((swift_name("KotlinArray")))
@interface SharedAppKotlinArray<T> : SharedAppBase
+ (instancetype)arrayWithSize:(int32_t)size init:(T _Nullable (^)(SharedAppInt *))init __attribute__((swift_name("init(size:init:)")));
+ (instancetype)alloc __attribute__((unavailable));
+ (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
- (T _Nullable)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
- (id<SharedAppKotlinIterator>)iterator __attribute__((swift_name("iterator()")));
- (void)setIndex:(int32_t)index value:(T _Nullable)value __attribute__((swift_name("set(index:value:)")));
@property (readonly) int32_t size __attribute__((swift_name("size")));
@end

__attribute__((swift_name("KotlinIterator")))
@protocol SharedAppKotlinIterator
@required
- (BOOL)hasNext __attribute__((swift_name("hasNext()")));
- (id _Nullable)next __attribute__((swift_name("next()")));
@end

#pragma pop_macro("_Nullable_result")
#pragma clang diagnostic pop
NS_ASSUME_NONNULL_END
