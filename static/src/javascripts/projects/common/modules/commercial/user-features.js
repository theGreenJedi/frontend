/* jscs:disable disallowDanglingUnderscores */
define([
    'common/utils/ajax-promise',
    'common/utils/cookies',
    'common/utils/config',
    'common/utils/storage',
    'common/modules/identity/api',
    'lodash/utilities/noop'
], function (
    ajaxPromise,
    cookies,
    config,
    storage,
    identity,
    noop
) {
    var userFeatures, PERSISTENCE_KEYS;

    userFeatures = {
        refresh : refresh,
        isAdfree : isAdfree,
        isPayingMember : isPayingMember,

        /* Test methods */
        _requestNewData : requestNewData,
        _deleteOldData : deleteOldData,
        _persistResponse : persistResponse
    };

    PERSISTENCE_KEYS = {
        ADFREE_COOKIE : 'gu_adfree_user',
        USER_FEATURES_EXPIRY_COOKIE : 'gu_user_features_expiry',
        PAYING_MEMBER_COOKIE : 'gu_paying_member'
    };

    function refresh() {
        if (identity.isUserLoggedIn() && needNewFeatureData()) {
            userFeatures._requestNewData();
        }
        if (haveDataAfterSignout()) {
            userFeatures._deleteOldData();
        }
    }

    function isAdfree() {
        // Defer to the value set by the preflight scripts
        // They need to determine how the page will appear before it starts rendering

        // This field might not be added if the feature switch is off
        if (config.commercial === undefined || config.commercial.showingAdfree === undefined) {
            return false;
        } else {
            return config.commercial.showingAdfree;
        }
    }

    function isPayingMember() {
        // Does NOT check if data has expired
        // If the user is logged in, but has no cookie yet, play it safe and assume they're a paying user
        return identity.isUserLoggedIn() && cookies.get(PERSISTENCE_KEYS.PAYING_MEMBER_COOKIE) !== 'false';
    }

    function needNewFeatureData() {
        return !hasAllFeaturesData() || featuresDataIsOld();
    }

    function haveDataAfterSignout() {
        return (!identity.isUserLoggedIn() && hasAnyFeaturesData());
    }

    function hasAllFeaturesData() {
        return cookies.get(PERSISTENCE_KEYS.ADFREE_COOKIE) &&
            cookies.get(PERSISTENCE_KEYS.USER_FEATURES_EXPIRY_COOKIE) &&
            cookies.get(PERSISTENCE_KEYS.PAYING_MEMBER_COOKIE);
    }

    function hasAnyFeaturesData() {
        return cookies.get(PERSISTENCE_KEYS.ADFREE_COOKIE) ||
            cookies.get(PERSISTENCE_KEYS.USER_FEATURES_EXPIRY_COOKIE) ||
            cookies.get(PERSISTENCE_KEYS.PAYING_MEMBER_COOKIE);
    }

    function featuresDataIsOld() {
        var featuresExpiryCookie = cookies.get(PERSISTENCE_KEYS.USER_FEATURES_EXPIRY_COOKIE),
            featuresExpiryTime = parseInt(featuresExpiryCookie, 10),
            timeNow = new Date().getTime();
        return timeNow >= featuresExpiryTime;
    }

    function deleteOldData() {
        // We expect adfree cookies to be cleaned up by the logout process, but what if the user's login simply times out?
        cookies.remove(PERSISTENCE_KEYS.ADFREE_COOKIE);
        cookies.remove(PERSISTENCE_KEYS.USER_FEATURES_EXPIRY_COOKIE);
        cookies.remove(PERSISTENCE_KEYS.PAYING_MEMBER_COOKIE);
    }

    function requestNewData() {
        ajaxPromise({
            url : config.page.userAttributesApiUrl + '/me/features',
            crossOrigin : true,
            withCredentials : true,
            error : function () {}
        }).then(persistResponse, noop);
    }

    function persistResponse(JsonResponse) {
        var expiryDate = new Date();
        expiryDate.setDate(expiryDate.getDate() + 1);
        cookies.add(PERSISTENCE_KEYS.ADFREE_COOKIE, JsonResponse.adFree);
        cookies.add(PERSISTENCE_KEYS.USER_FEATURES_EXPIRY_COOKIE, expiryDate.getTime().toString());
        cookies.add(PERSISTENCE_KEYS.PAYING_MEMBER_COOKIE, !JsonResponse.adblockMessage);
    }

    return userFeatures;
});
/* jscs:enable */
